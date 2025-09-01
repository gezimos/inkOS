package com.github.gezimos.inkos.ui.compose


import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.style.SettingsTheme

object SettingsComposable {

    @Composable
    fun PageIndicator(
        currentPage: Int,
        pageCount: Int,
        modifier: Modifier = Modifier,
        titleFontSize: TextUnit = TextUnit.Unspecified
    ) {
        // Use the current theme to determine dark mode
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val activeRes = com.github.gezimos.inkos.R.drawable.ic_current_page
        val inactiveRes = com.github.gezimos.inkos.R.drawable.ic_new_page
        val tintColor = if (isDark) Color.White else Color.Black
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            for (i in 0 until pageCount) {
                Image(
                    painter = painterResource(id = if (i == currentPage) activeRes else inactiveRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(tintColor),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (i == currentPage) 12.dp else 10.dp)
                )
            }
        }
    }

    @Composable
    fun PageHeader(
        @DrawableRes iconRes: Int,
        title: String,
        iconSize: Dp = 24.dp,
        onClick: () -> Unit = {},
        showStatusBar: Boolean = false,
        modifier: Modifier = Modifier,
        pageIndicator: (@Composable () -> Unit)? = null,
        titleFontSize: TextUnit = TextUnit.Unspecified // Use titleFontSize
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = if (showStatusBar) 36.dp else 12.dp)
                .padding(bottom = 12.dp)
                .padding(horizontal = SettingsTheme.color.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back icon
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                colorFilter = ColorFilter.tint(SettingsTheme.color.image),
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .size(iconSize)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Title centered
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                SettingsTitle(
                    text = title,
                    fontSize = titleFontSize,
                )
            }

            // Page indicator right-aligned, no extra padding
            if (pageIndicator != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    pageIndicator()
                }
            }
        }
    }

    @Composable
    fun TopMainHeader(
        @DrawableRes iconRes: Int,
        title: String,
        iconSize: Dp = 96.dp, // Default size for the icon
        fontSize: TextUnit = 24.sp, // Default font size for the title
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsTheme.color.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image Icon
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier
                    .size(iconSize)
                    .padding(bottom = 16.dp) // Bottom margin like in XML
            )

            // Title Text
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                modifier = Modifier
                    .padding(bottom = 24.dp)
            )
        }
    }

    @Composable
    fun SettingsHomeItem(
        title: String,
        imageVector: ImageVector? = null,
        onClick: () -> Unit = {},
        titleFontSize: TextUnit = TextUnit.Unspecified,
        iconSize: Dp = 18.dp,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFocused) Modifier.background(focusColor) else Modifier
                )
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
                .padding(horizontal = SettingsTheme.color.horizontalPadding)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageVector != null) {
                Image(
                    imageVector,
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(SettingsTheme.color.image),
                    modifier = Modifier
                        .size(24.dp)
                )
                Spacer(
                    modifier = Modifier
                        .width(16.dp)
                )
            }
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            // Chevron right icon
            Image(
                painter = painterResource(id = com.github.gezimos.inkos.R.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(SettingsTheme.color.image),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    @Composable
    fun SettingsTitle(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: TextUnit = TextUnit.Unspecified
    ) {
        // Make SettingsTitle same height as SettingsSwitch/Select and capitalize text
        val effectiveFontSize = if (fontSize.isSpecified) {
            (fontSize.value * 0.8).sp
        } else {
            14.sp // fallback to a small size
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp), // changed from 24.dp to 16.dp
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text.uppercase(),
                style = SettingsTheme.typography.header,
                fontSize = effectiveFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = SettingsTheme.color.horizontalPadding)
            )
        }
    }

    @Composable
    fun SettingsItem(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: TextUnit = TextUnit.Unspecified,
        fontColor: Color = SettingsTheme.typography.title.color
    ) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            fontSize = if (fontSize.isSpecified) fontSize else SettingsTheme.typography.title.fontSize,
            color = fontColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsTheme.color.horizontalPadding)
                .padding(vertical = 12.dp)
        )
    }

    @Composable
    fun CustomToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        val circleDiameter = 9.8.dp
        val circleBorder = 2.5.dp
        val lineWidth = 14.5.dp
        val lineHeight = 2.22.dp

        val switchColor = SettingsTheme.typography.title.color

        Row(
            modifier = Modifier
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!checked) {
                Box(
                    modifier = Modifier
                        .size(circleDiameter)
                        .border(circleBorder, switchColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .width(lineWidth)
                        .height(lineHeight)
                        .background(switchColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(lineWidth)
                        .height(lineHeight)
                        .background(switchColor)
                )
                Box(
                    modifier = Modifier
                        .size(circleDiameter)
                        .background(switchColor, CircleShape)
                )
            }
        }
    }

    @Composable
    fun SettingsSwitch(
        text: String,
        fontSize: TextUnit = TextUnit.Unspecified,
        defaultState: Boolean = false,
        enabled: Boolean = true,
        modifier: Modifier = Modifier,
        onCheckedChange: (Boolean) -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (isFocused) Modifier.background(focusColor) else Modifier
                )
                .clickable(
                    enabled = enabled,
                    onClick = { onCheckedChange(!defaultState) },
                    interactionSource = interactionSource,
                    indication = null
                )
                .padding(horizontal = SettingsTheme.color.horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = if (fontSize.isSpecified) {
                    SettingsTheme.typography.title.copy(fontSize = fontSize)
                } else SettingsTheme.typography.title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp), // vertical padding only on text
                color = if (enabled) {
                    SettingsTheme.typography.title.color
                } else Color.Gray
            )
            CustomToggleSwitch(
                checked = defaultState,
                onCheckedChange = { onCheckedChange(it) }
            )
        }
    }

    @Composable
    fun SettingsSelect(
        title: String,
        option: String,
        fontSize: TextUnit = 24.sp, // Default font size for the title
        fontColor: Color = SettingsTheme.typography.title.color,
        enabled: Boolean = true,
        onClick: () -> Unit = {},
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFocused) Modifier.background(focusColor) else Modifier
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
                .padding(vertical = 16.dp)
                .padding(horizontal = SettingsTheme.color.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                modifier = Modifier.weight(1f),
                color = if (enabled) SettingsTheme.typography.title.color else Color.Gray
            )

            Text(
                text = option,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                color = if (enabled) fontColor else Color.Gray
            )
        }
    }

    @Composable
    fun SettingsSelectWithColorPreview(
        title: String,
        hexColor: String,
        previewColor: Color,
        fontSize: TextUnit = 24.sp,
        enabled: Boolean = true,
        onClick: () -> Unit = {},
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isDark = androidx.compose.foundation.isSystemInDarkTheme()
        val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFocused) Modifier.background(focusColor) else Modifier
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null
                )
                .padding(vertical = 12.dp)
                .padding(horizontal = SettingsTheme.color.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                modifier = Modifier.weight(1f),
                color = if (enabled) SettingsTheme.typography.title.color else Color.Gray
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
            ) {
                Text(
                    text = hexColor,
                    style = SettingsTheme.typography.title,
                    fontSize = fontSize,
                    color = if (enabled) SettingsTheme.typography.title.color else Color.Gray
                )

                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 1.dp,
                            color = SettingsTheme.color.border,
                            shape = CircleShape
                        )
                ) {
                    drawCircle(color = previewColor)
                }
            }
        }
    }

    @Composable
    fun FullLineSeparator(isDark: Boolean) {
        val borderColor = SettingsTheme.color.border
        androidx.compose.material.Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsTheme.color.horizontalPadding), // changed
            color = borderColor,
            thickness = 2.dp
        )
    }

    @Composable
    fun SolidSeparator(isDark: Boolean) {
        val borderColor = SettingsTheme.color.border
        androidx.compose.material.Divider(
            modifier = Modifier
                .fillMaxWidth(),
            color = borderColor,
            thickness = 3.dp
        )
    }

    @Composable
    fun DashedSeparator(isDark: Boolean) {
        val borderColor = SettingsTheme.color.border
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .alpha(0.85f)
                .padding(horizontal = SettingsTheme.color.horizontalPadding) // already present
        ) {
            val dashWidth = 4f
            val gapWidth = 4f
            var x = 0f
            val y = size.height / 2
            while (x < size.width) {
                drawLine(
                    color = borderColor,
                    start = Offset(x, y),
                    end = Offset((x + dashWidth).coerceAtMost(size.width), y),
                    strokeWidth = size.height
                )
                x += dashWidth + gapWidth
            }
        }
    }
}