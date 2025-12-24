package com.github.gezimos.inkos.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.common.isSystemApp
import com.github.gezimos.inkos.AppsDrawerUiState
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.ui.AppsDrawerState

/**
 * Main layout component for the apps drawer (pure UI, no logic)
 */
@Composable
fun AppsDrawerLayout(
    state: AppsDrawerState,
    uiState: AppsDrawerUiState,
    displayApps: List<AppListItem>,
    paddedDisplayApps: List<AppListItem?>,
    totalPages: Int,
    isCalculated: Boolean,
    searchFocusRequester: FocusRequester,
    onAppClick: (AppListItem) -> Unit,
    onAppLongClick: (AppListItem) -> Unit,
    onDelete: (AppListItem) -> Unit,
    onRename: (String, String) -> Unit,
    onHideShow: (Constants.AppDrawerFlag, AppListItem) -> Unit,
    onLock: (AppListItem) -> Unit,
    onInfo: (AppListItem) -> Unit,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onSearchAction: () -> Unit,
    onInteract: () -> Unit,
    onCloseMenu: () -> Unit
) {
    val context = LocalContext.current
    val isSearchActive = !state.isHiddenAppsMode && state.searchQuery.text.isNotBlank()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
            if (!isCalculated) {
                // Show "Calculating..." while calculating appsPerPage
                // Block all interactions until calculation is complete
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Block all pointer events - consume all gestures
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Calculating...",
                        color = Theme.colors.text,
                        fontSize = 20.sp
                    )
                }
            } else if (displayApps.isEmpty() && !state.searchEnabled) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSearchActive) "No apps found" else "No apps available",
                        color = Theme.colors.text,
                        fontSize = 20.sp
                    )
                }
            } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        userScrollEnabled = false
                    ) {
                        // Show title for SetHomeApp and HiddenApps modes
                        if (state.flag == Constants.AppDrawerFlag.SetHomeApp || state.flag == Constants.AppDrawerFlag.HiddenApps) {
                            item(key = "title") {
                                val ctx = LocalContext.current
                                val customPath = uiState.customFontPath
                                val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
                                val appFontFamily = remember(appTypefaceNullable) {
                                    if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
                                }
                                
                                val titleText = when (state.flag) {
                                    Constants.AppDrawerFlag.SetHomeApp -> "SELECT APP"
                                    Constants.AppDrawerFlag.HiddenApps -> "HIDDEN APPS"
                                    else -> ""
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = uiState.appDrawerGap.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = titleText,
                                        color = Theme.colors.text,
                                        fontSize = uiState.appDrawerSize.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = appFontFamily
                                    )
                                }
                            }
                        }
                        
                        if (state.searchEnabled && !state.isHiddenAppsMode) {
                            item(key = "search_field") {
                                SearchField(
                                    searchQuery = state.searchQuery,
                                    onSearchQueryChange = onSearchQueryChange,
                                    uiState = uiState,
                                    focusRequester = searchFocusRequester,
                                    onInteract = onInteract,
                                    onSearchAction = onSearchAction,
                                    isDpadFocused = state.searchFocused
                                )
                            }
                        }
                        
                        itemsIndexed(
                            items = paddedDisplayApps,
                            key = { index, app ->
                                app?.let { "${it.activityPackage}_${it.activityClass}_${it.user.hashCode()}_$index" }
                                    ?: "placeholder_${state.currentPage}_$index"
                            }
                        ) { index, app ->
                            if (app != null) {
                                val isWorkProfile = try {
                                    app.user != android.os.Process.myUserHandle()
                                } catch (_: Exception) {
                                    false
                                }
                                val isLocked = uiState.lockedApps.contains(app.activityPackage)
                                val isNewlyInstalled = uiState.newlyInstalledApps.contains(app.activityPackage)
                                val isDarkTheme = uiState.appTheme == Constants.Theme.Dark
                                
                                // When A-Z filter is focused we keep the selected index but hide the
                                // DPAD focus pill on the app list to avoid visual confusion.
                                AppItem(
                                    app = app,
                                    uiState = uiState,
                                    flag = state.flag,
                                    isSelected = (index == state.selectedItemIndex),
                                    // Hide DPAD highlight when AZ filter has focus
                                    isDpadMode = state.isDpadMode && !state.azFilterFocused,
                                    showContextMenu = (state.contextMenuApp == app && state.showContextMenu),
                                    showRename = (state.renameApp == app && state.showRenameOverlay),
                                    isWorkProfile = isWorkProfile,
                                    isLocked = isLocked,
                                    isNewlyInstalled = isNewlyInstalled,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) },
                                    onDelete = onDelete,
                                    onRename = onRename,
                                    onHideShow = onHideShow,
                                    onLock = onLock,
                                    onInfo = onInfo,
                                    onCloseMenu = onCloseMenu
                                )
                            } else {
                                AppPlaceholderItem(uiState = uiState)
                            }
                        }
                    }

                    // Page indicator dots removed — controlled by UI settings elsewhere. 
                }
            }
        }

