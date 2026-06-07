package com.github.gezimos.inkos.ui.compose

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.R
import androidx.compose.ui.res.stringResource
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.helper.WallpaperUtility
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpaperUI(
    onBackClick: () -> Unit,
    showSheet: (@Composable () -> Unit) -> Unit,
    dismissSheet: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    isDark: Boolean = false,
    showStatusBar: Boolean = false,
    onExternalWallpaperClick: () -> Unit = {},
    onWallpaperSet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val wallpaperUtility = remember { WallpaperUtility(context) }
    val titleFontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize

    var isLoading by remember { mutableStateOf(false) }
    var showSetWallpaperScreen by remember { mutableStateOf(false) }
    var setWallpaperSheetTrigger by remember { mutableIntStateOf(0) }
    var currentImageResourceId by remember { mutableStateOf<Int?>(null) }
    var currentImageUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()

    val presets = WallpaperUtility.PRESET_WALLPAPERS
    var selectedIndex by remember { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, presets.lastIndex)
    val selectedPreset = presets[safeIndex]

    val screenWidthDp = context.resources.configuration.screenWidthDp
    val thumbsPerPage = if (screenWidthDp < 360) 4 else 6
    val thumbPages = remember(presets, thumbsPerPage) { presets.chunked(thumbsPerPage) }
    // Current thumb page follows selected index
    val currentThumbPage = safeIndex / thumbsPerPage

    fun setAndroidWallpaper(flags: Int, resourceId: Int?, uri: Uri?) {
        scope.launch {
            try {
                isLoading = true
                val success = withContext(Dispatchers.IO) {
                    when {
                        uri != null -> wallpaperUtility.setWallpaperFromUri(uri, flags)
                        resourceId != null -> wallpaperUtility.setWallpaperFromResource(resourceId, flags)
                        else -> false
                    }
                }
                isLoading = false
                if (success) onWallpaperSet?.invoke() ?: onBackClick()
            } catch (_: Exception) { isLoading = false }
        }
    }

    fun setInkOSWallpaper(resourceId: Int?, uri: Uri?) {
        scope.launch {
            try {
                isLoading = true
                val bitmap = withContext(Dispatchers.IO) {
                    when {
                        uri != null -> wallpaperUtility.loadBitmapFromUri(uri)
                        resourceId != null -> wallpaperUtility.loadBitmapFromResource(resourceId)
                        else -> null
                    }
                }
                if (bitmap != null) {
                    withContext(Dispatchers.IO) {
                        val fitted = wallpaperUtility.createFittedBitmap(bitmap, wallpaperUtility.screenWidth, wallpaperUtility.screenHeight)
                        val path = wallpaperUtility.saveBitmapToInternalStorage(fitted, "inkos_wallpaper.png")
                        Prefs(context).apply {
                            inkosWallpaperPath = path
                            inkosWallpaperResourceId = resourceId ?: 0
                            if (backgroundOpacity == 255) backgroundOpacity = 0
                        }
                        if (fitted != bitmap) fitted.recycle()
                        bitmap.recycle()
                    }
                }
                isLoading = false
                onWallpaperSet?.invoke() ?: onBackClick()
            } catch (_: Exception) { isLoading = false }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            currentImageResourceId = null
            currentImageUri = uri
            showSetWallpaperScreen = true
            setWallpaperSheetTrigger++
        }
    }

    fun triggerSet(preset: WallpaperUtility.PresetWallpaper) {
        if (preset.isGenerated) {
            showSheet {
                com.github.gezimos.inkos.ui.dialogs.SheetTitle(preset.name)
                AlignmentPickerRow(
                    variants = preset.variants!!,
                    onSelect = { variantId ->
                        dismissSheet()
                        currentImageResourceId = variantId
                        currentImageUri = null
                        showSetWallpaperScreen = true
                        setWallpaperSheetTrigger++
                    }
                )
            }
        } else {
            currentImageResourceId = preset.resourceId
            currentImageUri = null
            showSetWallpaperScreen = true
            setWallpaperSheetTrigger++
        }
    }

    SettingsTheme(isDark) {
        val screenScale = rememberScreenScale()
        val textColor = Theme.colors.text
        val bgColor = Theme.colors.background
        val cardShape = remember(prefs.textIslandsShape) { ShapeHelper.getRoundedCornerShape(prefs.textIslandsShape, pillRadius = 12.dp) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> { if (selectedIndex > 0) selectedIndex--; true }
                        Key.DirectionRight -> { if (selectedIndex < presets.lastIndex) selectedIndex++; true }
                        Key.DirectionCenter, Key.Enter -> { triggerSet(selectedPreset); true }
                        else -> false
                    }
                }
        ) {
            // Header
            SettingsComposable.PageHeader(
                iconRes = R.drawable.ic_back,
                title = selectedPreset.name.uppercase(),
                onClick = onBackClick,
                showStatusBar = showStatusBar,
                titleFontSize = titleFontSize,
                pageIndicator = {
                    val btnShape = remember { ShapeHelper.getRoundedCornerShape(prefs.textIslandsShape, pillRadius = 50.dp) }
                    val btnFontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.2f).sp else fontSize
                    val browseInteraction = remember { MutableInteractionSource() }
                    val browseFocused = browseInteraction.collectIsFocusedAsState().value
                    Text(
                        text = stringResource(R.string.browse),
                        style = SettingsTheme.typography.title,
                        fontSize = btnFontSize,
                        color = if (browseFocused) bgColor else textColor,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(btnShape)
                            .background(if (browseFocused) textColor else androidx.compose.ui.graphics.Color.Transparent)
                            .border(2.dp, textColor, btnShape)
                            .clickable(
                                interactionSource = browseInteraction,
                                indication = null
                            ) {
                                val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                                imagePickerLauncher.launch(intent)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            )

            var previewDragX by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp.scaled(screenScale))
                    .padding(top = 12.dp.scaled(screenScale), bottom = 12.dp.scaled(screenScale))
                    .clip(cardShape)
                    .border(1.5.dp.scaled(screenScale), textColor, cardShape)
                    .pointerInput(presets.size) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount -> previewDragX += dragAmount },
                            onDragEnd = {
                                val threshold = 48f
                                if (previewDragX > threshold && selectedIndex > 0) {
                                    selectedIndex--
                                } else if (previewDragX < -threshold && selectedIndex < presets.lastIndex) {
                                    selectedIndex++
                                }
                                previewDragX = 0f
                            },
                            onDragCancel = { previewDragX = 0f }
                        )
                    }
            ) {
                WallpaperPreviewContent(selectedPreset.resourceId, bgColor, textColor, Modifier.fillMaxSize())
            }

            // Paged thumbnail strip with swipe
            val pageThumbs = thumbPages.getOrNull(currentThumbPage) ?: emptyList()
            var thumbDragX by remember { mutableStateOf(0f) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp.scaled(screenScale))
                    .padding(bottom = 12.dp.scaled(screenScale))
                    .pointerInput(thumbPages.size, currentThumbPage) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount -> thumbDragX += dragAmount },
                            onDragEnd = {
                                val threshold = 48f
                                if (thumbDragX > threshold && currentThumbPage > 0) {
                                    selectedIndex = ((currentThumbPage - 1) * thumbsPerPage).coerceIn(0, presets.lastIndex)
                                } else if (thumbDragX < -threshold && currentThumbPage < thumbPages.lastIndex) {
                                    selectedIndex = ((currentThumbPage + 1) * thumbsPerPage).coerceIn(0, presets.lastIndex)
                                }
                                thumbDragX = 0f
                            },
                            onDragCancel = { thumbDragX = 0f }
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled(screenScale))
            ) {
                pageThumbs.forEachIndexed { _, preset ->
                    val globalIndex = presets.indexOf(preset)
                    val isSelected = globalIndex == safeIndex
                    val interactionSource = remember(globalIndex) { MutableInteractionSource() }
                    val isFocused = interactionSource.collectIsFocusedAsState().value
                    val highlighted = isSelected || isFocused

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(cardShape)
                            .background(if (highlighted) textColor else bgColor)
                            .border(
                                if (highlighted) 3.dp.scaled(screenScale) else 1.5.dp.scaled(screenScale),
                                textColor,
                                cardShape
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { selectedIndex = globalIndex }
                    ) {
                        Box(Modifier.fillMaxSize().clip(cardShape)) {
                            WallpaperPreviewContent(preset.resourceId, bgColor, textColor, Modifier.fillMaxSize())
                        }
                    }
                }
                // Fill empty slots on last page
                val empty = thumbsPerPage - pageThumbs.size
                repeat(empty) { Spacer(Modifier.weight(1f)) }
            }

            // Separator + SET button with chevrons
            Box(Modifier.fillMaxWidth().height(1.5.dp.scaled(screenScale)).background(textColor))
            val setInteraction = remember { MutableInteractionSource() }
            val setFocused = setInteraction.collectIsFocusedAsState().value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp.scaled(screenScale))
                    .background(if (setFocused) textColor else bgColor)
                    .clickable(interactionSource = setInteraction, indication = null) { triggerSet(selectedPreset) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp.scaled(screenScale)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous",
                        tint = if (setFocused) bgColor else textColor,
                        modifier = Modifier
                            .size(24.dp.scaled(screenScale))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                if (selectedIndex > 0) selectedIndex--
                            }
                    )
                    Text(
                        stringResource(R.string.wallpaper_set),
                        style = SettingsTheme.typography.title,
                        fontSize = titleFontSize,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (setFocused) bgColor else textColor
                    )
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next",
                        tint = if (setFocused) bgColor else textColor,
                        modifier = Modifier
                            .size(24.dp.scaled(screenScale))
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                if (selectedIndex < presets.lastIndex) selectedIndex++
                            }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.toast_please_wait), style = SettingsTheme.typography.body, fontSize = titleFontSize, color = textColor)
                }
            }
        }
    }

    // Set wallpaper bottom sheet
    LaunchedEffect(showSetWallpaperScreen, setWallpaperSheetTrigger) {
        if (showSetWallpaperScreen) {
            showSheet {
                com.github.gezimos.inkos.ui.dialogs.SetWallpaperSheet(
                    onSetForHome = {
                        val rid = currentImageResourceId; val uri = currentImageUri; showSetWallpaperScreen = false
                        Prefs(context).apply { inkosWallpaperPath = null; if (backgroundOpacity == 255) backgroundOpacity = 0 }
                        setAndroidWallpaper(android.app.WallpaperManager.FLAG_SYSTEM, rid, uri)
                    },
                    onSetForLockScreen = {
                        val rid = currentImageResourceId; val uri = currentImageUri; showSetWallpaperScreen = false
                        Prefs(context).apply { inkosWallpaperPath = null; if (backgroundOpacity == 255) backgroundOpacity = 0 }
                        setAndroidWallpaper(android.app.WallpaperManager.FLAG_LOCK, rid, uri)
                    },
                    onSetForBoth = {
                        val rid = currentImageResourceId; val uri = currentImageUri; showSetWallpaperScreen = false
                        Prefs(context).apply { inkosWallpaperPath = null; if (backgroundOpacity == 255) backgroundOpacity = 0 }
                        setAndroidWallpaper(android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK, rid, uri)
                    },
                    onSetInkOSNoCrop = {
                        val rid = currentImageResourceId; val uri = currentImageUri; showSetWallpaperScreen = false
                        setInkOSWallpaper(rid, uri)
                    },
                    onDismiss = { dismissSheet(); showSetWallpaperScreen = false },
                    onShowInkosInfo = {
                        dismissSheet()
                        showSheet { com.github.gezimos.inkos.ui.dialogs.InkosWallpaperInfoSheet(onDismiss = { dismissSheet(); setWallpaperSheetTrigger++ }) }
                    }
                )
            }
        }
    }
}

