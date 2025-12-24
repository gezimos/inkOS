package com.github.gezimos.inkos.ui.compose

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.github.gezimos.inkos.EinkHelper
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme

object OnboardingScreen {

    /**
     * Checks if notification listener service is enabled.
     * This is what we need to READ notifications from other apps.
     */
    private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    @Composable
    fun Show(
        onFinish: () -> Unit = {}
    ) {
        val context = LocalContext.current
        val prefs = remember { Prefs(context) }
        // Obtain MainViewModel to route preference writes through the single source-of-truth
        val activity = LocalActivity.current
        val viewModel = activity?.let { act ->
            if (act is androidx.lifecycle.ViewModelStoreOwner) {
                androidx.lifecycle.ViewModelProvider(act)[com.github.gezimos.inkos.MainViewModel::class.java]
            } else {
                null
            }
        }

        // Check if notification listener service is enabled (this is what we need to READ notifications)
        val hasNotificationListener = remember(context) { isNotificationListenerEnabled(context) }
        var pushNotificationsEnabled by remember { mutableStateOf(hasNotificationListener) }
        // Pre-enable Audio Widget
        var showAudioWidgetEnabled by remember { mutableStateOf(true) }
        // Pre-enable all widgets on page 2
        var showClock by remember { mutableStateOf(true) }
        var showDate by remember { mutableStateOf(true) }
        var showDateBatteryCombo by remember { mutableStateOf(true) }
        var showStatusBar by remember { mutableStateOf(prefs.showStatusBar) }
        var showQuote by remember { mutableStateOf(true) }
        // Home Alignment state
        var homeAlignment by remember { mutableStateOf(prefs.homeAlignment) }
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
            viewModel?.setOnboardingPage(page)
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
        val isDark = themeMode == Constants.Theme.Dark
        // Use Theme.colors for runtime colors inside SettingsTheme
        // Calculate top padding for status bar when enabled
        val topPadding = if (prefs.showStatusBar) {
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        } else {
            0.dp
        }
        // Calculate bottom padding for nav bar/gestures
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // Key to trigger recomposition when font changes - ensure SettingsTheme recreates with new font
        key(fontChangeKey) {
            SettingsTheme(isDark = isDark) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(Theme.colors.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        // Title at the top with status bar padding and 24dp padding
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = topPadding + 24.dp,
                                    start = 24.dp,
                                    end = 24.dp,
                                    bottom = 24.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_foreground),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(SettingsTheme.typography.title.color),
                                modifier = Modifier.size(titleFontSize.value.dp)
                            )
                            Text(
                                text = "Setup",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = SettingsTheme.typography.title.color
                            )
                        }

