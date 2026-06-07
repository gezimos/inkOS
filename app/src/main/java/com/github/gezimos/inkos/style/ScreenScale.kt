package com.github.gezimos.inkos.style

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.data.Prefs

/** UI scale modes. 0 = auto-detect from screen width. */
enum class UiScaleMode(val id: Int, val label: String, val scale: Float) {
    AUTO(0, "", 0f),       // label resolved at runtime to show detected mode
    TINY(1, "Tiny", 0.65f),
    SMALL(2, "Small", 0.75f),
    MEDIUM(3, "Medium", 0.85f),
    NORMAL(4, "Normal", 0.92f),
    BIG(5, "Big", 1.0f),
    LARGE(6, "Large", 1.08f),
    EXTRA_LARGE(7, "Extra Large", 1.16f);

    companion object {
        fun fromId(id: Int): UiScaleMode = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/** Detect the scale mode from screen width (used for auto and for labelling). */
fun detectScaleMode(context: Context): UiScaleMode {
    val widthDp = context.resources.configuration.screenWidthDp
    return when {
        widthDp < 250 -> UiScaleMode.TINY
        widthDp < 300 -> UiScaleMode.SMALL
        widthDp < 360 -> UiScaleMode.MEDIUM
        widthDp < 400 -> UiScaleMode.NORMAL
        widthDp < 600 -> UiScaleMode.BIG
        widthDp < 800 -> UiScaleMode.LARGE
        else -> UiScaleMode.EXTRA_LARGE
    }
}

@Composable
fun rememberScreenScale(): Float {
    val context = LocalContext.current
    val override = Prefs(context).uiScaleMode
    if (override != 0) return UiScaleMode.fromId(override).scale

    val widthDp = LocalConfiguration.current.screenWidthDp
    return remember(widthDp) {
        when {
            widthDp < 250 -> 0.65f
            widthDp < 300 -> 0.75f
            widthDp < 360 -> 0.85f
            widthDp < 400 -> 0.92f
            widthDp < 600 -> 1.0f
            widthDp < 800 -> 1.08f
            else -> 1.16f
        }
    }
}

fun Dp.scaled(scale: Float): Dp = (this.value * scale).dp

fun TextUnit.scaled(scale: Float): TextUnit = (this.value * scale).sp