@Composable
private fun WallpaperPreviewContent(
    resourceId: Int,
    bgColor: androidx.compose.ui.graphics.Color,
    fgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    when {
        resourceId == -1 -> Box(modifier.background(androidx.compose.ui.graphics.Color.White))
        resourceId == -2 -> Box(modifier.background(androidx.compose.ui.graphics.Color.Black))
        resourceId <= -3 -> GeneratedPatternPreview(resourceId, bgColor, fgColor, modifier)
        else -> {
            val fgR = fgColor.red; val fgG = fgColor.green; val fgB = fgColor.blue
            val bgR = bgColor.red; val bgG = bgColor.green; val bgB = bgColor.blue
            val colorMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                (bgR - fgR) * 0.299f, (bgR - fgR) * 0.587f, (bgR - fgR) * 0.114f, 0f, fgR * 255f,
                (bgG - fgG) * 0.299f, (bgG - fgG) * 0.587f, (bgG - fgG) * 0.114f, 0f, fgG * 255f,
                (bgB - fgB) * 0.299f, (bgB - fgB) * 0.587f, (bgB - fgB) * 0.114f, 0f, fgB * 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            Image(
                painterResource(resourceId), null, modifier,
                contentScale = ContentScale.Crop,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(colorMatrix)
            )
        }
    }
}

