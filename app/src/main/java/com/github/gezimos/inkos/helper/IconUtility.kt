package com.github.gezimos.inkos.helper

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.UserHandle
import android.util.LruCache
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import org.xmlpull.v1.XmlPullParser

object IconUtility {


    private const val TAG = "IconUtility"

    private val bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        maxOf(
            (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
            50 * 128 * 128 * 4 / 1024
        )
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    private val tintFilterCache = java.util.concurrent.ConcurrentHashMap<Int, ColorMatrixColorFilter>()

    @Volatile var tintContrast: Float = 1.0f

    fun clearCache() {
        bitmapCache.evictAll()
        tintFilterCache.clear()
    }

    fun getCachedBitmap(key: String): Bitmap? {
        return bitmapCache.get(key)
    }

    @Synchronized
    fun cacheBitmapIfAbsent(key: String, bitmap: Bitmap) {
        if (bitmapCache.get(key) == null) {
            bitmapCache.put(key, bitmap)
        }
    }

    fun appIconCacheKey(
        packageName: String,
        iconSourceMode: Int,
        selectedIconPackPackage: String,
        sizePx: Int,
        activityClass: String = "",
        userHandle: UserHandle? = null,
        shortcutId: String? = null,
        tintArgb: Int = 0,
        iconShapeId: Int = -1,
        bgArgb: Int = 0
    ): String = buildAppIconCacheKey(packageName, iconSourceMode, selectedIconPackPackage, sizePx, activityClass, userHandle, shortcutId, tintArgb, iconShapeId, bgArgb)

    private fun buildAppIconCacheKey(
        packageName: String,
        iconSourceMode: Int,
        selectedIconPackPackage: String,
        sizePx: Int,
        activityClass: String,
        userHandle: UserHandle?,
        shortcutId: String?,
        tintArgb: Int,
        iconShapeId: Int,
        bgArgb: Int
    ): String {
        val isSynth = isSyntheticPackage(packageName) &&
            (shortcutId == null || isInkOSInternalShortcut(shortcutId))
        val shapeSuffix = if ((iconSourceMode == 5 || iconSourceMode == 6) && iconShapeId >= 0) ":s$iconShapeId" else ""
        val contrastSuffix = if (iconSourceMode == 4) ":c${(tintContrast * 10f).toInt()}" else ""
        val bgSuffix = if (iconSourceMode == 6) ":b$bgArgb" else ""
        return if (shortcutId != null && !isSynth) {
            when (iconSourceMode) {
                4 -> "shortcut:mono:$tintArgb:$packageName:$shortcutId:$selectedIconPackPackage:$sizePx$contrastSuffix"
                5 -> "shortcut:minimal:$tintArgb:$packageName:$shortcutId:$selectedIconPackPackage:$sizePx$shapeSuffix"
                6 -> "shortcut:filled:$tintArgb:$packageName:$shortcutId:$selectedIconPackPackage:$sizePx$shapeSuffix$bgSuffix"
                else -> "shortcut:$packageName:$shortcutId:$iconSourceMode:$selectedIconPackPackage:$sizePx"
            }
        } else {
            buildCacheKey(packageName, iconSourceMode, sizePx, isSynth, selectedIconPackPackage, activityClass, userHandle, tintArgb) + shapeSuffix + contrastSuffix + bgSuffix
        }
    }

    fun loadAppIcon(
        context: Context,
        packageName: String,
        iconSourceMode: Int,
        selectedIconPackPackage: String,
        sizePx: Int,
        activityClass: String = "",
        userHandle: UserHandle? = null,
        shortcutId: String? = null,
        tintArgb: Int = 0,
        iconShapeId: Int = -1,
        bgArgb: Int = 0
    ): Pair<String, Bitmap?> {
        val isSynth = isSyntheticPackage(packageName) &&
            (shortcutId == null || isInkOSInternalShortcut(shortcutId))
        val isShortcutOverride = shortcutId != null && !isSynth
        val cacheKey = buildAppIconCacheKey(packageName, iconSourceMode, selectedIconPackPackage, sizePx, activityClass, userHandle, shortcutId, tintArgb, iconShapeId, bgArgb)

        // Fast path: already cached
        getCachedBitmap(cacheKey)?.let { return cacheKey to it }

        if (iconSourceMode == 0) return cacheKey to null

        val bitmap: Bitmap? = try {
            when {
                isSynth -> {
                    if (iconSourceMode == 5 || iconSourceMode == 6) {
                        val res = getSyntheticDrawableRes(packageName)
                        val sil = if (res != null) {
                            ContextCompat.getDrawable(context, res)
                                ?.let { renderMinimalSilhouette(it, sizePx, tintArgb) }
                                ?: getMinimalAppIconBitmap(context, context.packageName, sizePx, tintArgb)
                        } else {
                            getMinimalAppIconBitmap(context, context.packageName, sizePx, tintArgb)
                        }
                        if (iconSourceMode == 6 && sil != null) invertToFilledTile(sil, sizePx, tintArgb, bgArgb, iconShapeId) else sil
                    } else if (iconSourceMode == 4) {
                        val res = getSyntheticDrawableRes(packageName)
                        if (res != null) {
                            ContextCompat.getDrawable(context, res)?.let { d ->
                                val mut = d.mutate()
                                mut.setTint(tintArgb)
                                mut.setTintMode(PorterDuff.Mode.SRC_IN)
                                val size = sizePx.coerceAtLeast(1)
                                val padding = (size * 0.2f).toInt()
                                val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                mut.setBounds(padding, padding, size - padding, size - padding)
                                mut.draw(Canvas(bitmap))
                                bitmap
                            }
                        } else {
                            getMonochromeAppIconBitmap(context, context.packageName, sizePx, tintArgb)
                        }
                    } else {
                        val raw = if (iconSourceMode == 3) {
                            val cls = activityClass.ifEmpty {
                                context.packageManager.getLaunchIntentForPackage(packageName)?.component?.className ?: ""
                            }
                            IconPackUtility.getIconFromPack(context, selectedIconPackPackage, packageName, cls)
                                ?.let { drawableToBitmap(it, sizePx) }
                                ?: getSyntheticIconBitmap(context, packageName, sizePx)
                        } else getSyntheticIconBitmap(context, packageName, sizePx)
                        raw
                    }
                }

                isShortcutOverride -> {
                    if (iconSourceMode == 5 || iconSourceMode == 6) {
                        val isPinned = try {
                            PinnedShortcutUtility.isPinned(context, packageName, shortcutId!!, userHandle ?: android.os.Process.myUserHandle())
                        } catch (_: Exception) { false }
                        val sil = if (isPinned) {
                            // PWA / user-pinned: matrix-tint the shortcut bitmap (preserves brightness/structure)
                            val raw = getShortcutIconBitmap(context, packageName, shortcutId!!, userHandle, sizePx)
                                ?: getFullAppIconBitmap(context, packageName, sizePx, activityClass, userHandle)
                            raw?.let { bakeMatrixTint(it, tintArgb, sizePx) }
                                ?: getInkOSMonochromeDrawable(context)?.let { renderMonochromeBaked(it, sizePx, tintArgb) }
                        } else if (iconSourceMode == 6) {
                            // App shortcut: render parent app's Filled icon
                            getFilledAppIconBitmap(context, packageName, sizePx, tintArgb, bgArgb, activityClass, userHandle, iconShapeId)
                                ?: getInkOSMonochromeDrawable(context)?.let { renderMonochromeBaked(it, sizePx, tintArgb) }
                        } else {
                            // App shortcut (manifest/dynamic): ignore shortcut icon, render parent app's Minimal
                            getMinimalAppIconBitmap(context, packageName, sizePx, tintArgb, activityClass, userHandle, iconShapeId)
                                ?: getInkOSMonochromeDrawable(context)?.let { renderMonochromeBaked(it, sizePx, tintArgb) }
                        }
                        if (iconSourceMode == 6 && isPinned && sil != null) invertToFilledTile(sil, sizePx, tintArgb, bgArgb, iconShapeId) else sil
                    } else {
                        val raw = if (iconSourceMode == 3) {
                            val cls = activityClass.ifEmpty {
                                context.packageManager.getLaunchIntentForPackage(packageName)?.component?.className ?: ""
                            }
                            IconPackUtility.getIconFromPack(context, selectedIconPackPackage, packageName, cls)
                                ?.let { drawableToBitmap(it, sizePx) }
                                ?: getShortcutIconBitmap(context, packageName, shortcutId!!, userHandle, sizePx)
                                ?: getFullAppIconBitmap(context, packageName, sizePx, activityClass, userHandle)
                        } else {
                            getShortcutIconBitmap(context, packageName, shortcutId!!, userHandle, sizePx)
                                ?: getFullAppIconBitmap(context, packageName, sizePx, activityClass, userHandle)
                        }
                        if (iconSourceMode == 4 && raw != null) bakeMatrixTint(raw, tintArgb, sizePx) else raw
                    }
                }

                else -> when (iconSourceMode) {
                    2 -> getFullAppIconBitmap(context, packageName, sizePx, activityClass, userHandle)
                    4 -> getMonochromeAppIconBitmap(context, packageName, sizePx, tintArgb, activityClass, userHandle)
                    5 -> getMinimalAppIconBitmap(context, packageName, sizePx, tintArgb, activityClass, userHandle, iconShapeId)
                        ?: getInkOSMonochromeDrawable(context)?.let { renderMonochromeBaked(it, sizePx, tintArgb) }
                    6 -> getFilledAppIconBitmap(context, packageName, sizePx, tintArgb, bgArgb, activityClass, userHandle, iconShapeId)
                        ?: getInkOSMonochromeDrawable(context)?.let { renderMonochromeBaked(it, sizePx, tintArgb)?.let { silh -> invertToFilledTile(silh, sizePx, tintArgb, bgArgb, iconShapeId) } }
                    3 -> {
                        val cls = activityClass.ifEmpty {
                            context.packageManager.getLaunchIntentForPackage(packageName)?.component?.className ?: ""
                        }
                        IconPackUtility.getIconFromPack(context, selectedIconPackPackage, packageName, cls)
                            ?.let { drawableToBitmap(it, sizePx) }
                            ?: getFullAppIconBitmap(context, packageName, sizePx, activityClass, userHandle)
                    }
                    else -> null
                }
            }
        } catch (_: Exception) { null }

        if (bitmap != null) cacheBitmapIfAbsent(cacheKey, bitmap)
        return cacheKey to bitmap
    }

    fun buildCacheKey(
        packageName: String,
        iconSourceMode: Int,
        sizePx: Int,
        isSynthetic: Boolean,
        iconPackPackage: String = "",
        activityClass: String = "",
        userHandle: UserHandle? = null,
        tintArgb: Int = 0
    ): String {
        return if (isSynthetic) {
            when (iconSourceMode) {
                4 -> "synthetic:mono:$tintArgb:$packageName:$sizePx"
                5 -> "synthetic:minimal:$tintArgb:$packageName:$sizePx"
                6 -> "synthetic:filled:$tintArgb:$packageName:$sizePx"
                else -> "synthetic:$packageName:$sizePx"
            }
        } else {
            when (iconSourceMode) {
                3 -> "pack:$iconPackPackage:$packageName:$sizePx"
                4 -> if (activityClass.isNotEmpty() && userHandle != null) {
                    "mono:$tintArgb:$packageName:${userHandle}:$activityClass:$sizePx"
                } else {
                    "mono:$tintArgb:$packageName:$sizePx"
                }
                5 -> if (activityClass.isNotEmpty() && userHandle != null) {
                    "minimal:$tintArgb:$packageName:${userHandle}:$activityClass:$sizePx"
                } else {
                    "minimal:$tintArgb:$packageName:$sizePx"
                }
                6 -> if (activityClass.isNotEmpty() && userHandle != null) {
                    "filled:$tintArgb:$packageName:${userHandle}:$activityClass:$sizePx"
                } else {
                    "filled:$tintArgb:$packageName:$sizePx"
                }
                2 -> if (activityClass.isNotEmpty() && userHandle != null) {
                    "full:$packageName:${userHandle}:$activityClass:$sizePx"
                } else {
                    "full:$packageName:$sizePx"
                }
                else -> "full:$packageName:$sizePx"
            }
        }
    }

    fun isSyntheticPackage(packageName: String): Boolean {
        return packageName.startsWith("com.inkos.") || packageName.startsWith("app.inkos")
    }

    fun isInkOSInternalShortcut(shortcutId: String): Boolean {
        return shortcutId.startsWith("inkos_")
    }

    fun buildTintColorMatrix(tintColor: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.ColorMatrix {
        val tR = tintColor.red
        val tG = tintColor.green
        val tB = tintColor.blue
        val textLum = 0.2126f * tR + 0.7152f * tG + 0.0722f * tB
        val isDark = textLum < 0.5f
        val contrast = tintContrast
        val aR = (if (isDark) -0.2126f else 0.2126f) * contrast
        val aG = (if (isDark) -0.7152f else 0.7152f) * contrast
        val aB = (if (isDark) -0.0722f else 0.0722f) * contrast
        val aOff = if (isDark) 255f * contrast else 255f * (1f - contrast)
        val alphaWeight = 2f
        return androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, tR * 255f,
            0f, 0f, 0f, 0f, tG * 255f,
            0f, 0f, 0f, 0f, tB * 255f,
            aR, aG, aB, alphaWeight, aOff - alphaWeight * 255f
        ))
    }

