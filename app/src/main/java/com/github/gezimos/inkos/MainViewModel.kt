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
import com.github.gezimos.inkos.helper.CalendarEventsHelper
import com.github.gezimos.inkos.helper.IconUtility
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.ui.compose.HomeUiRenderState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val homeApps: List<HomeAppUiState> = emptyList(),
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showDateBatteryCombo: Boolean = false,
    val showNotificationCount: Boolean = false,
    val notificationCountSource: Int = 0, // 0 = SimpleTray, 1 = Letters
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
    val lettersTitle: String = "Letters",
    val lettersTitleSize: Int = 36,
    val notificationsTextSize: Int = 18,
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
    val clockAlignment: Int = 0,
    val dateAlignment: Int = 0,
    val quoteAlignment: Int = 0,
    val homeAppsYOffset: Int = 0,

    val bottomWidgetHeightPx: Int = 0,
    val screenHeightDp: Int = 0,
    val topWidgetMargin: Int = 0,
    val bottomWidgetMargin: Int = 0,
    val hideHomeApps: Boolean = false,
    val dateFont: Constants.FontFamily = Constants.FontFamily.System,
    val dateSize: Int = 0,
    val allCapsApps: Boolean = false,
    val smallCapsApps: Boolean = false,
    val showNotificationBadge: Boolean = false,
    val notificationIndicatorStyle: Int = 0,
    val hapticFeedback: Boolean = false,
    val vibrationScale: Int = 100,
    val pageIndicatorVisible: Boolean = false,
    val textIslands: Boolean = false,
    val textIslandsInverted: Boolean = false,
    val textIslandsShape: Int = 0,
    val showIcons: Boolean = false,
    val iconSourceMode: Int = 0,
    val iconShape: Int = 0,
    val iconTintContrast: Int = 10,
    val selectedIconPackPackage: String = "",
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
    val selectedAppShortcuts: Set<String>? = null, // null means all selected by default
    val einkHelperMode: Int = 0,
    // Advanced Settings
    val homeLocked: Boolean = false,
    val settingsLocked: Boolean = false,
    val longPressAppInfoEnabled: Boolean = false,
    val homeReset: Boolean = false,
    val extendHomeAppsArea: Boolean = false,
    val bottomWidgetType: String = "quote",
    val shortcutLeftIcon: Constants.ShortcutIcon = Constants.ShortcutIcon.Search,
    val shortcutLeftAction: Constants.Action = Constants.Action.Search,
    val shortcutRightIcon: Constants.ShortcutIcon = Constants.ShortcutIcon.Phone,
    val shortcutRightAction: Constants.Action = Constants.Action.OpenApp,
    val shortcutPageDots: Boolean = false,
    val shortcutHideOutline: Boolean = false,
    // Android AppWidget hosting
    val showAndroidWidget: Boolean = false,
    val androidWidgetId: Int = -1,
    val androidWidgetHeight: Int = 120,
    val androidWidgetMarginStart: Int = 0,
    val androidWidgetMarginEnd: Int = 0,
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
    val appDrawerSearchContactsEnabled: Boolean = false,
    val appDrawerSearchContactAccounts: Set<String>? = null,
    val appDrawerSearchWebEnabled: Boolean = false,
    val appDrawerSearchSettingsEnabled: Boolean = false,
    val appDrawerSearchMusicEnabled: Boolean = false,
    val appDrawerSearchFilesEnabled: Boolean = false,
    val appDrawerSearchHiddenAppsEnabled: Boolean = false,

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
    val dateFormatStyle: Int = 0,
    val clockMode: Int = 0,
    val clockStyle: Int = 0,
    val secondClockText: String = "",
    val secondAmPmText: String = "",
    // Events widget
    val eventsList: List<CalendarEventsHelper.CalendarEvent> = emptyList(),
    val eventsIndex: Int = 0,
    val hasCalendarPermission: Boolean = false,
    val eventsCalendarName: String = "",
    val eventsHideControls: Boolean = false,
    val maxHomeAppsYOffset: Int = Constants.MAX_HOME_APPS_Y_OFFSET
)

/**
 * Separate clock state so the 60-second ticker doesn't mutate the 140-field HomeUiState,
 * avoiding unnecessary recompositions in settings/drawer that observe HomeUiState.
 */
data class ClockState(
    val clockText: String = "",
    val amPmText: String = "",
    val is24Hour: Boolean = false,
    val secondClockText: String = "",
    val secondAmPmText: String = ""
)

