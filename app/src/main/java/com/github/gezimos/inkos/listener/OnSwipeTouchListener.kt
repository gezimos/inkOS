package com.github.gezimos.inkos.listener

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.github.gezimos.inkos.data.Constants
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.math.abs

/*
Swipe, double tap and long press touch listener for a view
Source: https://www.tutorialspoint.com/how-to-handle-swipe-gestures-in-kotlin
*/

internal open class OnSwipeTouchListener(c: Context?) : OnTouchListener {
    private var longPressOn = false
    private var doubleTapOn = false
    private val gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_UP)
            longPressOn = false
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        private val swipeThreshold: Int = 100
        private val swipeVelocityThreshold: Int = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (doubleTapOn) {
                doubleTapOn = false
                onTripleClick()
            }
            return super.onSingleTapUp(e)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            doubleTapOn = true
            // Ensure delayed callbacks run on the main thread (Timer runs background thread)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (doubleTapOn) {
                    doubleTapOn = false
                    onDoubleClick()
                }
            }, Constants.TRIPLE_TAP_DELAY_MS.toLong())
            return super.onDoubleTap(e)
        }

        override fun onLongPress(e: MotionEvent) {
            longPressOn = true
            // Run long-press timeout on main thread to allow safe UI interactions
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (longPressOn) onLongClick()
            }, Constants.LONG_PRESS_DELAY_MS.toLong())
            super.onLongPress(e)
        }

        override fun onFling(
            event1: MotionEvent?,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            try {
                val diffY = event2.y - event1!!.y
                val diffX = event2.x - event1.x
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                        if (diffX > 0) onSwipeRight() else onSwipeLeft()
                    }
                } else {
                    if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                        if (diffY < 0) onSwipeUp() else onSwipeDown()
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return false
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
    open fun onLongClick() {}
    open fun onDoubleClick() {}
    open fun onTripleClick() {}
    open fun onLongSwipe(direction: String) {}

    init {
        gestureDetector = GestureDetector(c, GestureListener())
    }
}

