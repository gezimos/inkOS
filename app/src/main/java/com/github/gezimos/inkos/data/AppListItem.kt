package com.github.gezimos.inkos.data

import android.os.UserHandle
import java.text.Collator

val collator: Collator = Collator.getInstance().apply {
    strength = Collator.PRIMARY
    decomposition = Collator.CANONICAL_DECOMPOSITION
}

/**
 * We create instances in 3 different places:
 * 1. app drawer
 * 2. recent apps
 * 3. home screen (for the list and for swipes/taps)
 *
 * @property activityLabel
 * label of the activity (`LauncherActivityInfo.label`)
 *
 * @property activityPackage
 * Package name of the activity (`LauncherActivityInfo.applicationInfo.packageName`)
 *
 * @property activityClass
 * (`LauncherActivityInfo.componentName.className`)
 *
 * @property user
 * userHandle is needed to resolve and start an activity.
 * And also we mark with a special icon the apps which belong to a managed user.
 *
 * @property customLabel
 * When user renames an app, we store the rename in Preferences.
 *
 * @property label
 * Use this property to render the list item.
 * It's either the original activity label (`activityLabel`) or a user-defined label (`definedLabel`).
 */

data class AppListItem(
    val activityLabel: String,
    val activityPackage: String,
    val activityClass: String,
    val user: UserHandle,
    var customLabel: String, // TODO make immutable by refining data flow
) : Comparable<AppListItem> {
    val label get() = customLabel.ifEmpty { activityLabel }

    /** Speed up sort and search */
    private val collationKey get() = collator.getCollationKey(label)

    override fun compareTo(other: AppListItem): Int =
        collationKey.compareTo(other.collationKey)
}
