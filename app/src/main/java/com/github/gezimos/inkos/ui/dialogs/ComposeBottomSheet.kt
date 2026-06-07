package com.github.gezimos.inkos.ui.dialogs

import android.app.Activity
import com.github.gezimos.inkos.ui.compose.toImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.rounded.FileOpen
import com.github.gezimos.inkos.helper.IconUtility
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.rounded.Restore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.gezimos.inkos.R

import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.helper.hideNavigationBar
import com.github.gezimos.inkos.helper.hideStatusBar
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.compose.SettingsComposable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
class ComposeBottomSheetHost(private val activity: Activity) {
    private var composeView: ComposeView? = null
    private var insetsListener: View.OnApplyWindowInsetsListener? = null

    /** Fired once after a visible sheet is dismissed (Back, swipe, or explicit). */
    var onDismissed: (() -> Unit)? = null

    companion object {
        private var isAnySheetShowing = false
    }

    init {
        (activity as? LifecycleOwner)?.lifecycle?.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                isAnySheetShowing = false
                composeView = null
            }
        })
    }

    fun dismiss() {
        val wasShowing = composeView != null
        isAnySheetShowing = false
        if (insetsListener != null) {
            try { activity.window.decorView.setOnApplyWindowInsetsListener(null) } catch (_: Exception) {}
            insetsListener = null
        }
        composeView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        composeView = null
        if (wasShowing) {
            onDismissed?.let { cb ->
                onDismissed = null
                cb()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun show(
        skipPartiallyExpanded: Boolean = true,
        content: @Composable () -> Unit
    ) {
        if (isAnySheetShowing) return
        dismiss()
        isAnySheetShowing = true
        
        val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return
        val prefs = Prefs(activity)
        val isDark = prefs.appTheme == Constants.Theme.Dark
        val shouldHideStatusBar = !prefs.showStatusBar
        val shouldHideNavBar = !prefs.showNavigationBar
        val navBarHeightPx = if (prefs.showNavigationBar) {
            val rootView = activity.window.decorView
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                rootView.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.navigationBars())?.bottom ?: 0
            } else {
                androidx.core.view.ViewCompat.getRootWindowInsets(rootView)
                    ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            }
        } else 0

        if (shouldHideStatusBar) {
            try { hideStatusBar(activity) } catch (_: Exception) {}
        }
        if (shouldHideNavBar) {
            try { hideNavigationBar(activity) } catch (_: Exception) {}
        }

        if (shouldHideStatusBar && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val decorView = activity.window.decorView
            insetsListener = View.OnApplyWindowInsetsListener { v, insets ->
                val builder = android.view.WindowInsets.Builder(insets)
                builder.setInsets(android.view.WindowInsets.Type.statusBars(), android.graphics.Insets.NONE)
                v.onApplyWindowInsets(builder.build())
            }
            decorView.setOnApplyWindowInsetsListener(insetsListener)
        }

        val hostView = ComposeView(activity).apply {
            setContent {
                SettingsTheme(isDark) {
                    var open by remember { mutableStateOf(true) }
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(Unit) { sheetState.show() }

                    val sheetCornerRadius = when (prefs.textIslandsShape) {
                        0 -> 24.dp  // Pill
                        1 -> 12.dp  // Rounded
                        else -> 0.dp // Square
                    }

                    if (open) {
                        ModalBottomSheet(
                            onDismissRequest = { open = false },
                            sheetState = sheetState,
                            dragHandle = null,
                            shape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius),
                            containerColor = Color.Transparent,
                            scrimColor = Color.Transparent,
                            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
                        ) {
                            // Android 16+ when status bar is hidden).
                            run {
                                val sheetView = androidx.compose.ui.platform.LocalView.current
                                DisposableEffect(Unit) {
                                    fun configureDialogWindow(v: View) {
                                        val dialogWindow = v.findDialogWindow() ?: return
                                        // Draw behind system bars to prevent content jump
                                        dialogWindow.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                                        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                                        // Match bar appearance
                                        val controller = androidx.core.view.WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
                                        val activityController = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                                        controller.isAppearanceLightStatusBars = activityController.isAppearanceLightStatusBars
                                        controller.isAppearanceLightNavigationBars = activityController.isAppearanceLightNavigationBars
                                        // Hide bars on the dialog window itself
                                        if (shouldHideStatusBar) {
                                            dialogWindow.hideStatusBarOnWindow()
                                            try { hideStatusBar(activity) } catch (_: Exception) {}
                                        }
                                        if (shouldHideNavBar) {
                                            dialogWindow.hideNavBarOnWindow()
                                            try { hideNavigationBar(activity) } catch (_: Exception) {}
                                        }
                                    }
                                    val listener = object : View.OnAttachStateChangeListener {
                                        override fun onViewAttachedToWindow(v: View) { configureDialogWindow(v) }
                                        override fun onViewDetachedFromWindow(v: View) {}
                                    }
                                    sheetView.addOnAttachStateChangeListener(listener)
                                    if (sheetView.isAttachedToWindow) configureDialogWindow(sheetView)
                                    onDispose { sheetView.removeOnAttachStateChangeListener(listener) }
                                }
                            }
                            val sheetShape = RoundedCornerShape(topStart = sheetCornerRadius, topEnd = sheetCornerRadius)
                            val borderColor = Theme.colors.text
                            val backgroundColor = Theme.colors.background
                            val screenScale = rememberScreenScale()
                            val swallowDragConnection = remember {
                                object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
                                    override fun onPostScroll(
                                        consumed: Offset,
                                        available: Offset,
                                        source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
                                    ): Offset {
                                        return if (available.y > 0) available else Offset.Zero
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .nestedScroll(swallowDragConnection)
                                    .padding(horizontal = 24.dp.scaled(screenScale))
                            ) {
                                val sheetOpenTime = remember { System.currentTimeMillis() }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyUp &&
                                                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) &&
                                                System.currentTimeMillis() - sheetOpenTime < 500L
                                            ) {
                                                return@onPreviewKeyEvent true // swallow leaked KeyUp
                                            }
                                            false
                                        }
                                        .background(backgroundColor, sheetShape)
                                        .drawBehind {
                                            val borderWidth = 2.dp.toPx()
                                            val cornerRadius = sheetCornerRadius.toPx()
                                            val halfBorder = borderWidth / 2
                                            
                                            val path = Path().apply {
                                                // Start at bottom left
                                                moveTo(halfBorder, size.height)
                                                // Left side going up
                                                lineTo(halfBorder, cornerRadius)
                                                // Top left rounded corner
                                                arcTo(
                                                    rect = androidx.compose.ui.geometry.Rect(
                                                        left = halfBorder,
                                                        top = halfBorder,
                                                        right = cornerRadius * 2 + halfBorder,
                                                        bottom = cornerRadius * 2 + halfBorder
                                                    ),
                                                    startAngleDegrees = 180f,
                                                    sweepAngleDegrees = 90f,
                                                    forceMoveTo = false
                                                )
                                                // Top side
                                                lineTo(size.width - cornerRadius, halfBorder)
                                                // Top right rounded corner
                                                arcTo(
                                                    rect = androidx.compose.ui.geometry.Rect(
                                                        left = size.width - cornerRadius * 2 - halfBorder,
                                                        top = halfBorder,
                                                        right = size.width - halfBorder,
                                                        bottom = cornerRadius * 2 + halfBorder
                                                    ),
                                                    startAngleDegrees = 270f,
                                                    sweepAngleDegrees = 90f,
                                                    forceMoveTo = false
                                                )
                                                // Right side going down
                                                lineTo(size.width - halfBorder, size.height)
                                            }
                                            
                                            drawPath(
                                                path = path,
                                                color = borderColor,
                                                style = Stroke(width = borderWidth)
                                            )
                                        }
                                        .padding(horizontal = SettingsTheme.color.horizontalPadding)
                                        .padding(top = 24.dp.scaled(screenScale), bottom = 24.dp.scaled(screenScale) + with(
                                            LocalDensity.current) { navBarHeightPx.toDp() }),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    content()
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .size(width = 40.dp, height = 8.dp)
                                        .background(
                                            Theme.colors.text,
                                            RoundedCornerShape(
                                                topStart = 0.dp,
                                                topEnd = 0.dp,
                                                bottomStart = 2.dp,
                                                bottomEnd = 2.dp
                                            )
                                        )
                                )
                            }
                        }
                    }

                    LaunchedEffect(open) {
                        if (!open) {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                dismiss()
                            }
                        }
                    }
                }
            }
        }

        root.addView(
            hostView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        composeView = hostView
    }

}

private fun View.findDialogWindow(): android.view.Window? {
    var viewParent: android.view.ViewParent? = parent
    while (viewParent != null) {
        if (viewParent is androidx.compose.ui.window.DialogWindowProvider) {
            return viewParent.window
        }
        viewParent = (viewParent as? View)?.parent
    }
    return null
}

private fun android.view.Window.hideStatusBarOnWindow() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        insetsController?.let { controller ->
            controller.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(android.view.WindowInsets.Type.statusBars())
        }
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}

private fun android.view.Window.hideNavBarOnWindow() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        insetsController?.let { controller ->
            controller.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(android.view.WindowInsets.Type.navigationBars())
        }
    } else {
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}

