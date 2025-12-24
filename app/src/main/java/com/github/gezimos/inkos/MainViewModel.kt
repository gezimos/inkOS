package com.github.gezimos.inkos

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.HomeAppUiState
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.style.resolveThemeColors
import com.github.gezimos.inkos.data.Constants.PrefKeys
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.ui.compose.HomeUiRenderState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val homeApps: List<HomeAppUiState> = emptyList(),
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showDateBatteryCombo: Boolean = false,
    val showNotificationCount: Boolean = false,
    val showQuote: Boolean = false,
    val showAmPm: Boolean = true,
    val showSecondClock: Boolean = false,
    val secondClockOffsetHours: Int = 0,
    val quoteText: String = "",
    val showAudioWidget: Boolean = false,
    val homeAppsNum: Int = 8,
    val homePagesNum: Int = 1,
    val appTheme: Constants.Theme = Constants.Theme.Light,
    val textColor: Int = 0,
    val backgroundColor: Int = 0,
    val appsFont: Constants.FontFamily = Constants.FontFamily.System,
    val clockFont: Constants.FontFamily = Constants.FontFamily.System,
    val quoteFont: Constants.FontFamily = Constants.FontFamily.System,
    val notificationsFont: Constants.FontFamily = Constants.FontFamily.System,
    val notificationFont: Constants.FontFamily = Constants.FontFamily.System,
    val statusFont: Constants.FontFamily = Constants.FontFamily.System,
    val lettersFont: Constants.FontFamily = Constants.FontFamily.System,
    val lettersTitleFont: Constants.FontFamily = Constants.FontFamily.System,
    val textPaddingSize: Int = 0,
    val appSize: Int = 0,
    val clockSize: Int = 0,
    val quoteSize: Int = 0,
    val settingsSize: Int = 16,
    val showStatusBar: Boolean = false,
    val showNavigationBar: Boolean = false,
    val backgroundOpacity: Int = 0,
    val dateText: String = "",
    val batteryText: String = "",
    val isCharging: Boolean = false,
    // New fields for HomeUI refactor
    val homeAlignment: Int = 0,
    val homeAppsYOffset: Int = 0,
    val topWidgetMargin: Int = 0,
    val bottomWidgetMargin: Int = 0,
    val hideHomeApps: Boolean = false,
    val dateFont: Constants.FontFamily = Constants.FontFamily.System,
    val dateSize: Int = 0,
    val allCapsApps: Boolean = false,
    val smallCapsApps: Boolean = false,
    val showNotificationBadge: Boolean = false,
    val hapticFeedback: Boolean = false,
    val pageIndicatorVisible: Boolean = false,
    val textIslands: Boolean = false,
    val textIslandsInverted: Boolean = false,
    val textIslandsShape: Int = 0,
    val showIcons: Boolean = false,
    // Notification Settings
    val pushNotificationsEnabled: Boolean = false,
    val showNotificationText: Boolean = false,
    val showMediaIndicator: Boolean = false,
    val showMediaName: Boolean = false,
    val showNotificationSenderName: Boolean = false,
    val showNotificationGroupName: Boolean = false,
    val showNotificationMessage: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val homeAppCharLimit: Int = 0,
    val clearConversationOnAppOpen: Boolean = false,
    val allowedBadgeNotificationApps: Set<String> = emptySet(),
    val allowedNotificationApps: Set<String> = emptySet(),
    val allowedSimpleTrayApps: Set<String> = emptySet(),
    // SimpleTray Settings
    val notificationsPerPage: Int = 3,
    val enableBottomNav: Boolean = true,
    // Extras Settings
    val einkRefreshEnabled: Boolean = false,
    val einkRefreshHomeButtonOnly: Boolean = false,
    val einkRefreshDelay: Int = 0,
    val useVolumeKeysForPages: Boolean = false,
    val selectedSystemShortcuts: Set<String> = emptySet(),
    val einkHelperEnabled: Boolean = false,
    // Advanced Settings
    val homeLocked: Boolean = false,
    val settingsLocked: Boolean = false,
    val longPressAppInfoEnabled: Boolean = false,
    val homeReset: Boolean = false,
    val extendHomeAppsArea: Boolean = false,
    // Gesture Settings
    val shortSwipeThresholdRatio: Float = 0f,
    val longSwipeThresholdRatio: Float = 0f,
    val doubleTapAction: Constants.Action = Constants.Action.Disabled,
    val clickClockAction: Constants.Action = Constants.Action.Disabled,
    val clickDateAction: Constants.Action = Constants.Action.Disabled,
    val swipeLeftAction: Constants.Action = Constants.Action.Disabled,
    val swipeRightAction: Constants.Action = Constants.Action.Disabled,
    val swipeUpAction: Constants.Action = Constants.Action.Disabled,
    val swipeDownAction: Constants.Action = Constants.Action.Disabled,
    val quoteAction: Constants.Action = Constants.Action.Disabled,
    val edgeSwipeBackEnabled: Boolean = true,

    // Drawer Settings
    val appDrawerSize: Int = Constants.DEFAULT_APP_DRAWER_SIZE,
    val appDrawerGap: Int = Constants.DEFAULT_APP_DRAWER_GAP,
    val appDrawerAlignment: Int = 0, // 0: Left, 1: Center, 2: Right
    val appDrawerAzFilter: Boolean = false,
    val appDrawerSearchEnabled: Boolean = true,
    val appDrawerAutoLaunch: Boolean = true,
    val appDrawerAutoShowKeyboard: Boolean = false,
    
    // Additional fields for centralized HomeUiRenderState
    val labelnotificationsTextSize: Int = 0,
    val clockCustomFontPath: String? = null,
    val dateCustomFontPath: String? = null,
    val quoteCustomFontPath: String? = null,
    val notificationCustomFontPath: String? = null,
    val currentPage: Int = 0,
    
    // Runtime clock data (updated by ticker)
    val clockText: String = "",
    val amPmText: String = "",
    val is24Hour: Boolean = false,
    val clockMode: Int = 0,
    val secondClockText: String = "",
    val secondAmPmText: String = ""
)

