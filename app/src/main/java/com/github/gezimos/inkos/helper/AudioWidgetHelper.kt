package com.github.gezimos.inkos.helper

import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class AudioWidgetHelper private constructor(private val context: Context) {

    data class MediaPlayerInfo(
        val packageName: String,
        val isPlaying: Boolean,
        val title: String?,
        val artist: String?,
        val controller: MediaController?
    )

    // Callback interface for notification updates
    interface MediaActionCallback {
        fun onMediaActionPerformed(packageName: String)
    }

    // Media player state management for widget
    private var _currentMediaPlayer: MediaPlayerInfo? = null
    private val _mediaPlayerLiveData = MutableLiveData<MediaPlayerInfo?>()
    val mediaPlayerLiveData: LiveData<MediaPlayerInfo?> = _mediaPlayerLiveData
    private var userDismissedPlayer = false
    private var mediaActionCallback: MediaActionCallback? = null

    companion object {
        @Volatile
        private var INSTANCE: AudioWidgetHelper? = null
        fun getInstance(context: Context): AudioWidgetHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioWidgetHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun setMediaActionCallback(callback: MediaActionCallback?) {
        mediaActionCallback = callback
    }

    fun updateMediaPlayer(
        packageName: String,
        token: MediaSession.Token?,
        isPlaying: Boolean,
        title: String?,
        artist: String?
    ) {
        val controller = token?.let {
            try {
                MediaController(context, it)
            } catch (e: Exception) {
                null
            }
        }

        // The controller is the source of truth for playback state.
        val definitiveIsPlaying = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        val currentPlayer = _currentMediaPlayer

        // If a different app is reporting, only accept it if it's actively playing.
        // This prevents a "paused" notification from an old app from overwriting the current one.
        if (currentPlayer != null && currentPlayer.packageName != packageName) {
            if (!definitiveIsPlaying) {
                return // Ignore paused/stopped notifications from non-active media apps.
            }
            // New app is playing, so it will take over. Reset dismissal state.
            userDismissedPlayer = false
        }

        // If the user dismissed the widget, don't show it again unless it starts playing.
        if (userDismissedPlayer && currentPlayer?.packageName == packageName && !definitiveIsPlaying) {
            return
        }

        // If we're updating a player to a "playing" state, reset any prior dismissal.
        if (definitiveIsPlaying) {
            userDismissedPlayer = false
        }

        val mediaInfo = MediaPlayerInfo(
            packageName = packageName,
            isPlaying = definitiveIsPlaying,
            title = title,
            artist = artist,
            controller = controller
        )

        _currentMediaPlayer = mediaInfo
        _mediaPlayerLiveData.postValue(mediaInfo)
    }

    fun clearMediaPlayer() {
        _currentMediaPlayer = null
        _mediaPlayerLiveData.postValue(null)
        userDismissedPlayer = false
    }

    fun dismissMediaPlayer() {
        userDismissedPlayer = true
        _currentMediaPlayer = null
        _mediaPlayerLiveData.postValue(null)
    }

    fun resetDismissalState() {
        userDismissedPlayer = false
    }

    fun forceRefreshState() {
        // Re-post current state to trigger UI updates
        _mediaPlayerLiveData.postValue(_currentMediaPlayer)
    }

    fun playPauseMedia(): Boolean {
        val controller = _currentMediaPlayer?.controller
        val packageName = _currentMediaPlayer?.packageName

        return if (controller != null && packageName != null) {
            try {
                val playbackState = controller.playbackState

                val newState = when (playbackState?.state) {
                    PlaybackState.STATE_PLAYING -> {
                        controller.transportControls.pause()
                        false // Will be paused
                    }

                    PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> {
                        controller.transportControls.play()
                        true // Will be playing
                    }

                    else -> {
                        return false
                    }
                }

                // Update local state immediately for responsive UI
                _currentMediaPlayer = _currentMediaPlayer?.copy(isPlaying = newState)
                _mediaPlayerLiveData.postValue(_currentMediaPlayer)

                // Trigger notification refresh after a short delay to sync with media session
                mediaActionCallback?.onMediaActionPerformed(packageName)

                // Post delayed sync to ensure state consistency
                Handler(Looper.getMainLooper()).postDelayed({
                    // Verify actual controller state and sync if needed
                    try {
                        val actualState = controller.playbackState?.state
                        val expectedPlaying = when (actualState) {
                            PlaybackState.STATE_PLAYING -> true
                            PlaybackState.STATE_PAUSED, PlaybackState.STATE_STOPPED -> false
                            else -> null
                        }

                        if (expectedPlaying != null && expectedPlaying != _currentMediaPlayer?.isPlaying) {
                            _currentMediaPlayer =
                                _currentMediaPlayer?.copy(isPlaying = expectedPlaying)
                            _mediaPlayerLiveData.postValue(_currentMediaPlayer)
                        }
                    } catch (e: Exception) {
                        // Ignore sync errors
                    }
                }, 150)

                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun skipToNext(): Boolean {
        val controller = _currentMediaPlayer?.controller
        val packageName = _currentMediaPlayer?.packageName
        return if (controller != null && packageName != null) {
            controller.transportControls.skipToNext()
            // Trigger notification refresh for track change
            mediaActionCallback?.onMediaActionPerformed(packageName)
            true
        } else false
    }

    fun skipToPrevious(): Boolean {
        val controller = _currentMediaPlayer?.controller
        val packageName = _currentMediaPlayer?.packageName
        return if (controller != null && packageName != null) {
            controller.transportControls.skipToPrevious()
            // Trigger notification refresh for track change
            mediaActionCallback?.onMediaActionPerformed(packageName)
            true
        } else false
    }

    fun stopMedia(): Boolean {
        val controller = _currentMediaPlayer?.controller
        val packageName = _currentMediaPlayer?.packageName
        return if (controller != null && packageName != null) {
            try {
                controller.transportControls.stop()
            } catch (e: Exception) {
                // The controller might be stale, but we still want to dismiss the widget.
            }

            // Immediately dismiss the widget when stop is pressed
            dismissMediaPlayer()

            // Trigger notification refresh to clear media badges
            mediaActionCallback?.onMediaActionPerformed(packageName)

            true
        } else false
    }

    fun openMediaApp(): Boolean {
        val packageName = _currentMediaPlayer?.packageName
        return if (packageName != null) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                    true
                } ?: false
            } catch (e: Exception) {
                false
            }
        } else false
    }

    fun getCurrentMediaPlayer(): MediaPlayerInfo? = _currentMediaPlayer
}