package com.github.gezimos.inkos.ui.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.material.Icon
import androidx.compose.material.Text
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SignalCellularOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.utils.BrightnessHelper
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.services.NotificationService
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SkipNext
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.resolveThemeColors
import com.github.gezimos.inkos.ui.compose.gestureHelper
import com.github.gezimos.inkos.ui.dialogs.LockedBottomSheetDialog
import kotlinx.coroutines.launch
import android.service.notification.StatusBarNotification

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class SimpleTrayFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var vibrator: Vibrator
    private var simpleTrayViewModel: SimpleTrayViewModel? = null
    private val wifiPanelAction = "android.settings.panel.action.WIFI"
    private val internetPanelAction = "android.settings.panel.action.INTERNET_CONNECTIVITY"
    private val keyPressTracker = com.github.gezimos.inkos.ui.compose.KeyPressTracker()
    private var dismissKeyDownTime: Long = 0L
    private val LONG_PRESS_THRESHOLD_MS = 700L
    private var wasPaused = false
    private var focusRestoreTriggerState: androidx.compose.runtime.MutableState<Int>? = null
    
    private val writeSettingsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* User returned from settings */ }
    
    private var permissionExplanationDialog: LockedBottomSheetDialog? = null
    
    /**
     * Shows an informational bottom sheet dialog explaining why a permission is needed before requesting it.
     */
    private fun showPermissionExplanationDialog(
        title: String,
        message: String,
        onContinue: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val context = requireContext()
        
        // Dismiss any existing permission dialog
        permissionExplanationDialog?.dismiss()
        
        // Grab prefs and resolve theme colors
        val prefs = Prefs(context)
        val (dlgTextColor, dlgBackground) = resolveThemeColors(context)
        
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            
            // Title
            val titleView = TextView(context).apply {
                text = title.uppercase()
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 16f
                try { setTextColor(dlgTextColor) } catch (_: Exception) {}
            }
            addView(titleView)
            
            // Message (only show if available - backend now provides category-specific text or null)
            message?.let { msg ->
                val messageView = TextView(context).apply {
                    text = msg
                    gravity = Gravity.START
                    textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                    textSize = 14f
                    val mPad = (8 * context.resources.displayMetrics.density).toInt()
                    setPadding(mPad, mPad, mPad, mPad)
                    try { setTextColor(dlgTextColor) } catch (_: Exception) {}
                }
                addView(messageView)
            }
            
            // Buttons row
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val btnMargin = (8 * context.resources.displayMetrics.density).toInt()
                val btnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(btnMargin, btnMargin, btnMargin, 0)
                }
                
                // Cancel button (if onCancel is provided)
                if (onCancel != null) {
                    val cancelBtn = Button(context).apply {
                        text = context.getString(android.R.string.cancel)
                        val btnPadding = (10 * context.resources.displayMetrics.density).toInt()
                        setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                        minWidth = 0
                        minimumWidth = 0
                        minHeight = 0
                        minimumHeight = 0
                        setOnClickListener {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onCancel()
                            permissionExplanationDialog?.dismiss()
                        }
                    }
                    // Style button
                    try {
                        val density = context.resources.displayMetrics.density
                        val radius = (6 * density)
                        val strokeWidth = (3f * density).toInt()
                        val bgDrawable = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = radius
                            setColor(dlgBackground)
                            setStroke(strokeWidth, dlgTextColor)
                        }
                        cancelBtn.background = bgDrawable
                        cancelBtn.setTextColor(dlgTextColor)
                    } catch (_: Exception) {}
                    cancelBtn.layoutParams = btnParams
                    addView(cancelBtn)
                }
                
                // Continue/OK button
                val continueBtn = Button(context).apply {
                    text = context.getString(android.R.string.ok)
                    val btnPadding = (10 * context.resources.displayMetrics.density).toInt()
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        permissionExplanationDialog?.dismiss()
                        onContinue()
                    }
                }
                // Style button
                try {
                    val density = context.resources.displayMetrics.density
                    val radius = (6 * density)
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(dlgBackground)
                        setStroke(strokeWidth, dlgTextColor)
                    }
                    continueBtn.background = bgDrawable
                    continueBtn.setTextColor(dlgTextColor)
                } catch (_: Exception) {}
                continueBtn.layoutParams = btnParams
                addView(continueBtn)
            }
            
            val buttonsParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * context.resources.displayMetrics.density).toInt()
            }
            buttonsRow.layoutParams = buttonsParams
            addView(buttonsRow)
        }
        
        // Apply fonts
        try {
            val fontFamily = prefs.getFontForContext("settings")
            val customFontPath = prefs.getCustomFontPathForContext("settings")
            val typeface = fontFamily.getFont(context, customFontPath)
            val textSize = prefs.settingsSize.toFloat()
            
            fun applyFont(view: View) {
                if (view is TextView) {
                    typeface?.let { view.typeface = it }
                    view.textSize = textSize
                } else if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        applyFont(view.getChildAt(i))
                    }
                }
            }
            applyFont(content)
        } catch (_: Exception) {}
        
        // Create and show bottom sheet dialog
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            content.setBackgroundColor(dlgBackground)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        
        dialog.setLocked(true)
        dialog.show()
        permissionExplanationDialog = dialog
        
        // WindowInsets padding for navigation bar
        try {
            val baseBottom = content.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (8 * context.resources.displayMetrics.density).toInt()
                        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, baseBottom + navBarInset + extra)
                    } catch (_: Exception) {}
                    insets
                }
                ViewCompat.requestApplyInsets(decor)
            }
        } catch (_: Exception) {}
    }
    
    private fun requestWriteSettingsPermission(showExplanation: Boolean = true) {
        val ctx = requireContext()
        if (!Settings.System.canWrite(ctx)) {
            if (showExplanation) {
                showPermissionExplanationDialog(
                    title = "Modify System Settings Permission",
                    message = "This app needs permission to modify system settings to control screen brightness. " +
                            "You will be taken to Android settings to grant this permission.",
                    onContinue = {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = "package:${ctx.packageName}".toUri()
                        writeSettingsPermissionLauncher.launch(intent)
                    }
                )
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = "package:${ctx.packageName}".toUri()
                writeSettingsPermissionLauncher.launch(intent)
            }
        }
    }
    
    private fun getBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH)
        }
    }
    private var pendingBluetoothAction: (() -> Unit)? = null
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            result[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            true
        }
        if (granted) {
            pendingBluetoothAction?.invoke()
            simpleTrayViewModel?.onPermissionResult(SimpleTrayPermissionRequest.BluetoothConnect, true)
        } else {
            context?.showShortToast("Bluetooth permission required to toggle")
            simpleTrayViewModel?.onPermissionResult(SimpleTrayPermissionRequest.BluetoothConnect, false)
        }
        pendingBluetoothAction = null
    }

    private fun requestBluetoothPermissions(onGranted: () -> Unit, showExplanation: Boolean = true) {
        val ctx = context ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onGranted()
            return
        }
        val perms = getBluetoothPermissions()
        val needsPermission = perms.any {
            ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED
        }
        if (!needsPermission) {
            onGranted()
            return
        }
        
        // Ensure fragment is attached before launching permission request
        if (!isAdded) {
            ctx.showShortToast("Please wait...")
            return
        }
        
        if (showExplanation) {
            showPermissionExplanationDialog(
                title = "Bluetooth Permission",
                message = "This app needs Bluetooth permission to toggle Bluetooth on and off from the quick settings tray.",
                onContinue = {
                    if (isAdded) {
                        pendingBluetoothAction = onGranted
                        try {
                            bluetoothPermissionLauncher.launch(perms)
                        } catch (e: IllegalStateException) {
                            // Launcher not ready yet, try again after a short delay
                            view?.postDelayed({
                                if (isAdded) {
                                    try {
                                        bluetoothPermissionLauncher.launch(perms)
                                    } catch (_: Exception) {
                                        ctx.showShortToast("Permission request failed. Please try again.")
                                    }
                                }
                            }, 100)
                        }
                    }
                }
            )
        } else {
            pendingBluetoothAction = onGranted
            try {
                bluetoothPermissionLauncher.launch(perms)
            } catch (e: IllegalStateException) {
                // Launcher not ready yet, try again after a short delay
                view?.postDelayed({
                    if (isAdded) {
                        try {
                            bluetoothPermissionLauncher.launch(perms)
                        } catch (_: Exception) {
                            ctx.showShortToast("Permission request failed. Please try again.")
                        }
                    }
                }, 100)
            }
        }
    }

    private val phoneStatePermission = Manifest.permission.READ_PHONE_STATE
    private var pendingPhoneStateAction: (() -> Unit)? = null
    private val phoneStatePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingPhoneStateAction?.invoke()
            simpleTrayViewModel?.onPermissionResult(SimpleTrayPermissionRequest.ReadPhoneState, true)
        } else {
            context?.showShortToast("Phone state permission required")
            simpleTrayViewModel?.onPermissionResult(SimpleTrayPermissionRequest.ReadPhoneState, false)
        }
        pendingPhoneStateAction = null
    }

    private fun requestPhoneStatePermission(onGranted: () -> Unit, showExplanation: Boolean = true) {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, phoneStatePermission) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
            return
        }
        
        // Ensure fragment is attached before launching permission request
        if (!isAdded) {
            ctx.showShortToast("Please wait...")
            return
        }
        
        if (showExplanation) {
            showPermissionExplanationDialog(
                title = "Phone State Permission",
                message = "This app needs phone state permission to read and control mobile data settings from the quick settings tray.",
                onContinue = {
                    if (isAdded) {
                        pendingPhoneStateAction = onGranted
                        try {
                            phoneStatePermissionLauncher.launch(phoneStatePermission)
                        } catch (e: IllegalStateException) {
                            // Launcher not ready yet, try again after a short delay
                            view?.postDelayed({
                                if (isAdded) {
                                    try {
                                        phoneStatePermissionLauncher.launch(phoneStatePermission)
                                    } catch (_: Exception) {
                                        ctx.showShortToast("Permission request failed. Please try again.")
                                    }
                                }
                            }, 100)
                        }
                    }
                }
            )
        } else {
            pendingPhoneStateAction = onGranted
            try {
                phoneStatePermissionLauncher.launch(phoneStatePermission)
            } catch (e: IllegalStateException) {
                // Launcher not ready yet, try again after a short delay
                view?.postDelayed({
                    if (isAdded) {
                        try {
                            phoneStatePermissionLauncher.launch(phoneStatePermission)
                        } catch (_: Exception) {
                            ctx.showShortToast("Permission request failed. Please try again.")
                        }
                    }
                }, 100)
            }
        }
    }

    private var pendingCameraAction: (() -> Unit)? = null
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingCameraAction?.invoke()
            simpleTrayViewModel?.onPermissionResult(SimpleTrayPermissionRequest.Camera, true)
        } else {
            context?.showShortToast("Camera permission required to use flashlight")
            simpleTrayViewModel?.onPermissionResult(SimpleTrayPermissionRequest.Camera, false)
        }
        pendingCameraAction = null
    }

    private fun requestCameraPermission(onGranted: () -> Unit, showExplanation: Boolean = true) {
        val ctx = context ?: return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            onGranted()
            return
        }
        
        // Ensure fragment is attached before launching permission request
        if (!isAdded) {
            ctx.showShortToast("Please wait...")
            return
        }
        
        if (showExplanation) {
            showPermissionExplanationDialog(
                title = "Camera Permission",
                message = "This app needs camera permission to use the flashlight feature from the quick settings tray.",
                onContinue = {
                    if (isAdded) {
                        pendingCameraAction = onGranted
                        try {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } catch (e: IllegalStateException) {
                            // Launcher not ready yet, try again after a short delay
                            view?.postDelayed({
                                if (isAdded) {
                                    try {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } catch (_: Exception) {
                                        ctx.showShortToast("Permission request failed. Please try again.")
                                    }
                                }
                            }, 100)
                        }
                    }
                }
            )
        } else {
            pendingCameraAction = onGranted
            try {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } catch (e: IllegalStateException) {
                // Launcher not ready yet, try again after a short delay
                view?.postDelayed({
                    if (isAdded) {
                        try {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } catch (_: Exception) {
                            ctx.showShortToast("Permission request failed. Please try again.")
                        }
                    }
                }, 100)
            }
        }
    }

    // Simple pager state accessible from both Compose and Activity handlers
    private class SimplePagerState(current: Int = 0, count: Int = 1) {
        private var _current = mutableIntStateOf(current)
        private var _count = mutableIntStateOf(count)
        var currentPage: Int
            get() = _current.intValue
            set(v) { _current.intValue = v }
        var pageCount: Int
            get() = _count.intValue
            set(v) { _count.intValue = v }
        fun scrollToPage(page: Int) {
            currentPage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = try { androidx.lifecycle.ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java] } catch (_: Exception) { null }
        val trayVm = try { androidx.lifecycle.ViewModelProvider(this)[SimpleTrayViewModel::class.java] } catch (_: Exception) { null }
        simpleTrayViewModel = trayVm
        prefs = vm?.getPrefs() ?: Prefs(requireContext())
        vibrator = requireContext().getSystemService(Vibrator::class.java)
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            val isDark = prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
            // Create state for focus restoration trigger that can be observed by Compose
            val triggerState = remember { mutableStateOf(0) }
            focusRestoreTriggerState = triggerState
            SettingsTheme(isDark = isDark) {
                SimpleTrayScreen(
                    composeView = composeView,
                    simpleTrayViewModel = trayVm,
                    requestBluetoothPermission = ::requestBluetoothPermissions,
                    requestPhoneStatePermission = ::requestPhoneStatePermission,
                    requestCameraPermission = ::requestCameraPermission,
                    focusRestoreTrigger = triggerState.value
                )
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // E-Ink refresh moved to MainActivity central handler; fragment-level call removed
        // Keep view focus so other key events can be received by the fragment if needed
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.requestFocus()

        // Local key listener remains responsible only for actions other than page up/down.
        // Page navigation (volume keys and optionally DPAD/pages) is forwarded from the Activity.
        view.setOnKeyListener { v, keyCode, event ->
            // Show quick hint when user presses left/right dpad keys to explain actions
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        showShortToast("Dismiss: Del, C, #1 | Long press to dismiss all")
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        showShortToast("Open: Enter, Dpad Center, #3")
                        return@setOnKeyListener true
                    }
                }
            }

            val mapped = com.github.gezimos.inkos.ui.compose.NavHelper.mapNotificationsKey(prefs, keyCode, event)
            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.None) return@setOnKeyListener false

            // If the mapped action is PageUp/PageDown, let the Activity handle it via the centralized handler.
            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageUp || mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageDown) {
                return@setOnKeyListener false
            }

            val composeView = v as? ComposeView
            val pagerState = composeView?.getTag(0xdeadbeef.toInt()) as? SimplePagerState
            val coroutineScope = composeView?.getTag(0xcafebabe.toInt()) as? kotlinx.coroutines.CoroutineScope
            val validNotifications = (composeView?.getTag(0xabcdef01.toInt()) as? List<*>)
                ?.filterIsInstance<Pair<String, NotificationManager.ConversationNotification>>()

            if (pagerState == null || coroutineScope == null || validNotifications == null) {
                android.util.Log.d("NotificationsFragment", "pagerState, coroutineScope, or validNotifications is null")
                return@setOnKeyListener false
            }

            when (mapped) {
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Dismiss -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            // Track when key was pressed down
                            dismissKeyDownTime = System.currentTimeMillis()
                            // Also check repeatCount for immediate long press detection
                            if (event.repeatCount > 15) {
                                // Long press detected via repeat count: dismiss all notifications
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                validNotifications.forEach { (pkg, notif) ->
                                    try {
                                        val isMedia = notif.category == android.app.Notification.CATEGORY_TRANSPORT
                                        if (isMedia) {
                                            // For media notifications, stop the media session
                                            AudioWidgetHelper.getInstance(requireContext()).stopMedia()
                                        } else {
                                            // For regular notifications, dismiss from system tray
                                            if (notif.notificationKey != null) {
                                                NotificationService.dismissNotification(notif.notificationKey)
                                            }
                                            // Remove from conversation notifications
                                            NotificationManager.getInstance(requireContext())
                                                .removeConversationNotification(pkg, notif.conversationId)
                                        }
                                    } catch (_: Exception) {}
                                }
                                showShortToast("All notifications dismissed")
                                dismissKeyDownTime = 0L // Reset to prevent double-trigger
                                return@setOnKeyListener true
                            }
                        }
                        KeyEvent.ACTION_UP -> {
                            val pressDuration = if (dismissKeyDownTime > 0) {
                                System.currentTimeMillis() - dismissKeyDownTime
                            } else {
                                0L
                            }
                            dismissKeyDownTime = 0L // Reset
                            
                            if (pressDuration >= LONG_PRESS_THRESHOLD_MS) {
                                // Long press: dismiss all notifications
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                validNotifications.forEach { (pkg, notif) ->
                                    try {
                                        // Dismiss notification from system tray (mark as read/snooze)
                                        if (notif.notificationKey != null) {
                                            NotificationService.dismissNotification(notif.notificationKey)
                                        }
                                        // Remove from conversation notifications
                                        NotificationManager.getInstance(requireContext())
                                            .removeConversationNotification(pkg, notif.conversationId)
                                    } catch (_: Exception) {}
                                }
                                showShortToast("All notifications dismissed")
                                return@setOnKeyListener true
                            } else {
                                // Short press: dismiss first notification on the current page
                                val notifIndex = pagerState.currentPage * prefs.notificationsPerPage
                                val (pkg, notif) = validNotifications.getOrNull(notifIndex) ?: return@setOnKeyListener true
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                
                                val isMedia = notif.category == android.app.Notification.CATEGORY_TRANSPORT
                                if (isMedia) {
                                    // For media notifications, stop the media session
                                    AudioWidgetHelper.getInstance(requireContext()).stopMedia()
                                } else {
                                    // For regular notifications, dismiss from system tray
                                    if (notif.notificationKey != null) {
                                        NotificationService.dismissNotification(notif.notificationKey)
                                    }
                                    // Remove the conversation notification so UI updates
                                    NotificationManager.getInstance(requireContext())
                                        .removeConversationNotification(pkg, notif.conversationId)
                                }
                                coroutineScope.launch {
                                    // recompute pages after removal
                                    val newCount = (validNotifications.size - 1).coerceAtLeast(0)
                                    val newPages = ((newCount + 2) / 3).coerceAtLeast(1)
                                    val nextPage = pagerState.currentPage.coerceAtMost(newPages - 1)
                                    kotlinx.coroutines.delay(150)
                                    if (validNotifications.size > 1) {
                                        pagerState.scrollToPage(nextPage)
                                    }
                                }
                                return@setOnKeyListener true
                            }
                        }
                    }
                    true
                }
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Open -> {
                    val notifIndex = pagerState.currentPage * prefs.notificationsPerPage
                    val (pkg, notif) = validNotifications.getOrNull(notifIndex) ?: return@setOnKeyListener true
                    NotificationManager.getInstance(requireContext())
                        .openNotification(pkg, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Don't register Activity-level page navigation handler - let Compose handle all DPAD navigation
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        act.pageNavigationHandler = null
        
        // Always restore focus after screen unlock (when wasPaused is true)
        // This fixes the issue where D-pad stops working after closing/reopening flip phone
        if (wasPaused) {
            wasPaused = false
            // Restore focus to fragment view
            view?.postDelayed({
                val currentView = view
                if (isAdded && !isDetached && currentView != null) {
                    try {
                        currentView.isFocusable = true
                        currentView.isFocusableInTouchMode = true
                        currentView.requestFocus()
                    } catch (_: Exception) {}
                }
            }, 350) // Delay to let MainActivity's onResume and window focus changes complete
            
            // Also trigger Compose focus restoration by incrementing a counter
            // This will be passed to SimpleTrayScreen to re-request Compose focus
            focusRestoreTriggerState?.value = (focusRestoreTriggerState?.value ?: 0) + 1
        }
    }

    override fun onPause() {
        super.onPause()
        wasPaused = true
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        if (act.pageNavigationHandler != null) act.pageNavigationHandler = null
    }

    @Composable
    fun SimpleTrayScreen(
        composeView: ComposeView,
        simpleTrayViewModel: SimpleTrayViewModel?,
        requestBluetoothPermission: (onGranted: () -> Unit) -> Unit,
        requestPhoneStatePermission: (onGranted: () -> Unit) -> Unit,
        requestCameraPermission: (onGranted: () -> Unit) -> Unit,
        focusRestoreTrigger: Int = 0
    ) {
        val ctx = LocalContext.current

        // Request focus on mount and when focus needs to be restored (after screen unlock)
        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        LaunchedEffect(focusRestoreTrigger) {
            // Re-request focus when trigger changes (after screen unlock)
            kotlinx.coroutines.delay(400) // Delay to ensure window has regained focus
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }

        // Use raw system notifications directly from NotificationService for 1:1 parity with Android notification tray
        // This ensures SimpleTray shows exactly what Android's notification tray shows, without caching
        val rawNotifications by NotificationService.sbnState.collectAsState(initial = emptyList())
        val prefsLocal = Prefs(ctx)
        val notificationManager = NotificationManager.getInstance(ctx)

        // Helper function to convert StatusBarNotification to ConversationNotification format
        fun convertSbnToConversationNotification(sbn: StatusBarNotification): NotificationManager.ConversationNotification {
            val packageName = sbn.packageName
            val extras = sbn.notification.extras
            
            // Check preferences to respect user settings
            val showGroup = prefsLocal.showNotificationGroupName
            
            // Extract conversation title - only if preference is enabled
            val conversationTitleRaw = if (showGroup) {
                extras?.getCharSequence("android.conversationTitle")?.toString()
                    ?: extras?.getString("android.conversationTitle")
            } else {
                null
            }
            
            // Extract sender/title - try multiple fields
            // For WhatsApp, android.title might be "Sender: GroupName" or just "Sender"
            val titleRaw = extras?.getCharSequence("android.title")?.toString()
                ?: extras?.getString("android.title")
            
            // Parse sender from title (might contain "Sender: GroupName" format)
            val senderRaw = if (!titleRaw.isNullOrBlank() && titleRaw.contains(": ")) {
                // If title contains ": ", split it - first part is sender, second part might be group
                titleRaw.split(": ", limit = 2).firstOrNull()?.trim()
            } else {
                titleRaw
            } ?: extras?.getCharSequence("android.subText")?.toString()
                ?: extras?.getString("android.subText")
            
            // Extract message - try multiple fields in priority order
            // CRITICAL: Make sure we get the actual message text, NEVER use group name as message
            val messageRaw = when {
                extras?.getCharSequence("android.bigText") != null -> 
                    extras.getCharSequence("android.bigText")?.toString()

                extras?.getCharSequence("android.text") != null -> 
                    extras.getCharSequence("android.text")?.toString()

                extras?.getCharSequenceArray("android.textLines") != null -> {
                    val lines = extras.getCharSequenceArray("android.textLines")
                    lines?.lastOrNull()?.toString()
                }
                
                // Try additional fields that some apps use
                extras?.getCharSequence("android.summaryText") != null -> 
                    extras.getCharSequence("android.summaryText")?.toString()
                
                extras?.getCharSequence("android.infoText") != null -> 
                    extras.getCharSequence("android.infoText")?.toString()
                
                // Ticker text (deprecated but still used by some apps)
                sbn.notification.tickerText != null -> 
                    sbn.notification.tickerText?.toString()

                else -> null
            }
            
            // CRITICAL: Ensure message is NEVER the group name
            // If messageRaw is the same as conversationTitle, it's likely wrong - set to null
            val message = if (!messageRaw.isNullOrBlank() && 
                !conversationTitleRaw.isNullOrBlank() && 
                messageRaw.trim().equals(conversationTitleRaw.trim(), ignoreCase = true)) {
                null // Message is the same as group name, which is wrong
            } else {
                messageRaw
            }
            
            // Improved conversation ID logic for better SMS threading and group chat support
            val notificationGroup = sbn.notification.group
            val conversationId = when {
                // First priority: Use notification group key if available
                !notificationGroup.isNullOrBlank() -> "group_${packageName}_$notificationGroup"
                
                // Second priority: Use conversation title if available (group chats) - but only for ID, not display
                !conversationTitleRaw.isNullOrBlank() -> conversationTitleRaw
                
                // For SMS apps, try to extract phone number or contact from various fields
                sbn.notification.category == android.app.Notification.CATEGORY_MESSAGE -> {
                    val phoneNumber = extras?.getString("android.people")?.firstOrNull()?.toString()
                        ?: extras?.getString("android.subText")
                        ?: extras?.getString("android.summaryText")
                        ?: senderRaw
                    
                    phoneNumber?.let { "sms_$it" } ?: senderRaw ?: "default"
                }
                
                // For other messaging apps, use sender as conversation ID (last resort)
                !senderRaw.isNullOrBlank() -> senderRaw
                
                // Fallback
                else -> sbn.key
            }
            
            // Only use conversationTitle if preference is enabled
            var conversationTitle = if (showGroup) conversationTitleRaw else null
            var sender = senderRaw
            // Normalize whitespace and cap message to 300 characters
            // CRITICAL: Make sure message is the actual message text, never the group name
            var finalMessage = message?.replace("\n", " ")
                ?.replace("\r", " ")
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?.take(300)
            
            // Fallback: if both sender/title and message are missing, use app label and category-specific text
            if ((conversationTitle.isNullOrBlank() && sender.isNullOrBlank()) && (finalMessage.isNullOrBlank())) {
                val pm = ctx.packageManager
                val appLabel = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                } catch (_: Exception) {
                    packageName
                }
                conversationTitle = appLabel
                sender = appLabel
                
                // Provide category-specific fallback text
                finalMessage = when (sbn.notification.category) {
                    android.app.Notification.CATEGORY_MESSAGE -> "New message"
                    android.app.Notification.CATEGORY_EMAIL -> "New email"
                    android.app.Notification.CATEGORY_CALL -> "Missed call"
                    android.app.Notification.CATEGORY_ALARM -> "Alarm"
                    android.app.Notification.CATEGORY_REMINDER -> "Reminder"
                    android.app.Notification.CATEGORY_EVENT -> "Event"
                    android.app.Notification.CATEGORY_PROMO -> "Promotion"
                    android.app.Notification.CATEGORY_TRANSPORT -> "Media playing"
                    android.app.Notification.CATEGORY_SYSTEM -> "System notification"
                    android.app.Notification.CATEGORY_SERVICE -> "Service notification"
                    android.app.Notification.CATEGORY_ERROR -> "Error"
                    android.app.Notification.CATEGORY_PROGRESS -> "Progress update"
                    android.app.Notification.CATEGORY_SOCIAL -> "Social update"
                    android.app.Notification.CATEGORY_STATUS -> "Status update"
                    android.app.Notification.CATEGORY_RECOMMENDATION -> "Recommendation"
                    else -> null
                }
            }
            
            return NotificationManager.ConversationNotification(
                conversationId = conversationId,
                conversationTitle = conversationTitle,
                sender = sender,
                message = finalMessage,
                timestamp = sbn.postTime,
                category = sbn.notification.category,
                notificationKey = sbn.key
            )
        }

        // Filter and convert raw notifications: exclude only summary notifications
        // This gives us 1:1 parity with Android's notification tray (minus summaries only)
        // Use SimpleTray-specific allowlist (empty allowlist = show all)
        val allowed = prefsLocal.allowedSimpleTrayApps
        
        val notificationsFromSbn = rawNotifications
            .filter { sbn ->
                // Filter out ALL media notifications (CATEGORY_TRANSPORT) - we'll use AudioWidgetHelper instead
                if (sbn.notification.category == android.app.Notification.CATEGORY_TRANSPORT) {
                    return@filter false
                }
                
                // Filter out summary notifications
                !notificationManager.isNotificationSummary(sbn) &&
                // Apply allowlist if configured (empty allowlist = show all)
                (allowed.isEmpty() || allowed.contains(sbn.packageName))
            }
            .mapNotNull { sbn ->
                convertSbnToConversationNotification(sbn)?.let { sbn.packageName to it }
            }
        
        // Add active media session from AudioWidgetHelper as fake notification
        // This replaces all CATEGORY_TRANSPORT notifications for consistent behavior
        // Observe AudioWidgetHelper's mediaPlayerState for real-time updates (title, artist, playing state)
        val audioWidgetHelper = AudioWidgetHelper.getInstance(ctx)
        val currentMediaPlayer by audioWidgetHelper.mediaPlayerState.collectAsState()
        val mediaNotifications: List<Pair<String, NotificationManager.ConversationNotification>> = 
            currentMediaPlayer?.let { player ->
                // Show notification for any active media session (playing OR paused)
                // Only hide when media is fully stopped
                val mediaNotif = NotificationManager.ConversationNotification(
                    conversationId = "media_${player.packageName}",
                    conversationTitle = null,
                    sender = player.title ?: "Media playing",
                    message = player.artist,
                    timestamp = System.currentTimeMillis(),
                    category = android.app.Notification.CATEGORY_TRANSPORT,
                    notificationKey = null
                )
                listOf(player.packageName to mediaNotif)
            } ?: emptyList()
        
        val validNotifications = (notificationsFromSbn + mediaNotifications)
            .sortedByDescending { (_, notif) -> notif.timestamp }
        
        // DPAD navigation state
        val isDpadModeState = remember { mutableStateOf(false) }
        var isDpadMode by isDpadModeState
        val focusZone = remember(validNotifications.size) { 
            mutableStateOf(
                if (validNotifications.isEmpty()) 
                    com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.CLEAR_ALL 
                else 
                    com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.NOTIFICATIONS
            ) 
        }
        val selectedQuickSettingIndex = remember { mutableIntStateOf(0) }
        val selectedNotificationIndex = remember { mutableIntStateOf(0) }
        val selectedBottomNavIndex = remember { mutableIntStateOf(0) }
        
        // Update focus zone when notifications are cleared
        LaunchedEffect(validNotifications.size) {
            if (validNotifications.isEmpty() && focusZone.value == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.NOTIFICATIONS) {
                focusZone.value = com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.CLEAR_ALL
            }
        }
        
        val coroutineScope = rememberCoroutineScope()
        val pageSize = prefsLocal.notificationsPerPage
        val pages = ((validNotifications.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        val pagerState = remember { SimplePagerState(0, pages) }

        // Update pagerState immediately when the number of notifications changes
        LaunchedEffect(validNotifications.size) {
            val oldPages = pagerState.pageCount
            pagerState.pageCount = pages
            if (pages != oldPages) {
                pagerState.currentPage = 0  // Reset to first page when number of pages changes
            } else if (pagerState.currentPage >= pages) {
                pagerState.currentPage = (pages - 1).coerceAtLeast(0)
            }
        }
        
        // Reset notification selection when page changes
        // Don't reset notification index on page changes - let DPAD navigation handle it
        // LaunchedEffect(pagerState.currentPage) {
        //     selectedNotificationIndex.intValue = 0
        // }

        // Compute visible notifications for the current page without caching
        val displayNotifications = run {
            val safePage = pagerState.currentPage.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
            val startIndex = safePage * pageSize
            val totalSize = validNotifications.size
            val endIndex = (startIndex + pageSize).coerceAtMost(totalSize)
            if (startIndex < totalSize) validNotifications.subList(startIndex, endIndex) else emptyList()
        }

        val appContext = remember(ctx) { ctx.applicationContext }
        val telephonyManager = remember(appContext) {
            appContext.getSystemService(TelephonyManager::class.java)
        }

        fun currentWifiState(): Boolean {
            return try {
                val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                // ACCESS_WIFI_STATE is a normal permission but check defensively.
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                    false
                } else {
                    wm?.isWifiEnabled == true
                }
            } catch (_: Exception) { false }
        }

        fun currentBluetoothState(): Boolean {
            return try {
                val btManager = appContext.getSystemService(BluetoothManager::class.java)
                val adapter = btManager?.adapter ?: return false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // Permission not granted; cannot reliably read adapter state
                        return false
                    }
                }
                try {
                    adapter.isEnabled
                } catch (_: SecurityException) {
                    false
                }
            } catch (_: Exception) { false }
        }

        fun currentCellState(): Boolean {
            return try {
                val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            } catch (_: Exception) { false }
        }

        fun currentMobileDataState(): Boolean {
            try {
                // If airplane mode is on, mobile data should be considered off
                val airplane = try { Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) } catch (_: Exception) { 0 }
                if (airplane == 1) return false
            } catch (_: Exception) {}

            // If there are no active SIM subscriptions, consider mobile data off
            try {
                val sm = try { appContext.getSystemService(android.telephony.SubscriptionManager::class.java) } catch (_: Exception) { null }
                val activeCount = try { sm?.activeSubscriptionInfoCount ?: 0 } catch (_: Exception) { 0 }
                if (activeCount <= 0) return false
            } catch (_: Exception) {}

            val tm = telephonyManager ?: return currentCellState()
            // minSdk >= O (26), so we can use isDataEnabled guarded by permission
            return if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    tm.isDataEnabled
                } catch (_: Exception) {
                    currentCellState()
                }
            } else {
                currentCellState()
            }
        }

        // Prefer reading the device mobile-data toggle where possible. This reads
        // TelephonyManager.isDataEnabled when permission is available, otherwise
        // falls back to Settings.Global.MOBILE_DATA and finally to connectivity check.
        fun readMobileToggleState(): Boolean {
            return try {
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        telephonyManager?.isDataEnabled ?: try { Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentMobileDataState() }
                    } catch (_: Exception) {
                        try { Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentMobileDataState() }
                    }
                } else {
                    try { Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentMobileDataState() }
                }
            } catch (_: Exception) { currentMobileDataState() }
        }

        val cameraManager = remember(appContext) { appContext.getSystemService(CameraManager::class.java) }
        val torchCameraId = remember(cameraManager) {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                val characteristics = try {
                    cameraManager.getCameraCharacteristics(id)
                } catch (_: Exception) {
                    null
                }
                characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }

        // Brightness state (initialize from prefs saved system value using perceptual mapping)
        val initialBrightnessPref = remember { prefs.brightnessLevel.coerceIn(0, 255) }
        val initialBrightnessValue = remember { 
            if (initialBrightnessPref == 0) 0f else kotlin.math.sqrt((initialBrightnessPref / 255f).coerceIn(0f, 1f))
        }
        var brightness by remember { mutableFloatStateOf(initialBrightnessValue) }
        
        val sliderToSystem: (Float) -> Int = { s ->
            val v = (s * s * 255f).toInt().coerceIn(0, 255)
            v
        }
        
        val onBrightnessChange: (Float) -> Unit = { newBrightness ->
            brightness = newBrightness
            val systemValue = sliderToSystem(newBrightness)
            
            // Check permission before trying to set brightness
            if (!Settings.System.canWrite(ctx)) {
                // Request permission with explanation
                requestWriteSettingsPermission(showExplanation = true)
                // Still update UI state and window brightness for immediate visual feedback
                // System brightness will be set after permission is granted
            }
            
            // Prefer VM-managed brightness when available
            if (simpleTrayViewModel != null) {
                try { simpleTrayViewModel.setBrightness(systemValue) } catch (_: Exception) {}
            } else {
                prefs.brightnessLevel = systemValue
                // Save non-zero values to lastBrightnessLevel for restoration
                if (systemValue > 0) {
                    prefs.lastBrightnessLevel = systemValue
                }
                if (Settings.System.canWrite(ctx)) {
                    try {
                        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, systemValue)
                    } catch (_: Exception) {}
                }
            }
            // Also set window brightness for immediate effect
            try {
                val activity = ctx as? android.app.Activity
                if (activity != null) {
                    val lp = activity.window.attributes
                    lp.screenBrightness = (systemValue / 255f).coerceIn(0.0f, 1f)
                    activity.window.attributes = lp
                }
            } catch (_: Exception) {}
        }
        
        // Quick settings toggle states reflect system status (collect from ViewModel when available)
        val vmLocal = simpleTrayViewModel
        val wifiState = vmLocal?.wifiEnabled?.collectAsState(initial = currentWifiState()) ?: remember { mutableStateOf(currentWifiState()) }
        val wifiEnabled by wifiState
        val bluetoothState = vmLocal?.bluetoothEnabled?.collectAsState(initial = currentBluetoothState()) ?: remember { mutableStateOf(currentBluetoothState()) }
        val bluetoothEnabled by bluetoothState
        val signalState = vmLocal?.mobileDataEnabled?.collectAsState(initial = run {
            // Prefer the device Mobile Data toggle state (isDataEnabled) rather than actual connectivity.
            try {
                readMobileToggleState()
            } catch (_: Exception) { currentMobileDataState() }
        }) ?: remember { mutableStateOf(currentMobileDataState()) }
        val signalEnabled by signalState
        val flashlightState = vmLocal?.flashlightEnabled?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
        val flashlightEnabled by flashlightState
        val brightnessEnabled = brightness > 0f

        val phonePermissionGranted = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        // Ensure we request phone-state permission when the fragment becomes active
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPhoneStatePermission {
                    try { simpleTrayViewModel?.refreshStates() } catch (_: Exception) {}
                }
            }
        }

        // If ViewModel is not available, fall back to fragment permission launcher to refresh mobile data state
        LaunchedEffect(Unit) {
            if (vmLocal == null) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    requestPhoneStatePermission {
                        /* fallback refresh */
                    }
                }
            }
        }

        // ViewModel manages system listeners for connectivity, wifi, bluetooth and ringer.
        // Trigger a refresh when the composable is active so the ViewModel can sync initial state.
        DisposableEffect(Unit) {
            simpleTrayViewModel?.refreshStates()
            onDispose { }
        }

        @SuppressLint("MissingPermission")
        // Telephony callbacks are handled in ViewModel; ask VM to refresh states when composable is active
        DisposableEffect(telephonyManager, phonePermissionGranted) {
            simpleTrayViewModel?.refreshStates()
            onDispose { }
        }

        // Torch state is managed in ViewModel; trigger a refresh here if needed
        DisposableEffect(cameraManager, torchCameraId) {
            simpleTrayViewModel?.refreshStates()
            onDispose { }
        }

        // Quick-setting triggers now open system panels/sheets so the user can toggle like a power shade
        fun openWifiPanel() {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(wifiPanelAction)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) {}
        }

        fun openInternetConnectivityPanel() {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(internetPanelAction)
            } else {
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            }
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) {}
        }

        fun openBluetoothSettings() {
            try {
                // Android 15+ requires manual Bluetooth toggle
                if (Build.VERSION.SDK_INT >= 35) {
                    ctx.showShortToast("Android 15+ requires manual Bluetooth toggle")
                }
                
                // Use Settings Panel on Android 10-14 for inline toggle experience
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 35) {
                    Intent("android.settings.panel.action.BLUETOOTH")
                } else {
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to regular Bluetooth settings if panel fails
                try {
                    val fallbackIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(fallbackIntent)
                } catch (_: Exception) {}
            }
        }

        fun openWifiSettingsPage() {
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) {}
        }

        fun openNetworkSettingsPage() {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) {}
        }

        // Collect ViewModel one-shot events and permission requests (when ViewModel available)
        LaunchedEffect(vmLocal) {
            if (vmLocal == null) return@LaunchedEffect
            // permission requests
            launch {
                vmLocal.permissionRequests.collect { req ->
                    when (req) {
                        SimpleTrayPermissionRequest.BluetoothConnect -> {
                            // Show explanation dialog before requesting permission
                            showPermissionExplanationDialog(
                                title = "Bluetooth Permission",
                                message = "This app needs Bluetooth permission to toggle Bluetooth on and off from the quick settings tray.",
                                onContinue = {
                                    if (isAdded) {
                                        val perms = getBluetoothPermissions()
                                        try {
                                            bluetoothPermissionLauncher.launch(perms)
                                        } catch (e: IllegalStateException) {
                                            // Launcher not ready yet, try again after a short delay
                                            view?.postDelayed({
                                                if (isAdded) {
                                                    try {
                                                        bluetoothPermissionLauncher.launch(perms)
                                                    } catch (_: Exception) {
                                                        context?.showShortToast("Permission request failed. Please try again.")
                                                    }
                                                }
                                            }, 100)
                                        }
                                    }
                                }
                            )
                        }
                        SimpleTrayPermissionRequest.Camera -> {
                            // Show explanation dialog before requesting permission
                            showPermissionExplanationDialog(
                                title = "Camera Permission",
                                message = "This app needs camera permission to use the flashlight feature from the quick settings tray.",
                                onContinue = {
                                    if (isAdded) {
                                        try {
                                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        } catch (e: IllegalStateException) {
                                            // Launcher not ready yet, try again after a short delay
                                            view?.postDelayed({
                                                if (isAdded) {
                                                    try {
                                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    } catch (_: Exception) {
                                                        context?.showShortToast("Permission request failed. Please try again.")
                                                    }
                                                }
                                            }, 100)
                                        }
                                    }
                                }
                            )
                        }
                        SimpleTrayPermissionRequest.ReadPhoneState -> {
                            // Show explanation dialog before requesting permission
                            showPermissionExplanationDialog(
                                title = "Phone State Permission",
                                message = "This app needs phone state permission to read and control mobile data settings from the quick settings tray.",
                                onContinue = {
                                    if (isAdded) {
                                        try {
                                            phoneStatePermissionLauncher.launch(phoneStatePermission)
                                        } catch (e: IllegalStateException) {
                                            // Launcher not ready yet, try again after a short delay
                                            view?.postDelayed({
                                                if (isAdded) {
                                                    try {
                                                        phoneStatePermissionLauncher.launch(phoneStatePermission)
                                                    } catch (_: Exception) {
                                                        context?.showShortToast("Permission request failed. Please try again.")
                                                    }
                                                }
                                            }, 100)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            // ui events
            launch {
                vmLocal.uiEvents.collect { ev ->
                    when (ev) {
                        SimpleTrayUiEvent.OpenWifiPanel -> openWifiPanel()
                        SimpleTrayUiEvent.OpenInternetPanel -> openInternetConnectivityPanel()
                        SimpleTrayUiEvent.OpenBluetoothSettings -> openBluetoothSettings()
                    }
                }
            }
        }

        fun toggleBluetoothState(): Boolean {
            // Android 15+ (API 35) restricts BluetoothAdapter.enable()/disable() for third-party apps
            // Open Settings directly instead of attempting deprecated API calls
            if (Build.VERSION.SDK_INT >= 35) {
                openBluetoothSettings()
                return bluetoothEnabled
            }
            
            val btManager = appContext.getSystemService(BluetoothManager::class.java)
            val adapter = btManager?.adapter ?: return bluetoothEnabled
            // Check BLUETOOTH_CONNECT permission on Android S+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // Caller should have requested permission; inform user and bail out
                    ctx.showShortToast("Bluetooth permission required to toggle")
                    return bluetoothEnabled
                }
            }
            return try {
                @Suppress("DEPRECATION")
                if (adapter.isEnabled) {
                    adapter.disable()
                    false
                } else {
                    adapter.enable()
                    true
                }
            } catch (_: SecurityException) {
                // Handle case where permission was revoked while calling
                ctx.showShortToast("Allow Bluetooth permission to toggle")
                bluetoothEnabled
            } catch (_: Exception) {
                bluetoothEnabled
            }
        }

        fun toggleFlashlight(): Boolean {
            val id = torchCameraId ?: run {
                ctx.showShortToast("No flashlight available")
                return flashlightEnabled
            }
            val manager = cameraManager ?: return flashlightEnabled
            return try {
                manager.setTorchMode(id, !flashlightEnabled)
                !flashlightEnabled
            } catch (_: SecurityException) {
                ctx.showShortToast("Allow camera permission to use flashlight")
                flashlightEnabled
            } catch (_: Exception) {
                flashlightEnabled
            }
        }

        fun toggleBrightness(): Boolean {
            val newBrightness = if (brightness > 0f) {
                // Save current brightness to lastBrightnessLevel before turning off
                val currentSystemValue = sliderToSystem(brightness)
                if (currentSystemValue > 0) {
                    prefs.lastBrightnessLevel = currentSystemValue
                }
                // Set brightness to 0 (off)
                0f
            } else {
                // Restore from lastBrightnessLevel (which has the last saved non-zero value)
                val savedPref = prefs.lastBrightnessLevel.coerceIn(1, 255)
                kotlin.math.sqrt((savedPref / 255f).coerceIn(0f, 1f))
            }
            onBrightnessChange(newBrightness)
            return newBrightness > 0f
        }

        fun openBrightnessSettings() {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (_: Exception) {}
        }

        DisposableEffect(pagerState, coroutineScope, validNotifications, composeView) {
            // expose our simple pager state so Activity-level handlers can read/update pages
            composeView.setTag(0xdeadbeef.toInt(), pagerState)
            composeView.setTag(0xcafebabe.toInt(), coroutineScope)
            composeView.setTag(0xabcdef01.toInt(), validNotifications)
            onDispose {
                composeView.setTag(0xdeadbeef.toInt(), null)
                composeView.setTag(0xcafebabe.toInt(), null)
                composeView.setTag(0xabcdef01.toInt(), null)
            }
        }

        // Detect navigation bar height for padding
        val view = LocalView.current
        val density = LocalDensity.current
        var navBarPadding by remember { mutableStateOf(0.dp) }
        var statusBarPadding by remember { mutableStateOf(0.dp) }
        
        LaunchedEffect(view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.rootWindowInsets?.let { insets ->
                    val navBar = insets.getInsets(android.view.WindowInsets.Type.navigationBars())
                    navBarPadding = with(density) { navBar.bottom.toDp() }
                    val statusBar = insets.getInsets(android.view.WindowInsets.Type.statusBars())
                    statusBarPadding = with(density) { statusBar.top.toDp() }
                }
            } else {
                ViewCompat.setOnApplyWindowInsetsListener(view) { _: View, insets: WindowInsetsCompat ->
                    val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    navBarPadding = with(density) { navBar.bottom.toDp() }
                    val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                    statusBarPadding = with(density) { statusBar.top.toDp() }
                    insets
                }
            }
        }

        // Notification font family available to child components (keep font family, sizes are hardcoded elsewhere)
        val notifTypefaceGlobal = remember(prefs.notificationsFont, prefs.getCustomFontPathForContext("notifications")) {
            try {
                prefs.getFontForContext("notifications").getFont(ctx, prefs.getCustomFontPathForContext("notifications"))
            } catch (_: Exception) { null }
        }
        val notifFontFamilyGlobal = remember(notifTypefaceGlobal) { notifTypefaceGlobal?.let { FontFamily(it) } ?: FontFamily.Default }

        // threshold in px to consider a vertical swipe as a page change
        val pageDragThreshold = with(density) { 48.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    val handled = com.github.gezimos.inkos.ui.compose.NavHelper.handleSimpleTrayKeyEvent(
                        keyEvent = keyEvent,
                        keyPressTracker = keyPressTracker,
                        isDpadModeSetter = { isDpadMode = it },
                        focusZone = focusZone,
                        selectedQuickSettingIndex = selectedQuickSettingIndex,
                        onQuickSettingActivate = { index ->
                            when (index) {
                                0 -> simpleTrayViewModel?.openWifiPanel() ?: openWifiPanel()
                                1 -> simpleTrayViewModel?.requestToggleBluetooth() ?: run {
                                    val performToggle = {
                                        toggleBluetoothState()
                                        // refresh local state if fallback
                                        Unit
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                        requestBluetoothPermission(performToggle)
                                    } else {
                                        performToggle()
                                    }
                                }
                                2 -> {
                                    simpleTrayViewModel?.openInternetPanel() ?: openInternetConnectivityPanel()
                                    val refreshState = {
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(600)
                                            simpleTrayViewModel?.refreshStates() ?: run {
                                                // fallback: update local signal state
                                                // signalEnabled = currentMobileDataState()
                                            }
                                        }
                                    }
                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                        refreshState()
                                    } else {
                                        requestPhoneStatePermission { refreshState() }
                                    }
                                }
                                3 -> {
                                    if (simpleTrayViewModel != null) {
                                        simpleTrayViewModel.requestToggleFlashlight()
                                    } else {
                                        val toggle = {
                                            (flashlightState as? androidx.compose.runtime.MutableState<Boolean>)?.value = toggleFlashlight()
                                        }
                                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            toggle()
                                        } else {
                                            requestCameraPermission(toggle)
                                        }
                                    }
                                }
                                4 -> {
                                    toggleBrightness()
                                }
                            }
                        },
                        onBrightnessAdjust = { delta ->
                            onBrightnessChange((brightness + delta).coerceIn(0f, 1f))
                        },
                        onClearAll = {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                            validNotifications.forEach { (pkg, notif) ->
                                try {
                                    val isMedia = notif.category == android.app.Notification.CATEGORY_TRANSPORT
                                    if (isMedia) {
                                        // For media notifications, stop the media session
                                        AudioWidgetHelper.getInstance(ctx).stopMedia()
                                    } else {
                                        // For regular notifications, dismiss from system tray
                                        if (notif.notificationKey != null) {
                                            NotificationService.dismissNotification(notif.notificationKey)
                                        }
                                        // Remove from conversation notifications
                                        NotificationManager.getInstance(ctx).removeConversationNotification(pkg, notif.conversationId)
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        selectedNotificationIndex = selectedNotificationIndex,
                        notificationsOnPageSize = displayNotifications.size,
                        pageSize = pageSize,
                        currentPage = pagerState.currentPage,
                        totalPages = pagerState.pageCount,
                        onNextPage = {
                            coroutineScope.launch {
                                if (pagerState.currentPage < pagerState.pageCount - 1) {
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                    vibratePaging(ctx)
                                }
                            }
                        },
                        onPreviousPage = {
                            coroutineScope.launch {
                                if (pagerState.currentPage > 0) {
                                    pagerState.scrollToPage(pagerState.currentPage - 1)
                                    vibratePaging(ctx)
                                }
                            }
                        },
                        onNotificationClick = { index ->
                            displayNotifications.getOrNull(index)?.let { (pkg, notif) ->
                                NotificationManager.getInstance(ctx)
                                    .openNotification(pkg, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                            }
                        },
                        onNotificationLongClick = { index ->
                            displayNotifications.getOrNull(index)?.let { (pkg, notif) ->
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                
                                val isMedia = notif.category == android.app.Notification.CATEGORY_TRANSPORT
                                if (isMedia) {
                                    // For media notifications, stop the media session
                                    AudioWidgetHelper.getInstance(ctx).stopMedia()
                                } else {
                                    // For regular notifications, dismiss from system tray
                                    if (notif.notificationKey != null) {
                                        NotificationService.dismissNotification(notif.notificationKey)
                                    }
                                    // Also remove from conversation notifications
                                    NotificationManager.getInstance(ctx).removeConversationNotification(pkg, notif.conversationId)
                                }
                            }
                        },
                        selectedBottomNavIndex = selectedBottomNavIndex,
                        onBottomNavActivate = { index ->
                            when (index) {
                                0 -> try { findNavController().popBackStack() } catch (_: Exception) {}
                                1 -> try { findNavController().navigate(R.id.settingsFragment) } catch (_: Exception) {}
                            }
                        },
                        bottomNavEnabled = prefsLocal.enableBottomNav
                    )
                    handled
                }
                .gestureHelper(onSwipeUp = { 
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    findNavController().popBackStack() 
                })
                .background(Theme.colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = statusBarPadding + 24.dp,
                        bottom = navBarPadding + 80.dp
                    )
                    .pointerInput(pagerState.currentPage, isDpadMode) {
                        // detect vertical swipes to change pages
                        var dragSum = 0f
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                isDpadModeState.value = false
                                dragSum += dragAmount
                            },
                            onDragEnd = {
                                if (dragSum < -pageDragThreshold) {
                                    // swipe up -> next page
                                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                                        pagerState.scrollToPage(pagerState.currentPage + 1)
                                        vibratePaging(ctx)
                                    }
                                } else if (dragSum > pageDragThreshold) {
                                    // swipe down -> previous page
                                    if (pagerState.currentPage > 0) {
                                        pagerState.scrollToPage(pagerState.currentPage - 1)
                                        vibratePaging(ctx)
                                    }
                                }
                                dragSum = 0f
                            }
                        )
                    }
            ) {
                // Quick Settings
                QuickSettingsRow(
                    isDpadMode = isDpadMode,
                    focusZone = focusZone.value,
                    selectedIndex = selectedQuickSettingIndex.intValue,
                    wifiEnabled = wifiEnabled,
                    onWifiToggle = { simpleTrayViewModel?.openWifiPanel() ?: openWifiPanel() },
                    onWifiLongPress = { openWifiSettingsPage() },
                        bluetoothEnabled = bluetoothEnabled,
                        onBluetoothToggle = {
                            if (simpleTrayViewModel != null) {
                                simpleTrayViewModel.requestToggleBluetooth()
                            } else {
                                val performToggle = {
                                    toggleBluetoothState()
                                    // bluetoothEnabled will be updated by receivers
                                    Unit
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    requestBluetoothPermission(performToggle)
                                } else {
                                    performToggle()
                                }
                            }
                        },
                        onBluetoothLongPress = { openBluetoothSettings() },
                        signalEnabled = signalEnabled,
                        onSignalToggle = {
                            if (simpleTrayViewModel != null) {
                                    simpleTrayViewModel.openInternetPanel()
                                    // Start polling the device toggle state and refresh ViewModel until it changes or timeout.
                                    coroutineScope.launch {
                                        val initial = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }

                                        val attempts = 15
                                        repeat(attempts) {
                                            kotlinx.coroutines.delay(300)
                                            val now = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }

                                            if (now != initial) {
                                                // trigger a full refresh so ViewModel state flows update
                                                try { simpleTrayViewModel.refreshStates() } catch (_: Exception) {}
                                                return@repeat
                                            }
                                        }
                                    }
                                } else {
                                    openInternetConnectivityPanel()
                                    // Poll locally when ViewModel is not available
                                    coroutineScope.launch {
                                        val initial = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }
                                        val attempts = 15
                                        repeat(attempts) {
                                            kotlinx.coroutines.delay(300)
                                            val now = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }
                                            if (now != initial) {
                                                (signalState as? androidx.compose.runtime.MutableState<Boolean>)?.value = now
                                                return@repeat
                                            }
                                        }
                                    }
                                }
                        },
                            onSignalLongPress = { openNetworkSettingsPage() },
                        flashlightEnabled = flashlightEnabled,
                        onFlashlightToggle = {
                            if (simpleTrayViewModel != null) {
                                simpleTrayViewModel.requestToggleFlashlight()
                            } else {
                                val toggle = {
                                    (flashlightState as? androidx.compose.runtime.MutableState<Boolean>)?.value = toggleFlashlight()
                                }
                                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    toggle()
                                } else {
                                    requestCameraPermission(toggle)
                                }
                            }
                        },
                        brightnessEnabled = brightnessEnabled,
                        onBrightnessToggle = { toggleBrightness() },
                        onBrightnessLongPress = { openBrightnessSettings() }
                    )
                
                // Brightness Slider (top 24dp, bottom 12dp)
                Box(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                    BrightnessSlider(
                        brightness = brightness,
                        onBrightnessChange = onBrightnessChange,
                        isDpadMode = isDpadModeState,
                        isFocused = isDpadMode && focusZone.value == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.BRIGHTNESS_SLIDER
                    )
                }
                
                // Clear All row - only show when there are notifications
                if (validNotifications.isNotEmpty()) {
                    ClearAllRow(
                        isFocused = isDpadMode && focusZone.value == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.CLEAR_ALL,
                        total = validNotifications.size,
                        onClearAll = {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                            // Clear all conversation notifications
                            validNotifications.forEach { (pkg, notif) ->
                                try {
                                    // Dismiss notification from system tray (mark as read/snooze)
                                    if (notif.notificationKey != null) {
                                        NotificationService.dismissNotification(notif.notificationKey)
                                    }
                                    // Remove from conversation notifications
                                    NotificationManager.getInstance(requireContext()).removeConversationNotification(pkg, notif.conversationId)
                                } catch (_: Exception) {}
                            }
                        },
                        fontFamily = notifFontFamilyGlobal
                    )
                }
                
                // Merged notification cards with paging (dynamic per page based on prefs)
                if (validNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_foreground),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Theme.colors.text),
                                modifier = Modifier
                                    .size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(id = R.string.no_notifications),
                                color = Theme.colors.text,
                                fontSize = 18.sp,
                                fontFamily = notifFontFamilyGlobal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    MergedNotificationCards(
                        notifications = displayNotifications,
                        isDpadMode = isDpadMode,
                        focusZone = focusZone.value,
                        selectedNotificationIndex = selectedNotificationIndex.intValue
                    )
                }
            }
            
            // Bottom navigation bar
            BottomNavigationBar(
                isDpadMode = isDpadMode,
                focusZone = focusZone.value,
                selectedIndex = selectedBottomNavIndex.intValue,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarPadding),
                enabled = prefsLocal.enableBottomNav,
                totalNotifications = validNotifications.size,
                onHomeClick = {
                    try {
                        findNavController().popBackStack()
                    } catch (_: Exception) {}
                },
                onSettingsClick = {
                    try {
                        // Open the Android system Settings app instead of the in-app Settings
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            // Fallback: navigate to in-app settings if system Settings can't be opened
                            findNavController().navigate(R.id.settingsFragment)
                        } catch (_: Exception) {}
                    }
                },
                centerContent = {}
            )
        }
    }
    
    @Composable
    fun QuickSettingsRow(
        isDpadMode: Boolean,
        focusZone: com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone,
        selectedIndex: Int,
        wifiEnabled: Boolean,
        onWifiToggle: () -> Unit,
        onWifiLongPress: (() -> Unit)? = null,
        bluetoothEnabled: Boolean,
        onBluetoothToggle: () -> Unit,
        onBluetoothLongPress: (() -> Unit)? = null,
        signalEnabled: Boolean,
        onSignalToggle: () -> Unit,
        onSignalLongPress: (() -> Unit)? = null,
        flashlightEnabled: Boolean,
        onFlashlightToggle: () -> Unit,
        brightnessEnabled: Boolean,
        onBrightnessToggle: () -> Unit,
        onBrightnessLongPress: (() -> Unit)? = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickSettingButton(
                icon = if (wifiEnabled) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                enabled = wifiEnabled,
                onClick = onWifiToggle,
                onLongClick = onWifiLongPress,
                isFocused = isDpadMode && focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 0
            )
            QuickSettingButton(
                icon = if (bluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                enabled = bluetoothEnabled,
                onClick = onBluetoothToggle,
                onLongClick = onBluetoothLongPress,
                isFocused = isDpadMode && focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 1
            )
            QuickSettingButton(
                icon = if (signalEnabled) Icons.Rounded.SignalCellularAlt else Icons.Rounded.SignalCellularOff,
                enabled = signalEnabled,
                onClick = onSignalToggle,
                onLongClick = onSignalLongPress,
                isFocused = isDpadMode && focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 2
            )
            QuickSettingButton(
                icon = if (flashlightEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                enabled = flashlightEnabled,
                onClick = onFlashlightToggle,
                isFocused = isDpadMode && focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 3
            )
            QuickSettingButton(
                icon = if (brightnessEnabled) Icons.Rounded.BrightnessHigh else Icons.Rounded.BrightnessLow,
                enabled = brightnessEnabled,
                onClick = onBrightnessToggle,
                onLongClick = onBrightnessLongPress,
                isFocused = isDpadMode && focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 4
            )

        }
    }
    
    @Composable
    fun QuickSettingButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        enabled: Boolean,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null,
        isFocused: Boolean = false
    ) {
        // Wrap handlers to trigger haptic feedback via VibrationHelper
        val handleClick: () -> Unit = {
            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
            try { onClick() } catch (_: Exception) {}
        }
        val handleLong: (() -> Unit)? = if (onLongClick != null) {
            {
                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                try { onLongClick() } catch (_: Exception) {}
            }
        } else null
        val bgColor = if (enabled) Theme.colors.text else Theme.colors.background
        val iconTint = if (enabled) Theme.colors.background else Theme.colors.text
        val borderColor = Theme.colors.text
        val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
        val density = LocalDensity.current
        val textIslandsShape = prefs.textIslandsShape
        
        // Calculate button shape based on textIslandsShape preference
        val buttonShape = remember(textIslandsShape) {
            when (textIslandsShape) {
                0 -> CircleShape // Pill (fully rounded)
                1 -> RoundedCornerShape(8.dp) // Rounded
                else -> RoundedCornerShape(0.dp) // Square
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (isFocused) {
                        Modifier.drawBehind {
                            val cornerRadius = when (textIslandsShape) {
                                0 -> 28.dp.toPx() // Pill (fully rounded, same as circle radius)
                                1 -> with(density) { 8.dp.toPx() } // Rounded
                                else -> 0f // Square
                            }
                            drawRoundRect(
                                color = highlightColor,
                                topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                                size = Size(56.dp.toPx(), 56.dp.toPx()),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    } else Modifier
                )
                .combinedClickable(
                    onClick = handleClick,
                    onLongClick = handleLong,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .background(
                    color = bgColor,
                    shape = buttonShape
                )
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = buttonShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
    
    @Composable
    fun BrightnessSlider(
        brightness: Float,
        onBrightnessChange: (Float) -> Unit,
        isDpadMode: androidx.compose.runtime.MutableState<Boolean>,
        isFocused: Boolean = false
    ) {
        val ctx = LocalContext.current
        // Helper mapping: slider (0..1) -> system brightness (0..255)
        fun sliderToSystem(s: Float): Int {
            val v = (s.coerceIn(0f, 1f) * s.coerceIn(0f, 1f) * 255f).toInt()
            return v.coerceIn(0, 255)
        }
        fun systemToSlider(sys: Int): Float {
            val norm = (sys.coerceIn(0, 255) / 255f).coerceIn(0f, 1f)
            return kotlin.math.sqrt(norm)
        }

        // initial brightness from prefs (which stores the user's intended value including 0)
        val initialSystemBrightness = remember {
            prefs.brightnessLevel
        }

        var internalBrightness by remember { 
            mutableFloatStateOf(
                if (initialSystemBrightness == 0) 0f else systemToSlider(initialSystemBrightness)
            )
        }

        // Don't automatically request write settings permission on first composition
        // Only request it when user actually tries to adjust brightness

        // Do NOT propagate internal changes automatically — only notify parent on user interaction

        // Sync when parent changes (debounced by small threshold)
        LaunchedEffect(brightness) {
            val newValue = brightness.coerceIn(0f, 1f)
            if (kotlin.math.abs(newValue - internalBrightness) > 0.02f) {
                internalBrightness = newValue
            }
        }

        // Observe system brightness changes so external changes update the slider
        DisposableEffect(ctx) {
            val resolver = ctx.contentResolver
            val observer = object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    try {
                        // Read actual system brightness
                        val systemBrightness = Settings.System.getInt(
                            resolver,
                            Settings.System.SCREEN_BRIGHTNESS
                        )
                        
                        // Check if user intended zero (system shows 1 but prefs shows 0)
                        val userIntended = if (systemBrightness == 1 && prefs.brightnessLevel == 0) {
                            0
                        } else {
                            // External change - update brightness via MainViewModel to keep state in sync
                            val activity = ctx as? androidx.activity.ComponentActivity
                            val vm = activity?.let { androidx.lifecycle.ViewModelProvider(it)[com.github.gezimos.inkos.MainViewModel::class.java] }
                            vm?.setBrightnessLevel(systemBrightness)
                            systemBrightness
                        }
                        
                        val slider = if (userIntended == 0) {
                            0f
                        } else {
                            systemToSlider(userIntended)
                        }
                        internalBrightness = slider
                        // notify parent so the external change updates the fragment state
                        try { onBrightnessChange(internalBrightness) } catch (_: Exception) {}
                    } catch (_: Exception) {}
                }
            }
            try {
                resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, observer)
            } catch (_: Exception) {}
            onDispose {
                try { resolver.unregisterContentObserver(observer) } catch (_: Exception) {}
            }
        }

        BrightnessSliderBar(
            internalBrightness = internalBrightness,
            onBrightnessChanged = { newBrightness ->
                internalBrightness = newBrightness
                onBrightnessChange(newBrightness)
            },
            ctx = ctx,
            prefs = prefs,
            sliderToSystem = ::sliderToSystem,
            isDpadMode = isDpadMode,
            isFocused = isFocused
        )
    }
    
    @Composable
    private fun BrightnessSliderBar(
        internalBrightness: Float,
        onBrightnessChanged: (Float) -> Unit,
        ctx: Context,
        prefs: Prefs,
        sliderToSystem: (Float) -> Int,
        isDpadMode: androidx.compose.runtime.MutableState<Boolean>,
        isFocused: Boolean = false
    ) {
        var localLastWriteTime by remember { mutableLongStateOf(0L) }
        var localBrightness by remember { mutableFloatStateOf(internalBrightness) }
        
        // Sync with parent prop when it changes externally
        LaunchedEffect(internalBrightness) {
            if (kotlin.math.abs(internalBrightness - localBrightness) > 0.01f) {
                localBrightness = internalBrightness
            }
        }
        
        val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
        val density = LocalDensity.current
        val textIslandsShape = prefs.textIslandsShape
        
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            val totalDp = maxWidth

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // interactive bar area
                Box(
                    modifier = Modifier
                        .width(totalDp)
                        .height(32.dp)
                        .then(
                            if (isFocused) {
                                Modifier.drawBehind {
                                    val cornerRadius = when (textIslandsShape) {
                                        0 -> size.height / 2f // Pill (fully rounded)
                                        1 -> with(density) { 8.dp.toPx() } // Rounded
                                        else -> 0f // Square
                                    }
                                    drawRoundRect(
                                        color = highlightColor,
                                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                    )
                                }
                            } else Modifier
                        )
                ) {
                    // outer border (stroke 2dp)
                    // Calculate slider shape based on textIslandsShape preference
                    val sliderShape = remember(textIslandsShape) {
                        when (textIslandsShape) {
                            0 -> RoundedCornerShape(18.dp) // Pill (keep as is)
                            1 -> RoundedCornerShape(8.dp) // Rounded
                            else -> RoundedCornerShape(0.dp) // Square
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 2.dp,
                                color = Theme.colors.text,
                                shape = sliderShape
                            )
                    ) {
                        val totalBarHeight = 32.dp
                        val innerTrackHeight = 20.dp
                        val innerPadding = (totalBarHeight - innerTrackHeight) / 2
                        // Calculate inner track shape based on textIslandsShape preference
                        val innerTrackShape = remember(textIslandsShape) {
                            when (textIslandsShape) {
                                0 -> RoundedCornerShape(12.dp) // Pill (keep as is)
                                1 -> RoundedCornerShape(8.dp) // Rounded
                                else -> RoundedCornerShape(0.dp) // Square
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(color = Color.Transparent, shape = innerTrackShape)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        isDpadMode.value = false
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        var frac = (offset.x / w).coerceIn(0f, 1f)
                                        // Larger zero threshold to make tapping the left-most area more reliable
                                        if (frac <= 0.05f) frac = 0f
                                        
                                        // Update local UI state immediately
                                        localBrightness = frac
                                        
                                        val intVal = if (frac == 0f) 0 else sliderToSystem(frac)
                                        // Check permission before setting brightness
                                        if (Settings.System.canWrite(ctx)) {
                                            BrightnessHelper.setBrightness(ctx, prefs, intVal)
                                            localLastWriteTime = System.currentTimeMillis()
                                        }
                                        
                                        // Notify parent (which will check and request permission if needed)
                                        onBrightnessChanged(frac)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, _ ->
                                            isDpadMode.value = false
                                            val w = size.width.toFloat().coerceAtLeast(1f)
                                            var frac = (change.position.x / w).coerceIn(0f, 1f)
                                            // Small threshold for zero to avoid accidental off
                                            if (frac <= 0.03f) frac = 0f
                                            
                                            // Update local UI state immediately - this makes the fill bar responsive!
                                            localBrightness = frac
                                            
                                            val intVal = if (frac == 0f) 0 else sliderToSystem(frac)
                                            // Throttle system writes but update UI every frame
                                            // Check permission before setting brightness
                                            if (Settings.System.canWrite(ctx)) {
                                                val now = System.currentTimeMillis()
                                                if (now - localLastWriteTime >= 80L || intVal == 0) {
                                                    if (BrightnessHelper.setBrightness(ctx, prefs, intVal)) {
                                                        localLastWriteTime = now
                                                    }
                                                }
                                            }
                                            
                                            // Notify parent (which will check and request permission if needed)
                                            onBrightnessChanged(frac)
                                        },
                                        onDragEnd = {
                                            // Ensure final value is written when user lifts finger (bypass throttle)
                                            val finalInt = if (localBrightness == 0f) 0 else sliderToSystem(localBrightness)
                                            // Check permission before setting brightness
                                            if (Settings.System.canWrite(ctx)) {
                                                BrightnessHelper.setBrightness(ctx, prefs, finalInt)
                                                localLastWriteTime = System.currentTimeMillis()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // filled portion inside inner track
                            val innerWidth = totalDp - (innerPadding * 2)
                            
                            if (localBrightness <= 0f) {
                                // show solid circular indicator at left when empty
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = Theme.colors.text,
                                            shape = CircleShape
                                        )
                                        .align(Alignment.CenterStart)
                                )
                            } else {
                                // Show filled bar with minimum width of 20dp (circle size)
                                val fillDp = (innerWidth * localBrightness).coerceAtLeast(20.dp)
                                // Calculate filled bar shape based on textIslandsShape preference
                                val filledBarShape = remember(textIslandsShape) {
                                    when (textIslandsShape) {
                                        0 -> RoundedCornerShape(12.dp) // Pill (keep as is)
                                        1 -> RoundedCornerShape(4.dp) // Rounded (decreased by 2 more dp)
                                        else -> RoundedCornerShape(0.dp) // Square
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .width(fillDp)
                                        .background(
                                            color = Theme.colors.text,
                                            shape = filledBarShape
                                        )
                                        .align(Alignment.CenterStart)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun BottomNavigationBar(
        isDpadMode: Boolean,
        focusZone: com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone,
        selectedIndex: Int,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        totalNotifications: Int,
        onHomeClick: () -> Unit,
        onSettingsClick: () -> Unit,
        centerContent: (@Composable () -> Unit)? = null
    ) {
        if (!enabled) return
        
        val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
        val density = LocalDensity.current
        val textIslandsShape = prefs.textIslandsShape
        
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isDpadMode && focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.BOTTOM_NAV && selectedIndex == 0) {
                            Modifier.drawBehind {
                                val cornerRadius = when (textIslandsShape) {
                                    0 -> 24.dp.toPx() // Pill (fully rounded, same as circle radius)
                                    1 -> with(density) { 8.dp.toPx() } // Rounded
                                    else -> 0f // Square
                                }
                                drawRoundRect(
                                    color = highlightColor,
                                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                                    size = Size(48.dp.toPx(), 48.dp.toPx()),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )
                            }
                        } else Modifier
                    )
                    .clickable(onClick = onHomeClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Home,
                    contentDescription = "Home",
                    tint = Theme.colors.text,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Center area: occupies remaining space and centers the provided content
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (centerContent != null) {
                    centerContent()
                } else if (totalNotifications > 0) {
                    Text(
                        text = "$totalNotifications",
                        color = Theme.colors.text,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isDpadMode && focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.BOTTOM_NAV && selectedIndex == 1) {
                            Modifier.drawBehind {
                                val cornerRadius = when (textIslandsShape) {
                                    0 -> 24.dp.toPx() // Pill (fully rounded, same as circle radius)
                                    1 -> with(density) { 8.dp.toPx() } // Rounded
                                    else -> 0f // Square
                                }
                                drawRoundRect(
                                    color = highlightColor,
                                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                                    size = Size(48.dp.toPx(), 48.dp.toPx()),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )
                            }
                        } else Modifier
                    )
                    .clickable(onClick = onSettingsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Theme.colors.text,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
    
    @Composable
    fun ClearAllRow(
        isFocused: Boolean = false,
        total: Int? = null,
        onClearAll: () -> Unit,
        fontFamily: FontFamily = FontFamily.Default
    ) {
        val textColor = Theme.colors.text
        val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
        val density = LocalDensity.current
        val textIslandsShape = prefs.textIslandsShape
        
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clear All",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily,
                modifier = Modifier
                    .then(
                        if (isFocused) {
                            Modifier.drawBehind {
                                val cornerRadius = when (textIslandsShape) {
                                    0 -> size.height / 2f // Pill (fully rounded)
                                    1 -> with(density) { 8.dp.toPx() } // Rounded
                                    else -> 0f // Square
                                }
                                drawRoundRect(
                                    color = highlightColor,
                                    topLeft = Offset(-8.dp.toPx(), -4.dp.toPx()),
                                    size = Size(size.width + 16.dp.toPx(), size.height + 8.dp.toPx()),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )
                            }
                        } else Modifier
                    )
                    .clickable(onClick = onClearAll)
            )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // show only the total number of notifications
                    if (total != null) {
                        Text(
                            text = "$total",
                            color = Theme.colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily
                        )
                    }
                }
        }
    }
    
    @Composable
    fun MergedNotificationCards(
        notifications: List<Pair<String, NotificationManager.ConversationNotification>>,
        isDpadMode: Boolean,
        focusZone: com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone,
        selectedNotificationIndex: Int
    ) {
        // Calculate notification box shape based on textIslandsShape preference
        // Pill: keep as is (12.dp), Rounded: 8.dp, Square: 0.dp
        val notificationBoxShape = remember(prefs.textIslandsShape) {
            when (prefs.textIslandsShape) {
                0 -> RoundedCornerShape(12.dp) // Pill (keep as is)
                1 -> RoundedCornerShape(8.dp) // Rounded
                else -> RoundedCornerShape(0.dp) // Square
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp)
                .border(
                    width = 2.dp,
                    color = Theme.colors.text,
                    shape = notificationBoxShape
                )
        ) {
            notifications.forEachIndexed { index, (packageName, notif) ->
                key(notif.conversationId) {
                    NotificationCardItem(
                        packageName = packageName,
                        notif = notif,
                        showTopDivider = index > 0,
                        isDpadMode = isDpadMode,
                        isFocused = focusZone == com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone.NOTIFICATIONS && index == selectedNotificationIndex
                    )
                }
            }
        }
    }

    @Composable
    fun NotificationCardItem(
        packageName: String,
        notif: NotificationManager.ConversationNotification,
        showTopDivider: Boolean,
        isDpadMode: Boolean = false,
        isFocused: Boolean = false
    ) {
        val context = requireContext()
        
        // Helper function to open notification settings for a package
        fun openNotificationSettingsForPackage(context: Context, packageName: String) {
            try {
                // Use ACTION_APP_NOTIFICATION_SETTINGS for Android 8.0+ to open notification settings for specific app
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } else {
                    // Fallback for older Android versions: open app info page
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {
                // If notification settings intent fails, try app info as fallback
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
        val appLabelInfo = remember(packageName) {
            try {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(packageName, 0)
                ).toString()
            } catch (_: Exception) {
                packageName
            }
        }

        val title: String = remember(notif.conversationTitle, notif.sender, prefs.showNotificationGroupName) {
            // Respect showNotificationGroupName preference - only use conversationTitle if preference is enabled
            when {
                !notif.sender.isNullOrBlank() -> notif.sender
                prefs.showNotificationGroupName && !notif.conversationTitle.isNullOrBlank() -> notif.conversationTitle
                else -> null
            } ?: appLabelInfo
        }

        // Check if this is a media notification
        val isMediaNotification = notif.category == android.app.Notification.CATEGORY_TRANSPORT

        // Get MediaController for media notifications
        // All media notifications are now fake notifications from AudioWidgetHelper
        val mediaController = remember(notif.notificationKey, packageName, isMediaNotification) {
            if (isMediaNotification) {
                // Get controller from AudioWidgetHelper (all media notifications are fake now)
                try {
                    val audioHelper = AudioWidgetHelper.getInstance(context)
                    val mediaPlayer = audioHelper.getCurrentMediaPlayer()
                    if (mediaPlayer?.packageName == packageName) {
                        mediaPlayer.controller
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
        
        // Make isPlaying reactive to AudioWidgetHelper state changes
        var isPlaying by remember { mutableStateOf(false) }
        
        LaunchedEffect(isMediaNotification, packageName) {
            if (isMediaNotification) {
                // Observe AudioWidgetHelper's media player state for real-time updates
                val audioHelper = AudioWidgetHelper.getInstance(context)
                audioHelper.mediaPlayerState.collect { mediaPlayer ->
                    isPlaying = mediaPlayer?.packageName == packageName && mediaPlayer.isPlaying
                }
            }
        }
        
        // Get media time info (elapsed/total or remaining) - update periodically
        var mediaTimeString by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(mediaController, isMediaNotification) {
            if (isMediaNotification && mediaController != null) {
                while (true) {
                    try {
                        val playbackState = mediaController.playbackState
                        val position = playbackState?.position ?: 0L
                        val metadata = mediaController.metadata
                        val duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                        
                        if (duration > 0) {
                            val elapsed = position / 1000 // Convert to seconds
                            val total = duration / 1000
                            val remaining = (total - elapsed).coerceAtLeast(0)
                            
                            // Format as MM:SS
                            fun formatTime(seconds: Long): String {
                                val mins = seconds / 60
                                val secs = seconds % 60
                                return String.format("%d:%02d", mins, secs)
                            }
                            
                            // Show remaining time if available, otherwise elapsed/total
                            mediaTimeString = if (remaining > 0) {
                                "-${formatTime(remaining)}"
                            } else {
                                "${formatTime(elapsed)}/${formatTime(total)}"
                            }
                        } else {
                            mediaTimeString = null
                        }
                    } catch (_: Exception) {
                        mediaTimeString = null
                    }
                    
                    // Update every second if playing, every 2 seconds if paused
                    kotlinx.coroutines.delay(if (isPlaying) 1000L else 2000L)
                }
            } else {
                mediaTimeString = null
            }
        }

        // Use media time if available, otherwise use notification timestamp
        val defaultTimeString = remember(notif.timestamp) {
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            timeFormat.format(notif.timestamp)
        }
        val timeString = if (isMediaNotification && mediaTimeString != null) {
            mediaTimeString ?: defaultTimeString
        } else {
            defaultTimeString
        }

        // Backend now provides category-specific fallbacks, so we don't need generic fallback here
        val message: String? = remember(notif.message) {
            notif.message?.takeIf { it.isNotBlank() }
        }

        // Notification font family from prefs (keep font family, retain hardcoded sizes)
        val notifTypeface = remember(prefs.notificationsFont, prefs.getCustomFontPathForContext("notifications")) {
            try {
                prefs.getFontForContext("notifications").getFont(context, prefs.getCustomFontPathForContext("notifications"))
            } catch (_: Exception) { null }
        }
        val notifFontFamily = remember(notifTypeface) { notifTypeface?.let { FontFamily(it) } ?: FontFamily.Default }
        val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
        val density = LocalDensity.current
        val textIslandsShape = prefs.textIslandsShape

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isDpadMode && isFocused) {
                        Modifier.drawBehind {
                            val cornerRadius = when (textIslandsShape) {
                                0 -> with(density) { 16.dp.toPx() } // Pill (bring back original less rounded)
                                1 -> with(density) { 8.dp.toPx() } // Rounded
                                else -> 0f // Square
                            }
                            drawRoundRect(
                                color = highlightColor,
                                topLeft = Offset(-8.dp.toPx(), -8.dp.toPx()),
                                size = Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx()),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    } else Modifier
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            NotificationManager.getInstance(context)
                                .openNotification(packageName, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                        },
                        onLongPress = {
                            try {
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                
                                if (isMediaNotification) {
                                    // For media notifications, stop the media session
                                    AudioWidgetHelper.getInstance(context).stopMedia()
                                } else {
                                    // For regular notifications, dismiss from system tray
                                    if (notif.notificationKey != null) {
                                        NotificationService.dismissNotification(notif.notificationKey)
                                    }
                                    // Also remove from conversation notifications
                                    NotificationManager.getInstance(context).removeConversationNotification(packageName, notif.conversationId)
                                }
                            } catch (_: Exception) {
                            }
                        }
                    )
                }
        ) {
            if (showTopDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Theme.colors.text)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                // Title on its own row with play/pause button on the right for media
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        color = Theme.colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = notifFontFamily,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Right side: Media control buttons (for media) + Settings icon (for all non-synthetic apps)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Media control buttons for media notifications
                        if (isMediaNotification && mediaController != null) {
                            // Previous button
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Theme.colors.text,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        try {
                                            mediaController.transportControls.skipToPrevious()
                                        } catch (_: Exception) {}
                                    }
                            )
                            
                            // Play/Pause button
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Theme.colors.text,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        try {
                                            if (isPlaying) {
                                                mediaController.transportControls.pause()
                                            } else {
                                                mediaController.transportControls.play()
                                            }
                                        } catch (_: Exception) {}
                                    }
                            )
                            
                            // Next button
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = Theme.colors.text,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        try {
                                            mediaController.transportControls.skipToNext()
                                        } catch (_: Exception) {}
                                    }
                            )
                        }
                        
                        // Settings icon - opens notification settings for this package
                        val isSyntheticPackage = packageName == "com.inkos.internal.app_drawer" ||
                                packageName == "com.inkos.internal.notifications" ||
                                packageName == "com.inkos.internal.search"
                        if (!isSyntheticPackage) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Notification settings",
                                tint = Theme.colors.text,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        try {
                                            openNotificationSettingsForPackage(context, packageName)
                                        } catch (_: Exception) {}
                                    }
                            )
                        }
                    }
                }

                // App name and time under the title (left-aligned)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = appLabelInfo,
                            color = Theme.colors.text,
                            fontSize = 12.sp,
                            fontFamily = notifFontFamily
                        )
                        Text(
                            text = "•",
                            color = Theme.colors.text,
                            fontSize = 12.sp,
                            fontFamily = notifFontFamily
                        )
                        Text(
                            text = timeString,
                            color = Theme.colors.text,
                            fontSize = 12.sp,
                            fontFamily = notifFontFamily
                        )
                    }
                }

                // Message (single-line) below app/time
                // Always show message row to maintain consistent notification height
                Text(
                    text = message ?: "",
                    color = Theme.colors.text,
                    fontSize = 14.sp,
                    fontFamily = notifFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Helper function for vibration - needs to be accessible from composable contexts
private fun vibratePaging(context: Context) {
    VibrationHelper.trigger(VibrationHelper.Effect.PAGE)
}