@Composable
private fun AlignmentPickerRow(variants: IntArray, onSelect: (Int) -> Unit) {
    val screenScale = rememberScreenScale()
    val context = LocalContext.current
    val shape = remember { ShapeHelper.getRoundedCornerShape(Prefs(context).textIslandsShape, pillRadius = 12.dp) }
    val alignments = if (variants.size == 2) listOf(0, 2) else listOf(0, 1, 2)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))) {
        alignments.forEachIndexed { idx, alignment ->
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused = interactionSource.collectIsFocusedAsState().value
            Box(
                modifier = Modifier.weight(1f).clip(shape)
                    .background(if (isFocused) Theme.colors.text else Theme.colors.background)
                    .then(if (!isFocused) Modifier.border(1.5.dp.scaled(screenScale), Theme.colors.text, shape) else Modifier)
                    .clickable(interactionSource = interactionSource, indication = null) { onSelect(variants[idx]) }
                    .padding(vertical = 20.dp.scaled(screenScale)),
                contentAlignment = Alignment.Center
            ) {
                with(SettingsComposable) { AlignmentIcon(alignment = alignment, tint = if (isFocused) Theme.colors.background else Theme.colors.text, size = 24.dp) }
            }
        }
    }
}

@Composable
private fun GeneratedPatternPreview(variant: Int, bgColor: androidx.compose.ui.graphics.Color, fgColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        drawRect(bgColor)
        val w = size.width; val h = size.height
        fun fadeAlpha(fraction: Float, fadeType: Int): Float { val raw = when (fadeType) { 0 -> 1f - fraction; 1 -> fraction; 2 -> kotlin.math.abs(fraction - 0.5f) * 2f; else -> 1f }; return raw * raw }
        val fadeType = when (variant) {
            WallpaperUtility.DOTS_LEFT, WallpaperUtility.LINES_LEFT, WallpaperUtility.GRID_LEFT, WallpaperUtility.DIAG_LEFT, WallpaperUtility.CROSS_LEFT, WallpaperUtility.WAVE_LEFT, WallpaperUtility.PLUS_LEFT, WallpaperUtility.DASH_LEFT, WallpaperUtility.SCATTER_LEFT -> 0
            WallpaperUtility.DOTS_RIGHT, WallpaperUtility.LINES_RIGHT, WallpaperUtility.GRID_RIGHT, WallpaperUtility.DIAG_RIGHT, WallpaperUtility.CROSS_RIGHT, WallpaperUtility.WAVE_RIGHT, WallpaperUtility.PLUS_RIGHT, WallpaperUtility.DASH_RIGHT, WallpaperUtility.SCATTER_RIGHT -> 1
            WallpaperUtility.DOTS_CENTER, WallpaperUtility.LINES_CENTER, WallpaperUtility.GRID_CENTER, WallpaperUtility.DIAG_CENTER, WallpaperUtility.CROSS_CENTER, WallpaperUtility.WAVE_CENTER, WallpaperUtility.PLUS_CENTER, WallpaperUtility.DASH_CENTER, WallpaperUtility.SCATTER_CENTER -> 2
            else -> 0
        }
        val sw = 1.dp.toPx()
        when (variant) {
            WallpaperUtility.DOTS_LEFT, WallpaperUtility.DOTS_RIGHT, WallpaperUtility.DOTS_CENTER -> {
                val sp = 16.dp.toPx(); val r = 1.5.dp.toPx()
                var y = sp / 2; while (y < h) { var x = sp / 2; while (x < w) { val a = fadeAlpha(x / w, fadeType); if (a > 0.02f) drawCircle(fgColor.copy(alpha = a), r, androidx.compose.ui.geometry.Offset(x, y)); x += sp }; y += sp }
            }
            WallpaperUtility.LINES_LEFT, WallpaperUtility.LINES_RIGHT, WallpaperUtility.LINES_CENTER -> {
                val sp = 12.dp.toPx(); val seg = 6.dp.toPx()
                var y = sp; while (y < h) { var x = 0f; while (x < w) { val a = fadeAlpha((x + seg / 2) / w, fadeType); if (a > 0.02f) drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset((x + seg).coerceAtMost(w), y), sw); x += seg }; y += sp }
            }
            WallpaperUtility.GRID_LEFT, WallpaperUtility.GRID_RIGHT, WallpaperUtility.GRID_CENTER -> {
                val t = 16.dp.toPx(); val cols = kotlin.math.round(w / t).toInt().coerceAtLeast(1); val rows = kotlin.math.round(h / t).toInt().coerceAtLeast(1); val spX = w / cols; val spY = h / rows; val seg = 6.dp.toPx()
                for (i in 0..rows) { val y = i * spY; var x = 0f; while (x < w) { val a = fadeAlpha((x + seg / 2) / w, fadeType); if (a > 0.02f) drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Offset((x + seg).coerceAtMost(w), y), sw); x += seg } }
                for (i in 0..cols) { val x = i * spX; val a = fadeAlpha((x / w).coerceIn(0f, 1f), fadeType); if (a > 0.02f) drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, h), sw) }
            }
            WallpaperUtility.DIAG_LEFT, WallpaperUtility.DIAG_RIGHT, WallpaperUtility.DIAG_CENTER -> {
                val sp = 12.dp.toPx(); var off = -h; while (off < w + h) { val steps = 30; val dy = h / steps; for (i in 0 until steps) { val sy = i * dy; val ey = (i + 1) * dy; val sx = off + sy; val ex = off + ey; val mx = ((sx + ex) / 2).coerceIn(0f, w); val a = fadeAlpha(mx / w, fadeType); if (a > 0.02f) drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(sx, sy), androidx.compose.ui.geometry.Offset(ex, ey), sw) }; off += sp }
            }
            WallpaperUtility.CIRCLES_BL, WallpaperUtility.CIRCLES_BR -> {
                val sp = 18.dp.toPx(); val cx = if (variant == WallpaperUtility.CIRCLES_BL) 0f else w; val cy = h; val maxR = kotlin.math.sqrt(w * w + h * h)
                var r = sp; while (r < maxR) { val a = (1f - r / maxR).let { it * it }; if (a > 0.02f) drawCircle(fgColor.copy(alpha = a), r, androidx.compose.ui.geometry.Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(sw)); r += sp }
            }
            WallpaperUtility.CROSS_LEFT, WallpaperUtility.CROSS_RIGHT, WallpaperUtility.CROSS_CENTER -> {
                val sp = 14.dp.toPx()
                for (dir in 0..1) { var off = -h; while (off < w + h) { val steps = 30; val dy = h / steps; for (i in 0 until steps) { val sy = i * dy; val ey = (i + 1) * dy; val sx: Float; val ex: Float; if (dir == 0) { sx = off + sy; ex = off + ey } else { sx = w - off - sy; ex = w - off - ey }; val mx = ((sx + ex) / 2).coerceIn(0f, w); val a = fadeAlpha(mx / w, fadeType); if (a > 0.02f) drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(sx, sy), androidx.compose.ui.geometry.Offset(ex, ey), sw) }; off += sp } }
            }
            WallpaperUtility.WAVE_LEFT, WallpaperUtility.WAVE_RIGHT, WallpaperUtility.WAVE_CENTER -> {
                val sp = 10.dp.toPx(); val amp = 3.dp.toPx(); val step = 1.dp.toPx()
                var baseY = sp / 2; while (baseY < h + sp) {
                    var x = step; var px = 0f; var py = baseY
                    while (x < w) { val sy = baseY + amp * kotlin.math.sin((x / w) * Math.PI.toFloat() * 8); val a = fadeAlpha(x / w, fadeType); if (a > 0.02f) drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(px, py), androidx.compose.ui.geometry.Offset(x, sy), sw); px = x; py = sy; x += step }
                    baseY += sp
                }
            }
            WallpaperUtility.PLUS_LEFT, WallpaperUtility.PLUS_RIGHT, WallpaperUtility.PLUS_CENTER -> {
                val sp = 14.dp.toPx(); val arm = 3.dp.toPx()
                var y = sp / 2; while (y < h) { var x = sp / 2; while (x < w) { val a = fadeAlpha(x / w, fadeType); if (a > 0.02f) { drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(x - arm, y), androidx.compose.ui.geometry.Offset(x + arm, y), sw); drawLine(fgColor.copy(alpha = a), androidx.compose.ui.geometry.Offset(x, y - arm), androidx.compose.ui.geometry.Offset(x, y + arm), sw) }; x += sp }; y += sp }
            }
            WallpaperUtility.DASH_LEFT, WallpaperUtility.DASH_RIGHT, WallpaperUtility.DASH_CENTER -> {
                // Binary preview
                val random = java.util.Random(99)
                val sizes = floatArrayOf(4f, 5f, 7f, 9f)
                val colCount = (w / 9.dp.toPx()).toInt().coerceAtLeast(1)
                val colW = w / colCount
                val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = (fgColor.value shr 32).toInt()
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val nc = drawContext.canvas.nativeCanvas
                for (col in 0 until colCount) {
                    val x = col * colW + colW * 0.3f
                    val charSz = sizes[random.nextInt(sizes.size)].dp.toPx()
                    val rowH = charSz * 1.3f
                    val colDen = 0.3f + random.nextFloat() * 0.5f
                    textPaint.textSize = charSz
                    val colOff = random.nextFloat() * rowH * 3
                    var y = colOff; while (y < h + rowH) {
                        if (random.nextFloat() < colDen) {
                            val a = fadeAlpha(x / w, fadeType) * (1f - (y / h) * 0.4f)
                            if (a > 0.02f) { textPaint.alpha = (a * 255).toInt().coerceIn(0, 255); nc.drawText(if (random.nextBoolean()) "1" else "0", x, y, textPaint) }
                        }
                        y += rowH
                    }
                }
            }
            WallpaperUtility.SCATTER_LEFT, WallpaperUtility.SCATTER_RIGHT, WallpaperUtility.SCATTER_CENTER -> {
                val colSp = 7.dp.toPx(); val rowSp = 7.dp.toPx(); val charSz = 5.dp.toPx()
                val random = java.util.Random(42)
                val chars = "0123456789ABCDEF"
                val argb = (fgColor.value shr 32).toInt()
                val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = argb
                    textSize = charSz
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                val nc = drawContext.canvas
                var x = colSp / 2; while (x < w) {
                    val colOff = random.nextInt(rowSp.toInt().coerceAtLeast(1)); val colDen = 0.4f + random.nextFloat() * 0.6f
                    var y = colOff.toFloat(); while (y < h) {
                        if (random.nextFloat() < colDen) {
                            val a = fadeAlpha(x / w, fadeType) * (1f - (y / h) * 0.5f)
                            if (a > 0.02f) {
                                textPaint.alpha = (a * 255).toInt().coerceIn(0, 255)
                                nc.nativeCanvas.drawText(chars[random.nextInt(chars.length)].toString(), x, y, textPaint)
                            }
                        }
                        y += rowSp
                    }
                    x += colSp
                }
            }
        }
    }
}
