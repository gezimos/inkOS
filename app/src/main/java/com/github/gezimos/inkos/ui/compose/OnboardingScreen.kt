package com.github.gezimos.inkos.ui.compose

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.style.SettingsTheme

object OnboardingScreen {

    @Composable
    fun Show(
        onFinish: () -> Unit = {},
        onRequestNotificationPermission: (() -> Unit)? = null
    ) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var pushNotificationsEnabled by remember { mutableStateOf(prefs.pushNotificationsEnabled) }
    var showAudioWidgetEnabled by remember { mutableStateOf(prefs.showAudioWidgetEnabled) }
    var showClock by remember { mutableStateOf(prefs.showClock) }
    var showDate by remember { mutableStateOf(prefs.showDate) }
    var showDateBatteryCombo by remember { mutableStateOf(prefs.showDateBatteryCombo) }
    var showStatusBar by remember { mutableStateOf(prefs.showStatusBar) }
    var showQuote by remember { mutableStateOf(prefs.showQuote) }
    // Keep theme selection state lifted so Finish handler can access it
    var themeMode by remember { mutableStateOf(prefs.appTheme) }

    // Add a trigger for font changes to force recomposition
    var fontChangeKey by remember { mutableIntStateOf(0) }
    var quickUniversalFont by remember { mutableStateOf(prefs.universalFont) }

        // State for onboarding page
        var page by remember { mutableIntStateOf(prefs.onboardingPage) }
        val totalPages = 3
        val settingsSize = (prefs.settingsSize - 3)
        val titleFontSize = (settingsSize * 1.5).sp

        // Persist onboarding page index when it changes
        LaunchedEffect(page) {
            prefs.onboardingPage = page
        }

        // Helper to resolve an Activity from a possibly-wrapped Context
        fun resolveActivity(ctx: android.content.Context): Activity? {
            var c: android.content.Context = ctx
            while (c is ContextWrapper) {
                if (c is Activity) return c
                c = c.baseContext
            }
            return null
        }

        // Determine background color using the current themeMode state (not prefs.appTheme)
        val isDark = when (themeMode) {
            Constants.Theme.Light -> false
            Constants.Theme.Dark -> true
            Constants.Theme.System -> com.github.gezimos.inkos.helper.isSystemInDarkMode(
                context
            )
        }
        val backgroundColor = when (themeMode) {
            Constants.Theme.System ->
                if (com.github.gezimos.inkos.helper.isSystemInDarkMode(context)) Color.Black else Color.White

            Constants.Theme.Dark -> Color.Black
            Constants.Theme.Light -> Color.White
        }
        val topPadding = if (prefs.showStatusBar) 42.dp else 42.dp
        // Calculate bottom padding for nav bar/gestures
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        
        // Key to trigger recomposition when font changes - ensure SettingsTheme recreates with new font
        key(fontChangeKey) {
            SettingsTheme(isDark = isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(backgroundColor)
            ) {
                // Top-aligned welcome and description
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = topPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_inkos),
                        contentDescription = "InkOS Logo",
                        colorFilter = ColorFilter.tint(SettingsTheme.typography.title.color),
                        modifier = Modifier
                            .width(42.dp)
                            .padding(bottom = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "inkOS",
                        style = SettingsTheme.typography.title,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                // Vertically centered switches, 3 per page
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // FocusRequesters for first item on each page
                    val focusRequesterPage0 = remember { FocusRequester() }
                    val focusRequesterPage1 = remember { FocusRequester() }
                    val focusRequesterPage2 = remember { FocusRequester() }
                    // Move focus to first item on page change
                    var einkHelperEnabled by remember { mutableStateOf(prefs.einkHelperEnabled) }
                    var volumeKeyNavigation by remember { mutableStateOf(prefs.useVolumeKeysForPages) }
                    when (page) {
                        0 -> {
                            // Page 1: Theme Mode, Universal Font, Show Status Bar, Volume key navigation
                            SettingsComposable.FullLineSeparator(isDark = false)
                            Box(modifier = Modifier.focusRequester(focusRequesterPage0)) {
                                SettingsComposable.SettingsSelect(
                                    title = "Theme Mode",
                                    option = themeMode.name,
                                    fontSize = titleFontSize,
                                    onClick = {
                                        // Cycle through System -> Light -> Dark -> System
                                        val next = when (themeMode) {
                                            Constants.Theme.System -> Constants.Theme.Light
                                            Constants.Theme.Light -> Constants.Theme.Dark
                                            Constants.Theme.Dark -> Constants.Theme.System
                                        }
                                        themeMode = next
                                        prefs.appTheme = next
                                    }
                                )
                            }
                            LaunchedEffect(page) {
                                focusRequesterPage0.requestFocus()
                            }
                            SettingsComposable.FullLineSeparator(isDark = false)
                            // Custom font selector that shows font in its own typeface
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused = interactionSource.collectIsFocusedAsState().value
                            val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isFocused) Modifier.background(focusColor) else Modifier
                                    )
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        // Cycle through built-in font presets (exclude Custom)
                                        val fontEntries = Constants.FontFamily.entries.filter { it != Constants.FontFamily.Custom }
                                        val idx = fontEntries.indexOf(quickUniversalFont).let { if (it == -1) 0 else it }
                                        val next = fontEntries[(idx + 1) % fontEntries.size]
                                        quickUniversalFont = next
                                        prefs.universalFont = next
                                        // Also update the main fontFamily to trigger SettingsTheme refresh
                                        prefs.fontFamily = next
                                        // Trigger recomposition to see font change throughout the screen
                                        fontChangeKey++
                                    }
                                    .padding(vertical = 16.dp)
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Universal Font",
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    modifier = Modifier.weight(1f),
                                    color = SettingsTheme.typography.title.color
                                )

