package com.github.gezimos.inkos.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Constants.Theme.Light
import com.github.gezimos.inkos.data.Constants.Theme.System
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager
class ExtrasFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var dialogBuilder: DialogManager

    // Paging state
    private var currentPage = intArrayOf(0)
    private var pageCount = intArrayOf(1)
    private var bottomInsetPx = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val isDark = when (prefs.appTheme) {
            Light -> false
            Dark -> true
            System -> com.github.gezimos.inkos.helper.isSystemInDarkMode(requireContext())
        }
        val backgroundColor = com.github.gezimos.inkos.helper.getHexForOpacity(prefs)
        val settingsSize = (prefs.settingsSize - 3)
        val context = requireContext()

        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Sticky header ComposeView
        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent {
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = "Extras",
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                if (pageCount[0] > 1)
                                    com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                        currentPage = currentPage[0],
                                        pageCount = pageCount[0]
                                    )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        
                    }
                }
            }
        }
        rootLayout.addView(headerView)

        val nestedScrollView = NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                androidx.compose.ui.platform.ComposeView(context).apply {
                    setContent {
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    ExtrasSettingsAllInOne(settingsSize.sp, isDark)
                                }
                            }
                        }
                    }
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        // Attach eink scroll behavior
        EinkScrollBehavior(context).attachToScrollView(nestedScrollView)

        // Handle bottom insets for navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        rootLayout.addView(
            nestedScrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Apply bottom padding to prevent scroll view from going under navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

        // Use EinkScrollBehavior callback to update page indicator reliably
        val scrollBehavior = EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            headerView.setContent {
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = "Extras",
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                if (pageCount[0] > 1)
                                    com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                        currentPage = currentPage[0],
                                        pageCount = pageCount[0]
                                    )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        
                    }
                }
            }
        }
        scrollBehavior.attachToScrollView(nestedScrollView)
        return rootLayout
    }

    @Composable
    fun ExtrasSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified, isDark: Boolean) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val einkRefreshEnabled = remember { mutableStateOf(prefs.einkRefreshEnabled) }
        val einkRefreshDelayState = remember { mutableStateOf(prefs.einkRefreshDelay) }
        val useVolumeKeys = remember { mutableStateOf(prefs.useVolumeKeysForPages) }
        val selectedShortcuts = remember { mutableStateOf(prefs.selectedSystemShortcuts.toSet()) }
        val einkHelperEnabled = remember { mutableStateOf(prefs.einkHelperEnabled) }
        val navController = findNavController()
        Column(modifier = Modifier.fillMaxSize()) {
            DashedSeparator()
            SettingsTitle(
                text = stringResource(R.string.eink_auto_mode),
                fontSize = titleFontSize,
            )
            DashedSeparator()
            SettingsSwitch(
                text = stringResource(R.string.eink_auto_mode),
                fontSize = titleFontSize,
                defaultState = einkHelperEnabled.value,
                onCheckedChange = {
                    einkHelperEnabled.value = it
                    prefs.einkHelperEnabled = it
                    requireActivity().recreate()
                }
            )
            DashedSeparator()
            SettingsTitle(
                text = "Extra Features",
                fontSize = titleFontSize,
            )
            DashedSeparator()
            SettingsSwitch(
                text = "Auto E-Ink Refresh",
                fontSize = titleFontSize,
                defaultState = einkRefreshEnabled.value,
                onCheckedChange = {
                    einkRefreshEnabled.value = it
                    prefs.einkRefreshEnabled = it
                }
            )
            DashedSeparator()
            SettingsSelect(
                title = "E-Ink Refresh Delay",
                option = "${einkRefreshDelayState.value} ms",
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "E-Ink Refresh Delay",
                        minValue = Constants.MIN_EINK_REFRESH_DELAY,
                        maxValue = Constants.MAX_EINK_REFRESH_DELAY,
                        currentValue = einkRefreshDelayState.value,
                        onValueSelected = { newDelay: Int ->
                            val snapped = ((newDelay + 12) / 25) * 25
                            einkRefreshDelayState.value = snapped
                            prefs.einkRefreshDelay = snapped
                        }
                    )
                }
            )
            DashedSeparator()
            SettingsSwitch(
                text = getString(R.string.use_volume_keys_for_pages),
                fontSize = titleFontSize,
                defaultState = useVolumeKeys.value,
                onCheckedChange = {
                    useVolumeKeys.value = it
                    prefs.useVolumeKeysForPages = it
                }
            )
            DashedSeparator()
            SettingsSelect(
                title = getString(R.string.system_shortcuts),
                option = if (selectedShortcuts.value.isEmpty()) "None" else "${selectedShortcuts.value.size} selected",
                fontSize = titleFontSize,
                onClick = {
                    val allShortcuts =
                        com.github.gezimos.inkos.helper.SystemShortcutHelper.systemShortcuts
                            .sortedBy { it.displayName.lowercase() }
                    val shortcutLabels = allShortcuts.map { it.displayName }
                    val shortcutIds = allShortcuts.map { it.packageId }
                    val checked =
                        shortcutIds.map { selectedShortcuts.value.contains(it) }.toBooleanArray()
                    dialogBuilder.showMultiChoiceDialog(
                        context = requireContext(),
                        title = getString(R.string.system_shortcuts),
                        items = shortcutLabels.toTypedArray(),
                        initialChecked = checked,
                        onConfirm = { selectedIndices ->
                            val selected = selectedIndices.map { shortcutIds[it] }.toMutableSet()
                            selectedShortcuts.value = selected
                            prefs.selectedSystemShortcuts = selected

                            // Remove hidden status for unchecked system shortcuts
                            val hiddenAppsSet = prefs.hiddenApps.toMutableSet()
                            val allShortcutKeys = shortcutIds.map {
                                it + "|" + android.os.Process.myUserHandle().toString()
                            }
                            val uncheckedShortcutKeys =
                                allShortcutKeys.filter { !selected.contains(it.substringBefore("|")) }
                            hiddenAppsSet.removeAll(uncheckedShortcutKeys)
                            prefs.hiddenApps = hiddenAppsSet
                        }
                    )
                }
            )
            DashedSeparator()
            SettingsSelect(
                title = "mKompakt Bluetooth",
                option = "Devices",
                fontSize = titleFontSize,
                onClick = {
                    navController.navigate(R.id.bluetoothFragment)
                }
            )
            DashedSeparator()
        }
    }
}
