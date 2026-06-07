package com.github.gezimos.inkos.data

import android.graphics.Color as AndroidColor
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.helper.WallpaperUtility

data class ThemePreset(
    val name: String,
    val lightTextColor: Int,
    val lightBackgroundColor: Int,
    val darkTextColor: Int,
    val darkBackgroundColor: Int,
    val description: String = "",
    val isCustom: Boolean = false,
    val font: Constants.FontFamily = Constants.FontFamily.System,
    val iconMode: Int = 0,          // 0=Text/Letters (E-ink default, no bitmaps)
    val iconShape: Int = 1,         // 1=Rounded
    val showIcons: Boolean = false, // E-ink default: icons off
    val drawerShowIcons: Boolean? = null, // null = same as showIcons
    val textIslands: Boolean = false,
    val textIslandsShape: Int = 1,  // 1=Rounded
    val homeAlignment: Int = 1,     // 1=Center
    val clockAlignment: Int = 1,    // 1=Center
    val dateAlignment: Int? = null, // null = same as clockAlignment
    val clockStyle: Int = 0,        // 0=Simple, 1=Flip, 2=Box Solid, 3=Round, 4=Split, 5=Horizontal, 6=Box Outline, 7=Stacked, 8=Analog, 9=Digital, 10=Matrix
    val bottomWidgetType: String = "quote", // "quote", "events", "disabled", "search" (shortcuts), "total_usage", "page_dots"
    val shortcutHideOutline: Boolean = false,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showDateBatteryCombo: Boolean = true,
    val clockSize: Int = 52,
    val showSecondClock: Boolean = false,
    val secondClockOffsetHours: Int = 0,
    val appSize: Int = 32,
    val appGap: Int = 10,                   // textPaddingSize (home)
    val appDrawerGap: Int = 8,              // gap between apps in drawer/recents
    val appDrawerSize: Int = 30,
    val appDrawerAlignment: Int = 0,       // 0=Left
    val quoteSize: Int? = null,            // null = don't change
    val quoteText: String? = null,         // null = don't change
    val homeAppsNum: Int = 15,
    val homePagesNum: Int = 3,
    val showNotificationCount: Boolean = true, // default matches prefs default
    val homeAppsYOffset: Int = 0,
    val topWidgetMargin: Int? = null,    // null = device default (from dimen resource)
    val bottomWidgetMargin: Int? = null, // null = device default (from dimen resource)
    val backgroundOpacity: Int? = null,  // null = don't change, 0-255
    val wallpaperResourceId: Int? = null, // null = don't change, drawable resource ID or negative for generated
    val allCapsApps: Boolean = false,   // 0=Normal, allCaps=UPPERCASE, smallCaps=lowercase
    val smallCapsApps: Boolean = false,
)

