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

    const val REQUEST_SET_DEFAULT_HOME = 777

    const val TRIPLE_TAP_DELAY_MS = 300
    const val LONG_PRESS_DELAY_MS = 500

    const val MIN_HOME_APPS = 0
    const val MAX_HOME_APPS = 30

    const val MIN_HOME_PAGES = 1

    // These are for the App Text Size (home screen app labels)
    const val MIN_APP_SIZE = 10
    const val MAX_APP_SIZE = 50

    // Add for settings text size
    const val MIN_SETTINGS_TEXT_SIZE = 8
    const val MAX_SETTINGS_TEXT_SIZE = 27

    // Add for notification text size
    const val MIN_LABEL_NOTIFICATION_TEXT_SIZE = 10
    const val MAX_LABEL_NOTIFICATION_TEXT_SIZE = 40

    const val BACKUP_WRITE = 1
    const val BACKUP_READ = 2

    const val THEME_BACKUP_WRITE = 11
    const val THEME_BACKUP_READ = 12


    const val MIN_TEXT_PADDING = 0
    const val MAX_TEXT_PADDING = 80
    
    // Specific min/max for Home Apps Y-Offset (dp)
    const val MIN_HOME_APPS_Y_OFFSET = 0
    const val MAX_HOME_APPS_Y_OFFSET = 500

    // Restore for date_size (not gap)
    const val MIN_CLOCK_SIZE = 12
    const val MAX_CLOCK_SIZE = 80

    // Update MAX_HOME_PAGES dynamically based on MAX_HOME_APPS
    const val DEFAULT_MAX_HOME_PAGES = 10
    var MAX_HOME_PAGES = DEFAULT_MAX_HOME_PAGES

    // Default widget margins
    const val DEFAULT_TOP_WIDGET_MARGIN = 35
    const val DEFAULT_BOTTOM_WIDGET_MARGIN = 50

    // Max widget margins
    const val MAX_TOP_WIDGET_MARGIN = 200
    const val MAX_BOTTOM_WIDGET_MARGIN = 200

    fun updateMaxHomePages(context: Context) {
        val prefs = Prefs(context)

        MAX_HOME_PAGES = if (prefs.homeAppsNum < DEFAULT_MAX_HOME_PAGES) {
            prefs.homeAppsNum
        } else {
            DEFAULT_MAX_HOME_PAGES
        }

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

    }

    enum class Action : EnumOption {
    Disabled,
    OpenApp,
    OpenAppDrawer,
    OpenNotificationsScreen,
    EinkRefresh,
    Brightness, // New action for brightness control
    LockScreen,
    ShowRecents,
    OpenQuickSettings,
    OpenPowerDialog,
    RestartApp,
    ExitLauncher,
    TogglePrivateSpace;

        fun getString(context: Context): String {
            return when (this) {
                OpenApp -> context.getString(R.string.open_app)
                TogglePrivateSpace -> context.getString(R.string.private_space)
                // NextPage/PreviousPage removed
                RestartApp -> context.getString(R.string.restart_launcher)
                OpenNotificationsScreen -> context.getString(R.string.notifications_screen_title)
                OpenAppDrawer -> context.getString(R.string.app_drawer)
                EinkRefresh -> context.getString(R.string.eink_refresh)
                ExitLauncher -> context.getString(R.string.settings_exit_inkos_title)
                LockScreen -> context.getString(R.string.lock_screen)
                ShowRecents -> context.getString(R.string.show_recents)
                OpenQuickSettings -> context.getString(R.string.quick_settings)
                OpenPowerDialog -> context.getString(R.string.power_dialog)
                Brightness -> "Brightness" // Temporary string, add to strings.xml later
                Disabled -> context.getString(R.string.disabled)
            }
        }

        @Composable
        override fun string(): String {
            return when (this) {
                OpenApp -> stringResource(R.string.open_app)
                TogglePrivateSpace -> stringResource(R.string.private_space)
                // NextPage/PreviousPage removed
                RestartApp -> stringResource(R.string.restart_launcher)
                OpenNotificationsScreen -> stringResource(R.string.notifications_screen_title)
                OpenAppDrawer -> stringResource(R.string.app_drawer)
                EinkRefresh -> stringResource(R.string.eink_refresh)
                ExitLauncher -> stringResource(R.string.settings_exit_inkos_title)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowRecents -> stringResource(R.string.show_recents)
                OpenQuickSettings -> stringResource(R.string.quick_settings)
                OpenPowerDialog -> stringResource(R.string.power_dialog)
                Brightness -> "Brightness" // Temporary string, add to strings.xml later
                Disabled -> stringResource(R.string.disabled)
            }
        }
    }

    enum class Theme : EnumOption {
        System,
        Dark,
        Light;

        // Function to get a string from a context (for non-Composable use)
        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                Dark -> context.getString(R.string.dark)
                Light -> context.getString(R.string.light)
            }
        }

        // Keep this for Composable usage
        @Composable
        override fun string(): String {
            return when (this) {
                System -> stringResource(R.string.system_default)
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
        }
    }

    enum class FontFamily : EnumOption {
        System,
        SpaceGrotesk,
        PlusJakarta,
        Merriweather,
        Manrope,
        Hoog,
        Custom; // Add Custom for user-uploaded font

        fun getFont(context: Context, customPath: String? = null): Typeface? {
            val prefs = Prefs(context)
            return when (this) {
                System -> getTrueSystemFont()
                Custom -> {
                    val path = customPath ?: prefs.customFontPath
                    if (!path.isNullOrBlank()) {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            try {
                                Typeface.createFromFile(path)
                            } catch (e: Exception) {
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
            }
        }

        fun getString(context: Context): String {
            return when (this) {
                System -> context.getString(R.string.system_default)
                SpaceGrotesk -> "Space Grotesk"
                PlusJakarta -> "Plus Jakarta"
                Merriweather -> context.getString(R.string.settings_font_merriweather)
                Manrope -> "Manrope Medium"
                Hoog -> context.getString(R.string.settings_font_hoog)
                Custom -> "Custom Font"
            }
        }

        companion object {
            // Helper to get all custom font display names and paths
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
                SpaceGrotesk -> "Space Grotesk"
                PlusJakarta -> "Plus Jakarta"
                Merriweather -> stringResource(R.string.settings_font_merriweather)
                Manrope -> "Manrope Medium"
                Hoog -> stringResource(R.string.settings_font_hoog)
                Custom -> "Custom Font"
            }
        }
    }
}