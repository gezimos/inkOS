package com.github.gezimos.inkos.ui.compose

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpeakerNotes
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme

object GuideScreen {

    @Composable
    fun Show(
        onFinish: () -> Unit = {}
    ) {
        val context = LocalContext.current
        val prefs = remember { Prefs(context) }
        
        // State for guide page
        var page by remember { mutableIntStateOf(0) }
        val totalPages = 4
        val settingsSize = (prefs.settingsSize - 3)
        val titleFontSize = (settingsSize * 1.5).sp
        val bodyFontSize = settingsSize.sp

        // Helper to resolve an Activity from a possibly-wrapped Context
        fun resolveActivity(ctx: android.content.Context): Activity? {
            var c: android.content.Context = ctx
            while (c is ContextWrapper) {
                if (c is Activity) return c
                c = c.baseContext
            }
            return null
        }

        // Determine background color using the current theme
        val isDark = prefs.appTheme == Constants.Theme.Dark
        // Calculate top padding for status bar when enabled
        val topPadding = if (prefs.showStatusBar) {
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        } else {
            0.dp
        }
        // Calculate bottom padding for nav bar/gestures
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        
        SettingsTheme(isDark = isDark) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Theme.colors.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    // Title at the top with status bar padding and 24dp padding
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topPadding + 24.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_foreground),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(SettingsTheme.typography.title.color),
                            modifier = Modifier.size(titleFontSize.value.dp)
                        )
                        Text(
                            text = when (page) {
                                0 -> "Guide / Home"
                                1 -> "Guide / Gestures"
                                2 -> "Guide / Home Notifications"
                                3 -> "Guide / Other Notificaitons"
                                else -> ""
                            },
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = SettingsTheme.typography.title.color
                        )
                    }
                    
                    // Content area with 24dp horizontal padding - centered vertically
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (page) {
                            0 -> {
                                // Page 1: Home
                                HomePageContent(
                                    titleFontSize = titleFontSize,
                                    bodyFontSize = bodyFontSize
                                )
                            }
                            1 -> {
                                // Page 2: Gestures
                                GesturesPageContent(
                                    titleFontSize = titleFontSize,
                                    bodyFontSize = bodyFontSize
                                )
                            }
                            2 -> {
                                // Page 3: Home Notifications
                                HomeNotificationsPageContent(
                                    titleFontSize = titleFontSize,
                                    bodyFontSize = bodyFontSize
                                )
                            }
                            3 -> {
                                // Page 4: Other Notifications
                                OtherNotificationsPageContent(
                                    titleFontSize = titleFontSize,
                                    bodyFontSize = bodyFontSize
                                )
                            }
                        }
                    }
                    
                    // Bottom navigation buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = bottomPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Back button - use fixed width to prevent cutoff
                    val backInteractionSource = remember { MutableInteractionSource() }
                    val backIsFocused = backInteractionSource.collectIsFocusedAsState().value
                    val focusColor = if (isDark) Color(0x33FFFFFF) else Color(0x22000000)
                    Box(
                        modifier = Modifier
                            .widthIn(min = 120.dp)
                            .heightIn(min = 56.dp)
                            .then(if (backIsFocused) Modifier.background(focusColor) else Modifier)
                            .clickable(
                                enabled = page > 0,
                                interactionSource = backInteractionSource,
                                indication = null
                            ) {
                                if (page > 0) page--
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (page > 0) {
                            Text(
                                text = "Back",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Page indicator in the center
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Custom page indicator that responds to theme changes
                        val activeRes = R.drawable.ic_current_page
                        val inactiveRes = R.drawable.ic_new_page
                        val tintColor = Theme.colors.text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 0 until totalPages) {
                                Image(
                                    painter = painterResource(id = if (i == page) activeRes else inactiveRes),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(tintColor),
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (i == page) 12.dp else 10.dp)
                                )
                            }
                        }
                    }
                    
                    // Next/Finish button - use fixed width to prevent cutoff
                    val nextInteractionSource = remember { MutableInteractionSource() }
                    val nextIsFocused = nextInteractionSource.collectIsFocusedAsState().value
                    Box(
                        modifier = Modifier
                            .widthIn(min = 120.dp)
                            .heightIn(min = 56.dp)
                            .then(if (nextIsFocused) Modifier.background(focusColor) else Modifier)
                            .clickable(
                                interactionSource = nextInteractionSource,
                                indication = null
                            ) {
                                if (page < totalPages - 1) {
                                    page++
                                } else {
                                    onFinish()
                                }
                            },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = if (page < totalPages - 1) "Next" else "Finish",
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun GesturesPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Gesture items
            GestureItem(
                icon = Icons.Rounded.ArrowUpward,
                title = "Swipe up: App Drawer",
                description = "Long Swipe up The list of all of your apps",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            GestureItem(
                icon = Icons.Rounded.ArrowDownward,
                title = "Swipe Down: Simple Tray",
                description = "Long Swipe Down to access simple Tray",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            GestureItem(
                icon = Icons.Rounded.ArrowBack,
                title = "Swipe Left: Letters",
                description = "Swipe Left to access Letters",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            GestureItem(
                icon = Icons.Rounded.ArrowForward,
                title = "Swipe Right: Recents",
                description = "Swipe Right to access Recents",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
        }
    }
    
    @Composable
    private fun HomePageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Home interaction items
            HomeInteractionItem(
                icon = Icons.Rounded.Settings,
                title = "Longpress",
                description = "in empty areas to open inkOS settings",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.TouchApp,
                title = "Longpress",
                description = "in \"select app\" to choose shortcuts",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.Widgets,
                title = "Tap clock/date/quote",
                description = "To access other shortcuts",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.SwipeVertical,
                title = "Short swipes",
                description = "To move between home pages",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
        }
    }
    
    @Composable
    private fun HomeNotificationsPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Home notification items
            HomeInteractionItem(
                icon = Icons.Rounded.Emergency,
                title = "Signal*",
                description = "Simple Asterisk (dot)",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.SpeakerNotes,
                title = "Signal",
                description = "The notification appears under",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.Lyrics,
                title = "Spotify",
                description = "Dua Lipa - Houdini",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.FilterList,
                title = "Allow List",
                description = "Settings/Notifications/Home Allow List",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
        }
    }
    
    @Composable
    private fun OtherNotificationsPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Other notification items
            HomeInteractionItem(
                icon = Icons.Rounded.FormatSize,
                title = "Letters",
                description = "Cached long form notifications",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.FilterList,
                title = "Allowlist",
                description = "Notifications/ Letters Allowlist",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.Notifications,
                title = "Simple tray",
                description = "Android type of notifications",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
            
            HomeInteractionItem(
                icon = Icons.Rounded.FilterList,
                title = "Allow List",
                description = "Notifications/SimpleTray Allowlist",
                titleFontSize = titleFontSize,
                bodyFontSize = bodyFontSize
            )
        }
    }
    
    @Composable
    private fun GestureItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsTheme.typography.title.color,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = SettingsTheme.typography.title.color
                )
                Text(
                    text = description,
                    style = SettingsTheme.typography.body,
                    fontSize = bodyFontSize,
                    color = SettingsTheme.typography.body.color
                )
            }
        }
    }
    
    @Composable
    private fun HomeInteractionItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsTheme.typography.title.color,
                modifier = Modifier.size(32.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = SettingsTheme.typography.title.color
                )
                Text(
                    text = description,
                    style = SettingsTheme.typography.body,
                    fontSize = bodyFontSize,
                    color = SettingsTheme.typography.body.color
                )
            }
        }
    }
}
