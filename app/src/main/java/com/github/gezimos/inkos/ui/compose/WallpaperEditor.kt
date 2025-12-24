package com.github.gezimos.inkos.ui.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.helper.WallpaperDither
import com.github.gezimos.inkos.helper.WallpaperHalftone
import com.github.gezimos.inkos.helper.WallpaperUtility
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix

@Composable
fun WallpaperEditor(
    onBackClick: () -> Unit,
    onSelect: (Int, WallpaperEditorState) -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    isDark: Boolean = false,
    showStatusBar: Boolean = false,
    imageResourceId: Int? = null,
    imageUri: Uri? = null,
    onBrowseClick: (() -> Unit)? = null,
    onPresetsClick: (() -> Unit)? = null,
    onShowSetWallpaper: ((WallpaperEditorState) -> Unit)? = null
) {
    val context = LocalContext.current
    val wallpaperUtility = remember { WallpaperUtility(context) }
    val supportsLockScreen = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
    val prefs = remember { Prefs(context) }
    val textIslandsShape = prefs.textIslandsShape
    val titleFontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize
    val buttonFontSize = fontSize
    
    val buttonShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 50.dp
        )
    }
    
    // Editor state - "light" effects (applied via Compose graphics layer - instant)
    var flipHorizontal by remember { mutableStateOf(false) }
    var flipVertical by remember { mutableStateOf(false) }
    var brightness by remember { mutableIntStateOf(0) }
    var contrast by remember { mutableIntStateOf(0) }
    var isInverted by remember { mutableStateOf(false) }
    
    // Editor state - "heavy" effects (require bitmap processing)
    var halftoneIntensity by remember { mutableIntStateOf(0) }
    var halftoneDotSize by remember { mutableIntStateOf(50) }  // 0-100: controls dot/line size within cell
    var halftoneShape by remember { mutableStateOf(WallpaperHalftone.HalftoneShape.DOTS) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var overlaySide by remember { mutableStateOf("left") }
    var overlaySpread by remember { mutableIntStateOf(40) }
    var overlayFalloff by remember { mutableIntStateOf(60) }
    var thresholdLevel by remember { mutableIntStateOf(50) }  // 0-100: threshold for black/white conversion
    var ditherEnabled by remember { mutableStateOf(false) }
    var ditherAlgorithm by remember { mutableStateOf(WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG) }
    var effectMode by remember { mutableStateOf("brightness") }
    
    // Bitmap states
    // sourceBitmap: original loaded image, never modified
    // processedBitmap: has overlay/halftone applied (heavy effects)
    // brightness/contrast/flip are applied via Compose ColorFilter/graphicsLayer (instant)
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    // Generation counter to track processing jobs and prevent stale updates
    var processingGeneration by remember { mutableStateOf(0) }
    // State for showing image selection buttons overlay
    var showImageSelectionButtons by remember { mutableStateOf(false) }
    
    // Disable back gesture/button - user must use the explicit back button in the UI
    BackHandler(enabled = true) {
        // Do nothing - this prevents accidental edge swipes from going back
    }
    
    // Create color matrix for brightness/contrast/invert - applied instantly via ColorFilter
    val colorMatrix = remember(brightness, contrast, isInverted) {
        val brightnessValue = brightness / 100f * 255f
        val contrastScale = (contrast + 100) / 100f
        val contrastTranslate = (-.5f * contrastScale + .5f) * 255f
        
        // Combine brightness, contrast, and invert into a single matrix
        if (isInverted) {
            // Invert: R' = 255 - R, G' = 255 - G, B' = 255 - B
            // Combined with brightness/contrast: apply contrast/brightness first, then invert
            ComposeColorMatrix(floatArrayOf(
                -contrastScale, 0f, 0f, 0f, 255f - (brightnessValue + contrastTranslate),
                0f, -contrastScale, 0f, 0f, 255f - (brightnessValue + contrastTranslate),
                0f, 0f, -contrastScale, 0f, 255f - (brightnessValue + contrastTranslate),
                0f, 0f, 0f, 1f, 0f
            ))
        } else {
            ComposeColorMatrix(floatArrayOf(
                contrastScale, 0f, 0f, 0f, brightnessValue + contrastTranslate,
                0f, contrastScale, 0f, 0f, brightnessValue + contrastTranslate,
                0f, 0f, contrastScale, 0f, brightnessValue + contrastTranslate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
    }
    
    // Load source bitmap when image changes
    LaunchedEffect(imageResourceId, imageUri) {
        processedBitmap = null
        sourceBitmap = null
        isLoading = true
        processingGeneration++
        
        val oldBitmap = sourceBitmap
        oldBitmap?.let { old ->
            withContext(Dispatchers.IO) {
                if (!old.isRecycled) old.recycle()
            }
        }
        
        val loaded = withContext(Dispatchers.IO) {
            try {
                if (imageUri != null) {
                    context.contentResolver.openInputStream(imageUri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                } else if (imageResourceId != null) {
                    wallpaperUtility.loadBitmapFromResource(imageResourceId)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        sourceBitmap = loaded
        isLoading = false
    }
    
    // Process only heavy effects (threshold, dithering, overlay, halftone) - these require bitmap manipulation
    // Brightness/contrast/flip/invert are handled by Compose graphics layer (instant)
    LaunchedEffect(
        sourceBitmap,
        thresholdLevel,
        ditherEnabled,
        ditherAlgorithm,
        halftoneIntensity,
        halftoneDotSize,
        halftoneShape,
        overlayEnabled,
        overlaySide,
        overlaySpread,
        overlayFalloff
    ) {
        val source = sourceBitmap
        if (source == null || source.isRecycled) {
            processedBitmap = null
            isProcessing = false
            return@LaunchedEffect
        }
        
        // Increment generation counter for this processing job
        processingGeneration++
        val myGeneration = processingGeneration
        
        // Capture current parameter values to detect if they change during processing
        val currentThresholdLevel = thresholdLevel
        val currentDitherEnabled = ditherEnabled
        val currentDitherAlgorithm = ditherAlgorithm
        val currentHalftoneIntensity = halftoneIntensity
        val currentHalftoneDotSize = halftoneDotSize
        val currentHalftoneShape = halftoneShape
        val currentOverlayEnabled = overlayEnabled
        val currentOverlaySide = overlaySide
        val currentOverlaySpread = overlaySpread
        val currentOverlayFalloff = overlayFalloff
        
        // Check if any heavy effects are enabled
        val hasThreshold = currentThresholdLevel != 50  // 50 is default/neutral
        val hasDithering = currentDitherEnabled
        val hasHalftone = currentHalftoneIntensity > 0
        val hasOverlay = currentOverlayEnabled
        
        // If no heavy effects, just convert source to ImageBitmap
        if (!hasThreshold && !hasDithering && !hasHalftone && !hasOverlay) {
            // Only update if this is still the current generation
            if (processingGeneration == myGeneration) {
                processedBitmap = source.asImageBitmap()
                isProcessing = false
            }
            return@LaunchedEffect
        }
        
        // Only show loading indicator for slow operations (threshold, dithering, halftone)
        val needsProcessing = hasThreshold || hasDithering || hasHalftone
        if (needsProcessing && processingGeneration == myGeneration) {
            isProcessing = true
        }
        
        val result = withContext(Dispatchers.IO) {
            try {
                if (source.isRecycled) return@withContext null
                
                var bitmap = source.copy(Bitmap.Config.ARGB_8888, true) ?: return@withContext null
                
                // Processing order: threshold → dithering → halftone → overlay
                
                // Apply threshold (converts to grayscale then black/white)
                if (hasThreshold) {
                    val new = wallpaperUtility.applyThreshold(bitmap, currentThresholdLevel)
                    if (new !== bitmap) { bitmap.recycle(); bitmap = new }
                }
                
                // Apply dithering (works on grayscale, converts to black/white with error diffusion)
                if (hasDithering && currentDitherAlgorithm != WallpaperDither.DitherAlgorithm.NONE) {
                    val ditherUtil = WallpaperDither()
                    val new = ditherUtil.applyDithering(bitmap, currentDitherAlgorithm)
                    if (new !== bitmap) { bitmap.recycle(); bitmap = new }
                }
                
                // Apply halftone (slow, shows loading indicator)
                if (hasHalftone) {
                    val halftoneUtil = WallpaperHalftone(context)
                    val new = halftoneUtil.convertToHalftone(
                        bitmap, 
                        intensity = currentHalftoneIntensity, 
                        dotSize = currentHalftoneDotSize,
                        shape = currentHalftoneShape
                    )
                    if (new !== bitmap) { bitmap.recycle(); bitmap = new }
                }
                
                // Apply overlay (fast, no loading indicator needed)
                if (hasOverlay && currentOverlaySpread > 0) {
                    val backgroundColor = prefs.backgroundColor
                    val new = wallpaperUtility.addGradientOverlay(bitmap, backgroundColor, currentOverlaySide, currentOverlaySpread, currentOverlayFalloff)
                    if (new !== bitmap) { bitmap.recycle(); bitmap = new }
                }
                
                bitmap.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        
        if (result != null && 
            processingGeneration == myGeneration &&
            sourceBitmap === source &&
            currentThresholdLevel == thresholdLevel &&
            currentDitherEnabled == ditherEnabled &&
            currentDitherAlgorithm == ditherAlgorithm &&
            currentHalftoneIntensity == halftoneIntensity &&
            currentHalftoneDotSize == halftoneDotSize &&
            currentHalftoneShape == halftoneShape &&
            currentOverlayEnabled == overlayEnabled &&
            currentOverlaySide == overlaySide &&
            currentOverlaySpread == overlaySpread &&
            currentOverlayFalloff == overlayFalloff) {
            processedBitmap = result
        }
        
        if (processingGeneration == myGeneration) {
            isProcessing = false
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            sourceBitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }
    
    // Helper functions
    fun createEditorState() = WallpaperEditorState(
        flipHorizontal = flipHorizontal,
        flipVertical = flipVertical,
        brightness = brightness,
        contrast = contrast,
        isInverted = isInverted,
        halftoneIntensity = halftoneIntensity,
        halftoneDotSize = halftoneDotSize,
        halftoneShape = halftoneShape,
        overlayEnabled = overlayEnabled,
        overlaySide = overlaySide,
        overlaySpread = overlaySpread,
        overlayFalloff = overlayFalloff,
        thresholdLevel = thresholdLevel,
        ditherEnabled = ditherEnabled,
        ditherAlgorithm = ditherAlgorithm
    )
    
    fun resetAll() {
        halftoneIntensity = 0
        halftoneDotSize = 50
        halftoneShape = WallpaperHalftone.HalftoneShape.DOTS
        flipHorizontal = false
        flipVertical = false
        brightness = 0
        contrast = 0
        isInverted = false
        overlayEnabled = false
        overlaySide = "left"
        overlaySpread = 40
        overlayFalloff = 60
        thresholdLevel = 50
        ditherEnabled = false
        ditherAlgorithm = WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG
        // Don't reset effectMode - keep the last tool used
    }
    
    SettingsTheme(isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background)
        ) {
            // Header
            SettingsComposable.PageHeader(
                iconRes = R.drawable.ic_back,
                title = "Editor",
                onClick = onBackClick,
                showStatusBar = showStatusBar,
                titleFontSize = titleFontSize,
                pageIndicator = if (onShowSetWallpaper != null) {
                    {
                        Box(
                            modifier = Modifier
                                .clip(buttonShape)
                                .background(Theme.colors.background)
                                .border(2.dp, Theme.colors.text, buttonShape)
                                .clickable(onClick = { onShowSetWallpaper?.invoke(createEditorState()) })
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Set",
                                style = SettingsTheme.typography.title,
                                color = Theme.colors.text,
                                fontSize = titleFontSize
                            )
                        }
                    }
                } else null
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Preview box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(2.dp, Theme.colors.text, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Theme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    val displayBitmap = processedBitmap
                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap,
                            contentDescription = "Preview - Tap to change image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = onBrowseClick != null || onPresetsClick != null) {
                                    showImageSelectionButtons = !showImageSelectionButtons
                                }
                                .graphicsLayer {
                                    // Flip via scale - instant, no bitmap processing
                                    scaleX = if (flipHorizontal) -1f else 1f
                                    scaleY = if (flipVertical) -1f else 1f
                                },
                            contentScale = ContentScale.Crop,
                            // Brightness/contrast/invert via ColorFilter - instant, no bitmap processing
                            colorFilter = if (brightness != 0 || contrast != 0 || isInverted) {
                                ColorFilter.colorMatrix(colorMatrix)
                            } else null
                        )
                    }
                    
                    // Image selection buttons overlay
                    if (showImageSelectionButtons && (onBrowseClick != null || onPresetsClick != null)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Theme.colors.background.copy(alpha = 0.8f))
                                .clickable { showImageSelectionButtons = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (onPresetsClick != null) {
                                    ImageSelectionButton(
                                        text = "Presets",
                                        onClick = {
                                            showImageSelectionButtons = false
                                            onPresetsClick()
                                        },
                                        fontSize = titleFontSize,
                                        shape = buttonShape,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (onBrowseClick != null) {
                                    ImageSelectionButton(
                                        text = "Browse",
                                        onClick = {
                                            showImageSelectionButtons = false
                                            onBrowseClick()
                                        },
                                        fontSize = titleFontSize,
                                        shape = buttonShape,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Show loading icon when loading, processing, or no preview
                    if (isLoading || isProcessing || displayBitmap == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Theme.colors.background.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_foreground),
                                contentDescription = "Loading",
                                modifier = Modifier.size(48.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Theme.colors.text)
                            )
                        }
                    }
                    
                    // Invert button in bottom left corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        EffectButton(
                            isSelected = isInverted,
                            onClick = { isInverted = !isInverted },
                            icon = Icons.Default.InvertColors,
                            contentDescription = "Invert",
                            shape = buttonShape,
                            modifier = Modifier
                        )
                    }
                    
                    // Reset button in bottom right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        EffectButton(
                            isSelected = false,
                            onClick = { resetAll() },
                            icon = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            shape = buttonShape,
                            modifier = Modifier
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Effect buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EffectButton(effectMode == "brightness", { effectMode = "brightness" }, 
                            Icons.Default.Brightness6, "Brightness", buttonShape, Modifier.weight(1f))
                        EffectButton(effectMode == "contrast", { effectMode = "contrast" },
                            Icons.Default.Contrast, "Contrast", buttonShape, Modifier.weight(1f))
                        EffectButton(effectMode == "flip" || flipHorizontal || flipVertical, { 
                            // Cycle through flip modes: none -> horizontal -> vertical -> both -> none
                            when {
                                !flipHorizontal && !flipVertical -> {
                                    flipHorizontal = true
                                    flipVertical = false
                                }
                                flipHorizontal && !flipVertical -> {
                                    flipHorizontal = false
                                    flipVertical = true
                                }
                                !flipHorizontal && flipVertical -> {
                                    flipHorizontal = true
                                    flipVertical = true
                                }
                                else -> {
                                    flipHorizontal = false
                                    flipVertical = false
                                }
                            }
                            effectMode = "flip"
                        }, Icons.Default.SwapHoriz, "Flip", buttonShape, Modifier.weight(1f))
                        EffectButton(effectMode == "overlay" || overlayEnabled, { 
                            effectMode = "overlay"
                            overlayEnabled = true 
                        }, Icons.Default.Gradient, "Overlay", buttonShape, Modifier.weight(1f))
                        EffectButton(effectMode == "dither" || ditherEnabled, { 
                            effectMode = "dither"
                            ditherEnabled = !ditherEnabled
                        }, Icons.Default.BlurOn, "Dither", buttonShape, Modifier.weight(1f))
                        EffectButton(effectMode == "halftone", { effectMode = "halftone" },
                            Icons.Default.Grain, "Halftone", buttonShape, Modifier.weight(1f))
                    }
                    
                    // Flip state display
                    if (effectMode == "flip") {
                        val flipState = when {
                            flipHorizontal && flipVertical -> "Both"
                            flipHorizontal -> "Horizontal"
                            flipVertical -> "Vertical"
                            else -> "None"
                        }
                        Text(
                            text = "Flip: $flipState",
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            color = Theme.colors.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                        )
                    }
                    
                    // Slider section
                    if (effectMode != "flip" && effectMode != "dither") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (effectMode) {
                                    "brightness" -> "Brightness: $brightness"
                                    "contrast" -> "Contrast: $contrast"
                                    "overlay" -> "Coverage: $overlaySpread%"
                                    else -> "Halftone: $halftoneIntensity"
                                },
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                color = Theme.colors.text,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minus button
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = Theme.colors.text,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            when (effectMode) {
                                                "brightness" -> brightness = (brightness - 1).coerceIn(-100, 100)
                                                "contrast" -> contrast = (contrast - 1).coerceIn(-100, 100)
                                                "overlay" -> overlaySpread = (overlaySpread - 1).coerceIn(25, 100)
                                                else -> halftoneIntensity = (halftoneIntensity - 1).coerceIn(0, 200)
                                            }
                                        }
                                )
                                
                                // Slider
                                Slider(
                                    value = when (effectMode) {
                                        "brightness" -> brightness.toFloat()
                                        "contrast" -> contrast.toFloat()
                                        "overlay" -> overlaySpread.toFloat()
                                        else -> halftoneIntensity.toFloat()
                                    },
                                    onValueChange = { v ->
                                        when (effectMode) {
                                            "brightness" -> brightness = v.roundToInt().coerceIn(-100, 100)
                                            "contrast" -> contrast = v.roundToInt().coerceIn(-100, 100)
                                            "overlay" -> overlaySpread = v.roundToInt().coerceIn(25, 100)
                                            else -> halftoneIntensity = v.roundToInt().coerceIn(0, 200)
                                        }
                                    },
                                    valueRange = when (effectMode) {
                                        "brightness", "contrast" -> -100f..100f
                                        "overlay" -> 25f..100f
                                        else -> 0f..200f
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Theme.colors.text,
                                        activeTrackColor = Theme.colors.text,
                                        inactiveTrackColor = Theme.colors.text
                                    )
                                )
                                
                                // Plus button
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = Theme.colors.text,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            when (effectMode) {
                                                "brightness" -> brightness = (brightness + 1).coerceIn(-100, 100)
                                                "contrast" -> contrast = (contrast + 1).coerceIn(-100, 100)
                                                "overlay" -> overlaySpread = (overlaySpread + 1).coerceIn(25, 100)
                                                else -> halftoneIntensity = (halftoneIntensity + 1).coerceIn(0, 200)
                                            }
                                        }
                                )
                            }
                            
                            if (effectMode == "overlay") {
                                // Second slider for smoothness
                                Text(
                                    text = "Smoothness: $overlayFalloff%",
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    color = Theme.colors.text,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = Theme.colors.text,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                overlayFalloff = (overlayFalloff - 1).coerceIn(0, 100)
                                            }
                                    )
                                    Slider(
                                        value = overlayFalloff.toFloat(),
                                        onValueChange = { v ->
                                            overlayFalloff = v.roundToInt().coerceIn(0, 100)
                                        },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Theme.colors.text,
                                            activeTrackColor = Theme.colors.text,
                                            inactiveTrackColor = Theme.colors.text
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Theme.colors.text,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                overlayFalloff = (overlayFalloff + 1).coerceIn(0, 100)
                                            }
                                    )
                                }
                                
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            TextButton("Left", overlaySide == "left", { overlaySide = "left" }, buttonFontSize, buttonShape, Modifier.weight(1f))
                            TextButton("Center", overlaySide == "center", { overlaySide = "center" }, buttonFontSize, buttonShape, Modifier.weight(1f))
                            TextButton("Right", overlaySide == "right", { overlaySide = "right" }, buttonFontSize, buttonShape, Modifier.weight(1f))
                                }
                            }
                            
                            if (effectMode == "halftone") {
                                // Second slider for dot/line size
                                Text(
                                    text = "Dot/Line Size: $halftoneDotSize%",
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    color = Theme.colors.text,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = Theme.colors.text,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                halftoneDotSize = (halftoneDotSize - 1).coerceIn(0, 100)
                                            }
                                    )
                                    Slider(
                                        value = halftoneDotSize.toFloat(),
                                        onValueChange = { v ->
                                            halftoneDotSize = v.roundToInt().coerceIn(0, 100)
                                        },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Theme.colors.text,
                                            activeTrackColor = Theme.colors.text,
                                            inactiveTrackColor = Theme.colors.text
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Theme.colors.text,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                halftoneDotSize = (halftoneDotSize + 1).coerceIn(0, 100)
                                            }
                                    )
                                }
                            }
                        }
                    }
                    
                    if (effectMode == "halftone") {
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            TextButton("Dots", halftoneShape == WallpaperHalftone.HalftoneShape.DOTS,
                                { halftoneShape = WallpaperHalftone.HalftoneShape.DOTS }, buttonFontSize, buttonShape, Modifier.weight(1f))
                            TextButton("Lines", halftoneShape == WallpaperHalftone.HalftoneShape.LINES,
                                { halftoneShape = WallpaperHalftone.HalftoneShape.LINES }, buttonFontSize, buttonShape, Modifier.weight(1f))
                        }
                    }
                    
                    // Dither controls: threshold slider + algorithm selector
                    if (effectMode == "dither") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Threshold slider (above algorithm buttons)
                            Text(
                                text = "Threshold: $thresholdLevel",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                color = Theme.colors.text,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = Theme.colors.text,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            thresholdLevel = (thresholdLevel - 1).coerceIn(0, 100)
                                        }
                                )
                                Slider(
                                    value = thresholdLevel.toFloat(),
                                    onValueChange = { v ->
                                        thresholdLevel = v.roundToInt().coerceIn(0, 100)
                                    },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Theme.colors.text,
                                        activeTrackColor = Theme.colors.text,
                                        inactiveTrackColor = Theme.colors.text
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = Theme.colors.text,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable {
                                            thresholdLevel = (thresholdLevel + 1).coerceIn(0, 100)
                                        }
                                    )
                                }
                                
                            // Algorithm selector buttons (below threshold)
                            if (ditherEnabled) {
                                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                                    TextButton("Floyd-Steinberg", ditherAlgorithm == WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG,
                                        { ditherAlgorithm = WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG }, buttonFontSize, buttonShape, Modifier.weight(1f))
                                    TextButton("Ordered", ditherAlgorithm == WallpaperDither.DitherAlgorithm.ORDERED,
                                        { ditherAlgorithm = WallpaperDither.DitherAlgorithm.ORDERED }, buttonFontSize, buttonShape, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) Theme.colors.text else Theme.colors.background)
            .border(2.dp, Theme.colors.text, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isSelected) Theme.colors.background else Theme.colors.text,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TextButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) Theme.colors.text else Theme.colors.background)
            .border(2.dp, Theme.colors.text, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            color = if (isSelected) Theme.colors.background else Theme.colors.text,
            fontSize = fontSize
        )
    }
}

@Composable
private fun ImageSelectionButton(
    text: String,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Theme.colors.text)
            .border(2.dp, Theme.colors.text, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            color = Theme.colors.background,
            fontSize = fontSize
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Theme.colors.background)
            .border(2.dp, Theme.colors.text, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            color = Theme.colors.text,
            fontSize = fontSize
        )
    }
}

