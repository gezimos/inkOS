package com.github.gezimos.inkos

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Thanks to UndefinedProgrammer for providing the fix for E-ink helper.
 * Credits: https://github.com/UndefinedProgrammer/InkMaster
 */
class EinkHelper(private val packageName: String) : DefaultLifecycleObserver {

    private var meinkService: IBinder? = null
    private var currentMeinkMode: Int = MEINK_MODE_DISABLED

    companion object {
        private const val TAG = "EinkHelper"
        private const val MEINK_SERVICE_NAME = "meink"
        private const val MEINK_DESCRIPTOR = "android.meink.IMeinkService"
        private const val MEINK_SET_MODE_TRANSACTION = 5
        const val MEINK_MODE_DISABLED = 0
        const val MEINK_MODE_CONTRAST = 1
        const val MEINK_MODE_CLEAR = 3
        const val MEINK_MODE_READING = 4

        fun modeName(mode: Int): String = when (mode) {
            MEINK_MODE_CLEAR -> "Clear"
            MEINK_MODE_CONTRAST -> "Contrast"
            MEINK_MODE_READING -> "Reading"
            else -> "Disabled"
        }

        fun nextMode(current: Int): Int = when (current) {
            MEINK_MODE_DISABLED -> MEINK_MODE_CLEAR
            MEINK_MODE_CLEAR -> MEINK_MODE_CONTRAST
            MEINK_MODE_CONTRAST -> MEINK_MODE_READING
            else -> MEINK_MODE_DISABLED
        }

        fun isMuditaKompakt(): Boolean =
            com.github.gezimos.inkos.helper.device.DeviceHelper.isMuditaKompakt()
    }

    @SuppressLint("PrivateApi")
    fun initializeMeinkService() {
        if (meinkService != null) return

        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod =
                serviceManagerClass.getDeclaredMethod("getService", String::class.java)

            meinkService = getServiceMethod.invoke(null, MEINK_SERVICE_NAME) as? IBinder

            if (meinkService != null) {
                Log.d(TAG, "MeInk service initialized successfully")
                setMeinkMode(currentMeinkMode)
            } else {
                Log.w(TAG, "MeInk service not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MeInk service", e)
        }
    }

    fun setMeinkMode(mode: Int) {
        if (mode == MEINK_MODE_DISABLED) {
            currentMeinkMode = mode
            Log.i(TAG, "MeInk disabled, using device default")
            return
        }
        if (meinkService == null) {
            Log.w(TAG, "MeInk service not ready, deferring mode $mode")
            currentMeinkMode = mode
            initializeMeinkService()
            return
        }

        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(MEINK_DESCRIPTOR)
            data.writeString(packageName)
            data.writeInt(mode)
            val success = meinkService!!.transact(MEINK_SET_MODE_TRANSACTION, data, reply, 0)
            if (success) {
                reply.readException()
                currentMeinkMode = mode
                Log.i(TAG, "MeInk mode set to $mode successfully")
            } else {
                Log.w(TAG, "MeInk setMode($mode) transact returned false")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in setMeinkMode($mode)", e)
            meinkService = null
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun getCurrentMeinkMode(): Int = currentMeinkMode

    fun reinitializeMeinkService() {
        meinkService = null
        initializeMeinkService()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        meinkService = null
    }
}
