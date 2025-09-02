package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import java.io.File

object BackgroundImageHelper {
    private var lastFailedUri: String? = null
    private var failureCount = 0
    private const val MAX_RETRIES = 3
    private const val CACHED_BACKGROUND_FILENAME = "cached_background.jpg"
    
    /**
     * Caches the background image to internal storage when first selected.
     * Returns the cached file path, or null if caching failed.
     */
    private fun cacheBackgroundImage(context: Context, uri: Uri): String? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val cacheDir = File(context.filesDir, "background_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val cachedFile = File(cacheDir, CACHED_BACKGROUND_FILENAME)
            
            inputStream.use { input ->
                cachedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d("BackgroundImageHelper", "Background image cached to: ${cachedFile.absolutePath}")
            return cachedFile.absolutePath
        } catch (e: Exception) {
            Log.e("BackgroundImageHelper", "Failed to cache background image: ${e.message}")
            return null
        }
    }
    
    /**
     * Gets the cached background image file if it exists.
     */
    private fun getCachedBackgroundFile(context: Context): File? {
        val cacheDir = File(context.filesDir, "background_cache")
        val cachedFile = File(cacheDir, CACHED_BACKGROUND_FILENAME)
        return if (cachedFile.exists()) cachedFile else null
    }
    
    /**
     * Loads a bitmap from a file path with optimization.
     */
    private fun loadBitmapFromFile(context: Context, filePath: String): Bitmap? {
        try {
            val dimensionOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, dimensionOptions)
            
            val originalWidth = dimensionOptions.outWidth
            val originalHeight = dimensionOptions.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e("BackgroundImageHelper", "Invalid cached image dimensions: ${originalWidth}x${originalHeight}")
                return null
            }
            
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val maxWidth = screenWidth * 2
            val maxHeight = screenHeight * 2
            
            var inSampleSize = 1
            if (originalWidth > maxWidth || originalHeight > maxHeight) {
                val halfWidth = originalWidth / 2
                val halfHeight = originalHeight / 2
                while ((halfWidth / inSampleSize) >= maxWidth && (halfHeight / inSampleSize) >= maxHeight) {
                    inSampleSize *= 2
                }
            }
            