// calculateDynamicAppsPerPage moved to AppsFragment

/**
 * Pure UI component for the search field
 */
@Composable
private fun SearchField(
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    uiState: AppsDrawerUiState,
    focusRequester: FocusRequester,
    onInteract: (() -> Unit)? = null,
    onSearchAction: (() -> Unit)? = null,
    isDpadFocused: Boolean = false
) {
    val ctx = LocalContext.current
    val customPath = uiState.customFontPath
    val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
    val appFontFamily = remember(appTypefaceNullable) {
        if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
    }

    var isFocused by remember { mutableStateOf(false) }
    
    val boxAlignment = remember(uiState.appDrawerAlignment) {
        when (uiState.appDrawerAlignment) {
            1 -> Alignment.Center
            2 -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    }
    
    val searchHighlightColor = Theme.colors.text
    val density = LocalDensity.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // When AZ filter is shown, add padding on the side where AZ filter is located
                // Right alignment (2) -> AZ on left -> start padding; otherwise -> AZ on right -> end padding
                if (uiState.appDrawerAzFilter) {
                    if (uiState.appDrawerAlignment == 2) Modifier.padding(start = azSidebarWidth)
                    else Modifier.padding(end = azSidebarWidth)
                } else {
                    Modifier
                }
            )
            .padding(vertical = uiState.appDrawerGap.dp)
            .then(
                if (isDpadFocused) {
                    Modifier.drawBehind {
                        val cornerRadius = ShapeHelper.getCornerRadius(
                            textIslandsShape = uiState.textIslandsShape,
                            height = size.height,
                            density = density
                        )
                        drawRoundRect(
                            color = searchHighlightColor,
                            cornerRadius = cornerRadius,
                            alpha = 0.2f
                        )
                    }
                } else Modifier
            ),
        contentAlignment = boxAlignment
    ) {
        // Determine alignment preference and adapt layout + text alignment.
        val alignmentPref = uiState.appDrawerAlignment
        val textAlign = if (alignmentPref == 2) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start
        val prefixChar = if (alignmentPref == 2) "<" else ">"
        // Hide prefix character when search is active (focused or has text)
        val showPrefix = !isFocused && searchQuery.text.isEmpty()
        val basicTextStyle = TextStyle(
            color = Theme.colors.text,
            fontSize = uiState.appDrawerSize.sp,
            fontFamily = appFontFamily,
            textAlign = textAlign
        )

        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {
                        }
                        onInteract?.invoke()
                    },
                    onLongClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = basicTextStyle,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Theme.colors.text),
            decorationBox = { innerTextField ->
                // When right-aligned, place editable area first and prefix at end so
                // the visual alignment matches app labels and pager dots.
                when (alignmentPref) {
                    1 -> {
                        // Centered: keep prefix then editable area
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            if (showPrefix) {
                                Text(
                                    text = prefixChar,
                                    color = Theme.colors.text,
                                    fontSize = uiState.appDrawerSize.sp,
                                    fontFamily = appFontFamily
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.text.isEmpty() && !isFocused) {
                                    Text(
                                        text = "_",
                                        color = Theme.colors.text.copy(alpha = 0.5f),
                                        fontSize = uiState.appDrawerSize.sp,
                                        fontFamily = appFontFamily,
                                        textAlign = textAlign
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                    2 -> {
                        // Right aligned: editable area first, prefix on the right
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.text.isEmpty() && !isFocused) {
                                    Text(
                                        text = "_",
                                        color = Theme.colors.text.copy(alpha = 0.5f),
                                        fontSize = uiState.appDrawerSize.sp,
                                        fontFamily = appFontFamily,
                                        textAlign = textAlign,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                // Align inner text to the end for right alignment
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    innerTextField()
                                }
                            }
                            if (showPrefix) {
                                Text(
                                    text = prefixChar,
                                    color = Theme.colors.text,
                                    fontSize = uiState.appDrawerSize.sp,
                                    fontFamily = appFontFamily
                                )
                            }
                        }
                    }
                    else -> {
                        // Left aligned (default)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showPrefix) {
                                Text(
                                    text = prefixChar,
                                    color = Theme.colors.text,
                                    fontSize = uiState.appDrawerSize.sp,
                                    fontFamily = appFontFamily
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.text.isEmpty() && !isFocused) {
                                    Text(
                                        text = "_",
                                        color = Theme.colors.text.copy(alpha = 0.5f),
                                        fontSize = uiState.appDrawerSize.sp,
                                        fontFamily = appFontFamily
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { onSearchAction?.invoke() }
            )
        )
    }
}

/**
 * Pure UI component for an app item in the list
 */
/**
 * Helper data class to hold app type flags - only computed when context menu is shown
 */
private data class AppTypeFlags(
    val isSynthetic: Boolean,
    val isSystemShortcut: Boolean,
    val isSystemApp: Boolean
)

/**
 * Compute app type flags lazily - only when context menu is shown
 */
@Composable
private fun computeAppTypeFlags(app: AppListItem, context: android.content.Context): AppTypeFlags {
    return remember(app.activityPackage) {
        val isSynthetic = app.activityPackage == "com.inkos.internal.app_drawer" ||
                          app.activityPackage == "com.inkos.internal.notifications" ||
                          app.activityPackage == "com.inkos.internal.simple_tray" ||
                          app.activityPackage == "com.inkos.internal.recents"
        val isSystemShortcut = app.activityPackage.startsWith("com.inkos.system.")
        val isSystemApp = context.isSystemApp(app.activityPackage)
        AppTypeFlags(isSynthetic, isSystemShortcut, isSystemApp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppListItem,
    uiState: AppsDrawerUiState,
    flag: Constants.AppDrawerFlag,
    isSelected: Boolean = false,
    isDpadMode: Boolean = false,
    showContextMenu: Boolean = false,
    showRename: Boolean = false,
    isWorkProfile: Boolean = false,
    isLocked: Boolean = false,
    isNewlyInstalled: Boolean = false,
    isDarkTheme: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: (AppListItem) -> Unit,
    onRename: (String, String) -> Unit,
    onHideShow: (Constants.AppDrawerFlag, AppListItem) -> Unit,
    onLock: (AppListItem) -> Unit,
    onInfo: (AppListItem) -> Unit,
    onCloseMenu: () -> Unit
) {
    val displayText = remember(app.customLabel, app.label) {
        if (app.customLabel.isNotEmpty()) app.customLabel else app.label
    }
    val finalText = remember(displayText, uiState.allCapsApps, uiState.smallCapsApps) {
        when {
            uiState.allCapsApps -> displayText.uppercase()
            uiState.smallCapsApps -> displayText.lowercase()
            else -> displayText
        }
    }
    val labelWithCloneIndicator = remember(finalText, isWorkProfile) {
        if (isWorkProfile) "$finalText^" else finalText
    }
    
    val boxAlignment = remember(uiState.appDrawerAlignment) {
        when (uiState.appDrawerAlignment) {
            1 -> Alignment.Center
            2 -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    }
    
    // Key renameText state to the app so it resets for different apps
    val initialText = if (app.customLabel.isNotEmpty()) app.customLabel else app.label
    var renameText by remember(app.activityPackage, showRename) { 
        mutableStateOf(
            TextFieldValue(
                text = if (showRename) initialText else "",
                selection = if (showRename) androidx.compose.ui.text.TextRange(initialText.length) else androidx.compose.ui.text.TextRange.Zero
            )
        ) 
    }
    var originalRenameText by remember(app.activityPackage, showRename) { mutableStateOf(if (showRename) initialText else "") }
    val renameFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(showRename) {
        if (showRename) {
            try {
                renameFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }
    
    val saveButtonText = remember(renameText.text, originalRenameText) {
        when {
            renameText.text.isEmpty() -> "Reset"
            renameText.text == originalRenameText -> "Close"
            else -> "Save"
        }
    }
    
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val customPath = uiState.customFontPath
    val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
    val appFontFamily = remember(appTypefaceNullable) {
        if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
    }
    val highlightPaddingPx = with(density) { 8.dp.toPx() }
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    // When highlighted, label text should be the theme background (we no longer use luminance heuristics)
    val invertedHighlightTextColor = bgColor
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // When AZ filter is shown, add padding on the side where AZ filter is located
                // Right alignment (2) -> AZ on left -> start padding; otherwise -> AZ on right -> end padding
                if (uiState.appDrawerAzFilter) {
                    if (uiState.appDrawerAlignment == 2) Modifier.padding(start = azSidebarWidth)
                    else Modifier.padding(end = azSidebarWidth)
                } else {
                    Modifier
                }
            )
            .padding(vertical = uiState.appDrawerGap.dp)
            .then(
                if (!showContextMenu && !showRename) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                } else Modifier
            ),
        contentAlignment = boxAlignment
    ) {
        when {
            showContextMenu -> {
                // Compute app type flags ONLY when context menu is shown (lazy evaluation)
                val ctx = LocalContext.current
                val appTypeFlags = computeAppTypeFlags(app, ctx)
                
                // Compute target height for app item so context menu buttons match it
                val itemTextHeightDp = with(LocalDensity.current) { uiState.appDrawerSize.sp.toDp() }
                val appItemHeight = itemTextHeightDp + (uiState.appDrawerGap.dp * 2)
                val contextMenuButtonSize = appItemHeight * 0.9f

                // Context menu state for DPAD navigation
                var focusedIndex by remember { mutableStateOf(if (isDpadMode) 0 else -1) }
                val contextMenuFocusRequester = remember { FocusRequester() }
                
                // Track if we should ignore the first Enter/DPAD key event (residual from long-press)
                var ignoreFirstActivation by remember { mutableStateOf(isDpadMode) }
                
                // Menu items: Delete(0), Rename(1), Hide/Show(2), Lock(3), Info(4), Close(5)
                // For system shortcuts: only Rename, Hide/Show, and Close are enabled
                val menuItemsEnabled = if (appTypeFlags.isSystemShortcut) {
                    listOf(false, true, true, false, false, true)
                } else {
                    listOf(!appTypeFlags.isSynthetic && !appTypeFlags.isSystemApp, !appTypeFlags.isSynthetic, true, !appTypeFlags.isSynthetic, !appTypeFlags.isSynthetic, true)
                }
                val menuItemCount = 6
                
                // Find next enabled item in direction
                fun findNextEnabled(from: Int, direction: Int): Int {
                    var next = from + direction
                    while (next in 0 until menuItemCount) {
                        if (menuItemsEnabled[next]) return next
                        next += direction
                    }
                    return from
                }
                
                // Request focus when shown in DPAD mode
                LaunchedEffect(isDpadMode) {
                    if (isDpadMode) {
                        focusedIndex = findNextEnabled(-1, 1) // Find first enabled
                        try { contextMenuFocusRequester.requestFocus() } catch (_: Exception) {}
                    } else {
                        focusedIndex = -1
                    }
                }
                
                // Context menu row (replaces app name)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(appItemHeight)
                        .focusRequester(contextMenuFocusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            when (keyEvent.key) {
                                Key.DirectionCenter, Key.Enter -> {
                                    // For activation keys, we need to handle both KeyDown and KeyUp
                                    // to properly ignore the residual key events from long-press
                                    if (keyEvent.type == KeyEventType.KeyUp) {
                                        if (ignoreFirstActivation) {
                                            // This is the key-up from the long-press that opened the menu
                                            ignoreFirstActivation = false
                                            return@onPreviewKeyEvent true
                                        }
                                        // Actual activation on key-up (after a fresh key-down)
                                        when (focusedIndex) {
                                            0 -> if (!appTypeFlags.isSynthetic && !appTypeFlags.isSystemShortcut) onDelete(app)
                                            1 -> if (!appTypeFlags.isSynthetic || appTypeFlags.isSystemShortcut) { onCloseMenu(); onRename(app.activityPackage, "") }
                                            2 -> onHideShow(flag, app)
                                            3 -> if (!appTypeFlags.isSynthetic && !appTypeFlags.isSystemShortcut) onLock(app)
                                            4 -> if (!appTypeFlags.isSynthetic && !appTypeFlags.isSystemShortcut) onInfo(app)
                                            5 -> onCloseMenu()
                                        }
                                        return@onPreviewKeyEvent true
                                    } else if (keyEvent.type == KeyEventType.KeyDown) {
                                        // Consume key-down but don't act yet (wait for key-up)
                                        // Also clear ignore flag on fresh key-down
                                        if (ignoreFirstActivation && keyEvent.nativeKeyEvent.repeatCount == 0) {
                                            // This is a fresh press after the long-press was released
                                            ignoreFirstActivation = false
                                        }
                                        return@onPreviewKeyEvent true
                                    }
                                    false
                                }
                                else -> {
                                    // For other keys, only handle on KeyDown
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    
                                    when (keyEvent.key) {
                                        Key.DirectionLeft -> {
                                            focusedIndex = findNextEnabled(focusedIndex, -1)
                                            true
                                        }
                                        Key.DirectionRight -> {
                                            focusedIndex = findNextEnabled(focusedIndex, 1)
                                            true
                                        }
                                        Key.Escape, Key.Back -> {
                                            onCloseMenu()
                                            true
                                        }
                                        else -> false
                                    }
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InlineContextButton(
                        icon = Icons.Rounded.Delete,
                        enabled = !appTypeFlags.isSynthetic && !appTypeFlags.isSystemApp && !appTypeFlags.isSystemShortcut,
                        uiState = uiState,
                        isFocused = isDpadMode && focusedIndex == 0,
                        sizeOverride = contextMenuButtonSize,
                        onClick = { onDelete(app) }
                    )
                    InlineContextButton(
                        icon = Icons.Rounded.DriveFileRenameOutline,
                        enabled = !appTypeFlags.isSynthetic || appTypeFlags.isSystemShortcut,
                        uiState = uiState,
                        isFocused = isDpadMode && focusedIndex == 1,
                        sizeOverride = contextMenuButtonSize,
                        onClick = { onCloseMenu(); onRename(app.activityPackage, "") }
                    )
                    InlineContextButton(
                        icon = if (flag == Constants.AppDrawerFlag.HiddenApps) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        enabled = true,
                        uiState = uiState,
                        isFocused = isDpadMode && focusedIndex == 2,
                        sizeOverride = contextMenuButtonSize,
                        onClick = { onHideShow(flag, app) }
                    )
                    InlineContextButton(
                        icon = if (isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        enabled = !appTypeFlags.isSynthetic && !appTypeFlags.isSystemShortcut,
                        uiState = uiState,
                        isFocused = isDpadMode && focusedIndex == 3,
                        sizeOverride = contextMenuButtonSize,
                        onClick = { onLock(app) }
                    )
                    InlineContextButton(
                        icon = Icons.Rounded.Info,
                        enabled = !appTypeFlags.isSynthetic && !appTypeFlags.isSystemShortcut,
                        uiState = uiState,
                        isFocused = isDpadMode && focusedIndex == 4,
                        sizeOverride = contextMenuButtonSize,
                        onClick = { onInfo(app) }
                    )
                    InlineContextButton(
                        icon = Icons.Rounded.Close,
                        enabled = true,
                        uiState = uiState,
                        isFocused = isDpadMode && focusedIndex == 5,
                        sizeOverride = contextMenuButtonSize,
                        onClick = { onCloseMenu() }
                    )
                }
            }
            showRename -> {
                // Track if save button is focused (for DPAD mode)
                var saveButtonFocused by remember { mutableStateOf(false) }
                val saveButtonFocusRequester = remember { FocusRequester() }
                
                // Rename field (replaces app name)
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(renameFocusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (keyEvent.key) {
                                    Key.DirectionRight, Key.Tab -> {
                                        // Move focus to save button
                                        saveButtonFocused = true
                                        try { saveButtonFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    else -> false
                                }
                            },
                        textStyle = TextStyle(
                            color = Theme.colors.text,
                            fontSize = uiState.appDrawerSize.sp,
                            fontFamily = appFontFamily
                        ),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Theme.colors.text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                when {
                                    renameText.text.isEmpty() -> {
                                        onRename(app.activityPackage, "RESET_TO_ORIGINAL")
                                    }
                                    renameText.text.trim() != originalRenameText -> {
                                        onRename(app.activityPackage, renameText.text.trim())
                                    }
                                }
                                onCloseMenu()
                            }
                        )
                    )
                    
                    // Use same color logic as context menu buttons:
                    // Normal (not focused): Shape with transparent background, 2dp textColor border, textColor text
                    // Focused (DPAD mode): Shape with solid textColor fill, 2dp textColor border, backgroundColor text
                    val saveBgColor = if (saveButtonFocused) textColor else Color.Transparent
                    val saveTextColor = if (saveButtonFocused) bgColor else textColor
                    val saveBorderColor = textColor
                    
                    // Calculate shape based on textIslandsShape preference (same as context menu buttons)
                    val saveButtonShape = remember(uiState.textIslandsShape) {
                        when (uiState.textIslandsShape) {
                            0 -> CircleShape
                            1 -> RoundedCornerShape(8.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .focusRequester(saveButtonFocusRequester)
                            .focusable()
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                when (keyEvent.key) {
                                    Key.DirectionLeft -> {
                                        // Move focus back to text field
                                        saveButtonFocused = false
                                        try { renameFocusRequester.requestFocus() } catch (_: Exception) {}
                                        true
                                    }
                                    Key.DirectionCenter, Key.Enter -> {
                                        when {
                                            renameText.text.isEmpty() -> {
                                                onRename(app.activityPackage, "RESET_TO_ORIGINAL")
                                            }
                                            renameText.text.trim() == originalRenameText -> {
                                                // Close: no changes
                                            }
                                            else -> {
                                                onRename(app.activityPackage, renameText.text.trim())
                                            }
                                        }
                                        onCloseMenu()
                                        true
                                    }
                                    Key.Escape, Key.Back -> {
                                        onCloseMenu()
                                        true
                                    }
                                    else -> false
                                }
                            }
                            .border(
                                width = 2.dp,
                                color = saveBorderColor,
                                shape = saveButtonShape
                            )
                            .background(
                                color = saveBgColor,
                                shape = saveButtonShape
                            )
                            .combinedClickable(
                                onClick = {
                                    when {
                                        renameText.text.isEmpty() -> {
                                            // Reset: pass special marker to clear custom label
                                            onRename(app.activityPackage, "RESET_TO_ORIGINAL")
                                        }
                                        renameText.text.trim() == originalRenameText -> {
                                            // Close: no changes, just close
                                        }
                                        else -> {
                                            // Save: apply the new name
                                            onRename(app.activityPackage, renameText.text.trim())
                                        }
                                    }
                                    onCloseMenu()
                                },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = saveButtonText,
                            color = saveTextColor,
                            fontSize = (uiState.appDrawerSize * 0.8f).sp
                        )
                    }
                }
            }
            else -> {
                // App name (normal state)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer(clip = false)
                            .drawBehind {
                                if (isSelected && isDpadMode) {
                                    val highlightWidth = size.width + (highlightPaddingPx * 2)
                                    val cornerRadius = ShapeHelper.getCornerRadius(
                                        textIslandsShape = uiState.textIslandsShape,
                                        height = size.height,
                                        density = density
                                    )
                                    drawRoundRect(
                                        color = textColor,
                                        topLeft = Offset(-highlightPaddingPx, 0f),
                                        size = Size(highlightWidth.coerceAtLeast(0f), size.height),
                                        cornerRadius = cornerRadius
                                    )
                                }
                            }
                    ) {
                        Text(
                            text = labelWithCloneIndicator,
                            color = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                            fontSize = uiState.appDrawerSize.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = appFontFamily
                        )
                    }
                    // Show NewReleases icon for newly installed apps
                    if (isNewlyInstalled) {
                        Icon(
                            imageVector = Icons.Rounded.NewReleases,
                            contentDescription = "Newly installed",
                            tint = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                            modifier = Modifier.size((uiState.appDrawerSize * 0.8f).dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pure UI component for inline context menu buttons
 */
@Composable
fun InlineContextButton(
    icon: ImageVector,
    enabled: Boolean,
    uiState: AppsDrawerUiState,
    isFocused: Boolean = false,
    sizeOverride: androidx.compose.ui.unit.Dp? = null,
    onClick: () -> Unit
) {
    val iconSize = (uiState.appDrawerSize * 2f).dp
    val buttonSize = sizeOverride ?: iconSize
    
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background

    // Calculate shape based on textIslandsShape preference
    val buttonShape = remember(uiState.textIslandsShape) {
        when (uiState.textIslandsShape) {
            0 -> CircleShape
            1 -> androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            else -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
        }
    }

    // Behavior:
    // - Normal (enabled, not focused): Shape with transparent background, 2dp textColor border, textColor icon
    // - Focused (DPAD mode): Shape with solid textColor fill, 2dp textColor border, backgroundColor icon
    // - Disabled: Shape with transparent background, 2dp textColor border (0.5 alpha), textColor icon (0.5 alpha)
    val backgroundColor = when {
        !enabled -> Color.Transparent
        isFocused -> textColor
        else -> Color.Transparent
    }
    val iconColor = when {
        !enabled -> textColor.copy(alpha = 0.25f)
        isFocused -> bgColor
        else -> textColor
    }
    val borderColor = if (!enabled) textColor.copy(alpha = 0.25f) else textColor
    
    Box(
        modifier = Modifier
            .size(buttonSize)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = buttonShape
            )
            .background(
                color = backgroundColor,
                shape = buttonShape
            )
            .combinedClickable(
                enabled = enabled,
                        onClick = { if (enabled) {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onClick()
                        } },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        val innerIconSize = (buttonSize * 0.6f)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(innerIconSize)
        )
    }
}

/**
 * Pure UI component for placeholder items
 */
@Composable
fun AppPlaceholderItem(uiState: AppsDrawerUiState) {
    val boxAlignment = when (uiState.appDrawerAlignment) {
        1 -> Alignment.Center
        2 -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // When AZ filter is shown, add padding on the side where AZ filter is located
                // Right alignment (2) -> AZ on left -> start padding; otherwise -> AZ on right -> end padding
                if (uiState.appDrawerAzFilter) {
                    if (uiState.appDrawerAlignment == 2) Modifier.padding(start = azSidebarWidth)
                    else Modifier.padding(end = azSidebarWidth)
                } else {
                    Modifier
                }
            )
            .padding(vertical = uiState.appDrawerGap.dp),
        contentAlignment = boxAlignment
    ) {
        val ctx = LocalContext.current
        val customPath = uiState.customFontPath
        val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
        val appFontFamily = remember(appTypefaceNullable) {
            if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
        }

        Text(
            text = "placeholder",
            color = Color.Transparent,
            fontSize = uiState.appDrawerSize.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = appFontFamily
        )
    }
}

// PageIndicator moved to `HomeUI.kt` so Home UI owns the component implementation.
