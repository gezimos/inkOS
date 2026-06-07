package com.github.gezimos.inkos.ui.compose

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.data.ThemePreset
import com.github.gezimos.inkos.data.ThemePresets
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost
import com.github.gezimos.inkos.ui.dialogs.ThemeConfigSheet

@Composable
fun ThemePresetPicker(
    isDark: Boolean,
    themeMode: Constants.Theme,
    onThemeModeChange: (Constants.Theme) -> Unit,
    viewModel: MainViewModel,
    prefs: Prefs,
    modifier: Modifier = Modifier,
    applyOnFirstComposition: Boolean = false,
    titleFontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    showThemeModeToggleUnderCards: Boolean = true,
    onPageChanged: (Int) -> Unit = {},
    applyFontImmediately: Boolean = true,
    requireConfirmation: Boolean = false,
    onConfirmSelect: (() -> Unit)? = null,
    onConfigClick: () -> Unit = {},
    einkMode: Boolean = false,
    currentIndexState: MutableIntState = remember { mutableIntStateOf(0) },
    focusZone: MutableState<ThemePresetFocusZone> = remember { mutableStateOf(ThemePresetFocusZone.CARD) },
    bottomRowIndex: MutableIntState = remember { mutableIntStateOf(0) },
    isDpadMode: Boolean = false,
    selectAction: MutableState<(() -> Unit)?> = remember { mutableStateOf(null) },
    applyColors: MutableState<Boolean> = remember { mutableStateOf(true) },
    applyFont: MutableState<Boolean> = remember { mutableStateOf(true) },
    applyIcons: MutableState<Boolean> = remember { mutableStateOf(true) },
    applyLayout: MutableState<Boolean> = remember { mutableStateOf(true) },
    applyWallpaper: MutableState<Boolean> = remember { mutableStateOf(true) }
) {
    val context = LocalContext.current
    val screenScale = rememberScreenScale()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveIsDark = when (themeMode) {
        Constants.Theme.Dark -> true
        Constants.Theme.Light -> false
        Constants.Theme.System -> systemDark
    }
    val presets = remember(einkMode) {
        if (einkMode) {
            ThemePresets.PRESETS.map {
                it.copy(
                    lightTextColor = AndroidColor.BLACK,
                    lightBackgroundColor = AndroidColor.WHITE,
                    darkTextColor = AndroidColor.WHITE,
                    darkBackgroundColor = AndroidColor.BLACK,
                )
            }
        } else {
            ThemePresets.PRESETS
        }
    }
    var currentIndex by currentIndexState
    var dragAmountX by remember { mutableStateOf(0f) }
    var initialized by remember { mutableStateOf(false) }

    val safeIndex = currentIndex.coerceIn(0, presets.lastIndex)

    fun applyPresetFull(preset: ThemePreset) {
        prefs.appliedThemeName = preset.name
        if (applyColors.value) {
            prefs.lightTextColor = preset.lightTextColor
            prefs.lightBackgroundColor = preset.lightBackgroundColor
            prefs.darkTextColor = preset.darkTextColor
            prefs.darkBackgroundColor = preset.darkBackgroundColor
            prefs.customThemeLightTextColor = preset.lightTextColor
            prefs.customThemeLightBackgroundColor = preset.lightBackgroundColor
            prefs.customThemeDarkTextColor = preset.darkTextColor
            prefs.customThemeDarkBackgroundColor = preset.darkBackgroundColor
            viewModel.setBackgroundColor(prefs.backgroundColor)
            viewModel.setTextColor(prefs.textColor)
        }
        if (applyFont.value) {
            if (applyFontImmediately) {
                viewModel.setUniversalFont(preset.font)
                viewModel.setUniversalFontEnabled(true)
            }
        }
        if (applyIcons.value) {
            viewModel.setIconSourceMode(preset.iconMode)
            viewModel.setIconShape(preset.iconShape)
            viewModel.setShowIcons(preset.showIcons)
            viewModel.setDrawerShowIcons(preset.drawerShowIcons ?: preset.showIcons)
        }
        if (applyLayout.value) {
            viewModel.setTextIslands(preset.textIslands)
            viewModel.setTextIslandsShape(preset.textIslandsShape)
            viewModel.setHomeAlignment(preset.homeAlignment)
            viewModel.setHomeClockAlignment(preset.clockAlignment)
            viewModel.setClockStyle(preset.clockStyle)
            viewModel.setShowSecondClock(preset.showSecondClock)
            viewModel.setSecondClockOffsetHours(preset.secondClockOffsetHours)
            viewModel.setBottomWidgetType(preset.bottomWidgetType)
            viewModel.setShortcutHideOutline(preset.shortcutHideOutline)
            viewModel.setShowClock(preset.showClock)
            viewModel.setShowDate(preset.showDate)
            viewModel.setShowDateBatteryCombo(preset.showDateBatteryCombo)
            viewModel.setHomeDateAlignment(preset.dateAlignment ?: preset.clockAlignment)
            viewModel.setHomeQuoteAlignment(preset.homeAlignment)
            viewModel.setClockSize((preset.clockSize * screenScale).toInt())
            viewModel.setAppSize((preset.appSize * screenScale).toInt())
            val defaultQuoteSize = (context.resources.getDimensionPixelSize(R.dimen.default_quote_size) / context.resources.displayMetrics.scaledDensity).toInt()
            viewModel.setQuoteSize(preset.quoteSize ?: defaultQuoteSize)
            preset.quoteText?.let { viewModel.setQuoteText(it) }
            viewModel.setTextPaddingSize((preset.appGap * screenScale).toInt())
            viewModel.setAppDrawerSize((preset.appDrawerSize * screenScale).toInt())
            viewModel.setAppDrawerGap((preset.appDrawerGap * screenScale).toInt())
            viewModel.setAppDrawerAlignment(preset.appDrawerAlignment)
            viewModel.setHomeAppsNum(preset.homeAppsNum)
            viewModel.setHomePagesNum(preset.homePagesNum)
            viewModel.setShowNotificationCount(preset.showNotificationCount)
            viewModel.setAllCapsApps(preset.allCapsApps)
            viewModel.setSmallCapsApps(preset.smallCapsApps)
            viewModel.setHomeAppsYOffset(preset.homeAppsYOffset)
            val density = context.resources.displayMetrics.density
            val defaultTopMargin = (context.resources.getDimension(R.dimen.default_top_widget_margin) / density).toInt()
            val defaultBottomMargin = (context.resources.getDimension(R.dimen.default_bottom_widget_margin) / density).toInt()
            viewModel.setTopWidgetMargin(preset.topWidgetMargin ?: defaultTopMargin)
            viewModel.setBottomWidgetMargin(preset.bottomWidgetMargin ?: defaultBottomMargin)
        }
        // Wallpaper and opacity
        if (applyWallpaper.value) {
            viewModel.setBackgroundOpacity(preset.backgroundOpacity ?: 255)
            if (preset.wallpaperResourceId != null) {
                Thread {
                    val wu = com.github.gezimos.inkos.helper.WallpaperUtility(context)
                    val bitmap = if (preset.wallpaperResourceId < 0) {
                        wu.loadGeneratedWallpaper(preset.wallpaperResourceId)
                    } else {
                        wu.loadBitmapFromResource(preset.wallpaperResourceId)
                    }
                    if (bitmap != null) {
                        val fitted = wu.createFittedBitmap(bitmap, wu.screenWidth, wu.screenHeight)
                        val path = wu.saveBitmapToInternalStorage(fitted, "inkos_wallpaper.png")
                        prefs.inkosWallpaperPath = path
                        prefs.inkosWallpaperResourceId = preset.wallpaperResourceId
                        if (fitted != bitmap) fitted.recycle()
                        bitmap.recycle()
                    }
                }.start()
            } else {
                prefs.inkosWallpaperPath = null
                prefs.inkosWallpaperResourceId = 0
            }
        }
        android.widget.Toast.makeText(context, "Theme applied", android.widget.Toast.LENGTH_SHORT).show()
        onConfirmSelect?.invoke()
    }

    selectAction.value = { applyPresetFull(presets[safeIndex]) }

    LaunchedEffect(currentIndex, effectiveIsDark, einkMode) {
        if (!initialized && !applyOnFirstComposition) {
            initialized = true
            return@LaunchedEffect
        }
        initialized = true
        val preset = presets[safeIndex]

        if (!requireConfirmation) {
            prefs.appliedThemeName = preset.name
            if (applyColors.value) {
                prefs.lightTextColor = preset.lightTextColor
                prefs.lightBackgroundColor = preset.lightBackgroundColor
                prefs.darkTextColor = preset.darkTextColor
                prefs.darkBackgroundColor = preset.darkBackgroundColor
                prefs.customThemeLightTextColor = preset.lightTextColor
                prefs.customThemeLightBackgroundColor = preset.lightBackgroundColor
                prefs.customThemeDarkTextColor = preset.darkTextColor
                prefs.customThemeDarkBackgroundColor = preset.darkBackgroundColor
                viewModel.setBackgroundColor(prefs.backgroundColor)
                viewModel.setTextColor(prefs.textColor)
            }
            if (applyFont.value) {
                if (applyFontImmediately) {
                    viewModel.setUniversalFont(preset.font)
                    viewModel.setUniversalFontEnabled(true)
                }
            }
            if (applyIcons.value) {
                viewModel.setIconSourceMode(preset.iconMode)
                viewModel.setIconShape(preset.iconShape)
                viewModel.setShowIcons(preset.showIcons)
                viewModel.setDrawerShowIcons(preset.drawerShowIcons ?: preset.showIcons)
            }
            if (applyLayout.value) {
                viewModel.setTextIslands(preset.textIslands)
                viewModel.setTextIslandsShape(preset.textIslandsShape)
                viewModel.setHomeAlignment(preset.homeAlignment)
                viewModel.setHomeClockAlignment(preset.clockAlignment)
                viewModel.setClockStyle(preset.clockStyle)
                viewModel.setBottomWidgetType(preset.bottomWidgetType)
                viewModel.setShowClock(preset.showClock)
                viewModel.setShowDate(preset.showDate)
                viewModel.setShowDateBatteryCombo(preset.showDateBatteryCombo)
                viewModel.setHomeDateAlignment(preset.dateAlignment ?: preset.clockAlignment)
                viewModel.setHomeQuoteAlignment(preset.homeAlignment)
                viewModel.setClockSize((preset.clockSize * screenScale).toInt())
                viewModel.setAppSize((preset.appSize * screenScale).toInt())
                val defaultQuoteSize = (context.resources.getDimensionPixelSize(R.dimen.default_quote_size) / context.resources.displayMetrics.scaledDensity).toInt()
                viewModel.setQuoteSize(preset.quoteSize ?: defaultQuoteSize)
                viewModel.setTextPaddingSize((preset.appGap * screenScale).toInt())
                viewModel.setAppDrawerSize((preset.appDrawerSize * screenScale).toInt())
                viewModel.setAppDrawerGap((preset.appDrawerGap * screenScale).toInt())
                viewModel.setAppDrawerAlignment(preset.appDrawerAlignment)
                viewModel.setHomeAppsNum(preset.homeAppsNum)
                viewModel.setHomePagesNum(preset.homePagesNum)
                viewModel.setShowNotificationCount(preset.showNotificationCount)
                viewModel.setAllCapsApps(preset.allCapsApps)
                viewModel.setSmallCapsApps(preset.smallCapsApps)
                viewModel.setHomeAppsYOffset(preset.homeAppsYOffset)
                val density = context.resources.displayMetrics.density
                val defaultTopMargin = (context.resources.getDimension(R.dimen.default_top_widget_margin) / density).toInt()
                val defaultBottomMargin = (context.resources.getDimension(R.dimen.default_bottom_widget_margin) / density).toInt()
                viewModel.setTopWidgetMargin(preset.topWidgetMargin ?: defaultTopMargin)
                viewModel.setBottomWidgetMargin(preset.bottomWidgetMargin ?: defaultBottomMargin)
            }
            if (applyWallpaper.value) {
                viewModel.setBackgroundOpacity(preset.backgroundOpacity ?: 255)
                if (preset.wallpaperResourceId != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        val wu = com.github.gezimos.inkos.helper.WallpaperUtility(context)
                        val bitmap = if (preset.wallpaperResourceId < 0) {
                            wu.loadGeneratedWallpaper(preset.wallpaperResourceId)
                        } else {
                            wu.loadBitmapFromResource(preset.wallpaperResourceId)
                        }
                        if (bitmap != null) {
                            val fitted = wu.createFittedBitmap(bitmap, wu.screenWidth, wu.screenHeight)
                            val path = wu.saveBitmapToInternalStorage(fitted, "inkos_wallpaper.png")
                            prefs.inkosWallpaperPath = path
                            prefs.inkosWallpaperResourceId = preset.wallpaperResourceId
                            if (fitted != bitmap) fitted.recycle()
                            bitmap.recycle()
                        }
                    }
                } else {
                    prefs.inkosWallpaperPath = null
                    prefs.inkosWallpaperResourceId = 0
                }
            }
        }

        onPageChanged(safeIndex)
    }

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp.scaled(screenScale))
        ) {
            val spacing = 16.dp.scaled(screenScale)
            val minChevronWidth = 32.dp.scaled(screenScale)
            val totalSpacing = spacing * 2
            val naturalCenterWidth = maxWidth - (minChevronWidth * 2) - totalSpacing

            val centerWidth = naturalCenterWidth
                .coerceAtMost(maxHeight * 1f)
                .coerceAtLeast(maxHeight * (9f / 22f))
                .coerceAtMost(naturalCenterWidth)
            val chevronWidth = ((maxWidth - centerWidth - totalSpacing) / 2)
                .coerceAtLeast(minChevronWidth)

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Left: right-edge peek of previous card + chevron
                val prevPeekIndex = if (safeIndex > 0) safeIndex - 1 else presets.lastIndex
                val prevPeekPreset = presets[prevPeekIndex]
                Box(
                    modifier = Modifier
                        .width(chevronWidth)
                        .fillMaxHeight()
                        .then(if (safeIndex > 0) Modifier.clickable { currentIndex-- } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    val prevBgColor = Color(if (effectiveIsDark) prevPeekPreset.darkBackgroundColor else prevPeekPreset.lightBackgroundColor)
                    val peekRadius = when (prefs.textIslandsShape) { 0 -> 16.dp; 1 -> 8.dp; else -> 0.dp }
                    val prevShape = RoundedCornerShape(topEnd = peekRadius, bottomEnd = peekRadius)
                    val prevBorderColor = Theme.colors.text
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(prevBgColor, prevShape)
                            .drawBehind {
                                val borderColor = prevBorderColor
                                val s = 1.5.dp.scaled(screenScale).toPx()
                                val r = peekRadius.toPx()
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, 0f)
                                    if (r > 0f) {
                                        lineTo(size.width - r, 0f)
                                        arcTo(androidx.compose.ui.geometry.Rect(size.width - 2 * r, 0f, size.width, 2 * r), -90f, 90f, false)
                                        lineTo(size.width, size.height - r)
                                        arcTo(androidx.compose.ui.geometry.Rect(size.width - 2 * r, size.height - 2 * r, size.width, size.height), 0f, 90f, false)
                                    } else {
                                        lineTo(size.width, 0f)
                                        lineTo(size.width, size.height)
                                    }
                                    lineTo(0f, size.height)
                                }
                                drawPath(path, borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(s))
                            }
                    )
                    if (safeIndex > 0) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
                            contentDescription = "Previous theme",
                            tint = Theme.colors.text,
                            modifier = Modifier.size(24.dp.scaled(screenScale))
                        )
                    }
                }
                // Center: current card with swipe gesture
                Box(
                    modifier = Modifier
                        .width(centerWidth)
                        .fillMaxHeight()
                        .pointerInput(presets.size, currentIndex) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    dragAmountX += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = 48f
                                    when {
                                        dragAmountX > threshold && currentIndex > 0 -> {
                                            currentIndex--
                                        }
                                        dragAmountX < -threshold && currentIndex < presets.lastIndex -> {
                                            currentIndex++
                                        }
                                    }
                                    dragAmountX = 0f
                                },
                                onDragCancel = {
                                    dragAmountX = 0f
                                }
                            )
                        }
                ) {
                    ThemePresetCard(
                        preset = presets[safeIndex],
                        isDark = effectiveIsDark,
                        isSelected = true,
                        titleFontSize = titleFontSize,
                        showTitle = false,
                        textIslandsShape = prefs.textIslandsShape,
                        screenScale = screenScale
                    )
                }
                // Right: left-edge peek of next card + chevron
                val nextPeekIndex = if (safeIndex < presets.lastIndex) safeIndex + 1 else 0
                val nextPeekPreset = presets[nextPeekIndex]
                Box(
                    modifier = Modifier
                        .width(chevronWidth)
                        .fillMaxHeight()
                        .then(if (safeIndex < presets.lastIndex) Modifier.clickable { currentIndex++ } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    val nextBgColor = Color(if (effectiveIsDark) nextPeekPreset.darkBackgroundColor else nextPeekPreset.lightBackgroundColor)
                    val nextPeekRadius = when (prefs.textIslandsShape) { 0 -> 16.dp; 1 -> 8.dp; else -> 0.dp }
                    val nextShape = RoundedCornerShape(topStart = nextPeekRadius, bottomStart = nextPeekRadius)
                    val nextBorderColor = Theme.colors.text
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(nextBgColor, nextShape)
                            .drawBehind {
                                val borderColor = nextBorderColor
                                val s = 1.5.dp.scaled(screenScale).toPx()
                                val r = nextPeekRadius.toPx()
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width, 0f)
                                    if (r > 0f) {
                                        lineTo(r, 0f)
                                        arcTo(androidx.compose.ui.geometry.Rect(0f, 0f, 2 * r, 2 * r), -90f, -90f, false)
                                        lineTo(0f, size.height - r)
                                        arcTo(androidx.compose.ui.geometry.Rect(0f, size.height - 2 * r, 2 * r, size.height), 180f, -90f, false)
                                    } else {
                                        lineTo(0f, 0f)
                                        lineTo(0f, size.height)
                                    }
                                    lineTo(size.width, size.height)
                                }
                                drawPath(path, borderColor, style = androidx.compose.ui.graphics.drawscope.Stroke(s))
                            }
                    )
                    if (safeIndex < presets.lastIndex) {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = "Next theme",
                            tint = Theme.colors.text,
                            modifier = Modifier.size(24.dp.scaled(screenScale))
                        )
                    }
                }
            }
        }
        if (showThemeModeToggleUnderCards) {
            val prefTextColor = Theme.colors.text
            val prefBackgroundColor = Theme.colors.background
            // Separator line above footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp.scaled(screenScale))
                    .background(prefTextColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp.scaled(screenScale)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: config (opens bottom sheet)
                val configHighlighted = isDpadMode && focusZone.value == ThemePresetFocusZone.BOTTOM_ROW && bottomRowIndex.intValue == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (configHighlighted) prefTextColor else prefBackgroundColor)
                        .clickable { onConfigClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CONFIG",
                        style = SettingsTheme.typography.title,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (configHighlighted) prefBackgroundColor else prefTextColor
                    )
                }

                // Vertical separator
                Box(
                    modifier = Modifier
                        .width(1.5.dp.scaled(screenScale))
                        .fillMaxHeight()
                        .background(prefTextColor)
                )

                // Page counter
                Box(
                    modifier = Modifier.width(54.dp.scaled(screenScale)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${safeIndex + 1}/${presets.size}",
                        style = SettingsTheme.typography.body,
                        fontSize = (titleFontSize.value * 0.7f).sp,
                        color = prefTextColor,
                        textAlign = TextAlign.Center
                    )
                }

                if (requireConfirmation) {
                    // Vertical separator
                    Box(
                        modifier = Modifier
                            .width(1.5.dp.scaled(screenScale))
                            .fillMaxHeight()
                            .background(prefTextColor)
                    )

                    // Right: Apply
                    val applyHighlighted = isDpadMode && focusZone.value == ThemePresetFocusZone.BOTTOM_ROW && bottomRowIndex.intValue == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (applyHighlighted) prefTextColor else prefBackgroundColor)
                            .clickable { applyPresetFull(presets[safeIndex]) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "APPLY",
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (applyHighlighted) prefBackgroundColor else prefTextColor
                        )
                    }
                }
            }

            // Bottom border when nav bar is visible
            val navView = androidx.compose.ui.platform.LocalView.current
            val hasNavBar = remember(navView) {
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(navView)
                insets != null && insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom > 0
            }
            if (hasNavBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp.scaled(screenScale))
                        .background(prefTextColor)
                )
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: ThemePreset,
    isDark: Boolean,
    isSelected: Boolean,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    showTitle: Boolean = true,
    textIslandsShape: Int = 0,
    screenScale: Float = 1f
) {
    val bgColor = Color(if (isDark) preset.darkBackgroundColor else preset.lightBackgroundColor)
    val cardShape = ShapeHelper.getRoundedCornerShape(textIslandsShape = textIslandsShape, pillRadius = 16.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showTitle) {
            Text(
                text = preset.name,
                style = SettingsTheme.typography.title,
                fontSize = titleFontSize,
                color = Theme.colors.text,
                modifier = Modifier.padding(bottom = 4.dp.scaled(screenScale))
            )
            Text(
                text = preset.description,
                style = SettingsTheme.typography.item,
                color = Theme.colors.text.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp.scaled(screenScale))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(
                    width = 1.5.dp.scaled(screenScale),
                    color = Theme.colors.text,
                    shape = cardShape
                )
                .clip(cardShape)
                .background(bgColor)
        ) {
            HomeScreenPreview(preset = preset, isDark = isDark)
        }
    }
}

