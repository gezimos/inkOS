package com.github.gezimos.inkos.ui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.text.format.DateFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.common.hideKeyboard
import com.github.gezimos.common.launchCalendar
import com.github.gezimos.common.openAlarmApp
import com.github.gezimos.common.openCameraApp
import com.github.gezimos.common.openDialerApp
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.HomeAppUiState
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.databinding.FragmentHomeBinding
import com.github.gezimos.inkos.helper.hideStatusBar
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.receivers.BatteryReceiver
import com.github.gezimos.inkos.helper.showStatusBar
import com.github.gezimos.inkos.helper.utils.AppReloader
import com.github.gezimos.inkos.helper.utils.BiometricHelper
import com.github.gezimos.inkos.helper.utils.NotificationBadgeUtil
import com.github.gezimos.inkos.helper.utils.PrivateSpaceManager
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.listener.OnSwipeTouchListener
import com.github.gezimos.inkos.listener.ViewSwipeTouchListener
import com.github.gezimos.inkos.services.NotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.github.gezimos.inkos.helper.KeyMapperHelper
import android.view.KeyEvent
import com.github.gezimos.inkos.helper.utils.EinkRefreshHelper
import com.github.gezimos.inkos.helper.utils.BackgroundImageHelper

class HomeFragment : Fragment(), View.OnClickListener, View.OnLongClickListener {

    companion object {
        @JvmStatic
        var isHomeVisible: Boolean = false

        @JvmStatic
        fun sendGoToFirstPageSignal() {
            // This will be set by MainActivity to trigger going to first page
            goToFirstPageSignal = true
        }

        @JvmStatic
        var goToFirstPageSignal: Boolean = false
    }

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var vibrator: Vibrator

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedAppIndex = 0

    // Add a BroadcastReceiver for user present (unlock)
    private var userPresentReceiver: android.content.BroadcastReceiver? = null

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val view = binding.root
        prefs = Prefs(requireContext())
        batteryReceiver = BatteryReceiver()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        biometricHelper = BiometricHelper(this)

        // Always hide keyboard and clear focus when entering HomeFragment
        hideKeyboard()
        requireActivity().window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.isinkosDefault()

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        @Suppress("DEPRECATION")
        vibrator = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator

        // Validate background image URI permissions before attempting to load
        BackgroundImageHelper.validateBackgroundImageUri(requireContext(), prefs)
        setupBackgroundImage()

        initObservers()
        initClickListeners()
        initSwipeTouchListener()

        // Initialize date display
        updateDateDisplay()

        // Observe home app UI state and update UI accordingly
        viewModel.homeAppsUiState.observe(viewLifecycleOwner) { homeAppsUiState ->
            updateHomeAppsUi(homeAppsUiState)
        }

        // Add observer for notification info and refresh UI state
        NotificationManager.getInstance(requireContext()).notificationInfoLiveData.observe(
            viewLifecycleOwner
        ) { notifications ->
            Log.d("HomeFragment", "notificationInfoLiveData updated: $notifications")
            // Always refresh the ViewModel state first
            viewModel.refreshHomeAppsUiState(requireContext())
            updateHomeAppsUi(viewModel.homeAppsUiState.value ?: emptyList())
            // --- Media playback notification observer logic ---
            val mediaNotification = notifications.values.firstOrNull {
                it.category == android.app.Notification.CATEGORY_TRANSPORT
            }
            viewModel.updateMediaPlaybackInfo(mediaNotification)
        }

        // Add observer for media player widget
        AudioWidgetHelper.getInstance(requireContext()).mediaPlayerLiveData.observe(
            viewLifecycleOwner
        ) { mediaPlayer ->
            updateMediaPlayerWidget(mediaPlayer)
        }

        // Immediately update widget state in case there's already an active player
        val currentMediaPlayer =
            AudioWidgetHelper.getInstance(requireContext()).getCurrentMediaPlayer()
        updateMediaPlayerWidget(currentMediaPlayer)

        // Set up media player click listeners once
        setupMediaPlayerClickListeners()

