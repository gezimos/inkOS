package com.github.gezimos.inkos.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import com.github.gezimos.inkos.R
import android.util.Log
import androidx.core.content.edit
import com.github.gezimos.inkos.helper.getUserHandleFromString
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val PREFS_FILENAME = "com.github.gezimos.inkos"

private const val APP_VERSION = "APP_VERSION"
private const val FIRST_OPEN = "FIRST_OPEN"
private const val LOCK_MODE = "LOCK_MODE"
private const val HOME_APPS_NUM = "HOME_APPS_NUM"
private const val HOME_PAGES_NUM = "HOME_PAGES_NUM"
private const val HOME_PAGES_PAGER = "HOME_PAGES_PAGER"

private const val HOME_PAGE_RESET = "HOME_PAGE_RESET"
private const val HOME_CLICK_AREA = "HOME_CLICK_AREA"
private const val STATUS_BAR = "STATUS_BAR"
private const val NAVIGATION_BAR = "NAVIGATION_BAR"
// SHOW_BATTERY constant removed
private const val SHOW_AUDIO_WIDGET_ENABLE = "SHOW_AUDIO_WIDGET"
private const val HOME_LOCKED = "HOME_LOCKED"
private const val SETTINGS_LOCKED = "SETTINGS_LOCKED"
private const val SELECTED_APP_SHORTCUTS = "SELECTED_APP_SHORTCUTS"
private const val SHOW_CLOCK = "SHOW_CLOCK"
private const val SWIPE_RIGHT_ACTION = "SWIPE_RIGHT_ACTION"
private const val SWIPE_LEFT_ACTION = "SWIPE_LEFT_ACTION"
private const val SWIPE_UP_ACTION = "SWIPE_UP_ACTION"
private const val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
private const val CLICK_CLOCK_ACTION = "CLICK_CLOCK_ACTION"
private const val SHORT_SWIPE_THRESHOLD_RATIO = "SHORT_SWIPE_THRESHOLD_RATIO"
private const val LONG_SWIPE_THRESHOLD_RATIO = "LONG_SWIPE_THRESHOLD_RATIO"
private const val DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val LOCKED_APPS = "LOCKED_APPS"
private const val LAUNCHER_FONT = "LAUNCHER_FONT"
private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"
private const val APP_SHORTCUT_ID = "APP_SHORTCUT_ID"
private const val APP_THEME = "APP_THEME"
private const val SWIPE_LEFT = "SWIPE_LEFT"
private const val SWIPE_RIGHT = "SWIPE_RIGHT"
private const val SWIPE_UP = "SWIPE_UP"
private const val SWIPE_DOWN = "SWIPE_DOWN"
private const val CLICK_CLOCK = "CLICK_CLOCK"
private const val DOUBLE_TAP = "DOUBLE_TAP"
private const val APP_SIZE_TEXT = "APP_SIZE_TEXT"
private const val CLOCK_SIZE_TEXT = "CLOCK_SIZE_TEXT"

private const val TEXT_SIZE_SETTINGS = "TEXT_SIZE_SETTINGS"
private const val TEXT_PADDING_SIZE = "TEXT_PADDING_SIZE"
private const val SHOW_NOTIFICATION_BADGE = "show_notification_badge"
private const val NOTIFICATION_INDICATOR_STYLE = "notification_indicator_style"
private const val ONBOARDING_PAGE = "ONBOARDING_PAGE"

private const val EDGE_SWIPE_BACK_ENABLED = "EDGE_SWIPE_BACK_ENABLED"

private const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
private const val BACKGROUND_OPACITY = "BACKGROUND_OPACITY"
private const val TEXT_COLOR = "TEXT_COLOR"
private const val DARK_BACKGROUND_COLOR = "DARK_BACKGROUND_COLOR"
private const val DARK_TEXT_COLOR = "DARK_TEXT_COLOR"

private const val APPS_FONT = "APPS_FONT"
private const val CLOCK_FONT = "CLOCK_FONT"
private const val STATUS_FONT = "STATUS_FONT"  // For Calendar, Alarm, Battery
private const val NOTIFICATION_FONT = "NOTIFICATION_FONT"
private const val QUOTE_FONT = "QUOTE_FONT"

private const val SMALL_CAPS_APPS = "SMALL_CAPS_APPS"
private const val ALL_CAPS_APPS = "ALL_CAPS_APPS"
private const val EINK_REFRESH_ENABLED = "EINK_REFRESH_ENABLED"
private const val EINK_REFRESH_HOME_BUTTON_ONLY = "EINK_REFRESH_HOME_BUTTON_ONLY"
private const val QUOTE_TEXT = "QUOTE_TEXT"
private const val QUOTE_TEXT_SIZE = "QUOTE_TEXT_SIZE"
private const val SHOW_QUOTE = "SHOW_QUOTE"

private const val SHOW_AM_PM = "SHOW_AM_PM"
private const val SHOW_SECOND_CLOCK = "SHOW_SECOND_CLOCK"
private const val CLOCK_MODE = "CLOCK_MODE"
private const val CLOCK_STYLE = "CLOCK_STYLE"
private const val SECOND_CLOCK_OFFSET_HOURS = "SECOND_CLOCK_OFFSET_HOURS"

// App Drawer specific preferences
private const val APP_DRAWER_APPS_PER_PAGE_CACHE = "APP_DRAWER_APPS_PER_PAGE_CACHE"
private const val APP_DRAWER_CONTAINER_HEIGHT_CACHE = "APP_DRAWER_CONTAINER_HEIGHT_CACHE"
private const val APP_DRAWER_SEARCH_CONTACTS = "APP_DRAWER_SEARCH_CONTACTS"
private const val APP_DRAWER_SEARCH_CONTACT_ACCOUNTS = "APP_DRAWER_SEARCH_CONTACT_ACCOUNTS"
private const val APP_DRAWER_SEARCH_WEB = "APP_DRAWER_SEARCH_WEB"
private const val APP_DRAWER_SEARCH_SETTINGS = "APP_DRAWER_SEARCH_SETTINGS"
private const val APP_DRAWER_SEARCH_MUSIC = "APP_DRAWER_SEARCH_MUSIC"
private const val APP_DRAWER_SEARCH_FILES = "APP_DRAWER_SEARCH_FILES"
private const val INITIAL_LAUNCH_COMPLETED = "INITIAL_LAUNCH_COMPLETED"
private const val HOME_APPS_Y_OFFSET = "HOME_APPS_Y_OFFSET"
private const val HIDE_HOME_APPS = "HIDE_HOME_APPS"
private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
private const val TEXT_ISLANDS = "TEXT_ISLANDS"
private const val TEXT_ISLANDS_INVERTED = "TEXT_ISLANDS_INVERTED"
private const val TEXT_ISLANDS_SHAPE = "TEXT_ISLANDS_SHAPE"
private const val SHOW_ICONS = "SHOW_ICONS"
private const val DRAWER_SHOW_ICONS = "DRAWER_SHOW_ICONS"
private const val ICON_SOURCE_MODE = "ICON_SOURCE_MODE"
private const val ICON_SHAPE = "ICON_SHAPE"
private const val ICON_TINT_CONTRAST = "ICON_TINT_CONTRAST"
private const val SELECTED_ICON_PACK_PACKAGE = "SELECTED_ICON_PACK_PACKAGE"
private const val INKOS_WALLPAPER_PATH = "INKOS_WALLPAPER_PATH"
private const val INKOS_WALLPAPER_RESOURCE_ID = "INKOS_WALLPAPER_RESOURCE_ID"
private const val PINNED_SHORTCUTS = "PINNED_SHORTCUTS"

// Recents screen preferences
private const val RECENTS_DEFAULT_VIEW  = "RECENTS_DEFAULT_VIEW"
private const val RECENTS_USAGE_FILTER  = "RECENTS_USAGE_FILTER"
private const val RECENTS_USAGE_UNIT    = "RECENTS_USAGE_UNIT"
private const val RECENTS_UNIT_COST         = "RECENTS_UNIT_COST"
private const val RECENTS_UNIT_CURRENCY     = "RECENTS_UNIT_CURRENCY"
private const val RECENTS_UNIT_COFFEE_PRICE = "RECENTS_UNIT_COFFEE_PRICE"
private const val RECENTS_UNIT_EMOJI        = "RECENTS_UNIT_EMOJI"

class Prefs(val context: Context) {
    /** UI scale override. 0 = auto-detect from screen width. See UiScaleMode enum. */
    var uiScaleMode: Int
        get() = prefs.getInt("ui_scale_mode", 0)
        set(value) = prefs.edit { putInt("ui_scale_mode", value) }

    /** Read a dimen resource as a raw sp value (strips density conversion). */
    private fun dimenSp(resId: Int): Int =
        (context.resources.getDimension(resId) / context.resources.displayMetrics.scaledDensity).toInt()

    /** Read a dimen resource as a raw dp value (strips density conversion). */
    private fun dimenDp(resId: Int): Int =
        (context.resources.getDimension(resId) / context.resources.displayMetrics.density).toInt()