@androidx.compose.runtime.Stable
data class AppsDrawerUiState(
    val appList: List<AppListItem> = emptyList(),
    val hiddenApps: List<AppListItem> = emptyList(),
    val azLetters: List<Char> = listOf('★') + ('A'..'Z').toList(),
    // New fields for AppsUI refactor
    val appsFont: Constants.FontFamily = Constants.FontFamily.System,
    val appDrawerSize: Int = 0,
    val appDrawerGap: Int = Constants.DEFAULT_APP_DRAWER_GAP,
    val appDrawerAlignment: Int = 0,
    val allCapsApps: Boolean = false,
    val smallCapsApps: Boolean = false,
    val showNotificationBadge: Boolean = false,
    val notificationIndicatorStyle: Int = 0,
    val hapticFeedback: Boolean = false,

    val lockedApps: Set<String> = emptySet(),
    val newlyInstalledApps: Set<String> = emptySet(),
    val appTheme: Constants.Theme = Constants.Theme.Light,
    val appDrawerAzFilter: Boolean = false,
    val customFontPath: String? = null,
    val appDrawerSearchEnabled: Boolean = true,
    val appDrawerAutoLaunch: Boolean = true,
    val appDrawerAutoShowKeyboard: Boolean = false,
    val appDrawerSearchContactsEnabled: Boolean = false,
    val appDrawerSearchContactAccounts: Set<String>? = null,
    val appDrawerSearchWebEnabled: Boolean = false,
    val appDrawerSearchSettingsEnabled: Boolean = false,
    val appDrawerSearchMusicEnabled: Boolean = false,
    val appDrawerSearchFilesEnabled: Boolean = false,
    val appDrawerSearchHiddenAppsEnabled: Boolean = false,
    val extendHomeAppsArea: Boolean = false,
    val textIslandsShape: Int = 0,
    val drawerShowIcons: Boolean = false,
    val iconSourceMode: Int = 0,
    val iconShape: Int = 0,
    val iconTintContrast: Int = 10,
    val selectedIconPackPackage: String = "",
    // Recents screen preferences
    val recentsDefaultView: Int = 0,
    val recentsUsageFilter: Int = 1,
    val recentsUsageUnit: Int = 0,
    val recentsUnitCost: Int = 10,
    val recentsUnitCurrencyChar: String = "$",
    val recentsUnitCoffeePrice: Int = 3,
    val recentsUnitEmojiChar: String = "☕",
    val appDrawerSortOrder: Int = 0,
    val privateSpaceApps: List<AppListItem> = emptyList(),
    val hasPrivateSpace: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)
    private val appsRepository = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
    
    private val _appUsageStats = MutableStateFlow<Map<String, Pair<Long, Long>>>(emptyMap())
    val appUsageStats: StateFlow<Map<String, Pair<Long, Long>>> = _appUsageStats.asStateFlow()

    private fun loadAppUsageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val hasPermission = appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    appContext.packageName
                ) == android.app.AppOpsManager.MODE_ALLOWED
                if (!hasPermission) emptyMap()
                else {
                    val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                    val endTime = System.currentTimeMillis()
                    val startTime = endTime - (30L * 24 * 60 * 60 * 1000)
                    usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                        .groupBy { it.packageName }
                        .mapValues { (_, stats) ->
                            stats.fold(Pair(0L, 0L)) { acc, stat ->
                                Pair(maxOf(acc.first, stat.lastTimeUsed), acc.second + stat.totalTimeInForeground)
                            }
                        }
                        .filterValues { (lastUsed, totalTime) -> lastUsed > 0 && totalTime > 0 }
                }
            } catch (_: Exception) { emptyMap() }
            _appUsageStats.value = result
        }
    }

    private val _clockState = MutableStateFlow(ClockState())

    private val _dailyTotalUsageMs = MutableStateFlow(0L)
    val dailyTotalUsageMs: StateFlow<Long> = _dailyTotalUsageMs.asStateFlow()

    fun loadDailyTotalUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
                val hasPermission = appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    appContext.packageName
                ) == android.app.AppOpsManager.MODE_ALLOWED
                if (!hasPermission) 0L
                else {
                    val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                    val endTime = System.currentTimeMillis()
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val startTime = cal.timeInMillis
                    val installedPackages = appsRepository.appList.value
                        .map { it.activityPackage }
                        .filterNot { it == "app.inkos" || it == "app.inkos.debug" || it == appContext.packageName }
                        .toSet()
                    val usageMap = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                        .filter { it.totalTimeInForeground > 0 && it.lastTimeUsed > 0 }
                        .groupBy { it.packageName }
                        .mapValues { (_, stats) ->
                            stats.fold(0L) { acc, stat -> acc + stat.totalTimeInForeground }
                        }
                    usageMap.entries
                        .filter { installedPackages.contains(it.key) }
                        .sumOf { it.value }
                }
            } catch (_: Exception) { 0L }
            _dailyTotalUsageMs.value = result
        }
    }

    private fun formatDailyUsage(ms: Long): String {
        val totalMinutes = (ms / 60_000).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
            hours > 0 -> "${hours}h"
            else -> "${minutes}min"
        }
    }

    private var appsPerPageCache: Int? = prefs.cachedAppsPerPage.let { if (it > 0) it else null }

    fun getCachedAppsPerPage(): Int? = appsPerPageCache

    fun cacheAppsPerPage(value: Int) {
        appsPerPageCache = value
        prefs.cachedAppsPerPage = value
    }

    fun clearAppsPerPageCache() {
        appsPerPageCache = null
        prefs.invalidateAppsPerPageCache()
    }

    // ================================================================================
    // ================================================================================

    private val _homeUiState = MutableStateFlow(HomeUiState(
        showClock = prefs.showClock,
        showDate = prefs.showDate,
        showDateBatteryCombo = prefs.showDateBatteryCombo,
        showNotificationCount = prefs.showNotificationCount,
        notificationCountSource = prefs.notificationCountSource,
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
        lettersTitle = prefs.lettersTitle,
        lettersTitleSize = prefs.lettersTitleSize,
        notificationsTextSize = prefs.notificationsTextSize,
        textPaddingSize = prefs.textPaddingSize,
        appSize = prefs.appSize,
        clockSize = prefs.clockSize,
        quoteSize = prefs.quoteSize,
        settingsSize = prefs.settingsSize,
        backgroundOpacity = prefs.backgroundOpacity,
        showStatusBar = prefs.showStatusBar,
        showNavigationBar = prefs.showNavigationBar,
        // Initialize new fields
        homeAlignment = prefs.homeAlignment,
        clockAlignment = prefs.homeClockAlignment,
        dateAlignment = prefs.homeDateAlignment,
        quoteAlignment = prefs.homeQuoteAlignment,
        homeAppsYOffset = prefs.homeAppsYOffset,
        screenHeightDp = (appContext.resources.displayMetrics.heightPixels / appContext.resources.displayMetrics.density).toInt(),

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
        appDrawerSearchContactsEnabled = prefs.appDrawerSearchContactsEnabled,
        appDrawerSearchContactAccounts = prefs.appDrawerSearchContactAccounts,
        appDrawerSearchWebEnabled = prefs.appDrawerSearchWebEnabled,
        appDrawerSearchSettingsEnabled = prefs.appDrawerSearchSettingsEnabled,
        appDrawerSearchMusicEnabled = prefs.appDrawerSearchMusicEnabled,
        appDrawerSearchFilesEnabled = prefs.appDrawerSearchFilesEnabled,
        appDrawerSearchHiddenAppsEnabled = prefs.appDrawerSearchHiddenAppsEnabled,
        allCapsApps = prefs.allCapsApps,
        smallCapsApps = prefs.smallCapsApps,
        showNotificationBadge = prefs.showNotificationBadge,
        notificationIndicatorStyle = prefs.notificationIndicatorStyle,
        hapticFeedback = prefs.hapticFeedback,
        vibrationScale = prefs.vibrationScale,
        pageIndicatorVisible = prefs.homePager,
        textIslands = prefs.textIslands,
        textIslandsInverted = prefs.textIslandsInverted,
        textIslandsShape = prefs.textIslandsShape,
        showIcons = prefs.showIcons,
        iconSourceMode = prefs.iconSourceMode,
        iconShape = prefs.iconShape,
        iconTintContrast = prefs.iconTintContrast,
        selectedIconPackPackage = prefs.selectedIconPackPackage,
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
        selectedAppShortcuts = prefs.selectedAppShortcuts.takeIf { it.isNotEmpty() },
        einkHelperMode = prefs.einkHelperMode,
        // Advanced Settings
        homeLocked = prefs.homeLocked,
        settingsLocked = prefs.settingsLocked,
        longPressAppInfoEnabled = prefs.longPressAppInfoEnabled,
        homeReset = prefs.homeReset,
        extendHomeAppsArea = prefs.extendHomeAppsArea,
        // Bottom widget + Android AppWidget hosting
        bottomWidgetType = prefs.bottomWidgetType,
        shortcutLeftIcon = prefs.shortcutLeftIcon,
        shortcutLeftAction = prefs.shortcutLeftAction,
        shortcutRightIcon = prefs.shortcutRightIcon,
        shortcutRightAction = prefs.shortcutRightAction,
        shortcutPageDots = prefs.shortcutPageDots,
        shortcutHideOutline = prefs.shortcutHideOutline,
        showAndroidWidget = prefs.showAndroidWidget,
        androidWidgetId = prefs.androidWidgetId,
        androidWidgetHeight = prefs.androidWidgetHeight,
        androidWidgetMarginStart = prefs.androidWidgetMarginStart,
        androidWidgetMarginEnd = prefs.androidWidgetMarginEnd,
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
        // Events widget
        eventsList = emptyList(),
        eventsIndex = 0,
        hasCalendarPermission = CalendarEventsHelper.hasCalendarPermission(appContext),
        eventsCalendarName = prefs.eventsCalendarName,
        eventsHideControls = prefs.eventsHideControls,
        // Initialize new centralized fields
        labelnotificationsTextSize = prefs.labelnotificationsTextSize,
        clockCustomFontPath = try { prefs.getCustomFontPathForContext("clock") } catch (_: Exception) { null },
        dateCustomFontPath = try { prefs.getCustomFontPathForContext("date") } catch (_: Exception) { null },
        quoteCustomFontPath = try { prefs.getCustomFontPathForContext("quote") } catch (_: Exception) { null },
        notificationCustomFontPath = try { prefs.getCustomFontPathForContext("notification") } catch (_: Exception) { null },
        currentPage = 0,
        dateFormatStyle = prefs.dateFormatStyle,
        clockMode = prefs.clockMode,
        clockStyle = prefs.clockStyle
    ))
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    val homeRenderState: StateFlow<HomeUiRenderState> = combine(
        _homeUiState,
        NotificationManager.getInstance(appContext).notificationInfoState,
        NotificationManager.getInstance(appContext).conversationNotificationsState,
        AudioWidgetHelper.getInstance(appContext).mediaPlayerState,
        com.github.gezimos.inkos.services.NotificationService.sbnState,
        appsRepository.iconCodes,
        _dailyTotalUsageMs,
        _clockState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val homeUi = values[0] as HomeUiState
        val dailyUsageMs = values[6] as Long
        @Suppress("UNCHECKED_CAST")
        val notifications = values[1] as Map<String, NotificationManager.NotificationInfo>
        val mediaInfo = values[3] as? AudioWidgetHelper.MediaPlayerInfo
        @Suppress("UNCHECKED_CAST")
        val rawNotifications = values[4] as List<android.service.notification.StatusBarNotification>
        @Suppress("UNCHECKED_CAST")
        val iconCodes = values[5] as Map<String, String>
        val clock = values[7] as ClockState
        val appsPerPage = if (homeUi.homePagesNum > 0) {
            kotlin.math.ceil(homeUi.homeApps.size.toDouble() / homeUi.homePagesNum).toInt().coerceAtLeast(1)
        } else {
            homeUi.homeApps.size.coerceAtLeast(1)
        }
        
        // Compute notification count from selected source
        val notificationCount = when (homeUi.notificationCountSource) {
            1 -> {
                @Suppress("UNCHECKED_CAST")
                val conversations = values[2] as Map<String, List<NotificationManager.ConversationNotification>>
                val allowed = homeUi.allowedNotificationApps
                conversations.entries.sumOf { (pkg, list) ->
                    if (allowed.isEmpty() || allowed.contains(pkg)) list.size else 0
                }
            }
            else -> {
                // SimpleTray: count live system notifications
                val allowed = homeUi.allowedSimpleTrayApps
                val notificationManager = NotificationManager.getInstance(appContext)
                val notificationsFromSbn = rawNotifications.count { sbn ->
                    if (sbn.notification.category == android.app.Notification.CATEGORY_TRANSPORT) {
                        return@count false
                    }
                    !notificationManager.isNotificationSummary(sbn) &&
                    (allowed.isEmpty() || allowed.contains(sbn.packageName))
                }
                val mediaCount = if (mediaInfo != null && (allowed.isEmpty() || allowed.contains(mediaInfo.packageName))) 1 else 0
                notificationsFromSbn + mediaCount
            }
        }
        
        HomeUiRenderState(
            homeApps = homeUi.homeApps,
            clockText = clock.clockText,
            dateText = homeUi.dateText,
            amPmText = clock.amPmText,
            is24Hour = clock.is24Hour,
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
            clockStyle = homeUi.clockStyle,
            homeAlignment = homeUi.homeAlignment,
            clockAlignment = homeUi.clockAlignment,
            dateAlignment = homeUi.dateAlignment,
            quoteAlignment = homeUi.quoteAlignment,
            homeAppsYOffset = homeUi.homeAppsYOffset,
            maxHomeAppsYOffset = homeUi.maxHomeAppsYOffset,

            bottomWidgetHeightPx = homeUi.bottomWidgetHeightPx,
            bottomWidgetMargin = homeUi.bottomWidgetMargin,
            topWidgetMargin = homeUi.topWidgetMargin,
            pageIndicatorVisible = homeUi.pageIndicatorVisible,
            pageIndicatorColor = homeUi.textColor,
            appPadding = homeUi.textPaddingSize,
            textIslands = homeUi.textIslands,
            textIslandsInverted = homeUi.textIslandsInverted,
            textIslandsShape = homeUi.textIslandsShape,
            showIcons = homeUi.showIcons,
            iconSourceMode = homeUi.iconSourceMode,
            iconShape = homeUi.iconShape,
            iconTintContrast = homeUi.iconTintContrast,
            selectedIconPackPackage = homeUi.selectedIconPackPackage,
            iconCodes = iconCodes,
            appTextSize = homeUi.appSize.toFloat(),
            hideHomeApps = homeUi.hideHomeApps,
            appDrawerGap = homeUi.appDrawerGap,
            showAmPm = homeUi.showAmPm,
            showSecondClock = homeUi.showSecondClock,
            secondClockText = clock.secondClockText,
            secondAmPmText = clock.secondAmPmText,
            secondClockOffsetHours = homeUi.secondClockOffsetHours,
            allCapsApps = homeUi.allCapsApps,
            smallCapsApps = homeUi.smallCapsApps,
            showNotificationBadge = homeUi.showNotificationBadge,
            notificationIndicatorStyle = homeUi.notificationIndicatorStyle,
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
            extendHomeAppsArea = homeUi.extendHomeAppsArea,
            bottomWidgetType = homeUi.bottomWidgetType,
            showAndroidWidget = homeUi.showAndroidWidget,
            androidWidgetId = homeUi.androidWidgetId,
            androidWidgetHeight = homeUi.androidWidgetHeight,
            androidWidgetMarginStart = homeUi.androidWidgetMarginStart,
            androidWidgetMarginEnd = homeUi.androidWidgetMarginEnd,
            eventsList = homeUi.eventsList,
            eventsIndex = homeUi.eventsIndex,
            hasCalendarPermission = homeUi.hasCalendarPermission,
            eventsCalendarName = homeUi.eventsCalendarName,
            eventsHideControls = prefs.eventsHideControls,
            eventsCalendarId = prefs.eventsCalendarId,
            eventsFilter = prefs.eventsFilter,
            shortcutLeftIcon = homeUi.shortcutLeftIcon,
            shortcutLeftAction = homeUi.shortcutLeftAction,
            shortcutRightIcon = homeUi.shortcutRightIcon,
            shortcutRightAction = homeUi.shortcutRightAction,
            shortcutPageDots = homeUi.shortcutPageDots,
            shortcutHideOutline = homeUi.shortcutHideOutline,
            totalUsageText = formatDailyUsage(dailyUsageMs)
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
            totalPages = prefs.homePagesNum,
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
            clockStyle = 0,
            homeAlignment = 0,
            clockAlignment = 0,
            dateAlignment = 0,
            quoteAlignment = 0,
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
            iconSourceMode = 0,
            iconShape = 0,
            iconTintContrast = 10,
            selectedIconPackPackage = "",
            iconCodes = emptyMap(),
            clockCustomFontPath = null,
            dateCustomFontPath = null,
            quoteCustomFontPath = null,
            notificationCustomFontPath = null,
            extendHomeAppsArea = false,
            bottomWidgetType = "quote",
            showAndroidWidget = false,
            androidWidgetId = -1,
            androidWidgetHeight = 120,
            androidWidgetMarginStart = 0,
            androidWidgetMarginEnd = 0,
            eventsList = emptyList(),
            eventsIndex = 0,
            hasCalendarPermission = false,
            eventsCalendarName = "",
            eventsHideControls = prefs.eventsHideControls,
            eventsCalendarId = -1L,
            eventsFilter = 1
        )
    )

    // API to update counts from UI
    fun setHomeAppsNum(value: Int) {
        prefs.homeAppsNum = value
        _homeUiState.value = _homeUiState.value.copy(homeAppsNum = value)
    }

    fun setHomePagesNum(value: Int) {
        prefs.homePagesNum = value
        _homeUiState.value = _homeUiState.value.copy(homePagesNum = value)
    }

    fun setEdgeSwipeBackEnabled(enabled: Boolean) {
        try {
            prefs.edgeSwipeBackEnabled = enabled
            _homeUiState.value = _homeUiState.value.copy(edgeSwipeBackEnabled = enabled)
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setEdgeSwipeBackEnabled failed", e)
        }
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
        notificationIndicatorStyle = prefs.notificationIndicatorStyle,
        hapticFeedback = prefs.hapticFeedback,

        lockedApps = prefs.lockedApps,
        newlyInstalledApps = prefs.newlyInstalledApps,
        appTheme = prefs.appTheme,
        appDrawerSearchEnabled = prefs.appDrawerSearchEnabled,
        appDrawerAutoLaunch = prefs.appDrawerAutoLaunch,
        appDrawerAutoShowKeyboard = prefs.appDrawerAutoShowKeyboard,
        appDrawerSearchContactsEnabled = prefs.appDrawerSearchContactsEnabled,
        appDrawerSearchContactAccounts = prefs.appDrawerSearchContactAccounts,
        appDrawerSearchWebEnabled = prefs.appDrawerSearchWebEnabled,
        appDrawerSearchSettingsEnabled = prefs.appDrawerSearchSettingsEnabled,
        appDrawerSearchMusicEnabled = prefs.appDrawerSearchMusicEnabled,
        appDrawerSearchFilesEnabled = prefs.appDrawerSearchFilesEnabled,
        appDrawerSearchHiddenAppsEnabled = prefs.appDrawerSearchHiddenAppsEnabled,
        extendHomeAppsArea = prefs.extendHomeAppsArea,
        textIslandsShape = prefs.textIslandsShape,
        drawerShowIcons = prefs.drawerShowIcons,
        iconSourceMode = prefs.iconSourceMode,
        iconShape = prefs.iconShape,
        iconTintContrast = prefs.iconTintContrast,
        selectedIconPackPackage = prefs.selectedIconPackPackage,
        recentsDefaultView = prefs.recentsDefaultView,
        recentsUsageFilter = prefs.recentsUsageFilter,
        recentsUsageUnit = prefs.recentsUsageUnit,
        recentsUnitCost = prefs.recentsUnitCost,
        recentsUnitCurrencyChar = prefs.recentsUnitCurrencyChar,
        recentsUnitCoffeePrice = prefs.recentsUnitCoffeePrice,
        recentsUnitEmojiChar = prefs.recentsUnitEmojiChar,
        appDrawerSortOrder = prefs.appDrawerSortOrder
    ))
    val appsDrawerUiState: StateFlow<AppsDrawerUiState> = _appsDrawerUiState.asStateFlow()

    init {
        try {
            if (!prefs.initialLaunchCompleted) {
                prefs.invalidateAppsPerPageCache()
                prefs.initialLaunchCompleted = true
            }
        } catch (_: Exception) {
            // ignore
        }
        IconUtility.tintContrast = prefs.iconTintContrast / 10f
        if (prefs.appDrawerSortOrder != 0) {
            loadAppUsageStats()
        }
        // Pre-load daily usage for Total Usage widget
        if (prefs.bottomWidgetType == Constants.BottomWidgetType.TotalUsage.value) {
            loadDailyTotalUsage()
        }
    }

    // ==============================================================================
    // ================================================================================

    init {
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

        viewModelScope.launch {
            prefs.edgeSwipeBackEnabledFlow.collect { enabled ->
                _homeUiState.value = _homeUiState.value.copy(edgeSwipeBackEnabled = enabled)
            }
        }

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
        viewModelScope.launch {
            combine(
                appsRepository.appList,
                appsRepository.hiddenAppsList,
                appsRepository.azLetters
            ) { appList, hiddenApps, azLetters ->
                Triple(appList, hiddenApps, azLetters)
            }.collect { (appList, hiddenApps, azLetters) ->
                _appsDrawerUiState.value = _appsDrawerUiState.value.copy(
                    appList = appList,
                    hiddenApps = hiddenApps,
                    azLetters = azLetters
                )
            }
        }
        // Observe private space state separately
        viewModelScope.launch {
            combine(
                appsRepository.privateSpaceApps,
                appsRepository.hasPrivateSpace
            ) { psApps, hasPS -> psApps to hasPS }
            .collect { (psApps, hasPS) ->
                _appsDrawerUiState.value = _appsDrawerUiState.value.copy(
                    privateSpaceApps = psApps,
                    hasPrivateSpace = hasPS
                )
            }
        }
        
        // Preload app list for instant app drawer opening
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
        appsRepository.refreshHiddenApps()

        viewModelScope.launch(Dispatchers.Default) {
            // Wait for app list to be populated
            appsRepository.appList.first { it.isNotEmpty() }
            preWarmDrawerIcons()
        }
    }

    private fun updateStateFromPreference(key: String) {
        val currentState = _homeUiState.value
        _homeUiState.value = when (key) {
            PrefKeys.APP_THEME -> currentState.copy(appTheme = prefs.appTheme)
            PrefKeys.TEXT_COLOR, PrefKeys.DARK_TEXT_COLOR -> try { currentState.copy(textColor = resolveThemeColors(appContext).first) } catch (_: Exception) { currentState.copy(textColor = android.graphics.Color.BLACK) }
            PrefKeys.BACKGROUND_COLOR, PrefKeys.DARK_BACKGROUND_COLOR -> try { currentState.copy(backgroundColor = resolveThemeColors(appContext).second) } catch (_: Exception) { currentState.copy(backgroundColor = android.graphics.Color.WHITE) }
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
            PrefKeys.SHOW_ANDROID_WIDGET -> currentState.copy(showAndroidWidget = prefs.showAndroidWidget)
            PrefKeys.BOTTOM_WIDGET_TYPE -> currentState.copy(bottomWidgetType = prefs.bottomWidgetType, showQuote = prefs.showQuote, showAndroidWidget = prefs.showAndroidWidget)
            PrefKeys.ANDROID_WIDGET_ID -> currentState.copy(androidWidgetId = prefs.androidWidgetId)
            PrefKeys.ANDROID_WIDGET_HEIGHT -> currentState.copy(androidWidgetHeight = prefs.androidWidgetHeight)
            PrefKeys.ANDROID_WIDGET_MARGIN_START -> currentState.copy(androidWidgetMarginStart = prefs.androidWidgetMarginStart)
            PrefKeys.ANDROID_WIDGET_MARGIN_END -> currentState.copy(androidWidgetMarginEnd = prefs.androidWidgetMarginEnd)
            PrefKeys.SHOW_DATE -> currentState.copy(showDate = prefs.showDate)
            PrefKeys.SHOW_CLOCK -> currentState.copy(showClock = prefs.showClock)
            PrefKeys.SHOW_AM_PM -> currentState.copy(showAmPm = prefs.showAmPm)
            PrefKeys.SHOW_SECOND_CLOCK -> currentState.copy(showSecondClock = prefs.showSecondClock)
            PrefKeys.SECOND_CLOCK_OFFSET_HOURS -> currentState.copy(secondClockOffsetHours = prefs.secondClockOffsetHours)
            PrefKeys.CLOCK_MODE -> currentState.copy(clockMode = prefs.clockMode)
            PrefKeys.CLOCK_STYLE -> currentState.copy(clockStyle = prefs.clockStyle)
            PrefKeys.SHOW_DATE_BATTERY_COMBO -> currentState.copy(showDateBatteryCombo = prefs.showDateBatteryCombo)
            PrefKeys.SHOW_NOTIFICATION_COUNT -> currentState.copy(showNotificationCount = prefs.showNotificationCount)
            PrefKeys.NOTIFICATION_COUNT_SOURCE -> currentState.copy(notificationCountSource = prefs.notificationCountSource)
            PrefKeys.BACKGROUND_OPACITY -> currentState.copy(backgroundOpacity = prefs.backgroundOpacity)
            // Update new fields
            PrefKeys.HOME_ALIGNMENT -> currentState.copy(homeAlignment = prefs.homeAlignment)
            PrefKeys.HOME_CLOCK_ALIGNMENT -> currentState.copy(clockAlignment = prefs.homeClockAlignment)
            PrefKeys.HOME_DATE_ALIGNMENT -> currentState.copy(dateAlignment = prefs.homeDateAlignment)
            PrefKeys.HOME_QUOTE_ALIGNMENT -> currentState.copy(quoteAlignment = prefs.homeQuoteAlignment)
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
            PrefKeys.APP_DRAWER_SEARCH_HIDDEN_APPS -> currentState.copy(appDrawerSearchHiddenAppsEnabled = prefs.appDrawerSearchHiddenAppsEnabled)
            PrefKeys.APP_DRAWER_AUTO_LAUNCH -> currentState.copy(appDrawerAutoLaunch = prefs.appDrawerAutoLaunch)
            PrefKeys.APP_DRAWER_AUTO_SHOW_KEYBOARD -> currentState.copy(appDrawerAutoShowKeyboard = prefs.appDrawerAutoShowKeyboard)
            PrefKeys.ALL_CAPS_APPS -> currentState.copy(allCapsApps = prefs.allCapsApps)
            PrefKeys.SMALL_CAPS_APPS -> currentState.copy(smallCapsApps = prefs.smallCapsApps)
            PrefKeys.SHOW_NOTIFICATION_BADGE -> currentState.copy(showNotificationBadge = prefs.showNotificationBadge)
            PrefKeys.NOTIFICATION_INDICATOR_STYLE -> currentState.copy(notificationIndicatorStyle = prefs.notificationIndicatorStyle)
            PrefKeys.HAPTIC_FEEDBACK -> currentState.copy(hapticFeedback = prefs.hapticFeedback)
            PrefKeys.VIBRATION_SCALE -> currentState.copy(vibrationScale = prefs.vibrationScale)
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
            PrefKeys.APPS_FONT,
            "universal_font",
            "universal_font_enabled" -> {
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
            PrefKeys.NOTIFICATION_INDICATOR_STYLE -> currentAppsState.copy(notificationIndicatorStyle = prefs.notificationIndicatorStyle)
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
            PrefKeys.APP_DRAWER_SEARCH_HIDDEN_APPS -> currentAppsState.copy(appDrawerSearchHiddenAppsEnabled = prefs.appDrawerSearchHiddenAppsEnabled)
            PrefKeys.TEXT_ISLANDS_SHAPE -> currentAppsState.copy(textIslandsShape = prefs.textIslandsShape)
            PrefKeys.DRAWER_SHOW_ICONS -> currentAppsState.copy(drawerShowIcons = prefs.drawerShowIcons)
            PrefKeys.SHOW_ICONS -> currentAppsState.copy(iconSourceMode = prefs.iconSourceMode, selectedIconPackPackage = prefs.selectedIconPackPackage)
            else -> currentAppsState
        }
    }

    init {
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
        // clockMode: 0=System, 1=Force24, 2=Force12
        val prefMode = _homeUiState.value.clockMode
        val is24HourSystem = android.text.format.DateFormat.is24HourFormat(appContext)
        val effectiveIs24Hour = when (prefMode) {
            1 -> true
            2, 3 -> false
            else -> is24HourSystem
        }
        val clockPattern = when {
            effectiveIs24Hour -> "HH:mm"
            prefMode == 3 -> "h:mm"
            else -> "hh:mm"
        }
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

        _clockState.value = ClockState(
            clockText = clockText,
            amPmText = amPmText,
            is24Hour = effectiveIs24Hour,
            secondClockText = secondClockText,
            secondAmPmText = secondAmPmText
        )
    }

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


    private val _typedCharEvents = MutableSharedFlow<Char>(replay = 4, extraBufferCapacity = 32)
    val typedCharEvents = _typedCharEvents.asSharedFlow()

    private val _backspaceEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val backspaceEvents = _backspaceEvents.asSharedFlow()

    fun emitTypedChar(ch: Char) {
        viewModelScope.launch { try { _typedCharEvents.emit(ch) } catch (e: Exception) { android.util.Log.w("MainViewModel", "emitTypedChar failed", e) } }
    }

    fun emitBackspaceEvent() {
        viewModelScope.launch { try { _backspaceEvents.emit(Unit) } catch (e: Exception) { android.util.Log.w("MainViewModel", "emitBackspaceEvent failed", e) } }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun clearTypedCharReplay() {
        try {
            _typedCharEvents.resetReplayCache()
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "clearTypedCharReplay failed", e)
        }
    }


    fun requestAppDrawerPageUp() {
        viewModelScope.launch { _appDrawerPageRequests.emit(1) }
    }

    fun requestAppDrawerPageDown() {
        viewModelScope.launch { _appDrawerPageRequests.emit(-1) }
    }

    fun refreshHomeAppsUiState(context: Context) {
        val notifications =
            NotificationManager.getInstance(context).notificationInfoState.value
        val textColor = try { resolveThemeColors(context).first } catch (_: Exception) { android.graphics.Color.BLACK }
        val backgroundColor = try { resolveThemeColors(context).second } catch (_: Exception) { android.graphics.Color.WHITE }
        val appFont = prefs.getFontForContext("apps")
            .getFont(context, prefs.getCustomFontPathForContext("apps"))
        val homeApps = (0 until prefs.homeAppsNum).map { i ->
            val appModel = prefs.getHomeAppModel(i)
            val aliasKey = if (appModel.shortcutId != null) {
                "app_alias_${appModel.activityPackage}_${appModel.shortcutId}"
            } else {
                "app_alias_${appModel.activityPackage}"
            }
            val customLabel = prefs.getAppAlias(aliasKey)
            val label = if (Constants.isSeparator(appModel.activityPackage)) appModel.activityLabel
                        else if (customLabel.isNotEmpty()) customLabel else appModel.activityLabel
            val notificationInfo = notifications[appModel.activityPackage]
            HomeAppUiState(
                id = i,
                label = label,
                font = appFont,
                color = textColor,
                notificationInfo = notificationInfo,
                activityPackage = appModel.activityPackage,
                activityClass = appModel.activityClass,
                user = appModel.user,
                shortcutId = appModel.shortcutId
            )
        }
        val labels = homeApps.map { it.label }
        appsRepository.updateIconCodes(labels, prefs.showIcons)
        
        // Update State for instant recomposition
        _homeUiState.value = _homeUiState.value.copy(
            homeApps = homeApps,
            textColor = textColor,
            backgroundColor = backgroundColor
        )

        refreshAudioWidgetState(context)

        if (_homeUiState.value.bottomWidgetType == Constants.BottomWidgetType.Events.value) {
            loadEvents(context)
        }
    }

    private fun refreshAudioWidgetState(context: Context) {
        try {
            val audioWidgetHelper = AudioWidgetHelper.getInstance(context)
            audioWidgetHelper.resetDismissalState()
        } catch (_: Exception) {
            // Ignore errors during widget refresh
        }
    }

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
            AppDrawerFlag.SetShortcutLeft -> {
                prefs.appShortcutLeft = app
                prefs.shortcutLeftAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(shortcutLeftAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            AppDrawerFlag.SetShortcutRight -> {
                prefs.appShortcutRight = app
                prefs.shortcutRightAction = Constants.Action.OpenApp
                try { _homeUiState.value = _homeUiState.value.copy(shortcutRightAction = Constants.Action.OpenApp) } catch (_: Exception) {}
            }
            else -> { /* LaunchApp, HiddenApps handled by launchApp() */ }
        }
    }

    /**
     * Save the ordered list of favorite apps from Edit Favorites mode.
     * Each app is stored by position index. Empty slots are cleared.
     */
    fun saveEditFavorites(favorites: List<AppListItem>) {
        val maxApps = prefs.homeAppsNum
        for (i in 0 until maxApps) {
            if (i < favorites.size) {
                prefs.setHomeAppModel(i, favorites[i])
            } else {
                // Clear the slot by storing an empty app
                prefs.setHomeAppModel(i, AppListItem(
                    activityLabel = "",
                    activityPackage = "",
                    activityClass = "",
                    user = Process.myUserHandle(),
                    customLabel = ""
                ))
            }
        }
        refreshHomeAppsUiState(appContext)
    }

    /**
     * Replace the hidden-apps set with the selection from the multi-select editor.
     * Uses the same key format as hideOrShowApp.
     */
    fun saveEditHiddenApps(selected: List<AppListItem>) {
        val newSet = selected.map { app ->
            if (app.shortcutId != null) {
                "${app.activityPackage}|${app.shortcutId}|${app.user}"
            } else {
                "${app.activityPackage}|${app.user}"
            }
        }.toMutableSet()
        prefs.hiddenApps = newSet
        getAppList(includeHiddenApps = false, flag = null)
        getHiddenApps()
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
        val clamped = mode.coerceIn(0, 3)
        prefs.clockMode = clamped
        _homeUiState.value = _homeUiState.value.copy(clockMode = clamped)
        try {
            updateClockState()
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setClockMode: updateClockState failed", e)
        }
    }

    fun setDateFormatStyle(style: Int) {
        val clamped = style.coerceIn(0, 4)
        prefs.dateFormatStyle = clamped
        _homeUiState.value = _homeUiState.value.copy(dateFormatStyle = clamped)
        try {
            refreshDateBattery(appContext)
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setDateFormatStyle: refreshDateBattery failed", e)
        }
    }

    fun setClockStyle(style: Int) {
        val clamped = style.coerceIn(0, 10)
        prefs.clockStyle = clamped
        // Auto-disable dual clocks for incompatible styles
        val dualClockStyles = intArrayOf(0, 2, 6)
        if (clamped !in dualClockStyles && prefs.showSecondClock) {
            prefs.showSecondClock = false
            _homeUiState.value = _homeUiState.value.copy(clockStyle = clamped, showSecondClock = false)
        } else {
            _homeUiState.value = _homeUiState.value.copy(clockStyle = clamped)
        }
    }

    fun setSecondClockOffsetHours(hours: Int) {
        val clamped = hours.coerceIn(-12, 14)
        prefs.secondClockOffsetHours = clamped
        _homeUiState.value = _homeUiState.value.copy(secondClockOffsetHours = clamped)
        try {
            updateClockState()
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setSecondClockOffsetHours: updateClockState failed", e)
        }
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

    fun setNotificationCountSource(source: Int) {
        _homeUiState.value = _homeUiState.value.copy(notificationCountSource = source)
        prefs.notificationCountSource = source
    }

    fun setShowAudioWidget(visibility: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(showAudioWidget = visibility)
        prefs.showAudioWidgetEnabled = visibility
    }

    fun setBottomWidgetType(type: String) {
        prefs.bottomWidgetType = type
        _homeUiState.value = _homeUiState.value.copy(
            bottomWidgetType = type,
            showQuote = type == Constants.BottomWidgetType.Quote.value,
            showAndroidWidget = type == Constants.BottomWidgetType.AndroidWidget.value
        )
        if (type == Constants.BottomWidgetType.TotalUsage.value) {
            loadDailyTotalUsage()
        }
    }

    fun setShortcutLeftIcon(icon: Constants.ShortcutIcon) {
        prefs.shortcutLeftIcon = icon
        _homeUiState.value = _homeUiState.value.copy(shortcutLeftIcon = icon)
    }

    fun setShortcutLeftAction(action: Constants.Action) {
        prefs.shortcutLeftAction = action
        _homeUiState.value = _homeUiState.value.copy(shortcutLeftAction = action)
    }

    fun setShortcutRightIcon(icon: Constants.ShortcutIcon) {
        prefs.shortcutRightIcon = icon
        _homeUiState.value = _homeUiState.value.copy(shortcutRightIcon = icon)
    }

    fun setShortcutRightAction(action: Constants.Action) {
        prefs.shortcutRightAction = action
        _homeUiState.value = _homeUiState.value.copy(shortcutRightAction = action)
    }

    fun setShortcutPageDots(enabled: Boolean) {
        prefs.shortcutPageDots = enabled
        _homeUiState.value = _homeUiState.value.copy(shortcutPageDots = enabled)
    }

    fun setShortcutHideOutline(enabled: Boolean) {
        prefs.shortcutHideOutline = enabled
        _homeUiState.value = _homeUiState.value.copy(shortcutHideOutline = enabled)
    }

    fun setAndroidWidgetId(widgetId: Int) {
        _homeUiState.value = _homeUiState.value.copy(androidWidgetId = widgetId)
        prefs.androidWidgetId = widgetId
    }

    fun setAndroidWidgetHeight(height: Int) {
        _homeUiState.value = _homeUiState.value.copy(androidWidgetHeight = height)
        prefs.androidWidgetHeight = height
    }

    fun setAndroidWidgetMarginStart(margin: Int) {
        _homeUiState.value = _homeUiState.value.copy(androidWidgetMarginStart = margin)
        prefs.androidWidgetMarginStart = margin
    }

    fun setAndroidWidgetMarginEnd(margin: Int) {
        _homeUiState.value = _homeUiState.value.copy(androidWidgetMarginEnd = margin)
        prefs.androidWidgetMarginEnd = margin
    }

    fun setEventsIndex(index: Int) {
        val current = _homeUiState.value
        val maxIdx = (current.eventsList.size - 1).coerceAtLeast(0)
        val clamped = index.coerceIn(0, maxIdx)
        prefs.eventsIndex = clamped
        _homeUiState.value = current.copy(eventsIndex = clamped)
    }

    fun setEventsCalendar(id: Long, name: String) {
        prefs.eventsCalendarId = id
        prefs.eventsCalendarName = name
        prefs.eventsIndex = 0
        _homeUiState.value = _homeUiState.value.copy(
            eventsCalendarName = name,
            eventsIndex = 0
        )
        loadEvents(appContext)
    }

    fun setEventsFilter(index: Int) {
        prefs.eventsFilter = index.coerceIn(0, 3)
        prefs.eventsIndex = 0
        _homeUiState.value = _homeUiState.value.copy(eventsIndex = 0)
        loadEvents(appContext)
    }

    fun setEventsHideControls(value: Boolean) {
        prefs.eventsHideControls = value
        _homeUiState.value = _homeUiState.value.copy(eventsHideControls = value)
    }

    fun loadEvents(context: Context) {
        viewModelScope.launch {
            val hasPerm = CalendarEventsHelper.hasCalendarPermission(context)
            val list = if (!hasPerm) {
                emptyList<CalendarEventsHelper.CalendarEvent>()
            } else {
                val calId = prefs.eventsCalendarId
                if (calId == -1L) emptyList()  // -1 = not selected, -2 = all calendars
                else withContext(Dispatchers.IO) {
                    CalendarEventsHelper.loadUpcomingEvents(context, calId, prefs.eventsFilter)
                }
            }
            val idx = prefs.eventsIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
            prefs.eventsIndex = idx
            _homeUiState.value = _homeUiState.value.copy(
                hasCalendarPermission = hasPerm,
                eventsList = list,
                eventsIndex = idx,
                eventsCalendarName = prefs.eventsCalendarName
            )
        }
    }

    fun refreshEvents(context: Context) {
        loadEvents(context)
    }

    fun setAppDrawerAutoShowKeyboard(enabled: Boolean) {
        try {
            prefs.appDrawerAutoShowKeyboard = enabled
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setAppDrawerAutoShowKeyboard: prefs write failed", e)
        }
        try {
            _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerAutoShowKeyboard = enabled)
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setAppDrawerAutoShowKeyboard: drawer state update failed", e)
        }
        try {
            _homeUiState.value = _homeUiState.value.copy(appDrawerAutoShowKeyboard = enabled)
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setAppDrawerAutoShowKeyboard: home state update failed", e)
        }
    }

    fun setQuoteText(text: String) {
        _homeUiState.value = _homeUiState.value.copy(quoteText = text)
        prefs.quoteText = text
    }

    fun updateDateAndBatteryText(dateText: String, batteryText: String, isCharging: Boolean) {
        _homeUiState.value = _homeUiState.value.copy(
            dateText = dateText,
            batteryText = batteryText,
            isCharging = isCharging
        )
    }
    
    fun refreshDateBattery(context: Context) {
        try {
            val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                val status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == android.os.BatteryManager.BATTERY_STATUS_FULL
                val batteryLevel = (level * 100 / scale.toFloat()).toInt()

                val datePattern = when (prefs.dateFormatStyle) {
                    1 -> "EEE, MMM d"
                    2 -> "MMM d"
                    3 -> "d MMM"
                    4 -> "EEEE"
                    else -> "EEE, d MMM"
                }
                val dateFormat = java.text.SimpleDateFormat(datePattern, java.util.Locale.getDefault())
                val dateText = dateFormat.format(java.util.Date())
                val batteryText = "$batteryLevel%"
                
                updateDateAndBatteryText(dateText, batteryText, isCharging)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "refreshDateBattery failed", e)
        }
    }
    
    @Deprecated("Use updateDateAndBatteryText instead", ReplaceWith("updateDateAndBatteryText(dateText, \"\", isCharging)"))
    fun updateDateText(dateText: String, isCharging: Boolean) {
        updateDateAndBatteryText(dateText, "", isCharging)
    }


    fun getAppLockKey(app: AppListItem): String {
        return if (app.isShortcut && app.shortcutId != null) {
            "${app.activityPackage}|${app.shortcutId}"
        } else {
            app.activityPackage
        }
    }

    fun isAppLocked(app: AppListItem): Boolean {
        return prefs.lockedApps.contains(getAppLockKey(app))
    }

    fun launchApp(appListItem: AppListItem) {
        launchUnlockedApp(appListItem)
    }

    private fun launchUnlockedApp(appListItem: AppListItem) {
        val packageName = appListItem.activityPackage
        val userHandle = appListItem.user
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        
        // Check if this is a shortcut
        if (appListItem.isShortcut && appListItem.shortcutId != null) {
            launchShortcut(packageName, appListItem.shortcutId, userHandle, launcher)
            return
        }
        
        // Regular app launch
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        if (activityInfo.isNotEmpty()) {
            val component = ComponentName(packageName, activityInfo.first().name)
            launchAppWithPermissionCheck(component, packageName, userHandle, launcher)
        } else {
            appContext.showShortToast("App not found")
        }
    }
    
    /**
     * Launch an app shortcut using LauncherApps.startShortcut()
     */
    private fun launchShortcut(
        packageName: String,
        shortcutId: String,
        userHandle: UserHandle,
        launcher: LauncherApps
    ) {
        try {
            launcher.startShortcut(packageName, shortcutId, null, null, userHandle)
            CrashHandler.logUserAction("$packageName Shortcut $shortcutId Launched")
            // Remove from newly installed apps when launched
            val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
            newlyInstalled.remove(packageName)
            prefs.newlyInstalledApps = newlyInstalled
        } catch (_: SecurityException) {
            // Try with current user handle as fallback
            try {
                launcher.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
                CrashHandler.logUserAction("$packageName Shortcut $shortcutId Launched")
                val newlyInstalled = prefs.newlyInstalledApps.toMutableSet()
                newlyInstalled.remove(packageName)
                prefs.newlyInstalledApps = newlyInstalled
            } catch (_: Exception) {
                appContext.showShortToast("Unable to launch shortcut")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Failed to launch shortcut: ${e.message}")
            try {
                val activityInfo = launcher.getActivityList(packageName, userHandle)
                if (activityInfo.isNotEmpty()) {
                    val component = ComponentName(packageName, activityInfo.first().name)
                    launchAppWithPermissionCheck(component, packageName, userHandle, launcher)
                } else {
                    appContext.showShortToast("Shortcut no longer available")
                }
            } catch (_: Exception) {
                appContext.showShortToast("Unable to launch shortcut")
            }
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

    fun getAppList(includeHiddenApps: Boolean = true, flag: AppDrawerFlag? = null): StateFlow<List<AppListItem>> {
        appsRepository.refreshAppList(includeHiddenApps, flag)
        return if (includeHiddenApps) appsRepository.allAppsList else appsRepository.appList
    }

    fun getHiddenApps() {
        appsRepository.refreshHiddenApps()
    }

    fun isinkosDefault() {
        isinkosDefault(appContext)
    }



    // ================================================================================
    // ================================================================================
    // HOME SCREEN & APP DRAWER OPERATIONS
    // ================================================================================

    fun renameApp(packageName: String, newName: String, flag: AppDrawerFlag? = null, shortcutId: String? = null) {
        // For shortcuts, use format: "package_shortcutId"
        // For regular apps, use format: "package"
        val aliasKey = if (shortcutId != null) {
            "${packageName}_$shortcutId"
        } else {
            packageName
        }
        
        if (newName.isEmpty()) {
            prefs.removeAppAlias(aliasKey)
        } else {
            prefs.setAppAlias(aliasKey, newName)
        }
        getAppList(includeHiddenApps = false, flag = flag)
        getHiddenApps()
    }

    /** Whether an app is currently in the hidden set (handles legacy and new key formats). */
    fun isAppHidden(app: AppListItem): Boolean {
        val hidden = prefs.hiddenApps
        val key = if (app.shortcutId != null) {
            "${app.activityPackage}|${app.shortcutId}|${app.user}"
        } else {
            "${app.activityPackage}|${app.user}"
        }
        return hidden.contains(key) ||
            hidden.contains("${app.activityPackage}|${app.user}") ||
            hidden.contains(app.activityPackage)
    }

    fun hideOrShowApp(flag: AppDrawerFlag, appModel: AppListItem) {
        val newSet = mutableSetOf<String>()
        newSet.addAll(prefs.hiddenApps)

        // For regular apps: use "package|user" format
        val key = if (appModel.shortcutId != null) {
            "${appModel.activityPackage}|${appModel.shortcutId}|${appModel.user}"
        } else {
            "${appModel.activityPackage}|${appModel.user}"
        }

        // Toggle based on the app's actual hidden state, not the screen flag — a hidden
        // app can surface in the normal drawer via "search hidden apps".
        val isHidden = isAppHidden(appModel)
        if (isHidden) {
            // Unhiding - remove both old and new format keys
            newSet.remove(appModel.activityPackage)
            newSet.remove("${appModel.activityPackage}|${appModel.user}")
            newSet.remove(key)
        } else {
            newSet.add(key)
        }
        prefs.hiddenApps = newSet
        getAppList(includeHiddenApps = (flag == AppDrawerFlag.HiddenApps), flag = flag)
        getHiddenApps()
    }

    // Toggle app lock status
    fun toggleAppLock(app: AppListItem) {
        val key = getAppLockKey(app)
        val lockedApps = prefs.lockedApps.toMutableSet()
        if (lockedApps.contains(key)) lockedApps.remove(key)
        else lockedApps.add(key)
        prefs.lockedApps = lockedApps
    }

    // Refresh app list after uninstall
    fun refreshAppListAfterUninstall(includeHiddenApps: Boolean = false) {
        clearAppsPerPageCache()
        getAppList(includeHiddenApps)
    }


    /**
     * Pre-warm the drawer icon cache so ALL icons are ready before the user opens the drawer.
     * Uses cached sizePx from the Fragment's Compose TextMeasurer (persisted in Prefs).
     * Falls back to TextPaint computation on first-ever launch.
     */
    private suspend fun preWarmDrawerIcons() {
        if (!prefs.drawerShowIcons) return
        val iconMode = prefs.iconSourceMode
        if (iconMode == 0) return

        val iconPack = prefs.selectedIconPackPackage
        val customFontPath = try { prefs.getCustomFontPathForContext("apps") } catch (_: Exception) { null }

        val sizePx = prefs.cachedDrawerIconSizePx.let { cached ->
            if (cached > 0) cached
            else {
                val screenScale = run {
                    val override = prefs.uiScaleMode
                    if (override != 0) com.github.gezimos.inkos.style.UiScaleMode.fromId(override).scale
                    else com.github.gezimos.inkos.style.detectScaleMode(appContext).scale
                }
                val dm = appContext.resources.displayMetrics
                val scaledSp = prefs.appDrawerSize * screenScale
                val textSizePx = scaledSp * dm.scaledDensity
                val paint = android.text.TextPaint().apply {
                    textSize = textSizePx
                    try { typeface = prefs.appsFont.getFont(appContext, customFontPath) } catch (_: Exception) {}
                }
                val layout = android.text.StaticLayout.Builder
                    .obtain("Ag", 0, 2, paint, Int.MAX_VALUE)
                    .setIncludePad(false)
                    .build()
                layout.height.coerceAtLeast(1)
            }
        }

        val apps = appsRepository.appList.value
        if (apps.isEmpty()) return

        val ioLimited = Dispatchers.IO.limitedParallelism(16)
        kotlinx.coroutines.coroutineScope {
            apps.forEach { app ->
                launch(ioLimited) {
                    try {
                        IconUtility.loadAppIcon(
                            context = appContext,
                            packageName = app.activityPackage,
                            iconSourceMode = iconMode,
                            selectedIconPackPackage = iconPack,
                            sizePx = sizePx,
                            activityClass = app.activityClass,
                            userHandle = app.user,
                            shortcutId = app.shortcutId,
                            tintArgb = if (iconMode == 4 || iconMode == 5 || iconMode == 6) prefs.textColor else 0,
                            iconShapeId = prefs.iconShape,
                            bgArgb = if (iconMode == 6) prefs.backgroundColor else 0
                        )
                    } catch (_: Exception) {}
                }
            }
        }
    }


    // Get Prefs instance for Compose
    fun getPrefs(): Prefs = prefs

    // ================================================================================
    // SETTINGS UPDATES (SSOT)
    // ================================================================================

    // Notification Settings
    fun setPushNotificationsEnabled(enabled: Boolean) {
        prefs.pushNotificationsEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(pushNotificationsEnabled = enabled)
    }

    fun setShowNotificationBadge(enabled: Boolean) {
        prefs.showNotificationBadge = enabled
        _homeUiState.value = _homeUiState.value.copy(showNotificationBadge = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(showNotificationBadge = enabled)
    }

    fun setNotificationIndicatorStyle(style: Int) {
        prefs.notificationIndicatorStyle = style
        _homeUiState.value = _homeUiState.value.copy(notificationIndicatorStyle = style)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(notificationIndicatorStyle = style)
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
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
    }


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
        NotificationManager.getInstance(appContext).refreshBadgeNotificationState()
    }

    fun setAllowedNotificationApps(apps: Set<String>) {
        prefs.allowedNotificationApps = apps.toMutableSet()
        _homeUiState.value = _homeUiState.value.copy(allowedNotificationApps = apps)
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
        recomputeMaxYOffset()
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
        val labels = _homeUiState.value.homeApps.map { it.label }
        appsRepository.updateIconCodes(labels, enabled)
        if (enabled && !prefs.drawerShowIcons) {
            setDrawerShowIcons(true)
        }
    }

    fun setIconSourceMode(mode: Int) {
        prefs.iconSourceMode = mode
        _homeUiState.value = _homeUiState.value.copy(iconSourceMode = mode)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(iconSourceMode = mode)
        IconUtility.clearCache()
    }

    fun setIconShape(shape: Int) {
        prefs.iconShape = shape
        _homeUiState.value = _homeUiState.value.copy(iconShape = shape)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(iconShape = shape)
    }

    fun setIconTintContrast(value: Int) {
        prefs.iconTintContrast = value
        IconUtility.tintContrast = prefs.iconTintContrast / 10f
        IconUtility.clearCache()
        _homeUiState.value = _homeUiState.value.copy(iconTintContrast = prefs.iconTintContrast)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(iconTintContrast = prefs.iconTintContrast)
        prefs.triggerForceRefreshHome()
    }

    fun setSelectedIconPackPackage(pkg: String) {
        prefs.selectedIconPackPackage = pkg
        _homeUiState.value = _homeUiState.value.copy(selectedIconPackPackage = pkg)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(selectedIconPackPackage = pkg)
        IconUtility.clearCache()
    }

    fun setDrawerShowIcons(enabled: Boolean) {
        prefs.drawerShowIcons = enabled
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(drawerShowIcons = enabled)
    }

    fun setAppDrawerSortOrder(order: Int) {
        prefs.appDrawerSortOrder = order
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSortOrder = order)
        if (order != 0) loadAppUsageStats()
    }

    fun setRecentsDefaultView(v: Int) {
        prefs.recentsDefaultView = v
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(recentsDefaultView = v)
    }

    fun setRecentsUsageFilter(v: Int) {
        prefs.recentsUsageFilter = v
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(recentsUsageFilter = v)
    }

    fun setRecentsUsageUnit(v: Int) {
        prefs.recentsUsageUnit = v
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(recentsUsageUnit = v)
    }

    fun setRecentsUnitCost(v: Int) {
        prefs.recentsUnitCost = v
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(recentsUnitCost = v)
    }

    fun setRecentsUnitCurrencyChar(v: String) {
        prefs.recentsUnitCurrencyChar = v
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(recentsUnitCurrencyChar = v)
    }

    fun setRecentsUnitCoffeePrice(v: Int) {
        prefs.recentsUnitCoffeePrice = v
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(recentsUnitCoffeePrice = v)
    }

    fun setRecentsUnitEmojiChar(v: String) {
        prefs.recentsUnitEmojiChar = v
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(recentsUnitEmojiChar = v)
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
        val max = _homeUiState.value.maxHomeAppsYOffset
        val clamped = offset.coerceIn(Constants.MIN_HOME_APPS_Y_OFFSET, max)
        prefs.homeAppsYOffset = clamped
        _homeUiState.value = _homeUiState.value.copy(homeAppsYOffset = clamped)
    }

    fun updateScreenHeightDp(heightDp: Int) {
        if (_homeUiState.value.screenHeightDp != heightDp) {
            _homeUiState.value = _homeUiState.value.copy(screenHeightDp = heightDp)
            recomputeMaxYOffset()
        }
    }

    fun updateBottomWidgetHeightPx(heightPx: Int) {
        if (_homeUiState.value.bottomWidgetHeightPx != heightPx) {
            _homeUiState.value = _homeUiState.value.copy(bottomWidgetHeightPx = heightPx)
            recomputeMaxYOffset()
        }
    }

    private fun recomputeMaxYOffset() {
        val state = _homeUiState.value
        val screenHDp = state.screenHeightDp
        if (screenHDp <= 0) return
        val density = appContext.resources.displayMetrics.density
        val bottomWidgetDp = (state.bottomWidgetHeightPx / density).toInt()
        val max = ((screenHDp / 2) - bottomWidgetDp - state.bottomWidgetMargin)
            .coerceIn(0, Constants.MAX_HOME_APPS_Y_OFFSET)
        if (state.maxHomeAppsYOffset != max) {
            val newOffset = state.homeAppsYOffset.coerceAtMost(max)
            _homeUiState.value = state.copy(
                maxHomeAppsYOffset = max,
                homeAppsYOffset = newOffset
            )
            if (newOffset != state.homeAppsYOffset) {
                prefs.homeAppsYOffset = newOffset
            }
        }
    }

    fun setTextPaddingSize(size: Int) {
        prefs.textPaddingSize = size
        _homeUiState.value = _homeUiState.value.copy(textPaddingSize = size)
    }

    fun setHomeAlignment(alignment: Int) {
        prefs.homeAlignment = alignment
        _homeUiState.value = _homeUiState.value.copy(homeAlignment = alignment)
    }

    fun setHomeClockAlignment(alignment: Int) {
        prefs.homeClockAlignment = alignment
        _homeUiState.value = _homeUiState.value.copy(clockAlignment = alignment)
    }

    fun setHomeDateAlignment(alignment: Int) {
        prefs.homeDateAlignment = alignment
        _homeUiState.value = _homeUiState.value.copy(dateAlignment = alignment)
    }

    fun setHomeQuoteAlignment(alignment: Int) {
        prefs.homeQuoteAlignment = alignment
        _homeUiState.value = _homeUiState.value.copy(quoteAlignment = alignment)
    }


    // Extras Settings
    fun setEinkRefreshEnabled(enabled: Boolean) {
        prefs.einkRefreshEnabled = enabled
        if (enabled) {
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

    fun setSelectedAppShortcuts(shortcuts: Set<String>?) {
        prefs.selectedAppShortcuts = shortcuts?.toMutableSet() ?: mutableSetOf()
        _homeUiState.value = _homeUiState.value.copy(selectedAppShortcuts = shortcuts)
        // Refresh app list to show/hide app shortcuts
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
    }

    fun setEinkHelperMode(mode: Int) {
        prefs.einkHelperMode = mode
        _homeUiState.value = _homeUiState.value.copy(einkHelperMode = mode)
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
        appsRepository.refreshAppList(includeHiddenApps = false, flag = null)
    }

    fun setAppDrawerAutoLaunch(enabled: Boolean) {
        prefs.appDrawerAutoLaunch = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerAutoLaunch = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerAutoLaunch = enabled)
    }

    fun setAppDrawerSearchContactsEnabled(enabled: Boolean) {
        prefs.appDrawerSearchContactsEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchContactsEnabled = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchContactsEnabled = enabled)
    }

    fun setAppDrawerSearchContactAccounts(accounts: Set<String>?) {
        prefs.appDrawerSearchContactAccounts = accounts?.toMutableSet()
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchContactAccounts = accounts)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchContactAccounts = accounts)
    }

    fun setAppDrawerSearchWebEnabled(enabled: Boolean) {
        prefs.appDrawerSearchWebEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchWebEnabled = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchWebEnabled = enabled)
    }

    fun setAppDrawerSearchSettingsEnabled(enabled: Boolean) {
        prefs.appDrawerSearchSettingsEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchSettingsEnabled = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchSettingsEnabled = enabled)
    }

    fun setAppDrawerSearchMusicEnabled(enabled: Boolean) {
        prefs.appDrawerSearchMusicEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchMusicEnabled = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchMusicEnabled = enabled)
    }

    fun setAppDrawerSearchFilesEnabled(enabled: Boolean) {
        prefs.appDrawerSearchFilesEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchFilesEnabled = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchFilesEnabled = enabled)
    }

    fun setAppDrawerSearchHiddenAppsEnabled(enabled: Boolean) {
        prefs.appDrawerSearchHiddenAppsEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(appDrawerSearchHiddenAppsEnabled = enabled)
        _appsDrawerUiState.value = _appsDrawerUiState.value.copy(appDrawerSearchHiddenAppsEnabled = enabled)
    }

    // ---------------------- Font / Text setters ----------------------
    fun setFontFamily(font: Constants.FontFamily) {
        prefs.fontFamily = font
        updateStateFromPreference(PrefKeys.APPS_FONT)
    }

    fun setUniversalFont(font: Constants.FontFamily) {
        prefs.universalFont = font
        _homeUiState.value = _homeUiState.value.copy(
            appsFont = prefs.appsFont,
            clockFont = prefs.clockFont,
            quoteFont = prefs.quoteFont,
            notificationsFont = prefs.notificationsFont,
            notificationFont = prefs.labelnotificationsFont,
            statusFont = prefs.statusFont,
            lettersFont = prefs.lettersFont,
            lettersTitleFont = prefs.lettersTitleFont,
            dateFont = prefs.dateFont,
        )
    }

    fun setUniversalFontEnabled(enabled: Boolean) {
        prefs.universalFontEnabled = enabled
        _homeUiState.value = _homeUiState.value.copy(
            appsFont = prefs.appsFont,
            clockFont = prefs.clockFont,
            quoteFont = prefs.quoteFont,
            notificationsFont = prefs.notificationsFont,
            notificationFont = prefs.labelnotificationsFont,
            statusFont = prefs.statusFont,
            lettersFont = prefs.lettersFont,
            lettersTitleFont = prefs.lettersTitleFont,
            dateFont = prefs.dateFont,
        )
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
        _homeUiState.value = _homeUiState.value.copy(lettersTitle = value)
    }

    fun setLettersTitleSize(size: Int) {
        prefs.lettersTitleSize = size
        _homeUiState.value = _homeUiState.value.copy(lettersTitleSize = size)
    }

    fun setOnboardingPage(page: Int) {
        prefs.onboardingPage = page
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setFirstOpen(open: Boolean) {
        prefs.firstOpen = open
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setLockModeOn(enabled: Boolean) {
        prefs.lockModeOn = enabled
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setLettersTextSize(size: Int) {
        prefs.lettersTextSize = size
        _homeUiState.value = _homeUiState.value.copy()
    }

    fun setNotificationsTextSize(size: Int) {
        prefs.notificationsTextSize = size
        _homeUiState.value = _homeUiState.value.copy(notificationsTextSize = size)
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
        try {
            _homeUiState.value = _homeUiState.value.copy(
                clockCustomFontPath = try { prefs.getCustomFontPathForContext("clock") } catch (_: Exception) { null },
                dateCustomFontPath = try { prefs.getCustomFontPathForContext("date") } catch (_: Exception) { null },
                quoteCustomFontPath = try { prefs.getCustomFontPathForContext("quote") } catch (_: Exception) { null },
                notificationCustomFontPath = try { prefs.getCustomFontPathForContext("notification") } catch (_: Exception) { null }
            )
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setCustomFontPath: home state update failed", e)
        }

        try {
            _appsDrawerUiState.value = _appsDrawerUiState.value.copy(
                customFontPath = try { prefs.getCustomFontPathForContext("apps") } catch (_: Exception) { null }
            )
        } catch (e: Exception) {
            android.util.Log.w("MainViewModel", "setCustomFontPath: drawer state update failed", e)
        }
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

    fun setVibrationScale(scale: Int) {
        val clamped = scale.coerceIn(0, 500)
        prefs.vibrationScale = clamped
        _homeUiState.value = _homeUiState.value.copy(vibrationScale = clamped)
    }

    fun setShowStatusBar(enabled: Boolean) {
        prefs.showStatusBar = enabled
        _homeUiState.value = _homeUiState.value.copy(showStatusBar = enabled)
    }

    fun setShowNavigationBar(enabled: Boolean) {
        prefs.showNavigationBar = enabled
        _homeUiState.value = _homeUiState.value.copy(showNavigationBar = enabled)
        clearAppsPerPageCache()
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