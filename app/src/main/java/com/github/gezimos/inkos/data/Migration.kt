package com.github.gezimos.inkos.data

import android.content.Context
import com.github.gezimos.inkos.BuildConfig

class Migration(val context: Context) {
    fun migratePreferencesOnVersionUpdate(prefs: Prefs) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val savedVersionCode = prefs.appVersion

        // Define a map of version code -> preferences to clear
        val versionCleanupMap = mapOf(
            171 to listOf(
                "APP_DARK_COLORS",
                "APP_LIGHT_COLORS",
                "HOME_FOLLOW_ACCENT",
                "ALL_APPS_TEXT"
            ),
            172 to listOf(
                "TIME_ALIGNMENT",
                "SHOW_TIME",
                "SHOW_TIME_FORMAT",
                "TIME_COLOR"
            ),
            175 to listOf(
                "CLICK_APP_USAGE"
            ),
            10803 to listOf(
                "SHOW_EDGE_PANEL",
                "EDGE_APPS_NUM"
            ),
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

        // Update the stored version code after cleanup
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

        prefs.appVersion = currentVersionCode
    }
}