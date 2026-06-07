package com.github.gezimos.inkos.ui.compose

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.os.bundleOf
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.initActionService
import com.github.gezimos.inkos.data.repository.AppsRepository
import com.github.gezimos.inkos.helper.utils.AppReloader
import com.github.gezimos.inkos.helper.utils.BrightnessHelper
import com.github.gezimos.inkos.helper.utils.EinkRefreshHelper
import com.github.gezimos.inkos.helper.utils.ProfileManager
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import kotlin.math.abs

object GestureHelper {

    @Volatile
    var lastElementClickTime = 0L
        private set

    @Volatile var exclusionY = 0f
    @Volatile var exclusionHeight = 0f

    fun notifyElementClicked() {
        lastElementClickTime = System.currentTimeMillis()
    }
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
                    val appsRepo = AppsRepository.getInstance(
                        context.applicationContext as android.app.Application
                    )
                    if (fragment != null &&
                        appsRepo.launchSyntheticOrSystemApp(
                            context, gestureApp.activityPackage, fragment, gestureApp.shortcutId
                        )
                    ) {
                        return
                    }
                    viewModel?.launchApp(gestureApp)
                }
            }
            Action.OpenAppDrawer -> {
                fragment?.findNavController()?.navigate(R.id.appListFragment)
            }
            Action.OpenLettersScreen -> {
                fragment?.findNavController()?.navigate(R.id.lettersFragment)
            }
            Action.OpenRecentsScreen -> {
                fragment?.findNavController()?.navigate(R.id.recentsFragment)
            }
            Action.OpenSimpleTray -> {
                fragment?.findNavController()?.navigate(R.id.simpleTrayFragment)
            }
            Action.OpenHub -> {
                fragment?.findNavController()?.navigate(R.id.hubFragment)
            }
            Action.OpenSettings -> {
                fragment?.findNavController()?.navigate(R.id.settingsFragment)
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    try { initActionService(context)?.lockScreen() } catch (_: Exception) {}
                }
            }
            Action.ShowRecents -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    try { initActionService(context)?.showRecents() } catch (_: Exception) {}
                }
            }
            Action.OpenQuickSettings -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    try { initActionService(context)?.openQuickSettings() } catch (_: Exception) {}
                }
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
                    ProfileManager(context).togglePrivateSpaceLock(showToast = true, launchSettings = true)
                } catch (_: Exception) {}
            }
            Action.ToggleWorkProfile -> {
                try {
                    ProfileManager(context).toggleWorkProfile(showToast = true)
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
            Action.Search -> {
                val bundle = bundleOf(
                    "flag" to Constants.AppDrawerFlag.LaunchApp.toString(),
                    "n" to 0,
                    "showSearch" to true,
                    "searchMode" to 0
                )
                try {
                    fragment?.findNavController()?.navigate(R.id.appsFragment, bundle)
                } catch (_: Exception) {
                    try {
                        fragment?.findNavController()?.navigate(R.id.action_mainFragment_to_appListFragment, bundle)
                    } catch (_: Exception) {
                        try {
                            fragment?.findNavController()?.navigate(R.id.appListFragment, bundle)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
            } else {
                context.openCameraApp()
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
            } else {
                context.openDialerApp()
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
            }
        } else {
            executeAction(context, fragment, viewModel, action)
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
            } else {
                context.openAlarmApp()
            }
        } else if (action != Action.Disabled) {
            executeAction(context, fragment, viewModel, action)
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
            } else {
                context.launchCalendar()
            }
        } else if (action != Action.Disabled) {
            executeAction(context, fragment, viewModel, action)
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
            }
        } else if (action != Action.Disabled) {
            executeAction(context, fragment, viewModel, action)
        }
    }
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
                executeAction(context, fragment, viewModel, action, app)
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
    onLongSwipeDown: (() -> Unit)? = null,
    onVerticalPageMove: ((deltaPages: Int) -> Unit)? = null,
    onAnyTouch: (() -> Unit)? = null,
    scrollPageMoveMultiplier: Float = 3.0f,
    useShortSwipeForActions: Boolean = false
): Modifier {
    val context = LocalContext.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { swipeThreshold.toPx() }.toInt()
    val velocityThreshold = 80 // Reduced from 100 for faster detection
    
    val lastPageMove = remember { androidx.compose.runtime.mutableStateOf(0L) }
    
    val currentOnAnyTouch = androidx.compose.runtime.rememberUpdatedState(onAnyTouch)
    val currentOnDoubleTap = androidx.compose.runtime.rememberUpdatedState(onDoubleTap)
    val currentOnSwipeLeft = androidx.compose.runtime.rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight = androidx.compose.runtime.rememberUpdatedState(onSwipeRight)
    val currentOnSwipeUp = androidx.compose.runtime.rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown = androidx.compose.runtime.rememberUpdatedState(onSwipeDown)
    val currentOnLongSwipeDown = androidx.compose.runtime.rememberUpdatedState(onLongSwipeDown)
    val currentOnVerticalPageMove = androidx.compose.runtime.rememberUpdatedState(onVerticalPageMove)
    val currentUseShortSwipeForActions = androidx.compose.runtime.rememberUpdatedState(useShortSwipeForActions)

    val currentPageMoveThresholdPx = androidx.compose.runtime.rememberUpdatedState(
        (thresholdPx * shortSwipeRatio * scrollPageMoveMultiplier).toInt().coerceAtLeast(8)
    )

    val gestureDetector = remember(thresholdPx, shortSwipeRatio, longSwipeRatio) {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            var lastPageMoveY = 0f
            var scrollPageMoveTriggered = false

            override fun onDown(e: MotionEvent): Boolean {
                lastPageMoveY = e.y
                scrollPageMoveTriggered = false
                try { currentOnAnyTouch.value?.invoke() } catch (_: Exception) {}
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false
                val callback = currentOnVerticalPageMove.value ?: return false

                if (currentOnSwipeUp.value != null || currentOnSwipeDown.value != null) return false

                val totalDiffX = e2.x - e1.x
                val totalDiffY = e2.y - e1.y

                // Vertical-dominant check
                if (abs(totalDiffX) > abs(totalDiffY)) return false

                val diffFromLastPageMove = e2.y - lastPageMoveY
                if (abs(diffFromLastPageMove) > currentPageMoveThresholdPx.value) {
                    val now = System.currentTimeMillis()
                    if (now - lastPageMove.value >= pageMoveCooldownMs) {
                        val delta = if (diffFromLastPageMove < 0) 1 else -1
                        try {
                            callback(delta)
                            lastPageMove.value = now
                            lastPageMoveY = e2.y
                            scrollPageMoveTriggered = true
                            return true
                        } catch (_: Exception) {}
                    }
                }
                return false
            }
            
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (System.currentTimeMillis() - GestureHelper.lastElementClickTime < 350) return false
                val exH = GestureHelper.exclusionHeight
                if (exH > 0f && e.y >= GestureHelper.exclusionY && e.y <= GestureHelper.exclusionY + exH) return false
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
                        val exH = GestureHelper.exclusionHeight
                        if (exH > 0f && event1.y >= GestureHelper.exclusionY && event1.y <= GestureHelper.exclusionY + exH) {
                            return false
                        }
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
                        val hasAnyLongSwipeCallbacks =
                            currentOnSwipeUp.value != null ||
                            currentOnSwipeDown.value != null ||
                            currentOnLongSwipeDown.value != null

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
                                    val downCallback = currentOnLongSwipeDown.value ?: currentOnSwipeDown.value
                                    downCallback?.invoke()
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            }
                            if (handled) return true
                        }

                        if (!scrollPageMoveTriggered) {
                            // Fallback for fast flicks not caught by onScroll
                            val pageMoveThresholdPx = if (hasAnyLongSwipeCallbacks) {
                                (thresholdPx * shortSwipeRatio * 0.7f).toInt().coerceAtLeast(8)
                            } else {
                                currentPageMoveThresholdPx.value
                            }
                            val pageMoveVelocityThreshold = (velocityThreshold * 0.3f).coerceAtLeast(1f)
                            val isVerticalSwipe = abs(diffY) > pageMoveThresholdPx && abs(velocityY) > pageMoveVelocityThreshold

                            if (isVerticalSwipe && !isLongSwipe) {
                                if (currentUseShortSwipeForActions.value) {
                                    val handled = if (diffY < 0) {
                                        try { currentOnSwipeUp.value?.invoke(); currentOnSwipeUp.value != null } catch (_: Exception) { false }
                                    } else {
                                        try { currentOnSwipeDown.value?.invoke(); currentOnSwipeDown.value != null } catch (_: Exception) { false }
                                    }
                                    if (handled) return true
                                }

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
            
            var multiTouchActive = false
            do {
                val event = awaitPointerEvent()
                val pressedCount = event.changes.count { it.pressed }

                if (pressedCount >= 2) {
                    if (!multiTouchActive) {
                        multiTouchActive = true
                        val first = event.changes.firstOrNull()
                        if (first != null) {
                            val cancelEvent = MotionEvent.obtain(
                                first.uptimeMillis,
                                first.uptimeMillis,
                                MotionEvent.ACTION_CANCEL,
                                first.position.x,
                                first.position.y,
                                0
                            )
                            gestureDetector.onTouchEvent(cancelEvent)
                            cancelEvent.recycle()
                        }
                    }
                    continue
                }
                if (multiTouchActive) continue

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

@Composable
fun Modifier.inkOsSafeDrawingPadding(): Modifier {
    val context = LocalContext.current
    val prefs = remember { com.github.gezimos.inkos.data.Prefs(context) }
    var insets = WindowInsets.safeDrawing
    if (!prefs.showStatusBar) insets = insets.exclude(WindowInsets.statusBars)
    if (!prefs.showNavigationBar) insets = insets.exclude(WindowInsets.navigationBars)
    insets = insets.exclude(WindowInsets.ime)
    return windowInsetsPadding(insets)
}
