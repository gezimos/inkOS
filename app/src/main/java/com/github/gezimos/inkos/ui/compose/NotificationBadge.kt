package com.github.gezimos.inkos.ui.compose

import android.app.Notification
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.services.NotificationManager

private const val SIGNAL_PACKAGE = "org.thoughtcrime.securesms"
private const val FALLBACK_MESSAGE = "Notification received"

data class NotificationBadgeConfig(
    val showBadge: Boolean,
    val allCapsApps: Boolean,
    val smallCapsApps: Boolean,
    val showMediaIndicator: Boolean,
    val showMediaName: Boolean,
    val showNotificationText: Boolean,
    val showNotificationSenderName: Boolean,
    val showNotificationGroupName: Boolean,
    val showNotificationMessage: Boolean,
    val homeAppCharLimit: Int
)

data class NotificationBadgeDisplay(
    val label: String,
    val showIndicator: Boolean,
    val subtitle: String?
)

fun buildNotificationBadgeDisplay(
    label: String,
    packageName: String,
    notificationInfo: NotificationManager.NotificationInfo?,
    mediaInfo: AudioWidgetHelper.MediaPlayerInfo?,
    config: NotificationBadgeConfig
): NotificationBadgeDisplay {
    val sanitizedLabel = sanitize(label)
    val displayLabel = when {
        config.allCapsApps -> sanitizedLabel.uppercase()
        config.smallCapsApps -> sanitizedLabel.lowercase()
        else -> sanitizedLabel
    }

    if (!config.showBadge || notificationInfo == null) {
        return NotificationBadgeDisplay(
            label = displayLabel,
            showIndicator = false,
            subtitle = null
        )
    }

    val title = sanitize(notificationInfo.title).takeIf { it.isNotBlank() }
    val text = sanitize(notificationInfo.text).takeIf { it.isNotBlank() }
    val isMedia = notificationInfo.category == Notification.CATEGORY_TRANSPORT
    val isPlaying = isMedia && mediaInfo?.packageName == packageName && mediaInfo.isPlaying
    val indicator = when {
        isMedia && isPlaying && config.showMediaIndicator -> true
        !isMedia && notificationInfo.count > 0 -> true
        else -> false
    }
    val charLimit = config.homeAppCharLimit.coerceAtLeast(0)

    val subtitle = when {
        isMedia && isPlaying && config.showMediaName -> buildMediaSubtitle(title, charLimit)
        !isMedia && config.showNotificationText && (title != null || text != null) ->
            buildMessagingSubtitle(title, text, packageName, config, charLimit)
        !isMedia && config.showNotificationText && title == null && text == null ->
            buildFallbackSubtitle(displayLabel, config, charLimit)
        else -> null
    }

    return NotificationBadgeDisplay(
        label = displayLabel,
        showIndicator = indicator,
        subtitle = subtitle
    )
}

private fun buildMediaSubtitle(title: String?, charLimit: Int): String? {
    val firstPart = title
        ?.split(" - ", ":", "|")
        ?.firstOrNull()
        ?.trim()
        .orEmpty()
    if (firstPart.isBlank() || firstPart.equals("transport", ignoreCase = true)) {
        return null
    }
    return firstPart.take(charLimit).takeIf { it.isNotBlank() }
}

private fun buildMessagingSubtitle(
    title: String?,
    text: String?,
    packageName: String,
    config: NotificationBadgeConfig,
    charLimit: Int
): String? {
    val showName = config.showNotificationSenderName
    val showGroup = config.showNotificationGroupName
    val showMessage = config.showNotificationMessage

    if (!showName && !showGroup && !showMessage) return null

    var sender = ""
    var group = ""
    if (!title.isNullOrBlank()) {
        val parts = title.split(": ", limit = 2)
        if (packageName == SIGNAL_PACKAGE) {
            if (parts.size == 1) {
                sender = parts[0]
                group = ""
            } else {
                group = parts.getOrNull(0) ?: ""
                sender = parts.getOrNull(1) ?: ""
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
    if (group == sender) group = ""

    sender = sanitize(sender)
    group = sanitize(group)
    val message = if (showMessage) text?.let { sanitize(it) } else null

    val builder = StringBuilder()
    if (showName && sender.isNotBlank()) {
        builder.append(sender)
    }
    if (showGroup && group.isNotBlank()) {
        if (builder.isNotEmpty()) builder.append(": ")
        builder.append(group)
    }
    if (showMessage && !message.isNullOrBlank()) {
        if (builder.isNotEmpty()) builder.append(": ")
        builder.append(message)
    }

    if (builder.isEmpty()) return null
    return builder.toString().take(charLimit).takeIf { it.isNotBlank() }
}

private fun buildFallbackSubtitle(
    displayLabel: String,
    config: NotificationBadgeConfig,
    charLimit: Int
): String? {
    val showName = config.showNotificationSenderName
    val showMessage = config.showNotificationMessage
    if (!showName && !showMessage) return null

    val builder = StringBuilder()
    if (showName && displayLabel.isNotBlank()) {
        builder.append(displayLabel)
    }
    if (showMessage) {
        if (builder.isNotEmpty()) builder.append(": ")
        builder.append(FALLBACK_MESSAGE)
    }

    if (builder.isEmpty()) return null
    return builder.toString().take(charLimit).takeIf { it.isNotBlank() }
}

private fun sanitize(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return value
        .replace("\n", " ")
        .replace("\r", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}