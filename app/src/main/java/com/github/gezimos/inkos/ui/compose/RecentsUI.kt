package com.github.gezimos.inkos.ui.compose
import com.github.gezimos.inkos.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.AppsDrawerUiState
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.helper.IconShape
import com.github.gezimos.inkos.helper.IconShapeUtility
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.AppsDrawerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
data class RecentAppItem(
    val app: AppListItem,
    val lastUsedTime: Long,
    val totalUsageTime: Long
)
@Composable
fun RecentsLayout(
    state: AppsDrawerState,
    uiState: AppsDrawerUiState,
    recentApps: List<RecentAppItem>,
    paddedRecentApps: List<RecentAppItem?>,
    isCalculated: Boolean,
    showStats: Boolean = false,
    onStatsToggle: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    headerFocused: Boolean = false,
    headerSelectedIndex: Int = 0,
    hasPermission: Boolean = false,
    onAppClick: (RecentAppItem) -> Unit,
    onAppLongClick: (RecentAppItem) -> Unit
) {
    val screenScale = rememberScreenScale()

    OneTimeTooltip(
        key = "tooltip_recents_shown",
        title = "Recents",
        lines = listOf("Tap the title to toggle between Recents and Most Used"),
        trigger = hasPermission
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isCalculated) {
            if (recentApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val ctx = LocalContext.current
                    val customPath = uiState.customFontPath
                    val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
                    val appFontFamily = remember(appTypefaceNullable) {
                        if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
                    }
                    Text(
                        text = stringResource(R.string.no_recent_apps),
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
                    // Recents title
                    item(key = "recents_title") {
                        val ctx = LocalContext.current
                        val customPath = uiState.customFontPath
                        val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
                        val appFontFamily = remember(appTypefaceNullable) {
                            if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
                        }
                        val textColor = Theme.colors.text
                        val bgColor = Theme.colors.background
                        val density = LocalDensity.current
                        val highlightPaddingPx = with(density) { 4.dp.toPx() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = uiState.appDrawerGap.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Single toggleable title on the left
                            Text(
                                text = (if (showStats) stringResource(R.string.option_most_used) else stringResource(R.string.shortcut_recents_short)).uppercase(),
                                color = if (headerFocused && headerSelectedIndex == 0) bgColor else textColor,
                                fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                maxLines = 1,
                                fontFamily = appFontFamily,
                                modifier = Modifier
                                    .graphicsLayer(clip = false)
                                    .drawBehind {
                                        if (headerFocused && headerSelectedIndex == 0) {
                                            val w = size.width + highlightPaddingPx * 2
                                            drawRoundRect(
                                                color = textColor,
                                                topLeft = Offset(-highlightPaddingPx, 0f),
                                                size = Size(w, size.height),
                                                cornerRadius = CornerRadius(4.dp.toPx())
                                            )
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = { onStatsToggle() },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                            )

                            // Settings cog on the right
                            Box(
                                modifier = Modifier
                                    .size(with(density) { uiState.appDrawerSize.sp.scaled(screenScale).toDp() + 8.dp })
                                    .graphicsLayer(clip = false)
                                    .drawBehind {
                                        if (headerFocused && headerSelectedIndex == 1) {
                                            drawRoundRect(
                                                color = textColor,
                                                cornerRadius = CornerRadius(4.dp.toPx())
                                            )
                                        }
                                    }
                                    .combinedClickable(
                                        onClick = onSettingsClick,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Recents settings",
                                    tint = if (headerFocused && headerSelectedIndex == 1) bgColor else textColor,
                                    modifier = Modifier
                                        .size(with(density) { uiState.appDrawerSize.sp.scaled(screenScale).toDp() })
                                )
                            }
                        }
                    }
                    
                    itemsIndexed(
                        items = paddedRecentApps,
                        key = { index, recentApp ->
                            recentApp?.let { "${it.app.activityPackage}_${it.app.activityClass}_${it.app.user.hashCode()}_$index" }
                                ?: "placeholder_${state.currentPage}_$index"
                        }
                    ) { index, recentApp ->
                        if (recentApp != null) {
                            RecentAppItem(
                                recentApp = recentApp,
                                uiState = uiState,
                                showStats = showStats,
                                isSelected = (index == state.selectedItemIndex),
                                isDpadMode = state.isDpadMode,
                                onClick = { onAppClick(recentApp) },
                                onLongClick = { onAppLongClick(recentApp) },
                                isLastItem = index == paddedRecentApps.lastIndex
                            )
                        } else {
                            RecentAppPlaceholderItem(uiState = uiState, isLastItem = index == paddedRecentApps.lastIndex)
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAppItem(
    recentApp: RecentAppItem,
    uiState: AppsDrawerUiState,
    showStats: Boolean = false,
    isSelected: Boolean = false,
    isDpadMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isLastItem: Boolean = false
) {
    val screenScale = rememberScreenScale()
    val app = recentApp.app
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
    val isOtherProfile = remember(app.user) {
        app.user != android.os.Process.myUserHandle()
    }
    val labelWithCloneIndicator = remember(finalText, isOtherProfile) {
        if (isOtherProfile) "$finalText^" else finalText
    }

    val isRightAligned = uiState.appDrawerAlignment == 2
    val isCenterAligned = uiState.appDrawerAlignment == 1
    val rowArrangement = remember(uiState.appDrawerAlignment) {
        when (uiState.appDrawerAlignment) {
            1 -> Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
            2 -> Arrangement.spacedBy(4.dp, Alignment.End)
            else -> Arrangement.spacedBy(4.dp, Alignment.Start)
        }
    }
    
    val ctx = LocalContext.current
    val customPath = uiState.customFontPath
    val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
    val appFontFamily = remember(appTypefaceNullable) {
        if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
    }
    val density = LocalDensity.current
    val highlightPaddingPx = with(density) { 8.dp.toPx() }
    val textColor = Theme.colors.text
    val bgColor = Theme.colors.background
    val invertedHighlightTextColor = bgColor
    
    val timeText: String = if (showStats) {
        when (uiState.recentsUsageUnit) {
            1 -> { // Money: hours × hourlyRate, e.g. "$42"
                val totalHours = recentApp.totalUsageTime / (1000.0 * 60 * 60)
                val value = (totalHours * uiState.recentsUnitCost).toInt()
                "${uiState.recentsUnitCurrencyChar}$value"
            }
            2 -> { // Coffee: (hours × hourlyRate) / coffeePrice, e.g. "14☕"
                val totalHours = recentApp.totalUsageTime / (1000.0 * 60 * 60)
                val coffeePrice = uiState.recentsUnitCoffeePrice.coerceAtLeast(1)
                val coffees = (totalHours * uiState.recentsUnitCost / coffeePrice).toInt()
                "$coffees ${uiState.recentsUnitEmojiChar}"
            }
            else -> { // Time (default)
                val totalMinutes = recentApp.totalUsageTime / (1000 * 60)
                when {
                    totalMinutes < 60 -> "${totalMinutes}m"
                    totalMinutes < 1440 -> "${totalMinutes / 60}h"
                    else -> "${totalMinutes / 1440}d"
                }
            }
        }
    } else {
        // Recents mode: show last used time
        val now = System.currentTimeMillis()
        val diff = now - recentApp.lastUsedTime
        when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                sdf.format(Date(recentApp.lastUsedTime))
            }
        }
    }

    val showLeaderDots = showStats && uiState.recentsUsageUnit != 0

    val textMeasurer = rememberTextMeasurer()
    val appNameHeight = remember(uiState.appDrawerSize, appFontFamily, screenScale, textMeasurer) {
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = uiState.appDrawerGap.dp, bottom = uiState.appDrawerGap.dp)
            .then(if (uiState.drawerShowIcons && appNameHeight > 0.dp) Modifier.height(appNameHeight) else Modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
                val showLeftIcon = uiState.drawerShowIcons && !isRightAligned && !isCenterAligned
                val showRightIcon = uiState.drawerShowIcons && isRightAligned
                val resolvedIconShape = remember(uiState.iconShape) {
                    IconShape.fromPreference(uiState.iconShape)
                }
                val modeIsFullBleed = remember(uiState.iconSourceMode) {
                    IconShapeUtility.isFullBleedMode(uiState.iconSourceMode)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = rowArrangement
                ) {
                    if (showLeftIcon) {
                        AppIconBox(
                            iconSourceMode = uiState.iconSourceMode,
                            iconShape = resolvedIconShape,
                            size = appNameHeight * 1.25f,
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
                                appNameHeight = appNameHeight * 1.25f,
                                appTextSize = uiState.appDrawerSize.toFloat() * screenScale,
                                labelColor = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                                labelFontFamily = appFontFamily,
                                iconShape = if (IconShapeUtility.shouldClipBitmap(uiState.iconSourceMode))
                                    IconShapeUtility.getComposeShape(resolvedIconShape, pillRadius = (appNameHeight * 1.25f) / 2)
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
                    val gapPadPx = with(density) { 6.dp.toPx() }.toInt()
                    Layout(
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer(clip = false),
                        content = {
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
                                    fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = appFontFamily
                                )
                            }
                            if (showLeaderDots) {
                                Text(
                                    text = ".".repeat(1000),
                                    color = textColor,
                                    fontSize = (uiState.appDrawerSize * 0.7f).sp.scaled(screenScale),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    fontFamily = appFontFamily
                                )
                            } else {
                                Box(modifier = Modifier)
                            }
                            Text(
                                text = timeText,
                                color = textColor,
                                fontSize = (uiState.appDrawerSize * 0.7f).sp.scaled(screenScale),
                                maxLines = 1,
                                fontFamily = appFontFamily
                            )
                        }
                    ) { measurables, constraints ->
                        val maxW = constraints.maxWidth
                        val timeP = measurables[2].measure(
                            Constraints(minWidth = 0, maxWidth = maxW, minHeight = 0, maxHeight = constraints.maxHeight)
                        )
                        val nameMaxW = (maxW - timeP.width - gapPadPx * 2).coerceAtLeast(0)
                        val nameP = measurables[0].measure(
                            Constraints(minWidth = 0, maxWidth = nameMaxW, minHeight = 0, maxHeight = constraints.maxHeight)
                        )
                        val dotsW = (maxW - timeP.width - nameP.width - gapPadPx * 2).coerceAtLeast(0)
                        val dotsP = measurables[1].measure(
                            Constraints(minWidth = dotsW, maxWidth = dotsW, minHeight = 0, maxHeight = constraints.maxHeight)
                        )
                        val rowH = maxOf(nameP.height, dotsP.height, timeP.height)
                        layout(maxW, rowH) {
                            if (isRightAligned) {
                                // Right-aligned: time on left, dots middle, name pinned to right
                                timeP.placeRelative(0, (rowH - timeP.height) / 2)
                                dotsP.placeRelative(timeP.width + gapPadPx, (rowH - dotsP.height) / 2)
                                nameP.placeRelative(maxW - nameP.width, (rowH - nameP.height) / 2)
                            } else {
                                // Left/center: name on left, dots middle, time pinned to right
                                nameP.placeRelative(0, (rowH - nameP.height) / 2)
                                dotsP.placeRelative(nameP.width + gapPadPx, (rowH - dotsP.height) / 2)
                                timeP.placeRelative(maxW - timeP.width, (rowH - timeP.height) / 2)
                            }
                        }
                    }
                    if (showRightIcon) {
                        AppIconBox(
                            iconSourceMode = uiState.iconSourceMode,
                            iconShape = resolvedIconShape,
                            size = appNameHeight * 1.25f,
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
                                appNameHeight = appNameHeight * 1.25f,
                                appTextSize = uiState.appDrawerSize.toFloat() * screenScale,
                                labelColor = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                                labelFontFamily = appFontFamily,
                                iconShape = if (IconShapeUtility.shouldClipBitmap(uiState.iconSourceMode))
                                    IconShapeUtility.getComposeShape(resolvedIconShape, pillRadius = (appNameHeight * 1.25f) / 2)
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
                }
            }
        }
@Composable
fun RecentAppPlaceholderItem(uiState: AppsDrawerUiState, isLastItem: Boolean = false) {
    val screenScale = rememberScreenScale()
    val boxAlignment = when (uiState.appDrawerAlignment) {
        1 -> Alignment.Center
        2 -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    val ctx = LocalContext.current
    val density = LocalDensity.current
    val placeholderTypefaceNullable = remember(uiState.appsFont, uiState.customFontPath) {
        try { uiState.appsFont.getFont(ctx, uiState.customFontPath) } catch (_: Exception) { null }
    }
    val placeholderFontFamily = remember(placeholderTypefaceNullable) {
        if (placeholderTypefaceNullable != null) FontFamily(placeholderTypefaceNullable) else FontFamily.Default
    }
    val placeholderTextMeasurer = rememberTextMeasurer()
    val placeholderHeight = remember(uiState.appDrawerSize, placeholderFontFamily, screenScale, placeholderTextMeasurer) {
        try {
            val measured = placeholderTextMeasurer.measure(
                text = AnnotatedString("Ag"),
                style = TextStyle(fontSize = uiState.appDrawerSize.sp.scaled(screenScale), fontFamily = placeholderFontFamily)
            ).size.height
            with(density) { measured.toDp() }
        } catch (_: Exception) {
            uiState.appDrawerSize.dp
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = uiState.appDrawerGap.dp, bottom = uiState.appDrawerGap.dp)
            .then(if (uiState.drawerShowIcons && placeholderHeight > 0.dp) Modifier.height(placeholderHeight) else Modifier),
        contentAlignment = boxAlignment
    ) {
        Text(
            text = "placeholder",
            color = Color.Transparent,
            fontSize = uiState.appDrawerSize.sp.scaled(screenScale),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = placeholderFontFamily
        )
    }
}
