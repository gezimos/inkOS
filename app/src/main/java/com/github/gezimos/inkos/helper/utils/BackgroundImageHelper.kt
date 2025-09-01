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
                    prefs.homeBackgroundImageUri = null
                    rootLayout.removeView(backgroundImageView)
                    context.showShortToast("Background image could not be loaded and was cleared")
                }
            } catch (e: Exception) {
                Log.e("BackgroundImageHelper", "Error loading background image: ${e.message}")
                prefs.homeBackgroundImageUri = null
                val backgroundImageView = rootLayout.findViewWithTag<ImageView>("home_background")
                if (backgroundImageView != null) {
                    rootLayout.removeView(backgroundImageView)
                }
                context.showShortToast("Background image error - cleared to prevent crashes")
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
     * Returns null if image cannot be safely loaded (and clears the URI to prevent crash loops).
     */
    fun loadSafeBackgroundImage(context: Context, prefs: Prefs, uri: Uri): Bitmap? {
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
                prefs.homeBackgroundImageUri = null
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
        } catch (e: Exception) {
            Log.e("BackgroundImageHelper", "Error in loadSafeBackgroundImage: ${e.message}")
            prefs.homeBackgroundImageUri = null
            return null
        }
    }
}
