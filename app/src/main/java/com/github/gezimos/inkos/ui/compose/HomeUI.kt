package com.github.gezimos.inkos.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import java.text.DateFormatSymbols
import java.util.Locale
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import com.github.gezimos.inkos.style.Theme
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.HomeAppUiState
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.services.NotificationManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

@Composable
fun HomeUI(
    state: HomeUiRenderState,
    callbacks: HomeUiCallbacks,
    selectedAppId: Int? = null,
    showDpadMode: Boolean = false,
    dpadActivatedAppId: Int? = null,
    onDpadActivatedHandled: (Int) -> Unit = {},
    onHomeAppsBoundsChanged: (Rect?) -> Unit = {},
    focusZone: FocusZone? = null,
    selectedMediaButton: Int? = null
) {
    val alignment = when (state.homeAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    // backgroundOpacity is stored as an Int; clamp to 0..255 and convert to 0..1 alpha here.
    val bgAlpha = (state.backgroundOpacity.coerceIn(0, 255).toFloat() / 255f)
    
    // Track app bounds, header bounds, bottom widget bounds, and screen size to constrain margins
    val density = LocalDensity.current
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    var appBounds by remember { mutableStateOf<Rect?>(null) }
    var headerBounds by remember { mutableStateOf<Rect?>(null) }
    var bottomWidgetBounds by remember { mutableStateOf<Rect?>(null) }
    
    // Get status bar height when visible
    val statusBarPadding = if (prefs.showStatusBar) {
        WindowInsets.statusBars.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    } else {
        0.dp
    }
    val statusBarHeightPx = with(density) { statusBarPadding.toPx() }
    
    // No constraints: use raw top/bottom paddings from prefs/state to avoid layout jumps
    val topOffset = state.topWidgetMargin.dp + statusBarPadding

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.colors.background.copy(alpha = bgAlpha))
    ) {
        // Header pinned to top (uses topWidgetMargin) so it doesn't push the centered apps
        val headerAlign = when (state.homeAlignment) {
            0 -> Alignment.TopStart
            2 -> Alignment.TopEnd
            else -> Alignment.TopCenter
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(headerAlign)
                .offset(y = topOffset)
                .padding(horizontal = 32.dp)
        ) {
            HomeHeader(
                state = state,
                onClockClick = callbacks.onClockClick,
                onDateClick = callbacks.onDateClick,
                onBatteryClick = callbacks.onBatteryClick,
                onNotificationCountClick = callbacks.onNotificationCountClick,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    // Measure header content height (bounds relative to Box parent, so top should be ~0)
                    // We only need the height for constraint calculation
                    val bounds = coordinates.boundsInParent()
                    headerBounds = Rect(
                        offset = Offset.Zero,
                        size = Size(bounds.width, bounds.height)
                    )
                },
                isClockFocused = showDpadMode && focusZone == FocusZone.CLOCK,
                isDateFocused = showDpadMode && focusZone == FocusZone.DATE,
                showTextIslands = state.textIslands
            )
        }

        // Centered area for apps and page indicator (not affected by topWidgetMargin)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            if (!state.hideHomeApps) {
                HomeAppsPager(
                    state = state,
                    callbacks = callbacks,
                    selectedAppId = selectedAppId,
                    showDpadMode = showDpadMode,
                    dpadActivatedAppId = dpadActivatedAppId,
                    onDpadActivatedHandled = onDpadActivatedHandled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = state.homeAppsYOffset.dp)
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInParent()
                            appBounds = bounds
                            onHomeAppsBoundsChanged(bounds)
                        }
                )
            } else {
                appBounds = null
                onHomeAppsBoundsChanged(null)
            }

            if (state.pageIndicatorVisible && state.totalPages > 1) {
                PageIndicator(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    color = Theme.colors.text,
                    showTextIslands = state.textIslands,
                    textIslandsInverted = state.textIslandsInverted,
                    textIslandsShape = state.textIslandsShape,
                    modifier = Modifier
                        .align(if (state.homeAlignment == 2) Alignment.CenterStart else Alignment.CenterEnd)
                        .offset(y = state.homeAppsYOffset.dp)
                )
            }
        }

        // Bottom widgets (media, quote) pinned to bottom using bottomWidgetMargin
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = state.bottomWidgetMargin.dp)
                .onGloballyPositioned { coordinates ->
                    // Measure bottom widget content height (bounds relative to parent)
                    val bounds = coordinates.boundsInParent()
                    bottomWidgetBounds = Rect(
                        offset = Offset.Zero,
                        size = Size(bounds.width, bounds.height)
                    )
                },
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (state.showMediaWidget && state.mediaInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HomeMediaWidget(
                    state = state, 
                    callbacks = callbacks,
                    selectedButtonIndex = if (showDpadMode && focusZone == FocusZone.MEDIA_WIDGET) selectedMediaButton else null,
                    showTextIslands = state.textIslands,
                    textIslandsInverted = state.textIslandsInverted,
                    textIslandsShape = state.textIslandsShape
                )
            }

            if (state.showQuote && state.quoteText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                QuoteBlock(
                    state = state, 
                    onClick = callbacks.onQuoteClick,
                    isFocused = showDpadMode && focusZone == FocusZone.QUOTE,
                    showTextIslands = state.textIslands
                )
            }
        }
    }
}

