package com.github.gezimos.inkos.services

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class NotificationManager private constructor(private val context: Context) {
    // (simplified) No per-package SMS cache; keep only active notification info map

    data class NotificationInfo(
        val count: Int,
        val title: String?,
        val text: String?,
        val category: String?,
        val timestamp: Long
    )

    data class ConversationNotification(
        val conversationId: String,
        val conversationTitle: String?,
        val sender: String?,
        val message: String?,
        val timestamp: Long,
        val category: String? = null,
        val notificationKey: String? = null
    )

    // Cache for summary detection to avoid repeated string processing
    private val summaryCache = mutableMapOf<String, Boolean>()


    private val notificationInfo = mutableMapOf<String, NotificationInfo>()
    private val _notificationInfoState = MutableStateFlow<Map<String, NotificationInfo>>(emptyMap())
    val notificationInfoState: StateFlow<Map<String, NotificationInfo>> = _notificationInfoState.asStateFlow()

    // Keep last posted snapshot to avoid spamming observers with identical maps
    private var lastPostedNotificationInfo: Map<String, NotificationInfo>? = null

    private val conversationNotifications =
        mutableMapOf<String, MutableMap<String, ConversationNotification>>()
    private val _conversationNotificationsState =
        MutableStateFlow<Map<String, List<ConversationNotification>>>(emptyMap())
    val conversationNotificationsState: StateFlow<Map<String, List<ConversationNotification>>> =
        _conversationNotificationsState.asStateFlow()


    private val NOTIF_SAVE_FILE = "inkos_notifications.json"

    companion object {
        @Volatile
        @Suppress("StaticFieldLeak") // Using applicationContext which is safe
        private var INSTANCE: NotificationManager? = null
        fun getInstance(context: Context): NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun updateBadgeNotification(packageName: String, info: NotificationInfo?) {
        if (info == null) {
            notificationInfo.remove(packageName)
        } else {
            notificationInfo[packageName] = info
        }
        // Removed debug log
        // Only filter by badge allowlist and force LiveData update
        val prefs = com.github.gezimos.inkos.data.Prefs(context)
        val allowed = prefs.allowedBadgeNotificationApps
        val filtered = if (allowed.isEmpty()) {
            // Create a completely new map to force LiveData update
            HashMap(notificationInfo)
        } else {
            // Create a new filtered map
            HashMap(notificationInfo.filter { (pkg, _) -> pkg in allowed })
        }
        // Only post when the filtered map actually changed to avoid redundant updates
        if (lastPostedNotificationInfo != filtered) {
            lastPostedNotificationInfo = HashMap(filtered)
            _notificationInfoState.value = filtered
        }
    }

    fun clearMediaNotification(packageName: String) {
        // Specifically clear media notifications - useful when media stops playing
        val currentInfo = notificationInfo[packageName]
        if (currentInfo?.category == android.app.Notification.CATEGORY_TRANSPORT) {
            updateBadgeNotification(packageName, null)
        }
    }

    /**
     * Refreshes badge notification state based on current allowlist.
     * Call this when the badge allowlist changes to immediately update the UI.
     */
    fun refreshBadgeNotificationState() {
        val prefs = com.github.gezimos.inkos.data.Prefs(context)
        val allowed = prefs.allowedBadgeNotificationApps
        val filtered = if (allowed.isEmpty()) {
            HashMap(notificationInfo)
        } else {
            HashMap(notificationInfo.filter { (pkg, _) -> pkg in allowed })
        }
        if (lastPostedNotificationInfo != filtered) {
            lastPostedNotificationInfo = HashMap(filtered)
            _notificationInfoState.value = filtered
        }
    }

    /**
     * Refreshes conversation notification state based on current allowlist.
     * Call this when the conversation allowlist changes to immediately update the UI.
     */
    fun refreshConversationNotificationState() {
        _conversationNotificationsState.value = getConversationNotifications()
    }

    fun getConversationNotifications(): Map<String, List<ConversationNotification>> {
        // Only filter by allowlist
        val prefs = com.github.gezimos.inkos.data.Prefs(context)
        val allowed = prefs.allowedNotificationApps
        return conversationNotifications
            .filter { (pkg, _) -> allowed.isEmpty() || pkg in allowed }
            .mapValues { entry ->
                entry.value.values.sortedByDescending { n -> n.timestamp }
            }
    }

    fun updateConversationNotification(
        packageName: String,
        conversation: ConversationNotification
    ) {
        val appMap = conversationNotifications.getOrPut(packageName) { mutableMapOf() }
        appMap[conversation.conversationId] = conversation
        _conversationNotificationsState.value = getConversationNotifications()
        saveConversationNotifications()
    }

    fun removeConversationNotification(packageName: String, conversationId: String) {
        val appMap = conversationNotifications[packageName]
        if (appMap != null) {
            appMap.remove(conversationId)
            if (appMap.isEmpty()) {
                conversationNotifications.remove(packageName)
            }
            _conversationNotificationsState.value = getConversationNotifications()
            saveConversationNotifications()
        }
    }

    /**
     * Opens a notification conversation and optionally removes it from the local list.
     * This consolidates the common pattern of opening a notification.
     *
     * @param packageName The package name of the app that owns the notification
     * @param notificationKey The notification key to open
     * @param conversationId The conversation ID to remove from local list (if removeAfterOpen is true)
     * @param removeAfterOpen Whether to remove the notification from local list after opening
     * @return true if the notification was opened successfully, false otherwise
     */
    fun openNotification(
        packageName: String,
        notificationKey: String?,
        conversationId: String? = null,
        removeAfterOpen: Boolean = true
    ): Boolean {
        return try {
            // Try to open the specific conversation using deep link
            val opened = NotificationService.sendConversationIntent(
                context, packageName, notificationKey
            )
            // Fall back to app launch if deep link failed
            if (!opened) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
            // Remove notification from local list after opening if requested
            if (removeAfterOpen && conversationId != null) {
                removeConversationNotification(packageName, conversationId)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun saveConversationNotifications() {
        try {
            val file = File(context.filesDir, NOTIF_SAVE_FILE)
            val mapToSave = conversationNotifications.mapValues { it.value.values.toList() }
            val json = Gson().toJson(mapToSave)
            file.writeText(json)
        } catch (_: Exception) {
        }
    }

    fun restoreConversationNotifications() {
        try {
            val file = File(context.filesDir, NOTIF_SAVE_FILE)
            if (!file.exists()) return
            val json = file.readText()
            val type = object : TypeToken<Map<String, List<ConversationNotification>>>() {}.type
            val restored: Map<String, List<ConversationNotification>> = Gson().fromJson(json, type)
            conversationNotifications.clear()
            restored.forEach { (pkg, list) ->
                conversationNotifications[pkg] =
                    list.associateBy { it.conversationId }.toMutableMap()
            }
            _conversationNotificationsState.value = getConversationNotifications()
        } catch (_: Exception) {
        }
    }

    /**
     * Extracts notification content (sender, group, text, title) from a StatusBarNotification.
     * Tries multiple Android notification fields to extract meaningful content.
     * This is shared logic used by both buildNotificationInfo() and buildNotificationInfoForRemaining().
     */
    private fun extractNotificationContent(
        sbn: android.service.notification.StatusBarNotification,
        prefs: com.github.gezimos.inkos.data.Prefs
    ): Triple<String?, String?, String?> {
        val extras = sbn.notification.extras
        val showSender = prefs.showNotificationSenderName
        val showGroup = prefs.showNotificationGroupName
        val showMessage = prefs.showNotificationMessage

        // Helper to normalize text
        fun normalizeText(text: String?): String? {
            return text?.trim()?.replace("\n", " ")?.replace("\r", " ")?.replace(Regex("\\s+"), " ")
        }

        // Extract group/conversation title first (needed to filter it out from other fields)
        val groupRaw = extras?.getCharSequence("android.conversationTitle")?.toString()
            ?: extras?.getString("android.conversationTitle")
        val group = if (showGroup) {
            normalizeText(groupRaw)
        } else null
        
        // Extract sender/title - try multiple fields
        // For WhatsApp, android.title might be "Sender: GroupName" or just "GroupName"
        val titleRaw = extras?.getCharSequence("android.title")?.toString()
            ?: extras?.getString("android.title")
        
        val sender: String? = if (showSender) {
            val senderText = if (!titleRaw.isNullOrBlank() && titleRaw.contains(": ")) {
                // If title contains ": ", split it - first part is sender, second part might be group
                titleRaw.split(": ", limit = 2).firstOrNull()?.trim()
            } else {
                titleRaw
            } ?: extras?.getCharSequence("android.subText")?.toString()
                ?: extras?.getString("android.subText")
            
            val normalizedSender = normalizeText(senderText)
            // CRITICAL: If sender is the same as group name, it's likely wrong - use null instead
            if (!normalizedSender.isNullOrBlank() && 
                !groupRaw.isNullOrBlank() && 
                normalizedSender.trim().equals(groupRaw.trim(), ignoreCase = true)) {
                null // Sender is actually the group name, which is wrong
            } else {
                normalizedSender
            }
        } else null
        
        // Extract message/text - try multiple fields in priority order
        // CRITICAL: Make sure we get the actual message text, NEVER use group name as message
        val text = if (showMessage) {
            val rawText = when {
                extras?.getCharSequence("android.bigText") != null -> 
                    extras.getCharSequence("android.bigText")?.toString()
                
                extras?.getCharSequence("android.text") != null -> 
                    extras.getCharSequence("android.text")?.toString()
                
                extras?.getCharSequenceArray("android.textLines") != null -> {
                    val lines = extras.getCharSequenceArray("android.textLines")
                    lines?.lastOrNull()?.toString()
                }
                
                // Try additional fields that some apps use
                extras?.getCharSequence("android.summaryText") != null -> 
                    extras.getCharSequence("android.summaryText")?.toString()
                
                extras?.getCharSequence("android.infoText") != null -> 
                    extras.getCharSequence("android.infoText")?.toString()
                
                // Ticker text (deprecated but still used by some apps)
                sbn.notification.tickerText != null -> 
                    sbn.notification.tickerText?.toString()
                
                else -> null
            }
            val normalizedText = normalizeText(rawText)?.take(30)
            // CRITICAL: If text is the same as group name, it's wrong - return null
            if (!normalizedText.isNullOrBlank() && 
                !groupRaw.isNullOrBlank() && 
                normalizedText.trim().equals(groupRaw.trim(), ignoreCase = true)) {
                null // Text is actually the group name, which is wrong
            } else {
                normalizedText
            }
        } else null

        return Triple(sender, group, text)
    }

    /**
     * Builds notification title and text from extracted content, with intelligent fallback.
     * Provides category-specific fallbacks instead of generic "Notification received".
     */
    private fun buildNotificationTitleAndText(
        sender: String?,
        group: String?,
        text: String?,
        packageName: String,
        category: String? = null
    ): Pair<String?, String?> {
        // Only include group in title if it's not null (preference is already checked in extractNotificationContent)
        var notifTitle = buildString {
            if (!sender.isNullOrBlank()) append(sender)
            if (!group.isNullOrBlank()) {
                if (isNotEmpty()) append(": ")
                append(group)
            }
        }.ifBlank { null }
        
        // CRITICAL: notifText should ALWAYS be the actual message text, never the group name
        var notifText = text
        
        // Only use fallback if both title and text are completely missing
        if ((notifTitle == null || notifTitle.isBlank()) && (notifText == null || notifText.isBlank())) {
            val pm = context.packageManager
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }
            notifTitle = appLabel
            
            // Provide category-specific fallback text instead of generic "Notification received"
            notifText = when (category) {
                android.app.Notification.CATEGORY_MESSAGE -> "New message"
                android.app.Notification.CATEGORY_EMAIL -> "New email"
                android.app.Notification.CATEGORY_CALL -> "Missed call"
                android.app.Notification.CATEGORY_ALARM -> "Alarm"
                android.app.Notification.CATEGORY_REMINDER -> "Reminder"
                android.app.Notification.CATEGORY_EVENT -> "Event"
                android.app.Notification.CATEGORY_PROMO -> "Promotion"
                android.app.Notification.CATEGORY_TRANSPORT -> "Media playing"
                android.app.Notification.CATEGORY_SYSTEM -> "System notification"
                android.app.Notification.CATEGORY_SERVICE -> "Service notification"
                android.app.Notification.CATEGORY_ERROR -> "Error"
                android.app.Notification.CATEGORY_PROGRESS -> "Progress update"
                android.app.Notification.CATEGORY_SOCIAL -> "Social update"
                android.app.Notification.CATEGORY_STATUS -> "Status update"
                android.app.Notification.CATEGORY_RECOMMENDATION -> "Recommendation"
                else -> null // Don't show generic fallback - just show app name
            }
        }
        
        return Pair(notifTitle, notifText)
    }

    fun buildNotificationInfo(
        sbn: android.service.notification.StatusBarNotification,
        prefs: com.github.gezimos.inkos.data.Prefs,
        activeNotifications: Array<android.service.notification.StatusBarNotification>
    ): NotificationInfo {
        // Get all notifications for this package
        val samePackage = activeNotifications.filter { it.packageName == sbn.packageName }
        
        // Filter out summary notifications to avoid showing "X messages from Y contacts"
        val nonSummaryNotifications = samePackage.filter { !isNotificationSummary(it) }
        
        // Choose the most recent non-summary notification, fallback to any notification if needed
        val notificationToShow = when {
            nonSummaryNotifications.isNotEmpty() -> nonSummaryNotifications.maxByOrNull { it.postTime }
            samePackage.isNotEmpty() -> samePackage.maxByOrNull { it.postTime }
            else -> null
        } ?: sbn

        val (sender, group, text) = extractNotificationContent(notificationToShow, prefs)
        val (notifTitle, notifText) = buildNotificationTitleAndText(
            sender, group, text, sbn.packageName, notificationToShow.notification.category
        )

        return NotificationInfo(
            count = samePackage.size.coerceAtLeast(1),
            title = notifTitle,
            text = notifText,
            category = notificationToShow.notification.category,
            timestamp = notificationToShow.postTime
        )
    }

    /**
     * Fixed: Builds notification info only from remaining active notifications for a package.
     * Used when a notification is removed to check if there are other notifications to display.
     * Returns null if no active notifications exist, ensuring badges are properly cleared.
     */
    fun buildNotificationInfoForRemaining(
        packageName: String,
        prefs: com.github.gezimos.inkos.data.Prefs,
        activeNotifications: Array<android.service.notification.StatusBarNotification>
    ): NotificationInfo? {
        // Find active notifications for this package only
        val samePackage = activeNotifications.filter { it.packageName == packageName }

        // If no active notifications remain for this package, return null
        if (samePackage.isEmpty()) {
            return null
        }

        // Filter out summary notifications to avoid showing "X messages from Y contacts"
        val nonSummaryNotifications = samePackage.filter { !isNotificationSummary(it) }
        
        // Choose the most recent non-summary notification, fallback to any notification if needed
        val notificationToShow = when {
            nonSummaryNotifications.isNotEmpty() -> nonSummaryNotifications.maxByOrNull { it.postTime }
            samePackage.isNotEmpty() -> samePackage.maxByOrNull { it.postTime }
            else -> null
        } ?: return null
        
        val (sender, group, text) = extractNotificationContent(notificationToShow, prefs)
        val (notifTitle, notifText) = buildNotificationTitleAndText(
            sender, group, text, packageName, notificationToShow.notification.category
        )

        return NotificationInfo(
            count = samePackage.size.coerceAtLeast(1),
            title = notifTitle,
            text = notifText,
            category = notificationToShow.notification.category,
            timestamp = notificationToShow.postTime
        )
    }

    /**
     * Detects if a notification is a summary notification that should be filtered out.
     * Summary notifications show generic text like "X messages from Y contacts" instead of actual content.
     * Uses caching to improve performance.
     */
    internal fun isNotificationSummary(sbn: android.service.notification.StatusBarNotification): Boolean {
        // Create cache key from notification key and post time to handle updates
        val cacheKey = "${sbn.key}_${sbn.postTime}"
        
        // Check cache first for performance
        summaryCache[cacheKey]?.let { return it }
        
        // Check if notification is marked as group summary (Android standard way)
        // Modern apps should always set this flag for summary notifications
        if (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) {
            summaryCache[cacheKey] = true
            return true
        }
        
        // Clean up cache periodically to prevent memory leaks (keep last 100 entries)
        if (summaryCache.size > 100) {
            val keysToRemove = summaryCache.keys.take(summaryCache.size - 50)
            keysToRemove.forEach { summaryCache.remove(it) }
        }
        
        // Not a summary (no flag set)
        summaryCache[cacheKey] = false
        return false
    }


}
