package com.github.gezimos.inkos.ui.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
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
import com.github.gezimos.inkos.helper.getTrueSystemFont
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FontsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var dialogBuilder: DialogManager
    private val PICK_FONT_FILE_REQUEST_CODE = 1001
    private var onCustomFontSelected: ((Typeface, String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val backgroundColor = com.github.gezimos.inkos.helper.getHexForOpacity(prefs)
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

        // Track bottom inset for padding in Compose header/content
        var bottomInsetPx = 0

        // Create sticky header ComposeView (we'll update its content from the scroll behavior callback)
        val headerView = ComposeView(context).apply {
            setContent {
                val density = LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.fonts_settings_title),
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
                        
                        if (bottomInsetDp > 0.dp) {
                            Spacer(modifier = Modifier.height(bottomInsetDp))
                        }
                    }
                }
            }
        }

        // Add scrollable settings content
        val nestedScrollView = NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                ComposeView(context).apply {
                    setContent {
                        val density = LocalDensity.current
                        val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    FontsSettingsAllInOne(settingsSize.sp, isDark)
                                    if (bottomInsetDp > 0.dp) {
                                        Spacer(modifier = Modifier.height(bottomInsetDp))
                                    }
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

        // Create a vertical LinearLayout to hold sticky header and scrollable content
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(headerView)
            addView(
                nestedScrollView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Create behavior with a callback to update page indicator reliably
        val scrollBehavior = EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            // Update header compose content to reflect the new page indicator state
            headerView.setContent {
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.fonts_settings_title),
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

        // Apply bottom padding to the root layout to prevent scroll view from going under navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

    // Page calculation and indicator updating are handled by EinkScrollBehavior callback

        return rootLayout
    }

    @Composable
    fun FontsSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified, isDark: Boolean) {
        findNavController()
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        // Universal Font Section State
        var universalFontState by remember { mutableStateOf(prefs.universalFont) }
        var universalFontEnabledState by remember { mutableStateOf(prefs.universalFontEnabled) }
        var settingsFontState by remember { mutableStateOf(prefs.fontFamily) }
        var settingsSize by remember { mutableStateOf(prefs.settingsSize) }
        // Home Fonts Section State
        var appsFontState by remember { mutableStateOf(prefs.appsFont) }
        var appSize by remember { mutableStateOf(prefs.appSize) }
        var clockFontState by remember { mutableStateOf(prefs.clockFont) }
        var clockSize by remember { mutableStateOf(prefs.clockSize) }
        var batteryFontState by remember { mutableStateOf(prefs.batteryFont) }
        var batterySize by remember { mutableStateOf(prefs.batterySize) }
        // Home App Type State
        var appNameMode by remember {
            mutableStateOf(
                when {
                    prefs.allCapsApps -> 2
                    prefs.smallCapsApps -> 1
                    else -> 0
                }
            )
        }
        // Date Font Section State
        var dateFontState by remember { mutableStateOf(prefs.dateFont) }
        var dateSize by remember { mutableStateOf(prefs.dateSize) }
        // Quote Font Section State
        var quoteFontState by remember { mutableStateOf(prefs.quoteFont) }
        var quoteSize by remember { mutableStateOf(prefs.quoteSize) }
        // Notification Fonts Section State
        var labelnotificationsFontState by remember { mutableStateOf(prefs.labelnotificationsFont) }
        var labelnotificationsFontSize by remember { mutableStateOf(prefs.labelnotificationsTextSize) }
        var notificationsFontState by remember { mutableStateOf(prefs.notificationsFont) }
        var notificationsTitleFontState by remember { mutableStateOf(prefs.lettersTitleFont) }
        var notificationsTitle by remember { mutableStateOf(prefs.lettersTitle) }
        var notificationsTitleSize by remember { mutableStateOf(prefs.lettersTitleSize) }
        var notificationsTextSize by remember { mutableStateOf(prefs.notificationsTextSize) }

        // --- Sync all font states when universal font or its enabled state changes ---
        LaunchedEffect(universalFontState, universalFontEnabledState) {
            if (universalFontEnabledState) {
                // When universal font is enabled, all fonts should match the universal font
                val font = universalFontState
                appsFontState = font
                clockFontState = font
                batteryFontState = font
                labelnotificationsFontState = font
                notificationsFontState = font
                notificationsTitleFontState = font
                dateFontState = font
                quoteFontState = font
            }
            // When universal font is disabled, DON'T change anything - preserve individual choices
        }

        // Use Column instead of LazyColumn (let parent NestedScrollView handle scrolling)
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Universal Custom Font Section (top, with Reset All on right) ---
            DashedSeparator(isDark = isDark)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsTitle(
                    text = stringResource(R.string.universal_custom_font),
                    fontSize = titleFontSize,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Reset All",
                    style = SettingsTheme.typography.button,
                    fontSize = if (titleFontSize.isSpecified) (titleFontSize.value * 0.7).sp else 14.sp,
                    modifier = Modifier
                        .padding(end = SettingsTheme.color.horizontalPadding)
                        .clickable {
                            prefs.fontFamily = Constants.FontFamily.System
                            prefs.universalFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("universal")
                            prefs.universalFontEnabled = false
                            prefs.appsFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("apps")
                            prefs.clockFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("clock")
                            prefs.statusFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("status")
                            prefs.labelnotificationsFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("notification")
                            prefs.dateFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("date")
                            prefs.quoteFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("quote")
                            prefs.batteryFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("battery")
                            prefs.lettersFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("letters")
                            prefs.lettersTitleFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("lettersTitle")
                            prefs.notificationsFont = Constants.FontFamily.System
                            prefs.removeCustomFontPath("notifications")
                            prefs.settingsSize = 16
                            prefs.appSize = 32
                            prefs.clockSize = 64
                            prefs.labelnotificationsTextSize = 16
                            prefs.batterySize = 18
                            prefs.dateSize = 18
                            prefs.quoteSize = 18
                            prefs.lettersTextSize = 18
                            prefs.lettersTitleSize = 36
                            prefs.lettersTitle = "Letters"
                            universalFontState = Constants.FontFamily.System
                            universalFontEnabledState = false
                            settingsFontState = Constants.FontFamily.System
                            settingsSize = 16
                            appsFontState = Constants.FontFamily.System
                            appSize = 32
                            clockFontState = Constants.FontFamily.System
                            clockSize = 64
                            batteryFontState = Constants.FontFamily.System
                            batterySize = 18
                            dateFontState = Constants.FontFamily.System
                            dateSize = 18
                            quoteFontState = Constants.FontFamily.System
                            quoteSize = 18
                            labelnotificationsFontState = Constants.FontFamily.System
                            labelnotificationsFontSize = 16
                            notificationsFontState = Constants.FontFamily.System
                            notificationsTitleFontState = Constants.FontFamily.System
                            notificationsTitle = "Letters"
                            notificationsTitleSize = 36
                            notificationsTextSize = 16
                        }
                )
            }
            DashedSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.universal_custom_font),
                fontSize = titleFontSize,
                defaultState = universalFontEnabledState,
                onCheckedChange = { enabled ->
                    prefs.universalFontEnabled = enabled
                    universalFontEnabledState = enabled
                    if (enabled) {
                        val font = prefs.universalFont
                        val fontPath =
                            if (font == Constants.FontFamily.Custom) prefs.getCustomFontPath("universal") else null
                        prefs.fontFamily = font
                        prefs.appsFont = font
                        prefs.clockFont = font
                        prefs.statusFont = font
                        prefs.labelnotificationsFont = font
                        prefs.notificationsFont = font
                        prefs.lettersTitleFont = font
                        prefs.dateFont = font
                        prefs.quoteFont = font
                        prefs.batteryFont = font
                        prefs.lettersFont = font
                        if (font == Constants.FontFamily.Custom && fontPath != null) {
                            val keys = listOf(
                                "universal",
                                "settings",
                                "apps",
                                "clock",
                                "status",
                                "notification",
                                "notifications",
                                "lettersTitle",
                                "date",
                                "quote",
                                "battery",
                                "letters"
                            )
                            for (key in keys) prefs.setCustomFontPath(key, fontPath)
                        }
                        settingsFontState = font
                    }
                }
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.universal_custom_font_selector),
                option = getFontDisplayName(universalFontState, "universal"),
                fontSize = titleFontSize,
                onClick = {
                    showFontSelectionDialogWithCustoms(
                        R.string.universal_custom_font_selector,
                        "universal"
                    ) { newFont, customPath ->
                        prefs.universalFont = newFont
                        universalFontState = newFont
                        val fontPath =
                            if (newFont == Constants.FontFamily.Custom) customPath else null
                        if (prefs.universalFontEnabled) {
                            prefs.fontFamily = newFont
                            prefs.appsFont = newFont
                            prefs.clockFont = newFont
                            prefs.statusFont = newFont
                            prefs.labelnotificationsFont = newFont
                            prefs.notificationsFont = newFont
                            prefs.lettersTitleFont = newFont
                            prefs.dateFont = newFont
                            prefs.quoteFont = newFont
                            prefs.batteryFont = newFont
                            prefs.lettersFont = newFont
                            if (newFont == Constants.FontFamily.Custom && fontPath != null) {
                                val keys = listOf(
                                    "universal",
                                    "settings",
                                    "apps",
                                    "clock",
                                    "status",
                                    "notification",
                                    "notifications",
                                    "lettersTitle",
                                    "date",
                                    "quote",
                                    "battery",
                                    "letters"
                                )
                                for (key in keys) prefs.setCustomFontPath(key, fontPath)
                            }
                            settingsFontState = newFont
                        }
                        // Refresh the fragment to show changes immediately
                        activity?.recreate()
                    }
                },
                enabled = prefs.universalFontEnabled
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.settings_font_section),
                option = getFontDisplayName(settingsFontState, "settings"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.settings_font_section,
                            "settings"
                        ) { newFont, customPath ->
                            prefs.fontFamily = newFont
                            settingsFontState = newFont
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.settings_text_size),
                option = settingsSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.settings_text_size),
                        minValue = Constants.MIN_SETTINGS_TEXT_SIZE,
                        maxValue = Constants.MAX_SETTINGS_TEXT_SIZE,
                        currentValue = prefs.settingsSize,
                        onValueSelected = { newSize ->
                            prefs.settingsSize = newSize
                            settingsSize = newSize
                        }
                    )
                }
            )

            // --- Home Fonts Section ---
            DashedSeparator(isDark)
            SettingsTitle(
                text = "Home Fonts",
                fontSize = titleFontSize
            )
            DashedSeparator(isDark)

            // Apps Font
            SettingsSelect(
                title = stringResource(R.string.apps_font),
                option = getFontDisplayName(appsFontState, "apps"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.apps_font,
                            "apps"
                        ) { newFont, customPath ->
                            prefs.appsFont = newFont
                            appsFontState = newFont
                            customPath?.let { prefs.setCustomFontPath("apps", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.app_text_size),
                option = appSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.app_text_size),
                        minValue = Constants.MIN_APP_SIZE,
                        maxValue = Constants.MAX_APP_SIZE,
                        currentValue = prefs.appSize,
                        onValueSelected = { newAppSize ->
                            prefs.appSize = newAppSize
                            appSize = newAppSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            val appNameModeLabels = listOf(
                stringResource(R.string.app_name_mode_normal),
                stringResource(R.string.small_caps_apps),
                stringResource(R.string.app_name_mode_all_caps)
            )
            SettingsSelect(
                title = stringResource(R.string.app_name_mode),
                option = appNameModeLabels[appNameMode],
                fontSize = titleFontSize,
                onClick = {
                    val nextMode = (appNameMode + 1) % 3
                    appNameMode = nextMode
                    prefs.smallCapsApps = nextMode == 1
                    prefs.allCapsApps = nextMode == 2
                }
            )
            DashedSeparator(isDark)

            // Clock Font
            SettingsSelect(
                title = stringResource(R.string.clock_font),
                option = getFontDisplayName(clockFontState, "clock"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.clock_font,
                            "clock"
                        ) { newFont, customPath ->
                            prefs.clockFont = newFont
                            clockFontState = newFont
                            customPath?.let { prefs.setCustomFontPath("clock", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.clock_text_size),
                option = clockSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.clock_text_size),
                        minValue = Constants.MIN_CLOCK_SIZE,
                        maxValue = Constants.MAX_CLOCK_SIZE,
                        currentValue = prefs.clockSize,
                        onValueSelected = { newClockSize ->
                            prefs.clockSize = newClockSize
                            clockSize = newClockSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            // Date Font
            SettingsSelect(
                title = stringResource(R.string.date_font),
                option = getFontDisplayName(dateFontState, "date"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.date_font,
                            "date"
                        ) { newFont, customPath ->
                            prefs.dateFont = newFont
                            dateFontState = newFont
                            customPath?.let { prefs.setCustomFontPath("date", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.date_text_size),
                option = dateSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.date_text_size),
                        minValue = 10,
                        maxValue = 64,
                        currentValue = dateSize,
                        onValueSelected = { newSize ->
                            dateSize = newSize
                            prefs.dateSize = newSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)

            // Battery Font
            SettingsSelect(
                title = stringResource(R.string.battery_font),
                option = getFontDisplayName(batteryFontState, "battery"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.battery_font,
                            "battery"
                        ) { newFont, customPath ->
                            prefs.batteryFont = newFont
                            batteryFontState = newFont
                            customPath?.let { prefs.setCustomFontPath("battery", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.battery_text_size),
                option = batterySize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.battery_text_size),
                        minValue = Constants.MIN_BATTERY_SIZE,
                        maxValue = Constants.MAX_BATTERY_SIZE,
                        currentValue = prefs.batterySize,
                        onValueSelected = { newBatterySize ->
                            prefs.batterySize = newBatterySize
                            batterySize = newBatterySize
                        }
                    )
                }
            )
            DashedSeparator(isDark)

            // Quote Font Section
            SettingsSelect(
                title = stringResource(R.string.quote_font),
                option = getFontDisplayName(quoteFontState, "quote"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.quote_font,
                            "quote"
                        ) { newFont, customPath ->
                            prefs.quoteFont = newFont
                            quoteFontState = newFont
                            customPath?.let { prefs.setCustomFontPath("quote", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(R.string.quote_text_size),
                option = quoteSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.quote_text_size),
                        minValue = 10,
                        maxValue = 64,
                        currentValue = prefs.quoteSize,
                        onValueSelected = { newQuoteSize ->
                            prefs.quoteSize = newQuoteSize
                            quoteSize = newQuoteSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)

            SettingsTitle(
                text = "Label Notifications",
                fontSize = titleFontSize
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = "Label Notifications Font",
                option = if (labelnotificationsFontState == Constants.FontFamily.Custom)
                    getFontDisplayName(labelnotificationsFontState, "notification")
                else labelnotificationsFontState.name,
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.app_notification_font,
                            "notification"
                        ) { newFont, customPath ->
                            prefs.labelnotificationsFont = newFont
                            labelnotificationsFontState = newFont
                            if (newFont == Constants.FontFamily.Custom && customPath != null) {
                                prefs.setCustomFontPath("notification", customPath)
                            }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = "Label Notifications Size",
                option = labelnotificationsFontSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "Label Notifications Size",
                        minValue = Constants.MIN_LABEL_NOTIFICATION_TEXT_SIZE,
                        maxValue = Constants.MAX_LABEL_NOTIFICATION_TEXT_SIZE,
                        currentValue = prefs.labelnotificationsTextSize,
                        onValueSelected = { newSize ->
                            prefs.labelnotificationsTextSize = newSize
                            labelnotificationsFontSize = newSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)

            SettingsTitle(
                text = "Notifications Window",
                fontSize = titleFontSize
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = "Window Title",
                option = notificationsTitle,
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showInputDialog(
                        context = requireContext(),
                        title = "Title",
                        initialValue = notificationsTitle,
                        onValueEntered = { newTitle ->
                            // Remove any newline characters to enforce single line
                            val singleLineTitle = newTitle.replace("\n", "")
                            prefs.lettersTitle = singleLineTitle
                            notificationsTitle = singleLineTitle
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = "Title Font",
                option = if (notificationsTitleFontState == Constants.FontFamily.Custom)
                    getFontDisplayName(notificationsTitleFontState, "lettersTitle")
                else notificationsTitleFontState.name,
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.notifications_font,
                            "lettersTitle"
                        ) { newFont, customPath ->
                            prefs.lettersTitleFont = newFont
                            notificationsTitleFontState = newFont
                            if (newFont == Constants.FontFamily.Custom && customPath != null) {
                                prefs.setCustomFontPath("lettersTitle", customPath)
                            }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = "Title Size",
                option = notificationsTitleSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "Title Size",
                        minValue = 10,
                        maxValue = 60,
                        currentValue = notificationsTitleSize,
                        onValueSelected = { newSize ->
                            prefs.lettersTitleSize = newSize
                            notificationsTitleSize = newSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = "Body Font",
                option = if (universalFontEnabledState) {
                    val universalFont = prefs.universalFont
                    if (universalFont == Constants.FontFamily.Custom)
                        getFontDisplayName(universalFont, "universal")
                    else universalFont.name
                } else if (notificationsFontState == Constants.FontFamily.Custom)
                    getFontDisplayName(notificationsFontState, "notifications")
                else notificationsFontState.name,
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.notifications_font,
                            "notifications"
                        ) { newFont, customPath ->
                            prefs.notificationsFont = newFont
                            notificationsFontState = newFont
                            if (newFont == Constants.FontFamily.Custom && customPath != null) {
                                prefs.setCustomFontPath("notifications", customPath)
                            }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = "Body Text Size",
                option = notificationsTextSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "Body Text Size",
                        minValue = Constants.MIN_LABEL_NOTIFICATION_TEXT_SIZE,
                        maxValue = Constants.MAX_LABEL_NOTIFICATION_TEXT_SIZE,
                        currentValue = notificationsTextSize,
                        onValueSelected = { newSize ->
                            prefs.notificationsTextSize = newSize
                            notificationsTextSize = newSize
                        }
                    )
                }
            )
            DashedSeparator(isDark)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    private fun getFontDisplayName(font: Constants.FontFamily, contextKey: String): String {
        val fontName = if (font == Constants.FontFamily.Custom) {
            val path = if (contextKey == "notifications") {
                // Use the correct custom font path for notifications
                prefs.getCustomFontPath("notifications")
                    ?: prefs.getCustomFontPath("universal")
            } else {
                prefs.getCustomFontPathForContext(contextKey)
            }
            path?.let { File(it).name } ?: font.name
        } else {
            font.name
        }

        // Truncate font names to 12 characters with "..." if longer
        return if (fontName.length > 12) "${fontName.take(12)}..." else fontName
    }

    private fun showFontSelectionDialogWithCustoms(
        titleResId: Int,
        contextKey: String,
        onFontSelected: (Constants.FontFamily, String?) -> Unit
    ) {
        val fontFamilyEntries = Constants.FontFamily.entries
            .filter { it != Constants.FontFamily.Custom }
        val context = requireContext()
        val prefs = Prefs(context)

        val builtInFontOptions = fontFamilyEntries.map { it.getString(context) }
        val builtInFonts = fontFamilyEntries.map { it.getFont(context) ?: getTrueSystemFont() }

        val customFonts = Constants.FontFamily.getAllCustomFonts(context)
        val customFontOptions = customFonts.map { it.first }
        val customFontPaths = customFonts.map { it.second }
        val customFontTypefaces = customFontPaths.map { path ->
            Constants.FontFamily.Custom.getFont(context, path) ?: getTrueSystemFont()
        }

        val addCustomFontOption = "Add Custom Font..." // Ensure this is capitalized

        val options = listOf(addCustomFontOption) + builtInFontOptions + customFontOptions
        val fonts =
            listOf(getTrueSystemFont()) + builtInFonts + customFontTypefaces // Add placeholder font for "Add Custom Font..."

        // Determine currently selected index so the dialog shows the radio for current choice
        var selectedIndex: Int? = null
        try {
            // If the current context uses custom font, match by path
            val currentFont = when (contextKey) {
                "universal" -> prefs.universalFont
                else -> prefs.getFontForContext(contextKey)
            }
            if (currentFont == Constants.FontFamily.Custom) {
                val path = if (contextKey == "notifications") prefs.getCustomFontPath("notifications") ?: prefs.getCustomFontPath("universal") else prefs.getCustomFontPathForContext(contextKey)
                if (path != null) {
                    val idx = customFontPaths.indexOf(path)
                    if (idx != -1) selectedIndex = 1 + builtInFontOptions.size + idx
                }
            } else {
                val idx = fontFamilyEntries.indexOf(currentFont)
                if (idx != -1) selectedIndex = 1 + idx
            }
        } catch (_: Exception) {}

        dialogBuilder.showSingleChoiceDialog(
            context = context,
            options = options.toTypedArray(),
            fonts = fonts,
            titleResId = titleResId,
            selectedIndex = selectedIndex,
            showButtons = false,
            isCustomFont = { option ->
                customFontOptions.contains(option)
            },
            nonSelectable = { option -> option.toString() == addCustomFontOption },
            onItemSelected = { selectedName ->
                // Use string comparison to handle reordered options
                if (selectedName.toString() == addCustomFontOption) {
                    pickCustomFontFile { typeface, path ->
                        prefs.setCustomFontPath(
                            contextKey,
                            path
                        )
                        prefs.addCustomFontPath(path)
                        onFontSelected(Constants.FontFamily.Custom, path)
                        activity?.recreate()
                    }
                } else {
                    val builtInIndex = builtInFontOptions.indexOf(selectedName)
                    if (builtInIndex != -1) {
                        onFontSelected(fontFamilyEntries[builtInIndex], null)
                        return@showSingleChoiceDialog
                    }
                    val customIndex = customFontOptions.indexOf(selectedName)
                    if (customIndex != -1) {
                        val path = customFontPaths[customIndex]
                        prefs.setCustomFontPath(
                            contextKey,
                            path
                        )
                        onFontSelected(Constants.FontFamily.Custom, path)
                        return@showSingleChoiceDialog
                    }
                }
            },
            onItemDeleted = { deletedName ->
                val customIndex = customFontOptions.indexOf(deletedName)
                if (customIndex != -1) {
                    val path = customFontPaths[customIndex]

                    // Remove the custom font from storage
                    prefs.removeCustomFontPathByPath(path)

                    // Find all contexts using this font and reset them to System font
                    val allKeys = prefs.customFontPathMap.filterValues { it == path }.keys
                    for (key in allKeys) {
                        prefs.removeCustomFontPath(key)
                        // Reset the font setting for this context to System
                        when (key) {
                            "universal" -> prefs.universalFont = Constants.FontFamily.System
                            "settings" -> prefs.fontFamily = Constants.FontFamily.System
                            "apps" -> prefs.appsFont = Constants.FontFamily.System
                            "clock" -> prefs.clockFont = Constants.FontFamily.System
                            "battery" -> prefs.batteryFont = Constants.FontFamily.System
                            "date" -> prefs.dateFont = Constants.FontFamily.System
                            "quote" -> prefs.quoteFont = Constants.FontFamily.System
                            "notification" -> prefs.labelnotificationsFont =
                                Constants.FontFamily.System

                            "notifications" -> prefs.notificationsFont = Constants.FontFamily.System
                            "lettersTitle" -> prefs.lettersTitleFont = Constants.FontFamily.System
                        }
                    }

                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun pickCustomFontFile(onFontPicked: (Typeface, String) -> Unit) {
        onCustomFontSelected = onFontPicked
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "font/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "font/ttf",
                    "font/otf",
                    "application/x-font-ttf",
                    "application/x-font-opentype",
                    "application/octet-stream"
                )
            )
        }
        startActivityForResult(intent, PICK_FONT_FILE_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FONT_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val fontFile = copyFontToInternalStorage(uri)
                if (fontFile != null) {
                    try {
                        val typeface = Typeface.createFromFile(fontFile)
                        // Try to access a property to force load
                        typeface.style
                        onCustomFontSelected?.invoke(typeface, fontFile.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Show error dialog to user
                        dialogBuilder.showErrorDialog(
                            requireContext(),
                            title = "Invalid Font File",
                            message = "The selected file could not be loaded as a font. Please choose a valid font file."
                        )
                    }
                } else {
                    dialogBuilder.showErrorDialog(
                        requireContext(),
                        title = "File Error",
                        message = "Could not copy the selected file. Please try again."
                    )
                }
            }
        }
    }

    private fun copyFontToInternalStorage(uri: Uri): File? {
        val fileName = getFileName(uri) ?: "custom_font.ttf"
        val file = File(requireContext().filesDir, fileName)
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = it.getString(index)
            }
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // No binding to clean up in Compose
    }

    override fun onStop() {
        super.onStop()
        dialogBuilder.singleChoiceDialog?.dismiss()
    }
}