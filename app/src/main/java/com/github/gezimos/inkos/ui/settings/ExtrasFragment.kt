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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
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
import com.github.gezimos.inkos.EinkHelper
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager
class ExtrasFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: com.github.gezimos.inkos.MainViewModel
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
        viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        dialogBuilder = DialogManager(requireContext(), requireActivity())
            val isDark = prefs.appTheme == Dark
        val backgroundColor = com.github.gezimos.inkos.helper.getHexForOpacity(requireContext())
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
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = "Extras",
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

        val nestedScrollView = NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                androidx.compose.ui.platform.ComposeView(context).apply {
                    setContent {
                        val homeUiState by viewModel.homeUiState.collectAsState()
                        val settingsSize = (homeUiState.settingsSize - 3)
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
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = "Extras",
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
        return rootLayout
    }

    @Composable
    fun ExtrasSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified, isDark: Boolean) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val uiState by viewModel.homeUiState.collectAsState()
        
        val isMuditaDevice = remember { EinkHelper.isMuditaKompakt() }
        val navController = findNavController()
        Column(modifier = Modifier.fillMaxSize()) {
            // Only show EinkHelper settings on Mudita Kompakt devices
            if (isMuditaDevice) {
                SettingsTitle(
                    text = stringResource(R.string.eink_auto_mode),
                    fontSize = titleFontSize,
                )
                SettingsSwitch(
                    text = stringResource(R.string.eink_auto_mode),
                    fontSize = titleFontSize,
                    defaultState = uiState.einkHelperEnabled,
                    onCheckedChange = {
                        viewModel.setEinkHelperEnabled(it)
                        requireActivity().recreate()
                    }
                )
            }

            SettingsTitle(
                text = "Extra Features",
                fontSize = titleFontSize,
            )

            SettingsSwitch(
                text = "Auto E-Ink Refresh",
                fontSize = titleFontSize,
                defaultState = uiState.einkRefreshEnabled,
                onCheckedChange = {
                    viewModel.setEinkRefreshEnabled(it)
                }
            )

            // Show the Home-only option only when Auto (master) refresh is enabled.
            if (uiState.einkRefreshEnabled) {
                SettingsSwitch(
                    text = "Auto Refresh Only in Home",
                    fontSize = titleFontSize,
                    defaultState = uiState.einkRefreshHomeButtonOnly,
                    onCheckedChange = {
                        viewModel.setEinkRefreshHomeButtonOnly(it)
                    }
                )
            }

            SettingsSelect(
                title = "E-Ink Refresh Delay",
                option = "${uiState.einkRefreshDelay} ms",
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "E-Ink Refresh Delay",
                        minValue = Constants.MIN_EINK_REFRESH_DELAY,
                        maxValue = Constants.MAX_EINK_REFRESH_DELAY,
                        currentValue = uiState.einkRefreshDelay.toInt(),
                        onValueSelected = { newDelay: Int ->
                            val snapped = ((newDelay + 12) / 25) * 25
                            viewModel.setEinkRefreshDelay(snapped)
                        }
                    )
                }
            )

            SettingsSwitch(
                text = getString(R.string.use_volume_keys_for_pages),
                fontSize = titleFontSize,
                defaultState = uiState.useVolumeKeysForPages,
                onCheckedChange = {
                    viewModel.setUseVolumeKeysForPages(it)
                }
            )
        }
    }
}
