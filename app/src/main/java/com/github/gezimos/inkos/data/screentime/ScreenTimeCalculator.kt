package com.github.gezimos.inkos.data.screentime

enum class ScreenTimeCategory { Online, Offline, Uncounted }

data class ScreenTimeSummary(
    val onlineMs: Long = 0L,
    val offlineMs: Long = 0L,
    val unlocks: Int = 0
) {
    val totalMs: Long get() = onlineMs + offlineMs
}

data class UsageEvent(
    val timestamp: Long,
    val type: Type,
    val packageName: String = "",
    val className: String = ""
) {
    enum class Type { ActivityResumed, ActivityPaused, ScreenOff, Unlocked }
}

object ScreenTimeCalculator {

    fun summarize(
        events: List<UsageEvent>,
        windowStart: Long,
        windowEnd: Long,
        categoryOf: (String) -> ScreenTimeCategory
    ): ScreenTimeSummary {
        val unlocks = events.count {
            it.type == UsageEvent.Type.Unlocked && it.timestamp in windowStart until windowEnd
        }
        return summarizeTotals(foregroundTimeByPackage(events, windowStart, windowEnd), unlocks, categoryOf)
    }

    fun summarizeTotals(
        foregroundMsByPackage: Map<String, Long>,
        unlocks: Int,
        categoryOf: (String) -> ScreenTimeCategory
    ): ScreenTimeSummary {
        var online = 0L
        var offline = 0L
        foregroundMsByPackage.forEach { (pkg, ms) ->
            when (categoryOf(pkg)) {
                ScreenTimeCategory.Online -> online += ms
                ScreenTimeCategory.Offline -> offline += ms
                ScreenTimeCategory.Uncounted -> Unit
            }
        }
        return ScreenTimeSummary(onlineMs = online, offlineMs = offline, unlocks = unlocks)
    }

    fun foregroundTimeByPackage(events: List<UsageEvent>, windowStart: Long, windowEnd: Long): Map<String, Long> {
        val totals = mutableMapOf<String, Long>()
        val foregroundSince = mutableMapOf<String, Long>()
        val resumedCounts = mutableMapOf<String, MutableMap<String, Int>>()

        fun leaveForeground(pkg: String, at: Long) {
            val since = foregroundSince.remove(pkg) ?: return
            val ms = minOf(at, windowEnd) - maxOf(since, windowStart)
            if (ms > 0) totals[pkg] = (totals[pkg] ?: 0L) + ms
        }

        for (event in events) {
            val pkg = event.packageName
            when (event.type) {
                UsageEvent.Type.ActivityResumed -> {
                    val counts = resumedCounts.getOrPut(pkg) { mutableMapOf() }
                    if (counts.isEmpty()) foregroundSince[pkg] = event.timestamp
                    counts[event.className] = (counts[event.className] ?: 0) + 1
                }
                UsageEvent.Type.ActivityPaused -> {
                    val counts = resumedCounts[pkg] ?: continue
                    val count = counts[event.className] ?: continue
                    if (count > 1) counts[event.className] = count - 1 else counts.remove(event.className)
                    if (counts.isEmpty()) {
                        resumedCounts.remove(pkg)
                        leaveForeground(pkg, event.timestamp)
                    }
                }
                UsageEvent.Type.ScreenOff -> {
                    foregroundSince.keys.toList().forEach { leaveForeground(it, event.timestamp) }
                    resumedCounts.clear()
                }
                UsageEvent.Type.Unlocked -> Unit
            }
        }
        foregroundSince.keys.toList().forEach { leaveForeground(it, windowEnd) }
        return totals
    }
}

class ScreenTimeCategories(
    private val countedPackages: Set<String>,
    private val offlinePackages: Set<String>,
    private val uncountedPackages: Set<String>
) {
    fun of(packageName: String): ScreenTimeCategory = when (packageName) {
        !in countedPackages, in uncountedPackages -> ScreenTimeCategory.Uncounted
        in offlinePackages -> ScreenTimeCategory.Offline
        else -> ScreenTimeCategory.Online
    }
}
