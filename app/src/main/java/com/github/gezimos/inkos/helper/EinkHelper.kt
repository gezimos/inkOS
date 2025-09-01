package com.github.gezimos.inkos

import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class EinkHelper : LifecycleObserver {

    private var meinkService: IBinder? = null
    private var isMeinkServiceInitialized = false
    private var currentMeinkMode: Int = MEINK_MODE_READING
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRetryMode: Int? = null
    private var retryAttempt: Int = 0

    companion object {
        private const val TAG = "EinkHelper"
        private const val MEINK_SERVICE_NAME = "meink"
        private const val MEINK_SET_MODE_TRANSACTION = 5
        const val MEINK_MODE_READING = 1
        const val MEINK_MODE_GAMMA = 2
        private const val MEINK_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    /**
     * Initialize MeInk service
     */
    fun initializeMeinkService() {
        if (isMeinkServiceInitialized) return

        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod =
                serviceManagerClass.getDeclaredMethod("getService", String::class.java)

            meinkService = getServiceMethod.invoke(null, MEINK_SERVICE_NAME) as? IBinder
            isMeinkServiceInitialized = true

            if (meinkService != null) {
                Log.d(TAG, "MeInk service initialized successfully")
                setMeinkMode(currentMeinkMode)
            } else {
                Log.w(TAG, "MeInk service not available, will retry")
                scheduleMeinkRetry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MeInk service", e)
            scheduleMeinkRetry()
        }
    }

    /**
     * Robust MeInk mode setting with retry logic
     */
    fun setMeinkMode(mode: Int, attempt: Int = 1) {
        if (!isMeinkServiceInitialized || meinkService == null) {
            Log.w(TAG, "MeInk service not ready, deferring mode $mode")
            currentMeinkMode = mode
            initializeMeinkService()
            return
        }

        try {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()

            try {
                data.writeInterfaceToken("android.meink.IMeinkService")
                data.writeInt(mode)

                val success = meinkService!!.transact(MEINK_SET_MODE_TRANSACTION, data, reply, 0)

                if (success) {
                    reply.readException()
                    currentMeinkMode = mode
                    Log.i(TAG, "MeInk mode set to $mode successfully")
                    handler.removeCallbacksAndMessages(null)
                    pendingRetryMode = null
                    retryAttempt = 0
                } else {
                    Log.w(TAG, "MeInk setMode($mode) failed, attempt $attempt")
                    if (attempt < MAX_RETRY_ATTEMPTS) {
                        scheduleMeinkRetry(mode, attempt + 1)
                    }
                }
            } finally {
                data.recycle()
                reply.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in setMeinkMode($mode), attempt $attempt", e)
            if (attempt < MAX_RETRY_ATTEMPTS) {
                scheduleMeinkRetry(mode, attempt + 1)
            }
        }
    }

    /**
     * Get current MeInk mode
     */
    fun getCurrentMeinkMode(): Int {
        return currentMeinkMode
    }

    /**
     * Schedule retry for MeInk service operations
     */
    private fun scheduleMeinkRetry(mode: Int? = null, attempt: Int = 1) {
        handler.removeCallbacksAndMessages(null)
        val retryDelay = MEINK_RETRY_DELAY_MS * attempt

        pendingRetryMode = mode
        retryAttempt = attempt

        handler.postDelayed({
            if (mode != null) {
                setMeinkMode(mode, attempt)
            } else {
                initializeMeinkService()
            }
        }, retryDelay)

        Log.d(TAG, "Scheduled MeInk retry in ${retryDelay}ms")
    }

    /**
     * Re-initialize MeInk service after configuration changes
     */
    fun reinitializeMeinkService() {
        isMeinkServiceInitialized = false
        meinkService = null
        initializeMeinkService()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        pendingRetryMode = null
        retryAttempt = 0
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        handler.removeCallbacksAndMessages(null)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        cleanup()
    }
}