                                Text(
                                    text = quickUniversalFont.getString(context),
                                    style = SettingsTheme.typography.title.copy(
                                        fontFamily = quickUniversalFont.getFont(context)?.let { 
                                            androidx.compose.ui.text.font.FontFamily(it) 
                                        } ?: androidx.compose.ui.text.font.FontFamily.Default
                                    ),
                                    fontSize = titleFontSize,
                                    color = SettingsTheme.typography.title.color
                                )
                            }
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Show Status Bar",
                                fontSize = titleFontSize,
                                defaultState = showStatusBar,
                                onCheckedChange = {
                                    showStatusBar = it
                                    prefs.showStatusBar = it
                                    // Resolve an Activity and show/hide the status bar
                                    resolveActivity(context)?.let { activity ->
                                        if (it) com.github.gezimos.inkos.helper.showStatusBar(activity)
                                        else com.github.gezimos.inkos.helper.hideStatusBar(activity)
                                    }
                                }
                            )
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Volume Key Navigation",
                                fontSize = titleFontSize,
                                defaultState = volumeKeyNavigation,
                                onCheckedChange = {
                                    volumeKeyNavigation = it
                                    prefs.useVolumeKeysForPages = it
                                }
                            )
                        }

                        1 -> {
                            // Page 2: Clock, Date, Battery, Quote
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Show Clock",
                                fontSize = titleFontSize,
                                defaultState = showClock,
                                modifier = Modifier.focusRequester(focusRequesterPage1),
                                onCheckedChange = {
                                    showClock = it
                                    prefs.showClock = it
                                }
                            )
                            LaunchedEffect(page) {
                                focusRequesterPage1.requestFocus()
                            }
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Show Date",
                                fontSize = titleFontSize,
                                defaultState = showDate,
                                onCheckedChange = {
                                    showDate = it
                                    prefs.showDate = it
                                }
                            )
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Show Battery",
                                fontSize = titleFontSize,
                                defaultState = showDateBatteryCombo,
                                onCheckedChange = {
                                    showDateBatteryCombo = it
                                    prefs.showDateBatteryCombo = it
                                }
                            )
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Show Quote",
                                fontSize = titleFontSize,
                                defaultState = showQuote,
                                onCheckedChange = {
                                    showQuote = it
                                    prefs.showQuote = it
                                }
                            )
                        }

                        2 -> {
                            // Page 3: Notifications first, then E-ink Quality Mode
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Enable Notifications",
                                fontSize = titleFontSize,
                                defaultState = pushNotificationsEnabled,
                                modifier = Modifier.focusRequester(focusRequesterPage2),
                                onCheckedChange = {
                                    pushNotificationsEnabled = it
                                    prefs.pushNotificationsEnabled = it
                                    if (it) {
                                        val intent =
                                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        onRequestNotificationPermission?.invoke()
                                    }
                                }
                            )
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "Enable Audio Widget",
                                fontSize = titleFontSize,
                                defaultState = showAudioWidgetEnabled,
                                onCheckedChange = {
                                    showAudioWidgetEnabled = it
                                    prefs.showAudioWidgetEnabled = it
                                }
                            )
                            LaunchedEffect(page) {
                                focusRequesterPage2.requestFocus()
                            }
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsSwitch(
                                text = "E-ink Quality Mode",
                                fontSize = titleFontSize,
                                defaultState = einkHelperEnabled,
                                onCheckedChange = {
                                    einkHelperEnabled = it
                                    prefs.einkHelperEnabled = it
                                }
                            )
                            SettingsComposable.FullLineSeparator(isDark = false)
                            SettingsComposable.SettingsHomeItem(
                                title = "Set as Default Launcher",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                },
                                titleFontSize = titleFontSize
                            )
                        }
                    }
                    SettingsComposable.FullLineSeparator(isDark = false)
                    // One static tip per page (avoid dynamic behavior)
                    // Provide a fallback/default tip string used when page index is unexpected
                    val defaultTip = "Tip: Use number keys to quickly open apps; long-press for options"
                    val tipText = when (page) {
                        0 -> "Tip: Longpress in home for Settings"
                        1 -> "Tip: Hold 9 in home for Settings"
                        2 -> "Tip: Manage E-ink in Settings/Extra"
                        else -> null
                    }
                    if (tipText != null) {
                        Text(
                            text = tipText,
                            style = SettingsTheme.typography.body,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(start = 36.dp, end = 36.dp, top = 24.dp)
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = defaultTip,
                            style = SettingsTheme.typography.body,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(start = 36.dp, end = 36.dp, top = 24.dp)
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // Bottom-aligned navigation buttons
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = bottomPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    val backInteractionSource = remember { MutableInteractionSource() }
                    val backIsFocused = backInteractionSource.collectIsFocusedAsState().value
                    val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .then(if (backIsFocused) Modifier.background(focusColor) else Modifier)
                            .clickable(
                                enabled = page > 0,
                                interactionSource = backInteractionSource,
                                indication = null
                            ) {
                                if (page > 0) page--
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (page > 0) {
                            Text(
                                text = "Back",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp)
                            )
                        }
                    }
                    // Page indicator in the center
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom page indicator that responds to theme changes
                        val activeRes = R.drawable.ic_current_page
                        val inactiveRes = R.drawable.ic_new_page
                        val tintColor = if (isDark) Color.White else Color.Black
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 0 until totalPages) {
                                Image(
                                    painter = painterResource(id = if (i == page) activeRes else inactiveRes),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(tintColor),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (i == page) 12.dp else 10.dp)
                                )
                            }
                        }
                    }
                    // Next/Finish button
                    val nextInteractionSource = remember { MutableInteractionSource() }
                    val nextIsFocused = nextInteractionSource.collectIsFocusedAsState().value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .then(if (nextIsFocused) Modifier.background(focusColor) else Modifier)
                            .clickable(
                                interactionSource = nextInteractionSource,
                                indication = null
                            ) {
                                if (page < totalPages - 1) {
                                    page++
                                } else {
                                    // Apply selected theme and corresponding default colors (mirror LookFeelFragment)
                                    // Activity resolution not required here

                                    val isDark = when (themeMode) {
                                        Constants.Theme.Light -> false
                                        Constants.Theme.Dark -> true
                                        Constants.Theme.System -> com.github.gezimos.inkos.helper.isSystemInDarkMode(context)
                                    }

                                    // Persist theme and colors
                                    prefs.appTheme = themeMode
                                    prefs.backgroundColor = if (isDark) Color.Black.toArgb() else Color.White.toArgb()
                                    prefs.appColor = if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                                    prefs.clockColor = if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                                    prefs.batteryColor = if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                                    prefs.dateColor = if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                                    prefs.quoteColor = if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                                    prefs.audioWidgetColor = if (isDark) Color.White.toArgb() else Color.Black.toArgb()

                                    // Apply theme mode immediately to ensure proper shadow application
                                    val newThemeMode = when (themeMode) {
                                        Constants.Theme.Light -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                        Constants.Theme.Dark -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                        Constants.Theme.System -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                    }
                                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(newThemeMode)

                                    // Signal HomeFragment to refresh its UI (colors, fonts, viewmodel state)
                                    try {
                                        prefs.triggerForceRefreshHome()
                                    } catch (_: Exception) {
                                        // ignore
                                    }
                                    
                                    onFinish()
                                }
                            },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = if (page < totalPages - 1) "Next" else "Finish",
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp)
                        )
                    }
                }
            }
        }
    }
}
}