package com.github.gezimos.inkos.data.screentime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import com.github.gezimos.inkos.data.Prefs
import java.util.Calendar

class ScreenTimeReader(private val context: Context) {

    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasUsageAccess(): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    // Events are read from a day before the window so that a session already running at
    // midnight is seen from its resume; the calculator clips it to the window.
    fun today(categories: ScreenTimeCategories): ScreenTimeSummary {
        val start = startOfToday()
        val end = System.currentTimeMillis()
        val events = readEvents(start - SESSION_LOOKBACK_MS, end)
        return ScreenTimeCalculator.summarize(events, start, end, categories::of)
    }

    // Android keeps raw events and daily buckets for only about ten days, so longer periods use
    // the coarser weekly/monthly/yearly buckets that INTERVAL_BEST picks.
    fun period(start: Long, end: Long, categories: ScreenTimeCategories): ScreenTimeSummary {
        val foregroundMs = usageStatsManager
            .queryAndAggregateUsageStats(start, end)
            .mapValues { (_, stats) -> stats.totalTimeInForeground }
        return ScreenTimeCalculator.summarizeTotals(foregroundMs, countUnlocks(start, end), categories::of)
    }

    fun categories(prefs: Prefs): ScreenTimeCategories = ScreenTimeCategories(
        countedPackages = launchablePackages(),
        offlinePackages = prefs.screenTimeOfflineApps,
        uncountedPackages = prefs.screenTimeUncountedApps
    )

    private fun launchablePackages(): Set<String> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return launcherApps.getActivityList(null, Process.myUserHandle())
            .map { it.applicationInfo.packageName }
            .filterNot { it in INKOS_PACKAGES || it == context.packageName }
            .toSet()
    }

    private fun readEvents(start: Long, end: Long): List<UsageEvent> {
        val usageEvents = usageStatsManager.queryEvents(start, end)
        val raw = UsageEvents.Event()
        val events = mutableListOf<UsageEvent>()
        while (usageEvents.getNextEvent(raw)) {
            val type = when (raw.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Type.ActivityResumed
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEvent.Type.ActivityPaused
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEvent.Type.ScreenOff
                UsageEvents.Event.KEYGUARD_HIDDEN -> UsageEvent.Type.Unlocked
                else -> null
            } ?: continue
            events += UsageEvent(raw.timeStamp, type, raw.packageName.orEmpty(), raw.className.orEmpty())
        }
        return events
    }

    private fun countUnlocks(start: Long, end: Long): Int {
        if (!unlocksSupported) return 0
        return usageStatsManager
            .queryEventStats(UsageStatsManager.INTERVAL_BEST, start, end)
            .filter { it.eventType == UsageEvents.Event.KEYGUARD_HIDDEN }
            .sumOf { it.count }
    }

    companion object {
        private val INKOS_PACKAGES = setOf("app.inkos", "app.inkos.debug")
        private const val SESSION_LOOKBACK_MS = 24L * 60 * 60 * 1000

        val unlocksSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        fun startOfToday(): Long = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