/** Returns the display name of the currently applied theme (preset name or "Custom"). */
fun getChosenThemeDisplayName(context: android.content.Context): String {
    val prefs = Prefs(context)
    return prefs.appliedThemeName ?: context.getString(R.string.custom_theme)
}
@Composable
fun ThemePresetsPage(
    onBackClick: () -> Unit,
    isDark: Boolean,
    showStatusBar: Boolean = false,
    viewModel: MainViewModel,
    prefs: Prefs,
    titleFontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    onConfirmSelect: (() -> Unit)? = null
) {
    val homeUiState by viewModel.homeUiState.collectAsState()
    val currentThemeMode = homeUiState.appTheme
    val einkMode = remember { mutableStateOf(com.github.gezimos.inkos.helper.device.DeviceHelper.isEinkDevice()) }

    val buttonShape = remember(prefs.textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = prefs.textIslandsShape, pillRadius = 16.dp
        )
    }

    // DPAD state
    val focusZone = remember { mutableStateOf(ThemePresetFocusZone.CARD) }
    val headerIndex = remember { mutableIntStateOf(0) }
    val bottomRowIndex = remember { mutableIntStateOf(0) }
    val currentIndexState = remember { mutableIntStateOf(0) }
    var isDpadMode by remember { mutableStateOf(false) }
    val selectAction = remember { mutableStateOf<(() -> Unit)?>(null) }
    // Skip options state
    val applyColors = remember { mutableStateOf(true) }
    val applyFont = remember { mutableStateOf(true) }
    val applyIcons = remember { mutableStateOf(true) }
    val applyLayout = remember { mutableStateOf(true) }
    val applyWallpaper = remember { mutableStateOf(true) }
    // Config bottom sheet
    val context = LocalContext.current
    val sheetHost = remember { (context as? Activity)?.let { ComposeBottomSheetHost(it) } }
    val showConfigSheet = {
        sheetHost?.show {
            ThemeConfigSheet(
                applyColors = applyColors,
                applyFont = applyFont,
                applyIcons = applyIcons,
                applyLayout = applyLayout,
                applyWallpaper = applyWallpaper
            )
        }
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    SettingsTheme(isDark) {
        val screenScale = rememberScreenScale()
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(prefBackgroundColor)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    NavHelper.handleThemePresetKeyEvent(
                        keyEvent = event,
                        isDpadModeSetter = { isDpadMode = it },
                        focusZone = focusZone,
                        headerIndex = headerIndex,
                        bottomRowIndex = bottomRowIndex,
                        currentIndex = currentIndexState,
                        presetsLastIndex = ThemePresets.PRESETS.lastIndex,
                        hasSelectButton = true,
                        headerMaxIndex = 2,
                        onBackClick = onBackClick,
                        onHeaderAction = { index ->
                            when (index) {
                                1 -> einkMode.value = !einkMode.value
                                2 -> {
                                    val newMode = when (currentThemeMode) {
                                        Constants.Theme.Light -> Constants.Theme.Dark
                                        Constants.Theme.Dark -> Constants.Theme.System
                                        Constants.Theme.System -> Constants.Theme.Light
                                    }
                                    viewModel.setAppTheme(newMode)
                                    AppCompatDelegate.setDefaultNightMode(
                                        when (newMode) {
                                            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                                            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                                            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                        }
                                    )
                                }
                            }
                        },
                        onModeToggle = { showConfigSheet() },
                        onSelect = { selectAction.value?.invoke() }
                    )
                }
        ) {
            val currentPresetName = ThemePresets.PRESETS.getOrNull(currentIndexState.intValue)?.name?.uppercase() ?: ""
            SettingsComposable.PageHeader(
                iconRes = R.drawable.ic_back,
                title = currentPresetName,
                onClick = onBackClick,
                showStatusBar = showStatusBar,
                titleFontSize = titleFontSize,
                backHighlighted = isDpadMode && focusZone.value == ThemePresetFocusZone.HEADER && headerIndex.intValue == 0,
                pageIndicator = {
                    val monoHighlighted = isDpadMode && focusZone.value == ThemePresetFocusZone.HEADER && headerIndex.intValue == 1
                    val modeHighlighted = isDpadMode && focusZone.value == ThemePresetFocusZone.HEADER && headerIndex.intValue == 2
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    ) {
                        // Mono toggle
                        val monoActive = einkMode.value
                        Text(
                            text = "Mono",
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            color = if (monoActive || monoHighlighted) prefBackgroundColor else prefTextColor,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(buttonShape)
                                .background(if (monoActive || monoHighlighted) prefTextColor else prefBackgroundColor)
                                .then(if (!monoActive && !monoHighlighted) Modifier.border(2.dp, prefTextColor, buttonShape) else Modifier)
                                .clickable { einkMode.value = !einkMode.value }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        // Theme mode icon cycle
                        val modeIcon = when (currentThemeMode) {
                            Constants.Theme.Light -> Icons.Rounded.LightMode
                            Constants.Theme.Dark -> Icons.Rounded.DarkMode
                            Constants.Theme.System -> Icons.Rounded.Android
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(buttonShape)
                                .background(if (modeHighlighted) prefTextColor else prefBackgroundColor)
                                .then(if (!modeHighlighted) Modifier.border(2.dp, prefTextColor, buttonShape) else Modifier)
                                .clickable {
                                    val newMode = when (currentThemeMode) {
                                        Constants.Theme.Light -> Constants.Theme.Dark
                                        Constants.Theme.Dark -> Constants.Theme.System
                                        Constants.Theme.System -> Constants.Theme.Light
                                    }
                                    viewModel.setAppTheme(newMode)
                                    AppCompatDelegate.setDefaultNightMode(
                                        when (newMode) {
                                            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                                            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                                            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                        }
                                    )
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = currentThemeMode.name,
                                tint = if (modeHighlighted) prefBackgroundColor else prefTextColor,
                                modifier = Modifier.size(20.dp.scaled(screenScale))
                            )
                        }
                    }
                }
            )
            ThemePresetPicker(
                isDark = isDark,
                themeMode = currentThemeMode,
                onThemeModeChange = { newMode ->
                    viewModel.setAppTheme(newMode)
                    AppCompatDelegate.setDefaultNightMode(
                        when (newMode) {
                            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                    )
                },
                viewModel = viewModel,
                prefs = prefs,
                applyOnFirstComposition = false,
                titleFontSize = titleFontSize,
                modifier = Modifier.fillMaxSize(),
                showThemeModeToggleUnderCards = true,
                requireConfirmation = true,
                onConfirmSelect = onConfirmSelect,
                onConfigClick = { showConfigSheet() },
                einkMode = einkMode.value,
                currentIndexState = currentIndexState,
                focusZone = focusZone,
                bottomRowIndex = bottomRowIndex,
                isDpadMode = isDpadMode,
                selectAction = selectAction,
                applyColors = applyColors,
                applyFont = applyFont,
                applyIcons = applyIcons,
                applyLayout = applyLayout,
                applyWallpaper = applyWallpaper
            )
        }
    }
}