@Composable
fun StepperSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int,
    step: Int = 1,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (maxValue < minValue) return
    val screenScale = rememberScreenScale()
    val valueFloat = value.toFloat()

    val sliderInteraction = remember { MutableInteractionSource() }
    val sliderFocused = sliderInteraction.collectIsFocusedAsState().value
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusable(interactionSource = sliderInteraction)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionLeft -> {
                        val nv = (value - step).coerceIn(minValue, maxValue)
                        if (nv != value) onValueChange(nv)
                        true
                    }
                    Key.DirectionRight -> {
                        val nv = (value + step).coerceIn(minValue, maxValue)
                        if (nv != value) onValueChange(nv)
                        true
                    }
                    else -> false
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp.scaled(screenScale))
                .clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    val nv = (value - step).coerceIn(minValue, maxValue)
                    if (nv != value) onValueChange(nv)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease), tint = Theme.colors.text, modifier = Modifier.size(20.dp.scaled(screenScale)))
        }

        val trackColor = Theme.colors.text
        val bgColor = Theme.colors.background
        val thumbRadius = 7.dp.scaled(screenScale)
        val trackHeight = 2.dp.scaled(screenScale)
        val borderWidth = 1.5.dp.scaled(screenScale)
        val range = maxValue.toFloat() - minValue.toFloat()
        Box(
            modifier = Modifier
                .weight(1f)
                .height(thumbRadius * 2 + 4.dp.scaled(screenScale))
                .pointerInput(minValue, maxValue, step) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val raw = ((fraction * range + minValue) / step).roundToInt() * step
                            val nv = raw.coerceIn(minValue, maxValue)
                            if (nv != value) onValueChange(nv)
                        }
                    ) { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val raw = ((fraction * range + minValue) / step).roundToInt() * step
                        val nv = raw.coerceIn(minValue, maxValue)
                        if (nv != value) onValueChange(nv)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val trackY = size.height / 2f
                val trackHeightPx = trackHeight.toPx()
                val thumbR = thumbRadius.toPx()
                val borderPx = borderWidth.toPx()
                val fraction = if (range > 0f) (valueFloat - minValue) / range else 0f
                val thumbX = fraction * size.width

                // Track
                drawRect(
                    color = trackColor,
                    topLeft = Offset(0f, trackY - trackHeightPx / 2f),
                    size = Size(size.width, trackHeightPx)
                )
                // Thumb: filled when focused, outlined when not
                val thumbFill = if (sliderFocused) trackColor else bgColor
                drawCircle(color = thumbFill, radius = thumbR, center = Offset(thumbX, trackY))
                drawCircle(color = trackColor, radius = thumbR, center = Offset(thumbX, trackY), style = Stroke(width = borderPx))
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp.scaled(screenScale))
                .clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    val nv = (value + step).coerceIn(minValue, maxValue)
                    if (nv != value) onValueChange(nv)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase), tint = Theme.colors.text, modifier = Modifier.size(20.dp.scaled(screenScale)))
        }
    }

}

@Composable
fun SliderSheet(
    title: String,
    minValue: Int,
    maxValue: Int,
    currentValue: Int,
    liveUpdate: Boolean = false,
    step: Int = 1,
    valueFormatter: (Int) -> String = { it.toString() },
    onValueSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableIntStateOf(currentValue) }

    SheetTitle(title)
    Text(
        text = valueFormatter(sliderValue),
        style = SettingsTheme.typography.item,
        color = Theme.colors.text,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    StepperSlider(
        value = sliderValue,
        onValueChange = {
            sliderValue = it
            if (liveUpdate) onValueSelected(sliderValue)
        },
        minValue = minValue,
        maxValue = maxValue,
        step = step,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    if (!liveUpdate) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Theme.colors.text, style = SettingsTheme.typography.button) }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onValueSelected(sliderValue); onDismiss() }) {
                Text(stringResource(R.string.btn_save), color = Theme.colors.text, style = SettingsTheme.typography.button)
            }
        }
    }
}

