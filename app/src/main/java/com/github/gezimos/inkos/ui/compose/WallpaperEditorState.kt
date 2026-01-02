package com.github.gezimos.inkos.ui.compose

import com.github.gezimos.inkos.helper.WallpaperHalftone
import com.github.gezimos.inkos.helper.WallpaperDither

data class WallpaperEditorState(
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Int = 0,
    val contrast: Int = 0,
    val isInverted: Boolean = false,
    val halftoneIntensity: Int = 0,
    val halftoneDotSize: Int = 50,  // 0-100: controls dot/line size within cell
    val halftoneShape: WallpaperHalftone.HalftoneShape = WallpaperHalftone.HalftoneShape.DOTS,
    val overlayEnabled: Boolean = false,
    val overlaySide: String = "left",
    val overlaySpread: Int = 40,  // Coverage area (25-100)
    val overlayFalloff: Int = 60,  // Gradient smoothness (0-100, higher = smoother)
    val thresholdLevel: Int = 50,  // 0-100: threshold for black/white conversion
    val ditherEnabled: Boolean = false,
    val ditherAlgorithm: WallpaperDither.DitherAlgorithm = WallpaperDither.DitherAlgorithm.FLOYD_STEINBERG,
    val cropEnabled: Boolean = false,  // Whether crop mode is active
    val cropX: Float = 0.5f,  // Normalized X position (0-1) of crop center
    val cropY: Float = 0.5f,  // Normalized Y position (0-1) of crop center
    val cropScale: Float = 0.8f  // Crop box scale (0.3-1.0, where 1.0 fills most of the image)
)
