package com.github.gezimos.inkos.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Constants.Theme.Light
import com.github.gezimos.inkos.data.Constants.Theme.System
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.helper.utils.PrivateSpaceManager
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.FullLineSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator
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
                            iconRes = R.drawable.ic_home,
                            title = stringResource(R.string.settings_name),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0],
                                    titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        SolidSeparator(isDark = isDark)
                        Spacer(modifier = Modifier.height(SettingsTheme.color.horizontalPadding))
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
        EinkScrollBehavior(context).attachToScrollView(nestedScrollView)
        root.addView(
            nestedScrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // --- Calculate pages and listen for scroll changes ---
        fun getCurrentPageIndex(
            scrollY: Int,
            viewportHeight: Int,
            contentHeight: Int,
            pageCount: Int
        ): Int {
            if (contentHeight <= viewportHeight) return 0
            val overlap = (viewportHeight * 0.2).toInt()
            val scrollStep = viewportHeight - overlap
            val maxScroll = (contentHeight - viewportHeight).coerceAtLeast(1)
            val clampedScrollY = scrollY.coerceIn(0, maxScroll)
            val page = Math.round(clampedScrollY.toFloat() / scrollStep)
            return page.coerceIn(0, pageCount - 1)
        }
        nestedScrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val contentHeight = nestedScrollView.getChildAt(0)?.height ?: 1
            val viewportHeight = nestedScrollView.height.takeIf { it > 0 } ?: 1
            val overlap = (viewportHeight * 0.2).toInt()
            val scrollStep = viewportHeight - overlap
            val pages =
                Math.ceil(((contentHeight - viewportHeight).toDouble() / scrollStep.toDouble()))
                    .toInt() + 1
            pageCount[0] = pages
            val scrollY = nestedScrollView.scrollY
            currentPage[0] = getCurrentPageIndex(scrollY, viewportHeight, contentHeight, pages)
            updateHeader(headerView)
        }
        nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val contentHeight = nestedScrollView.getChildAt(0)?.height ?: 1
            val viewportHeight = nestedScrollView.height.takeIf { it > 0 } ?: 1
            val overlap = (viewportHeight * 0.2).toInt()
            val scrollStep = viewportHeight - overlap
            val pages =
                Math.ceil(((contentHeight - viewportHeight).toDouble() / scrollStep.toDouble()))
                    .toInt() + 1
            pageCount[0] = pages
            currentPage[0] = getCurrentPageIndex(scrollY, viewportHeight, contentHeight, pages)
            updateHeader(headerView)
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Eink refresh: flash overlay if enabled
        com.github.gezimos.inkos.helper.utils.EinkRefreshHelper.refreshEink(
            requireContext(), prefs, null, useActivityRoot = true
        )
    }

    override fun onResume() {
        super.onResume()
        // Eink refresh: flash overlay if enabled
        com.github.gezimos.inkos.helper.utils.EinkRefreshHelper.refreshEink(
            requireContext(), prefs, null, useActivityRoot = true
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
                title = stringResource(R.string.settings_features_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showFeaturesSettings() },
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
                title = stringResource(R.string.settings_advanced_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { showAdvancedSettings() },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = "Open App Drawer",
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    navController.navigate(
                        R.id.appDrawerListFragment,
                        bundleOf("flag" to AppDrawerFlag.LaunchApp.toString())
                    )
                },
            )
            DashedSeparator(isDark = isDark)
            SettingsHomeItem(
                title = stringResource(R.string.settings_exit_inkos_title),
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = { exitLauncher() },
            )
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

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
        findNavController().navigate(
            R.id.action_settingsFragment_to_appListFragment,
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString())
        )
    }

    private fun showFavoriteApps() {
        findNavController().navigate(
            R.id.action_settingsFragment_to_appFavoriteFragment,
            bundleOf("flag" to AppDrawerFlag.SetHomeApp.toString())
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

    private fun exitLauncher() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(intent, "Choose your launcher"))
    }
}