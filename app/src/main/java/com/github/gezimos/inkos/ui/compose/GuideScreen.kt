package com.github.gezimos.inkos.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.github.gezimos.inkos.ui.compose.inkOsSafeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpeakerNotes
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled

class GuideFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                GuideScreen.Show(
                    onBack = { findNavController().popBackStack() },
                    onHome = { findNavController().popBackStack(R.id.mainFragment, false) }
                )
            }
        }
    }
}

object GuideScreen {

    enum class GuideCategory(val totalPages: Int) {
        Touch(4), DPadT9(2), Qwerty(1), Search(2)
    }

    @Composable
    fun Show(
        onBack: () -> Unit = {},
        onHome: () -> Unit = {}
    ) {
        val context = LocalContext.current
        val prefs = remember { Prefs(context) }

        val screenScaleRaw = com.github.gezimos.inkos.style.detectScaleMode(context).let { mode ->
            if (prefs.uiScaleMode != 0) com.github.gezimos.inkos.style.UiScaleMode.fromId(prefs.uiScaleMode).scale
            else mode.scale
        }
        val settingsSize = (prefs.settingsSize - 3)
        val titleFontSize = (settingsSize * 1.5f * screenScaleRaw).sp
        val bodyFontSize = (settingsSize * 1.2f * screenScaleRaw).sp
        val bigTitleSize = (settingsSize * 3f * screenScaleRaw).sp

        val systemDark = isSystemInDarkTheme()
        val isDark = when (prefs.appTheme) {
            Constants.Theme.Dark -> true
            Constants.Theme.Light -> false
            Constants.Theme.System -> systemDark
        }
        var category by remember { mutableStateOf<GuideCategory?>(null) }
        var page by remember { mutableStateOf(0) }
        val totalPages = category?.totalPages ?: 1

        BackHandler(enabled = category != null) {
            category = null
            page = 0
        }

        SettingsTheme(isDark = isDark) {
            val textColor = Theme.colors.text
            val bgColor = Theme.colors.background
            val screenScale = rememberScreenScale()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .inkOsSafeDrawingPadding()
            ) {
                // Content area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp.scaled(screenScale), vertical = 32.dp.scaled(screenScale))
                ) {
                    // Big title — same for all pages
                    Text(
                        text = "GUIDE",
                        style = SettingsTheme.typography.title,
                        fontSize = bigTitleSize,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        lineHeight = (bigTitleSize.value * 1.05f).sp
                    )

                    // Thick bar
                    Spacer(modifier = Modifier.height(16.dp.scaled(screenScale)))
                    Box(
                        modifier = Modifier
                            .width(60.dp.scaled(screenScale))
                            .height(5.dp)
                            .background(textColor)
                    )

                    Spacer(modifier = Modifier.height(32.dp.scaled(screenScale)))

                    // Page-specific content
                    when (category) {
                        null -> MenuContent(titleFontSize, bodyFontSize, screenScale, textColor) { picked ->
                            category = picked
                            page = 0
                        }
                        GuideCategory.Touch -> when (page) {
                            0 -> HomePageContent(titleFontSize, bodyFontSize, screenScale, textColor)
                            1 -> GesturesPageContent(titleFontSize, bodyFontSize, screenScale, textColor)
                            2 -> HomeNotificationsPageContent(titleFontSize, bodyFontSize, screenScale, textColor)
                            3 -> WidgetsPageContent(titleFontSize, bodyFontSize, screenScale, textColor)
                        }
                        GuideCategory.DPadT9 -> when (page) {
                            0 -> DPadHomePageContent(titleFontSize, bodyFontSize, screenScale, textColor)
                            1 -> DPadLettersPageContent(titleFontSize, bodyFontSize, screenScale, textColor)
                        }
                        GuideCategory.Qwerty -> QwertyPageContent(titleFontSize, bodyFontSize, screenScale, textColor)
                        GuideCategory.Search -> when (page) {
                            0 -> SearchPage1Content(titleFontSize, bodyFontSize, screenScale, textColor)
                            1 -> SearchPage2Content(titleFontSize, bodyFontSize, screenScale, textColor)
                        }
                    }

                    // Page subtitle at the bottom (APP VERSION style)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = when (category) {
                            null -> "PICK INPUT METHOD"
                            GuideCategory.Touch -> when (page) {
                                0 -> "HOME SCREEN INTERACTIONS"
                                1 -> "HOME GESTURES"
                                2 -> "HOME NOTIFICATIONS"
                                3 -> "HOME WIDGETS"
                                else -> ""
                            }
                            GuideCategory.DPadT9 -> when (page) {
                                0 -> "HOME SCREEN"
                                1 -> "LETTERS"
                                else -> ""
                            }
                            GuideCategory.Qwerty -> "QWERTY"
                            GuideCategory.Search -> "SEARCH"
                        },
                        style = SettingsTheme.typography.title,
                        fontSize = (titleFontSize.value * 0.7f).sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 2.sp
                    )
                }

                // Separator line above footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp.scaled(screenScale))
                        .background(textColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp.scaled(screenScale)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (category == null) {
                        // Menu footer: BACK | HOME
                        val backMenuInteraction = remember { MutableInteractionSource() }
                        val backMenuFocused = backMenuInteraction.collectIsFocusedAsState().value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (backMenuFocused) textColor else bgColor)
                                .clickable(
                                    interactionSource = backMenuInteraction,
                                    indication = null
                                ) { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BACK",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (backMenuFocused) bgColor else textColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.5.dp.scaled(screenScale))
                                .fillMaxHeight()
                                .background(textColor)
                        )

                        Box(
                            modifier = Modifier.width(54.dp.scaled(screenScale)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1/1",
                                style = SettingsTheme.typography.body,
                                fontSize = (titleFontSize.value * 0.7f).sp,
                                color = textColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.5.dp.scaled(screenScale))
                                .fillMaxHeight()
                                .background(textColor)
                        )

                        val homeInteraction = remember { MutableInteractionSource() }
                        val homeFocused = homeInteraction.collectIsFocusedAsState().value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (homeFocused) textColor else bgColor)
                                .clickable(
                                    interactionSource = homeInteraction,
                                    indication = null
                                ) { onHome() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "HOME",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (homeFocused) bgColor else textColor
                            )
                        }
                    } else {
                        // BACK
                        val backInteraction = remember { MutableInteractionSource() }
                        val backFocused = backInteraction.collectIsFocusedAsState().value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (backFocused) textColor else bgColor)
                                .clickable(
                                    interactionSource = backInteraction,
                                    indication = null
                                ) {
                                    if (page > 0) page-- else {
                                        category = null
                                        page = 0
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "BACK",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (backFocused) bgColor else textColor
                            )
                        }

                        // Vertical separator
                        Box(
                            modifier = Modifier
                                .width(1.5.dp.scaled(screenScale))
                                .fillMaxHeight()
                                .background(textColor)
                        )

                        // Page indicator
                        Box(
                            modifier = Modifier.width(54.dp.scaled(screenScale)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${page + 1}/$totalPages",
                                style = SettingsTheme.typography.body,
                                fontSize = (titleFontSize.value * 0.7f).sp,
                                color = textColor
                            )
                        }

                        // Vertical separator
                        Box(
                            modifier = Modifier
                                .width(1.5.dp.scaled(screenScale))
                                .fillMaxHeight()
                                .background(textColor)
                        )

                        // NEXT / DONE
                        val nextInteraction = remember { MutableInteractionSource() }
                        val nextFocused = nextInteraction.collectIsFocusedAsState().value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (nextFocused) textColor else bgColor)
                                .clickable(
                                    interactionSource = nextInteraction,
                                    indication = null
                                ) {
                                    if (page < totalPages - 1) {
                                        page++
                                    } else {
                                        category = null
                                        page = 0
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (page < totalPages - 1) "NEXT" else "DONE",
                                style = SettingsTheme.typography.title,
                                fontSize = titleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = if (nextFocused) bgColor else textColor
                            )
                        }
                    }
                }

                // Bottom border when nav bar is visible
                if (WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() > 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp.scaled(screenScale))
                            .background(textColor)
                    )
                }
            }
        }
    }

    @Composable
    private fun GuideItem(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp.scaled(screenScale)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(28.dp.scaled(screenScale))
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = description,
                    style = SettingsTheme.typography.body,
                    fontSize = bodyFontSize,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    private fun HomePageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.Settings, "Pinch (zoom out)", "In empty areas to open Quick Menu", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.TouchApp, "Long press app", "To choose or replace shortcuts", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Widgets, "Tap clock/date/quote", "To access other shortcuts", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.SwipeVertical, "Short swipes", "To move between home pages", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun GesturesPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.ArrowUpward, "Swipe Up", "App Drawer — the list of all your apps", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.ArrowDownward, "Swipe Down", "Simple Tray — Android-style notifications", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.ArrowBack, "Swipe Left", "Letters — cached long-form notifications", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.ArrowForward, "Swipe Right", "Recents — recently used apps", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun HomeNotificationsPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.Emergency, "Signal*", "Asterisk or dot indicates a notification", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.SpeakerNotes, "Signal", "Notification text appears under the app name", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Lyrics, "Spotify", "Now playing info shown under the app", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.FilterList, "Allow List", "Settings / Notifications / Home Allow List", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun WidgetsPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.FormatQuote, "Quote", "Display a custom text at the bottom of your home screen", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.CalendarMonth, "Events", "Show upcoming calendar events from your device", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Widgets, "Android Widget", "Embed any Android widget on your home screen", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Timer, "Total Usage", "Track your daily screen time at a glance", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun MenuContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color,
        onPick: (GuideCategory) -> Unit
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            MenuRow(Icons.Rounded.TouchApp, "Touch", "Tap-driven interactions", titleFontSize, bodyFontSize, screenScale, textColor) { onPick(GuideCategory.Touch) }
            MenuRow(Icons.Rounded.Dialpad, "DPad / T9", "Hardware keys & dialpad", titleFontSize, bodyFontSize, screenScale, textColor) { onPick(GuideCategory.DPadT9) }
            MenuRow(Icons.Rounded.Keyboard, "Qwerty", "Typing & nav mode", titleFontSize, bodyFontSize, screenScale, textColor) { onPick(GuideCategory.Qwerty) }
            MenuRow(Icons.Rounded.Search, "Search", "Apps, contacts, files, web, and more", titleFontSize, bodyFontSize, screenScale, textColor) { onPick(GuideCategory.Search) }
        }
    }

    @Composable
    private fun MenuRow(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        description: String,
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color,
        onClick: () -> Unit
    ) {
        val interaction = remember { MutableInteractionSource() }
        val focused = interaction.collectIsFocusedAsState().value
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null) { onClick() },
            horizontalArrangement = Arrangement.spacedBy(16.dp.scaled(screenScale)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(28.dp.scaled(screenScale))
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = description,
                    style = SettingsTheme.typography.body,
                    fontSize = bodyFontSize,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }

    @Composable
    private fun DPadHomePageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.Apps, "Long-press 9", "Open Quick Menu", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.TouchApp, "Long-press DPad center", "To choose or replace shortcuts", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.SwipeVertical, "DPad up / down", "Move between apps and home pages", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.SwapHoriz, "DPad left / right", "Trigger configured swipe actions", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun DPadLettersPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.OpenInNew, "DPad center / Enter / 3", "Open the focused notification", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Close, "Backspace / Menu / 1", "Dismiss the focused notification", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.ClearAll, "Long-press (touch only)", "Dismiss all notifications", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.SwipeVertical, "DPad up / down", "Move between notifications", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun QwertyPageContent(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.Search, "Search from home", "Just start typing from homescreen", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Keyboard, "Navigate with DPad", "Using keyboards such as Pastiera navmode.", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun SearchPage1Content(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.Apps, "Apps", "Find installed apps", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Bolt, "Shortcuts", "Find app shortcuts", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Contacts, "Contacts", "Find a contact to call or message", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Folder, "Files", "Find files on your device", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

    @Composable
    private fun SearchPage2Content(
        titleFontSize: androidx.compose.ui.unit.TextUnit,
        bodyFontSize: androidx.compose.ui.unit.TextUnit,
        screenScale: Float,
        textColor: androidx.compose.ui.graphics.Color
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp.scaled(screenScale))) {
            GuideItem(Icons.Rounded.MusicNote, "Music", "Find music in your library", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Language, "Web", "Search the web", titleFontSize, bodyFontSize, screenScale, textColor)
            GuideItem(Icons.Rounded.Link, "URL", "Open a URL directly", titleFontSize, bodyFontSize, screenScale, textColor)
        }
    }

}
