package com.github.gezimos.inkos.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import android.util.Log
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.edit
import com.github.gezimos.inkos.R
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
private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
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
private const val SYSTEM_SHORTCUTS_ENABLED = "SYSTEM_SHORTCUTS_ENABLED"
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
private const val ONBOARDING_PAGE = "ONBOARDING_PAGE"

private const val EDGE_SWIPE_BACK_ENABLED = "EDGE_SWIPE_BACK_ENABLED"

private const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
private const val BACKGROUND_OPACITY = "BACKGROUND_OPACITY"
private const val TEXT_COLOR = "TEXT_COLOR"

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
private const val SECOND_CLOCK_OFFSET_HOURS = "SECOND_CLOCK_OFFSET_HOURS"

// App Drawer specific preferences
private const val APP_DRAWER_APPS_PER_PAGE_CACHE = "APP_DRAWER_APPS_PER_PAGE_CACHE"
private const val APP_DRAWER_CONTAINER_HEIGHT_CACHE = "APP_DRAWER_CONTAINER_HEIGHT_CACHE"
private const val INITIAL_LAUNCH_COMPLETED = "INITIAL_LAUNCH_COMPLETED"
private const val HOME_APPS_Y_OFFSET = "HOME_APPS_Y_OFFSET"
private const val HIDE_HOME_APPS = "HIDE_HOME_APPS"
private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
private const val TEXT_ISLANDS = "TEXT_ISLANDS"
private const val TEXT_ISLANDS_INVERTED = "TEXT_ISLANDS_INVERTED"
private const val TEXT_ISLANDS_SHAPE = "TEXT_ISLANDS_SHAPE"
private const val SHOW_ICONS = "SHOW_ICONS"

class Prefs(val context: Context) {
    private val BRIGHTNESS_LEVEL = "BRIGHTNESS_LEVEL"
    private val LAST_BRIGHTNESS_LEVEL = "LAST_BRIGHTNESS_LEVEL"

    /**
     * Stores and retrieves the brightness level (0-255).
     * When set to 0, this represents "off" state.
     */
    var brightnessLevel: Int
        get() = prefs.getInt(BRIGHTNESS_LEVEL, 128) // Default to mid brightness
        set(value) = prefs.edit { putInt(BRIGHTNESS_LEVEL, value.coerceIn(0, 255)) }
    
    /**
     * Stores the last non-zero brightness level before turning off.
     * This is used to restore brightness when turning it back on.
     */
    var lastBrightnessLevel: Int
        get() = prefs.getInt(LAST_BRIGHTNESS_LEVEL, 128) // Default to mid brightness
        set(value) = prefs.edit { putInt(LAST_BRIGHTNESS_LEVEL, value.coerceIn(1, 255)) }
    var appQuoteWidget: AppListItem
        get() = loadApp("QUOTE_WIDGET")
        set(appModel) = storeApp("QUOTE_WIDGET", appModel)
    private val EINK_REFRESH_DELAY = "EINK_REFRESH_DELAY"
    private val SELECTED_SYSTEM_SHORTCUTS = "SELECTED_SYSTEM_SHORTCUTS"

    // Store selected system shortcuts (package IDs)
    var selectedSystemShortcuts: MutableSet<String>
        get() = prefs.getStringSet(SELECTED_SYSTEM_SHORTCUTS, mutableSetOf()) ?: mutableSetOf()
        set(value) = prefs.edit { putStringSet(SELECTED_SYSTEM_SHORTCUTS, value) }

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
                prefs.getString(CLICK_DATE_ACTION, Constants.Action.Disabled.name)
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
        get() = prefs.getInt(Constants.PrefKeys.DATE_SIZE_TEXT, 18)
        set(value) = prefs.edit { putInt(Constants.PrefKeys.DATE_SIZE_TEXT, value) }
    var showDate: Boolean
        get() = prefs.getBoolean("SHOW_DATE", false)
        set(value) = prefs.edit { putBoolean("SHOW_DATE", value) }

