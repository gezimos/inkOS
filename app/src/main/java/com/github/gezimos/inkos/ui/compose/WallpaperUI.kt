package com.github.gezimos.inkos.ui.compose

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.helper.WallpaperUtility
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import kotlinx.coroutines.launch

@Composable
fun WallpaperUI(
    onBackClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    isDark: Boolean = false,
    showStatusBar: Boolean = false,
    onExternalWallpaperClick: () -> Unit = {},
    onWallpaperSet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val wallpaperUtility = remember { WallpaperUtility(context) }
    val titleFontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize
    // Use base fontSize for buttons and content (not multiplied)
    val buttonFontSize = fontSize
    
    var selectedTab by remember { mutableStateOf(1) } // 0 = Pick Image, 1 = Presets
    var isLoading by remember { mutableStateOf(false) }
    var showSetWallpaperScreen by remember { mutableStateOf(false) }
    var showWallpaperEditorScreen by remember { mutableStateOf(false) }
    var pendingWallpaperAction by remember { mutableStateOf<((Int, WallpaperEditorState) -> Unit)?>(null) }
    var currentImageResourceId by remember { mutableStateOf<Int?>(null) }
    var currentImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var currentEditorState by remember { mutableStateOf<WallpaperEditorState?>(null) }
    val scope = rememberCoroutineScope()
    
    // Page state for presets
    var currentPresetPage by remember { mutableStateOf(0) }
    

    // Image picker launcher (for single image selection)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            currentImageResourceId = null
            currentImageUri = uri
            pendingWallpaperAction = { flags, editorState ->
                scope.launch {
                    try {
                        isLoading = true
                        val success = wallpaperUtility.setWallpaperFromUri(
                            uri, 
                            flags,
                            flipHorizontal = editorState.flipHorizontal,
                            flipVertical = editorState.flipVertical,
                            brightness = editorState.brightness,
                            contrast = editorState.contrast,
                            isInverted = editorState.isInverted,
                            thresholdLevel = editorState.thresholdLevel,
                            ditherEnabled = editorState.ditherEnabled,
                            ditherAlgorithm = editorState.ditherAlgorithm,
                            halftoneIntensity = editorState.halftoneIntensity,
                            halftoneDotSize = editorState.halftoneDotSize,
                            halftoneShape = editorState.halftoneShape,
                            overlayEnabled = editorState.overlayEnabled,
                            overlaySide = editorState.overlaySide,
                            overlaySpread = editorState.overlaySpread,
                            overlayFalloff = editorState.overlayFalloff
                        )
                        isLoading = false
                        if (success) {
                            onWallpaperSet?.invoke() ?: onBackClick()
                        }
                    } catch (_: Exception) {
                        isLoading = false
                    }
                }
            }
            showSetWallpaperScreen = true
        }
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
                title = "Wallpaper",
                onClick = onBackClick,
                showStatusBar = showStatusBar,
                titleFontSize = titleFontSize,
                pageIndicator = if (!showSetWallpaperScreen && selectedTab == 1) {
                    {
                        val itemsPerPage = 4
                        val totalPages = (WallpaperUtility.PRESET_WALLPAPERS.size + itemsPerPage - 1) / itemsPerPage
                        if (totalPages > 1) {
                            SettingsComposable.PageIndicator(
                                currentPage = currentPresetPage,
                                pageCount = totalPages
                            )
                        }
                    }
                } else null
            )

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabButton(
                    text = "Presets",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    fontSize = titleFontSize,
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Browse",
                    isSelected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = "image/*"
                        }
                        imagePickerLauncher.launch(intent)
                    },
                    fontSize = titleFontSize,
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "External",
                    isSelected = false,
                    onClick = {
                        onExternalWallpaperClick()
                    },
                    fontSize = titleFontSize,
                    modifier = Modifier.weight(1f)
                )
            }

            // Content
            Box(modifier = Modifier.weight(1f)) {
                if (!showSetWallpaperScreen) {
                    PresetsTab(
                        presets = WallpaperUtility.PRESET_WALLPAPERS,
                        onPresetClick = { preset ->
                            currentImageResourceId = preset.resourceId
                            currentImageUri = null
                            showSetWallpaperScreen = true
                        },
                        fontSize = titleFontSize,
                        currentPage = currentPresetPage,
                        onPageChanged = { currentPresetPage = it }
                    )
                }
            }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Theme.colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Setting wallpaper...",
                                style = SettingsTheme.typography.body,
                                fontSize = titleFontSize,
                                color = Theme.colors.text
                            )
                            Text(
                                text = "For wallpaper to be visible, adjust Background Opacity in Look & Feel.",
                                style = SettingsTheme.typography.body,
                                fontSize = buttonFontSize,
                                color = Theme.colors.text,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
            }
            
            // Set Wallpaper screen (selection screen)
            if (showSetWallpaperScreen && !showWallpaperEditorScreen) {
                // Disable edge swipe back when SetWallpaper is shown
                DisposableEffect(Unit) {
                    val activity = context as? com.github.gezimos.inkos.MainActivity
                    val wasEnabled = activity?.allowEdgeSwipeBack ?: true
                    activity?.allowEdgeSwipeBack = false
                    
                    onDispose {
                        // Re-enable edge swipe back when SetWallpaper is closed
                        activity?.allowEdgeSwipeBack = wasEnabled
                    }
                }
                
                SetWallpaper(
                    onBackClick = { 
                        showSetWallpaperScreen = false
                        pendingWallpaperAction = null
                        currentImageResourceId = null
                        currentImageUri = null
                        currentEditorState = null
                    },
                    onEditWallpaper = {
                        showWallpaperEditorScreen = true
                    },
                    allowEdit = currentImageUri != null, // Only allow editing for URI-based wallpapers, not presets
                    onSetForHome = {
                        val action = pendingWallpaperAction
                        val resourceId = currentImageResourceId
                        val uri = currentImageUri
                        val editorState = currentEditorState ?: WallpaperEditorState()
                        
                        showSetWallpaperScreen = false
                        pendingWallpaperAction = null
                        currentImageResourceId = null
                        currentImageUri = null
                        currentEditorState = null
                        
                        if (action != null) {
                            action(android.app.WallpaperManager.FLAG_SYSTEM, editorState)
                        } else if (resourceId != null) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    val success = wallpaperUtility.setWallpaperFromResource(
                                        resourceId,
                                        android.app.WallpaperManager.FLAG_SYSTEM,
                                        flipHorizontal = editorState.flipHorizontal,
                                        flipVertical = editorState.flipVertical,
                                        brightness = editorState.brightness,
                                        contrast = editorState.contrast,
                                        isInverted = editorState.isInverted,
                                        thresholdLevel = editorState.thresholdLevel,
                                        ditherEnabled = editorState.ditherEnabled,
                                        ditherAlgorithm = editorState.ditherAlgorithm,
                                        halftoneIntensity = editorState.halftoneIntensity,
                                        halftoneDotSize = editorState.halftoneDotSize,
                                        halftoneShape = editorState.halftoneShape,
                                        overlayEnabled = editorState.overlayEnabled,
                                        overlaySide = editorState.overlaySide,
                                        overlaySpread = editorState.overlaySpread,
                                        overlayFalloff = editorState.overlayFalloff
                                    )
                                    isLoading = false
                                    if (success) {
                                        onWallpaperSet?.invoke() ?: onBackClick()
                                    }
                                } catch (_: Exception) {
                                    isLoading = false
                                }
                            }
                        } else if (uri != null) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    val success = wallpaperUtility.setWallpaperFromUri(
                                        uri,
                                        android.app.WallpaperManager.FLAG_SYSTEM,
                                        flipHorizontal = editorState.flipHorizontal,
                                        flipVertical = editorState.flipVertical,
                                        brightness = editorState.brightness,
                                        contrast = editorState.contrast,
                                        isInverted = editorState.isInverted,
                                        thresholdLevel = editorState.thresholdLevel,
                                        ditherEnabled = editorState.ditherEnabled,
                                        ditherAlgorithm = editorState.ditherAlgorithm,
                                        halftoneIntensity = editorState.halftoneIntensity,
                                        halftoneDotSize = editorState.halftoneDotSize,
                                        halftoneShape = editorState.halftoneShape,
                                        overlayEnabled = editorState.overlayEnabled,
                                        overlaySide = editorState.overlaySide,
                                        overlaySpread = editorState.overlaySpread,
                                        overlayFalloff = editorState.overlayFalloff
                                    )
                                    isLoading = false
                                    if (success) {
                                        onWallpaperSet?.invoke() ?: onBackClick()
                                    }
                                } catch (_: Exception) {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    onSetForLockScreen = {
                        val action = pendingWallpaperAction
                        val resourceId = currentImageResourceId
                        val uri = currentImageUri
                        val editorState = currentEditorState ?: WallpaperEditorState()
                        
                        showSetWallpaperScreen = false
                        pendingWallpaperAction = null
                        currentImageResourceId = null
                        currentImageUri = null
                        currentEditorState = null
                        
                        if (action != null) {
                            action(android.app.WallpaperManager.FLAG_LOCK, editorState)
                        } else if (resourceId != null) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    val success = wallpaperUtility.setWallpaperFromResource(
                                        resourceId,
                                        android.app.WallpaperManager.FLAG_LOCK,
                                        flipHorizontal = editorState.flipHorizontal,
                                        flipVertical = editorState.flipVertical,
                                        brightness = editorState.brightness,
                                        contrast = editorState.contrast,
                                        isInverted = editorState.isInverted,
                                        thresholdLevel = editorState.thresholdLevel,
                                        ditherEnabled = editorState.ditherEnabled,
                                        ditherAlgorithm = editorState.ditherAlgorithm,
                                        halftoneIntensity = editorState.halftoneIntensity,
                                        halftoneDotSize = editorState.halftoneDotSize,
                                        halftoneShape = editorState.halftoneShape,
                                        overlayEnabled = editorState.overlayEnabled,
                                        overlaySide = editorState.overlaySide,
                                        overlaySpread = editorState.overlaySpread,
                                        overlayFalloff = editorState.overlayFalloff
                                    )
                                    isLoading = false
                                    if (success) {
                                        onWallpaperSet?.invoke() ?: onBackClick()
                                    }
                                } catch (_: Exception) {
                                    isLoading = false
                                }
                            }
                        } else if (uri != null) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    val success = wallpaperUtility.setWallpaperFromUri(
                                        uri,
                                        android.app.WallpaperManager.FLAG_LOCK,
                                        flipHorizontal = editorState.flipHorizontal,
                                        flipVertical = editorState.flipVertical,
                                        brightness = editorState.brightness,
                                        contrast = editorState.contrast,
                                        isInverted = editorState.isInverted,
                                        thresholdLevel = editorState.thresholdLevel,
                                        ditherEnabled = editorState.ditherEnabled,
                                        ditherAlgorithm = editorState.ditherAlgorithm,
                                        halftoneIntensity = editorState.halftoneIntensity,
                                        halftoneDotSize = editorState.halftoneDotSize,
                                        halftoneShape = editorState.halftoneShape,
                                        overlayEnabled = editorState.overlayEnabled,
                                        overlaySide = editorState.overlaySide,
                                        overlaySpread = editorState.overlaySpread,
                                        overlayFalloff = editorState.overlayFalloff
                                    )
                                    isLoading = false
                                    if (success) {
                                        onWallpaperSet?.invoke() ?: onBackClick()
                                    }
                                } catch (_: Exception) {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    onSetForBoth = {
                        val action = pendingWallpaperAction
                        val resourceId = currentImageResourceId
                        val uri = currentImageUri
                        val editorState = currentEditorState ?: WallpaperEditorState()
                        
                        showSetWallpaperScreen = false
                        pendingWallpaperAction = null
                        currentImageResourceId = null
                        currentImageUri = null
                        currentEditorState = null
                        
                        val flags = android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK
                        
                        if (action != null) {
                            action(flags, editorState)
                        } else if (resourceId != null) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    val success = wallpaperUtility.setWallpaperFromResource(
                                        resourceId,
                                        flags,
                                        flipHorizontal = editorState.flipHorizontal,
                                        flipVertical = editorState.flipVertical,
                                        brightness = editorState.brightness,
                                        contrast = editorState.contrast,
                                        isInverted = editorState.isInverted,
                                        thresholdLevel = editorState.thresholdLevel,
                                        ditherEnabled = editorState.ditherEnabled,
                                        ditherAlgorithm = editorState.ditherAlgorithm,
                                        halftoneIntensity = editorState.halftoneIntensity,
                                        halftoneDotSize = editorState.halftoneDotSize,
                                        halftoneShape = editorState.halftoneShape,
                                        overlayEnabled = editorState.overlayEnabled,
                                        overlaySide = editorState.overlaySide,
                                        overlaySpread = editorState.overlaySpread,
                                        overlayFalloff = editorState.overlayFalloff
                                    )
                                    isLoading = false
                                    if (success) {
                                        onWallpaperSet?.invoke() ?: onBackClick()
                                    }
                                } catch (_: Exception) {
                                    isLoading = false
                                }
                            }
                        } else if (uri != null) {
                            scope.launch {
                                try {
                                    isLoading = true
                                    val success = wallpaperUtility.setWallpaperFromUri(
                                        uri,
                                        flags,
                                        flipHorizontal = editorState.flipHorizontal,
                                        flipVertical = editorState.flipVertical,
                                        brightness = editorState.brightness,
                                        contrast = editorState.contrast,
                                        isInverted = editorState.isInverted,
                                        thresholdLevel = editorState.thresholdLevel,
                                        ditherEnabled = editorState.ditherEnabled,
                                        ditherAlgorithm = editorState.ditherAlgorithm,
                                        halftoneIntensity = editorState.halftoneIntensity,
                                        halftoneDotSize = editorState.halftoneDotSize,
                                        halftoneShape = editorState.halftoneShape,
                                        overlayEnabled = editorState.overlayEnabled,
                                        overlaySide = editorState.overlaySide,
                                        overlaySpread = editorState.overlaySpread,
                                        overlayFalloff = editorState.overlayFalloff
                                    )
                                    isLoading = false
                                    if (success) {
                                        onWallpaperSet?.invoke() ?: onBackClick()
                                    }
                                } catch (_: Exception) {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    fontSize = buttonFontSize,
                    isDark = isDark,
                    showStatusBar = showStatusBar
                )
            }
            
            // Wallpaper Editor screen
            if (showWallpaperEditorScreen) {
                // Disable edge swipe back when WallpaperEditor is shown
                DisposableEffect(Unit) {
                    val activity = context as? com.github.gezimos.inkos.MainActivity
                    val wasEnabled = activity?.allowEdgeSwipeBack ?: true
                    activity?.allowEdgeSwipeBack = false
                    
                    onDispose {
                        // Re-enable edge swipe back when WallpaperEditor is closed
                        activity?.allowEdgeSwipeBack = wasEnabled
                    }
                }
                
                key(if (currentImageUri != null) currentImageUri.toString() else (currentImageResourceId?.toString() ?: "none")) {
                    WallpaperEditor(
                        onBackClick = { 
                            showWallpaperEditorScreen = false
                        },
                        onShowSetWallpaper = { editorState ->
                            currentEditorState = editorState
                            showWallpaperEditorScreen = false
                        },
                        onBrowseClick = {
                            currentImageResourceId = null
                            val intent = Intent(Intent.ACTION_PICK).apply {
                                type = "image/*"
                            }
                            imagePickerLauncher.launch(intent)
                        },
                        onPresetsClick = {
                            showWallpaperEditorScreen = false
                            showSetWallpaperScreen = false
                            pendingWallpaperAction = null
                            currentImageResourceId = null
                            currentImageUri = null
                            selectedTab = 1
                        },
                        onSelect = { flags, editorState ->
                            showWallpaperEditorScreen = false
                            showSetWallpaperScreen = false
                            
                            val action = pendingWallpaperAction
                            val resourceId = currentImageResourceId
                            
                            pendingWallpaperAction = null
                            currentImageResourceId = null
                            currentImageUri = null
                            
                            if (action != null) {
                                action(flags, editorState)
                            } else if (resourceId != null) {
                                scope.launch {
                                    try {
                                        isLoading = true
                                        val success = wallpaperUtility.setWallpaperFromResource(
                                            resourceId,
                                            flags,
                                            flipHorizontal = editorState.flipHorizontal,
                                            flipVertical = editorState.flipVertical,
                                            brightness = editorState.brightness,
                                            contrast = editorState.contrast,
                                            isInverted = editorState.isInverted,
                                            thresholdLevel = editorState.thresholdLevel,
                                            ditherEnabled = editorState.ditherEnabled,
                                            ditherAlgorithm = editorState.ditherAlgorithm,
                                            halftoneIntensity = editorState.halftoneIntensity,
                                            halftoneDotSize = editorState.halftoneDotSize,
                                            halftoneShape = editorState.halftoneShape,
                                            overlayEnabled = editorState.overlayEnabled,
                                            overlaySide = editorState.overlaySide,
                                            overlaySpread = editorState.overlaySpread,
                                            overlayFalloff = editorState.overlayFalloff
                                        )
                                        isLoading = false
                                        if (success) {
                                            onWallpaperSet?.invoke() ?: onBackClick()
                                        }
                                    } catch (_: Exception) {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        fontSize = buttonFontSize,
                        isDark = isDark,
                        showStatusBar = showStatusBar,
                        imageResourceId = if (currentImageUri != null) null else currentImageResourceId,
                        imageUri = currentImageUri
                    )
                }
            }
        }

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = Prefs(context)
    val textIslandsShape = prefs.textIslandsShape
    val tabButtonShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 50.dp
        )
    }
    
    val backgroundColor = if (isSelected) {
        Theme.colors.text
    } else {
        Theme.colors.background
    }
    val textColor = if (isSelected) {
        Theme.colors.background
    } else {
        Theme.colors.text
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(tabButtonShape)
            .then(
                if (isSelected) {
                    Modifier.background(backgroundColor)
                } else {
                    Modifier
                        .background(backgroundColor)
                        .border(2.dp, Theme.colors.text, tabButtonShape)
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            fontSize = fontSize,
            color = textColor
        )
    }
}

@Composable
fun PresetsTab(
    presets: List<WallpaperUtility.PresetWallpaper>,
    onPresetClick: (WallpaperUtility.PresetWallpaper) -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    currentPage: Int,
    onPageChanged: (Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    
    // Calculate items per page: at least 4 (2 columns x 2 rows minimum)
    val itemsPerPage = 4
    val columns = 2
    
    // Chunk presets into pages
    val chunks = remember(presets, itemsPerPage) {
        presets.chunked(itemsPerPage)
    }
    
    val totalPages = chunks.size
    val pageIndex = currentPage.coerceIn(0, maxOf(totalPages - 1, 0))
    val pagePresets = chunks.getOrNull(pageIndex) ?: emptyList()
    
    val density = LocalDensity.current
    var gridHeight by remember { mutableStateOf(0.dp) }
    var imageHeight by remember { mutableStateOf(0.dp) }
    var isHeightCalculated by remember { mutableStateOf(false) }
    
    // Calculate image height only once when grid is first measured
    val topBottomPadding = 24.dp * 2 // 48dp total
    val verticalSpacing = 24.dp // One gap between 2 rows
    val textHeight = remember(fontSize) {
        if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) {
            (fontSize.value * 1.2).dp // Approximate text height with line spacing
        } else {
            20.dp // Default text height
        }
    }
    val textSpacing = 8.dp // Spacer between image and text
    val itemPadding = 8.dp * 2 // Top and bottom padding inside each item
    
    // Only calculate once when grid is first measured
    LaunchedEffect(gridHeight) {
        if (gridHeight > 0.dp && !isHeightCalculated) {
            // Total height needed for text and spacing per item
            val perItemTextSpace = textHeight + textSpacing + itemPadding
            // Available height for images: total - padding - vertical spacing - text space for 2 items
            val availableHeight = gridHeight - topBottomPadding - verticalSpacing - (perItemTextSpace * 2)
            imageHeight = (availableHeight / 2).coerceAtLeast(0.dp)
            isHeightCalculated = true
        }
    }
    
    // Use rememberUpdatedState to ensure callback is always current
    val currentOnPageChanged = androidx.compose.runtime.rememberUpdatedState(onPageChanged)
    val currentPageState = remember { mutableStateOf(currentPage) }
    
    // Sync external currentPage with internal state
    LaunchedEffect(currentPage) {
        currentPageState.value = currentPage
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 24.dp,
            bottom = 24.dp
        ),
        userScrollEnabled = false,
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size: androidx.compose.ui.unit.IntSize ->
                gridHeight = with(density) { size.height.toDp() }
            }
            .gestureHelper(
                shortSwipeRatio = prefs.shortSwipeThresholdRatio,
                longSwipeRatio = prefs.longSwipeThresholdRatio,
                onVerticalPageMove = { delta ->
                    if (totalPages <= 1) return@gestureHelper
                    val current = currentPageState.value
                    if (delta > 0) {
                        val newPage = (current + delta).coerceAtMost(totalPages - 1)
                        if (newPage != current) {
                            currentPageState.value = newPage
                            currentOnPageChanged.value(newPage)
                        }
                    } else if (delta < 0) {
                        val steps = -delta
                        val newPage = (current - steps).coerceAtLeast(0)
                        if (newPage != current) {
                            currentPageState.value = newPage
                            currentOnPageChanged.value(newPage)
                        }
                    }
                }
            )
    ) {
        items(pagePresets) { preset ->
            PresetItem(
                preset = preset,
                onClick = { onPresetClick(preset) },
                fontSize = fontSize,
                imageHeight = imageHeight
            )
        }
    }
}

@Composable
fun PresetItem(
    preset: WallpaperUtility.PresetWallpaper,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    imageHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = Prefs(context)
    val textIslandsShape = prefs.textIslandsShape
    val presetItemShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 12.dp
        )
    }
    val imageShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 8.dp
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(presetItemShape)
            .background(Theme.colors.background)
            .border(2.dp, Theme.colors.text, presetItemShape)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Image with dynamic height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (imageHeight != androidx.compose.ui.unit.Dp.Unspecified && imageHeight > 0.dp) Modifier.height(imageHeight) else Modifier.aspectRatio(1f))
        ) {
            Image(
                painter = painterResource(id = preset.resourceId),
                contentDescription = preset.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(imageShape)
                    .clickable { onClick() },
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = preset.name,
            style = SettingsTheme.typography.title,
            fontSize = fontSize,
            color = Theme.colors.text,
            textAlign = TextAlign.Center
        )
    }
}

