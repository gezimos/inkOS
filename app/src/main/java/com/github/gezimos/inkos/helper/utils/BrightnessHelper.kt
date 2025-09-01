package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.widget.Toast
import com.github.gezimos.inkos.data.Prefs
import androidx.core.net.toUri

object BrightnessHelper {
    fun toggleBrightness(context: Context, prefs: Prefs, window: android.view.Window) {
        // Check if we have permission to modify system settings
        if (!android.provider.Settings.System.canWrite(context)) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = ("package:${context.packageName}").toUri()
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(
                context,
                "Please enable 'Modify system settings' for inkOS to use brightness gesture. This is a feature for Mudita Kompakt.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val contentResolver = context.contentResolver

        try {
            // Get current system brightness (0-255)
            val currentSystemBrightness = android.provider.Settings.System.getInt(
                contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )

            // Determine if we're currently dimmed (brightness is 0 or very low)
            val isDimmed = currentSystemBrightness <= 1

            if (isDimmed) {
                // Restore brightness to the last saved non-zero value
                val savedBrightness = prefs.brightnessLevel.coerceIn(20, 255) // Ensure minimum readable brightness

                // Set system brightness
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    savedBrightness
                )

                // Reset window brightness to use system setting
                val params = window.attributes
                params.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params

                Toast.makeText(context, "Brightness restored to $savedBrightness", Toast.LENGTH_SHORT).show()
            } else {
                // ALWAYS save current brightness before dimming (this fixes the caching issue)
                prefs.brightnessLevel = currentSystemBrightness

                // Dim the screen to minimum
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    1  // Minimum brightness (almost off but still visible)
                )

                // Ensure window brightness doesn't override
                val params = window.attributes
                params.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params

                Toast.makeText(context, "Brightness dimmed (saved: $currentSystemBrightness)", Toast.LENGTH_SHORT).show()
            }
    } catch (_: android.provider.Settings.SettingNotFoundException) {
            // Fallback if system brightness can't be read
            Toast.makeText(context, "Brightness control not available", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
            // Handle permission issues
            Toast.makeText(context, "Permission required for brightness control", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Call this method to update the saved brightness level when the user manually changes brightness.
     * This ensures we always restore to the most recent "on" brightness level.
     */
    @Suppress("unused")
    fun updateSavedBrightness(context: Context, prefs: Prefs) {
        try {
            val currentSystemBrightness = android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
            
            // Only save if brightness is above minimum threshold (not dimmed)
            if (currentSystemBrightness >= 20) {
                prefs.brightnessLevel = currentSystemBrightness
            }
        } catch (_: Exception) {
            // Silently ignore if we can't read brightness
        }
    }
}
