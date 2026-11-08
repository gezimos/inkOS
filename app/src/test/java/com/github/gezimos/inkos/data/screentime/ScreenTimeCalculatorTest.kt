package com.github.gezimos.inkos.data.screentime

import com.github.gezimos.inkos.data.screentime.UsageEvent.Type.ActivityPaused
import com.github.gezimos.inkos.data.screentime.UsageEvent.Type.ActivityResumed
import com.github.gezimos.inkos.data.screentime.UsageEvent.Type.ScreenOff
import com.github.gezimos.inkos.data.screentime.UsageEvent.Type.Unlocked
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeCalculatorTest {

    private val start = 1_000L
    private val end = 10_000L

    private fun resumed(t: Long, pkg: String, cls: String = "Main") = UsageEvent(t, ActivityResumed, pkg, cls)
    private fun paused(t: Long, pkg: String, cls: String = "Main") = UsageEvent(t, ActivityPaused, pkg, cls)
    private fun screenOff(t: Long) = UsageEvent(t, ScreenOff)
    private fun unlocked(t: Long) = UsageEvent(t, Unlocked)

    private fun foreground(vararg events: UsageEvent) =
        ScreenTimeCalculator.foregroundTimeByPackage(events.toList(), start, end)

    @Test
    fun `counts time between resume and pause`() {
        assertEquals(mapOf("a" to 500L), foreground(resumed(2_000, "a"), paused(2_500, "a")))
    }

    @Test
    fun `sums several sessions of the same app`() {
        val result = foreground(
            resumed(2_000, "a"), paused(2_500, "a"),
            resumed(3_000, "a"), paused(3_200, "a")
        )
        assertEquals(mapOf("a" to 700L), result)
    }

    @Test
    fun `switching activities inside one app does not split or double count`() {
        val result = foreground(
            resumed(2_000, "a", "List"),
            resumed(2_400, "a", "Detail"),
            paused(2_401, "a", "List"),
            paused(3_000, "a", "Detail")
        )
        assertEquals(mapOf("a" to 1_000L), result)
    }

    @Test
    fun `session still open at window end is counted up to window end`() {
        assertEquals(mapOf("a" to 1_000L), foreground(resumed(9_000, "a")))
    }

    @Test
    fun `session started before window is counted from window start`() {
        assertEquals(mapOf("a" to 500L), foreground(resumed(500, "a"), paused(1_500, "a")))
    }

    @Test
    fun `pause without a matching resume is ignored`() {
        assertEquals(emptyMap<String, Long>(), foreground(paused(1_500, "a")))
    }

    @Test
    fun `pause after an already closed session is not counted`() {
        val result = foreground(resumed(2_000, "a"), screenOff(2_100), paused(2_200, "a"))
        assertEquals(mapOf("a" to 100L), result)
    }

    @Test
    fun `session started before window keeps counting when another activity of the app opens first`() {
        val result = foreground(
            resumed(500, "a", "Main"),
            resumed(1_800, "a", "Detail"),
            paused(1_801, "a", "Main"),
            paused(2_000, "a", "Detail")
        )
        assertEquals(mapOf("a" to 1_000L), result)
    }

    @Test
    fun `two resumed instances of the same activity keep the app in foreground until both pause`() {
        val result = foreground(
            resumed(2_000, "a", "Tab"),
            resumed(2_100, "a", "Tab"),
            paused(2_200, "a", "Tab"),
            paused(3_000, "a", "Tab")
        )
        assertEquals(mapOf("a" to 1_000L), result)
    }

    @Test
    fun `screen off closes every open session`() {
        val result = foreground(resumed(2_000, "a"), resumed(2_000, "b"), screenOff(2_300))
        assertEquals(mapOf("a" to 300L, "b" to 300L), result)
    }

    @Test
    fun `session ending after window is clipped to window end`() {
        assertEquals(mapOf("b" to 500L), foreground(resumed(9_500, "b"), paused(11_000, "b")))
    }

    @Test
    fun `summary splits time by category and skips uncounted apps`() {
        val events = listOf(
            resumed(2_000, "browser"), paused(3_000, "browser"),
            resumed(3_000, "reader"), paused(5_000, "reader"),
            resumed(5_000, "camera"), paused(6_000, "camera")
        )
        val categories = mapOf("reader" to ScreenTimeCategory.Offline, "camera" to ScreenTimeCategory.Uncounted)

        val summary = ScreenTimeCalculator.summarize(events, start, end) {
            categories[it] ?: ScreenTimeCategory.Online
        }

        assertEquals(ScreenTimeSummary(onlineMs = 1_000, offlineMs = 2_000, unlocks = 0), summary)
        assertEquals(3_000L, summary.totalMs)
    }

    @Test
    fun `summary counts only unlocks inside the window`() {
        val events = listOf(unlocked(500), unlocked(2_000), unlocked(9_999), unlocked(10_000))

        val summary = ScreenTimeCalculator.summarize(events, start, end) { ScreenTimeCategory.Online }

        assertEquals(2, summary.unlocks)
    }

    @Test
    fun `uncounted wins over offline and non launchable apps are never counted`() {
        val categories = ScreenTimeCategories(
            countedPackages = setOf("browser", "reader", "both"),
            offlinePackages = setOf("reader", "both", "hidden-system"),
            uncountedPackages = setOf("both")
        )

        assertEquals(ScreenTimeCategory.Online, categories.of("browser"))
        assertEquals(ScreenTimeCategory.Offline, categories.of("reader"))
        assertEquals(ScreenTimeCategory.Uncounted, categories.of("both"))
        assertEquals(ScreenTimeCategory.Uncounted, categories.of("hidden-system"))
    }
}
