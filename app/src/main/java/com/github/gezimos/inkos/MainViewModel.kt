package com.github.gezimos.inkos

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.common.hideKeyboard
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.HomeAppUiState
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.getAppsList
import com.github.gezimos.inkos.helper.getHiddenApps
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.helper.launchSyntheticOrSystemApp
import com.github.gezimos.inkos.helper.setDefaultHomeScreen
import com.github.gezimos.inkos.helper.utils.BiometricHelper
import com.github.gezimos.inkos.services.NotificationManager
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var biometricHelper: BiometricHelper

    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)

    // setup variables with initial values
    val appList = MutableLiveData<List<AppListItem>?>()
    val hiddenApps = MutableLiveData<List<AppListItem>?>()
    val homeAppsOrder = MutableLiveData<List<AppListItem>>()  // Store actual app items

    val showClock = MutableLiveData(prefs.showClock)
    val showDate = MutableLiveData(prefs.showDate)
    val showDateBatteryCombo = MutableLiveData(prefs.showDateBatteryCombo)
    val homeAppsNum = MutableLiveData(prefs.homeAppsNum)
    val homePagesNum = MutableLiveData(prefs.homePagesNum)

    val appTheme = MutableLiveData(prefs.appTheme)
    val appColor = MutableLiveData(prefs.appColor)
    val backgroundColor = MutableLiveData(prefs.backgroundColor)
    val clockColor = MutableLiveData(prefs.clockColor)
    val batteryColor = MutableLiveData(prefs.batteryColor)
    val dateColor = MutableLiveData(prefs.dateColor)
    val quoteColor = MutableLiveData(prefs.quoteColor)
    val audioWidgetColor = MutableLiveData<Int>(prefs.audioWidgetColor)
    val appsFont = MutableLiveData(prefs.appsFont)
    val clockFont = MutableLiveData(prefs.clockFont)
    val batteryFont = MutableLiveData(prefs.batteryFont)
    val quoteFont = MutableLiveData(prefs.quoteFont)
    val notificationsFont = MutableLiveData(prefs.notificationsFont)
    val notificationFont = MutableLiveData(prefs.labelnotificationsFont)
    val statusFont = MutableLiveData(prefs.statusFont)
    val lettersFont = MutableLiveData(prefs.lettersFont)
    val lettersTitleFont = MutableLiveData(prefs.lettersTitleFont)
    val textPaddingSize = MutableLiveData(prefs.textPaddingSize)
    val appSize = MutableLiveData(prefs.appSize)
    val clockSize = MutableLiveData(prefs.clockSize)
    val batterySize = MutableLiveData(prefs.batterySize)
    val quoteSize = MutableLiveData(prefs.quoteSize)
    val quoteText = MutableLiveData(prefs.quoteText)
    val showQuote = MutableLiveData(prefs.showQuote)
    val showAudioWidget = MutableLiveData(prefs.showAudioWidgetEnabled)
    val homeBackgroundImageOpacity = MutableLiveData(prefs.homeBackgroundImageOpacity)
    val homeBackgroundImageUri = MutableLiveData(prefs.homeBackgroundImageUri)

    // --- Home screen UI state ---
    private val _homeAppsUiState = MutableLiveData<List<HomeAppUiState>>()
    val homeAppsUiState: LiveData<List<HomeAppUiState>> = _homeAppsUiState

    fun updateMediaPlaybackInfo(info: NotificationManager.NotificationInfo?) {
        // _mediaPlaybackInfo.postValue(info)
    }

    // Listen for preference changes and update LiveData
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "APP_THEME" -> appTheme.postValue(prefs.appTheme)
            "APP_COLOR" -> appColor.postValue(prefs.appColor)
            "BACKGROUND_COLOR" -> backgroundColor.postValue(prefs.backgroundColor)
            "CLOCK_COLOR" -> clockColor.postValue(prefs.clockColor)
            "BATTERY_COLOR" -> batteryColor.postValue(prefs.batteryColor)
            "DATE_COLOR" -> dateColor.postValue(prefs.dateColor)
            "QUOTE_COLOR" -> quoteColor.postValue(prefs.quoteColor)
            "AUDIO_WIDGET_COLOR" -> audioWidgetColor.postValue(prefs.audioWidgetColor)
            "APPS_FONT" -> appsFont.postValue(prefs.appsFont)
            "CLOCK_FONT" -> clockFont.postValue(prefs.clockFont)
            "BATTERY_FONT" -> batteryFont.postValue(prefs.batteryFont)
            "QUOTE_FONT" -> quoteFont.postValue(prefs.quoteFont)
            "NOTIFICATIONS_FONT" -> notificationsFont.postValue(prefs.notificationsFont)
            "NOTIFICATION_FONT" -> notificationFont.postValue(prefs.labelnotificationsFont)
            "STATUS_FONT" -> statusFont.postValue(prefs.statusFont)
            "LETTERS_FONT" -> lettersFont.postValue(prefs.lettersFont)
            "LETTERS_TITLE_FONT" -> lettersTitleFont.postValue(prefs.lettersTitleFont)
            "TEXT_PADDING_SIZE" -> textPaddingSize.postValue(prefs.textPaddingSize)
            "APP_SIZE_TEXT" -> appSize.postValue(prefs.appSize)
            "CLOCK_SIZE_TEXT" -> clockSize.postValue(prefs.clockSize)
            "BATTERY_SIZE_TEXT" -> batterySize.postValue(prefs.batterySize)
            "QUOTE_TEXT_SIZE" -> quoteSize.postValue(prefs.quoteSize)
            "QUOTE_TEXT" -> quoteText.postValue(prefs.quoteText)
            "SHOW_QUOTE" -> showQuote.postValue(prefs.showQuote)
            "SHOW_AUDIO_WIDGET" -> showAudioWidget.postValue(prefs.showAudioWidgetEnabled)
            "SHOW_DATE" -> showDate.postValue(prefs.showDate)
            "SHOW_DATE_BATTERY_COMBO" -> showDateBatteryCombo.postValue(prefs.showDateBatteryCombo)
            "HOME_BACKGROUND_IMAGE_OPACITY" -> homeBackgroundImageOpacity.postValue(prefs.homeBackgroundImageOpacity)
            "HOME_BACKGROUND_IMAGE_URI" -> homeBackgroundImageUri.postValue(prefs.homeBackgroundImageUri)
        }
    }

    init {
        prefs.sharedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onCleared() {
        prefs.sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        // Unregister any other listeners or receivers here if added in future
        super.onCleared()
    }

    // Call this to refresh home app UI state (labels, fonts, colors, badges)
    fun refreshHomeAppsUiState(context: Context) {
        val notifications =
            NotificationManager.getInstance(context).notificationInfoLiveData.value ?: emptyMap()
        val appColor = prefs.appColor
        val appFont = prefs.getFontForContext("apps")
            .getFont(context, prefs.getCustomFontPathForContext("apps"))
        val homeApps = (0 until prefs.homeAppsNum).map { i ->
            val appModel = prefs.getHomeAppModel(i)
            val customLabel = prefs.getAppAlias("app_alias_${appModel.activityPackage}")
            val label = if (customLabel.isNotEmpty()) customLabel else appModel.activityLabel
            val notificationInfo = notifications[appModel.activityPackage]
            HomeAppUiState(
                id = i,
                label = label,
                font = appFont,
                color = appColor,
                notificationInfo = notificationInfo,
                activityPackage = appModel.activityPackage // Pass unique identifier
            )
        }
        // Only post if value changed
        if (_homeAppsUiState.value != homeApps) {
            _homeAppsUiState.postValue(homeApps)
        }

        // Also refresh audio widget state to ensure it appears after launcher restart
        refreshAudioWidgetState(context)
    }

    // Refresh audio widget state from active notifications after launcher restart
    private fun refreshAudioWidgetState(context: Context) {
        try {
            val audioWidgetHelper =
                AudioWidgetHelper.getInstance(context)

            // Simply reset dismissal state to allow widget to show when NotificationService reconnects
            // The NotificationService onListenerConnected() will handle proper restoration with working MediaController
            audioWidgetHelper.resetDismissalState()
        } catch (e: Exception) {
            // Ignore errors during widget refresh
        }
    }

    fun selectedApp(fragment: Fragment, app: AppListItem, flag: AppDrawerFlag, n: Int = 0) {
        when (flag) {
            AppDrawerFlag.LaunchApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.PrivateApps -> {
                launchApp(app, fragment)
            }

            AppDrawerFlag.SetHomeApp -> {
                prefs.setHomeAppModel(n, app)
                // Immediately refresh home UI state so HomeFragment gets updated data
                refreshHomeAppsUiState(fragment.requireContext())
                findNavController(fragment).popBackStack()
            }

            AppDrawerFlag.SetSwipeLeft -> {
                prefs.appSwipeLeft = app
                prefs.swipeLeftAction = com.github.gezimos.inkos.data.Constants.Action.OpenApp
            }

            AppDrawerFlag.SetSwipeRight -> {
                prefs.appSwipeRight = app
                prefs.swipeRightAction = com.github.gezimos.inkos.data.Constants.Action.OpenApp
            }

            AppDrawerFlag.SetDoubleTap -> prefs.appDoubleTap = app
            AppDrawerFlag.SetClickClock -> { /* no-op or implement if needed */
            }

            AppDrawerFlag.SetClickDate -> {
                prefs.appClickDate = app
                prefs.clickDateAction = com.github.gezimos.inkos.data.Constants.Action.OpenApp
                findNavController(fragment).popBackStack()
            }

            AppDrawerFlag.SetQuoteWidget -> {
                prefs.appQuoteWidget = app
                findNavController(fragment).popBackStack()
            }

            AppDrawerFlag.SetSwipeUp, AppDrawerFlag.SetSwipeDown -> { /* no-op, removed */
            }
        }
    }

    // Add a function to handle OpenAppDrawer action
    fun handleGestureAction(
        fragment: Fragment,
        action: com.github.gezimos.inkos.data.Constants.Action
    ) {
        when (action) {
            com.github.gezimos.inkos.data.Constants.Action.OpenAppDrawer -> {
                // Navigate to app drawer
                findNavController(fragment).navigate(R.id.appListFragment)
            }
            // ...existing code for other actions...
            else -> { /* existing logic */
            }
        }
    }

    fun setShowClock(visibility: Boolean) {
        showClock.value = visibility
    }

    fun setDefaultLauncher(visibility: Boolean) {
        // launcherDefault.value = visibility // Removed unused LiveData
    }

    fun launchApp(appListItem: AppListItem, fragment: Fragment) {
        val packageName = appListItem.activityPackage

        // Handle synthetic and system apps
        if (launchSyntheticOrSystemApp(appContext, packageName, fragment)) {
            return
        }

        biometricHelper = BiometricHelper(fragment)
        val currentLockedApps = prefs.lockedApps

        if (currentLockedApps.contains(packageName)) {
            fragment.hideKeyboard()
            biometricHelper.startBiometricAuth(appListItem, object : BiometricHelper.CallbackApp {
                override fun onAuthenticationSucceeded(appListItem: AppListItem) {
                    launchUnlockedApp(appListItem)
                }

                override fun onAuthenticationFailed() {
                    Log.e(
                        "Authentication",
                        appContext.getString(R.string.text_authentication_failed)
                    )
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> Log.e(
                            "Authentication",
                            appContext.getString(R.string.text_authentication_cancel)
                        )

                        else -> Log.e(
                            "Authentication",
                            appContext.getString(R.string.text_authentication_error).format(
                                errorMessage,
                                errorCode
                            )
                        )
                    }
                }
            })
        } else {
            launchUnlockedApp(appListItem)
        }
    }

    private fun launchUnlockedApp(appListItem: AppListItem) {
        val packageName = appListItem.activityPackage
        val userHandle = appListItem.user
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        if (activityInfo.isNotEmpty()) {
            val component = ComponentName(packageName, activityInfo.first().name)
            launchAppWithPermissionCheck(component, packageName, userHandle, launcher)
        } else {
            appContext.showShortToast("App not found")
        }
    }

    private fun launchAppWithPermissionCheck(
        component: ComponentName,
        packageName: String,
        userHandle: UserHandle,
        launcher: LauncherApps
    ) {
        try {
            launcher.startMainActivity(component, userHandle, null, null)
            CrashHandler.logUserAction("${component.packageName} App Launched")
        } catch (_: SecurityException) {
            try {
                launcher.startMainActivity(component, Process.myUserHandle(), null, null)
                CrashHandler.logUserAction("${component.packageName} App Launched")
            } catch (_: Exception) {
                appContext.showShortToast("Unable to launch app")
            }
        } catch (_: Exception) {
            appContext.showShortToast("Unable to launch app")
        }
    }

    fun getAppList(includeHiddenApps: Boolean = true, flag: AppDrawerFlag? = null) {
        viewModelScope.launch {
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

            // Always exclude hidden apps from the app drawer, regardless of flag
            val filteredApps: MutableList<AppListItem> = apps.filter { app ->
                !hiddenAppsSet.contains("${app.activityPackage}|${app.user}")
            }.toMutableList()

            // Add synthetic apps (App Drawer, Notifications, Empty Space) that are not hidden
            val syntheticApps =
                com.github.gezimos.inkos.helper.getSyntheticApps(prefs, flag, includeHiddenApps)
            val nonShortcutSyntheticApps = syntheticApps.filterNot {
                com.github.gezimos.inkos.helper.SystemShortcutHelper.isSystemShortcut(it.activityPackage)
            }.filter { app ->
                !hiddenAppsSet.contains("${app.activityPackage}|${app.user}")
            }
            filteredApps.addAll(nonShortcutSyntheticApps)

            // Add selected system shortcuts that are not hidden
            val selectedSystemShortcuts =
                com.github.gezimos.inkos.helper.SystemShortcutHelper.getSelectedSystemShortcutsAsAppItems(
                    prefs
                )
            val visibleSystemShortcuts = selectedSystemShortcuts.filter { app ->
                !hiddenAppsSet.contains("${app.activityPackage}|${app.user}")
            }
            filteredApps.addAll(visibleSystemShortcuts)

            // Only post if value changed
            if (appList.value != filteredApps) {
                appList.postValue(filteredApps)
            }
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            val hiddenAppsList = getHiddenApps(appContext, prefs, prefs.hiddenApps)

            // Only post if value changed
            if (hiddenApps.value != hiddenAppsList) {
                hiddenApps.postValue(hiddenAppsList)
            }
        }
    }

    fun isinkosDefault() {
        val isDefault = isinkosDefault(appContext)
        // launcherDefault.value = !isDefault // Removed unused LiveData
    }

    fun resetDefaultLauncherApp(context: Context) {
        setDefaultHomeScreen(context)
    }

    fun updateAppOrder(fromPosition: Int, toPosition: Int) {
        val currentOrder = homeAppsOrder.value?.toMutableList() ?: return

        // Move the actual app object in the list
        val app = currentOrder.removeAt(fromPosition)
        currentOrder.add(toPosition, app)

        // Only post if value changed
        if (homeAppsOrder.value != currentOrder) {
            homeAppsOrder.postValue(currentOrder)
        }
        saveAppOrder(currentOrder)  // Save new order in preferences
    }

    private fun saveAppOrder(order: List<AppListItem>) {
        order.forEachIndexed { index, app ->
            prefs.setHomeAppModel(index, app)  // Save app in its new order
        }
    }

    fun loadAppOrder() {
        val savedOrder = (0 until prefs.homeAppsNum)
            .mapNotNull { i ->
                prefs.getHomeAppModel(i).let { app ->
                    // Check for custom label
                    val customLabel = prefs.getAppAlias("app_alias_${app.activityPackage}")
                    if (customLabel.isNotEmpty()) {
                        app.customLabel = customLabel
                    }
                    app
                }
            }
        homeAppsOrder.postValue(savedOrder)
    }

    // --- App Drawer actions ---
    fun refreshAppListAfterUninstall(includeHiddenApps: Boolean = false) {
        getAppList(includeHiddenApps)
    }

    fun renameApp(packageName: String, newName: String, flag: AppDrawerFlag? = null) {
        if (newName.isEmpty()) {
            prefs.removeAppAlias(packageName)
        } else {
            prefs.setAppAlias(packageName, newName)
        }
        // Refresh app list to update labels with the current flag context
        getAppList(includeHiddenApps = false, flag = flag)
        getHiddenApps()
    }

    fun hideOrShowApp(flag: AppDrawerFlag, appModel: AppListItem) {
        val newSet = mutableSetOf<String>()
        newSet.addAll(prefs.hiddenApps)
        if (flag == AppDrawerFlag.HiddenApps) {
            newSet.remove(appModel.activityPackage)
            newSet.remove(appModel.activityPackage + "|" + appModel.user.toString())
        } else {
            newSet.add(appModel.activityPackage + "|" + appModel.user.toString())
        }
        prefs.hiddenApps = newSet
        getAppList(includeHiddenApps = (flag == AppDrawerFlag.HiddenApps), flag = flag)
        getHiddenApps()
    }
}