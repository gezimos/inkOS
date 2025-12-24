package com.github.gezimos.inkos.helper

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.floor

class WallpaperDither {

    enum class DitherAlgorithm {
        FLOYD_STEINBERG,
        ORDERED,
        NONE
    }

    /**
     * Apply dithering to convert grayscale image to black/white
     * @param bitmap Source bitmap (should be grayscale)
     * @param algorithm Dithering algorithm to use
     * @return Dithered bitmap (pure black/white)
     */
    fun applyDithering(
        bitmap: Bitmap,
        algorithm: DitherAlgorithm = DitherAlgorithm.FLOYD_STEINBERG
    ): Bitmap {
        if (algorithm == DitherAlgorithm.NONE) {
            return bitmap
        }

        return try {
            val width = bitmap.width
            val height = bitmap.height
            val ditheredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            // Copy pixels to int array for faster processing
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            when (algorithm) {
                DitherAlgorithm.FLOYD_STEINBERG -> {
                    applyFloydSteinberg(pixels, width, height)
                }
                DitherAlgorithm.ORDERED -> {
                    applyOrderedDithering(pixels, width, height)
                }
                DitherAlgorithm.NONE -> {
                    // Already handled above
                }
            }
            
            ditheredBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            ditheredBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperDither", "Failed to apply dithering", e)
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Floyd-Steinberg error diffusion dithering
     * Distributes quantization error to neighboring pixels
     */
    private fun applyFloydSteinberg(pixels: IntArray, width: Int, height: Int) {
        // Convert to grayscale error array
        val errors = FloatArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // Convert to grayscale using luminance formula
            errors[i] = (0.299f * r + 0.587f * g + 0.114f * b)
        }

        // Floyd-Steinberg error diffusion
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val oldPixel = errors[index]
                
                // Quantize to black (0) or white (255)
                val newPixel = if (oldPixel > 127.5f) 255f else 0f
                val error = oldPixel - newPixel
                
                // Set output pixel
                val outputValue = newPixel.toInt()
                pixels[index] = (0xFF shl 24) or (outputValue shl 16) or (outputValue shl 8) or outputValue
                
                // Distribute error to neighbors (Floyd-Steinberg pattern)
                if (x + 1 < width) {
                    errors[index + 1] += error * 7f / 16f
                }
                if (x - 1 >= 0 && y + 1 < height) {
                    errors[index + width - 1] += error * 3f / 16f
                }
                if (y + 1 < height) {
                    errors[index + width] += error * 5f / 16f
                }
                if (x + 1 < width && y + 1 < height) {
                    errors[index + width + 1] += error * 1f / 16f
                }
            }
        }
    }

    /**
     * Ordered dithering using 4x4 Bayer matrix
     * Faster than Floyd-Steinberg, creates a pattern-like effect
     */
    private fun applyOrderedDithering(pixels: IntArray, width: Int, height: Int) {
        // 4x4 Bayer matrix for ordered dithering
        val bayerMatrix = arrayOf(
            intArrayOf(0, 8, 2, 10),
            intArrayOf(12, 4, 14, 6),
            intArrayOf(3, 11, 1, 9),
            intArrayOf(15, 7, 13, 5)
        )
        val matrixSize = 4
        val threshold = 16

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pixel = pixels[index]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Convert to grayscale
                val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                
                // Get threshold from Bayer matrix
                val matrixX = x % matrixSize
                val matrixY = y % matrixSize
                val thresholdValue = bayerMatrix[matrixY][matrixX] * threshold
                
                // Apply dithering
                val ditheredValue = if (gray > thresholdValue) 255 else 0
                pixels[index] = (0xFF shl 24) or (ditheredValue shl 16) or (ditheredValue shl 8) or ditheredValue
            }
        }
    }
}
