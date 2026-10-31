package com.github.gezimos.inkos.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.em
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.screentime.ScreenTimeReader
import com.github.gezimos.inkos.data.screentime.ScreenTimeSummary

data class ScreenTimeDisplay(
    val summary: ScreenTimeSummary = ScreenTimeSummary(),
    val includeTotal: Boolean = true,
    val showSplit: Boolean = false,
    val showUnlocks: Boolean = false
)

class ScreenTimeLine(val text: AnnotatedString, val inlineContent: Map<String, InlineTextContent>)

private enum class ScreenTimeIcon(val vector: ImageVector, @StringRes val label: Int) {
    Online(Icons.Rounded.Wifi, R.string.screen_time_category_online),
    Offline(Icons.Rounded.WifiOff, R.string.screen_time_category_offline),
    Unlocks(Icons.Rounded.LockOpen, R.string.screen_time_unlocks)
}

private const val NO_BREAK_SPACE = ' '

fun formatScreenTimeDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${NO_BREAK_SPACE}${minutes}min"
        hours > 0 -> "${hours}h"
        else -> "${minutes}min"
    }
}

@Composable
fun screenTimeLine(display: ScreenTimeDisplay, iconTint: Color): ScreenTimeLine {
    val summary = display.summary
    val segments = buildList {
        if (display.includeTotal) add(null to formatScreenTimeDuration(summary.totalMs))
        if (display.showSplit) {
            add(ScreenTimeIcon.Online to formatScreenTimeDuration(summary.onlineMs))
            add(ScreenTimeIcon.Offline to formatScreenTimeDuration(summary.offlineMs))
        }
        if (display.showUnlocks && ScreenTimeReader.unlocksSupported) {
            add(ScreenTimeIcon.Unlocks to summary.unlocks.toString())
        }
    }
    val labels = ScreenTimeIcon.entries.associateWith { stringResource(it.label) }
    val text = buildAnnotatedString {
        segments.forEachIndexed { index, (icon, value) ->
            if (index > 0) append(" · ")
            if (icon != null) {
                appendInlineContent(icon.name, labels.getValue(icon))
                append(NO_BREAK_SPACE)
            }
            append(value)
        }
    }
    val inlineContent = ScreenTimeIcon.entries.associate { icon ->
        icon.name to InlineTextContent(Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter)) {
            Icon(icon.vector, contentDescription = null, tint = iconTint, modifier = Modifier.fillMaxSize())
        }
    }
    return ScreenTimeLine(text, inlineContent)
}
