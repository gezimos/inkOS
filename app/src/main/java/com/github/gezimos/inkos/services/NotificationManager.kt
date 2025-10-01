package com.github.gezimos.inkos.services

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
        val category: String? = null
    )

    // Cache for summary detection to avoid repeated string processing
    private val summaryCache = mutableMapOf<String, Boolean>()


    private val notificationInfo = mutableMapOf<String, NotificationInfo>()
    private val _notificationInfoLiveData = MutableLiveData<Map<String, NotificationInfo>>()
    val notificationInfoLiveData: LiveData<Map<String, NotificationInfo>> =
        _notificationInfoLiveData

    // Keep last posted snapshot to avoid spamming observers with identical maps
    private var lastPostedNotificationInfo: Map<String, NotificationInfo>? = null

    private val conversationNotifications =
        mutableMapOf<String, MutableMap<String, ConversationNotification>>()
    private val _conversationNotificationsLiveData =
        MutableLiveData<Map<String, List<ConversationNotification>>>()
    val conversationNotificationsLiveData: LiveData<Map<String, List<ConversationNotification>>> =
        _conversationNotificationsLiveData


    private val NOTIF_SAVE_FILE = "mlauncher_notifications.json"

    companion object {
        @Volatile
        private var INSTANCE: NotificationManager? = null
        fun getInstance(context: Context): NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getBadgeNotifications(): Map<String, NotificationInfo> {
        // Only filter by badge allowlist
        val prefs = com.github.gezimos.inkos.data.Prefs(context)
        val allowed = prefs.allowedBadgeNotificationApps
        return if (allowed.isEmpty()) {
            notificationInfo.toMap()
        } else {
            notificationInfo.filter { (pkg, _) -> pkg in allowed }
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
            _notificationInfoLiveData.postValue(filtered)
        }
    }

    fun clearMediaNotification(packageName: String) {
        // Specifically clear media notifications - useful when media stops playing
        val currentInfo = notificationInfo[packageName]
        if (currentInfo?.category == android.app.Notification.CATEGORY_TRANSPORT) {
            updateBadgeNotification(packageName, null)
        }
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
        _conversationNotificationsLiveData.postValue(getConversationNotifications())
        saveConversationNotifications()
    }

    fun removeConversationNotification(packageName: String, conversationId: String) {
        val appMap = conversationNotifications[packageName]
        if (appMap != null) {
            appMap.remove(conversationId)
            if (appMap.isEmpty()) {
                conversationNotifications.remove(packageName)
            }
            _conversationNotificationsLiveData.postValue(getConversationNotifications())
            saveConversationNotifications()
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
            _conversationNotificationsLiveData.postValue(getConversationNotifications())
        } catch (_: Exception) {
        }
    }

    fun buildNotificationInfo(
        sbn: android.service.notification.StatusBarNotification,
        prefs: com.github.gezimos.inkos.data.Prefs,
        activeNotifications: Array<android.service.notification.StatusBarNotification>
    ): NotificationInfo? {
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

        val extras = notificationToShow.notification.extras
        val showSender = prefs.showNotificationSenderName
        val showGroup = prefs.showNotificationGroupName
        val showMessage = prefs.showNotificationMessage

        val sender: String? = if (showSender) {
            extras?.getCharSequence("android.title")?.toString()?.trim()?.replace("\n", " ")
                ?.replace("\r", " ")?.replace(Regex("\\s+"), " ")
        } else null
        val group = if (showGroup) {
            extras?.getCharSequence("android.conversationTitle")?.toString()?.trim()
                ?.replace("\n", " ")?.replace("\r", " ")?.replace(Regex("\\s+"), " ")
        } else null
        val text = if (showMessage) {
            val rawText = when {
                extras?.getCharSequence("android.bigText") != null -> extras.getCharSequence("android.bigText")
                    ?.toString()

                extras?.getCharSequence("android.text") != null -> extras.getCharSequence("android.text")
                    ?.toString()

                extras?.getCharSequenceArray("android.textLines") != null -> {
                    val lines = extras.getCharSequenceArray("android.textLines")
                    lines?.lastOrNull()?.toString()
                }

                else -> null
            }
            rawText?.replace("\n", " ")?.replace("\r", " ")?.trim()?.replace(Regex("\\s+"), " ")
                ?.take(30)
        } else null

        var category = notificationToShow.notification.category
        var notifTitle = buildString {
            if (!sender.isNullOrBlank()) append(sender)
            if (!group.isNullOrBlank()) {
                if (isNotEmpty()) append(": ")
                append(group)
            }
        }.ifBlank { null }
        var notifText = text
        if ((notifTitle == null || notifTitle.isBlank()) && (notifText == null || notifText.isBlank())) {
            val pm = context.packageManager
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
            } catch (_: Exception) {
                sbn.packageName
            }
            notifTitle = appLabel
            notifText = "Notification received"
        }

        return NotificationInfo(
            count = samePackage.size.coerceAtLeast(1),
            title = notifTitle,
            text = notifText,
            category = category,
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
        
        val extras = notificationToShow.notification.extras
        val showSender = prefs.showNotificationSenderName
        val showGroup = prefs.showNotificationGroupName
        val showMessage = prefs.showNotificationMessage

        val sender: String? = if (showSender) {
            extras?.getCharSequence("android.title")?.toString()?.trim()?.replace("\n", " ")
                ?.replace("\r", " ")?.replace(Regex("\\s+"), " ")
        } else null
        val group = if (showGroup) {
            extras?.getCharSequence("android.conversationTitle")?.toString()?.trim()
                ?.replace("\n", " ")?.replace("\r", " ")?.replace(Regex("\\s+"), " ")
        } else null
        val text = if (showMessage) {
            val rawText = when {
                extras?.getCharSequence("android.bigText") != null -> extras.getCharSequence("android.bigText")
                    ?.toString()

                extras?.getCharSequence("android.text") != null -> extras.getCharSequence("android.text")
                    ?.toString()

                extras?.getCharSequenceArray("android.textLines") != null -> {
                    val lines = extras.getCharSequenceArray("android.textLines")
                    lines?.lastOrNull()?.toString()
                }

                else -> null
            }
            rawText?.replace("\n", " ")?.replace("\r", " ")?.trim()?.replace(Regex("\\s+"), " ")
                ?.take(30)
        } else null

        var category = notificationToShow.notification.category
        var notifTitle = buildString {
            if (!sender.isNullOrBlank()) append(sender)
            if (!group.isNullOrBlank()) {
                if (isNotEmpty()) append(": ")
                append(group)
            }
        }.ifBlank { null }
        var notifText = text
        if ((notifTitle == null || notifTitle.isBlank()) && (notifText == null || notifText.isBlank())) {
            val pm = context.packageManager
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }
            notifTitle = appLabel
            notifText = "Notification received"
        }

        return NotificationInfo(
            count = samePackage.size.coerceAtLeast(1),
            title = notifTitle,
            text = notifText,
            category = category,
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
        
        // Check if notification is marked as group summary (fast check first)
        if (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) {
            summaryCache[cacheKey] = true
            return true
        }
        
        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString() ?: ""
        val text = extras?.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras?.getCharSequence("android.bigText")?.toString() ?: ""
        
        // Fast check: if all content is empty, it's likely not a summary
        if (title.isBlank() && text.isBlank() && bigText.isBlank()) {
            summaryCache[cacheKey] = false
            return false
        }
        
        // Only convert to lowercase once and combine
        val allText = "$title $text $bigText".lowercase()
        
        // Comprehensive pattern checking for messaging app summaries
        val isSummaryPattern = when {
            // WhatsApp patterns
            allText.contains("messages from") -> true
            allText.contains("new messages") -> true
            allText.contains("messages in") -> true
            allText.contains("unread messages") -> true
            
            // Signal patterns
            allText.contains("most recent from") -> true
            allText.contains("most recent:") -> true
            
            // Telegram patterns
            allText.contains("messages") && allText.contains("chats") -> true
            allText.contains("unread") && allText.contains("chats") -> true
            
            // Viber patterns
            allText.contains("missed messages") -> true
            allText.contains("new messages from") -> true
            
            // Generic messaging patterns
            allText.contains("message from") && allText.contains("others") -> true
            allText.contains("and") && allText.contains("others") -> true
            allText.contains("+ more") -> true
            allText.contains("other messages") -> true
            
            else -> {
                // Check additional patterns that might indicate summaries
                val additionalPatterns = listOf(
                    "new message", "message from", "messages received", "conversation",
                    "group chat", "messages waiting", "pending messages",
                    "recent message", "latest message", "missed call", "missed calls"
                )
                additionalPatterns.any { pattern -> 
                    allText.contains(pattern) && (
                        allText.contains("from") || 
                        allText.contains("in") || 
                        allText.contains("received") ||
                        allText.contains("and") ||
                        allText.contains("others") ||
                        allText.matches(Regex(".*\\d+.*")) // Contains numbers (like "3 messages")
                    )
                }
            }
        }
        
        // Cache the result and return
        summaryCache[cacheKey] = isSummaryPattern
        
        // Debug logging to help identify new summary patterns (commented out for performance)
        // if (isSummaryPattern) {
        //     android.util.Log.d("NotificationManager", "Filtered summary from ${sbn.packageName}: '$allText'")
        // }
        
        // Clean up cache periodically to prevent memory leaks (keep last 100 entries)
        if (summaryCache.size > 100) {
            val keysToRemove = summaryCache.keys.take(summaryCache.size - 50)
            keysToRemove.forEach { summaryCache.remove(it) }
        }
        
        return isSummaryPattern
    }


}
