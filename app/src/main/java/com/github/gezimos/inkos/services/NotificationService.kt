package com.github.gezimos.inkos.services

import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.github.gezimos.inkos.helper.AudioWidgetHelper

class NotificationService : NotificationListenerService() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioWidgetHelper: AudioWidgetHelper

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationManager = NotificationManager.getInstance(applicationContext)
        audioWidgetHelper = AudioWidgetHelper.getInstance(applicationContext)
        notificationManager.restoreConversationNotifications()

        _sbnState.value = activeNotifications?.toList() ?: emptyList()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        val componentName = ComponentName(this, NotificationService::class.java)
        audioWidgetHelper.initialize(componentName)

        try {
            activeNotifications?.forEach { sbn ->
                updateBadgeNotification(sbn)
                if (shouldShowNotification(sbn.packageName)) {
                    updateConversationNotifications(sbn)
                }
            }

            _sbnState.value = activeNotifications?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationService", "Listener not yet authorized", e)
        }

        com.github.gezimos.inkos.widget.NotificationWidget.requestUpdate(applicationContext)
    }

    override fun onDestroy() {
        audioWidgetHelper.cleanup()
        instance = null
        super.onDestroy()
    }

    private fun shouldShowNotification(packageName: String): Boolean {
        val prefs = com.github.gezimos.inkos.data.Prefs(this)
        val allowed = prefs.allowedNotificationApps
        return allowed.isEmpty() || allowed.contains(packageName)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updateBadgeNotification(sbn)

        if (shouldShowNotification(sbn.packageName)) {
            updateConversationNotifications(sbn)
        }

        try {
            _sbnState.value = activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) { android.util.Log.w("NotificationService", "sbnState update failed", e) }

        // Update notification widget synchronously
        com.github.gezimos.inkos.widget.NotificationWidget.requestUpdate(applicationContext)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val activeNotifications = getActiveNotifications()
        val remainingForPackage = activeNotifications.filter { 
            it.packageName == sbn.packageName && it.key != sbn.key 
        }
        
        if (remainingForPackage.isEmpty()) {
            notificationManager.updateBadgeNotification(sbn.packageName, null)
        } else {
            val prefs = com.github.gezimos.inkos.data.Prefs(applicationContext)
            val notificationInfo = notificationManager.buildNotificationInfoForRemaining(
                sbn.packageName, prefs, activeNotifications
            )
            notificationManager.updateBadgeNotification(sbn.packageName, notificationInfo)
        }

        try {
            _sbnState.value = getActiveNotifications()?.toList() ?: emptyList()
        } catch (e: Exception) { android.util.Log.w("NotificationService", "sbnState update failed", e) }

        // Update notification widget synchronously
        com.github.gezimos.inkos.widget.NotificationWidget.requestUpdate(applicationContext)
    }

    companion object {
        private val _sbnState = MutableStateFlow<List<StatusBarNotification>>(emptyList())
        val sbnState: StateFlow<List<StatusBarNotification>> get() = _sbnState
        private val mainScope = MainScope()
        fun offerActiveNotifications(list: List<StatusBarNotification>?) {
            mainScope.launch {
                _sbnState.value = list ?: emptyList()
            }
        }
        
        // Instance reference for accessing service methods
        @Volatile
        private var instance: NotificationService? = null
        
        fun getInstance(): NotificationService? = instance
        
        /**
         * Send the conversation intent directly using the notification's PendingIntent.
         * This is the most reliable way to open the specific conversation.
         */
        fun sendConversationIntent(context: Context, packageName: String, notificationKey: String?): Boolean {
            if (notificationKey == null) return false
            
            val service = instance
            if (service != null) {
                return service.sendConversationIntent(packageName, notificationKey)
            }
            
            return false
        }
        
        /**
         * Execute a notification action by index. Returns false if the action requires
         * RemoteInput (reply) or if it fails, so the caller can fall back to opening the app.
         */
        fun executeNotificationAction(notificationKey: String, actionIndex: Int): Boolean {
            val service = instance ?: return false
            return try {
                val sbn = service.activeNotifications?.find { it.key == notificationKey } ?: return false
                val action = sbn.notification.actions?.getOrNull(actionIndex) ?: return false
                if (action.remoteInputs?.isNotEmpty() == true) return false
                action.actionIntent.send()
                true
            } catch (e: Exception) {
                android.util.Log.e("NotificationService", "Failed to execute action", e)
                false
            }
        }

        /**
         * Dismiss a notification from the system notification tray by its key.
         * This acts like "mark as read" or "snooze" - it removes the notification from the system tray.
         * For grouped notifications, dismisses all notifications in the same group.
         */
        fun dismissNotification(notificationKey: String?): Boolean {
            if (notificationKey == null) return false
            
            val service = instance
            if (service != null) {
                return try {
                    val activeNotifications = service.activeNotifications
                    val notification = activeNotifications?.find { it.key == notificationKey }
                    
                    if (notification != null) {
                        val notificationGroup = notification.notification.group
                        
                        if (!notificationGroup.isNullOrBlank()) {
                            val notificationsInGroup = activeNotifications?.filter { 
                                it.notification.group == notificationGroup && 
                                it.packageName == notification.packageName 
                            } ?: emptyList()
                            
                            // Dismiss all notifications in the group
                            notificationsInGroup.forEach { sbn ->
                                try {
                                    service.cancelNotification(sbn.key)
                                } catch (e: Exception) {
                                    android.util.Log.e("NotificationService", "Failed to dismiss grouped notification: ${sbn.key}", e)
                                }
                            }
                            
                            val groupSummary = activeNotifications?.find {
                                it.packageName == notification.packageName &&
                                it.notification.group == notificationGroup &&
                                (it.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0)
                            }
                            
                            if (groupSummary != null) {
                                try {
                                    service.cancelNotification(groupSummary.key)
                                } catch (e: Exception) {
                                    android.util.Log.e("NotificationService", "Failed to dismiss group summary: ${groupSummary.key}", e)
                                }
                            }
                            
                            true
                        } else {
                            service.cancelNotification(notificationKey)
                            true
                        }
                    } else {
                        // Notification no longer exists, but that's okay
                        false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NotificationService", "Failed to dismiss notification: $notificationKey", e)
                    false
                }
            }
            
            return false
        }
    }

    private fun updateBadgeNotification(sbn: StatusBarNotification) {
        val activeNotifications = try {
            getActiveNotifications()
        } catch (_: SecurityException) {
            return
        }
        val prefs = com.github.gezimos.inkos.data.Prefs(applicationContext)

        val notificationInfo =
            notificationManager.buildNotificationInfo(sbn, prefs, activeNotifications)
        notificationManager.updateBadgeNotification(sbn.packageName, notificationInfo)
    }

    private fun updateConversationNotifications(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        if (notificationManager.isNotificationSummary(sbn)) {
            return
        }
        
        if (sbn.notification.category == android.app.Notification.CATEGORY_TRANSPORT) {
            return
        }
        
        // Extract conversation title - try multiple fields
        val conversationTitleRaw = extras.getCharSequence("android.conversationTitle")?.toString()
            ?: extras.getString("android.conversationTitle")
        
        // Extract sender/title - try multiple fields
        val senderRaw = extras.getCharSequence("android.title")?.toString()
            ?: extras.getString("android.title")
            ?: extras.getCharSequence("android.subText")?.toString()
            ?: extras.getString("android.subText")
        
        val messageRaw = when {
            extras.getCharSequence("android.bigText") != null -> 
                extras.getCharSequence("android.bigText")?.toString()

            extras.getCharSequence("android.text") != null -> 
                extras.getCharSequence("android.text")?.toString()

            extras.getCharSequenceArray("android.textLines") != null -> {
                val lines = extras.getCharSequenceArray("android.textLines")
                lines?.lastOrNull()?.toString()
            }
            
            // Try additional fields that some apps use
            extras.getCharSequence("android.summaryText") != null -> 
                extras.getCharSequence("android.summaryText")?.toString()
            
            extras.getCharSequence("android.infoText") != null -> 
                extras.getCharSequence("android.infoText")?.toString()
            
            sbn.notification.tickerText != null -> 
                sbn.notification.tickerText?.toString()

            else -> null
        }
        
        val notificationGroup = sbn.notification.group
        val conversationId = when {
            !notificationGroup.isNullOrBlank() -> "group_${packageName}_$notificationGroup"
            
            !conversationTitleRaw.isNullOrBlank() -> conversationTitleRaw
            
            sbn.notification.category == android.app.Notification.CATEGORY_MESSAGE -> {
                val phoneNumber = extras.getString("android.people")?.firstOrNull()?.toString()
                    ?: extras.getString("android.subText")
                    ?: extras.getString("android.summaryText")
                    ?: senderRaw
                
                phoneNumber?.let { "sms_$it" } ?: senderRaw ?: "default"
            }
            
            !senderRaw.isNullOrBlank() -> senderRaw
            
            // Fallback
            else -> "default"
        }
        
        var conversationTitle = conversationTitleRaw
        var sender = senderRaw
        var message = messageRaw?.replace("\n", " ")
            ?.replace("\r", " ")
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.take(300)
        
        if ((conversationTitle.isNullOrBlank() && sender.isNullOrBlank()) && (message.isNullOrBlank())) {
            val pm = applicationContext.packageManager
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }
            conversationTitle = appLabel
            sender = appLabel
            
            // Provide category-specific fallback text
            message = when (sbn.notification.category) {
                android.app.Notification.CATEGORY_MESSAGE -> "New message"
                android.app.Notification.CATEGORY_EMAIL -> "New email"
                android.app.Notification.CATEGORY_CALL -> "Missed call"
                android.app.Notification.CATEGORY_ALARM -> "Alarm"
                android.app.Notification.CATEGORY_REMINDER -> "Reminder"
                android.app.Notification.CATEGORY_EVENT -> "Event"
                android.app.Notification.CATEGORY_PROMO -> "Promotion"
                android.app.Notification.CATEGORY_SYSTEM -> "System notification"
                android.app.Notification.CATEGORY_SERVICE -> "Service notification"
                android.app.Notification.CATEGORY_ERROR -> "Error"
                android.app.Notification.CATEGORY_PROGRESS -> "Progress update"
                android.app.Notification.CATEGORY_SOCIAL -> "Social update"
                android.app.Notification.CATEGORY_STATUS -> "Status update"
                android.app.Notification.CATEGORY_RECOMMENDATION -> "Recommendation"
                else -> null
            }
        }
        
        val timestamp = sbn.postTime
        notificationManager.updateConversationNotification(
            packageName,
            NotificationManager.ConversationNotification(
                conversationId = conversationId,
                conversationTitle = conversationTitle,
                sender = sender,
                message = message,
                timestamp = timestamp,
                category = sbn.notification.category,
                notificationKey = sbn.key
            )
        )
    }

    /**
     * Send the conversation intent directly using the notification's PendingIntent.
     * This is the most reliable way to open the specific conversation.
     */
    private fun sendConversationIntent(packageName: String, notificationKey: String?): Boolean {
        if (notificationKey == null) return false
        
        return try {
            val activeNotifications = activeNotifications
            val notification = activeNotifications?.find { 
                it.packageName == packageName && it.key == notificationKey 
            }
            
            notification?.notification?.contentIntent?.let { pendingIntent ->
                val options = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.app.ActivityOptions.makeBasic().apply {
                        setPendingIntentBackgroundActivityStartMode(
                            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    }.toBundle()
                } else {
                    null
                }
                
                val fillInIntent = android.content.Intent().apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                pendingIntent.send(
                    this,           // context
                    0,              // request code
                    fillInIntent,   // fill-in intent with NEW_TASK flag
                    null,           // onFinished callback
                    null,           // handler
                    null,           // required permissions
                    options         // activity options to allow background start
                )
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Failed to send conversation intent", e)
            e.printStackTrace()
            false
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }
}
