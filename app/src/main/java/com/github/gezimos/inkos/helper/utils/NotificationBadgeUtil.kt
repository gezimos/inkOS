package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.widget.TextView
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.services.NotificationManager

object NotificationBadgeUtil {
    
    fun clearSuppression(packageName: String) {
        // Placeholder function for compatibility - suppression logic removed
        // This is called when new notifications arrive but no longer needed
        // since we handle badge clearing directly through proper notification state management
    }

    fun updateNotificationForView(
        context: Context,
        prefs: Prefs,
        textView: TextView,
        notifications: Map<String, NotificationManager.NotificationInfo>
    ) {
        val appModel = prefs.getHomeAppModel(textView.id)
        val packageName = appModel.activityPackage
    val notificationInfo = notifications[packageName]
    // Removed verbose log: updateNotificationForView
        // Filtering is now handled in NotificationManager, so no need to filter here
        val customLabel = prefs.getAppAlias("app_alias_$packageName")
        val rawDisplayName = if (customLabel.isNotEmpty()) customLabel else appModel.activityLabel
        val displayName =
            rawDisplayName.replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ").trim()

    // Notification filtering (allowlist) is handled by NotificationManager
    // Check if we have a valid notification to display
    if (notificationInfo != null && prefs.showNotificationBadge) {
            // Removed verbose log: Rendering badge for $packageName
            val spanBuilder = SpannableStringBuilder()

            // Apply small caps transformation if enabled
            val finalDisplayName = when {
                prefs.allCapsApps -> displayName.uppercase()
                prefs.smallCapsApps -> displayName.lowercase()
                else -> displayName
            }

            // Add a space before app name to compensate for asterisk alignment
            val appFont = prefs.getFontForContext("apps")
                .getFont(context, prefs.getCustomFontPathForContext("apps"))
            val appNameSpan = SpannableString(" " + finalDisplayName)
            if (appFont != null) {
                appNameSpan.setSpan(
                    CustomTypefaceSpan(appFont),
                    0,
                    appNameSpan.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            spanBuilder.append(appNameSpan)

            val title = notificationInfo.title?.replace("\n", " ")?.replace("\r", " ")
                ?.replace(Regex("\\s+"), " ")?.trim()
            val text = notificationInfo.text?.replace("\n", " ")?.replace("\r", " ")
                ?.replace(Regex("\\s+"), " ")?.trim()
            val isMedia = notificationInfo.category == android.app.Notification.CATEGORY_TRANSPORT
            val audioWidgetHelper =
                AudioWidgetHelper.getInstance(context)
            val mediaPlayerInfo = audioWidgetHelper.getCurrentMediaPlayer()
            val isPlaying =
                mediaPlayerInfo?.isPlaying == true && mediaPlayerInfo.packageName == packageName

            // Only show asterisk or music note if not media, or if media is actually playing
            if (isMedia && isPlaying && prefs.showMediaIndicator) {
                // Use asterisk for media indicator, matching notification badge style
                spanBuilder.append("*")
            } else if (!isMedia && notificationInfo.count > 0) {
                // Asterisk as superscript (already styled)
                spanBuilder.append("*")
            }

            // Notification text logic
            if (isMedia && isPlaying && prefs.showMediaName) {
                // For media, show only the first part (title or artist), not the full name
                spanBuilder.append("\n")
                val charLimit = prefs.homeAppCharLimit
                // Split by common separators and take the first part
                val firstPart = title?.split(" - ", ":", "|")?.firstOrNull()?.trim() ?: ""
                val notifText =
                    if (!firstPart.isNullOrBlank() && firstPart.lowercase() != "transport") firstPart.take(
                        charLimit
                    ) else ""
                val notifSpan = SpannableString(notifText)
                notifSpan.setSpan(
                    AbsoluteSizeSpan(prefs.labelnotificationsTextSize, true),
                    0,
                    notifText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val notificationFont = prefs.getFontForContext("notification")
                    .getFont(context, prefs.getCustomFontPathForContext("notification"))
                if (notificationFont != null) {
                    notifSpan.setSpan(
                        CustomTypefaceSpan(notificationFont),
                        0,
                        notifText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                spanBuilder.append(notifSpan)
            } else if (!isMedia && prefs.showNotificationText && (!title.isNullOrBlank() || !text.isNullOrBlank())) {
                // For messaging/other notifications, apply toggles
                spanBuilder.append("\n")
                val charLimit = prefs.homeAppCharLimit
                val showName = prefs.showNotificationSenderName
                val showGroup = prefs.showNotificationGroupName
                val showMessage = prefs.showNotificationMessage

                // Parse sender and group from title, avoid duplication
                var sender = ""
                var group = ""
                if (!title.isNullOrBlank()) {
                    val parts = title.split(": ", limit = 2)
                    if (packageName == "org.thoughtcrime.securesms") { // Signal
                        if (parts.size == 1) {
                            // Single-person conversation: treat as sender
                            sender = parts[0]
                            group = ""
                        } else {
                            group = parts.getOrNull(0) ?: ""
                            sender = parts.getOrNull(1) ?: ""
                            // If group is empty or same as sender, treat as single-person
                            if (group.isBlank() || group == sender) {
                                sender = group
                                group = ""
                            }
                        }
                    } else {
                        sender = parts.getOrNull(0) ?: ""
                        group = parts.getOrNull(1) ?: ""
                    }
                }
                // If group is same as sender, don't show group
                if (group == sender) group = ""

                val message = if (showMessage) text?.replace("\n", " ")?.replace("\r", " ")
                    ?.replace(Regex("\\s+"), " ")?.trim() ?: "" else ""
                val notifText = buildString {
                    if (showName && sender.isNotBlank()) append(
                        sender.replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ")
                            .trim()
                    )
                    if (showGroup && group.isNotBlank()) {
                        if (isNotEmpty()) append(": ")
                        append(
                            group.replace("\n", " ").replace("\r", " ").replace(Regex("\\s+"), " ")
                                .trim()
                        )
                    }
                    if (showMessage && message.isNotBlank()) {
                        if (isNotEmpty()) append(": ")
                        append(message)
                    }
                }.take(charLimit)
                val notifSpan = SpannableString(notifText)
                notifSpan.setSpan(
                    AbsoluteSizeSpan(prefs.labelnotificationsTextSize, true),
                    0,
                    notifText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val notificationFont = prefs.getFontForContext("notification")
                    .getFont(context, prefs.getCustomFontPathForContext("notification"))
                if (notificationFont != null) {
                    notifSpan.setSpan(
                        CustomTypefaceSpan(notificationFont),
                        0,
                        notifText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                spanBuilder.append(notifSpan)
            } else if (!isMedia && prefs.showNotificationText && title.isNullOrBlank() && text.isNullOrBlank()) {
                // Fallback: no title or message, show app label as title and 'Notification received' as message
                spanBuilder.append("\n")
                val charLimit = prefs.homeAppCharLimit
                val fallbackTitle = displayName
                val fallbackMessage = "Notification received"
                val notifText = buildString {
                    if (prefs.showNotificationSenderName && fallbackTitle.isNotBlank()) append(
                        fallbackTitle
                    )
                    if (prefs.showNotificationMessage) {
                        if (isNotEmpty()) append(": ")
                        append(fallbackMessage)
                    }
                }.take(charLimit)
                val notifSpan = SpannableString(notifText)
                notifSpan.setSpan(
                    AbsoluteSizeSpan(prefs.labelnotificationsTextSize, true),
                    0,
                    notifText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val notificationFont = prefs.getFontForContext("notification")
                    .getFont(context, prefs.getCustomFontPathForContext("notification"))
                if (notificationFont != null) {
                    notifSpan.setSpan(
                        CustomTypefaceSpan(notificationFont),
                        0,
                        notifText.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                spanBuilder.append(notifSpan)
            }

            textView.text = spanBuilder
        } else {
            // Removed verbose log: No badge rendered for $packageName
            // Apply small caps transformation if enabled
            val finalDisplayName = when {
                prefs.allCapsApps -> displayName.uppercase()
                prefs.smallCapsApps -> displayName.lowercase()
                else -> displayName
            }
            textView.text = finalDisplayName
            textView.typeface = prefs.getFontForContext("apps")
                .getFont(context, prefs.getCustomFontPathForContext("apps"))
        }
    }

    // CustomTypefaceSpan copied from HomeFragment for reuse
    private class CustomTypefaceSpan(private val typeface: android.graphics.Typeface) :
        android.text.style.TypefaceSpan("") {
        override fun updateDrawState(ds: android.text.TextPaint) {
            ds.typeface = typeface
        }

        override fun updateMeasureState(paint: android.text.TextPaint) {
            paint.typeface = typeface
        }
    }
}

