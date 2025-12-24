package com.github.gezimos.inkos.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.github.gezimos.inkos.style.resolveThemeColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

class WallpaperHalftone(private val context: Context) {

    enum class HalftoneShape {
        DOTS,
        LINES
    }
    
    /**
     * Convert bitmap to halftone using theme colors (textColor and backgroundColor)
     * @param bitmap Source bitmap to convert
     * @param intensity Halftone intensity (0-200). Controls grid density (cell size)
     * @param dotSize Dot/line size (0-100). Controls size of dots/lines within each cell
     * @param shape Halftone shape: DOTS for circular dots, LINES for line pattern
     * @return Halftone-converted bitmap
     */
    fun convertToHalftone(
        bitmap: Bitmap, 
        intensity: Int = 50,
        dotSize: Int = 50,
        shape: HalftoneShape = HalftoneShape.DOTS
    ): Bitmap {
        if (intensity <= 0) {
            return bitmap
        }
        
        return try {
            val (textColor, backgroundColor) = resolveThemeColors(context)
            val width = bitmap.width
            val height = bitmap.height
            val halftoneBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(halftoneBitmap)
            
            // Fill with background color
            canvas.drawColor(backgroundColor)
            
            val paint = Paint().apply {
                color = textColor
                style = Paint.Style.FILL
                isAntiAlias = true
                strokeWidth = 1f
            }
            
            // Intensity controls the cell size (grid density), which affects pattern size
            // Lower intensity = larger cells = coarser halftone pattern
            // Higher intensity = smaller cells = finer halftone pattern
            // Intensity 0 = no halftone, Intensity 1-200 = halftone with varying cell sizes
            val adjustedCellSize = if (intensity <= 0) {
                return bitmap  // No halftone
            } else {
                // Map intensity (1-200) to cell size range
                // Intensity 1 = largest cells (coarsest pattern)
                // Intensity 200 = smallest cells (finest pattern)
                // Make resolution-aware: scale base range by image resolution
                val resolutionFactor = kotlin.math.sqrt((width * height) / (1920f * 1080f)).coerceIn(0.5f, 2f)
                val minCellSize = 5f * resolutionFactor  // Finest detail (scaled by resolution)
                val maxCellSize = 30f * resolutionFactor  // Coarsest detail (increased from 20f, scaled by resolution)
                // Invert: low intensity = large cells, high intensity = small cells
                val intensityFactor = intensity / 200f
                ((1f - intensityFactor) * (maxCellSize - minCellSize) + minCellSize).toInt()
            }
            
            // Process image in grid cells with adjusted cell size
            val cols = (width + adjustedCellSize - 1) / adjustedCellSize
            val rows = (height + adjustedCellSize - 1) / adjustedCellSize
            
            // Dot/line size within cell is controlled by dotSize parameter (0-100)
            // dotSize 0 = smallest dots/lines, dotSize 100 = largest dots/lines
            val dotSizeFactor = dotSize / 100f
            // For dots: maximum circle radius is proportional to cell size and dotSize
            // For lines: line thickness is proportional to cell size and dotSize
            val baseMaxRadius = adjustedCellSize / 2.2f  // Base max radius
            val maxRadius = baseMaxRadius * (0.3f + dotSizeFactor * 0.7f)  // Range: 30% to 100% of base
            val lineAngle = 45f  // 45 degrees for line halftone (like Photoshop)
            
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    // Calculate cell bounds
                    val startX = col * adjustedCellSize
                    val startY = row * adjustedCellSize
                    val endX = minOf(startX + adjustedCellSize, width)
                    val endY = minOf(startY + adjustedCellSize, height)
                    
                    // Calculate average grayscale value for this cell
                    var totalGray = 0f
                    var pixelCount = 0
                    
                    for (y in startY until endY) {
                        for (x in startX until endX) {
                            val pixel = bitmap.getPixel(x, y)
                            val r = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = pixel and 0xFF
                            
                            // Convert to grayscale using luminance formula
                            val gray = 0.299f * r + 0.587f * g + 0.114f * b
                            totalGray += gray
                            pixelCount++
                        }
                    }
                    
                    if (pixelCount > 0) {
                        val avgGray = totalGray / pixelCount
                        // Normalize to 0-1 range
                        val normalizedGray = avgGray / 255f
                        
                        val centerX = (startX + endX) / 2f
                        val centerY = (startY + endY) / 2f
                        val cellWidth = (endX - startX).toFloat()
                        val cellHeight = (endY - startY).toFloat()
                        
                        when (shape) {
                            HalftoneShape.DOTS -> {
                                // Calculate circle radius (proportional to darkness)
                                // Dark areas (low gray) should have large dots, light areas (high gray) should have small dots
                                // Invert: (1 - normalizedGray) so dark areas get large dots
                                val radius = (1f - normalizedGray) * maxRadius
                                
                                if (radius > 0.5f) { // Only draw if radius is meaningful
                                    canvas.drawCircle(centerX, centerY, radius, paint)
                                }
                            }
                            HalftoneShape.LINES -> {
                                // Draw lines: line thickness/spacing proportional to darkness
                                // Dark areas = thicker lines or more line coverage
                                // Light areas = thinner lines or less line coverage
                                
                                // Calculate line thickness based on coverage and dotSize
                                // Base max thickness scaled by dotSize (0-100)
                                val baseMaxThickness = adjustedCellSize * 0.5f
                                val maxLineThickness = baseMaxThickness * (0.3f + dotSizeFactor * 0.7f)  // Range: 30% to 100% of base
                                // Ensure minimum line thickness for visibility (12% of max)
                                val minLineThickness = maxLineThickness * 0.12f
                                val lineThickness = (1f - normalizedGray) * (maxLineThickness - minLineThickness) + minLineThickness
                                
                                if (lineThickness > 0.3f) {
                                    // Draw diagonal line at 45 degrees (like Photoshop)
                                    val angleRad = lineAngle * PI / 180.0
                                    val cosAngle = cos(angleRad).toFloat()
                                    val sinAngle = sin(angleRad).toFloat()
                                    
                                    // Calculate line endpoints to span the cell diagonally
                                    val halfDiagonal = sqrt((cellWidth * cellWidth + cellHeight * cellHeight).toDouble()).toFloat() / 2f
                                    val offsetX = halfDiagonal * cosAngle
                                    val offsetY = halfDiagonal * sinAngle
                                    
                                    paint.style = Paint.Style.STROKE
                                    paint.strokeWidth = lineThickness
                                    paint.strokeCap = Paint.Cap.ROUND
                                    
                                    // Draw line from one corner to opposite corner
                                    canvas.drawLine(
                                        centerX - offsetX,
                                        centerY - offsetY,
                                        centerX + offsetX,
                                        centerY + offsetY,
                                        paint
                                    )
                                    
                                    paint.style = Paint.Style.FILL  // Reset for next iteration
                                }
                            }
                        }
                    }
                }
            }
            
            halftoneBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperHalftone", "Failed to convert to halftone", e)
            e.printStackTrace()
            bitmap
        }
    }
}
