package com.github.gezimos.inkos

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.WallpaperRotationWorker

class inkOS : Application() {
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()

        // Initialize com.github.gezimos.common.CrashHandler to catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        // Initialize prefs here
        prefs = Prefs(applicationContext)

        setCustomFont(applicationContext)

        // Re-schedule wallpaper rotation if enabled (survives reboots)
        if (prefs.wallpaperRotationEnabled) {
            val items = WallpaperRotationWorker.buildRotationItems(prefs)
            if (items.size >= 2) {
                WallpaperRotationWorker.schedule(applicationContext, prefs.wallpaperRotationInterval)
            }
        }

        // Log app launch
        CrashHandler.logUserAction("App Launched")
    }


    private fun setCustomFont(context: Context) {
        // Load the custom font from resources
        val customFont = prefs.fontFamily.getFont(context)

        // Apply the custom font to different font families
        if (customFont != null) {
            TypefaceUtil.setDefaultFont("DEFAULT", customFont)
            TypefaceUtil.setDefaultFont("MONOSPACE", customFont)
            TypefaceUtil.setDefaultFont("SERIF", customFont)
            TypefaceUtil.setDefaultFont("SANS_SERIF", customFont)
        }
    }
}

object TypefaceUtil {

    fun setDefaultFont(staticTypefaceFieldName: String, fontAssetName: Typeface) {
        Log.e("setDefaultFont", "$staticTypefaceFieldName | $fontAssetName")
        try {
            val staticField = Typeface::class.java.getDeclaredField(staticTypefaceFieldName)
            staticField.isAccessible = true
            staticField.set(null, fontAssetName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
