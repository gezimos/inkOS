package com.github.gezimos.inkos.data

import android.content.Context
import com.github.gezimos.inkos.BuildConfig

class Migration(val context: Context) {
    fun migratePreferencesOnVersionUpdate(prefs: Prefs) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val savedVersionCode = prefs.appVersion

        // Define a map of version code -> preferences to clear
        val versionCleanupMap = mapOf<Int, List<String>>(
            101004 to listOf("HOME_BACKGROUND_IMAGE_URI", "HOME_BACKGROUND_IMAGE_OPACITY")
            // Add more versions and preferences to remove here
        )

        // Iterate over the versions and clear the relevant preferences
        for ((version, keys) in versionCleanupMap) {
            // Only clear preferences for versions between savedVersionCode and currentVersionCode
            if (version in (savedVersionCode + 1)..currentVersionCode) {
                // Remove the preferences for this version
                keys.forEach { key ->
                    prefs.remove(key)
                }
            }
        }        
        // Clear stored brightness if it equals the old minimum sentinel (20).
        // Removing the stored key lets the app save the user's new brightness correctly.
        try {
            val stored = prefs.brightnessLevel
            if (stored == 20) {
                prefs.remove("BRIGHTNESS_LEVEL")
            }
        } catch (_: Exception) {
            // Ignore errors (permissions, prefs issues, etc.)
        }

        // Migrate legacy paging vibration pref to the new global haptic pref.
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

        // Remove legacy integrated wallpaper preference — app now uses Android wallpaper instead.
        try {
            if (prefs.sharedPrefs.contains("WALLPAPER_ENABLED")) {
                prefs.remove("WALLPAPER_ENABLED")
            }
            // Also remove show_background flag if present (older backups may set this)
            if (prefs.sharedPrefs.contains("show_background")) {
                prefs.remove("show_background")
            }
            // NOTE: keep background color/opacities intact to preserve v0.4 behavior
        } catch (_: Exception) {
            // ignore
        }
        // Remove other deprecated preferences that may be present in very old backups.
        try {
            val deprecated = listOf(
                "SHOW_BATTERY",
                "APP_COLOR","CLOCK_COLOR","BATTERY_COLOR","DATE_COLOR","QUOTE_COLOR","AUDIO_WIDGET_COLOR",
                "HOME_BACKGROUND_IMAGE_URI","HOME_BACKGROUND_IMAGE_OPACITY",
                // battery-related legacy keys from v0.2
                "BATTERY_SIZE_TEXT","battery_font"
            )
            for (k in deprecated) {
                if (prefs.sharedPrefs.contains(k)) prefs.remove(k)
            }
        } catch (_: Exception) {
            // ignore
        }

        // Ensure known float prefs are stored as Float (use central helper in Prefs).
        try {
            prefs.ensureFloatPrefsAreFloat()
        } catch (_: Exception) {
            // ignore
        }

        // Migration: If showDateBatteryCombo was enabled, keep it enabled (now independent battery display)
        // This ensures users who had the combo enabled will still see battery after the refactor
        try {
            val sp = prefs.sharedPrefs
            val comboKey = "SHOW_DATE_BATTERY_COMBO"
            if (sp.contains(comboKey)) {
                val wasComboEnabled = try { sp.getBoolean(comboKey, false) } catch (_: Exception) { false }
                if (wasComboEnabled) {
                    // Keep the preference enabled (it's now used for independent battery display)
                    prefs.showDateBatteryCombo = true
                }
            }
        } catch (_: Exception) {
            // ignore migration errors
        }

        prefs.appVersion = currentVersionCode
    }
}