@Composable
fun <T> SingleChoiceSheet(
    title: String,
    options: List<T>,
    selectedIndex: Int?,
    optionLabel: (T) -> String = { it.toString() },
    fonts: List<AndroidTypeface>? = null,
    fontSize: Float = 18f,
    nonSelectable: (T) -> Boolean = { false },
    onItemDeleted: ((T, () -> Unit) -> Unit)? = null,
    isCustomFont: (T) -> Boolean = { false },
    isBuiltInFont: (T) -> Boolean = { false },
    onInfoClick: (() -> Unit)? = null,
    onSelect: (T, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedIndex) }
    LocalContext.current
    val textColor = Theme.colors.text
    Theme.colors.background

    SheetTitle(title)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        itemsIndexed(options) { index, option ->
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused = interactionSource.collectIsFocusedAsState().value
            val prefTextColor = Theme.colors.text
            val prefBackgroundColor = Theme.colors.background
            val controlColor = if (isFocused) prefBackgroundColor else Theme.colors.text
            val isNonSelectable = nonSelectable(option)
            val showDelete = onItemDeleted != null && isCustomFont(option)
            val label = optionLabel(option)
            val typeface = if (fonts != null && index < fonts.size) fonts[index] else null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .then(with(SettingsComposable) {
                        Modifier.pillHighlight(isFocused, 6.dp, prefTextColor)
                    })
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (isNonSelectable) {
                                onSelect(option, index)
                            } else {
                                currentSelection = index
                                onSelect(option, index)
                                onDismiss()
                            }
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !isNonSelectable && currentSelection == index,
                    onClick = null, // Handled by Row clickable
                    colors = RadioButtonDefaults.colors(
                        selectedColor = controlColor,
                        unselectedColor = controlColor
                    )
                )
                Spacer(Modifier.width(12.dp))
                if (typeface != null) {
                    val labelColor = if (isFocused) prefBackgroundColor else textColor
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                this.typeface = typeface
                                textSize = fontSize
                            }
                        },
                        update = { tv ->
                            tv.text = label
                            tv.setTextColor(android.graphics.Color.argb(
                                (labelColor.alpha * 255).toInt().coerceIn(0, 255),
                                (labelColor.red * 255).toInt().coerceIn(0, 255),
                                (labelColor.green * 255).toInt().coerceIn(0, 255),
                                (labelColor.blue * 255).toInt().coerceIn(0, 255)
                            ))
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Text(
                        label,
                        style = SettingsTheme.typography.item,
                        color = if (isFocused) prefBackgroundColor else Theme.colors.text,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (showDelete) {
                    Box(
                        modifier = Modifier
                            .size(32.dp.scaled(rememberScreenScale()))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    onItemDeleted?.invoke(option, onDismiss)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cd_delete_font),
                            tint = Theme.colors.text,
                            modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))
                        )
                    }
                }
                if (!isNonSelectable && isBuiltInFont(option) && onInfoClick != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp.scaled(rememberScreenScale()))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onInfoClick() }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.cd_font_license),
                            tint = Theme.colors.text,
                            modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabbedFontPickerSheet(
    title: String,
    builtInOptions: List<String>,
    builtInFonts: List<AndroidTypeface>,
    customOptions: List<String>,
    customFonts: List<AndroidTypeface>,
    selectedIndex: Int?,
    isBuiltInFont: (String) -> Boolean = { false },
    onInfoClick: (() -> Unit)? = null,
    addCustomFontLabel: String? = null,
    onAddCustomFont: (() -> Unit)? = null,
    onSelect: (String, Boolean) -> Unit,
    onDelete: ((String, () -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var currentSelection by remember { mutableStateOf(selectedIndex) }
    val textColor = Theme.colors.text
    val initialTab = if (selectedIndex != null && selectedIndex >= builtInOptions.size) 1 else 0
    val selectedTab = remember { mutableIntStateOf(initialTab) }

    val tabTitle = when (selectedTab.intValue) {
        1 -> stringResource(R.string.custom_fonts)
        else -> title
    }
    SheetTitle(tabTitle)
    Column(modifier = Modifier.fillMaxWidth()) {
        SheetTabRow(
            selectedIndex = selectedTab.intValue,
            tabs = listOf(
                { Text(stringResource(R.string.tab_fonts), style = SettingsTheme.typography.item, color = LocalContentColor.current) },
                { Text(stringResource(R.string.tab_custom), style = SettingsTheme.typography.item, color = LocalContentColor.current) }
            ),
            onTabSelected = { selectedTab.intValue = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            when (selectedTab.intValue) {
                0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    itemsIndexed(builtInOptions) { index, option ->
                        val typeface = if (index < builtInFonts.size) builtInFonts[index] else null
                        FontPickerRow(
                            label = option,
                            typeface = typeface,
                            isSelected = currentSelection == index,
                            textColor = textColor,
                            showInfoIcon = isBuiltInFont(option) && onInfoClick != null,
                            onInfoClick = onInfoClick,
                            onClick = {
                                currentSelection = index
                                onSelect(option, false)
                                onDismiss()
                            }
                        )
                    }
                }
                1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    if (addCustomFontLabel != null && onAddCustomFont != null) {
                        item {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused = interactionSource.collectIsFocusedAsState().value
                                            val prefTextColor = Theme.colors.text
                            val prefBackgroundColor = Theme.colors.background
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .then(with(SettingsComposable) {
                                        Modifier.pillHighlight(isFocused, 6.dp, prefTextColor)
                                    })
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { onAddCustomFont() }
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Match RadioButton sizing for alignment
                                Box(contentAlignment = Alignment.Center) {
                                    RadioButton(
                                        selected = false,
                                        onClick = null,
                                        modifier = Modifier.alpha(0f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.cd_add),
                                        tint = if (isFocused) prefBackgroundColor else textColor,
                                        modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    addCustomFontLabel,
                                    style = SettingsTheme.typography.item,
                                    color = if (isFocused) prefBackgroundColor else textColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    itemsIndexed(customOptions) { index, option ->
                        val globalIndex = builtInOptions.size + index
                        val typeface = if (index < customFonts.size) customFonts[index] else null
                        FontPickerRow(
                            label = option,
                            typeface = typeface,
                            isSelected = currentSelection == globalIndex,
                            textColor = textColor,
                            showDelete = onDelete != null,
                            onDelete = if (onDelete != null) { { onDelete(option, onDismiss) } } else null,
                            onClick = {
                                currentSelection = globalIndex
                                onSelect(option, true)
                                onDismiss()
                            }
                        )
                    }
                    if (customOptions.isEmpty() && (addCustomFontLabel == null || onAddCustomFont == null)) {
                        item {
                            Text(
                                stringResource(R.string.no_custom_fonts),
                                style = SettingsTheme.typography.item,
                                color = Theme.colors.text.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontPickerRow(
    label: String,
    typeface: AndroidTypeface?,
    isSelected: Boolean,
    textColor: Color,
    showInfoIcon: Boolean = false,
    onInfoClick: (() -> Unit)? = null,
    showDelete: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val prefTextColor = Theme.colors.text
    val prefBackgroundColor = Theme.colors.background
    val controlColor = if (isFocused) prefBackgroundColor else Theme.colors.text

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .then(with(SettingsComposable) {
                    Modifier.pillHighlight(isFocused, 6.dp, prefTextColor)
                })
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = controlColor,
                    unselectedColor = controlColor
                )
            )
            Spacer(Modifier.width(12.dp))
            if (typeface != null) {
                val labelColor = if (isFocused) prefBackgroundColor else textColor
                AndroidView(
                    factory = { ctx ->
                        TextView(ctx).apply {
                            this.typeface = typeface
                            textSize = 18f
                            maxLines = 1
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            includeFontPadding = false
                            gravity = android.view.Gravity.CENTER_VERTICAL
                        }
                    },
                    update = { tv ->
                        tv.text = label
                        tv.setTextColor(android.graphics.Color.argb(
                            (labelColor.alpha * 255).toInt().coerceIn(0, 255),
                            (labelColor.red * 255).toInt().coerceIn(0, 255),
                            (labelColor.green * 255).toInt().coerceIn(0, 255),
                            (labelColor.blue * 255).toInt().coerceIn(0, 255)
                        ))
                    },
                    modifier = Modifier.weight(1f).height(28.dp)
                )
            } else {
                Text(
                    label,
                    style = SettingsTheme.typography.item,
                    color = if (isFocused) prefBackgroundColor else Theme.colors.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (showDelete && onDelete != null) {
            Spacer(Modifier.width(8.dp))
            val deleteInteractionSource = remember { MutableInteractionSource() }
            val deleteIsFocused = deleteInteractionSource.collectIsFocusedAsState().value
            val deleteIconColor = if (deleteIsFocused) prefBackgroundColor else prefTextColor
            Box(
                modifier = Modifier
                    .size(28.dp.scaled(rememberScreenScale()))
                    .then(with(SettingsComposable) {
                        Modifier.pillHighlight(deleteIsFocused, 4.dp, prefTextColor)
                    })
                    .focusable(interactionSource = deleteInteractionSource)
                    .clickable(
                        indication = null,
                        interactionSource = deleteInteractionSource,
                        onClick = onDelete
                    )
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown &&
                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                        ) {
                            onDelete()
                            true
                        } else false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_font),
                    tint = deleteIconColor,
                    modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))
                )
            }
        }
        if (showInfoIcon && onInfoClick != null) {
            Spacer(Modifier.width(8.dp))
            val infoInteractionSource = remember { MutableInteractionSource() }
            val infoIsFocused = infoInteractionSource.collectIsFocusedAsState().value
            val infoIconColor = if (infoIsFocused) prefBackgroundColor else prefTextColor
            Box(
                modifier = Modifier
                    .size(28.dp.scaled(rememberScreenScale()))
                    .then(with(SettingsComposable) {
                        Modifier.pillHighlight(infoIsFocused, 4.dp, prefTextColor)
                    })
                    .focusable(interactionSource = infoInteractionSource)
                    .clickable(
                        indication = null,
                        interactionSource = infoInteractionSource,
                        onClick = { onInfoClick() }
                    )
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown &&
                            (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter)
                        ) {
                            onInfoClick()
                            true
                        } else false
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.cd_font_license),
                    tint = infoIconColor,
                    modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))
                )
            }
        }
    }
}

@Composable
fun GestureActionPickerSheet(
    title: String,
    @Suppress("UNUSED_PARAMETER") flag: AppDrawerFlag,
    allowedActions: List<Action>,
    currentAction: Action,
    currentAppLabel: String,
    viewModel: MainViewModel,
    onSelect: (Action, AppListItem?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val selectedTabIndex = remember { mutableIntStateOf(0) }
    val showOpenAppTab = Action.OpenApp in allowedActions

    val inkosActions = buildList {
        if (Action.Disabled in allowedActions) add(Action.Disabled)
        addAll(Action.INKOS_ACTIONS.filter { it in allowedActions })
    }
    val systemActions = buildList {
        if (Action.Disabled in allowedActions) add(Action.Disabled)
        addAll(Action.SYSTEM_ACTIONS.filter { it != Action.Disabled && it in allowedActions })
    }

    val appList by viewModel.getAppList(includeHiddenApps = true, flag = null).collectAsState(initial = emptyList())
    val filteredApps = remember(appList) {
        appList.filter {
            val pkg = it.activityPackage
            pkg.isNotBlank() &&
                !pkg.startsWith("com.inkos.internal.") &&
                !(pkg == context.packageName && !it.shortcutId.isNullOrEmpty())
        }.sortedWith(
            compareBy(java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }) {
                it.customLabel.takeIf { l -> l.isNotEmpty() } ?: it.activityLabel
            }
        )
    }

    val gestureTabTitle = when (selectedTabIndex.intValue) {
        0 -> if (showOpenAppTab) context.getString(R.string.open_app) else "inkOS"
        1 -> if (showOpenAppTab) "inkOS" else "Android"
        2 -> "Android"
        else -> title
    }
    SheetTitle(gestureTabTitle)
    Column(modifier = Modifier.fillMaxWidth()) {
        SheetTabRow(
            selectedIndex = selectedTabIndex.intValue,
            tabs = buildList {
                if (showOpenAppTab) add(@Composable { Text(context.getString(R.string.open_app), style = SettingsTheme.typography.item, color = LocalContentColor.current) })
                add(@Composable { Icon(painter = painterResource(R.drawable.ic_foreground), contentDescription = context.getString(R.string.gesture_tab_inkos), modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))) })
                add(@Composable { Icon(painter = painterResource(R.drawable.ic_system_shortcut), contentDescription = context.getString(R.string.gesture_tab_system), modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))) })
            },
            onTabSelected = { selectedTabIndex.intValue = it }
        )
        Spacer(modifier = Modifier.height(16.dp))

        val currentTab = selectedTabIndex.intValue
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            when {
                showOpenAppTab && currentTab == 0 -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        if (Action.Disabled in allowedActions) {
                            item {
                                GesturePickerRow(
                                    label = context.getString(R.string.disabled),
                                    isSelected = currentAction == Action.Disabled
                                ) {
                                    onSelect(Action.Disabled, null)
                                    onDismiss()
                                }
                            }
                        }
                        items(filteredApps) { app ->
                            val label = app.customLabel.takeIf { it.isNotEmpty() } ?: app.activityLabel
                            GesturePickerRow(
                                label = label,
                                isSelected = currentAction == Action.OpenApp && currentAppLabel == label
                            ) {
                                onSelect(Action.OpenApp, app)
                                onDismiss()
                            }
                        }
                    }
                }
                (showOpenAppTab && currentTab == 1) || (!showOpenAppTab && currentTab == 0) -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        items(inkosActions) { action ->
                            val label = if (action == Action.OpenAppDrawer) {
                                context.getString(R.string.app_drawer)
                            } else {
                                action.getString(context)
                            }
                            GesturePickerRow(
                                label = label,
                                isSelected = currentAction == action
                            ) {
                                onSelect(action, null)
                                onDismiss()
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        items(systemActions) { action ->
                            val label = if (action == Action.OpenAppDrawer) {
                                context.getString(R.string.app_drawer)
                            } else {
                                action.getString(context)
                            }
                            GesturePickerRow(
                                label = label,
                                isSelected = currentAction == action
                            ) {
                                onSelect(action, null)
                                onDismiss()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GesturePickerRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val prefTextColor = Theme.colors.text
    val prefBackgroundColor = Theme.colors.background
    val controlColor = if (isFocused) prefBackgroundColor else Theme.colors.text

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(with(SettingsComposable) { Modifier.pillHighlight(isFocused, 6.dp, prefTextColor) })
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = controlColor,
                unselectedColor = controlColor
            )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = SettingsTheme.typography.item,
            color = if (isFocused) prefBackgroundColor else Theme.colors.text,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
fun MultiChoiceSheet(
    title: String,
    items: List<String>,
    initialChecked: BooleanArray,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
    showSelectAll: Boolean = false
) {
    val checkedList = remember {
        mutableStateListOf<Boolean>().apply { addAll(initialChecked.toList()) }
    }
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val context = LocalContext.current
    val checkShape = remember { ShapeHelper.getRoundedCornerShape(Prefs(context).textIslandsShape, pillRadius = 50.dp, roundedRadius = 4.dp) }

    DisposableEffect(Unit) {
        onDispose {
            onConfirm(checkedList.indices.filter { checkedList[it] })
        }
    }

    SheetTitle(title)
    if (showSelectAll) {
        val screenScale = rememberScreenScale()
        val buttonShape = remember { ShapeHelper.getRoundedCornerShape(Prefs(context).textIslandsShape, pillRadius = 50.dp, roundedRadius = 4.dp) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectAllToggleButton(
                label = stringResource(R.string.select_all),
                textColor = textColor,
                bgColor = bgColor,
                shape = buttonShape,
                screenScale = screenScale,
                onClick = {
                    for (i in checkedList.indices) checkedList[i] = true
                    try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                }
            )
            SelectAllToggleButton(
                label = stringResource(R.string.deselect_all),
                textColor = textColor,
                bgColor = bgColor,
                shape = buttonShape,
                screenScale = screenScale,
                onClick = {
                    for (i in checkedList.indices) checkedList[i] = false
                    try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                }
            )
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        items(items.size, key = { it }) { index ->
            MultiChoiceRow(
                label = items[index],
                checked = checkedList[index],
                textColor = textColor,
                bgColor = bgColor,
                checkShape = checkShape,
                onToggle = { checkedList[index] = it }
            )
        }
    }
}

@Composable
private fun SelectAllToggleButton(
    label: String,
    textColor: Color,
    bgColor: Color,
    shape: androidx.compose.ui.graphics.Shape,
    screenScale: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val fg = if (isFocused) bgColor else textColor
    val bg = if (isFocused) textColor else Color.Transparent
    val baseFontSize = SettingsTheme.typography.button.fontSize
    val scaledFontSize = if (baseFontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (baseFontSize.value * 0.65f).sp else 11.sp
    Box(
        modifier = Modifier
            .border(2.dp.scaled(screenScale), textColor, shape)
            .background(bg, shape)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            style = SettingsTheme.typography.button,
            fontSize = scaledFontSize
        )
    }
}

@Composable
private fun MultiChoiceRow(
    label: String,
    checked: Boolean,
    textColor: Color,
    bgColor: Color,
    checkShape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
    onToggle: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val rowTextColor = if (isFocused) bgColor else textColor
    val rowBgColor = if (isFocused) textColor else bgColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(with(SettingsComposable) { Modifier.pillHighlight(isFocused, 6.dp, textColor, outerVertical = 6.dp) })
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onToggle(!checked)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(1.5.dp, rowTextColor, checkShape)
                .then(if (checked) Modifier.background(rowTextColor, checkShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = rowBgColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = SettingsTheme.typography.item, color = rowTextColor)
    }
}
data class ShortcutGroup(
    val groupName: String,
    val items: List<ShortcutItem>
)

data class ShortcutItem(
    val key: String,
    val label: String
)

@Composable
fun GroupedMultiChoiceSheet(
    title: String,
    pinnedGroup: ShortcutGroup?,
    appGroups: List<ShortcutGroup>,
    selectedKeys: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val currentSelection = remember { mutableStateOf(selectedKeys.toMutableSet()) }
    val checkShapeContext = LocalContext.current
    val checkShape = remember { ShapeHelper.getRoundedCornerShape(Prefs(checkShapeContext).textIslandsShape, pillRadius = 50.dp, roundedRadius = 4.dp) }

    fun toggleItem(key: String) {
        val newSelection = currentSelection.value.toMutableSet()
        if (newSelection.contains(key)) {
            newSelection.remove(key)
        } else {
            newSelection.add(key)
        }
        currentSelection.value = newSelection
        onSelectionChanged(newSelection)
    }

    fun toggleGroup(group: ShortcutGroup) {
        val groupKeys = group.items.map { it.key }.toSet()
        val allSelected = groupKeys.all { currentSelection.value.contains(it) }
        val newSelection = currentSelection.value.toMutableSet()
        if (allSelected) {
            // Deselect all in group
            newSelection.removeAll(groupKeys)
        } else {
            // Select all in group
            newSelection.addAll(groupKeys)
        }
        currentSelection.value = newSelection
        onSelectionChanged(newSelection)
    }
    
    fun isGroupFullySelected(group: ShortcutGroup): Boolean {
        return group.items.all { currentSelection.value.contains(it.key) }
    }
    
    fun isGroupPartiallySelected(group: ShortcutGroup): Boolean {
        val selectedCount = group.items.count { currentSelection.value.contains(it.key) }
        return selectedCount > 0 && selectedCount < group.items.size
    }

    SheetTitle(title)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Pinned shortcuts section
        if (pinnedGroup != null && pinnedGroup.items.isNotEmpty()) {
            item {
                GroupHeader(
                    groupName = pinnedGroup.groupName,
                    isFullySelected = isGroupFullySelected(pinnedGroup),
                    isPartiallySelected = isGroupPartiallySelected(pinnedGroup),
                    checkShape = checkShape,
                    onClick = { toggleGroup(pinnedGroup) }
                )
            }
            items(pinnedGroup.items) { item ->
                ShortcutItemRow(
                    item = item,
                    isSelected = currentSelection.value.contains(item.key),
                    onToggle = { toggleItem(item.key) },
                    indented = true,
                    checkShape = checkShape
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
            }
        }
        
        // App groups
        appGroups.forEach { group ->
            item {
                GroupHeader(
                    groupName = group.groupName,
                    isFullySelected = isGroupFullySelected(group),
                    isPartiallySelected = isGroupPartiallySelected(group),
                    checkShape = checkShape,
                    onClick = { toggleGroup(group) }
                )
            }
            items(group.items) { item ->
                ShortcutItemRow(
                    item = item,
                    isSelected = currentSelection.value.contains(item.key),
                    onToggle = { toggleItem(item.key) },
                    indented = true,
                    checkShape = checkShape
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(
    groupName: String,
    isFullySelected: Boolean,
    @Suppress("UNUSED_PARAMETER") isPartiallySelected: Boolean,
    checkShape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp),
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val rowTextColor = if (isFocused) bgColor else textColor
    val rowBgColor = if (isFocused) textColor else bgColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(with(SettingsComposable) {
                Modifier.pillHighlight(isFocused, 6.dp, textColor)
            })
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(1.5.dp, rowTextColor, checkShape)
                .then(if (isFullySelected) Modifier.background(rowTextColor, checkShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (isFullySelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = rowBgColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            groupName,
            style = SettingsTheme.typography.item.copy(fontWeight = FontWeight.Bold),
            color = rowTextColor
        )
    }
}

@Composable
private fun ShortcutItemRow(
    item: ShortcutItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    @Suppress("UNUSED_PARAMETER") indented: Boolean = false,
    checkShape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val rowTextColor = if (isFocused) bgColor else textColor
    val rowBgColor = if (isFocused) textColor else bgColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(with(SettingsComposable) {
                Modifier.pillHighlight(isFocused, 6.dp, textColor)
            })
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(1.5.dp, rowTextColor, checkShape)
                .then(if (isSelected) Modifier.background(rowTextColor, checkShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = rowBgColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "• ${item.label}",
            style = SettingsTheme.typography.item,
            color = rowTextColor
        )
    }
}

@Composable
private fun AppGroupLabelRow(groupName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            groupName,
            style = SettingsTheme.typography.item.copy(fontWeight = FontWeight.Bold),
            color = Theme.colors.text
        )
    }
}

@Composable
fun TabbedShortcutsSheet(
    title: String,
    appGroups: List<ShortcutGroup>,
    inkosItems: List<ShortcutItem>,
    pinnedItems: List<ShortcutItem>,
    selectedKeys: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentSelection = remember { mutableStateOf(selectedKeys.toMutableSet()) }
    val checkShapeContext = LocalContext.current
    val checkShape = remember { ShapeHelper.getRoundedCornerShape(Prefs(checkShapeContext).textIslandsShape, pillRadius = 50.dp, roundedRadius = 4.dp) }
    val selectedTabIndex = remember { mutableIntStateOf(0) }

    fun toggleItem(key: String) {
        val newSelection = currentSelection.value.toMutableSet()
        if (newSelection.contains(key)) newSelection.remove(key) else newSelection.add(key)
        currentSelection.value = newSelection
        onSelectionChanged(newSelection)
    }

    val tabTitle = when (selectedTabIndex.intValue) {
        1 -> "inkOS"
        2 -> "Android"
        3 -> context.getString(R.string.shortcut_tab_pinned)
        else -> title
    }
    SheetTitle(tabTitle)
    Column(modifier = Modifier.fillMaxWidth()) {
        SheetTabRow(
            selectedIndex = selectedTabIndex.intValue,
            tabs = listOf(
                { Icon(imageVector = Icons.Rounded.Apps, contentDescription = context.getString(R.string.shortcut_tab_apps), modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))) },
                { Icon(painter = painterResource(R.drawable.ic_foreground), contentDescription = context.getString(R.string.gesture_tab_inkos), modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))) },
                { Icon(imageVector = Icons.Rounded.Android, contentDescription = context.getString(R.string.gesture_tab_system), modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))) },
                { Icon(imageVector = Icons.Rounded.PushPin, contentDescription = context.getString(R.string.shortcut_tab_pinned), modifier = Modifier.size(24.dp.scaled(rememberScreenScale()))) }
            ),
            onTabSelected = { selectedTabIndex.intValue = it }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            when (selectedTabIndex.intValue) {
                0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    appGroups.forEach { group ->
                        item { AppGroupLabelRow(group.groupName) }
                        items(group.items) { item ->
                            ShortcutItemRow(
                                item = item,
                                isSelected = currentSelection.value.contains(item.key),
                                onToggle = { toggleItem(item.key) },
                                indented = true,
                                checkShape = checkShape
                            )
                        }
                    }
                    if (appGroups.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_app_shortcuts),
                                style = SettingsTheme.typography.item,
                                color = Theme.colors.text,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(inkosItems) { item ->
                        ShortcutItemRow(
                            item = item,
                            isSelected = currentSelection.value.contains(item.key),
                            onToggle = { toggleItem(item.key) },
                            indented = false,
                            checkShape = checkShape
                        )
                    }
                    if (inkosItems.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_inkos_shortcuts),
                                style = SettingsTheme.typography.item,
                                color = Theme.colors.text,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                2 -> Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.system_shortcuts_search_hint),
                        style = SettingsTheme.typography.item,
                        color = Theme.colors.text
                    )
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    items(pinnedItems) { item ->
                        ShortcutItemRow(
                            item = item,
                            isSelected = currentSelection.value.contains(item.key),
                            onToggle = { toggleItem(item.key) },
                            indented = false,
                            checkShape = checkShape
                        )
                    }
                    if (pinnedItems.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.no_pinned_shortcuts),
                                style = SettingsTheme.typography.item,
                                color = Theme.colors.text,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputSheet(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }

    fun saveAndDismiss() {
        onConfirm(textValue)
        onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Enter || keyEvent.key == Key.DirectionCenter)) {
                    saveAndDismiss()
                    true
                } else false
            }
    ) {
        SheetTitle(title)
        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it.replace("\n", "") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textStyle = SettingsTheme.typography.item.copy(color = Theme.colors.text),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { saveAndDismiss() })
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Theme.colors.text, style = SettingsTheme.typography.button) }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { saveAndDismiss() }) {
                Text(stringResource(R.string.btn_save), color = Theme.colors.text, style = SettingsTheme.typography.button)
            }
        }
    }
}

