package com.github.gezimos.inkos.ui.compose

import android.content.Intent
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.Density
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Reviews
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.HomeAppUiState
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.EditModeHelper
import com.github.gezimos.inkos.helper.IconShape
import com.github.gezimos.inkos.helper.IconShapeUtility
import com.github.gezimos.inkos.helper.IconUtility
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.data.ThemePreset
import com.github.gezimos.inkos.style.AppColors
import com.github.gezimos.inkos.style.LocalAppColors
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.dialogs.SheetTitle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeUI(
    state: HomeUiRenderState,
    callbacks: HomeUiCallbacks,
    selectedAppId: Int? = null,
    showDpadMode: Boolean = false,
    dpadActivatedAppId: Int? = null,
    onDpadActivatedHandled: (Int) -> Unit = {},
    onHomeAppsBoundsChanged: (Rect?) -> Unit = {},
    onBottomWidgetHeightChanged: (Int) -> Unit = {},
    focusZone: FocusZone? = null,
    selectedMediaButton: Int? = null,
    showEditMode: Boolean = false,
    navBarPaddingDp: Dp = 0.dp,
    isPreview: Boolean = false
) {
    val alignment = when (state.quoteAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }

    val bgAlpha = (state.backgroundOpacity.coerceIn(0, 255).toFloat() / 255f)
    val screenScale = rememberScreenScale()

    
    val density = LocalDensity.current
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    
    // --- Insets ---
    val statusBarPadding = if (isPreview || !prefs.showStatusBar) 0.dp else {
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        maxOf(statusTop, cutoutTop)
    }
    val navBarPadding = if (isPreview || !prefs.showNavigationBar) 0.dp else {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }

    val topOffset = maxOf(statusBarPadding, state.topWidgetMargin.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (!isPreview) {
            val inkosBitmap: Bitmap? = remember(prefs.inkosWallpaperPath) {
                prefs.inkosWallpaperPath?.let {
                    val file = java.io.File(it)
                    if (file.exists()) {
                        try {
                            BitmapFactory.decodeFile(it)
                        } catch (e: Exception) {
                            android.util.Log.e("HomeUI", "Failed to decode inkOS wallpaper bitmap", e)
                            null
                        }
                    } else null
                }
            }
            if (inkosBitmap != null) {
                Image(
                    bitmap = inkosBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Background overlay on top of inkOS wallpaper
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background.copy(alpha = bgAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    callbacks.onBackgroundClick()
                }
        )
        
        // First-launch tooltip sequence: pinch → swipe pages → long swipe drawer.
        // Single persistent bubble; Next advances through steps, X / outside skips remaining.
        if (!isPreview) {
            val tooltipKeyPinch = "tooltip_home_pinch_settings"
            val tooltipKeySwipePages = "tooltip_home_swipe_pages"
            val tooltipKeyLongSwipeDrawer = "tooltip_home_long_swipe_drawer"
            var tooltipStep by remember { mutableIntStateOf(-1) }
            LaunchedEffect(prefs.firstOpen) {
                tooltipStep = if (prefs.firstOpen) 0
                else when {
                    !prefs.isTooltipShown(tooltipKeyPinch) -> 1
                    !prefs.isTooltipShown(tooltipKeySwipePages) -> 2
                    !prefs.isTooltipShown(tooltipKeyLongSwipeDrawer) -> 3
                    else -> 0
                }
            }
            if (tooltipStep in 1..3) {
                val currentKey = when (tooltipStep) {
                    1 -> tooltipKeyPinch
                    2 -> tooltipKeySwipePages
                    else -> tooltipKeyLongSwipeDrawer
                }
                val isLastStep = tooltipStep == 3
                TooltipBubble(
                    title = when (tooltipStep) {
                        1 -> stringResource(R.string.settings_name)
                        2 -> "Home Pages"
                        else -> "App Drawer"
                    },
                    lines = when (tooltipStep) {
                        1 -> listOf(stringResource(R.string.tooltip_pinch_settings))
                        2 -> listOf("Swipe up/down to move between pages")
                        else -> listOf("Long swipe up to open App Drawer")
                    },
                    icon = when (tooltipStep) {
                        2 -> Icons.Rounded.SwipeVertical
                        3 -> Icons.Rounded.ArrowUpward
                        else -> null
                    },
                    animFrames = if (tooltipStep == 1) listOf(R.drawable.pinch1, R.drawable.pinch2) else emptyList(),
                    alignment = Alignment.Center,
                    nextLabel = if (isLastStep) "Done" else "Next",
                    onNext = {
                        prefs.markTooltipShown(currentKey)
                        tooltipStep = when {
                            !prefs.isTooltipShown(tooltipKeyPinch) -> 1
                            !prefs.isTooltipShown(tooltipKeySwipePages) -> 2
                            !prefs.isTooltipShown(tooltipKeyLongSwipeDrawer) -> 3
                            else -> 0
                        }
                    },
                    onDismiss = {
                        prefs.markTooltipShown(tooltipKeyPinch)
                        prefs.markTooltipShown(tooltipKeySwipePages)
                        prefs.markTooltipShown(tooltipKeyLongSwipeDrawer)
                        tooltipStep = 0
                    }
                )
            }
        }

        val headerAlign = when (state.clockAlignment) {
            0 -> Alignment.TopStart
            2 -> Alignment.TopEnd
            else -> Alignment.TopCenter
        }

        var headerHeightDp by remember { mutableStateOf(48.dp) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(headerAlign)
                .offset(y = topOffset)
                .padding(horizontal = 32.dp.scaled(screenScale))
                .onGloballyPositioned { headerHeightDp = with(density) { it.size.height.toDp() } }
        ) {
            HomeHeader(
                state = state,
                onClockClick = callbacks.onClockClick,
                onDateClick = callbacks.onDateClick,
                onBatteryClick = callbacks.onBatteryClick,
                onNotificationCountClick = callbacks.onNotificationCountClick,
                modifier = Modifier,
                isClockFocused = showDpadMode && focusZone == FocusZone.CLOCK,
                isDateFocused = showDpadMode && focusZone == FocusZone.DATE,
                showTextIslands = state.textIslands,
                showEditMode = showEditMode
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp.scaled(screenScale))
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
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
                        onHomeAppsBoundsChanged(coordinates.boundsInParent())
                    }
            )

            if (state.pageIndicatorVisible && state.totalPages > 1
                && !(state.shortcutPageDots && state.bottomWidgetType == Constants.BottomWidgetType.Shortcuts.value)
                && state.bottomWidgetType != Constants.BottomWidgetType.PageDots.value
            ) {
                PageIndicator(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    color = Theme.colors.text,
                    showTextIslands = state.textIslands,
                    textIslandsInverted = state.textIslandsInverted,
                    textIslandsShape = state.textIslandsShape,
                    dotSize = state.pageIndicatorDotSize.dp.scaled(screenScale),
                    dotSpacing = (state.pageIndicatorDotSize * 0.8f).dp.scaled(screenScale),
                    borderWidth = (state.pageIndicatorDotSize / 6f).dp.scaled(screenScale),
                    modifier = Modifier
                        .align(if (state.homeAlignment == 2) Alignment.CenterStart else Alignment.CenterEnd)
                        .offset(y = state.homeAppsYOffset.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp.scaled(screenScale))
                .align(Alignment.BottomCenter)
                .padding(bottom = maxOf(state.bottomWidgetMargin.dp, navBarPadding))
                .onGloballyPositioned { onBottomWidgetHeightChanged(it.size.height) },
            horizontalAlignment = alignment,
            verticalArrangement = Arrangement.Bottom
        ) {
            if (state.showMediaWidget && state.mediaInfo != null) {
                Spacer(modifier = Modifier.height(16.dp.scaled(screenScale)))
                HomeMediaWidget(
                    state = state,
                    callbacks = callbacks,
                    selectedButtonIndex = if (showDpadMode && focusZone == FocusZone.MEDIA_WIDGET) selectedMediaButton else null,
                    showTextIslands = state.textIslands,
                    textIslandsInverted = state.textIslandsInverted,
                    textIslandsShape = state.textIslandsShape,
                    showEditMode = showEditMode
                )
            }

            when (state.bottomWidgetType) {
                Constants.BottomWidgetType.Quote.value -> {
                    if ((state.quoteText.isNotBlank()) || showEditMode) {
                        Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                        QuoteBlock(
                            state = state,
                            onClick = callbacks.onBottomWidgetClick,
                            isFocused = showDpadMode && focusZone == FocusZone.QUOTE,
                            showTextIslands = state.textIslands,
                            showEditMode = showEditMode
                        )
                    }
                }
                Constants.BottomWidgetType.Events.value -> {
                    Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                    EventsBlock(
                        state = state,
                        callbacks = callbacks,
                        onBottomWidgetClick = callbacks.onBottomWidgetClick,
                        isEditMode = showEditMode,
                        isFocused = showDpadMode && focusZone == FocusZone.QUOTE,
                        showTextIslands = state.textIslands
                    )
                }
                Constants.BottomWidgetType.AndroidWidget.value -> {
                    Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                    HomeAndroidWidget(
                        widgetHeight = state.androidWidgetHeight,
                        marginStart = state.androidWidgetMarginStart,
                        marginEnd = state.androidWidgetMarginEnd,
                        hasWidget = state.androidWidgetId != -1,
                        isEditMode = showEditMode,
                        onClick = callbacks.onBottomWidgetClick,
                        onHeightChange = callbacks.onAndroidWidgetHeightChange,
                        onMarginStartChange = callbacks.onAndroidWidgetMarginStartChange,
                        onMarginEndChange = callbacks.onAndroidWidgetMarginEndChange
                    )
                }
                Constants.BottomWidgetType.Shortcuts.value -> {
                    Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                    ShortcutsBlock(
                        state = state,
                        onLeftClick = callbacks.onShortcutLeftClick,
                        onRightClick = callbacks.onShortcutRightClick,
                        onBottomWidgetClick = callbacks.onBottomWidgetClick,
                        isEditMode = showEditMode,
                        isFocused = showDpadMode && focusZone == FocusZone.QUOTE,
                        showTextIslands = state.textIslands
                    )
                }
                Constants.BottomWidgetType.TotalUsage.value -> {
                    Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                    TotalUsageBlock(
                        state = state,
                        onClick = callbacks.onBottomWidgetClick,
                        isFocused = showDpadMode && focusZone == FocusZone.QUOTE,
                        showTextIslands = state.textIslands,
                        showEditMode = showEditMode
                    )
                }
                Constants.BottomWidgetType.PageDots.value -> {
                    if (state.totalPages > 1 || showEditMode) {
                        Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                        PageDotsBlock(
                            state = state,
                            onClick = callbacks.onBottomWidgetClick,
                            isEditMode = showEditMode,
                            showTextIslands = state.textIslands
                        )
                    }
                }
                Constants.BottomWidgetType.Disabled.value -> {
                    if (showEditMode) {
                        Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                        BottomWidgetEditModeTapTarget(state = state, onClick = callbacks.onBottomWidgetClick)
                    }
                }
                else -> {
                    if ((state.quoteText.isNotBlank()) || showEditMode) {
                        Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                        QuoteBlock(
                            state = state,
                            onClick = callbacks.onBottomWidgetClick,
                            isFocused = showDpadMode && focusZone == FocusZone.QUOTE,
                            showTextIslands = state.textIslands,
                            showEditMode = showEditMode
                        )
                    }
                }
            }
        }
        
        // Edit mode: drag cue handles + overlay
        if (showEditMode) {
            // Top cue — drag to adjust top widget margin
            DragCueHandle(
                currentValueDp = state.topWidgetMargin,
                minValueDp = 0,
                maxValueDp = Constants.MAX_TOP_WIDGET_MARGIN,
                onValueChanged = callbacks.onTopMarginDrag,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = topOffset + headerHeightDp)
            )

            DragCueHandle(
                currentValueDp = state.homeAppsYOffset,
                minValueDp = Constants.MIN_HOME_APPS_Y_OFFSET,
                maxValueDp = state.maxHomeAppsYOffset,
                onValueChanged = callbacks.onAppsYOffsetDrag,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = state.homeAppsYOffset.dp)
            )

            val bottomWidgetHeightDp = with(density) { state.bottomWidgetHeightPx.toDp() }
            DragCueHandle(
                currentValueDp = state.bottomWidgetMargin,
                minValueDp = 0,
                maxValueDp = Constants.MAX_BOTTOM_WIDGET_MARGIN,
                onValueChanged = callbacks.onBottomMarginDrag,
                invertDirection = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = maxOf(state.bottomWidgetMargin.dp, navBarPadding))
                    .offset(y = -bottomWidgetHeightDp)
            )

            EditModeOverlay()
        }
    }
}
@Composable
fun EditModeOverlay() {
    val borderColor = Theme.colors.text.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .drawBehind {
                val strokeWidth = 1.5.dp.toPx()
                val dash = 8.dp.toPx()
                val gap = 6.dp.toPx()

                val path = androidx.compose.ui.graphics.Path().apply {
                    addRect(Rect(0f, 0f, size.width, size.height))
                }
                drawPath(
                    path = path,
                    color = borderColor,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            intervals = floatArrayOf(dash, gap),
                            phase = 0f
                        )
                    )
                )
            }
    )
}
@Composable
private fun DragCueHandle(
    currentValueDp: Int,
    minValueDp: Int,
    maxValueDp: Int,
    onValueChanged: (Int) -> Unit,
    invertDirection: Boolean = false,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textColor = Theme.colors.text
    var dragStartValueDp by remember { mutableStateOf(currentValueDp) }
    var dragAccumulatorPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val directionMultiplier = if (invertDirection) -1f else 1f

    Box(
        modifier = modifier
            .width(80.dp)
            .height(32.dp)
            .pointerInput(minValueDp, maxValueDp) {
                detectVerticalDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragStartValueDp = currentValueDp
                        dragAccumulatorPx = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        dragAccumulatorPx = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragAccumulatorPx = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatorPx += dragAmount
                        val deltaDp = (dragAccumulatorPx * directionMultiplier / density.density).toInt()
                        val newValue = (dragStartValueDp + deltaDp).coerceIn(minValueDp, maxValueDp)
                        onValueChanged(newValue)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Visible line
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .background(textColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
        )
        if (isDragging) {
            Text(
                text = "$dragStartValueDp",
                color = textColor.copy(alpha = 0.5f),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-16).dp)
            )
            Text(
                text = "$currentValueDp",
                color = textColor.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 16.dp)
            )
        }
    }
}

