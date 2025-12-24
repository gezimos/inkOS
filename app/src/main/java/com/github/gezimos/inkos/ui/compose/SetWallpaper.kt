package com.github.gezimos.inkos.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    isDark: Boolean = false,
    showStatusBar: Boolean = false,
    allowEdit: Boolean = true
) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
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
            // Header
            SettingsComposable.PageHeader(
                iconRes = R.drawable.ic_back,
                title = "Set Wallpaper",
                onClick = onBackClick,
                showStatusBar = showStatusBar,
                titleFontSize = titleFontSize
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Container with rounded corners and border
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(containerShape)
                        .background(Theme.colors.background)
                        .border(2.dp, Theme.colors.text, containerShape)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Wallpaper Editor button (only show if editing is allowed)
                    if (allowEdit) {
                        SetWallpaperOptionButton(
                            text = "Wallpaper Editor",
                            onClick = onEditWallpaper,
                            fontSize = titleFontSize,
                            shape = buttonShape,
                            isPrimary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Separator line (full width, no padding)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Theme.colors.text)
                        )
                    }
                    
                    // Title
                    Text(
                        text = "Set wallpaper for",
                        style = SettingsTheme.typography.title,
                        fontSize = titleFontSize,
                        color = Theme.colors.text,
                        modifier = Modifier
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    // Home Screen button
                    SetWallpaperOptionButton(
                        text = "Home Screen",
                        onClick = onSetForHome,
                        fontSize = titleFontSize,
                        shape = buttonShape,
                        isPrimary = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Lock Screen button (if supported)
                    if (supportsLockScreen) {
                        SetWallpaperOptionButton(
                            text = "Lock Screen",
                            onClick = onSetForLockScreen,
                            fontSize = titleFontSize,
                            shape = buttonShape,
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Both button
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
            .padding(vertical = 12.dp, horizontal = 16.dp),
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