private fun colorToHsv(c: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (c.red * 255).roundToInt().coerceIn(0, 255),
        (c.green * 255).roundToInt().coerceIn(0, 255),
        (c.blue * 255).roundToInt().coerceIn(0, 255),
        hsv
    )
    return hsv
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float): Color {
    val androidColor = android.graphics.Color.HSVToColor(
        (alpha * 255).roundToInt().coerceIn(0, 255),
        floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    )
    return Color(androidColor)
}

private fun colorToHex(c: Color): String {
    val r = (c.red * 255).roundToInt().coerceIn(0, 255)
    val g = (c.green * 255).roundToInt().coerceIn(0, 255)
    val b = (c.blue * 255).roundToInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun parseHex(hex: String): Color? {
    val s = hex.trim().removePrefix("#")
    if (s.length != 6) return null
    val r = s.substring(0, 2).toIntOrNull(16) ?: return null
    val g = s.substring(2, 4).toIntOrNull(16) ?: return null
    val b = s.substring(4, 6).toIntOrNull(16) ?: return null
    return Color(r / 255f, g / 255f, b / 255f, 1f)
}

@Composable
fun ColorPickerSheet(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val initialHsv = remember(initialColor) { colorToHsv(initialColor) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    val color = remember(hue, saturation, value) {
        hsvToColor(hue, saturation, value, 1f)
    }
    val hueColor = remember(hue) { hsvToColor(hue, 1f, 1f, 1f) }
    val hexString = remember(color) { colorToHex(color) }
    val onColorSelectedState = rememberUpdatedState(onColorSelected)
    LaunchedEffect(color) { onColorSelectedState.value(color) }
    val screenScale = rememberScreenScale()
    val pickerHeight = 120.dp.scaled(screenScale)
    val boxShape = RoundedCornerShape(8.dp.scaled(screenScale))
    var boxSize by remember { mutableStateOf(Offset.Zero) }
    val focusManager = LocalFocusManager.current

    SheetTitle(title)
    Spacer(Modifier.height(4.dp.scaled(screenScale)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(pickerHeight)
            .clip(boxShape)
            .border(1.5.dp.scaled(screenScale), Theme.colors.text, boxShape)
            .onSizeChanged { boxSize = Offset(it.width.toFloat(), it.height.toFloat()) }
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.Enter, Key.DirectionCenter -> {
                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    }
                    Key.DirectionLeft -> { saturation = (saturation - 0.03f).coerceIn(0f, 1f); true }
                    Key.DirectionRight -> { saturation = (saturation + 0.03f).coerceIn(0f, 1f); true }
                    Key.DirectionUp -> { value = (value + 0.03f).coerceIn(0f, 1f); true }
                    Key.DirectionDown -> { value = (value - 0.03f).coerceIn(0f, 1f); true }
                    else -> false
                }
            }
            .pointerInput(boxSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = boxSize.x.coerceAtLeast(1f)
                        val h = boxSize.y.coerceAtLeast(1f)
                        saturation = (offset.x / w).coerceIn(0f, 1f)
                        value = 1f - (offset.y / h).coerceIn(0f, 1f)
                    }
                ) { change, _ ->
                    change.consume()
                    val w = boxSize.x.coerceAtLeast(1f)
                    val h = boxSize.y.coerceAtLeast(1f)
                    saturation = (change.position.x / w).coerceIn(0f, 1f)
                    value = 1f - (change.position.y / h).coerceIn(0f, 1f)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, hueColor),
                    startX = 0f,
                    endX = w
                ),
                size = size
            )
            // Layer 2: vertical transparent -> black (value)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = h
                ),
                size = size
            )
            // Handle circle at (saturation * w, (1-value) * h)
            val px = saturation * size.width
            val py = (1f - value) * size.height
            val r = 8.dp.toPx()
            drawCircle(Color.White, radius = r, center = Offset(px, py))
            drawCircle(Color.Black.copy(alpha = 0.3f), radius = r + 1.5.dp.toPx(), center = Offset(px, py), style = Stroke(width = 1.5.dp.toPx()))
        }
    }

    // Hue slider: rainbow gradient track
    Spacer(Modifier.height(12.dp.scaled(screenScale)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp.scaled(screenScale))
            .clip(boxShape)
            .border(1.5.dp.scaled(screenScale), Theme.colors.text, boxShape)
    ) {
        val hueThumbR = 7.dp.scaled(screenScale)
        val hueBorderPx = 1.5.dp.scaled(screenScale)
        val hueBgColor = Theme.colors.background
        val hueTextColor = Theme.colors.text
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (keyEvent.key) {
                        Key.Enter, Key.DirectionCenter -> {
                            focusManager.moveFocus(FocusDirection.Next)
                            true
                        }
                        Key.DirectionLeft -> { hue = (hue - 5f).coerceIn(0f, 360f); true }
                        Key.DirectionRight -> { hue = (hue + 5f).coerceIn(0f, 360f); true }
                        else -> false
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            hue = ((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                        }
                    ) { change, _ ->
                        change.consume()
                        hue = ((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                    }
                }
        ) {
            val centerY = size.height / 2f
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF0000.toInt()), Color(0xFFFF7F00.toInt()),
                        Color(0xFFFFFF00.toInt()), Color(0xFF00FF00.toInt()),
                        Color(0xFF00FFFF.toInt()), Color(0xFF0000FF.toInt()),
                        Color(0xFF8B00FF.toInt()), Color(0xFFFF0000.toInt())
                    ),
                    startX = 0f, endX = size.width
                )
            )
            val thumbX = (hue / 360f) * size.width
            val thumbRPx = hueThumbR.toPx()
            val borderPx = hueBorderPx.toPx()
            drawCircle(color = hueBgColor, radius = thumbRPx, center = Offset(thumbX, centerY))
            drawCircle(color = hueTextColor, radius = thumbRPx, center = Offset(thumbX, centerY), style = Stroke(width = borderPx))
        }
    }

    // Hex color bar with swatch, hex code, copy/paste
    Spacer(Modifier.height(12.dp.scaled(screenScale)))
    val colorHexBarHeight = 28.dp.scaled(screenScale)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(colorHexBarHeight)
            .clip(boxShape)
            .border(1.5.dp.scaled(screenScale), Theme.colors.text, boxShape)
    ) {
        Box(
            modifier = Modifier
                .width(colorHexBarHeight)
                .fillMaxHeight()
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(1.5.dp.scaled(screenScale))
                .fillMaxHeight()
                .background(Theme.colors.text)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp.scaled(screenScale)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = hexString,
                style = SettingsTheme.typography.item,
                fontSize = SettingsTheme.typography.item.fontSize * 0.7f,
                color = Theme.colors.text,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .width(1.5.dp.scaled(screenScale))
                .fillMaxHeight()
                .background(Theme.colors.text)
        )
        Text(
            text = stringResource(R.string.btn_copy),
            style = SettingsTheme.typography.item,
            fontSize = SettingsTheme.typography.item.fontSize * 0.7f,
            color = Theme.colors.text,
            maxLines = 1,
            modifier = Modifier
                .fillMaxHeight()
                .clickable {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
                        ClipData.newPlainText("color", hexString)
                    )
                    android.widget.Toast.makeText(context, context.getString(R.string.toast_copied), android.widget.Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 8.dp.scaled(screenScale))
                .wrapContentHeight(Alignment.CenterVertically)
        )
        Box(
            modifier = Modifier
                .width(1.5.dp.scaled(screenScale))
                .fillMaxHeight()
                .background(Theme.colors.text)
        )
        Text(
            text = stringResource(R.string.btn_paste),
            style = SettingsTheme.typography.item,
            fontSize = SettingsTheme.typography.item.fontSize * 0.7f,
            color = Theme.colors.text,
            maxLines = 1,
            modifier = Modifier
                .fillMaxHeight()
                .clickable {
                    val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip
                    val pasted = clip?.getItemAt(0)?.text?.toString() ?: return@clickable
                    parseHex(pasted)?.let { parsed ->
                        val hsv = colorToHsv(parsed)
                        hue = hsv[0]
                        saturation = hsv[1]
                        value = hsv[2]
                        android.widget.Toast.makeText(context, context.getString(R.string.toast_pasted), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 8.dp.scaled(screenScale))
                .wrapContentHeight(Alignment.CenterVertically)
        )
    }

}

@Composable
fun PrivacyPolicySheet(onDismiss: () -> Unit) {
    SheetTitle("Privacy Policy")
    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        LazyColumn {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(stringResource(R.string.privacy_updated), style = SettingsTheme.typography.item, color = Theme.colors.text)

                    Text(stringResource(R.string.privacy_no_data_title), style = SettingsTheme.typography.title, color = Theme.colors.text, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.privacy_no_data_body), style = SettingsTheme.typography.item, color = Theme.colors.text)

                    Text(stringResource(R.string.privacy_no_server_title), style = SettingsTheme.typography.title, color = Theme.colors.text, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.privacy_no_server_body), style = SettingsTheme.typography.item, color = Theme.colors.text)

                    Text(stringResource(R.string.privacy_local_title), style = SettingsTheme.typography.title, color = Theme.colors.text, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.privacy_local_body), style = SettingsTheme.typography.item, color = Theme.colors.text)

                    Text(stringResource(R.string.privacy_open_source_title), style = SettingsTheme.typography.title, color = Theme.colors.text, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.privacy_open_source_body), style = SettingsTheme.typography.item, color = Theme.colors.text)
                }
            }
        }
    }
}

