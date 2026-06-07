package com.github.gezimos.inkos.ui.compose

import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.common.isSystemApp
import com.github.gezimos.inkos.AppsDrawerUiState
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.helper.IconUtility
import com.github.gezimos.inkos.helper.IconShape
import androidx.compose.ui.res.stringResource
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.helper.IconShapeUtility
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.AppsDrawerState
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
@Composable
fun AppsDrawerLayout(
    state: AppsDrawerState,
    uiState: AppsDrawerUiState,
    displayApps: List<AppListItem>,
    paddedDisplayApps: List<AppListItem?>,
    isCalculated: Boolean,
    searchFocusRequester: FocusRequester,
    onAppClick: (AppListItem) -> Unit,
    onAppLongClick: (AppListItem) -> Unit,
    onRename: (String, String) -> Unit,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    onSearchAction: () -> Unit,
    onInteract: () -> Unit,
    onCloseMenu: () -> Unit,
    onFavoriteToggle: ((AppListItem) -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    selectedFavoritesCount: Int = 0,
    maxFavorites: Int = 0,
    isFavoriteChecked: ((AppListItem) -> Boolean)? = null,
    onReorderFavorites: ((Int, Int) -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    homePagesNum: Int = 1
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val screenScale = rememberScreenScale()
    // Compute after textMeasurer
    val isSearchActive = !state.isHiddenAppsMode && state.searchQuery.text.isNotBlank()
    val customPath = uiState.customFontPath
    val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
    val appFontFamily = remember(appTypefaceNullable) {
        if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
    }

    // Compute icon height once for all items
    val textMeasurer = rememberTextMeasurer()
    val prefs = remember(ctx) { com.github.gezimos.inkos.data.Prefs(ctx) }
    val iconHeight: Dp = remember(uiState.appDrawerSize, appFontFamily, screenScale, density) {
        try {
            val measuredHeight = textMeasurer.measure(
                text = AnnotatedString("Ag"),
                style = TextStyle(fontSize = uiState.appDrawerSize.sp.scaled(screenScale), fontFamily = appFontFamily)
            ).size.height
            with(density) { measuredHeight.toDp() }
        } catch (_: Exception) {
            uiState.appDrawerSize.dp
        }
    }
    LaunchedEffect(iconHeight, density) {
        prefs.cachedDrawerIconSizePx = with(density) { iconHeight.toPx().toInt() }.coerceAtLeast(1)
    }

    val rowHeightPx = remember(iconHeight, uiState.appDrawerGap, density) {
        with(density) { (iconHeight + uiState.appDrawerGap.dp * 2).toPx() }
    }

    if (state.flag == com.github.gezimos.inkos.data.Constants.AppDrawerFlag.EditFavorites) {
        OneTimeTooltip(
            key = "tooltip_edit_favorites_shown",
            title = "Edit Favorites",
            lines = listOf(
                "Tap the numbers on top to change the number of apps/pages.",
                "Drag the = icon to re-order apps."
            )
        )
    }

    if (state.flag == com.github.gezimos.inkos.data.Constants.AppDrawerFlag.HiddenApps) {
        OneTimeTooltip(
            key = "tooltip_hidden_apps_shown",
            title = "Hidden Apps",
            lines = listOf(
                "Tap the title to multi-select apps to hide or unhide."
            )
        )
    }

    if (state.flag == com.github.gezimos.inkos.data.Constants.AppDrawerFlag.EditHiddenApps) {
        OneTimeTooltip(
            key = "tooltip_edit_hidden_apps_shown",
            title = "Hide Apps",
            lines = listOf(
                "Tap apps to hide or unhide them, then tap Done."
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
            if (!isCalculated) {
                // Block interactions during calculation
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
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
                        fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                        fontFamily = appFontFamily
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
                        fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                        fontFamily = appFontFamily
                    )
                }
            } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        userScrollEnabled = false
                    ) {
                        if (state.flag == Constants.AppDrawerFlag.SetHomeApp || state.flag == Constants.AppDrawerFlag.HiddenApps || state.flag == Constants.AppDrawerFlag.EditFavorites || state.flag == Constants.AppDrawerFlag.EditHiddenApps) {
                            item(key = "title") {
                                val titleText = when (state.flag) {
                                    Constants.AppDrawerFlag.SetHomeApp -> "SELECT APP"
                                    Constants.AppDrawerFlag.HiddenApps -> "HIDDEN APPS"
                                    Constants.AppDrawerFlag.EditFavorites -> "$selectedFavoritesCount/$maxFavorites apps"
                                    Constants.AppDrawerFlag.EditHiddenApps -> "$selectedFavoritesCount hidden"
                                    else -> ""
                                }

                                val hasDone = onDone != null
                                val titleClickable = onTitleClick != null
                                // HiddenApps: title-click affordance rendered as an edit icon (no Done button)
                                val showEditIcon = titleClickable && !hasDone
                                val isEditTitle = state.flag == Constants.AppDrawerFlag.EditFavorites || state.flag == Constants.AppDrawerFlag.EditHiddenApps
                                val isRightAlignedTitle = uiState.appDrawerAlignment == 2
                                val titleArrangement = when {
                                    hasDone || showEditIcon -> Arrangement.SpaceBetween
                                    uiState.appDrawerAlignment == 1 -> Arrangement.Center
                                    isRightAlignedTitle -> Arrangement.End
                                    else -> Arrangement.Start
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = uiState.appDrawerGap.dp),
                                    horizontalArrangement = titleArrangement,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val headerHighlightColor = Theme.colors.text
                                    val headerBgColor = Theme.colors.background
                                    val headerPadPx = with(density) { 8.dp.toPx() }
                                    fun headerHighlightMod(focused: Boolean): Modifier = if (focused && state.isDpadMode) {
                                        Modifier
                                            .graphicsLayer(clip = false)
                                            .drawBehind {
                                                val w = size.width + headerPadPx * 2
                                                val cr = ShapeHelper.getCornerRadius(
                                                    textIslandsShape = uiState.textIslandsShape,
                                                    height = size.height,
                                                    density = density
                                                )
                                                drawRoundRect(
                                                    color = headerHighlightColor,
                                                    topLeft = Offset(-headerPadPx, 0f),
                                                    size = Size(w.coerceAtLeast(0f), size.height),
                                                    cornerRadius = cr
                                                )
                                            }
                                    } else Modifier
                                    val doneTextColor = if (state.doneFocused && state.isDpadMode) headerBgColor else Theme.colors.text
                                    val doneHighlightMod = headerHighlightMod(state.doneFocused)
                                    val titleTextColor = if (state.titleFocused && state.isDpadMode) headerBgColor else Theme.colors.text
                                    val titleHighlightMod = headerHighlightMod(state.titleFocused)
                                    val editIconTint = if (state.titleFocused && state.isDpadMode) headerBgColor else Theme.colors.text
                                    val editIcon = @Composable {
                                        Box(
                                            modifier = Modifier
                                                .size(with(density) { uiState.appDrawerSize.sp.scaled(screenScale).toDp() + 8.dp })
                                                .graphicsLayer(clip = false)
                                                .drawBehind {
                                                    if (state.titleFocused && state.isDpadMode) {
                                                        drawRoundRect(
                                                            color = headerHighlightColor,
                                                            cornerRadius = CornerRadius(4.dp.toPx())
                                                        )
                                                    }
                                                }
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) { onTitleClick!!.invoke() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Edit hidden apps",
                                                tint = editIconTint,
                                                modifier = Modifier.size(with(density) { uiState.appDrawerSize.sp.scaled(screenScale).toDp() })
                                            )
                                        }
                                    }
                                    // Right-aligned: Done / edit icon on left
                                    if (hasDone && isRightAlignedTitle) {
                                        Text(
                                            text = "Done",
                                            color = doneTextColor,
                                            fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                            maxLines = 1,
                                            fontFamily = appFontFamily,
                                            modifier = doneHighlightMod.clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { onDone!!() }
                                        )
                                    }
                                    if (showEditIcon && isRightAlignedTitle) {
                                        editIcon()
                                    }
                                    Text(
                                        text = titleText,
                                        color = if (isEditTitle) titleTextColor else Theme.colors.text,
                                        fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = appFontFamily,
                                        modifier = if (titleClickable && hasDone) {
                                            titleHighlightMod.clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { onTitleClick!!.invoke() }
                                        } else Modifier
                                    )
                                    if (showEditIcon && !isRightAlignedTitle) {
                                        editIcon()
                                    }
                                    // Left/center: Done on right
                                    if (hasDone && !isRightAlignedTitle) {
                                        Text(
                                            text = "Done",
                                            color = doneTextColor,
                                            fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                            maxLines = 1,
                                            fontFamily = appFontFamily,
                                            modifier = doneHighlightMod.clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { onDone!!() }
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (state.searchEnabled && !state.isHiddenAppsMode) {
                            item(key = "search_field") {
                                SearchField(
                                    searchQuery = state.searchQuery,
                                    onSearchQueryChange = onSearchQueryChange,
                                    uiState = uiState,
                                    appFontFamily = appFontFamily,
                                    focusRequester = searchFocusRequester,
                                    onInteract = onInteract,
                                    onSearchAction = onSearchAction,
                                    isDpadFocused = state.searchFocused,
                                    azFilterLetter = state.azFilterLetter,
                                    onClearAzFilter = {
                                        state.azFilterLetter = null
                                        state.azFilterSelectedIndex = 0
                                    }
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
                                val isNewlyInstalled = uiState.newlyInstalledApps.contains(app.activityPackage)

                                val isSelectionMode = state.isEditFavoritesMode || state.isEditHiddenMode
                                val isChecked = if (isSelectionMode) isFavoriteChecked?.invoke(app) ?: false else false
                                val dimWhenMaxAndUnselected = state.isEditFavoritesMode && selectedFavoritesCount >= maxFavorites && !isChecked
                                val isDragging = state.isEditFavoritesMode && state.draggingIndex != null && state.selectedFavorites.getOrNull(state.draggingIndex!!)?.let { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass } == true
                                val favIndex = if (isChecked) state.selectedFavorites.indexOfFirst { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass } else -1
                                val showPageSep = state.isEditFavoritesMode && homePagesNum > 1 && maxFavorites > 0 && isChecked && favIndex >= 0 && run {
                                    val appsPerPage = maxFavorites / homePagesNum
                                    appsPerPage > 0 && (favIndex + 1) % appsPerPage == 0 && (favIndex + 1) < maxFavorites
                                }
                                val sepColor = if (showPageSep) Theme.colors.text.copy(alpha = 0.3f) else Color.Transparent
                                val sepMod = if (showPageSep) Modifier.drawBehind {
                                    val dashW = 6.dp.toPx()
                                    val gap = 4.dp.toPx()
                                    val sw = 1.dp.toPx()
                                    var x = 0f
                                    while (x < size.width) {
                                        drawLine(sepColor, Offset(x, size.height), Offset((x + dashW).coerceAtMost(size.width), size.height), sw)
                                        x += dashW + gap
                                    }
                                } else Modifier

                                AppItem(
                                    app = app,
                                    uiState = uiState,
                                    appFontFamily = appFontFamily,
                                    iconHeight = iconHeight,
                                    isSelected = (index == state.selectedItemIndex),
                                    isDpadMode = state.isDpadMode && !state.azFilterFocused && !state.doneFocused && !state.titleFocused && !state.dragHandleFocused,
                                    showRename = (state.renameApp == app && state.showRenameOverlay),
                                    isWorkProfile = isWorkProfile,
                                    isNewlyInstalled = isNewlyInstalled,
                                    onClick = {
                                        if (isSelectionMode) {
                                            onFavoriteToggle?.invoke(app)
                                        } else {
                                            onAppClick(app)
                                        }
                                    },
                                    onLongClick = { onAppLongClick(app) },
                                    onRename = onRename,
                                    onCloseMenu = onCloseMenu,
                                    isSelectionMode = isSelectionMode,
                                    isFavoriteChecked = isChecked,
                                    dimWhenMaxAndUnselected = dimWhenMaxAndUnselected,
                                    showDragHandle = state.isEditFavoritesMode && isChecked,
                                    isDragging = isDragging,
                                    dragOffsetY = state.dragOffsetY,
                                    onDragStart = if (state.isEditFavoritesMode && isChecked) {
                                        { state.draggingIndex = state.selectedFavorites.indexOfFirst { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass } }
                                    } else null,
                                    onDragEnd = if (state.isEditFavoritesMode && isChecked) {
                                        {
                                            state.draggingIndex?.let { fromIndex ->
                                                val deltaIndex = (state.dragOffsetY / rowHeightPx).roundToInt()
                                                val targetIndex = (fromIndex + deltaIndex).coerceIn(0, state.selectedFavorites.size - 1)
                                                if (targetIndex != fromIndex && fromIndex in state.selectedFavorites.indices) {
                                                    val item = state.selectedFavorites.removeAt(fromIndex)
                                                    state.selectedFavorites.add(targetIndex.coerceAtMost(state.selectedFavorites.size), item)
                                                    onReorderFavorites?.invoke(fromIndex, targetIndex)
                                                }
                                            }
                                            state.draggingIndex = null
                                            state.dragOffsetY = 0f
                                        }
                                    } else null,
                                    onDrag = if (state.isEditFavoritesMode && isChecked) {
                                        { dragAmount -> state.dragOffsetY += dragAmount }
                                    } else null,
                                    isDragHandleFocused = state.dragHandleFocused && index == state.selectedItemIndex && isChecked && state.dpadGrabbedIndex == null,
                                    isDpadGrabbed = state.dpadGrabbedIndex != null && state.dpadGrabbedIndex == favIndex,
                                    onDragHandleTap = if (state.isEditFavoritesMode && isChecked) { {
                                        if (state.dpadGrabbedIndex != null) {
                                            state.dpadGrabbedIndex = null
                                            state.draggingIndex = null
                                            state.dragOffsetY = 0f
                                        } else {
                                            if (favIndex >= 0) {
                                                state.dpadGrabbedIndex = favIndex
                                                state.draggingIndex = favIndex
                                            }
                                        }
                                    } } else null,
                                    isLastItem = index == paddedDisplayApps.lastIndex,
                                    modifier = sepMod
                                )
                            } else {
                                AppPlaceholderItem(uiState = uiState, appFontFamily = appFontFamily, isLastItem = index == paddedDisplayApps.lastIndex)
                            }
                        }
                    }

                }
            }

    }
@Composable
private fun SearchField(
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    uiState: AppsDrawerUiState,
    appFontFamily: FontFamily,
    focusRequester: FocusRequester,
    onInteract: (() -> Unit)? = null,
    onSearchAction: (() -> Unit)? = null,
    isDpadFocused: Boolean = false,
    azFilterLetter: Char? = null,
    onClearAzFilter: (() -> Unit)? = null
) {
    val screenScale = rememberScreenScale()
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

    val isAzMode = azFilterLetter != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (uiState.appDrawerAzFilter) {
                    if (uiState.appDrawerAlignment == 2) Modifier.padding(start = azSidebarWidth.scaled(rememberScreenScale()))
                    else Modifier.padding(end = azSidebarWidth.scaled(rememberScreenScale()))
                } else {
                    Modifier
                }
            )
            .padding(bottom = uiState.appDrawerGap.dp)
            ,
        contentAlignment = boxAlignment
    ) {
        if (isAzMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onClearAzFilter?.invoke() },
                contentAlignment = boxAlignment
            ) {
                Text(
                    text = azFilterLetter.toString(),
                    color = Theme.colors.text,
                    fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                    fontFamily = appFontFamily
                )
            }
        } else {
        // Adapt layout to alignment
        val alignmentPref = uiState.appDrawerAlignment
        val textAlign = if (alignmentPref == 2) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start
        val prefixChar = if (alignmentPref == 2) "<" else ">"
        val showPrefix = !isFocused && searchQuery.text.isEmpty()
        val basicTextStyle = TextStyle(
            color = Theme.colors.text,
            fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
            fontFamily = appFontFamily,
            textAlign = textAlign
        )

        CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
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
                when (alignmentPref) {
                    1 -> {
                        // Centered: keep prefix then editable area
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            if (showPrefix) {
                                Text(
                                    text = prefixChar,
                                    color = Theme.colors.text,
                                    fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                    fontFamily = appFontFamily
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.text.isEmpty() && !isFocused) {
                                    Text(
                                        text = "_",
                                        color = Theme.colors.text,
                                        fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                        fontFamily = appFontFamily,
                                        textAlign = textAlign
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                    2 -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.text.isEmpty() && !isFocused) {
                                    Text(
                                        text = "_",
                                        color = Theme.colors.text,
                                        fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                        fontFamily = appFontFamily,
                                        textAlign = textAlign,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    innerTextField()
                                }
                            }
                            if (showPrefix) {
                                Text(
                                    text = prefixChar,
                                    color = Theme.colors.text,
                                    fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                    fontFamily = appFontFamily
                                )
                            }
                        }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showPrefix) {
                                Text(
                                    text = prefixChar,
                                    color = Theme.colors.text,
                                    fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                    fontFamily = appFontFamily
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.text.isEmpty() && !isFocused) {
                                    Text(
                                        text = "_",
                                        color = Theme.colors.text,
                                        fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
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
        } // else (not azMode)
    }
}
/**
 * Helper data class to hold app type flags - only computed when context menu is shown
 */
private data class AppTypeFlags(
    val isSystemApp: Boolean,
    val isPinnedShortcut: Boolean = false,
    val isAppShortcut: Boolean = false,
    val isContact: Boolean = false,
    val isWebSearch: Boolean = false
)
@Composable
private fun computeAppTypeFlags(app: AppListItem, context: android.content.Context): AppTypeFlags {
    return remember(app.activityPackage, app.shortcutId) {
        val isContact = app.activityPackage.startsWith(Constants.INTERNAL_CONTACT_PREFIX)
        val isWebSearch = app.activityPackage == Constants.INTERNAL_WEB_SEARCH
        val isInternalPackage = isContact || isWebSearch || app.activityPackage.startsWith("com.inkos.")
        val isSystemApp = if (isInternalPackage) false else context.isSystemApp(app.activityPackage)

        val isAppShortcut = app.shortcutId != null

        val isPinnedShortcut = if (isAppShortcut) {
            com.github.gezimos.inkos.helper.PinnedShortcutUtility.isPinned(context, app)
        } else {
            false
        }

        AppTypeFlags(isSystemApp, isPinnedShortcut, isAppShortcut, isContact, isWebSearch)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppListItem,
    uiState: AppsDrawerUiState,
    appFontFamily: FontFamily = FontFamily.Default,
    iconHeight: Dp = 0.dp,
    isSelected: Boolean = false,
    isDpadMode: Boolean = false,
    showRename: Boolean = false,
    isWorkProfile: Boolean = false,
    isNewlyInstalled: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: (String, String) -> Unit,
    onCloseMenu: () -> Unit,
    isSelectionMode: Boolean = false,
    isFavoriteChecked: Boolean = false,
    dimWhenMaxAndUnselected: Boolean = false,
    showDragHandle: Boolean = false,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    onDragStart: (() -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDrag: ((Float) -> Unit)? = null,
    isDragHandleFocused: Boolean = false,
    isDpadGrabbed: Boolean = false,
    onDragHandleTap: (() -> Unit)? = null,
    isLastItem: Boolean = false,
    modifier: Modifier = Modifier
) {
    val screenScale = rememberScreenScale()
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
    val modeIsFullBleed = remember(uiState.iconSourceMode) { IconShapeUtility.isFullBleedMode(uiState.iconSourceMode) }

    val initialText = if (app.customLabel.isNotEmpty()) app.customLabel else app.label
    var renameText by remember(app.activityPackage, app.shortcutId, showRename) {
        mutableStateOf(
            TextFieldValue(
                text = if (showRename) initialText else "",
                selection = if (showRename) androidx.compose.ui.text.TextRange(initialText.length) else androidx.compose.ui.text.TextRange.Zero
            )
        )
    }
    var originalRenameText by remember(app.activityPackage, app.shortcutId, showRename) { mutableStateOf(if (showRename) initialText else "") }
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
    val highlightPaddingPx = with(density) { 8.dp.toPx() }
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val invertedHighlightTextColor = bgColor
    val scaledIconTextSize = uiState.appDrawerSize.toFloat() * screenScale

    val isContactOrWebSearch = remember(app.activityPackage) {
        app.activityPackage.startsWith(Constants.INTERNAL_CONTACT_PREFIX) ||
            app.activityPackage == Constants.INTERNAL_WEB_SEARCH
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = if (isDragging) dragOffsetY else 0f
                alpha = if (dimWhenMaxAndUnselected) 0.5f else 1f
            }
            .then(
                // AZ filter padding
                if (uiState.appDrawerAzFilter && !isSelectionMode) {
                    if (uiState.appDrawerAlignment == 2) Modifier.padding(start = azSidebarWidth.scaled(rememberScreenScale()))
                    else Modifier.padding(end = azSidebarWidth.scaled(rememberScreenScale()))
                } else {
                    Modifier
                }
            )
            .padding(top = uiState.appDrawerGap.dp, bottom = uiState.appDrawerGap.dp)
            .then(if (iconHeight > 0.dp && !showRename) Modifier.height(iconHeight) else Modifier)
            .then(
                if (isSelectionMode) {
                    // Edit mode: click to toggle
                    Modifier.clickable(
                        onClick = onClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                } else if (!showRename) {
                    // No context menu for contacts/web search
                    if (isContactOrWebSearch) {
                        Modifier.clickable(
                            onClick = onClick,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    } else {
                        Modifier.combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = boxAlignment
    ) {
        when {
            showRename -> {
                // Track if save button is focused (for DPAD mode)
                var saveButtonFocused by remember { mutableStateOf(false) }
                val saveButtonFocusRequester = remember { FocusRequester() }
                
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
                            fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                            fontFamily = appFontFamily
                        ),
                        singleLine = true,
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Theme.colors.text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                when {
                                    renameText.text.isEmpty() -> {
                                        onRename(app.activityPackage + (app.shortcutId?.let { "|$it" } ?: ""), "RESET_TO_ORIGINAL")
                                    }
                                    renameText.text.trim() != originalRenameText -> {
                                        onRename(app.activityPackage + (app.shortcutId?.let { "|$it" } ?: ""), renameText.text.trim())
                                    }
                                }
                                onCloseMenu()
                            }
                        )
                    )
                    
                    // Use same color logic as context menu buttons:
                    val saveBgColor = if (saveButtonFocused) textColor else Color.Transparent
                    val saveTextColor = if (saveButtonFocused) bgColor else textColor
                    val saveBorderColor = textColor
                    
                    // Shape from textIslandsShape
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
                                                onRename(app.activityPackage + (app.shortcutId?.let { "|$it" } ?: ""), "RESET_TO_ORIGINAL")
                                            }
                                            renameText.text.trim() == originalRenameText -> {
                                            }
                                            else -> {
                                                onRename(app.activityPackage + (app.shortcutId?.let { "|$it" } ?: ""), renameText.text.trim())
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
                                            onRename(app.activityPackage + (app.shortcutId?.let { "|$it" } ?: ""), "RESET_TO_ORIGINAL")
                                        }
                                        renameText.text.trim() == originalRenameText -> {
                                        }
                                        else -> {
                                            onRename(app.activityPackage + (app.shortcutId?.let { "|$it" } ?: ""), renameText.text.trim())
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
                            fontSize = (uiState.appDrawerSize * 0.8f).sp.scaled(screenScale),
                            fontFamily = appFontFamily
                        )
                    }
                }
            }
            else -> {
                val rowArrangement = when {
                    isSelectionMode && uiState.appDrawerAlignment == 1 -> Arrangement.spacedBy(4.dp, Alignment.Start)
                    uiState.appDrawerAlignment == 1 -> Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                    uiState.appDrawerAlignment == 2 -> Arrangement.spacedBy(4.dp, Alignment.End)
                    else -> Arrangement.spacedBy(4.dp, Alignment.Start)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = rowArrangement,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isRightAligned = uiState.appDrawerAlignment == 2
                    // Right-aligned: drag handle on left
                    if (showDragHandle && isRightAligned) {
                        val handleSize = uiState.appDrawerSize.dp
                        Box(
                            modifier = Modifier
                                .size(handleSize)
                                .graphicsLayer(clip = false)
                                .drawBehind {
                                    if (isDpadGrabbed) {
                                        drawRoundRect(
                                            color = textColor,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                    } else if (isDragHandleFocused) {
                                        drawRoundRect(
                                            color = textColor,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onDragHandleTap?.invoke() }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { onDragStart?.invoke() },
                                        onDragEnd = { onDragEnd?.invoke() },
                                        onDragCancel = { onDragEnd?.invoke() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDrag?.invoke(dragAmount.y)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = if (isDpadGrabbed) bgColor else textColor,
                                modifier = Modifier.size(handleSize * 0.8f)
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }
                    // Checkbox left/center
                    if (isSelectionMode && !isRightAligned) {
                        val checkboxSize = uiState.appDrawerSize.dp.coerceAtLeast(20.dp)
                        Icon(
                            imageVector = if (isFavoriteChecked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (isFavoriteChecked) "Selected" else "Not selected",
                            tint = textColor,
                            modifier = Modifier
                                .size(checkboxSize)
                                .padding(end = 8.dp)
                        )
                    }
                    if (uiState.drawerShowIcons && !isRightAligned && uiState.appDrawerAlignment != 1 && !Constants.isSeparator(app.activityPackage)) {
                        val resolvedIconShape = remember(uiState.iconShape) {
                            IconShape.fromPreference(uiState.iconShape)
                        }
                        AppIconBox(
                            iconSourceMode = uiState.iconSourceMode,
                            iconShape = resolvedIconShape,
                            size = iconHeight * 1.25f,
                            showBackground = isSelected && isDpadMode && !modeIsFullBleed,
                            backgroundColor = textColor,
                            showBorder = true,
                            borderColor = textColor,
                            paddingEnd = 10.dp
                        ) {
                            DrawerAppIconContent(
                                packageName = app.activityPackage,
                                label = displayText,
                                iconSourceMode = uiState.iconSourceMode,
                                selectedIconPackPackage = uiState.selectedIconPackPackage,
                                appNameHeight = iconHeight * 1.25f,
                                appTextSize = scaledIconTextSize,
                                labelColor = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                                labelFontFamily = appFontFamily,
                                iconShape = if (IconShapeUtility.shouldClipBitmap(uiState.iconSourceMode))
                                    IconShapeUtility.getComposeShape(resolvedIconShape, pillRadius = (iconHeight * 1.25f) / 2)
                                else null,
                                iconShapeId = uiState.iconShape,
                                activityClass = app.activityClass,
                                user = app.user,
                                shortcutId = app.shortcutId,
                                iconTintColor = textColor,
                                iconInvertColor = if (isSelected && isDpadMode) invertedHighlightTextColor else null,
                                iconTintContrast = uiState.iconTintContrast,
                                iconBgColor = bgColor
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .then(if (!isSelectionMode) Modifier.weight(1f, fill = false) else Modifier)
                            .then(
                                if (!isSelectionMode) Modifier.padding(
                                    start = if (uiState.appDrawerAlignment == 2) 14.dp else 0.dp,
                                    end = if (uiState.appDrawerAlignment == 0) 14.dp else 0.dp
                                ) else Modifier
                            )
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
                            fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = appFontFamily
                        )
                    }
                    // Drawer icon right
                    if (uiState.drawerShowIcons && isRightAligned && !Constants.isSeparator(app.activityPackage)) {
                        val resolvedIconShape = remember(uiState.iconShape) {
                            IconShape.fromPreference(uiState.iconShape)
                        }
                        AppIconBox(
                            iconSourceMode = uiState.iconSourceMode,
                            iconShape = resolvedIconShape,
                            size = iconHeight * 1.25f,
                            showBackground = isSelected && isDpadMode && !modeIsFullBleed,
                            backgroundColor = textColor,
                            showBorder = true,
                            borderColor = textColor,
                            paddingStart = 10.dp
                        ) {
                            DrawerAppIconContent(
                                packageName = app.activityPackage,
                                label = displayText,
                                iconSourceMode = uiState.iconSourceMode,
                                selectedIconPackPackage = uiState.selectedIconPackPackage,
                                appNameHeight = iconHeight * 1.25f,
                                appTextSize = scaledIconTextSize,
                                labelColor = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                                labelFontFamily = appFontFamily,
                                iconShape = if (IconShapeUtility.shouldClipBitmap(uiState.iconSourceMode))
                                    IconShapeUtility.getComposeShape(resolvedIconShape, pillRadius = (iconHeight * 1.25f) / 2)
                                else null,
                                iconShapeId = uiState.iconShape,
                                activityClass = app.activityClass,
                                user = app.user,
                                shortcutId = app.shortcutId,
                                iconTintColor = textColor,
                                iconInvertColor = if (isSelected && isDpadMode) invertedHighlightTextColor else null,
                                iconTintContrast = uiState.iconTintContrast,
                                iconBgColor = bgColor
                            )
                        }
                    }
                    // Checkbox right
                    if (isSelectionMode && isRightAligned) {
                        val checkboxSize = uiState.appDrawerSize.dp.coerceAtLeast(20.dp)
                        Icon(
                            imageVector = if (isFavoriteChecked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            contentDescription = if (isFavoriteChecked) "Selected" else "Not selected",
                            tint = textColor,
                            modifier = Modifier
                                .size(checkboxSize)
                                .padding(start = 8.dp)
                        )
                    }
                    if (isNewlyInstalled && !isSelectionMode) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Newly installed",
                            tint = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                            modifier = Modifier.size((uiState.appDrawerSize * 0.8f).dp)
                        )
                    }
                    // Spacer for drag handle
                    if (showDragHandle && !isRightAligned) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                        val handleSize = uiState.appDrawerSize.dp
                        Box(
                            modifier = Modifier
                                .size(handleSize)
                                .graphicsLayer(clip = false)
                                .drawBehind {
                                    if (isDpadGrabbed) {
                                        drawRoundRect(
                                            color = textColor,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                                        )
                                    } else if (isDragHandleFocused) {
                                        drawRoundRect(
                                            color = textColor,
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onDragHandleTap?.invoke() }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { onDragStart?.invoke() },
                                        onDragEnd = { onDragEnd?.invoke() },
                                        onDragCancel = { onDragEnd?.invoke() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDrag?.invoke(dragAmount.y)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = if (isDpadGrabbed) bgColor else textColor,
                                modifier = Modifier.size(handleSize * 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
internal fun DrawerAppIconContent(
    packageName: String,
    label: String,
    iconSourceMode: Int,
    selectedIconPackPackage: String,
    appNameHeight: Dp,
    appTextSize: Float,
    labelColor: Color,
    labelFontFamily: FontFamily,
    iconShape: Shape? = null,
    iconShapeId: Int = -1,
    activityClass: String = "",
    user: android.os.UserHandle? = null,
    shortcutId: String? = null,
    iconTintColor: Color = labelColor,
    iconInvertColor: Color? = null,
    iconTintContrast: Int = 10,
    iconBgColor: Color = Color.Transparent
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { appNameHeight.toPx().toInt() }.coerceAtLeast(1)

    val isSyntheticForIcon = remember(packageName, shortcutId) {
        IconUtility.isSyntheticPackage(packageName) &&
            (shortcutId == null || IconUtility.isInkOSInternalShortcut(shortcutId))
    }
    val isShortcutOverride = shortcutId != null && !isSyntheticForIcon

    // Contact with a photo URI stored in activityClass
    val isContactWithPhoto = remember(packageName, activityClass) {
        packageName.startsWith(Constants.INTERNAL_CONTACT_PREFIX) && activityClass.startsWith("content://")
    }

    if (iconSourceMode == 0) {
        // Mode 0: Letter codes for all apps and shortcuts
        val letterCode = remember(label, packageName) {
            when {
                packageName == Constants.INTERNAL_MUSIC -> "Mu"
                packageName == Constants.INTERNAL_FILES -> "Do"
                else -> IconUtility.generateCodeForLabel(label)
            }
        }
        if (letterCode.isNotEmpty()) {
            val dynamicFontSize = remember(letterCode, appTextSize) {
                val baseSize = appTextSize * 0.7f
                if (letterCode.length > 1) baseSize * 0.85f else baseSize
            }
            Text(
                text = letterCode,
                modifier = Modifier.wrapContentWidth(),
                style = TextStyle(
                    color = labelColor,
                    fontSize = dynamicFontSize.sp,
                    fontFamily = labelFontFamily,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                maxLines = 1
            )
        }
    } else {
        val tintArgb = remember(iconTintColor, iconSourceMode, isContactWithPhoto) {
            if (IconShapeUtility.isTintedMode(iconSourceMode) && !isContactWithPhoto) iconTintColor.toArgb() else 0
        }
        val bgArgb = remember(iconBgColor, iconSourceMode) {
            if (iconSourceMode == 6) iconBgColor.toArgb() else 0
        }
        val contactCacheKey = if (isContactWithPhoto) "contact_photo:$packageName:$sizePx" else null
        val imageBitmap = rememberAppIconBitmap(
            context, packageName, iconSourceMode, selectedIconPackPackage, sizePx,
            activityClass, user, shortcutId, tintArgb, iconShapeId,
            iconTintContrast = iconTintContrast,
            bgArgb = bgArgb,
            customCacheKey = contactCacheKey,
            customLoader = if (isContactWithPhoto) {
                {
                    IconUtility.getCachedBitmap(contactCacheKey!!)?.let { return@let it }
                    val loaded = try {
                        val uri = activityClass.toUri()
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            android.graphics.BitmapFactory.decodeStream(stream)?.let { raw ->
                                if (raw.width != sizePx || raw.height != sizePx) {
                                    val scaled = raw.scale(sizePx, sizePx, true)
                                    if (scaled !== raw) raw.recycle()
                                    scaled
                                } else raw
                            }
                        }
                    } catch (_: Exception) { null }
                        ?: IconUtility.getSyntheticIconBitmap(context, packageName, sizePx)
                    if (loaded != null) IconUtility.cacheBitmapIfAbsent(contactCacheKey, loaded)
                    loaded
                }
            } else null,
            nonCancellable = true
        )
        val effectiveShape = remember(iconShape, isContactWithPhoto) {
            iconShape ?: if (isContactWithPhoto) CircleShape else null
        }
        val clipMod = remember(effectiveShape) {
            if (effectiveShape != null) Modifier.clip(effectiveShape) else Modifier
        }
        val invertFilter = remember(iconInvertColor, iconSourceMode, isContactWithPhoto) {
            if (iconInvertColor != null && IconShapeUtility.isInkOsMode(iconSourceMode) && !isContactWithPhoto)
                cachedTintFilter(iconInvertColor)
            else null
        }

        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = label,
                modifier = Modifier.fillMaxSize().then(clipMod),
                contentScale = ContentScale.Fit,
                colorFilter = invertFilter
            )
        }
    }
}

private data class AppSheetAction(
    val icon: ImageVector,
    val label: String,
    val action: () -> Unit
)
@Composable
fun AppContextMenuSheet(
    app: AppListItem,
    flag: Constants.AppDrawerFlag,
    isLocked: Boolean,
    onDelete: (AppListItem) -> Unit,
    onRename: (String, String) -> Unit,
    onHideShow: (Constants.AppDrawerFlag, AppListItem) -> Unit,
    onLock: (AppListItem) -> Unit,
    onInfo: (AppListItem) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val appTypeFlags = computeAppTypeFlags(app, ctx)

    // Decide hide vs unhide by the app's actual hidden state, not the screen flag —
    // a hidden app can surface in the normal drawer via "search hidden apps".
    val isHidden = remember(app.activityPackage, app.shortcutId, app.user) {
        val hidden = com.github.gezimos.inkos.data.Prefs(ctx).hiddenApps
        val key = if (app.shortcutId != null) {
            "${app.activityPackage}|${app.shortcutId}|${app.user}"
        } else {
            "${app.activityPackage}|${app.user}"
        }
        hidden.contains(key) ||
            hidden.contains("${app.activityPackage}|${app.user}") ||
            hidden.contains(app.activityPackage)
    }

    val menuItemsEnabled = when {
        appTypeFlags.isContact || appTypeFlags.isWebSearch -> listOf(false, false, false, false, false, true)
        appTypeFlags.isPinnedShortcut -> listOf(true, true, true, true, false, true)
        appTypeFlags.isAppShortcut -> listOf(true, true, true, true, false, true)
        else -> listOf(
            !appTypeFlags.isSystemApp,
            true,
            true,
            true,
            true,
            true
        )
    }

    val deleteOrUnpinIcon = if (appTypeFlags.isPinnedShortcut || appTypeFlags.isAppShortcut)
        Icons.Rounded.PushPin else Icons.Rounded.Delete
    val displayName = if (app.customLabel.isNotEmpty()) app.customLabel else app.label

    val actions = buildList {
        if (menuItemsEnabled[0]) add(AppSheetAction(
            deleteOrUnpinIcon,
            if (appTypeFlags.isPinnedShortcut || appTypeFlags.isAppShortcut) stringResource(R.string.unpin_shortcut) else stringResource(R.string.uninstall),
            { onDelete(app); onDismiss() }
        ))
        if (menuItemsEnabled[1]) add(AppSheetAction(
            Icons.Rounded.DriveFileRenameOutline,
            stringResource(R.string.rename),
            { onRename(app.activityPackage + (app.shortcutId?.let { "|$it" } ?: ""), ""); onDismiss() }
        ))
        if (menuItemsEnabled[2]) add(AppSheetAction(
            if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
            if (isHidden) stringResource(R.string.unhide) else stringResource(R.string.hide),
            { onHideShow(flag, app); onDismiss() }
        ))
        if (menuItemsEnabled[3]) add(AppSheetAction(
            if (isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
            if (isLocked) stringResource(R.string.unlock) else stringResource(R.string.lock),
            { onLock(app); onDismiss() }
        ))
        if (menuItemsEnabled[4]) add(AppSheetAction(
            Icons.Rounded.Info,
            stringResource(R.string.app_info),
            { onInfo(app); onDismiss() }
        ))
    }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = displayName.uppercase(),
            style = SettingsTheme.typography.header,
            fontWeight = FontWeight.Bold,
            color = Theme.colors.text,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = app.activityPackage,
            style = SettingsTheme.typography.title,
            fontSize = (SettingsTheme.typography.title.fontSize.value * 0.65f).sp,
            color = Theme.colors.text.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions.forEach { action ->
            SettingsComposable.SettingsHomeItem(
                title = action.label,
                imageVector = action.icon,
                onClick = action.action,
                horizontalPadding = 0.dp,
                verticalPadding = 8.dp,
                focusVerticalPadding = 16.dp
            )
        }
    }
}
@Composable
fun AppPlaceholderItem(uiState: AppsDrawerUiState, appFontFamily: FontFamily = FontFamily.Default, isLastItem: Boolean = false) {
    val screenScale = rememberScreenScale()
    val boxAlignment = when (uiState.appDrawerAlignment) {
        1 -> Alignment.Center
        2 -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (uiState.appDrawerAzFilter) {
                    if (uiState.appDrawerAlignment == 2) Modifier.padding(start = azSidebarWidth.scaled(rememberScreenScale()))
                    else Modifier.padding(end = azSidebarWidth.scaled(rememberScreenScale()))
                } else {
                    Modifier
                }
            )
            .padding(top = uiState.appDrawerGap.dp, bottom = uiState.appDrawerGap.dp),
        contentAlignment = boxAlignment
    ) {
        Text(
            text = "placeholder",
            color = Color.Transparent,
            fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = appFontFamily
        )
    }
}