    private val BRIGHTNESS_LEVEL = "BRIGHTNESS_LEVEL"
    private val LAST_BRIGHTNESS_LEVEL = "LAST_BRIGHTNESS_LEVEL"
    var brightnessLevel: Int
        get() = prefs.getInt(BRIGHTNESS_LEVEL, 128) // Default to mid brightness
        set(value) = prefs.edit { putInt(BRIGHTNESS_LEVEL, value.coerceIn(0, 255)) }
    var lastBrightnessLevel: Int
        get() = prefs.getInt(LAST_BRIGHTNESS_LEVEL, 128) // Default to mid brightness
        set(value) = prefs.edit { putInt(LAST_BRIGHTNESS_LEVEL, value.coerceIn(1, 255)) }
    var appQuoteWidget: AppListItem
        get() = loadApp("QUOTE_WIDGET")
        set(appModel) = storeApp("QUOTE_WIDGET", appModel)
    private val EINK_REFRESH_DELAY = "EINK_REFRESH_DELAY"
    var einkRefreshDelay: Int
        get() = prefs.getInt(
            EINK_REFRESH_DELAY,
            Constants.DEFAULT_EINK_REFRESH_DELAY
        )
        set(value) = prefs.edit { putInt(EINK_REFRESH_DELAY, value) }
    var appClickDate: AppListItem
        get() = loadApp("CLICK_DATE")
        set(appModel) = storeApp("CLICK_DATE", appModel)
    private val CLICK_DATE_ACTION = "CLICK_DATE_ACTION"
    var clickDateAction: Constants.Action
        get() = try {
            Constants.Action.valueOf(
                prefs.getString(CLICK_DATE_ACTION, Constants.Action.OpenApp.name)
                    ?: Constants.Action.Disabled.name
            )
        } catch (_: Exception) {
            Constants.Action.Disabled
        }
        set(value) = prefs.edit { putString(CLICK_DATE_ACTION, value.name) }
    var dateFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString("date_font", Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString("date_font", value.name) }

    var dateSize: Int
        get() = prefs.getInt(Constants.PrefKeys.DATE_SIZE_TEXT, dimenSp(R.dimen.default_date_size))
        set(value) = prefs.edit { putInt(Constants.PrefKeys.DATE_SIZE_TEXT, value) }
    var dateFormatStyle: Int
        get() = prefs.getInt("DATE_FORMAT_STYLE", 0)
        set(value) = prefs.edit { putInt("DATE_FORMAT_STYLE", value.coerceIn(0, 4)) }

    var showDate: Boolean
        get() = prefs.getBoolean("SHOW_DATE", true)
        set(value) = prefs.edit { putBoolean("SHOW_DATE", value) }

    var showDateBatteryCombo: Boolean
        get() = prefs.getBoolean("SHOW_DATE_BATTERY_COMBO", true)
        set(value) = prefs.edit { putBoolean("SHOW_DATE_BATTERY_COMBO", value) }

    var showNotificationCount: Boolean
        get() = prefs.getBoolean("SHOW_NOTIFICATION_COUNT", true)
        set(value) = prefs.edit { putBoolean("SHOW_NOTIFICATION_COUNT", value) }

    var notificationCountSource: Int
        get() = prefs.getInt("NOTIFICATION_COUNT_SOURCE", 0)
        set(value) = prefs.edit { putInt("NOTIFICATION_COUNT_SOURCE", value) }

    var showQuote: Boolean
        get() = bottomWidgetType == Constants.BottomWidgetType.Quote.value
        set(value) {
            if (value) bottomWidgetType = Constants.BottomWidgetType.Quote.value
            else if (bottomWidgetType == Constants.BottomWidgetType.Quote.value) bottomWidgetType = Constants.BottomWidgetType.Disabled.value
        }

    var quoteText: String
        get() = prefs.getString(QUOTE_TEXT, "Stay inspired") ?: "Stay inspired"
        set(value) = prefs.edit { putString(QUOTE_TEXT, value) }

    var quoteSize: Int
        get() = prefs.getInt(QUOTE_TEXT_SIZE, dimenSp(R.dimen.default_quote_size))
        set(value) = prefs.edit { putInt(QUOTE_TEXT_SIZE, value) }

    var clockMode: Int
        get() = prefs.getInt(CLOCK_MODE, 0)
        set(value) = prefs.edit { putInt(CLOCK_MODE, value.coerceIn(0, 3)) }

    var clockStyle: Int
        get() = prefs.getInt(CLOCK_STYLE, 0)
        set(value) = prefs.edit { putInt(CLOCK_STYLE, value.coerceIn(0, 10)) }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    private val _preferenceChangeFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val preferenceChangeFlow = _preferenceChangeFlow.asSharedFlow()

