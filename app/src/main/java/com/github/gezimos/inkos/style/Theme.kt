package com.github.gezimos.inkos.style

import android.content.Context
import com.github.gezimos.inkos.data.Constants
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.gezimos.inkos.data.Prefs

/**
 * Unified Material Design 3 theme for inkOS.
 * - Wraps MaterialTheme with custom two-color system (text/background)
 * - Properly resolves System theme using isSystemInDarkTheme()
 * - Provides reactive theme updates via StateFlows
 * - All Material components respect custom theme colors
 */
data class AppColors(
    val text: Color,
    val background: Color,
)

internal val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        text = Color(0xFF000000),
        background = Color(0xFFFFFFFF),
    )
}

/** Optional Prefs to use for theme; when provided (e.g. by ColorEditorFragment), theme updates react to the same instance. */
val LocalPrefs = staticCompositionLocalOf<Prefs?> { null }

/**
 * Creates a Material 3 ColorScheme for dark theme using custom colors.
 */
private fun createDarkColorScheme(textColor: Color, backgroundColor: Color): ColorScheme {
    return darkColorScheme(
        primary = textColor,
        onPrimary = backgroundColor,
        primaryContainer = backgroundColor,
        onPrimaryContainer = textColor,
        secondary = textColor,
        onSecondary = backgroundColor,
        secondaryContainer = backgroundColor,
        onSecondaryContainer = textColor,
        tertiary = textColor,
        onTertiary = backgroundColor,
        tertiaryContainer = backgroundColor,
        onTertiaryContainer = textColor,
        error = Color(0xFFCF6679), // Material dark error color
        onError = backgroundColor,
        errorContainer = backgroundColor,
        onErrorContainer = Color(0xFFCF6679),
        background = backgroundColor,
        onBackground = textColor,
        surface = backgroundColor,
        onSurface = textColor,
        surfaceVariant = backgroundColor,
        onSurfaceVariant = textColor,
        outline = textColor,
        outlineVariant = textColor.copy(alpha = 0.5f),
        scrim = backgroundColor.copy(alpha = 0.9f),
        inverseSurface = textColor,
        inverseOnSurface = backgroundColor,
        inversePrimary = backgroundColor,
        surfaceTint = textColor.copy(alpha = 0.1f)
    )
}

/**
 * Creates a Material 3 ColorScheme for light theme using custom colors.
 */
private fun createLightColorScheme(textColor: Color, backgroundColor: Color): ColorScheme {
    return lightColorScheme(
        primary = textColor,
        onPrimary = backgroundColor,
        primaryContainer = backgroundColor,
        onPrimaryContainer = textColor,
        secondary = textColor,
        onSecondary = backgroundColor,
        secondaryContainer = backgroundColor,
        onSecondaryContainer = textColor,
        tertiary = textColor,
        onTertiary = backgroundColor,
        tertiaryContainer = backgroundColor,
        onTertiaryContainer = textColor,
        error = Color(0xFFB00020), // Material light error color
        onError = backgroundColor,
        errorContainer = backgroundColor,
        onErrorContainer = Color(0xFFB00020),
        background = backgroundColor,
        onBackground = textColor,
        surface = backgroundColor,
        onSurface = textColor,
        surfaceVariant = backgroundColor,
        onSurfaceVariant = textColor,
        outline = textColor,
        outlineVariant = textColor.copy(alpha = 0.5f),
        scrim = backgroundColor.copy(alpha = 0.9f),
        inverseSurface = textColor,
        inverseOnSurface = backgroundColor,
        inversePrimary = backgroundColor,
        surfaceTint = textColor.copy(alpha = 0.1f)
    )
}

@Composable
fun InkOSTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = LocalPrefs.current ?: Prefs(context)
    
    val appThemeState = prefs.appThemeFlow.collectAsState(initial = prefs.appTheme)
    val textColorState = prefs.textColorFlow.collectAsState(initial = prefs.textColor)
    val backgroundColorState = prefs.backgroundColorFlow.collectAsState(initial = prefs.backgroundColor)
    
    val systemDark = isSystemInDarkTheme()
    
    // Resolve effective theme
    val effectiveTheme = when (appThemeState.value) {
        Constants.Theme.System -> if (systemDark) Constants.Theme.Dark else Constants.Theme.Light
        else -> appThemeState.value
    }
    
    val isDark = effectiveTheme == Constants.Theme.Dark
    val text = Color(textColorState.value)
    val background = Color(backgroundColorState.value)
    
    // Create Material 3 ColorScheme
    val colorScheme = if (isDark) {
        createDarkColorScheme(text, background)
    } else {
        createLightColorScheme(text, background)
    }
    
    CompositionLocalProvider(LocalAppColors provides AppColors(text = text, background = background)) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/**
 * Legacy AppTheme name kept for backward compatibility.
 * Use InkOSTheme for new code.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    InkOSTheme(content = content)
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
