package com.github.gezimos.inkos.ui

import android.app.Notification
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Vibrator
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.HomeAppUiState
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.KeyMapperHelper
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.receivers.BatteryReceiver
import com.github.gezimos.inkos.helper.utils.BiometricHelper
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.GestureHelper
import com.github.gezimos.inkos.ui.compose.HomeMediaAction
import com.github.gezimos.inkos.ui.compose.HomeUI
import com.github.gezimos.inkos.ui.compose.HomeUiCallbacks
import com.github.gezimos.inkos.ui.compose.NavHelper
import com.github.gezimos.inkos.ui.compose.gestureHelper
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
    private val biometricHelper: BiometricHelper by lazy { BiometricHelper(this) }
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        vibrator = try {
            requireContext().getSystemService(Vibrator::class.java)
        } catch (_: Exception) {
            null
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

                // PERFORMANCE FIX: Use ONLY consolidated render state (single collection point)
                val fullRenderState by viewModel.homeRenderState.collectAsState()
                
                // PERFORMANCE FIX: Derive theme from render state to avoid double collection
                val isDark = remember(fullRenderState.backgroundColor) {
                    // Dark theme if background is dark (luminance < 0.5)
                    val color = fullRenderState.backgroundColor
                    val r = android.graphics.Color.red(color) / 255f
                    val g = android.graphics.Color.green(color) / 255f
                    val b = android.graphics.Color.blue(color) / 255f
                    val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                    luminance < 0.5f
                }

                SettingsTheme(isDark = isDark) {
                    // Hoist pointer input (gesture) handling into the Fragment so HomeUI remains UI-only
                    val homeAppsBoundsState = remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
                    LocalDensity.current
                    // gesture helper will provide consistent swipe threshold and cooldown

                    // PERFORMANCE FIX: Memoize chunks AND appsOnPage together
                    val chunks = remember(fullRenderState.homeApps, fullRenderState.appsPerPage) {
                        fullRenderState.homeApps.chunked(fullRenderState.appsPerPage)
                    }
                    val appsOnPage = remember(chunks, fullRenderState.currentPage) {
                        chunks.getOrNull(fullRenderState.currentPage) ?: emptyList()
                    }
                    val selectedIndex = remember { androidx.compose.runtime.mutableStateOf(0) }
                    val dpadMode = remember { androidx.compose.runtime.mutableStateOf(false) }
                    val dpadActivatedAppId = remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
                    val keyPressTracker = remember { com.github.gezimos.inkos.ui.compose.KeyPressTracker() }
                    val focusRequester = remember { FocusRequester() }
                    
                    // Multi-zone focus navigation state
                    val focusZone = remember { androidx.compose.runtime.mutableStateOf(com.github.gezimos.inkos.ui.compose.FocusZone.APPS) }
                    val selectedMediaButton = remember { androidx.compose.runtime.mutableStateOf(0) }

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
                                    showClock = fullRenderState.showClock,
                                    showDate = fullRenderState.showDate,
                                    showMediaWidget = fullRenderState.showMediaWidget && fullRenderState.mediaInfo != null,
                                    showQuote = fullRenderState.showQuote && fullRenderState.quoteText.isNotBlank(),
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
                                    onQuoteClick = { handleQuoteClick() }
                                )

                                return@onPreviewKeyEvent handled

                                // Let Activity handle printable keys/backspace so input routing
                                // is centralized (MainActivity -> MainViewModel -> AppsFragment).
                                // Do not consume the event here; return false so dispatch
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
                            } catch (_: Exception) {} }
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.isConsumed) return@awaitEachGesture
                                val longPress = awaitLongPressOrCancellation(down.id)
                                if (longPress != null && !longPress.isConsumed) {
                                    trySettings()
                                    longPress.consume()
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
                            callbacks = HomeUiCallbacks(
                                onAppClick = { handleHomeAppClick(it) },
                                onAppLongClick = { handleHomeAppLongClick(it) },
                                onClockClick = { handleClockClick() },
                                onDateClick = { handleDateClick() },
                                onBatteryClick = { handleBatteryClick() },
                                onNotificationCountClick = { handleNotificationCountClick() },
                                onQuoteClick = { handleQuoteClick() },
                                onMediaAction = { action -> handleMediaAction(action) },
                                onSwipeLeft = {},
                                onSwipeRight = {},
                                onPageDelta = { delta -> adjustPageBy(delta, fullRenderState.totalPages) },
                                onRootLongPress = { /* handled by wrapper */ }
                            ),
                            onHomeAppsBoundsChanged = { homeAppsBoundsState.value = it }
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
                    // Media state is now handled automatically by AudioWidgetHelper via MediaSessionManager
                    viewModel.refreshHomeAppsUiState(requireContext())
                }
            }
        }
        // Clock ticker now runs in ViewModel, no need to start it here
    }

    override fun onStart() {
        super.onStart()
        // Register battery receiver to update charging/battery info in the UI
        batteryReceiver = BatteryReceiver { newDateText: String, newBatteryText: String, isCharging: Boolean ->
            // Update ViewModel state only (clock ticker handles UI update)
            try { viewModel.updateDateAndBatteryText(newDateText, newBatteryText, isCharging) } catch (_: Exception) {}
        }
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            requireContext().registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Trigger an immediate battery update using the sticky intent if available
        try {
            val batteryIntent = requireContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { batteryReceiver.onReceive(requireContext(), it) }
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        isHomeVisible = true
        
        // Immediately update clock when fragment resumes (e.g., after unlocking phone)
        viewModel.refreshClock()
        
        // Handle external request to go to first page (sent from Activity)
        if (goToFirstPageSignal) {
            viewModel.setCurrentPage(0)
            goToFirstPageSignal = false
        } else if (prefs.homeReset) {
            // Reset to first page if home reset preference is enabled
            viewModel.setCurrentPage(0)
        }

        // Register Activity-level fragmentKeyHandler to receive raw Android key events
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
                            handleQuoteClick()
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
                        KeyMapperHelper.HomeKeyAction.OpenSettings -> {
                            trySettings()
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
                            // Best-effort: open home app list for the selected app
                            // Fallback to showing settings if no selection tracking available
                            try {
                                // Show app list for long-press (mimics long-press behavior)
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
        // Immediately update clock when window gains focus (e.g., after unlocking phone)
        viewModel.refreshClock()
    }

    // Renamed to reflect vertical movement (swipe up -> previous page)
    private fun handleSwipeUp(totalPages: Int) {
        if (totalPages <= 0) return
        // Do not wrap-around: if already at first page, stay there
        if (viewModel.homeUiState.value.currentPage > 0) {
            viewModel.previousPage()
            vibratePaging()
        }
    }

    // Renamed to reflect vertical movement (swipe down -> next page)
    private fun handleSwipeDown(totalPages: Int) {
        if (totalPages <= 0) return
        // Do not wrap-around: if already at last page, stay there
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
        val homeApp = prefs.getHomeAppModel(app.id)
        if (homeApp.activityPackage.isEmpty()) {
            showLongPressToast()
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
        GestureHelper.handleClockClick(requireContext(), this, viewModel, prefs)
    }

    private fun handleDateClick() {
        GestureHelper.handleDateClick(requireContext(), this, viewModel, prefs)
    }

    private fun handleBatteryClick() {
        try {
            // Open Android battery information/details page (most common battery settings dialog)
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            requireContext().startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            // Fallback to battery saver settings if power usage summary not available
            try {
                val fallbackIntent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                requireContext().startActivity(fallbackIntent)
            } catch (_: Exception) {
                // If both fail, try opening general battery settings
                try {
                    val generalIntent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    generalIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    requireContext().startActivity(generalIntent)
                } catch (_: Exception) {}
            }
        }
    }

    private fun handleQuoteClick() {
        GestureHelper.handleQuoteClick(requireContext(), this, viewModel, prefs)
    }

    private fun handleNotificationCountClick() {
        try {
            findNavController().navigate(R.id.action_mainFragment_to_simpleTrayFragment)
        } catch (_: Exception) {
            // Fallback navigation if action doesn't exist
            try {
                findNavController().navigate(R.id.simpleTrayFragment)
            } catch (_: Exception) {}
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

        // Handle synthetic and system apps first
        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
        if (appsRepo.launchSyntheticOrSystemApp(requireContext(), packageName, this)) {
            return
        }

        if (viewModel.isAppLocked(packageName)) {
            // run biometric auth then launch
            biometricHelper.startBiometricAuth(appListItem, object : BiometricHelper.CallbackApp {
                override fun onAuthenticationSucceeded(appListItem: com.github.gezimos.inkos.data.AppListItem) {
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
        } else {
            viewModel.launchApp(appListItem)
        }
    }

    private fun showAppList(flag: AppDrawerFlag, includeHiddenApps: Boolean, n: Int = 0, initialSearch: String? = null) {
        viewModel.getAppList(includeHiddenApps = includeHiddenApps, flag = flag)
        val bundle = bundleOf(
            "flag" to flag.toString(),
            "n" to n,
            "showSearch" to false
        )
        if (!initialSearch.isNullOrEmpty()) {
            bundle.putString("initialSearch", initialSearch)
        }
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

    private fun trySettings() {
        try {
            // Use a stronger haptic feedback for long-press->settings
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
}
