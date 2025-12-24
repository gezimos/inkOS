package com.github.gezimos.inkos.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager

class DrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var dialogBuilder: DialogManager

    // Paging state
    private var currentPage = intArrayOf(0)
    private var pageCount = intArrayOf(1)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        prefs = viewModel.getPrefs()
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val backgroundColor = getHexForOpacity(requireContext())
        val isDark = prefs.appTheme == Constants.Theme.Dark
        val context = requireContext()

        // Create a vertical LinearLayout to hold sticky header and scrollable content
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        var bottomInsetPx = 0
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Add sticky header ComposeView
        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                val density = LocalDensity.current
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.app_drawer),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
                            pageIndicator = {
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

        // Add scrollable settings content
        val nestedScrollView = androidx.core.widget.NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                androidx.compose.ui.platform.ComposeView(context).apply {
                    setContent {
                        val homeUiState by viewModel.homeUiState.collectAsState()
                        val settingsSize = (homeUiState.settingsSize - 3)
                        val density = LocalDensity.current
                        val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    DrawerSettingsAllInOne(settingsSize.sp)
                                    if (bottomInsetDp > 0.dp) Spacer(modifier = Modifier.height(bottomInsetDp))
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
        
        // Use EinkScrollBehavior callback to update page indicator reliably
        val scrollBehavior = com.github.gezimos.inkos.helper.utils.EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            headerView.setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.app_drawer),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
                            pageIndicator = {
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
        
        rootLayout.addView(
            nestedScrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Apply bottom padding to the root layout to prevent scroll view from going under navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

        return rootLayout
    }

    @Composable
    fun DrawerSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified) {
        val navController = findNavController()
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val uiState by viewModel.homeUiState.collectAsState()

        Column {

            SettingsHomeItem(
                title = "App Drawer",
                titleFontSize = titleFontSize,
                onClick = {
                    navController.navigate(
                        R.id.appsFragment,
                        bundleOf("flag" to Constants.AppDrawerFlag.LaunchApp.toString())
                    )
                }
            )
            
            SettingsHomeItem(
                title = "Hidden Apps",
                titleFontSize = titleFontSize,
                onClick = {
                    viewModel.getHiddenApps()
                    navController.navigate(
                        R.id.appsFragment,
                        bundleOf("flag" to Constants.AppDrawerFlag.HiddenApps.toString())
                    )
                }
            )
            
            SettingsTitle(
                text = stringResource(R.string.customizations),
                fontSize = titleFontSize,
            )
            
            SettingsSelect(
                title = stringResource(R.string.app_size),
                option = uiState.appDrawerSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.app_size),
                        minValue = Constants.MIN_APP_SIZE,
                        maxValue = Constants.MAX_APP_SIZE,
                        currentValue = uiState.appDrawerSize,
                        onValueSelected = { newVal ->
                            viewModel.setAppDrawerSize(newVal)
                        }
                    )
                }
            )
            
            SettingsSelect(
                title = stringResource(R.string.app_padding_size),
                option = uiState.appDrawerGap.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.app_padding_size),
                        minValue = Constants.MIN_TEXT_PADDING,
                        maxValue = Constants.MAX_TEXT_PADDING,
                        currentValue = uiState.appDrawerGap,
                        onValueSelected = { newVal ->
                            viewModel.setAppDrawerGap(newVal)
                        }
                    )
                }
            )
            
            // Cycle alignment between Left / Center / Right on each click (localized)
                // Cycle alignment between Left / Center / Right on each click (use short string keys)
                val alignmentLabels = listOf(
                    stringResource(R.string.left),
                    stringResource(R.string.center),
                    stringResource(R.string.right)
                )
            SettingsSelect(
                    title = stringResource(R.string.app_drawer_alignment),
                    option = alignmentLabels.getOrElse(uiState.appDrawerAlignment) { stringResource(R.string.left) },
                fontSize = titleFontSize,
                onClick = {
                    val next = (uiState.appDrawerAlignment + 1) % 3
                    viewModel.setAppDrawerAlignment(next)
                }
            )

            SettingsTitle(
                text = stringResource(R.string.filtring),
                fontSize = titleFontSize,
            )

            com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.az_filter),
                fontSize = titleFontSize,
                defaultState = uiState.appDrawerAzFilter,
                onCheckedChange = { checked: Boolean ->
                    viewModel.setAppDrawerAzFilter(checked)
                }
            )
            
            
            com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch(
                text = "Hide Home Apps", // TODO: localize string if desired
                fontSize = titleFontSize,
                defaultState = uiState.hideHomeApps,
                onCheckedChange = {
                    viewModel.setHideHomeApps(!uiState.hideHomeApps)
                }
            )
            
            SettingsSelect(
                title = getString(R.string.system_shortcuts),
                option = if (uiState.selectedSystemShortcuts.isEmpty()) "None" else "${uiState.selectedSystemShortcuts.size} selected",
                fontSize = titleFontSize,
                onClick = {
                    val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
                    val allShortcuts =
                        appsRepo.systemShortcuts
                            .sortedBy { it.displayName.lowercase() }
                    val shortcutLabels = allShortcuts.map { it.displayName }
                    val shortcutIds = allShortcuts.map { it.packageId }
                    val checked =
                        shortcutIds.map { uiState.selectedSystemShortcuts.contains(it) }.toBooleanArray()
                    dialogBuilder.showMultiChoiceDialog(
                        context = requireContext(),
                        title = getString(R.string.system_shortcuts),
                        items = shortcutLabels.toTypedArray(),
                        initialChecked = checked,
                        onConfirm = { selectedIndices ->
                            val selected = selectedIndices.map { shortcutIds[it] }.toMutableSet()
                            viewModel.setSelectedSystemShortcuts(selected)

                            // Remove hidden status for unchecked system shortcuts
                            val hiddenAppsSet = prefs.hiddenApps.toMutableSet()
                            val allShortcutKeys = shortcutIds.map {
                                it + "|" + android.os.Process.myUserHandle().toString()
                            }
                            val uncheckedShortcutKeys =
                                allShortcutKeys.filter { !selected.contains(it.substringBefore("|")) }
                            hiddenAppsSet.removeAll(uncheckedShortcutKeys)
                            viewModel.setHiddenApps(hiddenAppsSet)
                        }
                    )
                }
            )
            
            SettingsTitle(
                text = stringResource(R.string.search),
                fontSize = titleFontSize,
            )

            com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch(
                text = "Enable Search",
                fontSize = titleFontSize,
                defaultState = uiState.appDrawerSearchEnabled,
                onCheckedChange = { checked: Boolean ->
                    viewModel.setAppDrawerSearchEnabled(checked)
                }
            )

            com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch(
                text = "Auto Show Keyboard",
                fontSize = titleFontSize,
                defaultState = uiState.appDrawerAutoShowKeyboard,
                enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { checked: Boolean ->
                    viewModel.setAppDrawerAutoShowKeyboard(checked)
                }
            )
            
            com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch(
                text = "Auto-launch result",
                fontSize = titleFontSize,
                defaultState = uiState.appDrawerAutoLaunch,
                onCheckedChange = { checked: Boolean ->
                    viewModel.setAppDrawerAutoLaunch(checked)
                }
            )
        }
    }

    @Composable
    fun tuToDp(textUnit: TextUnit): Dp {
        val density = LocalDensity.current.density
        val scaledDensity = LocalDensity.current.fontScale
        val dpValue = textUnit.value * (density / scaledDensity)
        return dpValue.dp
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        prefs = viewModel.getPrefs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
