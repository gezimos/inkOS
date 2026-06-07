package com.github.gezimos.inkos.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.gezimos.inkos.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("com.github.gezimos.inkos", 0)
        val quoteText = prefs.getString("QUOTE_TEXT", "Stay inspired") ?: "Stay inspired"
        val quoteSize = prefs.getInt("QUOTE_TEXT_SIZE", 18)
        val alignment = prefs.getInt("HOME_QUOTE_ALIGNMENT", -1)
        val homeAlignment = prefs.getInt("HOME_ALIGNMENT", 0)
        val resolvedAlignment = if (alignment == -1) homeAlignment else alignment

        val textAlign = when (resolvedAlignment) {
            0 -> TextAlign.Start
            2 -> TextAlign.End
            else -> TextAlign.Center
        }
        val boxAlignment = when (resolvedAlignment) {
            0 -> Alignment.CenterStart
            2 -> Alignment.CenterEnd
            else -> Alignment.Center
        }

        val textColor = Color(prefs.getInt("TEXT_COLOR", AndroidColor.WHITE))
        val fontName = prefs.getString("QUOTE_FONT", "PublicSans") ?: "PublicSans"
        val fontFamily = mapFontFamily(fontName)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(launchIntent)),
                contentAlignment = boxAlignment
            ) {
                Text(
                    text = quoteText,
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = quoteSize.sp,
                        fontFamily = fontFamily,
                        textAlign = textAlign
                    )
                )
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val QUOTE_PREF_KEYS = setOf(
            "QUOTE_TEXT", "QUOTE_TEXT_SIZE", "QUOTE_FONT",
            "HOME_QUOTE_ALIGNMENT", "HOME_ALIGNMENT", "TEXT_COLOR"
        )

        /**
         * Maps the app's font enum names to the closest built-in font family.
         * Glance/RemoteViews can only use system font families, not custom .ttf files.
         */
        private fun mapFontFamily(name: String): FontFamily = when (name) {
            "Vollkorn", "Merriweather" -> FontFamily.Serif
            "Shortstack" -> FontFamily.Cursive
            "Hoog" -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }

        fun isQuotePrefKey(key: String): Boolean = key in QUOTE_PREF_KEYS

        fun requestUpdate(context: Context) {
            scope.launch {
                try {
                    QuoteWidget().updateAll(context)
                } catch (_: Exception) {}
            }
        }
    }
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()
}