/**
 * Page indicator used by Home UI. Kept in `HomeUI.kt` so Home owns rendering.
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    color: Color,
    modifier: Modifier = Modifier,
    showTextIslands: Boolean = false,
    textIslandsInverted: Boolean = false,
    textIslandsShape: Int = 0
) {
    val density = LocalDensity.current
    val islandPaddingPx = with(density) { 2.dp.toPx() }
    
    // Determine colors based on invert setting
    val islandBackgroundColor = if (textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (textIslandsInverted) Theme.colors.text else Theme.colors.background
    
    // Use island colors when text islands enabled, otherwise use default colors
    val dotColor = if (showTextIslands) islandTextColor else color
    val borderColor = if (showTextIslands) islandTextColor else color
    val inactiveBackgroundColor = if (showTextIslands) islandBackgroundColor else Theme.colors.background
    
    Box(
        modifier = modifier
            .drawBehind {
                // Show island background when text islands is enabled
                if (showTextIslands) {
                    val highlightWidth = size.width + (islandPaddingPx * 2)
                    val highlightHeight = size.height + (islandPaddingPx * 2)
                    val corner = ShapeHelper.getCornerRadius(
                        textIslandsShape = textIslandsShape,
                        height = highlightHeight,
                        density = density
                    )
                    drawRoundRect(
                        color = islandBackgroundColor,
                        topLeft = Offset(-islandPaddingPx, -islandPaddingPx),
                        size = Size(highlightWidth.coerceAtLeast(0f), highlightHeight.coerceAtLeast(0f)),
                        cornerRadius = corner
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .padding(all = if (showTextIslands) 2.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calculate dot shape based on textIslandsShape preference
            val dotShape = remember(textIslandsShape) {
                when (textIslandsShape) {
                    0 -> CircleShape // Pill (fully rounded)
                    else -> ShapeHelper.getRoundedCornerShape(
                        textIslandsShape = textIslandsShape,
                        roundedRadius = 4.dp // Matching inner bar
                    )
                }
            }
            
            repeat(totalPages) { index ->
                val selected = index == currentPage
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = dotColor, shape = dotShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = inactiveBackgroundColor, shape = dotShape)
                            .border(width = 2.dp, color = borderColor, shape = dotShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeUiRenderState,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onNotificationCountClick: () -> Unit,
    modifier: Modifier = Modifier,
    isClockFocused: Boolean = false,
    isDateFocused: Boolean = false,
    showTextIslands: Boolean = false
) {
    val context = LocalContext.current
    // Resolve typefaces using custom font paths (same pattern as AppsUI)
    val clockTypeface = remember(state.clockFont, state.clockCustomFontPath) {
        state.clockFont.getFont(context, state.clockCustomFontPath)
    }
    val dateTypeface = remember(state.dateFont, state.dateCustomFontPath) {
        state.dateFont.getFont(context, state.dateCustomFontPath)
    }
    val clockFontFamily = clockTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val dateFontFamily = dateTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val textAlign = when (state.homeAlignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val highlightColor = Theme.colors.text
    
    // Determine colors based on invert setting
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    // For dpad focus when text islands is enabled, use opposite colors for contrast
    val focusBackgroundColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background) else highlightColor
    val focusTextColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text) else Theme.colors.background

    Column(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = when (state.homeAlignment) {
            0 -> Alignment.Start
            2 -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    ) {
            if (state.showClock) {
                val amPmLabels = remember { DateFormatSymbols(Locale.getDefault()).amPmStrings }
                val amLabel = amPmLabels.getOrNull(0) ?: "AM"
                val pmLabel = amPmLabels.getOrNull(1) ?: "PM"
                val amPmSize = if (state.clockSize > 0) (kotlin.math.max(10f, state.clockSize * 0.28f)).sp else 10.sp

                val bgShape = remember(state.textIslandsShape) {
                    ShapeHelper.getRoundedCornerShape(
                        textIslandsShape = state.textIslandsShape,
                        pillRadius = 50.dp
                    )
                }
                
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .then(
                            if (isClockFocused || showTextIslands) {
                                val bgColor = when {
                                    isClockFocused && showTextIslands -> focusBackgroundColor
                                    isClockFocused -> highlightColor
                                    else -> islandBackgroundColor
                                }
                                Modifier.background(bgColor, bgShape).padding(horizontal = 8.dp)
                            } else {
                                Modifier
                            }
                        )
                        .clickableNoRipple(onClockClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.clockText,
                        modifier = Modifier.wrapContentHeight(),
                        style = TextStyle(
                            color = when {
                                isClockFocused && showTextIslands -> focusTextColor
                                isClockFocused -> Theme.colors.background
                                showTextIslands -> islandTextColor
                                else -> Theme.colors.text
                            },
                            fontSize = state.clockSize.sp,
                            fontFamily = clockFontFamily,
                            lineHeight = state.clockSize.sp
                        ),
                        maxLines = 1,
                        textAlign = textAlign
                    )

                    if (!state.is24Hour && state.showAmPm && state.amPmText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val activeLabel = if (state.amPmText.equals(amLabel, ignoreCase = true)) amLabel else pmLabel
                        Text(
                            text = activeLabel,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .wrapContentHeight(),
                            style = TextStyle(
                                color = when {
                                isClockFocused && showTextIslands -> focusTextColor
                                isClockFocused -> Theme.colors.background
                                showTextIslands -> islandTextColor
                                else -> Theme.colors.text
                            },
                                fontSize = amPmSize,
                                fontFamily = clockFontFamily,
                                lineHeight = amPmSize
                            ),
                            maxLines = 1
                        )
                    }

                    if (state.showSecondClock) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "/",
                            modifier = Modifier.wrapContentHeight(),
                            style = TextStyle(
                                color = if (isClockFocused) Theme.colors.background.copy(alpha = 0.9f) else if (showTextIslands) islandTextColor.copy(alpha = 0.9f) else Theme.colors.text.copy(alpha = 0.9f),
                                fontSize = (state.clockSize * 0.5f).sp,
                                fontFamily = clockFontFamily,
                                lineHeight = (state.clockSize * 0.5f).sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.secondClockText,
                            modifier = Modifier.wrapContentHeight(),
                            style = TextStyle(
                                color = when {
                                isClockFocused && showTextIslands -> focusTextColor
                                isClockFocused -> Theme.colors.background
                                showTextIslands -> islandTextColor
                                else -> Theme.colors.text
                            },
                                fontSize = state.clockSize.sp,
                                fontFamily = clockFontFamily,
                                lineHeight = state.clockSize.sp
                            ),
                            maxLines = 1
                        )

                        if (!state.is24Hour && state.showAmPm && state.secondAmPmText.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.secondAmPmText,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .wrapContentHeight(),
                                style = TextStyle(
                                    color = when {
                                isClockFocused && showTextIslands -> focusTextColor
                                isClockFocused -> Theme.colors.background
                                showTextIslands -> islandTextColor
                                else -> Theme.colors.text
                            },
                                    fontSize = amPmSize,
                                    fontFamily = clockFontFamily,
                                    lineHeight = amPmSize
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        if (state.showDate || state.showBattery || state.showNotificationCount) {
            val bgShape = remember(state.textIslandsShape) {
                ShapeHelper.getRoundedCornerShape(
                    textIslandsShape = state.textIslandsShape,
                    pillRadius = 50.dp
                )
            }
            
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .then(
                        if (isDateFocused || showTextIslands) {
                            val bgColor = when {
                                isDateFocused && showTextIslands -> focusBackgroundColor
                                isDateFocused -> highlightColor
                                else -> islandBackgroundColor
                            }
                            Modifier.background(bgColor, bgShape).padding(horizontal = 8.dp, vertical = 2.dp)
                        } else {
                            Modifier.padding(vertical = 2.dp)
                        }
                    ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    if (state.showDate) {
                        Box(
                            modifier = Modifier
                                .clickableNoRipple(onDateClick)
                        ) {
                            Text(
                                text = state.dateText,
                                style = TextStyle(
                                    color = when {
                                    isDateFocused && showTextIslands -> focusTextColor
                                    isDateFocused -> Theme.colors.background
                                    showTextIslands -> islandTextColor
                                    else -> Theme.colors.text
                                },
                                    fontSize = state.dateSize.sp,
                                    fontFamily = dateFontFamily,
                                    textAlign = textAlign
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    
                    if (state.showDate && (state.showBattery || state.showNotificationCount)) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "·",
                            style = TextStyle(
                                color = when {
                                    isDateFocused && showTextIslands -> focusTextColor
                                    isDateFocused -> Theme.colors.background
                                    showTextIslands -> islandTextColor
                                    else -> Theme.colors.text
                                },
                                fontSize = state.dateSize.sp,
                                fontFamily = dateFontFamily,
                                textAlign = textAlign
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    if (state.showBattery) {
                        Box(
                            modifier = Modifier
                                .clickableNoRipple(onBatteryClick)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.batteryText,
                                    style = TextStyle(
                                        color = when {
                                    isDateFocused && showTextIslands -> focusTextColor
                                    isDateFocused -> Theme.colors.background
                                    showTextIslands -> islandTextColor
                                    else -> Theme.colors.text
                                },
                                        fontSize = state.dateSize.sp,
                                        fontFamily = dateFontFamily,
                                        textAlign = textAlign
                                    ),
                                    maxLines = 1
                                )

                                if (state.isCharging) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.OfflineBolt,
                                        contentDescription = "Charging",
                                        tint = when {
                                        isDateFocused && showTextIslands -> focusTextColor
                                        isDateFocused -> Theme.colors.background
                                        showTextIslands -> islandTextColor
                                        else -> Theme.colors.text
                                    },
                                        modifier = Modifier.size((state.dateSize * 1f).dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    if ((state.showDate || state.showBattery) && state.showNotificationCount) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "·",
                            style = TextStyle(
                                color = when {
                                    isDateFocused && showTextIslands -> focusTextColor
                                    isDateFocused -> Theme.colors.background
                                    showTextIslands -> islandTextColor
                                    else -> Theme.colors.text
                                },
                                fontSize = state.dateSize.sp,
                                fontFamily = dateFontFamily,
                                textAlign = textAlign
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    if (state.showNotificationCount) {
                        Box(
                            modifier = Modifier
                                .clickableNoRipple(onNotificationCountClick)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${state.notificationCount}",
                                    style = TextStyle(
                                        color = when {
                                    isDateFocused && showTextIslands -> focusTextColor
                                    isDateFocused -> Theme.colors.background
                                    showTextIslands -> islandTextColor
                                    else -> Theme.colors.text
                                },
                                        fontSize = state.dateSize.sp,
                                        fontFamily = dateFontFamily,
                                        textAlign = textAlign
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notifications",
                                    tint = when {
                                        isDateFocused && showTextIslands -> focusTextColor
                                        isDateFocused -> Theme.colors.background
                                        showTextIslands -> islandTextColor
                                        else -> Theme.colors.text
                                    },
                                    modifier = Modifier.size((state.dateSize * 0.95f).dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun QuoteBlock(
    state: HomeUiRenderState,
    onClick: () -> Unit,
    isFocused: Boolean = false,
    showTextIslands: Boolean = false
) {
    val context = LocalContext.current
    // Resolve typeface using custom font path (same pattern as AppsUI)
    val quoteTypeface = remember(state.quoteFont, state.quoteCustomFontPath) {
        state.quoteFont.getFont(context, state.quoteCustomFontPath)
    }
    val quoteFontFamily = quoteTypeface?.let { FontFamily(it) } ?: FontFamily.Default

    val textAlign = when (state.homeAlignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val blockAlignment = when (state.homeAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val highlightPaddingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val highlightColor = Theme.colors.text
    val density = LocalDensity.current
    
    // Determine colors based on invert setting
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    // For dpad focus when text islands is enabled, use opposite colors for contrast
    val focusBackgroundColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background) else highlightColor
    val focusTextColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text) else Theme.colors.background

    val bgShape = remember(state.textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = state.textIslandsShape,
            pillRadius = 50.dp
        )
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = blockAlignment
    ) {
        Text(
            text = state.quoteText,
            style = TextStyle(
                color = when {
                    isFocused && showTextIslands -> focusTextColor
                    isFocused -> Theme.colors.background
                    showTextIslands -> islandTextColor
                    else -> Theme.colors.text
                },
                fontSize = state.quoteSize.sp,
                fontFamily = quoteFontFamily,
                textAlign = textAlign
            ),
            modifier = Modifier
                .wrapContentSize()
                .then(
                    if (isFocused || showTextIslands) {
                        val bgColor = when {
                            isFocused && showTextIslands -> focusBackgroundColor
                            isFocused -> highlightColor
                            else -> islandBackgroundColor
                        }
                        Modifier.background(bgColor, bgShape).padding(horizontal = 8.dp)
                    } else {
                        Modifier
                    }
                )
                .clickableNoRipple(onClick),
            textAlign = textAlign
        )
    }
}

@Composable
private fun HomeAppsPager(
    modifier: Modifier = Modifier,
    state: HomeUiRenderState,
    callbacks: HomeUiCallbacks,
    selectedAppId: Int? = null,
    showDpadMode: Boolean = false,
    dpadActivatedAppId: Int? = null,
    onDpadActivatedHandled: (Int) -> Unit = {}
) {
    val appsPerPage = state.appsPerPage.coerceAtLeast(1)
    val chunks = remember(state.homeApps, appsPerPage) {
        state.homeApps.chunked(appsPerPage)
    }
    val pageIndex = state.currentPage.coerceIn(0, maxOf(chunks.size - 1, 0))
    val appsOnPage = chunks.getOrNull(pageIndex) ?: emptyList()
    val textAlign = when (state.homeAlignment) {
        0 -> android.view.Gravity.START
        2 -> android.view.Gravity.END
        else -> android.view.Gravity.CENTER
    }

    val horizontalAlignment = when (state.homeAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    
    // Use pre-computed icon codes from state (cached in AppsRepository)
    val iconCodes = state.iconCodes
    
    // PERFORMANCE FIX: Hoist TextMeasurer to pager level (shared by all buttons)
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val appNameHeight = remember(state.appTextSize, textMeasurer) {
        try {
            val measuredHeight = textMeasurer.measure(
                text = AnnotatedString("Ag"), // Representative text for height
                style = TextStyle(fontSize = state.appTextSize.sp)
            ).size.height
            with(density) { measuredHeight.toDp() }
        } catch (_: Exception) {
            state.appTextSize.dp // Fallback
        }
    }
    
    // PERFORMANCE FIX: Pre-compute badgeConfig once for all buttons
    val badgeConfig = remember(
        state.showNotificationBadge,
        state.allCapsApps,
        state.smallCapsApps,
        state.showMediaIndicator,
        state.showMediaName,
        state.showNotificationText,
        state.showNotificationSenderName,
        state.showNotificationGroupName,
        state.showNotificationMessage,
        state.homeAppCharLimit
    ) {
        NotificationBadgeConfig(
            showBadge = state.showNotificationBadge,
            allCapsApps = state.allCapsApps,
            smallCapsApps = state.smallCapsApps,
            showMediaIndicator = state.showMediaIndicator,
            showMediaName = state.showMediaName,
            showNotificationText = state.showNotificationText,
            showNotificationSenderName = state.showNotificationSenderName,
            showNotificationGroupName = state.showNotificationGroupName,
            showNotificationMessage = state.showNotificationMessage,
            homeAppCharLimit = state.homeAppCharLimit
        )
    }
    
    // PERFORMANCE FIX: Pre-compute notification font family once
    val context = LocalContext.current
    val notificationTypeface = remember(state.notificationFont, state.notificationCustomFontPath) {
        state.notificationFont.getFont(context, state.notificationCustomFontPath)
    }
    val notificationFontFamily = remember(notificationTypeface) {
        notificationTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    }
    
    // PERFORMANCE FIX: Pre-compute colors once for all buttons
    val highlightColor = Theme.colors.text
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    val focusBackgroundColor = if (state.textIslands) {
        if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    } else highlightColor
    val focusTextColor = if (state.textIslands) {
        if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    } else Theme.colors.background

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = horizontalAlignment
    ) {
        appsOnPage.forEach { app ->
            HomeAppButton(
                app = app,
                state = state,
                gravity = textAlign,
                isSelected = (selectedAppId != null && app.id == selectedAppId),
                showDpadMode = showDpadMode,
                dpadActivatedAppId = dpadActivatedAppId,
                onDpadActivatedHandled = onDpadActivatedHandled,
                onClick = { callbacks.onAppClick(app) },
                onLongClick = { callbacks.onAppLongClick(app) },
                showTextIslands = state.textIslands,
                iconCodes = iconCodes,
                // Pass pre-computed values
                appNameHeight = appNameHeight,
                badgeConfig = badgeConfig,
                notificationFontFamily = notificationFontFamily,
                highlightColor = highlightColor,
                islandBackgroundColor = islandBackgroundColor,
                islandTextColor = islandTextColor,
                focusBackgroundColor = focusBackgroundColor,
                focusTextColor = focusTextColor
            )
        }
    }
}

@Composable
private fun HomeMediaWidget(
    state: HomeUiRenderState,
    callbacks: HomeUiCallbacks,
    selectedButtonIndex: Int? = null,
    showTextIslands: Boolean = false,
    textIslandsInverted: Boolean = false,
    textIslandsShape: Int = 0
) {
    val density = LocalDensity.current
    
    // Determine colors based on invert setting (matching other text islands)
    val islandBackgroundColor = if (textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (textIslandsInverted) Theme.colors.text else Theme.colors.background
    
    // Use island colors when text islands enabled, otherwise use default colors
    val tintColor = if (showTextIslands) islandTextColor else Theme.colors.text
    val highlightColor = if (showTextIslands) islandTextColor else Theme.colors.text
    val backgroundColor = if (showTextIslands) islandBackgroundColor else Color.Transparent
    
    // Calculate shape based on text islands shape setting
    val containerShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 50.dp
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showTextIslands) {
                    Modifier.background(backgroundColor, containerShape).padding(horizontal = 8.dp, vertical = 5.dp)
                } else {
                    Modifier.border(width = 2.dp, color = Theme.colors.text, shape = containerShape).padding(horizontal = 8.dp, vertical = 5.dp)
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show album art (32dp) for the open button when available; fallback to icon.
        val albumArtBitmap = try {
            state.mediaInfo?.controller?.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: state.mediaInfo?.controller?.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                ?: state.mediaInfo?.controller?.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        } catch (_: Exception) { null }

        // Shape for cover image/icon (36dp size)
        val coverShape = remember(textIslandsShape) {
            ShapeHelper.getRoundedCornerShape(
                textIslandsShape = textIslandsShape,
                pillRadius = 18.dp // 36dp / 2 = 18dp
            )
        }
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterVertically)
                .drawBehind {
                    if (selectedButtonIndex == 0) {
                        if (showTextIslands) {
                            val corner = ShapeHelper.getCornerRadius(
                                textIslandsShape = textIslandsShape,
                                height = 36.dp.toPx(),
                                density = density
                            )
                            // Use rounded rect for all shapes when text islands enabled
                            drawRoundRect(
                                color = highlightColor,
                                size = Size(36.dp.toPx() + 8.dp.toPx(), 36.dp.toPx() + 8.dp.toPx()),
                                cornerRadius = corner,
                                topLeft = Offset(-4.dp.toPx(), -4.dp.toPx())
                            )
                        } else {
                            // Use circle when text islands disabled
                            drawCircle(
                                color = highlightColor,
                                radius = size.minDimension / 2f + 4.dp.toPx()
                            )
                        }
                    }
                }
                .clip(coverShape)
                .clickableNoRippleGestureAware { callbacks.onMediaAction(HomeMediaAction.Open) },
            contentAlignment = Alignment.Center
        ) {
            if (albumArtBitmap != null) {
                Image(
                    bitmap = albumArtBitmap.asImageBitmap(),
                    contentDescription = stringResource(id = R.string.open_music_app),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(coverShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(width = 2.dp, color = tintColor, shape = coverShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = stringResource(id = R.string.open_music_app),
                        tint = if (selectedButtonIndex == 0) (if (showTextIslands) Theme.colors.text else Theme.colors.background) else tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        MediaButton(
            imageVector = Icons.Rounded.SkipPrevious,
            tint = tintColor,
            contentDescription = stringResource(id = R.string.previous_track),
            isSelected = selectedButtonIndex == 1,
            highlightColor = highlightColor,
            showTextIslands = showTextIslands,
            textIslandsShape = textIslandsShape,
            size = 32.dp
        ) {
            callbacks.onMediaAction(HomeMediaAction.Previous)
        }
        MediaButton(
            imageVector = if (state.mediaInfo?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            tint = tintColor,
            contentDescription = stringResource(id = R.string.play_or_pause),
            size = 42.dp,
            isSelected = selectedButtonIndex == 2,
            highlightColor = highlightColor,
            showTextIslands = showTextIslands,
            textIslandsShape = textIslandsShape
        ) {
            callbacks.onMediaAction(HomeMediaAction.PlayPause)
        }
        MediaButton(
            imageVector = Icons.Rounded.SkipNext,
            tint = tintColor,
            contentDescription = stringResource(id = R.string.next_track),
            isSelected = selectedButtonIndex == 3,
            highlightColor = highlightColor,
            showTextIslands = showTextIslands,
            textIslandsShape = textIslandsShape,
            size = 32.dp
        ) {
            callbacks.onMediaAction(HomeMediaAction.Next)
        }
        Box(modifier = Modifier.padding(end = 8.dp)) {
            MediaButton(
                imageVector = Icons.Rounded.Stop,
                tint = tintColor,
                contentDescription = stringResource(id = R.string.stop_playback),
                isSelected = selectedButtonIndex == 4,
                highlightColor = highlightColor,
                showTextIslands = showTextIslands,
                textIslandsShape = textIslandsShape,
                size = 32.dp
            ) {
                callbacks.onMediaAction(HomeMediaAction.Stop)
            }
        }
    }
}

@Composable
private fun MediaButton(
    imageVector: ImageVector? = null,
    iconRes: Int? = null,
    tint: Color,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp = 32.dp,
    isSelected: Boolean = false,
    highlightColor: Color = Color.White,
    showTextIslands: Boolean = false,
    textIslandsShape: Int = 0,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    
    if (imageVector != null) {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    if (isSelected) {
                        if (showTextIslands) {
                            val corner = ShapeHelper.getCornerRadius(
                                textIslandsShape = textIslandsShape,
                                height = size.toPx(),
                                density = density
                            )
                            // Use rounded rect for all shapes when text islands enabled
                            drawRoundRect(
                                color = highlightColor,
                                size = Size(size.toPx() + 8.dp.toPx(), size.toPx() + 8.dp.toPx()),
                                cornerRadius = corner,
                                topLeft = Offset(-4.dp.toPx(), -4.dp.toPx())
                            )
                        } else {
                            // Use circle when text islands disabled
                            drawCircle(
                                color = highlightColor,
                                radius = size.toPx() / 2f + 4.dp.toPx()
                            )
                        }
                    }
                }
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = if (isSelected) (if (showTextIslands) Theme.colors.text else Theme.colors.background) else tint,
                modifier = Modifier
                    .size(size)
                    .clickableNoRippleGestureAware(onClick)
            )
        }
    } else if (iconRes != null) {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    if (isSelected) {
                        drawCircle(
                            color = highlightColor,
                            radius = size.toPx() / 2f + 4.dp.toPx()
                        )
                    }
                }
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(if (isSelected) Theme.colors.background else tint),
                modifier = Modifier
                    .size(size)
                    .clickableNoRippleGestureAware(onClick)
            )
        }
    }
}

/**
 * Calculates the font size for icon codes (text icons).
 * - Single character: 70% of appTextSize
 * - Multi-character (2+): 59.5% of appTextSize (to fit better)
 */
