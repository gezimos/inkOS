package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.widget.Toast
import com.github.gezimos.inkos.data.Prefs
import androidx.core.net.toUri

object BrightnessHelper {
    fun toggleBrightness(context: Context, prefs: Prefs, window: android.view.Window) {
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

            val isDimmed = currentSystemBrightness <= 1

            if (isDimmed) {
                val savedBrightness = if (prefs.brightnessLevel > 0) {
                    prefs.brightnessLevel.coerceIn(0, 255)
                } else {
                    prefs.lastBrightnessLevel.coerceIn(1, 255)
                }

                // Set system brightness
                android.provider.Settings.System.putInt(
                    contentResolver,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    savedBrightness
                )
                
                // Update prefs to reflect restored brightness
                prefs.brightnessLevel = savedBrightness

                // Reset window brightness to use system setting
                val params = window.attributes
                params.screenBrightness = android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params

                Toast.makeText(context, "Brightness restored to $savedBrightness", Toast.LENGTH_SHORT).show()
            } else {
                if (currentSystemBrightness > 0) {
                    prefs.lastBrightnessLevel = currentSystemBrightness
                }
                prefs.brightnessLevel = 0  // Mark as off in prefs

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
    fun setBrightness(context: Context, prefs: Prefs, level: Int): Boolean {
        if (!android.provider.Settings.System.canWrite(context)) {
            return false
        }
        
        val clampedLevel = level.coerceIn(0, 255)
        return try {
            val writeVal = if (clampedLevel == 0) 1 else clampedLevel
            val success = android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                writeVal
            )
            if (success) {
                prefs.brightnessLevel = clampedLevel
                if (clampedLevel > 0) {
                    prefs.lastBrightnessLevel = clampedLevel
                }
            }
            success
        } catch (_: Exception) {
            false
        }
    }
}