    private val sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        try {
            when (key) {
                APP_THEME -> {
                    _appThemeFlow.value = appTheme
                    _backgroundColorFlow.value = backgroundColor
                    _textColorFlow.value = textColor
                }
                BACKGROUND_COLOR, TEXT_COLOR, DARK_BACKGROUND_COLOR, DARK_TEXT_COLOR -> {
                    _backgroundColorFlow.value = backgroundColor
                    _textColorFlow.value = textColor
                }
            }
            if (key != null) {
                try { _preferenceChangeFlow.tryEmit(key) } catch (e: Exception) { Log.w("Prefs", "preferenceChangeFlow emit failed for key=$key", e) }
                if (com.github.gezimos.inkos.widget.QuoteWidget.isQuotePrefKey(key)) {
                    com.github.gezimos.inkos.widget.QuoteWidget.requestUpdate(context)
                }
            }
        } catch (e: Exception) {
            Log.w("Prefs", "sharedPrefsListener failed", e)
        }
    }

    init {
        try { prefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener) } catch (e: Exception) { Log.w("Prefs", "registerOnSharedPreferenceChangeListener failed", e) }
        if (!prefs.contains(BACKGROUND_OPACITY)) {
            prefs.edit { putInt(BACKGROUND_OPACITY, 255) }
        }
        if (!prefs.contains(DARK_TEXT_COLOR)) {
            prefs.edit { putInt(DARK_TEXT_COLOR, android.graphics.Color.WHITE) }
        }
        if (!prefs.contains(DARK_BACKGROUND_COLOR)) {
            prefs.edit { putInt(DARK_BACKGROUND_COLOR, android.graphics.Color.BLACK) }
        }
    }

    var initialLaunchCompleted: Boolean
        get() = prefs.getBoolean(INITIAL_LAUNCH_COMPLETED, false)
        set(value) = prefs.edit { putBoolean(INITIAL_LAUNCH_COMPLETED, value) }

    val sharedPrefs: SharedPreferences
        get() = prefs

    private val CUSTOM_FONT_PATH_MAP_KEY = "custom_font_path_map"
    private val gson = Gson()

    private val floatPrefKeys = setOf(SHORT_SWIPE_THRESHOLD_RATIO, LONG_SWIPE_THRESHOLD_RATIO)

    private val deprecatedImportKeys = setOf(
        "SHOW_BATTERY",
        "APP_COLOR","CLOCK_COLOR","BATTERY_COLOR","DATE_COLOR","QUOTE_COLOR","AUDIO_WIDGET_COLOR",
        "HOME_BACKGROUND_IMAGE_URI","HOME_BACKGROUND_IMAGE_OPACITY",
        // battery-related legacy keys from v0.2
        "BATTERY_SIZE_TEXT","battery_font",
        "WALLPAPER_ENABLED","show_background",
        "use_vibration_for_paging",
        APP_DRAWER_APPS_PER_PAGE_CACHE,
        APP_DRAWER_CONTAINER_HEIGHT_CACHE,
        "APP_DRAWER_CACHED_GAP",
        "APP_DRAWER_CACHED_SIZE",
        Constants.PrefKeys.EVENTS_CALENDAR_ID,
        Constants.PrefKeys.EVENTS_CALENDAR_NAME
    )

    val themeExportKeys: Set<String> = setOf(
        APP_THEME, BACKGROUND_COLOR, BACKGROUND_OPACITY, TEXT_COLOR,
        DARK_BACKGROUND_COLOR, DARK_TEXT_COLOR,
        "custom_theme_light_text_color", "custom_theme_light_background_color",
        "custom_theme_dark_text_color", "custom_theme_dark_background_color",
        STATUS_BAR, NAVIGATION_BAR, TEXT_SIZE_SETTINGS,
        LAUNCHER_FONT, APPS_FONT, CLOCK_FONT, STATUS_FONT, NOTIFICATION_FONT, QUOTE_FONT,
        "date_font", "letters_font", "letters_title_font", "notifications_font",
        "universal_font", "universal_font_enabled",
        APP_SIZE_TEXT, CLOCK_SIZE_TEXT, QUOTE_TEXT_SIZE, TEXT_PADDING_SIZE,
        Constants.PrefKeys.DATE_SIZE_TEXT, "date_text_size",
        SHOW_ICONS, DRAWER_SHOW_ICONS, "ICON_SOURCE_MODE", ICON_SHAPE,
        HOME_APPS_NUM, HOME_PAGES_NUM, HOME_ALIGNMENT,
        "HOME_CLOCK_ALIGNMENT", "HOME_DATE_ALIGNMENT", "HOME_QUOTE_ALIGNMENT",
        HOME_APPS_Y_OFFSET, HIDE_HOME_APPS,
        "APP_DRAWER_ALIGNMENT", "APP_DRAWER_SIZE", "APP_DRAWER_GAP",
        Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED,
        "APP_DRAWER_AUTO_LAUNCH", "APP_DRAWER_AUTO_SHOW_KEYBOARD",
        "APP_DRAWER_AZ_FILTER", "APP_DRAWER_PAGER",
        SHOW_CLOCK, CLOCK_MODE, CLOCK_STYLE, SHOW_AM_PM, SHOW_SECOND_CLOCK,
        SECOND_CLOCK_OFFSET_HOURS, "SHOW_DATE_BATTERY_COMBO", "SHOW_NOTIFICATION_COUNT", "NOTIFICATION_COUNT_SOURCE", "DATE_FORMAT_STYLE",
        Constants.PrefKeys.BOTTOM_WIDGET_TYPE, QUOTE_TEXT, "top_widget_margin", "bottom_widget_margin",
        TEXT_ISLANDS, TEXT_ISLANDS_INVERTED, TEXT_ISLANDS_SHAPE,
        "SHOW_DATE", SHOW_QUOTE, SMALL_CAPS_APPS, ALL_CAPS_APPS,
        SHOW_AUDIO_WIDGET_ENABLE,
        INKOS_WALLPAPER_RESOURCE_ID
    )

    fun loadCustomThemeFromMap(map: Map<String, Any?>) {
        prefs.edit {
            (map["custom_theme_light_text_color"] as? Number)?.toInt()?.let { putInt("custom_theme_light_text_color", it) }
            (map["custom_theme_light_background_color"] as? Number)?.toInt()?.let { putInt("custom_theme_light_background_color", it) }
            (map["custom_theme_dark_text_color"] as? Number)?.toInt()?.let { putInt("custom_theme_dark_text_color", it) }
            (map["custom_theme_dark_background_color"] as? Number)?.toInt()?.let { putInt("custom_theme_dark_background_color", it) }
        }
    }

    fun saveThemeToMap(): Map<String, Any?> {
        val all = prefs.all
        return themeExportKeys.mapNotNull { key ->
            val value = all[key] ?: return@mapNotNull null
            key to value
        }.toMap()
    }

    /** Serialize current theme settings to a JSON string for theme-only backup. */
    fun saveThemeToJson(): String {
        val root = mapOf(
            "type" to "inkos_theme",
            "version" to 1,
            "prefs" to saveThemeToMap()
        )
        return com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root)
    }

    /** Import theme settings from a JSON string. Only applies visual/layout prefs, nothing private. */
    fun loadThemeFromJson(json: String) {
        @Suppress("UNCHECKED_CAST")
        val root = gson.fromJson(json, Map::class.java) as? Map<String, Any?>
            ?: throw IllegalArgumentException("Invalid theme file")
        val prefsMap = root["prefs"] as? Map<String, Any?> ?: emptyMap()
        val filtered = prefsMap.filterKeys { it in themeExportKeys }
        loadThemeFromMap(filtered, useCommit = true)
        loadCustomThemeFromMap(filtered)
        appTheme = appTheme
        val resId = inkosWallpaperResourceId
        if (resId != 0) {
            try {
                val wu = com.github.gezimos.inkos.helper.WallpaperUtility(context)
                val bitmap = if (resId < 0) wu.loadGeneratedWallpaper(resId) else wu.loadBitmapFromResource(resId)
                if (bitmap != null) {
                    val fitted = wu.createFittedBitmap(bitmap, wu.screenWidth, wu.screenHeight)
                    val path = wu.saveBitmapToInternalStorage(fitted, "inkos_wallpaper.png")
                    inkosWallpaperPath = path
                    if (fitted != bitmap) fitted.recycle()
                    bitmap.recycle()
                }
            } catch (_: Exception) {}
        }
        ensureWrittenToDisk()
        triggerForceRefreshHome()
    }

    fun loadThemeFromMap(map: Map<String, Any?>, useCommit: Boolean = false) {
        prefs.edit(commit = useCommit) {
            val pm = context.packageManager
            for ((key, value) in map) {
                if (key in deprecatedImportKeys) continue
                if (key == APP_DRAWER_APPS_PER_PAGE_CACHE || key == APP_DRAWER_CONTAINER_HEIGHT_CACHE ||
                    key == Constants.PrefKeys.EVENTS_CALENDAR_ID || key == Constants.PrefKeys.EVENTS_CALENDAR_NAME
                ) continue
                if (key == "allowed_notification_apps" || key == "allowed_badge_notification_apps" ||
                    key == HIDDEN_APPS || key == LOCKED_APPS
                ) {
                    val set = when (value) {
                        is Collection<*> -> value.filterIsInstance<String>().toMutableSet()
                        is String -> mutableSetOf(value)
                        else -> mutableSetOf<String>()
                    }
                    val filteredSet = set.filter { pkgUser ->
                        val pkg = pkgUser.split("|")[0]
                        if (pkg.startsWith("com.inkos.internal.")) true
                        else try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
                    }.toMutableSet()
                    putStringSet(key, filteredSet)
                    continue
                }
                if (key == "custom_font_paths") {
                    val set = when (value) {
                        is Collection<*> -> value.filterIsInstance<String>().toMutableSet()
                        is String -> mutableSetOf(value)
                        else -> mutableSetOf<String>()
                    }
                    putStringSet(key, set)
                    continue
                }
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Number -> {
                        if (key in floatPrefKeys) putFloat(key, value.toFloat())
                        else if (value.toDouble() == value.toInt().toDouble()) putInt(key, value.toInt())
                        else putFloat(key, value.toFloat())
                    }
                    is Collection<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
                    else -> {}
                }
            }
        }
    }

    fun ensureFloatPrefsAreFloat() {
        try {
            var changed = false
            val editor = prefs.edit()
            val all = prefs.all
            for (key in floatPrefKeys) {
                val v = all[key]
                if (v == null) continue
                if (v is Float) continue
                when (v) {
                    is Number -> {
                        editor.putFloat(key, v.toFloat())
                        changed = true
                    }
                    is String -> {
                        val f = v.toFloatOrNull()
                        if (f != null) {
                            editor.putFloat(key, f)
                            changed = true
                        }
                    }
                    else -> {}
                }
            }
            if (changed) editor.apply()
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun getFloatCompat(key: String, default: Float): Float {
        return try {
            prefs.getFloat(key, default)
        } catch (_: ClassCastException) {
            try {
                val raw = prefs.all[key]
                when (raw) {
                    is Number -> raw.toFloat()
                    is String -> raw.toFloatOrNull() ?: default
                    else -> default
                }
            } catch (_: Exception) {
                default
            }
        } catch (_: Exception) {
            default
        }
    }

    var customFontPathMap: MutableMap<String, String>
        get() {
            val json = prefs.getString(CUSTOM_FONT_PATH_MAP_KEY, "{}") ?: "{}"
            return gson.fromJson(json, object : TypeToken<MutableMap<String, String>>() {}.type)
                ?: mutableMapOf()
        }
        set(value) {
            prefs.edit { putString(CUSTOM_FONT_PATH_MAP_KEY, gson.toJson(value)) }
        }

    fun setCustomFontPath(context: String, path: String) {
        val map = customFontPathMap
        map[context] = path
        customFontPathMap = map
    }

    fun getCustomFontPath(context: String): String? {
        return customFontPathMap[context]
    }

    // Remove a custom font path from the context map
    fun removeCustomFontPath(context: String) {
        val map = customFontPathMap
        map.remove(context)
        customFontPathMap = map
    }

    // Remove a custom font path from the set of paths
    fun removeCustomFontPathByPath(path: String) {
        val set = customFontPaths
        set.remove(path)
        customFontPaths = set
    }

    var universalFontEnabled: Boolean
        get() = prefs.getBoolean("universal_font_enabled", true)
        set(value) {
            prefs.edit {
                putBoolean("universal_font_enabled", value)
            }
            if (value) {
                val font = universalFont
                appsFont = font
                clockFont = font
                statusFont = font
                labelnotificationsFont = font

                fontFamily = font
                lettersFont = font
                lettersTitleFont = font
                notificationsFont = font
                dateFont = font
                quoteFont = font
            }
        }

    var universalFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString("universal_font", Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) {
            prefs.edit {
                putString("universal_font", value.name)
            }
            fontFamily = value
            if (universalFontEnabled) {
                appsFont = value
                clockFont = value
                statusFont = value
                labelnotificationsFont = value

                fontFamily = value
                lettersFont = value
                lettersTitleFont = value
                notificationsFont = value
                dateFont = value
                quoteFont = value
            }
        }

    var font: String
        get() = prefs.getString("FONT", "Roboto") ?: "Roboto"
        set(value) = prefs.edit { putString("FONT", value) }

    var fontFamily: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(LAUNCHER_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(LAUNCHER_FONT, value.name) }

    var quoteFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(QUOTE_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(QUOTE_FONT, value.name) }

    var customFontPath: String?
        get() = prefs.getString("custom_font_path", null)
        set(value) = prefs.edit { putString("custom_font_path", value) }

    // Store a set of custom font paths
    var customFontPaths: MutableSet<String>
        get() = prefs.getStringSet("custom_font_paths", mutableSetOf()) ?: mutableSetOf()
        set(value) = prefs.edit { putStringSet("custom_font_paths", value) }

    // Add a new custom font path
    fun addCustomFontPath(path: String) {
        val set = customFontPaths
        set.add(path)
        customFontPaths = set
    }

    // ------------------
    // ------------------
    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(NOTIFICATIONS_ENABLED, value) }

    var notificationsFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(NOTIFICATIONS_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(NOTIFICATIONS_FONT, value.name) }

    var notificationsTextSize: Int
        get() = prefs.getInt(NOTIFICATIONS_TEXT_SIZE, dimenSp(R.dimen.default_notifications_text_size))
        set(value) = prefs.edit { putInt(NOTIFICATIONS_TEXT_SIZE, value) }

    var notificationsPerPage: Int
        get() = prefs.getInt(NOTIFICATIONS_PER_PAGE, 3).coerceIn(1, 5)
        set(value) = prefs.edit { putInt(NOTIFICATIONS_PER_PAGE, value.coerceIn(1, 5)) }

    var edgeSwipeBackEnabled: Boolean
        get() = prefs.getBoolean(EDGE_SWIPE_BACK_ENABLED, true)
        set(value) {
            prefs.edit { putBoolean(EDGE_SWIPE_BACK_ENABLED, value) }
            try { _edgeSwipeBackEnabledFlow.value = value } catch (e: Exception) { Log.w("Prefs", "edgeSwipeBackEnabledFlow update failed", e) }
        }

    var enableBottomNav: Boolean
        get() = prefs.getBoolean(ENABLE_BOTTOM_NAV, true)
        set(value) = prefs.edit { putBoolean(ENABLE_BOTTOM_NAV, value) }

    var showNotificationSenderFullName: Boolean
        get() = prefs.getBoolean("show_notification_sender_full_name", false)
        set(value) = prefs.edit { putBoolean("show_notification_sender_full_name", value) }

    var einkRefreshEnabled: Boolean
        get() = prefs.getBoolean(EINK_REFRESH_ENABLED, false)
        set(value) = prefs.edit { putBoolean(EINK_REFRESH_ENABLED, value) }

    var einkRefreshHomeButtonOnly: Boolean
        get() = prefs.getBoolean(EINK_REFRESH_HOME_BUTTON_ONLY, false)
        set(value) = prefs.edit { putBoolean(EINK_REFRESH_HOME_BUTTON_ONLY, value) }

    var smallCapsApps: Boolean
        get() = prefs.getBoolean(SMALL_CAPS_APPS, false)
        set(value) = prefs.edit { putBoolean(SMALL_CAPS_APPS, value) }

    var allCapsApps: Boolean
        get() = prefs.getBoolean(ALL_CAPS_APPS, false)
        set(value) = prefs.edit { putBoolean(ALL_CAPS_APPS, value) }

    /** When true, app drawer hides apps that are already configured as home apps. */
    var hideHomeApps: Boolean
        get() = prefs.getBoolean(HIDE_HOME_APPS, false)
        set(value) = prefs.edit { putBoolean(HIDE_HOME_APPS, value) }

    private val _forceRefreshHomeCounter = MutableStateFlow(0)
    val forceRefreshHomeFlow: StateFlow<Int> get() = _forceRefreshHomeCounter

    private val _appThemeFlow = MutableStateFlow(appTheme)
    val appThemeFlow: StateFlow<Constants.Theme> get() = _appThemeFlow

    private val _backgroundColorFlow = MutableStateFlow(backgroundColor)
    val backgroundColorFlow: StateFlow<Int> get() = _backgroundColorFlow

    private val _textColorFlow = MutableStateFlow(textColor)
    val textColorFlow: StateFlow<Int> get() = _textColorFlow

    // Edge-swipe back toggle flow
    private val _edgeSwipeBackEnabledFlow = MutableStateFlow(edgeSwipeBackEnabled)
    val edgeSwipeBackEnabledFlow: StateFlow<Boolean> get() = _edgeSwipeBackEnabledFlow

    fun triggerForceRefreshHome() {
        try {
            _forceRefreshHomeCounter.value = _forceRefreshHomeCounter.value + 1
        } catch (_: Exception) {
            // ignore
        }
    }

    var pushNotificationsEnabled: Boolean
        get() = prefs.getBoolean("push_notifications_enabled", false)
        set(value) = prefs.edit { putBoolean("push_notifications_enabled", value) }

    private val NOTIFICATION_SWITCHES_STATE_KEY = "notification_switches_state"

    fun saveToString(): String {
        val all: HashMap<String, Any?> = HashMap(prefs.all)
        all.remove(Constants.PrefKeys.EVENTS_CALENDAR_ID)
        all.remove(Constants.PrefKeys.EVENTS_CALENDAR_NAME)
        return Gson().toJson(all)
    }

    fun loadFromString(json: String) {
        prefs.edit {
            val all: HashMap<String, Any?> =
                Gson().fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type)
            val pm = context.packageManager
            for ((key, value) in all) {
                if (key in deprecatedImportKeys) continue
                if (key == "allowed_notification_apps" || key == "allowed_badge_notification_apps" ||
                    key == HIDDEN_APPS || key == LOCKED_APPS
                ) {
                    val set = when (value) {
                        is Collection<*> -> value.filterIsInstance<String>().toMutableSet()
                        is String -> mutableSetOf(value)
                        else -> mutableSetOf<String>()
                    }
                    val filteredSet = set.filter { pkgUser ->
                        val pkg = pkgUser.split("|")[0]
                        if (pkg.startsWith("com.inkos.internal.")) {
                            true
                        } else {
                            try {
                                pm.getPackageInfo(pkg, 0)
                                true
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }.toMutableSet()
                    putStringSet(key, filteredSet)
                    continue
                }
                if (key == "custom_font_paths") {
                    val set = when (value) {
                        is Collection<*> -> value.filterIsInstance<String>().toMutableSet()
                        is String -> mutableSetOf(value)
                        else -> mutableSetOf<String>()
                    }
                    putStringSet(key, set)
                    continue
                }
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Number -> {
                        if (key in floatPrefKeys) {
                            putFloat(key, value.toFloat())
                        } else {
                            if (value.toDouble() == value.toInt().toDouble()) {
                                putInt(key, value.toInt())
                            } else {
                                putFloat(key, value.toFloat())
                            }
                        }
                    }

                    is Collection<*> -> {
                        val set = value.filterIsInstance<String>().toMutableSet()
                        putStringSet(key, set)
                    }

                    else -> {
                        Log.d("backup error", "$value")
                    }
                }
            }
        }
    }

    fun getAppAlias(key: String): String {
        return prefs.getString(key, "").toString()
    }

    fun setAppAlias(packageName: String, appAlias: String) {
        prefs.edit { putString("app_alias_$packageName", appAlias) }
    }

    fun removeAppAlias(packageName: String) {
        prefs.edit { remove("app_alias_$packageName") }
    }

    var appVersion: Int
        get() = prefs.getInt(APP_VERSION, -1)
        set(value) = prefs.edit { putInt(APP_VERSION, value) }

    var firstOpen: Boolean
        get() = prefs.getBoolean(FIRST_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_OPEN, value) }

    fun commitFirstOpen(value: Boolean) {
        prefs.edit().putBoolean(FIRST_OPEN, value).commit()
    }

    var guideShown: Boolean
        get() = prefs.getBoolean("guide_shown", false)
        set(value) = prefs.edit { putBoolean("guide_shown", value) }

    fun commitGuideShown(value: Boolean) {
        prefs.edit().putBoolean("guide_shown", value).commit()
    }

    var themePickerShown: Boolean
        get() = prefs.getBoolean("theme_picker_shown", false)
        set(value) = prefs.edit { putBoolean("theme_picker_shown", value) }

    fun commitThemePickerShown(value: Boolean) {
        prefs.edit().putBoolean("theme_picker_shown", value).commit()
    }

    // One-time tooltip tracking
    var tooltipQuickMenuShown: Boolean
        get() = prefs.getBoolean("tooltip_quick_menu_shown", false)
        set(value) = prefs.edit { putBoolean("tooltip_quick_menu_shown", value) }

    var tooltipRecentsShown: Boolean
        get() = prefs.getBoolean("tooltip_recents_shown", false)
        set(value) = prefs.edit { putBoolean("tooltip_recents_shown", value) }

    var tooltipLettersShown: Boolean
        get() = prefs.getBoolean("tooltip_letters_shown", false)
        set(value) = prefs.edit { putBoolean("tooltip_letters_shown", value) }

    var tooltipEditModeShown: Boolean
        get() = prefs.getBoolean("tooltip_edit_mode_shown", false)
        set(value) = prefs.edit { putBoolean("tooltip_edit_mode_shown", value) }

    /** Generic one-time flag accessor for tooltips */
    fun isTooltipShown(key: String): Boolean = prefs.getBoolean(key, false)
    fun markTooltipShown(key: String) = prefs.edit { putBoolean(key, true) }

    var einkHelperMode: Int
        get() = prefs.getInt("eink_helper_mode", 0)
        set(value) = prefs.edit { putInt("eink_helper_mode", value) }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit { putBoolean(LOCK_MODE, value) }

    // ------------------
    // Home prefs (HomeFragment)
    // ------------------
    var homePager: Boolean
        get() = prefs.getBoolean(HOME_PAGES_PAGER, true)
        set(value) = prefs.edit { putBoolean(HOME_PAGES_PAGER, value) }


    var homeReset: Boolean
        get() = prefs.getBoolean(HOME_PAGE_RESET, true)
        set(value) = prefs.edit { putBoolean(HOME_PAGE_RESET, value) }

    var homeAppsNum: Int
        get() = prefs.getInt(HOME_APPS_NUM, 12)
        set(value) = prefs.edit { putInt(HOME_APPS_NUM, value) }

    var homePagesNum: Int
        get() = prefs.getInt(HOME_PAGES_NUM, 3)
        set(value) = prefs.edit { putInt(HOME_PAGES_NUM, value) }

    // ------------------
    // ------------------
    var lightTextColor: Int
        get() = prefs.getInt(TEXT_COLOR, android.graphics.Color.BLACK)
        set(value) = prefs.edit { putInt(TEXT_COLOR, value) }
    var lightBackgroundColor: Int
        get() = prefs.getInt(BACKGROUND_COLOR, android.graphics.Color.WHITE)
        set(value) = prefs.edit { putInt(BACKGROUND_COLOR, value) }
    var darkTextColor: Int
        get() = prefs.getInt(DARK_TEXT_COLOR, android.graphics.Color.WHITE)
        set(value) = prefs.edit { putInt(DARK_TEXT_COLOR, value) }
    var darkBackgroundColor: Int
        get() = prefs.getInt(DARK_BACKGROUND_COLOR, android.graphics.Color.BLACK)
        set(value) = prefs.edit { putInt(DARK_BACKGROUND_COLOR, value) }

    /** Saved custom theme – preserved when applying presets. */
    var customThemeLightTextColor: Int
        get() = prefs.getInt("custom_theme_light_text_color", lightTextColor)
        set(value) = prefs.edit { putInt("custom_theme_light_text_color", value) }
    var customThemeLightBackgroundColor: Int
        get() = prefs.getInt("custom_theme_light_background_color", lightBackgroundColor)
        set(value) = prefs.edit { putInt("custom_theme_light_background_color", value) }
    var customThemeDarkTextColor: Int
        get() = prefs.getInt("custom_theme_dark_text_color", darkTextColor)
        set(value) = prefs.edit { putInt("custom_theme_dark_text_color", value) }
    var customThemeDarkBackgroundColor: Int
        get() = prefs.getInt("custom_theme_dark_background_color", darkBackgroundColor)
        set(value) = prefs.edit { putInt("custom_theme_dark_background_color", value) }

    var backgroundColor: Int
        get() {
            val isDark = getResolvedTheme() == Constants.Theme.Dark
            return if (isDark) darkBackgroundColor else lightBackgroundColor
        }
        set(value) {
            val isDark = getResolvedTheme() == Constants.Theme.Dark
            if (isDark) darkBackgroundColor = value else lightBackgroundColor = value
            try { _backgroundColorFlow.value = value } catch (e: Exception) { Log.w("Prefs", "backgroundColorFlow update failed", e) }
        }

    var backgroundOpacity: Int
        get() = prefs.getInt(BACKGROUND_OPACITY, 255)
        set(value) = prefs.edit { putInt(BACKGROUND_OPACITY, value) }

    var textColor: Int
        get() {
            val isDark = getResolvedTheme() == Constants.Theme.Dark
            return if (isDark) darkTextColor else lightTextColor
        }
        set(value) {
            val isDark = getResolvedTheme() == Constants.Theme.Dark
            if (isDark) darkTextColor = value else lightTextColor = value
            try { _textColorFlow.value = value } catch (e: Exception) { Log.w("Prefs", "textColorFlow update failed", e) }
        }

    var appliedThemeName: String?
        get() = prefs.getString("applied_theme_name", null)
        set(value) = prefs.edit { if (value != null) putString("applied_theme_name", value) else remove("applied_theme_name") }

    var extendHomeAppsArea: Boolean
        get() = prefs.getBoolean(HOME_CLICK_AREA, false)
        set(value) = prefs.edit { putBoolean(HOME_CLICK_AREA, value) }

    var showStatusBar: Boolean
        get() = prefs.getBoolean(STATUS_BAR, false)
        set(value) = prefs.edit { putBoolean(STATUS_BAR, value) }

    var showNavigationBar: Boolean
        get() = prefs.getBoolean(NAVIGATION_BAR, false)
        set(value) = prefs.edit { putBoolean(NAVIGATION_BAR, value) }

    var showClock: Boolean
        get() = prefs.getBoolean(SHOW_CLOCK, true)
        set(value) = prefs.edit { putBoolean(SHOW_CLOCK, value) }

    var showAmPm: Boolean
        get() = prefs.getBoolean(SHOW_AM_PM, true)
        set(value) = prefs.edit { putBoolean(SHOW_AM_PM, value) }

    var showSecondClock: Boolean
        get() = prefs.getBoolean(SHOW_SECOND_CLOCK, false)
        set(value) = prefs.edit { putBoolean(SHOW_SECOND_CLOCK, value) }

    var secondClockOffsetHours: Int
        get() = prefs.getInt(SECOND_CLOCK_OFFSET_HOURS, 0)
        set(value) = prefs.edit { putInt(SECOND_CLOCK_OFFSET_HOURS, value.coerceIn(-12, 14)) }

    var showAudioWidgetEnabled: Boolean
        get() = prefs.getBoolean(SHOW_AUDIO_WIDGET_ENABLE, false)
        set(value) = prefs.edit { putBoolean(SHOW_AUDIO_WIDGET_ENABLE, value) }

    var bottomWidgetType: String
        get() {
            return prefs.getString(Constants.PrefKeys.BOTTOM_WIDGET_TYPE, null)
                ?: Constants.BottomWidgetType.Quote.value
        }
        set(value) = prefs.edit { putString(Constants.PrefKeys.BOTTOM_WIDGET_TYPE, value) }

    var showAndroidWidget: Boolean
        get() = bottomWidgetType == Constants.BottomWidgetType.AndroidWidget.value
        set(value) {
            if (value) bottomWidgetType = Constants.BottomWidgetType.AndroidWidget.value
            else if (bottomWidgetType == Constants.BottomWidgetType.AndroidWidget.value) bottomWidgetType = Constants.BottomWidgetType.Quote.value
        }

    var androidWidgetId: Int
        get() = prefs.getInt("ANDROID_WIDGET_ID", -1)
        set(value) = prefs.edit { putInt("ANDROID_WIDGET_ID", value) }

    var androidWidgetHeight: Int
        get() = prefs.getInt("ANDROID_WIDGET_HEIGHT", Constants.DEFAULT_ANDROID_WIDGET_HEIGHT)
        set(value) = prefs.edit { putInt("ANDROID_WIDGET_HEIGHT", value.coerceIn(Constants.MIN_ANDROID_WIDGET_HEIGHT, Constants.MAX_ANDROID_WIDGET_HEIGHT)) }

    var androidWidgetMarginStart: Int
        get() = prefs.getInt("ANDROID_WIDGET_MARGIN_START", Constants.DEFAULT_ANDROID_WIDGET_MARGIN)
        set(value) = prefs.edit { putInt("ANDROID_WIDGET_MARGIN_START", value.coerceIn(0, Constants.MAX_ANDROID_WIDGET_MARGIN)) }

    var eventsCalendarId: Long
        get() {
            val raw = try {
                prefs.getLong(Constants.PrefKeys.EVENTS_CALENDAR_ID, com.github.gezimos.inkos.helper.CalendarEventsHelper.ALL_CALENDARS_ID)
            } catch (_: ClassCastException) {
                (prefs.all[Constants.PrefKeys.EVENTS_CALENDAR_ID] as? Number)?.toLong() ?: com.github.gezimos.inkos.helper.CalendarEventsHelper.ALL_CALENDARS_ID
            }
            return if (raw == -1L) com.github.gezimos.inkos.helper.CalendarEventsHelper.ALL_CALENDARS_ID else raw
        }
        set(value) = prefs.edit { putLong(Constants.PrefKeys.EVENTS_CALENDAR_ID, value) }

    var eventsFilter: Int
        get() = prefs.getInt(Constants.PrefKeys.EVENTS_FILTER, 3)
        set(value) = prefs.edit { putInt(Constants.PrefKeys.EVENTS_FILTER, value.coerceIn(0, 3)) }

    var eventsIndex: Int
        get() = prefs.getInt(Constants.PrefKeys.EVENTS_INDEX, 0)
        set(value) = prefs.edit { putInt(Constants.PrefKeys.EVENTS_INDEX, value.coerceAtLeast(0)) }

    var eventsCalendarName: String
        get() = prefs.getString(Constants.PrefKeys.EVENTS_CALENDAR_NAME, "") ?: ""
        set(value) = prefs.edit { putString(Constants.PrefKeys.EVENTS_CALENDAR_NAME, value) }

    var eventsHideControls: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.EVENTS_HIDE_CONTROLS, false)
        set(value) = prefs.edit { putBoolean(Constants.PrefKeys.EVENTS_HIDE_CONTROLS, value) }

    var shortcutLeftIcon: Constants.ShortcutIcon
        get() = try {
            Constants.ShortcutIcon.valueOf(
                prefs.getString(Constants.PrefKeys.SHORTCUT_LEFT_ICON, Constants.ShortcutIcon.Search.name)
                    ?: Constants.ShortcutIcon.Search.name
            )
        } catch (_: Exception) { Constants.ShortcutIcon.Search }
        set(value) = prefs.edit { putString(Constants.PrefKeys.SHORTCUT_LEFT_ICON, value.name) }

    var shortcutLeftAction: Constants.Action
        get() = try {
            Constants.Action.valueOf(
                prefs.getString(Constants.PrefKeys.SHORTCUT_LEFT_ACTION, Constants.Action.Search.name)
                    ?: Constants.Action.Search.name
            )
        } catch (_: Exception) { Constants.Action.Search }
        set(value) = prefs.edit { putString(Constants.PrefKeys.SHORTCUT_LEFT_ACTION, value.name) }

    var appShortcutLeft: AppListItem
        get() = loadApp("SHORTCUT_LEFT")
        set(appModel) = storeApp("SHORTCUT_LEFT", appModel)

    var shortcutRightIcon: Constants.ShortcutIcon
        get() = try {
            Constants.ShortcutIcon.valueOf(
                prefs.getString(Constants.PrefKeys.SHORTCUT_RIGHT_ICON, Constants.ShortcutIcon.Camera.name)
                    ?: Constants.ShortcutIcon.Camera.name
            )
        } catch (_: Exception) { Constants.ShortcutIcon.Phone }
        set(value) = prefs.edit { putString(Constants.PrefKeys.SHORTCUT_RIGHT_ICON, value.name) }

    var shortcutRightAction: Constants.Action
        get() = try {
            Constants.Action.valueOf(
                prefs.getString(Constants.PrefKeys.SHORTCUT_RIGHT_ACTION, Constants.Action.OpenApp.name)
                    ?: Constants.Action.OpenApp.name
            )
        } catch (_: Exception) { Constants.Action.OpenApp }
        set(value) = prefs.edit { putString(Constants.PrefKeys.SHORTCUT_RIGHT_ACTION, value.name) }

    var appShortcutRight: AppListItem
        get() = loadApp("SHORTCUT_RIGHT")
        set(appModel) = storeApp("SHORTCUT_RIGHT", appModel)

    var shortcutPageDots: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.SHORTCUT_PAGE_DOTS, false)
        set(value) = prefs.edit { putBoolean(Constants.PrefKeys.SHORTCUT_PAGE_DOTS, value) }

    var shortcutHideOutline: Boolean
        get() = prefs.getBoolean("shortcut_hide_outline", false)
        set(value) = prefs.edit { putBoolean("shortcut_hide_outline", value) }

    var androidWidgetMarginEnd: Int
        get() = prefs.getInt("ANDROID_WIDGET_MARGIN_END", Constants.DEFAULT_ANDROID_WIDGET_MARGIN)
        set(value) = prefs.edit { putInt("ANDROID_WIDGET_MARGIN_END", value.coerceIn(0, Constants.MAX_ANDROID_WIDGET_MARGIN)) }

    var showNotificationBadge: Boolean
        get() = prefs.getBoolean(SHOW_NOTIFICATION_BADGE, true)
        set(value) = prefs.edit { putBoolean(SHOW_NOTIFICATION_BADGE, value) }

    var notificationIndicatorStyle: Int
        get() = prefs.getInt(NOTIFICATION_INDICATOR_STYLE, 0)
        set(value) = prefs.edit { putInt(NOTIFICATION_INDICATOR_STYLE, value) }

    var showNotificationText: Boolean
        get() = prefs.getBoolean("showNotificationText", true)
        set(value) = prefs.edit { putBoolean("showNotificationText", value) }

    var labelnotificationsTextSize: Int
        get() = prefs.getInt("notificationTextSize", 16)
        set(value) = prefs.edit { putInt("notificationTextSize", value) }

    var showNotificationSenderName: Boolean
        get() = prefs.getBoolean("show_notification_sender_name", true)
        set(value) = prefs.edit { putBoolean("show_notification_sender_name", value) }

    var showNotificationGroupName: Boolean
        get() = prefs.getBoolean("show_notification_group_name", true)
        set(value) = prefs.edit { putBoolean("show_notification_group_name", value) }

    var showNotificationMessage: Boolean
        get() = prefs.getBoolean("show_notification_message", true)
        set(value) = prefs.edit { putBoolean("show_notification_message", value) }

    // Media indicator and media name toggles
    var showMediaIndicator: Boolean
        get() = prefs.getBoolean("show_media_indicator", true)
        set(value) = prefs.edit { putBoolean("show_media_indicator", value) }

    var showMediaName: Boolean
        get() = prefs.getBoolean("show_media_name", true)
        set(value) = prefs.edit { putBoolean("show_media_name", value) }

    var clearConversationOnAppOpen: Boolean
        get() = prefs.getBoolean("clear_conversation_on_app_open", false)
        set(value) = prefs.edit { putBoolean("clear_conversation_on_app_open", value) }

    var homeLocked: Boolean
        get() = prefs.getBoolean(HOME_LOCKED, false)
        set(value) = prefs.edit { putBoolean(HOME_LOCKED, value) }

    var settingsLocked: Boolean
        get() = prefs.getBoolean(SETTINGS_LOCKED, false)
        set(value) = prefs.edit { putBoolean(SETTINGS_LOCKED, value) }

    var selectedAppShortcuts: MutableSet<String>
        get() = prefs.getStringSet(SELECTED_APP_SHORTCUTS, null) ?: mutableSetOf()
        set(value) = prefs.edit { putStringSet(SELECTED_APP_SHORTCUTS, value) }

    fun hasSelectedAppShortcutsBeenSet(): Boolean =
        prefs.getStringSet(SELECTED_APP_SHORTCUTS, null) != null

    // Helper to check if an app shortcut is selected
    fun isAppShortcutSelected(context: Context, key: String): Boolean {
        val selected = prefs.getStringSet(SELECTED_APP_SHORTCUTS, null)
        if (selected == null) {
            // First time - pinned shortcuts are enabled
            val pinnedShortcuts = com.github.gezimos.inkos.helper.PinnedShortcutUtility.getPinnedShortcuts(context)
            if (pinnedShortcuts.any { "${it.packageName}|${it.shortcutId}|${it.userHandle}" == key }) return true
            if (key.startsWith("${context.packageName}|inkos_")) return true
            return false
        }
        return selected.contains(key)
    }

    var swipeLeftAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_LEFT_ACTION,
                        Constants.Action.OpenLettersScreen.name // default: Notifications
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenLettersScreen
            }
        }
        set(value) = prefs.edit { putString(SWIPE_LEFT_ACTION, value.name) }

    var swipeRightAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_RIGHT_ACTION,
                        Constants.Action.OpenRecentsScreen.name // default: Open Recents
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenRecentsScreen
            }
        }
        set(value) = prefs.edit { putString(SWIPE_RIGHT_ACTION, value.name) }

    var swipeUpAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_UP_ACTION,
                        Constants.Action.OpenAppDrawer.name // default: Open App Drawer
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenAppDrawer
            }
        }
        set(value) = prefs.edit { putString(SWIPE_UP_ACTION, value.name) }

    var swipeDownAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_DOWN_ACTION,
                        Constants.Action.OpenSimpleTray.name // default: Open Simple Tray
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.Disabled
            }
        }
        set(value) = prefs.edit { putString(SWIPE_DOWN_ACTION, value.name) }

    var shortSwipeThresholdRatio: Float
        get() = getFloatCompat(SHORT_SWIPE_THRESHOLD_RATIO, Constants.DEFAULT_SHORT_SWIPE_RATIO)
            .coerceIn(Constants.MIN_SHORT_SWIPE_RATIO, Constants.MAX_SHORT_SWIPE_RATIO)
        set(value) = prefs.edit {
            putFloat(
                SHORT_SWIPE_THRESHOLD_RATIO,
                value.coerceIn(Constants.MIN_SHORT_SWIPE_RATIO, Constants.MAX_SHORT_SWIPE_RATIO)
            )
        }

    var longSwipeThresholdRatio: Float
        get() = getFloatCompat(LONG_SWIPE_THRESHOLD_RATIO, Constants.DEFAULT_LONG_SWIPE_RATIO)
            .coerceIn(Constants.MIN_LONG_SWIPE_RATIO, Constants.MAX_LONG_SWIPE_RATIO)
        set(value) = prefs.edit {
            putFloat(
                LONG_SWIPE_THRESHOLD_RATIO,
                value.coerceIn(Constants.MIN_LONG_SWIPE_RATIO, Constants.MAX_LONG_SWIPE_RATIO)
            )
        }

    var clickClockAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        CLICK_CLOCK_ACTION,
                        Constants.Action.OpenApp.name
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.Disabled
            }
        }
        set(value) = prefs.edit { putString(CLICK_CLOCK_ACTION, value.name) }

    var doubleTapAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        DOUBLE_TAP_ACTION,
                        Constants.Action.EinkRefresh.name // default: E-ink refresh
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.EinkRefresh
            }
        }
        set(value) = prefs.edit { putString(DOUBLE_TAP_ACTION, value.name) }

    private val QUOTE_ACTION = "QUOTE_ACTION"
    var quoteAction: Constants.Action
        get() = try {
            Constants.Action.valueOf(
                prefs.getString(QUOTE_ACTION, Constants.Action.Disabled.name)
                    ?: Constants.Action.Disabled.name
            )
        } catch (_: Exception) {
            Constants.Action.Disabled
        }
        set(value) = prefs.edit { putString(QUOTE_ACTION, value.name) }

    var appTheme: Constants.Theme
        get() {
            val stored = prefs.getString(APP_THEME, Constants.Theme.System.name) ?: Constants.Theme.System.name
            return try {
                Constants.Theme.valueOf(stored)
            } catch (_: Exception) {
                Constants.Theme.System
            }
        }
        set(value) {
            prefs.edit { putString(APP_THEME, value.name) }
            try { _appThemeFlow.value = value } catch (e: Exception) { Log.w("Prefs", "appThemeFlow update failed", e) }
            _backgroundColorFlow.value = backgroundColor
            _textColorFlow.value = textColor
        }
    fun getResolvedTheme(): Constants.Theme {
        val userTheme = appTheme
        return if (userTheme == Constants.Theme.System) {
            if (isSystemInDarkMode(context)) Constants.Theme.Dark else Constants.Theme.Light
        } else {
            userTheme
        }
    }
    fun onSystemThemeChanged() {
        try {
            if (appTheme == Constants.Theme.System) {
                _backgroundColorFlow.value = backgroundColor
                _textColorFlow.value = textColor
            }
        } catch (e: Exception) {
            Log.w("Prefs", "onSystemThemeChanged failed", e)
        }
    }


    var appsFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(APPS_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(APPS_FONT, value.name) }

    var clockFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(CLOCK_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(CLOCK_FONT, value.name) }

    var statusFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(STATUS_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(STATUS_FONT, value.name) }

    var labelnotificationsFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(NOTIFICATION_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(NOTIFICATION_FONT, value.name) }



    var lettersFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(LETTERS_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(LETTERS_FONT, value.name) }

    var lettersTextSize: Int
        get() = prefs.getInt(LETTERS_TEXT_SIZE, dimenSp(R.dimen.default_letters_text_size))
        set(value) = prefs.edit { putInt(LETTERS_TEXT_SIZE, value) }

    var lettersTitle: String
        get() = prefs.getString(LETTERS_TITLE, "Letters") ?: "Letters"
        set(value) = prefs.edit { putString(LETTERS_TITLE, value) }

    var lettersTitleFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(LETTERS_TITLE_FONT, Constants.FontFamily.PublicSans.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(LETTERS_TITLE_FONT, value.name) }

    var lettersTitleSize: Int
        get() = prefs.getInt(LETTERS_TITLE_SIZE, dimenSp(R.dimen.default_letters_title_size))
        set(value) = prefs.edit { putInt(LETTERS_TITLE_SIZE, value) }

    // ------------------
    // Apps prefs (AppsFragment, App Drawer)
    // ------------------

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(HIDDEN_APPS, value) }

    var lockedApps: MutableSet<String>
        get() = prefs.getStringSet(LOCKED_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(LOCKED_APPS, value) }
    fun notifyPinnedShortcutsChanged() {
        try { _preferenceChangeFlow.tryEmit(PINNED_SHORTCUTS) } catch (e: Exception) { Log.w("Prefs", "notifyPinnedShortcutsChanged emit failed", e) }
    }

    var newlyInstalledApps: MutableSet<String>
        get() = prefs.getStringSet("NEWLY_INSTALLED_APPS", mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet("NEWLY_INSTALLED_APPS", value) }
    fun getHomeAppModel(i: Int): AppListItem {
        return loadApp("$i")
    }

    fun setHomeAppModel(i: Int, appListItem: AppListItem) {
        storeApp("$i", appListItem)
    }

    var appSwipeLeft: AppListItem
        get() = loadApp(SWIPE_LEFT)
        set(appModel) = storeApp(SWIPE_LEFT, appModel)

    var appSwipeRight: AppListItem
        get() = loadApp(SWIPE_RIGHT)
        set(appModel) = storeApp(SWIPE_RIGHT, appModel)

    var appSwipeUp: AppListItem
        get() = loadApp(SWIPE_UP)
        set(appModel) = storeApp(SWIPE_UP, appModel)

    var appSwipeDown: AppListItem
        get() = loadApp(SWIPE_DOWN)
        set(appModel) = storeApp(SWIPE_DOWN, appModel)

    var appClickClock: AppListItem
        get() = loadApp(CLICK_CLOCK)
        set(appModel) = storeApp(CLICK_CLOCK, appModel)


    var appDoubleTap: AppListItem
        get() = loadApp(DOUBLE_TAP)
        set(appModel) = storeApp(DOUBLE_TAP, appModel)
    private fun loadApp(id: String): AppListItem {
        val appName = prefs.getString("${APP_NAME}_$id", "").toString()
        val appPackage = prefs.getString("${APP_PACKAGE}_$id", "").toString()
        val appActivityName = prefs.getString("${APP_ACTIVITY}_$id", "").toString()
        
        // Load shortcut ID (null for regular apps)
        val shortcutId = try {
            prefs.getString("${APP_SHORTCUT_ID}_$id", null)
        } catch (_: Exception) {
            null
        }
        
        val aliasKey = if (shortcutId != null) {
            "app_alias_${appPackage}_$shortcutId"
        } else {
            "app_alias_$appPackage"
        }
        val customLabel = getAppAlias(aliasKey)

        val userHandleString = try {
            prefs.getString("${APP_USER}_$id", "").toString()
        } catch (_: Exception) {
            ""
        }
        val userHandle: UserHandle = getUserHandleFromString(context, userHandleString)

        return AppListItem(
            activityLabel = appName,
            activityPackage = appPackage,
            customLabel = customLabel, // Set the custom label when loading the app
            activityClass = appActivityName,
            user = userHandle,
            shortcutId = shortcutId
        )
    }

    private fun storeApp(id: String, app: AppListItem) {
        prefs.edit {
            putString("${APP_NAME}_$id", if (Constants.isSeparator(app.activityPackage)) app.activityLabel else app.label)
            putString("${APP_PACKAGE}_$id", app.activityPackage)
            putString("${APP_ACTIVITY}_$id", app.activityClass)
            putString("${APP_USER}_$id", app.user.toString())
            
            if (app.shortcutId != null) {
                putString("${APP_SHORTCUT_ID}_$id", app.shortcutId)
            } else {
                remove("${APP_SHORTCUT_ID}_$id")
            }
        }
    }

    var appSize: Int
        get() {
            return try {
                prefs.getInt(APP_SIZE_TEXT, dimenSp(R.dimen.default_app_size))
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit { putInt(APP_SIZE_TEXT, value) }

    var clockSize: Int
        get() {
            return try {
                prefs.getInt(CLOCK_SIZE_TEXT, dimenSp(R.dimen.default_clock_size))
            } catch (_: Exception) {
                64
            }
        }
        set(value) = prefs.edit { putInt(CLOCK_SIZE_TEXT, value) }



    var settingsSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE_SETTINGS, dimenSp(R.dimen.default_settings_size))
            } catch (_: Exception) {
                17
            }
        }
        set(value) = prefs.edit { putInt(TEXT_SIZE_SETTINGS, value) }

    var textPaddingSize: Int
        get() = try {
            prefs.getInt(TEXT_PADDING_SIZE, dimenDp(R.dimen.default_app_gap))
        } catch (_: Exception) {
            12
        }
        set(value) = prefs.edit { putInt(TEXT_PADDING_SIZE, value) }

    var homeAppsYOffset: Int
        get() = try {
            prefs.getInt(HOME_APPS_Y_OFFSET, 0)
        } catch (_: Exception) {
            0
        }
    set(value) = prefs.edit { putInt(HOME_APPS_Y_OFFSET, value.coerceIn(Constants.MIN_HOME_APPS_Y_OFFSET, Constants.MAX_HOME_APPS_Y_OFFSET)) }


    // --- App Drawer specific settings ---
    var appDrawerSize: Int
        get() = prefs.getInt(Constants.PrefKeys.APP_DRAWER_SIZE, dimenSp(R.dimen.default_app_drawer_size))
        set(value) {
            prefs.edit { putInt(Constants.PrefKeys.APP_DRAWER_SIZE, value.coerceIn(Constants.MIN_APP_SIZE, Constants.MAX_APP_SIZE)) }
            invalidateAppsPerPageCache()
        }

    var appDrawerGap: Int
        get() = prefs.getInt(Constants.PrefKeys.APP_DRAWER_GAP, dimenDp(R.dimen.default_app_drawer_gap))
        set(value) {
            prefs.edit { putInt(Constants.PrefKeys.APP_DRAWER_GAP, value.coerceIn(Constants.MIN_TEXT_PADDING, Constants.MAX_TEXT_PADDING)) }
            invalidateAppsPerPageCache()
        }

    var cachedAppsPerPage: Int
        get() = prefs.getInt(APP_DRAWER_APPS_PER_PAGE_CACHE, -1)
        set(value) = prefs.edit { putInt(APP_DRAWER_APPS_PER_PAGE_CACHE, value) }

    var cachedContainerHeight: Int
        get() = prefs.getInt(APP_DRAWER_CONTAINER_HEIGHT_CACHE, -1)
        private set(value) = prefs.edit { putInt(APP_DRAWER_CONTAINER_HEIGHT_CACHE, value) }

    private var cachedAppDrawerGap: Int
        get() = prefs.getInt("APP_DRAWER_CACHED_GAP", -1)
        set(value) = prefs.edit { putInt("APP_DRAWER_CACHED_GAP", value) }

    private var cachedAppDrawerSize: Int
        get() = prefs.getInt("APP_DRAWER_CACHED_SIZE", -1)
        set(value) = prefs.edit { putInt("APP_DRAWER_CACHED_SIZE", value) }

    fun invalidateAppsPerPageCache() {
        cachedAppsPerPage = -1
        cachedContainerHeight = -1
        cachedAppDrawerGap = -1
        cachedAppDrawerSize = -1
    }

    var cachedDrawerIconSizePx: Int
        get() = prefs.getInt("CACHED_DRAWER_ICON_SIZE_PX", -1)
        set(value) = prefs.edit { putInt("CACHED_DRAWER_ICON_SIZE_PX", value) }
    
    var appDrawerAlignment: Int
        get() = prefs.getInt(Constants.PrefKeys.APP_DRAWER_ALIGNMENT, 0)
        set(value) = prefs.edit { putInt(Constants.PrefKeys.APP_DRAWER_ALIGNMENT, value.coerceIn(0, 2)) }

    var appDrawerSearchEnabled: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED, true)
        set(value) = prefs.edit { putBoolean(Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED, value) }

    // Default: false (do not automatically open IME)
    var appDrawerAutoShowKeyboard: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_SHOW_KEYBOARD, false)
        set(value) = prefs.edit {
            putBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_SHOW_KEYBOARD, value)
            if (value && !appDrawerSearchEnabled) appDrawerSearchEnabled = true
        }

    // Auto-launch single search result
    var appDrawerAutoLaunch: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_LAUNCH, value) }

    var appDrawerSearchContactsEnabled: Boolean
        get() = prefs.getBoolean(APP_DRAWER_SEARCH_CONTACTS, false)
        set(value) = prefs.edit { putBoolean(APP_DRAWER_SEARCH_CONTACTS, value) }

    /** Null means "all accounts" (default). Empty set means none selected. */
    var appDrawerSearchContactAccounts: MutableSet<String>?
        get() = prefs.getStringSet(APP_DRAWER_SEARCH_CONTACT_ACCOUNTS, null)?.toMutableSet()
        set(value) = prefs.edit {
            if (value == null) remove(APP_DRAWER_SEARCH_CONTACT_ACCOUNTS)
            else putStringSet(APP_DRAWER_SEARCH_CONTACT_ACCOUNTS, value)
        }

    var appDrawerSearchWebEnabled: Boolean
        get() = prefs.getBoolean(APP_DRAWER_SEARCH_WEB, false)
        set(value) = prefs.edit { putBoolean(APP_DRAWER_SEARCH_WEB, value) }

    var appDrawerSearchSettingsEnabled: Boolean
        get() = prefs.getBoolean(APP_DRAWER_SEARCH_SETTINGS, false)
        set(value) = prefs.edit { putBoolean(APP_DRAWER_SEARCH_SETTINGS, value) }

    var appDrawerSearchMusicEnabled: Boolean
        get() = prefs.getBoolean(APP_DRAWER_SEARCH_MUSIC, false)
        set(value) = prefs.edit { putBoolean(APP_DRAWER_SEARCH_MUSIC, value) }

    var appDrawerSearchFilesEnabled: Boolean
        get() = prefs.getBoolean(APP_DRAWER_SEARCH_FILES, false)
        set(value) = prefs.edit { putBoolean(APP_DRAWER_SEARCH_FILES, value) }

    var appDrawerSearchHiddenAppsEnabled: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_SEARCH_HIDDEN_APPS, false)
        set(value) = prefs.edit { putBoolean(Constants.PrefKeys.APP_DRAWER_SEARCH_HIDDEN_APPS, value) }

    var appDrawerAzFilter: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_AZ_FILTER, true)
        set(value) {
            prefs.edit {
                putBoolean(Constants.PrefKeys.APP_DRAWER_AZ_FILTER, value)
                if (value) {
                    // Enabling AZ filter should disable page indicator
                    putBoolean(Constants.PrefKeys.APP_DRAWER_PAGER, false)
                }
            }
        }

    var appDrawerSortOrder: Int
        get() = prefs.getInt(Constants.PrefKeys.APP_DRAWER_SORT_ORDER, 0)
        set(value) = prefs.edit { putInt(Constants.PrefKeys.APP_DRAWER_SORT_ORDER, value.coerceIn(0, 2)) }


    var homeAlignment: Int
        get() = prefs.getInt(HOME_ALIGNMENT, 0)
        set(value) = prefs.edit { putInt(HOME_ALIGNMENT, value.coerceIn(0, 2)) }

    var homeClockAlignment: Int
        get() {
            val v = prefs.getInt(Constants.PrefKeys.HOME_CLOCK_ALIGNMENT, -1)
            return if (v == -1) homeAlignment else v
        }
        set(value) = prefs.edit { putInt(Constants.PrefKeys.HOME_CLOCK_ALIGNMENT, value.coerceIn(0, 2)) }

    var homeDateAlignment: Int
        get() {
            val v = prefs.getInt(Constants.PrefKeys.HOME_DATE_ALIGNMENT, -1)
            return if (v == -1) homeAlignment else v
        }
        set(value) = prefs.edit { putInt(Constants.PrefKeys.HOME_DATE_ALIGNMENT, value.coerceIn(0, 2)) }

    var homeQuoteAlignment: Int
        get() {
            val v = prefs.getInt(Constants.PrefKeys.HOME_QUOTE_ALIGNMENT, -1)
            return if (v == -1) homeAlignment else v
        }
        set(value) = prefs.edit { putInt(Constants.PrefKeys.HOME_QUOTE_ALIGNMENT, value.coerceIn(0, 2)) }


    var textIslands: Boolean
        get() = prefs.getBoolean(TEXT_ISLANDS, false)
        set(value) = prefs.edit { putBoolean(TEXT_ISLANDS, value) }

    var textIslandsInverted: Boolean
        get() = prefs.getBoolean(TEXT_ISLANDS_INVERTED, false)
        set(value) = prefs.edit { putBoolean(TEXT_ISLANDS_INVERTED, value) }

    var textIslandsShape: Int
        get() = prefs.getInt(TEXT_ISLANDS_SHAPE, 0)
        set(value) = prefs.edit { putInt(TEXT_ISLANDS_SHAPE, value.coerceIn(0, 2)) }

    var showIcons: Boolean
        get() = prefs.getBoolean(SHOW_ICONS, false)
        set(value) = prefs.edit { putBoolean(SHOW_ICONS, value) }

    // Show icons in the app drawer
    var drawerShowIcons: Boolean
        get() = prefs.getBoolean(DRAWER_SHOW_ICONS, false)
        set(value) = prefs.edit { putBoolean(DRAWER_SHOW_ICONS, value) }

    var iconSourceMode: Int
        get() = prefs.getInt(ICON_SOURCE_MODE, 0)
        set(value) = prefs.edit { putInt(ICON_SOURCE_MODE, value.coerceIn(0, 6)) }

    var iconShape: Int
        get() = prefs.getInt(ICON_SHAPE, 0)
        set(value) = prefs.edit { putInt(ICON_SHAPE, value.coerceIn(0, 2)) }

    // Mode-4 (Tinted) matrix-tint contrast factor, stored as Int 0..30 (= 0.0..3.0 in 0.1 steps).
    var iconTintContrast: Int
        get() = prefs.getInt(ICON_TINT_CONTRAST, 10)
        set(value) = prefs.edit { putInt(ICON_TINT_CONTRAST, value.coerceIn(0, 30)) }

    // Selected icon pack package name (for mode 3)
    var selectedIconPackPackage: String
        get() = prefs.getString(SELECTED_ICON_PACK_PACKAGE, "") ?: ""
        set(value) = prefs.edit { putString(SELECTED_ICON_PACK_PACKAGE, value) }

    var homeAppCharLimit: Int
        get() = prefs.getInt("home_app_char_limit", 20) // default to 20
        set(value) = prefs.edit { putInt("home_app_char_limit", value) }

    var topWidgetMargin: Int
        get() = prefs.getInt("top_widget_margin", dimenDp(R.dimen.default_top_widget_margin))
        set(value) = prefs.edit { putInt("top_widget_margin", value) }

    var bottomWidgetMargin: Int
        get() = prefs.getInt("bottom_widget_margin", dimenDp(R.dimen.default_bottom_widget_margin))
        set(value) = prefs.edit { putInt("bottom_widget_margin", value) }

    // Recents screen preferences
    // 0 = Recents (last used), 1 = Most Used
    var recentsDefaultView: Int
        get() = prefs.getInt(RECENTS_DEFAULT_VIEW, 0)
        set(value) = prefs.edit { putInt(RECENTS_DEFAULT_VIEW, value.coerceIn(0, 1)) }

    var recentsUsageFilter: Int
        get() = prefs.getInt(RECENTS_USAGE_FILTER, 1)
        set(value) = prefs.edit { putInt(RECENTS_USAGE_FILTER, value.coerceIn(0, 3)) }

    // 0 = Time, 1 = Money, 2 = Coffee
    var recentsUsageUnit: Int
        get() = prefs.getInt(RECENTS_USAGE_UNIT, 0)
        set(value) = prefs.edit { putInt(RECENTS_USAGE_UNIT, value.coerceIn(0, 2)) }

    // Per-hour cost for Money/Coffee units
    var recentsUnitCost: Int
        get() = prefs.getInt(RECENTS_UNIT_COST, 10)
        set(value) = prefs.edit { putInt(RECENTS_UNIT_COST, value.coerceIn(1, 99999)) }

    // Currency symbol character (e.g. "$", "€")
    var recentsUnitCurrencyChar: String
        get() = prefs.getString(RECENTS_UNIT_CURRENCY, "$") ?: "$"
        set(value) = prefs.edit { putString(RECENTS_UNIT_CURRENCY, value) }

    // Price per coffee cup (used when unit = Coffee)
    var recentsUnitCoffeePrice: Int
        get() = prefs.getInt(RECENTS_UNIT_COFFEE_PRICE, 3)
        set(value) = prefs.edit { putInt(RECENTS_UNIT_COFFEE_PRICE, value.coerceIn(1, 99999)) }

    var recentsUnitEmojiChar: String
        get() = prefs.getString(RECENTS_UNIT_EMOJI, "☕") ?: "☕"
        set(value) = prefs.edit { putString(RECENTS_UNIT_EMOJI, value.ifEmpty { "☕" }) }

    fun remove(prefName: String) {
        prefs.edit { remove(prefName) }
    }

    fun clear() {
        prefs.edit { clear() }
        context.filesDir.listFiles()?.forEach { file ->
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        }
    }

    /** Ensures all pending SharedPreferences writes are committed to disk. Call before restarting the app. */
    fun ensureWrittenToDisk() {
        prefs.edit(commit = true) { putBoolean("_sync_", false); remove("_sync_") }
    }

    fun getFontForContext(context: String): Constants.FontFamily {
        return if (universalFontEnabled) universalFont else when (context) {
            "settings" -> fontFamily
            "apps" -> appsFont
            "clock" -> clockFont
            "status" -> statusFont
            "notification" -> labelnotificationsFont

            "quote" -> quoteFont
            "letters" -> lettersFont
            "lettersTitle" -> lettersTitleFont
            "notifications" -> notificationsFont
            "date" -> dateFont
            else -> Constants.FontFamily.System
        }
    }

    fun getCustomFontPathForContext(context: String): String? {
        return if (universalFontEnabled && universalFont == Constants.FontFamily.Custom) {
            getCustomFontPath("universal")
        } else {
            getCustomFontPath(context)
        }
    }

    // Per-app allowlist (was blocklist)
    var allowedNotificationApps: MutableSet<String>
        get() = prefs.getStringSet("allowed_notification_apps", mutableSetOf()) ?: mutableSetOf()
        set(value) = prefs.edit { putStringSet("allowed_notification_apps", value) }

    // Per-app allowlist for badge notifications
    var allowedBadgeNotificationApps: MutableSet<String>
        get() = prefs.getStringSet("allowed_badge_notification_apps", mutableSetOf())
            ?: mutableSetOf()
        set(value) = prefs.edit { putStringSet("allowed_badge_notification_apps", value) }

    // Per-app allowlist for SimpleTray notifications
    var allowedSimpleTrayApps: MutableSet<String>
        get() = prefs.getStringSet("allowed_simple_tray_apps", mutableSetOf())
            ?: mutableSetOf()
        set(value) = prefs.edit { putStringSet("allowed_simple_tray_apps", value) }

    // --- Volume keys for page navigation ---
    var useVolumeKeysForPages: Boolean
        get() = prefs.getBoolean("use_volume_keys_for_pages", false)
        set(value) = prefs.edit { putBoolean("use_volume_keys_for_pages", value) }

    var longPressAppInfoEnabled: Boolean
        get() = prefs.getBoolean("long_press_app_info_enabled", false)
        set(value) = prefs.edit { putBoolean("long_press_app_info_enabled", value) }


    // --- Global haptic feedback toggle (affects all helper-triggered vibration) ---
    var hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit { putBoolean("haptic_feedback", value) }

    var vibrationScale: Int
        get() = prefs.getInt("vibration_scale", 100)
        set(value) = prefs.edit { putInt("vibration_scale", value.coerceIn(0, 500)) }

    var onboardingPage: Int
        get() = prefs.getInt(ONBOARDING_PAGE, 0)
        set(value) = prefs.edit { putInt(ONBOARDING_PAGE, value) }

    var inkosWallpaperPath: String?
        get() = prefs.getString(INKOS_WALLPAPER_PATH, null)
        set(value) = prefs.edit { putString(INKOS_WALLPAPER_PATH, value) }

    var inkosWallpaperResourceId: Int
        get() = prefs.getInt(INKOS_WALLPAPER_RESOURCE_ID, 0)
        set(value) = prefs.edit { putInt(INKOS_WALLPAPER_RESOURCE_ID, value) }

    companion object {

        private const val LETTERS_FONT = "letters_font"
        private const val LETTERS_TEXT_SIZE = "letters_text_size"
        private const val LETTERS_TITLE = "letters_title"
        private const val LETTERS_TITLE_FONT = "letters_title_font"
        private const val LETTERS_TITLE_SIZE = "letters_title_size"
        private const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val NOTIFICATIONS_FONT = "notifications_font"
        private const val NOTIFICATIONS_TEXT_SIZE = "notifications_text_size"
        private const val NOTIFICATIONS_PER_PAGE = "notifications_per_page"
        private const val ENABLE_BOTTOM_NAV = "enable_bottom_nav"
    }
}