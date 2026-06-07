package com.github.gezimos.inkos.data

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.res.ResourcesCompat
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.helper.getTrueSystemFont

interface EnumOption {
    @Composable
    fun string(): String
}


object Constants {
    // E-ink refresh delay (ms)
    const val DEFAULT_EINK_REFRESH_DELAY = 100
    const val MIN_EINK_REFRESH_DELAY = 25
    const val MAX_EINK_REFRESH_DELAY = 1500

    const val MIN_HOME_APPS = 0
    const val MAX_HOME_APPS = 30

    const val MIN_HOME_PAGES = 1

    const val MIN_APP_SIZE = 10
    const val MAX_APP_SIZE = 60
    // Add for settings text size
    const val MIN_SETTINGS_TEXT_SIZE = 8
    const val MAX_SETTINGS_TEXT_SIZE = 27

    // Add for notification text size
    const val MIN_LABEL_NOTIFICATION_TEXT_SIZE = 10
    const val MAX_LABEL_NOTIFICATION_TEXT_SIZE = 40

    const val MIN_TEXT_PADDING = 0
    const val MAX_TEXT_PADDING = 80
    // Specific min/max for Home Apps Y-Offset (dp)
    const val MIN_HOME_APPS_Y_OFFSET = 0
    const val MAX_HOME_APPS_Y_OFFSET = 500

    // Restore for date_size (not gap)
    const val MIN_CLOCK_SIZE = 12
    const val MAX_CLOCK_SIZE = 100

    const val DEFAULT_MAX_HOME_PAGES = 10
    var MAX_HOME_PAGES = DEFAULT_MAX_HOME_PAGES

    const val DEFAULT_SHORT_SWIPE_RATIO = 0.25f
    const val DEFAULT_LONG_SWIPE_RATIO = 2.5f
    const val MIN_SHORT_SWIPE_RATIO = 0.01f
    const val MAX_SHORT_SWIPE_RATIO = 1.0f
    const val MIN_LONG_SWIPE_RATIO = 1.1f
    const val MAX_LONG_SWIPE_RATIO = 5.0f

    const val DEFAULT_APP_DRAWER_GAP = 8

    // Default font size (sp) for app drawer labels
    const val DEFAULT_APP_DRAWER_SIZE = 24

    const val INKOS_SHORTCUT_APP_DRAWER = "inkos_app_drawer"
    const val INKOS_SHORTCUT_NOTIFICATIONS = "inkos_notifications"
    const val INKOS_SHORTCUT_SIMPLE_TRAY = "inkos_simple_tray"
    const val INKOS_SHORTCUT_HUB = "inkos_hub"
    const val INKOS_SHORTCUT_RECENTS = "inkos_recents"
    const val INKOS_SHORTCUT_SETTINGS = "inkos_settings"

    const val ACTION_OPEN_APP_DRAWER = "com.github.gezimos.inkos.action.OPEN_APP_DRAWER"
    const val ACTION_OPEN_NOTIFICATIONS = "com.github.gezimos.inkos.action.OPEN_NOTIFICATIONS"
    const val ACTION_OPEN_SIMPLE_TRAY = "com.github.gezimos.inkos.action.OPEN_SIMPLE_TRAY"
    const val ACTION_OPEN_HUB = "com.github.gezimos.inkos.action.OPEN_HUB"
    const val ACTION_OPEN_RECENTS = "com.github.gezimos.inkos.action.OPEN_RECENTS"
    const val ACTION_OPEN_SETTINGS = "com.github.gezimos.inkos.action.OPEN_SETTINGS"


    const val INTERNAL_CONTACT_PREFIX = "com.inkos.contact."
    const val INTERNAL_WEB_SEARCH = "com.inkos.websearch"
    const val INTERNAL_SETTINGS = "com.inkos.settings"
    const val INTERNAL_MUSIC = "com.inkos.music"
    const val INTERNAL_FILES = "com.inkos.files"

    // Separators
    const val SEPARATOR_EMPTY = "com.inkos.separator.empty"
    const val SEPARATOR_EM_DASH = "com.inkos.separator.emdash"
    const val SEPARATOR_DOTS = "com.inkos.separator.dots"
    fun isSeparator(pkg: String) = pkg.startsWith("com.inkos.separator.")

