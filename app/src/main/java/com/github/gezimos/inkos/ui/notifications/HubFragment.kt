package com.github.gezimos.inkos.ui.notifications

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.services.NotificationService
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
class HubFragment : Fragment() {
    companion object {
        fun clearHubCache() { HubLayoutCache.clear() }
    }

    private lateinit var prefs: Prefs
    private var dismissKeyDownTime: Long = 0L
    private val LONG_PRESS_THRESHOLD_MS = 700L
    private var wasPaused = false
    private var focusRestoreTriggerState: androidx.compose.runtime.MutableState<Int>? = null
    private var permissionExplanationSheet: com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost? = null

    private fun showPermissionExplanationDialog(
        title: String,
        message: String,
        onContinue: () -> Unit
    ) {
        permissionExplanationSheet?.dismiss()
        val host = com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost(requireActivity())
        permissionExplanationSheet = host
        host.show {
            val screenScale = rememberScreenScale()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))
            ) {
                com.github.gezimos.inkos.ui.dialogs.SheetTitle(title)
                Text(text = message, style = SettingsTheme.typography.item, color = Theme.colors.text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = try { androidx.lifecycle.ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java] } catch (_: Exception) { null }
        prefs = vm?.getPrefs() ?: Prefs(requireContext())
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            val isDark = prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
            val triggerState = remember { mutableStateOf(0) }
            focusRestoreTriggerState = triggerState
            SettingsTheme(isDark = isDark) {
                HubScreen(
                    prefs = prefs,
                    composeView = composeView,
                    focusRestoreTrigger = triggerState.value,
                    onShowPermissionExplanationDialog = { title, message, onContinue ->
                        showPermissionExplanationDialog(title, message, onContinue)
                    },
                    onEditModeClick = {
                        com.github.gezimos.inkos.helper.EditModeHelper.showHubSettings(
                            requireContext(), this@HubFragment, prefs
                        ) { /* recomposition handles update */ }
                    },
                    onNavigateBack = {
                        try { findNavController().popBackStack() } catch (_: Exception) {}
                    }
                )
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
                ?.filterIsInstance<HubNotification>()

            if (pagerState == null || coroutineScope == null || validNotifications == null) {
                return@setOnKeyListener false
            }

            when (mapped) {
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Dismiss -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            dismissKeyDownTime = System.currentTimeMillis()
                            if (event.repeatCount > 15) {
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                validNotifications.forEach { hn ->
                                    try {
                                        if (hn.notif.notificationKey != null) {
                                            NotificationService.dismissNotification(hn.notif.notificationKey)
                                        }
                                        NotificationManager.getInstance(requireContext())
                                            .removeConversationNotification(hn.packageName, hn.notif.conversationId)
                                    } catch (_: Exception) {}
                                }
                                context?.showShortToast(getString(R.string.toast_all_dismissed))
                                dismissKeyDownTime = 0L
                                return@setOnKeyListener true
                            }
                        }
                        KeyEvent.ACTION_UP -> {
                            val pressDuration = if (dismissKeyDownTime > 0) {
                                System.currentTimeMillis() - dismissKeyDownTime
                            } else { 0L }
                            dismissKeyDownTime = 0L

                            if (pressDuration >= LONG_PRESS_THRESHOLD_MS) {
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                validNotifications.forEach { hn ->
                                    try {
                                        if (hn.notif.notificationKey != null) {
                                            NotificationService.dismissNotification(hn.notif.notificationKey)
                                        }
                                        NotificationManager.getInstance(requireContext())
                                            .removeConversationNotification(hn.packageName, hn.notif.conversationId)
                                    } catch (_: Exception) {}
                                }
                                context?.showShortToast(getString(R.string.toast_all_dismissed))
                                return@setOnKeyListener true
                            } else {
                                val notifIndex = pagerState.currentPage * 3
                                val hn = validNotifications.getOrNull(notifIndex) ?: return@setOnKeyListener true
                                try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                if (hn.notif.notificationKey != null) {
                                    NotificationService.dismissNotification(hn.notif.notificationKey)
                                }
                                NotificationManager.getInstance(requireContext())
                                    .removeConversationNotification(hn.packageName, hn.notif.conversationId)
                                return@setOnKeyListener true
                            }
                        }
                    }
                    true
                }
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Open -> {
                    val notifIndex = pagerState.currentPage * 3
                    val hn = validNotifications.getOrNull(notifIndex) ?: return@setOnKeyListener true
                    NotificationManager.getInstance(requireContext())
                        .openNotification(hn.packageName, hn.notif.notificationKey, hn.notif.conversationId, removeAfterOpen = true)
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
}