    var showDateBatteryCombo: Boolean
        get() = prefs.getBoolean("SHOW_DATE_BATTERY_COMBO", false)
        set(value) = prefs.edit { putBoolean("SHOW_DATE_BATTERY_COMBO", value) }

    var showNotificationCount: Boolean
        get() = prefs.getBoolean("SHOW_NOTIFICATION_COUNT", true)
        set(value) = prefs.edit { putBoolean("SHOW_NOTIFICATION_COUNT", value) }

    var showQuote: Boolean
        get() = prefs.getBoolean(SHOW_QUOTE, false)
        set(value) = prefs.edit { putBoolean(SHOW_QUOTE, value) }

    var quoteText: String
        get() = prefs.getString(QUOTE_TEXT, "Stay inspired") ?: "Stay inspired"
        set(value) = prefs.edit { putString(QUOTE_TEXT, value) }

    var quoteSize: Int
        get() = prefs.getInt(QUOTE_TEXT_SIZE, 18)
        set(value) = prefs.edit { putInt(QUOTE_TEXT_SIZE, value) }

    var clockMode: Int
        get() = prefs.getInt(CLOCK_MODE, 0)
        set(value) = prefs.edit { putInt(CLOCK_MODE, value.coerceIn(0, 2)) }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    // Keep flows in sync across different Prefs instances by listening to SharedPreferences changes.
    // Also emit changed keys on a SharedFlow so consumers can react to arbitrary preference changes.
    private val _preferenceChangeFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val preferenceChangeFlow = _preferenceChangeFlow.asSharedFlow()