    // Max widget margins
    const val MAX_TOP_WIDGET_MARGIN = 200
    const val MAX_BOTTOM_WIDGET_MARGIN = 200

    // Android AppWidget hosting defaults
    const val DEFAULT_ANDROID_WIDGET_HEIGHT = 120
    const val MIN_ANDROID_WIDGET_HEIGHT = 60
    const val MAX_ANDROID_WIDGET_HEIGHT = 400
    const val DEFAULT_ANDROID_WIDGET_MARGIN = 0 // percentage of screen width
    const val MAX_ANDROID_WIDGET_MARGIN = 80
    const val APPWIDGET_HOST_ID = 1024
    fun updateMaxHomePages(context: Context) {
        val prefs = Prefs(context)

        MAX_HOME_PAGES = if (prefs.homeAppsNum < DEFAULT_MAX_HOME_PAGES) {
            prefs.homeAppsNum
        } else {
            DEFAULT_MAX_HOME_PAGES
        }

    }

    object PrefKeys {
        const val APP_THEME = "APP_THEME"
        const val TEXT_COLOR = "TEXT_COLOR"
        const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
        const val DARK_TEXT_COLOR = "DARK_TEXT_COLOR"
        const val DARK_BACKGROUND_COLOR = "DARK_BACKGROUND_COLOR"
        const val STATUS_BAR = "STATUS_BAR"
        const val NAVIGATION_BAR = "NAVIGATION_BAR"
        const val TEXT_SIZE_SETTINGS = "TEXT_SIZE_SETTINGS"
        const val APPS_FONT = "APPS_FONT"
        const val CLOCK_FONT = "CLOCK_FONT"
        const val QUOTE_FONT = "QUOTE_FONT"
        const val NOTIFICATIONS_FONT = "NOTIFICATIONS_FONT"
        const val NOTIFICATION_FONT = "NOTIFICATION_FONT"
        const val STATUS_FONT = "STATUS_FONT"
        const val LETTERS_FONT = "LETTERS_FONT"
        const val LETTERS_TITLE_FONT = "LETTERS_TITLE_FONT"
        const val TEXT_PADDING_SIZE = "TEXT_PADDING_SIZE"
        const val APP_SIZE_TEXT = "APP_SIZE_TEXT"
        const val CLOCK_SIZE_TEXT = "CLOCK_SIZE_TEXT"
        const val QUOTE_TEXT_SIZE = "QUOTE_TEXT_SIZE"
        const val QUOTE_TEXT = "QUOTE_TEXT"
        const val SHOW_QUOTE = "SHOW_QUOTE"
        const val SHOW_AUDIO_WIDGET = "SHOW_AUDIO_WIDGET"
        const val SHOW_DATE = "SHOW_DATE"
        const val SHOW_AM_PM = "SHOW_AM_PM"
        const val SHOW_SECOND_CLOCK = "SHOW_SECOND_CLOCK"
        const val CLOCK_MODE = "CLOCK_MODE"
        const val CLOCK_STYLE = "CLOCK_STYLE"
        const val SECOND_CLOCK_OFFSET_HOURS = "SECOND_CLOCK_OFFSET_HOURS"
        const val SHOW_CLOCK = "SHOW_CLOCK"
        const val SHOW_DATE_BATTERY_COMBO = "SHOW_DATE_BATTERY_COMBO"
        const val SHOW_NOTIFICATION_COUNT = "SHOW_NOTIFICATION_COUNT"
        const val NOTIFICATION_COUNT_SOURCE = "NOTIFICATION_COUNT_SOURCE"
        const val BACKGROUND_OPACITY = "BACKGROUND_OPACITY"
        const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
        const val HOME_CLOCK_ALIGNMENT = "HOME_CLOCK_ALIGNMENT"
        const val HOME_DATE_ALIGNMENT = "HOME_DATE_ALIGNMENT"
        const val HOME_QUOTE_ALIGNMENT = "HOME_QUOTE_ALIGNMENT"
        const val HOME_APPS_Y_OFFSET = "HOME_APPS_Y_OFFSET"

