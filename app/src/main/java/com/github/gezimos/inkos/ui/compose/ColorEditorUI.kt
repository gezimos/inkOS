package com.github.gezimos.inkos.ui.compose

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.mutableIntStateOf
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager
@Composable
fun ColorEditorUI(
    onBackClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    isDark: Boolean = false,
    showStatusBar: Boolean = false,
    viewModel: MainViewModel,
    prefs: Prefs,
) {
    val context = LocalContext.current
    val dialogManager = remember { ComposeDialogManager(context, context as? android.app.Activity ?: throw Exception("Invalid Activity")) }
    val titleFontSize = if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize

    var isDpadMode by remember { mutableStateOf(false) }

    // DPAD state for color editor
    val ceZone = remember { mutableStateOf(ColorEditorFocusZone.TABS) }
    val ceHeaderIndex = remember { mutableIntStateOf(0) }
    val ceTabIndex = remember { mutableIntStateOf(0) }
    val ceColorRowIndex = remember { mutableIntStateOf(0) }
    val ceColorRowClickAction = remember { mutableStateOf<((Int) -> Unit)?>(null) }
    val outerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        try { outerFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    var customLightTextColor by remember { mutableStateOf(prefs.customThemeLightTextColor) }
    var customLightBackgroundColor by remember { mutableStateOf(prefs.customThemeLightBackgroundColor) }
    var customDarkTextColor by remember { mutableStateOf(prefs.customThemeDarkTextColor) }
    var customDarkBackgroundColor by remember { mutableStateOf(prefs.customThemeDarkBackgroundColor) }

    val buttonShape = remember(prefs.textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(textIslandsShape = prefs.textIslandsShape, pillRadius = 16.dp)
    }

    fun applyColors() {
        prefs.appliedThemeName = null
        prefs.customThemeLightTextColor = customLightTextColor
        prefs.customThemeLightBackgroundColor = customLightBackgroundColor
        prefs.customThemeDarkTextColor = customDarkTextColor
        prefs.customThemeDarkBackgroundColor = customDarkBackgroundColor
        prefs.lightTextColor = customLightTextColor
        prefs.lightBackgroundColor = customLightBackgroundColor
        prefs.darkTextColor = customDarkTextColor
        prefs.darkBackgroundColor = customDarkBackgroundColor
        val resolvedTheme = prefs.getResolvedTheme()
        val isDarkMode = resolvedTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
        if (isDarkMode) {
            viewModel.setTextColor(customDarkTextColor)
            viewModel.setBackgroundColor(customDarkBackgroundColor)
        } else {
            viewModel.setTextColor(customLightTextColor)
            viewModel.setBackgroundColor(customLightBackgroundColor)
        }
        android.widget.Toast.makeText(context, context.getString(R.string.theme_imported), android.widget.Toast.LENGTH_SHORT).show()
        onBackClick()
    }

    SettingsTheme(isDark) {
        val screenScale = rememberScreenScale()
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        val backHighlighted = isDpadMode && ceZone.value == ColorEditorFocusZone.HEADER && ceHeaderIndex.intValue == 0
        val saveHighlighted = isDpadMode && ceZone.value == ColorEditorFocusZone.HEADER && ceHeaderIndex.intValue == 1

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(prefBackgroundColor)
                .focusRequester(outerFocusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    NavHelper.handleColorEditorKeyEvent(
                        keyEvent = event,
                        isDpadModeSetter = { isDpadMode = it },
                        focusZone = ceZone,
                        headerIndex = ceHeaderIndex,
                        tabIndex = ceTabIndex,
                        colorRowIndex = ceColorRowIndex,
                        onBackClick = onBackClick,
                        onSave = { applyColors() },
                        onTabSelect = {},
                        onColorRowClick = { idx -> ceColorRowClickAction.value?.invoke(idx) }
                    )
                }
        ) {
            // Header with Save button
            SettingsComposable.PageHeader(
                iconRes = R.drawable.ic_back,
                title = stringResource(R.string.element_colors),
                onClick = onBackClick,
                showStatusBar = showStatusBar,
                titleFontSize = titleFontSize,
                backHighlighted = backHighlighted,
                pageIndicator = {
                    Box(
                        modifier = Modifier
                            .then(with(SettingsComposable) {
                                Modifier.pillHighlight(
                                    isHighlighted = saveHighlighted,
                                    color = prefTextColor,
                                    outerHorizontal = 4.dp,
                                    outerVertical = 4.dp
                                )
                            })
                            .clip(buttonShape)
                            .background(if (saveHighlighted) prefTextColor else prefBackgroundColor)
                            .then(if (!saveHighlighted) Modifier.border(1.5.dp.scaled(screenScale), prefTextColor, buttonShape) else Modifier)
                            .clickable(onClick = { applyColors() })
                            .padding(horizontal = 12.dp.scaled(screenScale), vertical = 6.dp.scaled(screenScale)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.btn_save),
                            style = SettingsTheme.typography.title,
                            color = if (saveHighlighted) prefBackgroundColor else prefTextColor,
                            fontSize = titleFontSize
                        )
                    }
                }
            )

            // Content
            Box(modifier = Modifier.weight(1f)) {
                CustomThemeCreator(
                    fontSize = titleFontSize,
                    prefs = prefs,
                    dialogManager = dialogManager,
                    lightTextColor = customLightTextColor,
                    lightBackgroundColor = customLightBackgroundColor,
                    darkTextColor = customDarkTextColor,
                    darkBackgroundColor = customDarkBackgroundColor,
                    onLightTextColorChange = { customLightTextColor = it },
                    onLightBackgroundColorChange = { customLightBackgroundColor = it },
                    onDarkTextColorChange = { customDarkTextColor = it },
                    onDarkBackgroundColorChange = { customDarkBackgroundColor = it },
                    isDpadMode = isDpadMode,
                    focusZone = ceZone.value,
                    tabIndex = ceTabIndex.intValue,
                    colorRowIndex = ceColorRowIndex.intValue,
                    onColorRowClickAction = ceColorRowClickAction
                )
            }
        }
    }
}

