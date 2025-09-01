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
private const val CLICK_CLOCK_ACTION = "CLICK_CLOCK_ACTION"
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
private const val CLICK_CLOCK = "CLICK_CLOCK"
private const val DOUBLE_TAP = "DOUBLE_TAP"
private const val APP_SIZE_TEXT = "APP_SIZE_TEXT"
private const val CLOCK_SIZE_TEXT = "CLOCK_SIZE_TEXT"
private const val BATTERY_SIZE_TEXT = "BATTERY_SIZE_TEXT"
private const val TEXT_SIZE_SETTINGS = "TEXT_SIZE_SETTINGS"
private const val TEXT_PADDING_SIZE = "TEXT_PADDING_SIZE"
private const val SHOW_NOTIFICATION_BADGE = "show_notification_badge"
private const val ONBOARDING_PAGE = "ONBOARDING_PAGE"

private const val BACKGROUND_COLOR = "BACKGROUND_COLOR"
private const val APP_COLOR = "APP_COLOR"
private const val CLOCK_COLOR = "CLOCK_COLOR"
private const val BATTERY_COLOR = "BATTERY_COLOR"
private const val DATE_COLOR = "DATE_COLOR"
private const val QUOTE_COLOR = "QUOTE_COLOR"
private const val AUDIO_WIDGET_COLOR = "AUDIO_WIDGET_COLOR"

private const val APPS_FONT = "APPS_FONT"
private const val CLOCK_FONT = "CLOCK_FONT"
private const val STATUS_FONT = "STATUS_FONT"  // For Calendar, Alarm, Battery
private const val NOTIFICATION_FONT = "NOTIFICATION_FONT"
private const val QUOTE_FONT = "QUOTE_FONT"

private const val SMALL_CAPS_APPS = "SMALL_CAPS_APPS"
private const val ALL_CAPS_APPS = "ALL_CAPS_APPS"
private const val EINK_REFRESH_ENABLED = "EINK_REFRESH_ENABLED"
private const val HOME_BACKGROUND_IMAGE_URI = "HOME_BACKGROUND_IMAGE_URI"
private const val HOME_BACKGROUND_IMAGE_OPACITY = "HOME_BACKGROUND_IMAGE_OPACITY"
private const val QUOTE_TEXT = "QUOTE_TEXT"
private const val QUOTE_TEXT_SIZE = "QUOTE_TEXT_SIZE"
private const val SHOW_QUOTE = "SHOW_QUOTE"

// App Drawer specific preferences
private const val APP_DRAWER_SIZE = "APP_DRAWER_SIZE"
private const val APP_DRAWER_GAP = "APP_DRAWER_GAP"
private const val APP_DRAWER_ALIGNMENT = "APP_DRAWER_ALIGNMENT"
private const val APP_DRAWER_PAGER = "APP_DRAWER_PAGER"

class Prefs(val context: Context) {
    private val BRIGHTNESS_LEVEL = "BRIGHTNESS_LEVEL"

