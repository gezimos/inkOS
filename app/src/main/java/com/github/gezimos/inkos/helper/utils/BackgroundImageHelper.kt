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

object BackgroundImageHelper {
    private var lastFailedUri: String? = null
    private var failureCount = 0
    private const val MAX_RETRIES = 3
    
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
     * Safely loads a background image with auto-optimization to prevent crashes.
     * Returns null if image cannot be safely loaded.
     */
    fun loadSafeBackgroundImage(context: Context, prefs: Prefs, uri: Uri): Bitmap? {
        try {
            val contentResolver = context.contentResolver
            
            // Check if we still have permission to access this URI
            val persistableUris = contentResolver.persistedUriPermissions
            val hasPermission = persistableUris.any { it.uri == uri && it.isReadPermission }
            
            if (!hasPermission) {
                Log.w("BackgroundImageHelper", "No persistent permission for URI during load: $uri")
                return null
            }
            
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
            val estimatedWidth = originalWidth / inSampleSize
            val estimatedHeight = originalHeight / inSampleSize
            val estimatedMemory = estimatedWidth * estimatedHeight * 2
            val maxSafeMemory = 50 * 1024 * 1024
            while (estimatedMemory > maxSafeMemory && inSampleSize < 32) {
                inSampleSize *= 2
            }
            val loadOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            return contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, loadOptions)
            }
        } catch (e: SecurityException) {
            Log.e("BackgroundImageHelper", "Security exception loading background image: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e("BackgroundImageHelper", "Error loading background image: ${e.message}")
            return null
        }
    }
}
