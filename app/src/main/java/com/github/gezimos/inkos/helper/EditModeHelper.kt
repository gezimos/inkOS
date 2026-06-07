package com.github.gezimos.inkos.helper

import android.content.Context
import com.github.gezimos.common.showLongToast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import com.github.gezimos.inkos.ui.dialogs.IconStylePickerSheet
import com.github.gezimos.inkos.ui.dialogs.SheetTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EditNotifications
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.BuildConfig
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.utils.ProfileManager
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.compose.SettingsComposable
import com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost
import com.github.gezimos.inkos.ui.dialogs.SheetTitle
import com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager
import com.github.gezimos.inkos.ui.dialogs.ShortcutGroup
import com.github.gezimos.inkos.ui.dialogs.ShortcutItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
object EditModeHelper {
    
    private val _isEditModeActive = MutableStateFlow(false)
    val isEditModeFlow: StateFlow<Boolean> = _isEditModeActive
    private var fontPickerCallback: ((callback: (android.graphics.Typeface, String) -> Unit) -> Unit)? = null

    fun initFontPicker(launcher: (callback: (android.graphics.Typeface, String) -> Unit) -> Unit) {
        fontPickerCallback = launcher
    }

    private var currentSheetRef: WeakReference<ComposeBottomSheetHost>? = null
    private inline var currentSheet: ComposeBottomSheetHost?
        get() = currentSheetRef?.get()
        set(value) { currentSheetRef = value?.let { WeakReference(it) } }
    fun toggleEditMode(): Boolean {
        val newState = !_isEditModeActive.value
        _isEditModeActive.value = newState
        VibrationHelper.trigger(VibrationHelper.Effect.CLICK)
        return newState
    }
    fun isEditMode(): Boolean = _isEditModeActive.value
    fun exitEditMode() {
        _isEditModeActive.value = false
        currentSheet?.dismiss()
        currentSheet = null
    }
    private fun buildGestureActions(
        context: Context,
        notificationsEnabled: Boolean,
        includeOpenApp: Boolean
    ): List<Constants.Action> {
        val psm = ProfileManager(context)
        val actions = Constants.Action.entries.filter {
            (psm.isPrivateSpaceSetUp() || it != Constants.Action.TogglePrivateSpace) &&
            (psm.hasWorkProfile() || it != Constants.Action.ToggleWorkProfile)
        }
        val result = actions.filter { action ->
            if (!includeOpenApp && action == Constants.Action.OpenApp) return@filter false
            when (action) {
                Constants.Action.OpenLettersScreen -> notificationsEnabled
                else -> true
            }
        }.toMutableList()
        if (!result.contains(Constants.Action.Brightness)) result.add(Constants.Action.Brightness)
        if (!result.contains(Constants.Action.OpenAppDrawer)) result.add(Constants.Action.OpenAppDrawer)
        if (!result.contains(Constants.Action.ExitLauncher)) result.add(Constants.Action.ExitLauncher)
        if (!result.contains(Constants.Action.LockScreen)) result.add(Constants.Action.LockScreen)
        if (!result.contains(Constants.Action.ShowRecents)) result.add(Constants.Action.ShowRecents)
        if (!result.contains(Constants.Action.OpenQuickSettings)) result.add(Constants.Action.OpenQuickSettings)
        if (!result.contains(Constants.Action.OpenPowerDialog)) result.add(Constants.Action.OpenPowerDialog)
        return result
    }

    private fun getFontDisplayName(prefs: Prefs, font: Constants.FontFamily, contextKey: String): String {
        val fontName = if (font == Constants.FontFamily.Custom) {
            val path = if (contextKey == "notifications") {
                prefs.getCustomFontPath("notifications") ?: prefs.getCustomFontPath("universal")
            } else { prefs.getCustomFontPathForContext(contextKey) }
            path?.let { java.io.File(it).name } ?: font.name
        } else { font.name }
        return if (fontName.length > 12) "${fontName.take(12)}..." else fontName
    }

