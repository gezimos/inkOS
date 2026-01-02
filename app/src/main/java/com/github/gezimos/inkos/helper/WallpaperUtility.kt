package com.github.gezimos.inkos.helper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.github.gezimos.inkos.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperUtility(private val context: Context) {

    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    companion object {
        // Preset wallpapers - using actual wallpaper images
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
        )
    }

    data class PresetWallpaper(val name: String, val resourceId: Int)

    /**
     * Set wallpaper from a bitmap
     * @param flags WallpaperManager.FLAG_SYSTEM for home, FLAG_LOCK for lock screen, or both
     */
    suspend fun setWallpaperFromBitmap(bitmap: Bitmap, flags: Int = WallpaperManager.FLAG_SYSTEM): Boolean = withContext(Dispatchers.IO) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // Force the bitmap to be exactly screen size to prevent parallax scrolling
            val screenSizedBitmap = createFittedBitmap(bitmap, screenWidth, screenHeight)
            
            // If both flags are set, set them individually for better compatibility
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

    /**
     * Set wallpaper to a solid color
     * @param color The color to set as wallpaper
     * @param flags WallpaperManager.FLAG_SYSTEM for home, FLAG_LOCK for lock screen, or both
     */
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

    /**
     * Set wallpaper from a resource ID (drawable)
     * @param flags WallpaperManager.FLAG_SYSTEM for home, FLAG_LOCK for lock screen, or both
     * @param flipHorizontal Whether to flip the image horizontally
     * @param flipVertical Whether to flip the image vertically
     * @param invert Whether to invert the image colors
     * @param brightness Brightness adjustment (-100 to 100)
     * @param contrast Contrast adjustment (-100 to 100)
     * @param halftoneIntensity Halftone intensity (0-100). 0 = no halftone, 100 = maximum effect
     * @param halftoneShape Halftone shape: DOTS for circular dots, LINES for line pattern
     * @param overlayEnabled Whether to apply gradient overlay
     * @param overlaySide Overlay side: "left", "right", or "center"
     * @param overlaySpread Overlay spread (25-100)
     */
    suspend fun setWallpaperFromResource(
        resourceId: Int, 
        flags: Int = WallpaperManager.FLAG_SYSTEM,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        brightness: Int = 0,
        contrast: Int = 0,
        isInverted: Boolean = false,
        thresholdLevel: Int = 50,
        ditherEnabled: Boolean = false,
        ditherAlgorithm: WallpaperDither.DitherAlgorithm = WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG,
        halftoneIntensity: Int = 0,
        halftoneDotSize: Int = 50,
        halftoneShape: WallpaperHalftone.HalftoneShape = WallpaperHalftone.HalftoneShape.DOTS,
        overlayEnabled: Boolean = false,
        overlaySide: String = "left",
        overlaySpread: Int = 40,
        overlayFalloff: Int = 60,
        cropEnabled: Boolean = false,
        cropX: Float = 0.5f,
        cropY: Float = 0.5f,
        cropScale: Float = 0.8f
    ): Boolean = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            val originalBitmap = loadBitmapFromResource(resourceId)
            if (originalBitmap != null) {
                // Create a fresh mutable copy to avoid any recycling/caching issues
                bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                
                // Apply transformations in order: Flip → Brightness → Contrast → Invert → Threshold → Dithering → Halftone → Overlay
                var transformedBitmap = bitmap
                
                if (flipHorizontal) {
                    android.util.Log.d("WallpaperUtility", "Applying horizontal flip")
                    val flipped = flipBitmapHorizontally(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (flipVertical) {
                    android.util.Log.d("WallpaperUtility", "Applying vertical flip")
                    val flipped = flipBitmapVertically(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (brightness != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying brightness: $brightness")
                    val adjusted = adjustBrightness(transformedBitmap, brightness)
                    if (adjusted != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (contrast != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying contrast: $contrast")
                    val adjusted = adjustContrast(transformedBitmap, contrast)
                    if (adjusted != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (isInverted) {
                    android.util.Log.d("WallpaperUtility", "Applying invert")
                    val inverted = invertBitmapColors(transformedBitmap)
                    if (inverted != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = inverted
                }
                // Apply threshold (converts to grayscale then black/white)
                if (thresholdLevel != 50) {
                    android.util.Log.d("WallpaperUtility", "Applying threshold: $thresholdLevel")
                    val threshold = applyThreshold(transformedBitmap, thresholdLevel)
                    if (threshold != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = threshold
                }
                // Apply dithering (works on grayscale, converts to black/white with error diffusion)
                if (ditherEnabled && ditherAlgorithm != WallpaperDither.DitherAlgorithm.NONE) {
                    android.util.Log.d("WallpaperUtility", "Applying dithering: $ditherAlgorithm")
                    val ditherUtil = WallpaperDither()
                    val dithered = ditherUtil.applyDithering(transformedBitmap, ditherAlgorithm)
                    if (dithered != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = dithered
                }
                // Apply halftone (slow, shows loading indicator)
                if (halftoneIntensity > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying halftone: intensity=$halftoneIntensity, dotSize=$halftoneDotSize, shape=$halftoneShape")
                    val halftoneUtility = WallpaperHalftone(context)
                    val halftoneBitmap = halftoneUtility.convertToHalftone(transformedBitmap, halftoneIntensity, halftoneDotSize, halftoneShape)
                    if (halftoneBitmap != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = halftoneBitmap
                }
                // Apply overlay (fast, no loading indicator needed)
                if (overlayEnabled && overlaySpread > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying overlay: side=$overlaySide, spread=$overlaySpread, falloff=$overlayFalloff")
                    val prefs = com.github.gezimos.inkos.data.Prefs(context)
                    val backgroundColor = prefs.backgroundColor
                    val overlay = addGradientOverlay(transformedBitmap, backgroundColor, overlaySide, overlaySpread, overlayFalloff)
                    if (overlay != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = overlay
                }
                
                android.util.Log.d("WallpaperUtility", "BEFORE CROP CHECK: cropEnabled=$cropEnabled, cropX=$cropX, cropY=$cropY, cropScale=$cropScale")
                
                // Apply crop (should be last transformation to get final wallpaper size)
                if (cropEnabled) {
                    android.util.Log.d("WallpaperUtility", "Applying crop: x=$cropX, y=$cropY, scale=$cropScale")
                    val cropped = cropBitmap(transformedBitmap, cropX, cropY, cropScale)
                    if (cropped != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = cropped
                } else {
                    android.util.Log.d("WallpaperUtility", "CROP SKIPPED: cropEnabled=$cropEnabled")
                }
                android.util.Log.d("WallpaperUtility", "All transformations applied, setting wallpaper")
                
                val result = setWallpaperFromBitmap(transformedBitmap, flags)
                // Clean up the bitmap we created
                if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                result
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set wallpaper from resource", e)
            bitmap?.recycle()
            false
        }
    }

    /**
     * Set wallpaper from a URI (image file)
     * @param flags WallpaperManager.FLAG_SYSTEM for home, FLAG_LOCK for lock screen, or both
     * @param flipHorizontal Whether to flip the image horizontally
     * @param flipVertical Whether to flip the image vertically
     * @param invert Whether to invert the image colors
     * @param brightness Brightness adjustment (-100 to 100)
     * @param contrast Contrast adjustment (-100 to 100)
     * @param halftoneIntensity Halftone intensity (0-100). 0 = no halftone, 100 = maximum effect
     * @param halftoneShape Halftone shape: DOTS for circular dots, LINES for line pattern
     * @param overlayEnabled Whether to apply gradient overlay
     * @param overlaySide Overlay side: "left", "right", or "center"
     * @param overlaySpread Overlay spread (25-100)
     */
    suspend fun setWallpaperFromUri(
        uri: Uri, 
        flags: Int = WallpaperManager.FLAG_SYSTEM,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        brightness: Int = 0,
        contrast: Int = 0,
        isInverted: Boolean = false,
        thresholdLevel: Int = 50,
        ditherEnabled: Boolean = false,
        ditherAlgorithm: WallpaperDither.DitherAlgorithm = WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG,
        halftoneIntensity: Int = 0,
        halftoneDotSize: Int = 50,
        halftoneShape: WallpaperHalftone.HalftoneShape = WallpaperHalftone.HalftoneShape.DOTS,
        overlayEnabled: Boolean = false,
        overlaySide: String = "left",
        overlaySpread: Int = 40,
        overlayFalloff: Int = 60,
        cropEnabled: Boolean = false,
        cropX: Float = 0.5f,
        cropY: Float = 0.5f,
        cropScale: Float = 0.8f
    ): Boolean = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            // Decode with options to get orientation info
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val inputStream1 = context.contentResolver.openInputStream(uri)
            if (inputStream1 == null) {
                return@withContext false
            }
            BitmapFactory.decodeStream(inputStream1, null, options)
            inputStream1.close()
            
            // Get EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                if (parcelFileDescriptor != null) {
                    val exif = ExifInterface(parcelFileDescriptor.fileDescriptor)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    parcelFileDescriptor.close()
                }
            } catch (e: Exception) {
                android.util.Log.w("WallpaperUtility", "Could not read EXIF orientation", e)
            }
            
            // Decode the actual bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val inputStream2 = context.contentResolver.openInputStream(uri)
            if (inputStream2 == null) {
                return@withContext false
            }
            bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()
            
            if (bitmap != null) {
                // Apply orientation correction
                var transformedBitmap = applyOrientation(bitmap, orientation)
                if (transformedBitmap != bitmap) {
                    bitmap.recycle()
                }
                
                // Apply transformations in order: Flip → Brightness → Contrast → Invert → Threshold → Dithering → Halftone → Overlay
                android.util.Log.d("WallpaperUtility", "setWallpaperFromUri: flipH=$flipHorizontal, flipV=$flipVertical, brightness=$brightness, contrast=$contrast, invert=$isInverted, threshold=$thresholdLevel, dither=$ditherEnabled, halftone=$halftoneIntensity dotSize=$halftoneDotSize, overlay=$overlayEnabled side=$overlaySide spread=$overlaySpread")
                
                if (flipHorizontal) {
                    android.util.Log.d("WallpaperUtility", "Applying horizontal flip")
                    val flipped = flipBitmapHorizontally(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (flipVertical) {
                    android.util.Log.d("WallpaperUtility", "Applying vertical flip")
                    val flipped = flipBitmapVertically(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (brightness != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying brightness: $brightness")
                    val adjusted = adjustBrightness(transformedBitmap, brightness)
                    if (adjusted != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (contrast != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying contrast: $contrast")
                    val adjusted = adjustContrast(transformedBitmap, contrast)
                    if (adjusted != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (isInverted) {
                    android.util.Log.d("WallpaperUtility", "Applying invert")
                    val inverted = invertBitmapColors(transformedBitmap)
                    if (inverted != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = inverted
                }
                // Apply threshold (converts to grayscale then black/white)
                if (thresholdLevel != 50) {
                    android.util.Log.d("WallpaperUtility", "Applying threshold: $thresholdLevel")
                    val threshold = applyThreshold(transformedBitmap, thresholdLevel)
                    if (threshold != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = threshold
                }
                // Apply dithering (works on grayscale, converts to black/white with error diffusion)
                if (ditherEnabled && ditherAlgorithm != WallpaperDither.DitherAlgorithm.NONE) {
                    android.util.Log.d("WallpaperUtility", "Applying dithering: $ditherAlgorithm")
                    val ditherUtil = WallpaperDither()
                    val dithered = ditherUtil.applyDithering(transformedBitmap, ditherAlgorithm)
                    if (dithered != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = dithered
                }
                // Apply halftone (slow, shows loading indicator)
                if (halftoneIntensity > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying halftone: intensity=$halftoneIntensity, dotSize=$halftoneDotSize, shape=$halftoneShape")
                    val halftoneUtility = WallpaperHalftone(context)
                    val halftoneBitmap = halftoneUtility.convertToHalftone(transformedBitmap, halftoneIntensity, halftoneDotSize, halftoneShape)
                    if (halftoneBitmap != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = halftoneBitmap
                }
                // Apply overlay (fast, no loading indicator needed)
                if (overlayEnabled && overlaySpread > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying overlay: side=$overlaySide, spread=$overlaySpread, falloff=$overlayFalloff")
                    val prefs = com.github.gezimos.inkos.data.Prefs(context)
                    val backgroundColor = prefs.backgroundColor
                    val overlay = addGradientOverlay(transformedBitmap, backgroundColor, overlaySide, overlaySpread, overlayFalloff)
                    if (overlay != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = overlay
                }
                
                android.util.Log.d("WallpaperUtility", "BEFORE CROP CHECK (URI): cropEnabled=$cropEnabled, cropX=$cropX, cropY=$cropY, cropScale=$cropScale")
                
                // Apply crop (should be last transformation to get final wallpaper size)
                if (cropEnabled) {
                    android.util.Log.d("WallpaperUtility", "Applying crop: x=$cropX, y=$cropY, scale=$cropScale")
                    val cropped = cropBitmap(transformedBitmap, cropX, cropY, cropScale)
                    if (cropped != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = cropped
                } else {
                    android.util.Log.d("WallpaperUtility", "CROP SKIPPED (URI): cropEnabled=$cropEnabled")
                }
                android.util.Log.d("WallpaperUtility", "All transformations applied, setting wallpaper")
                
                setWallpaperFromBitmap(transformedBitmap, flags)
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set wallpaper from URI", e)
            bitmap?.recycle()
            false
        }
    }
    
    /**
     * Apply EXIF orientation to bitmap
     */
    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return bitmap
        }
        
        val matrix = Matrix()
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

    /**
     * Load bitmap from a resource ID (drawable)
     * Loads at original size without density scaling
     */
    fun loadBitmapFromResource(resourceId: Int): Bitmap? {
        return try {
            if (resourceId == -1) {
                // White wallpaper
                return Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
            } else if (resourceId == -2) {
                // Black wallpaper
                return Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLACK) }
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
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to load bitmap from resource", e)
            null
        }
    }
    
    /**
     * Load bitmap from a URI (image file)
     */
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
    
    /**
     * Apply editor state effects to a bitmap
     */
    fun applyEditorStateToBitmap(bitmap: Bitmap, editorState: com.github.gezimos.inkos.ui.compose.WallpaperEditorState): Bitmap {
        var b = bitmap.copy(Bitmap.Config.ARGB_8888, true) ?: return bitmap
        
        // Apply crop first
        if (editorState.cropEnabled) {
            val cropped = cropBitmap(b, editorState.cropX, editorState.cropY, editorState.cropScale)
            if (cropped != b) {
                b.recycle()
                b = cropped
            }
        }
        
        // Apply flips
        if (editorState.flipHorizontal) {
            val flipped = flipBitmapHorizontally(b)
            if (flipped != b) {
                b.recycle()
                b = flipped
            }
        }
        if (editorState.flipVertical) {
            val flipped = flipBitmapVertically(b)
            if (flipped != b) {
                b.recycle()
                b = flipped
            }
        }
        
        // Brightness and contrast
        if (editorState.brightness != 0) {
            val adjusted = adjustBrightness(b, editorState.brightness)
            if (adjusted != b) {
                b.recycle()
                b = adjusted
            }
        }
        if (editorState.contrast != 0) {
            val adjusted = adjustContrast(b, editorState.contrast)
            if (adjusted != b) {
                b.recycle()
                b = adjusted
            }
        }
        
        if (editorState.isInverted) {
            val inverted = invertBitmapColors(b)
            if (inverted != b) {
                b.recycle()
                b = inverted
            }
        }
        
        if (editorState.thresholdLevel != 50) {
            val threshold = applyThreshold(b, editorState.thresholdLevel)
            if (threshold != b) {
                b.recycle()
                b = threshold
            }
        }
        
        if (editorState.ditherEnabled) {
            val dither = WallpaperDither()
            val dithered = dither.applyDithering(b, editorState.ditherAlgorithm)
            if (dithered != b) {
                b.recycle()
                b = dithered
            }
        }
        
        if (editorState.halftoneIntensity > 0) {
            val halftone = WallpaperHalftone(context)
            val halftoned = halftone.convertToHalftone(b, editorState.halftoneIntensity, editorState.halftoneDotSize, editorState.halftoneShape)
            if (halftoned != b) {
                b.recycle()
                b = halftoned
            }
        }
        
        if (editorState.overlayEnabled) {
            val prefs = com.github.gezimos.inkos.data.Prefs(context)
            val overlay = addGradientOverlay(b, prefs.backgroundColor, editorState.overlaySide, editorState.overlaySpread, editorState.overlayFalloff)
            if (overlay != b) {
                b.recycle()
                b = overlay
            }
        }
        
        return b
    }
    
    /**
     * Save bitmap to internal storage
     */
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
    
    /**
     * Flip bitmap horizontally
     */
    fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
        return try {
            val matrix = Matrix().apply {
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to flip bitmap", e)
            bitmap
        }
    }
    
    /**
     * Invert bitmap colors
     */
    fun invertBitmapColors(bitmap: Bitmap): Bitmap {
        return try {
            val colorMatrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            
            val invertedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(invertedBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            invertedBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to invert bitmap colors", e)
            bitmap
        }
    }
    
    /**
     * Flip bitmap vertically
     */
    fun flipBitmapVertically(bitmap: Bitmap): Bitmap {
        return try {
            val matrix = Matrix().apply {
                postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to flip bitmap vertically", e)
            bitmap
        }
    }
    
    /**
     * Rotate bitmap by specified degrees
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        return try {
            val matrix = Matrix().apply {
                postRotate(degrees.toFloat())
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to rotate bitmap", e)
            bitmap
        }
    }
    
    /**
     * Crop bitmap based on normalized crop position and scale
     * @param bitmap Source bitmap
     * @param cropX Normalized X position (0-1) of crop center
     * @param cropY Normalized Y position (0-1) of crop center
     * @param cropScale Scale of crop area (0.3-1.0)
     * @param targetWidth Target width for the cropped image (usually screen width)
     * @param targetHeight Target height for the cropped image (usually screen height)
     * @return Cropped bitmap matching target dimensions
     */
    fun cropBitmap(
        bitmap: Bitmap,
        cropX: Float,
        cropY: Float,
        cropScale: Float = 0.8f,
        targetWidth: Int = screenWidth,
        targetHeight: Int = screenHeight
    ): Bitmap {
        return try {
            android.util.Log.d("WallpaperUtility", "cropBitmap called: cropX=$cropX, cropY=$cropY, scale=$cropScale")
            android.util.Log.d("WallpaperUtility", "Source bitmap: ${bitmap.width}x${bitmap.height}")
            android.util.Log.d("WallpaperUtility", "Target size: ${targetWidth}x${targetHeight}")
            
            val sourceWidth = bitmap.width
            val sourceHeight = bitmap.height
            val targetAspectRatio = targetHeight.toFloat() / targetWidth.toFloat()
            val scale = cropScale.coerceIn(0.3f, 1.0f)
            
            // Calculate maximum possible crop region dimensions (maintaining target aspect ratio)
            val maxCropWidth: Int
            val maxCropHeight: Int
            val sourceAspectRatio = sourceHeight.toFloat() / sourceWidth.toFloat()
            
            if (sourceAspectRatio > targetAspectRatio) {
                // Source is taller than target, crop height
                maxCropWidth = sourceWidth
                maxCropHeight = (sourceWidth * targetAspectRatio).toInt()
            } else {
                // Source is wider than target, crop width
                maxCropHeight = sourceHeight
                maxCropWidth = (sourceHeight / targetAspectRatio).toInt()
            }
            
            // Apply scale to crop dimensions
            val cropWidth = (maxCropWidth * scale).toInt().coerceAtMost(sourceWidth)
            val cropHeight = (maxCropHeight * scale).toInt().coerceAtMost(sourceHeight)
            
            android.util.Log.d("WallpaperUtility", "Crop dimensions: ${cropWidth}x${cropHeight}")
            
            // Calculate maximum allowed crop area (leaving space for the crop box to move)
            val availableWidth = sourceWidth - cropWidth
            val availableHeight = sourceHeight - cropHeight
            
            // Convert normalized position to absolute pixel position
            val cropLeft = (availableWidth * cropX).toInt().coerceIn(0, availableWidth.coerceAtLeast(0))
            val cropTop = (availableHeight * cropY).toInt().coerceIn(0, availableHeight.coerceAtLeast(0))
            
            android.util.Log.d("WallpaperUtility", "Crop position: left=$cropLeft, top=$cropTop")
            
            // Extract the crop region
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropLeft,
                cropTop,
                cropWidth.coerceAtMost(sourceWidth - cropLeft),
                cropHeight.coerceAtMost(sourceHeight - cropTop)
            )
            
            android.util.Log.d("WallpaperUtility", "Cropped bitmap: ${croppedBitmap.width}x${croppedBitmap.height}")
            croppedBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to crop bitmap", e)
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Apply threshold/posterization to convert grayscale to pure black/white
     * @param bitmap Source bitmap
     * @param thresholdLevel Threshold level (0-100), where 50 = middle gray
     * @return Threshold bitmap (pure black/white)
     */
    fun applyThreshold(bitmap: Bitmap, thresholdLevel: Int): Bitmap {
        if (thresholdLevel < 0 || thresholdLevel > 100) return bitmap
        
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val thresholdValue = (thresholdLevel / 100f * 255f).toInt()
            
            val thresholdBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Convert to grayscale using luminance formula
                val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                
                // Apply threshold: above threshold = white, below = black
                val outputValue = if (gray > thresholdValue) 255 else 0
                pixels[i] = (0xFF shl 24) or (outputValue shl 16) or (outputValue shl 8) or outputValue
            }
            
            thresholdBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            thresholdBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to apply threshold", e)
            e.printStackTrace()
            bitmap
        }
    }
    
    /**
     * Adjust brightness (-100 to 100)
     */
    fun adjustBrightness(bitmap: Bitmap, brightness: Int): Bitmap {
        if (brightness == 0) return bitmap
        
        return try {
            val brightnessValue = brightness / 100f
            val colorMatrix = ColorMatrix().apply {
                set(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, brightnessValue * 255f,
                        0f, 1f, 0f, 0f, brightnessValue * 255f,
                        0f, 0f, 1f, 0f, brightnessValue * 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            
            val adjustedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(adjustedBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            adjustedBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to adjust brightness", e)
            bitmap
        }
    }
    
    /**
     * Adjust contrast (-100 to 100)
     */
    fun adjustContrast(bitmap: Bitmap, contrast: Int): Bitmap {
        if (contrast == 0) return bitmap
        
        return try {
            // Convert contrast from -100..100 to scale factor
            // -100 = 0.0 (no contrast), 0 = 1.0 (normal), 100 = 2.0 (high contrast)
            val contrastValue = (contrast + 100) / 100f
            val translate = (-.5f * contrastValue + .5f) * 255f
            
            val colorMatrix = ColorMatrix(floatArrayOf(
                contrastValue, 0f, 0f, 0f, translate,
                0f, contrastValue, 0f, 0f, translate,
                0f, 0f, contrastValue, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            
            val adjustedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(adjustedBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            
            adjustedBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to adjust contrast", e)
            bitmap
        }
    }
    
    /**
     * Add gradient overlay with independent coverage and falloff controls
     * @param spread Coverage area (25-100): How much of the screen is covered
     * @param falloff Gradient smoothness (0-100): How smooth the transition is (0=sharp, 100=very smooth)
     * 
     * For left/right: spread controls coverage, falloff controls how gradual the fade is
     * For center: spread controls total coverage from center, falloff controls smoothness
     */
    fun addGradientOverlay(bitmap: Bitmap, backgroundColor: Int, side: String, spread: Int, falloff: Int): Bitmap {
        if (spread <= 0) return bitmap
        
        android.util.Log.d("WallpaperUtility", "addGradientOverlay: side=$side, spread=$spread%, falloff=$falloff%")
        
        return try {
            val overlayBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(overlayBitmap)
            
            val paint = Paint()
            val width = bitmap.width.toFloat()
            val height = bitmap.height.toFloat()
            
            // Enable anti-aliasing and dithering for smooth gradients
            paint.isAntiAlias = true
            paint.isDither = true
            
            when (side) {
                "left" -> {
                    // spread = total coverage area from left edge
                    // falloff = what percentage of coverage is gradient (rest is solid)
                    val coverageWidth = width * (spread / 100f)
                    val solidRatio = 1f - (falloff / 100f)
                    val solidEnd = coverageWidth * solidRatio
                    
                    // Draw solid portion first
                    if (solidEnd > 0) {
                        paint.shader = null
                        paint.color = backgroundColor
                        canvas.drawRect(0f, 0f, solidEnd, height, paint)
                    }
                    
                    // Draw gradient from solid end to coverage end
                    if (solidEnd < coverageWidth) {
                        val gradient = LinearGradient(
                            solidEnd, 0f, coverageWidth, 0f,
                            backgroundColor, android.graphics.Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                        paint.shader = gradient
                        canvas.drawRect(solidEnd, 0f, coverageWidth, height, paint)
                    }
                }
                "right" -> {
                    val coverageWidth = width * (spread / 100f)
                    val solidRatio = 1f - (falloff / 100f)
                    val gradientWidth = coverageWidth * (falloff / 100f)
                    val gradientStart = width - coverageWidth
                    val solidStart = width - (coverageWidth * solidRatio)
                    
                    // Draw gradient first
                    if (gradientWidth > 0) {
                        val gradient = LinearGradient(
                            gradientStart, 0f, solidStart, 0f,
                            android.graphics.Color.TRANSPARENT, backgroundColor,
                            Shader.TileMode.CLAMP
                        )
                        paint.shader = gradient
                        canvas.drawRect(gradientStart, 0f, solidStart, height, paint)
                    }
                    
                    // Draw solid portion
                    if (solidStart < width) {
                        paint.shader = null
                        paint.color = backgroundColor
                        canvas.drawRect(solidStart, 0f, width, height, paint)
                    }
                }
                "center" -> {
                    val centerX = width / 2f
                    val totalCoverage = width * (spread / 100f)
                    val halfCoverage = totalCoverage / 2f
                    val solidRatio = 1f - (falloff / 100f)
                    val solidHalfWidth = halfCoverage * solidRatio
                    
                    // Draw solid center area first
                    if (solidHalfWidth > 0) {
                        paint.shader = null
                        paint.color = backgroundColor
                        canvas.drawRect(centerX - solidHalfWidth, 0f, centerX + solidHalfWidth, height, paint)
                    }
                    
                    // Left gradient
                    if (solidHalfWidth < halfCoverage) {
                        val leftGradient = LinearGradient(
                            centerX - solidHalfWidth, 0f, centerX - halfCoverage, 0f,
                            backgroundColor, android.graphics.Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                        paint.shader = leftGradient
                        canvas.drawRect(centerX - halfCoverage, 0f, centerX - solidHalfWidth, height, paint)
                    }
                    
                    // Right gradient
                    if (solidHalfWidth < halfCoverage) {
                        val rightGradient = LinearGradient(
                            centerX + solidHalfWidth, 0f, centerX + halfCoverage, 0f,
                            backgroundColor, android.graphics.Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                        paint.shader = rightGradient
                        canvas.drawRect(centerX + solidHalfWidth, 0f, centerX + halfCoverage, height, paint)
                    }
                }
            }
            
            overlayBitmap
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to add gradient overlay", e)
            bitmap
        }
    }
    
    private fun createFittedBitmap(sourceBitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
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
    
    suspend fun setNoCropWallpaperFromResource(
        resourceId: Int, 
        flags: Int = WallpaperManager.FLAG_SYSTEM,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        brightness: Int = 0,
        contrast: Int = 0,
        isInverted: Boolean = false,
        thresholdLevel: Int = 50,
        ditherEnabled: Boolean = false,
        ditherAlgorithm: WallpaperDither.DitherAlgorithm = WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG,
        halftoneIntensity: Int = 0,
        halftoneDotSize: Int = 50,
        halftoneShape: WallpaperHalftone.HalftoneShape = WallpaperHalftone.HalftoneShape.DOTS,
        overlayEnabled: Boolean = false,
        overlaySide: String = "left",
        overlaySpread: Int = 40,
        overlayFalloff: Int = 60
    ): Boolean = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        var fitted: Bitmap? = null
        try {
            val originalBitmap = loadBitmapFromResource(resourceId)
            if (originalBitmap != null) {
                // Create a fresh mutable copy to avoid any recycling/caching issues
                bitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                
                // Apply transformations in order: Flip → Brightness → Contrast → Invert → Threshold → Dithering → Halftone → Overlay
                android.util.Log.d("WallpaperUtility", "setNoCropWallpaperFromResource: flipH=$flipHorizontal, flipV=$flipVertical, brightness=$brightness, contrast=$contrast, invert=$isInverted, threshold=$thresholdLevel, dither=$ditherEnabled, halftone=$halftoneIntensity dotSize=$halftoneDotSize, overlay=$overlayEnabled side=$overlaySide spread=$overlaySpread")
                var transformedBitmap = bitmap
                
                if (flipHorizontal) {
                    android.util.Log.d("WallpaperUtility", "Applying horizontal flip")
                    val flipped = flipBitmapHorizontally(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (flipVertical) {
                    android.util.Log.d("WallpaperUtility", "Applying vertical flip")
                    val flipped = flipBitmapVertically(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (brightness != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying brightness: $brightness")
                    val adjusted = adjustBrightness(transformedBitmap, brightness)
                    if (adjusted != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (contrast != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying contrast: $contrast")
                    val adjusted = adjustContrast(transformedBitmap, contrast)
                    if (adjusted != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (isInverted) {
                    android.util.Log.d("WallpaperUtility", "Applying invert")
                    val inverted = invertBitmapColors(transformedBitmap)
                    if (inverted != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = inverted
                }
                // Apply threshold (converts to grayscale then black/white)
                if (thresholdLevel != 50) {
                    android.util.Log.d("WallpaperUtility", "Applying threshold: $thresholdLevel")
                    val threshold = applyThreshold(transformedBitmap, thresholdLevel)
                    if (threshold != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = threshold
                }
                // Apply dithering (works on grayscale, converts to black/white with error diffusion)
                if (ditherEnabled && ditherAlgorithm != WallpaperDither.DitherAlgorithm.NONE) {
                    android.util.Log.d("WallpaperUtility", "Applying dithering: $ditherAlgorithm")
                    val ditherUtil = WallpaperDither()
                    val dithered = ditherUtil.applyDithering(transformedBitmap, ditherAlgorithm)
                    if (dithered != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = dithered
                }
                // Apply halftone (slow, shows loading indicator)
                if (halftoneIntensity > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying halftone: intensity=$halftoneIntensity, dotSize=$halftoneDotSize, shape=$halftoneShape")
                    val halftoneUtility = WallpaperHalftone(context)
                    val halftoneBitmap = halftoneUtility.convertToHalftone(transformedBitmap, halftoneIntensity, halftoneDotSize, halftoneShape)
                    if (halftoneBitmap != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = halftoneBitmap
                }
                // Apply overlay (fast, no loading indicator needed)
                if (overlayEnabled && overlaySpread > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying overlay: side=$overlaySide, spread=$overlaySpread, falloff=$overlayFalloff")
                    val prefs = com.github.gezimos.inkos.data.Prefs(context)
                    val backgroundColor = prefs.backgroundColor
                    val overlay = addGradientOverlay(transformedBitmap, backgroundColor, overlaySide, overlaySpread, overlayFalloff)
                    if (overlay != transformedBitmap) {
                        if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                    }
                    transformedBitmap = overlay
                }
                android.util.Log.d("WallpaperUtility", "All transformations applied, creating fitted bitmap")
                
                fitted = createFittedBitmap(transformedBitmap, screenWidth, screenHeight)
                val result = setWallpaperFromBitmap(fitted, flags)
                // Clean up
                if (transformedBitmap != bitmap) transformedBitmap?.recycle()
                result
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set no crop wallpaper from resource", e)
            false
        } finally {
            bitmap?.recycle()
            fitted?.recycle()
        }
    }
    
    suspend fun setNoCropWallpaperFromUri(
        uri: Uri, 
        flags: Int = WallpaperManager.FLAG_SYSTEM,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        brightness: Int = 0,
        contrast: Int = 0,
        isInverted: Boolean = false,
        thresholdLevel: Int = 50,
        ditherEnabled: Boolean = false,
        ditherAlgorithm: WallpaperDither.DitherAlgorithm = WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG,
        halftoneIntensity: Int = 0,
        halftoneDotSize: Int = 50,
        halftoneShape: WallpaperHalftone.HalftoneShape = WallpaperHalftone.HalftoneShape.DOTS,
        overlayEnabled: Boolean = false,
        overlaySide: String = "left",
        overlaySpread: Int = 40,
        overlayFalloff: Int = 60
    ): Boolean = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        var fitted: Bitmap? = null
        try {
            // Decode with options to get orientation info
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            val inputStream1 = context.contentResolver.openInputStream(uri)
            if (inputStream1 == null) {
                return@withContext false
            }
            BitmapFactory.decodeStream(inputStream1, null, options)
            inputStream1.close()
            
            // Get EXIF orientation
            var orientation = ExifInterface.ORIENTATION_NORMAL
            try {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                if (parcelFileDescriptor != null) {
                    val exif = ExifInterface(parcelFileDescriptor.fileDescriptor)
                    orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    parcelFileDescriptor.close()
                }
            } catch (e: Exception) {
                android.util.Log.w("WallpaperUtility", "Could not read EXIF orientation", e)
            }
            
            // Decode the actual bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val inputStream2 = context.contentResolver.openInputStream(uri)
            if (inputStream2 == null) {
                return@withContext false
            }
            bitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
            inputStream2.close()
            
            if (bitmap != null) {
                // Apply orientation correction
                var transformedBitmap = applyOrientation(bitmap, orientation)
                if (transformedBitmap != bitmap) {
                    bitmap.recycle()
                }
                
                // Apply transformations in order: Flip → Brightness → Contrast → Invert → Threshold → Dithering → Halftone → Overlay
                android.util.Log.d("WallpaperUtility", "setNoCropWallpaperFromUri: flipH=$flipHorizontal, flipV=$flipVertical, brightness=$brightness, contrast=$contrast, invert=$isInverted, threshold=$thresholdLevel, dither=$ditherEnabled, halftone=$halftoneIntensity dotSize=$halftoneDotSize, overlay=$overlayEnabled side=$overlaySide spread=$overlaySpread")
                
                if (flipHorizontal) {
                    android.util.Log.d("WallpaperUtility", "Applying horizontal flip")
                    val flipped = flipBitmapHorizontally(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (flipVertical) {
                    android.util.Log.d("WallpaperUtility", "Applying vertical flip")
                    val flipped = flipBitmapVertically(transformedBitmap)
                    if (flipped != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = flipped
                }
                if (brightness != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying brightness: $brightness")
                    val adjusted = adjustBrightness(transformedBitmap, brightness)
                    if (adjusted != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (contrast != 0) {
                    android.util.Log.d("WallpaperUtility", "Applying contrast: $contrast")
                    val adjusted = adjustContrast(transformedBitmap, contrast)
                    if (adjusted != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = adjusted
                }
                if (isInverted) {
                    android.util.Log.d("WallpaperUtility", "Applying invert")
                    val inverted = invertBitmapColors(transformedBitmap)
                    if (inverted != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = inverted
                }
                // Apply threshold (converts to grayscale then black/white)
                if (thresholdLevel != 50) {
                    android.util.Log.d("WallpaperUtility", "Applying threshold: $thresholdLevel")
                    val threshold = applyThreshold(transformedBitmap, thresholdLevel)
                    if (threshold != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = threshold
                }
                // Apply dithering (works on grayscale, converts to black/white with error diffusion)
                if (ditherEnabled && ditherAlgorithm != WallpaperDither.DitherAlgorithm.NONE) {
                    android.util.Log.d("WallpaperUtility", "Applying dithering: $ditherAlgorithm")
                    val ditherUtil = WallpaperDither()
                    val dithered = ditherUtil.applyDithering(transformedBitmap, ditherAlgorithm)
                    if (dithered != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = dithered
                }
                // Apply halftone (slow, shows loading indicator)
                if (halftoneIntensity > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying halftone: intensity=$halftoneIntensity, dotSize=$halftoneDotSize, shape=$halftoneShape")
                    val halftoneUtility = WallpaperHalftone(context)
                    val halftoneBitmap = halftoneUtility.convertToHalftone(transformedBitmap, halftoneIntensity, halftoneDotSize, halftoneShape)
                    if (halftoneBitmap != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = halftoneBitmap
                }
                // Apply overlay (fast, no loading indicator needed)
                if (overlayEnabled && overlaySpread > 0) {
                    android.util.Log.d("WallpaperUtility", "Applying overlay: side=$overlaySide, spread=$overlaySpread, falloff=$overlayFalloff")
                    val prefs = com.github.gezimos.inkos.data.Prefs(context)
                    val backgroundColor = prefs.backgroundColor
                    val overlay = addGradientOverlay(transformedBitmap, backgroundColor, overlaySide, overlaySpread, overlayFalloff)
                    if (overlay != transformedBitmap) {
                        transformedBitmap.recycle()
                    }
                    transformedBitmap = overlay
                }
                android.util.Log.d("WallpaperUtility", "All transformations applied, creating fitted bitmap")
                
                fitted = createFittedBitmap(transformedBitmap, screenWidth, screenHeight)
                val result = setWallpaperFromBitmap(fitted, flags)
                // Clean up
                transformedBitmap.recycle()
                result
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("WallpaperUtility", "Failed to set no crop wallpaper from URI", e)
            false
        } finally {
            bitmap?.recycle()
            fitted?.recycle()
        }
    }
    
}
