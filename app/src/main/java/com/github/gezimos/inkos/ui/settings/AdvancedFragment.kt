package com.github.gezimos.inkos.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.isBiometricEnabled
import com.github.gezimos.inkos.BuildConfig
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Constants.Theme.Light
import com.github.gezimos.inkos.data.Constants.Theme.System
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.utils.AppReloader
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.dialogs.DialogManager

class AdvancedFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var dialogBuilder: DialogManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val backgroundColor = getHexForOpacity(prefs)
        val isDark = when (prefs.appTheme) {
            Light -> false
            Dark -> true
            System -> isSystemInDarkMode(requireContext())
        }
        val settingsSize = (prefs.settingsSize - 3)
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
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Add sticky header ComposeView
        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent {
                val density = LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.advanced_settings_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0],
                                    titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        
                        if (bottomInsetDp > 0.dp) {
                            Spacer(modifier = Modifier.height(bottomInsetDp))
                        }
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
                        val density = LocalDensity.current
                        val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    AdvancedSettingsAllInOne(settingsSize.sp)
                                    if (bottomInsetDp > 0.dp) {
                                        Spacer(modifier = Modifier.height(bottomInsetDp))
                                    }
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
        rootLayout.addView(
            nestedScrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

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
                val density = LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.advanced_settings_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0],
                                    titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        
                        if (bottomInsetDp > 0.dp) Spacer(modifier = Modifier.height(bottomInsetDp))
                    }
                }
            }
        }
        scrollBehavior.attachToScrollView(nestedScrollView)
        return rootLayout
    }

    @Composable
    fun AdvancedSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified) {
        findNavController()
        val isDark = isSystemInDarkMode(requireContext())
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val iconSize = if (fontSize.isSpecified) tuToDp((fontSize * 0.8)) else tuToDp(fontSize)
        val changeLauncherText = if (!isinkosDefault(requireContext())) {
            R.string.advanced_settings_set_as_default_launcher
        } else {
            R.string.advanced_settings_change_default_launcher
        }
        val navController = findNavController()
        // Remove verticalScroll and header, handled by parent ComposeView
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Home App Management section moved from FeaturesFragment ---
            var toggledAppsLocked by remember { mutableStateOf(prefs.homeLocked) }
            var toggledSettingsLocked by remember { mutableStateOf(prefs.settingsLocked) }
            var toggledLongPressAppInfo by remember { mutableStateOf(prefs.longPressAppInfoEnabled) }
            DashedSeparator(isDark = isDark)
            SettingsSwitch(
                text = stringResource(R.string.lock_home_apps),
                fontSize = titleFontSize,
                defaultState = toggledAppsLocked,
                onCheckedChange = { checked ->
                    toggledAppsLocked = checked
                    prefs.homeLocked = checked
                    if (!checked) {
                        toggledLongPressAppInfo = false
                        prefs.longPressAppInfoEnabled = false
                    }
                }
            )
            DashedSeparator(isDark = isDark)
            SettingsSwitch(
                text = stringResource(R.string.longpress_app_info),
                fontSize = titleFontSize,
                defaultState = toggledLongPressAppInfo,
                enabled = toggledAppsLocked,
                onCheckedChange = { checked ->
                    toggledLongPressAppInfo = checked
                    prefs.longPressAppInfoEnabled = checked
                }
            )
            if (requireContext().isBiometricEnabled()) {
                DashedSeparator(isDark = isDark)
                SettingsSwitch(
                    text = stringResource(R.string.lock_settings),
                    fontSize = titleFontSize,
                    defaultState = toggledSettingsLocked,
                    onCheckedChange = { checked ->
                        toggledSettingsLocked = checked
                        prefs.settingsLocked = checked
                    }
                )
            }
            DashedSeparator(isDark = isDark)
            // App Info item with version text (opens app info dialog on click)
            SettingsSelect(
                title = stringResource(R.string.app_version),
                option = "v0.3",
                fontSize = titleFontSize,
                enabled = true,
                onClick = {
                    openAppInfo(
                        requireContext(),
                        android.os.Process.myUserHandle(),
                        BuildConfig.APPLICATION_ID
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_backup_restore_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    dialogBuilder.showBackupRestoreDialog()
                }
            )
            DashedSeparator(isDark)
            SettingsHomeItem(
                title = stringResource(changeLauncherText) +
                        if (!isinkosDefault(requireContext())) "*" else "",
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    // Open system Default Home App settings for proper launcher selection UI
                    val intent =
                        android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                    requireContext().startActivity(intent)
                }
            )
            DashedSeparator(isDark)
            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_restart_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    AppReloader.restartApp(requireContext())
                }
            )
            DashedSeparator(isDark)
            SettingsHomeItem(
                title = stringResource(R.string.settings_exit_inkos_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { exitLauncher() }
            )
        }
    }

    @Composable
    fun tuToDp(textUnit: TextUnit): Dp {
        val density = LocalDensity.current.density
        val scaledDensity = LocalDensity.current.fontScale
        val dpValue = textUnit.value * (density / scaledDensity)
        return dpValue.dp  // Convert to Dp using the 'dp' extension
    }


    private fun dismissDialogs() {
        dialogBuilder.backupRestoreDialog?.dismiss()
        // dialogBuilder.saveLoadThemeDialog?.dismiss() // Remove theme dialog dismiss
    }

    private fun exitLauncher() {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(android.content.Intent.createChooser(intent, "Choose your launcher"))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.isinkosDefault()

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onStop() {
        super.onStop()
        dismissDialogs()
    }
}