        const val HIDE_HOME_APPS = "HIDE_HOME_APPS"
        const val APP_DRAWER_ALIGNMENT = "APP_DRAWER_ALIGNMENT"
        const val APP_DRAWER_SIZE = "APP_DRAWER_SIZE"
        const val APP_DRAWER_GAP = "APP_DRAWER_GAP"
        const val APP_DRAWER_SEARCH_ENABLED = "APP_DRAWER_SEARCH_ENABLED"
        const val APP_DRAWER_SEARCH_HIDDEN_APPS = "APP_DRAWER_SEARCH_HIDDEN_APPS"
        const val APP_DRAWER_AUTO_LAUNCH = "APP_DRAWER_AUTO_LAUNCH"
        const val APP_DRAWER_AUTO_SHOW_KEYBOARD = "APP_DRAWER_AUTO_SHOW_KEYBOARD"
        const val TEXT_ISLANDS = "TEXT_ISLANDS"
        const val TEXT_ISLANDS_INVERTED = "TEXT_ISLANDS_INVERTED"
        const val TEXT_ISLANDS_SHAPE = "TEXT_ISLANDS_SHAPE"
        const val SHOW_ICONS = "SHOW_ICONS"
        const val DRAWER_SHOW_ICONS = "DRAWER_SHOW_ICONS"
        const val APP_DRAWER_AZ_FILTER = "APP_DRAWER_AZ_FILTER"
        const val APP_DRAWER_SORT_ORDER = "APP_DRAWER_SORT_ORDER"
        const val APP_DRAWER_PAGER = "APP_DRAWER_PAGER"
        const val SHOW_NOTIFICATION_BADGE = "show_notification_badge"
        const val NOTIFICATION_INDICATOR_STYLE = "notification_indicator_style"
        const val TOP_WIDGET_MARGIN = "TOP_WIDGET_MARGIN"
        const val BOTTOM_WIDGET_MARGIN = "BOTTOM_WIDGET_MARGIN"
        const val DATE_FONT = "DATE_FONT"
        const val DATE_SIZE_TEXT = "date_text_size"
        const val ALL_CAPS_APPS = "ALL_CAPS_APPS"
        const val SMALL_CAPS_APPS = "SMALL_CAPS_APPS"
        const val HAPTIC_FEEDBACK = "HAPTIC_FEEDBACK"
        const val VIBRATION_SCALE = "VIBRATION_SCALE"
        const val HOME_PAGER = "HOME_PAGER"
        const val LOCKED_APPS = "LOCKED_APPS"
        const val SHOW_ANDROID_WIDGET = "SHOW_ANDROID_WIDGET"
        const val ANDROID_WIDGET_ID = "ANDROID_WIDGET_ID"
        const val ANDROID_WIDGET_HEIGHT = "ANDROID_WIDGET_HEIGHT"
        const val ANDROID_WIDGET_MARGIN_START = "ANDROID_WIDGET_MARGIN_START"
        const val ANDROID_WIDGET_MARGIN_END = "ANDROID_WIDGET_MARGIN_END"
        const val BOTTOM_WIDGET_TYPE = "BOTTOM_WIDGET_TYPE"
        const val EVENTS_CALENDAR_ID = "EVENTS_CALENDAR_ID"
        const val EVENTS_FILTER = "EVENTS_FILTER"
        const val EVENTS_INDEX = "EVENTS_INDEX"
        const val EVENTS_CALENDAR_NAME = "EVENTS_CALENDAR_NAME"
        const val EVENTS_HIDE_CONTROLS = "EVENTS_HIDE_CONTROLS"
        const val SHORTCUT_LEFT_ICON = "SHORTCUT_LEFT_ICON"
        const val SHORTCUT_LEFT_ACTION = "SHORTCUT_LEFT_ACTION"
        const val SHORTCUT_RIGHT_ICON = "SHORTCUT_RIGHT_ICON"
        const val SHORTCUT_RIGHT_ACTION = "SHORTCUT_RIGHT_ACTION"
        const val SHORTCUT_PAGE_DOTS = "SHORTCUT_PAGE_DOTS"
    }

    /** Events filter: 0=24h, 1=1 week, 2=2 weeks, 3=1 month */
    enum class EventsFilter(val index: Int, val days: Int) {
        HOURS_24(0, 1),
        WEEK_1(1, 7),
        WEEKS_2(2, 14),
        MONTH_1(3, 30)
    }

