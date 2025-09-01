package com.github.gezimos.inkos.helper.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.github.gezimos.inkos.data.Prefs
import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class EinkScrollBehavior(
    private val context: Context,
    private var touchThreshold: Float = 50f,     // Threshold to detect significant movement
    // Scroll by full page height by default
    private var timeThresholdMs: Long = 300,     // Minimum time between page turns (milliseconds)
    private val prefs: Prefs = Prefs(context),   // Add Prefs for vibration preference
    private val onPageChanged: ((pageIndex: Int, pageCount: Int) -> Unit)? = null // callback
) {
    companion object {
        private const val TAG = "EinkScrollBehavior"
    }
    private var lastY: Float = 0f
    private var startY: Float = 0f
    private var lastScrollTime: Long = 0         // Track time of last scroll action
    private var contentHeight: Int = 0
    private var viewportHeight: Int = 0
    private var hasScrolled: Boolean = false     // Track if scroll has occurred in this gesture
    private var lastReportedPage: Int = -1
    private var lastReportedCount: Int = -1
    private var activePointerId: Int = -1

    // Fixed vibrator initialization
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager =
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun attachToScrollView(scrollView: View) {
        when (scrollView) {
            is ScrollView -> attachToRegularScrollView(scrollView)
            is NestedScrollView -> attachToNestedScrollView(scrollView)
        }
    }

    private fun attachToRegularScrollView(scrollView: ScrollView) {
        scrollView.isSmoothScrollingEnabled = false
        setupScrollView(scrollView)
    }

    private fun attachToNestedScrollView(scrollView: NestedScrollView) {
        scrollView.isSmoothScrollingEnabled = false
        setupScrollView(scrollView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupScrollView(view: View) {
        // Wait for layout to calculate dimensions
        view.post {
            updateDimensions(view)
            // Report initial page state
            reportPageForScroll(view.scrollY)
        }

        // Also listen for layout changes (fonts, dynamic content)
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateDimensions(view)
            reportPageForScroll(view.scrollY)
        }

    view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
            // Track primary pointer and reset gesture state
            activePointerId = event.getPointerId(0)
            startY = event.y
            lastY = startY
            hasScrolled = false // Reset scroll state for new gesture
            true
                }

                MotionEvent.ACTION_MOVE -> {
            // Ignore multi-touch gestures
            if (event.pointerCount > 1) return@setOnTouchListener true
            if (hasScrolled) return@setOnTouchListener true // Only allow one scroll per gesture
                    val deltaY = lastY - event.y
                    val currentTime = System.currentTimeMillis()

                    // Only handle significant movements and respect time threshold
                    if (abs(deltaY) > touchThreshold && (currentTime - lastScrollTime > timeThresholdMs)) {
                        // Update dimensions just before calculating scroll
                        updateDimensions(view)

                        // Get current scroll position
                        val currentScroll = view.scrollY

                        // Calculate overlap (20% of viewport height)
                        val overlap = (viewportHeight * 0.2).toInt()

                        val maxScroll = max(0, contentHeight - viewportHeight)
                        val step = max(1, viewportHeight - overlap)

                        // Compute page counts
                        val pages = if (maxScroll <= 0) 1 else (1 + ((maxScroll + step - 1) / step))

                        // Deterministic page index calculation using nearest page start
                        fun pageIndexForScroll(scrollPos: Int): Int {
                            if (maxScroll <= 0) return 0
                            var bestIndex = 0
                            var bestDist = abs(scrollPos - min(0 * step, maxScroll))
                            for (i in 1 until pages) {
                                val start = min(i * step, maxScroll)
                                val dist = abs(scrollPos - start)
                                if (dist < bestDist) {
                                    bestIndex = i
                                    bestDist = dist
                                }
                            }
                            return bestIndex
                        }

                        val currentPageIdx = pageIndexForScroll(currentScroll)
                        // Use lastReportedPage as authoritative when available to avoid skipping
                        val basePageIdx = if (lastReportedPage in 0 until pages) lastReportedPage else currentPageIdx

            if (deltaY > 0) {
                            // Scroll down one page at a time (by page index), never skip intermediate pages
                            if (currentScroll < maxScroll) {
                val nextPage = (basePageIdx + 1).coerceAtMost(pages - 1)
                val nextScrollStart = min(nextPage * step, maxScroll)
                val nextScroll = nextScrollStart
                Log.d(TAG, "touch -> down: scrollY=$currentScroll, deltaY=$deltaY, basePage=$basePageIdx, computedPage=$currentPageIdx, nextPage=$nextPage, targetY=$nextScroll, pages=$pages, step=$step, maxScroll=$maxScroll")
                scrollToPosition(view, nextScroll)
                                performHapticFeedback()
                                lastScrollTime = currentTime
                                hasScrolled = true
                            }
                        } else if (deltaY < 0) {
                            // Scroll up one page at a time
                            if (currentScroll > 0) {
                val prevPage = (basePageIdx - 1).coerceAtLeast(0)
                val prevScroll = min(prevPage * step, maxScroll)
                Log.d(TAG, "touch -> up: scrollY=$currentScroll, deltaY=$deltaY, basePage=$basePageIdx, computedPage=$currentPageIdx, prevPage=$prevPage, targetY=$prevScroll, pages=$pages, step=$step, maxScroll=$maxScroll")
                scrollToPosition(view, prevScroll)
                                performHapticFeedback()
                                lastScrollTime = currentTime
                                hasScrolled = true
                            }
                        }
                        lastY = event.y
                    }
                    true
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    // If the primary pointer went up, reset to avoid stuck state
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == activePointerId) {
                        activePointerId = -1
                        hasScrolled = false
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hasScrolled = false // Reset for next gesture
                    activePointerId = -1
                    true
                }

                else -> false
            }
        }
    }

    private fun reportPageForScroll(scrollY: Int) {
        if (viewportHeight <= 0) return
        val overlap = (viewportHeight * 0.2).toInt()
        val step = max(1, viewportHeight - overlap)
        val maxScroll = max(0, contentHeight - viewportHeight)
        // Number of pages: the first page + how many full steps fit into maxScroll (ceil)
        val pages = if (maxScroll <= 0) 1 else (1 + ((maxScroll + step - 1) / step))

        // Build page starts and pick the nearest start to determine the page
        var page = 0
        if (maxScroll <= 0) {
            page = 0
        } else {
            var bestIndex = 0
            var bestDist = abs(scrollY - min(0 * step, maxScroll))
            for (i in 1 until pages) {
                val start = min(i * step, maxScroll)
                val dist = abs(scrollY - start)
                if (dist < bestDist) {
                    bestIndex = i
                    bestDist = dist
                }
            }
            page = bestIndex.coerceIn(0, pages - 1)
        }
        if (page != lastReportedPage || pages != lastReportedCount) {
            lastReportedPage = page
            lastReportedCount = pages
            Log.d(TAG, "report -> scrollY=$scrollY, page=$page, pages=$pages, step=$step, maxScroll=$maxScroll")
            onPageChanged?.invoke(page, pages)
        }
    }

    fun reset() {
        hasScrolled = false
        lastScrollTime = 0
    }

    private fun updateDimensions(view: View) {
        contentHeight = when (view) {
            is ScrollView -> view.getChildAt(0)?.height ?: 0
            is NestedScrollView -> view.getChildAt(0)?.height ?: 0
            else -> 0
        }
        viewportHeight = view.height
    }

    private fun scrollToPosition(view: View, targetY: Int) {
        // Ensure we don't scroll outside content bounds
        val boundedTargetY = when (view) {
            is ScrollView -> {
                val maxScroll = getMaxScrollY(view)
                targetY.coerceIn(0, maxScroll)
            }

            is NestedScrollView -> {
                val maxScroll = getMaxScrollY(view)
                targetY.coerceIn(0, maxScroll)
            }

            else -> targetY
        }

        // Apply the scroll without any animation (e-ink: instant jumps)
        when (view) {
            is ScrollView -> view.scrollTo(0, boundedTargetY)
            is NestedScrollView -> view.scrollTo(0, boundedTargetY)
        }

        // Report new page state after the scroll
        reportPageForScroll(boundedTargetY)
    }

    private fun getMaxScrollY(view: View): Int {
        updateDimensions(view)
        return max(0, contentHeight - viewportHeight)
    }

    private fun performHapticFeedback() {
        if (!prefs.useVibrationForPaging) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context.checkSelfPermission(Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            50,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (_: Exception) {
            // Silently handle any vibration-related errors
        }
    }
}