private fun colorToHex(color: Int): String =
    "#%02X%02X%02X".format(AndroidColor.red(color), AndroidColor.green(color), AndroidColor.blue(color))

@Composable
fun CustomThemeCreator(
    fontSize: androidx.compose.ui.unit.TextUnit,
    prefs: Prefs,
    dialogManager: ComposeDialogManager,
    lightTextColor: Int,
    lightBackgroundColor: Int,
    darkTextColor: Int,
    darkBackgroundColor: Int,
    onLightTextColorChange: (Int) -> Unit,
    onLightBackgroundColorChange: (Int) -> Unit,
    onDarkTextColorChange: (Int) -> Unit,
    onDarkBackgroundColorChange: (Int) -> Unit,
    isDpadMode: Boolean = false,
    focusZone: ColorEditorFocusZone = ColorEditorFocusZone.TABS,
    tabIndex: Int = 0,
    colorRowIndex: Int = 0,
    onColorRowClickAction: androidx.compose.runtime.MutableState<((Int) -> Unit)?>? = null
) {
    val context = LocalContext.current
    val screenScale = rememberScreenScale()
    var editingLightTheme by remember { mutableStateOf(true) }

    // Sync tab selection from DPAD
    LaunchedEffect(tabIndex, isDpadMode) {
        if (isDpadMode) {
            editingLightTheme = tabIndex == 0
        }
    }

    // Register color row click callback for DPAD Enter
    LaunchedEffect(editingLightTheme) {
        onColorRowClickAction?.value = { idx ->
            when (idx) {
                0 -> dialogManager.showColorPickerDialog(
                    context = context,
                    titleResId = R.string.background_color,
                    color = if (editingLightTheme) lightBackgroundColor else darkBackgroundColor,
                    onItemSelected = { if (editingLightTheme) onLightBackgroundColorChange(it) else onDarkBackgroundColorChange(it) }
                )
                1 -> dialogManager.showColorPickerDialog(
                    context = context,
                    titleResId = R.string.text_color,
                    color = if (editingLightTheme) lightTextColor else darkTextColor,
                    onItemSelected = { if (editingLightTheme) onLightTextColorChange(it) else onDarkTextColorChange(it) }
                )
            }
        }
    }
    val textIslandsShape = prefs.textIslandsShape
    val tabShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(textIslandsShape = textIslandsShape, pillRadius = 50.dp)
    }
    val boxShape = remember(textIslandsShape) {
        ShapeHelper.getRoundedCornerShape(textIslandsShape = textIslandsShape, pillRadius = 16.dp)
    }
    val lightBgColor = Color(lightBackgroundColor)
    val lightTextColorCompose = Color(lightTextColor)
    val darkBgColor = Color(darkBackgroundColor)
    val darkTextColorCompose = Color(darkTextColor)

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp.scaled(screenScale)),
        verticalArrangement = Arrangement.spacedBy(16.dp.scaled(screenScale))
    ) {
        // Light/Dark tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))
        ) {
            val lightTabHighlighted = isDpadMode && focusZone == ColorEditorFocusZone.TABS && tabIndex == 0
            val darkTabHighlighted = isDpadMode && focusZone == ColorEditorFocusZone.TABS && tabIndex == 1
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(with(SettingsComposable) {
                        Modifier.pillHighlight(
                            isHighlighted = lightTabHighlighted,
                            color = Theme.colors.text,
                            outerHorizontal = 4.dp,
                            outerVertical = 4.dp
                        )
                    })
                    .clip(tabShape)
                    .then(
                        if (editingLightTheme) Modifier.background(Theme.colors.text)
                        else Modifier
                            .background(Theme.colors.background)
                            .border(1.5.dp.scaled(screenScale), Theme.colors.text, tabShape)
                    )
                    .clickable { editingLightTheme = true }
                    .padding(vertical = 10.dp.scaled(screenScale)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.light),
                    style = SettingsTheme.typography.title,
                    fontSize = fontSize,
                    color = if (lightTabHighlighted) Theme.colors.background
                        else if (editingLightTheme) Theme.colors.background
                        else Theme.colors.text
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(with(SettingsComposable) {
                        Modifier.pillHighlight(
                            isHighlighted = darkTabHighlighted,
                            color = Theme.colors.text,
                            outerHorizontal = 4.dp,
                            outerVertical = 4.dp
                        )
                    })
                    .clip(tabShape)
                    .then(
                        if (!editingLightTheme) Modifier.background(Theme.colors.text)
                        else Modifier
                            .background(Theme.colors.background)
                            .border(1.5.dp.scaled(screenScale), Theme.colors.text, tabShape)
                    )
                    .clickable { editingLightTheme = false }
                    .padding(vertical = 10.dp.scaled(screenScale)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.dark),
                    style = SettingsTheme.typography.title,
                    fontSize = fontSize,
                    color = if (darkTabHighlighted) Theme.colors.background
                        else if (!editingLightTheme) Theme.colors.background
                        else Theme.colors.text
                )
            }
        }

        // Preview boxes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp.scaled(screenScale)),
            horizontalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(boxShape)
                    .background(lightBgColor)
                    .border(1.5.dp.scaled(screenScale), Theme.colors.text, boxShape)
                    .padding(16.dp.scaled(screenScale))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                    verticalArrangement = Arrangement.spacedBy(4.dp.scaled(screenScale))
                ) {
                    Text(text = "Light Theme", style = SettingsTheme.typography.title, fontSize = fontSize, color = lightTextColorCompose)
                    Text(text = "12:20", style = SettingsTheme.typography.body, fontSize = fontSize, color = lightTextColorCompose)
                    Text(text = "Phone", style = SettingsTheme.typography.body, fontSize = fontSize, color = lightTextColorCompose)
                    Text(text = "Messages", style = SettingsTheme.typography.body, fontSize = fontSize, color = lightTextColorCompose)
                    Text(text = "Email", style = SettingsTheme.typography.body, fontSize = fontSize, color = lightTextColorCompose)
                    Text(text = "Camera", style = SettingsTheme.typography.body, fontSize = fontSize, color = lightTextColorCompose)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(boxShape)
                    .background(darkBgColor)
                    .border(1.5.dp.scaled(screenScale), Theme.colors.text, boxShape)
                    .padding(16.dp.scaled(screenScale))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                    verticalArrangement = Arrangement.spacedBy(4.dp.scaled(screenScale))
                ) {
                    Text(text = "Dark Theme", style = SettingsTheme.typography.title, fontSize = fontSize, color = darkTextColorCompose)
                    Text(text = "12:20", style = SettingsTheme.typography.body, fontSize = fontSize, color = darkTextColorCompose)
                    Text(text = "Phone", style = SettingsTheme.typography.body, fontSize = fontSize, color = darkTextColorCompose)
                    Text(text = "Messages", style = SettingsTheme.typography.body, fontSize = fontSize, color = darkTextColorCompose)
                    Text(text = "Email", style = SettingsTheme.typography.body, fontSize = fontSize, color = darkTextColorCompose)
                    Text(text = "Camera", style = SettingsTheme.typography.body, fontSize = fontSize, color = darkTextColorCompose)
                }
            }
        }

        // Color picker rows
        val bgRowHighlighted = isDpadMode && focusZone == ColorEditorFocusZone.COLOR_ROWS && colorRowIndex == 0
        val textRowHighlighted = isDpadMode && focusZone == ColorEditorFocusZone.COLOR_ROWS && colorRowIndex == 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(with(SettingsComposable) {
                    Modifier.pillHighlight(isHighlighted = bgRowHighlighted, color = Theme.colors.text, outerHorizontal = 4.dp, outerVertical = 4.dp)
                })
                .clip(boxShape)
                .background(if (bgRowHighlighted) Theme.colors.text else Theme.colors.background)
                .then(if (!bgRowHighlighted) Modifier.border(1.5.dp.scaled(screenScale), Theme.colors.text, boxShape) else Modifier)
                .clickable {
                    dialogManager.showColorPickerDialog(
                        context = context, titleResId = R.string.background_color,
                        color = if (editingLightTheme) lightBackgroundColor else darkBackgroundColor,
                        onItemSelected = { if (editingLightTheme) onLightBackgroundColorChange(it) else onDarkBackgroundColorChange(it) }
                    )
                }
                .padding(16.dp.scaled(screenScale)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))
        ) {
            Box(modifier = Modifier.size(32.dp.scaled(screenScale)).border(1.5.dp.scaled(screenScale), Theme.colors.text, androidx.compose.foundation.shape.CircleShape).padding(2.dp.scaled(screenScale)).border(1.dp, Color.White, androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(if (editingLightTheme) lightBgColor else darkBgColor))
            Text(text = context.getString(R.string.background_color), style = SettingsTheme.typography.body, fontSize = fontSize, color = if (bgRowHighlighted) Theme.colors.background else Theme.colors.text, modifier = Modifier.weight(1f))
            Text(text = colorToHex(if (editingLightTheme) lightBackgroundColor else darkBackgroundColor), style = SettingsTheme.typography.body, fontSize = fontSize, color = if (bgRowHighlighted) Theme.colors.background else Theme.colors.text)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(with(SettingsComposable) {
                    Modifier.pillHighlight(isHighlighted = textRowHighlighted, color = Theme.colors.text, outerHorizontal = 4.dp, outerVertical = 4.dp)
                })
                .clip(boxShape)
                .background(if (textRowHighlighted) Theme.colors.text else Theme.colors.background)
                .then(if (!textRowHighlighted) Modifier.border(1.5.dp.scaled(screenScale), Theme.colors.text, boxShape) else Modifier)
                .clickable {
                    dialogManager.showColorPickerDialog(
                        context = context, titleResId = R.string.text_color,
                        color = if (editingLightTheme) lightTextColor else darkTextColor,
                        onItemSelected = { if (editingLightTheme) onLightTextColorChange(it) else onDarkTextColorChange(it) }
                    )
                }
                .padding(16.dp.scaled(screenScale)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))
        ) {
            Box(modifier = Modifier.size(32.dp.scaled(screenScale)).border(1.5.dp.scaled(screenScale), Theme.colors.text, androidx.compose.foundation.shape.CircleShape).padding(2.dp.scaled(screenScale)).border(1.dp, Color.White, androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).background(Color(if (editingLightTheme) lightTextColor else darkTextColor)))
            Text(text = context.getString(R.string.text_color), style = SettingsTheme.typography.body, fontSize = fontSize, color = if (textRowHighlighted) Theme.colors.background else Theme.colors.text, modifier = Modifier.weight(1f))
            Text(text = colorToHex(if (editingLightTheme) lightTextColor else darkTextColor), style = SettingsTheme.typography.body, fontSize = fontSize, color = if (textRowHighlighted) Theme.colors.background else Theme.colors.text)
        }
    }
}