    private val sharedPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        try {
            when (key) {
                APP_THEME -> _appThemeFlow.value = appTheme
                BACKGROUND_COLOR -> _backgroundColorFlow.value = backgroundColor
                TEXT_COLOR -> _textColorFlow.value = textColor
            }
            if (key != null) {
                try { _preferenceChangeFlow.tryEmit(key) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    init {
        try { prefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener) } catch (_: Exception) {}
        // NOTE: float coercion now runs during migration (one-time) instead of every startup.
    }

    // Indicates whether the app has completed its initial launch actions (first run)
    var initialLaunchCompleted: Boolean
        get() = prefs.getBoolean(INITIAL_LAUNCH_COMPLETED, false)
        set(value) = prefs.edit { putBoolean(INITIAL_LAUNCH_COMPLETED, value) }

    val sharedPrefs: SharedPreferences
        get() = prefs

    private val CUSTOM_FONT_PATH_MAP_KEY = "custom_font_path_map"
    private val gson = Gson()

    // Keys that must be stored as Float in SharedPreferences. Ensure importer/migrations keep them as Float.
    private val floatPrefKeys = setOf(SHORT_SWIPE_THRESHOLD_RATIO, LONG_SWIPE_THRESHOLD_RATIO)

    // Keys that were removed or belong to legacy features. Skip them when importing old backups.
    private val deprecatedImportKeys = setOf(
        "SHOW_BATTERY",
        "APP_COLOR","CLOCK_COLOR","BATTERY_COLOR","DATE_COLOR","QUOTE_COLOR","AUDIO_WIDGET_COLOR",
        "HOME_BACKGROUND_IMAGE_URI","HOME_BACKGROUND_IMAGE_OPACITY",
        // battery-related legacy keys from v0.2
        "BATTERY_SIZE_TEXT","battery_font",
        "WALLPAPER_ENABLED","show_background",
        "use_vibration_for_paging",
        // Do not import device-specific UI caches (can cause huge gaps / clipping after restore)
        APP_DRAWER_APPS_PER_PAGE_CACHE,
        APP_DRAWER_CONTAINER_HEIGHT_CACHE,
        "APP_DRAWER_CACHED_GAP",
        "APP_DRAWER_CACHED_SIZE"
    )

    // Convert any stored numeric/string values for known float prefs into Float entries.
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

    // Read a float from SharedPreferences but tolerate values stored as Integer or String.
    private fun getFloatCompat(key: String, default: Float): Float {
        return try {
            prefs.getFloat(key, default)
        } catch (e: ClassCastException) {
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
                // Apply universal font to all elements when enabled
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
                // When universal font is enabled and changed, update all relevant preferences
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
    // Notifications prefs (NotificationsFragment, SimpleTray)
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
        get() = prefs.getInt(NOTIFICATIONS_TEXT_SIZE, 18)
        set(value) = prefs.edit { putInt(NOTIFICATIONS_TEXT_SIZE, value) }

    var notificationsPerPage: Int
        get() = prefs.getInt(NOTIFICATIONS_PER_PAGE, 3).coerceIn(1, 3)
        set(value) = prefs.edit { putInt(NOTIFICATIONS_PER_PAGE, value.coerceIn(1, 3)) }

    var edgeSwipeBackEnabled: Boolean
        get() = prefs.getBoolean(EDGE_SWIPE_BACK_ENABLED, true)
        set(value) {
            prefs.edit { putBoolean(EDGE_SWIPE_BACK_ENABLED, value) }
            try { _edgeSwipeBackEnabledFlow.value = value } catch (_: Exception) {}
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

    // --- Push Notifications Master Switch ---
    private val _pushNotificationsEnabledFlow = MutableStateFlow(pushNotificationsEnabled)
    val pushNotificationsEnabledFlow: StateFlow<Boolean> get() = _pushNotificationsEnabledFlow

    // Counter-based flow to request a Home refresh (increment to signal a new request)
    private val _forceRefreshHomeCounter = MutableStateFlow(0)
    val forceRefreshHomeFlow: StateFlow<Int> get() = _forceRefreshHomeCounter

    // Theme-related flows so Compose can observe changes and recompose immediately
    private val _appThemeFlow = MutableStateFlow(appTheme)
    val appThemeFlow: StateFlow<Constants.Theme> get() = _appThemeFlow

    private val _backgroundColorFlow = MutableStateFlow(backgroundColor)
    val backgroundColorFlow: StateFlow<Int> get() = _backgroundColorFlow

    private val _textColorFlow = MutableStateFlow(textColor)
    val textColorFlow: StateFlow<Int> get() = _textColorFlow

    // Edge-swipe back toggle flow
    private val _edgeSwipeBackEnabledFlow = MutableStateFlow(edgeSwipeBackEnabled)
    val edgeSwipeBackEnabledFlow: StateFlow<Boolean> get() = _edgeSwipeBackEnabledFlow

    // Increment the counter to signal a home refresh request
    fun triggerForceRefreshHome() {
        try {
            _forceRefreshHomeCounter.value = _forceRefreshHomeCounter.value + 1
        } catch (_: Exception) {
            // ignore
        }
    }

    var pushNotificationsEnabled: Boolean
        get() = prefs.getBoolean("push_notifications_enabled", false)
        set(value) {
            prefs.edit { putBoolean("push_notifications_enabled", value) }
            _pushNotificationsEnabledFlow.value = value
        }

    // Save/restore notification-related switches' state
    private val NOTIFICATION_SWITCHES_STATE_KEY = "notification_switches_state"

    fun saveNotificationSwitchesState() {
        val state = mapOf(
            "showNotificationBadge" to showNotificationBadge,
            "showNotificationText" to showNotificationText,
            "showNotificationSenderName" to showNotificationSenderName,
            "showNotificationGroupName" to showNotificationGroupName,
            "showNotificationMessage" to showNotificationMessage,
            "showMediaIndicator" to showMediaIndicator,
            "showMediaName" to showMediaName,
            "notificationsEnabled" to notificationsEnabled,
            "showNotificationSenderFullName" to showNotificationSenderFullName,
            "clearConversationOnAppOpen" to clearConversationOnAppOpen
        )
        prefs.edit { putString(NOTIFICATION_SWITCHES_STATE_KEY, gson.toJson(state)) }
    }

    fun restoreNotificationSwitchesState() {
        val json = prefs.getString(NOTIFICATION_SWITCHES_STATE_KEY, null) ?: return
        val type = object : TypeToken<Map<String, Boolean>>() {}.type
        val state: Map<String, Boolean> = gson.fromJson(json, type) ?: return
        state["showNotificationBadge"]?.let { showNotificationBadge = it }
        state["showNotificationText"]?.let { showNotificationText = it }
        state["showNotificationSenderName"]?.let { showNotificationSenderName = it }
        state["showNotificationGroupName"]?.let { showNotificationGroupName = it }
        state["showNotificationMessage"]?.let { showNotificationMessage = it }
        state["showMediaIndicator"]?.let { showMediaIndicator = it }
        state["showMediaName"]?.let { showMediaName = it }
        state["notificationsEnabled"]?.let { notificationsEnabled = it }
        state["showNotificationSenderFullName"]?.let { showNotificationSenderFullName = it }
        state["clearConversationOnAppOpen"]?.let { clearConversationOnAppOpen = it }
    }

    fun disableAllNotificationSwitches() {
        showNotificationBadge = false
        showNotificationText = false
        showNotificationSenderName = false
        showNotificationGroupName = false
        showNotificationMessage = false
        showMediaIndicator = false
        showMediaName = false
        notificationsEnabled = false
        showNotificationSenderFullName = false
        clearConversationOnAppOpen = false
    }

    fun saveToString(): String {
        val all: HashMap<String, Any?> = HashMap(prefs.all)
        return Gson().toJson(all)
    }

    fun loadFromString(json: String) {
        prefs.edit {
            val all: HashMap<String, Any?> =
                Gson().fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type)
            val pm = context.packageManager
            // If the backup explicitly sets BACKGROUND_COLOR to -1 (sentinel),
            // treat that as "no explicit background" and skip applying background/wallpaper keys.
            var skipBackground = false
            try {
                val rawBg = all[BACKGROUND_COLOR]
                if (rawBg != null) {
                    when (rawBg) {
                        is Number -> if (rawBg.toInt() == -1) skipBackground = true
                        is String -> {
                            val i = rawBg.toIntOrNull()
                            if (i != null && i == -1) skipBackground = true
                        }
                        else -> {}
                    }
                }
            } catch (_: Exception) {}
            for ((key, value) in all) {
                // Skip legacy wallpaper/background keys when backup uses sentinel -1
                if (skipBackground && (key == "WALLPAPER_ENABLED" || key == "show_background" || key == BACKGROUND_COLOR || key == "background_opacity" || key == BACKGROUND_OPACITY)) {
                    continue
                }
                // Skip deprecated/removed preferences entirely when importing old backups.
                if (key in deprecatedImportKeys) continue
                // Explicitly handle allowlists as sets of strings, and filter out non-existent apps
                if (key == "allowed_notification_apps" || key == "allowed_badge_notification_apps" ||
                    key == HIDDEN_APPS || key == LOCKED_APPS
                ) {
                    val set = when (value) {
                        is Collection<*> -> value.filterIsInstance<String>().toMutableSet()
                        is String -> mutableSetOf(value)
                        else -> mutableSetOf<String>()
                    }
                    // For hidden/locked apps, filter by package name only (ignore user handle)
                    // Keep internal synthetic apps (com.inkos.internal.*) and system shortcuts
                    // even if they are not installable packages on the system.
                    val filteredSet = set.filter { pkgUser ->
                        val pkg = pkgUser.split("|")[0]
                        // preserve internal synthetic apps and system shortcuts
                        if (pkg.startsWith("com.inkos.internal.") ||
                            pkg.startsWith("com.inkos.system.")
                        ) {
                            true
                        } else {
                            try {
                                pm.getPackageInfo(pkg, 0)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }.toMutableSet()
                    putStringSet(key, filteredSet)
                    continue
                }
                // Explicitly handle custom_font_paths (can come as List or Set from Gson)
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

                    is MutableSet<*> -> {
                        val list = value.filterIsInstance<String>().toSet()
                        putStringSet(key, list)
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

    var guideShown: Boolean
        get() = prefs.getBoolean("guide_shown", false)
        set(value) = prefs.edit { putBoolean("guide_shown", value) }

    var einkHelperEnabled: Boolean
        get() = prefs.getBoolean("eink_helper_enabled", false)
        set(value) = prefs.edit { putBoolean("eink_helper_enabled", value) }

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_SETTINGS_OPEN, value) }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit { putBoolean(LOCK_MODE, value) }

    // ------------------
    // Home prefs (HomeFragment)
    // ------------------
    var homePager: Boolean
        get() = prefs.getBoolean(HOME_PAGES_PAGER, true)
        set(value) = prefs.edit { putBoolean(HOME_PAGES_PAGER, value) }

    var appDrawerPager: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_PAGER, false)
        set(value) {
            prefs.edit {
                putBoolean(Constants.PrefKeys.APP_DRAWER_PAGER, value)
                if (value) {
                    // Page indicator and AZ filter are mutually exclusive: enabling pager disables AZ filter
                    putBoolean(Constants.PrefKeys.APP_DRAWER_AZ_FILTER, false)
                }
            }
        }

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
    // Shared prefs (Theme, Colors, Fonts) — used across Home, Apps, Notifications
    // ------------------
    var backgroundColor: Int
        get() = prefs.getInt(BACKGROUND_COLOR, getColor(context, getColorInt("bg")))
        set(value) {
            prefs.edit { putInt(BACKGROUND_COLOR, value) }
            try { _backgroundColorFlow.value = value } catch (_: Exception) {}
        }

    var backgroundOpacity: Int
        get() = prefs.getInt(BACKGROUND_OPACITY, 255)
        set(value) = prefs.edit { putInt(BACKGROUND_OPACITY, value) }

    var textColor: Int
        get() = prefs.getInt(TEXT_COLOR, getColor(context, getColorInt("txt")))
        set(value) {
            prefs.edit { putInt(TEXT_COLOR, value) }
            try { _textColorFlow.value = value } catch (_: Exception) {}
        }

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
        get() = prefs.getBoolean(SHOW_CLOCK, false)
        set(value) = prefs.edit { putBoolean(SHOW_CLOCK, value) }

    var showAmPm: Boolean
        get() = prefs.getBoolean(SHOW_AM_PM, true)
        set(value) = prefs.edit { putBoolean(SHOW_AM_PM, value) }

    // Second clock: enabled and hour offset (simple +/- hours from local time)
    var showSecondClock: Boolean
        get() = prefs.getBoolean(SHOW_SECOND_CLOCK, false)
        set(value) = prefs.edit { putBoolean(SHOW_SECOND_CLOCK, value) }

    var secondClockOffsetHours: Int
        get() = prefs.getInt(SECOND_CLOCK_OFFSET_HOURS, 0)
        set(value) = prefs.edit { putInt(SECOND_CLOCK_OFFSET_HOURS, value.coerceIn(-12, 14)) }

    var showAudioWidgetEnabled: Boolean
        get() = prefs.getBoolean(SHOW_AUDIO_WIDGET_ENABLE, false)
        set(value) = prefs.edit { putBoolean(SHOW_AUDIO_WIDGET_ENABLE, value) }

    var showNotificationBadge: Boolean
        get() = prefs.getBoolean(SHOW_NOTIFICATION_BADGE, true)
        set(value) = prefs.edit { putBoolean(SHOW_NOTIFICATION_BADGE, value) }

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

    var systemShortcutsEnabled: Boolean
        get() = prefs.getBoolean(SYSTEM_SHORTCUTS_ENABLED, false)
        set(value) = prefs.edit { putBoolean(SYSTEM_SHORTCUTS_ENABLED, value) }

    var swipeLeftAction: Constants.Action
        get() {
            return try {
                Constants.Action.valueOf(
                    prefs.getString(
                        SWIPE_LEFT_ACTION,
                        Constants.Action.OpenNotificationsScreen.name // default: Notifications
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenNotificationsScreen
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
                        Constants.Action.Disabled.name
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
            val stored = prefs.getString(APP_THEME, Constants.Theme.Light.name)?.toString() ?: Constants.Theme.Light.name
            return try {
                // Handle legacy stored value "System" by mapping it to the current
                // system mode at runtime (Dark -> Dark, otherwise Light).
                if (stored.equals("System", ignoreCase = true)) {
                    if (isSystemInDarkMode(context)) Constants.Theme.Dark else Constants.Theme.Light
                } else {
                    Constants.Theme.valueOf(stored)
                }
            } catch (_: Exception) {
                Constants.Theme.Light
            }
        }
        set(value) {
            prefs.edit { putString(APP_THEME, value.name) }
            try { _appThemeFlow.value = value } catch (_: Exception) {}
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
        get() = prefs.getInt(LETTERS_TEXT_SIZE, 18)
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
        get() = prefs.getInt(LETTERS_TITLE_SIZE, 36)
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

    var newlyInstalledApps: MutableSet<String>
        get() = prefs.getStringSet("NEWLY_INSTALLED_APPS", mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet("NEWLY_INSTALLED_APPS", value) }

    /**
     * By the number in home app list, get the list item.
     * TODO why not just save it as a list?
     */
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

    /**
     *  Restore an `AppListItem` from preferences.
     *
     *  We store not only application name, but everything needed to start the item.
     *  Because thus we save time to query the system about it?
     *
     *  TODO store with protobuf instead of serializing manually.
     */
    private fun loadApp(id: String): AppListItem {
        val appName = prefs.getString("${APP_NAME}_$id", "").toString()
        val appPackage = prefs.getString("${APP_PACKAGE}_$id", "").toString()
        val appActivityName = prefs.getString("${APP_ACTIVITY}_$id", "").toString()
        val customLabel = getAppAlias("app_alias_$appPackage")

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
        )
    }

    private fun storeApp(id: String, app: AppListItem) {
        prefs.edit {

            putString("${APP_NAME}_$id", app.label)
            putString("${APP_PACKAGE}_$id", app.activityPackage)
            putString("${APP_ACTIVITY}_$id", app.activityClass)
            putString("${APP_ALIAS}_$id", app.customLabel)
            putString("${APP_USER}_$id", app.user.toString())
        }
    }

    var appSize: Int
        get() {
            return try {
                prefs.getInt(APP_SIZE_TEXT,27)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit { putInt(APP_SIZE_TEXT, value) }

    var clockSize: Int
        get() {
            return try {
                prefs.getInt(CLOCK_SIZE_TEXT, 48)
            } catch (_: Exception) {
                64
            }
        }
        set(value) = prefs.edit { putInt(CLOCK_SIZE_TEXT, value) }



    var settingsSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE_SETTINGS, 16)
            } catch (_: Exception) {
                17
            }
        }
        set(value) = prefs.edit { putInt(TEXT_SIZE_SETTINGS, value) }

    var textPaddingSize: Int
        get() = try {
            prefs.getInt(TEXT_PADDING_SIZE, 10)
        } catch (_: Exception) {
            12
        }
        set(value) = prefs.edit { putInt(TEXT_PADDING_SIZE, value) }

    // Vertical offset (Y) applied to home apps grouping (in dp). Default 0.
    var homeAppsYOffset: Int
        get() = try {
            prefs.getInt(HOME_APPS_Y_OFFSET, 0)
        } catch (_: Exception) {
            0
        }
    set(value) = prefs.edit { putInt(HOME_APPS_Y_OFFSET, value.coerceIn(Constants.MIN_HOME_APPS_Y_OFFSET, Constants.MAX_HOME_APPS_Y_OFFSET)) }

    // --- App Drawer specific settings ---
    // Size for app drawer labels (defaults to existing appSize)
    var appDrawerSize: Int
        get() = prefs.getInt(Constants.PrefKeys.APP_DRAWER_SIZE, Constants.DEFAULT_APP_DRAWER_SIZE)
        set(value) {
            prefs.edit { putInt(Constants.PrefKeys.APP_DRAWER_SIZE, value.coerceIn(Constants.MIN_APP_SIZE, Constants.MAX_APP_SIZE)) }
            invalidateAppsPerPageCache()
        }

    // Gap / padding between app labels in the drawer (defaults to textPaddingSize)
    var appDrawerGap: Int
        get() = prefs.getInt(Constants.PrefKeys.APP_DRAWER_GAP, Constants.DEFAULT_APP_DRAWER_GAP)
        set(value) {
            prefs.edit { putInt(Constants.PrefKeys.APP_DRAWER_GAP, value.coerceIn(Constants.MIN_TEXT_PADDING, Constants.MAX_TEXT_PADDING)) }
            invalidateAppsPerPageCache()
        }

    // Cached appsPerPage calculation for instant app drawer display
    var cachedAppsPerPage: Int
        get() = prefs.getInt(APP_DRAWER_APPS_PER_PAGE_CACHE, -1)
        private set(value) = prefs.edit { putInt(APP_DRAWER_APPS_PER_PAGE_CACHE, value) }

    var cachedContainerHeight: Int
        get() = prefs.getInt(APP_DRAWER_CONTAINER_HEIGHT_CACHE, -1)
        private set(value) = prefs.edit { putInt(APP_DRAWER_CONTAINER_HEIGHT_CACHE, value) }

    // Cache the gap and size used in calculation to detect when recalculation is needed
    private var cachedAppDrawerGap: Int
        get() = prefs.getInt("APP_DRAWER_CACHED_GAP", -1)
        set(value) = prefs.edit { putInt("APP_DRAWER_CACHED_GAP", value) }

    private var cachedAppDrawerSize: Int
        get() = prefs.getInt("APP_DRAWER_CACHED_SIZE", -1)
        set(value) = prefs.edit { putInt("APP_DRAWER_CACHED_SIZE", value) }

    fun updateAppsPerPageCache(appsPerPage: Int, containerHeight: Int, gap: Int, size: Int) {
        prefs.edit {
            putInt(APP_DRAWER_APPS_PER_PAGE_CACHE, appsPerPage)
            putInt(APP_DRAWER_CONTAINER_HEIGHT_CACHE, containerHeight)
            putInt("APP_DRAWER_CACHED_GAP", gap)
            putInt("APP_DRAWER_CACHED_SIZE", size)
        }
    }

    fun invalidateAppsPerPageCache() {
        cachedAppsPerPage = -1
        cachedContainerHeight = -1
        cachedAppDrawerGap = -1
        cachedAppDrawerSize = -1
    }
    
    // Check if cache is valid for current settings
    fun isAppsPerPageCacheValid(containerHeight: Int, gap: Int, size: Int): Boolean {
        return cachedAppsPerPage > 0 &&
               cachedContainerHeight == containerHeight &&
               cachedAppDrawerGap == gap &&
               cachedAppDrawerSize == size
    }

    // Alignment for app drawer labels: 0 = START, 1 = CENTER, 2 = END. Default uses start (0).
    var appDrawerAlignment: Int
        get() = prefs.getInt(Constants.PrefKeys.APP_DRAWER_ALIGNMENT, 0)
        set(value) = prefs.edit { putInt(Constants.PrefKeys.APP_DRAWER_ALIGNMENT, value.coerceIn(0, 2)) }

    // Enable search bar in app drawer
    var appDrawerSearchEnabled: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED, true)
        set(value) = prefs.edit { putBoolean(Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED, value) }

    // Auto-show soft keyboard when opening the app drawer search field
    // Default: false (do not automatically open IME)
    var appDrawerAutoShowKeyboard: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_SHOW_KEYBOARD, false)
        set(value) = prefs.edit {
            putBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_SHOW_KEYBOARD, value)
            // Ensure search is enabled when enabling auto-show keyboard
            if (value) putBoolean(Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED, true)
        }

    // Auto-launch single search result
    var appDrawerAutoLaunch: Boolean
        get() = prefs.getBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(Constants.PrefKeys.APP_DRAWER_AUTO_LAUNCH, value) }

    // AZ Filter: when true, show A→Z sidebar instead of page indicator
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

    // NOTE: `appsPerPage` removed — app drawer now computes pages dynamically.

    // Alignment for home apps: 0 = START, 1 = CENTER, 2 = END. Default uses start (0).
    var homeAlignment: Int
        get() = prefs.getInt(HOME_ALIGNMENT, 0)
        set(value) = prefs.edit { putInt(HOME_ALIGNMENT, value.coerceIn(0, 2)) }

    // Text Islands: Show pill-shaped ripples around text for better legibility with wallpapers
    var textIslands: Boolean
        get() = prefs.getBoolean(TEXT_ISLANDS, false)
        set(value) = prefs.edit { putBoolean(TEXT_ISLANDS, value) }

    // Text Islands: Invert colors (bg color for pill, text color for text)
    var textIslandsInverted: Boolean
        get() = prefs.getBoolean(TEXT_ISLANDS_INVERTED, false)
        set(value) = prefs.edit { putBoolean(TEXT_ISLANDS_INVERTED, value) }

    // Text Islands: Shape of the ripple (0=Pill, 1=Rounded, 2=Square)
    var textIslandsShape: Int
        get() = prefs.getInt(TEXT_ISLANDS_SHAPE, 0)
        set(value) = prefs.edit { putInt(TEXT_ISLANDS_SHAPE, value.coerceIn(0, 2)) }

    // Show simple icons for apps (periodic table style)
    var showIcons: Boolean
        get() = prefs.getBoolean(SHOW_ICONS, false)
        set(value) = prefs.edit { putBoolean(SHOW_ICONS, value) }

    var homeAppCharLimit: Int
        get() = prefs.getInt("home_app_char_limit", 20) // default to 20
        set(value) = prefs.edit { putInt("home_app_char_limit", value) }

    var topWidgetMargin: Int
        get() = prefs.getInt("top_widget_margin", Constants.DEFAULT_TOP_WIDGET_MARGIN)
        set(value) = prefs.edit { putInt("top_widget_margin", value) }

    var bottomWidgetMargin: Int
        get() = prefs.getInt("bottom_widget_margin", Constants.DEFAULT_BOTTOM_WIDGET_MARGIN)
        set(value) = prefs.edit { putInt("bottom_widget_margin", value) }

    private fun getColorInt(type: String): Int {
        when (appTheme) {
            Constants.Theme.Dark -> {
                return if (type == "bg") R.color.black
                else R.color.white
            }

            Constants.Theme.Light -> {
                return if (type == "bg") R.color.white  // #FFFFFF for background
                else R.color.black  // #000000 for app, date, clock, alarm, battery
            }
        }
            return if (appTheme == Constants.Theme.Dark) {
                if (type == "bg") R.color.black else R.color.white
            } else {
                if (type == "bg") R.color.white else R.color.black
            }
    }

    // return app label
    fun getAppName(location: Int): String {
        return getHomeAppModel(location).activityLabel
    }

    fun remove(prefName: String) {
        prefs.edit { remove(prefName) }
    }

    fun clear() {
        prefs.edit { clear() }
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

    // NOTE: per recent UX change, paging-specific vibration pref removed.

    // --- Global haptic feedback toggle (affects all helper-triggered vibration) ---
    var hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit { putBoolean("haptic_feedback", value) }

    var onboardingPage: Int
        get() = prefs.getInt(ONBOARDING_PAGE, 0)
        set(value) = prefs.edit { putInt(ONBOARDING_PAGE, value) }

    fun setGestureApp(flag: Constants.AppDrawerFlag, app: AppListItem) {
        when (flag) {
            Constants.AppDrawerFlag.SetSwipeLeft -> {
                appSwipeLeft = app
                swipeLeftAction = Constants.Action.OpenApp
            }

            Constants.AppDrawerFlag.SetSwipeRight -> {
                appSwipeRight = app
                swipeRightAction = Constants.Action.OpenApp
            }

            Constants.AppDrawerFlag.SetClickClock -> {
                appClickClock = app
                clickClockAction = Constants.Action.OpenApp
            }

            else -> {}
        }
    }

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