package com.github.gezimos.inkos.ui.compose


import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.focusable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
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
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme

object SettingsComposable {

    @Composable
    private fun Modifier.pillFocusHighlight(
        isFocused: Boolean,
        inset: Dp = 6.dp,
        color: Color,
        outerHorizontal: Dp = 16.dp,
        outerVertical: Dp = 16.dp
    ): Modifier {
        val density = LocalDensity.current
        val context = LocalContext.current
        val prefs = Prefs(context)
        val textIslandsShape = prefs.textIslandsShape
        
        val insetPx = with(density) { inset.toPx() }
        val outerPxH = with(density) { outerHorizontal.toPx() }
        val outerPxV = with(density) { outerVertical.toPx() }
        return this
            .graphicsLayer(clip = false)
            .drawBehind {
                if (isFocused) {
                    // Draw a shape that extends outerHorizontal/outerVertical outside the element bounds,
                    // and keeps an inner inset from the element edges.
                    // Shape is determined by textIslandsShape preference: 0=Pill, 1=Rounded, 2=Square
                    val drawH = (size.height + outerPxV * 2 - insetPx * 2).coerceAtLeast(0f)
                    val drawW = (size.width + outerPxH * 2 - insetPx * 2).coerceAtLeast(0f)
                    val topLeft = Offset(-outerPxH + insetPx, -outerPxV + insetPx)
                    
                    // Calculate corner radius based on textIslandsShape preference
                    val cornerRadius = ShapeHelper.getCornerRadius(
                        textIslandsShape = textIslandsShape,
                        height = drawH,
                        density = density
                    )
                    
                    drawRoundRect(
                        color = color,
                        topLeft = topLeft,
                        size = Size(drawW, drawH),
                        cornerRadius = cornerRadius
                    )
                }
            }
    }

    @Composable
    fun PageIndicator(
        currentPage: Int,
        pageCount: Int,
        modifier: Modifier = Modifier
    ) {
        val tintColor = SettingsTheme.color.settings
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            for (i in 0 until pageCount) {
                Image(
                    painter = painterResource(id = if (i == currentPage) com.github.gezimos.inkos.R.drawable.ic_current_page else com.github.gezimos.inkos.R.drawable.ic_new_page),
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
                .padding(horizontal = SettingsTheme.color.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back icon: make focusable and highlight like other rows. Request focus on composition
            val prefs = Prefs(LocalContext.current)
            val prefTextColor = Theme.colors.text
            val prefBackgroundColor = Theme.colors.background
            val backFocusRequester = remember { FocusRequester() }
            var backFocused by remember { mutableStateOf(false) }
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                // Do NOT invert/tint the icon when focused; keep consistent icon color
                colorFilter = ColorFilter.tint(SettingsTheme.color.image),
                modifier = Modifier
                    .focusRequester(backFocusRequester)
                        .onFocusChanged { backFocused = it.isFocused }
                        .focusable()
                    .clickable(onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    })
                    .size(iconSize)
            )
            LaunchedEffect(Unit) {
                // Request focus for the header back icon so DPAD navigation lands here immediately
                backFocusRequester.requestFocus()
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title centered
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                SettingsHeaderTitle(
                    text = title,
                    fontSize = titleFontSize,
                )
            }

            // Page indicator right-aligned, no extra padding
            if (pageIndicator != null) {
                pageIndicator()
            }
        }
    }