                        // Content area - centered vertically between title and buttons
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // FocusRequesters for first item on each page
                            val focusRequesterPage0 = remember { FocusRequester() }
                            val focusRequesterPage1 = remember { FocusRequester() }
                            val focusRequesterPage2 = remember { FocusRequester() }
                            // Move focus to first item on page change
                            var volumeKeyNavigation by remember { mutableStateOf(prefs.useVolumeKeysForPages) }
                            val isMuditaDevice = remember { EinkHelper.isMuditaKompakt() }
                            when (page) {
                                0 -> {
                                    // Page 1: Theme Mode, Universal Font, Show Status Bar, Volume key navigation
                                    Box(modifier = Modifier.focusRequester(focusRequesterPage0)) {
                                        SettingsComposable.SettingsSelect(
                                            title = "Theme Mode",
                                            option = themeMode.name,
                                            fontSize = titleFontSize,
                                            onClick = {
                                                // Toggle between Light and Dark and apply immediate preview
                                                val next =
                                                    if (themeMode == Constants.Theme.Light) Constants.Theme.Dark else Constants.Theme.Light
                                                themeMode = next
                                                viewModel?.setAppTheme(next)
                                                // Persist default colors for immediate preview (use explicit mapping for the target theme)
                                                val bgForNext =
                                                    if (next == Constants.Theme.Dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                                val txtForNext =
                                                    if (next == Constants.Theme.Dark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                                viewModel?.setBackgroundColor(bgForNext)
                                                viewModel?.setTextColor(txtForNext)
                                                // Apply AppCompat night mode so elevation/shadows update immediately
                                                val newMode =
                                                    if (next == Constants.Theme.Dark) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                                    newMode
                                                )
                                            }
                                        )
                                    }
                                    LaunchedEffect(page) {
                                        focusRequesterPage0.requestFocus()
                                    }
                                    SettingsComposable.DashedSeparator()
                                    // Custom font selector that shows font in its own typeface
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isFocused =
                                        interactionSource.collectIsFocusedAsState().value
                                    val focusColor =
                                        Theme.colors.text.copy(alpha = if (isDark) 0.2f else 0.13f)
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
                                                val fontEntries =
                                                    Constants.FontFamily.entries.filter { it != Constants.FontFamily.Custom }
                                                val idx = fontEntries.indexOf(quickUniversalFont)
                                                    .let { if (it == -1) 0 else it }
                                                val next = fontEntries[(idx + 1) % fontEntries.size]
                                                quickUniversalFont = next
                                                viewModel?.setUniversalFont(next)
                                                // Also update the main fontFamily to trigger SettingsTheme refresh
                                                viewModel?.setFontFamily(next)
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
                                                fontFamily = quickUniversalFont.getFont(context)
                                                    ?.let {
                                                        androidx.compose.ui.text.font.FontFamily(it)
                                                    }
                                                    ?: androidx.compose.ui.text.font.FontFamily.Default
                                            ),
                                            fontSize = titleFontSize,
                                            color = SettingsTheme.typography.title.color
                                        )
                                    }
                                    SettingsComposable.DashedSeparator()
                                    SettingsComposable.SettingsSwitch(
                                        text = "Show Status Bar",
                                        fontSize = titleFontSize,
                                        defaultState = showStatusBar,
                                        onCheckedChange = {
                                            showStatusBar = it
                                            viewModel?.setShowStatusBar(it)
                                            // Resolve an Activity and show/hide the status bar
                                            resolveActivity(context)?.let { activity ->
                                                if (it) com.github.gezimos.inkos.helper.showStatusBar(
                                                    activity
                                                )
                                                else com.github.gezimos.inkos.helper.hideStatusBar(
                                                    activity
                                                )
                                            }
                                        }
                                    )
                                    SettingsComposable.DashedSeparator()
                                    SettingsComposable.SettingsSwitch(
                                        text = "Volume Key Navigation",
                                        fontSize = titleFontSize,
                                        defaultState = volumeKeyNavigation,
                                        onCheckedChange = {
                                            volumeKeyNavigation = it
                                            viewModel?.setUseVolumeKeysForPages(it)
                                        }
                                    )
                                }

                                1 -> {
                                    // Page 2: Clock, Date, Battery, Quote (all pre-enabled)
                                    SettingsComposable.SettingsSwitch(
                                        text = "Show Clock",
                                        fontSize = titleFontSize,
                                        defaultState = showClock,
                                        modifier = Modifier.focusRequester(focusRequesterPage1),
                                        onCheckedChange = {
                                            showClock = it
                                            viewModel?.setShowClock(it)
                                        }
                                    )
                                    LaunchedEffect(page) {
                                        focusRequesterPage1.requestFocus()
                                        // Pre-enable all widgets on page 2 when page loads
                                        if (!showClock) {
                                            showClock = true
                                            viewModel?.setShowClock(true)
                                        }
                                        if (!showDate) {
                                            showDate = true
                                            viewModel?.setShowDate(true)
                                        }
                                        if (!showDateBatteryCombo) {
                                            showDateBatteryCombo = true
                                            viewModel?.setShowDateBatteryCombo(true)
                                        }
                                        if (!showQuote) {
                                            showQuote = true
                                            viewModel?.setShowQuote(true)
                                        }
                                    }
                                    SettingsComposable.DashedSeparator()
                                    SettingsComposable.SettingsSwitch(
                                        text = "Show Date",
                                        fontSize = titleFontSize,
                                        defaultState = showDate,
                                        onCheckedChange = {
                                            showDate = it
                                            viewModel?.setShowDate(it)
                                        }
                                    )
                                    SettingsComposable.DashedSeparator()
                                    SettingsComposable.SettingsSwitch(
                                        text = "Show Battery",
                                        fontSize = titleFontSize,
                                        defaultState = showDateBatteryCombo,
                                        onCheckedChange = {
                                            showDateBatteryCombo = it
                                            viewModel?.setShowDateBatteryCombo(it)
                                        }
                                    )
                                    SettingsComposable.DashedSeparator()
                                    SettingsComposable.SettingsSwitch(
                                        text = "Show Quote",
                                        fontSize = titleFontSize,
                                        defaultState = showQuote,
                                        onCheckedChange = {
                                            showQuote = it
                                            viewModel?.setShowQuote(it)
                                        }
                                    )
                                }

                                2 -> {
                                    // Page 3: Notifications first, then E-ink Quality Mode
                                    // Check listener status when page is shown
                                    LaunchedEffect(page, context) {
                                        if (page == 2) {
                                            // Small delay to ensure settings have been applied if user just returned
                                            kotlinx.coroutines.delay(300)
                                            val hasListener = isNotificationListenerEnabled(context)
                                            pushNotificationsEnabled = hasListener
                                            // Sync with ViewModel
                                            viewModel?.setPushNotificationsEnabled(hasListener)
                                        }
                                    }

                                    // Periodically check listener status when on page 2 (e.g., when returning from settings)
                                    LaunchedEffect(page, context) {
                                        if (page == 2) {
                                            // Check every second while on page 2 to catch listener changes when user returns
                                            var currentPage = page
                                            while (currentPage == 2) {
                                                kotlinx.coroutines.delay(1000)
                                                // Re-check page state
                                                currentPage = page
                                                if (currentPage == 2) {
                                                    val hasListener =
                                                        isNotificationListenerEnabled(context)
                                                    if (pushNotificationsEnabled != hasListener) {
                                                        pushNotificationsEnabled = hasListener
                                                        viewModel?.setPushNotificationsEnabled(
                                                            hasListener
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    SettingsComposable.SettingsSwitch(
                                        text = "Enable Notifications",
                                        fontSize = titleFontSize,
                                        defaultState = pushNotificationsEnabled,
                                        modifier = Modifier.focusRequester(focusRequesterPage2),
                                        onCheckedChange = { requestedState ->
                                            if (requestedState) {
                                                // User wants to enable notifications - check if listener is enabled
                                                val hasListener =
                                                    isNotificationListenerEnabled(context)

                                                // If listener is already enabled, enable immediately
                                                if (hasListener) {
                                                    pushNotificationsEnabled = true
                                                    viewModel?.setPushNotificationsEnabled(true)
                                                    return@SettingsSwitch
                                                }

                                                // Otherwise, open notification listener settings
                                                val intent =
                                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)

                                                // Keep switch disabled - it will be enabled when user returns and listener is enabled
                                                // The LaunchedEffect will check and update the switch when user returns
                                                pushNotificationsEnabled = false
                                            } else {
                                                // User wants to disable notifications - open settings so they can revoke the permission
                                                val intent =
                                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)

                                                // Keep current state - it will update when user returns and actually disables the listener
                                                // The LaunchedEffect will check and update the switch when user returns
                                            }
                                        }
                                    )
                                    SettingsComposable.DashedSeparator()
                                    SettingsComposable.SettingsSwitch(
                                        text = "Enable Audio Widget",
                                        fontSize = titleFontSize,
                                        defaultState = showAudioWidgetEnabled,
                                        onCheckedChange = {
                                            showAudioWidgetEnabled = it
                                            viewModel?.setShowAudioWidget(it)
                                        }
                                    )
                                    LaunchedEffect(page) {
                                        focusRequesterPage2.requestFocus()
                                        // Pre-enable Audio Widget when page loads
                                        if (!showAudioWidgetEnabled) {
                                            showAudioWidgetEnabled = true
                                            viewModel?.setShowAudioWidget(true)
                                        }
                                    }
                                    SettingsComposable.DashedSeparator()
                                    // Home Alignment selector (replaces E-ink mode)
                                    val alignmentLabels = listOf(
                                        stringResource(R.string.left),
                                        stringResource(R.string.center),
                                        stringResource(R.string.right)
                                    )
                                    SettingsComposable.SettingsSelect(
                                        title = "Home Alignment",
                                        option = alignmentLabels.getOrElse(homeAlignment) { stringResource(R.string.left) },
                                        fontSize = titleFontSize,
                                        onClick = {
                                            val next = (homeAlignment + 1) % 3
                                            homeAlignment = next
                                            viewModel?.setHomeAlignment(next)
                                        }
                                    )
                                    SettingsComposable.DashedSeparator()
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
                        }

                        // Bottom navigation buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = bottomPadding),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back button - use fixed width to prevent cutoff
                            val backInteractionSource = remember { MutableInteractionSource() }
                            val backIsFocused =
                                backInteractionSource.collectIsFocusedAsState().value
                            val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 120.dp)
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
                                        modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                                val tintColor = Theme.colors.text
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
                            // Next/Finish button - use fixed width to prevent cutoff
                            val nextInteractionSource = remember { MutableInteractionSource() }
                            val nextIsFocused =
                                nextInteractionSource.collectIsFocusedAsState().value
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 120.dp)
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

                                            themeMode == Constants.Theme.Dark

                                            // Persist theme choice and default colors via ViewModel
                                            viewModel?.setAppTheme(themeMode)
                                            val bgForMode =
                                                if (themeMode == Constants.Theme.Dark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                            val txtForMode =
                                                if (themeMode == Constants.Theme.Dark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                            viewModel?.setBackgroundColor(bgForMode)
                                            viewModel?.setTextColor(txtForMode)

                                            // Apply theme mode immediately to ensure proper shadow application
                                            val newThemeMode = when (themeMode) {
                                                Constants.Theme.Light -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                                Constants.Theme.Dark -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                            }
                                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                                newThemeMode
                                            )

                                            // Ensure all preferences are saved before finishing
                                            // Widget preferences (already saved via ViewModel during onboarding)
                                            viewModel?.setShowClock(showClock)
                                            viewModel?.setShowDate(showDate)
                                            viewModel?.setShowDateBatteryCombo(showDateBatteryCombo)
                                            viewModel?.setShowQuote(showQuote)
                                            viewModel?.setShowAudioWidget(showAudioWidgetEnabled)
                                            viewModel?.setHomeAlignment(homeAlignment)
                                            
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
                                    modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}