/**
 * Draws a single 7-segment digit on a Canvas.
 * Each segment is a hexagon with beveled ends (like a real LCD/LED display).
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSevenSegmentDigit(
    digit: Int,
    color: Color,
    ghostBrush: androidx.compose.ui.graphics.Brush,
    ox: Float,
    oy: Float,
    w: Float,
    h: Float
) {
    val segments = when (digit) {
        0 -> booleanArrayOf(true, true, true, true, true, true, false)
        1 -> booleanArrayOf(false, true, true, false, false, false, false)
        2 -> booleanArrayOf(true, true, false, true, true, false, true)
        3 -> booleanArrayOf(true, true, true, true, false, false, true)
        4 -> booleanArrayOf(false, true, true, false, false, true, true)
        5 -> booleanArrayOf(true, false, true, true, false, true, true)
        6 -> booleanArrayOf(true, false, true, true, true, true, true)
        7 -> booleanArrayOf(true, true, true, false, false, false, false)
        8 -> booleanArrayOf(true, true, true, true, true, true, true)
        9 -> booleanArrayOf(true, true, true, true, false, true, true)
        else -> booleanArrayOf(false, false, false, false, false, false, false)
    }

    val t = w * 0.15f
    val g = w * 0.012f
    val ht = t * 0.5f
    val midY = h * 0.5f

    fun seg(on: Boolean, path: androidx.compose.ui.graphics.Path) {
        if (on) drawPath(path, color) else drawPath(path, brush = ghostBrush)
    }

    fun hSeg(left: Float, right: Float, top: Float): androidx.compose.ui.graphics.Path {
        return androidx.compose.ui.graphics.Path().apply {
            moveTo(left, top + ht)
            lineTo(left + ht, top)
            lineTo(right - ht, top)
            lineTo(right, top + ht)
            lineTo(right - ht, top + t)
            lineTo(left + ht, top + t)
            close()
        }
    }

    fun vSeg(left: Float, top: Float, bottom: Float): androidx.compose.ui.graphics.Path {
        return androidx.compose.ui.graphics.Path().apply {
            moveTo(left + ht, top)
            lineTo(left + t, top + ht)
            lineTo(left + t, bottom - ht)
            lineTo(left + ht, bottom)
            lineTo(left, bottom - ht)
            lineTo(left, top + ht)
            close()
        }
    }

    // Horizontal segments sit between the vertical segment columns
    val hL = ox + t + g
    val hR = ox + w - t - g

    // Vertical segments sit between the horizontal segment rows
    val vTop = oy + t + g
    val vMidTop = oy + midY - ht - g
    val vMidBot = oy + midY + ht + g
    val vBot = oy + h - t - g

    seg(segments[0], hSeg(hL, hR, oy))                  // A (top)
    seg(segments[3], hSeg(hL, hR, oy + h - t))          // D (bottom)
    seg(segments[6], hSeg(hL, hR, oy + midY - ht))      // G (middle)

    seg(segments[5], vSeg(ox, vTop, vMidTop))            // F (top-left)
    seg(segments[1], vSeg(ox + w - t, vTop, vMidTop))    // B (top-right)

    seg(segments[4], vSeg(ox, vMidBot, vBot))            // E (bottom-left)
    seg(segments[2], vSeg(ox + w - t, vMidBot, vBot))    // C (bottom-right)
}

@Composable
private fun SevenSegmentClock(
    clockText: String,
    color: Color,
    sizeDp: Dp,
    modifier: Modifier = Modifier
) {
    val hours = clockText.substringBefore(":").trim()
    val minutes = clockText.substringAfter(":", "").trim().take(2)
    val digits = listOf(
        hours.padStart(2, '0')[0].digitToIntOrNull() ?: 0,
        hours.padStart(2, '0')[1].digitToIntOrNull() ?: 0,
        minutes.padStart(2, '0')[0].digitToIntOrNull() ?: 0,
        minutes.padStart(2, '0')[1].digitToIntOrNull() ?: 0
    )

    val digitWidth = sizeDp * 0.55f
    val digitHeight = sizeDp
    val digitSpacing = sizeDp * 0.06f
    val colonWidth = sizeDp * 0.18f
    val totalWidth = digitWidth * 4 + digitSpacing * 3 + colonWidth
    val isEink = remember { com.github.gezimos.inkos.helper.device.DeviceHelper.isEinkDevice() }
    val ghostBrush = remember(color, isEink) {
        if (isEink) {
            val argb = color.toArgb()
            val bmp = android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888)
            for (x in 0 until 4) for (y in 0 until 4) bmp.setPixel(x, y, 0)
            bmp.setPixel(0, 0, argb)
            bmp.setPixel(2, 2, argb)
            androidx.compose.ui.graphics.ShaderBrush(
                android.graphics.BitmapShader(bmp, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
            )
        } else {
            androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.06f))
        }
    }

    Canvas(modifier = modifier.size(width = totalWidth, height = digitHeight)) {
        val dw = digitWidth.toPx()
        val dh = digitHeight.toPx()
        val ds = digitSpacing.toPx()
        val cw = colonWidth.toPx()

        var x = 0f

        // Hour digits
        drawSevenSegmentDigit(digits[0], color, ghostBrush, x, 0f, dw, dh)
        x += dw + ds
        drawSevenSegmentDigit(digits[1], color, ghostBrush, x, 0f, dw, dh)
        x += dw + ds

        // Colon
        val dotSize = dw * 0.12f
        val colonCenterX = x + cw * 0.5f - dotSize * 0.5f
        drawRect(color, topLeft = Offset(colonCenterX, dh * 0.3f), size = Size(dotSize, dotSize))
        drawRect(color, topLeft = Offset(colonCenterX, dh * 0.63f), size = Size(dotSize, dotSize))
        x += cw + ds

        // Minute digits
        drawSevenSegmentDigit(digits[2], color, ghostBrush, x, 0f, dw, dh)
        x += dw + ds
        drawSevenSegmentDigit(digits[3], color, ghostBrush, x, 0f, dw, dh)
    }
}

/** 3x5 dot matrix patterns for digits 0-9. Each row is 3 bits wide (bit 2 = left, bit 0 = right). */
private val DOT_MATRIX_DIGITS = arrayOf(
    intArrayOf(0b111, 0b101, 0b101, 0b101, 0b111), // 0
    intArrayOf(0b010, 0b110, 0b010, 0b010, 0b010), // 1
    intArrayOf(0b111, 0b001, 0b111, 0b100, 0b111), // 2
    intArrayOf(0b111, 0b001, 0b111, 0b001, 0b111), // 3
    intArrayOf(0b101, 0b101, 0b111, 0b001, 0b001), // 4
    intArrayOf(0b111, 0b100, 0b111, 0b001, 0b111), // 5
    intArrayOf(0b111, 0b100, 0b111, 0b101, 0b111), // 6
    intArrayOf(0b111, 0b001, 0b010, 0b010, 0b010), // 7
    intArrayOf(0b111, 0b101, 0b111, 0b101, 0b111), // 8
    intArrayOf(0b111, 0b101, 0b111, 0b001, 0b111), // 9
)

/** Build a 17x5 boolean grid for "HH:MM" clock display.
 *  Layout: digit(3) + gap(1) + digit(3) + gap(1) + colon(1) + gap(1) + digit(3) + gap(1) + digit(3) = 17 */
private fun buildClockGrid(h1: Int, h2: Int, m1: Int, m2: Int): Array<BooleanArray> {
    val grid = Array(5) { BooleanArray(17) }
    fun placeDigit(digit: Int, colOffset: Int) {
        val p = DOT_MATRIX_DIGITS[digit.coerceIn(0, 9)]
        for (row in 0 until 5) for (col in 0 until 3) {
            grid[row][colOffset + col] = (p[row] shr (2 - col)) and 1 == 1
        }
    }
    placeDigit(h1, 0)    // cols 0-2
    placeDigit(h2, 4)    // cols 4-6
    grid[1][8] = true     // colon
    grid[3][8] = true
    placeDigit(m1, 10)   // cols 10-12
    placeDigit(m2, 14)   // cols 14-16
    return grid
}

/** General-purpose dot matrix grid. isDotFilled returns true=filled, false=outlined, null=no dot. */
@Composable
private fun MatrixGrid(
    cols: Int,
    rows: Int,
    color: Color,
    dotPitchDp: Dp,
    isDotFilled: (col: Int, row: Int) -> Boolean?,
    modifier: Modifier = Modifier
) {
    val dotSize = dotPitchDp * 0.65f
    val isEink = remember { com.github.gezimos.inkos.helper.device.DeviceHelper.isEinkDevice() }
    val ghostBrush = remember(color, isEink) {
        if (isEink) {
            val argb = color.toArgb()
            val tile = 3
            val bmp = android.graphics.Bitmap.createBitmap(tile, tile, android.graphics.Bitmap.Config.ARGB_8888)
            for (x in 0 until tile) for (y in 0 until tile) bmp.setPixel(x, y, 0)
            bmp.setPixel(1, 1, argb)
            androidx.compose.ui.graphics.ShaderBrush(
                android.graphics.BitmapShader(bmp, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
            )
        } else {
            androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.15f))
        }
    }
    Canvas(modifier = modifier.size(width = dotPitchDp * cols, height = dotPitchDp * rows)) {
        val pitch = dotPitchDp.toPx()
        val dRad = dotSize.toPx() / 2f
        for (row in 0 until rows) for (col in 0 until cols) {
            val filled = isDotFilled(col, row) ?: continue
            val cx = col * pitch + pitch / 2f
            val cy = row * pitch + pitch / 2f
            if (filled) {
                drawCircle(color, dRad, Offset(cx, cy))
            } else {
                drawCircle(brush = ghostBrush, radius = dRad, center = Offset(cx, cy))
            }
        }
    }
}

/** 17x5 dot matrix clock grid. */
@Composable
private fun MatrixClockGrid(
    clockText: String,
    color: Color,
    dotPitchDp: Dp,
    modifier: Modifier = Modifier
) {
    val hours = clockText.substringBefore(":").trim().padStart(2, '0')
    val minutes = clockText.substringAfter(":", "").trim().take(2).padStart(2, '0')
    val h1 = hours[0].digitToIntOrNull() ?: 0
    val h2 = hours[1].digitToIntOrNull() ?: 0
    val m1 = minutes[0].digitToIntOrNull() ?: 0
    val m2 = minutes[1].digitToIntOrNull() ?: 0
    val grid = remember(h1, h2, m1, m2) { buildClockGrid(h1, h2, m1, m2) }

    MatrixGrid(
        cols = 17, rows = 5, color = color, dotPitchDp = dotPitchDp,
        isDotFilled = { col, row -> grid[row][col] },
        modifier = modifier
    )
}