    private fun showFontPickerDialog(
        context: Context,
        dialogManager: ComposeDialogManager,
        vm: com.github.gezimos.inkos.MainViewModel,
        prefs: Prefs,
        titleResId: Int,
        fontContextKey: String,
        getCurrentFont: () -> Constants.FontFamily,
        setFont: (Constants.FontFamily) -> Unit,
        onUpdate: () -> Unit
    ) {
        val fontFamilyEntries = Constants.FontFamily.entries.filter { it != Constants.FontFamily.Custom }
        val builtInFontOptions = fontFamilyEntries.map { it.getString(context) }
        val builtInFonts = fontFamilyEntries.map { it.getFont(context) ?: getTrueSystemFont() }
        val oflFontOptions = fontFamilyEntries
            .filter { it != Constants.FontFamily.System }
            .map { it.getString(context) }

        val customFonts = Constants.FontFamily.getAllCustomFonts(context)
        val customFontOptions = customFonts.map { it.first }
        val customFontPaths = customFonts.map { it.second }
        val customFontTypefaces = customFontPaths.map { path ->
            Constants.FontFamily.Custom.getFont(context, path) ?: getTrueSystemFont()
        }

        val hasCustomFontPicker = fontPickerCallback != null

        var selectedIndex: Int? = null
        try {
            val currentFont = getCurrentFont()
            if (currentFont == Constants.FontFamily.Custom) {
                val path = prefs.getCustomFontPathForContext(fontContextKey)
                if (path != null) {
                    val idx = customFontPaths.indexOf(path)
                    if (idx != -1) selectedIndex = builtInFontOptions.size + idx
                }
            } else {
                val idx = fontFamilyEntries.indexOf(currentFont)
                if (idx != -1) selectedIndex = idx
            }
        } catch (_: Exception) {}

        dialogManager.showTabbedFontPicker(
            context = context,
            titleResId = titleResId,
            builtInOptions = builtInFontOptions,
            builtInFonts = builtInFonts,
            customOptions = customFontOptions,
            customFonts = customFontTypefaces,
            selectedIndex = selectedIndex,
            isBuiltInFont = { option -> oflFontOptions.contains(option) },
            onInfoClick = {
                dialogManager.dismissAll()
                dialogManager.showFontLicenseSheet()
            },
            addCustomFontLabel = if (hasCustomFontPicker) context.getString(R.string.add_custom_font) else null,
            onAddCustomFont = if (hasCustomFontPicker) { {
                fontPickerCallback?.invoke { _, path ->
                    vm.setUniversalFontEnabled(false)
                    vm.setCustomFontPath(fontContextKey, path)
                    vm.addCustomFontPath(path)
                    prefs.setCustomFontPath(fontContextKey, path)
                    setFont(Constants.FontFamily.Custom)
                    onUpdate()
                    dialogManager.dismissAll()
                }
            } } else null,
            onItemSelected = { selectedName, isCustom ->
                vm.setUniversalFontEnabled(false)
                if (!isCustom) {
                    val builtInIndex = builtInFontOptions.indexOf(selectedName)
                    if (builtInIndex != -1) {
                        val selectedFont = fontFamilyEntries[builtInIndex]
                        prefs.setCustomFontPath(fontContextKey, "")
                        setFont(selectedFont)
                        onUpdate()
                    }
                } else {
                    val customIndex = customFontOptions.indexOf(selectedName)
                    if (customIndex != -1) {
                        val path = customFontPaths[customIndex]
                        prefs.setCustomFontPath(fontContextKey, path)
                        vm.setCustomFontPath(fontContextKey, path)
                        setFont(Constants.FontFamily.Custom)
                        onUpdate()
                    }
                }
            },
            onItemDeleted = { deletedName, dismiss ->
                val customIndex = customFontOptions.indexOf(deletedName)
                if (customIndex != -1) {
                    val path = customFontPaths[customIndex]
                    vm.removeCustomFontPathByPath(path)
                    onUpdate()
                    dismiss()
                }
            }
        )
    }
    fun showClockSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()

        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showClockSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]

        showDialog(context, prefs) {
            val selectedTabIndex = remember { mutableStateOf(0) }
            val screenScale = rememberScreenScale()
            val uiState by vm.homeUiState.collectAsState()
            val clockTitle = when (selectedTabIndex.value) { 0 -> stringResource(R.string.tab_style); 1 -> stringResource(R.string.tab_function); else -> stringResource(R.string.tab_gestures) }
            SheetTitle(clockTitle)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SheetTabRow(
                    selectedIndex = selectedTabIndex.value,
                    tabs = listOf(
                        { Icon(imageVector = Icons.Rounded.TextFields, contentDescription = stringResource(R.string.tab_style), ) },
                        { Icon(imageVector = Icons.Rounded.Checklist, contentDescription = stringResource(R.string.tab_function), ) },
                        { Icon(imageVector = Icons.Rounded.TouchApp, contentDescription = stringResource(R.string.tab_gestures), ) }
                    ),
                    onTabSelected = { selectedTabIndex.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp.scaled(screenScale)))
                when (selectedTabIndex.value) {
                    0 -> {
                        // Style tab

                        SettingRow(label = stringResource(R.string.clock_text_size), value = "${uiState.clockSize}") {
                            currentSheet?.dismiss()
                            dialogManager.showSliderDialog(
                                context = context,
                                title = context.getString(R.string.clock_text_size),
                                minValue = 10,
                                maxValue = 200,
                                currentValue = uiState.clockSize,
                                liveUpdate = true,
                                onValueSelected = { value ->
                                    vm.setClockSize(value)
                                    onUpdate()
                                }
                            )
                        }

                        SettingRow(label = stringResource(R.string.clock_font), value = getFontDisplayName(prefs, uiState.clockFont, "clock")) {
                            currentSheet?.dismiss()
                            showFontPickerDialog(
                                context = context,
                                dialogManager = dialogManager,
                                vm = vm,
                                prefs = prefs,
                                titleResId = R.string.clock_font,
                                fontContextKey = "clock",
                                getCurrentFont = { prefs.clockFont },
                                setFont = { font ->
                                    vm.setClockFont(font)
                                },
                                onUpdate = onUpdate
                            )
                        }

                        val clockStyleOrder = intArrayOf(0, 2, 6, 1, 3, 8, 9, 10, 4, 5, 7)
                        val clockStyleLabels = arrayOf(stringResource(R.string.option_default), stringResource(R.string.option_box_solid), stringResource(R.string.option_box_outline), stringResource(R.string.option_flip), stringResource(R.string.option_round), stringResource(R.string.option_analog), stringResource(R.string.option_digital), stringResource(R.string.option_matrix), stringResource(R.string.option_split), stringResource(R.string.option_horizontal), stringResource(R.string.option_stacked))
                        val clockDisplayIndex = clockStyleOrder.indexOf(uiState.clockStyle).coerceAtLeast(0)
                        CycleRow(
                            label = stringResource(R.string.clock_style),
                            options = clockStyleLabels,
                            currentIndex = clockDisplayIndex,
                        ) { nextDisplayIndex ->
                            vm.setClockStyle(clockStyleOrder[nextDisplayIndex])
                            onUpdate()
                        }
                        SettingRow(label = stringResource(R.string.fonts_settings_title), value = "") {
                            currentSheet?.dismiss()
                            try {
                                fragment.findNavController().navigate(
                                    R.id.settingsFragment,
                                    bundleOf("initialRoute" to "Fonts")
                                )
                            } catch (_: Exception) {}
                        }
                    }
                    1 -> {
                        // Function tab

                        ToggleRow(stringResource(R.string.show_clock), uiState.showClock) { checked ->
                            vm.setShowClock(checked)
                            onUpdate()
                        }

                        val clockOptions = arrayOf(stringResource(R.string.option_system), stringResource(R.string.option_24_hour), stringResource(R.string.option_12_hour), stringResource(R.string.option_12_hour_alt))
                        CycleRow(
                            label = stringResource(R.string.clock_format),
                            options = clockOptions,
                            currentIndex = uiState.clockMode,
                        ) { nextIndex ->
                            vm.setClockMode(nextIndex)
                            onUpdate()
                        }

                        ToggleRow(stringResource(R.string.show_am_pm), uiState.showAmPm) { checked ->
                            vm.setShowAmPm(checked)
                            onUpdate()
                        }

                        if (uiState.clockStyle in intArrayOf(0, 2, 6)) {
                            ToggleRow(stringResource(R.string.dual_clocks), uiState.showSecondClock) { checked ->
                                vm.setShowSecondClock(checked)
                                onUpdate()
                            }

                            if (uiState.showSecondClock) {
                                SettingRow(label = stringResource(R.string.second_clock_offset), value = "${uiState.secondClockOffsetHours}h") {
                                    currentSheet?.dismiss()
                                    dialogManager.showSliderDialog(
                                        context = context,
                                        title = context.getString(R.string.second_clock_offset),
                                        minValue = -12,
                                        maxValue = 12,
                                        currentValue = uiState.secondClockOffsetHours,
                                        liveUpdate = true,
                                        onValueSelected = { value ->
                                            vm.setSecondClockOffsetHours(value)
                                            onUpdate()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Gestures tab
                        val clickClockActions = buildGestureActions(context, uiState.notificationsEnabled, includeOpenApp = true)
                        val appLabelClickClock = prefs.appClickClock.activityLabel
                        val clickClockDisplay = when (uiState.clickClockAction) {
                            Constants.Action.OpenApp -> if (appLabelClickClock.isEmpty()) context.getString(R.string.open_app) else appLabelClickClock
                            Constants.Action.OpenAppDrawer -> context.getString(R.string.app_drawer)
                            else -> uiState.clickClockAction.getString(context)
                        }
                        SettingRow(label = stringResource(R.string.clock_click_app), value = clickClockDisplay) {
                            currentSheet?.dismiss()
                            dialogManager.showGestureActionPicker(
                                context = context,
                                titleResId = R.string.clock_click_app,
                                flag = Constants.AppDrawerFlag.SetClickClock,
                                allowedActions = clickClockActions,
                                currentAction = uiState.clickClockAction,
                                currentAppLabel = appLabelClickClock,
                                viewModel = vm,
                                onSelect = { action, app ->
                                    if (action == Constants.Action.OpenApp && app != null) {
                                        vm.selectAppForFlag(app, Constants.AppDrawerFlag.SetClickClock)
                                    } else {
                                        vm.setClickClockAction(action)
                                    }
                                    onUpdate()
                                }
                            )
                        }

                        SettingRow(label = stringResource(R.string.gestures_settings_title), value = "") {
                            currentSheet?.dismiss()
                            try {
                                fragment.findNavController().navigate(R.id.settingsFragment)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }
    fun showDateSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()

        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showDateSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]

        showDialog(context, prefs) {
            val selectedTabIndex = remember { mutableStateOf(0) }
            val screenScale = rememberScreenScale()
            val uiState by vm.homeUiState.collectAsState()
            val dateTitle = when (selectedTabIndex.value) { 0 -> stringResource(R.string.tab_style); 1 -> stringResource(R.string.tab_function); else -> stringResource(R.string.tab_gestures) }
            SheetTitle(dateTitle)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SheetTabRow(
                    selectedIndex = selectedTabIndex.value,
                    tabs = listOf(
                        { Icon(imageVector = Icons.Rounded.TextFields, contentDescription = stringResource(R.string.tab_style), ) },
                        { Icon(imageVector = Icons.Rounded.Checklist, contentDescription = stringResource(R.string.tab_function), ) },
                        { Icon(imageVector = Icons.Rounded.TouchApp, contentDescription = stringResource(R.string.tab_gestures), ) }
                    ),
                    onTabSelected = { selectedTabIndex.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp.scaled(screenScale)))
                when (selectedTabIndex.value) {
                    0 -> {
                        // Style tab: Date Size and Date Font
                        SettingRow(label = stringResource(R.string.date_text_size), value = "${uiState.dateSize}") {
                            currentSheet?.dismiss()
                            dialogManager.showSliderDialog(
                                context = context,
                                title = context.getString(R.string.date_text_size),
                                minValue = 10,
                                maxValue = 100,
                                currentValue = uiState.dateSize,
                                liveUpdate = true,
                                onValueSelected = { value ->
                                    vm.setDateSize(value)
                                    onUpdate()
                                }
                            )
                        }

                        SettingRow(label = stringResource(R.string.date_font), value = getFontDisplayName(prefs, uiState.dateFont, "date")) {
                            currentSheet?.dismiss()
                            showFontPickerDialog(
                                context = context,
                                dialogManager = dialogManager,
                                vm = vm,
                                prefs = prefs,
                                titleResId = R.string.date_font,
                                fontContextKey = "date",
                                getCurrentFont = { prefs.getFontForContext("date") },
                                setFont = { font ->
                                    vm.setDateFont(font)
                                },
                                onUpdate = onUpdate
                            )
                        }
                        SettingRow(label = stringResource(R.string.fonts_settings_title), value = "") {
                            currentSheet?.dismiss()
                            try {
                                fragment.findNavController().navigate(
                                    R.id.settingsFragment,
                                    bundleOf("initialRoute" to "Fonts")
                                )
                            } catch (_: Exception) {}
                        }
                    }
                    1 -> {
                        // Function tab: toggles
                        ToggleRow(stringResource(R.string.show_date), uiState.showDate) { checked ->
                            vm.setShowDate(checked)
                            vm.refreshDateBattery(context)
                            onUpdate()
                        }

                        val dateFormatOptions = arrayOf(stringResource(R.string.option_date_format_1), stringResource(R.string.option_date_format_2), stringResource(R.string.option_date_format_3), stringResource(R.string.option_date_format_4), stringResource(R.string.option_date_format_5))
                        CycleRow(label = stringResource(R.string.date_format), options = dateFormatOptions, currentIndex = uiState.dateFormatStyle.coerceIn(0, 4)) { nextIndex ->
                            vm.setDateFormatStyle(nextIndex)
                            vm.refreshDateBattery(context)
                            onUpdate()
                        }

                        ToggleRow(stringResource(R.string.show_battery), uiState.showDateBatteryCombo) { checked ->
                            vm.setShowDateBatteryCombo(checked)
                            vm.refreshDateBattery(context)
                            onUpdate()
                        }

                        ToggleRow(stringResource(R.string.show_notification_count), uiState.showNotificationCount) { checked ->
                            vm.setShowNotificationCount(checked)
                            onUpdate()
                        }

                        if (uiState.showNotificationCount) {
                            val countSourceOptions = arrayOf(stringResource(R.string.option_simple_tray), stringResource(R.string.option_letters), stringResource(R.string.option_hub))
                            CycleRow(
                                label = stringResource(R.string.desc_notification_style),
                                options = countSourceOptions,
                                currentIndex = uiState.notificationCountSource.coerceIn(0, 2),
                            ) { nextIndex ->
                                vm.setNotificationCountSource(nextIndex)
                                onUpdate()
                            }
                        }
                    }
                    2 -> {
                        // Gestures tab
                        val clickDateActions = buildGestureActions(context, uiState.notificationsEnabled, includeOpenApp = true)
                        val appLabelClickDate = prefs.appClickDate.activityLabel
                        val clickDateDisplay = when (uiState.clickDateAction) {
                            Constants.Action.OpenApp -> if (appLabelClickDate.isEmpty()) context.getString(R.string.open_app) else appLabelClickDate
                            Constants.Action.OpenAppDrawer -> context.getString(R.string.app_drawer)
                            else -> uiState.clickDateAction.getString(context)
                        }
                        SettingRow(label = stringResource(R.string.date_click_app), value = clickDateDisplay) {
                            currentSheet?.dismiss()
                            dialogManager.showGestureActionPicker(
                                context = context,
                                titleResId = R.string.date_click_app,
                                flag = Constants.AppDrawerFlag.SetClickDate,
                                allowedActions = clickDateActions,
                                currentAction = uiState.clickDateAction,
                                currentAppLabel = appLabelClickDate,
                                viewModel = vm,
                                onSelect = { action, app ->
                                    if (action == Constants.Action.OpenApp && app != null) {
                                        vm.selectAppForFlag(app, Constants.AppDrawerFlag.SetClickDate)
                                    } else {
                                        vm.setClickDateAction(action)
                                    }
                                    onUpdate()
                                }
                            )
                        }

                        SettingRow(label = stringResource(R.string.gestures_settings_title), value = "") {
                            currentSheet?.dismiss()
                            try {
                                fragment.findNavController().navigate(R.id.settingsFragment)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }
    fun showEventsCalendarPicker(
        context: Context,
        dialogManager: ComposeDialogManager,
        viewModel: com.github.gezimos.inkos.MainViewModel,
        prefs: Prefs,
        onComplete: () -> Unit = {}
    ) {
        val calendars = CalendarEventsHelper.loadCalendars(context)
        if (calendars.isEmpty()) {
            android.widget.Toast.makeText(context, context.getString(R.string.events_no_calendar), android.widget.Toast.LENGTH_SHORT).show()
            onComplete()
            return
        }
        val allCalendarsLabel = context.getString(R.string.events_all_calendars)
        val labels = listOf(allCalendarsLabel) + calendars.map { it.second }
        val currentIdx = when {
            prefs.eventsCalendarId == CalendarEventsHelper.ALL_CALENDARS_ID -> 0
            else -> (calendars.indexOfFirst { it.first == prefs.eventsCalendarId }.let { if (it >= 0) it + 1 else 0 })
        }
        dialogManager.showSingleChoiceDialog(
            context = context,
            options = labels.toTypedArray(),
            titleResId = R.string.events_choose_calendar,
            selectedIndex = currentIdx,
            onItemSelected = { selectedLabel ->
                if (selectedLabel == allCalendarsLabel) {
                    viewModel.setEventsCalendar(
                        CalendarEventsHelper.ALL_CALENDARS_ID,
                        allCalendarsLabel
                    )
                } else {
                    val pair = calendars.find { it.second == selectedLabel }
                    if (pair != null) {
                        viewModel.setEventsCalendar(pair.first, pair.second)
                    }
                }
                onComplete()
            }
        )
    }
    fun showBottomWidgetSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()
        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showBottomWidgetSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        val appWidgetHelper = AppWidgetHelper.getInstance(context)
        val bottomWidgetLabels = listOf(
            context.getString(R.string.bottom_widget_disabled),
            context.getString(R.string.bottom_widget_quote),
            context.getString(R.string.bottom_widget_events),
            context.getString(R.string.bottom_widget_android),
            context.getString(R.string.bottom_widget_shortcuts),
            context.getString(R.string.bottom_widget_total_usage),
            context.getString(R.string.bottom_widget_page_dots)
        )
        val bottomWidgetValues = listOf(
            Constants.BottomWidgetType.Disabled.value,
            Constants.BottomWidgetType.Quote.value,
            Constants.BottomWidgetType.Events.value,
            Constants.BottomWidgetType.AndroidWidget.value,
            Constants.BottomWidgetType.Shortcuts.value,
            Constants.BottomWidgetType.TotalUsage.value,
            Constants.BottomWidgetType.PageDots.value
        )
        showDialog(context, prefs) {
            val selectedTabIndex = remember { mutableStateOf(0) }
            val screenScale = rememberScreenScale()
            val uiState by vm.homeUiState.collectAsState()
            val widgetTitle = when (selectedTabIndex.value) { 0 -> stringResource(R.string.tab_function); 1 -> stringResource(R.string.tab_style); else -> stringResource(R.string.tab_gestures) }
            SheetTitle(widgetTitle)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SheetTabRow(
                    selectedIndex = selectedTabIndex.value,
                    tabs = listOf(
                        { Icon(imageVector = Icons.Rounded.Checklist, contentDescription = stringResource(R.string.tab_function), ) },
                        { Icon(imageVector = Icons.Rounded.TextFields, contentDescription = stringResource(R.string.tab_style), ) },
                        { Icon(imageVector = Icons.Rounded.TouchApp, contentDescription = stringResource(R.string.tab_gestures), ) }
                    ),
                    onTabSelected = { selectedTabIndex.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp.scaled(screenScale)))
                when (selectedTabIndex.value) {
                    0 -> {
                        val currentIdx = bottomWidgetValues.indexOf(uiState.bottomWidgetType).coerceAtLeast(0)
                        SettingRow(label = context.getString(R.string.bottom_widget), value = bottomWidgetLabels.getOrElse(currentIdx) { "" }) {
                            currentSheet?.dismiss()
                            dialogManager.showSingleChoiceDialog(
                                context = context,
                                options = bottomWidgetLabels.toTypedArray(),
                                titleResId = R.string.bottom_widget,
                                selectedIndex = currentIdx,
                                onItemSelected = { selectedLabel ->
                                    val idx = bottomWidgetLabels.indexOf(selectedLabel)
                                    if (idx >= 0) vm.setBottomWidgetType(bottomWidgetValues[idx])
                                    onUpdate()
                                }
                            )
                        }
                        when (uiState.bottomWidgetType) {
                            Constants.BottomWidgetType.Quote.value -> {
                                SettingRow(label = stringResource(R.string.quote_text), value = uiState.quoteText) {
                                    currentSheet?.dismiss()
                                    dialogManager.showInputDialog(context = context, title = context.getString(R.string.quote_text), initialValue = uiState.quoteText) {
                                        vm.setQuoteText(it)
                                        onUpdate()
                                    }
                                }
                            }
                            Constants.BottomWidgetType.Events.value -> {
                                val calendarDisplayName = when {
                                    prefs.eventsCalendarId == CalendarEventsHelper.ALL_CALENDARS_ID -> context.getString(R.string.events_all_calendars)
                                    prefs.eventsCalendarName.isNotBlank() -> prefs.eventsCalendarName
                                    else -> context.getString(R.string.events_no_calendar)
                                }
                                SettingRow(label = context.getString(R.string.events_choose_calendar), value = calendarDisplayName) {
                                    currentSheet?.dismiss()
                                    if (!CalendarEventsHelper.hasCalendarPermission(context)) {
                                        android.widget.Toast.makeText(context, context.getString(R.string.events_grant_permission_subtitle), android.widget.Toast.LENGTH_SHORT).show()
                                        return@SettingRow
                                    }
                                    showEventsCalendarPicker(context, dialogManager, vm, prefs, onUpdate)
                                }
                                val filterLabels = listOf(
                                    context.getString(R.string.events_filter_24h),
                                    context.getString(R.string.events_filter_1week),
                                    context.getString(R.string.events_filter_2weeks),
                                    context.getString(R.string.events_filter_1month)
                                )
                                SettingRow(label = context.getString(R.string.events_filter), value = filterLabels.getOrElse(prefs.eventsFilter) { context.getString(R.string.events_filter_1week) }) {
                                    currentSheet?.dismiss()
                                    dialogManager.showSingleChoiceDialog(
                                        context = context,
                                        options = filterLabels.toTypedArray(),
                                        titleResId = R.string.events_filter,
                                        selectedIndex = prefs.eventsFilter,
                                        onItemSelected = { selectedLabel ->
                                            val idx = filterLabels.indexOf(selectedLabel)
                                            if (idx >= 0) {
                                                vm.setEventsFilter(idx)
                                                onUpdate()
                                            }
                                        }
                                    )
                                }
                                ToggleRow("Hide Chevrons & Icon", prefs.eventsHideControls) { checked ->
                                    vm.setEventsHideControls(checked)
                                    onUpdate()
                                }
                            }
                            Constants.BottomWidgetType.AndroidWidget.value -> {
                                val currentLabel = appWidgetHelper.getCurrentWidgetLabel() ?: stringResource(R.string.option_none_widget)
                                SettingRow(label = stringResource(R.string.choose_android_widget), value = currentLabel) {
                                    currentSheet?.dismiss()
                                    showWidgetPickerSheet(context, fragment, prefs, appWidgetHelper, vm, onUpdate)
                                }
                                if (uiState.androidWidgetId != -1) {
                                    SettingRow(label = stringResource(R.string.remove_android_widget), value = "") {
                                        currentSheet?.dismiss()
                                        appWidgetHelper.removeWidget()
                                        vm.setAndroidWidgetId(-1)
                                        onUpdate()
                                    }
                                }
                            }
                            Constants.BottomWidgetType.Shortcuts.value -> {
                                val shortcutActions = buildGestureActions(context, uiState.notificationsEnabled, includeOpenApp = true)

                                SettingRow(label = stringResource(R.string.shortcut_left_icon), value = uiState.shortcutLeftIcon.name) {
                                    currentSheet?.dismiss()
                                    dialogManager.showShortcutIconPicker(
                                        currentIcon = uiState.shortcutLeftIcon,
                                        onSelect = { icon ->
                                            vm.setShortcutLeftIcon(icon)
                                            onUpdate()
                                        }
                                    )
                                }
                                val appLabelLeft = prefs.appShortcutLeft.activityLabel
                                val leftDisplay = when (uiState.shortcutLeftAction) {
                                    Constants.Action.OpenApp -> if (appLabelLeft.isEmpty()) context.getString(R.string.open_app) else appLabelLeft
                                    else -> uiState.shortcutLeftAction.getString(context)
                                }
                                SettingRow(label = stringResource(R.string.shortcut_left_action), value = leftDisplay) {
                                    currentSheet?.dismiss()
                                    dialogManager.showGestureActionPicker(
                                        context = context,
                                        titleResId = R.string.bottom_widget,
                                        flag = Constants.AppDrawerFlag.SetShortcutLeft,
                                        allowedActions = shortcutActions,
                                        currentAction = uiState.shortcutLeftAction,
                                        currentAppLabel = appLabelLeft,
                                        viewModel = vm,
                                        onSelect = { action, app ->
                                            if (action == Constants.Action.OpenApp && app != null) {
                                                vm.selectAppForFlag(app, Constants.AppDrawerFlag.SetShortcutLeft)
                                            } else {
                                                vm.setShortcutLeftAction(action)
                                            }
                                            onUpdate()
                                        }
                                    )
                                }

                                SettingRow(label = stringResource(R.string.shortcut_right_icon), value = uiState.shortcutRightIcon.name) {
                                    currentSheet?.dismiss()
                                    dialogManager.showShortcutIconPicker(
                                        currentIcon = uiState.shortcutRightIcon,
                                        onSelect = { icon ->
                                            vm.setShortcutRightIcon(icon)
                                            onUpdate()
                                        }
                                    )
                                }
                                val appLabelRight = prefs.appShortcutRight.activityLabel
                                val rightDisplay = when (uiState.shortcutRightAction) {
                                    Constants.Action.OpenApp -> if (appLabelRight.isEmpty()) context.getString(R.string.open_app) else appLabelRight
                                    else -> uiState.shortcutRightAction.getString(context)
                                }
                                SettingRow(label = stringResource(R.string.shortcut_right_action), value = rightDisplay) {
                                    currentSheet?.dismiss()
                                    dialogManager.showGestureActionPicker(
                                        context = context,
                                                titleResId = R.string.bottom_widget,
                                                flag = Constants.AppDrawerFlag.SetShortcutRight,
                                                allowedActions = shortcutActions,
                                                currentAction = uiState.shortcutRightAction,
                                                currentAppLabel = appLabelRight,
                                                viewModel = vm,
                                                onSelect = { action, app ->
                                                    if (action == Constants.Action.OpenApp && app != null) {
                                                        vm.selectAppForFlag(app, Constants.AppDrawerFlag.SetShortcutRight)
                                                    } else {
                                                        vm.setShortcutRightAction(action)
                                                    }
                                                    onUpdate()
                                                }
                                            )
                                        }

                                // Bottom Page Dots
                                ToggleRow(stringResource(R.string.bottom_widget_page_dots), uiState.shortcutPageDots) { checked ->
                                    vm.setShortcutPageDots(checked)
                                }
                                ToggleRow(stringResource(R.string.hide_outline), uiState.shortcutHideOutline) { checked ->
                                    vm.setShortcutHideOutline(checked)
                                }
                            }
                            else -> {}
                        }
                    }
                    1 -> {
                        when (uiState.bottomWidgetType) {
                            Constants.BottomWidgetType.Quote.value -> {
                                SettingRow(label = "Quote Size", value = "${uiState.quoteSize}") {
                                    currentSheet?.dismiss()
                                    dialogManager.showSliderDialog(context = context, title = "Quote Size", minValue = 10, maxValue = 100, currentValue = uiState.quoteSize, liveUpdate = true) {
                                        vm.setQuoteSize(it)
                                        onUpdate()
                                    }
                                }
                                SettingRow(label = "Quote Font", value = getFontDisplayName(prefs, uiState.quoteFont, "quote")) {
                                    currentSheet?.dismiss()
                                    showFontPickerDialog(context = context, dialogManager = dialogManager, vm = vm, prefs = prefs, titleResId = R.string.quote_font, fontContextKey = "quote", getCurrentFont = { prefs.quoteFont }, setFont = { vm.setQuoteFont(it) }, onUpdate = onUpdate)
                                }
                            }
                            Constants.BottomWidgetType.Events.value -> {
                                SettingRow(label = "Quote Size", value = "${uiState.quoteSize}") {
                                    currentSheet?.dismiss()
                                    dialogManager.showSliderDialog(context = context, title = "Quote Size", minValue = 10, maxValue = 100, currentValue = uiState.quoteSize, liveUpdate = true) {
                                        vm.setQuoteSize(it)
                                        onUpdate()
                                    }
                                }
                                SettingRow(label = "Quote Font", value = getFontDisplayName(prefs, uiState.quoteFont, "quote")) {
                                    currentSheet?.dismiss()
                                    showFontPickerDialog(context = context, dialogManager = dialogManager, vm = vm, prefs = prefs, titleResId = R.string.quote_font, fontContextKey = "quote", getCurrentFont = { prefs.quoteFont }, setFont = { vm.setQuoteFont(it) }, onUpdate = onUpdate)
                                }
                            }
                            Constants.BottomWidgetType.Shortcuts.value -> {
                                SettingRow(label = "Icon Size", value = "${uiState.quoteSize}") {
                                    currentSheet?.dismiss()
                                    dialogManager.showSliderDialog(context = context, title = "Icon Size", minValue = 10, maxValue = 100, currentValue = uiState.quoteSize, liveUpdate = true) {
                                        vm.setQuoteSize(it)
                                        onUpdate()
                                    }
                                }
                            }
                            Constants.BottomWidgetType.TotalUsage.value -> {
                                SettingRow(label = "Text Size", value = "${uiState.quoteSize}") {
                                    currentSheet?.dismiss()
                                    dialogManager.showSliderDialog(context = context, title = "Text Size", minValue = 10, maxValue = 100, currentValue = uiState.quoteSize, liveUpdate = true) {
                                        vm.setQuoteSize(it)
                                        onUpdate()
                                    }
                                }
                                SettingRow(label = "Font", value = getFontDisplayName(prefs, uiState.quoteFont, "quote")) {
                                    currentSheet?.dismiss()
                                    showFontPickerDialog(context = context, dialogManager = dialogManager, vm = vm, prefs = prefs, titleResId = R.string.quote_font, fontContextKey = "quote", getCurrentFont = { prefs.quoteFont }, setFont = { vm.setQuoteFont(it) }, onUpdate = onUpdate)
                                }
                            }
                            Constants.BottomWidgetType.AndroidWidget.value -> {
                                SettingRow(label = "Widget Size", value = "${uiState.androidWidgetHeight}dp / ${uiState.androidWidgetMarginStart}% / ${uiState.androidWidgetMarginEnd}%") {
                                    currentSheet?.dismiss()
                                    showDialog(context, prefs) {
                                        var height by remember { androidx.compose.runtime.mutableIntStateOf(prefs.androidWidgetHeight) }
                                        var leftMargin by remember { androidx.compose.runtime.mutableIntStateOf(prefs.androidWidgetMarginStart) }
                                        var rightMargin by remember { androidx.compose.runtime.mutableIntStateOf(prefs.androidWidgetMarginEnd) }

                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text("Widget height: ${height}dp", style = SettingsTheme.typography.item, color = Theme.colors.text)
                                            com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                                value = height,
                                                onValueChange = { value ->
                                                    height = value
                                                    vm.setAndroidWidgetHeight(value)
                                                    onUpdate()
                                                },
                                                minValue = Constants.MIN_ANDROID_WIDGET_HEIGHT,
                                                maxValue = Constants.MAX_ANDROID_WIDGET_HEIGHT
                                            )

                                            Text("Left margin: ${leftMargin}%", style = SettingsTheme.typography.item, color = Theme.colors.text)
                                            com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                                value = leftMargin,
                                                onValueChange = { value ->
                                                    leftMargin = value
                                                    vm.setAndroidWidgetMarginStart(value)
                                                    onUpdate()
                                                },
                                                minValue = 0,
                                                maxValue = Constants.MAX_ANDROID_WIDGET_MARGIN
                                            )

                                            Text("Right margin: ${rightMargin}%", style = SettingsTheme.typography.item, color = Theme.colors.text)
                                            com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                                value = rightMargin,
                                                onValueChange = { value ->
                                                    rightMargin = value
                                                    vm.setAndroidWidgetMarginEnd(value)
                                                    onUpdate()
                                                },
                                                minValue = 0,
                                                maxValue = Constants.MAX_ANDROID_WIDGET_MARGIN
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                        SettingRow(label = stringResource(R.string.fonts_settings_title), value = "") {
                            currentSheet?.dismiss()
                            try {
                                fragment.findNavController().navigate(
                                    R.id.settingsFragment,
                                    bundleOf("initialRoute" to "Fonts")
                                )
                            } catch (_: Exception) {}
                        }
                    }
                    2 -> {
                        when (uiState.bottomWidgetType) {
                            Constants.BottomWidgetType.Quote.value -> {
                                val quoteActions = buildGestureActions(context, uiState.notificationsEnabled, includeOpenApp = true)
                                val appLabelQuote = prefs.appQuoteWidget.activityLabel
                                val quoteDisplay = when (uiState.quoteAction) {
                                    Constants.Action.OpenApp -> if (appLabelQuote.isEmpty()) context.getString(R.string.open_app) else appLabelQuote
                                    Constants.Action.OpenAppDrawer -> context.getString(R.string.app_drawer)
                                    else -> uiState.quoteAction.getString(context)
                                }
                                SettingRow(label = "Click Quote Action", value = quoteDisplay) {
                                    currentSheet?.dismiss()
                                    dialogManager.showGestureActionPicker(
                                        context = context,
                                        titleResId = R.string.quote_click_app,
                                        flag = Constants.AppDrawerFlag.SetQuoteWidget,
                                        allowedActions = quoteActions,
                                        currentAction = uiState.quoteAction,
                                        currentAppLabel = appLabelQuote,
                                        viewModel = vm,
                                        onSelect = { action, app ->
                                            if (action == Constants.Action.OpenApp && app != null) {
                                                vm.selectAppForFlag(app, Constants.AppDrawerFlag.SetQuoteWidget)
                                            } else {
                                                vm.setQuoteAction(action)
                                            }
                                            onUpdate()
                                        }
                                    )
                                }
                            }
                            else -> {}
                        }
                        SettingRow(label = stringResource(R.string.gestures_settings_title), value = "") {
                            currentSheet?.dismiss()
                            try { fragment.findNavController().navigate(R.id.settingsFragment) } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }
    fun showBackgroundSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()

        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showBackgroundSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        
        showDialog(context, prefs) {
            val selectedTabIndex = remember { mutableStateOf(0) }
            val screenScale = rememberScreenScale()
            val uiState by vm.homeUiState.collectAsState()
            val bgTitle = when (selectedTabIndex.value) { 0 -> "Theme"; 1 -> "Alignment"; 2 -> "Islands/Corners"; else -> "Gestures" }
            SheetTitle(bgTitle)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SheetTabRow(
                    selectedIndex = selectedTabIndex.value,
                    tabs = listOf(
                        { Icon(imageVector = Icons.Rounded.Palette, contentDescription = "Theme", ) },
                        { Icon(imageVector = Icons.AutoMirrored.Rounded.FormatAlignLeft, contentDescription = "Alignment", ) },
                        { Icon(imageVector = Icons.AutoMirrored.Rounded.ViewList, contentDescription = "Islands/Corners", ) },
                        { Icon(imageVector = Icons.Rounded.TouchApp, contentDescription = "Gestures", ) }
                    ),
                    onTabSelected = { selectedTabIndex.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp.scaled(screenScale)))
                when (selectedTabIndex.value) {
                    0 -> {
                        // Theme tab

                        SettingRow(label = "Theme Mode", value = when (uiState.appTheme) {
                            Constants.Theme.Dark -> Constants.Theme.Dark.getString(context)
                            Constants.Theme.Light -> Constants.Theme.Light.getString(context)
                            else -> Constants.Theme.System.getString(context)
                        }) {
                            currentSheet?.dismiss()
                            val themeOptions = arrayOf(
                                Constants.Theme.Light,
                                Constants.Theme.Dark,
                                Constants.Theme.System
                            )
                            val selectedIndex = themeOptions.indexOf(uiState.appTheme).coerceAtLeast(0)
                            dialogManager.showSingleChoiceDialog(
                                context = context,
                                options = themeOptions,
                                titleResId = R.string.theme_mode,
                                selectedIndex = selectedIndex,
                                onItemSelected = { newTheme ->
                                    vm.setAppTheme(newTheme)
                                    try {
                                        fragment.requireActivity().recreate()
                                    } catch (_: Exception) {
                                        try { vm.refreshHomeAppsUiState(fragment.requireContext()) } catch (_: Exception) {}
                                    }
                                    onUpdate()
                                }
                            )
                        }

                        SettingRow(label = "Chosen Theme", value = com.github.gezimos.inkos.ui.compose.getChosenThemeDisplayName(context)) {
                            currentSheet?.dismiss()
                            fragment.findNavController().navigate(R.id.themePresetsFragment)
                        }

                        ColorSettingRow(
                            label = "Colors",
                            color1 = Color(uiState.backgroundColor),
                            color2 = Color(uiState.textColor),
                        ) {
                            currentSheet?.dismiss()
                            fragment.findNavController().navigate(R.id.colorEditorFragment)
                        }

                        val wallpaperVis = 100 - (uiState.backgroundOpacity.coerceIn(0, 255) * 100 / 255)
                        SettingRow(label = "Wallpaper Visibility", value = "$wallpaperVis%") {
                            currentSheet?.dismiss()
                            dialogManager.showSliderDialog(
                                context = context,
                                title = "Wallpaper Visibility",
                                minValue = 0,
                                maxValue = 100,
                                currentValue = wallpaperVis,
                                liveUpdate = true,
                                onValueSelected = { value ->
                                    vm.setBackgroundOpacity(255 - (value * 255 / 100))
                                    onUpdate()
                                }
                            )
                        }

                        SettingRow(label = "Wallpaper", value = "Set") {
                            currentSheet?.dismiss()
                            fragment.findNavController().navigate(R.id.wallpaperFragment)
                        }
                    }
                    1 -> {
                        // Alignment tab

                        val alignmentOptions = arrayOf("Left", "Center", "Right")

                        CycleRow(label = context.getString(R.string.home_clock_alignment), options = alignmentOptions, currentIndex = uiState.clockAlignment.coerceIn(0, 2), showAlignment = true) { nextIndex ->
                            vm.setHomeClockAlignment(nextIndex)
                            onUpdate()
                        }

                        CycleRow(label = context.getString(R.string.home_date_alignment), options = alignmentOptions, currentIndex = uiState.dateAlignment.coerceIn(0, 2), showAlignment = true) { nextIndex ->
                            vm.setHomeDateAlignment(nextIndex)
                            onUpdate()
                        }

                        CycleRow(label = "Apps Alignment", options = alignmentOptions, currentIndex = uiState.homeAlignment.coerceIn(0, 2), showAlignment = true) { nextIndex ->
                            vm.setHomeAlignment(nextIndex)
                            onUpdate()
                        }

                        CycleRow(label = context.getString(R.string.home_quote_alignment), options = alignmentOptions, currentIndex = uiState.quoteAlignment.coerceIn(0, 2), showAlignment = true) { nextIndex ->
                            vm.setHomeQuoteAlignment(nextIndex)
                            onUpdate()
                        }

                        SettingRow(label = "Margin/Offset", value = "${uiState.topWidgetMargin}/${uiState.bottomWidgetMargin}/${uiState.homeAppsYOffset}") {
                            currentSheet?.dismiss()
                            showDialog(context, prefs) {
                                var topMargin by remember { androidx.compose.runtime.mutableIntStateOf(prefs.topWidgetMargin) }
                                var bottomMargin by remember { androidx.compose.runtime.mutableIntStateOf(prefs.bottomWidgetMargin) }
                                var yOffset by remember { androidx.compose.runtime.mutableIntStateOf(prefs.homeAppsYOffset) }

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        "Top widget margin: $topMargin",
                                        style = SettingsTheme.typography.item,
                                        color = Theme.colors.text
                                    )
                                    com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                        value = topMargin,
                                        onValueChange = { value ->
                                            topMargin = value
                                            try {
                                                val vmRef = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
                                                vmRef.setTopWidgetMargin(value)
                                            } catch (_: Exception) {
                                                prefs.topWidgetMargin = value
                                            }
                                            onUpdate()
                                        },
                                        minValue = 0,
                                        maxValue = Constants.MAX_TOP_WIDGET_MARGIN
                                    )

                                    Text(
                                        "Bottom widget margin: $bottomMargin",
                                        style = SettingsTheme.typography.item,
                                        color = Theme.colors.text
                                    )
                                    com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                        value = bottomMargin,
                                        onValueChange = { value ->
                                            bottomMargin = value
                                            try {
                                                val vmRef = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
                                                vmRef.setBottomWidgetMargin(value)
                                            } catch (_: Exception) {
                                                prefs.bottomWidgetMargin = value
                                            }
                                            onUpdate()
                                        },
                                        minValue = 0,
                                        maxValue = Constants.MAX_BOTTOM_WIDGET_MARGIN
                                    )

                                    Text(
                                        "Home apps Y-offset: $yOffset",
                                        style = SettingsTheme.typography.item,
                                        color = Theme.colors.text
                                    )
                                    com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                        value = yOffset,
                                        onValueChange = { value ->
                                            yOffset = value
                                            try {
                                                val vmRef = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
                                                vmRef.setHomeAppsYOffset(value)
                                            } catch (_: Exception) {
                                                prefs.homeAppsYOffset = value
                                            }
                                            onUpdate()
                                        },
                                        minValue = Constants.MIN_HOME_APPS_Y_OFFSET,
                                        maxValue = uiState.maxHomeAppsYOffset
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Islands/Corners tab

                        ToggleRow("Enable Text Islands", uiState.textIslands) { checked ->
                            vm.setTextIslands(checked)
                            onUpdate()
                        }

                        ToggleRow("Invert Islands", uiState.textIslandsInverted) { checked ->
                            vm.setTextIslandsInverted(checked)
                            onUpdate()
                        }

                        val shapeOptions = arrayOf("Pill", "Rounded", "Square")
                        CycleRow(
                            label = "Corners",
                            options = shapeOptions,
                            currentIndex = uiState.textIslandsShape,
                        ) { next ->
                            vm.setTextIslandsShape(next)
                            onUpdate()
                        }
                    }
                    3 -> {
                        // Gestures tab (Double tap action)
                        val doubleTapActions = buildGestureActions(context, uiState.notificationsEnabled, includeOpenApp = false)
                        val appLabelDoubleTap = prefs.appDoubleTap.activityLabel
                        val doubleTapDisplay = when (uiState.doubleTapAction) {
                            Constants.Action.OpenAppDrawer -> context.getString(R.string.app_drawer)
                            else -> uiState.doubleTapAction.getString(context)
                        }
                        SettingRow(label = "Double Tap Action", value = doubleTapDisplay) {
                            currentSheet?.dismiss()
                            dialogManager.showGestureActionPicker(
                                context = context,
                                titleResId = R.string.double_tap,
                                flag = Constants.AppDrawerFlag.SetDoubleTap,
                                allowedActions = doubleTapActions,
                                currentAction = uiState.doubleTapAction,
                                currentAppLabel = appLabelDoubleTap,
                                viewModel = vm,
                                onSelect = { action, _ ->
                                    vm.setDoubleTapAction(action)
                                    onUpdate()
                                }
                            )
                        }

                        SettingRow(label = stringResource(R.string.gestures_settings_title), value = "") {
                            currentSheet?.dismiss()
                            try {
                                fragment.findNavController().navigate(R.id.settingsFragment)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }
    fun showWidgetPickerSheet(
        context: Context,
        fragment: Fragment,
        prefs: Prefs,
        appWidgetHelper: AppWidgetHelper,
        vm: com.github.gezimos.inkos.MainViewModel,
        onUpdate: () -> Unit
    ) {
        val widgetApps = appWidgetHelper.getInstalledWidgetsByApp()
        if (widgetApps.isEmpty()) {
            android.widget.Toast.makeText(context, "No widgets available", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        showDialog(context, prefs) {
            val selectedApp = remember { mutableStateOf<AppWidgetHelper.WidgetApp?>(null) }
            val app = selectedApp.value

            if (app == null) {
                WidgetAppListContent(
                    context = context,
                    widgetApps = widgetApps,
                    onAppSelected = { selectedApp.value = it },
                    onClose = { currentSheet?.dismiss() }
                )
            } else {
                WidgetListContent(
                    app = app,
                    appWidgetHelper = appWidgetHelper,
                    onBack = { selectedApp.value = null },
                    onWidgetPicked = { providerInfo ->
                        currentSheet?.dismiss()
                        appWidgetHelper.pickWidgetFromProvider(
                            providerInfo = providerInfo,
                            onSuccess = { widgetId ->
                                vm.setAndroidWidgetId(widgetId)
                                onUpdate()
                            },
                            onNeedsPermission = { widgetId, bindIntent ->
                                val homeFragment = fragment as? com.github.gezimos.inkos.ui.HomeFragmentCompose
                                if (homeFragment != null) {
                                    homeFragment.pendingWidgetId = widgetId
                                    homeFragment.widgetBindLauncher.launch(bindIntent)
                                }
                            },
                            onNeedsConfigure = { widgetId, configIntent ->
                                val homeFragment = fragment as? com.github.gezimos.inkos.ui.HomeFragmentCompose
                                if (homeFragment != null) {
                                    homeFragment.pendingWidgetId = widgetId
                                    try {
                                        homeFragment.widgetConfigureLauncher.launch(configIntent)
                                    } catch (e: SecurityException) {
                                        android.util.Log.e("EditModeHelper", "Widget configure activity not exported", e)
                                        context.showLongToast("Widget configuration not available")
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
    }
    fun showAppSettings(context: Context, fragment: Fragment, prefs: Prefs, initialTabIndex: Int = 0, onUpdate: () -> Unit = {}) {
        currentSheet?.dismiss()

        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showAppSettings(context, fragment, prefs, initialTabIndex, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        
        showDialog(context, prefs) {
        val selectedTabIndex = remember { mutableStateOf(initialTabIndex.coerceIn(0, 3)) }
        val uiState by vm.homeUiState.collectAsState()
        val appTitle = when (selectedTabIndex.value) { 0 -> "Style"; 1 -> "Icons"; 2 -> "Function"; else -> "Notifications" }
        SheetTitle(appTitle)
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SheetTabRow(
                    selectedIndex = selectedTabIndex.value,
                    tabs = listOf(
                        { Icon(imageVector = Icons.Rounded.TextFields, contentDescription = "Style", ) },
                        { Icon(imageVector = Icons.Rounded.Stars, contentDescription = "Icons", ) },
                        { Icon(imageVector = Icons.Rounded.Checklist, contentDescription = "Function", ) },
                        { Icon(imageVector = Icons.Rounded.EditNotifications, contentDescription = "Notifications", ) }
                    ),
                    onTabSelected = { selectedTabIndex.value = it }
                )
            Spacer(modifier = Modifier.height(16.dp))
            when (selectedTabIndex.value) {
                0 -> {
                    // Style tab

                    SettingRow(label = context.getString(R.string.apps_font), value = getFontDisplayName(prefs, uiState.appsFont, "apps")) {
                        currentSheet?.dismiss()
                        showFontPickerDialog(
                            context = context,
                            dialogManager = dialogManager,
                            vm = vm,
                            prefs = prefs,
                            titleResId = R.string.apps_font,
                            fontContextKey = "apps",
                            getCurrentFont = { prefs.getFontForContext("apps") },
                            setFont = { font ->
                                vm.setAppsFont(font)
                            },
                            onUpdate = onUpdate
                        )
                    }

                    SettingRow(label = "App Size/Gap", value = "${uiState.appSize}/${uiState.textPaddingSize}") {
                        currentSheet?.dismiss()
                        showDialog(context, prefs) {
                            var appSize by remember { androidx.compose.runtime.mutableIntStateOf(prefs.appSize) }
                            var appGap by remember { androidx.compose.runtime.mutableIntStateOf(prefs.textPaddingSize) }

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "App text size: $appSize",
                                    style = SettingsTheme.typography.item,
                                    color = Theme.colors.text
                                )
                                com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                    value = appSize,
                                    onValueChange = { value ->
                                        appSize = value
                                        vm.setAppSize(value)
                                        onUpdate()
                                    },
                                    minValue = 10,
                                    maxValue = 100
                                )

                                Text(
                                    "App gap: $appGap",
                                    style = SettingsTheme.typography.item,
                                    color = Theme.colors.text
                                )
                                com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                    value = appGap,
                                    onValueChange = { value ->
                                        appGap = value
                                        vm.setTextPaddingSize(value)
                                        onUpdate()
                                    },
                                    minValue = 0,
                                    maxValue = 50
                                )
                            }
                        }
                    }

                    

                    val appNameOptions = arrayOf(
                        context.getString(R.string.app_name_mode_normal),
                        context.getString(R.string.app_name_mode_all_caps),
                        context.getString(R.string.small_caps_apps)
                    )
                    val appNameCurrent = when {
                        uiState.allCapsApps -> 1
                        uiState.smallCapsApps -> 2
                        else -> 0
                    }
                    CycleRow(label = "App Name Type", options = appNameOptions, currentIndex = appNameCurrent) { nextIndex ->
                        when (nextIndex) {
                            0 -> { vm.setAllCapsApps(false); vm.setSmallCapsApps(false) }
                            1 -> { vm.setAllCapsApps(true); vm.setSmallCapsApps(false) }
                            2 -> { vm.setAllCapsApps(false); vm.setSmallCapsApps(true) }
                        }
                        onUpdate()
                    }
                    SettingRow(label = stringResource(R.string.fonts_settings_title), value = "") {
                        currentSheet?.dismiss()
                        try {
                            fragment.findNavController().navigate(
                                R.id.settingsFragment,
                                bundleOf("initialRoute" to "Fonts")
                            )
                        } catch (_: Exception) {}
                    }
                }
                1 -> {
                    val packs = remember { IconPackUtility.getInstalledIconPacks(context) }
                    IconStylePickerSheet(
                        currentMode = uiState.iconSourceMode,
                        currentIconPack = uiState.selectedIconPackPackage,
                        iconPacks = packs,
                        showIcons = uiState.showIcons,
                        currentIconShape = uiState.iconShape,
                        onSelect = { mode, packPkg ->
                            if (mode == -1) {
                                vm.setShowIcons(false)
                            } else {
                                vm.setShowIcons(true)
                                vm.setIconSourceMode(mode)
                                if (mode == 3 && packPkg != null) {
                                    vm.setSelectedIconPackPackage(packPkg)
                                }
                            }
                            onUpdate()
                        },
                        onShapeSelect = { shape ->
                            vm.setIconShape(shape)
                            onUpdate()
                        },
                        onDismiss = {}
                    )

                    if (uiState.iconSourceMode == 4) {
                        SettingRow(
                            label = context.getString(R.string.icon_tint_contrast),
                            value = String.format("%.1f×", uiState.iconTintContrast / 10f)
                        ) {
                            currentSheet?.dismiss()
                            dialogManager.showSliderDialog(
                                context = context,
                                title = context.getString(R.string.icon_tint_contrast),
                                minValue = 0, maxValue = 30,
                                currentValue = uiState.iconTintContrast,
                                onValueSelected = { v ->
                                    vm.setIconTintContrast(v)
                                    onUpdate()
                                }
                            )
                        }
                    }
                }
                2 -> {
                    // Function tab

                    SettingRow(label = "Nr of Apps/Pages", value = "${uiState.homeAppsNum}/${uiState.homePagesNum}") {
                        currentSheet?.dismiss()
                        showDialog(context, prefs) {
                            var homeApps by remember { androidx.compose.runtime.mutableIntStateOf(prefs.homeAppsNum) }
                            var homePages by remember { androidx.compose.runtime.mutableIntStateOf(prefs.homePagesNum) }

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "Number of home apps: $homeApps",
                                    style = SettingsTheme.typography.item,
                                    color = Theme.colors.text
                                )
                                com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                    value = homeApps,
                                    onValueChange = { value ->
                                        homeApps = value
                                        vm.setHomeAppsNum(value)
                                        try { Constants.updateMaxHomePages(context) } catch (_: Exception) {}
                                        onUpdate()
                                    },
                                    minValue = Constants.MIN_HOME_APPS,
                                    maxValue = Constants.MAX_HOME_APPS
                                )

                                Text(
                                    "Number of pages: $homePages",
                                    style = SettingsTheme.typography.item,
                                    color = Theme.colors.text
                                )
                                com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                    value = homePages,
                                    onValueChange = { value ->
                                        homePages = value
                                        vm.setHomePagesNum(value)
                                        onUpdate()
                                    },
                                    minValue = Constants.MIN_HOME_PAGES,
                                    maxValue = Constants.MAX_HOME_PAGES
                                )

                            }
                        }
                    }

                    ToggleRow("Page Indicators", uiState.pageIndicatorVisible) { checked ->
                        vm.setPageIndicatorVisible(checked)
                        onUpdate()
                    }

                    // Home page reset (from FeaturesFragment)
                    ToggleRow(context.getString(R.string.home_page_reset), uiState.homeReset) { checked ->
                        vm.setHomeReset(checked)
                        onUpdate()
                    }

                    ToggleRow(context.getString(R.string.extend_home_apps_area), uiState.extendHomeAppsArea) { checked ->
                        vm.setExtendHomeAppsArea(checked)
                        onUpdate()
                    }
                }
                3 -> {
                    // Notifications tab

                    ToggleRow("Notification Badge", uiState.showNotificationBadge) { checked ->
                        vm.setShowNotificationBadge(checked)
                        onUpdate()
                    }

                    if (uiState.showNotificationBadge) {
                        val indicators = com.github.gezimos.inkos.data.Constants.NotificationIndicator.entries
                        val currentIdx = uiState.notificationIndicatorStyle.coerceIn(0, indicators.lastIndex)
                        val current = indicators[currentIdx]
                        SettingRow(label = "Badge Indicator", value = current.symbol) {
                            currentSheet?.dismiss()
                            dialogManager.showBadgeIndicatorPicker(
                                currentIndicator = current,
                                onSelect = { indicator ->
                                    vm.setNotificationIndicatorStyle(indicator.ordinal)
                                    onUpdate()
                                }
                            )
                        }
                    }

                    ToggleRow("Label Notifications", uiState.showNotificationText) { checked ->
                        vm.setShowNotificationText(checked)
                        onUpdate()
                    }

                    ToggleRow("Media Playing Badge", uiState.showMediaIndicator) { checked ->
                        vm.setShowMediaIndicator(checked)
                        onUpdate()
                    }

                    ToggleRow("Media Playing Name", uiState.showMediaName) { checked ->
                        vm.setShowMediaName(checked)
                        onUpdate()
                    }

                    // Move Character Limit above the Audio Widget
                    SettingRow(label = "Character Limit", value = "${uiState.homeAppCharLimit}") {
                        currentSheet?.dismiss()
                        dialogManager.showSliderDialog(
                            context = context,
                            title = "Character Limit",
                            minValue = 1,
                            maxValue = 50,
                            currentValue = uiState.homeAppCharLimit,
                            liveUpdate = true,
                            onValueSelected = { value ->
                                vm.setHomeAppCharLimit(value)
                                onUpdate()
                            }
                        )
                    }

                    ToggleRow("Enable Audio Widget", uiState.showAudioWidget) { checked ->
                        vm.setShowAudioWidget(checked)
                        onUpdate()
                    }

                    // Link to full notification settings fragment
                    SettingRow(label = "All Notification settings >", value = "") {
                        currentSheet?.dismiss()
                        try {
                            fragment.findNavController().navigate(
                                R.id.settingsFragment,
                                bundleOf("initialRoute" to "Notifications")
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
    }
    fun showHomeAppsAndPagesOnly(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()
        showDialog(context, prefs) {
            val vm = try {
                androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
            } catch (_: Exception) { null }

            var homeApps by remember { androidx.compose.runtime.mutableIntStateOf(prefs.homeAppsNum) }
            var homePages by remember { androidx.compose.runtime.mutableIntStateOf(prefs.homePagesNum) }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Home apps slider
                Text(
                    "Number of home apps: $homeApps",
                    style = SettingsTheme.typography.item,
                    color = Theme.colors.text
                )
                com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                    value = homeApps,
                    onValueChange = { value ->
                        homeApps = value
                        if (vm != null) vm.setHomeAppsNum(value) else prefs.homeAppsNum = value
                        try { Constants.updateMaxHomePages(context) } catch (_: Exception) {}
                        onUpdate()
                    },
                    minValue = Constants.MIN_HOME_APPS,
                    maxValue = Constants.MAX_HOME_APPS
                )

                // Pages slider
                Text(
                    "Number of pages: $homePages",
                    style = SettingsTheme.typography.item,
                    color = Theme.colors.text
                )
                com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                    value = homePages,
                    onValueChange = { value ->
                        homePages = value
                        if (vm != null) vm.setHomePagesNum(value) else prefs.homePagesNum = value
                        onUpdate()
                    },
                    minValue = Constants.MIN_HOME_PAGES,
                    maxValue = Constants.MAX_HOME_PAGES
                )

            }
        }
    }
    fun showAppDrawerSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit, folderPickerLauncher: androidx.activity.result.ActivityResultLauncher<android.net.Uri?>? = null) {
        currentSheet?.dismiss()
        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showAppDrawerSettings(context, fragment, prefs, onUpdate, folderPickerLauncher) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        showDialog(context, prefs) {
            val selectedTabIndex = remember { mutableStateOf(0) }
            val screenScale = rememberScreenScale()
            val uiState by vm.homeUiState.collectAsState()
            val drawerState by vm.appsDrawerUiState.collectAsState()
            val drawerTitle = when (selectedTabIndex.value) { 0 -> "Style"; 1 -> "Search"; else -> "Filtering" }
            SheetTitle(drawerTitle)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SheetTabRow(
                    selectedIndex = selectedTabIndex.value,
                    tabs = listOf(
                        { Icon(imageVector = Icons.Rounded.TextFields, contentDescription = "Style", ) },
                        { Icon(imageVector = Icons.Rounded.Search, contentDescription = "Search", ) },
                        { Icon(imageVector = Icons.Rounded.FilterList, contentDescription = "Filtering", ) }
                    ),
                    onTabSelected = { selectedTabIndex.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp.scaled(screenScale)))
                when (selectedTabIndex.value) {
                    0 -> {
                        SettingRow(label = "App Size/Gap", value = "${uiState.appDrawerSize}/${uiState.appDrawerGap}") {
                            currentSheet?.dismiss()
                            showDialog(context, prefs) {
                                var drawerSize by remember { androidx.compose.runtime.mutableIntStateOf(uiState.appDrawerSize) }
                                var drawerGap by remember { androidx.compose.runtime.mutableIntStateOf(uiState.appDrawerGap) }

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        "App size: $drawerSize",
                                        style = SettingsTheme.typography.item,
                                        color = Theme.colors.text
                                    )
                                    com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                        value = drawerSize,
                                        onValueChange = { value ->
                                            drawerSize = value
                                            vm.setAppDrawerSize(value)
                                            onUpdate()
                                        },
                                        minValue = Constants.MIN_APP_SIZE,
                                        maxValue = Constants.MAX_APP_SIZE
                                    )

                                    Text(
                                        "App gap: $drawerGap",
                                        style = SettingsTheme.typography.item,
                                        color = Theme.colors.text
                                    )
                                    com.github.gezimos.inkos.ui.dialogs.StepperSlider(
                                        value = drawerGap,
                                        onValueChange = { value ->
                                            drawerGap = value
                                            vm.setAppDrawerGap(value)
                                            onUpdate()
                                        },
                                        minValue = Constants.MIN_TEXT_PADDING,
                                        maxValue = Constants.MAX_TEXT_PADDING
                                    )
                                }
                            }
                        }
                        val alignmentLabels = arrayOf(
                            context.getString(R.string.left),
                            context.getString(R.string.center),
                            context.getString(R.string.right)
                        )
                        CycleRow(
                            label = context.getString(R.string.app_drawer_alignment),
                            options = alignmentLabels,
                            currentIndex = uiState.appDrawerAlignment,
                            showAlignment = true
                        ) { nextIndex ->
                            vm.setAppDrawerAlignment(nextIndex)
                            onUpdate()
                        }
                        ToggleRow("Enable Icons", drawerState.drawerShowIcons) { checked ->
                            vm.setDrawerShowIcons(checked)
                            onUpdate()
                        }
                    }
                    1 -> {
                        ToggleRow("Enable Search", uiState.appDrawerSearchEnabled) { checked ->
                            vm.setAppDrawerSearchEnabled(checked)
                            onUpdate()
                        }
                        val searchEnabled = uiState.appDrawerSearchEnabled
                        ToggleRow("Auto Show Keyboard", uiState.appDrawerAutoShowKeyboard, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerAutoShowKeyboard(checked)
                            onUpdate()
                        }
                        ToggleRow("Auto-launch result", uiState.appDrawerAutoLaunch, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerAutoLaunch(checked)
                            onUpdate()
                        }
                        ToggleRow(context.getString(R.string.search_hidden_apps), uiState.appDrawerSearchHiddenAppsEnabled, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerSearchHiddenAppsEnabled(checked)
                            onUpdate()
                        }
                        ToggleRow("Search Contacts", uiState.appDrawerSearchContactsEnabled, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerSearchContactsEnabled(checked)
                            onUpdate()
                        }
                        if (uiState.appDrawerSearchContactsEnabled && searchEnabled) {
                            val accounts = remember { ContactsHelper.getAvailableAccounts(context) }
                            if (accounts.size > 1) {
                                val selectedAccounts = uiState.appDrawerSearchContactAccounts
                                val displayOption = if (selectedAccounts == null) "All" else "${selectedAccounts.size}/${accounts.size}"
                                SettingRow(label = "Contacts to show", value = displayOption) {
                                    currentSheet?.dismiss()
                                    val items = accounts.map { it.second }.toTypedArray()
                                    val initialChecked = BooleanArray(accounts.size) { idx ->
                                        selectedAccounts == null || accounts[idx].first in selectedAccounts
                                    }
                                    dialogManager.showMultiChoiceDialog(
                                        context = context,
                                        title = "Contacts to show",
                                        items = items,
                                        initialChecked = initialChecked,
                                        onConfirm = { selectedIndices ->
                                            val selected = selectedIndices.map { accounts[it].first }.toSet()
                                            vm.setAppDrawerSearchContactAccounts(
                                                if (selected.size == accounts.size) null else selected
                                            )
                                            onUpdate()
                                        }
                                    )
                                }
                            }
                        }
                        ToggleRow("Search Web", uiState.appDrawerSearchWebEnabled, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerSearchWebEnabled(checked)
                            onUpdate()
                        }
                        ToggleRow("Search Settings", uiState.appDrawerSearchSettingsEnabled, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerSearchSettingsEnabled(checked)
                            onUpdate()
                        }
                        ToggleRow("Search Music", uiState.appDrawerSearchMusicEnabled, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerSearchMusicEnabled(checked)
                            onUpdate()
                        }
                        ToggleRow("Search Files", uiState.appDrawerSearchFilesEnabled, enabled = searchEnabled) { checked ->
                            vm.setAppDrawerSearchFilesEnabled(checked)
                            onUpdate()
                            if (checked && folderPickerLauncher != null && FileSearchHelper.getPersistedFolders(context).isEmpty()) {
                                currentSheet?.dismiss()
                                folderPickerLauncher.launch(null)
                            }
                        }
                        if (uiState.appDrawerSearchFilesEnabled && searchEnabled && folderPickerLauncher != null) {
                            val folderCount = remember {
                                FileSearchHelper.getPersistedFolders(context).size
                            }
                            SettingRow(label = "Search Folders", value = if (folderCount == 0) "Add" else folderCount.toString()) {
                                currentSheet?.dismiss()
                                dialogManager.showSheet {
                                    com.github.gezimos.inkos.ui.dialogs.SearchFoldersSheet(
                                        context = context,
                                        onAddFolder = {
                                            dialogManager.dismissAll()
                                            folderPickerLauncher.launch(null)
                                        },
                                        onRemoveFolder = { uri ->
                                            FileSearchHelper.removeFolderAccess(context, uri)
                                        },
                                        onFolderCountChanged = { },
                                        onDismiss = { dialogManager.dismissAll() }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        ToggleRow(context.getString(R.string.az_filter), uiState.appDrawerAzFilter) { checked ->
                            vm.setAppDrawerAzFilter(checked)
                            onUpdate()
                        }
                        val sortOrderOptions = arrayOf("A-Z", "Most Used", "Last Used")
                        CycleRow(
                            label = "Sort Order",
                            options = sortOrderOptions,
                            currentIndex = drawerState.appDrawerSortOrder.coerceIn(0, 2),
                        ) { nextIndex ->
                            vm.setAppDrawerSortOrder(nextIndex)
                            onUpdate()
                        }
                        ToggleRow("Hide Home Apps", uiState.hideHomeApps) { checked ->
                            vm.setHideHomeApps(checked)
                            onUpdate()
                        }
                        SettingRow(
                            label = context.getString(R.string.settings_hidden_apps_title),
                            value = if (prefs.hiddenApps.isEmpty()) "None" else "${prefs.hiddenApps.size} hidden",
                        ) {
                            currentSheet?.dismiss()
                            vm.getHiddenApps()
                            fragment.findNavController().navigate(
                                R.id.appsFragment,
                                bundleOf("flag" to Constants.AppDrawerFlag.HiddenApps.toString())
                            )
                            onUpdate()
                        }
                        val appShortcutsOptionText = run {
                            val allShortcuts = getAllAppShortcuts(context)
                            val validKeys = allShortcuts.map { it.key }.toSet()
                            val inkosKeys = allShortcuts.filter {
                                it.packageName == BuildConfig.APPLICATION_ID && it.shortcutId.startsWith("inkos_")
                            }.map { it.key }.toSet()
                            val selectedRaw = prefs.selectedAppShortcuts
                            val selectionBeenSet = prefs.hasSelectedAppShortcutsBeenSet()
                            val effectiveSelection = if (!selectionBeenSet) {
                                allShortcuts.filter { it.isPinned }.map { it.key }.toSet() + inkosKeys
                            } else {
                                selectedRaw.filter { validKeys.contains(it) }.toSet() +
                                    inkosKeys.filter { validKeys.contains(it) }
                            }
                            if (allShortcuts.isEmpty()) "None"
                            else if (effectiveSelection.isEmpty()) "None"
                            else "${effectiveSelection.size} selected"
                        }
                        SettingRow(
                            label = context.getString(R.string.app_shortcuts),
                            value = appShortcutsOptionText,
                        ) {
                            currentSheet?.dismiss()
                            val allShortcuts = getAllAppShortcuts(context)
                            if (allShortcuts.isEmpty()) {
                                android.widget.Toast.makeText(context, "No app shortcuts available", android.widget.Toast.LENGTH_SHORT).show()
                                return@SettingRow
                            }
                            val inkosShortcuts = allShortcuts.filter {
                                it.packageName == BuildConfig.APPLICATION_ID && it.shortcutId.startsWith("inkos_")
                            }
                            val pinnedShortcuts = allShortcuts.filter {
                                it.isPinned && it.packageName != BuildConfig.APPLICATION_ID
                            }
                            val appShortcuts = allShortcuts.filter {
                                !it.isPinned && it.packageName != BuildConfig.APPLICATION_ID
                            }
                            val inkosItems = inkosShortcuts.map { ShortcutItem(it.key, it.label) }
                            val pinnedItems = pinnedShortcuts.map { ShortcutItem(it.key, it.label) }
                            val pm = context.packageManager
                            val appGroups = appShortcuts.groupBy { it.packageName }.map { (pkg, shortcuts) ->
                                val appName = try {
                                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                                } catch (_: Exception) { pkg.substringAfterLast('.') }
                                ShortcutGroup(
                                    groupName = appName,
                                    items = shortcuts.map { ShortcutItem(it.key, it.label) }
                                )
                            }.sortedBy { it.groupName.lowercase() }
                            val validKeys = allShortcuts.map { it.key }.toSet()
                            val inkosKeysSet = inkosShortcuts.map { it.key }.toSet()
                            val selectedRaw = prefs.selectedAppShortcuts
                            val selectionBeenSet = prefs.hasSelectedAppShortcutsBeenSet()
                            val effectiveSelection = if (!selectionBeenSet) {
                                pinnedShortcuts.map { it.key }.toSet() + inkosKeysSet
                            } else {
                                selectedRaw.filter { validKeys.contains(it) }.toSet()
                            }
                            if (selectionBeenSet && effectiveSelection.size < selectedRaw.size) {
                                vm.setSelectedAppShortcuts(effectiveSelection)
                            }
                            dialogManager.showTabbedShortcutsDialog(
                                title = context.getString(R.string.app_shortcuts),
                                appGroups = appGroups,
                                inkosItems = inkosItems,
                                pinnedItems = pinnedItems,
                                selectedKeys = effectiveSelection,
                                onSelectionChanged = { selected -> vm.setSelectedAppShortcuts(selected); onUpdate() }
                            )
                        }
                    }
                }
            }
        }
    }
    fun showNotificationScreenSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()
        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showNotificationScreenSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        showDialog(context, prefs) {
            val selectedTabIndex = remember { mutableStateOf(0) }
            val screenScale = rememberScreenScale()
            val uiState by vm.homeUiState.collectAsState()
            val notifTitle = when (selectedTabIndex.value) { 0 -> "Style"; else -> "Function" }
            SheetTitle(notifTitle)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                SheetTabRow(
                    selectedIndex = selectedTabIndex.value,
                    tabs = listOf(
                        { Icon(imageVector = Icons.Rounded.TextFields, contentDescription = "Style", ) },
                        { Icon(imageVector = Icons.Rounded.Checklist, contentDescription = "Function", ) }
                    ),
                    onTabSelected = { selectedTabIndex.value = it }
                )
                Spacer(modifier = Modifier.height(8.dp.scaled(screenScale)))
                when (selectedTabIndex.value) {
                    0 -> {
                        // Style tab: notification screen typography
                        SettingRow(
                            label = context.getString(R.string.title_label),
                            value = uiState.lettersTitle,
                        ) {
                            currentSheet?.dismiss()
                            dialogManager.showInputDialog(
                                context = context,
                                title = context.getString(R.string.title_label),
                                initialValue = uiState.lettersTitle,
                                onValueEntered = { text ->
                                    val singleLine = text.replace("\n", "")
                                    vm.setLettersTitle(singleLine)
                                    onUpdate()
                                }
                            )
                        }
                        SettingRow(
                            label = context.getString(R.string.title_font),
                            value = getFontDisplayName(prefs, uiState.lettersTitleFont, "lettersTitle"),
                        ) {
                            currentSheet?.dismiss()
                            showFontPickerDialog(
                                context = context,
                                dialogManager = dialogManager,
                                vm = vm,
                                prefs = prefs,
                                titleResId = R.string.title_font,
                                fontContextKey = "lettersTitle",
                                getCurrentFont = { prefs.getFontForContext("lettersTitle") },
                                setFont = { font ->
                                    vm.setLettersTitleFont(font)
                                },
                                onUpdate = onUpdate
                            )
                        }
                        SettingRow(
                            label = context.getString(R.string.title_size),
                            value = "${uiState.lettersTitleSize}",
                        ) {
                            currentSheet?.dismiss()
                            dialogManager.showSliderDialog(
                                context = context,
                                title = context.getString(R.string.title_size),
                                minValue = 10,
                                maxValue = 60,
                                currentValue = uiState.lettersTitleSize,
                                liveUpdate = true,
                                onValueSelected = { value ->
                                    vm.setLettersTitleSize(value)
                                    onUpdate()
                                }
                            )
                        }
                        SettingRow(
                            label = context.getString(R.string.body_font),
                            value = getFontDisplayName(prefs, uiState.notificationsFont, "notifications"),
                        ) {
                            currentSheet?.dismiss()
                            showFontPickerDialog(
                                context = context,
                                dialogManager = dialogManager,
                                vm = vm,
                                prefs = prefs,
                                titleResId = R.string.body_font,
                                fontContextKey = "notifications",
                                getCurrentFont = { prefs.getFontForContext("notifications") },
                                setFont = { font ->
                                    vm.setNotificationsFont(font)
                                },
                                onUpdate = onUpdate
                            )
                        }
                        SettingRow(
                            label = context.getString(R.string.body_text_size),
                            value = "${uiState.notificationsTextSize}",
                        ) {
                            currentSheet?.dismiss()
                            dialogManager.showSliderDialog(
                                context = context,
                                title = context.getString(R.string.body_text_size),
                                minValue = Constants.MIN_LABEL_NOTIFICATION_TEXT_SIZE,
                                maxValue = Constants.MAX_LABEL_NOTIFICATION_TEXT_SIZE,
                                currentValue = uiState.notificationsTextSize,
                                liveUpdate = true,
                                onValueSelected = { value ->
                                    vm.setNotificationsTextSize(value)
                                    onUpdate()
                                }
                            )
                        }
                    }
                    1 -> {
                        // Function tab: toggles + links to full settings
                        ToggleRow(
                            context.getString(R.string.letters_window),
                            uiState.notificationsEnabled
                        ) { checked ->
                            vm.setNotificationsEnabled(checked)
                            onUpdate()
                        }
                        ToggleRow(
                            "Clear conversation on app open",
                            uiState.clearConversationOnAppOpen
                        ) { checked ->
                            vm.setClearConversationOnAppOpen(checked)
                            onUpdate()
                        }
                        SettingRow(label = "All Notification settings >", value = "") {
                            currentSheet?.dismiss()
                            try {
                                fragment.findNavController().navigate(R.id.settingsFragment)
                            } catch (_: Exception) {}
                        }
                        SettingRow(
                            label = context.getString(R.string.letters_allowlist),
                            value = if (uiState.allowedNotificationApps.isEmpty()) "None" else "${uiState.allowedNotificationApps.size} selected",
                        ) {
                            currentSheet?.dismiss()
                            fragment.viewLifecycleOwner.lifecycleScope.launch {
                                vm.getAppList(includeHiddenApps = true).collectLatest { appListItems ->
                                    if (appListItems.isEmpty()) return@collectLatest
                                    val filteredApps = appListItems.filter {
                                        val pkg = it.activityPackage
                                        pkg.isNotBlank() &&
                                            !IconUtility.isSyntheticPackage(pkg) &&
                                            !(it.isShortcut && it.shortcutId != null && IconUtility.isInkOSInternalShortcut(it.shortcutId))
                                    }
                                    data class AppInfo(val label: String, val packageName: String, val user: String)
                                    val allApps = filteredApps.map {
                                        AppInfo(
                                            label = it.customLabel.takeIf { l -> l.isNotEmpty() } ?: it.activityLabel,
                                            packageName = it.activityPackage,
                                            user = it.user.toString()
                                        )
                                    }
                                    val initialSelected = vm.homeUiState.value.allowedNotificationApps
                                    val collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }
                                    val sortedApps = allApps.sortedWith(
                                        compareByDescending<AppInfo> { initialSelected.contains(it.packageName) }
                                            .then(compareBy(collator) { it.label })
                                    )
                                    val appLabels = sortedApps.map { it.label }
                                    val appPackages = sortedApps.map { it.packageName }
                                    val checkedItems = appPackages.mapIndexed { idx, pkg ->
                                        val userStr = sortedApps[idx].user
                                        val pkgWithUser = if (userStr.isNotBlank()) "$pkg|$userStr" else pkg
                                        initialSelected.contains(pkg) || initialSelected.contains(pkgWithUser)
                                    }.toBooleanArray()
                                    dialogManager.showMultiChoiceDialog(
                                        context = context,
                                        title = context.getString(R.string.letters_allowlist),
                                        items = appLabels.toTypedArray(),
                                        initialChecked = checkedItems,
                                        maxHeightRatio = 0.60f,
                                        onConfirm = { selectedIndices ->
                                            val selectedPkgs = selectedIndices.map { idx ->
                                                val pkg = appPackages[idx]
                                                val userStr = sortedApps[idx].user
                                                val pkgWithUser = if (userStr.isNotBlank()) "$pkg|$userStr" else pkg
                                                if (initialSelected.contains(pkgWithUser)) pkgWithUser else pkg
                                            }.toSet()
                                            vm.setAllowedNotificationApps(selectedPkgs)
                                            onUpdate()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    fun showSimpleTrayNotificationSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()
        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showSimpleTrayNotificationSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        showDialog(context, prefs) {
            val uiState by vm.homeUiState.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Title(text = "Simple Tray")
                SettingRow(
                    label = context.getString(R.string.notifications_per_page),
                    value = uiState.notificationsPerPage.toString(),
                ) {
                    val next = (uiState.notificationsPerPage % 5) + 1
                    vm.setNotificationsPerPage(next)
                    onUpdate()
                }
                SettingRow(
                    label = "Simple Tray Allowlist",
                    value = if (uiState.allowedSimpleTrayApps.isEmpty()) "None" else "${uiState.allowedSimpleTrayApps.size} selected",
                ) {
                    currentSheet?.dismiss()
                    fragment.viewLifecycleOwner.lifecycleScope.launch {
                        vm.getAppList(includeHiddenApps = true).collectLatest { appListItems ->
                            if (appListItems.isEmpty()) return@collectLatest
                            val filteredApps = appListItems.filter {
                                val pkg = it.activityPackage
                                pkg.isNotBlank() &&
                                    !IconUtility.isSyntheticPackage(pkg) &&
                                    !(it.isShortcut && it.shortcutId != null && IconUtility.isInkOSInternalShortcut(it.shortcutId))
                            }
                            data class AppInfo(val label: String, val packageName: String, val user: String)
                            val allApps = filteredApps.map {
                                AppInfo(
                                    label = it.customLabel.takeIf { l -> l.isNotEmpty() } ?: it.activityLabel,
                                    packageName = it.activityPackage,
                                    user = it.user.toString()
                                )
                            }
                            val initialSelected = vm.homeUiState.value.allowedSimpleTrayApps
                            val collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }
                            val sortedApps = allApps.sortedWith(
                                compareByDescending<AppInfo> { initialSelected.contains(it.packageName) }
                                    .then(compareBy(collator) { it.label })
                            )
                            val appLabels = sortedApps.map { it.label }
                            val appPackages = sortedApps.map { it.packageName }
                            val checkedItems = appPackages.mapIndexed { idx, pkg ->
                                val userStr = sortedApps[idx].user
                                val pkgWithUser = if (userStr.isNotBlank()) "$pkg|$userStr" else pkg
                                initialSelected.contains(pkg) || initialSelected.contains(pkgWithUser)
                            }.toBooleanArray()
                            dialogManager.showMultiChoiceDialog(
                                context = context,
                                title = "Simple Tray Apps",
                                items = appLabels.toTypedArray(),
                                initialChecked = checkedItems,
                                maxHeightRatio = 0.60f,
                                onConfirm = { selectedIndices ->
                                    val selectedPkgs = selectedIndices.map { idx ->
                                        val pkg = appPackages[idx]
                                        val userStr = sortedApps[idx].user
                                        val pkgWithUser = if (userStr.isNotBlank()) "$pkg|$userStr" else pkg
                                        if (initialSelected.contains(pkgWithUser)) pkgWithUser else pkg
                                    }.toSet()
                                    vm.setAllowedSimpleTrayApps(selectedPkgs)
                                    onUpdate()
                                }
                            )
                        }
                    }
                }
                SettingRow(label = "All Notification Settings >", value = "") {
                    currentSheet?.dismiss()
                    try {
                        fragment.findNavController().navigate(R.id.settingsFragment)
                    } catch (_: Exception) {}
                }
            }
        }
    }
    fun showHubSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()
        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showHubSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        showDialog(context, prefs) {
            val uiState by vm.homeUiState.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Title(text = "Hub")
                SettingRow(
                    label = "Hub Allowlist",
                    value = if (uiState.allowedSimpleTrayApps.isEmpty()) "None" else "${uiState.allowedSimpleTrayApps.size} selected",
                ) {
                    currentSheet?.dismiss()
                    fragment.viewLifecycleOwner.lifecycleScope.launch {
                        vm.getAppList(includeHiddenApps = true).collectLatest { appListItems ->
                            if (appListItems.isEmpty()) return@collectLatest
                            val filteredApps = appListItems.filter {
                                val pkg = it.activityPackage
                                pkg.isNotBlank() &&
                                    !IconUtility.isSyntheticPackage(pkg) &&
                                    !(it.isShortcut && it.shortcutId != null && IconUtility.isInkOSInternalShortcut(it.shortcutId))
                            }
                            data class AppInfo(val label: String, val packageName: String, val user: String)
                            val allApps = filteredApps.map {
                                AppInfo(
                                    label = it.customLabel.takeIf { l -> l.isNotEmpty() } ?: it.activityLabel,
                                    packageName = it.activityPackage,
                                    user = it.user.toString()
                                )
                            }
                            val initialSelected = vm.homeUiState.value.allowedSimpleTrayApps
                            val collator = java.text.Collator.getInstance().apply { strength = java.text.Collator.PRIMARY }
                            val sortedApps = allApps.sortedWith(
                                compareByDescending<AppInfo> { initialSelected.contains(it.packageName) }
                                    .then(compareBy(collator) { it.label })
                            )
                            val appLabels = sortedApps.map { it.label }
                            val appPackages = sortedApps.map { it.packageName }
                            val checkedItems = appPackages.mapIndexed { idx, pkg ->
                                val userStr = sortedApps[idx].user
                                val pkgWithUser = if (userStr.isNotBlank()) "$pkg|$userStr" else pkg
                                initialSelected.contains(pkg) || initialSelected.contains(pkgWithUser)
                            }.toBooleanArray()
                            dialogManager.showMultiChoiceDialog(
                                context = context,
                                title = "Hub Apps",
                                items = appLabels.toTypedArray(),
                                initialChecked = checkedItems,
                                maxHeightRatio = 0.60f,
                                onConfirm = { selectedIndices ->
                                    val selectedPkgs = selectedIndices.map { idx ->
                                        val pkg = appPackages[idx]
                                        val userStr = sortedApps[idx].user
                                        val pkgWithUser = if (userStr.isNotBlank()) "$pkg|$userStr" else pkg
                                        if (initialSelected.contains(pkgWithUser)) pkgWithUser else pkg
                                    }.toSet()
                                    vm.setAllowedSimpleTrayApps(selectedPkgs)
                                    onUpdate()
                                }
                            )
                        }
                    }
                }
                SettingRow(label = "All Notification Settings >", value = "") {
                    currentSheet?.dismiss()
                    try {
                        fragment.findNavController().navigate(R.id.settingsFragment)
                    } catch (_: Exception) {}
                }
            }
        }
    }
    fun showSimpleTrayBackgroundSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()
        ComposeDialogManager(context, fragment.requireActivity())
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        showDialog(context, prefs) {
            val uiState by vm.homeUiState.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Title(text = "Simple Tray")
                ToggleRow(
                    text = context.getString(R.string.enable_bottom_navigation),
                    checked = uiState.enableBottomNav
                ) { checked ->
                    vm.setEnableBottomNav(checked)
                    onUpdate()
                }
                SettingRow(label = "All Notification Settings >", value = "") {
                    currentSheet?.dismiss()
                    try {
                        fragment.findNavController().navigate(R.id.settingsFragment)
                    } catch (_: Exception) {}
                }
            }
        }
    }
    fun showRecentsSettings(context: Context, fragment: Fragment, prefs: Prefs, onUpdate: () -> Unit) {
        currentSheet?.dismiss()
        val dialogManager = ComposeDialogManager(context, fragment.requireActivity())
        dialogManager.onAfterDismiss = { showRecentsSettings(context, fragment, prefs, onUpdate) }
        val vm = androidx.lifecycle.ViewModelProvider(fragment.requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        showDialog(context, prefs) {
            val uiState by vm.appsDrawerUiState.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Title(text = "Recents")

                val viewOptions = arrayOf("Recents", "Most Used")
                CycleRow("Default View", viewOptions, uiState.recentsDefaultView) { next ->
                    vm.setRecentsDefaultView(next)
                    onUpdate()
                }

                val filterOptions = arrayOf("Today", "This Week", "This Month", "All Time")
                CycleRow("Usage Period", filterOptions, uiState.recentsUsageFilter) { next ->
                    vm.setRecentsUsageFilter(next)
                    onUpdate()
                }

                val unitOptions = arrayOf("Time", "Money", "Coffee")
                CycleRow("Most Used Unit", unitOptions, uiState.recentsUsageUnit) { next ->
                    vm.setRecentsUsageUnit(next)
                    onUpdate()
                }

                if (uiState.recentsUsageUnit == 1) { // Money
                    SettingRow("Currency Symbol", uiState.recentsUnitCurrencyChar) {
                        currentSheet?.dismiss()
                        dialogManager.showInputDialog(
                            context = context,
                            title = "Currency Symbol",
                            initialValue = uiState.recentsUnitCurrencyChar
                        ) { input ->
                            val char = input.trim().take(1).ifEmpty { "$" }
                            vm.setRecentsUnitCurrencyChar(char)
                            onUpdate()
                        }
                    }
                    SettingRow("Hourly Rate", uiState.recentsUnitCost.toString()) {
                        currentSheet?.dismiss()
                        dialogManager.showInputDialog(
                            context = context,
                            title = "Hourly Rate (whole number)",
                            initialValue = uiState.recentsUnitCost.toString()
                        ) { input ->
                            val cost = input.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 99999)
                                ?: uiState.recentsUnitCost
                            vm.setRecentsUnitCost(cost)
                            onUpdate()
                        }
                    }
                } else if (uiState.recentsUsageUnit == 2) { // Coffee
                    SettingRow("Unit Emoji", uiState.recentsUnitEmojiChar) {
                        currentSheet?.dismiss()
                        dialogManager.showInputDialog(
                            context = context,
                            title = "Unit Emoji",
                            initialValue = uiState.recentsUnitEmojiChar
                        ) { input ->
                            val emoji = input.trim().ifEmpty { "☕" }
                            vm.setRecentsUnitEmojiChar(emoji)
                            onUpdate()
                        }
                    }
                    SettingRow("Coffee Price", uiState.recentsUnitCoffeePrice.toString()) {
                        currentSheet?.dismiss()
                        dialogManager.showInputDialog(
                            context = context,
                            title = "Price per coffee (whole number)",
                            initialValue = uiState.recentsUnitCoffeePrice.toString()
                        ) { input ->
                            val price = input.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 99999)
                                ?: uiState.recentsUnitCoffeePrice
                            vm.setRecentsUnitCoffeePrice(price)
                            onUpdate()
                        }
                    }
                    SettingRow("Hourly Rate", uiState.recentsUnitCost.toString()) {
                        currentSheet?.dismiss()
                        dialogManager.showInputDialog(
                            context = context,
                            title = "Hourly Rate (whole number)",
                            initialValue = uiState.recentsUnitCost.toString()
                        ) { input ->
                            val cost = input.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 99999)
                                ?: uiState.recentsUnitCost
                            vm.setRecentsUnitCost(cost)
                            onUpdate()
                        }
                    }
                }

                SettingRow("Usage Permission", "") {
                    currentSheet?.dismiss()
                    try {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Helper functions (Compose)

    @Composable
    private fun Title(text: String) {
        Text(
            text = text.uppercase(),
            style = SettingsTheme.typography.header,
            fontWeight = FontWeight.Bold,
            color = Theme.colors.text,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
    }

    @Composable
    private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .then(with(SettingsComposable) {
                    Modifier.pillHighlight(isHighlighted, 6.dp, prefTextColor)
                })
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isHighlighted) prefBackgroundColor else prefTextColor,
                style = SettingsTheme.typography.item,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                color = if (isHighlighted) prefBackgroundColor else prefTextColor,
                style = SettingsTheme.typography.item,
                textAlign = TextAlign.End
            )
        }
    }

    @Composable
    private fun ColorSettingRow(label: String, color1: Color, color2: Color, onClick: () -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background
        val circleBorder = if (isHighlighted) prefBackgroundColor else prefTextColor

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .then(with(SettingsComposable) {
                    Modifier.pillHighlight(isHighlighted, 6.dp, prefTextColor)
                })
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onClick()
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isHighlighted) prefBackgroundColor else prefTextColor,
                style = SettingsTheme.typography.item,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .requiredSize(16.dp)
                        .border(1.5.dp, circleBorder, CircleShape)
                        .padding(1.5.dp)
                        .border(1.dp, Color.White, CircleShape)
                        .background(color1, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .requiredSize(16.dp)
                        .border(1.5.dp, circleBorder, CircleShape)
                        .padding(1.5.dp)
                        .border(1.dp, Color.White, CircleShape)
                        .background(color2, CircleShape)
                )
            }
        }
    }

    @Composable
    private fun ToggleRow(text: String, checked: Boolean, enabled: Boolean = true, onToggle: (Boolean) -> Unit) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = enabled && isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background

        val isChecked = checked

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .alpha(if (enabled) 1f else 0.5f)
                .then(with(SettingsComposable) {
                    Modifier.pillHighlight(isHighlighted, 6.dp, prefTextColor)
                })
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (!enabled) return@clickable
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onToggle(!isChecked)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (isHighlighted) prefBackgroundColor else prefTextColor,
                style = SettingsTheme.typography.item,
                modifier = Modifier.weight(1f)
            )
            Box(contentAlignment = Alignment.Center) {
                SettingsComposable.CustomToggleSwitch(
                    checked = isChecked,
                    onCheckedChange = { checkedState ->
                        if (enabled) {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onToggle(checkedState)
                        }
                    },
                    tint = if (isHighlighted) prefBackgroundColor else prefTextColor
                )
            }
        }
    }

    @Composable
    private fun CycleRow(label: String, options: Array<String>, currentIndex: Int, showAlignment: Boolean = false, onCycle: (Int) -> Unit) {
        var idx by androidx.compose.runtime.remember(currentIndex) { mutableStateOf(currentIndex.coerceIn(0, options.size - 1)) }
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused = interactionSource.collectIsFocusedAsState().value
        val isHighlighted = isFocused
        val prefTextColor = Theme.colors.text
        val prefBackgroundColor = Theme.colors.background

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .then(with(SettingsComposable) {
                    Modifier.pillHighlight(isHighlighted, 6.dp, prefTextColor)
                })
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        idx = (idx + 1) % options.size
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onCycle(idx)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isHighlighted) prefBackgroundColor else prefTextColor,
                style = SettingsTheme.typography.item,
                modifier = Modifier.weight(1f)
            )
            if (showAlignment) {
                with(SettingsComposable) {
                    AlignmentIcon(
                        alignment = idx,
                        tint = if (isHighlighted) prefBackgroundColor else prefTextColor
                    )
                }
            } else {
                Text(
                    text = options[idx],
                    color = if (isHighlighted) prefBackgroundColor else prefTextColor,
                    style = SettingsTheme.typography.item,
                    textAlign = TextAlign.End
                )
            }
        }
    }
    
    private fun showDialog(context: Context, prefs: Prefs, content: @Composable () -> Unit) {
        currentSheet?.dismiss()
        val activity = context as? android.app.Activity ?: return
        val host = ComposeBottomSheetHost(activity)
        host.show {
            SettingsTheme(isDark = prefs.getResolvedTheme() == Constants.Theme.Dark) {
                content()
            }
        }
        currentSheet = host
    }
    
}

@Composable
internal fun WidgetAppListContent(
    context: Context,
    widgetApps: List<AppWidgetHelper.WidgetApp>,
    onAppSelected: (AppWidgetHelper.WidgetApp) -> Unit,
    onClose: () -> Unit
) {
    val pm = context.packageManager
    Text(
        text = "CHOOSE APP",
        style = SettingsTheme.typography.header,
        fontWeight = FontWeight.Bold,
        color = Theme.colors.text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        textAlign = TextAlign.Start
    )
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(widgetApps) { app ->
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused = interactionSource.collectIsFocusedAsState().value
            val isHighlighted = isFocused
            val prefTextColor = Theme.colors.text
            val prefBackgroundColor = Theme.colors.background

            val icon = remember(app.packageName) {
                try { pm.getApplicationIcon(app.packageName) } catch (_: Exception) { null }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .then(with(SettingsComposable) {
                        Modifier.pillHighlight(isHighlighted, 6.dp, prefTextColor)
                    })
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onAppSelected(app)
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    val bmp = remember(app.packageName) {
                        drawableToBitmap(icon).asImageBitmap()
                    }
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.graphics.painter.BitmapPainter(bmp),
                        contentDescription = app.appLabel,
                        modifier = Modifier
                            .height(32.dp)
                            .padding(end = 12.dp)
                    )
                }
                Text(
                    text = app.appLabel,
                    style = SettingsTheme.typography.item,
                    color = if (isHighlighted) prefBackgroundColor else prefTextColor,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${app.widgets.size}",
                    style = SettingsTheme.typography.item,
                    color = if (isHighlighted) prefBackgroundColor.copy(alpha = 0.7f) else prefTextColor.copy(alpha = 0.5f)
                )
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
        androidx.compose.material3.TextButton(onClick = onClose) {
            Text("Close", color = Theme.colors.text)
        }
    }
}

@Composable
internal fun WidgetListContent(
    app: AppWidgetHelper.WidgetApp,
    appWidgetHelper: AppWidgetHelper,
    onBack: () -> Unit,
    onWidgetPicked: (android.appwidget.AppWidgetProviderInfo) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("← Back", color = Theme.colors.text)
        }
        Text(
            text = app.appLabel.uppercase(),
            style = SettingsTheme.typography.header,
            fontWeight = FontWeight.Bold,
            color = Theme.colors.text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(400.dp)
    ) {
        items(app.widgets) { widget ->
            val label = widget.first
            val providerInfo = widget.second
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused = interactionSource.collectIsFocusedAsState().value
            val isHighlighted = isFocused
            val prefTextColor = Theme.colors.text
            val prefBackgroundColor = Theme.colors.background

            val preview = remember(providerInfo.provider.className) {
                appWidgetHelper.loadWidgetPreview(providerInfo)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(with(SettingsComposable) {
                        Modifier.pillHighlight(isHighlighted, 8.dp, prefTextColor)
                    })
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onWidgetPicked(providerInfo)
                        }
                    )
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    style = SettingsTheme.typography.item,
                    fontWeight = FontWeight.Medium,
                    color = if (isHighlighted) prefBackgroundColor else prefTextColor
                )
                if (preview != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val bmp = remember(providerInfo.provider.className) {
                        drawableToBitmap(preview).asImageBitmap()
                    }
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.graphics.painter.BitmapPainter(bmp),
                        contentDescription = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        alpha = if (isHighlighted) 0.9f else 1f
                    )
                }
                val minW = providerInfo.minWidth
                val minH = providerInfo.minHeight
                if (minW > 0 && minH > 0) {
                    Text(
                        text = "${minW}×${minH} dp",
                        style = SettingsTheme.typography.item,
                        color = (if (isHighlighted) prefBackgroundColor else prefTextColor).copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
        androidx.compose.material3.TextButton(onClick = onBack) {
            Text("Back", color = Theme.colors.text)
        }
    }
}

internal fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): android.graphics.Bitmap {
    if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = androidx.core.graphics.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
