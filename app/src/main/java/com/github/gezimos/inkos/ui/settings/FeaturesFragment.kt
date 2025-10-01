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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Constants.Theme.Light
import com.github.gezimos.inkos.data.Constants.Theme.System
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.FullLineSeparator
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
        prefs = Prefs(requireContext())
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val backgroundColor = getHexForOpacity(prefs)
        val isDark = when (prefs.appTheme) {
            Light -> false
            Dark -> true
            System -> isSystemInDarkMode(requireContext())
        }
        val settingsSize = (prefs.settingsSize - 3)
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
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.settings_home_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0],
                                    titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
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
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.settings_home_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0],
                                    titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
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
        var selectedHomeAppsNum by remember { mutableIntStateOf(prefs.homeAppsNum) }
        var selectedHomePagesNum by remember { mutableIntStateOf(prefs.homePagesNum) }
        var toggledHomePager by remember { mutableStateOf(prefs.homePager) }
        var toggledHomeReset by remember { mutableStateOf(prefs.homeReset) }
        var toggledShowClock by remember { mutableStateOf(prefs.showClock) }
        var toggledShowDate by remember { mutableStateOf(prefs.showDate) }
        var toggledShowDateBatteryCombo by remember { mutableStateOf(prefs.showDateBatteryCombo) }
        var toggledShowQuote by remember { mutableStateOf(prefs.showQuote) }
        var toggledShowAudioWidget by remember { mutableStateOf(prefs.showAudioWidgetEnabled) }
        var quoteTextState by remember { mutableStateOf(prefs.quoteText) }
        var appNameMode by remember {
            mutableStateOf(
                when {
                    prefs.allCapsApps -> 2
                    prefs.smallCapsApps -> 1
                    else -> 0
                }
            )
        }
        // Layout & Positioning section (moved from LookFeelFragment)
        var toggledExtendHomeAppsArea = remember { mutableStateOf(prefs.extendHomeAppsArea) }
        var selectedPaddingSize = remember { mutableStateOf(prefs.textPaddingSize) }
    var selectedHomeAppsYOffset = remember { mutableStateOf(prefs.homeAppsYOffset) }
        var selectedTopWidgetMargin = remember { mutableStateOf(prefs.topWidgetMargin) }
        var selectedBottomWidgetMargin = remember { mutableStateOf(prefs.bottomWidgetMargin) }
        // Remove verticalScroll and isDark param, handled by parent ComposeView
        Column(modifier = Modifier.fillMaxWidth()) {
            FullLineSeparator(isDark)
            // --- Layout & Positioning (moved) ---
            SettingsTitle(
                text = stringResource(R.string.layout_positioning),
                fontSize = titleFontSize,
            )
            FullLineSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.app_padding_size),
                option = selectedPaddingSize.value.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.app_padding_size),
                        minValue = Constants.MIN_TEXT_PADDING,
                        maxValue = Constants.MAX_TEXT_PADDING,
                        currentValue = selectedPaddingSize.value,
                        onValueSelected = { newPaddingSize ->
                            selectedPaddingSize.value = newPaddingSize
                            prefs.textPaddingSize = newPaddingSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.home_apps_y_offset),
                option = selectedHomeAppsYOffset.value.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.home_apps_y_offset),
                        minValue = Constants.MIN_HOME_APPS_Y_OFFSET,
                        maxValue = Constants.MAX_HOME_APPS_Y_OFFSET,
                        currentValue = selectedHomeAppsYOffset.value,
                        onValueSelected = { newValue ->
                            selectedHomeAppsYOffset.value = newValue
                            prefs.homeAppsYOffset = newValue
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.top_widget_margin),
                option = selectedTopWidgetMargin.value.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.top_widget_margin),
                        minValue = 0,
                        maxValue = Constants.MAX_TOP_WIDGET_MARGIN,
                        currentValue = selectedTopWidgetMargin.value,
                        onValueSelected = { newValue ->
                            selectedTopWidgetMargin.value = newValue
                            prefs.topWidgetMargin = newValue
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.bottom_widget_margin),
                option = selectedBottomWidgetMargin.value.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.bottom_widget_margin),
                        minValue = 0,
                        maxValue = Constants.MAX_BOTTOM_WIDGET_MARGIN,
                        currentValue = selectedBottomWidgetMargin.value,
                        onValueSelected = { newValue ->
                            selectedBottomWidgetMargin.value = newValue
                            prefs.bottomWidgetMargin = newValue
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsHomeItem(
                title = stringResource(R.string.reorder_apps),
                onClick = {
                    findNavController().navigate(
                        R.id.action_settingsFeaturesFragment_to_appFavoriteFragment,
                        bundleOf("flag" to Constants.AppDrawerFlag.SetHomeApp.toString())
                    )
                },
                titleFontSize = titleFontSize
            )
            FullLineSeparator(isDark)
            // Home Apps Section
            SettingsTitle(
                text = stringResource(R.string.home_apps),
                fontSize = titleFontSize,
            )
            FullLineSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.apps_on_home_screen),
                option = selectedHomeAppsNum.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.apps_on_home_screen),
                        minValue = Constants.MIN_HOME_APPS,
                        maxValue = Constants.MAX_HOME_APPS,
                        currentValue = prefs.homeAppsNum,
                        onValueSelected = { newHomeAppsNum ->
                            selectedHomeAppsNum = newHomeAppsNum
                            prefs.homeAppsNum = newHomeAppsNum
                            viewModel.homeAppsNum.value = newHomeAppsNum
                            // Recompute page limit after apps change
                            Constants.updateMaxHomePages(requireContext())
                            if (selectedHomePagesNum > Constants.MAX_HOME_PAGES) {
                                selectedHomePagesNum = Constants.MAX_HOME_PAGES
                                prefs.homePagesNum = selectedHomePagesNum
                                viewModel.homePagesNum.value = selectedHomePagesNum
                            }
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.pages_on_home_screen),
                option = selectedHomePagesNum.toString(),
                fontSize = titleFontSize,
                onClick = {
                    Constants.updateMaxHomePages(requireContext())
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.pages_on_home_screen),
                        minValue = Constants.MIN_HOME_PAGES,
                        maxValue = Constants.MAX_HOME_PAGES,
                        currentValue = prefs.homePagesNum,
                        onValueSelected = { newHomePagesNum ->
                            selectedHomePagesNum = newHomePagesNum
                            prefs.homePagesNum = newHomePagesNum
                            viewModel.homePagesNum.value = newHomePagesNum
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.enable_home_pager),
                fontSize = titleFontSize,
                defaultState = toggledHomePager,
                onCheckedChange = {
                    toggledHomePager = !prefs.homePager
                    prefs.homePager = toggledHomePager
                }
            )
            DashedSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.home_page_reset),
                fontSize = titleFontSize,
                defaultState = toggledHomeReset,
                onCheckedChange = {
                    toggledHomeReset = !prefs.homeReset
                    prefs.homeReset = toggledHomeReset
                }
            )
            // Top Widgets Section
            FullLineSeparator(isDark = isDark)
            SettingsTitle(
                text = stringResource(R.string.top_widgets),
                fontSize = titleFontSize,
            )
            FullLineSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.show_clock),
                fontSize = titleFontSize,
                defaultState = toggledShowClock,
                onCheckedChange = {
                    toggledShowClock = !prefs.showClock
                    prefs.showClock = toggledShowClock
                    viewModel.setShowClock(prefs.showClock)
                }
            )
            DashedSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.show_date),
                fontSize = titleFontSize,
                defaultState = toggledShowDate,
                onCheckedChange = {
                    toggledShowDate = !prefs.showDate
                    prefs.showDate = toggledShowDate
                    // If date is turned off, also turn off date+battery combo
                    if (!toggledShowDate && toggledShowDateBatteryCombo) {
                        toggledShowDateBatteryCombo = false
                        prefs.showDateBatteryCombo = false
                    }
                }
            )
            // Only show Date + Battery combo when Date is enabled
            if (toggledShowDate) {
                DashedSeparator(isDark)
                SettingsSwitch(
                    text = stringResource(R.string.show_date_battery_combo),
                    fontSize = titleFontSize,
                    defaultState = toggledShowDateBatteryCombo,
                    onCheckedChange = {
                        toggledShowDateBatteryCombo = !prefs.showDateBatteryCombo
                        prefs.showDateBatteryCombo = toggledShowDateBatteryCombo
                    }
                )
            }
            // Bottom Widgets Section
            FullLineSeparator(isDark = isDark)
            SettingsTitle(
                text = stringResource(R.string.bottom_widgets),
                fontSize = titleFontSize,
            )
            FullLineSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.show_audio_widget),
                fontSize = titleFontSize,
                defaultState = toggledShowAudioWidget,
                onCheckedChange = {
                    toggledShowAudioWidget = !prefs.showAudioWidgetEnabled
                    prefs.showAudioWidgetEnabled = toggledShowAudioWidget
                }
            )
            DashedSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.show_quote),
                fontSize = titleFontSize,
                defaultState = toggledShowQuote,
                onCheckedChange = {
                    toggledShowQuote = !prefs.showQuote
                    prefs.showQuote = toggledShowQuote
                }
            )
            // Only show Quote text when Quote widget is enabled
            if (toggledShowQuote) {
                DashedSeparator(isDark)
                SettingsSelect(
                    title = stringResource(R.string.quote_text),
                    option = if (quoteTextState.length > 12) "${quoteTextState.take(12)}..." else quoteTextState,
                    fontSize = titleFontSize,
                    onClick = {
                        dialogBuilder.showInputDialog(
                            context = requireContext(),
                            title = getString(R.string.quote_text),
                            initialValue = quoteTextState,
                            onValueEntered = { newText ->
                                prefs.quoteText = newText
                                quoteTextState = newText
                            }
                        )
                    }
                )
            }
            FullLineSeparator(isDark)

            // --- Others Section ---
            SettingsTitle(
                text = "Other Functions",
                fontSize = titleFontSize,
            )
            FullLineSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.extend_home_apps_area),
                fontSize = titleFontSize,
                defaultState = toggledExtendHomeAppsArea.value,
                onCheckedChange = {
                    toggledExtendHomeAppsArea.value = !prefs.extendHomeAppsArea
                    prefs.extendHomeAppsArea = toggledExtendHomeAppsArea.value
                }
            )
            FullLineSeparator(isDark)
        }
    }

    private fun goBackToLastFragment() {
        findNavController().popBackStack()
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
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

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