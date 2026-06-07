package com.github.gezimos.inkos.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.os.UserHandle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.gezimos.inkos.helper.IconShape
import com.github.gezimos.inkos.helper.IconShapeUtility
import com.github.gezimos.inkos.helper.IconUtility
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private val tintFilterByArgb = ConcurrentHashMap<Int, ColorFilter>()

fun cachedTintFilter(color: Color): ColorFilter =
    tintFilterByArgb.getOrPut(color.toArgb()) { ColorFilter.tint(color) }

@Composable
fun rememberAppIconBitmap(
    context: Context,
    packageName: String,
    iconSourceMode: Int,
    selectedIconPackPackage: String,
    sizePx: Int,
    activityClass: String = "",
    user: UserHandle? = null,
    shortcutId: String? = null,
    tintArgb: Int = 0,
    iconShapeId: Int = -1,
    customCacheKey: String? = null,
    customLoader: (suspend () -> Bitmap?)? = null,
    nonCancellable: Boolean = false,
    iconTintContrast: Int = 10,
    bgArgb: Int = 0
): ImageBitmap? {
    val cacheKey = remember(customCacheKey, packageName, iconSourceMode, selectedIconPackPackage, sizePx, activityClass, user, shortcutId, tintArgb, iconShapeId, iconTintContrast, bgArgb) {
        customCacheKey ?: IconUtility.appIconCacheKey(packageName, iconSourceMode, selectedIconPackPackage, sizePx, activityClass, user, shortcutId, tintArgb, iconShapeId, bgArgb)
    }
    val initialBitmap = remember(cacheKey) { IconUtility.getCachedBitmap(cacheKey) }
    var bitmap by remember(cacheKey) { mutableStateOf(initialBitmap) }
    if (bitmap == null) {
        LaunchedEffect(cacheKey) {
            val ctx = if (nonCancellable) Dispatchers.Default + NonCancellable else Dispatchers.Default
            bitmap = withContext(ctx) {
                customLoader?.invoke()
                    ?: IconUtility.loadAppIcon(context, packageName, iconSourceMode, selectedIconPackPackage, sizePx, activityClass, user, shortcutId, tintArgb, iconShapeId, bgArgb).second
            }
        }
    }
    return remember(bitmap) { bitmap?.asImageBitmap() }
}
@Composable
fun AppIconBox(
    iconSourceMode: Int,
    iconShape: IconShape,
    size: Dp,
    showBackground: Boolean,
    backgroundColor: Color,
    showBorder: Boolean,
    borderColor: Color,
    paddingStart: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val composeShape = remember(iconShape, size) {
        IconShapeUtility.getComposeShape(iconShape, pillRadius = size / 2)
    }
    val borderWidth = if (showBorder) IconShapeUtility.borderWidthForMode(iconSourceMode) else null
    val clipBitmap = IconShapeUtility.shouldClipBitmap(iconSourceMode)

    Box(
        modifier = modifier
            .padding(start = paddingStart, end = paddingEnd)
            .requiredSize(size)
            .then(
                if (borderWidth != null)
                    Modifier.border(width = borderWidth, color = borderColor, shape = composeShape)
                else Modifier
            )
            .graphicsLayer(clip = false)
            .drawBehind {
                if (showBackground) {
                    val corner = IconShapeUtility.getCornerRadius(
                        iconShape = iconShape,
                        heightPx = size.toPx(),
                        density = density
                    )
                    drawRoundRect(
                        color = backgroundColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.toPx(), size.toPx()),
                        cornerRadius = corner
                    )
                }
            }
            .then(if (clipBitmap) Modifier.clip(composeShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
