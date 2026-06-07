package com.github.gezimos.inkos.data

import android.os.UserHandle
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

private val DIACRITICS = Regex("\\p{InCombiningDiacriticalMarks}+")
private val PUNCTUATION = Regex("[-_+,. ]")

/** Lightweight normalization identical to SearchHelper.normalizeString — lives here to avoid
 *  a circular dependency and to keep AppListItem self-contained. */
internal fun normalizeLabel(input: String): String {
    return Normalizer.normalize(input.uppercase(Locale.getDefault()), Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .replace(PUNCTUATION, "")
}

val collator: Collator = Collator.getInstance().apply {
    strength = Collator.PRIMARY
    decomposition = Collator.CANONICAL_DECOMPOSITION
}

data class AppListItem(
    val activityLabel: String,
    val activityPackage: String,
    val activityClass: String,
    val user: UserHandle,
    var customLabel: String, // TODO make immutable by refining data flow
    val shortcutId: String? = null, // Non-null for app shortcuts, null for regular apps
) : Comparable<AppListItem> {
    val label get() = customLabel.ifEmpty { activityLabel }

    /** Returns true if this item represents an app shortcut */
    val isShortcut: Boolean get() = shortcutId != null

    /** Pre-computed normalized label for search/filter — avoids repeated Unicode normalization. */
    val normalizedLabel: String by lazy { normalizeLabel(label) }

    /** Normalized original app name (only differs from normalizedLabel when an alias is set). */
    val normalizedOriginalLabel: String? by lazy {
        if (customLabel.isNotEmpty() && customLabel != activityLabel) normalizeLabel(activityLabel) else null
    }

    /** Pre-computed first character of normalized label for instant A-Z filtering. */
    val normalizedFirstChar: Char? by lazy {
        normalizedLabel.trimStart().firstOrNull()?.takeIf { it in 'A'..'Z' }
    }

    /** Speed up sort and search — lazy so getCollationKey() runs once, not per compareTo() call. */
    private val collationKey by lazy { collator.getCollationKey(label) }

    override fun compareTo(other: AppListItem): Int =
        collationKey.compareTo(other.collationKey)
}