            val loadOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            
            return BitmapFactory.decodeFile(filePath, loadOptions)
        } catch (e: Exception) {
            Log.e("BackgroundImageHelper", "Error loading bitmap from file: ${e.message}")
            return null
        }
    }
    
    /**
     * Loads a bitmap directly from URI (fallback method).
     */
    private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        try {
            val contentResolver = context.contentResolver
            
            val dimensionOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, dimensionOptions)
            }
            
            val originalWidth = dimensionOptions.outWidth
            val originalHeight = dimensionOptions.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e("BackgroundImageHelper", "Invalid image dimensions: ${originalWidth}x${originalHeight}")
                return null
            }
            
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val maxWidth = screenWidth * 2
            val maxHeight = screenHeight * 2
            
            var inSampleSize = 1
            if (originalWidth > maxWidth || originalHeight > maxHeight) {
                val halfWidth = originalWidth / 2
                val halfHeight = originalHeight / 2
                while ((halfWidth / inSampleSize) >= maxWidth && (halfHeight / inSampleSize) >= maxHeight) {
                    inSampleSize *= 2
                }
            }
            
            val loadOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            
            return contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, loadOptions)
            }
        } catch (e: Exception) {
            Log.e("BackgroundImageHelper", "Error loading bitmap from URI: ${e.message}")
            return null
        }
    }
    
    /**
     * Clears the cached background image.
     */
    fun clearCachedBackground(context: Context) {
        try {
            val cachedFile = getCachedBackgroundFile(context)
            if (cachedFile != null && cachedFile.exists()) {
                cachedFile.delete()
                Log.d("BackgroundImageHelper", "Cached background image cleared")
            }
        } catch (e: Exception) {
            Log.e("BackgroundImageHelper", "Error clearing cached background: ${e.message}")
        }
    }
    
    /**
     * Validates that the stored background image URI is still accessible.
     * Clears invalid URIs to prevent repeated load failures.
     */
    fun validateBackgroundImageUri(context: Context, prefs: Prefs) {
        val backgroundImageUri = prefs.homeBackgroundImageUri
        if (!backgroundImageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(backgroundImageUri)
                val contentResolver = context.contentResolver
                
                // Reset failure count if this is a different URI
                if (lastFailedUri != backgroundImageUri) {
                    lastFailedUri = null
                    failureCount = 0
                }
                
                // First check persistent permissions
                val persistableUris = contentResolver.persistedUriPermissions
                val hasPermission = persistableUris.any { it.uri == uri && it.isReadPermission }
                
                if (!hasPermission) {
                    failureCount++
                    Log.w("BackgroundImageHelper", "Background image URI lost persistent permission (attempt $failureCount/$MAX_RETRIES): $backgroundImageUri")
                    
                    if (failureCount >= MAX_RETRIES) {
                        prefs.homeBackgroundImageUri = null
                        lastFailedUri = null
                        failureCount = 0
                        Log.w("BackgroundImageHelper", "Background image URI cleared after $MAX_RETRIES failed attempts")
                    } else {
                        lastFailedUri = backgroundImageUri
                    }
                    return
                }
                
                // Additional check: try to actually access the URI
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    // Just check if we can read the first few bytes
                    val buffer = ByteArray(4)
                    inputStream.read(buffer)
                }
                
                // Success - reset failure tracking
                lastFailedUri = null
                failureCount = 0
                Log.d("BackgroundImageHelper", "Background image URI validated successfully: $backgroundImageUri")
                
            } catch (e: SecurityException) {
                failureCount++
                Log.w("BackgroundImageHelper", "Background image URI access denied (attempt $failureCount/$MAX_RETRIES): $backgroundImageUri")
                
                if (failureCount >= MAX_RETRIES) {
                    prefs.homeBackgroundImageUri = null
                    lastFailedUri = null
                    failureCount = 0
                } else {
                    lastFailedUri = backgroundImageUri
                }
            } catch (e: Exception) {
                failureCount++
                Log.w("BackgroundImageHelper", "Background image URI not accessible (attempt $failureCount/$MAX_RETRIES): $backgroundImageUri - ${e.message}")
                
                if (failureCount >= MAX_RETRIES) {
                    prefs.homeBackgroundImageUri = null
                    lastFailedUri = null
                    failureCount = 0
                } else {
                    lastFailedUri = backgroundImageUri
                }
            }
        }
    }

    /**
     * Sets up the background image for the provided root layout using the URI in prefs.
     * Handles image loading, optimization, and error feedback.
     */
    fun setupBackgroundImage(
        context: Context,
        prefs: Prefs,
        viewModel: MainViewModel?,
        rootLayout: ViewGroup
    ) {
        val backgroundImageUri = prefs.homeBackgroundImageUri
        if (!backgroundImageUri.isNullOrEmpty()) {
            Log.d("BackgroundImageHelper", "Background image URI found: $backgroundImageUri")
            try {
                var backgroundImageView = rootLayout.findViewWithTag<ImageView>("home_background")
                if (backgroundImageView == null) {
                    backgroundImageView = ImageView(context).apply {
                        tag = "home_background"
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageDrawable(null)
                        clearAnimation()
                        alpha = 1.0f
                    }
                    rootLayout.addView(backgroundImageView, 0)
                }
                val uri = Uri.parse(backgroundImageUri)
                val bitmap = loadSafeBackgroundImage(context, prefs, uri)
                if (bitmap != null) {
                    backgroundImageView.setImageDrawable(null)
                    backgroundImageView.setImageBitmap(bitmap)
                    val opacity = viewModel?.homeBackgroundImageOpacity?.value ?: prefs.homeBackgroundImageOpacity
                    val opacityFloat = opacity / 100f
                    backgroundImageView.alpha = opacityFloat
                    backgroundImageView.imageAlpha = (opacity * 2.55f).toInt().coerceIn(0, 255)
                    backgroundImageView.post {
                        backgroundImageView.alpha = opacityFloat
                        backgroundImageView.imageAlpha = (opacity * 2.55f).toInt().coerceIn(0, 255)
                    }
                } else {
                    // Only clear URI if we're certain it's permanently invalid
                    // The validateBackgroundImageUri() method handles clearing invalid URIs
                    Log.w("BackgroundImageHelper", "Background image could not be loaded, but keeping URI for retry")
                    rootLayout.removeView(backgroundImageView)
                }
            } catch (e: Exception) {
                Log.e("BackgroundImageHelper", "Error loading background image: ${e.message}")
                val backgroundImageView = rootLayout.findViewWithTag<ImageView>("home_background")
                if (backgroundImageView != null) {
                    rootLayout.removeView(backgroundImageView)
                }
                // Don't clear URI here - let validateBackgroundImageUri handle permanent failures
                Log.w("BackgroundImageHelper", "Background image load failed, but keeping URI for retry")
            }
        } else {
            val backgroundImageView = rootLayout.findViewWithTag<ImageView>("home_background")
            if (backgroundImageView != null) {
                rootLayout.removeView(backgroundImageView)
            }
        }
    }

    /**
     * Loads background image from cache or original URI, with automatic caching.
     */
    fun loadSafeBackgroundImage(context: Context, prefs: Prefs, uri: Uri): Bitmap? {
        try {
            // First try to load from cache
            val cachedFile = getCachedBackgroundFile(context)
            if (cachedFile != null) {
                Log.d("BackgroundImageHelper", "Loading background from cache: ${cachedFile.absolutePath}")
                val bitmap = loadBitmapFromFile(context, cachedFile.absolutePath)
                if (bitmap != null) {
                    return bitmap
                }
                // If cached file is corrupted, delete it and continue with original URI
                Log.w("BackgroundImageHelper", "Cached background file corrupted, deleting and reloading")
                cachedFile.delete()
            }
            
            // If no cache, try to load from original URI and cache it
            Log.d("BackgroundImageHelper", "No cached background found, attempting to load and cache from URI")
            
            val contentResolver = context.contentResolver
            
            // Check permissions first
            val persistableUris = contentResolver.persistedUriPermissions
            val hasPermission = persistableUris.any { it.uri == uri && it.isReadPermission }
            
            if (!hasPermission) {
                Log.w("BackgroundImageHelper", "No persistent permission for URI: $uri")
                return null
            }
            
            // Try to cache the image first
            val cachedPath = cacheBackgroundImage(context, uri)
            if (cachedPath != null) {
                // Load from the newly cached file
                return loadBitmapFromFile(context, cachedPath)
            }
            
            // If caching failed, try to load directly (fallback)
            Log.w("BackgroundImageHelper", "Caching failed, loading directly from URI")
            return loadBitmapFromUri(context, uri)
            
        } catch (e: Exception) {
            Log.e("BackgroundImageHelper", "Error loading background image: ${e.message}")
            return null
        }
    }
}