@Composable
fun FontLicenseSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val licenseText = remember {
        context.resources.openRawResource(R.raw.ofl_license).bufferedReader().use { it.readText() }
    }
    SheetTitle(stringResource(R.string.ofl_title))
    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        LazyColumn {
            item {
                Text(
                    text = licenseText,
                    style = SettingsTheme.typography.item,
                    color = Theme.colors.text,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun GestureVsPageConflictSheet(onDismiss: () -> Unit) {
    SheetTitle("Gesture vs page conflict")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "By default if you have multiple pages of apps in home, the swipe up/down will require long swipes.",
            style = SettingsTheme.typography.item,
            color = Theme.colors.text
        )
        Text(
            text = "If you have only one page of apps, it will require short swipes.",
            style = SettingsTheme.typography.item,
            color = Theme.colors.text
        )
        Text(
            text = "So you can choose either way, also below you can adjust the swiping thresholds.",
            style = SettingsTheme.typography.item,
            color = Theme.colors.text
        )
    }
}

@Composable
fun ErrorSheet(title: String, message: String, onDismiss: () -> Unit) {
    SheetTitle(title)
    Text(text = message, style = SettingsTheme.typography.item, color = Theme.colors.text, modifier = Modifier.padding(vertical = 8.dp))
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_ok), color = Theme.colors.text, style = SettingsTheme.typography.button) }
    }
}

