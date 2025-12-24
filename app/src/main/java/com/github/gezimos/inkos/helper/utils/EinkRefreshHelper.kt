package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs

object EinkRefreshHelper {
    /**
     * Triggers an E-Ink refresh by flashing an overlay on the given ViewGroup.
     * @param context Context for theme and color resolution
     * @param prefs Prefs instance for theme and refresh settings
     * @param rootView The ViewGroup to add the overlay to
     * @param delayMs How long the overlay should be visible (ms)
     * @param useActivityRoot If true, will try to add overlay to activity decorView (for fragments with Compose root)
     */
    fun refreshEink(
        context: Context,
        prefs: Prefs,
        rootView: ViewGroup?,
        delayMs: Int = 100,
        useActivityRoot: Boolean = false
    ) {
        if (!prefs.einkRefreshEnabled) return

        // Ensure UI operations are performed on the main thread
        Handler(Looper.getMainLooper()).post {
            val isDark = prefs.appTheme == Constants.Theme.Dark
            val overlayColor =
                if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            val overlay = View(context)
            overlay.setBackgroundColor(overlayColor)
            overlay.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val parent = if (useActivityRoot) {
                (context as? android.app.Activity)?.window?.decorView as? ViewGroup
            } else {
                rootView
            }
            parent?.addView(overlay)
            overlay.bringToFront()
            Handler(Looper.getMainLooper()).postDelayed({
                parent?.removeView(overlay)
            }, delayMs.toLong())
        }
    }

    /**
     * Forces an E-Ink refresh by flashing an overlay, bypassing the einkRefreshEnabled preference.
     * This is useful for gesture-triggered refreshes that should work independently of the global setting.
     * @param context Context for theme and color resolution
     * @param prefs Prefs instance for theme resolution
     * @param rootView The ViewGroup to add the overlay to
     * @param delayMs How long the overlay should be visible (ms)
     * @param useActivityRoot If true, will try to add overlay to activity decorView (for fragments with Compose root)
     */
    fun refreshEinkForced(
        context: Context,
        prefs: Prefs,
        rootView: ViewGroup?,
        delayMs: Int = 100,
        useActivityRoot: Boolean = false
    ) {
        // Ensure UI operations are performed on the main thread
        Handler(Looper.getMainLooper()).post {
            val isDark = prefs.appTheme == Constants.Theme.Dark
            val overlayColor =
                if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            val overlay = View(context)
            overlay.setBackgroundColor(overlayColor)
            overlay.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val parent = if (useActivityRoot) {
                (context as? android.app.Activity)?.window?.decorView as? ViewGroup
            } else {
                rootView
            }
            parent?.addView(overlay)
            overlay.bringToFront()
            Handler(Looper.getMainLooper()).postDelayed({
                parent?.removeView(overlay)
            }, delayMs.toLong())
        }
    }
}