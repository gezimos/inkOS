package com.github.gezimos.inkos.helper

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
enum class IconShape(val id: Int) {
    PILL(0),
    ROUNDED(1),
    SQUARE(2);

    companion object {
        fun fromPreference(value: Int): IconShape =
            entries.firstOrNull { it.id == value } ?: PILL
    }
}
object IconShapeUtility {
    fun getComposeShape(iconShape: IconShape, pillRadius: Dp = 50.dp): Shape =
        when (iconShape) {
            IconShape.PILL    -> RoundedCornerShape(pillRadius)
            IconShape.ROUNDED -> RoundedCornerShape(8.dp)
            IconShape.SQUARE  -> RoundedCornerShape(0.dp)
        }
    fun getCornerRadius(
        iconShape: IconShape,
        heightPx: Float,
        density: Density
    ): CornerRadius = when (iconShape) {
        IconShape.PILL    -> CornerRadius(heightPx / 2f, heightPx / 2f)
        IconShape.ROUNDED -> {
            val radiusPx = with(density) { 8.dp.toPx() }
            CornerRadius(radiusPx, radiusPx)
        }
        IconShape.SQUARE  -> CornerRadius(0f, 0f)
    }
    fun borderWidthForMode(iconSourceMode: Int): Dp? = when (iconSourceMode) {
        0    -> 2.dp
        4    -> 1.dp
        else -> null
    }
    fun shouldClipBitmap(iconSourceMode: Int): Boolean = iconSourceMode in intArrayOf(2, 3, 4)

    fun isTintedMode(iconSourceMode: Int): Boolean = iconSourceMode == 4 || iconSourceMode == 5 || iconSourceMode == 6
    fun isInkOsMode(iconSourceMode: Int): Boolean = iconSourceMode == 4
    fun isMinimalMode(iconSourceMode: Int): Boolean = iconSourceMode == 5
    fun isFilledMode(iconSourceMode: Int): Boolean = iconSourceMode == 6
    fun isFullBleedMode(iconSourceMode: Int): Boolean = iconSourceMode == 5 || iconSourceMode == 6
}