@Composable
fun BackupRestoreSheet(
    onBackupAllData: () -> Unit,
    onRestoreData: () -> Unit,
    onClearAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    SheetTitle(stringResource(R.string.backup_restore_title))

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.backup_all_data),
            imageVector = Icons.Rounded.Smartphone,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = { onBackupAllData(); onDismiss() }
        )
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.restore_data),
            imageVector = Icons.Rounded.Restore,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = { onRestoreData(); onDismiss() }
        )
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.clear_all_data),
            imageVector = Icons.Filled.Delete,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = {
                onDismiss()
                onClearAllData()
            }
        )
    }
}

@Composable
fun ThemeSheet(
    onExportTheme: () -> Unit,
    onImportTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    SheetTitle(stringResource(R.string.theme_export_import))

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.export_theme),
            imageVector = Icons.Rounded.Edit,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = { onExportTheme(); onDismiss() }
        )
        SettingsComposable.SettingsHomeItem(
            title = stringResource(R.string.import_theme),
            imageVector = Icons.Rounded.FileOpen,
            horizontalPadding = 0.dp,
            verticalPadding = 8.dp,
            focusVerticalPadding = 16.dp,
            onClick = { onImportTheme(); onDismiss() }
        )
    }
}

@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    SheetTitle(title)
    Text(
        text = message,
        style = SettingsTheme.typography.item,
        color = Theme.colors.text,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(cancelText, color = Theme.colors.text, style = SettingsTheme.typography.button) }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmText, color = Theme.colors.text, style = SettingsTheme.typography.button) }
    }
}

@Composable
fun SetWallpaperSheet(
    onSetForHome: () -> Unit,
    onSetForLockScreen: () -> Unit,
    onSetForBoth: () -> Unit,
    onSetInkOSNoCrop: () -> Unit = {},
    onDismiss: () -> Unit,
    onShowInkosInfo: () -> Unit
) {
    var showAndroidOptions by remember { mutableStateOf(false) }

    SheetTitle("Set wallpaper for")

    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        if (!showAndroidOptions) {
            SettingsComposable.SettingsHomeItem(
                title = stringResource(R.string.wallpaper_inkos),
                iconRes = R.drawable.ic_foreground,
                horizontalPadding = 0.dp,
                verticalPadding = 8.dp,
                focusVerticalPadding = 16.dp,
                onClick = { onSetInkOSNoCrop(); onDismiss() }
            )

            SettingsComposable.SettingsHomeItem(
                title = stringResource(R.string.wallpaper_android),
                imageVector = Icons.Rounded.Android,
                horizontalPadding = 0.dp,
                verticalPadding = 8.dp,
                focusVerticalPadding = 16.dp,
                onClick = { showAndroidOptions = true }
            )

            SettingsComposable.SettingsHomeItem(
                title = stringResource(R.string.about_inkos_wallpaper),
                imageVector = Icons.Outlined.Info,
                horizontalPadding = 0.dp,
                verticalPadding = 8.dp,
                focusVerticalPadding = 16.dp,
                onClick = onShowInkosInfo
            )
        } else {
            SettingsComposable.SettingsHomeItem(
                title = stringResource(R.string.wallpaper_home_screen),
                imageVector = Icons.Rounded.Home,
                horizontalPadding = 0.dp,
                verticalPadding = 8.dp,
                focusVerticalPadding = 16.dp,
                onClick = { onSetForHome(); onDismiss() }
            )

            SettingsComposable.SettingsHomeItem(
                title = stringResource(R.string.wallpaper_lock_screen),
                imageVector = Icons.Rounded.Lock,
                horizontalPadding = 0.dp,
                verticalPadding = 8.dp,
                focusVerticalPadding = 16.dp,
                onClick = { onSetForLockScreen(); onDismiss() }
            )

            SettingsComposable.SettingsHomeItem(
                title = stringResource(R.string.wallpaper_both),
                imageVector = Icons.Rounded.Smartphone,
                horizontalPadding = 0.dp,
                verticalPadding = 8.dp,
                focusVerticalPadding = 16.dp,
                onClick = { onSetForBoth(); onDismiss() }
            )
        }
    }
}

@Composable
fun InkosWallpaperInfoSheet(onDismiss: () -> Unit) {
    SheetTitle("What is inkOS Wallpaper?")
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.inkos_wallpaper_explanation),
            style = SettingsTheme.typography.item,
            color = Theme.colors.text,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