data class AppsDrawerUiState(
    val appList: List<AppListItem> = emptyList(),
    val hiddenApps: List<AppListItem> = emptyList(),
    // Pre-computed A-Z letters for instant AZ sidebar rendering
    val azLetters: List<Char> = listOf('★') + ('A'..'Z').toList(),
    // New fields for AppsUI refactor
    val appsFont: Constants.FontFamily = Constants.FontFamily.System,
    val appDrawerSize: Int = 0,
    val appDrawerGap: Int = Constants.DEFAULT_APP_DRAWER_GAP,
    val appDrawerAlignment: Int = 0,
    val allCapsApps: Boolean = false,
    val smallCapsApps: Boolean = false,
    val showNotificationBadge: Boolean = false,
    val hapticFeedback: Boolean = false,
    
    val lockedApps: Set<String> = emptySet(),
    val newlyInstalledApps: Set<String> = emptySet(),
    val appTheme: Constants.Theme = Constants.Theme.Light,
    val appDrawerAzFilter: Boolean = false,
    val customFontPath: String? = null,
    val appDrawerSearchEnabled: Boolean = true,
    val appDrawerAutoLaunch: Boolean = true
    ,
    val appDrawerAutoShowKeyboard: Boolean = false,
    val extendHomeAppsArea: Boolean = false,
    val textIslandsShape: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)
    private val appsRepository = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
    
    // Simple cache for appsPerPage - cleared when dependencies change
    private var appsPerPageCache: Int? = null
    
    fun getCachedAppsPerPage(): Int? = appsPerPageCache
    
    fun cacheAppsPerPage(value: Int) {
        appsPerPageCache = value
    }
    
    fun clearAppsPerPageCache() {
        appsPerPageCache = null
    }

    // ================================================================================
    // STATE MANAGEMENT (StateFlow - Single Source of Truth)
    // ================================================================================

    private val _homeUiState = MutableStateFlow(HomeUiState(
        showClock = prefs.showClock,
        showDate = prefs.showDate,
        showDateBatteryCombo = prefs.showDateBatteryCombo,
        showNotificationCount = prefs.showNotificationCount,
        showQuote = prefs.showQuote,
        showAmPm = prefs.showAmPm,
        showSecondClock = prefs.showSecondClock,
        secondClockOffsetHours = prefs.secondClockOffsetHours,
        quoteText = prefs.quoteText,
        showAudioWidget = prefs.showAudioWidgetEnabled,
        homeAppsNum = prefs.homeAppsNum,
        homePagesNum = prefs.homePagesNum,
        appTheme = prefs.appTheme,
        textColor = try { resolveThemeColors(appContext).first } catch (_: Exception) { android.graphics.Color.BLACK },
        backgroundColor = try { resolveThemeColors(appContext).second } catch (_: Exception) { android.graphics.Color.WHITE },
        appsFont = prefs.appsFont,
        clockFont = prefs.clockFont,
        quoteFont = prefs.quoteFont,
        notificationsFont = prefs.notificationsFont,
        notificationFont = prefs.labelnotificationsFont,
        statusFont = prefs.statusFont,
        lettersFont = prefs.lettersFont,
        lettersTitleFont = prefs.lettersTitleFont,
        textPaddingSize = prefs.textPaddingSize,
        appSize = prefs.appSize,
        clockSize = prefs.clockSize,
        quoteSize = prefs.quoteSize,
        backgroundOpacity = prefs.backgroundOpacity,
        showStatusBar = prefs.showStatusBar,
        showNavigationBar = prefs.showNavigationBar,
        // Initialize new fields
        homeAlignment = prefs.homeAlignment,
        homeAppsYOffset = prefs.homeAppsYOffset,
        topWidgetMargin = prefs.topWidgetMargin,
        bottomWidgetMargin = prefs.bottomWidgetMargin,
        hideHomeApps = prefs.hideHomeApps,
        dateFont = prefs.dateFont,
        dateSize = prefs.dateSize,
        appDrawerGap = prefs.appDrawerGap,
        appDrawerSize = prefs.appDrawerSize,
        appDrawerAlignment = prefs.appDrawerAlignment,
        appDrawerAzFilter = prefs.appDrawerAzFilter,
        appDrawerSearchEnabled = prefs.appDrawerSearchEnabled,
        appDrawerAutoLaunch = prefs.appDrawerAutoLaunch,
        appDrawerAutoShowKeyboard = prefs.appDrawerAutoShowKeyboard,
        allCapsApps = prefs.allCapsApps,
        smallCapsApps = prefs.smallCapsApps,
        showNotificationBadge = prefs.showNotificationBadge,
        hapticFeedback = prefs.hapticFeedback,
        pageIndicatorVisible = prefs.homePager,
        textIslands = prefs.textIslands,
        textIslandsInverted = prefs.textIslandsInverted,
        textIslandsShape = prefs.textIslandsShape,
        showIcons = prefs.showIcons,
        // Notification Settings
        pushNotificationsEnabled = prefs.pushNotificationsEnabled,
        showNotificationText = prefs.showNotificationText,
        showMediaIndicator = prefs.showMediaIndicator,
        showMediaName = prefs.showMediaName,
        showNotificationSenderName = prefs.showNotificationSenderName,
        showNotificationGroupName = prefs.showNotificationGroupName,
        showNotificationMessage = prefs.showNotificationMessage,
        notificationsEnabled = prefs.notificationsEnabled,
        homeAppCharLimit = prefs.homeAppCharLimit,
        clearConversationOnAppOpen = prefs.clearConversationOnAppOpen,
        allowedBadgeNotificationApps = prefs.allowedBadgeNotificationApps,
        allowedNotificationApps = prefs.allowedNotificationApps,
        allowedSimpleTrayApps = prefs.allowedSimpleTrayApps,
        // SimpleTray Settings
        notificationsPerPage = prefs.notificationsPerPage,
        enableBottomNav = prefs.enableBottomNav,
        // Extras Settings
        einkRefreshEnabled = prefs.einkRefreshEnabled,
        einkRefreshHomeButtonOnly = prefs.einkRefreshHomeButtonOnly,
        einkRefreshDelay = prefs.einkRefreshDelay,
        useVolumeKeysForPages = prefs.useVolumeKeysForPages,
        selectedSystemShortcuts = prefs.selectedSystemShortcuts,
        einkHelperEnabled = prefs.einkHelperEnabled,
        // Advanced Settings
        homeLocked = prefs.homeLocked,
        settingsLocked = prefs.settingsLocked,
        longPressAppInfoEnabled = prefs.longPressAppInfoEnabled,
        homeReset = prefs.homeReset,
        extendHomeAppsArea = prefs.extendHomeAppsArea,
        // Gesture Settings
        shortSwipeThresholdRatio = prefs.shortSwipeThresholdRatio,
        longSwipeThresholdRatio = prefs.longSwipeThresholdRatio,
        // Edge swipe back enabled
        edgeSwipeBackEnabled = prefs.edgeSwipeBackEnabled,
        doubleTapAction = prefs.doubleTapAction,
        clickClockAction = prefs.clickClockAction,
        clickDateAction = prefs.clickDateAction,
        swipeLeftAction = prefs.swipeLeftAction,
        swipeRightAction = prefs.swipeRightAction,
        swipeUpAction = prefs.swipeUpAction,
        swipeDownAction = prefs.swipeDownAction,
        quoteAction = prefs.quoteAction,
        // Initialize new centralized fields
        labelnotificationsTextSize = prefs.labelnotificationsTextSize,
        clockCustomFontPath = try { prefs.getCustomFontPathForContext("clock") } catch (_: Exception) { null },
        dateCustomFontPath = try { prefs.getCustomFontPathForContext("date") } catch (_: Exception) { null },
        quoteCustomFontPath = try { prefs.getCustomFontPathForContext("quote") } catch (_: Exception) { null },
        notificationCustomFontPath = try { prefs.getCustomFontPathForContext("notification") } catch (_: Exception) { null },
        currentPage = 0,
        clockMode = prefs.clockMode
    ))
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    // Consolidated render state combining all data sources for HomeFragment
    val homeRenderState: StateFlow<HomeUiRenderState> = combine(
        _homeUiState,
        NotificationManager.getInstance(appContext).notificationInfoState,
        NotificationManager.getInstance(appContext).conversationNotificationsState,
        AudioWidgetHelper.getInstance(appContext).mediaPlayerState,
        com.github.gezimos.inkos.services.NotificationService.sbnState,
        appsRepository.iconCodes
    ) { values ->
        val homeUi = values[0] as HomeUiState
        val notifications = values[1] as Map<String, NotificationManager.NotificationInfo>
        val conversationNotifications = values[2] as Map<String, NotificationManager.NotificationInfo>
        val mediaInfo = values[3] as AudioWidgetHelper.MediaPlayerInfo?
        val rawNotifications = values[4] as List<android.service.notification.StatusBarNotification>
        val iconCodes = values[5] as Map<String, String>
        val appsPerPage = if (homeUi.homePagesNum > 0) {
            kotlin.math.ceil(homeUi.homeApps.size.toDouble() / homeUi.homePagesNum).toInt().coerceAtLeast(1)
        } else {
            homeUi.homeApps.size.coerceAtLeast(1)
        }
        
        // PERFORMANCE FIX: Compute notification count using cached prefs reference
        // (avoid creating new Prefs instance on every combine emission)
        val notificationCount = run {
            val allowed = homeUi.allowedNotificationApps  // Use cached allowlist from HomeUiState
            val notificationManager = NotificationManager.getInstance(appContext)
            
            // Filter raw notifications exactly like SimpleTrayFragment
            val notificationsFromSbn = rawNotifications.count { sbn ->
                // Filter out ALL media notifications (CATEGORY_TRANSPORT) - we'll use AudioWidgetHelper instead
                if (sbn.notification.category == android.app.Notification.CATEGORY_TRANSPORT) {
                    return@count false
                }
                
                // Filter out summary notifications
                !notificationManager.isNotificationSummary(sbn) &&
                // Apply allowlist if configured (empty allowlist = show all)
                (allowed.isEmpty() || allowed.contains(sbn.packageName))
            }
            
            // Add fake media notification if media is playing/paused
            val mediaCount = if (mediaInfo != null) 1 else 0
            
            notificationsFromSbn + mediaCount
        }
        
        HomeUiRenderState(
            homeApps = homeUi.homeApps,
            clockText = homeUi.clockText,
            dateText = homeUi.dateText,
            amPmText = homeUi.amPmText,
            is24Hour = homeUi.is24Hour,
            isCharging = homeUi.isCharging,
            showClock = homeUi.showClock,
            showDate = homeUi.showDate,
            showBattery = homeUi.showDateBatteryCombo,
            batteryText = homeUi.batteryText,
            showNotificationCount = homeUi.showNotificationCount,
            notificationCount = notificationCount,
            showQuote = homeUi.showQuote,
            quoteText = homeUi.quoteText,
            backgroundColor = homeUi.backgroundColor,
            backgroundOpacity = homeUi.backgroundOpacity,
            textColor = homeUi.textColor,
            appsPerPage = appsPerPage,
            totalPages = homeUi.homePagesNum,
            currentPage = homeUi.currentPage,
            notifications = notifications,
            mediaInfo = mediaInfo,
            mediaWidgetColor = homeUi.textColor,
            showMediaWidget = homeUi.showAudioWidget,
            showMediaIndicator = homeUi.showMediaIndicator,
            showMediaName = homeUi.showMediaName,
            clockFont = homeUi.clockFont,
            quoteFont = homeUi.quoteFont,
            clockSize = homeUi.clockSize,
            quoteSize = homeUi.quoteSize,
            dateFont = homeUi.dateFont,
            dateSize = homeUi.dateSize,
            clockMode = homeUi.clockMode,
            homeAlignment = homeUi.homeAlignment,
            homeAppsYOffset = homeUi.homeAppsYOffset,
            bottomWidgetMargin = homeUi.bottomWidgetMargin,
            topWidgetMargin = homeUi.topWidgetMargin,
            pageIndicatorVisible = homeUi.pageIndicatorVisible,
            pageIndicatorColor = homeUi.textColor,
            appPadding = homeUi.textPaddingSize,
            textIslands = homeUi.textIslands,
            textIslandsInverted = homeUi.textIslandsInverted,
            textIslandsShape = homeUi.textIslandsShape,
            showIcons = homeUi.showIcons,
            iconCodes = iconCodes,
            appTextSize = homeUi.appSize.toFloat(),
            hideHomeApps = homeUi.hideHomeApps,
            appDrawerGap = homeUi.appDrawerGap,
            showAmPm = homeUi.showAmPm,
            showSecondClock = homeUi.showSecondClock,
            secondClockText = homeUi.secondClockText,
            secondAmPmText = homeUi.secondAmPmText,
            secondClockOffsetHours = homeUi.secondClockOffsetHours,
            allCapsApps = homeUi.allCapsApps,
            smallCapsApps = homeUi.smallCapsApps,
            showNotificationBadge = homeUi.showNotificationBadge,
            showNotificationText = homeUi.showNotificationText,
            showNotificationSenderName = homeUi.showNotificationSenderName,
            showNotificationGroupName = homeUi.showNotificationGroupName,
            showNotificationMessage = homeUi.showNotificationMessage,
            homeAppCharLimit = homeUi.homeAppCharLimit,
            notificationTextSize = homeUi.labelnotificationsTextSize,
            notificationFont = homeUi.notificationFont,
            clockCustomFontPath = homeUi.clockCustomFontPath,
            dateCustomFontPath = homeUi.dateCustomFontPath,
            quoteCustomFontPath = homeUi.quoteCustomFontPath,
            notificationCustomFontPath = homeUi.notificationCustomFontPath,
            extendHomeAppsArea = homeUi.extendHomeAppsArea
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiRenderState(
            homeApps = emptyList(),
            clockText = "",
            dateText = "",
            amPmText = "",
            is24Hour = false,
            isCharging = false,
            showClock = true,
            showDate = true,
            showBattery = false,
            batteryText = "",
            showNotificationCount = false,
            notificationCount = 0,
            showQuote = false,
            quoteText = "",
            backgroundColor = android.graphics.Color.WHITE,
            backgroundOpacity = 255,
            textColor = android.graphics.Color.BLACK,
            appsPerPage = 1,
            totalPages = 1,
            currentPage = 0,
            notifications = emptyMap(),
            mediaInfo = null,
            mediaWidgetColor = android.graphics.Color.BLACK,
            showMediaWidget = false,
            showMediaIndicator = false,
            showMediaName = false,
            clockFont = Constants.FontFamily.System,
            quoteFont = Constants.FontFamily.System,
            clockSize = 40,
            quoteSize = 16,
            dateFont = Constants.FontFamily.System,
            dateSize = 18,
            clockMode = 0,
            homeAlignment = 0,
            homeAppsYOffset = 0,
            bottomWidgetMargin = 0,
            topWidgetMargin = 0,
            pageIndicatorVisible = false,
            pageIndicatorColor = android.graphics.Color.BLACK,
            appPadding = 0,
            appTextSize = 16f,
            hideHomeApps = false,
            appDrawerGap = 0,
            showAmPm = true,
            showSecondClock = false,
            secondClockText = "",
            secondAmPmText = "",
            secondClockOffsetHours = 0,
            allCapsApps = false,
            smallCapsApps = false,
            showNotificationBadge = false,
            showNotificationText = false,
            showNotificationSenderName = false,
            showNotificationGroupName = false,
            showNotificationMessage = false,
            homeAppCharLimit = 0,
            notificationTextSize = 16,
            notificationFont = Constants.FontFamily.System,
            textIslands = false,
            textIslandsInverted = false,
            textIslandsShape = 0,
            showIcons = false,
            iconCodes = emptyMap(),
            clockCustomFontPath = null,
            dateCustomFontPath = null,
            quoteCustomFontPath = null,
            notificationCustomFontPath = null,
            extendHomeAppsArea = false
        )
    )

    private val _homeAppsNum = MutableStateFlow(prefs.homeAppsNum)

    private val _homePagesNum = MutableStateFlow(prefs.homePagesNum)

    // API to update counts from UI
    fun setHomeAppsNum(value: Int) {
        prefs.homeAppsNum = value
        _homeAppsNum.value = value
        _homeUiState.value = _homeUiState.value.copy(homeAppsNum = value)
    }

    fun setHomePagesNum(value: Int) {
        prefs.homePagesNum = value
        _homePagesNum.value = value
        _homeUiState.value = _homeUiState.value.copy(homePagesNum = value)
    }

    fun setEdgeSwipeBackEnabled(enabled: Boolean) {
        try {
            prefs.edgeSwipeBackEnabled = enabled
            _homeUiState.value = _homeUiState.value.copy(edgeSwipeBackEnabled = enabled)
        } catch (_: Exception) {}
    }

    private val _appsDrawerUiState = MutableStateFlow(AppsDrawerUiState(
        appsFont = prefs.appsFont,
        customFontPath = try { prefs.getCustomFontPathForContext("apps") } catch (_: Exception) { null },
        appDrawerSize = prefs.appDrawerSize,
        appDrawerGap = prefs.appDrawerGap,
        appDrawerAlignment = prefs.appDrawerAlignment,
        appDrawerAzFilter = prefs.appDrawerAzFilter,
        allCapsApps = prefs.allCapsApps,
        smallCapsApps = prefs.smallCapsApps,
        showNotificationBadge = prefs.showNotificationBadge,
        hapticFeedback = prefs.hapticFeedback,
        
        lockedApps = prefs.lockedApps,
        newlyInstalledApps = prefs.newlyInstalledApps,
        appTheme = prefs.appTheme,
        appDrawerSearchEnabled = prefs.appDrawerSearchEnabled,
        appDrawerAutoLaunch = prefs.appDrawerAutoLaunch,
        appDrawerAutoShowKeyboard = prefs.appDrawerAutoShowKeyboard,
        extendHomeAppsArea = prefs.extendHomeAppsArea,
        textIslandsShape = prefs.textIslandsShape
    ))
    val appsDrawerUiState: StateFlow<AppsDrawerUiState> = _appsDrawerUiState.asStateFlow()

    // Invalidate cached appsPerPage only on first launch so first render recalculates
    init {
        try {
            if (!prefs.initialLaunchCompleted) {
                prefs.invalidateAppsPerPageCache()
                prefs.initialLaunchCompleted = true
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    // ==============================================================================
    // PREFERENCE LISTENING (collect Prefs StateFlows to update StateFlow UI state)
    // ================================================================================

    init {
        // Collect theme and color flows so UI updates immediately when tokens change.
        viewModelScope.launch {
            prefs.appThemeFlow.collect { theme ->
                _homeUiState.value = _homeUiState.value.copy(appTheme = theme)
            }
        }

        viewModelScope.launch {
            prefs.textColorFlow.collect { color ->
                _homeUiState.value = _homeUiState.value.copy(textColor = color)
            }
        }

        viewModelScope.launch {
            prefs.backgroundColorFlow.collect { color ->
                _homeUiState.value = _homeUiState.value.copy(backgroundColor = color)
            }
        }

        // Edge swipe back preference -> update HomeUiState immediately
        viewModelScope.launch {
            prefs.edgeSwipeBackEnabledFlow.collect { enabled ->
                _homeUiState.value = _homeUiState.value.copy(edgeSwipeBackEnabled = enabled)
            }
        }

        // Listen for explicit refresh requests originating from Prefs
        viewModelScope.launch {
            prefs.forceRefreshHomeFlow.collect {
                try {
                    refreshHomeAppsUiState(appContext)
                } catch (_: Exception) {
                    // ignore errors during refresh
                }
            }
        }

        // Observe repository app list and update UI state
        // Combine all three flows to update state atomically
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                appsRepository.appList,
                appsRepository.hiddenAppsList,
                appsRepository.azLetters
            ) { appList, hiddenApps, azLetters ->
                Triple(appList, hiddenApps, azLetters)
            }.collect { (appList, hiddenApps, azLetters) ->
                // Clear cache when app list changes (apps added/removed)
                val previousAppListSize = _appsDrawerUiState.value.appList.size
                if (appList.size != previousAppListSize) {
                    clearAppsPerPageCache()
                }
                _appsDrawerUiState.value = _appsDrawerUiState.value.copy(
                    appList = appList,
                    hiddenApps = hiddenApps,
                    azLetters = azLetters
                )
            }
        }
        
        // Preload app list for instant app drawer opening
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
    }

    private fun updateStateFromPreference(key: String) {
        val currentState = _homeUiState.value
        _homeUiState.value = when (key) {
            PrefKeys.APP_THEME -> currentState.copy(appTheme = prefs.appTheme)
            PrefKeys.TEXT_COLOR -> try { currentState.copy(textColor = resolveThemeColors(appContext).first) } catch (_: Exception) { currentState.copy(textColor = android.graphics.Color.BLACK) }
            PrefKeys.BACKGROUND_COLOR -> try { currentState.copy(backgroundColor = resolveThemeColors(appContext).second) } catch (_: Exception) { currentState.copy(backgroundColor = android.graphics.Color.WHITE) }
            PrefKeys.STATUS_BAR -> currentState.copy(showStatusBar = prefs.showStatusBar)
            PrefKeys.NAVIGATION_BAR -> currentState.copy(showNavigationBar = prefs.showNavigationBar)
            PrefKeys.TEXT_SIZE_SETTINGS -> currentState.copy(settingsSize = prefs.settingsSize)
            PrefKeys.APPS_FONT -> currentState.copy(appsFont = prefs.appsFont)
            PrefKeys.CLOCK_FONT -> currentState.copy(clockFont = prefs.clockFont)
            PrefKeys.QUOTE_FONT -> currentState.copy(quoteFont = prefs.quoteFont)
            PrefKeys.NOTIFICATIONS_FONT -> currentState.copy(notificationsFont = prefs.notificationsFont)
            PrefKeys.NOTIFICATION_FONT -> currentState.copy(notificationFont = prefs.labelnotificationsFont)
            PrefKeys.STATUS_FONT -> currentState.copy(statusFont = prefs.statusFont)
            PrefKeys.LETTERS_FONT -> currentState.copy(lettersFont = prefs.lettersFont)
            PrefKeys.LETTERS_TITLE_FONT -> currentState.copy(lettersTitleFont = prefs.lettersTitleFont)
            PrefKeys.TEXT_PADDING_SIZE -> currentState.copy(textPaddingSize = prefs.textPaddingSize)
            PrefKeys.APP_SIZE_TEXT -> currentState.copy(appSize = prefs.appSize)
            PrefKeys.CLOCK_SIZE_TEXT -> currentState.copy(clockSize = prefs.clockSize)
            PrefKeys.QUOTE_TEXT_SIZE -> currentState.copy(quoteSize = prefs.quoteSize)
            PrefKeys.QUOTE_TEXT -> currentState.copy(quoteText = prefs.quoteText)
            PrefKeys.SHOW_QUOTE -> currentState.copy(showQuote = prefs.showQuote)
            PrefKeys.SHOW_AUDIO_WIDGET -> currentState.copy(showAudioWidget = prefs.showAudioWidgetEnabled)
            PrefKeys.SHOW_DATE -> currentState.copy(showDate = prefs.showDate)
            PrefKeys.SHOW_CLOCK -> currentState.copy(showClock = prefs.showClock)
            PrefKeys.SHOW_AM_PM -> currentState.copy(showAmPm = prefs.showAmPm)
            PrefKeys.SHOW_SECOND_CLOCK -> currentState.copy(showSecondClock = prefs.showSecondClock)
            PrefKeys.SECOND_CLOCK_OFFSET_HOURS -> currentState.copy(secondClockOffsetHours = prefs.secondClockOffsetHours)
            PrefKeys.CLOCK_MODE -> currentState.copy(clockMode = prefs.clockMode)
            PrefKeys.SHOW_DATE_BATTERY_COMBO -> currentState.copy(showDateBatteryCombo = prefs.showDateBatteryCombo)
            PrefKeys.SHOW_NOTIFICATION_COUNT -> currentState.copy(showNotificationCount = prefs.showNotificationCount)
            PrefKeys.BACKGROUND_OPACITY -> currentState.copy(backgroundOpacity = prefs.backgroundOpacity)
            // Update new fields
            PrefKeys.HOME_ALIGNMENT -> currentState.copy(homeAlignment = prefs.homeAlignment)
            PrefKeys.HOME_APPS_Y_OFFSET -> currentState.copy(homeAppsYOffset = prefs.homeAppsYOffset)
            PrefKeys.TOP_WIDGET_MARGIN -> currentState.copy(topWidgetMargin = prefs.topWidgetMargin)
            PrefKeys.BOTTOM_WIDGET_MARGIN -> currentState.copy(bottomWidgetMargin = prefs.bottomWidgetMargin)
            PrefKeys.HIDE_HOME_APPS -> currentState.copy(hideHomeApps = prefs.hideHomeApps)
            PrefKeys.DATE_FONT -> currentState.copy(dateFont = prefs.dateFont)
            PrefKeys.DATE_SIZE_TEXT -> currentState.copy(dateSize = prefs.dateSize)
            PrefKeys.APP_DRAWER_GAP -> currentState.copy(appDrawerGap = prefs.appDrawerGap)
            PrefKeys.APP_DRAWER_SIZE -> currentState.copy(appDrawerSize = prefs.appDrawerSize)
            PrefKeys.APP_DRAWER_ALIGNMENT -> currentState.copy(appDrawerAlignment = prefs.appDrawerAlignment)
            PrefKeys.APP_DRAWER_AZ_FILTER -> currentState.copy(appDrawerAzFilter = prefs.appDrawerAzFilter)
            
            PrefKeys.APP_DRAWER_SEARCH_ENABLED -> currentState.copy(appDrawerSearchEnabled = prefs.appDrawerSearchEnabled)
            PrefKeys.APP_DRAWER_AUTO_LAUNCH -> currentState.copy(appDrawerAutoLaunch = prefs.appDrawerAutoLaunch)
            PrefKeys.APP_DRAWER_AUTO_SHOW_KEYBOARD -> currentState.copy(appDrawerAutoShowKeyboard = prefs.appDrawerAutoShowKeyboard)
            PrefKeys.ALL_CAPS_APPS -> currentState.copy(allCapsApps = prefs.allCapsApps)
            PrefKeys.SMALL_CAPS_APPS -> currentState.copy(smallCapsApps = prefs.smallCapsApps)
            PrefKeys.SHOW_NOTIFICATION_BADGE -> currentState.copy(showNotificationBadge = prefs.showNotificationBadge)
            PrefKeys.HAPTIC_FEEDBACK -> currentState.copy(hapticFeedback = prefs.hapticFeedback)
            PrefKeys.HOME_PAGER -> currentState.copy(pageIndicatorVisible = prefs.homePager)
            PrefKeys.TEXT_ISLANDS -> currentState.copy(textIslands = prefs.textIslands)
            PrefKeys.TEXT_ISLANDS_INVERTED -> currentState.copy(textIslandsInverted = prefs.textIslandsInverted)
            PrefKeys.TEXT_ISLANDS_SHAPE -> currentState.copy(textIslandsShape = prefs.textIslandsShape)
            PrefKeys.SHOW_ICONS -> currentState.copy(showIcons = prefs.showIcons)
            else -> currentState
        }

        // Update AppsDrawerUiState
        val currentAppsState = _appsDrawerUiState.value
        _appsDrawerUiState.value = when (key) {
            PrefKeys.APPS_FONT -> {
                clearAppsPerPageCache()
                currentAppsState.copy(
                    appsFont = prefs.appsFont,
                    customFontPath = try { prefs.getCustomFontPathForContext("apps") } catch (_: Exception) { null }
                )
            }
            PrefKeys.APP_DRAWER_SIZE -> {
                clearAppsPerPageCache()
                currentAppsState.copy(appDrawerSize = prefs.appDrawerSize)
            }
            PrefKeys.APP_DRAWER_GAP -> {
                clearAppsPerPageCache()
                currentAppsState.copy(appDrawerGap = prefs.appDrawerGap)
            }
            PrefKeys.APP_DRAWER_ALIGNMENT -> currentAppsState.copy(appDrawerAlignment = prefs.appDrawerAlignment)
            PrefKeys.ALL_CAPS_APPS -> currentAppsState.copy(allCapsApps = prefs.allCapsApps)
            PrefKeys.SMALL_CAPS_APPS -> currentAppsState.copy(smallCapsApps = prefs.smallCapsApps)
            PrefKeys.SHOW_NOTIFICATION_BADGE -> currentAppsState.copy(showNotificationBadge = prefs.showNotificationBadge)
            PrefKeys.HAPTIC_FEEDBACK -> currentAppsState.copy(hapticFeedback = prefs.hapticFeedback)
            
            PrefKeys.LOCKED_APPS -> currentAppsState.copy(lockedApps = prefs.lockedApps)
            "NEWLY_INSTALLED_APPS" -> currentAppsState.copy(newlyInstalledApps = prefs.newlyInstalledApps)
            PrefKeys.APP_THEME -> currentAppsState.copy(appTheme = prefs.appTheme)
            PrefKeys.APP_DRAWER_AZ_FILTER -> currentAppsState.copy(appDrawerAzFilter = prefs.appDrawerAzFilter)
            PrefKeys.APP_DRAWER_SEARCH_ENABLED -> {
                clearAppsPerPageCache()
                currentAppsState.copy(appDrawerSearchEnabled = prefs.appDrawerSearchEnabled)
            }
            PrefKeys.APP_DRAWER_AUTO_LAUNCH -> currentAppsState.copy(appDrawerAutoLaunch = prefs.appDrawerAutoLaunch)
            PrefKeys.APP_DRAWER_AUTO_SHOW_KEYBOARD -> currentAppsState.copy(appDrawerAutoShowKeyboard = prefs.appDrawerAutoShowKeyboard)
            PrefKeys.TEXT_ISLANDS_SHAPE -> currentAppsState.copy(textIslandsShape = prefs.textIslandsShape)
            else -> currentAppsState
        }
    }

    init {
        // Collect generic preference change keys and update derived state accordingly
        viewModelScope.launch {
            prefs.preferenceChangeFlow.collect { key ->
                try {
                    updateStateFromPreference(key)
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
    }

    // ================================================================================
    // CLOCK TICKER (updates clock-related fields in HomeUiState every 60s)
    // ================================================================================

    init {
        viewModelScope.launch {
            while (true) {
                updateClockState()
                kotlinx.coroutines.delay(60_000L)
            }
        }
    }

    private fun updateClockState() {
        // Determine effective 24h mode based on user preference (clockMode)
        // clockMode: 0=System, 1=Force24, 2=Force12
        val prefMode = _homeUiState.value.clockMode
        val is24HourSystem = android.text.format.DateFormat.is24HourFormat(appContext)
        val effectiveIs24Hour = when (prefMode) {
            1 -> true
            2 -> false
            else -> is24HourSystem
        }
        val clockPattern = if (effectiveIs24Hour) "HH:mm" else "hh:mm"
        val timeFormatter = java.text.SimpleDateFormat(clockPattern, java.util.Locale.getDefault())
        val now = java.util.Date()
        val clockText = timeFormatter.format(now)
        val amPmText = if (effectiveIs24Hour || !_homeUiState.value.showAmPm) {
            ""
        } else {
            try {
                java.text.SimpleDateFormat("a", java.util.Locale.getDefault()).format(now)
            } catch (_: Exception) {
                ""
            }
        }

        var secondClockText = ""
        var secondAmPmText = ""
        if (_homeUiState.value.showSecondClock) {
            try {
                val cal = java.util.Calendar.getInstance()
                cal.time = now
                cal.add(java.util.Calendar.HOUR_OF_DAY, _homeUiState.value.secondClockOffsetHours)
                secondClockText = timeFormatter.format(cal.time)
                secondAmPmText = if (effectiveIs24Hour || !_homeUiState.value.showAmPm) {
                    ""
                } else {
                    try {
                        java.text.SimpleDateFormat("a", java.util.Locale.getDefault()).format(cal.time)
                    } catch (_: Exception) {
                        ""
                    }
                }
            } catch (_: Exception) {
                secondClockText = ""
                secondAmPmText = ""
            }
        }

        _homeUiState.value = _homeUiState.value.copy(
            clockText = clockText,
            amPmText = amPmText,
            is24Hour = effectiveIs24Hour,
            secondClockText = secondClockText,
            secondAmPmText = secondAmPmText
        )
    }

    // Public method to trigger immediate clock update (e.g., when screen is unlocked)
    fun refreshClock() {
        updateClockState()
    }

    // ================================================================================
    // PAGE MANAGEMENT (for home screen pagination)
    // ================================================================================

    fun setCurrentPage(page: Int) {
        val totalPages = _homeUiState.value.homePagesNum
        val clampedPage = page.coerceIn(0, maxOf(totalPages - 1, 0))
        _homeUiState.value = _homeUiState.value.copy(currentPage = clampedPage)
    }

    fun nextPage() {
        val currentPage = _homeUiState.value.currentPage
        val totalPages = _homeUiState.value.homePagesNum
        if (currentPage < totalPages - 1) {
            setCurrentPage(currentPage + 1)
        }
    }

    fun previousPage() {
        val currentPage = _homeUiState.value.currentPage
        if (currentPage > 0) {
            setCurrentPage(currentPage - 1)
        }
    }

    // ================================================================================
    // EVENT EMISSIONS (SharedFlow for UI events)
    // ================================================================================

    private val _appDrawerPageRequests = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val appDrawerPageRequests = _appDrawerPageRequests.asSharedFlow()


    // Typed-char events from hardware/physical keyboard. Use a small replay buffer
    // so the first character emitted while navigating to the drawer is not lost.
    private val _typedCharEvents = MutableSharedFlow<Char>(replay = 4, extraBufferCapacity = 32)
    val typedCharEvents = _typedCharEvents.asSharedFlow()

    private val _backspaceEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val backspaceEvents = _backspaceEvents.asSharedFlow()

    // Convenience emitters for external callers (Activity / Home) to deliver
    // printable characters or backspace events into the apps drawer input stream.
    fun emitTypedChar(ch: Char) {
        viewModelScope.launch { try { _typedCharEvents.emit(ch) } catch (_: Exception) {} }
    }

    fun emitBackspaceEvent() {
        viewModelScope.launch { try { _backspaceEvents.emit(Unit) } catch (_: Exception) {} }
    }

    // Clear any replayed typed chars so reopening the drawer doesn't receive stale input
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun clearTypedCharReplay() {
        try {
            _typedCharEvents.resetReplayCache()
        } catch (_: Exception) {}
    }

    // emitTypedChar removed — emitting into `_typedCharEvents` is not required from here.
    // emitBackspace() removed — use emitBackspaceEvent() instead

    fun requestAppDrawerPageUp() {
        viewModelScope.launch { _appDrawerPageRequests.emit(1) }
    }

    fun requestAppDrawerPageDown() {
        viewModelScope.launch { _appDrawerPageRequests.emit(-1) }
    }

    // Call this to refresh home app UI state (labels, fonts, colors, badges)
    fun refreshHomeAppsUiState(context: Context) {
        val notifications =
            NotificationManager.getInstance(context).notificationInfoState.value
        val textColor = try { resolveThemeColors(context).first } catch (_: Exception) { android.graphics.Color.BLACK }
        val backgroundColor = try { resolveThemeColors(context).second } catch (_: Exception) { android.graphics.Color.WHITE }
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
                color = textColor,
                notificationInfo = notificationInfo,
                activityPackage = appModel.activityPackage // Pass unique identifier
            )
        }
        // Update icon codes in repository when home apps change
        val labels = homeApps.map { it.label }
        appsRepository.updateIconCodes(labels, prefs.showIcons)
        
        // Update State for instant recomposition
        _homeUiState.value = _homeUiState.value.copy(
            homeApps = homeApps,
            textColor = textColor,
            backgroundColor = backgroundColor
        )

        // Also refresh audio widget state to ensure it appears after launcher restart
        refreshAudioWidgetState(context)
    }

    // Refresh audio widget state after launcher restart
    private fun refreshAudioWidgetState(context: Context) {
        try {
            val audioWidgetHelper = AudioWidgetHelper.getInstance(context)
            // Reset dismissal state and force refresh to pick up any active media sessions
            audioWidgetHelper.resetDismissalState()
        } catch (_: Exception) {
            // Ignore errors during widget refresh
        }
    }

    // Compose-friendly method to select/configure app based on flag
    fun selectAppForFlag(app: AppListItem, flag: AppDrawerFlag, position: Int = 0) {
        when (flag) {
            AppDrawerFlag.SetHomeApp -> {
                prefs.setHomeAppModel(position, app)
                refreshHomeAppsUiState(appContext)
            }
            AppDrawerFlag.SetSwipeLeft -> {
                prefs.appSwipeLeft = app
                prefs.swipeLeftAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(swipeLeftAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetSwipeUp -> {
                prefs.appSwipeUp = app
                prefs.swipeUpAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(swipeUpAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetSwipeRight -> {
                prefs.appSwipeRight = app
                prefs.swipeRightAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(swipeRightAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetSwipeDown -> {
                prefs.appSwipeDown = app
                prefs.swipeDownAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(swipeDownAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetClickClock -> {
                prefs.appClickClock = app
                prefs.clickClockAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(clickClockAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetClickDate -> {
                prefs.appClickDate = app
                prefs.clickDateAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(clickDateAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetQuoteWidget -> {
                prefs.appQuoteWidget = app
                prefs.quoteAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(quoteAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetDoubleTap -> {
                prefs.appDoubleTap = app
            }
            else -> { /* LaunchApp, HiddenApps handled by launchApp() */ }
        }
    }

    fun setShowClock(visibility: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showClock = visibility)
        prefs.showClock = visibility
    }

    fun setShowAmPm(enabled: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showAmPm = enabled)
        prefs.showAmPm = enabled
    }

    fun setShowSecondClock(enabled: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showSecondClock = enabled)
        prefs.showSecondClock = enabled
    }

    fun setClockMode(mode: Int) {
        val clamped = mode.coerceIn(0, 2)
        prefs.clockMode = clamped
        _homeUiState.value = _homeUiState.value.copy(clockMode = clamped)
        try {
            updateClockState()
        } catch (_: Exception) {}
    }

    fun setSecondClockOffsetHours(hours: Int) {
        val clamped = hours.coerceIn(-12, 14)
        prefs.secondClockOffsetHours = clamped
        _homeUiState.value = _homeUiState.value.copy(secondClockOffsetHours = clamped)
        try {
            updateClockState()
        } catch (_: Exception) {}
    }

    fun setShowDate(visibility: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showDate = visibility)
        prefs.showDate = visibility
    }

    fun setShowDateBatteryCombo(visibility: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showDateBatteryCombo = visibility)
        prefs.showDateBatteryCombo = visibility
    }

    fun setShowNotificationCount(visibility: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showNotificationCount = visibility)
        prefs.showNotificationCount = visibility
    }

    fun setShowAudioWidget(visibility: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showAudioWidget = visibility)
        prefs.showAudioWidgetEnabled = visibility
    }

    fun setShowQuote(visibility: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showQuote = visibility)
        prefs.showQuote = visibility
    }

    fun setAppDrawerAutoShowKeyboard(enabled: Boolean) {
        try {
            prefs.appDrawerAutoShowKeyboard = enabled
        } catch (_: Exception) {}
        try {
            _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerAutoShowKeyboard = enabled)
        } catch (_: Exception) {}
        try {
            _homeUiState.value = _homeUiState.value.copy(appDrawerAutoShowKeyboard = enabled)
        } catch (_: Exception) {}
    }

    fun setQuoteText(text: String) {
        _homeUiState.value = _homeUiState.value.copy(quoteText = text)
        prefs.quoteText = text
    }

    // Update persisted date text, battery text, and charging state (called from BatteryReceiver)
    fun updateDateAndBatteryText(dateText: String, batteryText: String, isCharging: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(
            dateText = dateText,
            batteryText = batteryText,
            isCharging = isCharging
        )
    }
    
    // Legacy method for backward compatibility (kept for now)
    @Deprecated("Use updateDateAndBatteryText instead", ReplaceWith("updateDateAndBatteryText(dateText, \"\", isCharging)"))
    fun updateDateText(dateText: String, isCharging: Boolean) {
        updateDateAndBatteryText(dateText, "", isCharging)
    }

    // `setDefaultLauncher` removed — use `isinkosDefault()` or `setDefaultHomeScreen(context)` directly.

    // Check if app is locked (for Compose to handle biometric prompt)
    fun isAppLocked(packageName: String): Boolean {
        return prefs.lockedApps.contains(packageName)
    }

    // Launch app without biometric check (call after biometric auth in Compose)
    fun launchApp(appListItem: AppListItem) {
        launchUnlockedApp(appListItem)
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
        @Suppress("UNUSED_PARAMETER") packageName: String, // Kept for API compatibility
        userHandle: UserHandle,
        launcher: LauncherApps
    ) {
        try {
            launcher.startMainActivity(component, userHandle, null, null)
            CrashHandler.logUserAction("${component.packageName} App Launched")
            // Remove from newly installed apps when launched
            val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
            newlyInstalled.remove(component.packageName)
            prefs.newlyInstalledApps = newlyInstalled
        } catch (_: SecurityException) {
            try {
                launcher.startMainActivity(component, Process.myUserHandle(), null, null)
                CrashHandler.logUserAction("${component.packageName} App Launched")
                // Remove from newly installed apps when launched
                val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
                newlyInstalled.remove(component.packageName)
                prefs.newlyInstalledApps = newlyInstalled
            } catch (_: Exception) {
                appContext.showShortToast("Unable to launch app")
            }
        } catch (_: Exception) {
            appContext.showShortToast("Unable to launch app")
        }
    }

    fun getAppList(includeHiddenApps: Boolean = true, flag: AppDrawerFlag? = null) {
        appsRepository.refreshAppList(includeHiddenApps, flag)
    }

    fun getHiddenApps() {
        appsRepository.refreshHiddenApps()
    }

    fun isinkosDefault() {
        isinkosDefault(appContext)
        // launcherDefault.value = !isDefault // Removed unused LiveData
    }

    // resetDefaultLauncherApp removed; call `setDefaultHomeScreen(context)` directly when needed.

    // updateAppOrder/saveAppOrder removed; app order can be managed directly via Prefs if needed.

    // ================================================================================
    // ================================================================================
    // HOME SCREEN & APP DRAWER OPERATIONS
    // ================================================================================

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

    // Toggle app lock status
    fun toggleAppLock(packageName: String) {
        val lockedApps = prefs.lockedApps.toMutableSet()
        if (lockedApps.contains(packageName)) {
            lockedApps.remove(packageName)
        } else {
            lockedApps.add(packageName)
        }
        prefs.lockedApps = lockedApps
    }

    // Refresh app list after uninstall
    fun refreshAppListAfterUninstall(includeHiddenApps: Boolean = false) {
        clearAppsPerPageCache()
        getAppList(includeHiddenApps)
    }

    // refreshAppList() removed — use refreshAppListAfterUninstall() or getAppList() directly

    // setHomeApp() removed — use selectAppForFlag() with AppDrawerFlag.SetHomeApp instead

    // Legacy method - AudioWidgetHelper now handles media state automatically via MediaSessionManager
    @Deprecated("AudioWidgetHelper handles media state automatically")
    fun updateMediaPlaybackInfo(_info: NotificationManager.NotificationInfo?) {
        // No-op: AudioWidgetHelper now uses MediaSessionManager directly
    }

    // Get Prefs instance for Compose
    fun getPrefs(): Prefs = prefs

    // ================================================================================
    // SETTINGS UPDATES (SSOT)
    // ================================================================================

    // Notification Settings
    fun togglePushNotifications(enabled: Boolean) {
        if (!enabled) {
            prefs.saveNotificationSwitchesState()
            prefs.disableAllNotificationSwitches()
            // Update state to reflect disabled switches
            _homeUiState.value = _homeUiState.value.copy(
                pushNotificationsEnabled = false,
                showNotificationBadge = false,
                showNotificationText = false,
                showMediaIndicator = false,
                showMediaName = false,
                showNotificationSenderName = false,
                showNotificationGroupName = false,
                showNotificationMessage = false,
                notificationsEnabled = false,
                clearConversationOnAppOpen = false
            )
        } else {
            prefs.restoreNotificationSwitchesState()
            // Update state to reflect restored switches
            _homeUiState.value = _homeUiState.value.copy(
                pushNotificationsEnabled = true,
                showNotificationBadge = prefs.showNotificationBadge,
                showNotificationText = prefs.showNotificationText,
                showMediaIndicator = prefs.showMediaIndicator,
                showMediaName = prefs.showMediaName,
                showNotificationSenderName = prefs.showNotificationSenderName,
                showNotificationGroupName = prefs.showNotificationGroupName,
                showNotificationMessage = prefs.showNotificationMessage,
                notificationsEnabled = prefs.notificationsEnabled,
                clearConversationOnAppOpen = prefs.clearConversationOnAppOpen
            )
        }
    }

    fun setPushNotificationsEnabled(enabled: Boolean) {
        prefs.pushNotificationsEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(pushNotificationsEnabled = enabled)
    }

    fun setShowNotificationBadge(enabled: Boolean) {
        prefs.showNotificationBadge = enabled
        _homeUiState.value = _homeUiState.value.copy(showNotificationBadge = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(showNotificationBadge = enabled)
    }

    fun setShowNotificationText(enabled: Boolean) {
        prefs.showNotificationText = enabled
        _homeUiState.value = _homeUiState.value.copy(showNotificationText = enabled)
    }

    fun setShowMediaIndicator(enabled: Boolean) {
        prefs.showMediaIndicator = enabled
        _homeUiState.value = _homeUiState.value.copy(showMediaIndicator = enabled)
    }

    fun setShowMediaName(enabled: Boolean) {
        prefs.showMediaName = enabled
        _homeUiState.value = _homeUiState.value.copy(showMediaName = enabled)
    }

    fun setShowNotificationSenderName(enabled: Boolean) {
        prefs.showNotificationSenderName = enabled
        _homeUiState.value = _homeUiState.value.copy(showNotificationSenderName = enabled)
    }

    fun setShowNotificationGroupName(enabled: Boolean) {
        prefs.showNotificationGroupName = enabled
        _homeUiState.value = _homeUiState.value.copy(showNotificationGroupName = enabled)
    }

    fun setShowNotificationMessage(enabled: Boolean) {
        prefs.showNotificationMessage = enabled
        _homeUiState.value = _homeUiState.value.copy(showNotificationMessage = enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.notificationsEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(notificationsEnabled = enabled)
        // Directly refresh app list to ensure immediate update
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
    }

    // setRecentsEnabled() removed — recents feature not currently implemented in UI

    fun setHomeAppCharLimit(limit: Int) {
        prefs.homeAppCharLimit = limit
        _homeUiState.value = _homeUiState.value.copy(homeAppCharLimit = limit)
    }

    fun setClearConversationOnAppOpen(enabled: Boolean) {
        prefs.clearConversationOnAppOpen = enabled
        _homeUiState.value = _homeUiState.value.copy(clearConversationOnAppOpen = enabled)
    }

    fun setAllowedBadgeNotificationApps(apps: Set<String>) {
        prefs.allowedBadgeNotificationApps = apps.toMutableSet()
        _homeUiState.value = _homeUiState.value.copy(allowedBadgeNotificationApps = apps)
        // Immediately refresh NotificationManager state to reflect allowlist changes
        NotificationManager.getInstance(appContext).refreshBadgeNotificationState()
    }

    fun setAllowedNotificationApps(apps: Set<String>) {
        prefs.allowedNotificationApps = apps.toMutableSet()
        _homeUiState.value = _homeUiState.value.copy(allowedNotificationApps = apps)
        // Immediately refresh NotificationManager state to reflect allowlist changes
        NotificationManager.getInstance(appContext).refreshConversationNotificationState()
    }

    fun setAllowedSimpleTrayApps(apps: Set<String>) {
        prefs.allowedSimpleTrayApps = apps.toMutableSet()
        _homeUiState.value = _homeUiState.value.copy(allowedSimpleTrayApps = apps)
    }

    fun setTopWidgetMargin(margin: Int) {
        prefs.topWidgetMargin = margin
        _homeUiState.value = _homeUiState.value.copy(topWidgetMargin = margin)
    }

    fun setBottomWidgetMargin(margin: Int) {
        prefs.bottomWidgetMargin = margin
        _homeUiState.value = _homeUiState.value.copy(bottomWidgetMargin = margin)
    }

    fun setPageIndicatorVisible(visible: Boolean) {
        prefs.homePager = visible
        _homeUiState.value = _homeUiState.value.copy(pageIndicatorVisible = visible)
    }

    fun setTextIslands(enabled: Boolean) {
        prefs.textIslands = enabled
        _homeUiState.value = _homeUiState.value.copy(textIslands = enabled)
    }

    fun setTextIslandsInverted(inverted: Boolean) {
        prefs.textIslandsInverted = inverted
        _homeUiState.value = _homeUiState.value.copy(textIslandsInverted = inverted)
    }

    fun setTextIslandsShape(shape: Int) {
        prefs.textIslandsShape = shape
        _homeUiState.value = _homeUiState.value.copy(textIslandsShape = shape)
    }

    fun setShowIcons(enabled: Boolean) {
        prefs.showIcons = enabled
        _homeUiState.value = _homeUiState.value.copy(showIcons = enabled)
        // Update icon codes when showIcons preference changes
        val labels = _homeUiState.value.homeApps.map { it.label }
        appsRepository.updateIconCodes(labels, enabled)
    }

    fun setHomeReset(enabled: Boolean) {
        prefs.homeReset = enabled
        _homeUiState.value = _homeUiState.value.copy(homeReset = enabled)
    }

    fun setExtendHomeAppsArea(enabled: Boolean) {
        prefs.extendHomeAppsArea = enabled
        _homeUiState.value = _homeUiState.value.copy(extendHomeAppsArea = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(extendHomeAppsArea = enabled)
    }

    fun setHomeAppsYOffset(offset: Int) {
        prefs.homeAppsYOffset = offset
        _homeUiState.value = _homeUiState.value.copy(homeAppsYOffset = offset)
    }

    fun setTextPaddingSize(size: Int) {
        prefs.textPaddingSize = size
        _homeUiState.value = _homeUiState.value.copy(textPaddingSize = size)
    }

    fun setHomeAlignment(alignment: Int) {
        prefs.homeAlignment = alignment
        _homeUiState.value = _homeUiState.value.copy(homeAlignment = alignment)
    }

    // Extras Settings
    fun setEinkRefreshEnabled(enabled: Boolean) {
        prefs.einkRefreshEnabled = enabled
        if (enabled) {
            // Auto mode takes precedence over Home-only mode: clear Home-only if auto enabled
            prefs.einkRefreshHomeButtonOnly = false
        }
        _homeUiState.value = _homeUiState.value.copy(
            einkRefreshEnabled = enabled,
            einkRefreshHomeButtonOnly = prefs.einkRefreshHomeButtonOnly
        )
    }

    fun setEinkRefreshHomeButtonOnly(enabled: Boolean) {
        prefs.einkRefreshHomeButtonOnly = enabled
        _homeUiState.value = _homeUiState.value.copy(
            einkRefreshHomeButtonOnly = enabled,
            einkRefreshEnabled = prefs.einkRefreshEnabled
        )
    }

    fun setEinkRefreshDelay(delay: Int) {
        prefs.einkRefreshDelay = delay
        _homeUiState.value = _homeUiState.value.copy(einkRefreshDelay = delay)
    }

    fun setUseVolumeKeysForPages(enabled: Boolean) {
        prefs.useVolumeKeysForPages = enabled
        _homeUiState.value = _homeUiState.value.copy(useVolumeKeysForPages = enabled)
    }

    fun setSelectedSystemShortcuts(shortcuts: Set<String>) {
        prefs.selectedSystemShortcuts = shortcuts.toMutableSet()
        _homeUiState.value = _homeUiState.value.copy(selectedSystemShortcuts = shortcuts)
        // Directly refresh app list to ensure immediate update
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
    }

    fun setEinkHelperEnabled(enabled: Boolean) {
        prefs.einkHelperEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(einkHelperEnabled = enabled)
    }

    // Advanced Settings
    fun setHomeLocked(locked: Boolean) {
        prefs.homeLocked = locked
        _homeUiState.value = _homeUiState.value.copy(homeLocked = locked)
    }

    fun setSettingsLocked(locked: Boolean) {
        prefs.settingsLocked = locked
        _homeUiState.value = _homeUiState.value.copy(settingsLocked = locked)
    }

    fun setLongPressAppInfoEnabled(enabled: Boolean) {
        prefs.longPressAppInfoEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(longPressAppInfoEnabled = enabled)
    }

    // Gesture Settings
    fun setShortSwipeThresholdRatio(ratio: Float) {
        prefs.shortSwipeThresholdRatio = ratio
        _homeUiState.value = _homeUiState.value.copy(shortSwipeThresholdRatio = ratio)
    }

    fun setLongSwipeThresholdRatio(ratio: Float) {
        prefs.longSwipeThresholdRatio = ratio
        _homeUiState.value = _homeUiState.value.copy(longSwipeThresholdRatio = ratio)
    }

    fun setDoubleTapAction(action: Constants.Action) {
        prefs.doubleTapAction = action
        _homeUiState.value = _homeUiState.value.copy(doubleTapAction = action)
    }

    fun setClickClockAction(action: Constants.Action) {
        prefs.clickClockAction = action
        _homeUiState.value = _homeUiState.value.copy(clickClockAction = action)
    }

    fun setClickDateAction(action: Constants.Action) {
        prefs.clickDateAction = action
        _homeUiState.value = _homeUiState.value.copy(clickDateAction = action)
    }

    fun setSwipeLeftAction(action: Constants.Action) {
        prefs.swipeLeftAction = action
        _homeUiState.value = _homeUiState.value.copy(swipeLeftAction = action)
    }

    fun setSwipeRightAction(action: Constants.Action) {
        prefs.swipeRightAction = action
        _homeUiState.value = _homeUiState.value.copy(swipeRightAction = action)
    }

    fun setSwipeUpAction(action: Constants.Action) {
        prefs.swipeUpAction = action
        _homeUiState.value = _homeUiState.value.copy(swipeUpAction = action)
    }

    fun setSwipeDownAction(action: Constants.Action) {
        prefs.swipeDownAction = action
        _homeUiState.value = _homeUiState.value.copy(swipeDownAction = action)
    }

    fun setQuoteAction(action: Constants.Action) {
        prefs.quoteAction = action
        _homeUiState.value = _homeUiState.value.copy(quoteAction = action)
    }

    // Drawer Settings Setters
    fun setAppDrawerSize(size: Int) {
        prefs.appDrawerSize = size
        clearAppsPerPageCache()
        _homeUiState.value = _homeUiState.value.copy(appDrawerSize = size)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSize = size)
    }

    fun setAppDrawerGap(gap: Int) {
        prefs.appDrawerGap = gap
        clearAppsPerPageCache()
        _homeUiState.value = _homeUiState.value.copy(appDrawerGap = gap)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerGap = gap)
    }

    fun setAppDrawerAlignment(alignment: Int) {
        prefs.appDrawerAlignment = alignment
        _homeUiState.value = _homeUiState.value.copy(appDrawerAlignment = alignment)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerAlignment = alignment)
    }

    fun setAppDrawerAzFilter(enabled: Boolean) {
        prefs.appDrawerAzFilter = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerAzFilter = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerAzFilter = enabled)
    }
    fun setAppDrawerSearchEnabled(enabled: Boolean) {
        prefs.appDrawerSearchEnabled = enabled
        clearAppsPerPageCache()
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchEnabled = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchEnabled = enabled)
        // Directly refresh app list to ensure immediate update
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
    }

    fun setAppDrawerAutoLaunch(enabled: Boolean) {
        prefs.appDrawerAutoLaunch = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerAutoLaunch = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerAutoLaunch = enabled)
    }

    // ---------------------- Font / Text setters ----------------------
    fun setFontFamily(font: Constants.FontFamily) {
        prefs.fontFamily = font
        // fontFamily affects multiple derived settings; trigger a refresh
        updateStateFromPreference(PrefKeys.APPS_FONT)
    }

    fun setUniversalFont(font: Constants.FontFamily) {
        prefs.universalFont = font
        updateStateFromPreference(PrefKeys.APPS_FONT)
    }

    fun setUniversalFontEnabled(enabled: Boolean) {
        prefs.universalFontEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setAppsFont(font: Constants.FontFamily) {
        prefs.appsFont = font
        clearAppsPerPageCache()
        updateStateFromPreference(PrefKeys.APPS_FONT)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appsFont = prefs.appsFont)
    }

    fun setClockFont(font: Constants.FontFamily) {
        prefs.clockFont = font
        updateStateFromPreference(PrefKeys.CLOCK_FONT)
    }

    fun setDateFont(font: Constants.FontFamily) {
        prefs.dateFont = font
        updateStateFromPreference(PrefKeys.DATE_FONT)
    }

    fun setQuoteFont(font: Constants.FontFamily) {
        prefs.quoteFont = font
        updateStateFromPreference(PrefKeys.QUOTE_FONT)
    }

    fun setNotificationsFont(font: Constants.FontFamily) {
        prefs.notificationsFont = font
        updateStateFromPreference(PrefKeys.NOTIFICATIONS_FONT)
    }

    /**
     * Reset all font preferences to the canonical default.
     * Uses the centralized setters so prefs and ViewModel state stay in sync.
     */
    fun resetFontsToDefault() {
        val defaultFont = Constants.FontFamily.PublicSans
        // Apply universal font propagation then disable it so individual slots reflect the default
        setUniversalFont(defaultFont)
        setUniversalFontEnabled(true)
        setUniversalFontEnabled(false)
        // Ensure launcher font and specific slots are set
        setFontFamily(defaultFont)
        setAppsFont(defaultFont)
        setClockFont(defaultFont)
        setStatusFont(defaultFont)
        setLabelNotificationsFont(defaultFont)
        setDateFont(defaultFont)
        setQuoteFont(defaultFont)
        setLettersFont(defaultFont)
        setLettersTitleFont(defaultFont)
        setNotificationsFont(defaultFont)

        // Clear custom font paths for known contexts
        listOf("universal", "apps", "clock", "status", "notification", "date", "quote", "letters", "lettersTitle", "notifications").forEach {
            setCustomFontPath(it, null)
        }
    }

    fun setLabelNotificationsFont(font: Constants.FontFamily) {
        prefs.labelnotificationsFont = font
        updateStateFromPreference(PrefKeys.NOTIFICATION_FONT)
    }

    fun setLettersTitleFont(font: Constants.FontFamily) {
        prefs.lettersTitleFont = font
        updateStateFromPreference(PrefKeys.LETTERS_TITLE_FONT)
    }

    fun setLettersFont(font: Constants.FontFamily) {
        prefs.lettersFont = font
        updateStateFromPreference(PrefKeys.LETTERS_FONT)
    }

    fun setStatusFont(font: Constants.FontFamily) {
        prefs.statusFont = font
        updateStateFromPreference(PrefKeys.STATUS_FONT)
    }

    fun setSettingsSize(size: Int) {
        prefs.settingsSize = size
        _homeUiState.value = _homeUiState.value.copy(settingsSize = prefs.settingsSize)
    }

    fun setAppSize(size: Int) {
        prefs.appSize = size
        _homeUiState.value = _homeUiState.value.copy(appSize = prefs.appSize)
    }

    fun setClockSize(size: Int) {
        prefs.clockSize = size
        _homeUiState.value = _homeUiState.value.copy(clockSize = prefs.clockSize)
    }

    fun setDateSize(size: Int) {
        prefs.dateSize = size
        _homeUiState.value = _homeUiState.value.copy(dateSize = prefs.dateSize)
    }

    fun setQuoteSize(size: Int) {
        prefs.quoteSize = size
        _homeUiState.value = _homeUiState.value.copy(quoteSize = prefs.quoteSize)
    }

    fun setLabelNotificationsTextSize(size: Int) {
        prefs.labelnotificationsTextSize = size
        _homeUiState.value = _homeUiState.value.copy(labelnotificationsTextSize = prefs.labelnotificationsTextSize)
    }

    fun setLettersTitle(value: String) {
        prefs.lettersTitle = value
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setLettersTitleSize(size: Int) {
        prefs.lettersTitleSize = size
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setOnboardingPage(page: Int) {
        prefs.onboardingPage = page
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setFirstOpen(open: Boolean) {
        prefs.firstOpen = open
        // No dedicated HomeUiState field for firstOpen, but emit a copy to notify observers
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setLockModeOn(enabled: Boolean) {
        prefs.lockModeOn = enabled
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setHiddenApps(hidden: Set<String>) {
        prefs.hiddenApps = hidden.toMutableSet()
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setBrightnessLevel(level: Int) {
        prefs.brightnessLevel = level
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setLettersTextSize(size: Int) {
        prefs.lettersTextSize = size
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setNotificationsTextSize(size: Int) {
        prefs.notificationsTextSize = size
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setSmallCapsApps(enabled: Boolean) {
        prefs.smallCapsApps = enabled
        _homeUiState.value = _homeUiState.value.copy(smallCapsApps = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(smallCapsApps = enabled)
    }

    fun setAllCapsApps(enabled: Boolean) {
        prefs.allCapsApps = enabled
        _homeUiState.value = _homeUiState.value.copy(allCapsApps = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(allCapsApps = enabled)
    }

    // Custom font path helpers
    fun setCustomFontPath(contextKey: String, path: String?) {
        if (path == null) {
            prefs.removeCustomFontPath(contextKey)
        } else {
            prefs.setCustomFontPath(contextKey, path)
            prefs.addCustomFontPath(path)
        }
        // Immediately update ViewModel state so Compose remembers re-resolve typefaces
        try {
            _homeUiState.value = _homeUiState.value.copy(
                clockCustomFontPath = try { prefs.getCustomFontPathForContext("clock") } catch (_: Exception) { null },
                dateCustomFontPath = try { prefs.getCustomFontPathForContext("date") } catch (_: Exception) { null },
                quoteCustomFontPath = try { prefs.getCustomFontPathForContext("quote") } catch (_: Exception) { null },
                notificationCustomFontPath = try { prefs.getCustomFontPathForContext("notification") } catch (_: Exception) { null }
            )
        } catch (_: Exception) {}

        try {
            _appsDrawerUiState.value = _appsDrawerUiState.value.copy(
                customFontPath = try { prefs.getCustomFontPathForContext("apps") } catch (_: Exception) { null }
            )
        } catch (_: Exception) {}
    }

    fun removeCustomFontPathByPath(path: String) {
        prefs.removeCustomFontPathByPath(path)
    }

    fun addCustomFontPath(path: String) {
        prefs.addCustomFontPath(path)
    }

    fun setHideHomeApps(enabled: Boolean) {
        prefs.hideHomeApps = enabled
        _homeUiState.value = _homeUiState.value.copy(hideHomeApps = enabled)
    }

    // Look & Feel Settings Setters
    fun setAppTheme(theme: Constants.Theme) {
        prefs.appTheme = theme
        _homeUiState.value = _homeUiState.value.copy(appTheme = theme)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appTheme = theme)
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.hapticFeedback = enabled
        _homeUiState.value = _homeUiState.value.copy(hapticFeedback = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(hapticFeedback = enabled)
    }

    fun setShowStatusBar(enabled: Boolean) {
        prefs.showStatusBar = enabled
        _homeUiState.value = _homeUiState.value.copy(showStatusBar = enabled)
    }

    fun setShowNavigationBar(enabled: Boolean) {
        prefs.showNavigationBar = enabled
        _homeUiState.value = _homeUiState.value.copy(showNavigationBar = enabled)
    }

    fun setBackgroundColor(color: Int) {
        prefs.backgroundColor = color
        _homeUiState.value = _homeUiState.value.copy(backgroundColor = color)
    }

    fun setTextColor(color: Int) {
        prefs.textColor = color
        _homeUiState.value = _homeUiState.value.copy(textColor = color)
    }

    fun setBackgroundOpacity(opacity: Int) {
        prefs.backgroundOpacity = opacity
        _homeUiState.value = _homeUiState.value.copy(backgroundOpacity = opacity)
    }

    fun setNotificationsPerPage(pageCount: Int) {
        prefs.notificationsPerPage = pageCount
        _homeUiState.value = _homeUiState.value.copy(notificationsPerPage = pageCount)
    }

    fun setEnableBottomNav(enabled: Boolean) {
        prefs.enableBottomNav = enabled
        _homeUiState.value = _homeUiState.value.copy(enableBottomNav = enabled)
    }

}