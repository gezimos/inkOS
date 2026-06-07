package com.github.gezimos.inkos.data

import android.content.Context
import com.github.gezimos.inkos.BuildConfig

class Migration(val context: Context) {
    fun migratePreferencesOnVersionUpdate(prefs: Prefs) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val savedVersionCode = prefs.appVersion

        val versionCleanupMap = mapOf<Int, List<String>>(
            101004 to listOf("HOME_BACKGROUND_IMAGE_URI", "HOME_BACKGROUND_IMAGE_OPACITY")
            // Add more versions and preferences to remove here
        )

        for ((version, keys) in versionCleanupMap) {
            if (version in (savedVersionCode + 1)..currentVersionCode) {
                // Remove the preferences for this version
                keys.forEach { key ->
                    prefs.remove(key)
                }
            }
        }        
        try {
            val stored = prefs.brightnessLevel
            if (stored == 20) {
                prefs.remove("BRIGHTNESS_LEVEL")
            }
        } catch (_: Exception) {
            // Ignore errors (permissions, prefs issues, etc.)
        }

        try {
            val sp = prefs.sharedPrefs
            val legacyKey = "use_vibration_for_paging"
            if (sp.contains(legacyKey)) {
                val legacyVal = try { sp.getBoolean(legacyKey, false) } catch (_: Exception) { false }
                if (legacyVal) {
                    prefs.hapticFeedback = true
                }
                // Remove the old key to avoid stale prefs
                prefs.remove(legacyKey)
            }
        } catch (_: Exception) {
            // ignore migration errors
        }

        try {
            if (prefs.sharedPrefs.contains("WALLPAPER_ENABLED")) {
                prefs.remove("WALLPAPER_ENABLED")
            }
            if (prefs.sharedPrefs.contains("show_background")) {
                prefs.remove("show_background")
            }
        } catch (_: Exception) {
            // ignore
        }
        try {
            val deprecated = listOf(
                "SHOW_BATTERY",
                "APP_COLOR","CLOCK_COLOR","BATTERY_COLOR","DATE_COLOR","QUOTE_COLOR","AUDIO_WIDGET_COLOR",
                "ALARM_CLOCK_COLOR",
                "HOME_BACKGROUND_IMAGE_URI","HOME_BACKGROUND_IMAGE_OPACITY",
                // battery-related legacy keys from v0.2
                "BATTERY_SIZE_TEXT","battery_font",
                // stale legacy keys
                "custom_font_path", "background_opacity"
            )
            for (k in deprecated) {
                if (prefs.sharedPrefs.contains(k)) prefs.remove(k)
            }
        } catch (_: Exception) {
            // ignore
        }

        try {
            val sp = prefs.sharedPrefs
            val currentFilesDir = context.filesDir.absolutePath
            val oldPkgPattern = Regex("/data/user/\\d+/[^/]+/files/")

            // Migrate custom_font_paths set
            val paths = sp.getStringSet("custom_font_paths", null)
            if (paths != null) {
                val newPaths = paths.map { path ->
                    val fileName = path.substringAfterLast("/")
                    val newPath = "$currentFilesDir/$fileName"
                    if (java.io.File(newPath).exists()) newPath
                    else if (java.io.File(path).exists()) path
                    else newPath // keep new path, font will need re-import
                }.toMutableSet()
                if (newPaths != paths) {
                    sp.edit().putStringSet("custom_font_paths", newPaths).apply()
                }
            }

            // Migrate custom_font_path_map JSON
            val mapJson = sp.getString("custom_font_path_map", null)
            if (mapJson != null && oldPkgPattern.containsMatchIn(mapJson)) {
                val updated = oldPkgPattern.replace(mapJson) { "${currentFilesDir}/" }
                sp.edit().putString("custom_font_path_map", updated).apply()
            }
        } catch (_: Exception) {
            // ignore
        }

        try {
            prefs.ensureFloatPrefsAreFloat()
        } catch (_: Exception) {
            // ignore
        }

        try {
            val sp = prefs.sharedPrefs
            val comboKey = "SHOW_DATE_BATTERY_COMBO"
            if (sp.contains(comboKey)) {
                val wasComboEnabled = try { sp.getBoolean(comboKey, false) } catch (_: Exception) { false }
                if (wasComboEnabled) {
                    prefs.showDateBatteryCombo = true
                }
            }
        } catch (_: Exception) {
            // ignore migration errors
        }

