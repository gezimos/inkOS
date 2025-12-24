package com.github.gezimos.inkos.ui.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.view.WindowManager
import android.view.View
import android.widget.FrameLayout
import android.graphics.Rect
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import com.github.gezimos.inkos.data.Prefs
import android.content.res.Configuration
import android.view.KeyEvent
import android.graphics.Color
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * A BottomSheetDialog variant that can be locked (non-draggable) and
 * supports an optional key event listener for aggressive key capture.
 */
class LockedBottomSheetDialog(context: Context, theme: Int = 0) : BottomSheetDialog(context, theme) {
    private var locked: Boolean = false
    var keyEventListener: ((KeyEvent) -> Boolean)? = null
    private var imeLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    fun setLocked(lock: Boolean) {
        locked = lock
    }

    override fun onStart() {
        super.onStart()
        try {
            val behaviorField = BottomSheetBehavior.from(window!!.decorView.findViewById(com.google.android.material.R.id.design_bottom_sheet))
            behaviorField.isDraggable = !locked
        } catch (_: Exception) {
            // ignore - best-effort
        }
        // Replace the usual single-color dim scrim with a tiled drawable pattern (best-effort).
        try {
            // Build a checkerboard tile with ~10px squares (4x4 squares per tile)
            val tileSize = 16
            val square = 4
            val bmp = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Detect night mode and invert pattern: in dark mode show light squares, in light mode show dark squares
            val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isNight = uiMode == Configuration.UI_MODE_NIGHT_YES
            // Use theme colors for the scrim pattern so dialog visuals follow app theme
            val (resolvedTextColor, resolvedBgColor) = try {
                com.github.gezimos.inkos.style.resolveThemeColors(context)
            } catch (_: Exception) {
                // Fall back to Prefs-stored colors so we don't hardcode black/white here
                val p = try { Prefs(context) } catch (_: Exception) { null }
                Pair(p?.textColor ?: android.graphics.Color.BLACK, p?.backgroundColor ?: android.graphics.Color.WHITE)
            }

            val darkPaint = if (isNight) {
                Paint().apply { color = Color.TRANSPARENT }
            } else {
                val r = Color.red(resolvedTextColor)
                val g = Color.green(resolvedTextColor)
                val b = Color.blue(resolvedTextColor)
                Paint().apply { color = Color.argb(140, r, g, b) }
            }

            val lightPaint = if (isNight) {
                val r = Color.red(resolvedBgColor)
                val g = Color.green(resolvedBgColor)
                val b = Color.blue(resolvedBgColor)
                Paint().apply { color = Color.argb(140, r, g, b) }
            } else {
                Paint().apply { color = Color.TRANSPARENT }
            }

            var y = 0
            var row = 0
            while (y < tileSize) {
                var x = 0
                var col = 0
                while (x < tileSize) {
                    val useDark = (row + col) % 2 == 0
                    val paint = if (useDark) darkPaint else lightPaint
                    canvas.drawRect(x.toFloat(), y.toFloat(), (x + square).toFloat(), (y + square).toFloat(), paint)
                    x += square
                    col++
                }
                y += square
                row++
            }

            val drawable = BitmapDrawable(context.resources, bmp).apply {
                setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            }

            // Apply as the window background so it fills the scrim area
            window?.setBackgroundDrawable(drawable)
            // Ensure we preserve the app's status bar hidden state when a dialog opens.
            // Dialog windows have their own system UI state and can make the status bar reappear.
            try {
                val prefs = try { Prefs(context) } catch (_: Exception) { null }
                val shouldShowStatusBar = prefs?.showStatusBar ?: true
                if (!shouldShowStatusBar) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        // Hide status bars on the dialog window
                        window?.insetsController?.hide(android.view.WindowInsets.Type.statusBars())
                    } else {
                        @Suppress("DEPRECATION")
                        window?.decorView?.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
                    }
                }
            } catch (_: Exception) {}
            // Keep a subtle dim beneath pattern by enabling dim behind with a low amount
            try {
                val attrs = window?.attributes
                if (attrs != null) {
                    // Make the background dim around 10% as requested
                    attrs.dimAmount = 0.20f
                    window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    window?.attributes = attrs
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {
            // best-effort; if anything fails, fallback to default behavior
        }

        // Ask the window to resize when IME is shown so the bottom sheet can move above the keyboard.
        try {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        } catch (_: Exception) {}

        // Add a lightweight layout listener to expand the bottom sheet when the IME appears.
        try {
            val decor = window?.decorView ?: return
            imeLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                try {
                    val r = Rect()
                    decor.getWindowVisibleDisplayFrame(r)
                    val screenHeight = decor.rootView.height
                    val keypadHeight = screenHeight - r.bottom
                    val visibleRatio = if (screenHeight > 0) keypadHeight.toFloat() / screenHeight else 0f

                    // If IME covers more than ~15% of the screen, treat keyboard as visible.
                    if (visibleRatio > 0.15f) {
                        val sheet = decor.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                        if (sheet != null) {
                            try {
                                val behavior = BottomSheetBehavior.from(sheet)
                                // Try expanding the sheet so its action buttons remain visible above IME.
                                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                                // Also increase peek height slightly to keep sheet above keyboard if needed.
                                behavior.peekHeight = r.bottom
                            } catch (_: Exception) {
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
            decor.viewTreeObserver.addOnGlobalLayoutListener(imeLayoutListener)
        } catch (_: Exception) {}
    }

    override fun onDetachedFromWindow() {
        try {
            val decor = window?.decorView
            if (imeLayoutListener != null && decor != null) {
                decor.viewTreeObserver.removeOnGlobalLayoutListener(imeLayoutListener)
                imeLayoutListener = null
            }
        } catch (_: Exception) {}
        super.onDetachedFromWindow()
    }

    private fun hideKeyboard() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val token = window?.decorView?.windowToken
            if (imm != null && token != null) {
                imm.hideSoftInputFromWindow(token, 0)
            } else {
                // fallback: try current focus
                val view: View? = try { currentFocus } catch (_: Exception) { null }
                val vt = view?.windowToken ?: window?.decorView?.windowToken
                if (imm != null && vt != null) imm.hideSoftInputFromWindow(vt, 0)
            }
        } catch (_: Exception) {}
    }

    override fun dismiss() {
        hideKeyboard()
        super.dismiss()
    }

    override fun cancel() {
        hideKeyboard()
        super.cancel()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Let caller intercept keys (volume, back, etc.) first
        try {
            val handled = keyEventListener?.invoke(event) ?: false
            if (handled) return true
        } catch (_: Exception) {
        }
        return super.dispatchKeyEvent(event)
    }
}
