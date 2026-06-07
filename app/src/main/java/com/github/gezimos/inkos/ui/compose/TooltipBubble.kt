package com.github.gezimos.inkos.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
@Composable
fun OneTimeTooltip(
    key: String,
    title: String,
    lines: List<String>,
    icon: ImageVector? = null,
    animFrames: List<Int> = emptyList(),
    trigger: Boolean = true,
    alignment: Alignment = Alignment.Center
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger && !prefs.isTooltipShown(key)) {
            show = true
        }
    }

    if (show) {
        TooltipBubble(title = title, lines = lines, icon = icon, animFrames = animFrames, alignment = alignment) {
            prefs.markTooltipShown(key)
            show = false
        }
    }
}
@Composable
fun TooltipBubble(
    title: String,
    lines: List<String>,
    icon: ImageVector? = null,
    animFrames: List<Int> = emptyList(),
    alignment: Alignment = Alignment.Center,
    nextLabel: String? = null,
    onNext: (() -> Unit)? = null,
    minContentHeight: Dp = 0.dp,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        visible = true
    }
    if (!visible) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            try {
                val window = view.rootView?.layoutParams?.let { params ->
                    if (params is android.view.WindowManager.LayoutParams) {
                        params.dimAmount = 0f
                        params.flags = params.flags or android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
                        (view.context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager)
                            .updateViewLayout(view.rootView, params)
                    }
                }
            } catch (_: Exception) {}
        }

        val context = LocalContext.current
        val prefs = remember { Prefs(context) }
        val screenScale = rememberScreenScale()
        val textColor = Theme.colors.text
        val bgColor = Theme.colors.background
        val shape = ShapeHelper.getRoundedCornerShape(
            textIslandsShape = prefs.textIslandsShape,
            pillRadius = 16.dp,
            roundedRadius = 8.dp
        )
        val closeShape = ShapeHelper.getRoundedCornerShape(
            textIslandsShape = prefs.textIslandsShape,
            pillRadius = 50.dp,
            roundedRadius = 6.dp
        )
        val closeSize = 24.dp.scaled(screenScale)
        val borderWidth = 1.5.dp.scaled(screenScale)

        // Tap-to-dismiss layer + bubble
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = alignment
        ) {
            // Wrapper for bubble + overlapping X button
            Box(contentAlignment = Alignment.TopEnd) {
                // Bubble
                Column(
                    modifier = Modifier
                        .width(300.dp.scaled(screenScale))
                        .padding(top = closeSize / 2) // space for X overlap
                        .clip(shape)
                        .border(borderWidth, textColor, shape)
                        .background(bgColor)
                        .padding(horizontal = 16.dp.scaled(screenScale), vertical = 14.dp.scaled(screenScale))
                ) {
                    // Title
                    Text(
                        text = title.uppercase(),
                        style = SettingsTheme.typography.header,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(8.dp.scaled(screenScale)))

                    Row(
                        modifier = if (minContentHeight > 0.dp) Modifier.heightIn(min = minContentHeight.scaled(screenScale)) else Modifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (animFrames.isNotEmpty() || icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp.scaled(screenScale))
                                    .padding(end = 10.dp.scaled(screenScale)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (animFrames.isNotEmpty()) {
                                    var frame by remember { mutableIntStateOf(0) }
                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            kotlinx.coroutines.delay(600)
                                            frame = (frame + 1) % animFrames.size
                                        }
                                    }
                                    Image(
                                        painter = painterResource(animFrames[frame]),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(textColor),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(32.dp.scaled(screenScale))
                                    )
                                }
                            }
                        }
                        Column {
                            lines.forEach { line ->
                                Text(
                                    text = line,
                                    style = SettingsTheme.typography.item,
                                    fontSize = (SettingsTheme.typography.item.fontSize.value * 0.85f).sp,
                                    color = textColor,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    if (nextLabel != null && onNext != null) {
                        Spacer(modifier = Modifier.height(10.dp.scaled(screenScale)))
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .clip(closeShape)
                                .border(borderWidth, textColor, closeShape)
                                .background(bgColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onNext() }
                                .padding(horizontal = 14.dp.scaled(screenScale), vertical = 6.dp.scaled(screenScale))
                        ) {
                            Text(
                                text = nextLabel.uppercase(),
                                style = SettingsTheme.typography.item,
                                fontSize = (SettingsTheme.typography.item.fontSize.value * 0.8f).sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = -(8.dp.scaled(screenScale)))
                        .size(closeSize)
                        .clip(closeShape)
                        .background(bgColor)
                        .border(borderWidth, textColor, closeShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = textColor,
                        modifier = Modifier.size(14.dp.scaled(screenScale))
                    )
                }
            }
        }
    }
}