        if (101005 in (savedVersionCode + 1)..currentVersionCode) {
            try {
                val sp = prefs.sharedPrefs
                val legacyToShortcutId = mapOf(
                    "com.inkos.internal.app_drawer"   to "inkos_app_drawer",
                    "com.inkos.internal.notifications" to "inkos_notifications",
                    "com.inkos.internal.simple_tray"  to "inkos_simple_tray",
                    "com.inkos.internal.hub"           to "inkos_hub",
                    "com.inkos.internal.recents"       to "inkos_recents"
                )
                val slotIds = buildList {
                    for (i in 0..29) add(i.toString())
                    addAll(listOf("SWIPE_LEFT", "SWIPE_RIGHT", "SWIPE_UP", "SWIPE_DOWN",
                                  "CLICK_CLOCK", "DOUBLE_TAP", "CLICK_DATE", "QUOTE_WIDGET"))
                }
                val editor = sp.edit()
                for (slot in slotIds) {
                    val pkgKey = "APP_PACKAGE_$slot"
                    val pkg = sp.getString(pkgKey, null) ?: continue
                    // Migrate old empty space to new separator
                    if (pkg == "com.inkos.internal.empty_space") {
                        editor.putString(pkgKey, Constants.SEPARATOR_EMPTY)
                        editor.putString("APP_NAME_$slot", " ")
                        editor.putString("APP_ACTIVITY_$slot", "")
                        continue
                    }
                    val newShortcutId = legacyToShortcutId[pkg] ?: continue
                    editor.putString(pkgKey, "app.inkos")
                    editor.putString("APP_SHORTCUT_ID_$slot", newShortcutId)
                    editor.putString("APP_ACTIVITY_$slot", "com.github.gezimos.inkos.MainActivity")
                }

                // Update HIDDEN_APPS and LOCKED_APPS sets
                val appId = BuildConfig.APPLICATION_ID
                for (setKey in listOf("HIDDEN_APPS", "LOCKED_APPS")) {
                    val oldSet = sp.getStringSet(setKey, null) ?: continue
                    val newSet = mutableSetOf<String>()
                    var changed = false
                    for (entry in oldSet) {
                        val parts = entry.split("|")
                        val pkg = parts.getOrNull(0) ?: ""
                        val userPart = if (parts.size >= 2) parts[1] else "UserHandle{0}"
                        val internalShortcutId = legacyToShortcutId[pkg]
                        when {
                            internalShortcutId != null -> {
                                newSet.add("$appId|$internalShortcutId|$userPart")
                                changed = true
                            }
                            pkg.startsWith("com.inkos.system.") -> {
                                val sysId = "system_${pkg.removePrefix("com.inkos.system.")}"
                                newSet.add("$appId|$sysId|$userPart")
                                changed = true
                            }
                            else -> newSet.add(entry)
                        }
                    }
                    if (changed) editor.putStringSet(setKey, newSet)
                }
                editor.apply()
            } catch (_: Exception) {
            }
        }

        try {
            val sp = prefs.sharedPrefs
            if (!sp.getBoolean("migration_letters_rename_done", false)) {
                val gestureKeys = listOf(
                    "SWIPE_LEFT_ACTION", "SWIPE_RIGHT_ACTION", "SWIPE_UP_ACTION", "SWIPE_DOWN_ACTION",
                    "DOUBLE_TAP_ACTION", "CLICK_CLOCK_ACTION", "CLICK_DATE_ACTION", "QUOTE_ACTION"
                )
                val editor = sp.edit()
                for (key in gestureKeys) {
                    if (sp.getString(key, null) == "OpenNotificationsScreen") {
                        editor.putString(key, "OpenLettersScreen")
                    }
                }
                editor.putBoolean("migration_letters_rename_done", true)
                editor.apply()
            }
        } catch (_: Exception) {}

        try {
            val sp = prefs.sharedPrefs
            if (!sp.contains("BOTTOM_WIDGET_TYPE")) {
                val showAndroid = sp.getBoolean("SHOW_ANDROID_WIDGET", false)
                val showQuote = sp.getBoolean("SHOW_QUOTE", false)
                val inferred = when {
                    showAndroid -> "android_widget"
                    showQuote -> "quote"
                    else -> "quote"
                }
                sp.edit().putString("BOTTOM_WIDGET_TYPE", inferred).apply()
            }
        } catch (_: Exception) {}


        try {
            if (savedVersionCode <= 101004) {
                val sp = prefs.sharedPrefs
                val importedText = sp.getInt("TEXT_COLOR", android.graphics.Color.BLACK)
                val importedBg = sp.getInt("BACKGROUND_COLOR", android.graphics.Color.WHITE)
                val wasDark = (sp.getString("APP_THEME", "Light") ?: "Light") == "Dark"

                val editor = sp.edit()
                if (wasDark) {
                    editor.putInt("DARK_TEXT_COLOR", importedText)
                    editor.putInt("DARK_BACKGROUND_COLOR", importedBg)
                    editor.putInt("TEXT_COLOR", importedBg)
                    editor.putInt("BACKGROUND_COLOR", importedText)
                } else {
                    editor.putInt("DARK_TEXT_COLOR", importedBg)
                    editor.putInt("DARK_BACKGROUND_COLOR", importedText)
                }
                editor.apply()
            }
        } catch (_: Exception) {}

        try {
            val sp = prefs.sharedPrefs
            if (!sp.contains("eink_helper_mode") && sp.contains("eink_helper_enabled")) {
                val wasEnabled = try { sp.getBoolean("eink_helper_enabled", false) } catch (_: Exception) { false }
                sp.edit()
                    .putInt("eink_helper_mode", if (wasEnabled) 3 else 0) // 3 = Clear
                    .remove("eink_helper_enabled")
                    .apply()
            }
        } catch (_: Exception) {}

        if (savedVersionCode in 1..101004) {
            prefs.commitFirstOpen(true)
            prefs.commitThemePickerShown(true) // Skip auto theme picker, it's embedded in onboarding
        }

        prefs.appVersion = currentVersionCode
    }
}