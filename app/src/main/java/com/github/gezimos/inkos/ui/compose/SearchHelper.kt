package com.github.gezimos.inkos.ui.compose

import android.view.KeyEvent
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.gezimos.inkos.data.AppListItem
import java.text.Normalizer
import java.util.Locale

/**
 * Utilities to convert Compose `Key` keys to characters, update search TextFieldValue,
 * and perform fuzzy scoring/matching for search.
 */
object SearchHelper {
    /**
     * Convert a Compose `Key` to a printable Char where applicable, otherwise null.
     */
    fun keyToChar(key: Key): Char? = when (key) {
        Key.A -> 'a'
        Key.B -> 'b'
        Key.C -> 'c'
        Key.D -> 'd'
        Key.E -> 'e'
        Key.F -> 'f'
        Key.G -> 'g'
        Key.H -> 'h'
        Key.I -> 'i'
        Key.J -> 'j'
        Key.K -> 'k'
        Key.L -> 'l'
        Key.M -> 'm'
        Key.N -> 'n'
        Key.O -> 'o'
        Key.P -> 'p'
        Key.Q -> 'q'
        Key.R -> 'r'
        Key.S -> 's'
        Key.T -> 't'
        Key.U -> 'u'
        Key.V -> 'v'
        Key.W -> 'w'
        Key.X -> 'x'
        Key.Y -> 'y'
        Key.Z -> 'z'
        else -> null
    }

    /**
     * Append a printable char to `searchQuery`, keeping selection at end.
     */
    fun appendCharToSearch(searchQuery: TextFieldValue, ch: Char): TextFieldValue {
        val newText = (searchQuery.text + ch).lowercase()
        return TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        )
    }

    /**
     * Apply backspace/delete to the search query, returning a new TextFieldValue.
     */
    fun backspaceSearch(searchQuery: TextFieldValue): TextFieldValue {
        if (searchQuery.text.isEmpty()) return searchQuery
        val newText = searchQuery.text.dropLast(1)
        return TextFieldValue(
            text = newText,
            selection = TextRange(newText.length)
        )
    }

    // Android keyCode mapping removed — use Compose `Key` overload or Activity `event.unicodeChar`.

    /**
     * Convert an Android `KeyEvent` to a printable Char, honoring the system's keyboard layout
     * and modifiers by using `KeyEvent.unicodeChar`.
     * Returns `null` when the event does not produce a printable character.
     */
    fun keyEventToChar(event: KeyEvent): Char? {
        return try {
            val unicode = event.unicodeChar
            if (unicode == 0) return null
            val ch = unicode.toChar()
            // Only accept ASCII letters a-z; ignore space, digits, punctuation, and other scripts
            val lower = ch.lowercaseChar()
            if (lower in 'a'..'z') lower else null
        } catch (_: Exception) {
            null
        }
    }

    // --- Fuzzy search utilities (migrated from helper/FuzzyFinder.kt) ---

    /**
     * Score an AppListItem given the `searchChars` and a `topScore` multiplier.
     */
    fun scoreApp(app: AppListItem, searchChars: String, topScore: Int): Int {
        val appLabel = app.label
        val normalizedAppLabel = normalizeString(appLabel)
        val normalizedSearchChars = normalizeString(searchChars)

        val fuzzyScore = calculateFuzzyScore(normalizedAppLabel, normalizedSearchChars)
        return (fuzzyScore * topScore).toInt()
    }

    /**
     * Score an arbitrary string against the search chars.
     */
    fun scoreString(appLabel: String, searchChars: String, topScore: Int): Int {
        val normalizedAppLabel = normalizeString(appLabel)
        val normalizedSearchChars = normalizeString(searchChars)

        val fuzzyScore = calculateFuzzyScore(normalizedAppLabel, normalizedSearchChars)
        return (fuzzyScore * topScore).toInt()
    }

    /**
     * Simple match check for normalized containment.
     */
    fun isMatch(appLabel: String, searchChars: String): Boolean {
        val normalizedAppLabel = normalizeString(appLabel)
        val normalizedSearchChars = normalizeString(searchChars)

        return normalizedAppLabel.contains(normalizedSearchChars, ignoreCase = true)
    }

    /**
     * Check if the normalized app label starts with the given prefix (case-insensitive,
     * diacritics removed). Used for A→Z filtering.
     */
    fun startsWith(appLabel: String, prefix: String): Boolean {
        if (prefix.isEmpty()) return false
        val normalizedAppLabel = normalizeString(appLabel)
        val normalizedPrefix = normalizeString(prefix)
        return normalizedAppLabel.startsWith(normalizedPrefix)
    }

    // Simplified normalization for app label and search string
    @VisibleForTesting
    internal fun normalizeString(input: String): String {
        return input
            .uppercase(Locale.getDefault())
            .let { normalizeDiacritics(it) }
            .replace(Regex("[-_+,. ]"), "")
    }

    // Remove diacritics from a string
    private fun normalizeDiacritics(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    // Fuzzy matching logic
    private fun calculateFuzzyScore(s1: String, s2: String): Float {
        val m = s1.length
        val n = s2.length
        var matchCount = 0
        var s1Index = 0

        // Iterate over each character in s2 and check if it exists in s1
        for (c2 in s2) {
            var found = false

            // Start searching for c2 from the current s1Index
            for (j in s1Index until m) {
                if (s1[j] == c2) {
                    found = true
                    s1Index = j + 1  // Move to the next position in s1
                    break
                }
            }

            // If the current character in s2 is not found in s1, return a score of 0
            if (!found) return 0f

            matchCount++
        }

        // Return score based on the ratio of matched characters to the longer string length
        return matchCount.toFloat() / maxOf(m, n)
    }
}
