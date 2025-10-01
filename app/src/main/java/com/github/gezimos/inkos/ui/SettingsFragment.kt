package com.github.gezimos.inkos.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
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
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Constants.Theme.Light
import com.github.gezimos.inkos.data.Constants.Theme.System
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.helper.utils.PrivateSpaceManager
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.FullLineSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SolidSeparator


class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private var rootLayout: android.widget.LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        val isDark = when (prefs.appTheme) {
            Light -> false
            Dark -> true
            System -> isSystemInDarkMode(requireContext())
        }
        val settingsSize = (prefs.settingsSize - 3)
        val backgroundColor = getHexForOpacity(prefs)
        val context = requireContext()
        val currentPage = intArrayOf(0)
        val pageCount = intArrayOf(1)

        val root = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout = root

        var bottomInsetPx = 0
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val navBarInset =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Helper to update header
        fun updateHeader(headerView: androidx.compose.ui.platform.ComposeView) {
            headerView.setContent {
                LocalDensity.current
                // Remove bottomInsetDp from header
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_inkos,
                            title = stringResource(R.string.settings_name),
                            onClick = {
                                val nav = findNavController()
                                val popped = try {
                                    nav.popBackStack(R.id.mainFragment, false)
                                } catch (_: Exception) {
                                    false
                                }
                                if (!popped) {
                                    try {
                                        // Ensure we land on the main/home fragment by clearing back stack
                                        val navOptions = androidx.navigation.NavOptions.Builder()
                                            .setPopUpTo(nav.graph.startDestinationId, true)
                                            .build()
                                        nav.navigate(R.id.mainFragment, null, navOptions)
                                    } catch (_: Exception) {
                                        nav.navigate(R.id.mainFragment)
                                    }
                                }
                            },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                // Small 'bmc' drawable tinted like the page indicator (white in dark mode, black in light)
                                val composeCtx = androidx.compose.ui.platform.LocalContext.current
                                val buyMeUri = "https://buymeacoffee.com/gezimos".toUri()
                                val buyMeIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, buyMeUri)
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.bmc),
                                    contentDescription = null,
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                        if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black
                                    ),
                                    modifier = Modifier
                                        .height(18.dp)
                                        .clickable {
                                            try {
                                                composeCtx.startActivity(buyMeIntent)
                                            } catch (_: Exception) {
                                                // Ignore failures to open browser
                                            }
                                        }
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        
                        // (No bottomInsetDp here)
                    }
                }
            }
        }

        // Add sticky header ComposeView
        val headerView = androidx.compose.ui.platform.ComposeView(context)
        updateHeader(headerView)
        root.addView(headerView)

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
                                    SettingsAllInOne(settingsSize.sp)
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
        // Use callback-enabled EinkScrollBehavior to update header page indicator
        val settingsScrollBehavior = EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            updateHeader(headerView)
        }
        settingsScrollBehavior.attachToScrollView(nestedScrollView)
        root.addView(
            nestedScrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Apply bottom padding to the root layout to prevent scroll view from going under navbar
        root.post {
            root.setPadding(0, 0, 0, bottomInsetPx)
            root.clipToPadding = false
        }

    // Header updates are driven by the EinkScrollBehavior callback above.
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Eink refresh: flash overlay if enabled
        com.github.gezimos.inkos.helper.utils.EinkRefreshHelper.refreshEink(
            requireContext(), prefs, null, prefs.einkRefreshDelay, useActivityRoot = true
        )
    }

    override fun onResume() {
        super.onResume()
        // Eink refresh: flash overlay if enabled
        com.github.gezimos.inkos.helper.utils.EinkRefreshHelper.refreshEink(
            requireContext(), prefs, null, prefs.einkRefreshDelay, useActivityRoot = true
        )
    }

    @Composable
    fun SettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified) {
        val navController = findNavController()
        val isDark = isSystemInDarkMode(requireContext())
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val iconSize = if (fontSize.isSpecified) tuToDp((fontSize * 0.8)) else tuToDp(fontSize)
        val privateSpaceManager = PrivateSpaceManager(requireContext())
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            FullLineSeparator(isDark = isDark)
            SettingsHomeItem(
                title = stringResource(R.string.settings_home_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showFeaturesSettings() },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = "App Drawer",
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    // Open Drawer settings page
                    navController.navigate(R.id.settingsDrawerFragment)
                },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = stringResource(R.string.fonts_settings_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showFontsSettings() },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = stringResource(R.string.settings_look_feel_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showLookFeelSettings() },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = stringResource(R.string.settings_gestures_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showGesturesSettings() },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = stringResource(R.string.notification_section),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showNotificationSettings() },
            )
            DashedSeparator(isDark = isDark)
            if (privateSpaceManager.isPrivateSpaceSupported() &&
                privateSpaceManager.isPrivateSpaceSetUp(showToast = false, launchSettings = false)
            ) {
                SettingsHomeItem(
                    title = stringResource(R.string.private_space),
                    titleFontSize = titleFontSize,
                    iconSize = iconSize,
                    onClick = {
                        privateSpaceManager.togglePrivateSpaceLock(
                            showToast = true,
                            launchSettings = true
                        )
                    }
                )
                DashedSeparator(isDark = isDark)
            }
            SettingsHomeItem(
                title = stringResource(R.string.settings_advanced_title) +
                        if (!isinkosDefault(requireContext())) "*" else "",
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showAdvancedSettings() },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = "Extras",
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    navController.navigate(R.id.extrasFragment)
                },
            )
            DashedSeparator(isDark = isDark)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    @Composable
    fun tuToDp(textUnit: TextUnit): Dp {
        val density = LocalDensity.current.density
        val scaledDensity = LocalDensity.current.fontScale
        val dpValue = textUnit.value * (density / scaledDensity)
        return dpValue.dp
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        viewModel.isinkosDefault()

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun showFeaturesSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsFeaturesFragment,
        )
    }

    private fun showFontsSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_fontsFragment
        )
    }

    private fun showLookFeelSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsLookFeelFragment,
        )
    }

    private fun showGesturesSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsGesturesFragment,
        )
    }

    private fun showAdvancedSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_settingsAdvancedFragment,
        )
    }

    private fun showNotificationSettings() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_notificationSettingsFragment
        )
    }
}