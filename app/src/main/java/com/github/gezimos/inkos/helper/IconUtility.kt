package com.github.gezimos.inkos.helper

/**
 * Utility for generating simple icon codes from app labels.
 * Icons are displayed like a periodic table (no brackets).
 * 
 * Rules:
 * - 1 word: take first 2 letters (e.g., "Calendar" -> "Ca")
 * - 2+ words: take first letter of first word + first letter of second word (e.g., "Keep Notes" -> "Kn")
 * - Conflicts are resolved by using the next available letter
 */
object IconUtility {
    /**
     * Generates icon codes for a list of app labels, resolving conflicts automatically.
     * Returns a map of label -> icon code.
     */
    fun generateIconCodes(labels: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val usedCodes = mutableSetOf<String>()
        
        for (label in labels) {
            val code = generateCodeForLabel(label, usedCodes)
            result[label] = code
            usedCodes.add(code)
        }
        
        return result
    }
    
    /**
     * Generates an icon code for a single label.
     * Returns format "Aa" (uppercase first letter, lowercase second letter) if two letters available.
     * Returns single letter (uppercase) if only one letter is available.
     * If conflicts need to be resolved, use generateIconCodes() instead.
     */
    fun generateCodeForLabel(label: String, usedCodes: Set<String> = emptySet()): String {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return ""
        
        // Find all letters in the text
        val letters = trimmed.filter { it.isLetter() }
        
        // If only one letter, return just that letter (uppercase)
        if (letters.length == 1) {
            return letters[0].uppercaseChar().toString()
        }
        
        // Find first letter (uppercase)
        val firstLetter = trimmed.firstOrNull { it.isLetter() }?.uppercaseChar() ?: 'X'
        
        // Find first letter after the first word (lowercase)
        // This handles cases like "Amazon Shopping" -> "As" and "E-Reader" -> "Er"
        val secondLetter = findSecondLetter(trimmed)?.lowercaseChar()
        
        // If no second letter found, return just the first letter
        if (secondLetter == null) {
            return firstLetter.toString()
        }
        
        val baseCode = "$firstLetter$secondLetter"
        
        // Resolve conflicts if needed
        if (usedCodes.contains(baseCode)) {
            return resolveConflict(baseCode, trimmed, usedCodes)
        }
        
        return baseCode
    }
    
    /**
     * Finds the first letter that comes after the first word.
     * Handles separators like spaces, hyphens, etc.
     * Examples:
     * - "Amazon Shopping" -> 'S' (first letter of second word)
     * - "E-Reader" -> 'R' (first letter after hyphen)
     * - "Calendar" -> 'a' (second letter of single word)
     */
    private fun findSecondLetter(text: String): Char? {
        // First, find where the first word ends
        var firstWordEnd = 0
        var foundFirstLetter = false
        
        for (i in text.indices) {
            if (text[i].isLetter()) {
                if (!foundFirstLetter) {
                    foundFirstLetter = true
                }
            } else if (foundFirstLetter) {
                // We've found the first word, now look for the next letter
                for (j in i until text.length) {
                    if (text[j].isLetter()) {
                        return text[j]
                    }
                }
                break
            }
        }
        
        // If we didn't find a second word, use the second letter of the first word
        if (foundFirstLetter) {
            for (i in 1 until text.length) {
                if (text[i].isLetter()) {
                    return text[i]
                }
            }
        }
        
        return null
    }
    
    /**
     * Resolves conflicts by trying variations while maintaining "Aa" format.
     * Tries different letter combinations from the original text.
     */
    private fun resolveConflict(baseCode: String, text: String, usedCodes: Set<String>): String {
        val firstLetter = baseCode[0].uppercaseChar()
        val letters = text.filter { it.isLetter() }
        
        if (letters.length >= 2) {
            // Try 1st + 3rd letter
            if (letters.length >= 3) {
                val candidate = "${firstLetter}${letters[2].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
            
            // Try 1st + 4th letter
            if (letters.length >= 4) {
                val candidate = "${firstLetter}${letters[3].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
            
            // Try 2nd + 3rd letter
            if (letters.length >= 3) {
                val candidate = "${letters[1].uppercaseChar()}${letters[2].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
            
            // Try 1st + 5th letter
            if (letters.length >= 5) {
                val candidate = "${firstLetter}${letters[4].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
        }
        
        // Fallback: append numbers
        var counter = 1
        while (true) {
            val candidate = "${firstLetter}${counter}"
            if (!usedCodes.contains(candidate)) {
                return candidate
            }
            counter++
            if (counter > 99) break // Safety limit
        }
        
        // Last resort
        return baseCode
    }
}
