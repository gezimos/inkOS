package com.github.gezimos.inkos.ui.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable
import com.github.gezimos.inkos.ui.dialogs.DialogManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationSettingsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: com.github.gezimos.inkos.MainViewModel
    private lateinit var dialogManager: DialogManager

    // `user` is included so we can detect stored entries that use the package|user format
    data class AppInfo(val label: String, val packageName: String, val user: String? = null)

    /**
     * Checks if notification listener service is enabled.
     * This is what we need to READ notifications from other apps.
     */
    private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        dialogManager = DialogManager(requireContext(), requireActivity())
        val isDark = prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
        val backgroundColor = com.github.gezimos.inkos.helper.getHexForOpacity(requireContext())
        val context = requireContext()
        // --- Dot indicator state ---
        val currentPage = intArrayOf(0)
        val pageCount = intArrayOf(1)

        // Create a vertical LinearLayout to hold sticky header and scrollable content
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        var bottomInsetPx = 0
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Add sticky header ComposeView
        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        SettingsComposable.PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.notification_section),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
                            pageIndicator = {
                                SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                    }
                }
            }
        }
        rootLayout.addView(headerView)

        // Add scrollable settings content
        val nestedScrollView = androidx.core.widget.NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                androidx.compose.ui.platform.ComposeView(context).apply {
                    setContent {
                        val homeUiState by viewModel.homeUiState.collectAsState()
                        val settingsSize = (homeUiState.settingsSize - 3)
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    NotificationSettingsAllInOne(settingsSize.sp)
                                }
                            }
                        }
                    }
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        com.github.gezimos.inkos.helper.utils.EinkScrollBehavior(context)
            .attachToScrollView(nestedScrollView)

        // Create layout params that account for navigation bar
        val scrollViewLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        rootLayout.addView(nestedScrollView, scrollViewLayoutParams)

        // Apply bottom padding to the root layout to prevent scroll view from going under navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

        // Use EinkScrollBehavior callback to update page indicator reliably
        val scrollBehavior = com.github.gezimos.inkos.helper.utils.EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            headerView.setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        SettingsComposable.PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.notification_section),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
                            pageIndicator = {
                                SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                    }
                }
            }
        }
        scrollBehavior.attachToScrollView(nestedScrollView)
        return rootLayout
    }



    @Composable
    fun NotificationSettingsAllInOne(fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified) {
        val context = requireContext()
        val titleFontSize =
            if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize
        
        val uiState by viewModel.homeUiState.collectAsState()

        // Check if notification listener service is enabled (this is what we need to READ notifications)
        val hasNotificationListener = remember(context) { isNotificationListenerEnabled(context) }
        
        // Use actual listener status for switch state, not just the preference
        var pushNotificationsEnabled by remember { mutableStateOf(hasNotificationListener) }
        
        // Check immediately when composable is created, then poll periodically to catch changes
        LaunchedEffect(context) {
            // Check immediately
            val initialListener = isNotificationListenerEnabled(context)
            pushNotificationsEnabled = initialListener
            viewModel.setPushNotificationsEnabled(initialListener)
            
            // Then check periodically to catch changes when user returns from settings
            while (true) {
                kotlinx.coroutines.delay(1000)
                val hasListener = isNotificationListenerEnabled(context)
                if (pushNotificationsEnabled != hasListener) {
                    pushNotificationsEnabled = hasListener
                    // Sync with ViewModel
                    viewModel.setPushNotificationsEnabled(hasListener)
                }
            }
        }

        // Helper function to open notification listener settings
        fun openNotificationListenerSettings() {
            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                // fallback: open app details if notification listener settings not available
                val fallbackIntent =
                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply {
                            data = android.net.Uri.fromParts("package", context.packageName, null)
                        }
                context.startActivity(fallbackIntent)
            }
        }
        
        // When toggling master switch, check and request notification listener access
        fun onPushNotificationsToggle(requestedState: Boolean) {
            if (requestedState) {
                // User wants to enable notifications - check if listener is enabled
                val hasListener = isNotificationListenerEnabled(context)
                
                // If listener is already enabled, enable immediately
                if (hasListener) {
                    pushNotificationsEnabled = true
                    viewModel.setPushNotificationsEnabled(true)
                    return
                }
                
                // Otherwise, open notification listener settings
                openNotificationListenerSettings()
                
                // Don't enable switch yet - wait for listener to be enabled
                // The periodic check will update it when user returns
                pushNotificationsEnabled = false
            } else {
                // User wants to disable notifications - open settings so they can revoke the permission
                openNotificationListenerSettings()
                
                // Keep current state - it will update when user returns and actually disables the listener
                // The periodic check will update it when user returns
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Push Notifications master switch
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.push_notifications),
                fontSize = titleFontSize,
                defaultState = pushNotificationsEnabled,
                onCheckedChange = { onPushNotificationsToggle(it) }
            )
            SettingsComposable.SettingsTitle(
                text = stringResource(R.string.notification_home),
                fontSize = titleFontSize
            )
            // Notification Badge
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_notification_badge),
                fontSize = titleFontSize,
                defaultState = uiState.showNotificationBadge,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    viewModel.setShowNotificationBadge(it)
                }
            )
            // Notification Text
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_notification_text),
                fontSize = titleFontSize,
                defaultState = uiState.showNotificationText,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    viewModel.setShowNotificationText(it)
                }
            )
            // Media Playing Indicator
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_media_playing_indicator),
                fontSize = titleFontSize,
                defaultState = uiState.showMediaIndicator,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    viewModel.setShowMediaIndicator(it)
                }
            )
            // Media Playing Name
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_media_playing_name),
                fontSize = titleFontSize,
                defaultState = uiState.showMediaName,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    viewModel.setShowMediaName(it)
                }
            )
            
            var showBadgeDialog by remember { mutableStateOf(false) }
            SettingsComposable.SettingsSelect(
                title = stringResource(R.string.home_notifications_allowlist),
                option = uiState.allowedBadgeNotificationApps.size.toString(),
                fontSize = titleFontSize,
                onClick = { showBadgeDialog = true },
                enabled = pushNotificationsEnabled
            )
            if (showBadgeDialog) {
                LaunchedEffect(Unit) {
                    showBadgeDialog = false
                    showAppAllowlistDialog(
                        title = "Label Notification Apps",
                        initialSelected = uiState.allowedBadgeNotificationApps,
                        onConfirm = { selected ->
                            viewModel.setAllowedBadgeNotificationApps(selected)
                        }
                    )
                }
            }
            // Chat section
            SettingsComposable.SettingsTitle(
                text = stringResource(R.string.chat_notifications_section),
                fontSize = titleFontSize
            )
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_sender_name),
                fontSize = titleFontSize,
                defaultState = uiState.showNotificationSenderName,
                onCheckedChange = {
                    viewModel.setShowNotificationSenderName(it)
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_conversation_group_name),
                fontSize = titleFontSize,
                defaultState = uiState.showNotificationGroupName,
                onCheckedChange = {
                    viewModel.setShowNotificationGroupName(it)
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_message),
                fontSize = titleFontSize,
                defaultState = uiState.showNotificationMessage,
                onCheckedChange = {
                    viewModel.setShowNotificationMessage(it)
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.SettingsSelect(
                title = stringResource(R.string.badge_character_limit),
                option = uiState.homeAppCharLimit.toString(),
                fontSize = titleFontSize,
                onClick = {
                    DialogManager(requireContext(), requireActivity()).showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.badge_character_limit),
                        minValue = 5,
                        maxValue = 50,
                        currentValue = uiState.homeAppCharLimit,
                        onValueSelected = { newValue ->
                            viewModel.setHomeAppCharLimit(newValue)
                        }
                    )
                },
                enabled = pushNotificationsEnabled
            )
            // Filter section
            SettingsComposable.SettingsTitle(
                text = stringResource(R.string.notifications_window_title),
                fontSize = titleFontSize
            )
            
            var showAllowlistDialog by remember { mutableStateOf(false) }
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.enable_notifications),
                fontSize = titleFontSize,
                defaultState = uiState.notificationsEnabled,
                onCheckedChange = {
                    viewModel.setNotificationsEnabled(it)
                },
                enabled = pushNotificationsEnabled
            )
            // Clear conversation on app open
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.clear_conversation_on_app_open),
                fontSize = titleFontSize,
                defaultState = uiState.clearConversationOnAppOpen,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    viewModel.setClearConversationOnAppOpen(it)
                }
            )
            SettingsComposable.SettingsSelect(
                title = "Notification Allowlist",
                option = uiState.allowedNotificationApps.size.toString(),
                fontSize = titleFontSize,
                onClick = { showAllowlistDialog = true },
                enabled = pushNotificationsEnabled
            )
            if (showAllowlistDialog) {
                LaunchedEffect(Unit) {
                    showAllowlistDialog = false
                    showAppAllowlistDialog(
                        title = "Notification Window Apps",
                        initialSelected = uiState.allowedNotificationApps,
                        onConfirm = { selected ->
                            viewModel.setAllowedNotificationApps(selected)
                        },
                        // includeHidden parameter removed; dialog always shows full list
                    )
                }
            }
            
            // Simple Tray section
            SettingsComposable.SettingsTitle(
                text = "Simple Tray",
                fontSize = titleFontSize
            )
            SettingsComposable.SettingsSelect(
                title = stringResource(R.string.notifications_per_page),
                option = uiState.notificationsPerPage.toString(),
                fontSize = titleFontSize,
                onClick = {
                    // Cycle through 1, 2, 3 like alignment switch (0, 1, 2 -> 1, 2, 3)
                    val next = ((uiState.notificationsPerPage - 1 + 1) % 3) + 1
                    viewModel.setNotificationsPerPage(next)
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.enable_bottom_navigation),
                fontSize = titleFontSize,
                defaultState = uiState.enableBottomNav,
                onCheckedChange = {
                    viewModel.setEnableBottomNav(it)
                },
                enabled = pushNotificationsEnabled
            )
            var showSimpleTrayAllowlistDialog by remember { mutableStateOf(false) }
            SettingsComposable.SettingsSelect(
                title = "Simple Tray Allowlist",
                option = uiState.allowedSimpleTrayApps.size.toString(),
                fontSize = titleFontSize,
                onClick = { showSimpleTrayAllowlistDialog = true },
                enabled = pushNotificationsEnabled
            )
            if (showSimpleTrayAllowlistDialog) {
                LaunchedEffect(Unit) {
                    showSimpleTrayAllowlistDialog = false
                    showAppAllowlistDialog(
                        title = "Simple Tray Apps",
                        initialSelected = uiState.allowedSimpleTrayApps,
                        onConfirm = { selected ->
                            viewModel.setAllowedSimpleTrayApps(selected)
                        },
                    )
                }
            }
        }
    }



    // Helper to show app allowlist dialog using DialogManager (imperative, not Compose)
    private fun showAppAllowlistDialog(
        title: String,
        initialSelected: Set<String>,
        onConfirm: (Set<String>) -> Unit
    ) {
        // Use MainViewModel's appList instead of getInstalledApps
        val activity = requireActivity()
        val viewModel =
            ViewModelProvider(activity)[com.github.gezimos.inkos.MainViewModel::class.java]
    // Always request the full list (include hidden apps). Keep the dialog
    // simple: we only filter out internal/synthetic entries and show all
    // other apps so both allowlists behave the same.
    viewModel.getAppList(includeHiddenApps = true)
        // Collect from StateFlow instead of observing LiveData
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.appsDrawerUiState.collectLatest { appsDrawerState ->
                val appListItems = appsDrawerState.appList
                if (appListItems.isEmpty()) return@collectLatest
                // Exclude internal/synthetic apps only; show everything else
                val filteredApps = appListItems.filter {
                    val pkg = it.activityPackage
                    pkg.isNotBlank() &&
                            !pkg.startsWith("com.inkos.internal.") &&
                            !pkg.startsWith("com.inkos.system.")
                }
                val allApps = filteredApps.map {
                    AppInfo(
                        label = it.customLabel.takeIf { l -> l.isNotEmpty() } ?: it.activityLabel,
                        packageName = it.activityPackage,
                        user = it.user.toString()
                    )
                }
                // Sort: selected apps first, then unselected, both alphabetically
                val sortedApps = allApps.sortedWith(
                    compareByDescending<AppInfo> { initialSelected.contains(it.packageName) }
                        .thenBy { it.label.lowercase() }
                )
                val appLabels = sortedApps.map { it.label }
                val appPackages = sortedApps.map { it.packageName }
                val checkedItems = appPackages.mapIndexed { idx, pkg ->
                    val userStr = sortedApps[idx].user
                    val pkgWithUser = if (!userStr.isNullOrBlank()) "$pkg|$userStr" else pkg
                    initialSelected.contains(pkg) || initialSelected.contains(pkgWithUser)
                }.toBooleanArray()

                dialogManager.showMultiChoiceDialog(
                    context = requireContext(),
                    title = title,
                    items = appLabels.toTypedArray(),
                    initialChecked = checkedItems,
                    maxHeightRatio = 0.60f, // Make allowlist dialogs taller (60% of screen)
                    onConfirm = { selectedIndices ->
                        val selectedPkgs = selectedIndices.map { appPackages[it] }.toSet()
                        onConfirm(selectedPkgs)
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
