package com.github.gezimos.inkos.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.AppsDrawerUiState
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.ui.AppsDrawerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class to represent a recent app with usage info
 */
data class RecentAppItem(
    val app: AppListItem,
    val lastUsedTime: Long,
    val totalUsageTime: Long
)

/**
 * Main layout component for the recents screen (simplified from AppsUI)
 */
@Composable
fun RecentsLayout(
    state: AppsDrawerState,
    uiState: AppsDrawerUiState,
    recentApps: List<RecentAppItem>,
    paddedRecentApps: List<RecentAppItem?>,
    totalPages: Int,
    isCalculated: Boolean,
    showStats: Boolean = false,
    onStatsToggle: () -> Unit = {},
    onAppClick: (RecentAppItem) -> Unit,
    onAppLongClick: (RecentAppItem) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (isCalculated) {
            if (recentApps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent apps",
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
                    // Recents title
                    item(key = "recents_title") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = uiState.appDrawerGap.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val ctx = LocalContext.current
                            val customPath = uiState.customFontPath
                            val appTypefaceNullable = remember(uiState.appsFont, customPath) { uiState.appsFont.getFont(ctx, customPath) }
                            val appFontFamily = remember(appTypefaceNullable) {
                                if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
                            }
                            
                            // Title on the left
                            Text(
                                text = if (showStats) "MOST USED" else "RECENTS",
                                color = Theme.colors.text,
                                fontSize = uiState.appDrawerSize.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = appFontFamily
                            )
                            
                            // Stats icon on the right
                            Icon(
                                imageVector = Icons.Rounded.HourglassBottom,
                                contentDescription = "Toggle stats view",
                                tint = Theme.colors.text,
                                modifier = Modifier
                                    .size(with(LocalDensity.current) { uiState.appDrawerSize.sp.toDp() })
                                    .combinedClickable(
                                        onClick = onStatsToggle,
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                            )
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
                                onLongClick = { onAppLongClick(recentApp) }
                            )
                        } else {
                            RecentAppPlaceholderItem(uiState = uiState)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recent app item component showing app name and last used time
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentAppItem(
    recentApp: RecentAppItem,
    uiState: AppsDrawerUiState,
    showStats: Boolean = false,
    isSelected: Boolean = false,
    isDpadMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
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
    
    val boxAlignment = remember(uiState.appDrawerAlignment) {
        when (uiState.appDrawerAlignment) {
            1 -> Alignment.Center
            2 -> Alignment.CenterEnd
            else -> Alignment.CenterStart
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
    
    // Format time based on mode
    val timeText = remember(recentApp.lastUsedTime, recentApp.totalUsageTime, showStats) {
        if (showStats) {
            // Format total usage time
            val totalMinutes = recentApp.totalUsageTime / (1000 * 60)
            when {
                totalMinutes < 60 -> "${totalMinutes}m"
                totalMinutes < 1440 -> "${totalMinutes / 60}h"
                else -> "${totalMinutes / 1440}d"
            }
        } else {
            // Format last used time
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
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = uiState.appDrawerGap.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = boxAlignment
    ) {
                // Normal display: Show app name and last used time on same row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (isSelected && isDpadMode) {
                                val hlPad = highlightPaddingPx
                                val highlightWidth = size.width + (hlPad * 2)
                                val cornerRadius = ShapeHelper.getCornerRadius(
                                    textIslandsShape = uiState.textIslandsShape,
                                    height = size.height,
                                    density = density
                                )
                                drawRoundRect(
                                    color = textColor,
                                    topLeft = Offset(-hlPad, 0f),
                                    size = Size(highlightWidth.coerceAtLeast(0f), size.height),
                                    cornerRadius = cornerRadius
                                )
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App name (with DPAD highlight)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = finalText,
                            color = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                            fontSize = uiState.appDrawerSize.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = appFontFamily
                        )
                    }

                    // Time display (last used or total usage) — include in pill when selected
                    Text(
                        text = timeText,
                        color = if (isSelected && isDpadMode) invertedHighlightTextColor else textColor,
                        fontSize = (uiState.appDrawerSize * 0.7f).sp,
                        maxLines = 1,
                        fontFamily = appFontFamily
                    )
                }
            }
        }


/**
 * Placeholder item for recents
 */
@Composable
fun RecentAppPlaceholderItem(uiState: AppsDrawerUiState) {
    val boxAlignment = when (uiState.appDrawerAlignment) {
        1 -> Alignment.Center
        2 -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