    @Composable
    fun SettingsHomeItem(
        title: String,
        imageVector: ImageVector? = null,
        onClick: () -> Unit = {},
        titleFontSize: TextUnit = TextUnit.Unspecified,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val prefs = Prefs(LocalContext.current)
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsTheme.color.horizontalPadding)
                .padding(vertical = 16.dp)
                .pillFocusHighlight(isFocused, 6.dp, prefTextColor)
                .clickable(
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    },
                    interactionSource = interactionSource,
                    indication = null
                ),
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
                modifier = Modifier.weight(1f),
                color = if (isFocused) prefBackgroundColor else SettingsTheme.typography.title.color
            )
            // Chevron right icon
            Image(
                painter = painterResource(id = com.github.gezimos.inkos.R.drawable.ic_chevron_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (isFocused) prefBackgroundColor else SettingsTheme.color.image),
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
                text = "• ${text.uppercase()}",
                style = SettingsTheme.typography.header,
                fontSize = effectiveFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = SettingsTheme.color.horizontalPadding)
            )

            // Dashed separator to the right of the title that fills remaining space
            Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                DashedSeparator()
            }
        }
    }

    @Composable
    fun SettingsHeaderTitle(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: TextUnit = TextUnit.Unspecified
    ) {
        // Same height as SettingsSwitch/Select but without the bullet prefix
        val effectiveFontSize = if (fontSize.isSpecified) {
            (fontSize.value * 0.8).sp
        } else {
            14.sp // fallback to a small size
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
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
    fun CustomToggleSwitch(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        tint: Color = SettingsTheme.typography.title.color
    ) {
        val circleDiameter = 9.8.dp
        val circleBorder = 2.5.dp
        val lineWidth = 14.5.dp
        val lineHeight = 2.22.dp

        val switchColor = tint

        Row(
            modifier = Modifier
                .clickable {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                    onCheckedChange(!checked)
                }
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
        val prefs = Prefs(LocalContext.current)
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsTheme.color.horizontalPadding)
                .padding(vertical = 16.dp)
                .pillFocusHighlight(isFocused, 6.dp, prefTextColor)
                .clickable(
                    enabled = enabled,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                        onCheckedChange(!defaultState)
                    },
                    interactionSource = interactionSource,
                    indication = null
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = if (fontSize.isSpecified) {
                    SettingsTheme.typography.title.copy(fontSize = fontSize)
                } else SettingsTheme.typography.title,
                modifier = Modifier
                    .weight(1f),
                color = if (!enabled) prefTextColor.copy(alpha = 0.25f) else if (isFocused) prefBackgroundColor else SettingsTheme.typography.title.color
            )
            CustomToggleSwitch(
                checked = defaultState,
                onCheckedChange = { onCheckedChange(it) },
                tint = if (isFocused) prefBackgroundColor else SettingsTheme.typography.title.color
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
        val prefs = Prefs(LocalContext.current)
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsTheme.color.horizontalPadding)
                .padding(vertical = 16.dp)
                .pillFocusHighlight(isFocused, 6.dp, prefTextColor)
                .clickable(
                    enabled = enabled,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    },
                    interactionSource = interactionSource,
                    indication = null
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                modifier = Modifier.weight(1f),
                color = if (isFocused) prefBackgroundColor else SettingsTheme.typography.title.color
            )

            Text(
                text = option,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                color = if (!enabled) prefTextColor.copy(alpha = 0.25f) else if (isFocused) prefBackgroundColor else fontColor
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
        val prefs = Prefs(LocalContext.current)
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SettingsTheme.color.horizontalPadding)
                .padding(vertical = 12.dp)
                .pillFocusHighlight(isFocused, 6.dp, prefTextColor)
                .clickable(
                    enabled = enabled,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    },
                    interactionSource = interactionSource,
                    indication = null
                )
            ,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = SettingsTheme.typography.title,
                fontSize = fontSize,
                modifier = Modifier.weight(1f),
                color = if (!enabled) prefTextColor.copy(alpha = 0.25f) else if (isFocused) prefBackgroundColor else SettingsTheme.typography.title.color
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
                    color = if (!enabled) prefTextColor.copy(alpha = 0.25f) else if (isFocused) prefBackgroundColor else SettingsTheme.typography.title.color
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
    fun SolidSeparator() {
        val borderColor = SettingsTheme.color.border
        androidx.compose.material.Divider(
            modifier = Modifier
                .fillMaxWidth(),
            color = borderColor,
            thickness = 3.dp
        )
    }

    @Composable
    fun DashedSeparator() {
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