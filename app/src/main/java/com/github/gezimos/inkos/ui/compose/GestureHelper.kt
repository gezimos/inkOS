package com.github.gezimos.inkos.ui.compose

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.launchCalendar
import com.github.gezimos.common.openAlarmApp
import com.github.gezimos.common.openCameraApp
import com.github.gezimos.common.openDialerApp
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.initActionService
import com.github.gezimos.inkos.helper.utils.AppReloader
import com.github.gezimos.inkos.helper.utils.BrightnessHelper
import com.github.gezimos.inkos.helper.utils.EinkRefreshHelper
import com.github.gezimos.inkos.helper.utils.PrivateSpaceManager
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import kotlin.math.abs

/**
 * Centralized gesture handling logic for the entire app.
 * Provides both low-level gesture detection (swipes, taps) and high-level action execution.
 */
object GestureHelper {
    
    /**
     * Execute a configured action (from user preferences).
     */
    fun executeAction(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        action: Action,
        gestureApp: AppListItem? = null
    ) {
        when (action) {
            Action.Disabled -> {
                // Do nothing
            }
            Action.OpenApp -> {
                if (gestureApp != null && gestureApp.activityPackage.isNotEmpty()) {
                    viewModel?.launchApp(gestureApp)
                }
            }
            Action.OpenAppDrawer -> {
                fragment?.findNavController()?.navigate(R.id.appListFragment)
            }
            Action.OpenNotificationsScreen -> {
                fragment?.findNavController()?.navigate(R.id.notificationsFragment)
            }
            Action.OpenRecentsScreen -> {
                fragment?.findNavController()?.navigate(R.id.recentsFragment)
            }
            Action.OpenSimpleTray -> {
                fragment?.findNavController()?.navigate(R.id.simpleTrayFragment)
            }
            Action.EinkRefresh -> {
                try {
                    val prefs = Prefs(context)
                    EinkRefreshHelper.refreshEinkForced(
                        context,
                        prefs,
                        null,
                        prefs.einkRefreshDelay,
                        useActivityRoot = true
                    )
                } catch (_: Exception) {}
            }
            Action.LockScreen -> {
                try {
                    initActionService(context)?.lockScreen()
                } catch (_: Exception) {}
            }
            Action.ShowRecents -> {
                try {
                    initActionService(context)?.showRecents()
                } catch (_: Exception) {}
            }
            Action.OpenQuickSettings -> {
                try {
                    initActionService(context)?.openQuickSettings()
                } catch (_: Exception) {}
            }
            Action.OpenPowerDialog -> {
                try {
                    initActionService(context)?.openPowerDialog()
                } catch (_: Exception) {}
            }
            Action.RestartApp -> {
                try {
                    AppReloader.restartApp(context)
                } catch (_: Exception) {}
            }
            Action.ExitLauncher -> {
                fragment?.activity?.finish()
            }
            Action.TogglePrivateSpace -> {
                try {
                    PrivateSpaceManager(context).togglePrivateSpaceLock(showToast = true, launchSettings = true)
                } catch (_: Exception) {}
            }
            Action.Brightness -> {
                try {
                    val prefs = Prefs(context)
                    fragment?.activity?.window?.let { window ->
                        BrightnessHelper.toggleBrightness(context, prefs, window)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Handle swipe left gesture based on user preferences.
     */
    fun handleSwipeLeft(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.swipeLeftAction
        val app = prefs.appSwipeLeft
        
        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            } else {
                context.openCameraApp()
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }

    /**
     * Handle swipe right gesture based on user preferences.
     */
    fun handleSwipeRight(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.swipeRightAction
        val app = prefs.appSwipeRight
        
        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            } else {
                context.openDialerApp()
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }

    /**
     * Handle swipe up gesture based on user preferences.
     */
    fun handleSwipeUp(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.swipeUpAction
        val app = prefs.appSwipeUp

        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }

    /**
     * Handle swipe down gesture based on user preferences.
     */
    fun handleSwipeDown(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.swipeDownAction
        val app = prefs.appSwipeDown

        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }

    /**
     * Handle clock click gesture based on user preferences.
     */
    fun handleClockClick(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.clickClockAction
        val app = prefs.appClickClock
        
        // Provide a small haptic for clock taps
        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            } else {
                context.openAlarmApp()
            }
        } else if (action == Action.Disabled) {
            context.showShortToast(context.getString(R.string.edit_gestures_settings_toast))
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }

    /**
     * Handle date click gesture based on user preferences.
     */
    fun handleDateClick(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.clickDateAction
        val app = prefs.appClickDate
        
        // Small haptic for date taps
        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            } else {
                context.launchCalendar()
            }
        } else if (action == Action.Disabled) {
            context.showShortToast(context.getString(R.string.edit_gestures_settings_toast))
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }

    /**
     * Handle quote click gesture based on user preferences.
     */
    fun handleQuoteClick(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.quoteAction
        val app = prefs.appQuoteWidget
        
        // Small haptic for quote taps
        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            }
        } else if (action == Action.Disabled) {
            context.showShortToast(context.getString(R.string.edit_gestures_settings_toast))
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }

    /**
     * Handle double tap gesture based on user preferences.
     */
    fun handleDoubleTap(
        context: Context,
        fragment: Fragment?,
        viewModel: MainViewModel?,
        prefs: Prefs
    ) {
        val action = prefs.doubleTapAction
        val app = prefs.appDoubleTap
        
        // Haptic for double-tap gestures
        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
        if (action == Action.OpenApp) {
            if (app.activityPackage.isNotEmpty()) {
                viewModel?.launchApp(app)
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }
}
@Composable
fun Modifier.gestureHelper(
    swipeThreshold: Dp = 100.dp,
    shortSwipeRatio: Float = Constants.DEFAULT_SHORT_SWIPE_RATIO,
    longSwipeRatio: Float = Constants.DEFAULT_LONG_SWIPE_RATIO,
    pageMoveCooldownMs: Long = 30L, // Reduced for faster response
    onDoubleTap: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
    onVerticalPageMove: ((deltaPages: Int) -> Unit)? = null,
    onAnyTouch: (() -> Unit)? = null
): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { swipeThreshold.toPx() }.toInt()
    val velocityThreshold = 80 // Reduced from 100 for faster detection
    
    val lastPageMove = remember { androidx.compose.runtime.mutableStateOf(0L) }
    
    // PERFORMANCE FIX: Use rememberUpdatedState for callbacks to avoid stale closure captures
    val currentOnAnyTouch = androidx.compose.runtime.rememberUpdatedState(onAnyTouch)
    val currentOnDoubleTap = androidx.compose.runtime.rememberUpdatedState(onDoubleTap)
    val currentOnSwipeLeft = androidx.compose.runtime.rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight = androidx.compose.runtime.rememberUpdatedState(onSwipeRight)
    val currentOnSwipeUp = androidx.compose.runtime.rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown = androidx.compose.runtime.rememberUpdatedState(onSwipeDown)
    val currentOnVerticalPageMove = androidx.compose.runtime.rememberUpdatedState(onVerticalPageMove)
    
    val gestureDetector = remember(thresholdPx, shortSwipeRatio, longSwipeRatio) {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                try { currentOnAnyTouch.value?.invoke() } catch (_: Exception) {}
                return true
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                try { currentOnDoubleTap.value?.invoke() } catch (_: Exception) {}
                return super.onDoubleTap(e)
            }
            
            override fun onFling(
                event1: MotionEvent?,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                try {
                    if (event1 == null) return false
                    
                    val diffY = event2.y - event1.y
                    val diffX = event2.x - event1.x

                    // Directional locking from OnSwipeTouchListener
                    if (abs(diffX) > abs(diffY)) {
                        // Horizontal swipe dominates
                        if (abs(diffX) > thresholdPx && abs(velocityX) > velocityThreshold) {
                            if (diffX > 0) {
                                try { currentOnSwipeRight.value?.invoke() } catch (_: Exception) {}
                            } else {
                                try { currentOnSwipeLeft.value?.invoke() } catch (_: Exception) {}
                            }
                            return true
                        }
                    } else {
                        // Vertical swipe dominates
                        val hasLongSwipeCallbacks = currentOnSwipeUp.value != null || currentOnSwipeDown.value != null
                        
                        if (hasLongSwipeCallbacks) {
                            // Check long swipe first to avoid conflicts
                            val longSwipeThresholdPx = (thresholdPx * longSwipeRatio).toInt().coerceAtLeast(thresholdPx)
                            val longVelocityThreshold = (velocityThreshold * 1.8f).coerceAtLeast(velocityThreshold.toFloat())
                            val isLongSwipe = abs(diffY) > longSwipeThresholdPx && abs(velocityY) > longVelocityThreshold
                            
                            if (isLongSwipe) {
                                val handled = if (diffY < 0) {
                                    try {
                                        currentOnSwipeUp.value?.invoke()
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                } else {
                                    try {
                                        currentOnSwipeDown.value?.invoke()
                                        true
                                    } catch (_: Exception) {
                                        false
                                    }
                                }
                                if (handled) return true
                            }
                            
                            // If long swipe callbacks exist, only treat short swipes as page moves
                            val shortSwipeThresholdPx = (thresholdPx * shortSwipeRatio * 0.7f).toInt().coerceAtLeast(8)
                            val shortVelocityThreshold = (velocityThreshold * 0.3f).coerceAtLeast(1f)
                            val isShortSwipe = abs(diffY) > shortSwipeThresholdPx && abs(velocityY) > shortVelocityThreshold
                            
                            if (isShortSwipe && !isLongSwipe) {
                                val deltaPages = if (diffY < 0) 1 else -1
                                val now = System.currentTimeMillis()
                                
                                val callback = currentOnVerticalPageMove.value
                                if (callback != null && now - lastPageMove.value >= pageMoveCooldownMs) {
                                    try {
                                        callback(deltaPages)
                                        lastPageMove.value = now
                                        return true
                                    } catch (_: Exception) {}
                                }
                            }
                        } else {
                            // No long swipe callbacks - treat ALL vertical swipes as page moves (short or long)
                            val pageMoveThresholdPx = (thresholdPx * shortSwipeRatio * 0.7f).toInt().coerceAtLeast(8)
                            val pageMoveVelocityThreshold = (velocityThreshold * 0.3f).coerceAtLeast(1f)
                            val isVerticalSwipe = abs(diffY) > pageMoveThresholdPx && abs(velocityY) > pageMoveVelocityThreshold
                            
                            if (isVerticalSwipe) {
                                val deltaPages = if (diffY < 0) 1 else -1
                                val now = System.currentTimeMillis()
                                
                                val callback = currentOnVerticalPageMove.value
                                if (callback != null && now - lastPageMove.value >= pageMoveCooldownMs) {
                                    try {
                                        callback(deltaPages)
                                        lastPageMove.value = now
                                        return true
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                return false
            }
        })
    }
    
    // PERFORMANCE FIX: Reuse and properly recycle MotionEvent objects
    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var motionEvent = MotionEvent.obtain(
                down.uptimeMillis,
                down.uptimeMillis,
                MotionEvent.ACTION_DOWN,
                down.position.x,
                down.position.y,
                0
            )
            gestureDetector.onTouchEvent(motionEvent)
            motionEvent.recycle()
            
            do {
                val event = awaitPointerEvent()
                // Only process the first change to reduce allocations
                val change = event.changes.firstOrNull() ?: break
                motionEvent = MotionEvent.obtain(
                    change.uptimeMillis,
                    change.uptimeMillis,
                    when {
                        change.pressed -> MotionEvent.ACTION_MOVE
                        else -> MotionEvent.ACTION_UP
                    },
                    change.position.x,
                    change.position.y,
                    0
                )
                gestureDetector.onTouchEvent(motionEvent)
                motionEvent.recycle()
            } while (event.changes.any { it.pressed })
        }
    }
}
