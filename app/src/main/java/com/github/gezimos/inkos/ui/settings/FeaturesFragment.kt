package com.github.gezimos.inkos.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager

class FeaturesFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var dialogBuilder: DialogManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val backgroundColor = getHexForOpacity(requireContext())
        val isDark = prefs.appTheme == Dark
        val context = requireContext()
        // --- Dot indicator state ---
        val currentPage = intArrayOf(0)
        val pageCount = intArrayOf(1)

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
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.settings_home_title),
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
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    FeaturesSettingsAllInOne(settingsSize.sp, isDark)
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
        com.github.gezimos.inkos.helper.utils.EinkScrollBehavior(context)
            .attachToScrollView(nestedScrollView)
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

        // Use EinkScrollBehavior callback to update page indicator reliably (e-ink friendly)
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
                            title = stringResource(R.string.settings_home_title),
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
    fun FeaturesSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified, isDark: Boolean) {
        findNavController()
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        
        // Observe ViewModel state
        val homeUiState by viewModel.homeUiState.collectAsState()
        
        // Local state for items NOT in HomeUiState (yet)
        // var toggledHomeReset by remember { mutableStateOf(prefs.homeReset) } // Removed
        // val toggledExtendHomeAppsArea = remember { mutableStateOf(prefs.extendHomeAppsArea) } // Removed

        // Derived state for app name mode
        val appNameMode = when {
            homeUiState.allCapsApps -> 2
            homeUiState.smallCapsApps -> 1
            else -> 0
        }

        // Remove verticalScroll and isDark param, handled by parent ComposeView
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Layout & Positioning (moved) ---
            SettingsTitle(
                text = stringResource(R.string.layout_positioning),
                fontSize = titleFontSize,
            )
            SettingsSelect(
                title = stringResource(R.string.app_padding_size),
                option = homeUiState.textPaddingSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.app_padding_size),
                        minValue = Constants.MIN_TEXT_PADDING,
                        maxValue = Constants.MAX_TEXT_PADDING,
                        currentValue = homeUiState.textPaddingSize,
                        onValueSelected = { newPaddingSize ->
                            viewModel.setTextPaddingSize(newPaddingSize)
                        }
                    )
                }
            )
            SettingsSelect(
                title = stringResource(R.string.home_apps_y_offset),
                option = homeUiState.homeAppsYOffset.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.home_apps_y_offset),
                        minValue = Constants.MIN_HOME_APPS_Y_OFFSET,
                        maxValue = Constants.MAX_HOME_APPS_Y_OFFSET,
                        currentValue = homeUiState.homeAppsYOffset,
                        onValueSelected = { newValue ->
                            viewModel.setHomeAppsYOffset(newValue)
                        }
                    )
                }
            )
            SettingsSelect(
                title = stringResource(R.string.top_widget_margin),
                option = homeUiState.topWidgetMargin.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.top_widget_margin),
                        minValue = 0,
                        maxValue = Constants.MAX_TOP_WIDGET_MARGIN,
                        currentValue = homeUiState.topWidgetMargin,
                        onValueSelected = { newValue ->
                            viewModel.setTopWidgetMargin(newValue)
                        }
                    )
                }
            )
            SettingsSelect(
                title = stringResource(R.string.bottom_widget_margin),
                option = homeUiState.bottomWidgetMargin.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.bottom_widget_margin),
                        minValue = 0,
                        maxValue = Constants.MAX_BOTTOM_WIDGET_MARGIN,
                        currentValue = homeUiState.bottomWidgetMargin,
                        onValueSelected = { newValue ->
                            viewModel.setBottomWidgetMargin(newValue)
                        }
                    )
                }
            )
            val alignmentLabels = listOf(
                stringResource(R.string.left),
                stringResource(R.string.center),
                stringResource(R.string.right)
            )
            SettingsSelect(
                title = "Home Alignment",
                option = alignmentLabels.getOrElse(homeUiState.homeAlignment) { stringResource(R.string.left) },
                fontSize = titleFontSize,
                onClick = {
                    val next = (homeUiState.homeAlignment + 1) % 3
                    viewModel.setHomeAlignment(next)
                }
            )
            
            // Home Apps Section
            SettingsTitle(
                text = stringResource(R.string.home_apps),
                fontSize = titleFontSize,
            )
            SettingsSelect(
                title = stringResource(R.string.apps_on_home_screen),
                option = homeUiState.homeAppsNum.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.apps_on_home_screen),
                        minValue = Constants.MIN_HOME_APPS,
                        maxValue = Constants.MAX_HOME_APPS,
                        currentValue = homeUiState.homeAppsNum,
                        onValueSelected = { newHomeAppsNum ->
                            viewModel.setHomeAppsNum(newHomeAppsNum)
                            // Recompute page limit after apps change
                            Constants.updateMaxHomePages(requireContext())
                            if (homeUiState.homePagesNum > Constants.MAX_HOME_PAGES) {
                                viewModel.setHomePagesNum(Constants.MAX_HOME_PAGES)
                            }
                        }
                    )
                }
            )
            SettingsSelect(
                title = stringResource(R.string.pages_on_home_screen),
                option = homeUiState.homePagesNum.toString(),
                fontSize = titleFontSize,
                onClick = {
                    Constants.updateMaxHomePages(requireContext())
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.pages_on_home_screen),
                        minValue = Constants.MIN_HOME_PAGES,
                        maxValue = Constants.MAX_HOME_PAGES,
                        currentValue = homeUiState.homePagesNum,
                        onValueSelected = { newHomePagesNum ->
                            viewModel.setHomePagesNum(newHomePagesNum)
                        }
                    )
                }
            )
            SettingsSwitch(
                text = stringResource(R.string.enable_home_pager),
                fontSize = titleFontSize,
                defaultState = homeUiState.pageIndicatorVisible,
                onCheckedChange = {
                    viewModel.setPageIndicatorVisible(it)
                }
            )
            
            SettingsSwitch(
                text = stringResource(R.string.home_page_reset),
                fontSize = titleFontSize,
                defaultState = homeUiState.homeReset,
                onCheckedChange = {
                    viewModel.setHomeReset(it)
                }
            )
            // Top Widgets Section
            SettingsTitle(
                text = stringResource(R.string.top_widgets),
                fontSize = titleFontSize,
            )
            SettingsSwitch(
                text = stringResource(R.string.show_clock),
                fontSize = titleFontSize,
                defaultState = homeUiState.showClock,
                onCheckedChange = {
                    viewModel.setShowClock(it)
                }
            )
                // Clock format selector: System / Force 24h / Force 12h
            val clockModeLabels = listOf(
                stringResource(R.string.clock_mode_system),
                stringResource(R.string.clock_mode_24),
                stringResource(R.string.clock_mode_12)
            )
            SettingsSelect(
                title = stringResource(R.string.clock_format),
                option = clockModeLabels.getOrElse(homeUiState.clockMode) { stringResource(R.string.clock_mode_system) },
                fontSize = titleFontSize,
                onClick = {
                    val next = (homeUiState.clockMode + 1) % 3
                    viewModel.setClockMode(next)
                }
            )
            SettingsSwitch(
                text = "Show AM/PM",
                fontSize = titleFontSize,
                defaultState = homeUiState.showAmPm,
                onCheckedChange = {
                    viewModel.setShowAmPm(it)
                }
            )
            SettingsSwitch(
                text = "Dual Clocks",
                fontSize = titleFontSize,
                defaultState = homeUiState.showSecondClock,
                onCheckedChange = {
                    viewModel.setShowSecondClock(it)
                }
            )

            // Only show the offset selector when the second clock (dual clock) is enabled
            if (homeUiState.showSecondClock) {
                SettingsSelect(
                    title = "Second clock offset",
                    option = if (homeUiState.secondClockOffsetHours >= 0) "+${homeUiState.secondClockOffsetHours}" else homeUiState.secondClockOffsetHours.toString(),
                    fontSize = titleFontSize,
                    onClick = {
                        dialogBuilder.showSliderDialog(
                            context = requireContext(),
                            title = "Second clock offset",
                            minValue = -12,
                            maxValue = 14,
                            currentValue = homeUiState.secondClockOffsetHours,
                            onValueSelected = { newOffset ->
                                viewModel.setSecondClockOffsetHours(newOffset)
                            }
                        )
                    }
                )
            }
            
            SettingsSwitch(
                text = stringResource(R.string.show_date),
                fontSize = titleFontSize,
                defaultState = homeUiState.showDate,
                onCheckedChange = {
                    viewModel.setShowDate(it)
                }
            )
            SettingsSwitch(
                text = stringResource(R.string.show_battery),
                fontSize = titleFontSize,
                defaultState = homeUiState.showDateBatteryCombo,
                onCheckedChange = {
                    viewModel.setShowDateBatteryCombo(it)
                }
            )
            SettingsSwitch(
                text = stringResource(R.string.show_notification_count),
                fontSize = titleFontSize,
                defaultState = homeUiState.showNotificationCount,
                onCheckedChange = {
                    viewModel.setShowNotificationCount(it)
                }
            )
            // Bottom Widgets Section
            SettingsTitle(
                text = stringResource(R.string.bottom_widgets),
                fontSize = titleFontSize,
            )
            SettingsSwitch(
                text = stringResource(R.string.show_audio_widget),
                fontSize = titleFontSize,
                defaultState = homeUiState.showAudioWidget,
                onCheckedChange = {
                    viewModel.setShowAudioWidget(it)
                }
            )
            
            SettingsSwitch(
                text = stringResource(R.string.show_quote),
                fontSize = titleFontSize,
                defaultState = homeUiState.showQuote,
                onCheckedChange = {
                    viewModel.setShowQuote(it)
                }
            )
            // Only show Quote text when Quote widget is enabled
            if (homeUiState.showQuote) {
                SettingsSelect(
                    title = stringResource(R.string.quote_text),
                    option = if (homeUiState.quoteText.length > 12) "${homeUiState.quoteText.take(12)}..." else homeUiState.quoteText,
                    fontSize = titleFontSize,
                    onClick = {
                        dialogBuilder.showInputDialog(
                            context = requireContext(),
                            title = getString(R.string.quote_text),
                            initialValue = homeUiState.quoteText,
                            onValueEntered = { newText ->
                                viewModel.setQuoteText(newText)
                            }
                        )
                    }
                )
            }
            

            // --- Others Section ---
            SettingsTitle(
                text = "Other Functions",
                fontSize = titleFontSize,
            )
            SettingsSwitch(
                text = stringResource(R.string.extend_home_apps_area),
                fontSize = titleFontSize,
                defaultState = homeUiState.extendHomeAppsArea,
                onCheckedChange = {
                    viewModel.setExtendHomeAppsArea(it)
                }
            )
            
        }
    }

    private fun dismissDialogs() {
        dialogBuilder.singleChoiceDialog?.dismiss()
        dialogBuilder.sliderDialog?.dismiss()
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

        viewModel.isinkosDefault()

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onStop() {
        super.onStop()
        dismissDialogs()
    }
}