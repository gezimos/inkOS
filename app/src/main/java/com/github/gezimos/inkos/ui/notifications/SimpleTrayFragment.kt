package com.github.gezimos.inkos.ui.notifications

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.EditModeHelper
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.services.NotificationService
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost
import com.github.gezimos.inkos.ui.dialogs.SheetTitle
import kotlinx.coroutines.launch

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class SimpleTrayFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var vibrator: Vibrator
    private var simpleTrayViewModel: SimpleTrayViewModel? = null
    private var dismissKeyDownTime: Long = 0L
    private val LONG_PRESS_THRESHOLD_MS = 700L
    private var wasPaused = false
    private var focusRestoreTriggerState: androidx.compose.runtime.MutableState<Int>? = null

    private val writeSettingsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* User returned from settings */ }

    private var permissionExplanationDialog: ComposeBottomSheetHost? = null
    private fun showPermissionExplanationDialog(
        title: String,
        message: String,
        onContinue: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        requireContext()
        permissionExplanationDialog?.dismiss()
        val host = ComposeBottomSheetHost(requireActivity())
        permissionExplanationDialog = host

        host.show {
            val screenScale = rememberScreenScale()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))
            ) {
                SheetTitle(title)
                Text(text = message, style = SettingsTheme.typography.item, color = Theme.colors.text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onCancel != null) {
                        TextButton(onClick = {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onCancel()
                            host.dismiss()
                        }) {
                            Text(text = getString(android.R.string.cancel), style = SettingsTheme.typography.button, color = Theme.colors.text)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        host.dismiss()
                        onContinue()
                    }) {
                        Text(text = getString(android.R.string.ok), style = SettingsTheme.typography.button, color = Theme.colors.text)
                    }
                }
            }
        }
    }

    private fun requestWriteSettingsPermission(showExplanation: Boolean = true) {
        val ctx = requireContext()
        if (!Settings.System.canWrite(ctx)) {
            if (showExplanation) {
                showPermissionExplanationDialog(
                    title = getString(R.string.perm_write_settings_title),
                    message = "This app needs permission to modify system settings to control screen brightness. " +
                        "You will be taken to Android settings to grant this permission.",
                    onContinue = {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            intent.data = "package:${ctx.packageName}".toUri()
                            writeSettingsPermissionLauncher.launch(intent)
                        } catch (e: Exception) {
                            Log.w("SimpleTray", "Failed to launch write settings permission", e)
                        }
                    }
                )
            } else {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = "package:${ctx.packageName}".toUri()
                    writeSettingsPermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.w("SimpleTray", "Failed to launch write settings permission", e)
                }
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
        } else true
        if (granted) {
            pendingBluetoothAction?.invoke()
            simpleTrayViewModel?.onPermissionResult(SimpleTrayPermissionRequest.BluetoothConnect, true)
        } else {
            context?.showShortToast(getString(R.string.toast_bluetooth_permission))
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

        if (!isAdded) {
            ctx.showShortToast(ctx.getString(R.string.toast_please_wait))
            return
        }

        if (showExplanation) {
            showPermissionExplanationDialog(
                title = getString(R.string.perm_bluetooth_title),
                message = getString(R.string.perm_bluetooth_body),
                onContinue = {
                    if (isAdded) {
                        pendingBluetoothAction = onGranted
                        try {
                            bluetoothPermissionLauncher.launch(perms)
                        } catch (e: IllegalStateException) {
                            view?.postDelayed({
                                if (isAdded) {
                                    try {
                                        bluetoothPermissionLauncher.launch(perms)
                                    } catch (_: Exception) {
                                        ctx.showShortToast(getString(R.string.toast_permission_failed))
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
                view?.postDelayed({
                    if (isAdded) {
                        try {
                            bluetoothPermissionLauncher.launch(perms)
                        } catch (_: Exception) {
                            ctx.showShortToast(getString(R.string.toast_permission_failed))
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
            context?.let { it.showShortToast(it.getString(R.string.toast_phone_permission)) }
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

        if (!isAdded) {
            ctx.showShortToast(ctx.getString(R.string.toast_please_wait))
            return
        }

        if (showExplanation) {
            showPermissionExplanationDialog(
                title = getString(R.string.perm_phone_title),
                message = getString(R.string.perm_phone_body),
                onContinue = {
                    if (isAdded) {
                        pendingPhoneStateAction = onGranted
                        try {
                            phoneStatePermissionLauncher.launch(phoneStatePermission)
                        } catch (e: IllegalStateException) {
                            view?.postDelayed({
                                if (isAdded) {
                                    try {
                                        phoneStatePermissionLauncher.launch(phoneStatePermission)
                                    } catch (_: Exception) {
                                        ctx.showShortToast(getString(R.string.toast_permission_failed))
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
                view?.postDelayed({
                    if (isAdded) {
                        try {
                            phoneStatePermissionLauncher.launch(phoneStatePermission)
                        } catch (_: Exception) {
                            ctx.showShortToast(getString(R.string.toast_permission_failed))
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
            context?.let { it.showShortToast(it.getString(R.string.toast_camera_permission)) }
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

        if (!isAdded) {
            ctx.showShortToast(ctx.getString(R.string.toast_please_wait))
            return
        }

        if (showExplanation) {
            showPermissionExplanationDialog(
                title = getString(R.string.perm_camera_title),
                message = getString(R.string.perm_camera_body),
                onContinue = {
                    if (isAdded) {
                        pendingCameraAction = onGranted
                        try {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } catch (e: IllegalStateException) {
                            view?.postDelayed({
                                if (isAdded) {
                                    try {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    } catch (_: Exception) {
                                        ctx.showShortToast(getString(R.string.toast_permission_failed))
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
                view?.postDelayed({
                    if (isAdded) {
                        try {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } catch (_: Exception) {
                            ctx.showShortToast(getString(R.string.toast_permission_failed))
                        }
                    }
                }, 100)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = try { androidx.lifecycle.ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java] } catch (e: Exception) { Log.e("SimpleTray", "Failed to get MainViewModel", e); null }
        val trayVm = try { androidx.lifecycle.ViewModelProvider(this)[SimpleTrayViewModel::class.java] } catch (e: Exception) { Log.e("SimpleTray", "Failed to get SimpleTrayViewModel", e); null }
        simpleTrayViewModel = trayVm
        prefs = vm?.getPrefs() ?: Prefs(requireContext())
        vibrator = requireContext().getSystemService(Vibrator::class.java)
        val composeView = ComposeView(requireContext())
        val fragment = this
        composeView.setContent {
            val isDark = prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
            val isEditMode by EditModeHelper.isEditModeFlow.collectAsState(initial = EditModeHelper.isEditMode())
            val simpleTrayRefreshTrigger = remember { mutableStateOf(0) }
            val triggerState = remember { mutableStateOf(0) }
            focusRestoreTriggerState = triggerState
            val onSimpleTraySettingsUpdate: () -> Unit = {
                simpleTrayRefreshTrigger.value += 1
            }
            SettingsTheme(isDark = isDark) {
                key(simpleTrayRefreshTrigger.value) {
                    SimpleTrayScreen(
                        prefs = prefs,
                        composeView = composeView,
                        simpleTrayViewModel = trayVm,
                        requestBluetoothPermission = ::requestBluetoothPermissions,
                        requestPhoneStatePermission = ::requestPhoneStatePermission,
                        requestCameraPermission = ::requestCameraPermission,
                        onShowPermissionExplanationDialog = { title, message, onContinue ->
                            showPermissionExplanationDialog(title, message, onContinue)
                        },
                        onLaunchBluetoothPermission = {
                            if (isAdded) {
                                val perms = getBluetoothPermissions()
                                try {
                                    bluetoothPermissionLauncher.launch(perms)
                                } catch (e: IllegalStateException) {
                                    view?.postDelayed({
                                        if (isAdded) {
                                            try {
                                                bluetoothPermissionLauncher.launch(perms)
                                            } catch (_: Exception) {
                                                context?.showShortToast(getString(R.string.toast_permission_failed))
                                            }
                                        }
                                    }, 100)
                                }
                            }
                        },
                        onLaunchCameraPermission = {
                            if (isAdded) {
                                try {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                } catch (e: IllegalStateException) {
                                    view?.postDelayed({
                                        if (isAdded) {
                                            try {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            } catch (_: Exception) {
                                                context?.showShortToast(getString(R.string.toast_permission_failed))
                                            }
                                        }
                                    }, 100)
                                }
                            }
                        },
                        onLaunchPhoneStatePermission = {
                            if (isAdded) {
                                try {
                                    phoneStatePermissionLauncher.launch(phoneStatePermission)
                                } catch (e: IllegalStateException) {
                                    view?.postDelayed({
                                        if (isAdded) {
                                            try {
                                                phoneStatePermissionLauncher.launch(phoneStatePermission)
                                            } catch (_: Exception) {
                                                context?.showShortToast(getString(R.string.toast_permission_failed))
                                            }
                                        }
                                    }, 100)
                                }
                            }
                        },
                        onRequestWriteSettingsPermission = { requestWriteSettingsPermission(showExplanation = true) },
                        onNavigateBack = {
                            try { findNavController().popBackStack() } catch (_: Exception) {}
                        },
                        onNavigateToSettings = {
                            try { findNavController().navigate(R.id.settingsFragment) } catch (_: Exception) {}
                        },
                        focusRestoreTrigger = triggerState.value,
                        isEditMode = isEditMode,
                        onNotificationEditModeClick = {
                            EditModeHelper.showSimpleTrayNotificationSettings(
                                fragment.requireContext(), fragment, prefs, onSimpleTraySettingsUpdate
                            )
                        },
                        onBackgroundEditModeClick = {
                            EditModeHelper.showSimpleTrayBackgroundSettings(
                                fragment.requireContext(), fragment, prefs, onSimpleTraySettingsUpdate
                            )
                        }
                    )
                }
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.defaultFocusHighlightEnabled = false
        view.foreground = null
        view.background = null
        view.requestFocus()

        view.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        context?.showShortToast("Dismiss: Del, C, #1 | Long press to dismiss all")
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        context?.showShortToast("Open: Enter, Dpad Center, #3")
                        return@setOnKeyListener true
                    }
                }
            }

            val mapped = com.github.gezimos.inkos.ui.compose.NavHelper.mapNotificationsKey(prefs, keyCode, event)
            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.None) return@setOnKeyListener false

            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageUp ||
                mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageDown) {
                return@setOnKeyListener false
            }

            val composeView = v as? ComposeView
            val pagerState = composeView?.getTag(0xdeadbeef.toInt()) as? SimplePagerState
            val coroutineScope = composeView?.getTag(0xcafebabe.toInt()) as? kotlinx.coroutines.CoroutineScope
            val validNotifications = (composeView?.getTag(0xabcdef01.toInt()) as? List<*>)
                ?.filterIsInstance<Pair<String, NotificationManager.ConversationNotification>>()

            if (pagerState == null || coroutineScope == null || validNotifications == null) {
                Log.d("NotificationsFragment", "pagerState, coroutineScope, or validNotifications is null")
                return@setOnKeyListener false
            }

            when (mapped) {
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Dismiss -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            dismissKeyDownTime = System.currentTimeMillis()
                            if (event.repeatCount > 15) {
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                validNotifications.forEach { (pkg, notif) ->
                                    try {
                                        val isMedia = notif.category == android.app.Notification.CATEGORY_TRANSPORT
                                        if (isMedia) {
                                            AudioWidgetHelper.getInstance(requireContext()).stopMedia()
                                        } else {
                                            if (notif.notificationKey != null) {
                                                NotificationService.dismissNotification(notif.notificationKey)
                                            }
                                            NotificationManager.getInstance(requireContext())
                                                .removeConversationNotification(pkg, notif.conversationId)
                                        }
                                    } catch (e: Exception) {
                                        Log.w("SimpleTray", "Failed to dismiss notification for $pkg", e)
                                    }
                                }
                                context?.showShortToast(getString(R.string.toast_all_dismissed))
                                dismissKeyDownTime = 0L
                                return@setOnKeyListener true
                            }
                        }
                        KeyEvent.ACTION_UP -> {
                            val pressDuration = if (dismissKeyDownTime > 0) {
                                System.currentTimeMillis() - dismissKeyDownTime
                            } else 0L
                            dismissKeyDownTime = 0L

                            if (pressDuration >= LONG_PRESS_THRESHOLD_MS) {
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                validNotifications.forEach { (pkg, notif) ->
                                    try {
                                        if (notif.notificationKey != null) {
                                            NotificationService.dismissNotification(notif.notificationKey)
                                        }
                                        NotificationManager.getInstance(requireContext())
                                            .removeConversationNotification(pkg, notif.conversationId)
                                    } catch (e: Exception) {
                                        Log.w("SimpleTray", "Failed to dismiss notification for $pkg", e)
                                    }
                                }
                                context?.showShortToast(getString(R.string.toast_all_dismissed))
                                return@setOnKeyListener true
                            } else {
                                val notifIndex = pagerState.currentPage * prefs.notificationsPerPage
                                val (pkg, notif) = validNotifications.getOrNull(notifIndex) ?: return@setOnKeyListener true
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}

                                val isMedia = notif.category == android.app.Notification.CATEGORY_TRANSPORT
                                if (isMedia) {
                                    AudioWidgetHelper.getInstance(requireContext()).stopMedia()
                                } else {
                                    if (notif.notificationKey != null) {
                                        NotificationService.dismissNotification(notif.notificationKey)
                                    }
                                    NotificationManager.getInstance(requireContext())
                                        .removeConversationNotification(pkg, notif.conversationId)
                                }
                                coroutineScope.launch {
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
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        act.pageNavigationHandler = null

        if (wasPaused) {
            wasPaused = false
            view?.postDelayed({
                val currentView = view
                if (isAdded && !isDetached && currentView != null) {
                    try {
                        currentView.isFocusable = true
                        currentView.isFocusableInTouchMode = true
                        currentView.requestFocus()
                    } catch (_: Exception) {}
                }
            }, 350)
            focusRestoreTriggerState?.value = (focusRestoreTriggerState?.value ?: 0) + 1
        }
    }

    override fun onPause() {
        super.onPause()
        wasPaused = true
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        if (act.pageNavigationHandler != null) act.pageNavigationHandler = null
    }

    override fun onDestroyView() {
        permissionExplanationDialog?.dismiss()
        permissionExplanationDialog = null
        super.onDestroyView()
    }
}
