package com.github.gezimos.inkos.services

import android.content.Context
import android.media.session.MediaSession
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.github.gezimos.inkos.helper.AudioWidgetHelper

class NotificationService : NotificationListenerService() {
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioWidgetHelper: AudioWidgetHelper

    // Track active media controllers to monitor metadata changes
    private val activeMediaControllers =
        mutableMapOf<String, android.media.session.MediaController>()
    private val mediaCallbacks =
        mutableMapOf<String, android.media.session.MediaController.Callback>()

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManager.getInstance(applicationContext)
        audioWidgetHelper = AudioWidgetHelper.getInstance(applicationContext)
        notificationManager.restoreConversationNotifications()

        // Set up callback to refresh notifications when widget actions occur
        audioWidgetHelper.setMediaActionCallback(object : AudioWidgetHelper.MediaActionCallback {
            override fun onMediaActionPerformed(packageName: String) {
                refreshMediaNotificationForPackage(packageName)
            }
        })
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Only restore active media playback notifications after service (re)start
        activeNotifications?.filter {
            it.notification.category == android.app.Notification.CATEGORY_TRANSPORT
        }?.forEach { sbn ->
            updateBadgeNotification(sbn)
            if (shouldShowNotification(sbn.packageName)) {
                updateConversationNotifications(sbn)
            }

            // Also restore audio widget state with proper MediaController
            val extras = sbn.notification.extras
            val token =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    extras?.getParcelable("android.mediaSession", MediaSession.Token::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras?.getParcelable<MediaSession.Token>("android.mediaSession")
                }

            if (token != null) {
                try {
                    val controller = android.media.session.MediaController(this, token)
                    val playbackState = controller.playbackState
                    val isPlaying =
                        playbackState != null && playbackState.state == android.media.session.PlaybackState.STATE_PLAYING
                    val isPaused =
                        playbackState != null && playbackState.state == android.media.session.PlaybackState.STATE_PAUSED

                    if (isPlaying || isPaused) {
                        // Clear other media notifications if this one is playing
                        if (isPlaying) {
                            clearOtherMediaNotifications(sbn.packageName)
                        }

                        // Restore audio widget with proper MediaController
                        val title = extras?.getCharSequence("android.title")?.toString()
                        val text = when {
                            extras?.getCharSequence("android.bigText") != null ->
                                extras.getCharSequence("android.bigText")?.toString()?.take(30)

                            extras?.getCharSequence("android.text") != null ->
                                extras.getCharSequence("android.text")?.toString()?.take(30)

                            extras?.getCharSequenceArray("android.textLines") != null -> {
                                val lines = extras.getCharSequenceArray("android.textLines")
                                lines?.lastOrNull()?.toString()?.take(30)
                            }

                            else -> null
                        }

                        audioWidgetHelper.updateMediaPlayer(
                            packageName = sbn.packageName,
                            token = token,
                            isPlaying = isPlaying,
                            title = title,
                            artist = text
                        )

                        // Register callback to monitor changes
                        registerMediaControllerCallback(sbn.packageName, controller)
                    }
                } catch (_: Exception) {
                    // If MediaController fails, clear any stale widget state
                    audioWidgetHelper.clearMediaPlayer()
                }
            }
        }
        // Clean up any stale media notifications on connect
        cleanupStaleMediaNotifications()
    }

    override fun onDestroy() {
        // Clean up all media controller callbacks
        activeMediaControllers.keys.toList().forEach { packageName ->
            cleanupMediaControllerCallback(packageName)
        }
        super.onDestroy()
    }

    private fun registerMediaControllerCallback(
        packageName: String,
        controller: android.media.session.MediaController
    ) {
        // Remove existing callback if any
        val existingCallback = mediaCallbacks[packageName]
        if (existingCallback != null) {
            try {
                activeMediaControllers[packageName]?.unregisterCallback(existingCallback)
            } catch (_: Exception) {
            }
        }

        // Create new callback to monitor metadata changes
        val callback = object : android.media.session.MediaController.Callback() {
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                super.onMetadataChanged(metadata)
                // When metadata changes (new track), refresh the notification badge
                refreshMediaNotificationForPackage(packageName)
            }

            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                super.onPlaybackStateChanged(state)
                // When playback state changes, refresh the notification badge
                refreshMediaNotificationForPackage(packageName)
            }

            override fun onSessionDestroyed() {
                super.onSessionDestroyed()
                // Clean up when session is destroyed
                mediaCallbacks.remove(packageName)
                activeMediaControllers.remove(packageName)
                notificationManager.clearMediaNotification(packageName)
            }
        }

        try {
            controller.registerCallback(callback)
            activeMediaControllers[packageName] = controller
            mediaCallbacks[packageName] = callback
        } catch (_: Exception) {
            // If callback registration fails, clean up
            mediaCallbacks.remove(packageName)
            activeMediaControllers.remove(packageName)
        }
    }

    private fun shouldShowNotification(packageName: String): Boolean {
        val prefs = com.github.gezimos.inkos.data.Prefs(this)
        val allowed = prefs.allowedNotificationApps
        // If allowlist is empty, allow all. Otherwise, only allow if in allowlist.
        return allowed.isEmpty() || allowed.contains(packageName)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Removed aggressive SMS cleanup - let SMS apps and system handle their own notification management
        // This allows proper conversation threading like other messaging apps (Signal, WhatsApp, etc.)
        
        // Debug logging for SMS notifications
        if (sbn.notification.category == android.app.Notification.CATEGORY_MESSAGE) {
            android.util.Log.d("NotificationService", "SMS Posted: ${sbn.packageName} - ${sbn.key}")
            val extras = sbn.notification.extras
            android.util.Log.d("NotificationService", "SMS Title: ${extras?.getString("android.title")}")
            android.util.Log.d("NotificationService", "SMS ConversationTitle: ${extras?.getString("android.conversationTitle")}")
        }
        
        // Always update badge notification, let NotificationManager filter by allowlist
    // Clear any transient home-screen suppression for this package when a new notification arrives
    com.github.gezimos.inkos.helper.utils.NotificationBadgeUtil.clearSuppression(sbn.packageName)
    updateBadgeNotification(sbn)

        // Handle media widget updates for TRANSPORT category notifications
        if (sbn.notification.category == android.app.Notification.CATEGORY_TRANSPORT) {
            val extras = sbn.notification.extras
            val token =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    extras?.getParcelable("android.mediaSession", MediaSession.Token::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras?.getParcelable<MediaSession.Token>("android.mediaSession")
                }

            if (token != null) {
                try {
                    val controller = android.media.session.MediaController(this, token)
                    val playbackState = controller.playbackState
                    val isPlaying =
                        playbackState != null && playbackState.state == android.media.session.PlaybackState.STATE_PLAYING
                    val isPaused =
                        playbackState != null && playbackState.state == android.media.session.PlaybackState.STATE_PAUSED

                    // Register callback to monitor automatic track changes
                    registerMediaControllerCallback(sbn.packageName, controller)

                    if (isPlaying || isPaused) {
                        // Clear media notifications from other apps when new media starts playing
                        if (isPlaying) {
                            clearOtherMediaNotifications(sbn.packageName)
                        }

                        // Show widget for both playing and paused states
                        val title = extras?.getCharSequence("android.title")?.toString()
                        val text = when {
                            extras?.getCharSequence("android.bigText") != null ->
                                extras.getCharSequence("android.bigText")?.toString()?.take(30)

                            extras?.getCharSequence("android.text") != null ->
                                extras.getCharSequence("android.text")?.toString()?.take(30)

                            extras?.getCharSequenceArray("android.textLines") != null -> {
                                val lines = extras.getCharSequenceArray("android.textLines")
                                lines?.lastOrNull()?.toString()?.take(30)
                            }

                            else -> null
                        }

                        audioWidgetHelper.updateMediaPlayer(
                            packageName = sbn.packageName,
                            token = token,
                            isPlaying = isPlaying, // Pass actual playing state
                            title = title,
                            artist = text
                        )

                    } else {
                        // Clear widget only if stopped/error (not paused)
                        audioWidgetHelper.clearMediaPlayer()
                        // Also clear this app's media notification
                        notificationManager.clearMediaNotification(sbn.packageName)
                    }
                } catch (_: Exception) {
                    audioWidgetHelper.clearMediaPlayer()
                    notificationManager.clearMediaNotification(sbn.packageName)
                }
            }
        }

        // Only update conversation notifications if allowed in notification allowlist
        if (shouldShowNotification(sbn.packageName)) {
            updateConversationNotifications(sbn)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Debug logging for SMS notifications
        if (sbn.notification.category == android.app.Notification.CATEGORY_MESSAGE) {
            android.util.Log.d("NotificationService", "SMS Removed: ${sbn.packageName} - ${sbn.key}")
        }
        
        // Fixed: Properly handle badge clearing when system removes notifications
        // Check if there are any remaining active notifications for this package
        val activeNotifications = getActiveNotifications()
        val remainingForPackage = activeNotifications.filter { 
            it.packageName == sbn.packageName && it.key != sbn.key 
        }
        
        if (remainingForPackage.isEmpty()) {
            // No more notifications for this package, clear the badge completely
            notificationManager.updateBadgeNotification(sbn.packageName, null)
        } else {
            // There are remaining notifications, update badge with the most recent one
            val prefs = com.github.gezimos.inkos.data.Prefs(applicationContext)
            val notificationInfo = notificationManager.buildNotificationInfoForRemaining(
                sbn.packageName, prefs, activeNotifications
            )
            notificationManager.updateBadgeNotification(sbn.packageName, notificationInfo)
        }

        // Check if removed notification was media and clear player if needed
        if (sbn.notification.category == android.app.Notification.CATEGORY_TRANSPORT) {
            val currentPlayer = audioWidgetHelper.getCurrentMediaPlayer()
            if (currentPlayer?.packageName == sbn.packageName) {
                audioWidgetHelper.clearMediaPlayer()
            }
            // Also clear this app's media notification explicitly
            notificationManager.clearMediaNotification(sbn.packageName)

            // Clean up media controller callback
            cleanupMediaControllerCallback(sbn.packageName)
        }
    }

    private fun refreshMediaNotificationForPackage(packageName: String) {
        // Find the current media notification for this package and refresh it
        activeNotifications?.find {
            it.packageName == packageName &&
                    it.notification.category == android.app.Notification.CATEGORY_TRANSPORT
        }?.let { sbn ->
            try {
                // Simply reprocess the notification to get fresh metadata from media controller
                updateBadgeNotification(sbn)
            } catch (e: Exception) {
                // If we can't process the notification, clear it
                notificationManager.clearMediaNotification(packageName)
            }
        } ?: run {
            // No active notification found for this package, clear any cached notification
            notificationManager.clearMediaNotification(packageName)
        }
    }

    private fun cleanupStaleMediaNotifications() {
        // Check all media notifications and clear ones that are no longer playing
        activeNotifications?.filter {
            it.notification.category == android.app.Notification.CATEGORY_TRANSPORT
        }?.forEach { sbn ->
            val extras = sbn.notification.extras
            val token =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    extras?.getParcelable("android.mediaSession", MediaSession.Token::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras?.getParcelable<MediaSession.Token>("android.mediaSession")
                }

            val shouldClear = if (token != null) {
                try {
                    val controller = android.media.session.MediaController(this, token)
                    val playbackState = controller.playbackState
                    // Clear notification if not playing
                    playbackState == null || playbackState.state != android.media.session.PlaybackState.STATE_PLAYING
                } catch (_: Exception) {
                    true // Clear if we can't access media controller
                }
            } else {
                true // Clear if no media session token
            }

            if (shouldClear) {
                notificationManager.clearMediaNotification(sbn.packageName)
                cleanupMediaControllerCallback(sbn.packageName)
            }
        }
    }

    private fun cleanupMediaControllerCallback(packageName: String) {
        val callback = mediaCallbacks[packageName]
        if (callback != null) {
            try {
                activeMediaControllers[packageName]?.unregisterCallback(callback)
            } catch (_: Exception) {
            }
            activeMediaControllers.remove(packageName)
            mediaCallbacks.remove(packageName)
        }
    }

    private fun clearOtherMediaNotifications(currentPlayingPackage: String) {
        // Get all active notifications and clear media notifications from other apps
        activeNotifications?.filter {
            it.notification.category == android.app.Notification.CATEGORY_TRANSPORT &&
                    it.packageName != currentPlayingPackage
        }?.forEach { sbn ->
            // Clear the media notification for this app
            notificationManager.clearMediaNotification(sbn.packageName)
        }
    }

    private fun updateBadgeNotification(sbn: StatusBarNotification) {
        val activeNotifications = getActiveNotifications()
        val prefs = com.github.gezimos.inkos.data.Prefs(applicationContext)

        // Use the original buildNotificationInfo logic from NotificationManager
        val notificationInfo =
            notificationManager.buildNotificationInfo(sbn, prefs, activeNotifications)
        notificationManager.updateBadgeNotification(sbn.packageName, notificationInfo)
    }

    private fun updateConversationNotifications(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        // Filter out summary notifications early to prevent them from appearing in NotificationsFragment
        if (notificationManager.isNotificationSummary(sbn)) {
            // android.util.Log.d("NotificationService", "Filtering out summary notification from $packageName for conversations")
            return
        }
        
        val conversationTitleRaw = extras.getString("android.conversationTitle")
        val senderRaw = extras.getString("android.title")
        val messageRaw = when {
            extras.getCharSequence("android.bigText") != null -> extras.getCharSequence("android.bigText")
                ?.toString()

            extras.getCharSequence("android.text") != null -> extras.getCharSequence("android.text")
                ?.toString()

            extras.getCharSequenceArray("android.textLines") != null -> {
                val lines = extras.getCharSequenceArray("android.textLines")
                lines?.lastOrNull()?.toString()
            }

            else -> null
        }
        
        // Improved conversation ID logic for better SMS threading
        val conversationId = when {
            // Use conversation title if available (group chats)
            !conversationTitleRaw.isNullOrBlank() -> conversationTitleRaw
            
            // For SMS apps, try to extract phone number or contact from various fields
            sbn.notification.category == android.app.Notification.CATEGORY_MESSAGE -> {
                // Try different extras that SMS apps use for phone numbers/contacts
                val phoneNumber = extras.getString("android.people")?.firstOrNull()?.toString()
                    ?: extras.getString("android.subText")
                    ?: extras.getString("android.summaryText")
                    ?: senderRaw
                
                // Use phone number/contact as conversation ID for better threading
                phoneNumber?.let { "sms_$it" } ?: senderRaw ?: "default"
            }
            
            // For other messaging apps, use sender as conversation ID
            !senderRaw.isNullOrBlank() -> senderRaw
            
            // Fallback
            else -> "default"
        }
        
        var conversationTitle = conversationTitleRaw
        var sender = senderRaw
        var message = messageRaw
        
        // Fallback: if both sender/title and message are missing, use app label and 'Notification received'
        if ((conversationTitle.isNullOrBlank() && sender.isNullOrBlank()) && (message.isNullOrBlank())) {
            val pm = applicationContext.packageManager
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }
            conversationTitle = appLabel
            sender = appLabel
            message = "Notification received"
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
                category = sbn.notification.category
            )
        )
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
    }
}