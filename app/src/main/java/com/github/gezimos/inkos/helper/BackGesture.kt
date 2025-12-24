package com.github.gezimos.inkos.helper

import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.utils.VibrationHelper as VH

/**
 * Helper that detects horizontal edge swipes and triggers NavController.popBackStack().
 * The caller should provide an allowProvider lambda which returns true when edge-swipe
 * behavior is currently allowed (e.g. fragment opt-out + prefs).
 */
class BackGesture(
    private val activityRoot: View,
    private val navController: NavController,
    private val prefs: Prefs,
    private val allowProvider: () -> Boolean = { true }
) {
    private var initialX = 0f
    private var initialY = 0f
    private var prevX = 0f
    private var totalX = 0f
    private var active = false

    fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            if (!prefs.edgeSwipeBackEnabled) return false
            if (!allowProvider()) return false

            val density = activityRoot.resources.displayMetrics.density
            val edgeThresholdPx = 48 * density
            val flingThreshold = 64 * density

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = ev.x
                    initialY = ev.y
                    prevX = ev.x
                    totalX = 0f
                    val sysInsets = ViewCompat.getRootWindowInsets(activityRoot)
                        ?.getInsets(WindowInsetsCompat.Type.systemGestures())
                    val leftInset = sysInsets?.left ?: 0
                    val rightInset = sysInsets?.right ?: 0
                    val width = activityRoot.width
                    active = (initialX <= edgeThresholdPx + leftInset) || (initialX >= width - (edgeThresholdPx + rightInset))
                }
                MotionEvent.ACTION_MOVE -> {
                    if (active) {
                        val dx = ev.x - prevX
                        totalX += dx
                        prevX = ev.x
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (active) {
                        val dy = ev.y - initialY
                        val absX = kotlin.math.abs(totalX)
                        val absY = kotlin.math.abs(dy)
                        if (absX > flingThreshold && absX > absY) {
                            try {
                                if (navController.currentDestination?.id != com.github.gezimos.inkos.R.id.mainFragment) {
                                    navController.popBackStack()
                                    VH.trigger(VH.Effect.PAGE)
                                    active = false
                                    return true
                                }
                            } catch (_: Exception) {}
                        }
                    }
                    active = false
                }
            }
        } catch (_: Exception) {}
        return false
    }
}