    enum class BottomWidgetType(val value: String) {
        Disabled("disabled"),
        Quote("quote"),
        Events("events"),
        AndroidWidget("android_widget"),
        Shortcuts("search"), // value kept as "search" for backward compat
        TotalUsage("total_usage"),
        PageDots("page_dots")
    }

    enum class NotificationIndicator(val symbol: String, val label: String, val isSuperscript: Boolean) {
        Asterisk("*", "Asterisk", true),
        FilledCircle("●", "Filled Circle", false),
        OutlineCircle("○", "Outline Circle", false),
        FilledDiamond("◆", "Filled Diamond", false),
        FilledSquare("■", "Filled Square", false);

        companion object {
            fun fromOrdinal(value: Int): NotificationIndicator =
                entries.getOrElse(value) { Asterisk }
        }
    }

    enum class ShortcutIcon {
        Disabled, Search, Phone, Messages, Camera, Notes, Bubble, Music, Light, Star, Clock
    }

    enum class BackupType {
        FullSystem,
        Theme
    }

    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
        PrivateApps,
        SetHomeApp,
        SetSwipeUp,
        SetSwipeDown,
        SetSwipeLeft,
        SetSwipeRight,
        SetClickClock,
        SetDoubleTap,
        SetClickDate,
        SetQuoteWidget,
        EditFavorites,
        EditHiddenApps,
        SetShortcutLeft,
        SetShortcutRight,

    }

    enum class Action : EnumOption {
    Disabled,
    OpenApp,
    OpenAppDrawer,
    OpenLettersScreen,
    OpenRecentsScreen,
    OpenSimpleTray,
    EinkRefresh,
    Brightness, // New action for brightness control
    LockScreen,
    ShowRecents,
    OpenQuickSettings,
    OpenPowerDialog,
    RestartApp,
    ExitLauncher,
    OpenHub,
    OpenSettings,
    Search,
    TogglePrivateSpace,
    ToggleWorkProfile;

        companion object {
            /** inkOS-related actions for gesture picker Tab 2 */
            val INKOS_ACTIONS = listOf(
                OpenAppDrawer, OpenLettersScreen, OpenRecentsScreen,
                OpenSimpleTray, OpenHub, OpenSettings, Search, EinkRefresh, Brightness
            )
            /** System actions for gesture picker Tab 3 */
            val SYSTEM_ACTIONS = listOf(
                LockScreen, ShowRecents, OpenQuickSettings, OpenPowerDialog,
                RestartApp, ExitLauncher, TogglePrivateSpace, ToggleWorkProfile, Disabled
            )
        }

        fun getString(context: Context): String {
            return when (this) {
                OpenApp -> context.getString(R.string.open_app)
                TogglePrivateSpace -> context.getString(R.string.private_space)
                ToggleWorkProfile -> context.getString(R.string.work_profile)
                RestartApp -> context.getString(R.string.restart_launcher)
                OpenLettersScreen -> context.getString(R.string.letters_screen_title)
                OpenRecentsScreen -> "Recents"
                OpenSimpleTray -> context.getString(R.string.simple_tray_title)
                OpenHub -> context.getString(R.string.shortcut_hub_short)
                OpenSettings -> "Settings"
                OpenAppDrawer -> context.getString(R.string.app_drawer)
                EinkRefresh -> context.getString(R.string.eink_refresh)
                ExitLauncher -> context.getString(R.string.settings_exit_inkos_title)
                LockScreen -> context.getString(R.string.lock_screen)
                ShowRecents -> context.getString(R.string.show_recents)
                OpenQuickSettings -> context.getString(R.string.quick_settings)
                OpenPowerDialog -> context.getString(R.string.power_dialog)
                Brightness -> "Brightness" // Temporary string, add to strings.xml later
                Search -> "Search"
                Disabled -> context.getString(R.string.disabled)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                OpenApp -> stringResource(R.string.open_app)
                TogglePrivateSpace -> stringResource(R.string.private_space)
                ToggleWorkProfile -> stringResource(R.string.work_profile)
                RestartApp -> stringResource(R.string.restart_launcher)
                OpenLettersScreen -> stringResource(R.string.letters_screen_title)
                OpenRecentsScreen -> "Recents"
                OpenSimpleTray -> stringResource(R.string.simple_tray_title)
                OpenHub -> stringResource(R.string.shortcut_hub_short)
                OpenSettings -> "Settings"
                OpenAppDrawer -> stringResource(R.string.app_drawer)
                EinkRefresh -> stringResource(R.string.eink_refresh)
                ExitLauncher -> stringResource(R.string.settings_exit_inkos_title)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowRecents -> stringResource(R.string.show_recents)
                OpenQuickSettings -> stringResource(R.string.quick_settings)
                OpenPowerDialog -> stringResource(R.string.power_dialog)
                Brightness -> "Brightness" // Temporary string, add to strings.xml later
                Search -> "Search"
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class Theme : EnumOption {
        Dark,
        Light,
        System;

        fun getString(context: Context): String {
            return when (this) {
                Dark -> context.getString(R.string.dark)
                Light -> context.getString(R.string.light)
                System -> context.getString(R.string.system_default)
            }
        }

        // Keep this for Composable usage
        @Composable
        override fun string(): String {
            return when (this) {
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
                System -> stringResource(R.string.system_default)
            }
        }
    }

    enum class FontFamily : EnumOption {
        PublicSans,
        Iosevka,
        ZalandoExp,
        Vollkorn,
        SpaceGrotesk,
        PlusJakarta,
        Merriweather,
        Shortstack,
        Manrope,
        Hoog,
        System,
        Custom;

        fun getFont(context: Context, customPath: String? = null): Typeface? {
            val prefs = Prefs(context)
            return when (this) {
                System -> getTrueSystemFont()
                Shortstack -> ResourcesCompat.getFont(context, R.font.shortstack)
                PublicSans -> ResourcesCompat.getFont(context, R.font.publicsans)
                Vollkorn -> ResourcesCompat.getFont(context, R.font.vollkorn)
                Custom -> {
                    val path = customPath ?: prefs.customFontPath
                    if (!path.isNullOrBlank()) {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            try {
                                Typeface.createFromFile(path)
                            } catch (_: Exception) {
                                getTrueSystemFont()
                            }
                        } else {
                            getTrueSystemFont()
                        }
                    } else getTrueSystemFont()
                }

                SpaceGrotesk -> ResourcesCompat.getFont(context, R.font.spacegrotesk)
                PlusJakarta -> ResourcesCompat.getFont(context, R.font.plusjakartasansaitalic)
                Merriweather -> ResourcesCompat.getFont(context, R.font.merriweather)
                Manrope -> ResourcesCompat.getFont(context, R.font.manropemedium)
                Hoog -> ResourcesCompat.getFont(context, R.font.hoog)
                Iosevka -> ResourcesCompat.getFont(context, R.font.iosevka)
                ZalandoExp -> ResourcesCompat.getFont(context, R.font.zalandosansexpandedregular)
            }
        }

        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                Shortstack -> "Shortstack"
                PublicSans -> "Public Sans"
                Vollkorn -> "Vollkorn"
                SpaceGrotesk -> "Space Grotesk"
                PlusJakarta -> "Plus Jakarta"
                Merriweather -> context.getString(R.string.settings_font_merriweather)
                Manrope -> "Manrope Medium"
                Hoog -> context.getString(R.string.settings_font_hoog)
                Iosevka -> "Iosevka"
                ZalandoExp -> "Zalando Expanded"
                Custom -> "Custom Font"
            }
        }

        companion object {
            fun getAllCustomFonts(context: Context): List<Pair<String, String>> {
                val prefs = Prefs(context)
                return prefs.customFontPaths.map { path ->
                    // Remove "Custom:" and limit to 24 chars
                    path.substringAfterLast('/').take(24) to path
                }
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                Shortstack -> "Shortstack"
                PublicSans -> "Public Sans"
                Vollkorn -> "Vollkorn"
                SpaceGrotesk -> "Space Grotesk"
                PlusJakarta -> "Plus Jakarta"
                Merriweather -> stringResource(R.string.settings_font_merriweather)
                Manrope -> "Manrope Medium"
                Hoog -> stringResource(R.string.settings_font_hoog)
                Iosevka -> "Iosevka"
                ZalandoExp -> "Zalando Expanded"
                Custom -> "Custom Font"
            }
        }
    }
}