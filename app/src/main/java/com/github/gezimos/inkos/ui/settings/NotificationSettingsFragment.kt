package com.github.gezimos.inkos.ui.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable
import com.github.gezimos.inkos.ui.dialogs.DialogManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationSettingsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var dialogManager: DialogManager

    private var onCustomFontSelected: ((com.github.gezimos.inkos.data.Constants.FontFamily) -> Unit)? =
        null

    // Helper data class for app info
    data class AppInfo(val label: String, val packageName: String)

    @Suppress("unused")
    private fun getInstalledApps(): List<AppInfo> {
        val pm = requireContext().packageManager
        val apps = pm.getInstalledApplications(0)
        return apps.map {
            val label = pm.getApplicationLabel(it).toString()
            AppInfo(label, it.packageName)
        }.sortedBy { it.label.lowercase() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        dialogManager = DialogManager(requireContext(), requireActivity())
        val isDark = when (prefs.appTheme) {
            com.github.gezimos.inkos.data.Constants.Theme.Light -> false
            com.github.gezimos.inkos.data.Constants.Theme.Dark -> true
            com.github.gezimos.inkos.data.Constants.Theme.System -> isSystemInDarkMode(
                requireContext()
            )
        }
        val settingsSize = (prefs.settingsSize - 3)
        val backgroundColor = com.github.gezimos.inkos.helper.getHexForOpacity(prefs)
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
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        SettingsComposable.PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.notification_section),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0],
                                    titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                        SettingsComposable.SolidSeparator(isDark = isDark)
                        Spacer(modifier = Modifier.height(SettingsTheme.color.horizontalPadding))
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
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        SettingsComposable.PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.notification_section),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0],
                                    titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                        SettingsComposable.SolidSeparator(isDark = isDark)
                        Spacer(modifier = Modifier.height(SettingsTheme.color.horizontalPadding))
                    }
                }
            }
        }
        scrollBehavior.attachToScrollView(nestedScrollView)
        return rootLayout
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: android.content.Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val fileName = getFileName(uri) ?: "custom_font.ttf"
                val file = java.io.File(requireContext().filesDir, fileName)
                try {
                    val inputStream: java.io.InputStream? =
                        requireContext().contentResolver.openInputStream(uri)
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    // Save path and call callback
                    val prefs = Prefs(requireContext())
                    prefs.customFontPath = file.absolutePath
                    // Add to custom font set
                    prefs.addCustomFontPath(file.absolutePath)
                    onCustomFontSelected?.invoke(com.github.gezimos.inkos.data.Constants.FontFamily.Custom)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = it.getString(index)
            }
        }
        return name
    }

    // Helper to display a short, user-friendly font name for custom fonts
    @Suppress("unused")
    private fun getShortFontDisplayName(fontPath: String?, maxLen: Int = 24): String {
        if (fontPath.isNullOrBlank()) return "Custom Font"
        val fileName = fontPath.substringAfterLast('/')
        return if (fileName.length > maxLen) {
            fileName.take(maxLen - 3) + "..."
        } else fileName
    }

    @Composable
    fun NotificationSettingsAllInOne(fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified) {
        val context = requireContext()
        val isDark = isSystemInDarkMode(context)
        val titleFontSize =
            if (fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize
        val prefs = remember { Prefs(context) }
        var pushNotificationsEnabled by remember { mutableStateOf(prefs.pushNotificationsEnabled) }

        // Observe pushNotificationsEnabledFlow for real-time updates
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        LaunchedEffect(prefs) {
            lifecycleOwner.lifecycleScope.launch {
                prefs.pushNotificationsEnabledFlow.collectLatest { enabled ->
                    pushNotificationsEnabled = enabled
                }
            }
        }

        // --- Compose state for all notification switches ---
        var showNotificationBadge by rememberSaveable { mutableStateOf(prefs.showNotificationBadge) }
        var showNotificationText by rememberSaveable { mutableStateOf(prefs.showNotificationText) }
        var showMediaIndicator by rememberSaveable { mutableStateOf(prefs.showMediaIndicator) }
        var showMediaName by rememberSaveable { mutableStateOf(prefs.showMediaName) }
        var showSenderName by rememberSaveable { mutableStateOf(prefs.showNotificationSenderName) }
        var showGroupName by rememberSaveable { mutableStateOf(prefs.showNotificationGroupName) }
        var showMessage by rememberSaveable { mutableStateOf(prefs.showNotificationMessage) }
        var notificationsEnabled by rememberSaveable { mutableStateOf(prefs.notificationsEnabled) }
        var charLimit by rememberSaveable { mutableStateOf(prefs.homeAppCharLimit) }
        var clearConversationOnAppOpen by rememberSaveable { mutableStateOf(prefs.clearConversationOnAppOpen) }

        // --- Add state for allowlists to trigger recomposition ---
        var badgeAllowlist by remember { mutableStateOf(prefs.allowedBadgeNotificationApps.toSet()) }
        var allowlist by remember { mutableStateOf(prefs.allowedNotificationApps.toSet()) }

        // When toggling master switch, update all states
        fun onPushNotificationsToggle(newValue: Boolean) {
            // Always launch Notification Listener Settings when toggling
            val intent =
                android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
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
            if (!newValue) {
                prefs.saveNotificationSwitchesState()
                prefs.disableAllNotificationSwitches()
                showNotificationBadge = false
                showNotificationText = false
                showMediaIndicator = false
                showMediaName = false
                showSenderName = false
                showGroupName = false
                showMessage = false
                notificationsEnabled = false
                clearConversationOnAppOpen = false
            } else {
                prefs.restoreNotificationSwitchesState()
                showNotificationBadge = prefs.showNotificationBadge
                showNotificationText = prefs.showNotificationText
                showMediaIndicator = prefs.showMediaIndicator
                showMediaName = prefs.showMediaName
                showSenderName = prefs.showNotificationSenderName
                showGroupName = prefs.showNotificationGroupName
                showMessage = prefs.showNotificationMessage
                notificationsEnabled = prefs.notificationsEnabled
                clearConversationOnAppOpen = prefs.clearConversationOnAppOpen
            }
            prefs.pushNotificationsEnabled = newValue
            pushNotificationsEnabled = newValue
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsComposable.FullLineSeparator(isDark = isDark)
            // Push Notifications master switch
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.push_notifications),
                fontSize = titleFontSize,
                defaultState = pushNotificationsEnabled,
                onCheckedChange = { onPushNotificationsToggle(!pushNotificationsEnabled) }
            )
            SettingsComposable.FullLineSeparator(isDark = isDark)
            SettingsComposable.SettingsTitle(
                text = stringResource(R.string.notification_home),
                fontSize = titleFontSize
            )
            SettingsComposable.FullLineSeparator(isDark = isDark)
            // Notification Badge
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_notification_badge),
                fontSize = titleFontSize,
                defaultState = showNotificationBadge,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    showNotificationBadge = !showNotificationBadge
                    prefs.showNotificationBadge = showNotificationBadge
                }
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            // Notification Text
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_notification_text),
                fontSize = titleFontSize,
                defaultState = showNotificationText,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    showNotificationText = !showNotificationText
                    prefs.showNotificationText = showNotificationText
                }
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            // Media Playing Indicator
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_media_playing_indicator),
                fontSize = titleFontSize,
                defaultState = showMediaIndicator,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    showMediaIndicator = !showMediaIndicator
                    prefs.showMediaIndicator = showMediaIndicator
                }
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            // Media Playing Name
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_media_playing_name),
                fontSize = titleFontSize,
                defaultState = showMediaName,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    showMediaName = !showMediaName
                    prefs.showMediaName = showMediaName
                }
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            val badgeAllowlistState = badgeAllowlist // for recomposition
            var showBadgeDialog by remember { mutableStateOf(false) }
            SettingsComposable.SettingsSelect(
                title = stringResource(R.string.home_notifications_allowlist),
                option = badgeAllowlistState.size.toString(),
                fontSize = titleFontSize,
                onClick = { showBadgeDialog = true },
                enabled = pushNotificationsEnabled
            )
            if (showBadgeDialog) {
                LaunchedEffect(Unit) {
                    showBadgeDialog = false
                    showAppAllowlistDialog(
                        title = "Label Notification Apps",
                        initialSelected = badgeAllowlistState,
                        onConfirm = { selected ->
                            prefs.allowedBadgeNotificationApps = selected.toMutableSet()
                            badgeAllowlist = selected.toMutableSet() // update state to refresh UI
                        }
                    )
                }
            }
            SettingsComposable.FullLineSeparator(isDark = isDark)
            // Chat section
            SettingsComposable.SettingsTitle(
                text = stringResource(R.string.chat_notifications_section),
                fontSize = titleFontSize
            )
            SettingsComposable.FullLineSeparator(isDark = isDark)
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_sender_name),
                fontSize = titleFontSize,
                defaultState = showSenderName,
                onCheckedChange = {
                    showSenderName = !showSenderName
                    prefs.showNotificationSenderName = showSenderName
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_conversation_group_name),
                fontSize = titleFontSize,
                defaultState = showGroupName,
                onCheckedChange = {
                    showGroupName = !showGroupName
                    prefs.showNotificationGroupName = showGroupName
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.show_message),
                fontSize = titleFontSize,
                defaultState = showMessage,
                onCheckedChange = {
                    showMessage = !showMessage
                    prefs.showNotificationMessage = showMessage
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            SettingsComposable.SettingsSelect(
                title = stringResource(R.string.badge_character_limit),
                option = charLimit.toString(),
                fontSize = titleFontSize,
                onClick = {
                    DialogManager(requireContext(), requireActivity()).showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.badge_character_limit),
                        minValue = 5,
                        maxValue = 50,
                        currentValue = charLimit,
                        onValueSelected = { newValue ->
                            prefs.homeAppCharLimit = newValue
                            charLimit = newValue
                        }
                    )
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.FullLineSeparator(isDark = isDark)
            // Filter section
            SettingsComposable.SettingsTitle(
                text = stringResource(R.string.notifications_window_title),
                fontSize = titleFontSize
            )
            SettingsComposable.FullLineSeparator(isDark = isDark)
            var notificationsEnabled by remember { mutableStateOf(prefs.notificationsEnabled) }
            val allowlistState = allowlist // for recomposition
            var showAllowlistDialog by remember { mutableStateOf(false) }
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.enable_notifications),
                fontSize = titleFontSize,
                defaultState = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = !notificationsEnabled
                    prefs.notificationsEnabled = notificationsEnabled
                },
                enabled = pushNotificationsEnabled
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            // Clear conversation on app open
            SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.clear_conversation_on_app_open),
                fontSize = titleFontSize,
                defaultState = clearConversationOnAppOpen,
                enabled = pushNotificationsEnabled,
                onCheckedChange = {
                    clearConversationOnAppOpen = !clearConversationOnAppOpen
                    prefs.clearConversationOnAppOpen = clearConversationOnAppOpen
                }
            )
            SettingsComposable.DashedSeparator(isDark = isDark)
            SettingsComposable.SettingsSelect(
                title = "Notification Allowlist",
                option = allowlistState.size.toString(),
                fontSize = titleFontSize,
                onClick = { showAllowlistDialog = true },
                enabled = pushNotificationsEnabled
            )
            if (showAllowlistDialog) {
                LaunchedEffect(Unit) {
                    showAllowlistDialog = false
                    showAppAllowlistDialog(
                        title = "Notification Window Apps",
                        initialSelected = allowlistState,
                        onConfirm = { selected ->
                            prefs.allowedNotificationApps = selected.toMutableSet()
                            allowlist = selected.toMutableSet() // update state to refresh UI
                        }
                    )
                }
            }
            SettingsComposable.FullLineSeparator(isDark = isDark)
        }
    }

    // Font selection dialog helper (copied from FontsFragment, but simplified for notification fonts)
    @Suppress("unused")
    private fun showFontSelectionDialogWithCustoms(
        titleResId: Int,
        onFontSelected: (com.github.gezimos.inkos.data.Constants.FontFamily, String?) -> Unit
    ) {
        val fontFamilyEntries =
            com.github.gezimos.inkos.data.Constants.FontFamily.entries
                .filter { it != com.github.gezimos.inkos.data.Constants.FontFamily.Custom }
        val context = requireContext()
        val prefs = Prefs(context)

        val builtInFontOptions = fontFamilyEntries.map { it.getString(context) }
        val builtInFonts = fontFamilyEntries.map {
            it.getFont(context) ?: com.github.gezimos.inkos.helper.getTrueSystemFont()
        }

        val customFonts =
            com.github.gezimos.inkos.data.Constants.FontFamily.getAllCustomFonts(
                context
            )
        val customFontOptions = customFonts.map { it.first }
        val customFontPaths = customFonts.map { it.second }
        val customFontTypefaces = customFontPaths.map { path ->
            com.github.gezimos.inkos.data.Constants.FontFamily.Custom.getFont(
                context,
                path
            ) ?: com.github.gezimos.inkos.helper.getTrueSystemFont()
        }

        val addCustomFontOption = "Add Custom Font..."

        val options = builtInFontOptions + customFontOptions + addCustomFontOption
        val fonts =
            builtInFonts + customFontTypefaces + com.github.gezimos.inkos.helper.getTrueSystemFont()

        dialogManager.showSingleChoiceDialog(
            context = context,
            options = options.toTypedArray(),
            fonts = fonts,
            titleResId = titleResId,
            onItemSelected = { selectedName ->
                when (selectedName) {
                    addCustomFontOption -> {
                        pickCustomFontFile { _, path ->
                            prefs.setCustomFontPath("custom", path)
                            prefs.addCustomFontPath(path)
                            onFontSelected(
                                com.github.gezimos.inkos.data.Constants.FontFamily.Custom,
                                path
                            )
                            activity?.recreate()
                        }
                    }

                    else -> {
                        val builtInIndex = builtInFontOptions.indexOf(selectedName)
                        if (builtInIndex != -1) {
                            onFontSelected(fontFamilyEntries[builtInIndex], null)
                            return@showSingleChoiceDialog
                        }
                        val customIndex = customFontOptions.indexOf(selectedName)
                        if (customIndex != -1) {
                            val path = customFontPaths[customIndex]
                            onFontSelected(
                                com.github.gezimos.inkos.data.Constants.FontFamily.Custom,
                                path
                            )
                            return@showSingleChoiceDialog
                        }
                    }
                }
            }
        )
    }

    // Font file picker for custom fonts (copied from FontsFragment, but only path is needed)
    @Suppress("UNUSED_PARAMETER")
    private fun pickCustomFontFile(onFontPicked: (android.graphics.Typeface, String) -> Unit) {
        onCustomFontSelected = { _ ->
            // Not used here, but required for interface
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "font/*"
            putExtra(
                android.content.Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "font/ttf",
                    "font/otf",
                    "application/x-font-ttf",
                    "application/x-font-opentype",
                    "application/octet-stream"
                )
            )
        }
        startActivityForResult(intent, 1002)
    }

    // Helper to show app allowlist dialog using DialogManager (imperative, not Compose)
    private fun showAppAllowlistDialog(
        title: String,
        initialSelected: Set<String>,
        onConfirm: (Set<String>) -> Unit,
        includeHidden: Boolean = true // Add this param to control hidden apps
    ) {
        // Use MainViewModel's appList instead of getInstalledApps
        val activity = requireActivity()
        val viewModel =
            androidx.lifecycle.ViewModelProvider(activity)[com.github.gezimos.inkos.MainViewModel::class.java]
        viewModel.getAppList(includeHiddenApps = includeHidden)
        val appListLiveData = viewModel.appList
        // Observe once and show dialog when data is available
        appListLiveData.observe(viewLifecycleOwner) { appListItems ->
            if (appListItems == null) return@observe
            // Exclude synthetic apps, but include hidden apps
            val filteredApps = appListItems.filter {
                val pkg = it.activityPackage ?: ""
                pkg.isNotBlank() &&
                        !pkg.startsWith("com.inkos.internal.") &&
                        !pkg.startsWith("com.inkos.system.")
            }
            val allApps = filteredApps.map {
                AppInfo(label = it.customLabel.takeIf { l -> !l.isNullOrEmpty() }
                    ?: it.activityLabel, packageName = it.activityPackage)
            }
            // Sort: selected apps first, then unselected, both alphabetically
            val sortedApps = allApps.sortedWith(
                compareByDescending<AppInfo> { initialSelected.contains(it.packageName) }
                    .thenBy { it.label.lowercase() }
            )
            val appLabels = sortedApps.map { it.label }
            val appPackages = sortedApps.map { it.packageName }
            val checkedItems = appPackages.map { initialSelected.contains(it) }.toBooleanArray()

            dialogManager.showMultiChoiceDialog(
                context = requireContext(),
                title = title,
                items = appLabels.toTypedArray(),
                initialChecked = checkedItems,
                onConfirm = { selectedIndices ->
                    val selectedPkgs = selectedIndices.map { appPackages[it] }.toSet()
                    onConfirm(selectedPkgs)
                }
            )
            // Remove observer after first use
            appListLiveData.removeObservers(viewLifecycleOwner)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
