package com.github.gezimos.inkos.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeLineTest {

    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun `under an hour shows only minutes`() {
        assertEquals("0min", formatScreenTimeDuration(59_999))
        assertEquals("25min", formatScreenTimeDuration(25 * minute))
    }

    @Test
    fun `whole hours omit minutes`() {
        assertEquals("2h", formatScreenTimeDuration(2 * hour))
    }

    @Test
    fun `hours and minutes are joined by a non breaking space`() {
        assertEquals("1h 30min", formatScreenTimeDuration(hour + 30 * minute))
    }

    @Test
    fun `durations beyond a day keep counting hours`() {
        assertEquals("312h 5min", formatScreenTimeDuration(312 * hour + 5 * minute))
    }
}
