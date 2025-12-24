package com.github.gezimos.inkos.helper

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility for creating shapes based on textIslandsShape preference.
 * 
 * textIslandsShape values:
 * - 0: Pill/Circle (fully rounded)
 * - 1: Rounded (8.dp corners)
 * - 2: Square (0.dp corners)
 */
object ShapeHelper {
    
    /**
     * Returns a RoundedCornerShape based on textIslandsShape preference.
     * 
     * @param textIslandsShape The shape preference (0=Pill, 1=Rounded, 2=Square)
     * @param pillRadius The corner radius to use for pill mode (default: 50.dp for fully rounded)
     * @param roundedRadius The corner radius to use for rounded mode (default: 8.dp)
     * @return RoundedCornerShape with appropriate corner radius
     */
    fun getRoundedCornerShape(
        textIslandsShape: Int,
        pillRadius: Dp = 50.dp,
        roundedRadius: Dp = 8.dp
    ): RoundedCornerShape {
        return when (textIslandsShape) {
            0 -> RoundedCornerShape(pillRadius) // Pill (fully rounded)
            1 -> RoundedCornerShape(roundedRadius) // Rounded
            else -> RoundedCornerShape(0.dp) // Square
        }
    }
    
    /**
     * Returns CircleShape for pill mode, or RoundedCornerShape for rounded/square modes.
     * Useful when pill mode should be a perfect circle.
     * 
     * @param textIslandsShape The shape preference (0=Pill, 1=Rounded, 2=Square)
     * @param roundedRadius The corner radius to use for rounded mode (default: 8.dp)
     * @return CircleShape for pill, RoundedCornerShape for others
     */
    @Suppress("UNCHECKED_CAST")
    fun getShape(
        textIslandsShape: Int,
        roundedRadius: Dp = 8.dp
    ): Any {
        return when (textIslandsShape) {
            0 -> CircleShape // Pill (fully rounded, perfect circle)
            1 -> RoundedCornerShape(roundedRadius) // Rounded
            else -> RoundedCornerShape(0.dp) // Square
        }
    }
    
    /**
     * Returns CornerRadius for drawBehind operations based on textIslandsShape preference.
     * 
     * @param textIslandsShape The shape preference (0=Pill, 1=Rounded, 2=Square)
     * @param height The height of the element (for pill mode, uses height/2)
     * @param density Density for converting dp to pixels
     * @param roundedRadiusDp The corner radius in dp for rounded mode (default: 8.dp)
     * @return CornerRadius with appropriate values
     */
    fun getCornerRadius(
        textIslandsShape: Int,
        height: Float,
        density: androidx.compose.ui.unit.Density,
        roundedRadiusDp: Dp = 8.dp
    ): CornerRadius {
        return when (textIslandsShape) {
            0 -> CornerRadius(height / 2f, height / 2f) // Pill (fully rounded)
            1 -> {
                val radiusPx = with(density) { roundedRadiusDp.toPx() }
                CornerRadius(radiusPx, radiusPx) // Rounded
            }
            else -> CornerRadius(0f, 0f) // Square
        }
    }
    
    /**
     * Returns corner radius in pixels for GradientDrawable operations.
     * Used for Android View-based buttons in dialogs.
     * 
     * @param textIslandsShape The shape preference (0=Pill, 1=Rounded, 2=Square)
     * @param density Display density for converting dp to pixels
     * @param pillRadiusDp The corner radius in dp for pill mode (default: 50.dp)
     * @param roundedRadiusDp The corner radius in dp for rounded mode (default: 8.dp)
     * @return Corner radius in pixels
     */
    fun getCornerRadiusPx(
        textIslandsShape: Int,
        density: Float,
        pillRadiusDp: Float = 50f,
        roundedRadiusDp: Float = 8f
    ): Float {
        return when (textIslandsShape) {
            0 -> pillRadiusDp * density // Pill (fully rounded)
            1 -> roundedRadiusDp * density // Rounded
            else -> 0f * density // Square
        }
    }
    
}
