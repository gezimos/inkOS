package com.github.gezimos.inkos.helper

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.gezimos.inkos.data.Prefs
import java.util.concurrent.TimeUnit

class WallpaperRotationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        if (!prefs.wallpaperRotationEnabled) return Result.success()

        val items = buildRotationItems(prefs)
        if (items.isEmpty()) return Result.success()

        val currentIndex = prefs.wallpaperRotationIndex % items.size
        val item = items[currentIndex]
        val utility = WallpaperUtility(applicationContext)
        val flags = android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK

        when (item) {
            is RotationItem.Preset -> utility.setWallpaperFromResource(item.resourceId, flags)
            is RotationItem.UserImage -> utility.setWallpaperFromUri(Uri.parse(item.uriString), flags)
        }

        prefs.wallpaperRotationIndex = (currentIndex + 1) % items.size

        if (prefs.wallpaperRotationInterval < 15) {
            scheduleOneShot(applicationContext, prefs.wallpaperRotationInterval)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "wallpaper_rotation"
        private const val WORK_NAME_SHORT = "wallpaper_rotation_short"

        fun schedule(context: Context, intervalMinutes: Int) {
            val wm = WorkManager.getInstance(context)
            if (intervalMinutes < 15) {
                wm.cancelUniqueWork(WORK_NAME)
                scheduleOneShot(context, intervalMinutes)
            } else {
                wm.cancelUniqueWork(WORK_NAME_SHORT)
                val request = PeriodicWorkRequestBuilder<WallpaperRotationWorker>(
                    intervalMinutes.toLong(), TimeUnit.MINUTES
                ).build()
                wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
            }
        }

        internal fun scheduleOneShot(context: Context, delayMinutes: Int) {
            val request = OneTimeWorkRequestBuilder<WallpaperRotationWorker>()
                .setInitialDelay(delayMinutes.toLong(), TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_SHORT, ExistingWorkPolicy.REPLACE, request
            )
        }

        fun cancel(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(WORK_NAME)
            wm.cancelUniqueWork(WORK_NAME_SHORT)
        }

        fun buildRotationItems(prefs: Prefs): List<RotationItem> {
            val items = mutableListOf<RotationItem>()
            // Preset indices
            prefs.wallpaperRotationPresets
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in WallpaperUtility.PRESET_WALLPAPERS.indices }
                .forEach { items.add(RotationItem.Preset(WallpaperUtility.PRESET_WALLPAPERS[it].resourceId)) }
            // User URIs
            prefs.wallpaperRotationUris
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { items.add(RotationItem.UserImage(it)) }
            return items
        }
    }

    sealed class RotationItem {
        data class Preset(val resourceId: Int) : RotationItem()
        data class UserImage(val uriString: String) : RotationItem()
    }
}
