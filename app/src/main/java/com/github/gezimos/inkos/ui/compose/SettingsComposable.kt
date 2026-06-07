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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
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
import com.github.gezimos.inkos.style.LocalPrefs
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled

object SettingsComposable {

    @Composable
    fun tuToDp(textUnit: TextUnit): Dp {
        val density = LocalDensity.current.density
        val scaledDensity = LocalDensity.current.fontScale
        val dpValue = textUnit.value * (density / scaledDensity)
        return dpValue.dp
    }

    @Composable
    fun Modifier.pillHighlight(
        isHighlighted: Boolean,
        inset: Dp = 6.dp,
        color: Color,
        outerHorizontal: Dp = 16.dp,
        outerVertical: Dp = 16.dp,
        textIslandsShape: Int = LocalPrefs.current?.textIslandsShape ?: 0
    ): Modifier {
        val density = LocalDensity.current

        val insetPx = with(density) { inset.toPx() }
        val outerPxH = with(density) { outerHorizontal.toPx() }
        val outerPxV = with(density) { outerVertical.toPx() }
        return this
            .graphicsLayer(clip = false)
            .drawBehind {
                if (isHighlighted) {
                    val drawH = (size.height + outerPxV * 2 - insetPx * 2).coerceAtLeast(0f)
                    val drawW = (size.width + outerPxH * 2 - insetPx * 2).coerceAtLeast(0f)
                    val topLeft = Offset(-outerPxH + insetPx, -outerPxV + insetPx)
                    
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
        val screenScale = rememberScreenScale()
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
                        .padding(horizontal = 2.dp.scaled(screenScale))
                        .size(if (i == currentPage) 12.dp.scaled(screenScale) else 10.dp.scaled(screenScale))
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
        titleFontSize: TextUnit = TextUnit.Unspecified,
        backHighlighted: Boolean = false
    ) {
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        val screenScale = rememberScreenScale()
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(horizontal = SettingsTheme.color.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backFocusRequester = remember { FocusRequester() }
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                colorFilter = ColorFilter.tint(if (backHighlighted) prefBackgroundColor else SettingsTheme.color.image),
                modifier = Modifier
                    .focusRequester(backFocusRequester)
                    .focusable()
                    .pillHighlight(
                        isHighlighted = backHighlighted,
                        color = prefTextColor,
                        outerHorizontal = 8.dp,
                        outerVertical = 8.dp
                    )
                    .clickable(onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    })
                    .size(iconSize.scaled(screenScale))
            )
            LaunchedEffect(Unit) {
                backFocusRequester.requestFocus()
            }

            Spacer(modifier = Modifier.width(16.dp.scaled(screenScale)))

            // Title left-aligned to match setting items
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
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
        @DrawableRes iconRes: Int? = null,
        description: String? = null,
        onClick: () -> Unit = {},
        titleFontSize: TextUnit = TextUnit.Unspecified,
        horizontalPadding: Dp = SettingsTheme.color.horizontalPadding,
        verticalPadding: Dp = 10.dp.scaled(rememberScreenScale()),
        focusVerticalPadding: Dp = 12.dp.scaled(rememberScreenScale()),
        iconPadding: Dp = 0.dp
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        val screenScale = rememberScreenScale()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(vertical = verticalPadding)
                .pillHighlight(isHighlighted, 6.dp, prefTextColor, outerVertical = focusVerticalPadding)
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
            val iconBoxSize = 24.dp.scaled(screenScale)
            if (imageVector != null) {
                Image(
                    imageVector,
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(if (isHighlighted) prefBackgroundColor else SettingsTheme.color.image),
                    modifier = Modifier
                        .size(iconBoxSize)
                        .padding(iconPadding)
                )
                Spacer(
                    modifier = Modifier
                        .width(16.dp.scaled(screenScale))
                )
            } else if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    colorFilter = ColorFilter.tint(if (isHighlighted) prefBackgroundColor else SettingsTheme.color.image),
                    modifier = Modifier
                        .size(iconBoxSize)
                        .padding(iconPadding)
                )
                Spacer(modifier = Modifier.width(16.dp.scaled(screenScale)))
            }
            Column {
                Text(
                    text = title,
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize,
                    color = if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                )
                if (description != null) {
                    val descFontSize = if (titleFontSize.isSpecified) (titleFontSize.value * 0.65f).sp else (SettingsTheme.typography.title.fontSize.value * 0.65f).sp
                    Text(
                        text = description,
                        style = SettingsTheme.typography.title,
                        fontSize = descFontSize,
                        color = if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsTitle(
        text: String,
        modifier: Modifier = Modifier,
        fontSize: TextUnit = TextUnit.Unspecified,
        horizontalPadding: Dp = SettingsTheme.color.horizontalPadding,
        verticalPadding: Dp = 8.dp.scaled(rememberScreenScale())
    ) {
        val effectiveFontSize = if (fontSize.isSpecified) {
            (fontSize.value * 0.8).sp
        } else {
            SettingsTheme.typography.header.fontSize
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "• ${text.uppercase()}",
                style = SettingsTheme.typography.header,
                fontSize = effectiveFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = horizontalPadding)
            )

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
        val screenScale = rememberScreenScale()
        val effectiveFontSize = if (fontSize.isSpecified) {
            (fontSize.value * 0.8).sp
        } else {
            14.sp.scaled(screenScale)
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp.scaled(screenScale)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text.uppercase(),
                style = SettingsTheme.typography.header,
                fontSize = effectiveFontSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }


    @Composable
    fun CustomToggleSwitch(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        tint: Color = SettingsTheme.typography.title.color
    ) {
        val screenScale = rememberScreenScale()
        val circleDiameter = 12.dp.scaled(screenScale)
        val circleBorder = 2.5.dp.scaled(screenScale)
        val lineWidth = 12.dp.scaled(screenScale)
        val lineHeight = 2.5.dp.scaled(screenScale)

        val switchColor = tint

        Row(
            modifier = Modifier
                .clickable {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    onCheckedChange(!checked)
                },
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
    fun AlignmentIcon(alignment: Int, tint: Color = Theme.colors.text, size: Dp = 18.dp) {
        val screenScale = rememberScreenScale()
        val s = size.scaled(screenScale)
        val lineH = 2.5.dp.scaled(screenScale)
        val gap = 3.dp.scaled(screenScale)
        val widths = listOf(1f, 0.5f, 0.85f)
        val hAlign = when (alignment) {
            0 -> Alignment.Start
            2 -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
        Box(modifier = Modifier.size(s), contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalAlignment = hAlign
            ) {
                widths.forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(lineH)
                            .background(tint)
                    )
                }
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
        description: String? = null,
        horizontalPadding: Dp = SettingsTheme.color.horizontalPadding,
        verticalPadding: Dp = 10.dp.scaled(rememberScreenScale()),
        onCheckedChange: (Boolean) -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(vertical = verticalPadding)
                .pillHighlight(isHighlighted, 6.dp, prefTextColor)
                .clickable(
                    enabled = enabled,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                        onCheckedChange(!defaultState)
                    },
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        style = if (fontSize.isSpecified) {
                            SettingsTheme.typography.title.copy(fontSize = fontSize)
                        } else SettingsTheme.typography.title,
                        color = if (!enabled) prefTextColor.copy(alpha = 0.4f) else if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                    )
                    if (description != null) {
                        val descFontSize = if (fontSize.isSpecified) (fontSize.value * 0.65f).sp else (SettingsTheme.typography.title.fontSize.value * 0.65f).sp
                        Text(
                            text = description,
                            style = SettingsTheme.typography.title,
                            fontSize = descFontSize,
                            color = if (!enabled) prefTextColor.copy(alpha = 0.4f) else if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                        )
                    }
                }
                CustomToggleSwitch(
                    checked = defaultState,
                    onCheckedChange = { if (enabled) onCheckedChange(it) },
                    tint = if (!enabled) prefTextColor.copy(alpha = 0.4f) else if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                )
            }
        }
    }

    @Composable
    fun SettingsSelect(
        title: String,
        option: String,
        fontSize: TextUnit = TextUnit.Unspecified, // Default font size for the title
        fontColor: Color = SettingsTheme.typography.title.color,
        enabled: Boolean = true,
        description: String? = null,
        optionAlignment: Int? = null,
        horizontalPadding: Dp = SettingsTheme.color.horizontalPadding,
        verticalPadding: Dp = 10.dp.scaled(rememberScreenScale()),
        onClick: () -> Unit = {},
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(vertical = verticalPadding)
                .pillHighlight(isHighlighted, 6.dp, prefTextColor)
                .clickable(
                    enabled = enabled,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    },
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val textColor = if (!enabled) prefTextColor.copy(alpha = 0.4f) else if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = SettingsTheme.typography.title,
                        fontSize = fontSize,
                        color = textColor
                    )
                    if (description != null) {
                        val descFontSize = if (fontSize.isSpecified) (fontSize.value * 0.65f).sp else (SettingsTheme.typography.title.fontSize.value * 0.65f).sp
                        Text(
                            text = description,
                            style = SettingsTheme.typography.title,
                            fontSize = descFontSize,
                            color = textColor
                        )
                    }
                }

