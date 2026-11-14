package com.github.gezimos.inkos.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class T9SearchTest {
    private fun score(label: String, query: String) =
        SearchHelper.scorePreNormalized(SearchHelper.normalizeString(label), SearchHelper.normalizeString(query), 100, label)

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
        assertEquals(0, score("Calendar", "3") + score("Calendar", "36")) // "e","en" mid-word
    }

    @Test
    fun prefixRanksAboveWordStart() {
        assertTrue(score("Files", "345") > score("Material Files", "345"))
    }

    @Test
    fun lettersStillUseLiteralSearch() {
        assertEquals(100, score("Etar", "et"))
    }
}