    fun getInkOSFullIconBitmap(context: Context, sizePx: Int): Bitmap? {
        if (sizePx <= 0) return null

        val cacheKey = "inkos_full:$sizePx"
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(context.packageName)
            val bitmap = if (drawable is AdaptiveIconDrawable) {
                renderAdaptiveIcon(drawable, sizePx)
            } else {
                drawableToBitmap(drawable, sizePx)
            }
            bitmapCache.put(cacheKey, bitmap)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error getting inkOS full icon bitmap", e)
            null
        }
    }

    fun getSyntheticIconBitmap(context: Context, packageName: String, sizePx: Int): Bitmap? {
        if (sizePx <= 0) return null

        val drawableRes = getSyntheticDrawableRes(packageName)
            ?: return getInkOSFullIconBitmap(context, sizePx)

        val cacheKey = "synthetic:$packageName:$sizePx"
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val drawable = ContextCompat.getDrawable(context, drawableRes)
                ?: return getInkOSFullIconBitmap(context, sizePx)

            val size = sizePx.coerceAtLeast(1)
            val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(Color.WHITE)

            val padding = (size * 0.2f).toInt()
            drawable.setBounds(padding, padding, size - padding, size - padding)
            drawable.draw(canvas)

            bitmapCache.put(cacheKey, bitmap)
            bitmap
        } catch (_: Exception) {
            getInkOSFullIconBitmap(context, sizePx)
        }
    }

    private fun getSyntheticDrawableRes(packageName: String): Int? {
        return when {
            packageName.startsWith(Constants.INTERNAL_CONTACT_PREFIX) -> R.drawable.ic_contacts
            packageName == Constants.INTERNAL_MUSIC -> R.drawable.ic_music
            packageName == Constants.INTERNAL_FILES -> R.drawable.ic_files
            packageName == Constants.INTERNAL_WEB_SEARCH -> R.drawable.ic_web
            packageName == Constants.INTERNAL_SETTINGS -> R.drawable.ic_settings
            else -> null
        }
    }

    fun getFullAppIconBitmap(
        context: Context,
        packageName: String,
        sizePx: Int,
        activityClass: String = "",
        userHandle: UserHandle? = null
    ): Bitmap? {
        if (packageName.isBlank() || sizePx <= 0) return null

        val cacheKey = buildCacheKey(packageName, 2, sizePx, false, "", activityClass, userHandle)
        bitmapCache.get(cacheKey)?.let { return it }

        val sourceIcon = resolveSourceIconDrawable(context, packageName, activityClass, userHandle) ?: return null
        val bitmap = if (sourceIcon is AdaptiveIconDrawable) {
            renderAdaptiveIcon(sourceIcon, sizePx)
        } else {
            drawableToBitmap(sourceIcon, sizePx)
        }
        bitmapCache.put(cacheKey, bitmap)
        return bitmap
    }

    private fun resolveSourceIconDrawable(
        context: Context,
        packageName: String,
        activityClass: String,
        userHandle: UserHandle?
    ): Drawable? {
        if (activityClass.isNotEmpty() && userHandle != null) {
            try {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
                if (launcherApps != null) {
                    val activities = launcherApps.getActivityList(packageName, userHandle)
                    val matching = activities.find { it.name == activityClass }
                        ?: activities.firstOrNull()
                    if (matching != null) {
                        val density = context.resources.displayMetrics.densityDpi
                        return matching.getIcon(density)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "LauncherApps icon failed for $packageName, falling back to PackageManager", e)
            }
        }
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found for icon: $packageName", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting source icon for $packageName", e)
            null
        }
    }

    fun getMonochromeAppIconBitmap(
        context: Context,
        packageName: String,
        sizePx: Int,
        tintArgb: Int,
        activityClass: String = "",
        userHandle: UserHandle? = null
    ): Bitmap? {
        if (packageName.isBlank() || sizePx <= 0) return null

        val sourceIcon = resolveSourceIconDrawable(context, packageName, activityClass, userHandle) ?: return null

        val adaptiveSource: AdaptiveIconDrawable? = when {
            sourceIcon is AdaptiveIconDrawable -> sourceIcon
            else -> try {
                context.packageManager.getApplicationIcon(packageName) as? AdaptiveIconDrawable
            } catch (_: Exception) { null }
        }

        if (adaptiveSource != null) {
            return bakeMatrixTint(renderAdaptiveIcon(adaptiveSource, sizePx), tintArgb, sizePx)
        }
        // Non-adaptive: legacy mono PNG → SRC_IN tint at native sizePx so dark-ink icons stay
        // visible in dark mode (matrix tint zeroes out alpha for uniform-luminance ink).
        val raw = drawableToBitmap(sourceIcon, sizePx)
        if (isEffectivelyMonochrome(raw)) {
            raw.recycle()
            val tinted = sourceIcon.mutate()
            tinted.setTint(tintArgb)
            tinted.setTintMode(PorterDuff.Mode.SRC_IN)
            return drawableToBitmap(tinted, sizePx)
        }
        return bakeMatrixTint(raw, tintArgb, sizePx)
    }

    private fun getInkOSMonochromeDrawable(context: Context): Drawable? {
        return try {
            val pm = context.packageManager
            val ourIcon = pm.getApplicationIcon(context.packageName) as? AdaptiveIconDrawable
            val mono: Drawable? = if (ourIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ourIcon.monochrome
            } else {
                extractMonochromeFromManifest(context, context.packageName)
            }
            mono ?: ourIcon?.foreground
                ?: ContextCompat.getDrawable(context, R.drawable.ic_foreground)
        } catch (_: Exception) {
            ContextCompat.getDrawable(context, R.drawable.ic_foreground)
        }
    }

    fun getMinimalAppIconBitmap(
        context: Context, packageName: String, sizePx: Int, tintArgb: Int,
        activityClass: String = "", userHandle: UserHandle? = null, iconShapeId: Int = -1
    ): Bitmap? = getMinimalOrFilledIconBitmap(context, packageName, sizePx, tintArgb, 0, activityClass, userHandle, iconShapeId, filled = false)

    fun getFilledAppIconBitmap(
        context: Context, packageName: String, sizePx: Int, tintArgb: Int, bgArgb: Int,
        activityClass: String = "", userHandle: UserHandle? = null, iconShapeId: Int = -1
    ): Bitmap? = getMinimalOrFilledIconBitmap(context, packageName, sizePx, tintArgb, bgArgb, activityClass, userHandle, iconShapeId, filled = true)

    private fun getMinimalOrFilledIconBitmap(
        context: Context, packageName: String, sizePx: Int, tintArgb: Int, bgArgb: Int,
        activityClass: String, userHandle: UserHandle?, iconShapeId: Int, filled: Boolean
    ): Bitmap? {
        if (packageName.isBlank() || sizePx <= 0) return null
        val sourceIcon = resolveSourceIconDrawable(context, packageName, activityClass, userHandle) ?: return null
        val size = sizePx.coerceAtLeast(1)
        val rawSize = size * 2

        val adaptiveSource: AdaptiveIconDrawable? = when {
            sourceIcon is AdaptiveIconDrawable -> sourceIcon
            else -> try {
                context.packageManager.getApplicationIcon(packageName) as? AdaptiveIconDrawable
            } catch (_: Exception) { null }
        }

        if (adaptiveSource != null) {
            // Type 1a: explicit monochrome layer
            val mono: Drawable? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                adaptiveSource.monochrome
            } else {
                extractMonochromeFromManifest(context, packageName, activityClass)
            }
            if (mono != null) {
                return finalizeType1or2(renderMinimalSilhouette(mono, sizePx, tintArgb), sizePx, tintArgb, bgArgb, iconShapeId, filled)
            }
            adaptiveSource.foreground?.let { fg ->
                val mut = fg.mutate()
                mut.setTintList(null)
                val raw = createBitmap(rawSize, rawSize, Bitmap.Config.ARGB_8888)
                mut.setBounds(0, 0, rawSize, rawSize)
                mut.draw(Canvas(raw))

                if (isEffectivelyMonochrome(raw)) {
                    val sil = renderMinimalSilhouetteFromBitmap(raw, sizePx, tintArgb)
                    raw.recycle()
                    return finalizeType1or2(sil, sizePx, tintArgb, bgArgb, iconShapeId, filled)
                }
                // Type 2: bg absent → matrix-tint of fg-only
                if (!isBackgroundPresent(adaptiveSource)) {
                    val tinted = bakeMatrixTint(raw, tintArgb, rawSize)
                    if (tinted !== raw) raw.recycle()
                    return finalizeType1or2(normalizeBitmap(tinted, size), sizePx, tintArgb, bgArgb, iconShapeId, filled)
                }
                // Type 3: bg+fg composite → luminance silhouette + clip (Filled also pads to glyph size first).
                // System-wrapped legacy icons land here too (solid bg + fg of original raster), so force-shape the glyph.
                raw.recycle()
                val composite = renderAdaptiveIcon(adaptiveSource, rawSize)
                return renderType3LuminanceSilhouette(composite, sizePx, tintArgb, bgArgb, iconShapeId, filled, clipGlyphToShape = true)
            }
        }
        // Non-adaptive: legacy mono → silhouette; else Type 3 (Filled also pads).
        val srcBitmap = createBitmap(rawSize, rawSize, Bitmap.Config.ARGB_8888)
        val mut = sourceIcon.mutate()
        mut.setTintList(null)
        mut.setBounds(0, 0, rawSize, rawSize)
        mut.draw(Canvas(srcBitmap))
        if (isEffectivelyMonochrome(srcBitmap)) {
            val sil = renderMinimalSilhouetteFromBitmap(srcBitmap, sizePx, tintArgb)
            srcBitmap.recycle()
            return finalizeType1or2(sil, sizePx, tintArgb, bgArgb, iconShapeId, filled)
        }
        return renderType3LuminanceSilhouette(srcBitmap, sizePx, tintArgb, bgArgb, iconShapeId, filled, clipGlyphToShape = true)
    }

    private fun finalizeType1or2(silhouette: Bitmap, sizePx: Int, tintArgb: Int, bgArgb: Int, iconShapeId: Int, filled: Boolean): Bitmap =
        if (filled) invertToFilledTile(silhouette, sizePx, tintArgb, bgArgb, iconShapeId) else silhouette

    private fun renderType3LuminanceSilhouette(src: Bitmap, sizePx: Int, tintArgb: Int, bgArgb: Int, iconShapeId: Int, filled: Boolean, clipGlyphToShape: Boolean = false): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        return if (filled) {
            val glyphSize = (size * FILLED_GLYPH_SCALE).toInt().coerceAtLeast(1)
            val rawSil = renderLuminanceSilhouette(src, glyphSize, tintArgb)
            src.recycle()
            val sil = if (clipGlyphToShape) clipBitmapToIconShape(rawSil, iconShapeId) else rawSil
            invertToFilledTile(sil, sizePx, tintArgb, bgArgb, iconShapeId)
        } else {
            val sil = renderLuminanceSilhouette(src, size, tintArgb)
            src.recycle()
            clipBitmapToIconShape(sil, iconShapeId)
        }
    }

    private const val FILLED_GLYPH_SCALE = 0.55f

    private fun invertToFilledTile(silhouette: Bitmap, sizePx: Int, tintArgb: Int, bgArgb: Int, iconShapeId: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val glyphSize = (size * FILLED_GLYPH_SCALE).toInt().coerceAtLeast(1)
        val glyph = if (silhouette.width != glyphSize || silhouette.height != glyphSize) {
            val s = silhouette.scale(glyphSize, glyphSize, true)
            if (s !== silhouette) silhouette.recycle()
            s
        } else silhouette

        val tile = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tile)
        canvas.drawColor(tintArgb)
        val left = (size - glyphSize) / 2f
        val top = (size - glyphSize) / 2f
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = PorterDuffColorFilter(bgArgb, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(glyph, left, top, paint)
        glyph.recycle()
        return clipBitmapToIconShape(tile, iconShapeId)
    }

    private fun isBackgroundPresent(adaptive: AdaptiveIconDrawable): Boolean {
        val bg = adaptive.background ?: return false
        val probeSize = 16
        val probe = createBitmap(probeSize, probeSize, Bitmap.Config.ARGB_8888)
        bg.setBounds(0, 0, probeSize, probeSize)
        bg.draw(Canvas(probe))
        val coverage = alphaCoverage(probe)
        probe.recycle()
        return coverage > BG_PRESENT_COVERAGE_THRESHOLD
    }

    private const val BG_PRESENT_COVERAGE_THRESHOLD = 0.05f

    private const val LUM_CUTOUT_PERCENTILE = 0.70f

    private fun alphaCoverage(bmp: Bitmap): Float {
        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        var opaque = 0
        var total = 0
        val step = 4
        var y = 0
        while (y < h) {
            var x = 0
            val rowBase = y * w
            while (x < w) {
                if ((pixels[rowBase + x] ushr 24) and 0xff > 64) opaque++
                total++
                x += step
            }
            y += step
        }
        return if (total == 0) 0f else opaque.toFloat() / total
    }

    private fun renderLuminanceSilhouette(srcBitmap: Bitmap, sizePx: Int, tintArgb: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        // Downscale source to target size first so we work on fewer pixels.
        val scaled = if (srcBitmap.width != size || srcBitmap.height != size) {
            srcBitmap.scale(size, size, true)
        } else srcBitmap

        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)

        // Find luminance range among opaque pixels — defines the icon's "dark vs bright" split.
        var minLum = 1f
        var maxLum = 0f
        for (p in pixels) {
            if ((p ushr 24) and 0xff > 64) {
                val r = (p shr 16) and 0xff
                val g = (p shr 8) and 0xff
                val b = p and 0xff
                val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
                if (lum < minLum) minLum = lum
                if (lum > maxLum) maxLum = lum
            }
        }
        val cutoutThreshold = minLum + (maxLum - minLum) * LUM_CUTOUT_PERCENTILE
        val tintRGB = tintArgb and 0x00ffffff
        val opaqueTint = (255 shl 24) or tintRGB

        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xff
            if (a < 64) {
                pixels[i] = opaqueTint
                continue
            }
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
            pixels[i] = if (lum > cutoutThreshold) 0 else opaqueTint
        }

        val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== srcBitmap) scaled.recycle()
        return out
    }

    private fun clipBitmapToIconShape(src: Bitmap, iconShapeId: Int): Bitmap {
        if (iconShapeId < 0 || iconShapeId == 2) return src
        val w = src.width
        val h = src.height
        val result = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val path = android.graphics.Path()
        val rect = android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat())
        when (iconShapeId) {
            0 -> path.addOval(rect, android.graphics.Path.Direction.CW)
            1 -> {
                val r = w * 0.10f
                path.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW)
            }
        }
        canvas.clipPath(path)
        canvas.drawBitmap(src, 0f, 0f, null)
        src.recycle()
        return result
    }

    private fun isEffectivelyMonochrome(src: Bitmap): Boolean {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val seen = HashSet<Int>(8)
        val step = 4
        var y = 0
        while (y < h) {
            var x = 0
            val rowBase = y * w
            while (x < w) {
                val p = pixels[rowBase + x]
                if ((p ushr 24) and 0xff > 64) {
                    val r = (p shr 20) and 0xf
                    val g = (p shr 12) and 0xf
                    val b = (p shr 4) and 0xf
                    seen.add((r shl 8) or (g shl 4) or b)
                    if (seen.size > 3) return false
                }
                x += step
            }
            y += step
        }
        return true
    }

    private fun renderMinimalSilhouette(drawable: Drawable, sizePx: Int, tintArgb: Int): Bitmap {
        val tinted = drawable.mutate()
        tinted.setTint(tintArgb)
        tinted.setTintMode(PorterDuff.Mode.SRC_IN)
        return renderDrawableNormalized(tinted, sizePx)
    }

    private fun renderMinimalSilhouetteFromBitmap(src: Bitmap, sizePx: Int, tintArgb: Int): Bitmap {
        val tinted = createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = android.graphics.PorterDuffColorFilter(tintArgb, PorterDuff.Mode.SRC_IN)
        }
        Canvas(tinted).drawBitmap(src, 0f, 0f, paint)
        return normalizeBitmap(tinted, sizePx.coerceAtLeast(1))
    }

    private fun renderDrawableNormalized(drawable: Drawable, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val rawSize = size * 2 // oversample for sharp scale-down
        val raw = createBitmap(rawSize, rawSize, Bitmap.Config.ARGB_8888)
        val rawCanvas = Canvas(raw)
        drawable.setBounds(0, 0, rawSize, rawSize)
        drawable.draw(rawCanvas)
        return normalizeBitmap(raw, size)
    }

    private fun normalizeBitmap(src: Bitmap, size: Int): Bitmap {
        val srcW = src.width
        val srcH = src.height
        val pixels = IntArray(srcW * srcH)
        src.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)
        var minX = srcW
        var minY = srcH
        var maxX = -1
        var maxY = -1
        for (y in 0 until srcH) {
            val rowBase = y * srcW
            for (x in 0 until srcW) {
                val a = (pixels[rowBase + x] ushr 24) and 0xff
                if (a > 16) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) {
            src.recycle()
            return createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }
        val w = maxX - minX + 1
        val h = maxY - minY + 1
        val padding = 0.04f
        val targetMax = size * (1f - 2f * padding)
        val scale = targetMax / maxOf(w, h)
        val scaledW = (w * scale).toInt().coerceAtLeast(1)
        val scaledH = (h * scale).toInt().coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(src, minX, minY, w, h)
        src.recycle()
        val scaled = if (scaledW != w || scaledH != h) {
            val s = cropped.scale(scaledW, scaledH, true)
            if (s !== cropped) cropped.recycle()
            s
        } else cropped

        val result = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(result)
        val left = (size - scaledW) / 2f
        val top = (size - scaledH) / 2f
        resultCanvas.drawBitmap(scaled, left, top, null)
        scaled.recycle()
        return result
    }

    private fun extractMonochromeFromManifest(
        context: Context,
        packageName: String,
        activityClass: String = ""
    ): Drawable? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val res = pm.getResourcesForApplication(appInfo)

            val candidates = mutableListOf<Int>()
            if (activityClass.isNotEmpty()) {
                try {
                    val activityInfo = pm.getActivityInfo(android.content.ComponentName(packageName, activityClass), 0)
                    if (activityInfo.icon != 0) candidates += activityInfo.icon
                } catch (_: Exception) {}
            }
            if (appInfo.icon != 0) candidates += appInfo.icon

            try {
                val field = appInfo.javaClass.getField("roundIconResource")
                val roundRes = field.getInt(appInfo)
                if (roundRes != 0) candidates += roundRes
            } catch (_: Throwable) {}

            if (candidates.isEmpty()) return null

            val androidNs = "http://schemas.android.com/apk/res/android"
            for (iconRes in candidates.distinct()) {
                var drawableRes = 0
                val parser: XmlResourceParser = try { res.getXml(iconRes) } catch (_: Exception) { continue }
                try {
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG && parser.name == "monochrome") {
                            drawableRes = parser.getAttributeResourceValue(androidNs, "drawable", 0)
                            break
                        }
                        event = parser.next()
                    }
                } finally {
                    parser.close()
                }
                if (drawableRes != 0) {
                    return ResourcesCompat.getDrawable(res, drawableRes, null)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private const val ADAPTIVE_LAYER_RATIO = 108f / 72f

    private fun cropToSafeArea(layerBitmap: Bitmap, sizePx: Int): Bitmap {
        val layerSize = layerBitmap.width
        val offset = (layerSize - sizePx) / 2
        val result = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        Canvas(result).drawBitmap(layerBitmap, -offset.toFloat(), -offset.toFloat(), null)
        layerBitmap.recycle()
        return result
    }

    private fun renderMonochromeBaked(drawable: Drawable, sizePx: Int, tintArgb: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val layerSize = (size * ADAPTIVE_LAYER_RATIO).toInt()

        val tinted = drawable.mutate()
        tinted.setTint(tintArgb)
        tinted.setTintMode(PorterDuff.Mode.SRC_IN)

        val layerBitmap = createBitmap(layerSize, layerSize, Bitmap.Config.ARGB_8888)
        tinted.setBounds(0, 0, layerSize, layerSize)
        tinted.draw(Canvas(layerBitmap))
        return cropToSafeArea(layerBitmap, size)
    }

    private fun bakeMatrixTint(srcBitmap: Bitmap, tintArgb: Int, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val filter = tintFilterCache.getOrPut(tintArgb) {
            val tintColor = androidx.compose.ui.graphics.Color(tintArgb)
            val composeMatrix = buildTintColorMatrix(tintColor)
            ColorMatrixColorFilter(android.graphics.ColorMatrix(composeMatrix.values))
        }
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = filter
        }
        val result = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val srcRect = android.graphics.Rect(0, 0, srcBitmap.width, srcBitmap.height)
        val dstRect = android.graphics.Rect(0, 0, size, size)
        canvas.drawBitmap(srcBitmap, srcRect, dstRect, paint)
        return result
    }

    @Suppress("NewApi")
    private fun renderAdaptiveIcon(icon: AdaptiveIconDrawable, sizePx: Int): Bitmap {
        val size = sizePx.coerceAtLeast(1)
        val layerSize = (size * ADAPTIVE_LAYER_RATIO).toInt()

        val layerBitmap = createBitmap(layerSize, layerSize, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(layerBitmap)

        icon.background?.let { bg ->
            bg.setBounds(0, 0, layerSize, layerSize)
            bg.draw(layerCanvas)
        }
        icon.foreground?.let { fg ->
            fg.setBounds(0, 0, layerSize, layerSize)
            fg.draw(layerCanvas)
        }
        return cropToSafeArea(layerBitmap, size)
    }

    private const val SHORTCUT_ICON_DIR = "shortcut_icons"

    private fun shortcutIconFileName(packageName: String, shortcutId: String): String {
        val raw = "${packageName}_${shortcutId}"
        return raw.replace(Regex("[^a-zA-Z0-9._\\-]"), "_") + ".png"
    }

    fun savePinnedShortcutIcon(context: Context, packageName: String, shortcutId: String, drawable: Drawable) {
        try {
            val bitmap = drawableToShortcutBitmap(drawable, 128)
            val dir = java.io.File(context.filesDir, SHORTCUT_ICON_DIR).also { it.mkdirs() }
            val file = java.io.File(dir, shortcutIconFileName(packageName, shortcutId))
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not save shortcut icon for $packageName/$shortcutId", e)
        }
    }

    fun deleteShortcutIconsForPackage(context: Context, packageName: String) {
        try {
            val dir = java.io.File(context.filesDir, SHORTCUT_ICON_DIR)
            if (!dir.exists()) return
            val sanitizedPrefix = packageName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_") + "_"
            dir.listFiles()?.forEach { file ->
                if (file.name.startsWith(sanitizedPrefix)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not clean shortcut icons for $packageName", e)
        }
    }

    private fun loadPinnedShortcutIcon(context: Context, packageName: String, shortcutId: String, sizePx: Int): Bitmap? {
        return try {
            val file = java.io.File(context.filesDir, "$SHORTCUT_ICON_DIR/${shortcutIconFileName(packageName, shortcutId)}")
            if (!file.exists()) return null
            val raw = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return null
            if (raw.width == sizePx && raw.height == sizePx) raw
            else raw.scale(sizePx, sizePx, true)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToShortcutBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        return if (drawable is AdaptiveIconDrawable) {
            renderAdaptiveIcon(drawable, sizePx)
        } else {
            drawableToBitmap(drawable, sizePx)
        }
    }

    fun getShortcutIconBitmap(
        context: Context,
        packageName: String,
        shortcutId: String,
        userHandle: UserHandle?,
        sizePx: Int
    ): Bitmap? {
        if (sizePx <= 0) return null

        val cacheKey = "shortcut:$packageName:$shortcutId:${userHandle}:$sizePx"
        bitmapCache.get(cacheKey)?.let { return it }

        val systemBitmap = try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            if (launcherApps != null && launcherApps.hasShortcutHostPermission()) {
                val profile = userHandle ?: android.os.Process.myUserHandle()
                val query = LauncherApps.ShortcutQuery().apply {
                    setPackage(packageName)
                    setShortcutIds(listOf(shortcutId))
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                    )
                }
                val shortcuts = launcherApps.getShortcuts(query, profile)
                val shortcut = shortcuts?.firstOrNull()
                if (shortcut != null) {
                    val density = context.resources.displayMetrics.densityDpi
                    val drawable = launcherApps.getShortcutIconDrawable(shortcut, density)
                    if (drawable != null) drawableToShortcutBitmap(drawable, sizePx) else null
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Shortcut icon query failed for $packageName/$shortcutId", e)
            null
        }

        val result = systemBitmap
            ?: loadPinnedShortcutIcon(context, packageName, shortcutId, sizePx)

        if (result != null) bitmapCache.put(cacheKey, result)
        return result
    }

    fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val bitmap = createBitmap(
            sizePx.coerceAtLeast(1),
            sizePx.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun generateIconCodes(labels: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val usedCodes = mutableSetOf<String>()
        
        for (label in labels) {
            val code = generateCodeForLabel(label, usedCodes)
            result[label] = code
            usedCodes.add(code)
        }
        
        return result
    }
    
    fun generateCodeForLabel(label: String, usedCodes: Set<String> = emptySet()): String {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return ""
        
        val letters = trimmed.filter { it.isLetter() }

        if (letters.length == 1) {
            return letters[0].uppercaseChar().toString()
        }
        
        val firstLetter = trimmed.firstOrNull { it.isLetter() }?.uppercaseChar() ?: 'X'
        val secondLetter = findSecondLetter(trimmed)?.lowercaseChar()

        if (secondLetter == null) {
            return firstLetter.toString()
        }
        
        val baseCode = "$firstLetter$secondLetter"

        if (usedCodes.contains(baseCode)) {
            return resolveConflict(baseCode, trimmed, usedCodes)
        }
        
        return baseCode
    }
    
    private fun findSecondLetter(text: String): Char? {
        var foundFirstLetter = false

        for (i in text.indices) {
            if (text[i].isLetter()) {
                if (!foundFirstLetter) {
                    foundFirstLetter = true
                }
            } else if (foundFirstLetter) {
                for (j in i until text.length) {
                    if (text[j].isLetter()) {
                        return text[j]
                    }
                }
                break
            }
        }
        
        if (foundFirstLetter) {
            for (i in 1 until text.length) {
                if (text[i].isLetter()) {
                    return text[i]
                }
            }
        }
        
        return null
    }
    
    private fun resolveConflict(baseCode: String, text: String, usedCodes: Set<String>): String {
        val firstLetter = baseCode[0].uppercaseChar()
        val letters = text.filter { it.isLetter() }
        
        if (letters.length >= 2) {
            if (letters.length >= 3) {
                val candidate = "${firstLetter}${letters[2].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
            
            if (letters.length >= 4) {
                val candidate = "${firstLetter}${letters[3].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
            
            if (letters.length >= 3) {
                val candidate = "${letters[1].uppercaseChar()}${letters[2].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
            
            if (letters.length >= 5) {
                val candidate = "${firstLetter}${letters[4].lowercaseChar()}"
                if (!usedCodes.contains(candidate)) {
                    return candidate
                }
            }
        }
        
        var counter = 1
        while (true) {
            val candidate = "${firstLetter}${counter}"
            if (!usedCodes.contains(candidate)) {
                return candidate
            }
            counter++
            if (counter > 99) break
        }
        
        return baseCode
    }
}
