package com.github.gezimos.inkos.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.AlertDialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme

@Composable
fun SetWallpaper(
    onBackClick: () -> Unit,
    onEditWallpaper: () -> Unit,
    onSetForHome: () -> Unit,
    onSetForLockScreen: () -> Unit,
    onSetForBoth: () -> Unit,
    onSetInkOSNoCrop: () -> Unit = {},
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    isDark: Boolean = false,
    showStatusBar: Boolean = false,
    allowEdit: Boolean = true
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val hasInkosWallpaper = remember { mutableStateOf(prefs.inkosWallpaperPath != null) }
    val showInkosDialog = remember { mutableStateOf(false) }
    val textIslandsShape = prefs.textIslandsShape
    val titleFontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize
    val buttonFontSize = fontSize
    
    val buttonShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 50.dp
        )
    }
    
    val containerShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(
            textIslandsShape = textIslandsShape,
            pillRadius = 12.dp
        )
    }
    
    val supportsLockScreen = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N
    
    SettingsTheme(isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background)
        ) {
            // Top: full-width Wallpaper Editor button (replaces header)
            if (allowEdit) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SetWallpaperOptionButton(
                        text = "Wallpaper Editor",
                        onClick = onEditWallpaper,
                        fontSize = titleFontSize,
                        shape = buttonShape,
                        isPrimary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // First box: inkOS wallpaper info + inkOS buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(containerShape)
                        .background(Theme.colors.background)
                        .border(2.dp, Theme.colors.text, containerShape)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "What is inkOS wallpaper?",
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            color = Theme.colors.text,
                            modifier = Modifier.clickable { showInkosDialog.value = true },
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Theme.colors.text)
                                .clickable { showInkosDialog.value = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = Theme.colors.background,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SetWallpaperOptionButton(
                            text = "inkOS Wallpaper",
                            onClick = onSetInkOSNoCrop,
                            fontSize = titleFontSize,
                            shape = buttonShape,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        )

                        if (hasInkosWallpaper.value) {
                            SetWallpaperOptionButton(
                                text = "✕",
                                onClick = {
                                    hasInkosWallpaper.value = false
                                    val prefs = Prefs(context)
                                    prefs.inkosWallpaperPath = null
                                },
                                fontSize = titleFontSize,
                                shape = buttonShape,
                                isPrimary = false,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Second box: set wallpaper for (home/lock/both)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(containerShape)
                        .background(Theme.colors.background)
                        .border(2.dp, Theme.colors.text, containerShape)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Set wallpaper for",
                        style = SettingsTheme.typography.title,
                        fontSize = titleFontSize,
                        color = Theme.colors.text,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    SetWallpaperOptionButton(
                        text = "Home Screen",
                        onClick = onSetForHome,
                        fontSize = titleFontSize,
                        shape = buttonShape,
                        isPrimary = false,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (supportsLockScreen) {
                        SetWallpaperOptionButton(
                            text = "Lock Screen",
                            onClick = onSetForLockScreen,
                            fontSize = titleFontSize,
                            shape = buttonShape,
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        )

                        SetWallpaperOptionButton(
                            text = "Both",
                            onClick = onSetForBoth,
                            fontSize = titleFontSize,
                            shape = buttonShape,
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
    
    // Dialog for inkOS wallpaper explanation
    if (showInkosDialog.value) {
        AlertDialog(
            onDismissRequest = { showInkosDialog.value = false },
            title = {
                Text(
                    text = "What is inkOS Wallpaper?",
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize,
                    color = Theme.colors.text
                )
            },
            text = {
                Text(
                    text = "inkOS wallpaper is a \"faux\" wallpaper, it's an image that appears over the actual android wallpaper layer.\n\n" +
                           "It's function it's meant to circumvent Android cropping and zooming in your wallpaper which might break the the 1:1 pixel scaling which can cause defect to your image especially if you're using an e-ink device.\n\n" +
                           "Make sure to use correct aspect ratio, or have the exact resolution as your display.\n\n" +
                           "If you want to go back to using android wallpaper then click the X (clear button near the inkOS Wallpaper button)",
                    style = SettingsTheme.typography.body,
                    fontSize = buttonFontSize,
                    color = Theme.colors.text
                )
            },
            confirmButton = {
                TextButton(onClick = { showInkosDialog.value = false }) {
                    Text(
                        text = "OK",
                        style = SettingsTheme.typography.title,
                        fontSize = buttonFontSize,
                        color = Theme.colors.text
                    )
                }
            },
            backgroundColor = Theme.colors.background.copy(alpha = 1f),
            contentColor = Theme.colors.text
        )
    }
}

@Composable
private fun SetWallpaperOptionButton(
    text: String,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit,
    shape: androidx.compose.ui.graphics.Shape,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (isPrimary) {
                    Modifier
                        .background(Theme.colors.text)
                        .border(2.dp, Theme.colors.text, shape)
                } else {
                    Modifier
                        .background(Theme.colors.background)
                        .border(2.dp, Theme.colors.text, shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            color = if (isPrimary) Theme.colors.background else Theme.colors.text,
            fontSize = fontSize
        )
    }
}