    /**
     * Stores and retrieves the brightness level (0-255).
     */
    var brightnessLevel: Int
        get() = prefs.getInt(BRIGHTNESS_LEVEL, 128) // Default to mid brightness
        set(value) = prefs.edit { putInt(BRIGHTNESS_LEVEL, value.coerceIn(0, 255)) }
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
            com.github.gezimos.inkos.data.Constants.DEFAULT_EINK_REFRESH_DELAY
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
                prefs.getString("date_font", Constants.FontFamily.System.name).toString()
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString("date_font", value.name) }

    var dateSize: Int
        get() = prefs.getInt("date_text_size", 15)
        set(value) = prefs.edit { putInt("date_text_size", value) }
    var showDate: Boolean
        get() = prefs.getBoolean("SHOW_DATE", false)
        set(value) = prefs.edit { putBoolean("SHOW_DATE", value) }

    var showDateBatteryCombo: Boolean
        get() = prefs.getBoolean("SHOW_DATE_BATTERY_COMBO", false)
        set(value) = prefs.edit { putBoolean("SHOW_DATE_BATTERY_COMBO", value) }

    var showQuote: Boolean
        get() = prefs.getBoolean(SHOW_QUOTE, false)
        set(value) = prefs.edit { putBoolean(SHOW_QUOTE, value) }

    var quoteText: String
        get() = prefs.getString(QUOTE_TEXT, "Stay inspired") ?: "Stay inspired"
        set(value) = prefs.edit { putString(QUOTE_TEXT, value) }

    var quoteSize: Int
        get() = prefs.getInt(QUOTE_TEXT_SIZE, 18)
        set(value) = prefs.edit { putInt(QUOTE_TEXT_SIZE, value) }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    val sharedPrefs: SharedPreferences
        get() = prefs

    private val CUSTOM_FONT_PATH_MAP_KEY = "custom_font_path_map"
    private val gson = Gson()

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
                batteryFont = font
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
                prefs.getString("universal_font", Constants.FontFamily.System.name)!!
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
                batteryFont = value
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
                prefs.getString(LAUNCHER_FONT, Constants.FontFamily.System.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(LAUNCHER_FONT, value.name) }

    var quoteFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(QUOTE_FONT, Constants.FontFamily.System.name)!!
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

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(NOTIFICATIONS_ENABLED, value) }

    var notificationsFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(NOTIFICATIONS_FONT, Constants.FontFamily.System.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(NOTIFICATIONS_FONT, value.name) }

    var notificationsTextSize: Int
        get() = prefs.getInt(NOTIFICATIONS_TEXT_SIZE, 18)
        set(value) = prefs.edit { putInt(NOTIFICATIONS_TEXT_SIZE, value) }

    var showNotificationSenderFullName: Boolean
        get() = prefs.getBoolean("show_notification_sender_full_name", false)
        set(value) = prefs.edit { putBoolean("show_notification_sender_full_name", value) }

    var einkRefreshEnabled: Boolean
        get() = prefs.getBoolean(EINK_REFRESH_ENABLED, false)
        set(value) = prefs.edit { putBoolean(EINK_REFRESH_ENABLED, value) }

    var smallCapsApps: Boolean
        get() = prefs.getBoolean(SMALL_CAPS_APPS, false)
        set(value) = prefs.edit { putBoolean(SMALL_CAPS_APPS, value) }

    var allCapsApps: Boolean
        get() = prefs.getBoolean(ALL_CAPS_APPS, false)
        set(value) = prefs.edit { putBoolean(ALL_CAPS_APPS, value) }

    var homeBackgroundImageUri: String?
        get() = prefs.getString(HOME_BACKGROUND_IMAGE_URI, null)
        set(value) = prefs.edit { putString(HOME_BACKGROUND_IMAGE_URI, value) }

    var homeBackgroundImageOpacity: Int
        get() = prefs.getInt(HOME_BACKGROUND_IMAGE_OPACITY, 100)
        set(value) = prefs.edit { putInt(HOME_BACKGROUND_IMAGE_OPACITY, value) }

    // --- Push Notifications Master Switch ---
    private val _pushNotificationsEnabledFlow = MutableStateFlow(pushNotificationsEnabled)
    val pushNotificationsEnabledFlow: StateFlow<Boolean> get() = _pushNotificationsEnabledFlow

    // Counter-based flow to request a Home refresh (increment to signal a new request)
    private val _forceRefreshHomeCounter = MutableStateFlow(0)
    val forceRefreshHomeFlow: StateFlow<Int> get() = _forceRefreshHomeCounter

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
            for ((key, value) in all) {
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
                            com.github.gezimos.inkos.helper.SystemShortcutHelper.isSystemShortcut(pkg)
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
                when (value) {
                    is String -> putString(key, value)
                    is Boolean -> putBoolean(key, value)
                    is Number -> {
                        if (value.toDouble() == value.toInt().toDouble()) {
                            putInt(key, value.toInt())
                        } else {
                            putFloat(key, value.toFloat())
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

    var einkHelperEnabled: Boolean
        get() = prefs.getBoolean("eink_helper_enabled", false)
        set(value) = prefs.edit { putBoolean("eink_helper_enabled", value) }

    var firstSettingsOpen: Boolean
        get() = prefs.getBoolean(FIRST_SETTINGS_OPEN, true)
        set(value) = prefs.edit { putBoolean(FIRST_SETTINGS_OPEN, value) }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit { putBoolean(LOCK_MODE, value) }

    var homePager: Boolean
        get() = prefs.getBoolean(HOME_PAGES_PAGER, true)
        set(value) = prefs.edit { putBoolean(HOME_PAGES_PAGER, value) }

    var appDrawerPager: Boolean
        get() = prefs.getBoolean(APP_DRAWER_PAGER, true)
        set(value) = prefs.edit { putBoolean(APP_DRAWER_PAGER, value) }

    var homeReset: Boolean
        get() = prefs.getBoolean(HOME_PAGE_RESET, true)
        set(value) = prefs.edit { putBoolean(HOME_PAGE_RESET, value) }

    var homeAppsNum: Int
        get() = prefs.getInt(HOME_APPS_NUM, 12)
        set(value) = prefs.edit { putInt(HOME_APPS_NUM, value) }

    var homePagesNum: Int
        get() = prefs.getInt(HOME_PAGES_NUM, 3)
        set(value) = prefs.edit { putInt(HOME_PAGES_NUM, value) }

    var backgroundColor: Int
        get() = prefs.getInt(BACKGROUND_COLOR, getColor(context, getColorInt("bg")))
        set(value) = prefs.edit { putInt(BACKGROUND_COLOR, value) }

    var appColor: Int
        get() = prefs.getInt(APP_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(APP_COLOR, value) }

    var clockColor: Int
        get() = prefs.getInt(CLOCK_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(CLOCK_COLOR, value) }

    var batteryColor: Int
        get() = prefs.getInt(BATTERY_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(BATTERY_COLOR, value) }

    var dateColor: Int
        get() = prefs.getInt(DATE_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(DATE_COLOR, value) }

    var quoteColor: Int
        get() = prefs.getInt(QUOTE_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(QUOTE_COLOR, value) }

    var audioWidgetColor: Int
        get() = prefs.getInt(AUDIO_WIDGET_COLOR, getColor(context, getColorInt("txt")))
        set(value) = prefs.edit { putInt(AUDIO_WIDGET_COLOR, value) }

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
                        Constants.Action.OpenAppDrawer.name // default: Open App Drawer
                    ).toString()
                )
            } catch (_: Exception) {
                Constants.Action.OpenAppDrawer
            }
        }
        set(value) = prefs.edit { putString(SWIPE_RIGHT_ACTION, value.name) }

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
            return try {
                Constants.Theme.valueOf(
                    prefs.getString(APP_THEME, Constants.Theme.System.name).toString()
                )
            } catch (_: Exception) {
                Constants.Theme.System
            }
        }
        set(value) = prefs.edit { putString(APP_THEME, value.name) }


    var appsFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(APPS_FONT, Constants.FontFamily.System.name).toString()
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(APPS_FONT, value.name) }

    var clockFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(CLOCK_FONT, Constants.FontFamily.System.name).toString()
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(CLOCK_FONT, value.name) }

    var statusFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(STATUS_FONT, Constants.FontFamily.System.name).toString()
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(STATUS_FONT, value.name) }

    var labelnotificationsFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(NOTIFICATION_FONT, Constants.FontFamily.System.name).toString()
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(NOTIFICATION_FONT, value.name) }

    var batteryFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(BATTERY_FONT, Constants.FontFamily.System.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(BATTERY_FONT, value.name) }

    var lettersFont: Constants.FontFamily
        get() = try {
            Constants.FontFamily.valueOf(
                prefs.getString(LETTERS_FONT, Constants.FontFamily.System.name)!!
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
                prefs.getString(LETTERS_TITLE_FONT, Constants.FontFamily.System.name)!!
            )
        } catch (_: Exception) {
            Constants.FontFamily.System
        }
        set(value) = prefs.edit { putString(LETTERS_TITLE_FONT, value.name) }

    var lettersTitleSize: Int
        get() = prefs.getInt(LETTERS_TITLE_SIZE, 36)
        set(value) = prefs.edit { putInt(LETTERS_TITLE_SIZE, value) }

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(HIDDEN_APPS, value) }

    var lockedApps: MutableSet<String>
        get() = prefs.getStringSet(LOCKED_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit { putStringSet(LOCKED_APPS, value) }

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

    var batterySize: Int
        get() {
            return try {
                prefs.getInt(BATTERY_SIZE_TEXT, 18)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit { putInt(BATTERY_SIZE_TEXT, value) }

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
            prefs.getInt(TEXT_PADDING_SIZE, 15)
        } catch (_: Exception) {
            12
        }
        set(value) = prefs.edit { putInt(TEXT_PADDING_SIZE, value) }

    // --- App Drawer specific settings ---
    // Size for app drawer labels (defaults to existing appSize)
    var appDrawerSize: Int
        get() = prefs.getInt(APP_DRAWER_SIZE, appSize)
        set(value) = prefs.edit { putInt(APP_DRAWER_SIZE, value.coerceIn(Constants.MIN_APP_SIZE, Constants.MAX_APP_SIZE)) }

    // Gap / padding between app labels in the drawer (defaults to textPaddingSize)
    var appDrawerGap: Int
        get() = prefs.getInt(APP_DRAWER_GAP, textPaddingSize)
        set(value) = prefs.edit { putInt(APP_DRAWER_GAP, value.coerceIn(Constants.MIN_TEXT_PADDING, Constants.MAX_TEXT_PADDING)) }

    // Alignment for app drawer labels: 0 = START, 1 = CENTER, 2 = END. Default uses start (0).
    var appDrawerAlignment: Int
        get() = prefs.getInt(APP_DRAWER_ALIGNMENT, 0)
        set(value) = prefs.edit { putInt(APP_DRAWER_ALIGNMENT, value.coerceIn(0, 2)) }

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
            Constants.Theme.System -> {
                return if (isSystemInDarkMode(context)) {
                    if (type == "bg") R.color.black
                    else R.color.white
                } else {
                    if (type == "bg") R.color.white
                    else R.color.black
                }
            }

            Constants.Theme.Dark -> {
                return if (type == "bg") R.color.black
                else R.color.white
            }

            Constants.Theme.Light -> {
                return if (type == "bg") R.color.white  // #FFFFFF for background
                else R.color.black  // #000000 for app, date, clock, alarm, battery
            }
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
            "battery" -> batteryFont
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

    // --- Volume keys for page navigation ---
    var useVolumeKeysForPages: Boolean
        get() = prefs.getBoolean("use_volume_keys_for_pages", false)
        set(value) = prefs.edit { putBoolean("use_volume_keys_for_pages", value) }

    var longPressAppInfoEnabled: Boolean
        get() = prefs.getBoolean("long_press_app_info_enabled", false)
        set(value) = prefs.edit { putBoolean("long_press_app_info_enabled", value) }

    // --- Vibration for paging ---
    var useVibrationForPaging: Boolean
        get() = prefs.getBoolean("use_vibration_for_paging", true)
        set(value) = prefs.edit { putBoolean("use_vibration_for_paging", value) }

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
                swipeRightAction = Constants.Action.OpenAppDrawer
            }

            Constants.AppDrawerFlag.SetClickClock -> {
                appClickClock = app
                clickClockAction = Constants.Action.OpenApp
            }

            else -> {}
        }
    }

    companion object {
        private const val BATTERY_FONT = "battery_font"
        private const val LETTERS_FONT = "letters_font"
        private const val LETTERS_TEXT_SIZE = "letters_text_size"
        private const val LETTERS_TITLE = "letters_title"
        private const val LETTERS_TITLE_FONT = "letters_title_font"
        private const val LETTERS_TITLE_SIZE = "letters_title_size"
        private const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val NOTIFICATIONS_FONT = "notifications_font"
        private const val NOTIFICATIONS_TEXT_SIZE = "notifications_text_size"
    }
}