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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.FullLineSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SolidSeparator
import com.github.gezimos.inkos.ui.dialogs.DialogManager

class DrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
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
            Constants.Theme.Light -> false
            Constants.Theme.Dark -> true
            Constants.Theme.System -> isSystemInDarkMode(requireContext())
        }
        val settingsSize = (prefs.settingsSize - 3)
        val context = requireContext()

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
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.app_drawer),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {},
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
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
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    DrawerSettingsAllInOne((settingsSize).sp, isDark)
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

        return rootLayout
    }

    @Composable
    fun DrawerSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified, isDark: Boolean) {
        val navController = findNavController()
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val iconSize = if (fontSize.isSpecified) tuToDp((fontSize * 0.8)) else tuToDp(fontSize)
        
        var currentSize by remember { mutableStateOf(prefs.appDrawerSize) }
        var currentGap by remember { mutableStateOf(prefs.appDrawerGap) }
        var currentAlignment by remember { mutableStateOf(prefs.appDrawerAlignment) }
    var toggledAppDrawerPager by remember { mutableStateOf(prefs.appDrawerPager) }

        Column {
            FullLineSeparator(isDark)

            SettingsHomeItem(
                title = "App Drawer",
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    navController.navigate(
                        R.id.appDrawerListFragment,
                        bundleOf("flag" to Constants.AppDrawerFlag.LaunchApp.toString())
                    )
                }
            )
            DashedSeparator(isDark = isDark)

            SettingsHomeItem(
                title = "Hidden Apps",
                titleFontSize = titleFontSize,
                iconSize = iconSize,
                onClick = {
                    viewModel.getHiddenApps()
                    navController.navigate(
                        R.id.appDrawerListFragment,
                        bundleOf("flag" to Constants.AppDrawerFlag.HiddenApps.toString())
                    )
                }
            )
            FullLineSeparator(isDark)

            SettingsTitle(
                text = stringResource(R.string.customizations),
                fontSize = titleFontSize,
            )
            FullLineSeparator(isDark)

            SettingsSelect(
                title = stringResource(R.string.app_size),
                option = currentSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.app_size),
                        minValue = Constants.MIN_APP_SIZE,
                        maxValue = Constants.MAX_APP_SIZE,
                        currentValue = currentSize,
                        onValueSelected = { newVal ->
                            prefs.appDrawerSize = newVal
                            currentSize = newVal
                        }
                    )
                }
            )
            DashedSeparator(isDark)

            SettingsSelect(
                title = stringResource(R.string.app_padding_size),
                option = currentGap.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.app_padding_size),
                        minValue = Constants.MIN_TEXT_PADDING,
                        maxValue = Constants.MAX_TEXT_PADDING,
                        currentValue = currentGap,
                        onValueSelected = { newVal ->
                            prefs.appDrawerGap = newVal
                            currentGap = newVal
                        }
                    )
                }
            )
            DashedSeparator(isDark)

            // Cycle alignment between Left / Center / Right on each click (localized)
                // Cycle alignment between Left / Center / Right on each click (use short string keys)
                val alignmentLabels = listOf(
                    stringResource(R.string.left),
                    stringResource(R.string.center),
                    stringResource(R.string.right)
                )
            SettingsSelect(
                    title = stringResource(R.string.app_drawer_alignment),
                    option = alignmentLabels.getOrElse(currentAlignment) { stringResource(R.string.left) },
                fontSize = titleFontSize,
                onClick = {
                    val next = (currentAlignment + 1) % 3
                    prefs.appDrawerAlignment = next
                    currentAlignment = next
                }
            )
            DashedSeparator(isDark)
            com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch(
                text = stringResource(R.string.enable_home_pager),
                fontSize = titleFontSize,
                defaultState = toggledAppDrawerPager,
                onCheckedChange = {
                    toggledAppDrawerPager = !prefs.appDrawerPager
                    prefs.appDrawerPager = toggledAppDrawerPager
                }
            )
            FullLineSeparator(isDark = isDark)
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
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}
