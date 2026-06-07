package com.github.gezimos.inkos.helper

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("StaticFieldLeak")
class AudioWidgetHelper private constructor(private val context: Context) {

    data class MediaState(
        val packageName: String,
        val controller: MediaController,
        val metadata: MediaMetadata?,
        val playbackState: PlaybackState?,
        val isPlaying: Boolean
    ) {
        val title: String? get() = metadata?.description?.title?.toString()
        val artist: String? get() = metadata?.description?.subtitle?.toString()
    }

    private val _mediaState = MutableStateFlow<MediaState?>(null)

    data class MediaPlayerInfo(
        val packageName: String,
        val isPlaying: Boolean,
        val title: String?,
        val artist: String?,
        val controller: MediaController?
    )

    val mediaPlayerState: StateFlow<MediaPlayerInfo?> = MutableStateFlow<MediaPlayerInfo?>(null)
    private val _mediaPlayerState get() = mediaPlayerState as MutableStateFlow<MediaPlayerInfo?>

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeSessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private var currentController: MediaController? = null
    private var currentCallback: MediaController.Callback? = null
    private var userDismissed = false
    private var dismissedPackageName: String? = null

    companion object {
        @Volatile
        private var INSTANCE: AudioWidgetHelper? = null

        fun getInstance(context: Context): AudioWidgetHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioWidgetHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    fun initialize(componentName: ComponentName) {
        mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        setupActiveSessionsListener(componentName)
    }
    fun cleanup() {
        activeSessionsListener?.let { listener ->
            mediaSessionManager?.removeOnActiveSessionsChangedListener(listener)
        }
        activeSessionsListener = null
        unregisterCurrentCallback()
        mediaSessionManager = null
    }

    private fun setupActiveSessionsListener(componentName: ComponentName) {
        val manager = mediaSessionManager ?: return

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            handleActiveSessionsChanged(controllers)
        }

        activeSessionsListener = listener

        try {
            manager.addOnActiveSessionsChangedListener(listener, componentName)
            // Check initial sessions
            val controllers = manager.getActiveSessions(componentName)
            handleActiveSessionsChanged(controllers)
        } catch (e: Exception) {
            android.util.Log.e("AudioWidgetHelper", "Failed to set up sessions listener", e)
        }
    }

    private fun handleActiveSessionsChanged(controllers: List<MediaController>?) {
        val activeController = controllers?.firstOrNull { controller ->
            val state = controller.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED
        }

        if (activeController != null) {
            val dismissedPkg = dismissedPackageName ?: currentController?.packageName
            if (userDismissed &&
                activeController.packageName == dismissedPkg &&
                activeController.playbackState?.state != PlaybackState.STATE_PLAYING) {
                return
            }

            if (activeController.playbackState?.state == PlaybackState.STATE_PLAYING) {
                userDismissed = false
                dismissedPackageName = null
            }
            
            registerControllerCallback(activeController)
            updateState(activeController)
        } else {
            // No active sessions - clear state
            unregisterCurrentCallback()
            clearState()
        }
    }

    private fun registerControllerCallback(controller: MediaController) {
        // Only re-register if it's a different controller
        if (currentController?.sessionToken == controller.sessionToken) {
            return
        }

        unregisterCurrentCallback()

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateState(controller)
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                if (state?.state == PlaybackState.STATE_STOPPED) {
                    clearState()
                } else {
                    updateState(controller)
                }
            }

