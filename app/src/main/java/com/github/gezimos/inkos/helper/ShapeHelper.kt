package com.github.gezimos.inkos.helper

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
object ShapeHelper {
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