@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    color: Color,
    modifier: Modifier = Modifier,
    showTextIslands: Boolean = false,
    textIslandsInverted: Boolean = false,
    textIslandsShape: Int = 0,
    dotSize: Dp = 12.dp,
    dotSpacing: Dp = 10.dp,
    borderWidth: Dp = 2.dp
) {
    val density = LocalDensity.current
    val islandPaddingPx = with(density) { 2.dp.toPx() }
    
    // Determine colors based on invert setting
    val islandBackgroundColor = if (textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (textIslandsInverted) Theme.colors.text else Theme.colors.background
    
    val dotColor = if (showTextIslands) islandTextColor else color
    val borderColor = if (showTextIslands) islandTextColor else color
    val inactiveBackgroundColor = if (showTextIslands) islandBackgroundColor else Theme.colors.background
    
    Box(
        modifier = modifier
            .drawBehind {
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
            verticalArrangement = Arrangement.spacedBy(dotSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                            .size(dotSize)
                            .background(color = dotColor, shape = dotShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .background(color = inactiveBackgroundColor, shape = dotShape)
                            .border(width = borderWidth, color = borderColor, shape = dotShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipClockDigit(
    digit: Char,
    clockSize: Int,
    fontFamily: FontFamily,
    containerShape: RoundedCornerShape,
    borderColor: Color,
    bgColor: Color,
    textColor: Color,
    boxWidthDp: Dp,
    boxHeightDp: Dp,
    showCenterLine: Boolean = true,
    showBorder: Boolean = true
) {
    val density = LocalDensity.current
    val digitStyle = TextStyle(
        fontSize = clockSize.sp,
        fontFamily = fontFamily,
        lineHeight = clockSize.sp
    )
    Box(
        modifier = Modifier
            .size(boxWidthDp, boxHeightDp)
            .then(if (showBorder) Modifier.border(2.dp, borderColor, containerShape) else Modifier)
            .then(if (bgColor != Color.Transparent) Modifier.background(bgColor, containerShape) else Modifier)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        // Normal digit
        Text(
            text = digit.toString(),
            style = digitStyle.copy(color = textColor),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            maxLines = 1
        )
        if (showCenterLine) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen)
                    .drawWithContent {
                        val lineH = with(density) { 2.dp.toPx() }.coerceAtLeast(1f)
                        val cy = size.height / 2f
                        drawIntoCanvas { canvas ->
                            canvas.save()
                            canvas.clipRect(
                                left = 0f,
                                top = cy - lineH / 2f,
                                right = size.width,
                                bottom = cy + lineH / 2f
                            )
                        }
                        drawRect(textColor)
                        drawContent()
                        drawIntoCanvas { canvas -> canvas.restore() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit.toString(),
                    style = digitStyle.copy(color = bgColor),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    maxLines = 1
                )
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
    showTextIslands: Boolean = false,
    showEditMode: Boolean = false
) {
    val context = LocalContext.current
    val clockTypeface = remember(state.clockFont, state.clockCustomFontPath) {
        state.clockFont.getFont(context, state.clockCustomFontPath)
    }
    val dateTypeface = remember(state.dateFont, state.dateCustomFontPath) {
        state.dateFont.getFont(context, state.dateCustomFontPath)
    }
    val clockFontFamily = clockTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val dateFontFamily = dateTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val textAlign = when (state.clockAlignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val dateRowAlign = when (state.dateAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val dateRowTextAlign = when (state.dateAlignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val highlightColor = Theme.colors.text
    
    // Determine colors based on invert setting
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    val focusBackgroundColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background) else highlightColor
    val focusTextColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text) else Theme.colors.background

    BoxWithConstraints(modifier = modifier.fillMaxWidth().wrapContentHeight()) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val textMeasurer = rememberTextMeasurer()
        val localDensity = LocalDensity.current
        val screenScale = rememberScreenScale()

        val effectiveClockSize = remember(
            state.clockStyle, state.clockSize, state.clockText, state.secondClockText,
            state.showSecondClock, clockFontFamily, maxWidthPx, screenScale
        ) {
            val s = (state.clockSize * screenScale).toInt()
            when (state.clockStyle) {
                3, 8 -> { // Round / Analog: diameter = clockSize * 4 dp
                    val diameterPx = s * 4f * localDensity.density
                    if (diameterPx <= maxWidthPx) s
                    else (s * maxWidthPx / diameterPx).toInt().coerceAtLeast(Constants.MIN_CLOCK_SIZE)
                }
                1 -> { // Flip: 4 boxes of ~clockSize*0.95dp + gaps
                    val totalPx = (s * 0.95f * 4 + 36) * localDensity.density
                    if (totalPx <= maxWidthPx) s
                    else (s * maxWidthPx / totalPx).toInt().coerceAtLeast(Constants.MIN_CLOCK_SIZE)
                }
                7 -> { // Stacked: hours over minutes
                    val singleWidth = textMeasurer.measure(
                        text = state.clockText.substringBefore(":").padStart(2, '0'),
                        style = TextStyle(fontSize = s.sp, fontFamily = clockFontFamily),
                        maxLines = 1
                    ).size.width
                    if (singleWidth <= maxWidthPx.toInt()) s
                    else (s * maxWidthPx / singleWidth).toInt().coerceAtLeast(Constants.MIN_CLOCK_SIZE)
                }
                else -> { // Default (0,2,6), Split (4), Horizontal (5)
                    val measureText = if (state.showSecondClock) {
                        state.clockText + "  /  " + state.secondClockText
                    } else state.clockText
                    val measured = textMeasurer.measure(
                        text = measureText,
                        style = TextStyle(fontSize = s.sp, fontFamily = clockFontFamily),
                        maxLines = 1
                    )
                    if (measured.size.width <= maxWidthPx.toInt()) s
                    else (s * maxWidthPx / measured.size.width).toInt().coerceAtLeast(Constants.MIN_CLOCK_SIZE)
                }
            }
        }

        val effectiveDateSize = remember(state.dateText, state.dateSize, dateFontFamily, maxWidthPx, screenScale) {
            val s = (state.dateSize * screenScale).toInt()
            if (state.dateText.isBlank()) s
            else {
                val measured = textMeasurer.measure(
                    text = state.dateText,
                    style = TextStyle(fontSize = s.sp, fontFamily = dateFontFamily),
                    maxLines = 1
                )
                if (measured.size.width <= maxWidthPx.toInt()) s
                else (s * maxWidthPx / measured.size.width).toInt().coerceAtLeast(8)
            }
        }

        @Suppress("NAME_SHADOWING")
        val state = state.copy(clockSize = effectiveClockSize, dateSize = effectiveDateSize)
        val scaledClockSize = if (state.clockStyle in intArrayOf(3, 8)) state.clockSize * 0.75f else state.clockSize.toFloat()
        val scaledDateSize = state.dateSize.toFloat()

    Column(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = when (state.clockAlignment) {
            0 -> Alignment.Start
            2 -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    ) {
            if (state.showClock || showEditMode) {
                val amPmLabels = remember { DateFormatSymbols(Locale.getDefault()).amPmStrings }
                val amLabel = amPmLabels.getOrNull(0) ?: "AM"
                val pmLabel = amPmLabels.getOrNull(1) ?: "PM"
                val amPmSize = if (state.clockSize > 0) (kotlin.math.max(10f, state.clockSize * 0.28f)).sp else 10.sp

                val clockPillRadius = (state.clockSize * 2).dp
                val bgShape = remember(state.textIslandsShape, clockPillRadius) {
                    ShapeHelper.getRoundedCornerShape(
                        textIslandsShape = state.textIslandsShape,
                        pillRadius = clockPillRadius
                    )
                }

                if (state.clockStyle == 3) {
                    val roundBgShape = remember { CircleShape }
                    val roundSizeDp = (state.clockSize * 4f).dp
                    val ringStrokeDp = 4.dp
                    val progress = run {
                        val hourPart = state.clockText.substringBefore(":").trim().toIntOrNull() ?: 0
                        val minutePart = state.clockText.substringAfter(":", "0").trim().take(2).toIntOrNull() ?: 0
                        val m = minutePart.coerceIn(0, 59)
                        val hour24 = if (state.is24Hour) {
                            hourPart.coerceIn(0, 23)
                        } else {
                            val h12 = hourPart.coerceIn(1, 12)
                            val isPm = state.amPmText.equals(pmLabel, ignoreCase = true)
                            if (isPm) if (h12 == 12) 12 else h12 + 12 else if (h12 == 12) 0 else h12
                        }
                        (hour24 * 60 + m) / (24f * 60f)
                    }
                    val roundTextColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .then(
                                if (isClockFocused || showTextIslands) {
                                    val bgColor = when {
                                        isClockFocused && showTextIslands -> focusBackgroundColor
                                        isClockFocused -> highlightColor
                                        else -> islandBackgroundColor
                                    }
                                    Modifier.background(bgColor, roundBgShape).padding(8.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f)
                            .clickableNoRipple(onClockClick),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(roundSizeDp),
                            contentAlignment = Alignment.Center
                        ) {
                            val density = LocalDensity.current
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val centerX = size.minDimension / 2f
                                val centerY = size.minDimension / 2f
                                val ringStrokePx = with(density) { ringStrokeDp.toPx() }
                                val arcRadiusPx = (size.minDimension / 2f) - ringStrokePx / 2f
                                val arcRect = Rect(
                                    left = centerX - arcRadiusPx,
                                    top = centerY - arcRadiusPx,
                                    right = centerX + arcRadiusPx,
                                    bottom = centerY + arcRadiusPx
                                )
                                drawArc(
                                    color = roundTextColor,
                                    startAngle = 270f,
                                    sweepAngle = progress * 360f,
                                    useCenter = false,
                                    topLeft = Offset(arcRect.left, arcRect.top),
                                    size = Size(arcRect.width, arcRect.height),
                                    style = Stroke(width = ringStrokePx)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .wrapContentSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val roundSmallSize = (state.clockSize * 0.45f).coerceAtLeast(10f)
                                if (state.showBattery || showEditMode) {
                                    Row(
                                        modifier = Modifier.clickableNoRipple(onBatteryClick),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = state.batteryText,
                                            style = TextStyle(
                                                color = roundTextColor,
                                                fontSize = roundSmallSize.sp,
                                                fontFamily = dateFontFamily,
                                                lineHeight = roundSmallSize.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                        if (state.isCharging) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.OfflineBolt,
                                                contentDescription = stringResource(R.string.cd_charging),
                                                tint = roundTextColor,
                                                modifier = Modifier.size(roundSmallSize.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height((state.clockSize * 0.12f).dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickableNoRipple(onClockClick),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = state.clockText,
                                        style = TextStyle(
                                            color = roundTextColor,
                                            fontSize = scaledClockSize.sp,
                                            fontFamily = clockFontFamily,
                                            lineHeight = scaledClockSize.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                                if (state.dateText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height((state.clockSize * 0.12f).dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickableNoRipple(onDateClick),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.dateText,
                                            style = TextStyle(
                                                color = roundTextColor.copy(alpha = 0.9f),
                                                fontSize = roundSmallSize.sp,
                                                fontFamily = dateFontFamily,
                                                lineHeight = roundSmallSize.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (state.showNotificationCount || showEditMode) {
                                    Spacer(modifier = Modifier.height((state.clockSize * 0.12f).dp))
                                    Row(
                                        modifier = Modifier.clickableNoRipple(onNotificationCountClick),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "${state.notificationCount}",
                                            style = TextStyle(
                                                color = roundTextColor,
                                                fontSize = roundSmallSize.sp,
                                                fontFamily = dateFontFamily,
                                                lineHeight = roundSmallSize.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = stringResource(R.string.cd_notifications),
                                            tint = roundTextColor,
                                            modifier = Modifier.size(roundSmallSize.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (state.clockStyle == 1) {
                    val hoursPart = state.clockText.substringBefore(":").trim()
                    val minutesPart = state.clockText.substringAfter(":", "").trim().take(2)
                    val hoursPadded = hoursPart.padStart(2, '0').take(2)
                    val minutesPadded = minutesPart.padStart(2, '0').take(2)
                    val digitGap = 6.dp
                    val flipBoxWidth = (state.clockSize * 0.95f).dp
                    val flipBoxHeight = (state.clockSize * 1.35f).dp
                    val flipContainerShape = remember(state.textIslandsShape) {
                        val effectiveShape = if (state.textIslandsShape == 0) 1 else state.textIslandsShape
                        ShapeHelper.getRoundedCornerShape(
                            textIslandsShape = effectiveShape,
                            pillRadius = minOf(flipBoxWidth, flipBoxHeight) / 2,
                            roundedRadius = 8.dp
                        )
                    }
                    val flipBgColor = when {
                        isClockFocused && showTextIslands -> focusBackgroundColor
                        isClockFocused -> highlightColor
                        showTextIslands -> islandBackgroundColor
                        else -> Theme.colors.text
                    }
                    val flipTextColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.background
                    }
                    val flipBorderColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    val flipOutsideColor = Theme.colors.text
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(bottom = 16.dp)
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f)
                            .clickableNoRipple(onClockClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box {
                            FlipClockDigit(
                                digit = hoursPadded.getOrElse(0) { '0' },
                                clockSize = state.clockSize,
                                fontFamily = clockFontFamily,
                                containerShape = flipContainerShape,
                                borderColor = flipBorderColor,
                                bgColor = flipBgColor,
                                textColor = flipTextColor,
                                boxWidthDp = flipBoxWidth,
                                boxHeightDp = flipBoxHeight,
                                showCenterLine = true,
                                showBorder = false
                            )
                            if (!state.is24Hour && state.showAmPm && state.amPmText.isNotBlank()) {
                                val activeLabel = if (state.amPmText.equals(amLabel, ignoreCase = true)) amLabel else pmLabel
                                Text(
                                    text = activeLabel,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 4.dp, top = 4.dp),
                                    style = TextStyle(
                                        color = flipTextColor,
                                        fontSize = (scaledClockSize * 0.2f).sp,
                                        fontFamily = clockFontFamily,
                                        lineHeight = (scaledClockSize * 0.2f).sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(digitGap))
                        FlipClockDigit(
                            digit = hoursPadded.getOrElse(1) { '0' },
                            clockSize = state.clockSize,
                            fontFamily = clockFontFamily,
                            containerShape = flipContainerShape,
                            borderColor = flipBorderColor,
                            bgColor = flipBgColor,
                            textColor = flipTextColor,

                            boxWidthDp = flipBoxWidth,
                            boxHeightDp = flipBoxHeight,
                            showCenterLine = true,
                            showBorder = false
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = ":",
                            style = TextStyle(
                                color = flipOutsideColor,
                                fontSize = scaledClockSize.sp,
                                fontFamily = clockFontFamily,
                                lineHeight = scaledClockSize.sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FlipClockDigit(
                            digit = minutesPadded.getOrElse(0) { '0' },
                            clockSize = state.clockSize,
                            fontFamily = clockFontFamily,
                            containerShape = flipContainerShape,
                            borderColor = flipBorderColor,
                            bgColor = flipBgColor,
                            textColor = flipTextColor,

                            boxWidthDp = flipBoxWidth,
                            boxHeightDp = flipBoxHeight,
                            showCenterLine = true,
                            showBorder = false
                        )
                        Spacer(modifier = Modifier.width(digitGap))
                        FlipClockDigit(
                            digit = minutesPadded.getOrElse(1) { '0' },
                            clockSize = state.clockSize,
                            fontFamily = clockFontFamily,
                            containerShape = flipContainerShape,
                            borderColor = flipBorderColor,
                            bgColor = flipBgColor,
                            textColor = flipTextColor,

                            boxWidthDp = flipBoxWidth,
                            boxHeightDp = flipBoxHeight,
                            showCenterLine = true,
                            showBorder = false
                        )
                    }
                } else if (state.clockStyle == 4) {
                    val splitHours = state.clockText.substringBefore(":").trim()
                    val splitMinutes = state.clockText.substringAfter(":", "").trim().take(2)
                    val splitSmallSize = (state.clockSize * 0.35f).coerceAtLeast(14f)
                    val splitColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    val splitContentHeight = (state.clockSize * 1.1f).dp + 4.dp + (splitSmallSize * 0.9f).dp
                    val splitLineHeight = splitContentHeight + 16.dp
                    val splitBlockAlignment = when (state.clockAlignment) {
                        0 -> Alignment.CenterStart
                        2 -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = splitBlockAlignment
                    ) {
                        Row(
                            modifier = Modifier
                                .then(
                                    if (isClockFocused || showTextIslands) {
                                        val bgColor = when {
                                            isClockFocused && showTextIslands -> focusBackgroundColor
                                            isClockFocused -> highlightColor
                                            else -> islandBackgroundColor
                                        }
                                        Modifier.background(bgColor, bgShape).padding(vertical = 4.dp, horizontal = 12.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Column(horizontalAlignment = Alignment.End) {
                                Box(modifier = Modifier.clickableNoRipple(onClockClick)) {
                                    Text(
                                        text = splitHours,
                                        style = TextStyle(color = splitColor, fontSize = scaledClockSize.sp, fontFamily = clockFontFamily, lineHeight = scaledClockSize.sp),
                                        maxLines = 1
                                    )
                                }
                                if (state.dateText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.clickableNoRipple(onDateClick)) {
                                        Text(
                                            text = state.dateText,
                                            style = TextStyle(color = splitColor, fontSize = splitSmallSize.sp, fontFamily = dateFontFamily, lineHeight = splitSmallSize.sp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(splitLineHeight)
                                .background(splitColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))
                        Column(horizontalAlignment = Alignment.Start) {
                                Box(modifier = Modifier.clickableNoRipple(onClockClick)) {
                                    Text(
                                        text = splitMinutes,
                                        style = TextStyle(color = splitColor, fontSize = scaledClockSize.sp, fontFamily = clockFontFamily, lineHeight = scaledClockSize.sp),
                                        maxLines = 1
                                    )
                                }
                                if (state.showBattery || state.showNotificationCount || showEditMode) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickableNoRipple(onBatteryClick)
                                                .graphicsLayer(alpha = if (showEditMode && !state.showBattery) 0.5f else 1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = state.batteryText,
                                                style = TextStyle(color = splitColor, fontSize = splitSmallSize.sp, fontFamily = dateFontFamily),
                                                maxLines = 1
                                            )
                                            if (state.isCharging) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Rounded.OfflineBolt, contentDescription = stringResource(R.string.cd_charging), tint = splitColor, modifier = Modifier.size(splitSmallSize.dp))
                                            }
                                        }
                                        if (state.showNotificationCount || showEditMode) {
                                            Text(text = " · ", style = TextStyle(color = splitColor, fontSize = splitSmallSize.sp), maxLines = 1)
                                        }
                                        if (state.showNotificationCount || showEditMode) {
                                        Row(
                                            modifier = Modifier
                                                .clickableNoRipple(onNotificationCountClick)
                                                .graphicsLayer(alpha = if (showEditMode && !state.showNotificationCount) 0.5f else 1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${state.notificationCount}",
                                                style = TextStyle(color = splitColor, fontSize = splitSmallSize.sp, fontFamily = dateFontFamily),
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.cd_notifications), tint = splitColor, modifier = Modifier.size(splitSmallSize.dp))
                                        }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (state.clockStyle == 5) {
                    val horizSmallSize = (state.clockSize * 0.35f).coerceAtLeast(14f)
                    val horizColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    val horizDateColumnAlign = if (state.clockAlignment == 2) Alignment.End else Alignment.Start
                    val horizDateRowArrangement = if (state.clockAlignment == 2) Arrangement.End else Arrangement.Start
                    val horizDateTextAlign = if (state.clockAlignment == 2) TextAlign.End else TextAlign.Start
                    val horizDateColumn = @Composable {
                        Column(
                            modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min),
                            horizontalAlignment = horizDateColumnAlign
                        ) {
                            if (state.dateText.isNotBlank()) {
                                Box(modifier = Modifier.clickableNoRipple(onDateClick)) {
                                    Text(
                                        text = state.dateText,
                                        style = TextStyle(color = horizColor, fontSize = horizSmallSize.sp, fontFamily = dateFontFamily, lineHeight = horizSmallSize.sp),
                                        textAlign = horizDateTextAlign,
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(horizColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = horizDateRowArrangement
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickableNoRipple(onBatteryClick)
                                            .graphicsLayer(alpha = if (showEditMode && !state.showBattery) 0.5f else 1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = state.batteryText,
                                            style = TextStyle(color = horizColor, fontSize = horizSmallSize.sp, fontFamily = dateFontFamily),
                                            textAlign = horizDateTextAlign,
                                            maxLines = 1
                                        )
                                        if (state.isCharging) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(modifier = Modifier.size(horizSmallSize.dp), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.OfflineBolt, contentDescription = stringResource(R.string.cd_charging), tint = horizColor, modifier = Modifier.requiredSize((horizSmallSize * 1.1f).dp))
                                            }
                                        }
                                    }
                                    if (state.showNotificationCount || showEditMode) {
                                        Text(text = " · ", style = TextStyle(color = horizColor, fontSize = horizSmallSize.sp), maxLines = 1)
                                    }
                                    if (state.showNotificationCount || showEditMode) {
                                        Row(
                                            modifier = Modifier
                                                .clickableNoRipple(onNotificationCountClick)
                                                .graphicsLayer(alpha = if (showEditMode && !state.showNotificationCount) 0.5f else 1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${state.notificationCount}",
                                                style = TextStyle(color = horizColor, fontSize = horizSmallSize.sp, fontFamily = dateFontFamily),
                                                textAlign = horizDateTextAlign,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(modifier = Modifier.size(horizSmallSize.dp), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.cd_notifications), tint = horizColor, modifier = Modifier.requiredSize((horizSmallSize * 1.1f).dp))
                                            }
                                        }
                                    }
                                }
                        }
                    }
                    val horizTimeBlock = @Composable {
                        Box(modifier = Modifier.clickableNoRipple(onClockClick)) {
                            Text(
                                text = state.clockText,
                                style = TextStyle(color = horizColor, fontSize = scaledClockSize.sp, fontFamily = clockFontFamily, lineHeight = scaledClockSize.sp),
                                maxLines = 1
                            )
                        }
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
                                    Modifier.background(bgColor, bgShape).padding(horizontal = 8.dp, vertical = 4.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (state.clockAlignment == 2) {
                            horizDateColumn()
                            Spacer(modifier = Modifier.width(20.dp.scaled(screenScale)))
                            horizTimeBlock()
                        } else {
                            horizTimeBlock()
                            Spacer(modifier = Modifier.width(20.dp.scaled(screenScale)))
                            horizDateColumn()
                        }
                    }
                } else if (state.clockStyle == 7) {
                    val stackedHours = state.clockText.substringBefore(":").trim().padStart(2, '0').take(2)
                    val stackedMinutes = state.clockText.substringAfter(":", "").trim().take(2).padStart(2, '0')
                    val stackedColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    val stackedBgModifier = when {
                        isClockFocused && showTextIslands -> Modifier.background(focusBackgroundColor, bgShape).padding(horizontal = 8.dp, vertical = 4.dp)
                        isClockFocused -> Modifier.background(highlightColor, bgShape).padding(horizontal = 8.dp, vertical = 4.dp)
                        showTextIslands -> Modifier.background(islandBackgroundColor, bgShape).padding(horizontal = 8.dp, vertical = 4.dp)
                        else -> Modifier
                    }

                    val stackedTextStyle = TextStyle(
                        color = stackedColor,
                        fontSize = scaledClockSize.sp,
                        fontFamily = clockFontFamily,
                        lineHeight = scaledClockSize.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )

                    @Composable
                    fun StackedClock(hours: String, minutes: String) {
                        Text(
                            text = "$hours\n$minutes",
                            style = stackedTextStyle,
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .then(stackedBgModifier)
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f)
                            .clickableNoRipple(onClockClick)
                    ) {
                        StackedClock(stackedHours, stackedMinutes)
                    }
                } else if (state.clockStyle == 8) {
                    val analogSizeDp = (state.clockSize * 4f).dp
                    val analogStrokeDp = 2.dp
                    val hourHandWidth = 4.dp
                    val minuteHandWidth = 2.dp
                    val centerDotRadius = 4.dp
                    val hourAngle = run {
                        val hourPart = state.clockText.substringBefore(":").trim().toIntOrNull() ?: 0
                        val minutePart = state.clockText.substringAfter(":", "0").trim().take(2).toIntOrNull() ?: 0
                        val m = minutePart.coerceIn(0, 59)
                        val hour12 = if (state.is24Hour) {
                            hourPart.coerceIn(0, 23) % 12
                        } else {
                            val h = hourPart.coerceIn(1, 12)
                            if (h == 12) 0 else h
                        }
                        (hour12 + m / 60f) * 30f
                    }
                    val minuteAngle = run {
                        val minutePart = state.clockText.substringAfter(":", "0").trim().take(2).toIntOrNull() ?: 0
                        minutePart.coerceIn(0, 59) * 6f
                    }
                    val analogColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .then(
                                if (isClockFocused || showTextIslands) {
                                    val bgColor = when {
                                        isClockFocused && showTextIslands -> focusBackgroundColor
                                        isClockFocused -> highlightColor
                                        else -> islandBackgroundColor
                                    }
                                    Modifier.background(bgColor, remember { CircleShape }).padding(8.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f)
                            .clickableNoRipple(onClockClick),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(analogSizeDp),
                            contentAlignment = Alignment.Center
                        ) {
                            val density = LocalDensity.current
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val cx = size.minDimension / 2f
                                val cy = size.minDimension / 2f
                                val strokePx = with(density) { analogStrokeDp.toPx() }
                                val radius = (size.minDimension / 2f) - strokePx

                                // Circle outline
                                drawCircle(
                                    color = analogColor,
                                    radius = radius,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = strokePx)
                                )

                                // Hour hand (55% of radius, thick)
                                val hourLen = radius * 0.55f
                                val hourRad = Math.toRadians((hourAngle - 90f).toDouble())
                                val hourEndX = cx + (hourLen * cos(hourRad)).toFloat()
                                val hourEndY = cy + (hourLen * sin(hourRad)).toFloat()
                                drawLine(
                                    color = analogColor,
                                    start = Offset(cx, cy),
                                    end = Offset(hourEndX, hourEndY),
                                    strokeWidth = with(density) { hourHandWidth.toPx() },
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )

                                // Minute hand (80% of radius, thinner)
                                val minLen = radius * 0.80f
                                val minRad = Math.toRadians((minuteAngle - 90f).toDouble())
                                val minEndX = cx + (minLen * cos(minRad)).toFloat()
                                val minEndY = cy + (minLen * sin(minRad)).toFloat()
                                drawLine(
                                    color = analogColor,
                                    start = Offset(cx, cy),
                                    end = Offset(minEndX, minEndY),
                                    strokeWidth = with(density) { minuteHandWidth.toPx() },
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )

                                // Center dot
                                drawCircle(
                                    color = analogColor,
                                    radius = with(density) { centerDotRadius.toPx() },
                                    center = Offset(cx, cy)
                                )
                            }
                        }

                    }
                } else if (state.clockStyle == 9) {
                    val digitalColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    val digitalSizeDp = (state.clockSize * 1.8f).dp
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .then(
                                when {
                                    isClockFocused && showTextIslands -> Modifier.background(focusBackgroundColor, bgShape).padding(horizontal = 12.dp, vertical = 8.dp)
                                    isClockFocused -> Modifier.background(highlightColor, bgShape).padding(horizontal = 12.dp, vertical = 8.dp)
                                    showTextIslands -> Modifier.background(islandBackgroundColor, bgShape).padding(horizontal = 12.dp, vertical = 8.dp)
                                    else -> Modifier
                                }
                            )
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f)
                            .clickableNoRipple(onClockClick)
                    ) {
                        SevenSegmentClock(
                            clockText = state.clockText,
                            color = digitalColor,
                            sizeDp = digitalSizeDp
                        )
                    }
                } else if (state.clockStyle == 10) {
                    val matrixColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    val matrixDotPitch = (state.clockSize * 0.22f).dp
                    val matrixLabelSize = (state.clockSize * 0.28f).coerceAtLeast(10f).sp
                    val matrixBgMod = when {
                        isClockFocused && showTextIslands -> Modifier.background(focusBackgroundColor, bgShape).padding(horizontal = 8.dp, vertical = 6.dp)
                        isClockFocused -> Modifier.background(highlightColor, bgShape).padding(horizontal = 8.dp, vertical = 6.dp)
                        showTextIslands -> Modifier.background(islandBackgroundColor, bgShape).padding(horizontal = 8.dp, vertical = 6.dp)
                        else -> Modifier
                    }
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .then(matrixBgMod)
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Calendar grid
                        if (state.showDate || showEditMode) {
                            MatrixGrid(
                                cols = 7, rows = 5, color = matrixColor, dotPitchDp = matrixDotPitch,
                                isDotFilled = { col, row ->
                                    val cal = java.util.Calendar.getInstance()
                                    val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
                                    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                                    val firstCal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1) }
                                    val firstDow = (firstCal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
                                    val dayNum = row * 7 + col - firstDow + 1
                                    when {
                                        dayNum in 1..dayOfMonth -> true
                                        dayNum in (dayOfMonth + 1)..daysInMonth -> false
                                        else -> null
                                    }
                                },
                                modifier = Modifier
                                    .clickableNoRipple(onDateClick)
                                    .graphicsLayer(alpha = if (showEditMode && !state.showDate) 0.5f else 1f)
                            )
                            // Separator column (invisible)
                            MatrixGrid(cols = 1, rows = 5, color = matrixColor, dotPitchDp = matrixDotPitch, isDotFilled = { _, _ -> false })
                        }
                        // Clock grid
                        MatrixClockGrid(
                            clockText = state.clockText, color = matrixColor, dotPitchDp = matrixDotPitch,
                            modifier = Modifier.clickableNoRipple(onClockClick)
                        )
                        // Battery grid
                        if (state.showBattery || showEditMode) {
                            // Separator column (invisible)
                            MatrixGrid(cols = 1, rows = 5, color = matrixColor, dotPitchDp = matrixDotPitch, isDotFilled = { _, _ -> false })
                            val batteryPct = (state.batteryText.filter { it.isDigit() }.toIntOrNull() ?: 0).coerceIn(0, 100)
                            val filledDots = (batteryPct * 35 / 100).coerceIn(0, 35)
                            MatrixGrid(
                                cols = 7, rows = 5, color = matrixColor, dotPitchDp = matrixDotPitch,
                                isDotFilled = { col, row ->
                                    val idx = row * 7 + col
                                    if (idx < filledDots) true else false
                                },
                                modifier = Modifier
                                    .clickableNoRipple(onBatteryClick)
                                    .graphicsLayer(alpha = if (showEditMode && !state.showBattery) 0.5f else 1f)
                            )
                        }
                    }
                } else {
                    // Default (0), Box Solid (2), Box Outline (6)
                    val isBoxSolid = state.clockStyle == 2
                    val isBoxOutline = state.clockStyle == 6
                    val defaultTextColor = when {
                        isClockFocused && showTextIslands -> focusTextColor
                        isClockFocused -> Theme.colors.background
                        isBoxSolid -> Theme.colors.background
                        showTextIslands -> islandTextColor
                        else -> Theme.colors.text
                    }
                    Row(
                        modifier = Modifier
                            .wrapContentSize()
                            .then(
                                when {
                                    isClockFocused || (isBoxSolid && showTextIslands) -> {
                                        val bgColor = when {
                                            isClockFocused && showTextIslands -> focusBackgroundColor
                                            isClockFocused -> highlightColor
                                            else -> islandBackgroundColor
                                        }
                                        Modifier.background(bgColor, bgShape).padding(horizontal = 16.dp, vertical = 4.dp)
                                    }
                                    isBoxSolid -> {
                                        Modifier.background(Theme.colors.text, bgShape).padding(horizontal = 16.dp, vertical = 4.dp)
                                    }
                                    isBoxOutline && showTextIslands -> {
                                        Modifier.background(islandBackgroundColor, bgShape)
                                            .border(width = 2.dp, color = islandTextColor, shape = bgShape)
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    }
                                    isBoxOutline -> {
                                        Modifier.border(width = 2.dp, color = Theme.colors.text, shape = bgShape).padding(horizontal = 16.dp, vertical = 4.dp)
                                    }
                                    showTextIslands -> {
                                        Modifier.background(islandBackgroundColor, bgShape).padding(horizontal = 8.dp)
                                    }
                                    else -> Modifier
                                }
                            )
                            .graphicsLayer(alpha = if (showEditMode && !state.showClock) 0.5f else 1f)
                            .clickableNoRipple(onClockClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.clockText,
                            modifier = Modifier.wrapContentHeight(),
                            style = TextStyle(
                                color = defaultTextColor,
                                fontSize = scaledClockSize.sp,
                                fontFamily = clockFontFamily,
                                lineHeight = scaledClockSize.sp
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
                                    color = defaultTextColor,
                                    fontSize = amPmSize,
                                    fontFamily = clockFontFamily,
                                    lineHeight = amPmSize
                                ),
                                maxLines = 1
                            )
                        }

                        if (state.showSecondClock) {
                            Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))
                            Text(
                                text = "/",
                                modifier = Modifier.wrapContentHeight(),
                                style = TextStyle(
                                    color = defaultTextColor.copy(alpha = 0.9f),
                                    fontSize = (scaledClockSize * 0.5f).sp,
                                    fontFamily = clockFontFamily,
                                    lineHeight = (scaledClockSize * 0.5f).sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.secondClockText,
                                modifier = Modifier.wrapContentHeight(),
                                style = TextStyle(
                                    color = defaultTextColor,
                                    fontSize = scaledClockSize.sp,
                                    fontFamily = clockFontFamily,
                                    lineHeight = scaledClockSize.sp
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
                                        color = defaultTextColor,
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
            }
            Spacer(modifier = Modifier.height(6.dp))
        if ((state.showDate || state.showBattery || state.showNotificationCount || showEditMode) && state.clockStyle !in 3..5 && state.clockStyle != 10) {
            val bgShape = remember(state.textIslandsShape) {
                ShapeHelper.getRoundedCornerShape(
                    textIslandsShape = state.textIslandsShape,
                    pillRadius = 50.dp
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = when (dateRowAlign) {
                    Alignment.Start -> Alignment.CenterStart
                    Alignment.End -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
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
                            Modifier.background(bgColor, bgShape).padding(4.dp)
                        } else {
                            Modifier.padding(vertical = 2.dp)
                        }
                    ),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    if (state.showDate || showEditMode) {
                        Box(
                            modifier = Modifier
                                .clickableNoRipple(onDateClick)
                                .graphicsLayer(alpha = if (showEditMode && !state.showDate) 0.5f else 1f)
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
                                    fontSize = scaledDateSize.sp,
                                    fontFamily = dateFontFamily,
                                    textAlign = dateRowTextAlign
                                ),
                                maxLines = 1
                            )
                        }
                    }
                    
                    if (((state.showDate || showEditMode) && state.showBattery) || showEditMode) {
                        Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))
                        Text(
                            text = "·",
                            style = TextStyle(
                                color = when {
                                    isDateFocused && showTextIslands -> focusTextColor
                                    isDateFocused -> Theme.colors.background
                                    showTextIslands -> islandTextColor
                                    else -> Theme.colors.text
                                },
                                fontSize = scaledDateSize.sp,
                                fontFamily = dateFontFamily,
                                textAlign = dateRowTextAlign
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))
                    }
                    
                    if (state.showBattery || showEditMode) {
                        Box(
                            modifier = Modifier
                                .clickableNoRipple(onBatteryClick)
                                .graphicsLayer(alpha = if (showEditMode && !state.showBattery) 0.5f else 1f)
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
                                        fontSize = scaledDateSize.sp,
                                        fontFamily = dateFontFamily,
                                        textAlign = dateRowTextAlign
                                    ),
                                    maxLines = 1
                                )

                                if (state.isCharging) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.OfflineBolt,
                                        contentDescription = stringResource(R.string.cd_charging),
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
                    
                    if ((((state.showDate || state.showBattery) && state.showNotificationCount) || (showEditMode && (state.showBattery || state.showNotificationCount))) || showEditMode) {
                        Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))
                        Text(
                            text = "·",
                            style = TextStyle(
                                color = when {
                                    isDateFocused && showTextIslands -> focusTextColor
                                    isDateFocused -> Theme.colors.background
                                    showTextIslands -> islandTextColor
                                    else -> Theme.colors.text
                                },
                                fontSize = scaledDateSize.sp,
                                fontFamily = dateFontFamily,
                                textAlign = dateRowTextAlign
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))
                    }
                    
                    if (state.showNotificationCount || showEditMode) {
                        Box(
                            modifier = Modifier
                                .clickableNoRipple(onNotificationCountClick)
                                .graphicsLayer(alpha = if (showEditMode && !state.showNotificationCount) 0.5f else 1f)
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
                                        fontSize = scaledDateSize.sp,
                                        fontFamily = dateFontFamily,
                                        textAlign = dateRowTextAlign
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = stringResource(R.string.cd_notifications),
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
    } // Column
    } // BoxWithConstraints

@Composable
private fun QuoteBlock(
    state: HomeUiRenderState,
    onClick: () -> Unit,
    isFocused: Boolean = false,
    showTextIslands: Boolean = false,
    showEditMode: Boolean = false
) {
    val context = LocalContext.current
    val quoteTypeface = remember(state.quoteFont, state.quoteCustomFontPath) {
        state.quoteFont.getFont(context, state.quoteCustomFontPath)
    }
    val quoteFontFamily = quoteTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val scaledQuoteSize = state.quoteSize * rememberScreenScale()

    val textAlign = when (state.quoteAlignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val blockAlignment = when (state.quoteAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val highlightColor = Theme.colors.text

    // Determine colors based on invert setting
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    val focusBackgroundColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background) else highlightColor
    val focusTextColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text) else Theme.colors.background

    val bgShape = remember(state.textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = state.textIslandsShape,
            pillRadius = 50.dp
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (showEditMode && state.bottomWidgetType != Constants.BottomWidgetType.Quote.value) 0.5f else 1f),
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
                fontSize = scaledQuoteSize.sp,
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
private fun TotalUsageBlock(
    state: HomeUiRenderState,
    onClick: () -> Unit,
    isFocused: Boolean = false,
    showTextIslands: Boolean = false,
    showEditMode: Boolean = false
) {
    val context = LocalContext.current
    val quoteTypeface = remember(state.quoteFont, state.quoteCustomFontPath) {
        state.quoteFont.getFont(context, state.quoteCustomFontPath)
    }
    val quoteFontFamily = quoteTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val scaledQuoteSize = state.quoteSize * rememberScreenScale()

    val textAlign = when (state.quoteAlignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val blockAlignment = when (state.quoteAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val highlightColor = Theme.colors.text
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    val focusBackgroundColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background) else highlightColor
    val focusTextColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text) else Theme.colors.background

    val bgShape = remember(state.textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = state.textIslandsShape,
            pillRadius = 50.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (showEditMode && state.bottomWidgetType != Constants.BottomWidgetType.TotalUsage.value) 0.5f else 1f),
        horizontalAlignment = blockAlignment
    ) {
        Text(
            text = state.totalUsageText,
            style = TextStyle(
                color = when {
                    isFocused && showTextIslands -> focusTextColor
                    isFocused -> Theme.colors.background
                    showTextIslands -> islandTextColor
                    else -> Theme.colors.text
                },
                fontSize = scaledQuoteSize.sp,
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
private fun EventsBlock(
    state: HomeUiRenderState,
    callbacks: HomeUiCallbacks,
    onBottomWidgetClick: () -> Unit,
    isEditMode: Boolean = false,
    isFocused: Boolean = false,
    showTextIslands: Boolean = false
) {
    val context = LocalContext.current
    LocalDensity.current
    val scaledQuoteSize = state.quoteSize * rememberScreenScale()
    val highlightColor = Theme.colors.text
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    val focusBackgroundColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background) else highlightColor
    val focusTextColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text) else Theme.colors.background
    val textColor = when {
        isFocused && showTextIslands -> focusTextColor
        isFocused -> Theme.colors.background
        showTextIslands -> islandTextColor
        else -> Color(state.textColor)
    }
    val quoteTypeface = remember(state.quoteFont, state.quoteCustomFontPath) {
        state.quoteFont.getFont(context, state.quoteCustomFontPath)
    }
    val quoteFontFamily = quoteTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val blockAlignment = when (state.quoteAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val defaultQuoteSize = remember { context.resources.getDimensionPixelSize(R.dimen.default_quote_size) / context.resources.displayMetrics.scaledDensity }
    val iconScale = if (defaultQuoteSize > 0) state.quoteSize / defaultQuoteSize else 1f
    val iconButtonSize = (36 * iconScale).dp
    val buttonShape = remember(state.textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = state.textIslandsShape,
            pillRadius = iconButtonSize / 2,
            roundedRadius = 8.dp
        )
    }
    val bgShape = remember(state.textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = state.textIslandsShape,
            pillRadius = 50.dp
        )
    }

    val isRightAligned = state.quoteAlignment == 2
    val textAlign = when (state.quoteAlignment) {
        2 -> TextAlign.End
        1 -> TextAlign.Center
        else -> TextAlign.Start
    }

    // Shared composable lambdas to avoid duplication
    val hideControls = state.eventsHideControls
    val calendarIcon: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(iconButtonSize)
                .background(Theme.colors.background, buttonShape)
                .border(1.5.dp, textColor, buttonShape)
                .clip(buttonShape)
                .then(if (isEditMode) Modifier else Modifier.clickableNoRipple(callbacks.onEventsCalendarClick)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = stringResource(R.string.events_choose_calendar),
                tint = textColor,
                modifier = Modifier.size((iconButtonSize * 0.6f))
            )
        }
    }

    val chevronSize = (28 * iconScale).dp
    val chevrons: @Composable () -> Unit = {
        val canPrev = state.eventsIndex > 0 && state.eventsList.isNotEmpty()
        val canNext = state.eventsIndex < state.eventsList.size - 1 && state.eventsList.isNotEmpty()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.cd_previous_event),
                tint = if (canPrev) textColor else textColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(chevronSize)
                    .clip(androidx.compose.ui.graphics.RectangleShape)
                    .graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }
                    .then(
                        when {
                            isEditMode -> Modifier
                            canPrev -> Modifier.combinedClickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {
                                    try { VibrationHelper.trigger(VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                                    GestureHelper.notifyElementClicked()
                                    callbacks.onEventsPrevClick()
                                },
                                onLongClick = {
                                    try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                    GestureHelper.notifyElementClicked()
                                    callbacks.onEventsFirstClick()
                                }
                            )
                            else -> Modifier.clickableNoRipple { }
                        }
                    )
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = stringResource(R.string.cd_next_event),
                tint = if (canNext) textColor else textColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(chevronSize)
                    .clip(androidx.compose.ui.graphics.RectangleShape)
                    .graphicsLayer { scaleX = 1.25f; scaleY = 1.25f }
                    .then(
                        when {
                            isEditMode -> Modifier
                            canNext -> Modifier.combinedClickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {
                                    try { VibrationHelper.trigger(VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                                    GestureHelper.notifyElementClicked()
                                    callbacks.onEventsNextClick()
                                },
                                onLongClick = {
                                    try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                    GestureHelper.notifyElementClicked()
                                    callbacks.onEventsLastClick()
                                }
                            )
                            else -> Modifier.clickableNoRipple { }
                        }
                    )
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFocused || showTextIslands) {
                    val bgColor = when {
                        isFocused && showTextIslands -> focusBackgroundColor
                        isFocused -> highlightColor
                        else -> islandBackgroundColor
                    }
                    Modifier.background(bgColor, bgShape).padding(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    Modifier
                }
            )
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                GestureHelper.exclusionY = pos.y
                GestureHelper.exclusionHeight = coords.size.height.toFloat()
            }
            .then(
                if (!isEditMode && state.eventsList.size > 1) {
                    val thresholdPx = with(LocalDensity.current) { 40.dp.toPx() }
                    var dragTotal = 0f
                    var triggered = false
                    Modifier.draggable(
                        state = rememberDraggableState { delta ->
                            dragTotal += delta
                            if (!triggered && abs(dragTotal) > thresholdPx) {
                                triggered = true
                                GestureHelper.notifyElementClicked()
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                                if (dragTotal < 0 && state.eventsIndex < state.eventsList.size - 1) {
                                    callbacks.onEventsNextClick()
                                } else if (dragTotal > 0 && state.eventsIndex > 0) {
                                    callbacks.onEventsPrevClick()
                                }
                            }
                        },
                        orientation = Orientation.Horizontal,
                        onDragStarted = { dragTotal = 0f; triggered = false },
                    )
                } else Modifier
            )
            .clickableNoRipple(onBottomWidgetClick),
        horizontalArrangement = if (isRightAligned) Arrangement.End else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!hideControls) {
            if (isRightAligned) chevrons() else calendarIcon()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (hideControls) Modifier else Modifier.padding(start = 16.dp, end = 16.dp))
                .then(
                    when {
                        isEditMode -> Modifier
                        !state.hasCalendarPermission -> Modifier.clickableNoRipple(callbacks.onEventsGrantPermissionClick)
                        state.eventsList.isNotEmpty() -> Modifier.clickableNoRipple {
                            val event = state.eventsList.getOrNull(state.eventsIndex.coerceIn(0, state.eventsList.size - 1))
                                ?: state.eventsList.first()
                            callbacks.onEventsEventClick(event)
                        }
                        else -> Modifier
                    }
                ),
            horizontalAlignment = blockAlignment
        ) {
            when {
                !state.hasCalendarPermission -> {
                    Text(
                        text = stringResource(R.string.events_grant_permission) + " " + stringResource(R.string.events_grant_permission_subtitle),
                        style = TextStyle(
                            color = textColor,
                            fontSize = scaledQuoteSize.sp,
                            fontFamily = quoteFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                state.eventsCalendarId == -1L -> {
                    Text(
                        text = stringResource(R.string.events_no_calendar),
                        style = TextStyle(
                            color = textColor,
                            fontSize = scaledQuoteSize.sp,
                            fontFamily = quoteFontFamily
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                state.eventsList.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.events_no_events),
                        style = TextStyle(
                            color = textColor,
                            fontSize = scaledQuoteSize.sp,
                            fontFamily = quoteFontFamily
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                else -> {
                    val event = state.eventsList.getOrNull(state.eventsIndex.coerceIn(0, state.eventsList.size - 1))
                        ?: state.eventsList.first()
                    val dateStr = formatEventDate(event.beginTime)
                    val locationStr = event.location?.takeIf { it.isNotBlank() }
                    val titleLine = buildString {
                        append(dateStr)
                        if (locationStr != null) append(" • ").append(locationStr)
                    }
                    Text(
                        text = titleLine,
                        style = TextStyle(
                            color = textColor,
                            fontSize = (scaledQuoteSize * 0.75f).sp,
                            fontFamily = quoteFontFamily,
                            fontWeight = FontWeight.Bold,
                            textAlign = textAlign
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = event.title.ifBlank { stringResource(R.string.events_no_events) },
                        style = TextStyle(
                            color = textColor.copy(alpha = 0.9f),
                            fontSize = (scaledQuoteSize * 0.95f).sp,
                            fontFamily = quoteFontFamily,
                            textAlign = textAlign
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!hideControls) {
            if (isRightAligned) calendarIcon() else chevrons()
        }
    }
}

private fun formatEventDate(beginTime: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = beginTime
    val today = java.util.Calendar.getInstance()
    val isToday = cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
            cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
    return if (isToday) {
        java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(beginTime))
            .let { "Today $it" }
    } else {
        java.text.SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(java.util.Date(beginTime))
    }
}

internal fun Constants.ShortcutIcon.toImageVector(): ImageVector = when (this) {
    Constants.ShortcutIcon.Search -> Icons.Rounded.Search
    Constants.ShortcutIcon.Phone -> Icons.Rounded.Phone
    Constants.ShortcutIcon.Messages -> Icons.AutoMirrored.Rounded.Chat
    Constants.ShortcutIcon.Camera -> Icons.Rounded.CameraAlt
    Constants.ShortcutIcon.Notes -> Icons.Rounded.EditNote
    Constants.ShortcutIcon.Bubble -> Icons.Rounded.Reviews
    Constants.ShortcutIcon.Music -> Icons.Rounded.LibraryMusic
    Constants.ShortcutIcon.Light -> Icons.Rounded.LightMode
    Constants.ShortcutIcon.Star -> Icons.Rounded.Star
    Constants.ShortcutIcon.Clock -> Icons.Rounded.AccessTime
    Constants.ShortcutIcon.Disabled -> Icons.Rounded.Search
}

@Composable
private fun ShortcutsBlock(
    state: HomeUiRenderState,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onBottomWidgetClick: () -> Unit,
    isEditMode: Boolean = false,
    isFocused: Boolean = false,
    showTextIslands: Boolean = false
) {
    val highlightColor = Theme.colors.text
    val islandBackgroundColor = if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background
    val focusBackgroundColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.text else Theme.colors.background) else highlightColor
    val focusTextColor = if (showTextIslands) (if (state.textIslandsInverted) Theme.colors.background else Theme.colors.text) else Theme.colors.background
    val iconColor = when {
        isFocused && showTextIslands -> focusTextColor
        isFocused -> Theme.colors.background
        showTextIslands -> islandTextColor
        else -> Color(state.textColor)
    }
    val iconButtonSize = (state.quoteSize * 1.5f).dp
    val buttonShape = remember(state.textIslandsShape, iconButtonSize) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = state.textIslandsShape,
            pillRadius = iconButtonSize / 2,
            roundedRadius = 8.dp
        )
    }
    val showLeft = state.shortcutLeftIcon != Constants.ShortcutIcon.Disabled
    val showRight = state.shortcutRightIcon != Constants.ShortcutIcon.Disabled

    val islandBgModifier = if (isFocused || showTextIslands) {
        val bgColor = when {
            isFocused && showTextIslands -> focusBackgroundColor
            isFocused -> highlightColor
            else -> islandBackgroundColor
        }
        Modifier.background(bgColor, buttonShape).padding(4.dp)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditMode) Modifier.clickableNoRipple(onBottomWidgetClick) else Modifier),
        horizontalArrangement = when {
            isEditMode || (showLeft && showRight) -> Arrangement.SpaceBetween
            // Single icon + dots: spread them apart
            (showLeft || showRight) && state.shortcutPageDots && state.totalPages > 1 -> Arrangement.SpaceBetween
            showRight -> Arrangement.End
            else -> Arrangement.Start
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showLeft || isEditMode) {
            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = if (isEditMode && !showLeft) 0.5f else 1f)
                    .then(islandBgModifier)
                    .size(iconButtonSize)
                    .then(if (state.shortcutHideOutline) Modifier else if (isFocused || showTextIslands) Modifier.border(1.5.dp, iconColor, buttonShape) else Modifier.background(Theme.colors.background, buttonShape).border(1.5.dp, iconColor, buttonShape))
                    .clip(buttonShape)
                    .then(if (isEditMode) Modifier else Modifier.clickableNoRipple(onLeftClick)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showLeft) state.shortcutLeftIcon.toImageVector() else Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_left_shortcut),
                    tint = iconColor,
                    modifier = Modifier.size(if (state.shortcutHideOutline) iconButtonSize else iconButtonSize * 0.6f)
                )
            }
        }
        if (state.shortcutPageDots && state.totalPages > 1 && (showLeft || showRight)) {
            val sc = rememberScreenScale()
            val dotSize = state.pageIndicatorDotSize.dp.scaled(sc)
            val dotSpacing = (state.pageIndicatorDotSize * 0.8f).dp.scaled(sc)
            val dotBorderWidth = (state.pageIndicatorDotSize / 6f).dp.scaled(sc)
            val dotColor = Color(state.textColor)
            val dotShape = remember(state.textIslandsShape) {
                when (state.textIslandsShape) {
                    0 -> CircleShape
                    else -> ShapeHelper.getRoundedCornerShape(
                        textIslandsShape = state.textIslandsShape,
                        roundedRadius = 4.dp
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(dotSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(state.totalPages) { index ->
                    if (index == state.currentPage) {
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .background(color = dotColor, shape = dotShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .background(color = Theme.colors.background, shape = dotShape)
                                .border(width = dotBorderWidth, color = dotColor, shape = dotShape)
                        )
                    }
                }
            }
        }
        if (showRight || isEditMode) {
            Box(
                modifier = Modifier
                    .graphicsLayer(alpha = if (isEditMode && !showRight) 0.5f else 1f)
                    .then(islandBgModifier)
                    .size(iconButtonSize)
                    .then(if (state.shortcutHideOutline) Modifier else if (isFocused || showTextIslands) Modifier.border(1.5.dp, iconColor, buttonShape) else Modifier.background(Theme.colors.background, buttonShape).border(1.5.dp, iconColor, buttonShape))
                    .clip(buttonShape)
                    .then(if (isEditMode) Modifier else Modifier.clickableNoRipple(onRightClick)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showRight) state.shortcutRightIcon.toImageVector() else Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.cd_right_shortcut),
                    tint = iconColor,
                    modifier = Modifier.size(if (state.shortcutHideOutline) iconButtonSize else iconButtonSize * 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PageDotsBlock(
    state: HomeUiRenderState,
    onClick: () -> Unit,
    isEditMode: Boolean = false,
    showTextIslands: Boolean = false
) {
    val blockAlignment = when (state.quoteAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    val sc = rememberScreenScale()
    val dotSize = state.pageIndicatorDotSize.dp.scaled(sc)
    val dotSpacing = (state.pageIndicatorDotSize * 0.8f).dp.scaled(sc)
    val dotBorderWidth = (state.pageIndicatorDotSize / 6f).dp.scaled(sc)
    val dotColor = Color(state.textColor)
    val dotShape = remember(state.textIslandsShape) {
        when (state.textIslandsShape) {
            0 -> CircleShape
            else -> ShapeHelper.getRoundedCornerShape(
                textIslandsShape = state.textIslandsShape,
                roundedRadius = 4.dp
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditMode) Modifier.clickableNoRipple(onClick) else Modifier),
        horizontalAlignment = blockAlignment
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dotSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(state.totalPages) { index ->
                if (index == state.currentPage) {
                    Box(modifier = Modifier.size(dotSize).background(color = dotColor, shape = dotShape))
                } else {
                    Box(modifier = Modifier.size(dotSize)
                        .background(color = Theme.colors.background, shape = dotShape)
                        .border(width = dotBorderWidth, color = dotColor, shape = dotShape))
                }
            }
        }
    }
}
@Composable
private fun BottomWidgetEditModeTapTarget(state: HomeUiRenderState, onClick: () -> Unit) {
    val context = LocalContext.current
    val quoteTypeface = remember(state.quoteFont, state.quoteCustomFontPath) {
        state.quoteFont.getFont(context, state.quoteCustomFontPath)
    }
    val quoteFontFamily = quoteTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    val scaledQuoteSize = state.quoteSize * rememberScreenScale()
    val textAlign = when (state.quoteAlignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }
    val blockAlignment = when (state.quoteAlignment) {
        0 -> Alignment.Start
        2 -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    Column(
        modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = 0.5f),
        horizontalAlignment = blockAlignment
    ) {
        Text(
            text = stringResource(R.string.bottom_widget),
            style = TextStyle(
                color = Theme.colors.text,
                fontSize = scaledQuoteSize.sp,
                fontFamily = quoteFontFamily,
                textAlign = textAlign
            ),
            modifier = Modifier
                .wrapContentSize()
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
    val screenScale = rememberScreenScale()
    val scaledAppTextSize = state.appTextSize * screenScale
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
    
    val iconCodes = state.iconCodes
    
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val pagerContext = LocalContext.current
    val pagerPrefs = remember(pagerContext) { com.github.gezimos.inkos.data.Prefs(pagerContext) }
    val pagerAppsFont = remember(pagerPrefs.appsFont) { pagerPrefs.appsFont }
    val pagerAppsCustomFontPath = remember(pagerPrefs) {
        try { pagerPrefs.getCustomFontPathForContext("apps") } catch (_: Exception) { null }
    }
    val pagerAppsTypeface = remember(pagerAppsFont, pagerAppsCustomFontPath) {
        try { pagerAppsFont.getFont(pagerContext, pagerAppsCustomFontPath) } catch (_: Exception) { null }
    }
    val pagerAppsFontFamily = remember(pagerAppsTypeface) {
        if (pagerAppsTypeface != null) FontFamily(pagerAppsTypeface) else FontFamily.Default
    }
    val appNameHeight = remember(state.appTextSize, screenScale, pagerAppsFontFamily, textMeasurer) {
        try {
            val measuredHeight = textMeasurer.measure(
                text = AnnotatedString("Ag"),
                style = TextStyle(fontSize = scaledAppTextSize.sp, fontFamily = pagerAppsFontFamily)
            ).size.height
            with(density) { measuredHeight.toDp() }
        } catch (_: Exception) {
            state.appTextSize.dp
        }
    }
    
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
    
    val context = LocalContext.current
    val notificationTypeface = remember(state.notificationFont, state.notificationCustomFontPath) {
        state.notificationFont.getFont(context, state.notificationCustomFontPath)
    }
    val notificationFontFamily = remember(notificationTypeface) {
        notificationTypeface?.let { FontFamily(it) } ?: FontFamily.Default
    }
    
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
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(clip = false),
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
    textIslandsShape: Int = 0,
    showEditMode: Boolean = false
) {
    val density = LocalDensity.current
    
    val islandBackgroundColor = if (textIslandsInverted) Theme.colors.background else Theme.colors.text
    val islandTextColor = if (textIslandsInverted) Theme.colors.text else Theme.colors.background
    
    val tintColor = if (showTextIslands) islandTextColor else Theme.colors.text
    val highlightColor = if (showTextIslands) islandTextColor else Theme.colors.text
    val backgroundColor = if (showTextIslands) islandBackgroundColor else Color.Transparent
    
    val containerShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 50.dp
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = if (showEditMode && !state.showMediaWidget) 0.5f else 1f)
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
        val metadata = state.mediaInfo?.controller?.metadata
        val albumArtBitmap = remember(metadata) {
            try {
                metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                    ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            } catch (_: Exception) { null }
        }

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
private fun HomeAndroidWidget(
    widgetHeight: Int,
    marginStart: Int,
    marginEnd: Int,
    hasWidget: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onHeightChange: (Int) -> Unit,
    onMarginStartChange: (Int) -> Unit,
    onMarginEndChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var containerWidthPx by remember { mutableStateOf(0f) }
    val pxPerDp = with(density) { 1.dp.toPx() }
    val handleBarColor = Theme.colors.text

    val latestHeight by androidx.compose.runtime.rememberUpdatedState(widgetHeight)
    val latestMarginStart by androidx.compose.runtime.rememberUpdatedState(marginStart)
    val latestMarginEnd by androidx.compose.runtime.rememberUpdatedState(marginEnd)
    val latestOnHeightChange by androidx.compose.runtime.rememberUpdatedState(onHeightChange)
    val latestOnMarginStartChange by androidx.compose.runtime.rememberUpdatedState(onMarginStartChange)
    val latestOnMarginEndChange by androidx.compose.runtime.rememberUpdatedState(onMarginEndChange)

    val startFraction = marginStart / 100f
    val endFraction = marginEnd / 100f
    val widgetShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerWidthPx = it.size.width.toFloat() }
    ) {
        val totalWidth = maxWidth
        val startOffset = totalWidth * startFraction
        val widgetW = totalWidth * (1f - startFraction - endFraction)

        Box(
            modifier = Modifier
                .offset(x = startOffset)
                .width(widgetW)
                .height(widgetHeight.dp)
                .graphicsLayer(clip = false),
            contentAlignment = Alignment.Center
        ) {
            // Clipped content area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (!hasWidget || isEditMode) {
                            Modifier.border(2.dp, Theme.colors.text, widgetShape)
                        } else Modifier
                    )
                    .clip(widgetShape)
                    .then(
                        if (!hasWidget) Modifier.clickable { onClick() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasWidget) {
                    val appWidgetHelper = remember {
                        com.github.gezimos.inkos.helper.AppWidgetHelper.getInstance(context)
                    }
                    val widgetView by appWidgetHelper.widgetViewState.collectAsState()

                    widgetView?.let { view ->
                        key(view) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.FrameLayout(ctx).apply {
                                        (view.parent as? android.view.ViewGroup)?.removeView(view)
                                        addView(view, android.widget.FrameLayout.LayoutParams(
                                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                                        ).apply { gravity = android.view.Gravity.CENTER })
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } ?: run {
                        Text(stringResource(R.string.loading_widget), color = Theme.colors.text.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                } else {
                    Text("Tap to choose widget", color = Theme.colors.text.copy(alpha = 0.5f), fontSize = 14.sp)
                }

                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(6.dp)
                            .border(2.dp, Theme.colors.text, widgetShape)
                            .background(Theme.colors.background, widgetShape)
                            .padding(6.dp)
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.cd_edit_widget),
                            tint = Theme.colors.text,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            if (isEditMode) {
                // Bottom
                WidgetEdgeHandle(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(24.dp).offset(y = 12.dp),
                    barWidth = 40.dp, barHeight = 6.dp, barColor = handleBarColor,
                    onDrag = { _, dragY ->
                        val h = (latestHeight + (dragY / pxPerDp).toInt())
                            .coerceIn(Constants.MIN_ANDROID_WIDGET_HEIGHT, Constants.MAX_ANDROID_WIDGET_HEIGHT)
                        if (h != latestHeight) latestOnHeightChange(h)
                    }
                )
                // Top
                WidgetEdgeHandle(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().height(24.dp).offset(y = (-12).dp),
                    barWidth = 40.dp, barHeight = 6.dp, barColor = handleBarColor,
                    onDrag = { _, dragY ->
                        val h = (latestHeight + (-dragY / pxPerDp).toInt())
                            .coerceIn(Constants.MIN_ANDROID_WIDGET_HEIGHT, Constants.MAX_ANDROID_WIDGET_HEIGHT)
                        if (h != latestHeight) latestOnHeightChange(h)
                    }
                )
                WidgetEdgeHandle(
                    modifier = Modifier.align(Alignment.CenterEnd).width(24.dp).fillMaxHeight().offset(x = 12.dp),
                    barWidth = 6.dp, barHeight = 40.dp, barColor = handleBarColor,
                    onDrag = { dragX, _ ->
                        if (containerWidthPx > 0f) {
                            val deltaPct = (dragX / containerWidthPx * 100f).toInt()
                            val maxEnd = (100 - latestMarginStart - 20).coerceAtLeast(0)
                            val m = (latestMarginEnd - deltaPct).coerceIn(0, maxEnd)
                            if (m != latestMarginEnd) latestOnMarginEndChange(m)
                        }
                    }
                )
                WidgetEdgeHandle(
                    modifier = Modifier.align(Alignment.CenterStart).width(24.dp).fillMaxHeight().offset(x = (-12).dp),
                    barWidth = 6.dp, barHeight = 40.dp, barColor = handleBarColor,
                    onDrag = { dragX, _ ->
                        if (containerWidthPx > 0f) {
                            val deltaPct = (dragX / containerWidthPx * 100f).toInt()
                            val maxStart = (100 - latestMarginEnd - 20).coerceAtLeast(0)
                            val m = (latestMarginStart + deltaPct).coerceIn(0, maxStart)
                            if (m != latestMarginStart) latestOnMarginStartChange(m)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun WidgetEdgeHandle(
    modifier: Modifier,
    barWidth: Dp,
    barHeight: Dp,
    barColor: Color,
    onDrag: (dragX: Float, dragY: Float) -> Unit
) {
    val latestOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    latestOnDrag(dragAmount.x, dragAmount.y)
                }
            }
            .drawBehind {
                val bw = barWidth.toPx()
                val bh = barHeight.toPx()
                val r = kotlin.math.min(bw, bh) / 2
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset((size.width - bw) / 2, (size.height - bh) / 2),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(r, r)
                )
            }
    )
}

@Composable
private fun MediaButton(
    imageVector: ImageVector? = null,
    iconRes: Int? = null,
    tint: Color,
    contentDescription: String,
    size: Dp = 32.dp,
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
private fun calculateIconCodeFontSize(iconCode: String, appTextSize: Float): Float {
    val baseSize = appTextSize * 0.7f
    return if (iconCode.length > 1) {
        baseSize * 0.85f
    } else {
        baseSize
    }
}

@Composable
private fun HomeAppIconContent(
    context: android.content.Context,
    app: HomeAppUiState,
    iconSourceMode: Int,
    selectedIconPackPackage: String,
    letterCode: String?,
    appNameHeight: Dp,
    appTextSize: Float,
    labelColor: Color,
    labelFontFamily: FontFamily,
    iconShape: androidx.compose.ui.graphics.Shape? = null,
    iconShapeId: Int = -1,
    /** When set and the mode is tinted (4 or 5), use this for the icon tint so the bitmap does not change on focus. */
    systemTintedIconTintOverride: Color? = null,
    /** When non-null and the mode is inkOS (4), recolor the icon at compose time (used only on the focused item). */
    iconInvertColor: Color? = null,
    iconTintContrast: Int = 10,
    iconBgColor: Color = Color.Transparent
) {
    val density = LocalDensity.current
    val sizePx = with(density) { appNameHeight.toPx().toInt() }.coerceAtLeast(1)

    val isPlaceholder = app.activityPackage.isBlank() && app.shortcutId == null

    val isSyntheticForIcon = remember(app.activityPackage, app.shortcutId) {
        IconUtility.isSyntheticPackage(app.activityPackage) &&
            (app.shortcutId == null || IconUtility.isInkOSInternalShortcut(app.shortcutId))
    }
    val isShortcutOverride = app.shortcutId != null && !isSyntheticForIcon

    if (iconSourceMode == 0 && !isPlaceholder) {
        // Mode 0: Letter codes for all apps and shortcuts
        if (!letterCode.isNullOrEmpty()) {
            val dynamicFontSize = remember(letterCode, appTextSize) {
                calculateIconCodeFontSize(letterCode, appTextSize)
            }
            Text(
                text = letterCode,
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
    } else {
        val effectivePackage = if (isPlaceholder) "app.inkos" else app.activityPackage
        val tintArgb = remember(systemTintedIconTintOverride, labelColor, iconSourceMode) {
            if (IconShapeUtility.isTintedMode(iconSourceMode)) (systemTintedIconTintOverride ?: labelColor).toArgb() else 0
        }
        val bgArgb = remember(iconBgColor, iconSourceMode) {
            if (iconSourceMode == 6) iconBgColor.toArgb() else 0
        }
        val imageBitmap = rememberAppIconBitmap(
            context, effectivePackage, iconSourceMode, selectedIconPackPackage, sizePx,
            app.activityClass, app.user, app.shortcutId, tintArgb, iconShapeId,
            iconTintContrast = iconTintContrast,
            bgArgb = bgArgb
        )
        val invertFilter = remember(iconInvertColor, iconSourceMode) {
            if (iconInvertColor != null && IconShapeUtility.isInkOsMode(iconSourceMode)) cachedTintFilter(iconInvertColor) else null
        }
        val useIconBg = (iconSourceMode == 2 || IconShapeUtility.isInkOsMode(iconSourceMode)) && invertFilter == null
        val clipMod = remember(iconShape) {
            if (iconShape != null) Modifier.clip(iconShape) else Modifier
        }

        if (imageBitmap != null) {
            Box(modifier = Modifier.fillMaxSize().then(clipMod)) {
                if (useIconBg) {
                    Box(modifier = Modifier.matchParentSize().background(Theme.colors.background))
                }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = app.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    colorFilter = invertFilter
                )
            }
        }
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
    appNameHeight: Dp,
    badgeConfig: NotificationBadgeConfig,
    notificationFontFamily: FontFamily,
    highlightColor: Color,
    islandBackgroundColor: Color,
    islandTextColor: Color,
    focusBackgroundColor: Color,
    focusTextColor: Color
) {
    val screenScale = rememberScreenScale()
    val scaledAppTextSize = state.appTextSize * screenScale
    val scaledIconTextSize = scaledAppTextSize
    val scaledNotifTextSize = state.notificationTextSize * screenScale
    val highlightPaddingPx = with(LocalDensity.current) { 8.dp.toPx() }
    val isDpadActivated = dpadActivatedAppId == app.id
    val pulseAlpha = remember { androidx.compose.animation.core.Animatable(0f) }
    val placeholderLabel = stringResource(id = R.string.select_app)
    val isOtherProfile = app.user != null && app.user != android.os.Process.myUserHandle()
    val rawLabel = (app.label.ifBlank { placeholderLabel }).let { if (isOtherProfile) "$it^" else it }
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
    val context = LocalContext.current
    val isSep = Constants.isSeparator(app.activityPackage)
    val hasIcon = !isSep && state.showIcons && state.homeAlignment != 1 &&
        (state.iconSourceMode != 0 || (iconCodes[app.label]?.isNotEmpty() == true))
    val resolvedIconShape = remember(state.iconShape) {
        IconShape.fromPreference(state.iconShape)
    }
    val iconClipShape = remember(resolvedIconShape, state.iconSourceMode, appNameHeight) {
        if (IconShapeUtility.shouldClipBitmap(state.iconSourceMode))
            IconShapeUtility.getComposeShape(resolvedIconShape, pillRadius = (appNameHeight * 1.25f) / 2)
        else null
    }
    val modeIsFullBleed = remember(state.iconSourceMode) { IconShapeUtility.isFullBleedMode(state.iconSourceMode) }
    val modeIsInkOs = remember(state.iconSourceMode) { IconShapeUtility.isInkOsMode(state.iconSourceMode) }
    val modeIsTinted = remember(state.iconSourceMode) { IconShapeUtility.isTintedMode(state.iconSourceMode) }
    val pulseColor = Theme.colors.text

    val isFocused = showDpadMode && isSelected
    val labelColor = when {
        isSep && app.activityPackage == Constants.SEPARATOR_EMPTY -> Color.Transparent
        isFocused && showTextIslands -> focusTextColor
        isFocused -> Theme.colors.background
        showTextIslands -> islandTextColor
        else -> Theme.colors.text
    }
    val systemTintedIconTintStable = when {
        showTextIslands && !state.textIslandsInverted && modeIsTinted -> Theme.colors.text
        showTextIslands -> islandTextColor
        else -> Theme.colors.text
    }
    val systemTintedIconBoxBg = when {
        showTextIslands && !state.textIslandsInverted && modeIsInkOs -> Theme.colors.background
        else -> null
    }
    val indicator = remember(state.notificationIndicatorStyle) {
        Constants.NotificationIndicator.fromOrdinal(state.notificationIndicatorStyle)
    }
    val isRightAligned = gravity == android.view.Gravity.END
    val labelAnnotated = remember(resolvedLabel, badgeDisplay.showIndicator, state.appTextSize, indicator, isRightAligned) {
        buildAnnotatedString {
            if (badgeDisplay.showIndicator && indicator.isSuperscript && isRightAligned) {
                withStyle(
                    SpanStyle(
                        baselineShift = BaselineShift(0.15f),
                        fontSize = (scaledAppTextSize * 0.75f).sp
                    )
                ) {
                    append(indicator.symbol)
                }
                append("\u2009")
            }
            append(resolvedLabel)
            if (badgeDisplay.showIndicator && indicator.isSuperscript && !isRightAligned) {
                append("\u2009")
                withStyle(
                    SpanStyle(
                        baselineShift = BaselineShift(0.15f),
                        fontSize = (scaledAppTextSize * 0.75f).sp
                    )
                ) {
                    append(indicator.symbol)
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
    
    val boxContentAlignment = when (gravity) {
        android.view.Gravity.END -> Alignment.CenterEnd
        android.view.Gravity.CENTER -> Alignment.Center
        else -> Alignment.CenterStart
    }
    
    
    Box(
        modifier = Modifier
            .then(if (state.extendHomeAppsArea) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
            .graphicsLayer(clip = false)
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

        // Otherwise, show normal layout (icon + app name)
        if (subtitle != null) {
            val isRightAligned = gravity == android.view.Gravity.END

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (hasIcon) Modifier.height(appNameHeight) else Modifier)
                    .graphicsLayer(clip = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (gravity) {
                    android.view.Gravity.END -> Arrangement.End
                    android.view.Gravity.CENTER -> Arrangement.Center
                    else -> Arrangement.Start
                }
            ) {
                if (!isRightAligned && hasIcon) {
                    AppIconBox(
                        iconSourceMode = state.iconSourceMode,
                        iconShape = resolvedIconShape,
                        size = appNameHeight * 1.25f,
                        showBackground = ((showDpadMode && isSelected) || showTextIslands) && !modeIsFullBleed,
                        backgroundColor = when {
                            isFocused && showTextIslands -> focusBackgroundColor
                            isFocused -> highlightColor
                            systemTintedIconBoxBg != null -> systemTintedIconBoxBg
                            else -> islandBackgroundColor
                        },
                        showBorder = !showTextIslands || modeIsInkOs,
                        borderColor = if (showTextIslands) islandBackgroundColor else Theme.colors.text,
                        paddingEnd = 10.dp
                    ) {
                        HomeAppIconContent(
                            context = context,
                            app = app,
                            iconSourceMode = state.iconSourceMode,
                            selectedIconPackPackage = state.selectedIconPackPackage,
                            letterCode = iconCodes[app.label],
                            appNameHeight = appNameHeight * 1.25f,
                            appTextSize = scaledIconTextSize,
                            labelColor = labelColor,
                            labelFontFamily = labelFontFamily,
                            iconShape = iconClipShape,
                            iconShapeId = state.iconShape,
                            systemTintedIconTintOverride = if (modeIsTinted) systemTintedIconTintStable else null,
                            iconInvertColor = if (isFocused) labelColor else null,
                            iconTintContrast = state.iconTintContrast,
                            iconBgColor = Theme.colors.background
                        )
                    }
                }

                val notificationRipplePad = if (showTextIslands || (showDpadMode && isSelected)) with(density) { highlightPaddingPx.toDp() } else 0.dp

                Layout(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(
                            start = if (!isRightAligned && hasIcon) 10.dp else 0.dp,
                            end = if (isRightAligned && hasIcon) 10.dp else 0.dp
                        )
                        .height(appNameHeight)
                        .graphicsLayer(clip = false),
                    content = {
                        // App name
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (badgeDisplay.showIndicator && !indicator.isSuperscript && isRightAligned) {
                                    val dotSize = (scaledAppTextSize * 0.3f).dp
                                    IndicatorDot(indicator, dotSize, labelColor)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = labelAnnotated,
                                    modifier = Modifier.wrapContentWidth(),
                                    style = TextStyle(
                                        color = labelColor,
                                        fontSize = (scaledAppTextSize * 0.85f).sp,
                                        fontFamily = labelFontFamily,
                                        textAlign = textAlign
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (badgeDisplay.showIndicator && !indicator.isSuperscript && !isRightAligned) {
                                    val dotSize = (scaledAppTextSize * 0.3f).dp
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IndicatorDot(indicator, dotSize, labelColor)
                                }
                            }
                        }

                        // Notification message
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
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
                                    fontSize = scaledNotifTextSize.sp,
                                    fontFamily = notificationFontFamily,
                                    textAlign = textAlign
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val looseConstraints = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                    val labelPlaceable = measurables.getOrNull(0)?.measure(looseConstraints)
                    val notificationPlaceable = measurables.getOrNull(1)?.measure(looseConstraints)

                    val layoutHeight = constraints.minHeight
                    val contentWidth = listOfNotNull(labelPlaceable?.width, notificationPlaceable?.width).maxOrNull() ?: 0
                    val layoutWidth = constraints.constrainWidth(contentWidth)

                    fun horizontalX(width: Int): Int = when (horizontalAlignment) {
                        Alignment.End -> layoutWidth - width
                        Alignment.CenterHorizontally -> (layoutWidth - width) / 2
                        else -> 0
                    }

                    val totalContentHeight =
                        (labelPlaceable?.height ?: 0) + (notificationPlaceable?.height ?: 0)
                    val topOffset = (layoutHeight - totalContentHeight) / 2
                    val labelY = topOffset
                    val notificationY = labelY + (labelPlaceable?.height ?: 0)

                    layout(layoutWidth, layoutHeight) {
                        labelPlaceable?.placeRelative(horizontalX(labelPlaceable.width), labelY)
                        notificationPlaceable?.placeRelative(horizontalX(notificationPlaceable.width), notificationY)
                    }
                }

                // Icon on right (when right-aligned)
                if (isRightAligned && hasIcon) {
                    AppIconBox(
                        iconSourceMode = state.iconSourceMode,
                        iconShape = resolvedIconShape,
                        size = appNameHeight * 1.25f,
                        showBackground = ((showDpadMode && isSelected) || showTextIslands) && !modeIsFullBleed,
                        backgroundColor = when {
                            isFocused && showTextIslands -> focusBackgroundColor
                            isFocused -> highlightColor
                            else -> islandBackgroundColor
                        },
                        showBorder = !showTextIslands || modeIsInkOs,
                        borderColor = if (showTextIslands) islandBackgroundColor else Theme.colors.text,
                        paddingStart = 10.dp
                    ) {
                        HomeAppIconContent(
                            context = context,
                            app = app,
                            iconSourceMode = state.iconSourceMode,
                            selectedIconPackPackage = state.selectedIconPackPackage,
                            letterCode = iconCodes[app.label],
                            appNameHeight = appNameHeight * 1.25f,
                            appTextSize = scaledIconTextSize,
                            labelColor = labelColor,
                            labelFontFamily = labelFontFamily,
                            iconShape = iconClipShape,
                            iconShapeId = state.iconShape,
                            systemTintedIconTintOverride = if (modeIsTinted) systemTintedIconTintStable else null,
                            iconInvertColor = if (isFocused) labelColor else null,
                            iconTintContrast = state.iconTintContrast,
                            iconBgColor = Theme.colors.background
                        )
                    }
                }
            }
        } else {
            // Normal layout: icon + app name (no notification)
            val isRightAligned = gravity == android.view.Gravity.END

            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .then(if (hasIcon) Modifier.height(appNameHeight) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = when (gravity) {
                    android.view.Gravity.END -> Arrangement.End
                    android.view.Gravity.CENTER -> Arrangement.Center
                    else -> Arrangement.Start
                }
            ) {
                // Icon on left (when not right-aligned)
                if (!isRightAligned && hasIcon) {
                    AppIconBox(
                        iconSourceMode = state.iconSourceMode,
                        iconShape = resolvedIconShape,
                        size = appNameHeight * 1.25f,
                        showBackground = ((showDpadMode && isSelected) || showTextIslands) && !modeIsFullBleed,
                        backgroundColor = when {
                            isFocused && showTextIslands -> focusBackgroundColor
                            isFocused -> highlightColor
                            systemTintedIconBoxBg != null -> systemTintedIconBoxBg
                            else -> islandBackgroundColor
                        },
                        showBorder = !showTextIslands || modeIsInkOs,
                        borderColor = if (showTextIslands) islandBackgroundColor else Theme.colors.text,
                        paddingEnd = 10.dp
                    ) {
                        HomeAppIconContent(
                            context = context,
                            app = app,
                            iconSourceMode = state.iconSourceMode,
                            selectedIconPackPackage = state.selectedIconPackPackage,
                            letterCode = iconCodes[app.label],
                            appNameHeight = appNameHeight * 1.25f,
                            appTextSize = scaledIconTextSize,
                            labelColor = labelColor,
                            labelFontFamily = labelFontFamily,
                            iconShape = iconClipShape,
                            iconShapeId = state.iconShape,
                            systemTintedIconTintOverride = if (modeIsTinted) systemTintedIconTintStable else null,
                            iconInvertColor = if (isFocused) labelColor else null,
                            iconTintContrast = state.iconTintContrast,
                            iconBgColor = Theme.colors.background
                        )
                    }
                }

                // App label container (with its own ripple)
                val ripplePad = if (showTextIslands || (showDpadMode && isSelected)) with(density) { highlightPaddingPx.toDp() } else 0.dp
                Box(
                    modifier = Modifier
                        .padding(
                            start = if (!isRightAligned && hasIcon) 10.dp else 0.dp,
                            end = if (isRightAligned && hasIcon) 10.dp else 0.dp
                        )
                        .padding(horizontal = ripplePad)
                        .graphicsLayer(clip = false)
                        .drawBehind {
                            if (isFocused || showTextIslands) {
                                val pad = highlightPaddingPx
                                val corner = ShapeHelper.getCornerRadius(
                                    textIslandsShape = state.textIslandsShape,
                                    height = size.height,
                                    density = density
                                )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (badgeDisplay.showIndicator && !indicator.isSuperscript && isRightAligned) {
                            val dotSize = (scaledAppTextSize * 0.35f).dp
                            IndicatorDot(indicator, dotSize, labelColor)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = labelAnnotated,
                            modifier = Modifier.wrapContentWidth(),
                            style = TextStyle(
                                color = labelColor,
                                fontSize = scaledAppTextSize.sp,
                                fontFamily = labelFontFamily,
                                textAlign = textAlign
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (badgeDisplay.showIndicator && !indicator.isSuperscript && !isRightAligned) {
                            val dotSize = (scaledAppTextSize * 0.35f).dp
                            Spacer(modifier = Modifier.width(4.dp))
                            IndicatorDot(indicator, dotSize, labelColor)
                        }
                    }
                }
                
                // Icon on right (when right-aligned)
                if (isRightAligned && hasIcon) {
                    AppIconBox(
                        iconSourceMode = state.iconSourceMode,
                        iconShape = resolvedIconShape,
                        size = appNameHeight * 1.25f,
                        showBackground = ((showDpadMode && isSelected) || showTextIslands) && !modeIsFullBleed,
                        backgroundColor = when {
                            isFocused && showTextIslands -> focusBackgroundColor
                            isFocused -> highlightColor
                            else -> islandBackgroundColor
                        },
                        showBorder = !showTextIslands || modeIsInkOs,
                        borderColor = if (showTextIslands) islandBackgroundColor else Theme.colors.text,
                        paddingStart = 10.dp
                    ) {
                        HomeAppIconContent(
                            context = context,
                            app = app,
                            iconSourceMode = state.iconSourceMode,
                            selectedIconPackPackage = state.selectedIconPackPackage,
                            letterCode = iconCodes[app.label],
                            appNameHeight = appNameHeight * 1.25f,
                            appTextSize = scaledIconTextSize,
                            labelColor = labelColor,
                            labelFontFamily = labelFontFamily,
                            iconShape = iconClipShape,
                            iconShapeId = state.iconShape,
                            systemTintedIconTintOverride = if (modeIsTinted) systemTintedIconTintStable else null,
                            iconInvertColor = if (isFocused) labelColor else null,
                            iconTintContrast = state.iconTintContrast,
                            iconBgColor = Theme.colors.background
                        )
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

@Composable
private fun IndicatorDot(
    indicator: Constants.NotificationIndicator,
    size: Dp,
    color: Color
) {
    Canvas(modifier = Modifier.size(size)) {
        when (indicator) {
            Constants.NotificationIndicator.FilledCircle ->
                drawCircle(color = color)
            Constants.NotificationIndicator.OutlineCircle ->
                drawCircle(color = color, style = Stroke(width = this.size.width * 0.2f))
            Constants.NotificationIndicator.FilledDiamond -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(this@Canvas.size.width / 2, 0f)
                    lineTo(this@Canvas.size.width, this@Canvas.size.height / 2)
                    lineTo(this@Canvas.size.width / 2, this@Canvas.size.height)
                    lineTo(0f, this@Canvas.size.height / 2)
                    close()
                }
                drawPath(path, color = color)
            }
            Constants.NotificationIndicator.FilledSquare ->
                drawRect(color = color)
            else -> {}
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = null, indication = null) {
        GestureHelper.notifyElementClicked()
        onClick()
    }
private fun Modifier.clickableNoRippleGestureAware(onClick: () -> Unit): Modifier {
    return this.then(
        Modifier.pointerInput(Unit) {
            val clickThreshold = 10.dp.toPx() // Maximum movement to consider it a click
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
                            if (movement > clickThreshold) {
                                isClick = false
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })
                
                if (isClick && maxMovement <= clickThreshold) {
                    onClick()
                }
            }
        }
    )
}
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
    val clockStyle: Int,
    val quoteSize: Int,
    val dateFont: Constants.FontFamily,
    val dateSize: Int,
    val homeAlignment: Int,
    val clockAlignment: Int = 0,
    val dateAlignment: Int = 0,
    val quoteAlignment: Int = 0,
    val homeAppsYOffset: Int,
    val maxHomeAppsYOffset: Int = Constants.MAX_HOME_APPS_Y_OFFSET,

    val bottomWidgetHeightPx: Int = 0,
    val bottomWidgetMargin: Int,
    val topWidgetMargin: Int,
    val pageIndicatorVisible: Boolean,
    val pageIndicatorColor: Int,
    val pageIndicatorDotSize: Float = 12f,
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
    val notificationIndicatorStyle: Int = 0,
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
    val iconSourceMode: Int = 0,
    val iconShape: Int = 0,
    val iconTintContrast: Int = 10,
    val selectedIconPackPackage: String = "",
    val iconCodes: Map<String, String> = emptyMap(),
    val clockCustomFontPath: String?,
    val dateCustomFontPath: String?,
    val quoteCustomFontPath: String?,
    val notificationCustomFontPath: String?,
    val extendHomeAppsArea: Boolean,
    val bottomWidgetType: String = "quote",
    // Android AppWidget hosting
    val showAndroidWidget: Boolean = false,
    val androidWidgetId: Int = -1,
    val androidWidgetHeight: Int = 120,
    val androidWidgetMarginStart: Int = 0,
    val androidWidgetMarginEnd: Int = 0,
    // Events widget
    val eventsList: List<com.github.gezimos.inkos.helper.CalendarEventsHelper.CalendarEvent> = emptyList(),
    val eventsIndex: Int = 0,
    val hasCalendarPermission: Boolean = false,
    val eventsCalendarName: String = "",
    val eventsHideControls: Boolean = false,
    val eventsCalendarId: Long = -1L,
    val eventsFilter: Int = 1,
    // Shortcuts widget
    val shortcutLeftIcon: Constants.ShortcutIcon = Constants.ShortcutIcon.Search,
    val shortcutLeftAction: Constants.Action = Constants.Action.Search,
    val shortcutRightIcon: Constants.ShortcutIcon = Constants.ShortcutIcon.Phone,
    val shortcutRightAction: Constants.Action = Constants.Action.OpenApp,
    val shortcutPageDots: Boolean = false,
    val shortcutHideOutline: Boolean = false,
    // Total Usage widget
    val totalUsageText: String = "0min"
)
data class HomeUiCallbacks(
    val onAppClick: (HomeAppUiState) -> Unit,
    val onAppLongClick: (HomeAppUiState) -> Unit,
    val onClockClick: () -> Unit,
    val onDateClick: () -> Unit,
    val onBatteryClick: () -> Unit,
    val onNotificationCountClick: () -> Unit,
    val onBottomWidgetClick: () -> Unit,
    val onEventsGrantPermissionClick: () -> Unit = {},
    val onEventsCalendarClick: () -> Unit = {},
    val onEventsEventClick: (com.github.gezimos.inkos.helper.CalendarEventsHelper.CalendarEvent) -> Unit = {},
    val onEventsPrevClick: () -> Unit = {},
    val onEventsNextClick: () -> Unit = {},
    val onEventsFirstClick: () -> Unit = {},
    val onEventsLastClick: () -> Unit = {},
    val onShortcutLeftClick: () -> Unit = {},
    val onShortcutRightClick: () -> Unit = {},
    val onMediaAction: (HomeMediaAction) -> Unit,
    val onSwipeLeft: () -> Unit,
    val onSwipeRight: () -> Unit,
    val onPageDelta: (Int) -> Unit,
    val onRootLongPress: () -> Unit,
    val onBackgroundClick: () -> Unit = {},
    val onAndroidWidgetHeightChange: (Int) -> Unit = {},
    val onAndroidWidgetMarginStartChange: (Int) -> Unit = {},
    val onAndroidWidgetMarginEndChange: (Int) -> Unit = {},
    val onTopMarginDrag: (Int) -> Unit = {},
    val onAppsYOffsetDrag: (Int) -> Unit = {},
    val onBottomMarginDrag: (Int) -> Unit = {}
)

enum class HomeMediaAction { Open, Previous, PlayPause, Next, Stop }
@Composable
internal fun HomeScreenPreview(preset: ThemePreset, isDark: Boolean) {
    val bgColorInt = if (isDark) preset.darkBackgroundColor else preset.lightBackgroundColor
    val textColorInt = if (isDark) preset.darkTextColor else preset.lightTextColor
    val bgColor = Color(bgColorInt)
    val textColor = Color(textColorInt)
    val context = LocalContext.current
    remember { Prefs(context) }
    val presetTypeface = remember(preset.font) { preset.font.getFont(context) }
    val fakeState = remember(preset, isDark, presetTypeface) {
        val pm = context.packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pagesNum = preset.homePagesNum
        val appsPerPage = if (preset.homeAppsNum <= 0 || pagesNum <= 0) {
            0
        } else {
            kotlin.math.ceil(preset.homeAppsNum.toDouble() / pagesNum).toInt().coerceAtLeast(1)
        }
        val previewLabels = listOf("Phone", "Messages", "Camera", "Gallery", "Contacts", "Spotify", "YouTube", "Podcasts", "Calendar", "Clock", "Maps", "Notes", "Music", "Weather", "Settings")
        val resolved = pm.queryIntentActivities(launchIntent, PackageManager.GET_META_DATA)
            .filter { it.activityInfo.packageName != context.packageName }
            .take(appsPerPage)
        val fakeApps = resolved.mapIndexed { i, ri ->
            HomeAppUiState(
                id = i,
                label = previewLabels.getOrElse(i) { "App" },
                font = presetTypeface,
                color = textColorInt,
                activityPackage = ri.activityInfo.packageName,
            )
        }.ifEmpty {
            previewLabels.take(appsPerPage).mapIndexed { i, label ->
                HomeAppUiState(id = i, label = label, font = presetTypeface, color = textColorInt, activityPackage = "com.android.phone")
            }
        }.take(appsPerPage)
        val isEvents = preset.bottomWidgetType == "events"
        val fakeEventList = if (isEvents) {
            listOf(
                com.github.gezimos.inkos.helper.CalendarEventsHelper.CalendarEvent(
                    eventId = 1L, title = "Team Standup", location = null,
                    beginTime = 1704362400000L, endTime = 1704366000000L,
                ),
                com.github.gezimos.inkos.helper.CalendarEventsHelper.CalendarEvent(
                    eventId = 2L, title = "Lunch", location = null,
                    beginTime = 1704376800000L, endTime = 1704380400000L,
                ),
            )
        } else emptyList()
        HomeUiRenderState(
            homeApps = fakeApps,
            clockText = "10:24",
            dateText = "Mon, Mar 3",
            amPmText = "AM",
            is24Hour = true,
            isCharging = false,
            showClock = preset.showClock,
            showDate = preset.showDate,
            showBattery = preset.showDateBatteryCombo,
            batteryText = "75%",
            showNotificationCount = preset.showNotificationCount,
            notificationCount = if (preset.showNotificationCount) 3 else 0,
            showQuote = !isEvents,
            quoteText = preset.quoteText ?: "stay inspired",
            backgroundColor = bgColorInt,
            backgroundOpacity = preset.backgroundOpacity ?: 255,
            textColor = textColorInt,
            appsPerPage = appsPerPage,
            totalPages = pagesNum,
            currentPage = 0,
            notifications = emptyMap(),
            mediaInfo = null,
            mediaWidgetColor = textColorInt,
            showMediaWidget = false,
            showMediaIndicator = false,
            showMediaName = false,
            clockFont = preset.font,
            quoteFont = preset.font,
            clockSize = preset.clockSize,
            clockMode = 0,
            clockStyle = preset.clockStyle,
            quoteSize = preset.quoteSize ?: (context.resources.getDimensionPixelSize(R.dimen.default_quote_size) / context.resources.displayMetrics.scaledDensity).toInt(),
            dateFont = preset.font,
            dateSize = (context.resources.getDimensionPixelSize(R.dimen.default_date_size) / context.resources.displayMetrics.scaledDensity).toInt(),
            homeAlignment = preset.homeAlignment,
            clockAlignment = preset.clockAlignment,
            dateAlignment = preset.dateAlignment ?: preset.clockAlignment,
            quoteAlignment = preset.homeAlignment,
            homeAppsYOffset = 0,
            bottomWidgetMargin = 32,
            topWidgetMargin = 32,
            pageIndicatorVisible = true,
            pageIndicatorColor = textColorInt,
            pageIndicatorDotSize = 12f,
            appPadding = preset.appGap,
            appTextSize = preset.appSize.toFloat(),
            hideHomeApps = false,
            appDrawerGap = 8,
            showAmPm = false,
            showSecondClock = preset.showSecondClock,
            secondClockText = if (preset.showSecondClock) "02:24" else "",
            secondAmPmText = if (preset.showSecondClock) "AM" else "",
            secondClockOffsetHours = preset.secondClockOffsetHours,
            allCapsApps = preset.allCapsApps,
            smallCapsApps = preset.smallCapsApps,
            showNotificationBadge = false,
            showNotificationText = false,
            showNotificationSenderName = false,
            showNotificationGroupName = false,
            showNotificationMessage = false,
            homeAppCharLimit = 20,
            notificationTextSize = 12,
            notificationFont = preset.font,
            textIslands = preset.textIslands,
            textIslandsInverted = false,
            textIslandsShape = preset.textIslandsShape,
            showIcons = preset.showIcons,
            iconSourceMode = preset.iconMode,
            iconShape = preset.iconShape,
            selectedIconPackPackage = "",
            iconCodes = if (preset.iconMode == 0 && preset.showIcons)
                IconUtility.generateIconCodes(fakeApps.map { it.label })
            else emptyMap(),
            clockCustomFontPath = null,
            dateCustomFontPath = null,
            quoteCustomFontPath = null,
            notificationCustomFontPath = null,
            extendHomeAppsArea = false,
            bottomWidgetType = preset.bottomWidgetType,
            shortcutHideOutline = preset.shortcutHideOutline,
            eventsList = fakeEventList,
            eventsIndex = 0,
            hasCalendarPermission = isEvents,
            eventsCalendarName = if (isEvents) "My Calendar" else "",
            eventsCalendarId = if (isEvents) -2L else -1L,
            eventsFilter = 1,
        )
    }
    val fakeCallbacks = remember {
        HomeUiCallbacks(
            onAppClick = {},
            onAppLongClick = {},
            onClockClick = {},
            onDateClick = {},
            onBatteryClick = {},
            onNotificationCountClick = {},
            onBottomWidgetClick = {},
            onMediaAction = {},
            onSwipeLeft = {},
            onSwipeRight = {},
            onPageDelta = {},
            onRootLongPress = {},
        )
    }
    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Wallpaper background if preset has one
        if (preset.wallpaperResourceId != null) {
            val fgR = textColor.red; val fgG = textColor.green; val fgB = textColor.blue
            val bgR = bgColor.red; val bgG = bgColor.green; val bgB = bgColor.blue
            val colorMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                (bgR - fgR) * 0.299f, (bgR - fgR) * 0.587f, (bgR - fgR) * 0.114f, 0f, fgR * 255f,
                (bgG - fgG) * 0.299f, (bgG - fgG) * 0.587f, (bgG - fgG) * 0.114f, 0f, fgG * 255f,
                (bgB - fgB) * 0.299f, (bgB - fgB) * 0.587f, (bgB - fgB) * 0.114f, 0f, fgB * 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            if (preset.wallpaperResourceId > 0) {
                // Drawable resource wallpaper
                Image(
                    painter = painterResource(preset.wallpaperResourceId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                )
            } else {
                val generatedBitmap = remember(preset.wallpaperResourceId, bgColorInt, textColorInt, presetTypeface) {
                    try {
                        val wu = com.github.gezimos.inkos.helper.WallpaperUtility(context)
                        wu.loadGeneratedWallpaper(preset.wallpaperResourceId, bgColorInt, textColorInt, presetTypeface)
                    } catch (_: Exception) { null }
                }
                if (generatedBitmap != null) {
                    Image(
                        bitmap = generatedBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        val widthScale = maxWidth.value / 360f
        val heightScale = maxHeight.value / 640f
        val scale = minOf(widthScale, heightScale).coerceAtMost(1f)
        val origDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = origDensity.density * scale,
                fontScale = origDensity.fontScale,
            ),
            LocalAppColors provides AppColors(text = textColor, background = bgColor),
        ) {
            HomeUI(state = fakeState, callbacks = fakeCallbacks, isPreview = true)
        }
    }
}
@Composable
fun QuickMenuSheet(
    onInkOSSettings: () -> Unit,
    onEditMode: () -> Unit,
    onEditFavorites: () -> Unit,
    onLookFeel: () -> Unit,
    onAbout: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    remember { Prefs(context) }

    SheetTitle(stringResource(R.string.quick_menu))

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // inkOS Settings
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.inkos_settings),
            iconRes = R.drawable.ic_foreground,
            iconPadding = 2.dp,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = {
                onInkOSSettings()
                onDismiss()
            }
        )

        // Edit Mode
        SettingsComposable.SettingsHomeItem(
            title = if (EditModeHelper.isEditMode()) stringResource(R.string.exit_edit_mode) else stringResource(R.string.edit_mode),
            imageVector = Icons.Rounded.FormatSize,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = {
                onEditMode()
                onDismiss()
            }
        )

        // Edit Favorites
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.edit_favorites),
            imageVector = Icons.Rounded.Star,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = {
                onEditFavorites()
                onDismiss()
            }
        )

        // Look & Feel
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.look_feel_settings_title),
            imageVector = Icons.Rounded.Palette,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = {
                onLookFeel()
                onDismiss()
            }
        )

        // About
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.about),
            imageVector = Icons.Rounded.Info,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = {
                onAbout()
                onDismiss()
            }
        )
    }
}
