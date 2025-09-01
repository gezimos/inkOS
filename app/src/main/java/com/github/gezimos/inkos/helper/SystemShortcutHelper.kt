package com.github.gezimos.inkos.helper

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.data.AppListItem

/**
 * Helper class for managing system shortcut synthetic apps.
 * These apps provide direct access to Android system activities and settings.
 */
object SystemShortcutHelper {
    /**
     * Get only selected system shortcuts as AppListItems
     */
    fun getSelectedSystemShortcutsAsAppItems(prefs: com.github.gezimos.inkos.data.Prefs): List<AppListItem> {
        val selected = prefs.selectedSystemShortcuts
        return systemShortcuts.filter { selected.contains(it.packageId) }
            .map { shortcut ->
                val customLabel = prefs.getAppAlias("app_alias_${shortcut.packageId}")
                createAppListItem(shortcut, customLabel)
            }
    }

    /**
     * Data class representing a system shortcut definition
     */
    data class SystemShortcut(
        val packageId: String,
        val displayName: String,
        val targetPackage: String,
        val targetActivity: String,
        val intentType: IntentType = IntentType.COMPONENT
    )

    /**
     * Types of intents used for launching system activities
     */
    enum class IntentType {
        COMPONENT,      // Standard ComponentName intent
        ACTION,         // Intent with action
        SPECIAL         // Custom intent handling
    }

    /**
     * List of all available system shortcuts
     */
    val systemShortcuts = listOf(
        SystemShortcut(
            packageId = "com.inkos.system.app_memory_usage",
            displayName = "Memory Usage",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$AppMemoryUsageActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.battery_optimization",
            displayName = "Battery Optimization",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$HighPowerApplicationsActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.notification_log",
            displayName = "Notification Log",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$NotificationStationActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.developer_options",
            displayName = "Developer Options",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$DevelopmentSettingsDashboardActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.mobile_network",
            displayName = "Mobile Network",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.network.telephony.MobileNetworkActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.sim_lock",
            displayName = "SIM Lock",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$IccLockSettingsActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.vision_settings",
            displayName = "Vision Settings",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.accessibility.AccessibilitySettingsForSetupWizardActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.data_usage",
            displayName = "Data Usage",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$DataUsageSummaryActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.app_notifications",
            displayName = "App Notifications",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$NotificationAppListActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.settings_search",
            displayName = "Settings Search",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$SettingsActivity",
            intentType = IntentType.ACTION
        ),
        SystemShortcut(
            packageId = "com.inkos.system.storage_manager",
            displayName = "Storage Manager",
            targetPackage = "com.android.storagemanager",
            targetActivity = "com.android.storagemanager.deletionhelper.DeletionHelperActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.brightness_dialog",
            displayName = "Brightness Dialog",
            targetPackage = "com.android.systemui",
            targetActivity = "com.android.systemui.settings.brightness.BrightnessDialog",
            intentType = IntentType.SPECIAL
        ),
    )

    /**
     * Check if a package name is a system shortcut
     */
    fun isSystemShortcut(packageName: String): Boolean {
        return packageName.startsWith("com.inkos.system.")
    }

    /**
     * Get system shortcut by package name
     */
    fun getSystemShortcut(packageName: String): SystemShortcut? {
        return systemShortcuts.find { it.packageId == packageName }
    }

    /**
     * Create AppListItem for a system shortcut
     */
    fun createAppListItem(shortcut: SystemShortcut, customLabel: String = ""): AppListItem {
        // Append a visible marker to indicate this is a System Shortcut
        val labeledName = "${shortcut.displayName} {"
        return AppListItem(
            activityLabel = labeledName,
            activityPackage = shortcut.packageId,
            activityClass = shortcut.targetActivity,
            user = Process.myUserHandle(),
            customLabel = customLabel
        )
    }

