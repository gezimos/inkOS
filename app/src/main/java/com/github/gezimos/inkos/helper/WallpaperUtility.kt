package com.github.gezimos.inkos.helper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.github.gezimos.inkos.data.Prefs
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.github.gezimos.inkos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperUtility(private val context: Context) {

    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    companion object {
        val PRESET_WALLPAPERS = listOf(
            PresetWallpaper("White", -1),
            PresetWallpaper("Black", -2),
            PresetWallpaper("Japan", R.drawable.japan_left),
            PresetWallpaper("Japan 2", R.drawable.japan2),
            PresetWallpaper("NYC", R.drawable.nyc_left),
            PresetWallpaper("SF", R.drawable.sf_left),
            PresetWallpaper("Trees", R.drawable.trees_left),
            PresetWallpaper("Bamboo", R.drawable.bamboo),
            PresetWallpaper("Blossom", R.drawable.blossom3),
            PresetWallpaper("Building", R.drawable.building),
            PresetWallpaper("Forrest", R.drawable.forrest),
            PresetWallpaper("Leaf", R.drawable.leaf),
            PresetWallpaper("Liberty", R.drawable.liberty),
            PresetWallpaper("Urban", R.drawable.urban),
            PresetWallpaper("Halfdots", R.drawable.halfdots),
            PresetWallpaper("Halfdots Two", R.drawable.halfdots_two),
            PresetWallpaper("Dots", DOTS_LEFT, intArrayOf(DOTS_LEFT, DOTS_CENTER, DOTS_RIGHT)),
            PresetWallpaper("Lines", LINES_LEFT, intArrayOf(LINES_LEFT, LINES_CENTER, LINES_RIGHT)),
            PresetWallpaper("Grid", GRID_LEFT, intArrayOf(GRID_LEFT, GRID_CENTER, GRID_RIGHT)),
            PresetWallpaper("Diagonal", DIAG_LEFT, intArrayOf(DIAG_LEFT, DIAG_CENTER, DIAG_RIGHT)),
            PresetWallpaper("Circles", CIRCLES_BL, intArrayOf(CIRCLES_BL, CIRCLES_BR)),
            PresetWallpaper("Crosshatch", CROSS_LEFT, intArrayOf(CROSS_LEFT, CROSS_CENTER, CROSS_RIGHT)),
            PresetWallpaper("Waves", WAVE_LEFT, intArrayOf(WAVE_LEFT, WAVE_CENTER, WAVE_RIGHT)),
            PresetWallpaper("Plus", PLUS_LEFT, intArrayOf(PLUS_LEFT, PLUS_CENTER, PLUS_RIGHT)),
            PresetWallpaper("Matrix", SCATTER_LEFT, intArrayOf(SCATTER_LEFT, SCATTER_CENTER, SCATTER_RIGHT)),
            PresetWallpaper("Binary", DASH_LEFT, intArrayOf(DASH_LEFT, DASH_CENTER, DASH_RIGHT)),
        )

        const val DOTS_LEFT = -3
        const val DOTS_RIGHT = -4
        const val DOTS_CENTER = -5
        const val LINES_LEFT = -6
        const val LINES_RIGHT = -7
        const val LINES_CENTER = -8
        const val GRID_LEFT = -9
        const val GRID_RIGHT = -10
        const val GRID_CENTER = -11
        const val DIAG_LEFT = -12
        const val DIAG_RIGHT = -13
        const val DIAG_CENTER = -14
        const val CIRCLES_BL = -15
        const val CIRCLES_BR = -16
        const val CROSS_LEFT = -17
        const val CROSS_RIGHT = -18
        const val CROSS_CENTER = -19
        const val WAVE_LEFT = -20
        const val WAVE_RIGHT = -21
        const val WAVE_CENTER = -22
        const val PLUS_LEFT = -23
        const val PLUS_RIGHT = -24
        const val PLUS_CENTER = -25
        const val DASH_LEFT = -26
        const val DASH_RIGHT = -27
        const val DASH_CENTER = -28
        const val SCATTER_LEFT = -29
        const val SCATTER_RIGHT = -30
        const val SCATTER_CENTER = -31
        private const val GENERATED_MIN = -31
    }

    data class PresetWallpaper(
        val name: String,
        val resourceId: Int,
        val variants: IntArray? = null
    ) {
        val isGenerated get() = variants != null
    }
    suspend fun setWallpaperFromBitmap(bitmap: Bitmap, flags: Int = WallpaperManager.FLAG_SYSTEM): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)

            val screenSizedBitmap = createFittedBitmap(bitmap, screenWidth, screenHeight)

            if (flags == (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)) {
                try {
                    wallpaperManager.setBitmap(screenSizedBitmap, null, false, WallpaperManager.FLAG_SYSTEM)
                } catch (e: Exception) {
                    android.util.Log.e("WallpaperUtility", "Failed to set home screen wallpaper", e)
                }
                try {
                    wallpaperManager.setBitmap(screenSizedBitmap, null, false, WallpaperManager.FLAG_LOCK)
                } catch (e: Exception) {
                    android.util.Log.e("WallpaperUtility", "Failed to set lock screen wallpaper", e)
                }
            } else {
                wallpaperManager.setBitmap(screenSizedBitmap, null, false, flags)
            }

            screenSizedBitmap.recycle()

            true
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set wallpaper", e)
            e.printStackTrace()
            false
        }
    }
    suspend fun setSolidColorWallpaper(color: Int, flags: Int = WallpaperManager.FLAG_SYSTEM): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(color)
            setWallpaperFromBitmap(bitmap, flags)
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set solid color wallpaper", e)
            false
        }
    }
    suspend fun setWallpaperFromResource(
        resourceId: Int,
        flags: Int = WallpaperManager.FLAG_SYSTEM
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmapFromResource(resourceId)
            if (bitmap != null) {
                val result = setWallpaperFromBitmap(bitmap, flags)
                bitmap.recycle()
                result
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set wallpaper from resource", e)
            false
        }
    }
    suspend fun setWallpaperFromUri(
        uri: Uri,
        flags: Int = WallpaperManager.FLAG_SYSTEM
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmapFromUri(uri)
            if (bitmap != null) {
                // Get EXIF orientation and apply
                var oriented = bitmap
                try {
                    val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    if (parcelFileDescriptor != null) {
                        val exif = ExifInterface(parcelFileDescriptor.fileDescriptor)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        oriented = applyOrientation(bitmap, orientation)
                        if (oriented != bitmap) bitmap.recycle()
                        parcelFileDescriptor.close()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("WallpaperUtility", "Could not read EXIF orientation", e)
                }

                val result = setWallpaperFromBitmap(oriented, flags)
                oriented.recycle()
                result
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set wallpaper from URI", e)
            false
        }
    }
    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return bitmap
        }

        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        return try {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("WallpaperUtility", "Out of memory when applying orientation", e)
            bitmap
        }
    }
    fun loadBitmapFromResource(resourceId: Int): Bitmap? {
        return try {
            if (resourceId == -1) {
                // White wallpaper
                return Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
            } else if (resourceId == -2) {
                // Black wallpaper
                return Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
            } else if (resourceId <= -3 && resourceId >= GENERATED_MIN) {
                return generatePattern(resourceId)
            }

            // First get the dimensions without loading
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Now load at original size, ignoring density
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inScaled = false
            options.inDensity = 0  // Ignore density
            options.inTargetDensity = 0  // Ignore target density

            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            android.util.Log.d("WallpaperUtility", "Loaded resource bitmap: ${bitmap?.width}x${bitmap?.height}")
            // Recolor B&W image wallpapers with theme colors
            if (bitmap != null) recolorBitmap(bitmap) else null
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to load bitmap from resource", e)
            null
        }
    }
    private fun recolorBitmap(source: Bitmap): Bitmap {
        val prefs = Prefs(context)
        val fgColor = prefs.textColor
        val bgColor = prefs.backgroundColor

        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val fgR = Color.red(fgColor); val fgG = Color.green(fgColor); val fgB = Color.blue(fgColor)
        val bgR = Color.red(bgColor); val bgG = Color.green(bgColor); val bgB = Color.blue(bgColor)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = Color.alpha(pixel)
            if (a == 0) continue

            val r = Color.red(pixel); val g = Color.green(pixel); val b = Color.blue(pixel)
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

            val newR = (fgR + (bgR - fgR) * lum).toInt().coerceIn(0, 255)
            val newG = (fgG + (bgG - fgG) * lum).toInt().coerceIn(0, 255)
            val newB = (fgB + (bgB - fgB) * lum).toInt().coerceIn(0, 255)

            pixels[i] = Color.argb(a, newR, newG, newB)
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        source.recycle()
        return result
    }

    /** Compute fade alpha for left/right/center variants. */
    private fun fadeAlpha(fraction: Float, fadeType: Int): Float {
        val raw = when (fadeType) {
            0 -> 1f - fraction   // dense left, clear right
            1 -> fraction        // clear left, dense right
            2 -> kotlin.math.abs(fraction - 0.5f) * 2f // dense edges, clear center
            else -> 1f
        }
        return raw * raw // quadratic curve — wider visible zone
    }

    /** Map a generated wallpaper ID to its fade type: 0=left, 1=right, 2=center. */
    private fun fadeTypeOf(id: Int): Int = when (id) {
        DOTS_LEFT, LINES_LEFT, GRID_LEFT, DIAG_LEFT, CROSS_LEFT, WAVE_LEFT, PLUS_LEFT, DASH_LEFT, SCATTER_LEFT -> 0
        DOTS_RIGHT, LINES_RIGHT, GRID_RIGHT, DIAG_RIGHT, CROSS_RIGHT, WAVE_RIGHT, PLUS_RIGHT, DASH_RIGHT, SCATTER_RIGHT -> 1
        DOTS_CENTER, LINES_CENTER, GRID_CENTER, DIAG_CENTER, CROSS_CENTER, WAVE_CENTER, PLUS_CENTER, DASH_CENTER, SCATTER_CENTER -> 2
        CIRCLES_BL -> 3
        CIRCLES_BR -> 4
        else -> 0
    }

    /** Generate a pattern wallpaper bitmap using current theme colors. */
    /** Public accessor for theme presets to generate a wallpaper by pattern variant ID. */
    fun loadGeneratedWallpaper(variant: Int): Bitmap = generatePattern(variant)

    /** Generate with custom colors and optional font (for theme preset previews). */
    fun loadGeneratedWallpaper(variant: Int, bgColor: Int, fgColor: Int, font: android.graphics.Typeface? = null): Bitmap =
        generatePattern(variant, bgColor, fgColor, font)

    private fun generatePattern(variant: Int, overrideBg: Int? = null, overrideFg: Int? = null, overrideFont: android.graphics.Typeface? = null): Bitmap {
        val prefs = Prefs(context)
        val bgColor = overrideBg ?: prefs.backgroundColor
        val fgColor = overrideFg ?: prefs.textColor
        val density = displayMetrics.density
        val appFont: android.graphics.Typeface = overrideFont ?: try {
            val customPath = prefs.getCustomFontPathForContext("apps")
            prefs.appsFont.getFont(context, customPath) ?: android.graphics.Typeface.MONOSPACE
        } catch (_: Exception) { android.graphics.Typeface.MONOSPACE }

        val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(bgColor)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            style = Paint.Style.FILL
        }
        val fadeType = fadeTypeOf(variant)

        when (variant) {
            DOTS_LEFT, DOTS_RIGHT, DOTS_CENTER -> drawDots(canvas, paint, fadeType)
            LINES_LEFT, LINES_RIGHT, LINES_CENTER -> drawLines(canvas, paint, fadeType, density)
            GRID_LEFT, GRID_RIGHT, GRID_CENTER -> drawGrid(canvas, paint, fadeType, density)
            DIAG_LEFT, DIAG_RIGHT, DIAG_CENTER -> drawDiagonal(canvas, paint, fadeType, density)

            CIRCLES_BL, CIRCLES_BR -> drawCircles(canvas, paint, variant, density)
            CROSS_LEFT, CROSS_RIGHT, CROSS_CENTER -> drawCrosshatch(canvas, paint, fadeType, density)
            WAVE_LEFT, WAVE_RIGHT, WAVE_CENTER -> drawWaves(canvas, paint, fadeType, density)
            PLUS_LEFT, PLUS_RIGHT, PLUS_CENTER -> drawPlus(canvas, paint, fadeType, density)
            DASH_LEFT, DASH_RIGHT, DASH_CENTER -> drawBinary(canvas, paint, fadeType, density, appFont)
            SCATTER_LEFT, SCATTER_RIGHT, SCATTER_CENTER -> drawScatter(canvas, paint, fadeType, density, appFont)
        }
        return bitmap
    }

    private fun drawDots(canvas: Canvas, paint: Paint, fadeType: Int) {
        val density = displayMetrics.density
        val spacing = (24 * density).toInt()
        val dotRadius = 1.5f * density
        var y = spacing / 2
        while (y < screenHeight) {
            var x = spacing / 2
            while (x < screenWidth) {
                val alpha = fadeAlpha(x.toFloat() / screenWidth, fadeType)
                if (alpha > 0.02f) {
                    paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(x.toFloat(), y.toFloat(), dotRadius, paint)
                }
                x += spacing
            }
            y += spacing
        }
    }

    private fun drawLines(canvas: Canvas, paint: Paint, fadeType: Int, density: Float) {
        val spacing = (20 * density).toInt()
        val strokeWidth = 0.75f * density
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        var y = spacing / 2
        while (y < screenHeight) {
            // Draw line in segments for per-segment alpha
            val segW = (8 * density).toInt()
            var x = 0
            while (x < screenWidth) {
                val mid = (x + segW / 2).toFloat() / screenWidth
                val alpha = fadeAlpha(mid, fadeType)
                if (alpha > 0.02f) {
                    paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(x.toFloat(), y.toFloat(), (x + segW).toFloat().coerceAtMost(screenWidth.toFloat()), y.toFloat(), paint)
                }
                x += segW
            }
            y += spacing
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawGrid(canvas: Canvas, paint: Paint, fadeType: Int, density: Float) {
        val targetSpacing = 24f * density
        val cols = kotlin.math.round(screenWidth / targetSpacing).toInt().coerceAtLeast(1)
        val rows = kotlin.math.round(screenHeight / targetSpacing).toInt().coerceAtLeast(1)
        val spacingX = screenWidth.toFloat() / cols
        val spacingY = screenHeight.toFloat() / rows

        val strokeWidth = 0.5f * density
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        val segLen = 8f * density

        for (i in 0..rows) {
            val y = i * spacingY
            var x = 0f
            while (x < screenWidth) {
                val mid = (x + segLen / 2) / screenWidth
                val alpha = fadeAlpha(mid, fadeType)
                if (alpha > 0.02f) {
                    paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(x, y, (x + segLen).coerceAtMost(screenWidth.toFloat()), y, paint)
                }
                x += segLen
            }
        }
        // Vertical lines (including left and right edges)
        for (i in 0..cols) {
            val x = i * spacingX
            val fraction = (x / screenWidth).coerceIn(0f, 1f)
            val alpha = fadeAlpha(fraction, fadeType)
            if (alpha > 0.02f) {
                paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                var y = 0f
                while (y < screenHeight) {
                    canvas.drawLine(x, y, x, (y + segLen).coerceAtMost(screenHeight.toFloat()), paint)
                    y += segLen
                }
            }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawDiagonal(canvas: Canvas, paint: Paint, fadeType: Int, density: Float) {
        val spacing = (18 * density).toInt()
        val strokeWidth = 0.75f * density
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        // 45-degree lines from top-left to bottom-right
        val total = screenWidth + screenHeight
        var offset = -screenHeight
        while (offset < total) {
            val x1 = offset.toFloat()
            val x2 = (offset + screenHeight).toFloat()
            // Draw in segments for fade
            val steps = 40
            val dy = screenHeight.toFloat() / steps
            for (i in 0 until steps) {
                val sy = i * dy
                val ey = (i + 1) * dy
                val sx = x1 + (x2 - x1) * (sy / screenHeight)
                val ex = x1 + (x2 - x1) * (ey / screenHeight)
                val midX = ((sx + ex) / 2).coerceIn(0f, screenWidth.toFloat())
                val alpha = fadeAlpha(midX / screenWidth, fadeType)
                if (alpha > 0.02f) {
                    paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(sx, sy, ex, ey, paint)
                }
            }
            offset += spacing
        }
        paint.style = Paint.Style.FILL
    }


    private fun drawCircles(canvas: Canvas, paint: Paint, variant: Int, density: Float) {
        val strokeWidth = 0.75f * density
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        val spacing = (28 * density)
        // Origin: bottom-left or bottom-right
        val cx = if (variant == CIRCLES_BL) 0f else screenWidth.toFloat()
        val cy = screenHeight.toFloat()
        val maxRadius = kotlin.math.sqrt((screenWidth * screenWidth + screenHeight * screenHeight).toFloat())
        var r = spacing
        while (r < maxRadius) {
            val distFraction = (r / maxRadius)
            val alpha = (1f - distFraction) * (1f - distFraction)
            if (alpha > 0.02f) {
                paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(cx, cy, r, paint)
            }
            r += spacing
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawCrosshatch(canvas: Canvas, paint: Paint, fadeType: Int, density: Float) {
        val spacing = (22 * density).toInt()
        val strokeWidth = 0.5f * density
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        val total = screenWidth + screenHeight
        // 45-degree lines (top-left to bottom-right)
        for (dir in 0..1) {
            var offset = -screenHeight
            while (offset < total) {
                val steps = 40
                val dy = screenHeight.toFloat() / steps
                for (i in 0 until steps) {
                    val sy = i * dy
                    val ey = (i + 1) * dy
                    val sx: Float
                    val ex: Float
                    if (dir == 0) {
                        sx = offset + sy
                        ex = offset + ey
                    } else {
                        sx = screenWidth - offset - sy
                        ex = screenWidth - offset - ey
                    }
                    val midX = ((sx + ex) / 2).coerceIn(0f, screenWidth.toFloat())
                    val alpha = fadeAlpha(midX / screenWidth, fadeType)
                    if (alpha > 0.02f) {
                        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                        canvas.drawLine(sx, sy, ex, ey, paint)
                    }
                }
                offset += spacing
            }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawWaves(canvas: Canvas, paint: Paint, fadeType: Int, density: Float) {
        val spacing = (22 * density)
        val amplitude = (6 * density)
        val strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        val step = (2 * density)
        var baseY = spacing / 2
        while (baseY < screenHeight + spacing) {
            var x = step
            var prevX = 0f
            var prevY = baseY
            while (x < screenWidth) {
                val sy = baseY + amplitude * kotlin.math.sin((x / screenWidth) * Math.PI * 8).toFloat()
                val mid = x / screenWidth
                val alpha = fadeAlpha(mid, fadeType)
                if (alpha > 0.02f) {
                    paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(prevX, prevY, x, sy, paint)
                }
                prevX = x; prevY = sy
                x += step
            }
            baseY += spacing
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawPlus(canvas: Canvas, paint: Paint, fadeType: Int, density: Float) {
        val spacing = (28 * density).toInt()
        val armLen = 6f * density
        val strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        var y = spacing / 2
        while (y < screenHeight) {
            var x = spacing / 2
            while (x < screenWidth) {
                val alpha = fadeAlpha(x.toFloat() / screenWidth, fadeType)
                if (alpha > 0.02f) {
                    paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
                    canvas.drawLine(x - armLen, y.toFloat(), x + armLen, y.toFloat(), paint)
                    canvas.drawLine(x.toFloat(), y - armLen, x.toFloat(), y + armLen, paint)
                }
                x += spacing
            }
            y += spacing
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawBinary(canvas: Canvas, paint: Paint, fadeType: Int, density: Float, font: android.graphics.Typeface = android.graphics.Typeface.MONOSPACE) {
        val random = java.util.Random(99)
        val sizes = floatArrayOf(8f, 12f, 16f, 22f) // different text sizes in dp
        val colCount = screenWidth / (18 * density).toInt()
        val colWidth = screenWidth.toFloat() / colCount

        for (col in 0 until colCount) {
            val x = col * colWidth + colWidth * 0.3f
            val sizeIdx = random.nextInt(sizes.size)
            val charSize = sizes[sizeIdx] * density
            val rowH = charSize * 1.3f
            val colDensity = 0.3f + random.nextFloat() * 0.5f

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = paint.color
                textSize = charSize
                typeface = font
            }

            val colOffset = random.nextFloat() * rowH * 3
            var y = colOffset
            while (y < screenHeight + rowH) {
                if (random.nextFloat() < colDensity) {
                    val alpha = fadeAlpha(x / screenWidth, fadeType)
                    val vertFade = 1f - (y / screenHeight) * 0.4f
                    val finalAlpha = alpha * vertFade
                    if (finalAlpha > 0.02f) {
                        textPaint.alpha = (finalAlpha * 255).toInt().coerceIn(0, 255)
                        canvas.drawText(if (random.nextBoolean()) "1" else "0", x, y, textPaint)
                    }
                }
                y += rowH
            }
        }
    }

    private fun drawScatter(canvas: Canvas, paint: Paint, fadeType: Int, density: Float, font: android.graphics.Typeface = android.graphics.Typeface.MONOSPACE) {
        // Matrix-style falling characters
        val charSize = 10f * density
        val colSpacing = (14 * density).toInt()
        val rowSpacing = (14 * density).toInt()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paint.color
            textSize = charSize
            typeface = font
        }
        val chars = "01234567890ABCDEF"
        val random = java.util.Random(42)

        var col = 0
        var x = colSpacing / 2
        while (x < screenWidth) {
            val colOffset = random.nextInt(rowSpacing)
            val colDensity = 0.4f + random.nextFloat() * 0.6f // 40-100% of rows filled
            var y = colOffset
            while (y < screenHeight) {
                if (random.nextFloat() < colDensity) {
                    val alpha = fadeAlpha(x.toFloat() / screenWidth, fadeType)
                    // Vertical fade: characters fade out toward bottom
                    val vertFade = 1f - (y.toFloat() / screenHeight) * 0.5f
                    val finalAlpha = alpha * vertFade
                    if (finalAlpha > 0.02f) {
                        textPaint.alpha = (finalAlpha * 255).toInt().coerceIn(0, 255)
                        val ch = chars[random.nextInt(chars.length)]
                        canvas.drawText(ch.toString(), x.toFloat(), y.toFloat(), textPaint)
                    }
                }
                y += rowSpacing
            }
            x += colSpacing
            col++
        }
    }
    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            return bitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to load bitmap from URI", e)
            return null
        }
    }

    fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to load bitmap from path", e)
            null
        }
    }
    fun saveBitmapToInternalStorage(bitmap: Bitmap, filename: String): String? {
        try {
            val file = java.io.File(context.filesDir, filename)
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            return file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to save bitmap", e)
            return null
        }
    }

    fun createFittedBitmap(sourceBitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val canvasBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val srcWidth = sourceBitmap.width.toFloat()
        val srcHeight = sourceBitmap.height.toFloat()
        val scaleX = targetWidth.toFloat() / srcWidth
        val scaleY = targetHeight.toFloat() / srcHeight
        val scale = maxOf(scaleX, scaleY)
        val scaledWidth = srcWidth * scale
        val scaledHeight = srcHeight * scale
        val srcLeft = maxOf(0f, (scaledWidth - targetWidth) / 2 / scale)
        val srcTop = maxOf(0f, (scaledHeight - targetHeight) / 2 / scale)
        val srcRight = minOf(srcWidth, srcLeft + targetWidth / scale)
        val srcBottom = minOf(srcHeight, srcTop + targetHeight / scale)
        val srcRect = android.graphics.Rect(srcLeft.toInt(), srcTop.toInt(), srcRight.toInt(), srcBottom.toInt())
        val dstRect = android.graphics.RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        canvas.drawBitmap(sourceBitmap, srcRect, dstRect, paint)
        return canvasBitmap
    }
}
