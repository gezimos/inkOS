package com.github.gezimos.inkos.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class T9SearchTest {
    private fun score(label: String, query: String, t9: Boolean = true) =
        SearchHelper.scorePreNormalized(SearchHelper.normalizeString(label), SearchHelper.normalizeString(query), 100, label, t9)

    @Test
    fun encodesKeypadGroups() {
        assertEquals("22233344455566677778889999", SearchHelper.toT9("ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
        assertEquals("1234", SearchHelper.toT9("1234"))
    }

    @Test
    fun digitsMatchLabelPrefix() {
        assertTrue(score("Etar", "3827") > 0)
        assertTrue(score("Clock", "25625") > 0)
        assertEquals(0, score("Etar", "3828"))
    }

    @Test
    fun digitsMatchWordStartButNotMidWord() {
        assertTrue(score("Material Files", "34537") > 0) // "Files"
        assertTrue(score("WhatsApp", "277") > 0) // camel-case word "App"
        assertEquals(0, score("Calendar", "3") + score("Calendar", "36")) // "e","en" mid-word
    }

    @Test
    fun prefixRanksAboveWordStart() {
        assertTrue(score("Files", "345") > score("Material Files", "345"))
    }

    @Test
    fun digitsMatchLiterallyWithoutKeypad() {
        assertEquals(0, score("Etar", "3827", t9 = false))
        assertEquals(100, score("2048", "2048", t9 = false))
    }

    @Test
    fun lettersStillUseLiteralSearch() {
        assertEquals(100, score("Etar", "et"))
    }
}