    /**
     * Launch a system shortcut
     */
    fun launchSystemShortcut(context: Context, packageName: String): Boolean {
        val shortcut = getSystemShortcut(packageName) ?: return false

        return try {
            when (shortcut.intentType) {
                IntentType.COMPONENT -> launchWithComponent(context, shortcut)
                IntentType.ACTION -> launchWithAction(context, shortcut)
                IntentType.SPECIAL -> launchWithSpecialHandling(context, shortcut)
            }
        } catch (e: Exception) {
            Log.e("SystemShortcutHelper", "Failed to launch ${shortcut.displayName}", e)
            context.showShortToast("Unable to launch ${shortcut.displayName}")
            false
        }
    }

    /**
     * Launch using ComponentName (most common)
     */
    private fun launchWithComponent(context: Context, shortcut: SystemShortcut): Boolean {
        val intent = Intent().apply {
            component = ComponentName(shortcut.targetPackage, shortcut.targetActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
        Log.d("SystemShortcutHelper", "Launched ${shortcut.displayName} via Component")
        return true
    }

    /**
     * Launch using Intent action (for special cases)
     */
    private fun launchWithAction(context: Context, shortcut: SystemShortcut): Boolean {
        when (shortcut.packageId) {
            "com.inkos.system.settings_search" -> {
                // Settings Search - try multiple approaches for compatibility
                val intent = Intent().apply {
                    action = "android.settings.APP_SEARCH_SETTINGS"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // Try the search settings action first
                try {
                    context.startActivity(intent)
                    CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
                    Log.d(
                        "SystemShortcutHelper",
                        "Launched ${shortcut.displayName} via APP_SEARCH_SETTINGS"
                    )
                    return true
                } catch (e: Exception) {
                    // Fallback to main settings
                    val fallbackIntent = Intent().apply {
                        action = "android.settings.SETTINGS"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched (fallback)")
                    Log.d(
                        "SystemShortcutHelper",
                        "Launched ${shortcut.displayName} via fallback SETTINGS"
                    )
                    return true
                }
            }

            else -> {
                // Fallback to component launch for other ACTION type shortcuts
                val intent = Intent().apply {
                    component = ComponentName(shortcut.targetPackage, shortcut.targetActivity)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
                CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
                Log.d("SystemShortcutHelper", "Launched ${shortcut.displayName} via Action")
                return true
            }
        }
    }

    /**
     * Launch with special handling (brightness dialog and other special cases)
     */
    private fun launchWithSpecialHandling(context: Context, shortcut: SystemShortcut): Boolean {
        when (shortcut.packageId) {
            "com.inkos.system.brightness_dialog" -> {
                // Brightness dialog requires special intent setup
                val intent = Intent().apply {
                    component = ComponentName(shortcut.targetPackage, shortcut.targetActivity)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    // Some devices might need additional flags
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                context.startActivity(intent)
                CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
                Log.d(
                    "SystemShortcutHelper",
                    "Launched ${shortcut.displayName} with special handling"
                )
                return true
            }

            else -> {
                // Fallback to component launch
                return launchWithComponent(context, shortcut)
            }
        }
    }

    /**
     * Get all system shortcuts as AppListItems with custom labels applied
     */
    fun getAllSystemShortcutsAsAppItems(prefs: com.github.gezimos.inkos.data.Prefs): List<AppListItem> {
        return systemShortcuts.map { shortcut ->
            val customLabel = prefs.getAppAlias("app_alias_${shortcut.packageId}")
            createAppListItem(shortcut, customLabel)
        }
    }

    /**
     * Get system shortcuts filtered by hidden status
     */
    fun getFilteredSystemShortcuts(
        prefs: com.github.gezimos.inkos.data.Prefs,
        includeHidden: Boolean = false,
        onlyHidden: Boolean = false
    ): List<AppListItem> {
        val hiddenApps = prefs.hiddenApps

        return systemShortcuts.mapNotNull { shortcut ->
            val isHidden = hiddenApps.contains("${shortcut.packageId}|${Process.myUserHandle()}")

            val shouldInclude = when {
                onlyHidden -> isHidden
                includeHidden -> true
                else -> !isHidden
            }

            if (shouldInclude) {
                val customLabel = prefs.getAppAlias("app_alias_${shortcut.packageId}")
                createAppListItem(shortcut, customLabel)
            } else {
                null
            }
        }
    }
}
