package com.github.gezimos.inkos.helper

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
@SuppressLint("StaticFieldLeak")
class AppWidgetHelper private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AppWidgetHelper"

        @Volatile
        private var INSTANCE: AppWidgetHelper? = null

        fun getInstance(context: Context): AppWidgetHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppWidgetHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val appWidgetHost = AppWidgetHost(context, Constants.APPWIDGET_HOST_ID)
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val prefs = Prefs(context)

    private val _widgetView = MutableStateFlow<AppWidgetHostView?>(null)
    val widgetViewState: StateFlow<AppWidgetHostView?> = _widgetView

    private var isListening = false
    fun startListening() {
        if (!isListening) {
            try {
                appWidgetHost.startListening()
                isListening = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AppWidgetHost listening", e)
            }
        }
        // Restore previously bound widget if available
        restoreWidget()
    }
    fun stopListening() {
        if (isListening) {
            try {
                appWidgetHost.stopListening()
                isListening = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AppWidgetHost listening", e)
            }
        }
    }
    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }
    fun deallocateWidgetId(widgetId: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(widgetId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deallocate widget ID $widgetId", e)
        }
    }
    fun buildPickerIntent(widgetId: Int): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
    }
    fun getWidgetInfo(widgetId: Int): AppWidgetProviderInfo? {
        return try {
            appWidgetManager.getAppWidgetInfo(widgetId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get widget info for ID $widgetId", e)
            null
        }
    }
    fun isWidgetBound(widgetId: Int): Boolean {
        return getWidgetInfo(widgetId) != null
    }
    fun bindWidget(widgetId: Int, providerInfo: AppWidgetProviderInfo): Boolean {
        return try {
            appWidgetManager.bindAppWidgetIdIfAllowed(
                widgetId,
                providerInfo.provider
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind widget", e)
            false
        }
    }
    fun createWidgetView(widgetId: Int): AppWidgetHostView? {
        val info = getWidgetInfo(widgetId) ?: return null
        return try {
            appWidgetHost.createView(context, widgetId, info).also {
                it.setAppWidget(widgetId, info)
                it.setPadding(0, 0, 0, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create widget view for ID $widgetId", e)
            null
        }
    }
    fun onWidgetPicked(widgetId: Int) {
        prefs.androidWidgetId = widgetId
        val view = createWidgetView(widgetId)
        _widgetView.value = view
    }
    fun removeWidget() {
        val currentId = prefs.androidWidgetId
        if (currentId != -1) {
            deallocateWidgetId(currentId)
            prefs.androidWidgetId = -1
        }
        _widgetView.value = null
    }
    fun getConfigureIntent(widgetId: Int): Intent? {
        val info = getWidgetInfo(widgetId) ?: return null
        val configureActivity = info.configure ?: return null
        return Intent().apply {
            component = configureActivity
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
    }
    private fun restoreWidget() {
        val widgetId = prefs.androidWidgetId
        if (widgetId != -1 && prefs.showAndroidWidget) {
            val view = createWidgetView(widgetId)
            if (view != null) {
                _widgetView.value = view
            } else {
                // Widget no longer valid — clean up
                Log.w(TAG, "Stored widget ID $widgetId is no longer valid, clearing")
                prefs.androidWidgetId = -1
                _widgetView.value = null
            }
        } else {
            _widgetView.value = null
        }
    }
    fun getCurrentWidgetLabel(): String? {
        val widgetId = prefs.androidWidgetId
        if (widgetId == -1) return null
        val info = getWidgetInfo(widgetId) ?: return null
        return try {
            info.loadLabel(context.packageManager)
        } catch (_: Exception) {
            info.provider.shortClassName
        }
    }
    fun getInstalledWidgets(): List<Pair<String, AppWidgetProviderInfo>> {
        return try {
            appWidgetManager.installedProviders
                .map { info ->
                    val label = try {
                        info.loadLabel(context.packageManager)
                    } catch (_: Exception) {
                        info.provider.shortClassName
                    }
                    label to info
                }
                .sortedBy { it.first.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed widgets", e)
            emptyList()
        }
    }
    data class WidgetApp(
        val packageName: String,
        val appLabel: String,
        val widgets: List<Pair<String, AppWidgetProviderInfo>>
    )
    fun getInstalledWidgetsByApp(): List<WidgetApp> {
        return try {
            val pm = context.packageManager
            appWidgetManager.installedProviders
                .groupBy { it.provider.packageName }
                .map { (packageName, providers) ->
                    val appLabel = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                    } catch (_: Exception) {
                        packageName
                    }
                    val widgets = providers.map { info ->
                        val label = try {
                            info.loadLabel(pm)
                        } catch (_: Exception) {
                            info.provider.shortClassName
                        }
                        label to info
                    }.sortedBy { it.first.lowercase() }
                    WidgetApp(packageName, appLabel, widgets)
                }
                .sortedBy { it.appLabel.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed widgets by app", e)
            emptyList()
        }
    }
    fun loadWidgetPreview(info: AppWidgetProviderInfo): android.graphics.drawable.Drawable? {
        return try {
            val pm = context.packageManager
            info.loadPreviewImage(context, 0)
                ?: inflatePreviewLayout(info)
                ?: info.loadIcon(context, 0)
                ?: pm.getApplicationIcon(info.provider.packageName)
        } catch (_: Exception) {
            null
        }
    }
    private fun inflatePreviewLayout(info: AppWidgetProviderInfo): android.graphics.drawable.Drawable? {
        val layoutId = info.previewLayout
        if (layoutId == 0 || info.provider.packageName != context.packageName) return null
        return try {
            val view = LayoutInflater.from(context).inflate(layoutId, null)
            val density = context.resources.displayMetrics.density
            val widthPx = (info.minWidth * density).toInt().coerceAtLeast(1)
            val heightPx = (info.minHeight * density).toInt().coerceAtLeast(1)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            BitmapDrawable(context.resources, bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inflate preview layout", e)
            null
        }
    }
    fun pickWidgetFromProvider(
        providerInfo: AppWidgetProviderInfo,
        onSuccess: (widgetId: Int) -> Unit,
        onNeedsPermission: (widgetId: Int, intent: Intent) -> Unit,
        onNeedsConfigure: (widgetId: Int, configIntent: Intent) -> Unit
    ) {
        // Clean up old widget
        val oldId = prefs.androidWidgetId
        if (oldId != -1) {
            removeWidget()
        }

        val widgetId = allocateWidgetId()

        // Try binding directly
        val bound = bindWidget(widgetId, providerInfo)
        if (!bound) {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
            }
            onNeedsPermission(widgetId, bindIntent)
            return
        }

        // Check if configure activity is needed
        val configureActivity = providerInfo.configure
        if (configureActivity != null) {
            val configIntent = Intent().apply {
                component = configureActivity
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            onNeedsConfigure(widgetId, configIntent)
            return
        }

        // All good — create view and persist
        onWidgetPicked(widgetId)
        onSuccess(widgetId)
    }
}