            override fun onSessionDestroyed() {
                clearState()
            }
        }

        try {
            controller.registerCallback(callback)
            currentController = controller
            currentCallback = callback
        } catch (e: Exception) {
            android.util.Log.e("AudioWidgetHelper", "Failed to register callback", e)
        }
    }

    private fun unregisterCurrentCallback() {
        currentCallback?.let { callback ->
            try {
                currentController?.unregisterCallback(callback)
            } catch (_: Exception) {}
        }
        currentCallback = null
        currentController = null
    }

    private fun updateState(controller: MediaController) {
        if (userDismissed && controller.playbackState?.state != PlaybackState.STATE_PLAYING) {
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        val state = MediaState(
            packageName = controller.packageName,
            controller = controller,
            metadata = metadata,
            playbackState = playbackState,
            isPlaying = isPlaying
        )
        _mediaState.value = state

        // Update legacy state for compatibility
        _mediaPlayerState.value = MediaPlayerInfo(
            packageName = controller.packageName,
            isPlaying = isPlaying,
            title = state.title,
            artist = state.artist,
            controller = controller
        )
    }

    private fun clearState() {
        _mediaState.value = null
        _mediaPlayerState.value = null
    }

    // ========== Public Control Methods ==========

    fun playPauseMedia(): Boolean {
        val controller = _mediaState.value?.controller ?: return false
        return try {
            val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            if (isPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun skipToNext(): Boolean {
        val controller = _mediaState.value?.controller ?: return false
        return try {
            controller.transportControls.skipToNext()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun skipToPrevious(): Boolean {
        val controller = _mediaState.value?.controller ?: return false
        return try {
            controller.transportControls.skipToPrevious()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stopMedia(): Boolean {
        val controller = _mediaState.value?.controller ?: return false
        return try {
            unregisterCurrentCallback()
            controller.transportControls.stop()
            dismissMediaPlayer()
            true
        } catch (_: Exception) {
            dismissMediaPlayer()
            false
        }
    }

    fun openMediaApp(): Boolean {
        val packageName = _mediaState.value?.packageName ?: return false
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                return true
            }
        } catch (e: Exception) {
            android.util.Log.w("AudioWidgetHelper", "Launch intent failed, will try sessionActivity if available", e)
        }

        val controller = _mediaState.value?.controller
        val pending = controller?.sessionActivity
        if (pending != null) {
            try {
                pending.send()
                return true
            } catch (e: Exception) {
                android.util.Log.w("AudioWidgetHelper", "sessionActivity.send() failed as fallback", e)
            }
        }

        return false
    }

    fun dismissMediaPlayer() {
        userDismissed = true
        dismissedPackageName = currentController?.packageName ?: _mediaState.value?.packageName
        clearState()
    }

    fun resetDismissalState() {
        val manager = mediaSessionManager ?: return
        val componentName = ComponentName(context, com.github.gezimos.inkos.services.NotificationService::class.java)
        try {
            val controllers = manager.getActiveSessions(componentName)
            handleActiveSessionsChanged(controllers)
        } catch (_: Exception) {}
    }

    // Legacy compatibility methods
    fun getCurrentMediaPlayer(): MediaPlayerInfo? = _mediaPlayerState.value
    
    @Suppress("unused")
    fun clearMediaPlayer() {
        clearState()
    }

    @Suppress("unused")
    fun forceRefreshState() {
        val manager = mediaSessionManager ?: return
        val componentName = ComponentName(context, com.github.gezimos.inkos.services.NotificationService::class.java)
        try {
            val controllers = manager.getActiveSessions(componentName)
            handleActiveSessionsChanged(controllers)
        } catch (_: Exception) {}
    }

    @Deprecated("No longer needed - MediaSessionManager handles this automatically")
    @Suppress("UNUSED_PARAMETER", "unused")
    fun updateMediaPlayer(
        packageName: String,
        token: android.media.session.MediaSession.Token?,
        isPlaying: Boolean,
        title: String?,
        artist: String?
    ) {
    }

    interface MediaActionCallback {
        fun onMediaActionPerformed(packageName: String)
    }

    @Deprecated("No longer needed - callbacks are handled internally")
    @Suppress("UNUSED_PARAMETER", "unused")
    fun setMediaActionCallback(callback: MediaActionCallback?) {
        // No-op - kept for compatibility
    }
}