fun IconStylePickerSheet(
    currentMode: Int,
    currentIconPack: String,
    iconPacks: List<Pair<String, String>>,
    showIcons: Boolean = true,
    currentIconShape: Int = 0,
    onSelect: (mode: Int, packPackage: String?) -> Unit,
    onShapeSelect: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val screenScale = rememberScreenScale()
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val prefs = remember { Prefs(context) }
    var liveIconShape by remember { mutableIntStateOf(currentIconShape) }
    val islandsShape = remember { prefs.textIslandsShape }
    remember(islandsShape) { ShapeHelper.getRoundedCornerShape(islandsShape, pillRadius = 50.dp, roundedRadius = 4.dp) }
    val cardShape = remember(islandsShape) { ShapeHelper.getRoundedCornerShape(islandsShape, pillRadius = 12.dp, roundedRadius = 6.dp) }
    val iconPreviewShape = remember(liveIconShape) { ShapeHelper.getRoundedCornerShape(liveIconShape, pillRadius = 50.dp, roundedRadius = 8.dp) }
    val iconSizePx = remember { (48 * context.resources.displayMetrics.density).toInt() }

    // Android system icon for System preview
    val androidBitmap = remember {
        try {
            val drawable = context.packageManager.getApplicationIcon("com.android.settings")
            IconUtility.drawableToBitmap(drawable, iconSizePx)
        } catch (_: Exception) { null }
    }

    data class IconOption(val label: String, val mode: Int, val packPkg: String? = null)
    val options = remember(iconPacks) {
        buildList {
            add(IconOption(context.getString(R.string.icon_none), -1))
            add(IconOption(context.getString(R.string.icon_letters), 0))
            add(IconOption(context.getString(R.string.icon_inkos), 4))
            add(IconOption(context.getString(R.string.icon_minimal), 5))
            add(IconOption(context.getString(R.string.icon_filled), 6))
            add(IconOption(context.getString(R.string.icon_system), 2))
            iconPacks.forEach { (pkg, name) -> add(IconOption(name, 3, pkg)) }
        }
    }
    var selectedIdx by remember(currentMode, currentIconPack, showIcons) {
        mutableIntStateOf(if (!showIcons) 0 else when (currentMode) {
            0 -> 1
            4 -> 2
            5 -> 3
            6 -> 4
            2 -> 5
            3 -> {
                val packIdx = iconPacks.indexOfFirst { it.first == currentIconPack }
                if (packIdx >= 0) 6 + packIdx else 1
            }
            else -> 1
        })
    }

    val spacing = 12.dp.scaled(screenScale)
    val minCardWidth = 70.dp.scaled(screenScale)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val visibleCount = ((maxWidth + spacing) / (minCardWidth + spacing)).toInt().coerceIn(3, 5).coerceAtMost(options.size)
        val totalSpacing = spacing * (visibleCount - 1)
        val cardWidth = (maxWidth - totalSpacing) / visibleCount
        val pageCount = ((options.size + visibleCount - 1) / visibleCount).coerceAtLeast(1)
        val initialPage = (selectedIdx / visibleCount).coerceIn(0, pageCount - 1)
        var currentPage by remember(pageCount) { mutableIntStateOf(initialPage) }
        Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp.scaled(screenScale)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ICON STYLE",
                style = SettingsTheme.typography.header,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            if (pageCount > 1) {
                PageIndicator(currentPage = currentPage, pageCount = pageCount)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp.scaled(screenScale))
                .pointerInput(pageCount) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragEnd = {
                            val threshold = 50f
                            if (totalDrag < -threshold && currentPage < pageCount - 1) currentPage++
                            else if (totalDrag > threshold && currentPage > 0) currentPage--
                        }
                    ) { _, dragAmount -> totalDrag += dragAmount }
                }
        ) {
            val start = currentPage * visibleCount
            val end = minOf(start + visibleCount, options.size)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (index in start until end) {
                    val option = options[index]
                    val isSelected = index == selectedIdx
                    val cardInteraction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .focusHalo(cardInteraction, cardShape)
                        .clip(cardShape)
                    .border(1.5.dp.scaled(screenScale), textColor, cardShape)
                    .background(if (isSelected) textColor else Color.Transparent)
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null
                    ) {
                        selectedIdx = index
                        onSelect(option.mode, option.packPkg)
                    }
                    .padding(12.dp.scaled(screenScale)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val itemFg = if (isSelected) bgColor else textColor
                val itemBg = if (isSelected) textColor else bgColor
                val previewSize = 40.dp.scaled(screenScale)
                Box(
                    modifier = Modifier.size(previewSize),
                    contentAlignment = Alignment.Center
                ) {
                    when (option.mode) {
                        -1 -> {
                            // None: crossed-out icon
                            Box(
                                modifier = Modifier
                                    .size(previewSize)
                                    .border(1.5.dp.scaled(screenScale), itemFg, iconPreviewShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = stringResource(R.string.icon_none),
                                    tint = itemFg,
                                    modifier = Modifier.size(previewSize * 0.5f)
                                )
                            }
                        }
                        0 -> {
                            Box(
                                modifier = Modifier
                                    .size(previewSize)
                                    .border(1.5.dp.scaled(screenScale), itemFg, iconPreviewShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.icon_preview_text),
                                    style = SettingsTheme.typography.title,
                                    fontSize = with(LocalDensity.current) { (previewSize * 0.4f).toSp() },
                                    fontWeight = FontWeight.Bold,
                                    color = itemFg
                                )
                            }
                        }
                        2 -> {
                            // System: Android settings icon (full color)
                            androidBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = stringResource(R.string.icon_system),
                                    modifier = Modifier.size(previewSize).clip(iconPreviewShape)
                                )
                            }
                        }
                        4 -> {
                            Box(
                                modifier = Modifier
                                    .size(previewSize)
                                    .clip(iconPreviewShape)
                                    .background(itemBg)
                                    .border(1.5.dp.scaled(screenScale), itemFg, iconPreviewShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_foreground),
                                    contentDescription = "Tinted",
                                    tint = itemFg,
                                    modifier = Modifier.size(previewSize * 0.55f)
                                )
                            }
                        }
                        6 -> {
                            // Filled: tinted tile clipped to iconShape with glyph carved out as bgColor.
                            Box(
                                modifier = Modifier
                                    .size(previewSize)
                                    .clip(iconPreviewShape)
                                    .background(itemFg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_foreground),
                                    contentDescription = "Filled",
                                    tint = itemBg,
                                    modifier = Modifier.size(previewSize * 0.6f)
                                )
                            }
                        }
                        5 -> {
                            Box(
                                modifier = Modifier.size(previewSize),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_foreground),
                                    contentDescription = "Glyph",
                                    tint = itemFg,
                                    modifier = Modifier.size(previewSize * 0.8f)
                                )
                            }
                        }
                        3 -> {
                            // Icon pack: show the icon pack app's own icon
                            val packAppBitmap = remember(option.packPkg) {
                                option.packPkg?.let { pkg ->
                                    try {
                                        val drawable = context.packageManager.getApplicationIcon(pkg)
                                        IconUtility.drawableToBitmap(drawable, iconSizePx)
                                    } catch (_: Exception) { null }
                                }
                            }
                            packAppBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = option.label,
                                    modifier = Modifier.size(previewSize).clip(iconPreviewShape)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp.scaled(screenScale)))
                Text(
                    text = option.label,
                    style = SettingsTheme.typography.item,
                    fontSize = (15 * screenScale).sp,
                    color = itemFg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                )
            }
                }
            }
        }
        }
    }

    // Icon Shape picker — Minimal (mode 5) clips Type 3 output (composite + bg-context)
    // to this shape, so it's relevant for that mode too.
    if (onShapeSelect != null) {
        Spacer(Modifier.height(12.dp.scaled(screenScale)))
        SheetTitle("Icon Shape")
        var shapeIdx by remember(currentIconShape) { mutableIntStateOf(currentIconShape) }
        val shapeOptions = listOf(context.getString(R.string.option_pill) to RoundedCornerShape(50), context.getString(R.string.option_rounded) to RoundedCornerShape(4.dp.scaled(screenScale)), context.getString(R.string.option_square) to RoundedCornerShape(0.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            shapeOptions.forEachIndexed { index, (label, shape) ->
                val isShapeSelected = index == shapeIdx
                val shapeFg = if (isShapeSelected) bgColor else textColor
                val shapeInteraction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .focusHalo(shapeInteraction, cardShape)
                        .clip(cardShape)
                        .border(1.5.dp.scaled(screenScale), textColor, cardShape)
                        .background(if (isShapeSelected) textColor else Color.Transparent)
                        .clickable(
                            interactionSource = shapeInteraction,
                            indication = null
                        ) {
                            shapeIdx = index
                            liveIconShape = index
                            onShapeSelect(index)
                        }
                        .padding(horizontal = 10.dp.scaled(screenScale), vertical = 12.dp.scaled(screenScale)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp.scaled(screenScale))
                            .border(1.5.dp.scaled(screenScale), shapeFg, shape),
                    )
                    Spacer(Modifier.width(8.dp.scaled(screenScale)))
                    Text(
                        text = label,
                        style = SettingsTheme.typography.item,
                        fontSize = (15 * screenScale).sp,
                        color = shapeFg,
                        maxLines = 1
                    )
                }
            }
        }
    }

}

@Composable
fun ShortcutIconPickerSheet(
    currentIcon: Constants.ShortcutIcon,
    onSelect: (Constants.ShortcutIcon) -> Unit
) {
    val screenScale = rememberScreenScale()
    val context = LocalContext.current
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val islandsShape = remember { Prefs(context).textIslandsShape }
    val cardShape = remember(islandsShape) { ShapeHelper.getRoundedCornerShape(islandsShape, pillRadius = 12.dp, roundedRadius = 6.dp) }

    val icons = Constants.ShortcutIcon.entries
    var selectedIdx by remember { mutableIntStateOf(icons.indexOf(currentIcon).coerceAtLeast(0)) }

    SheetTitle("Shortcut Icon")
    val spacing = 12.dp.scaled(screenScale)
    val minCardWidth = 70.dp.scaled(screenScale)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val visibleCount = ((maxWidth + spacing) / (minCardWidth + spacing)).toInt().coerceIn(3, 6).coerceAtMost(icons.size)
        val totalSpacing = spacing * (visibleCount - 1)
        val cardWidth = (maxWidth - totalSpacing) / visibleCount
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(vertical = 4.dp.scaled(screenScale))
        ) {
            itemsIndexed(icons.toList()) { index, icon ->
                val isSelected = index == selectedIdx
                val fg = if (isSelected) bgColor else textColor
                val cardInteraction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .focusHalo(cardInteraction, cardShape)
                        .clip(cardShape)
                        .border(1.5.dp.scaled(screenScale), textColor, cardShape)
                        .background(if (isSelected) textColor else Color.Transparent)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null
                        ) {
                            selectedIdx = index
                            onSelect(icon)
                        }
                        .padding(12.dp.scaled(screenScale)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (icon == Constants.ShortcutIcon.Disabled) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = stringResource(R.string.disabled),
                            tint = fg,
                            modifier = Modifier.size(28.dp.scaled(screenScale))
                        )
                    } else {
                        Icon(
                            imageVector = icon.toImageVector(),
                            contentDescription = icon.name,
                            tint = fg,
                            modifier = Modifier.size(28.dp.scaled(screenScale))
                        )
                    }
                    Spacer(Modifier.height(6.dp.scaled(screenScale)))
                    Text(
                        text = icon.name,
                        style = SettingsTheme.typography.item,
                        color = fg,
                        fontSize = (11 * screenScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

}



@Composable
fun SheetTitle(text: String) {
    val screenScale = rememberScreenScale()
    Text(
        text = text.uppercase(),
        style = SettingsTheme.typography.header,
        fontWeight = FontWeight.Bold,
        color = Theme.colors.text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp.scaled(screenScale)),
        textAlign = TextAlign.Start
    )
}

