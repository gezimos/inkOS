package com.github.gezimos.inkos.ui

import android.app.Notification
import com.github.gezimos.common.openCameraApp
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.github.gezimos.inkos.ui.compose.OneTimeTooltip
import com.github.gezimos.inkos.ui.compose.TooltipBubble
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.HomeAppUiState
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.AppWidgetHelper
import com.github.gezimos.inkos.helper.EditModeHelper
import com.github.gezimos.inkos.helper.KeyMapperHelper
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.receivers.BatteryReceiver
import com.github.gezimos.inkos.helper.utils.BiometricHelper
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.common.launchCalendar
import com.github.gezimos.common.launchCalendarEvent
import com.github.gezimos.inkos.ui.compose.GestureHelper
import com.github.gezimos.inkos.ui.compose.HomeMediaAction
import com.github.gezimos.inkos.ui.compose.HomeUI
import com.github.gezimos.inkos.ui.compose.HomeUiCallbacks
import com.github.gezimos.inkos.ui.compose.NavHelper
import com.github.gezimos.inkos.ui.compose.gestureHelper
import com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragmentCompose : Fragment() {

    companion object {
        @JvmStatic
        var isHomeVisible: Boolean = false

        @JvmStatic
        var goToFirstPageSignal: Boolean = false

        @JvmStatic
        var pendingWidgetPickerSignal: Boolean = false

        @JvmStatic
        fun sendGoToFirstPageSignal() {
            goToFirstPageSignal = true
        }
    }

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var batteryReceiver: BatteryReceiver
    
    private val notificationManager: NotificationManager by lazy {
        NotificationManager.getInstance(requireContext())
    }
    private val audioWidgetHelper: AudioWidgetHelper by lazy {
        AudioWidgetHelper.getInstance(requireContext())
    }
    private val appWidgetHelper: AppWidgetHelper by lazy {
        AppWidgetHelper.getInstance(requireContext())
    }
    private val biometricHelper: BiometricHelper by lazy { BiometricHelper(this) }
    private val dialogManager: ComposeDialogManager by lazy {
        ComposeDialogManager(requireContext(), requireActivity())
    }
    private var vibrator: Vibrator? = null

    // Widget picker launcher
    var pendingWidgetId: Int = -1
    private lateinit var widgetPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var calendarPermissionLauncher: ActivityResultLauncher<String>
    lateinit var widgetConfigureLauncher: ActivityResultLauncher<Intent>
    lateinit var widgetBindLauncher: ActivityResultLauncher<Intent>

    // Font picker launcher for edit mode
    private var onCustomFontSelected: ((android.graphics.Typeface, String) -> Unit)? = null
    private lateinit var fontPickerLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        vibrator = try {
            requireContext().getSystemService(Vibrator::class.java)
        } catch (_: Exception) {
            null
        }

        // Register widget picker result handler
        widgetPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (widgetId != -1) {
                    // Check if widget needs configuration
                    val configIntent = appWidgetHelper.getConfigureIntent(widgetId)
                    if (configIntent != null) {
                        pendingWidgetId = widgetId
                        widgetConfigureLauncher.launch(configIntent)
                    } else {
                        appWidgetHelper.onWidgetPicked(widgetId)
                        viewModel.setAndroidWidgetId(widgetId)
                    }
                }
            } else {
                if (pendingWidgetId != -1) {
                    appWidgetHelper.deallocateWidgetId(pendingWidgetId)
                    pendingWidgetId = -1
                }
            }
        }

        // Register widget configure result handler
        widgetConfigureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK && pendingWidgetId != -1) {
                appWidgetHelper.onWidgetPicked(pendingWidgetId)
                viewModel.setAndroidWidgetId(pendingWidgetId)
            } else if (pendingWidgetId != -1) {
                appWidgetHelper.deallocateWidgetId(pendingWidgetId)
            }
            pendingWidgetId = -1
        }

        // Register calendar permission for Events widget
        calendarPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            viewModel.refreshEvents(requireContext())
            viewModel.refreshHomeAppsUiState(requireContext())
        }

        // Register font picker for edit mode
        fontPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: android.net.Uri? ->
            if (uri != null) {
                val fontFile = copyFontToInternalStorage(uri)
                if (fontFile != null) {
                    try {
                        val typeface = android.graphics.Typeface.createFromFile(fontFile)
                        typeface.style // validate
                        onCustomFontSelected?.invoke(typeface, fontFile.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        dialogManager.showErrorDialog(requireContext(), title = "Invalid Font File", message = "The selected file could not be loaded as a font. Please choose a valid font file.")
                    }
                } else {
                    dialogManager.showErrorDialog(requireContext(), title = "File Error", message = "Could not copy the selected file. Please try again.")
                }
            }
        }
        EditModeHelper.initFontPicker { callback ->
            onCustomFontSelected = callback
            fontPickerLauncher.launch(arrayOf(
                "font/ttf", "font/otf", "application/x-font-ttf",
                "application/x-font-opentype", "application/octet-stream"
            ))
        }

        // Register widget bind permission result handler
        widgetBindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK && pendingWidgetId != -1) {
                // Binding granted — check if configure is needed
                val configIntent = appWidgetHelper.getConfigureIntent(pendingWidgetId)
                if (configIntent != null) {
                    widgetConfigureLauncher.launch(configIntent)
                } else {
                    appWidgetHelper.onWidgetPicked(pendingWidgetId)
                    viewModel.setAndroidWidgetId(pendingWidgetId)
                    pendingWidgetId = -1
                }
            } else if (pendingWidgetId != -1) {
                appWidgetHelper.deallocateWidgetId(pendingWidgetId)
                pendingWidgetId = -1
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                LocalContext.current

                val fullRenderState by viewModel.homeRenderState.collectAsState()
                
                val isDark = remember(fullRenderState.backgroundColor) {
                    val color = fullRenderState.backgroundColor
                    val r = android.graphics.Color.red(color) / 255f
                    val g = android.graphics.Color.green(color) / 255f
                    val b = android.graphics.Color.blue(color) / 255f
                    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                    luminance < 0.5f
                }

                SettingsTheme(isDark = isDark) {
                    val homeAppsBoundsState = remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                    LocalDensity.current

                    val chunks = remember(fullRenderState.homeApps, fullRenderState.appsPerPage) {
                        fullRenderState.homeApps.chunked(fullRenderState.appsPerPage)
                    }
                    val appsOnPage = remember(chunks, fullRenderState.currentPage) {
                        chunks.getOrNull(fullRenderState.currentPage) ?: emptyList()
                    }
                    val selectedIndex = remember { mutableStateOf(0) }
                    val dpadMode = remember { mutableStateOf(false) }
                    val dpadActivatedAppId = remember { mutableStateOf<Int?>(null) }
                    val keyPressTracker = remember { com.github.gezimos.inkos.ui.compose.KeyPressTracker() }
                    val focusRequester = remember { FocusRequester() }
                    
                    // Multi-zone focus navigation state
                    val focusZone = remember { mutableStateOf(com.github.gezimos.inkos.ui.compose.FocusZone.APPS) }
                    val selectedMediaButton = remember { mutableStateOf(0) }

                    
                    // Observe edit mode state changes
                    val isEditMode by EditModeHelper.isEditModeFlow.collectAsState()

                    // One-time edit mode tooltip — multi-step with Next button
                    val editTooltipKey = "tooltip_edit_mode_shown"
                    var editTooltipStep by remember { mutableIntStateOf(0) }
                    LaunchedEffect(isEditMode) {
                        editTooltipStep = if (isEditMode && !prefs.isTooltipShown(editTooltipKey)) 1 else 0
                    }
                    if (isEditMode && editTooltipStep in 1..5) {
                        val isLastEditStep = editTooltipStep == 5
                        TooltipBubble(
                            title = when (editTooltipStep) {
                                1 -> "Background"
                                2 -> "Clock"
                                3 -> "Date"
                                4 -> "Bottom Widget"
                                else -> "Cue Bars"
                            },
                            lines = when (editTooltipStep) {
                                1 -> listOf("Tap on background (empty areas) to edit theme, alignment, etc")
                                2 -> listOf("Tap on clock to edit clock widget, font size, clock format etc")
                                3 -> listOf("Click on date to edit date row size, font, format etc")
                                4 -> listOf("Click on bottom widget to edit widget size, widget type & other settings")
                                else -> listOf("Drag the cue bars to adjust the position of the home elements")
                            },
                            icon = when (editTooltipStep) {
                                1 -> Icons.Rounded.Palette
                                2 -> Icons.Rounded.AccessTime
                                3 -> Icons.Rounded.CalendarMonth
                                4 -> Icons.Rounded.Widgets
                                else -> Icons.Rounded.DragHandle
                            },
                            alignment = Alignment.Center,
                            nextLabel = if (isLastEditStep) "Done" else "Next",
                            minContentHeight = 80.dp,
                            onNext = {
                                if (isLastEditStep) {
                                    prefs.markTooltipShown(editTooltipKey)
                                    editTooltipStep = 0
                                } else {
                                    editTooltipStep++
                                }
                            },
                            onDismiss = {
                                prefs.markTooltipShown(editTooltipKey)
                                editTooltipStep = 0
                            }
                        )
                    }

                    val lastPinchTriggerTime = remember { mutableStateOf(0L) }

                    LaunchedEffect(Unit) {
                        try { focusRequester.requestFocus() } catch (_: Exception) {}
                    }

                    LaunchedEffect(fullRenderState.currentPage, appsOnPage.size) {
                        selectedIndex.value = selectedIndex.value.coerceIn(0, maxOf(appsOnPage.size - 1, 0))
                    }
                    
        LaunchedEffect(focusZone.value) {
            if (focusZone.value != com.github.gezimos.inkos.ui.compose.FocusZone.APPS) {
                selectedIndex.value = -1
            } else if (appsOnPage.isNotEmpty() && selectedIndex.value < 0) {
                selectedIndex.value = 0
            }
            if (focusZone.value != com.github.gezimos.inkos.ui.compose.FocusZone.MEDIA_WIDGET) {
                selectedMediaButton.value = 0
            }
        }
        
                    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { keyEvent ->
                            try {
                                val handled = NavHelper.handleHomeKeyEvent(
                                    keyEvent = keyEvent,
                                    keyPressTracker = keyPressTracker,
                                    dpadMode = dpadMode,
                                    selectedIndex = selectedIndex,
                                    appsOnPageSize = appsOnPage.size,
                                    currentPage = fullRenderState.currentPage,
                                    totalPages = fullRenderState.totalPages,
                                    appsPerPage = fullRenderState.appsPerPage,
                                    adjustPageBy = { delta -> try { adjustPageBy(delta, fullRenderState.totalPages) } catch (_: Exception) {} },
                                    onAppClick = { index ->
                                        val app = appsOnPage.getOrNull(index)
                                        if (app != null) {
                                            dpadActivatedAppId.value = app.id
                                            handleHomeAppClick(app)
                                        }
                                    },
                                    onAppLongClick = { index ->
                                        val app = appsOnPage.getOrNull(index)
                                        if (app != null) {
                                            handleHomeAppLongClick(app)
                                        }
                                    },
                                    onNavigateBack = { /* not used here */ },
                                    onSwipeLeft = {
                                        try {
                                            GestureHelper.handleSwipeLeft(requireContext(), this@HomeFragmentCompose, viewModel, prefs)
                                        } catch (_: Exception) {}
                                    },
                                    onSwipeRight = {
                                        try {
                                            GestureHelper.handleSwipeRight(requireContext(), this@HomeFragmentCompose, viewModel, prefs)
                                        } catch (_: Exception) {}
                                    },
                                    disableSwipeGestures = focusZone.value == com.github.gezimos.inkos.ui.compose.FocusZone.MEDIA_WIDGET,
                                    // Multi-zone parameters
                                    focusZone = focusZone,
                                    selectedMediaButton = selectedMediaButton,
                                    showClock = fullRenderState.showClock || isEditMode,
                                    showDate = fullRenderState.showDate || isEditMode,
                                    showMediaWidget = (fullRenderState.showMediaWidget && fullRenderState.mediaInfo != null) || (isEditMode && fullRenderState.showMediaWidget),
                                    showQuote = fullRenderState.bottomWidgetType != Constants.BottomWidgetType.Disabled.value || isEditMode,
                                    onClockClick = { handleClockClick() },
                                    onDateClick = { handleDateClick() },
                                    onMediaAction = { buttonIndex ->
                                        val action = when (buttonIndex) {
                                            0 -> HomeMediaAction.Open
                                            1 -> HomeMediaAction.Previous
                                            2 -> HomeMediaAction.PlayPause
                                            3 -> HomeMediaAction.Next
                                            4 -> HomeMediaAction.Stop
                                            else -> HomeMediaAction.PlayPause
                                        }
                                        handleMediaAction(action)
                                    },
                                    onQuoteClick = { handleBottomWidgetClick() },
                                    onSwipeUp = if (prefs.swipeDownAction != Action.Disabled) {
                                        { try { GestureHelper.handleSwipeDown(requireContext(), this@HomeFragmentCompose, viewModel, prefs) } catch (_: Exception) {} }
                                    } else null,
                                    onSwipeDown = if (prefs.swipeUpAction != Action.Disabled) {
                                        { try { GestureHelper.handleSwipeUp(requireContext(), this@HomeFragmentCompose, viewModel, prefs) } catch (_: Exception) {} }
                                    } else null
                                )

                                return@onPreviewKeyEvent handled

                                // continues to the Activity.
                            } catch (_: Exception) {}
                            false
                        }
                        .gestureHelper(
                            shortSwipeRatio = prefs.shortSwipeThresholdRatio,
                            longSwipeRatio = prefs.longSwipeThresholdRatio,
                            onDoubleTap = {
                                try { dpadMode.value = false } catch (_: Exception) {}
                                try { handleDoubleTapAction() } catch (_: Exception) {}
                            },
                            onSwipeLeft = {
                                GestureHelper.handleSwipeLeft(requireContext(), this@HomeFragmentCompose, viewModel, prefs)
                            },
                            onSwipeRight = {
                                GestureHelper.handleSwipeRight(requireContext(), this@HomeFragmentCompose, viewModel, prefs)
                            },
                            onSwipeUp = if (prefs.swipeUpAction != Action.Disabled) {
                                {
                                    try { GestureHelper.handleSwipeUp(requireContext(), this@HomeFragmentCompose, viewModel, prefs) } catch (_: Exception) {}
                                }
                            } else null,
                            onSwipeDown = if (prefs.swipeDownAction != Action.Disabled) {
                                {
                                    try { GestureHelper.handleSwipeDown(requireContext(), this@HomeFragmentCompose, viewModel, prefs) } catch (_: Exception) {}
                                }
                            } else null,
                            onVerticalPageMove = { delta ->
                                try {
                                    if (fullRenderState.totalPages > 1) adjustPageBy(delta, fullRenderState.totalPages)
                                } catch (_: Exception) {}
                            },
                            onAnyTouch = { try {
                                dpadMode.value = false
                                focusZone.value = com.github.gezimos.inkos.ui.compose.FocusZone.APPS
                            } catch (_: Exception) {} },
                            useShortSwipeForActions = fullRenderState.totalPages <= 1
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                    val currentTime = System.currentTimeMillis()
                                    if (zoom < 0.99f && currentTime - lastPinchTriggerTime.value > 800L) {
                                        lastPinchTriggerTime.value = currentTime
                                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                                        showQuickMenuWithAuth()
                                    }
                                }
                        }
                    ) {
                        HomeUI(
                            state = fullRenderState,
                            selectedAppId = appsOnPage.getOrNull(selectedIndex.value)?.id,
                            showDpadMode = dpadMode.value,
                            dpadActivatedAppId = dpadActivatedAppId.value,
                            onDpadActivatedHandled = { id -> if (dpadActivatedAppId.value == id) dpadActivatedAppId.value = null },
                            focusZone = focusZone.value,
                            selectedMediaButton = selectedMediaButton.value,
                            showEditMode = isEditMode,
                            callbacks = HomeUiCallbacks(
                                onAppClick = { handleHomeAppClick(it) },
                                onAppLongClick = { handleHomeAppLongClick(it) },
                                onClockClick = { handleClockClick() },
                                onDateClick = { handleDateClick() },
                                onBatteryClick = { handleBatteryClick() },
                                onNotificationCountClick = { handleNotificationCountClick() },
                                onBottomWidgetClick = { handleBottomWidgetClick() },
                                onEventsGrantPermissionClick = { handleEventsGrantPermissionClick() },
                                onEventsCalendarClick = { requireContext().launchCalendar() },
                                onEventsEventClick = { event -> requireContext().launchCalendarEvent(event.eventId) },
                                onEventsPrevClick = { viewModel.setEventsIndex(viewModel.homeUiState.value.eventsIndex - 1) },
                                onEventsNextClick = { viewModel.setEventsIndex(viewModel.homeUiState.value.eventsIndex + 1) },
                                onEventsFirstClick = { viewModel.setEventsIndex(0) },
                                onEventsLastClick = { viewModel.setEventsIndex(viewModel.homeUiState.value.eventsList.size - 1) },
                                onShortcutLeftClick = {
                                    val action = viewModel.homeUiState.value.shortcutLeftAction
                                    val app = prefs.appShortcutLeft
                                    GestureHelper.executeAction(requireContext(), this@HomeFragmentCompose, viewModel, action, app)
                                },
                                onShortcutRightClick = {
                                    val action = viewModel.homeUiState.value.shortcutRightAction
                                    val app = prefs.appShortcutRight
                                    if (action == Constants.Action.OpenApp && app.activityPackage.isEmpty()) {
                                        requireContext().openCameraApp()
                                    } else {
                                        GestureHelper.executeAction(requireContext(), this@HomeFragmentCompose, viewModel, action, app)
                                    }
                                },
                                onMediaAction = { action -> handleMediaAction(action) },
                                onSwipeLeft = {},
                                onSwipeRight = {},
                                onPageDelta = { delta -> adjustPageBy(delta, fullRenderState.totalPages) },
                                onRootLongPress = { /* handled by wrapper */ },
                                onBackgroundClick = { handleBackgroundClick() },
                                onAndroidWidgetHeightChange = { height -> viewModel.setAndroidWidgetHeight(height) },
                                onAndroidWidgetMarginStartChange = { margin -> viewModel.setAndroidWidgetMarginStart(margin) },
                                onAndroidWidgetMarginEndChange = { margin -> viewModel.setAndroidWidgetMarginEnd(margin) },
                                onTopMarginDrag = { viewModel.setTopWidgetMargin(it) },
                                onAppsYOffsetDrag = { viewModel.setHomeAppsYOffset(it) },
                                onBottomMarginDrag = { viewModel.setBottomWidgetMargin(it) }
                            ),
                            onHomeAppsBoundsChanged = { homeAppsBoundsState.value = it },
                            onBottomWidgetHeightChanged = { viewModel.updateBottomWidgetHeightPx(it) }
                        )
                    }
                }

            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.isinkosDefault()
        viewModel.refreshHomeAppsUiState(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                NotificationManager.getInstance(requireContext()).notificationInfoState.collect { _ ->
                    // Refresh home apps to update notification badges
                    viewModel.refreshHomeAppsUiState(requireContext())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for AppWidget updates
        appWidgetHelper.startListening()
        batteryReceiver = BatteryReceiver { newDateText: String, newBatteryText: String, isCharging: Boolean ->
            try { viewModel.updateDateAndBatteryText(newDateText, newBatteryText, isCharging) } catch (_: Exception) {}
        }
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            requireContext().registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val batteryIntent = requireContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { batteryReceiver.onReceive(requireContext(), it) }
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop listening for AppWidget updates
        appWidgetHelper.stopListening()
        try {
            requireContext().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        isHomeVisible = true
        
        viewModel.refreshClock()
        
        if (goToFirstPageSignal) {
            viewModel.setCurrentPage(0)
            goToFirstPageSignal = false
        } else if (prefs.homeReset) {
            viewModel.setCurrentPage(0)
        }

        if (prefs.bottomWidgetType == Constants.BottomWidgetType.Events.value) {
            viewModel.refreshEvents(requireContext())
        }

        // Refresh total usage when returning to home
        if (prefs.bottomWidgetType == Constants.BottomWidgetType.TotalUsage.value) {
            viewModel.loadDailyTotalUsage()
        }

        if (pendingWidgetPickerSignal) {
            pendingWidgetPickerSignal = false
            launchWidgetPicker()
        }

        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        act.fragmentKeyHandler = object : com.github.gezimos.inkos.MainActivity.FragmentKeyHandler {
            override fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
                try {
                    val mapped = KeyMapperHelper.mapHomeKey(prefs, keyCode, event)
                    when (mapped) {
                        KeyMapperHelper.HomeKeyAction.None -> return false
                        KeyMapperHelper.HomeKeyAction.ClickClock -> {
                            handleClockClick()
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.ClickDate -> {
                            handleDateClick()
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.ClickQuote -> {
                            handleBottomWidgetClick()
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.DoubleTap -> {
                            handleDoubleTapAction()
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.PageUp -> {
                            adjustPageBy(-1, viewModel.homeUiState.value.homePagesNum)
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.PageDown -> {
                            adjustPageBy(1, viewModel.homeUiState.value.homePagesNum)
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.OpenQuickMenu -> {
                            showQuickMenuWithAuth()
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.SwipeLeft -> {
                            GestureHelper.handleSwipeLeft(requireContext(), this@HomeFragmentCompose, viewModel, prefs)
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.SwipeRight -> {
                            GestureHelper.handleSwipeRight(requireContext(), this@HomeFragmentCompose, viewModel, prefs)
                            return true
                        }
                        KeyMapperHelper.HomeKeyAction.LongPressSelected -> {
                            try {
                                showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = true)
                            } catch (_: Exception) {}
                            return true
                        }
                        else -> return false
                    }
                } catch (_: Exception) {}
                return false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isHomeVisible = false
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        if (act.fragmentKeyHandler != null) act.fragmentKeyHandler = null
    }

    // Called by Activity when window focus is regained
    fun onWindowFocusGained() {
        viewModel.refreshClock()
    }

    private fun handleSwipeUp(totalPages: Int) {
        if (totalPages <= 0) return
        if (viewModel.homeUiState.value.currentPage > 0) {
            viewModel.previousPage()
            vibratePaging()
        }
    }

    private fun handleSwipeDown(totalPages: Int) {
        if (totalPages <= 0) return
        if (viewModel.homeUiState.value.currentPage < totalPages - 1) {
            viewModel.nextPage()
            vibratePaging()
        }
    }

    private fun adjustPageBy(delta: Int, totalPages: Int) {
        if (delta == 0) return
        if (totalPages <= 0) return
        val steps = abs(delta)
        if (delta > 0) {
            repeat(steps) { handleSwipeDown(totalPages) }
        } else {
            repeat(steps) { handleSwipeUp(totalPages) }
        }
    }

    private fun handleHomeAppClick(app: HomeAppUiState) {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showAppSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }
        
        val homeApp = prefs.getHomeAppModel(app.id)
        if (homeApp.activityPackage.isEmpty() || Constants.isSeparator(homeApp.activityPackage)) {
            return
        }


        val notifications = notificationManager.notificationInfoState.value
        val notificationInfo = notifications[homeApp.activityPackage]
        val isMedia = notificationInfo?.category == Notification.CATEGORY_TRANSPORT
        if (!isMedia) {
            notificationManager.updateBadgeNotification(homeApp.activityPackage, null)
            if (prefs.clearConversationOnAppOpen) {
                val conversations = notificationManager.conversationNotificationsState.value
                conversations[homeApp.activityPackage]?.forEach { conversation ->
                    notificationManager.removeConversationNotification(homeApp.activityPackage, conversation.conversationId)
                }
            }
        }

        launchAppMaybeBiometric(homeApp)
    }

    private fun handleHomeAppLongClick(app: HomeAppUiState) {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showAppSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }
        
        vibratePaging()
        if (prefs.homeLocked) {
            if (prefs.longPressAppInfoEnabled) {
                val model = prefs.getHomeAppModel(app.id)
                if (model.activityPackage.isNotEmpty()) {
                    openAppInfo(requireContext(), model.user, model.activityPackage)
                }
            }
            return
        }
        showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = true, n = app.id)
    }

    private fun handleClockClick() {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showClockSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }
        GestureHelper.handleClockClick(requireContext(), this, viewModel, prefs)
    }

    private fun handleDateClick() {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showDateSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }
        GestureHelper.handleDateClick(requireContext(), this, viewModel, prefs)
    }

    private fun handleBatteryClick() {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showDateSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }
        
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireContext().startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                requireContext().startActivity(fallbackIntent)
            } catch (_: Exception) {
                try {
                    val generalIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    generalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    requireContext().startActivity(generalIntent)
                } catch (_: Exception) {}
            }
        }
    }

    private fun handleEventsGrantPermissionClick() {
        calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
    }

    private fun handleBottomWidgetClick() {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showBottomWidgetSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }
        when (prefs.bottomWidgetType) {
            Constants.BottomWidgetType.Quote.value ->
                GestureHelper.handleQuoteClick(requireContext(), this, viewModel, prefs)
            Constants.BottomWidgetType.Events.value -> {
                if (!com.github.gezimos.inkos.helper.CalendarEventsHelper.hasCalendarPermission(requireContext())) {
                    handleEventsGrantPermissionClick()
                } else {
                    requireContext().launchCalendar()
                }
            }
            Constants.BottomWidgetType.AndroidWidget.value -> {
                if (prefs.androidWidgetId == -1) {
                    EditModeHelper.showWidgetPickerSheet(
                        requireContext(), this, prefs, appWidgetHelper, viewModel
                    ) {
                        viewModel.refreshHomeAppsUiState(requireContext())
                    }
                }
            }
            Constants.BottomWidgetType.Shortcuts.value -> {
                val action = viewModel.homeUiState.value.shortcutLeftAction
                val app = prefs.appShortcutLeft
                GestureHelper.executeAction(requireContext(), this, viewModel, action, app)
            }
            Constants.BottomWidgetType.TotalUsage.value -> {
                try {
                    findNavController().navigate(R.id.recentsFragment)
                } catch (_: Exception) {}
            }
            else -> {}
        }
    }

    private fun handleNotificationCountClick() {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showDateSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }

        when (prefs.notificationCountSource) {
            1 -> {
                // Letters mode: navigate to LettersFragment
                try {
                    findNavController().navigate(R.id.lettersFragment)
                } catch (_: Exception) {}
            }
            2 -> {
                // Hub mode: navigate to HubFragment
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_hubFragment)
                } catch (_: Exception) {
                    try {
                        findNavController().navigate(R.id.hubFragment)
                    } catch (_: Exception) {}
                }
            }
            else -> {
                // SimpleTray mode (default)
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_simpleTrayFragment)
                } catch (_: Exception) {
                    try {
                        findNavController().navigate(R.id.simpleTrayFragment)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun handleBackgroundClick() {
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showBackgroundSettings(requireContext(), this, prefs) {
                viewModel.refreshHomeAppsUiState(requireContext())
            }
            return
        }
    }


    fun launchWidgetPicker() {
        val oldId = prefs.androidWidgetId
        if (oldId != -1) {
            appWidgetHelper.removeWidget()
            viewModel.setAndroidWidgetId(-1)
        }

        val widgetId = appWidgetHelper.allocateWidgetId()
        pendingWidgetId = widgetId
        val intent = appWidgetHelper.buildPickerIntent(widgetId)
        try {
            widgetPickerLauncher.launch(intent)
        } catch (_: Exception) {
            appWidgetHelper.deallocateWidgetId(widgetId)
            pendingWidgetId = -1
            requireContext().showShortToast("Widget picker not available")
        }
    }

    private fun handleDoubleTapAction() {
        GestureHelper.handleDoubleTap(requireContext(), this, viewModel, prefs)
    }

    private fun handleMediaAction(action: HomeMediaAction) {
        val vibrate = { vibrateForWidget() }
        when (action) {
            HomeMediaAction.Open -> if (audioWidgetHelper.openMediaApp()) vibrate()
            HomeMediaAction.Previous -> if (audioWidgetHelper.skipToPrevious()) vibrate()
            HomeMediaAction.PlayPause -> if (audioWidgetHelper.playPauseMedia()) vibrate()
            HomeMediaAction.Next -> if (audioWidgetHelper.skipToNext()) vibrate()
            HomeMediaAction.Stop -> if (audioWidgetHelper.stopMedia()) vibrate()
        }
    }
    private fun launchAppMaybeBiometric(appListItem: com.github.gezimos.inkos.data.AppListItem) {
        val packageName = appListItem.activityPackage

        val fragment = this
        if (viewModel.isAppLocked(appListItem)) {
            biometricHelper.startBiometricAuth(appListItem, object : BiometricHelper.CallbackApp {
                override fun onAuthenticationSucceeded(appListItem: com.github.gezimos.inkos.data.AppListItem) {
                    val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
                    if (appsRepo.launchSyntheticOrSystemApp(requireContext(), packageName, fragment, appListItem.shortcutId)) {
                        return
                    }
                    viewModel.launchApp(appListItem)
                }

                override fun onAuthenticationFailed() {
                    android.util.Log.e("Authentication", getString(R.string.text_authentication_failed))
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> android.util.Log.e(
                            "Authentication",
                            getString(R.string.text_authentication_cancel)
                        )
                        else -> android.util.Log.e(
                            "Authentication",
                            getString(R.string.text_authentication_error).format(errorMessage, errorCode)
                        )
                    }
                }
            })
            return
        }

        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
        if (appsRepo.launchSyntheticOrSystemApp(requireContext(), packageName, this, appListItem.shortcutId)) {
            return
        }
        viewModel.launchApp(appListItem)
    }

    private fun showAppList(flag: AppDrawerFlag, includeHiddenApps: Boolean, n: Int = 0) {
        viewModel.getAppList(includeHiddenApps = includeHiddenApps, flag = flag)
        val bundle = bundleOf(
            "flag" to flag.toString(),
            "n" to n,
            "showSearch" to false
        )
        try {
            findNavController().navigate(R.id.appsFragment, bundle)
        } catch (_: Exception) {
            try {
                findNavController().navigate(R.id.action_mainFragment_to_appListFragment, bundle)
            } catch (_: Exception) {
                findNavController().navigate(R.id.appListFragment, bundle)
            }
        }
    }

    private fun showQuickMenu() {
        dialogManager.showQuickMenu(
            onInkOSSettings = { sendToSettingFragment() },
            onEditMode = {
                EditModeHelper.toggleEditMode()
                viewModel.refreshHomeAppsUiState(requireContext())
            },
            onEditFavorites = { navigateToEditFavorites() },
            onLookFeel = { navigateToLookFeel() },
            onAbout = { navigateToAbout() }
        )
    }

    private fun showQuickMenuWithAuth() {
        if (prefs.settingsLocked) {
            biometricHelper.startBiometricSettingsAuth(object : BiometricHelper.CallbackSettings {
                override fun onAuthenticationSucceeded() {
                    showQuickMenu()
                }
                override fun onAuthenticationFailed() {
                    android.util.Log.e("Authentication", getString(R.string.text_authentication_failed))
                }
                override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                        android.util.Log.e("Authentication", getString(R.string.text_authentication_error).format(errorMessage, errorCode))
                    }
                }
            })
        } else {
            showQuickMenu()
        }
    }

    private fun trySettings() {
        try {
            VibrationHelper.trigger(VibrationHelper.Effect.LONG_PRESS)
        } catch (_: Exception) {}
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (prefs.settingsLocked) {
                biometricHelper.startBiometricSettingsAuth(object : BiometricHelper.CallbackSettings {
                    override fun onAuthenticationSucceeded() {
                        sendToSettingFragment()
                    }

                    override fun onAuthenticationFailed() {
                        android.util.Log.e("Authentication", getString(R.string.text_authentication_failed))
                    }

                    override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> android.util.Log.e(
                                "Authentication",
                                getString(R.string.text_authentication_cancel)
                            )
                            else -> android.util.Log.e(
                                "Authentication",
                                getString(R.string.text_authentication_error).format(errorMessage, errorCode)
                            )
                        }
                    }
                })
            } else {
                sendToSettingFragment()
            }
        }
    }

    private fun sendToSettingFragment() {
        try {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        } catch (_: Exception) {
        }
    }

    private fun vibratePaging() {
        VibrationHelper.trigger(VibrationHelper.Effect.PAGE)
    }

    private fun vibrateForWidget() {
        VibrationHelper.trigger(VibrationHelper.Effect.CLICK)
    }

    private fun showLongPressToast() = requireContext().showShortToast(getString(R.string.long_press_to_select_app))
    
    // Quick Menu Actions
    private fun navigateToInkOSSettings() {
        try {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        } catch (_: Exception) {
            // Fallback to direct navigation
            try {
                findNavController().navigate(R.id.settingsFragment)
            } catch (_: Exception) {}
        }
    }
    
    private fun toggleHomeLock() {
        val isLocked = prefs.homeLocked
        viewModel.setHomeLocked(!isLocked)
        VibrationHelper.trigger(VibrationHelper.Effect.CLICK)
    }
    
    private fun openSystemSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireContext().startActivity(intent)
        } catch (_: Exception) {
            navigateToInkOSSettings()
        }
    }
    
    private fun navigateToWallpaper() {
        try {
            findNavController().navigate(R.id.wallpaperFragment)
        } catch (_: Exception) {}
    }
    
    private fun navigateToLookFeel() {
        try {
            findNavController().navigate(
                R.id.settingsFragment,
                androidx.core.os.bundleOf("initialRoute" to "LookFeel")
            )
        } catch (_: Exception) {}
    }

    private fun navigateToAbout() {
        try {
            findNavController().navigate(
                R.id.settingsFragment,
                androidx.core.os.bundleOf("initialRoute" to "Support")
            )
        } catch (_: Exception) {}
    }
    
    private fun navigateToEditFavorites() {
        showAppList(AppDrawerFlag.EditFavorites, includeHiddenApps = true)
    }

    private fun copyFontToInternalStorage(uri: android.net.Uri): java.io.File? {
        val fileName = getFileName(uri) ?: "custom_font.ttf"
        val file = java.io.File(requireContext().filesDir, fileName)
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return file
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    private fun getFileName(uri: android.net.Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use { if (it.moveToFirst()) { val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (index >= 0) name = it.getString(index) } }
        return name
    }
}
