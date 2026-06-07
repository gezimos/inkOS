package com.github.gezimos.inkos.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getTrueSystemFont

@Immutable
data class ReplacementTypography(
    val header: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val item: TextStyle,
    val button: TextStyle,
    val buttonDisabled: TextStyle,
)

@Immutable
data class ReplacementColor(
    val settings: Color,
    val image: Color,
    val selector: Color,
    val border: Color,
    val horizontalPadding: Dp,
)

val LocalReplacementTypography = staticCompositionLocalOf {
    ReplacementTypography(
        header = TextStyle.Default,
        title = TextStyle.Default,
        body = TextStyle.Default,
        item = TextStyle.Default,
        button = TextStyle.Default,
        buttonDisabled = TextStyle.Default,
    )
}
val LocalReplacementColor = staticCompositionLocalOf {
    ReplacementColor(
        settings = Color.Unspecified,
        image = Color.Unspecified,
        selector = Color.Unspecified,
        border = Color.Unspecified,
        horizontalPadding = 0.dp,
    )
}

@Composable
fun SettingsTheme(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { Prefs(context) }

    CompositionLocalProvider(LocalPrefs provides prefs) {
        InkOSTheme {
            val fontFamily = prefs.getFontForContext("settings")
            val customPath = prefs.getCustomFontPathForContext("settings")
            val color = Theme.colors.text
            val settingsSizePref = prefs.settingsSize
            val screenScale = rememberScreenScale()

            val replacementTypography = remember(fontFamily, customPath, settingsSizePref, color, screenScale) {
                val currentFont = fontFamily.getFont(context, customPath) ?: getTrueSystemFont()
                val fontFam = androidx.compose.ui.text.font.FontFamily(currentFont)
                val baseSizeValue = (settingsSizePref - 3).toFloat() * screenScale
                val itemSize = (baseSizeValue * 1.5f).sp
                val sectionSize = (itemSize.value * 0.8f).sp

                ReplacementTypography(
                    header = TextStyle(fontSize = sectionSize, color = color, fontFamily = fontFam),
                    title = TextStyle(fontSize = itemSize, color = color, fontFamily = fontFam),
                    body = TextStyle(fontSize = itemSize, color = color, fontFamily = fontFam),
                    item = TextStyle(fontSize = itemSize, color = color, fontFamily = fontFam),
                    button = TextStyle(fontSize = itemSize, color = color, fontFamily = fontFam),
                    buttonDisabled = TextStyle(fontSize = itemSize, color = color.copy(alpha = 0.5f), fontFamily = fontFam),
                )
            }
            val replacementColor = remember(color, screenScale) {
                ReplacementColor(
                    settings = color,
                    image = color,
                    selector = color,
                    border = color,
                    horizontalPadding = 24.dp.scaled(screenScale)
                )
            }

            CompositionLocalProvider(
                LocalReplacementTypography provides replacementTypography,
                LocalReplacementColor provides replacementColor,
            ) {
                content()
            }
        }
    }
}

object SettingsTheme {
    val typography: ReplacementTypography
        @Composable
        get() = LocalReplacementTypography.current

    val color: ReplacementColor
        @Composable
        get() = LocalReplacementColor.current
}