@Composable
fun Modifier.focusHalo(
    interactionSource: MutableInteractionSource,
    shape: androidx.compose.ui.graphics.Shape
): Modifier {
    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val isFocused = interactionSource.collectIsFocusedAsState().value
    return if (isFocused) this.background(highlightColor, shape) else this
}

@Composable
fun SheetTabRow(
    selectedIndex: Int,
    tabs: List<@Composable () -> Unit>,
    onTabSelected: (Int) -> Unit
) {
    val screenScale = rememberScreenScale()
    val context = LocalContext.current
    val textIslandsShape = remember { Prefs(context).textIslandsShape }
    val tabShape = remember { ShapeHelper.getRoundedCornerShape(textIslandsShape, pillRadius = 50.dp, roundedRadius = 4.dp) }
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp.scaled(screenScale), vertical = 4.dp.scaled(screenScale)),
        horizontalArrangement = Arrangement.spacedBy(6.dp.scaled(screenScale))
    ) {
        tabs.forEachIndexed { index, content ->
            val selected = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp.scaled(screenScale))
                    .focusHalo(interactionSource, tabShape)
                    .border(1.5.dp.scaled(screenScale), textColor, tabShape)
                    .background(if (selected) textColor else Color.Transparent, tabShape)
                    .clip(tabShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides if (selected) bgColor else textColor) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SearchFoldersSheet(
    context: Context,
    onAddFolder: () -> Unit,
    onRemoveFolder: (Uri) -> Unit,
    onFolderCountChanged: (Int) -> Unit,
    @Suppress("UNUSED_PARAMETER") onDismiss: () -> Unit
) {
    var folders by remember {
        mutableStateOf(com.github.gezimos.inkos.helper.FileSearchHelper.getPersistedFolders(context))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp.scaled(rememberScreenScale()), vertical = 16.dp)
    ) {
        Text(
            "Search Folders",
            style = SettingsTheme.typography.title,
            color = SettingsTheme.typography.title.color
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (folders.isEmpty()) {
            Text(
                stringResource(R.string.no_folders_selected),
                style = SettingsTheme.typography.title,
                color = SettingsTheme.typography.title.color.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        for ((uri, name) in folders) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    name,
                    style = SettingsTheme.typography.title,
                    color = SettingsTheme.typography.title.color,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.size(32.dp.scaled(rememberScreenScale())).clickable {
                        onRemoveFolder(uri)
                        folders = com.github.gezimos.inkos.helper.FileSearchHelper.getPersistedFolders(context)
                        onFolderCountChanged(folders.size)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cd_remove),
                        tint = SettingsTheme.typography.title.color
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "+ Add folder",
            style = SettingsTheme.typography.title,
            color = SettingsTheme.typography.title.color,
            modifier = Modifier
                .clickable {
                    onFolderCountChanged(folders.size)
                    onAddFolder()
                }
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
fun ThemeConfigSheet(
    applyColors: MutableState<Boolean>,
    applyFont: MutableState<Boolean>,
    applyIcons: MutableState<Boolean>,
    applyLayout: MutableState<Boolean>,
    applyWallpaper: MutableState<Boolean> = remember { mutableStateOf(true) }
) {
    val screenScale = rememberScreenScale()
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val context = LocalContext.current
    val islandsShape = remember { Prefs(context).textIslandsShape }
    val cardShape = remember(islandsShape) { ShapeHelper.getRoundedCornerShape(islandsShape, pillRadius = 12.dp, roundedRadius = 6.dp) }

    data class ConfigItem(val label: String, val state: MutableState<Boolean>)
    val items = listOf(
        ConfigItem(context.getString(R.string.config_colors), applyColors),
        ConfigItem(context.getString(R.string.config_font), applyFont),
        ConfigItem(context.getString(R.string.config_icons), applyIcons),
        ConfigItem(context.getString(R.string.config_layout), applyLayout),
        ConfigItem(context.getString(R.string.config_wallpaper), applyWallpaper)
    )

    val spacing = 12.dp.scaled(screenScale)
    val minCardWidth = 70.dp.scaled(screenScale)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val visibleCount = ((maxWidth + spacing) / (minCardWidth + spacing)).toInt().coerceIn(3, 6).coerceAtMost(items.size)
        val totalSpacing = spacing * (visibleCount - 1)
        val cardWidth = (maxWidth - totalSpacing) / visibleCount
        val pageCount = ((items.size + visibleCount - 1) / visibleCount).coerceAtLeast(1)
        var currentPage by remember(pageCount) { mutableIntStateOf(0) }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp.scaled(screenScale)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "THEME CONFIG",
                    style = SettingsTheme.typography.header,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                if (pageCount > 1) {
                    PageIndicator(currentPage = currentPage, pageCount = pageCount)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp.scaled(screenScale))
                    .pointerInput(pageCount) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                val threshold = 50f
                                if (totalDrag < -threshold && currentPage < pageCount - 1) currentPage++
                                else if (totalDrag > threshold && currentPage > 0) currentPage--
                            }
                        ) { _, dragAmount -> totalDrag += dragAmount }
                    }
            ) {
                val start = currentPage * visibleCount
                val end = minOf(start + visibleCount, items.size)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    for (index in start until end) {
                        val item = items[index]
                        val isActive = item.state.value
                        val fg = if (isActive) bgColor else textColor
                        Column(
                            modifier = Modifier
                                .width(cardWidth)
                                .clip(cardShape)
                                .border(1.5.dp.scaled(screenScale), textColor, cardShape)
                                .background(if (isActive) textColor else Color.Transparent)
                                .clickable { item.state.value = !item.state.value }
                                .padding(12.dp.scaled(screenScale)),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.height(28.dp.scaled(screenScale)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Rounded.Check else Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(22.dp.scaled(screenScale))
                                )
                            }
                            Spacer(Modifier.height(6.dp.scaled(screenScale)))
                            Text(
                                text = item.label,
                                style = SettingsTheme.typography.item,
                                color = fg,
                                fontSize = (11 * screenScale).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp.scaled(screenScale)))
    Text(
        text = stringResource(R.string.theme_config_hint),
        style = SettingsTheme.typography.item,
        color = textColor.copy(alpha = 0.5f),
        fontSize = (11 * screenScale).sp
    )
}

@Composable
fun BadgeIndicatorPickerSheet(
    currentIndicator: Constants.NotificationIndicator,
    onSelect: (Constants.NotificationIndicator) -> Unit
) {
    val screenScale = rememberScreenScale()
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val context = LocalContext.current
    val islandsShape = remember { Prefs(context).textIslandsShape }
    val cardShape = remember(islandsShape) { ShapeHelper.getRoundedCornerShape(islandsShape, pillRadius = 12.dp, roundedRadius = 6.dp) }

    val indicators = Constants.NotificationIndicator.entries
    var selectedIdx by remember { mutableIntStateOf(indicators.indexOf(currentIndicator).coerceAtLeast(0)) }

    SheetTitle("Badge Indicator")
    val spacing = 12.dp.scaled(screenScale)
    val minCardWidth = 70.dp.scaled(screenScale)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val visibleCount = ((maxWidth + spacing) / (minCardWidth + spacing)).toInt().coerceIn(3, 6).coerceAtMost(indicators.size)
        val totalSpacing = spacing * (visibleCount - 1)
        val cardWidth = (maxWidth - totalSpacing) / visibleCount
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(vertical = 4.dp.scaled(screenScale))
        ) {
            itemsIndexed(indicators.toList()) { index, indicator ->
                val isSelected = index == selectedIdx
                val fg = if (isSelected) bgColor else textColor
                val cardInteraction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .width(cardWidth)
                        .focusHalo(cardInteraction, cardShape)
                        .clip(cardShape)
                        .border(1.5.dp.scaled(screenScale), textColor, cardShape)
                        .background(if (isSelected) textColor else Color.Transparent)
                        .clickable(
                            interactionSource = cardInteraction,
                            indication = null
                        ) {
                            selectedIdx = index
                            onSelect(indicator)
                        }
                        .padding(12.dp.scaled(screenScale)),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.height(28.dp.scaled(screenScale)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = indicator.symbol,
                            color = fg,
                            fontSize = (22 * screenScale).sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(6.dp.scaled(screenScale)))
                    Text(
                        text = indicator.label,
                        style = SettingsTheme.typography.item,
                        color = fg,
                        fontSize = (11 * screenScale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
