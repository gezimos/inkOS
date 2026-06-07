package com.github.gezimos.inkos.widget

import android.app.Notification
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.github.gezimos.inkos.MainActivity
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.services.NotificationService
import com.github.gezimos.inkos.helper.IconUtility
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.data.Prefs

class NotificationWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        requestUpdate(context)
    }

    companion object {

        private val ICON_IDS = intArrayOf(R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4, R.id.icon5)
        private val COUNT_IDS = intArrayOf(R.id.count1, R.id.count2, R.id.count3, R.id.count4, R.id.count5)
        private val SEP_IDS = intArrayOf(R.id.sep1, R.id.sep2, R.id.sep3, R.id.sep4)

        /**
         * Build the widget from the current notification state and push it
         * to all NotificationWidget instances. Called directly from
         * NotificationService so updates are synchronous with system events.
         */
        fun requestUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, NotificationWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (widgetIds == null || widgetIds.isEmpty()) return

            val notificationManager = NotificationManager.getInstance(context)

            val raw = NotificationService.sbnState.value
            val prefsLocal = Prefs(context)
            val allowed = if (prefsLocal.allowedSimpleTrayApps.isNotEmpty()) prefsLocal.allowedSimpleTrayApps else prefsLocal.allowedNotificationApps
            val filtered = raw.filter { sbn ->
                sbn.notification.category != Notification.CATEGORY_TRANSPORT &&
                        !notificationManager.isNotificationSummary(sbn) &&
                        (allowed.isEmpty() || allowed.contains(sbn.packageName))
            }

            data class AppNotifInfo(val packageName: String, val activity: String?, val count: Int, val latestTime: Long)

            val perApp = filtered
                .groupBy { it.packageName }
                .map { (pkg, sbns) ->
                    val activity = sbns.firstOrNull()?.notification?.extras?.getString("android.intent.extra.COMPONENT_NAME")
                    AppNotifInfo(pkg, activity, sbns.size, sbns.maxOf { it.postTime })
                }
                .sortedByDescending { it.latestTime }
                .take(3)

            // Read user preferences
            val prefs = Prefs(context)
            val textColor = prefs.textColor
            val textSize = 18
            val iconShapePref = prefs.iconShape

            val pm = context.packageManager
            val iconSizePx = ((textSize * 1.8f) * context.resources.displayMetrics.density + 10f)
                .toInt().coerceIn(24, 96)
            val cornerRadiusPx = ShapeHelper.getCornerRadiusPx(iconShapePref, context.resources.displayMetrics.density)

            val views = RemoteViews(context.packageName, R.layout.notification_widget)

            // Tap opens SimpleTray
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_SIMPLE_TRAY", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 1001, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.notification_widget_root, pendingIntent)

            if (perApp.isEmpty()) {
                // Hide all slots, show empty icon
                for (i in 0 until 5) {
                    views.setViewVisibility(ICON_IDS[i], View.GONE)
                    views.setViewVisibility(COUNT_IDS[i], View.GONE)
                }
                for (id in SEP_IDS) views.setViewVisibility(id, View.GONE)
                views.setViewVisibility(R.id.empty_icon, View.VISIBLE)
                context.getDrawable(R.drawable.ic_foreground)?.let { drawable ->
                    androidx.core.graphics.drawable.DrawableCompat.setTint(drawable.mutate(), textColor)
                    val emptyIconSizePx = (24 * context.resources.displayMetrics.density).toInt()
                    val bmp = roundedBitmap(drawable, emptyIconSizePx, 0f)
                    views.setImageViewBitmap(R.id.empty_icon, bmp)
                } ?: views.setImageViewResource(R.id.empty_icon, R.drawable.ic_foreground)
            } else {
                views.setViewVisibility(R.id.empty_icon, View.GONE)

                for (i in 0 until 5) {
                    if (i < perApp.size) {
                        val info = perApp[i]
                        views.setViewVisibility(ICON_IDS[i], View.VISIBLE)
                        val appLaunchIntent = pm.getLaunchIntentForPackage(info.packageName)
                        val slotPendingIntent = if (appLaunchIntent != null) {
                            appLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            PendingIntent.getActivity(
                                context, 2000 + i, appLaunchIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        } else pendingIntent
                        views.setOnClickPendingIntent(ICON_IDS[i], slotPendingIntent)
                        views.setOnClickPendingIntent(COUNT_IDS[i], slotPendingIntent)
                        try {
                            val iconBitmap = IconUtility.getFullAppIconBitmap(context, info.packageName, iconSizePx)?.let {
                                roundedBitmap(it, cornerRadiusPx)
                            } ?: run {
                                val drawable = pm.getApplicationIcon(info.packageName)
                                roundedBitmap(drawable, iconSizePx, cornerRadiusPx)
                            }
                            views.setImageViewBitmap(ICON_IDS[i], iconBitmap)
                        } catch (e: Exception) {
                            Log.w("NotificationWidget", "Failed to load icon for ${info.packageName}", e)
                        }

                        // Show count
                        views.setViewVisibility(COUNT_IDS[i], View.VISIBLE)
                        views.setTextViewText(COUNT_IDS[i], info.count.toString())
                        views.setTextColor(COUNT_IDS[i], textColor)
                        views.setTextViewTextSize(
                            COUNT_IDS[i], TypedValue.COMPLEX_UNIT_SP, textSize.toFloat()
                        )

                        // Show separator between slots
                        if (i < perApp.size - 1 && i < SEP_IDS.size) {
                            views.setViewVisibility(SEP_IDS[i], View.VISIBLE)
                            views.setTextColor(SEP_IDS[i], textColor)
                            views.setTextViewTextSize(
                                SEP_IDS[i], TypedValue.COMPLEX_UNIT_SP, textSize.toFloat()
                            )
                        }
                    } else {
                        views.setViewVisibility(ICON_IDS[i], View.GONE)
                        views.setViewVisibility(COUNT_IDS[i], View.GONE)
                    }
                }
                // Hide unused separators
                for (i in (perApp.size - 1).coerceAtLeast(0) until SEP_IDS.size) {
                    views.setViewVisibility(SEP_IDS[i], View.GONE)
                }
            }

            for (id in widgetIds) {
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun roundedBitmap(drawable: Drawable, sizePx: Int, cornerRadiusPx: Float): Bitmap {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            if (cornerRadiusPx > 0f) {
                val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                val outCanvas = Canvas(output)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                outCanvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), cornerRadiusPx, cornerRadiusPx, paint)
                paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                outCanvas.drawBitmap(bitmap, 0f, 0f, paint)
                return output
            }
            return bitmap
        }
        private fun roundedBitmap(bitmap: Bitmap, cornerRadiusPx: Float): Bitmap {
            val sizePx = bitmap.width
            val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawRoundRect(RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), cornerRadiusPx, cornerRadiusPx, paint)
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            return output
        }
    }
}