                if (optionAlignment != null) {
                    AlignmentIcon(
                        alignment = optionAlignment,
                        tint = if (!enabled) prefTextColor.copy(alpha = 0.4f) else if (isHighlighted) prefBackgroundColor else prefTextColor
                    )
                } else {
                    Text(
                        text = option,
                        style = SettingsTheme.typography.item,
                        fontSize = fontSize,
                        color = if (!enabled) prefTextColor.copy(alpha = 0.4f) else if (isHighlighted) prefBackgroundColor else fontColor
                    )
                }
            }
        }
    }

    @Composable
    fun SettingsSelectWithDualColorPreview(
        title: String,
        color1: Color,
        color2: Color,
        fontSize: TextUnit = TextUnit.Unspecified,
        enabled: Boolean = true,
        description: String? = null,
        horizontalPadding: Dp = SettingsTheme.color.horizontalPadding,
        verticalPadding: Dp = 10.dp.scaled(rememberScreenScale()),
        onClick: () -> Unit = {},
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(vertical = verticalPadding)
                .pillHighlight(isHighlighted, 6.dp, prefTextColor)
                .clickable(
                    enabled = enabled,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    },
                    interactionSource = interactionSource,
                    indication = null
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = SettingsTheme.typography.title,
                        fontSize = fontSize,
                        color = if (!enabled) prefTextColor.copy(alpha = 0.4f) else if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                    )
                    if (description != null) {
                        val descFontSize = if (fontSize.isSpecified) (fontSize.value * 0.65f).sp else (SettingsTheme.typography.title.fontSize.value * 0.65f).sp
                        Text(
                            text = description,
                            style = SettingsTheme.typography.title,
                            fontSize = descFontSize,
                            color = if (isHighlighted) prefBackgroundColor else SettingsTheme.typography.title.color
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, if (isHighlighted) prefBackgroundColor else prefTextColor, CircleShape)
                            .padding(1.5.dp)
                            .border(1.dp, Color.White, CircleShape)
                            .background(color1, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.5.dp, if (isHighlighted) prefBackgroundColor else prefTextColor, CircleShape)
                            .padding(1.5.dp)
                            .border(1.dp, Color.White, CircleShape)
                            .background(color2, CircleShape)
                    )
                }
            }
        }
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