package com.github.gezimos.inkos.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.ColorFilter
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val azSidebarWidth = 20.dp

@Composable
fun AZSidebar(
    modifier: Modifier = Modifier,
    edgeWidth: Dp = 20.dp,
    letters: List<Char> = listOf('★') + (('A')..('Z')).toList(),
    /** Filled circle color for the active letter */
    appTextColor: Color = Theme.colors.text,
    /** When provided, match the parent apps container height (in pixels). */
    containerHeightPx: Int = 0,
    /** Optional currently-selected letter (null means star / all apps) */
    selectedLetter: Char? = null,
    onLetterSelected: (String) -> Unit,
    onTouchStart: (() -> Unit)? = null,
    /** Called when touch ends. Passes the final selected letter (or null when none). */
    onTouchEnd: ((String?) -> Unit)? = null,
    /** DPAD navigation: external control of selected index */
    dpadSelectedIndex: Int? = null,
    /** DPAD navigation: callback when DPAD changes selection */
    onDpadIndexChange: ((Int) -> Unit)? = null,
    /** Shape preference for the indicator (0=Pill, 1=Rounded, 2=Square) */
    textIslandsShape: Int = 0
) {
    var heightPx by remember { mutableStateOf(0f) }
    // Initialize selected index based on provided `selectedLetter` (null -> star)
    val initialIdx = remember(letters, selectedLetter) {
        val target = selectedLetter ?: '★'
        val idx = letters.indexOf(target).coerceAtLeast(0)
        idx.coerceIn(0, maxOf(letters.size - 1, 0))
    }
    // Use external DPAD index if provided, otherwise use internal state
    var internalSelectedIdx by remember { mutableStateOf(initialIdx) }
    val selectedIdx = dpadSelectedIndex ?: internalSelectedIdx
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .width(edgeWidth)
            .then(if (containerHeightPx > 0) Modifier.height(with(density) { containerHeightPx.toDp() }) else Modifier.fillMaxHeight())
            .onSizeChanged { heightPx = it.height.toFloat() }
            .pointerInput(letters) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onTouchStart?.invoke()
                    // capture local coordinates relative to this layout
                    val initial = down.position
                    if (heightPx <= 0f) {
                        // wait for layout to record size
                    }
                    fun indexForY(y: Float): Int {
                        val itemH = if (heightPx > 0f) heightPx / letters.size else 0f
                        return if (itemH > 0f) ((y / itemH).toInt()).coerceIn(0, letters.size - 1) else 0
                    }

                    // immediately report initial
                    var lastIdx = indexForY(initial.y)
                    internalSelectedIdx = lastIdx
                    try { onLetterSelected(letters[lastIdx].toString()) } catch (_: Exception) {}

                    do {
                        val event = awaitPointerEvent()
                        // End gesture when released or when no pointers are pressed
                        if (event.type == PointerEventType.Release || event.changes.none { it.pressed }) break
                        val pos = event.changes.firstOrNull()?.position ?: Offset.Zero
                        val idx = indexForY(pos.y)
                        if (idx != lastIdx) {
                            lastIdx = idx
                            internalSelectedIdx = idx
                            try { onLetterSelected(letters[idx].toString()) } catch (_: Exception) {}
                        }
                        event.changes.forEach { it.consumePositionChange() }
                    } while (true)

                    // report final selected letter (or null if none)
                    val finalLetter = letters.getOrNull(lastIdx)?.toString()
                    onTouchEnd?.invoke(finalLetter)
                }
            }
            .background(Color.Transparent)
            , contentAlignment = Alignment.Center
    ) {
        // Height of the active indicator (was `circleSize` previously)
        val indicatorHeight = 20.dp
        val pillWidth = 20.dp
        val activeTextSize = 16.sp
        val inactiveTextSize = 14.sp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            // Use SpaceBetween to distribute letters evenly regardless of measured height
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            letters.forEachIndexed { idx, ch ->
                val isActive = idx == selectedIdx
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (isActive) {
                        val fill = appTextColor
                        // Per theme rule: active fill = text token, glyph/letter = background token
                        val letterColor = Theme.colors.background
                        // Calculate shape based on textIslandsShape preference
                        val indicatorShape = remember(textIslandsShape) {
                            when (textIslandsShape) {
                                0 -> CircleShape
                                1 -> RoundedCornerShape(8.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                        }
                        // Active indicator with shape styling (wider than tall)
                        Box(
                            modifier = Modifier.size(width = pillWidth, height = indicatorHeight).background(fill, shape = indicatorShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ch == '★') {
                                Image(
                                    painter = painterResource(R.drawable.ic_foreground),
                                    contentDescription = "star",
                                    modifier = Modifier.size(indicatorHeight.times(0.5f)),
                                    colorFilter = ColorFilter.tint(letterColor)
                                )
                            } else {
                                BasicText(
                                    text = ch.toString(),
                                    style = androidx.compose.ui.text.TextStyle(
                                        color = letterColor,
                                        fontSize = activeTextSize,
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    } else {
                        if (ch == '★') {
                            Image(
                                painter = painterResource(R.drawable.ic_foreground),
                                contentDescription = "star",
                                modifier = Modifier.size(indicatorHeight.times(0.4f)),
                                colorFilter = ColorFilter.tint(appTextColor)
                            )
                        } else {
                            BasicText(
                                text = ch.toString(),
                                style = androidx.compose.ui.text.TextStyle(
                                    color = appTextColor,
                                    fontSize = inactiveTextSize,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