        // Centralized key handling via KeyMapperHelper
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener { _, keyCode, event ->
            val action = KeyMapperHelper.mapHomeKey(prefs, keyCode, event)
            when (action) {
                is KeyMapperHelper.HomeKeyAction.MoveSelectionDown -> {
                    moveSelectionDown()
                    true
                }
                is KeyMapperHelper.HomeKeyAction.PageUp -> {
                    // Let Activity handle PageUp/PageDown (volume keys) centrally. Return false so Activity can forward.
                    return@setOnKeyListener false
                }
                is KeyMapperHelper.HomeKeyAction.PageDown -> {
                    // Let Activity handle PageUp/PageDown (volume keys) centrally. Return false so Activity can forward.
                    return@setOnKeyListener false
                }
                is KeyMapperHelper.HomeKeyAction.GestureLeft -> {
                    when (val act = action.action) {
                        Action.OpenApp -> openSwipeLeftApp()
                        else -> handleOtherAction(act)
                    }
                    CrashHandler.logUserAction("DPAD_RIGHT Gesture (SwipeLeft)")
                    true
                }
                is KeyMapperHelper.HomeKeyAction.GestureRight -> {
                    when (val act = action.action) {
                        Action.OpenApp -> openSwipeRightApp()
                        else -> handleOtherAction(act)
                    }
                    CrashHandler.logUserAction("DPAD_LEFT Gesture (SwipeRight)")
                    true
                }
                is KeyMapperHelper.HomeKeyAction.LongPressSelected -> {
                    val view = binding.homeAppsLayout.getChildAt(selectedAppIndex)
                    if (view != null) onLongClick(view) else false
                }
                is KeyMapperHelper.HomeKeyAction.ClickClock -> {
                    when (val action = prefs.clickClockAction) {
                        Action.OpenApp -> openClickClockApp()
                        Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                        else -> handleOtherAction(action)
                    }
                    CrashHandler.logUserAction("Clock Clicked (key)")
                    true
                }
                is KeyMapperHelper.HomeKeyAction.ClickQuote -> {
                    when (val action = prefs.quoteAction) {
                        Action.OpenApp -> {
                            if (prefs.appQuoteWidget.activityPackage.isNotEmpty()) {
                                viewModel.launchApp(prefs.appQuoteWidget, this)
                            } else {
                                showLongPressToast()
                            }
                        }
                        Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                        else -> handleOtherAction(action)
                    }
                    CrashHandler.logUserAction("Quote Clicked (key)")
                    true
                }
                is KeyMapperHelper.HomeKeyAction.ClickDate -> {
                    when (val action = prefs.clickDateAction) {
                        Action.OpenApp -> openClickDateApp()
                        Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                        else -> handleOtherAction(action)
                    }
                    CrashHandler.logUserAction("Date Clicked (key)")
                    true
                }
                is KeyMapperHelper.HomeKeyAction.DoubleTap -> {
                    when (val action = prefs.doubleTapAction) {
                        Action.OpenApp -> openDoubleTapApp()
                        else -> handleOtherAction(action)
                    }
                    CrashHandler.logUserAction("DoubleTap (key)")
                    true
                }
                is KeyMapperHelper.HomeKeyAction.OpenSettings -> {
                    trySettings()
                    true
                }
                else -> false
            }
        }
    }

    // ...existing code...

    private fun updateHomeAppsUi(homeAppsUiState: List<HomeAppUiState>) {
        val notifications =
            NotificationManager.getInstance(requireContext()).notificationInfoLiveData.value
                ?: emptyMap()
        homeAppsUiState.forEach { uiState ->
            val view = binding.homeAppsLayout.findViewWithTag<TextView>(uiState.activityPackage)
            if (view != null) {
                NotificationBadgeUtil.updateNotificationForView(
                    requireContext(),
                    prefs,
                    view,
                    notifications
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Always hide keyboard and clear focus when resuming HomeFragment
        hideKeyboard()
        requireActivity().window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        isHomeVisible = true

        // Disable transition animations for specific elements before updates
        binding.quote.clearAnimation()
        binding.homeScreenPager.clearAnimation()

    // Eink refresh: flash overlay if enabled
    EinkRefreshHelper.refreshEink(requireContext(), prefs, binding.root as? ViewGroup, prefs.einkRefreshDelay)
        // Centralized reset logic for home button
        if (prefs.homeReset || goToFirstPageSignal) {
            currentPage = 0
            selectedAppIndex = 0
            goToFirstPageSignal = false
            updateAppsVisibility(prefs.homePagesNum)
            focusAppButton(selectedAppIndex)
        } else {
            updateAppsVisibility(prefs.homePagesNum)
            focusAppButton(selectedAppIndex)
        }

        // Ensure fragment root has focus so volume keys are handled by the fragment
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()

        // Register Activity-level page navigation handler so volume keys are forwarded
        val act = activity as? com.github.gezimos.inkos.MainActivity
        act?.pageNavigationHandler = object : com.github.gezimos.inkos.MainActivity.PageNavigationHandler {
            override val handleDpadAsPage: Boolean = false

            override fun pageUp() {
                val totalPages = prefs.homePagesNum
                handleSwipeLeft(totalPages)
                vibratePaging()
            }

            override fun pageDown() {
                val totalPages = prefs.homePagesNum
                handleSwipeRight(totalPages)
                vibratePaging()
            }
        }

        // Also register a fragment key handler so keypad shortcuts (2,6,7,8 etc.) keep working
        act?.fragmentKeyHandler = object : com.github.gezimos.inkos.MainActivity.FragmentKeyHandler {
            override fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
                val action = KeyMapperHelper.mapHomeKey(prefs, keyCode, event)
                // only handle ACTION_DOWN mapping already applied in KeyMapperHelper
                when (action) {
                    is KeyMapperHelper.HomeKeyAction.MoveSelectionDown -> {
                        moveSelectionDown()
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.PageUp -> {
                        // volume/page keys routed to pageNavigationHandler; ignore here
                        return false
                    }
                    is KeyMapperHelper.HomeKeyAction.PageDown -> {
                        return false
                    }
                    is KeyMapperHelper.HomeKeyAction.GestureLeft -> {
                        when (val act = action.action) {
                            Action.OpenApp -> openSwipeLeftApp()
                            else -> handleOtherAction(act)
                        }
                        CrashHandler.logUserAction("DPAD_RIGHT Gesture (SwipeLeft)")
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.GestureRight -> {
                        when (val act = action.action) {
                            Action.OpenApp -> openSwipeRightApp()
                            else -> handleOtherAction(act)
                        }
                        CrashHandler.logUserAction("DPAD_LEFT Gesture (SwipeRight)")
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.LongPressSelected -> {
                        val view = binding.homeAppsLayout.getChildAt(selectedAppIndex)
                        if (view != null) onLongClick(view) else false
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.ClickClock -> {
                        when (val action = prefs.clickClockAction) {
                            Action.OpenApp -> openClickClockApp()
                            Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                            else -> handleOtherAction(action)
                        }
                        CrashHandler.logUserAction("Clock Clicked (key)")
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.ClickQuote -> {
                        when (val action = prefs.quoteAction) {
                            Action.OpenApp -> {
                                if (prefs.appQuoteWidget.activityPackage.isNotEmpty()) {
                                    viewModel.launchApp(prefs.appQuoteWidget, this@HomeFragment)
                                } else {
                                    showLongPressToast()
                                }
                            }
                            Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                            else -> handleOtherAction(action)
                        }
                        CrashHandler.logUserAction("Quote Clicked (key)")
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.ClickDate -> {
                        when (val action = prefs.clickDateAction) {
                            Action.OpenApp -> openClickDateApp()
                            Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                            else -> handleOtherAction(action)
                        }
                        CrashHandler.logUserAction("Date Clicked (key)")
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.DoubleTap -> {
                        when (val action = prefs.doubleTapAction) {
                            Action.OpenApp -> openDoubleTapApp()
                            else -> handleOtherAction(action)
                        }
                        CrashHandler.logUserAction("DoubleTap (key)")
                        return true
                    }
                    is KeyMapperHelper.HomeKeyAction.OpenSettings -> {
                        trySettings()
                        return true
                    }
                    else -> return false
                }
            }
        }

        // Refresh home app UI state on resume
        viewModel.refreshHomeAppsUiState(requireContext())
        
        // Re-validate background image URI in case permissions were restored
        BackgroundImageHelper.validateBackgroundImageUri(requireContext(), prefs)

        // Re-apply notification badges to all home app views
        val notifications = NotificationManager.getInstance(requireContext()).getBadgeNotifications()
        binding.homeAppsLayout.children.forEach { view ->
            if (view is TextView) {
                NotificationBadgeUtil.updateNotificationForView(requireContext(), prefs, view, notifications)
            }
        }

        // Force refresh audio widget state on resume to ensure proper sync
        val audioWidgetHelper = AudioWidgetHelper.getInstance(requireContext())
        audioWidgetHelper.forceRefreshState()
        val currentMediaPlayer = audioWidgetHelper.getCurrentMediaPlayer()
        updateMediaPlayerWidget(currentMediaPlayer)
    }

    override fun onPause() {
        super.onPause()
        isHomeVisible = false
    val act = activity as? com.github.gezimos.inkos.MainActivity
    if (act?.pageNavigationHandler != null) act.pageNavigationHandler = null
    if (act?.fragmentKeyHandler != null) act.fragmentKeyHandler = null
    }

    private fun moveSelectionDown() {
        val totalApps = getTotalAppsCount()
        val totalPages = prefs.homePagesNum
        val appsPerPage = if (totalPages > 0) (totalApps + totalPages - 1) / totalPages else 0
        totalApps - 1

        currentPage * appsPerPage
        val endIdx = minOf((currentPage + 1) * appsPerPage, totalApps) - 1

        if (selectedAppIndex < endIdx) {
            // Move to next app in current page
            selectedAppIndex++
        } else {
            // At last app of current page
            if (currentPage < totalPages - 1) {
                // Move to first app of next page
                currentPage++
                selectedAppIndex = currentPage * appsPerPage
            } else {
                // Wrap to first app of first page
                currentPage = 0
                selectedAppIndex = 0
            }
        }
        updateAppsVisibility(totalPages)
        focusAppButton(selectedAppIndex)
    }

    private fun focusAppButton(index: Int) {
        val view = binding.homeAppsLayout.getChildAt(index)
        view?.requestFocus()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStart() {
        super.onStart()
        if (prefs.showStatusBar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())

        batteryReceiver = BatteryReceiver()
        /* register battery changes */
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            requireContext().registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Register user present receiver to refresh on unlock
        if (userPresentReceiver == null) {
            userPresentReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == Intent.ACTION_USER_PRESENT) {
                        // Trigger the same refresh as onResume
                        if (isAdded) {
                            // Eink refresh: flash overlay if enabled
                            if (prefs.einkRefreshEnabled) {
                                val isDark = when (prefs.appTheme) {
                                    com.github.gezimos.inkos.data.Constants.Theme.Light -> false
                                    com.github.gezimos.inkos.data.Constants.Theme.Dark -> true
                                    com.github.gezimos.inkos.data.Constants.Theme.System -> com.github.gezimos.inkos.helper.isSystemInDarkMode(
                                        requireContext()
                                    )
                                }
                                val overlayColor =
                                    if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
                                val overlay = View(requireContext())
                                overlay.setBackgroundColor(overlayColor)
                                overlay.layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                (binding.root as ViewGroup).addView(overlay)
                                overlay.bringToFront()
                                overlay.postDelayed({
                                    (binding.root as ViewGroup).removeView(overlay)
                                }, 120)
                            }
                            // Centralized reset logic for home button
                            if (prefs.homeReset || goToFirstPageSignal) {
                                currentPage = 0
                                selectedAppIndex = 0
                                goToFirstPageSignal = false
                                updateAppsVisibility(prefs.homePagesNum)
                                focusAppButton(selectedAppIndex)
                            } else {
                                updateAppsVisibility(prefs.homePagesNum)
                                focusAppButton(selectedAppIndex)
                            }
                            // Refresh home app UI state
                            viewModel.refreshHomeAppsUiState(requireContext())
                            // Re-apply notification badges to all home app views
                            val notifications =
                                NotificationManager.getInstance(
                                    requireContext()
                                ).getBadgeNotifications()
                            binding.homeAppsLayout.children.forEach { view ->
                                if (view is TextView) {
                                    NotificationBadgeUtil.updateNotificationForView(
                                        requireContext(),
                                        prefs,
                                        view,
                                        notifications
                                    )
                                }
                            }
                            // Force refresh audio widget state after unlock
                            val audioWidgetHelper = AudioWidgetHelper.getInstance(requireContext())
                            audioWidgetHelper.forceRefreshState()
                            val currentMediaPlayer = audioWidgetHelper.getCurrentMediaPlayer()
                            updateMediaPlayerWidget(currentMediaPlayer)
                        }
                    }
                }
            }
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            requireContext().registerReceiver(userPresentReceiver, filter)
        }

        binding.apply {
            val is24HourFormat = DateFormat.is24HourFormat(requireContext())
            // Use a fixed pattern to guarantee no AM/PM
            val timePattern = if (is24HourFormat) "HH:mm" else "hh:mm"
            clock.format12Hour = timePattern
            clock.format24Hour = timePattern

            // Set top margin of clock based on prefs.topWidgetMargin (dp to px)
            val layoutParams = clock.layoutParams as? LinearLayout.LayoutParams
            if (layoutParams != null) {
                val density = requireContext().resources.displayMetrics.density
                layoutParams.topMargin = (prefs.topWidgetMargin * density).toInt()
                clock.layoutParams = layoutParams
            }
            
            // Set bottom margin of bottom widgets wrapper
            applyBottomWidgetMargin()
            
            // Battery widget setup removed
            binding.quote.textSize = prefs.quoteSize.toFloat()
            binding.quote.visibility = if (prefs.showQuote) View.VISIBLE else View.GONE
            mainLayout.setBackgroundColor(prefs.backgroundColor)
            clock.setTextColor(prefs.clockColor)
            binding.quote.setTextColor(prefs.quoteColor)
            binding.quote.text = prefs.quoteText
            binding.quote.typeface = prefs.getFontForContext("quote")
                .getFont(requireContext(), prefs.getCustomFontPathForContext("quote"))
            root.findViewById<TextView>(R.id.date)?.setTextColor(prefs.dateColor)
            applyAudioWidgetColor(prefs.audioWidgetColor)

            homeAppsLayout.children.forEach { view ->
                if (view is TextView) {
                    view.setTextColor(prefs.appColor)
                    view.typeface = prefs.getFontForContext("apps")
                        .getFont(requireContext(), prefs.getCustomFontPathForContext("apps"))
                }
            }
            clock.typeface = prefs.getFontForContext("clock")
                .getFont(requireContext(), prefs.getCustomFontPathForContext("clock"))


        }

        binding.homeAppsLayout.children.forEach { view ->
            if (view is TextView) {
                val appModel = prefs.getHomeAppModel(view.id)
                val customLabel = prefs.getAppAlias("app_alias_${appModel.activityPackage}")
                val displayText =
                    if (customLabel.isNotEmpty()) customLabel else appModel.activityLabel
                // Apply small caps transformation if enabled (without modifying the original data)
                view.text = when {
                    prefs.allCapsApps -> displayText.uppercase()
                    prefs.smallCapsApps -> displayText.lowercase()
                    else -> displayText
                }
                view.typeface = prefs.getFontForContext("apps")
                    .getFont(requireContext(), prefs.getCustomFontPathForContext("apps"))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            /* unregister battery changes if the receiver is registered */
            requireContext().unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Unregister user present receiver
        if (userPresentReceiver != null) {
            try {
                requireContext().unregisterReceiver(userPresentReceiver)
            } catch (_: Exception) {
            }
            userPresentReceiver = null
        }
    }

    override fun onClick(view: View) {

        when (view.id) {
            R.id.clock -> {
                when (val action = prefs.clickClockAction) {
                    Action.OpenApp -> openClickClockApp()
                    Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("Clock Clicked")
            }

            R.id.quote -> {
                when (val action = prefs.quoteAction) {
                    Action.OpenApp -> {
                        if (prefs.appQuoteWidget.activityPackage.isNotEmpty()) {
                            viewModel.launchApp(prefs.appQuoteWidget, this)
                        } else {
                            showLongPressToast()
                        }
                    }
                    Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("Quote Clicked")
            }

            R.id.date -> {
                when (val action = prefs.clickDateAction) {
                    Action.OpenApp -> openClickDateApp()
                    Action.Disabled -> showShortToast(getString(R.string.edit_gestures_settings_toast))
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("Date Clicked")
            }

            // Battery widget click removed

            else -> {
                try { // Launch app
                    val appLocation = view.id
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openClickDateApp() {
        // Use appClickDate, not appClickClock
        if (prefs.appClickDate.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickDate, this)
        else
            requireContext().launchCalendar()
    }

    override fun onLongClick(view: View): Boolean {
        if (prefs.homeLocked) {
            if (prefs.longPressAppInfoEnabled) {
                // Open app info for the long-pressed app
                val n = view.id
                val appModel = prefs.getHomeAppModel(n)
                if (appModel.activityPackage.isNotEmpty()) {
                    openAppInfo(requireContext(), appModel.user, appModel.activityPackage)
                    CrashHandler.logUserAction("Show App Info")
                }
                return true
            }
            return true
        }
        val n = view.id
        showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = true, n)
        CrashHandler.logUserAction("Show App List")
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSwipeTouchListener() {
        binding.touchArea.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                when (val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> {
                        requireActivity().runOnUiThread {
                            handleOtherAction(action)
                        }
                    }
                }
                CrashHandler.logUserAction("SwipeLeft Gesture")
            }

            override fun onSwipeRight() {
                when (val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> {
                        requireActivity().runOnUiThread {
                            handleOtherAction(action)
                        }
                    }
                }
                CrashHandler.logUserAction("SwipeRight Gesture")
            }

            override fun onSwipeUp() {
                // Hardcoded: always go to next page
                handleSwipeRight(prefs.homePagesNum)
                CrashHandler.logUserAction("SwipeUp Gesture (PageNext)")
            }

            override fun onSwipeDown() {
                // Hardcoded: always go to previous page
                handleSwipeLeft(prefs.homePagesNum)
                CrashHandler.logUserAction("SwipeDown Gesture (PagePrevious)")
            }

            override fun onLongClick() {
                CrashHandler.logUserAction("Launcher Settings Opened")
                trySettings()
            }

            override fun onDoubleClick() {
                when (val action = prefs.doubleTapAction) {
                    Action.OpenApp -> openDoubleTapApp()
                    else -> {
                        requireActivity().runOnUiThread {
                            handleOtherAction(action)
                        }
                    }
                }
                CrashHandler.logUserAction("DoubleClick Gesture")
            }

            override fun onLongSwipe(direction: String) {
                when (direction) {
                    "up" -> handleSwipeRight(prefs.homePagesNum)
                    "down" -> handleSwipeLeft(prefs.homePagesNum)
                    "left" -> when (val action = prefs.swipeLeftAction) {
                        Action.OpenApp -> openSwipeLeftApp()
                        else -> handleOtherAction(action)
                    }

                    "right" -> when (val action = prefs.swipeRightAction) {
                        Action.OpenApp -> openSwipeRightApp()
                        else -> handleOtherAction(action)
                    }
                }
                CrashHandler.logUserAction("LongSwipe_${direction} Gesture")
            }
        })
    }

    private fun initClickListeners() {
        binding.apply {
            clock.setOnClickListener(this@HomeFragment)
            quote.setOnClickListener(this@HomeFragment)
        }
        
        // Set up date click listener (date view is found dynamically as it's not in binding)
        val dateView = binding.root.findViewById<TextView>(R.id.date)
        dateView?.setOnClickListener(this@HomeFragment)
    }

    private fun initObservers() {
        binding.apply {
            // Remove firstRunTips logic
            // if (prefs.firstSettingsOpen) firstRunTips.visibility = View.VISIBLE
            // else firstRunTips.visibility = View.GONE

            clock.gravity = Gravity.CENTER
            homeAppsLayout.gravity = Gravity.CENTER
            clock.layoutParams = (clock.layoutParams as LinearLayout.LayoutParams).apply {
                gravity = Gravity.CENTER
            }
            homeAppsLayout.children.forEach { view ->
                (view as? TextView)?.gravity = Gravity.CENTER
            }
        }

        with(viewModel) {
            homeAppsNum.observe(viewLifecycleOwner) {
                updateAppCount(it)
            }
            showClock.observe(viewLifecycleOwner) {
                binding.clock.visibility = if (it) View.VISIBLE else View.GONE
            }
            showDate.observe(viewLifecycleOwner) {
                // Refresh date display when showDate changes
                updateDateDisplay()
            }
            showDateBatteryCombo.observe(viewLifecycleOwner) {
                // Refresh date display when showDateBatteryCombo changes
                updateDateDisplay()
            }
            // --- LiveData observers for all preferences ---
            appTheme.observe(viewLifecycleOwner) {
                // Optionally, trigger theme change if needed
            }
            appColor.observe(viewLifecycleOwner) { color ->
                binding.homeAppsLayout.children.forEach { view ->
                    if (view is TextView) {
                        view.setTextColor(color)
                        view.setHintTextColor(color)
                    }
                }
                // Refresh page indicators to apply new color
                updateAppsVisibility(prefs.homePagesNum)
            }
            backgroundColor.observe(viewLifecycleOwner) { color ->
                binding.mainLayout.setBackgroundColor(color)
            }
            clockColor.observe(viewLifecycleOwner) { color ->
                binding.clock.setTextColor(color)
            }
            // batteryColor observer removed
            dateColor.observe(viewLifecycleOwner) { color ->
                binding.root.findViewById<TextView>(R.id.date)?.setTextColor(color)
            }
            quoteColor.distinctUntilChanged().observe(viewLifecycleOwner) { color ->
                binding.quote.setTextColor(color)
            }
            audioWidgetColor.observe(viewLifecycleOwner) { color ->
                applyAudioWidgetColor(color)
            }
            showAudioWidget.distinctUntilChanged().observe(viewLifecycleOwner) { show ->
                // Force refresh the audio widget state when toggled
                val audioWidgetHelper = AudioWidgetHelper.getInstance(requireContext())
                if (show) {
                    // When enabling the widget, force a refresh to sync with current media state
                    audioWidgetHelper.resetDismissalState()
                    // Trigger immediate state refresh from NotificationService
                    val currentMediaPlayer = audioWidgetHelper.getCurrentMediaPlayer()
                    updateMediaPlayerWidget(currentMediaPlayer)
                } else {
                    // When disabling the widget, hide it immediately
                    binding.mediaPlayerWidget.isVisible = false
                }
            }
            showQuote.distinctUntilChanged().observe(viewLifecycleOwner) { show ->
                binding.quote.isVisible = show
            }
            quoteText.distinctUntilChanged().observe(viewLifecycleOwner) { text ->
                binding.quote.text = text
            }
            appsFont.observe(viewLifecycleOwner) { font ->
                binding.homeAppsLayout.children.forEach { view ->
                    if (view is TextView) {
                        view.typeface = prefs.getFontForContext("apps")
                            .getFont(requireContext(), prefs.getCustomFontPathForContext("apps"))
                    }
                }
            }
            clockFont.observe(viewLifecycleOwner) { font ->
                binding.clock.typeface = prefs.getFontForContext("clock")
                    .getFont(requireContext(), prefs.getCustomFontPathForContext("clock"))
            }
            // batteryFont observer removed
            quoteFont.distinctUntilChanged().observe(viewLifecycleOwner) { font ->
                binding.quote.typeface = prefs.getFontForContext("quote")
                    .getFont(requireContext(), prefs.getCustomFontPathForContext("quote"))
            }
            textPaddingSize.observe(viewLifecycleOwner) { padding ->
                binding.homeAppsLayout.children.forEach { view ->
                    if (view is TextView) {
                        view.setPadding(0, padding, 0, padding)
                    }
                }
            }
            appSize.observe(viewLifecycleOwner) { size ->
                binding.homeAppsLayout.children.forEach { view ->
                    if (view is TextView) {
                        view.textSize = size.toFloat()
                    }
                }
            }
            clockSize.observe(viewLifecycleOwner) { size ->
                binding.clock.textSize = size.toFloat()
            }
            // batterySize observer removed
            quoteSize.distinctUntilChanged().observe(viewLifecycleOwner) { size ->
                binding.quote.textSize = size.toFloat()
            }
            homeBackgroundImageOpacity.distinctUntilChanged()
                .observe(viewLifecycleOwner) { opacity ->
                    Log.d("HomeFragment", "Opacity changed to: $opacity")
                    // Apply opacity directly to existing background image if it exists
                    val rootLayout = binding.root as ViewGroup
                    val backgroundImageView =
                        rootLayout.findViewWithTag<ImageView>("home_background")
                    if (backgroundImageView != null) {
                        val opacityFloat = opacity / 100f
                        Log.d(
                            "HomeFragment",
                            "Applying opacity $opacityFloat to existing image view"
                        )
                        // Set alpha directly without triggering image reload
                        backgroundImageView.alpha = opacityFloat
                        // Use post to ensure the alpha is applied after any pending UI updates
                        backgroundImageView.post {
                            backgroundImageView.alpha = opacityFloat
                            Log.d("HomeFragment", "Post-delayed opacity confirmed: $opacityFloat")
                        }
                    } else if (!prefs.homeBackgroundImageUri.isNullOrEmpty()) {
                        Log.d(
                            "HomeFragment",
                            "No background image view found but URI exists, calling setupBackgroundImage()"
                        )
                        // Only call setupBackgroundImage if there's actually a URI to load
                        setupBackgroundImage()
                    }
                }
            homeBackgroundImageUri.distinctUntilChanged().observe(viewLifecycleOwner) { uri ->
                Log.d("HomeFragment", "URI changed to: $uri")
                // Only refresh background image when URI actually changes
                setupBackgroundImage()
            }

        }
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.getAppName(location).isEmpty()) showLongPressToast()
        else {
            val homeApp = prefs.getHomeAppModel(location)
            val packageName = homeApp.activityPackage

            // Handle empty space synthetic app - do nothing
            if (packageName == "com.inkos.internal.empty_space") {
                return
            }

            val notificationManager = NotificationManager.getInstance(requireContext())
            val notifications = notificationManager.notificationInfoLiveData.value ?: emptyMap()
            val notificationInfo = notifications[packageName]
            
            // Fixed: Clear badge notification when app is opened (except for media that should persist while playing)
            // User interaction with the app means they've seen the notification content
            val isMediaPlayback = notificationInfo?.category == android.app.Notification.CATEGORY_TRANSPORT
            if (!isMediaPlayback) {
                // Clear the badge when opening the app - user has seen/interacted with the app
                notificationManager.updateBadgeNotification(packageName, null)
                
                // Optionally clear conversation notifications if user preference is enabled
                if (prefs.clearConversationOnAppOpen) {
                    // Clear all conversation notifications for this package
                    val conversations = notificationManager.conversationNotificationsLiveData.value ?: emptyMap()
                    conversations[packageName]?.forEach { conversation ->
                        notificationManager.removeConversationNotification(packageName, conversation.conversationId)
                    }
                }
            }
            
            viewModel.launchApp(homeApp, this)
        }
    }

    private fun showAppList(flag: AppDrawerFlag, includeHiddenApps: Boolean = false, n: Int = 0) {
        viewModel.getAppList(includeHiddenApps, flag)
        try {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(
                    R.id.action_mainFragment_to_appListFragment,
                    bundleOf("flag" to flag.toString(), "n" to n)
                )
            }
        } catch (e: Exception) {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                findNavController().navigate(
                    R.id.appListFragment,
                    bundleOf("flag" to flag.toString())
                )
            }
            e.printStackTrace()
        }
    }

    private fun openSwipeLeftApp() {
        if (prefs.appSwipeLeft.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appSwipeLeft, this)
        else
            requireContext().openCameraApp()
    }

    private fun openSwipeRightApp() {
        if (prefs.appSwipeRight.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appSwipeRight, this)
        else
            requireContext().openDialerApp()
    }

    private fun openClickClockApp() {
        if (prefs.appClickClock.activityPackage.isNotEmpty())
            viewModel.launchApp(prefs.appClickClock, this)
        else
            requireContext().openAlarmApp()
    }

    private fun openDoubleTapApp() {
        when (prefs.doubleTapAction) {
            Action.OpenApp -> {
                if (prefs.appDoubleTap.activityPackage.isNotEmpty())
                    viewModel.launchApp(prefs.appDoubleTap, this)
                else
                    AppReloader.restartApp(requireContext())
            }
            Action.OpenNotificationsScreen -> {
                requireActivity().runOnUiThread {
                    findNavController().navigate(R.id.action_mainFragment_to_notificationsFragment)
                }
            }
            Action.RestartApp -> AppReloader.restartApp(requireContext())
            Action.TogglePrivateSpace -> handleOtherAction(Action.TogglePrivateSpace)
            // NextPage/PreviousPage removed; no-op for legacy values
            Action.OpenAppDrawer -> showAppList(AppDrawerFlag.SetHomeApp, includeHiddenApps = false)
            Action.EinkRefresh -> {
                        EinkRefreshHelper.refreshEinkForced(requireContext(), prefs, binding.root as? ViewGroup, prefs.einkRefreshDelay)
            }
            Action.ExitLauncher -> exitLauncher()
            Action.LockScreen -> handleOtherAction(Action.LockScreen)
            Action.ShowRecents -> handleOtherAction(Action.ShowRecents)
            Action.OpenQuickSettings -> handleOtherAction(Action.OpenQuickSettings)
            Action.OpenPowerDialog -> handleOtherAction(Action.OpenPowerDialog)
            Action.Brightness -> handleOtherAction(Action.Brightness)
            Action.Disabled -> {}
        }
    }

    @SuppressLint("NewApi")
    private fun handleOtherAction(action: Action) {
        when (action) {
            Action.TogglePrivateSpace -> PrivateSpaceManager(requireContext()).togglePrivateSpaceLock(
                showToast = true,
                launchSettings = true
            )
            Action.OpenApp -> {} // this should be handled in the respective onSwipe[Up,Down,Right,Left] functions
            // NextPage/PreviousPage removed — paging is handled by swipe gestures only
            Action.RestartApp -> AppReloader.restartApp(requireContext())
            // OpenNotificationsScreenAlt removed; merged with OpenNotificationsScreen
            Action.OpenNotificationsScreen -> {
                requireActivity().runOnUiThread {
                    findNavController().navigate(R.id.action_mainFragment_to_notificationsFragment)
                }
            }
            Action.OpenAppDrawer -> showAppList(AppDrawerFlag.LaunchApp, includeHiddenApps = false)
            Action.EinkRefresh -> {
                EinkRefreshHelper.refreshEinkForced(requireContext(), prefs, binding.root as? ViewGroup, prefs.einkRefreshDelay)
            }
            Action.ExitLauncher -> exitLauncher()
            Action.LockScreen -> {
                com.github.gezimos.inkos.helper.initActionService(requireContext())?.lockScreen()
            }
            Action.ShowRecents -> {
                com.github.gezimos.inkos.helper.initActionService(requireContext())?.showRecents()
            }
            Action.OpenQuickSettings -> {
                com.github.gezimos.inkos.helper.initActionService(requireContext())
                    ?.openQuickSettings()
            }
            Action.OpenPowerDialog -> {
                com.github.gezimos.inkos.helper.initActionService(requireContext())
                    ?.openPowerDialog()
            }
            Action.Brightness -> {
                com.github.gezimos.inkos.helper.utils.BrightnessHelper.toggleBrightness(requireContext(), prefs, requireActivity().window)
            }
            Action.Disabled -> {}
        }
    }

    private fun showLongPressToast() = showShortToast(getString(R.string.long_press_to_select_app))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getHomeAppsGestureListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onLongClick(view: View) {
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                textOnClick(view)
            }

            override fun onSwipeLeft() {
                when (val action = prefs.swipeLeftAction) {
                    Action.OpenApp -> openSwipeLeftApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeLeft Gesture")
            }

            override fun onSwipeRight() {
                when (val action = prefs.swipeRightAction) {
                    Action.OpenApp -> openSwipeRightApp()
                    else -> handleOtherAction(action)
                }
                CrashHandler.logUserAction("SwipeRight Gesture")
            }

            override fun onSwipeUp() {
                // Hardcoded: always go to next page
                handleSwipeRight(prefs.homePagesNum)
                CrashHandler.logUserAction("SwipeUp Gesture (NextPage)")
            }

            override fun onSwipeDown() {
                // Hardcoded: always go to previous page
                handleSwipeLeft(prefs.homePagesNum)
                CrashHandler.logUserAction("SwipeDown Gesture (PreviousPage)")
            }

            override fun onLongSwipe(direction: String) {
                when (direction) {
                    "up" -> handleSwipeRight(prefs.homePagesNum)
                    "down" -> handleSwipeLeft(prefs.homePagesNum)
                    "left" -> when (val action = prefs.swipeLeftAction) {
                        Action.OpenApp -> openSwipeLeftApp()
                        else -> handleOtherAction(action)
                    }

                    "right" -> when (val action = prefs.swipeRightAction) {
                        Action.OpenApp -> openSwipeRightApp()
                        else -> handleOtherAction(action)
                    }
                }
                CrashHandler.logUserAction("LongSwipe_${direction} Gesture")
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun updateAppCount(newAppsNum: Int) {
        val oldAppsNum = binding.homeAppsLayout.childCount
        val diff = newAppsNum - oldAppsNum

        // Add dynamic padding based on visible widgets
        updateHomeAppsPadding()

        if (diff > 0) {
            // Add new apps
            for (i in oldAppsNum until newAppsNum) {
                val view = layoutInflater.inflate(R.layout.home_app_button, null) as TextView
                view.apply {
                    textSize = prefs.appSize.toFloat()
                    id = i
                    val appModel = prefs.getHomeAppModel(i)
                    val customLabel = prefs.getAppAlias("app_alias_${appModel.activityPackage}")

                    // Handle empty space synthetic app - display as blank
                    if (appModel.activityPackage == "com.inkos.internal.empty_space") {
                        text = "" // Empty text for spacing
                        isClickable = false // Make non-clickable
                        isFocusable = false // Make non-focusable
                        alpha = 0.0f // Make invisible but take up space
                    } else {
                        val displayText =
                            if (customLabel.isNotEmpty()) customLabel else appModel.activityLabel
                        // Apply small caps transformation if enabled (without modifying the original data)
                        text = when {
                            prefs.allCapsApps -> displayText.uppercase()
                            prefs.smallCapsApps -> displayText.lowercase()
                            else -> displayText
                        }
                        isClickable = true
                        isFocusable = true
                        alpha = 1.0f
                    }
                    setTextColor(prefs.appColor)
                    setHintTextColor(prefs.appColor)
                    @SuppressLint("ClickableViewAccessibility")
                    setOnTouchListener(getHomeAppsGestureListener(context, this))
                    setOnClickListener(this@HomeFragment)
                    // Centralized key handling for app button via KeyMapperHelper
                    setOnKeyListener { _, keyCode, event ->
                        val mapped = KeyMapperHelper.mapAppButtonKey(prefs, keyCode, event)
                        when (mapped) {
                            is KeyMapperHelper.HomeKeyAction.MoveSelectionDown -> {
                                moveSelectionDown()
                                true
                            }
                            is KeyMapperHelper.HomeKeyAction.GestureLeft -> {
                                when (val act = mapped.action) {
                                    Action.OpenApp -> openSwipeLeftApp()
                                    else -> handleOtherAction(act)
                                }
                                CrashHandler.logUserAction("DPAD_RIGHT Gesture (SwipeLeft)")
                                true
                            }
                            is KeyMapperHelper.HomeKeyAction.GestureRight -> {
                                when (val act = mapped.action) {
                                    Action.OpenApp -> openSwipeRightApp()
                                    else -> handleOtherAction(act)
                                }
                                CrashHandler.logUserAction("DPAD_LEFT Gesture (SwipeRight)")
                                true
                            }
                            is KeyMapperHelper.HomeKeyAction.LongPressSelected -> {
                                onLongClick(this)
                                true
                            }
                            is KeyMapperHelper.HomeKeyAction.OpenSettings -> {
                                trySettings()
                                true
                            }
                            else -> false
                        }
                    }

                    if (!prefs.extendHomeAppsArea) {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    gravity = Gravity.CENTER // Always use center gravity
                    isFocusable = true
                    isFocusableInTouchMode = true

                    val padding: Int = prefs.textPaddingSize
                    setPadding(0, padding, 0, padding)
                    setTextColor(prefs.appColor)

                    // Apply apps font
                    typeface = prefs.getFontForContext("apps")
                        .getFont(context, prefs.getCustomFontPathForContext("apps"))

                    tag = appModel.activityPackage // Assign unique tag
                }
                binding.homeAppsLayout.addView(view)
            }
        } else if (diff < 0) {
            binding.homeAppsLayout.removeViews(oldAppsNum + diff, -diff)
        }

        // Update the total number of pages and calculate maximum apps per page
        updatePagesAndAppsPerPage(prefs.homeAppsNum, prefs.homePagesNum)
    }

    private fun updateHomeAppsPadding() {
        binding.apply {
            // Check if any of the main widgets are visible
            val hasVisibleWidgets = clock.isVisible

            // Apply padding only if widgets are visible
            val defaultPadding = resources.getDimensionPixelSize(R.dimen.home_apps_default_padding)

            // Apply the padding
            homeAppsLayout.setPadding(
                homeAppsLayout.paddingLeft,
                if (hasVisibleWidgets) defaultPadding else 0,
                homeAppsLayout.paddingRight,
                homeAppsLayout.paddingBottom
            )
        }
    }

    // updates number of apps visible on home screen
    // does nothing if number has not changed
    private var currentPage = 0
    private var appsPerPage = 0

    private fun updatePagesAndAppsPerPage(totalApps: Int, totalPages: Int) {
        appsPerPage = if (totalPages > 0) {
            (totalApps + totalPages - 1) / totalPages
        } else {
            0
        }
        updateAppsVisibility(totalPages)
    }

    private fun updateAppsVisibility(totalPages: Int) {
        val startIdx = currentPage * appsPerPage
        val endIdx = minOf((currentPage + 1) * appsPerPage, getTotalAppsCount())

        for (i in 0 until getTotalAppsCount()) {
            val view = binding.homeAppsLayout.getChildAt(i)
            view.visibility = if (i in startIdx until endIdx) View.VISIBLE else View.GONE
        }

        // Disable animations on the pager before making changes
        binding.homeScreenPager.clearAnimation()

        // Clear existing page indicators
        binding.homeScreenPager.removeAllViews()

        // Create new page indicators
        val sizeInDp = 12
        val density = requireContext().resources.displayMetrics.density
        val sizeInPx = (sizeInDp * density).toInt()
        val spacingInPx = (12 * density).toInt() // 6dp spacing between indicators

        for (pageIndex in 0 until totalPages) {
            val imageView = ImageView(requireContext()).apply {
                val drawableRes = if (pageIndex == currentPage) {
                    R.drawable.ic_current_page
                } else {
                    R.drawable.ic_new_page
                }

                val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)?.apply {
                    val colorFilterColor: ColorFilter =
                        PorterDuffColorFilter(prefs.appColor, PorterDuff.Mode.SRC_IN)
                    colorFilter = colorFilterColor
                }
                setImageDrawable(drawable)

                layoutParams = LinearLayout.LayoutParams(sizeInPx, sizeInPx).apply {
                    if (pageIndex > 0) {
                        topMargin = spacingInPx
                    }
                }
            }
            binding.homeScreenPager.addView(imageView)
        }

        // Show/hide the page indicator based on preferences
        if (prefs.homePagesNum > 1 && prefs.homePager) {
            // Set visibility without animation
            if (binding.homeScreenPager.visibility != View.VISIBLE) {
                binding.homeScreenPager.clearAnimation()
                binding.homeScreenPager.visibility = View.VISIBLE
            }
        } else {
            if (binding.homeScreenPager.visibility != View.GONE) {
                binding.homeScreenPager.clearAnimation()
                binding.homeScreenPager.visibility = View.GONE
            }
        }
    }

    private fun handleSwipeLeft(totalPages: Int) {
        if (totalPages <= 0) return // Prevent issues if totalPages is 0 or negative

        currentPage = if (currentPage == 0) {
            totalPages - 1 // Wrap to last page if on the first page
        } else {
            currentPage - 1 // Move to the previous page
        }

        updateAppsVisibility(totalPages)
        vibratePaging()
    }

    private fun handleSwipeRight(totalPages: Int) {
        if (totalPages <= 0) return // Prevent issues if totalPages is 0 or negative

        currentPage = if (currentPage == totalPages - 1) {
            0 // Wrap to first page if on the last page
        } else {
            currentPage + 1 // Move to the next page
        }

        updateAppsVisibility(totalPages)
        vibratePaging()
    }

    private fun vibratePaging() {
        if (prefs.useVibrationForPaging) {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50) // Vibrate for 50 milliseconds
        }
    }

    private fun vibrateForWidget() {
        if (prefs.useVibrationForPaging) {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50) // Vibrate for 50 milliseconds
        }
    }

    private fun getTotalAppsCount(): Int {
        return binding.homeAppsLayout.childCount
    }

    private fun trySettings() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (prefs.settingsLocked) {
                biometricHelper.startBiometricSettingsAuth(object :
                    BiometricHelper.CallbackSettings {
                    override fun onAuthenticationSucceeded() {
                        sendToSettingFragment()
                    }

                    override fun onAuthenticationFailed() {
                        Log.e(
                            "Authentication",
                            getString(R.string.text_authentication_failed)
                        )
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errorMessage: CharSequence?
                    ) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED -> Log.e(
                                "Authentication",
                                getString(R.string.text_authentication_cancel)
                            )

                            else ->
                                Log.e(
                                    "Authentication",
                                    getString(R.string.text_authentication_error).format(
                                        errorMessage,
                                        errorCode
                                    )
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
            // Remove firstOpen(false) call, as it is only for first run tip
            // viewModel.firstOpen(false)
        } catch (e: java.lang.Exception) {
            Log.d("onLongClick", e.toString())
        }
    }

    fun setupBackgroundImage() {
    BackgroundImageHelper.setupBackgroundImage(requireContext(), prefs, viewModel, binding.root as ViewGroup)
    }

    // ...existing code...

    // Called from MainActivity when window regains focus (e.g., overlay closed)
    fun onWindowFocusGained() {
    EinkRefreshHelper.refreshEink(requireContext(), prefs, binding.root as? ViewGroup, prefs.einkRefreshDelay)
        // Optionally, refresh UI state if needed
        viewModel.refreshHomeAppsUiState(requireContext())
    }

    // Update date display based on current preferences
    private fun updateDateDisplay() {
        val showClock = prefs.showClock
        val showDate = prefs.showDate
        val clockView = binding.root.findViewById<TextView>(R.id.clock)
        val dateView = binding.root.findViewById<TextView>(R.id.date)
        // Redundant safeguard: ensure date click listener is set even if view is recreated
        dateView?.setOnClickListener(this@HomeFragment)
        val density = requireContext().resources.displayMetrics.density
        val clockLayoutParams = clockView.layoutParams as? LinearLayout.LayoutParams
        val dateLayoutParams = dateView.layoutParams as? LinearLayout.LayoutParams

        // Update bottom widget margin for bottom widgets wrapper
        applyBottomWidgetMargin()

        if (showClock && showDate) {
            clockView.visibility = View.VISIBLE
            dateView.visibility = View.VISIBLE

            // Apply font and size from prefs
            dateView.textSize = prefs.dateSize.toFloat()
            dateView.typeface = prefs.getFontForContext("date")
                .getFont(requireContext(), prefs.getCustomFontPathForContext("date"))
            // Margin: only clock gets top margin, date gets 0
            if (clockLayoutParams != null) {
                clockLayoutParams.topMargin = (prefs.topWidgetMargin * density).toInt()
                clockView.layoutParams = clockLayoutParams
            }
            if (dateLayoutParams != null) {
                dateLayoutParams.topMargin = 0
                dateView.layoutParams = dateLayoutParams
            }

            // Trigger battery receiver to update date with current battery level
            val batteryIntent =
                requireContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { batteryReceiver.onReceive(requireContext(), it) }
        } else if (showClock) {
            clockView.visibility = View.VISIBLE
            dateView.visibility = View.GONE
            if (clockLayoutParams != null) {
                clockLayoutParams.topMargin = (prefs.topWidgetMargin * density).toInt()
                clockView.layoutParams = clockLayoutParams
            }
        } else if (showDate) {
            clockView.visibility = View.GONE
            dateView.visibility = View.VISIBLE

            dateView.textSize = prefs.dateSize.toFloat()
            dateView.typeface = prefs.getFontForContext("date")
                .getFont(requireContext(), prefs.getCustomFontPathForContext("date"))
            if (dateLayoutParams != null) {
                dateLayoutParams.topMargin = (prefs.topWidgetMargin * density).toInt()
                dateView.layoutParams = dateLayoutParams
            }

            // Trigger battery receiver to update date with current battery level
            val batteryIntent =
                requireContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { batteryReceiver.onReceive(requireContext(), it) }
        } else {
            clockView.visibility = View.GONE
            dateView.visibility = View.GONE
        }
    }

    private fun updateMediaPlayerWidget(mediaPlayer: AudioWidgetHelper.MediaPlayerInfo?) {
        // Check if audio widget is enabled in preferences
        if (mediaPlayer != null && prefs.showAudioWidgetEnabled) {
            binding.mediaPlayerWidget.visibility = View.VISIBLE

            // Update play/pause button drawable based on current state
            val isPlaying = mediaPlayer.isPlaying
            val playPauseDrawable = if (isPlaying) R.drawable.audio_pause else R.drawable.audio_play
            binding.mediaPlayPause.setImageResource(playPauseDrawable)
        } else {
            binding.mediaPlayerWidget.visibility = View.GONE
        }
    }

    private fun setupMediaPlayerClickListeners() {
        val audioWidgetHelper = AudioWidgetHelper.getInstance(requireContext())

        binding.mediaOpenApp.setOnClickListener {
            vibrateForWidget()
            audioWidgetHelper.openMediaApp()
        }

        binding.mediaPrevious.setOnClickListener {
            vibrateForWidget()
            audioWidgetHelper.skipToPrevious()
        }

        binding.mediaPlayPauseContainer.setOnClickListener {
            vibrateForWidget()
            audioWidgetHelper.playPauseMedia()
        }

        binding.mediaNext.setOnClickListener {
            vibrateForWidget()
            audioWidgetHelper.skipToNext()
        }

        binding.mediaStop.setOnClickListener {
            vibrateForWidget()
            audioWidgetHelper.stopMedia()
        }
    }

    private fun applyAudioWidgetColor(color: Int) {
        // Apply tint to all audio widget ImageViews
        // Each FrameLayout contains one ImageView as a child
        (binding.mediaOpenApp.getChildAt(0) as? ImageView)?.imageTintList =
            android.content.res.ColorStateList.valueOf(color)
        (binding.mediaPrevious.getChildAt(0) as? ImageView)?.imageTintList =
            android.content.res.ColorStateList.valueOf(color)
        (binding.mediaNext.getChildAt(0) as? ImageView)?.imageTintList =
            android.content.res.ColorStateList.valueOf(color)
        (binding.mediaStop.getChildAt(0) as? ImageView)?.imageTintList =
            android.content.res.ColorStateList.valueOf(color)

        // For the play/pause container, apply color to the circle background (first child)
        (binding.mediaPlayPauseContainer.getChildAt(0) as? ImageView)?.imageTintList =
            android.content.res.ColorStateList.valueOf(color)
        // The play/pause icon itself already has a special tint for contrast
    }

    private fun applyBottomWidgetMargin() {
        val bottomWidgetsWrapper = binding.root.findViewById<LinearLayout>(R.id.bottomWidgetsWrapper)
        bottomWidgetsWrapper?.let { wrapper ->
            val wrapperLayoutParams = wrapper.layoutParams as? ViewGroup.MarginLayoutParams
            if (wrapperLayoutParams != null) {
                val density = requireContext().resources.displayMetrics.density
                wrapperLayoutParams.bottomMargin = (prefs.bottomWidgetMargin * density).toInt()
                wrapper.layoutParams = wrapperLayoutParams
            }
        }
    }

    private fun exitLauncher() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(intent, "Choose your launcher"))
    }
}
