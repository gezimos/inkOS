package com.github.gezimos.inkos.data.repository

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getAppsList
import com.github.gezimos.inkos.helper.IconUtility
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Centralized repository for all app list management including:
 * - Regular installed apps
 * - Synthetic apps (App Drawer, Notifications, Recents, SimpleTray)
 * - Note: Search is a UI element, not a synthetic app
 * - System shortcuts
 * - Hidden apps
 * - Package install/uninstall events
 * - Preference change listeners
 */
class AppsRepository(application: Application) {
    private val appContext: Context = application.applicationContext
    private val prefs = Prefs(appContext)
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Main + 
        kotlinx.coroutines.SupervisorJob()
    )
    
    // ================================================================================
    // STATE FLOWS
    // ================================================================================
    
    private val _appList = MutableStateFlow<List<AppListItem>>(emptyList())
    val appList: StateFlow<List<AppListItem>> = _appList.asStateFlow()
    
    private val _hiddenAppsList = MutableStateFlow<List<AppListItem>>(emptyList())
    val hiddenAppsList: StateFlow<List<AppListItem>> = _hiddenAppsList.asStateFlow()
    
    private val _azLetters = MutableStateFlow<List<Char>>(listOf('★') + ('A'..'Z').toList())
    val azLetters: StateFlow<List<Char>> = _azLetters.asStateFlow()
    
    // Cached icon codes for home apps (keyed by app label)
    private val _iconCodes = MutableStateFlow<Map<String, String>>(emptyMap())
    val iconCodes: StateFlow<Map<String, String>> = _iconCodes.asStateFlow()
    
    // ================================================================================
    // PACKAGE INSTALL RECEIVER
    // ================================================================================
    
    private var packageInstallReceiver: PackageInstallReceiver? = null
    
    /**
     * Internal BroadcastReceiver that listens for package installation and replacement events.
     */
    private inner class PackageInstallReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val packageName = intent.data?.schemeSpecificPart

            if (action != Intent.ACTION_PACKAGE_ADDED &&
                action != Intent.ACTION_PACKAGE_REPLACED &&
                action != Intent.ACTION_PACKAGE_REMOVED) {
                return
            }

            if (packageName.isNullOrEmpty()) {
                return
            }

            try {
                // Track newly installed apps (not replaced or removed)
                if (action == Intent.ACTION_PACKAGE_ADDED && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
                    newlyInstalled.add(packageName)
                    prefs.newlyInstalledApps = newlyInstalled
                } else if (action == Intent.ACTION_PACKAGE_REMOVED) {
                    // Remove from newly installed set when uninstalled
                    val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
                    newlyInstalled.remove(packageName)
                    prefs.newlyInstalledApps = newlyInstalled
                }
                
                onPackageChanged()
            } catch (e: Exception) {
                // Silently ignore errors
            }
        }
    }
    
    // ================================================================================
    // INITIALIZATION
    // ================================================================================
    
    init {
        // Listen to preference changes that affect app list
        coroutineScope.launch {
            prefs.preferenceChangeFlow.collect { key ->
                when {
                    key == "SELECTED_SYSTEM_SHORTCUTS" ||
                    key == "SYSTEM_SHORTCUTS_ENABLED" ||
                    key == "notifications_enabled" ||
                    key == Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED ||
                    key == "HIDDEN_APPS" ||
                    key == Constants.PrefKeys.HIDE_HOME_APPS ||
                    key == "HOME_APPS_NUM" ||
                    key.startsWith("app_alias_") -> {
                        refreshAppList(includeHiddenApps = false, flag = null)
                    }
                }
            }
        }
    }
    
    // ================================================================================
    // PUBLIC API - PACKAGE RECEIVER
    // ================================================================================
    
    fun registerPackageReceiver(activity: android.app.Activity) {
        try {
            packageInstallReceiver = PackageInstallReceiver()
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
            activity.registerReceiver(packageInstallReceiver, filter)
        } catch (_: Exception) {
            // Silently ignore registration errors
        }
    }
    
    fun unregisterPackageReceiver(activity: android.app.Activity) {
        try {
            packageInstallReceiver?.let {
                activity.unregisterReceiver(it)
                packageInstallReceiver = null
            }
        } catch (_: Exception) {
            // Silently ignore unregistration errors
        }
    }
    
    fun onPackageChanged() {
        refreshAppList(includeHiddenApps = false, flag = null)
    }
    
    // ================================================================================
    // PUBLIC API - APP LIST MANAGEMENT
    // ================================================================================
    
    /**
     * Refresh the app list with all filtering and synthetic apps applied
     */
    fun refreshAppList(includeHiddenApps: Boolean = false, flag: AppDrawerFlag? = null) {
        coroutineScope.launch {
            try {
                // Get regular installed apps
                val apps = getAppsList(
                    appContext,
                    includeRegularApps = true,
                    includeHiddenApps = includeHiddenApps
                )
                
                // Load custom labels for each app
                apps.forEach { app ->
                    val customLabel = prefs.getAppAlias("app_alias_${app.activityPackage}")
                    if (customLabel.isNotEmpty()) {
                        app.customLabel = customLabel
                    }
                }
                
                val hiddenAppsSet = prefs.hiddenApps
                
                // Build a set of activityPackage|user for apps currently set on the home screen
                val homeAppKeys: Set<String> = if (prefs.hideHomeApps) {
                    (0 until prefs.homeAppsNum).mapNotNull { index ->
                        val homeApp = prefs.getHomeAppModel(index)
                        if (homeApp.activityPackage.isNotEmpty()) {
                            "${homeApp.activityPackage}|${homeApp.user.toString()}"
                        } else {
                            null
                        }
                    }.toSet()
                } else {
                    emptySet()
                }
                
                // Filter hidden apps based on includeHiddenApps parameter
                val filteredApps: MutableList<AppListItem> = if (includeHiddenApps) {
                    apps.filter { app ->
                        if (homeAppKeys.isNotEmpty()) {
                            !homeAppKeys.contains("${app.activityPackage}|${app.user.toString()}")
                        } else {
                            true
                        }
                    }.toMutableList()
                } else {
                    apps.filter { app ->
                        val key = "${app.activityPackage}|${app.user.toString()}"
                        // Check both new format (package|user) and old format (package only) for backwards compatibility
                        val isHidden = hiddenAppsSet.contains(key) || hiddenAppsSet.contains(app.activityPackage)
                        val isHomeApp = homeAppKeys.contains(key)
                        !isHidden && !isHomeApp
                    }.toMutableList()
                }
                
                // Add synthetic apps (excluding system shortcuts which are added separately)
                val syntheticApps = getSyntheticApps(flag, includeHiddenApps)
                val nonShortcutSyntheticApps = syntheticApps.filterNot {
                    isSystemShortcut(it.activityPackage)
                }.let { apps ->
                    if (includeHiddenApps) {
                        apps.filter { app ->
                            if (homeAppKeys.isNotEmpty()) {
                                !homeAppKeys.contains("${app.activityPackage}|${app.user.toString()}")
                            } else {
                                true
                            }
                        }
                    } else {
                        apps.filter { app ->
                            val key = "${app.activityPackage}|${app.user.toString()}"
                            // Check both new format (package|user) and old format (package only) for backwards compatibility
                            val isHidden = hiddenAppsSet.contains(key) || hiddenAppsSet.contains(app.activityPackage)
                            val isHomeApp = homeAppKeys.contains(key)
                            !isHidden && !isHomeApp
                        }
                    }
                }
                
                // Add non-shortcut synthetic apps (Search is now a UI element, not an app)
                filteredApps.addAll(nonShortcutSyntheticApps)
                
                // Add selected system shortcuts
                // If shortcuts are explicitly selected, show them (even if systemShortcutsEnabled is false)
                val selected = prefs.selectedSystemShortcuts
                Log.d("AppsRepository", "refreshAppList: systemShortcutsEnabled=${prefs.systemShortcutsEnabled}, selected count=${selected.size}, selected=$selected")
                
                if (selected.isNotEmpty()) {
                    val selectedSystemShortcuts = getSelectedSystemShortcutsAsAppItems()
                    Log.d("AppsRepository", "getSelectedSystemShortcutsAsAppItems returned ${selectedSystemShortcuts.size} items")
                    val visibleSystemShortcuts = if (includeHiddenApps) {
                        selectedSystemShortcuts.filter { app ->
                            if (homeAppKeys.isNotEmpty()) {
                                !homeAppKeys.contains("${app.activityPackage}|${app.user.toString()}")
                            } else {
                                true
                            }
                        }
                    } else {
                        selectedSystemShortcuts.filter { app ->
                            val key = "${app.activityPackage}|${app.user.toString()}"
                            // Check both new format (package|user) and old format (package only) for backwards compatibility
                            val isHidden = hiddenAppsSet.contains(key) || hiddenAppsSet.contains(app.activityPackage)
                            val isHomeApp = homeAppKeys.contains(key)
                            val shouldInclude = !isHidden && !isHomeApp
                            if (!shouldInclude) {
                                Log.d("AppsRepository", "Filtering out ${app.activityPackage}: isHidden=$isHidden, isHomeApp=$isHomeApp")
                            }
                            shouldInclude
                        }
                    }
                    Log.d("AppsRepository", "Adding ${visibleSystemShortcuts.size} visible system shortcuts to app list (total filtered apps: ${filteredApps.size})")
                    filteredApps.addAll(visibleSystemShortcuts)
                    Log.d("AppsRepository", "After adding shortcuts, filtered apps count: ${filteredApps.size}")
                } else {
                    Log.d("AppsRepository", "System shortcuts not added: selected empty=${selected.isEmpty()}")
                }
                
                // Compute azLetters from the final filtered list
                val azLetters = computeAzLetters(filteredApps)
                
                // Update State
                // Only update _appList when includeHiddenApps is false (for regular app drawer)
                // When includeHiddenApps is true, we're viewing hidden apps, so don't pollute the regular app list
                if (!includeHiddenApps) {
                    _appList.value = filteredApps
                    _azLetters.value = azLetters
                } else {
                    // When viewing hidden apps, refresh the hidden apps list instead
                    // Don't touch _appList - it should remain with only non-hidden apps
                    refreshHiddenApps()
                    // Don't update azLetters when viewing hidden apps - keep the regular app drawer's letters
                }
            } catch (e: Exception) {
                Log.e("AppsRepository", "Error refreshing app list", e)
            }
        }
    }
    
    /**
     * Refresh hidden apps list
     */
    fun refreshHiddenApps() {
        coroutineScope.launch {
            try {
                val hiddenAppsList = getHiddenAppsList(prefs.hiddenApps)
                _hiddenAppsList.value = hiddenAppsList
            } catch (e: Exception) {
                Log.e("AppsRepository", "Error refreshing hidden apps", e)
            }
        }
    }
    
    // ================================================================================
    // PUBLIC API - LAUNCH HELPERS
    // ================================================================================
    
    /**
     * Handle launching synthetic and system apps
     * Returns true if the app was handled (launched or is a special case), false if it should be handled normally
     */
    fun launchSyntheticOrSystemApp(context: Context, packageName: String, fragment: Fragment): Boolean {
        when {
            // Handle synthetic "App Drawer" item
            packageName == Constants.INTERNAL_APP_DRAWER -> {
                try {
                    if (fragment.findNavController().currentDestination?.id == R.id.mainFragment) {
                        fragment.findNavController()
                            .navigate(R.id.action_mainFragment_to_appListFragment)
                    }
                } catch (_: Exception) {
                    fragment.findNavController().navigate(R.id.appListFragment)
                }
                return true
            }

            // Search is now a UI element, not a synthetic app - no special handling needed

            // Handle synthetic "Notifications" item
            packageName == Constants.INTERNAL_NOTIFICATIONS -> {
                try {
                    fragment.findNavController().navigate(R.id.notificationsFragment)
                } catch (_: Exception) {
                    fragment.findNavController().navigate(R.id.notificationsFragment)
                }
                return true
            }

            // Handle synthetic "Simple Tray" item
            packageName == Constants.INTERNAL_SIMPLE_TRAY -> {
                try {
                    fragment.findNavController().navigate(R.id.simpleTrayFragment)
                } catch (_: Exception) {
                    fragment.findNavController().navigate(R.id.simpleTrayFragment)
                }
                return true
            }

            // Handle synthetic "Recents" item
            packageName == Constants.INTERNAL_RECENTS -> {
                try {
                    fragment.findNavController().navigate(R.id.recentsFragment)
                } catch (_: Exception) {
                    fragment.findNavController().navigate(R.id.recentsFragment)
                }
                return true
            }

            // Handle system shortcuts
            isSystemShortcut(packageName) -> {
                return launchSystemShortcut(context, packageName)
            }

            else -> return false
        }
    }
    
    // ================================================================================
    // SYSTEM SHORTCUTS SECTION
    // ================================================================================
    
    /**
     * Data class representing a system shortcut definition
     */
    data class SystemShortcut(
        val packageId: String,
        val displayName: String,
        val targetPackage: String,
        val targetActivity: String,
        val intentType: SystemShortcutIntentType = SystemShortcutIntentType.COMPONENT
    )
    
    /**
     * Types of intents used for launching system activities
     */
    enum class SystemShortcutIntentType {
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
            intentType = SystemShortcutIntentType.ACTION
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
            intentType = SystemShortcutIntentType.SPECIAL
        ),
        SystemShortcut(
            packageId = "com.inkos.system.privacy",
            displayName = "Privacy",
            targetPackage = "com.android.settings",
            targetActivity = "com.android.settings.Settings\$PrivacyDashboardActivity"
        ),
        SystemShortcut(
            packageId = "com.inkos.system.default_apps",
            displayName = "Default Apps",
            targetPackage = "com.android.settings",
            targetActivity = "android.settings.MANAGE_DEFAULT_APPS_SETTINGS",
            intentType = SystemShortcutIntentType.ACTION
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
     * Get only selected system shortcuts as AppListItems
     */
    fun getSelectedSystemShortcutsAsAppItems(): List<AppListItem> {
        val selected = prefs.selectedSystemShortcuts
        Log.d("AppsRepository", "getSelectedSystemShortcutsAsAppItems: selected=$selected, systemShortcuts count=${systemShortcuts.size}")
        val filtered = systemShortcuts.filter { selected.contains(it.packageId) }
        Log.d("AppsRepository", "Filtered to ${filtered.size} shortcuts")
        return filtered.map { shortcut ->
            val customLabel = prefs.getAppAlias("app_alias_${shortcut.packageId}")
            createSystemShortcutAppListItem(shortcut, customLabel)
        }
    }
    
    /**
     * Get all system shortcuts as AppListItems with custom labels applied
     */
    fun getAllSystemShortcutsAsAppItems(): List<AppListItem> {
        return systemShortcuts.map { shortcut ->
            val customLabel = prefs.getAppAlias("app_alias_${shortcut.packageId}")
            createSystemShortcutAppListItem(shortcut, customLabel)
        }
    }
    
    /**
     * Get system shortcuts filtered by hidden status
     */
    fun getFilteredSystemShortcuts(
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
                createSystemShortcutAppListItem(shortcut, customLabel)
            } else {
                null
            }
        }
    }
    
    /**
     * Launch a system shortcut
     */
    fun launchSystemShortcut(context: Context, packageName: String): Boolean {
        val shortcut = getSystemShortcut(packageName) ?: return false

        return try {
            when (shortcut.intentType) {
                SystemShortcutIntentType.COMPONENT -> launchSystemShortcutWithComponent(context, shortcut)
                SystemShortcutIntentType.ACTION -> launchSystemShortcutWithAction(context, shortcut)
                SystemShortcutIntentType.SPECIAL -> launchSystemShortcutWithSpecialHandling(context, shortcut)
            }
        } catch (e: Exception) {
            Log.e("AppsRepository", "Failed to launch ${shortcut.displayName}", e)
            context.showShortToast("Unable to launch ${shortcut.displayName}")
            false
        }
    }
    
    /**
     * Create AppListItem for a system shortcut
     */
    private fun createSystemShortcutAppListItem(shortcut: SystemShortcut, customLabel: String = ""): AppListItem {
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
     * Launch using ComponentName (most common)
     */
    private fun launchSystemShortcutWithComponent(context: Context, shortcut: SystemShortcut): Boolean {
        val intent = Intent().apply {
            component = ComponentName(shortcut.targetPackage, shortcut.targetActivity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
        Log.d("AppsRepository", "Launched ${shortcut.displayName} via Component")
        return true
    }
    
    /**
     * Launch using Intent action (for special cases)
     */
    private fun launchSystemShortcutWithAction(context: Context, shortcut: SystemShortcut): Boolean {
        when (shortcut.packageId) {
            "com.inkos.system.default_apps" -> {
                val intent = Intent().apply {
                    action = shortcut.targetActivity
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
                Log.d("AppsRepository", "Launched ${shortcut.displayName} via Action")
                return true
            }
            
            "com.inkos.system.settings_search" -> {
                val intent = Intent().apply {
                    action = "android.settings.APP_SEARCH_SETTINGS"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(intent)
                    CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
                    Log.d("AppsRepository", "Launched ${shortcut.displayName} via APP_SEARCH_SETTINGS")
                    return true
                } catch (e: Exception) {
                    val fallbackIntent = Intent().apply {
                        action = "android.settings.SETTINGS"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched (fallback)")
                    Log.d("AppsRepository", "Launched ${shortcut.displayName} via fallback SETTINGS")
                    return true
                }
            }

            else -> {
                val intent = Intent().apply {
                    component = ComponentName(shortcut.targetPackage, shortcut.targetActivity)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)
                CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
                Log.d("AppsRepository", "Launched ${shortcut.displayName} via Action")
                return true
            }
        }
    }
    
    /**
     * Launch with special handling (brightness dialog and other special cases)
     */
    private fun launchSystemShortcutWithSpecialHandling(context: Context, shortcut: SystemShortcut): Boolean {
        when (shortcut.packageId) {
            "com.inkos.system.brightness_dialog" -> {
                val intent = Intent().apply {
                    component = ComponentName(shortcut.targetPackage, shortcut.targetActivity)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                context.startActivity(intent)
                CrashHandler.logUserAction("${shortcut.displayName} System Shortcut Launched")
                Log.d("AppsRepository", "Launched ${shortcut.displayName} with special handling")
                return true
            }

            else -> {
                return launchSystemShortcutWithComponent(context, shortcut)
            }
        }
    }
    
    // ================================================================================
    // SYNTHETIC APPS SECTION
    // ================================================================================
    
    /**
     * Get synthetic apps (internal apps + system shortcuts if enabled)
     */
    private fun getSyntheticApps(flag: AppDrawerFlag?, includeHiddenApps: Boolean): List<AppListItem> {
        val syntheticApps = mutableListOf<AppListItem>()

        // Add internal synthetic apps (App Drawer, Notifications, Search, Recents)
        if (flag == AppDrawerFlag.SetHomeApp ||
            flag == AppDrawerFlag.LaunchApp ||
            flag == AppDrawerFlag.HiddenApps ||
            flag == null
        ) {
            syntheticApps.addAll(getInternalSyntheticApps(flag, includeHiddenApps))
        }

        // NOTE: System shortcuts are NOT added here - they're added separately in refreshAppList()
        // based on selectedSystemShortcuts. This method only returns internal synthetic apps.

        return syntheticApps
    }
    
    /**
     * Get internal synthetic apps (App Drawer, Notifications, Recents, SimpleTray)
     * Note: Search is now a UI element, not a synthetic app
     */
    private fun getInternalSyntheticApps(
        flag: AppDrawerFlag?,
        includeHiddenApps: Boolean
    ): List<AppListItem> {
        val apps = mutableListOf<AppListItem>()
        val hiddenApps = prefs.hiddenApps

        // App Drawer synthetic app
        val appDrawerPackage = Constants.INTERNAL_APP_DRAWER
        val isAppDrawerHidden = hiddenApps.contains("${appDrawerPackage}|${Process.myUserHandle()}")

        if (shouldIncludeSyntheticApp(flag, isAppDrawerHidden, includeHiddenApps)) {
            val customLabel = prefs.getAppAlias("app_alias_$appDrawerPackage")
            apps.add(
                AppListItem(
                    activityLabel = "App Drawer",
                    activityPackage = appDrawerPackage,
                    activityClass = "com.github.gezimos.inkos.ui.AppsFragment",
                    user = Process.myUserHandle(),
                    customLabel = customLabel
                )
            )
        }

        // Notifications synthetic app (if enabled)
        if (prefs.notificationsEnabled) {
            val notificationsPackage = Constants.INTERNAL_NOTIFICATIONS
            val isNotificationsHidden =
                hiddenApps.contains("${notificationsPackage}|${Process.myUserHandle()}")

            if (shouldIncludeSyntheticApp(flag, isNotificationsHidden, includeHiddenApps)) {
                val customLabel = prefs.getAppAlias("app_alias_$notificationsPackage")
                apps.add(
                    AppListItem(
                        activityLabel = "Notifications",
                        activityPackage = notificationsPackage,
                        activityClass = "com.github.gezimos.inkos.ui.notifications.NotificationsFragment",
                        user = Process.myUserHandle(),
                        customLabel = customLabel
                    )
                )
            }
        }

        // Simple Tray synthetic app (always included unless hidden)
        run {
            val simpleTrayPackage = Constants.INTERNAL_SIMPLE_TRAY
            val isSimpleTrayHidden = hiddenApps.contains("${simpleTrayPackage}|${Process.myUserHandle()}")

            if (shouldIncludeSyntheticApp(flag, isSimpleTrayHidden, includeHiddenApps)) {
                val customLabel = prefs.getAppAlias("app_alias_$simpleTrayPackage")
                apps.add(
                    AppListItem(
                        activityLabel = "Simple Tray",
                        activityPackage = simpleTrayPackage,
                        activityClass = "com.github.gezimos.inkos.ui.notifications.SimpleTrayFragment",
                        user = Process.myUserHandle(),
                        customLabel = customLabel
                    )
                )
            }
        }

        // Recents synthetic app (always included unless hidden)
        run {
            val recentsPackage = Constants.INTERNAL_RECENTS
            val isRecentsHidden = hiddenApps.contains("${recentsPackage}|${Process.myUserHandle()}")

            if (shouldIncludeSyntheticApp(flag, isRecentsHidden, includeHiddenApps)) {
                val customLabel = prefs.getAppAlias("app_alias_$recentsPackage")
                apps.add(
                    AppListItem(
                        activityLabel = "Recents",
                        activityPackage = recentsPackage,
                        activityClass = "com.github.gezimos.inkos.ui.RecentsFragment",
                        user = Process.myUserHandle(),
                        customLabel = customLabel
                    )
                )
            }
        }

        return apps
    }
    
    /**
     * Get system shortcuts for the given context
     */
    private fun getSystemShortcutsForContext(
        flag: AppDrawerFlag?,
        includeHiddenApps: Boolean
    ): List<AppListItem> {
        return when (flag) {
            AppDrawerFlag.HiddenApps -> {
                getFilteredSystemShortcuts(includeHidden = false, onlyHidden = true)
            }

            AppDrawerFlag.SetHomeApp -> {
                getFilteredSystemShortcuts(includeHidden = false, onlyHidden = false)
            }

            else -> {
                getFilteredSystemShortcuts(includeHidden = includeHiddenApps, onlyHidden = false)
            }
        }
    }
    
    /**
     * Determine if a synthetic app should be included based on context and hidden status
     */
    private fun shouldIncludeSyntheticApp(
        flag: AppDrawerFlag?,
        isHidden: Boolean,
        includeHiddenApps: Boolean
    ): Boolean {
        return when (flag) {
            AppDrawerFlag.SetHomeApp -> !isHidden || includeHiddenApps
            AppDrawerFlag.LaunchApp -> !isHidden
            AppDrawerFlag.HiddenApps -> isHidden
            null -> !isHidden || includeHiddenApps
            else -> false
        }
    }
    
    // ================================================================================
    // HIDDEN APPS SECTION
    // ================================================================================
    
    /**
     * Get all hidden apps (both synthetic and regular apps) for hidden apps management
     */
    private suspend fun getHiddenAppsList(hiddenAppsSet: Set<String>): List<AppListItem> {
        val hiddenApps = mutableListOf<AppListItem>()

        // Get all installed apps to match against
        val allApps = getAppsList(appContext, includeRegularApps = true, includeHiddenApps = true)

        for (hiddenApp in hiddenAppsSet) {
            try {
                val parts = hiddenApp.split("|")
                val packageName = parts[0]

                when {
                    // Handle internal synthetic apps
                    packageName == Constants.INTERNAL_APP_DRAWER -> {
                        val customLabel = prefs.getAppAlias("app_alias_$packageName")
                        hiddenApps.add(
                            AppListItem(
                                activityLabel = "App Drawer",
                                activityPackage = packageName,
                                activityClass = "com.github.gezimos.inkos.ui.AppsFragment",
                                user = Process.myUserHandle(),
                                customLabel = customLabel
                            )
                        )
                    }

                    packageName == Constants.INTERNAL_NOTIFICATIONS -> {
                        val customLabel = prefs.getAppAlias("app_alias_$packageName")
                        hiddenApps.add(
                            AppListItem(
                                activityLabel = "Notifications",
                                activityPackage = packageName,
                                activityClass = "com.github.gezimos.inkos.ui.notifications.NotificationsFragment",
                                user = Process.myUserHandle(),
                                customLabel = customLabel
                            )
                        )
                    }

                    packageName == Constants.INTERNAL_SIMPLE_TRAY -> {
                        val customLabel = prefs.getAppAlias("app_alias_$packageName")
                        hiddenApps.add(
                            AppListItem(
                                activityLabel = "Simple Tray",
                                activityPackage = packageName,
                                activityClass = "com.github.gezimos.inkos.ui.notifications.SimpleTrayFragment",
                                user = Process.myUserHandle(),
                                customLabel = customLabel
                            )
                        )
                    }

                    packageName == Constants.INTERNAL_RECENTS -> {
                        val customLabel = prefs.getAppAlias("app_alias_$packageName")
                        hiddenApps.add(
                            AppListItem(
                                activityLabel = "Recents",
                                activityPackage = packageName,
                                activityClass = "com.github.gezimos.inkos.ui.RecentsFragment",
                                user = Process.myUserHandle(),
                                customLabel = customLabel
                            )
                        )
                    }

                    // Handle system shortcuts
                    isSystemShortcut(packageName) -> {
                        val shortcut = getSystemShortcut(packageName)
                        if (shortcut != null) {
                            val customLabel = prefs.getAppAlias("app_alias_$packageName")
                            val item = createSystemShortcutAppListItem(shortcut, customLabel)
                            hiddenApps.add(item)
                        }
                    }

                    // Handle regular apps
                    else -> {
                        val app = if (parts.size > 1) {
                            allApps.find { app ->
                                app.activityPackage == packageName &&
                                        app.user.toString() == parts[1]
                            }
                        } else {
                            allApps.find { app ->
                                app.activityPackage == packageName
                            }
                        }

                        app?.let {
                            val customLabel = prefs.getAppAlias("app_alias_${it.activityPackage}")
                            if (customLabel.isNotEmpty()) {
                                it.customLabel = customLabel
                            }
                            hiddenApps.add(it)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppsRepository", "Error processing hidden app: $hiddenApp", e)
                continue
            }
        }

        return hiddenApps
    }
    
    // ================================================================================
    // UTILITY METHODS
    // ================================================================================
    
    /**
     * Helper to compute A-Z letters from app list in one pass
     */
    private fun computeAzLetters(apps: List<AppListItem>): List<Char> {
        return try {
            val present = BooleanArray(26)
            for (app in apps) {
                val label = try { app.label } catch (_: Exception) { null } ?: continue
                if (label.isEmpty()) continue
                val normalized = try { 
                    com.github.gezimos.inkos.ui.compose.SearchHelper.normalizeString(label) 
                } catch (_: Exception) { "" }
                val first = normalized.trimStart().firstOrNull() ?: continue
                if (first in 'A'..'Z') {
                    present[first - 'A'] = true
                }
            }
            val letters = mutableListOf<Char>()
            for (i in 0 until 26) if (present[i]) letters.add(('A' + i))
            listOf('★') + letters
        } catch (_: Exception) {
            listOf('★') + ('A'..'Z').toList()
        }
    }
    
    /**
     * Compute and cache icon codes for a list of app labels.
     * This is called when home apps change to pre-compute icon codes.
     * 
     * @param labels List of app labels to generate icon codes for
     * @param showIcons Whether icons are enabled (if false, clears the cache)
     */
    fun updateIconCodes(labels: List<String>, showIcons: Boolean) {
        coroutineScope.launch {
            try {
                if (showIcons && labels.isNotEmpty()) {
                    val codes = IconUtility.generateIconCodes(labels)
                    _iconCodes.value = codes
                } else {
                    _iconCodes.value = emptyMap()
                }
            } catch (e: Exception) {
                Log.e("AppsRepository", "Error computing icon codes", e)
                _iconCodes.value = emptyMap()
            }
        }
    }
    
    // ================================================================================
    // SINGLETON
    // ================================================================================
    
    companion object {
        @Volatile
        private var INSTANCE: AppsRepository? = null
        
        fun getInstance(application: Application): AppsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppsRepository(application).also { INSTANCE = it }
            }
        }
    }
}