private fun calculateIconCodeFontSize(iconCode: String, appTextSize: Float): Float {
    val baseSize = appTextSize * 0.7f
    return if (iconCode.length > 1) {
        baseSize * 0.85f
    } else {
        baseSize
    }
}

@Composable
private fun HomeAppButton(
    app: HomeAppUiState,
    state: HomeUiRenderState,
    gravity: Int,
    isSelected: Boolean = false,
    showDpadMode: Boolean = false,
    dpadActivatedAppId: Int? = null,
    onDpadActivatedHandled: (Int) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showTextIslands: Boolean = false,
    iconCodes: Map<String, String> = emptyMap(),
    // PERFORMANCE FIX: Pre-computed values from HomeAppsPager
    appNameHeight: androidx.compose.ui.unit.Dp,
    badgeConfig: NotificationBadgeConfig,
    notificationFontFamily: FontFamily,
    highlightColor: Color,
    islandBackgroundColor: Color,
    islandTextColor: Color,
    focusBackgroundColor: Color,
    focusTextColor: Color
) {
    val highlightPaddingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val isDpadActivated = dpadActivatedAppId == app.id
    val pulseAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val placeholderLabel = stringResource(id = R.string.select_app)
    val rawLabel = app.label.ifBlank { placeholderLabel }
    val notificationInfo = state.notifications[app.activityPackage]

    // Use pre-computed badgeConfig from pager
    val badgeDisplay = remember(
        rawLabel,
        app.activityPackage,
        notificationInfo,
        state.mediaInfo,
        badgeConfig
    ) {
        buildNotificationBadgeDisplay(
            label = rawLabel,
            packageName = app.activityPackage,
            notificationInfo = notificationInfo,
            mediaInfo = state.mediaInfo,
            config = badgeConfig
        )
    }

    val resolvedLabel = badgeDisplay.label.ifBlank { placeholderLabel }
    val textAlign = when (gravity) {
        android.view.Gravity.END -> TextAlign.End
        android.view.Gravity.CENTER -> TextAlign.Center
        else -> TextAlign.Start
    }
    val horizontalAlignment = when (gravity) {
        android.view.Gravity.END -> Alignment.End
        android.view.Gravity.CENTER -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }

    val labelFontFamily = remember(app.font) {
        app.font?.let { FontFamily(it) } ?: FontFamily.Default
    }
    // Use pre-computed notificationFontFamily from pager
    val pulseColor = Theme.colors.text

    // Text color: use focus text color when focused in dpad mode with text islands, island text color when text islands enabled
    val isFocused = showDpadMode && isSelected
    val labelColor = when {
        isFocused && showTextIslands -> focusTextColor
        isFocused -> Theme.colors.background
        showTextIslands -> islandTextColor
        else -> Theme.colors.text
    }
    val labelAnnotated = remember(resolvedLabel, badgeDisplay.showIndicator, state.appTextSize) {
        buildAnnotatedString {
            append(resolvedLabel)
            if (badgeDisplay.showIndicator) {
                withStyle(
                    SpanStyle(
                        baselineShift = BaselineShift.Superscript,
                        fontSize = (state.appTextSize * 0.55f).sp
                    )
                ) {
                    append("*")
                }
            }
        }
    }
    val subtitle = badgeDisplay.subtitle?.takeIf { it.isNotBlank() }
    val subtitleColor = when {
        isFocused && showTextIslands -> focusTextColor
        isFocused -> Theme.colors.background
        showTextIslands -> islandTextColor
        else -> Theme.colors.text
    }

    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    
    // Box content alignment should respect the horizontal alignment setting
    val boxContentAlignment = when (gravity) {
        android.view.Gravity.END -> Alignment.CenterEnd
        android.view.Gravity.CENTER -> Alignment.Center
        else -> Alignment.CenterStart
    }
    
    // PERFORMANCE FIX: Use pre-computed appNameHeight from pager (no TextMeasurer, no state updates)
    
    Box(
        modifier = Modifier
            .then(if (state.extendHomeAppsArea) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
            .padding(vertical = state.appPadding.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = boxContentAlignment
    ) {
        if (isDpadActivated) {
            LaunchedEffect(app.id) {
                try {
                    pulseAlpha.snapTo(0.9f)
                    pulseAlpha.animateTo(0f, animationSpec = tween(durationMillis = 220))
                } catch (_: Exception) {}
                onDpadActivatedHandled(app.id)
            }
        }

        // If there's a notification, show icon + 2 lines (app name + notification)
        // Otherwise, show normal layout (icon + app name)
        if (subtitle != null) {
            val isRightAligned = gravity == android.view.Gravity.END
            val density = LocalDensity.current
            
            // Split exactly in half with a small gap
            val gapSize = 2.dp
            val notificationRowHeight = appNameHeight
            val boxHeight = remember(notificationRowHeight, gapSize) {
                (notificationRowHeight - gapSize) / 2f
            }
            
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (gravity) {
                    android.view.Gravity.END -> Arrangement.End
                    android.view.Gravity.CENTER -> Arrangement.Center
                    else -> Arrangement.Start
                }
            ) {
                // Icon on left (when not right-aligned) or right (when right-aligned)
                if (!isRightAligned && state.showIcons && state.homeAlignment != 1) {
                    val iconCode = iconCodes[app.label]
                    if (iconCode != null && iconCode.isNotEmpty()) {
                        val iconBorderShape = remember(state.textIslandsShape, appNameHeight) {
                            ShapeHelper.getRoundedCornerShape(
                                textIslandsShape = state.textIslandsShape,
                                pillRadius = appNameHeight / 2
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .requiredSize(appNameHeight)
                                .then(if (!showTextIslands) Modifier.border(width = 2.dp, color = Theme.colors.text, shape = iconBorderShape) else Modifier)
                                .graphicsLayer(clip = false)
                                .drawBehind {
                                    if ((showDpadMode && isSelected) || showTextIslands) {
                                        val corner = ShapeHelper.getCornerRadius(
                                            textIslandsShape = state.textIslandsShape,
                                            height = size.height,
                                            density = density
                                        )
                                        val isFocused = showDpadMode && isSelected
                                        val bgColor = when {
                                            isFocused && showTextIslands -> focusBackgroundColor
                                            isFocused -> highlightColor
                                            else -> islandBackgroundColor
                                        }
                                        drawRoundRect(
                                            color = bgColor,
                                            topLeft = Offset(0f, 0f),
                                            size = Size(size.height, size.height),
                                            cornerRadius = corner
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val dynamicFontSize = remember(iconCode, state.appTextSize) {
                                calculateIconCodeFontSize(iconCode, state.appTextSize)
                            }
                            
                            Text(
                                text = iconCode,
                                modifier = Modifier.wrapContentWidth(),
                                style = TextStyle(
                                    color = labelColor,
                                    fontSize = dynamicFontSize.sp,
                                    fontFamily = labelFontFamily,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // Two-line layout: App name (top) + Notification (bottom)
                // Calculate ripple padding once for notification layout
                val notificationRipplePad = if (showTextIslands || (showDpadMode && isSelected)) with(density) { highlightPaddingPx.toDp() } else 0.dp
                
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(
                            start = if (!isRightAligned && state.showIcons && state.homeAlignment != 1 && iconCodes[app.label] != null && iconCodes[app.label]!!.isNotEmpty()) 10.dp else 0.dp,
                            end = if (isRightAligned && state.showIcons && state.homeAlignment != 1 && iconCodes[app.label] != null && iconCodes[app.label]!!.isNotEmpty()) 10.dp else 0.dp
                        )
                        .height(notificationRowHeight),
                    horizontalAlignment = horizontalAlignment,
                    verticalArrangement = Arrangement.Top
                ) {
                    // App name (exactly half height) - with its own ripple
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(boxHeight)
                            .padding(horizontal = notificationRipplePad)
                            .graphicsLayer(clip = false)
                        .drawBehind {
                            if ((showDpadMode && isSelected) || showTextIslands) {
                                val pad = highlightPaddingPx
                                val corner = ShapeHelper.getCornerRadius(
                                    textIslandsShape = state.textIslandsShape,
                                    height = size.height,
                                    density = density
                                )
                                val isFocused = showDpadMode && isSelected
                                val bgColor = when {
                                    isFocused && showTextIslands -> focusBackgroundColor
                                    isFocused -> highlightColor
                                    else -> islandBackgroundColor
                                }
                                drawRoundRect(
                                    color = bgColor,
                                    topLeft = Offset(-pad, 0f),
                                    size = Size(size.width + (pad * 2), size.height),
                                    cornerRadius = corner
                                )
                            }
                        },
                        contentAlignment = when (horizontalAlignment) {
                            Alignment.Start -> Alignment.CenterStart
                            Alignment.End -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    ) {
                        Text(
                            text = resolvedLabel.uppercase(Locale.getDefault()),
                            modifier = Modifier.wrapContentWidth(),
                            style = TextStyle(
                                color = labelColor,
                                fontSize = (state.appTextSize * 0.5f).sp,
                                fontFamily = labelFontFamily,
                                fontWeight = FontWeight.Bold,
                                textAlign = textAlign,
                                lineHeight = with(density) { boxHeight.toSp() }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Gap between app name and notification
                    Spacer(modifier = Modifier.height(gapSize))
                    
                    // Notification message (exactly half height) - with its own ripple
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(boxHeight)
                            .padding(horizontal = notificationRipplePad)
                            .graphicsLayer(clip = false)
                            .drawBehind {
                                if ((showDpadMode && isSelected) || showTextIslands) {
                                    val pad = highlightPaddingPx
                                    val corner = ShapeHelper.getCornerRadius(
                                        textIslandsShape = state.textIslandsShape,
                                        height = size.height,
                                        density = density
                                    )
                                    val isFocused = showDpadMode && isSelected
                                    val bgColor = when {
                                        isFocused && showTextIslands -> focusBackgroundColor
                                        isFocused -> highlightColor
                                        else -> islandBackgroundColor
                                    }
                                    drawRoundRect(
                                        color = bgColor,
                                        topLeft = Offset(-pad, 0f),
                                        size = Size(size.width + (pad * 2), size.height),
                                        cornerRadius = corner
                                    )
                                }
                            },
                        contentAlignment = when (horizontalAlignment) {
                            Alignment.Start -> Alignment.CenterStart
                            Alignment.End -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    ) {
                        Text(
                            text = subtitle,
                            modifier = Modifier.wrapContentWidth(),
                            style = TextStyle(
                                color = subtitleColor.copy(alpha = 0.9f),
                                fontSize = (state.appTextSize * 0.5f).sp,
                                fontFamily = notificationFontFamily,
                                textAlign = textAlign,
                                lineHeight = with(density) { boxHeight.toSp() }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Icon on right (when right-aligned)
                if (isRightAligned && state.showIcons && state.homeAlignment != 1) {
                    val iconCode = iconCodes[app.label]
                    if (iconCode != null && iconCode.isNotEmpty()) {
                        val iconBorderShape = remember(state.textIslandsShape, appNameHeight) {
                            ShapeHelper.getRoundedCornerShape(
                                textIslandsShape = state.textIslandsShape,
                                pillRadius = appNameHeight / 2
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .requiredSize(appNameHeight)
                                .then(if (!showTextIslands) Modifier.border(width = 2.dp, color = Theme.colors.text, shape = iconBorderShape) else Modifier)
                                .graphicsLayer(clip = false)
                                .drawBehind {
                                    if ((showDpadMode && isSelected) || showTextIslands) {
                                        val corner = ShapeHelper.getCornerRadius(
                                            textIslandsShape = state.textIslandsShape,
                                            height = size.height,
                                            density = density
                                        )
                                        val isFocused = showDpadMode && isSelected
                                        val bgColor = when {
                                            isFocused && showTextIslands -> focusBackgroundColor
                                            isFocused -> highlightColor
                                            else -> islandBackgroundColor
                                        }
                                        drawRoundRect(
                                            color = bgColor,
                                            topLeft = Offset(0f, 0f),
                                            size = Size(size.height, size.height),
                                            cornerRadius = corner
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val dynamicFontSize = remember(iconCode, state.appTextSize) {
                                calculateIconCodeFontSize(iconCode, state.appTextSize)
                            }
                            
                            Text(
                                text = iconCode,
                                modifier = Modifier.wrapContentWidth(),
                                style = TextStyle(
                                    color = labelColor,
                                    fontSize = dynamicFontSize.sp,
                                    fontFamily = labelFontFamily,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        } else {
            // Normal layout: icon + app name (no notification)
            val isRightAligned = gravity == android.view.Gravity.END
            
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (gravity) {
                    android.view.Gravity.END -> Arrangement.End
                    android.view.Gravity.CENTER -> Arrangement.Center
                    else -> Arrangement.Start
                }
            ) {
                // Icon on left (when not right-aligned)
                if (!isRightAligned && state.showIcons && state.homeAlignment != 1) {
                    val iconCode = iconCodes[app.label]
                    if (iconCode != null && iconCode.isNotEmpty()) {
                        val iconBorderShape = remember(state.textIslandsShape, appNameHeight) {
                            ShapeHelper.getRoundedCornerShape(
                                textIslandsShape = state.textIslandsShape,
                                pillRadius = appNameHeight / 2
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .requiredSize(appNameHeight)
                                .then(if (!showTextIslands) Modifier.border(width = 2.dp, color = Theme.colors.text, shape = iconBorderShape) else Modifier)
                                .graphicsLayer(clip = false)
                                .drawBehind {
                                    if ((showDpadMode && isSelected) || showTextIslands) {
                                        val corner = ShapeHelper.getCornerRadius(
                                            textIslandsShape = state.textIslandsShape,
                                            height = size.height,
                                            density = density
                                        )
                                        val isFocused = showDpadMode && isSelected
                                        val bgColor = when {
                                            isFocused && showTextIslands -> focusBackgroundColor
                                            isFocused -> highlightColor
                                            else -> islandBackgroundColor
                                        }
                                        drawRoundRect(
                                            color = bgColor,
                                            topLeft = Offset(0f, 0f),
                                            size = Size(size.height, size.height),
                                            cornerRadius = corner
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val dynamicFontSize = remember(iconCode, state.appTextSize) {
                                calculateIconCodeFontSize(iconCode, state.appTextSize)
                            }
                            
                            Text(
                                text = iconCode,
                                modifier = Modifier.wrapContentWidth(),
                                style = TextStyle(
                                    color = labelColor,
                                    fontSize = dynamicFontSize.sp,
                                    fontFamily = labelFontFamily,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // App label container (with its own ripple)
                val ripplePad = if (showTextIslands || (showDpadMode && isSelected)) with(density) { highlightPaddingPx.toDp() } else 0.dp
                Box(
                    modifier = Modifier
                        .padding(
                            start = if (!isRightAligned && state.showIcons && state.homeAlignment != 1 && iconCodes[app.label] != null && iconCodes[app.label]!!.isNotEmpty()) 10.dp else 0.dp,
                            end = if (isRightAligned && state.showIcons && state.homeAlignment != 1 && iconCodes[app.label] != null && iconCodes[app.label]!!.isNotEmpty()) 10.dp else 0.dp
                        )
                        .padding(horizontal = ripplePad)
                        // PERFORMANCE FIX: Removed onSizeChanged that caused recomposition loops
                        .graphicsLayer(clip = false)
                        .drawBehind {
                            if ((showDpadMode && isSelected) || showTextIslands) {
                                val pad = highlightPaddingPx
                                val corner = ShapeHelper.getCornerRadius(
                                    textIslandsShape = state.textIslandsShape,
                                    height = size.height,
                                    density = density
                                )
                                val isFocused = showDpadMode && isSelected
                                val bgColor = when {
                                    isFocused && showTextIslands -> focusBackgroundColor
                                    isFocused -> highlightColor
                                    else -> islandBackgroundColor
                                }
                                drawRoundRect(
                                    color = bgColor,
                                    topLeft = Offset(-pad, 0f),
                                    size = Size(size.width + (pad * 2), size.height),
                                    cornerRadius = corner
                                )
                            }
                        }
                ) {
                    Text(
                        text = labelAnnotated,
                        modifier = Modifier.wrapContentWidth(),
                        style = TextStyle(
                            color = labelColor,
                            fontSize = state.appTextSize.sp,
                            fontFamily = labelFontFamily,
                            textAlign = textAlign
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Icon on right (when right-aligned)
                if (isRightAligned && state.showIcons && state.homeAlignment != 1) {
                    val iconCode = iconCodes[app.label]
                    if (iconCode != null && iconCode.isNotEmpty()) {
                        val iconBorderShape = remember(state.textIslandsShape, appNameHeight) {
                            ShapeHelper.getRoundedCornerShape(
                                textIslandsShape = state.textIslandsShape,
                                pillRadius = appNameHeight / 2
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .requiredSize(appNameHeight)
                                .then(if (!showTextIslands) Modifier.border(width = 2.dp, color = Theme.colors.text, shape = iconBorderShape) else Modifier)
                                .graphicsLayer(clip = false)
                                .drawBehind {
                                    if ((showDpadMode && isSelected) || showTextIslands) {
                                        val corner = ShapeHelper.getCornerRadius(
                                            textIslandsShape = state.textIslandsShape,
                                            height = size.height,
                                            density = density
                                        )
                                        val isFocused = showDpadMode && isSelected
                                        val bgColor = when {
                                            isFocused && showTextIslands -> focusBackgroundColor
                                            isFocused -> highlightColor
                                            else -> islandBackgroundColor
                                        }
                                        drawRoundRect(
                                            color = bgColor,
                                            topLeft = Offset(0f, 0f),
                                            size = Size(size.height, size.height),
                                            cornerRadius = corner
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val dynamicFontSize = remember(iconCode, state.appTextSize) {
                                calculateIconCodeFontSize(iconCode, state.appTextSize)
                            }
                            
                            Text(
                                text = iconCode,
                                modifier = Modifier.wrapContentWidth(),
                                style = TextStyle(
                                    color = labelColor,
                                    fontSize = dynamicFontSize.sp,
                                    fontFamily = labelFontFamily,
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (isDpadActivated) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .graphicsLayer(clip = false)
                    .drawBehind {
                        val alpha = pulseAlpha.value.coerceIn(0f, 1f)
                        if (alpha > 0f) {
                            val pad = highlightPaddingPx
                            val pulseWidth = size.width + (pad * 2)
                            val corner = CornerRadius(size.height / 2f, size.height / 2f)
                            drawRoundRect(
                                color = pulseColor.copy(alpha = alpha),
                                topLeft = Offset(-pad, 0f),
                                size = Size(pulseWidth.coerceAtLeast(0f), size.height),
                                cornerRadius = corner
                            )
                        }
                    }
            ) {}
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    composed {
        this.then(
            Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            }
        )
    }

/**
 * Clickable modifier that doesn't interfere with swipe gestures.
 * Only triggers onClick if it's a click (small movement), allowing swipes to pass through to gesture handler.
 */
@Composable
private fun Modifier.clickableNoRippleGestureAware(onClick: () -> Unit): Modifier {
    val density = LocalDensity.current
    val clickThreshold = with(density) { 10.dp.toPx() } // Maximum movement to consider it a click
    return this.then(
        Modifier.pointerInput(clickThreshold) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var maxMovement = 0f
                var isClick = true
                
                // Track movement through the gesture
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { change ->
                        if (change.id == down.id) {
                            val movement = abs(change.position.x - down.position.x) + abs(change.position.y - down.position.y)
                            maxMovement = maxOf(maxMovement, movement)
                            // If movement exceeds threshold, it's a swipe, not a click
                            if (movement > clickThreshold) {
                                isClick = false
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })
                
                // Only trigger onClick if it was a click (small movement)
                if (isClick && maxMovement <= clickThreshold) {
                    onClick()
                }
            }
        }
    )
}

/**
 * Rendering state passed from the Fragment to the Compose UI.
 */
data class HomeUiRenderState(
    val homeApps: List<HomeAppUiState>,
    val clockText: String,
    val dateText: String,
    val amPmText: String,
    val is24Hour: Boolean,
    val isCharging: Boolean,
    val showClock: Boolean,
    val showDate: Boolean,
    val showBattery: Boolean,
    val batteryText: String,
    val showNotificationCount: Boolean,
    val notificationCount: Int,
    val showQuote: Boolean,
    val quoteText: String,
    val backgroundColor: Int,
    val backgroundOpacity: Int,
    val textColor: Int,
    val appsPerPage: Int,
    val totalPages: Int,
    val currentPage: Int,
    val notifications: Map<String, NotificationManager.NotificationInfo>,
    val mediaInfo: AudioWidgetHelper.MediaPlayerInfo?,
    val mediaWidgetColor: Int,
    val showMediaWidget: Boolean,
    val showMediaIndicator: Boolean,
    val showMediaName: Boolean,
    val clockFont: Constants.FontFamily,
    val quoteFont: Constants.FontFamily,
    val clockSize: Int,
    val clockMode: Int,
    val quoteSize: Int,
    val dateFont: Constants.FontFamily,
    val dateSize: Int,
    val homeAlignment: Int,
    val homeAppsYOffset: Int,
    val bottomWidgetMargin: Int,
    val topWidgetMargin: Int,
    val pageIndicatorVisible: Boolean,
    val pageIndicatorColor: Int,
    val appPadding: Int,
    val appTextSize: Float,
    val hideHomeApps: Boolean,
    // New fields
    val appDrawerGap: Int,
    val showAmPm: Boolean,
    val showSecondClock: Boolean,
    val secondClockText: String,
    val secondAmPmText: String,
    val secondClockOffsetHours: Int,
    val allCapsApps: Boolean,
    val smallCapsApps: Boolean,
    val showNotificationBadge: Boolean,
    val showNotificationText: Boolean,
    val showNotificationSenderName: Boolean,
    val showNotificationGroupName: Boolean,
    val showNotificationMessage: Boolean,
    val homeAppCharLimit: Int,
    val notificationTextSize: Int,
    val notificationFont: Constants.FontFamily,
    val textIslands: Boolean,
    val textIslandsInverted: Boolean,
    val textIslandsShape: Int,
    val showIcons: Boolean,
    val iconCodes: Map<String, String> = emptyMap(),
    // Custom font paths (following AppsDrawerUiState pattern)
    val clockCustomFontPath: String?,
    val dateCustomFontPath: String?,
    val quoteCustomFontPath: String?,
    val notificationCustomFontPath: String?,
    val extendHomeAppsArea: Boolean
)

/**
 * Interaction callbacks from Compose back to the Fragment.
 */
data class HomeUiCallbacks(
    val onAppClick: (HomeAppUiState) -> Unit,
    val onAppLongClick: (HomeAppUiState) -> Unit,
    val onClockClick: () -> Unit,
    val onDateClick: () -> Unit,
    val onBatteryClick: () -> Unit,
    val onNotificationCountClick: () -> Unit,
    val onQuoteClick: () -> Unit,
    val onMediaAction: (HomeMediaAction) -> Unit,
    val onSwipeLeft: () -> Unit,
    val onSwipeRight: () -> Unit,
    val onPageDelta: (Int) -> Unit,
    val onRootLongPress: () -> Unit
)

enum class HomeMediaAction { Open, Previous, PlayPause, Next, Stop }
