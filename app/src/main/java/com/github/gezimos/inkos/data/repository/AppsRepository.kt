package com.github.gezimos.inkos.data.repository

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.BuildConfig
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getAppsList
import com.github.gezimos.inkos.helper.IconUtility
import com.github.gezimos.inkos.helper.PinnedShortcutUtility
import com.github.gezimos.inkos.helper.utils.ProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
class AppsRepository(application: Application) {
    private val appContext: Context = application.applicationContext
    private val prefs = Prefs(appContext)

    /** Blacklisted package names loaded from res/xml/blacklist.xml. */
    private val blacklist: Set<String> by lazy {
        val packages = mutableSetOf<String>()
        try {
            val parser = appContext.resources.getXml(R.xml.blacklist)
            while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "app") {
                    parser.getAttributeValue(null, "packageName")?.let { packages.add(it) }
                }
            }
            parser.close()
        } catch (_: Exception) {}
        packages
    }
    private val profileManager = ProfileManager(appContext)
    /** Cached private space user handle — queried once, updated on package changes. */
    @Volatile private var cachedPrivateSpaceUser: android.os.UserHandle? =
        try { profileManager.getPrivateSpaceUser() } catch (_: Exception) { null }

    private val coroutineScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Default +
        kotlinx.coroutines.SupervisorJob()
    )
    
    // ================================================================================
    // STATE FLOWS
    // ================================================================================
    
    private val _appList = MutableStateFlow<List<AppListItem>>(emptyList())
    val appList: StateFlow<List<AppListItem>> = _appList.asStateFlow()
    
    private val _hiddenAppsList = MutableStateFlow<List<AppListItem>>(emptyList())
    val hiddenAppsList: StateFlow<List<AppListItem>> = _hiddenAppsList.asStateFlow()
    
    private val _allAppsList = MutableStateFlow<List<AppListItem>>(emptyList())
    val allAppsList: StateFlow<List<AppListItem>> = _allAppsList.asStateFlow()
    
    private val _azLetters = MutableStateFlow<List<Char>>(listOf('★') + ('A'..'Z').toList())
    val azLetters: StateFlow<List<Char>> = _azLetters.asStateFlow()
    
    // Private space apps (separated from main list)
    private val _privateSpaceApps = MutableStateFlow<List<AppListItem>>(emptyList())
    val privateSpaceApps: StateFlow<List<AppListItem>> = _privateSpaceApps.asStateFlow()

    private val _hasPrivateSpace = MutableStateFlow(
        cachedPrivateSpaceUser != null && try { !profileManager.isPrivateSpaceLocked() } catch (_: Exception) { false }
    )
    val hasPrivateSpace: StateFlow<Boolean> = _hasPrivateSpace.asStateFlow()

    private val _iconCodes = MutableStateFlow<Map<String, String>>(emptyMap())
    val iconCodes: StateFlow<Map<String, String>> = _iconCodes.asStateFlow()
    
    // ================================================================================
    // PACKAGE INSTALL RECEIVER
    // ================================================================================
    
    private var packageInstallReceiver: PackageInstallReceiver? = null
    private var profileChangeReceiver: android.content.BroadcastReceiver? = null
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
                if (action == Intent.ACTION_PACKAGE_ADDED && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
                    newlyInstalled.add(packageName)
                    prefs.newlyInstalledApps = newlyInstalled
                } else if (action == Intent.ACTION_PACKAGE_REMOVED) {
                    // Remove from newly installed set when uninstalled
                    val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
                    newlyInstalled.remove(packageName)
                    prefs.newlyInstalledApps = newlyInstalled
                    // Clean up orphaned shortcut icon files
                    IconUtility.deleteShortcutIconsForPackage(context, packageName)
                }
                
                onPackageChanged()
            } catch (e: Exception) {
                // Silently ignore errors
            }
        }
    }
    
    // ================================================================================
    // SHORTCUT CHANGE LISTENER
    // ================================================================================
    
    private var shortcutChangeCallback: LauncherApps.Callback? = null
    private inner class ShortcutChangeCallback : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: android.os.UserHandle?) {
        }
        
        override fun onPackageAdded(packageName: String?, user: android.os.UserHandle?) {
        }
        
        override fun onPackageChanged(packageName: String?, user: android.os.UserHandle?) {
            try {
                this@AppsRepository.onPackageChanged()
            } catch (e: Exception) {
                Log.w("AppsRepository", "onPackageChanged callback failed", e)
            }
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: android.os.UserHandle?,
            replacing: Boolean
        ) {
            try {
                this@AppsRepository.onPackageChanged()
            } catch (e: Exception) {
                Log.w("AppsRepository", "onPackagesAvailable callback failed", e)
            }
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: android.os.UserHandle?,
            replacing: Boolean
        ) {
            try {
                this@AppsRepository.onPackageChanged()
            } catch (e: Exception) {
                Log.w("AppsRepository", "onPackagesUnavailable callback failed", e)
            }
        }

        override fun onShortcutsChanged(
            packageName: String,
            shortcuts: MutableList<android.content.pm.ShortcutInfo>,
            user: android.os.UserHandle
        ) {
            Log.d("AppsRepository", "Shortcuts changed for $packageName: ${shortcuts.size} shortcuts")
            try {
                this@AppsRepository.onPackageChanged()
            } catch (e: Exception) {
                Log.w("AppsRepository", "onShortcutsChanged callback failed", e)
            }
        }
    }
    
    // ================================================================================
    // INITIALIZATION
    // ================================================================================
    
    init {
        coroutineScope.launch {
            prefs.preferenceChangeFlow.collect { key ->
                when {
                    key == "SELECTED_APP_SHORTCUTS" ||
                    key == "notifications_enabled" ||
                    key == Constants.PrefKeys.APP_DRAWER_SEARCH_ENABLED ||
                    key == "HIDDEN_APPS" ||
                    key == Constants.PrefKeys.HIDE_HOME_APPS ||
                    key == "HOME_APPS_NUM" ||
                    key == "PINNED_SHORTCUTS" ||
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
        
        try {
            profileChangeReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    onPackageChanged()
                }
            }
            val profileFilter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            }
            activity.registerReceiver(profileChangeReceiver, profileFilter)
        } catch (_: Exception) {}

        // Register shortcut change listener (API 25+)
        registerShortcutChangeListener()
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
        
        try {
            profileChangeReceiver?.let {
                activity.unregisterReceiver(it)
                profileChangeReceiver = null
            }
        } catch (_: Exception) {}

        // Unregister shortcut change listener
        unregisterShortcutChangeListener()
    }
    private fun registerShortcutChangeListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null && shortcutChangeCallback == null) {
                    shortcutChangeCallback = ShortcutChangeCallback()
                    launcherApps.registerCallback(shortcutChangeCallback)
                    Log.d("AppsRepository", "Shortcut change listener registered")
                }
            } catch (e: Exception) {
                Log.e("AppsRepository", "Failed to register shortcut change listener", e)
            }
        }
    }
    private fun unregisterShortcutChangeListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val launcherApps = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                shortcutChangeCallback?.let {
                    launcherApps?.unregisterCallback(it)
                    shortcutChangeCallback = null
                    Log.d("AppsRepository", "Shortcut change listener unregistered")
                }
            } catch (e: Exception) {
                Log.e("AppsRepository", "Failed to unregister shortcut change listener", e)
            }
        }
    }
    
    fun onPackageChanged() {
        cachedPrivateSpaceUser = try { profileManager.getPrivateSpaceUser() } catch (_: Exception) { null }
        refreshAppList(includeHiddenApps = false, flag = null)
    }
    
    // ================================================================================
    // PUBLIC API - APP LIST MANAGEMENT
    // ================================================================================
    fun refreshAppList(includeHiddenApps: Boolean = false, flag: AppDrawerFlag? = null) {
        coroutineScope.launch {
            try {
                // Get regular installed apps (including shortcuts)
                val apps = getAppsList(
                    appContext,
                    includeRegularApps = true,
                    includeHiddenApps = includeHiddenApps,
                    includeShortcuts = true
                )
                
                // Load custom labels for each app
                apps.forEach { app ->
                    val aliasKey = if (app.isShortcut) {
                        "app_alias_${app.activityPackage}_${app.shortcutId}"
                    } else {
                        "app_alias_${app.activityPackage}"
                    }
                    val customLabel = prefs.getAppAlias(aliasKey)
                    if (customLabel.isNotEmpty()) {
                        app.customLabel = customLabel
                    }
                }
                
                val hiddenAppsSet = prefs.hiddenApps
                
                val homeAppKeys: Set<String> = if (prefs.hideHomeApps && (flag == null || flag == AppDrawerFlag.LaunchApp)) {
                    (0 until prefs.homeAppsNum).mapNotNull { index ->
                        val homeApp = prefs.getHomeAppModel(index)
                        if (homeApp.activityPackage.isNotEmpty()) {
                            getAppKey(homeApp)
                        } else {
                            null
                        }
                    }.toSet()
                } else {
                    emptySet()
                }
                
                val filteredApps: MutableList<AppListItem> = if (includeHiddenApps) {
                    apps.filter { app ->
                        val key = getAppKey(app)
                        val isHomeApp = homeAppKeys.isNotEmpty() && homeAppKeys.contains(key)
                        !isHomeApp && isShortcutInSelection(app, key)
                    }.toMutableList()
                } else {
                    apps.filter { app ->
                        val key = getAppKey(app)
                        val regularKey = "${app.activityPackage}|${app.user.toString()}"
                        val isHidden = hiddenAppsSet.contains(key) ||
                            hiddenAppsSet.contains(regularKey) ||
                            hiddenAppsSet.contains(app.activityPackage)
                        val isHomeApp = homeAppKeys.contains(key)
                        !isHidden && !isHomeApp && isShortcutInSelection(app, key)
                    }.toMutableList()
                }
                
                if (!prefs.notificationsEnabled) {
                    filteredApps.removeIf { app ->
                        app.activityPackage == BuildConfig.APPLICATION_ID &&
                        app.shortcutId == Constants.INKOS_SHORTCUT_NOTIFICATIONS
                    }
                }

                // Filter blacklisted packages
                if (blacklist.isNotEmpty()) {
                    filteredApps.removeIf { app -> blacklist.contains(app.activityPackage) }
                }
                
                val pinnedShortcuts = PinnedShortcutUtility.getPinnedShortcutsAsAppItems(appContext)
                if (pinnedShortcuts.isNotEmpty()) {
                    val visiblePinnedShortcuts = pinnedShortcuts.filter { app ->
                        val key = getAppKey(app)
                        val isHidden = if (!includeHiddenApps) hiddenAppsSet.contains(key) else false
                        val isHomeApp = homeAppKeys.contains(key)
                        isShortcutInSelection(app, key) && !isHidden && !isHomeApp
                    }
                    Log.d("AppsRepository", "Adding ${visiblePinnedShortcuts.size} pinned shortcuts to app list (${pinnedShortcuts.size - visiblePinnedShortcuts.size} filtered out)")
                    filteredApps.addAll(visiblePinnedShortcuts)
                }
                
                val privateSpaceUser = cachedPrivateSpaceUser
                val isUnlocked = privateSpaceUser != null && !profileManager.isPrivateSpaceLocked()
                _hasPrivateSpace.value = isUnlocked

                val regularApps: MutableList<AppListItem>
                val psApps: List<AppListItem>
                if (privateSpaceUser != null) {
                    val (ps, regular) = filteredApps.partition { it.user == privateSpaceUser }
                    regularApps = regular.toMutableList()
                    psApps = if (isUnlocked) ps else emptyList()
                } else {
                    regularApps = filteredApps
                    psApps = emptyList()
                }

                val azLetters = computeAzLetters(regularApps)

                // Update State
                if (!includeHiddenApps) {
                    _appList.value = regularApps
                    _privateSpaceApps.value = psApps
                    _azLetters.value = azLetters
                } else {
                    _allAppsList.value = filteredApps
                }
            } catch (e: Exception) {
                Log.e("AppsRepository", "Error refreshing app list", e)
            }
        }
    }
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
    fun launchSyntheticOrSystemApp(
        context: Context, packageName: String, fragment: Fragment, shortcutId: String? = null
    ): Boolean {
        when {
            packageName == BuildConfig.APPLICATION_ID && shortcutId != null -> {
                try {
                    when (shortcutId) {
                        Constants.INKOS_SHORTCUT_APP_DRAWER -> {
                            if (fragment.findNavController().currentDestination?.id == R.id.mainFragment) {
                                fragment.findNavController()
                                    .navigate(R.id.action_mainFragment_to_appListFragment)
                            } else {
                                fragment.findNavController().navigate(R.id.appListFragment)
                            }
                        }
                        Constants.INKOS_SHORTCUT_NOTIFICATIONS ->
                            fragment.findNavController().navigate(R.id.lettersFragment)
                        Constants.INKOS_SHORTCUT_SIMPLE_TRAY ->
                            fragment.findNavController().navigate(R.id.simpleTrayFragment)
                        Constants.INKOS_SHORTCUT_HUB ->
                            fragment.findNavController().navigate(R.id.hubFragment)
                        Constants.INKOS_SHORTCUT_RECENTS ->
                            fragment.findNavController().navigate(R.id.recentsFragment)
                        Constants.INKOS_SHORTCUT_SETTINGS ->
                            fragment.findNavController().navigate(R.id.settingsFragment)
                        else -> return false
                    }
                } catch (_: Exception) {
                    return false
                }
                return true
            }

            else -> return false
        }
    }
    
    // ================================================================================
    // HIDDEN APPS SECTION
    // ================================================================================
    private suspend fun getHiddenAppsList(hiddenAppsSet: Set<String>): List<AppListItem> {
        val hiddenApps = mutableListOf<AppListItem>()

        val allApps = getAppsList(appContext, includeRegularApps = true, includeHiddenApps = true, includeShortcuts = true)

        for (hiddenApp in hiddenAppsSet) {
            try {
                val parts = hiddenApp.split("|")
                val packageName = parts[0]
                
                val isAppShortcut = parts.size == 3 &&
                    !packageName.startsWith("com.inkos.")

                when {
                    isAppShortcut -> {
                        val shortcutId = parts[1]
                        val userHandleString = parts[2]
                        val app = allApps.find { item ->
                            item.activityPackage == packageName &&
                            item.shortcutId == shortcutId &&
                            item.user.toString() == userHandleString
                        }
                        
                        app?.let {
                            val customLabel = prefs.getAppAlias("app_alias_${it.activityPackage}_${it.shortcutId}")
                            if (customLabel.isNotEmpty()) {
                                it.customLabel = customLabel
                            }
                            hiddenApps.add(it)
                        }
                    }

                    else -> {
                        val app = if (parts.size > 1) {
                            allApps.find { item ->
                                item.activityPackage == packageName &&
                                item.shortcutId == null && // Regular app, not a shortcut
                                item.user.toString() == parts[1]
                            }
                        } else {
                            allApps.find { item ->
                                item.activityPackage == packageName &&
                                item.shortcutId == null // Regular app, not a shortcut
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
    // PINNED SHORTCUTS SECTION
    // ================================================================================
    fun isPinnedShortcut(app: AppListItem): Boolean {
        if (app.shortcutId == null) return false
        return PinnedShortcutUtility.isPinned(appContext, app)
    }
    fun unpinShortcut(app: AppListItem): Boolean {
        if (app.shortcutId == null) return false
        val removed = PinnedShortcutUtility.removePinnedShortcut(
            context = appContext,
            packageName = app.activityPackage,
            shortcutId = app.shortcutId,
            userHandle = app.user
        )
        if (removed) {
            prefs.notifyPinnedShortcutsChanged()
        }
        return removed
    }
    
    // ================================================================================
    // UTILITY METHODS
    // ================================================================================
    private fun getAppKey(app: AppListItem): String {
        return if (app.isShortcut && app.shortcutId != null) {
            "${app.activityPackage}|${app.shortcutId}|${app.user.toString()}"
        } else {
            "${app.activityPackage}|${app.user.toString()}"
        }
    }
    
    /** Whether a shortcut is selected for display (regular apps always true). */
    private fun isShortcutInSelection(app: AppListItem, key: String): Boolean = when {
        !app.isShortcut -> true
        else -> prefs.isAppShortcutSelected(appContext, key)
    }
    private fun computeAzLetters(apps: List<AppListItem>): List<Char> {
        return try {
            val present = BooleanArray(26)
            for (app in apps) {
                val ch = app.normalizedFirstChar ?: continue
                present[ch - 'A'] = true
            }
            val letters = mutableListOf<Char>()
            for (i in 0 until 26) if (present[i]) letters.add(('A' + i))
            listOf('★') + letters
        } catch (_: Exception) {
            listOf('★') + ('A'..'Z').toList()
        }
    }
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