object ThemePresets {
    val PRESETS = listOf(
        // 1. Classic
        ThemePreset(
            name = "Classic",
            lightTextColor = AndroidColor.BLACK,
            lightBackgroundColor = AndroidColor.WHITE,
            darkTextColor = AndroidColor.WHITE,
            darkBackgroundColor = AndroidColor.BLACK,
            font = Constants.FontFamily.PublicSans,
            quoteText = "Keep it simple.",
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            clockStyle = 0,
            appSize = 32,
            appDrawerSize = 30,
            homeAppsNum = 15,
            homePagesNum = 3,
        ),
        // 2. Haze
        ThemePreset(
            name = "Haze",
            lightTextColor = AndroidColor.parseColor("#1A1A1A"),
            lightBackgroundColor = AndroidColor.parseColor("#F5F5F5"),
            darkTextColor = AndroidColor.parseColor("#E8E8E8"),
            darkBackgroundColor = AndroidColor.parseColor("#1A1A1A"),
            font = Constants.FontFamily.ZalandoExp,
            textIslandsShape = 0,       // Pill
            homeAlignment = 1,
            clockAlignment = 1,
            dateAlignment = 1,
            clockStyle = 4,
            bottomWidgetType = "page_dots",
            appSize = 30,
            appDrawerSize = 28,
            homeAppsNum = 12,
            homePagesNum = 3,
            backgroundOpacity = 160,
            wallpaperResourceId = WallpaperUtility.DOTS_CENTER,
        ),
        // 3. Lite
        ThemePreset(
            name = "Lite",
            lightTextColor = AndroidColor.BLACK,
            lightBackgroundColor = AndroidColor.WHITE,
            darkTextColor = AndroidColor.WHITE,
            darkBackgroundColor = AndroidColor.BLACK,
            font = Constants.FontFamily.PublicSans,
            textIslandsShape = 0,
            homeAlignment = 1,
            clockAlignment = 1,
            dateAlignment = 1,
            bottomWidgetType = "disabled",
            showClock = false,
            showDate = false,
            showDateBatteryCombo = false,
            appSize = 42,
            appGap = 14,
            appDrawerGap = 12,
            appDrawerSize = 36,
            appDrawerAlignment = 1,
            homeAppsNum = 12,
            homePagesNum = 2,
            showNotificationCount = false,
        ),
        // 4. Terminal
        ThemePreset(
            name = "Terminal",
            lightTextColor = AndroidColor.parseColor("#071404"),
            lightBackgroundColor = AndroidColor.WHITE,
            darkTextColor = AndroidColor.parseColor("#50cf3c"),
            darkBackgroundColor = AndroidColor.parseColor("#071404"),
            font = Constants.FontFamily.Iosevka,
            iconMode = 0,
            showIcons = true,
            quoteText = "$ sudo reboot",
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            homeAppsNum = 6,
            homePagesNum = 1,
            appSize = 24,
            appDrawerSize = 24,
            backgroundOpacity = 100,
            wallpaperResourceId = WallpaperUtility.SCATTER_RIGHT,
        ),
        // 5. Indigo
        ThemePreset(
            name = "Indigo",
            lightTextColor = AndroidColor.parseColor("#041B4B"),
            lightBackgroundColor = AndroidColor.parseColor("#EDF2F7"),
            darkTextColor = AndroidColor.parseColor("#BDC3C7"),
            darkBackgroundColor = AndroidColor.parseColor("#041B4B"),
            font = Constants.FontFamily.ZalandoExp,
            iconMode = 5,
            showIcons = true,
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            clockStyle = 0,
            bottomWidgetType = "events",
            appSize = 28,
            appDrawerSize = 26,
            homeAppsNum = 12,
            homePagesNum = 3,
            backgroundOpacity = 140,
            wallpaperResourceId = WallpaperUtility.DOTS_RIGHT,
        ),
        // 7. Journal
        ThemePreset(
            name = "Journal",
            lightTextColor = AndroidColor.parseColor("#3C2F2F"),
            lightBackgroundColor = AndroidColor.parseColor("#F4ECD8"),
            darkTextColor = AndroidColor.parseColor("#F4ECD8"),
            darkBackgroundColor = AndroidColor.parseColor("#3C2F2F"),
            font = Constants.FontFamily.Vollkorn,
            quoteText = "write your story",
            iconMode = 4,               // Tinted (inkOS)
            iconShape = 0,              // Pill
            showIcons = true,
            textIslandsShape = 0,       // Pill
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            showSecondClock = true,
            secondClockOffsetHours = -8,
            homeAppsNum = 12,
            homePagesNum = 3,
            appSize = 28,
            appDrawerSize = 26,
            backgroundOpacity = 175,
            wallpaperResourceId = R.drawable.sf_left,
        ),
        // 8. Leaf
        ThemePreset(
            name = "Leaf",
            lightTextColor = AndroidColor.parseColor("#2D4A2B"),
            lightBackgroundColor = AndroidColor.parseColor("#E8F5E8"),
            darkTextColor = AndroidColor.parseColor("#B8D4B6"),
            darkBackgroundColor = AndroidColor.parseColor("#1A2E1A"),
            font = Constants.FontFamily.ZalandoExp,
            quoteText = "breathe deep",
            iconMode = 4,
            iconShape = 1,
            showIcons = true,
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            clockStyle = 7,         // Stacked
            appSize = 26,
            appDrawerSize = 26,
            homeAppsNum = 5,
            homePagesNum = 1,
            backgroundOpacity = 160,
            wallpaperResourceId = R.drawable.leaf,
        ),
        // 9. Bold
        ThemePreset(
            name = "Bold",
            lightTextColor = AndroidColor.BLACK,
            lightBackgroundColor = AndroidColor.WHITE,
            darkTextColor = AndroidColor.parseColor("#F7A228"),
            darkBackgroundColor = AndroidColor.BLACK,
            font = Constants.FontFamily.PublicSans,
            textIslandsShape = 0,   // Pill corners
            homeAlignment = 2,
            clockAlignment = 2,
            dateAlignment = 2,
            clockStyle = 2,         // Box Solid
            bottomWidgetType = "search",
            shortcutHideOutline = true,
            quoteSize = 21,
            appSize = 34,
            appDrawerSize = 32,
            homeAppsNum = 15,
            homePagesNum = 3,
            backgroundOpacity = 140,
            wallpaperResourceId = WallpaperUtility.DIAG_LEFT,
        ),
        // 9. Coast
        ThemePreset(
            name = "Coast",
            lightTextColor = AndroidColor.parseColor("#1B4965"),
            lightBackgroundColor = AndroidColor.parseColor("#E0F2F7"),
            darkTextColor = AndroidColor.parseColor("#A8D5E2"),
            darkBackgroundColor = AndroidColor.parseColor("#1B4965"),
            font = Constants.FontFamily.Shortstack,
            quoteText = "write that down.",
            iconMode = 2,
            iconShape = 0,          // Pill
            showIcons = true,
            homeAlignment = 2,
            clockAlignment = 0,
            dateAlignment = 2,
            clockStyle = 8,             // Analog
            clockSize = 40,
            showDate = false,
            showDateBatteryCombo = false,
            showNotificationCount = false,
            appSize = 28,
            appDrawerSize = 26,
            homeAppsNum = 6,
            homePagesNum = 1,
            backgroundOpacity = 140,
            wallpaperResourceId = WallpaperUtility.GRID_LEFT,
        ),
        // 10. Graphite
        ThemePreset(
            name = "Graphite",
            lightTextColor = AndroidColor.parseColor("#3D3D3D"),
            lightBackgroundColor = AndroidColor.parseColor("#F0F0F0"),
            darkTextColor = AndroidColor.parseColor("#C8C8C8"),
            darkBackgroundColor = AndroidColor.parseColor("#1E1E1E"),
            font = Constants.FontFamily.Manrope,
            homeAlignment = 1,
            clockAlignment = 1,
            dateAlignment = 1,
            clockStyle = 1,         // Flip
            bottomWidgetType = "total_usage",
            appSize = 28,
            appDrawerSize = 26,
            homeAppsNum = 4,
            homePagesNum = 1,
            smallCapsApps = true,
            backgroundOpacity = 200,
            wallpaperResourceId = WallpaperUtility.DASH_CENTER,
        ),
        // 11. Plum
        ThemePreset(
            name = "Plum",
            lightTextColor = AndroidColor.parseColor("#3B0A45"),
            lightBackgroundColor = AndroidColor.parseColor("#F8EEF9"),
            darkTextColor = AndroidColor.parseColor("#DA70D6"),
            darkBackgroundColor = AndroidColor.parseColor("#1A0A1F"),
            font = Constants.FontFamily.Manrope,
            homeAlignment = 1,
            clockAlignment = 1,
            dateAlignment = 1,
            clockStyle = 3,             // Round
            clockSize = 38,
            bottomWidgetType = "events",
            appSize = 30,
            appDrawerSize = 28,
            homeAppsNum = 12,
            homePagesNum = 3,
            backgroundOpacity = 100,
            wallpaperResourceId = WallpaperUtility.WAVE_CENTER,
        ),
        // 12. Neon
        ThemePreset(
            name = "Neon",
            lightTextColor = AndroidColor.parseColor("#008B8B"),
            lightBackgroundColor = AndroidColor.parseColor("#E0FFFE"),
            darkTextColor = AndroidColor.parseColor("#00FFFF"),
            darkBackgroundColor = AndroidColor.parseColor("#0A0A1A"),
            font = Constants.FontFamily.SpaceGrotesk,
            iconMode = 4,               // Tinted
            iconShape = 0,              // Pill
            showIcons = true,
            textIslandsShape = 0,       // Pill corners
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            clockStyle = 5,             // Horizontal
            bottomWidgetType = "search",
            quoteSize = 21,
            appSize = 26,
            appDrawerSize = 24,
            homeAppsNum = 6,
            homePagesNum = 1,
            allCapsApps = true,
            backgroundOpacity = 140,
            wallpaperResourceId = WallpaperUtility.CIRCLES_BL,
        ),
        // 13. Typer
        ThemePreset(
            name = "Typer",
            lightTextColor = AndroidColor.BLACK,
            lightBackgroundColor = AndroidColor.WHITE,
            darkTextColor = AndroidColor.WHITE,
            darkBackgroundColor = AndroidColor.BLACK,
            font = Constants.FontFamily.Iosevka,
            iconMode = 4,
            showIcons = true,
            quoteText = "I \u2764 NYC",
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            homeAppsNum = 5,
            homePagesNum = 1,
            appSize = 24,
            appDrawerSize = 24,
            backgroundOpacity = 10,
            wallpaperResourceId = R.drawable.nyc_left,
        ),
        // 14. Woods
        ThemePreset(
            name = "Woods",
            lightTextColor = AndroidColor.parseColor("#1A1A1A"),
            lightBackgroundColor = AndroidColor.parseColor("#F5F5F5"),
            darkTextColor = AndroidColor.parseColor("#E8E8E8"),
            darkBackgroundColor = AndroidColor.parseColor("#1A1A1A"),
            font = Constants.FontFamily.ZalandoExp,
            textIslandsShape = 0,       // Pill
            homeAlignment = 1,
            clockAlignment = 1,
            dateAlignment = 1,
            clockStyle = 4,             // Split
            bottomWidgetType = "search",
            shortcutHideOutline = true,
            quoteSize = 27,
            appSize = 30,
            appDrawerSize = 28,
            homeAppsNum = 0,
            homePagesNum = 0,
            backgroundOpacity = 160,
            wallpaperResourceId = R.drawable.forrest,
        )
    )
}
