package com.github.gezimos.inkos.style

import android.content.Context
import com.github.gezimos.inkos.data.Constants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.gezimos.inkos.data.Prefs

/**
 * Minimal two-color theme bridge.
 * - `text` and `background` are canonical tokens.
 * - Compose consumers read `Theme.colors`.
 * - View code can call `resolveThemeColors(context)` to get ints.
 */
data class AppColors(
    val text: Color,
    val background: Color,
)

private val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        text = Color(0xFF000000),
        background = Color(0xFFFFFFFF),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = Prefs(context)
    // Collect theme prefs as State so Compose recomposes when they change
    val appThemeState = prefs.appThemeFlow.collectAsState(initial = prefs.appTheme)
    val textColorState = prefs.textColorFlow.collectAsState(initial = prefs.textColor)
    val backgroundColorState = prefs.backgroundColorFlow.collectAsState(initial = prefs.backgroundColor)

    val isDark = appThemeState.value == Constants.Theme.Dark
    val text = Color(textColorState.value)
    val background = Color(backgroundColorState.value)

    CompositionLocalProvider(LocalAppColors provides AppColors(text = text, background = background)) {
        content()
    }
}

object Theme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

/**
 * Helper for traditional (View) code to resolve text/background as ARGB ints.
 */
fun resolveThemeColors(context: Context): Pair<Int, Int> {
    val prefs = Prefs(context)
    return Pair(prefs.textColor, prefs.backgroundColor)
}
