package com.github.gezimos.inkos.data

import android.content.Context
import com.github.gezimos.inkos.BuildConfig

class Migration(val context: Context) {
    fun migratePreferencesOnVersionUpdate(prefs: Prefs) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val savedVersionCode = prefs.appVersion

        // Define a map of version code -> preferences to clear
        val versionCleanupMap = mapOf<Int, List<String>>(
            101004 to emptyList()
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

        prefs.appVersion = currentVersionCode
    }
}