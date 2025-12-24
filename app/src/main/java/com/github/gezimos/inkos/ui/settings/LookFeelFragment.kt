package com.github.gezimos.inkos.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.hideNavigationBar
import com.github.gezimos.inkos.helper.hideStatusBar
import com.github.gezimos.inkos.helper.showNavigationBar
import com.github.gezimos.inkos.helper.showStatusBar
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelectWithColorPreview
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager

class LookFeelFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var dialogBuilder: DialogManager

    // Callbacks for updating UI state
    private var onBackgroundOpacityChanged: ((Int) -> Unit)? = null
    // Minimum acceptable contrast ratio between text and background
    private val MIN_CONTRAST = 3.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewModel early so Compose content can read its state immediately.
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        prefs = viewModel.getPrefs()
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val backgroundColor = getHexForOpacity(requireContext())
        val isDark = prefs.appTheme == Constants.Theme.Dark
        val context = requireContext()
        val currentPage = intArrayOf(0)
        val pageCount = intArrayOf(1)

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

        // Marked as @Composable for Compose usage
        @Composable
        fun HeaderContent() {
            androidx.compose.ui.platform.LocalDensity.current
            // Observe settingsSize from ViewModel so header updates when font size changes
            val homeUiStateValue by viewModel.homeUiState.collectAsState()
            val settingsSize = (homeUiStateValue.settingsSize - 3)
            // Remove bottomInsetDp from header
            SettingsTheme(isDark) {
                Column(Modifier.fillMaxWidth()) {
                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = stringResource(R.string.look_feel_settings_title),
                        onClick = { findNavController().popBackStack() },
                        showStatusBar = homeUiStateValue.showStatusBar,
                        pageIndicator = {
                            com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                currentPage = currentPage[0],
                                pageCount = pageCount[0]
                            )
                        },
                        titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                    )
                }
            }
        }

        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent { HeaderContent() }
        }
        rootLayout.addView(headerView)

        val contentComposeView = androidx.compose.ui.platform.ComposeView(context)
        val nestedScrollView = androidx.core.widget.NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                contentComposeView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        
        contentComposeView.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
            // Observe settingsSize from ViewModel so content recomposes when font size changes
            val homeUiStateValue by viewModel.homeUiState.collectAsState()
            val settingsSize = (homeUiStateValue.settingsSize - 3)
            
            // Force scroll view to remeasure when settingsSize changes
            androidx.compose.runtime.LaunchedEffect(homeUiStateValue.settingsSize) {
                nestedScrollView.post {
                    nestedScrollView.requestLayout()
                    contentComposeView.requestLayout()
                }
            }
            
            SettingsTheme(isDark) {
                Box(Modifier.fillMaxSize()) {
                    Column {
                        LookFeelSettingsAllInOne(settingsSize.sp, isDark)
                        if (bottomInsetDp > 0.dp) {
                            Spacer(modifier = Modifier.height(bottomInsetDp))
                        }
                    }
                }
            }
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

        // Use EinkScrollBehavior callback to update header and page indicator
        val scrollBehavior = com.github.gezimos.inkos.helper.utils.EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            headerView.setContent { HeaderContent() }
        }
        scrollBehavior.attachToScrollView(nestedScrollView)
        return rootLayout
    }

    @Composable
    fun LookFeelSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified, isDark: Boolean) {
        findNavController()
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        // Ensure ViewModel is initialized before observing (prevents Android 15-16 lifecycle race)
        if (!::viewModel.isInitialized) {
            // Fallback UI or loading indicator if ViewModel isn't ready yet
            return
        }
        // Observe values from ViewModel so Compose updates when prefs change elsewhere
        val uiState by viewModel.homeUiState.collectAsState()
        
        Constants.updateMaxHomePages(requireContext())
        SettingsTheme(isDark) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Theme Mode
                    // DashedSeparator() removed as per patch requirement

            // Visibility & Display
            SettingsTitle(
                text = stringResource(R.string.visibility_display),
                fontSize = titleFontSize,
            )
                    // DashedSeparator() removed as per patch requirement
            SettingsSelect(
                title = stringResource(id = R.string.theme_mode),
                option = if (uiState.appTheme == Constants.Theme.Dark) "Dark" else "Light",
                fontSize = titleFontSize,
                onClick = {
                    // Toggle between Light and Dark only (System mode removed)
                    val newTheme = if (uiState.appTheme == Constants.Theme.Light) Constants.Theme.Dark else Constants.Theme.Light
                    viewModel.setAppTheme(newTheme)
                    
                    val isDarkTheme = newTheme == Constants.Theme.Dark
                    val bg = if (isDarkTheme) Color.Black.toArgb() else Color.White.toArgb()
                    val txt = if (isDarkTheme) Color.White.toArgb() else Color.Black.toArgb()
                    
                    viewModel.setBackgroundColor(bg)
                    viewModel.setTextColor(txt)
                    requireActivity().recreate()
                }
            )
                    // DashedSeparator() removed as per patch requirement
            SettingsSwitch(
                text = "Vibration Feedback",
                fontSize = titleFontSize,
                defaultState = uiState.hapticFeedback,
                onCheckedChange = { checked ->
                    viewModel.setHapticFeedback(checked)
                    try { VibrationHelper.setEnabled(checked) } catch (_: Exception) {}
                }
            )
                    // DashedSeparator() removed as per patch requirement
            SettingsSwitch(
                text = stringResource(R.string.show_status_bar),
                fontSize = titleFontSize,
                defaultState = uiState.showStatusBar,
                onCheckedChange = { checked ->
                    viewModel.setShowStatusBar(checked)
                    if (checked) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())
                }
            )
                    // DashedSeparator() removed as per patch requirement
            SettingsSwitch(
                text = stringResource(R.string.show_navigation_bar),
                fontSize = titleFontSize,
                defaultState = uiState.showNavigationBar,
                onCheckedChange = { checked ->
                    viewModel.setShowNavigationBar(checked)
                    if (checked) showNavigationBar(requireActivity()) else hideNavigationBar(requireActivity())
                }
            )
            
            // Element Colors
                    // DashedSeparator() removed as per patch requirement
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsTitle(
                    text = stringResource(R.string.element_colors),
                    fontSize = titleFontSize,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.reset),
                    style = SettingsTheme.typography.button,
                    fontSize = if (titleFontSize.isSpecified) (titleFontSize.value * 0.7).sp else 14.sp,
                    modifier = Modifier
                        .padding(end = SettingsTheme.color.horizontalPadding)
                        .clickable {
                            val isDarkMode = uiState.appTheme == Constants.Theme.Dark
                            val bg = if (isDarkMode) Color.Black.toArgb() else Color.White.toArgb()
                            val txt = if (isDarkMode) Color.White.toArgb() else Color.Black.toArgb()
                            
                            viewModel.setBackgroundColor(bg)
                            viewModel.setTextColor(txt)
                            
                            val lum = ColorUtils.calculateLuminance(bg)
                            val newTheme = if (lum < 0.5) Constants.Theme.Dark else Constants.Theme.Light
                            viewModel.setAppTheme(newTheme)
                            requireActivity().recreate()
                        }
                )
            }
                    // DashedSeparator() removed as per patch requirement
            SettingsSelect(
                title = "Set Wallpaper",
                option = "Open",
                fontSize = titleFontSize,
                onClick = {
                    findNavController().navigate(R.id.action_settingsLookFeelFragment_to_wallpaperFragment)
                }
            )
         // DashedSeparator() removed as per patch requirement
            SettingsSelect(
                title = "Background Opacity",
                option = "${uiState.backgroundOpacity.coerceIn(0,255)}",
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "Background Opacity",
                        minValue = 0,
                        maxValue = 255,
                        currentValue = uiState.backgroundOpacity,
                        onValueSelected = { newOpacity ->
                            viewModel.setBackgroundOpacity(newOpacity)
                        }
                    )
                }
            )

                    // DashedSeparator() removed as per patch requirement
            val hexBackgroundColor =
                String.format("#%06X", (0xFFFFFF and uiState.backgroundColor))
            SettingsSelectWithColorPreview(
                title = stringResource(R.string.background_color),
                hexColor = hexBackgroundColor,
                previewColor = Color(uiState.backgroundColor),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showColorPickerDialog(
                        context = requireContext(),
                        color = uiState.backgroundColor,
                        titleResId = R.string.background_color,
                        onItemSelected = { selectedColor ->
                            val contrast = ColorUtils.calculateContrast(uiState.textColor, selectedColor)
                            if (contrast < MIN_CONTRAST) {
                                showShortToast(
                                    "Selected background color is too similar to text color. Choose a different color."
                                )
                            } else {
                                viewModel.setBackgroundColor(selectedColor)
                                val lum = ColorUtils.calculateLuminance(selectedColor)
                                val newTheme = if (lum < 0.5) Constants.Theme.Dark else Constants.Theme.Light
                                viewModel.setAppTheme(newTheme)
                                requireActivity().recreate()
                            }
                        })
                }
            )
                    // DashedSeparator() removed as per patch requirement
            val hexTextColor = String.format("#%06X", (0xFFFFFF and uiState.textColor))
            SettingsSelectWithColorPreview(
                title = "Text Color",
                hexColor = hexTextColor,
                previewColor = Color(uiState.textColor),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showColorPickerDialog(
                        context = requireContext(),
                        color = uiState.textColor,
                        titleResId = R.string.text_color,
                        onItemSelected = { selectedColor ->
                            val contrast = ColorUtils.calculateContrast(selectedColor, uiState.backgroundColor)
                            if (contrast < MIN_CONTRAST) {
                                showShortToast(
                                    "Selected text color is too similar to background color. Choose a different color."
                                )
                            } else {
                                viewModel.setTextColor(selectedColor)
                                // Determine theme from text luminance: dark text -> Light theme
                                val lum = ColorUtils.calculateLuminance(selectedColor)
                                val newTheme = if (lum < 0.5) Constants.Theme.Light else Constants.Theme.Dark
                                viewModel.setAppTheme(newTheme)
                                requireActivity().recreate()
                            }
                        })
                }
            )
            
            // Text Islands Section
            SettingsTitle(
                text = "Text Islands",
                fontSize = titleFontSize,
            )
            SettingsSwitch(
                text = "Enable Text Islands",
                fontSize = titleFontSize,
                defaultState = uiState.textIslands,
                onCheckedChange = {
                    viewModel.setTextIslands(it)
                }
            )
            
            SettingsSwitch(
                text = "Invert Islands",
                fontSize = titleFontSize,
                defaultState = uiState.textIslandsInverted,
                onCheckedChange = {
                    viewModel.setTextIslandsInverted(it)
                }
            )
            
            // Corners selector moved to Icons & Buttons section
            
            // Icons Section
            SettingsTitle(
                text = "Icons & Buttons",
                fontSize = titleFontSize,
            )
            SettingsSwitch(
                text = "Show Icons",
                fontSize = titleFontSize,
                defaultState = uiState.showIcons,
                onCheckedChange = {
                    viewModel.setShowIcons(it)
                }
            )
            // Corners selector (moved here from Text Islands)
            val shapeLabels = listOf(
                "Pill",
                "Rounded",
                "Square"
            )
            SettingsSelect(
                title = "Corners",
                option = shapeLabels.getOrElse(uiState.textIslandsShape) { "Pill" },
                fontSize = titleFontSize,
                onClick = {
                    val next = (uiState.textIslandsShape + 1) % 3
                    viewModel.setTextIslandsShape(next)
                }
            )
                    // DashedSeparator() removed as per patch requirement
            }
        }
    }


    private fun dismissDialogs() {
        dialogBuilder.colorPickerDialog?.dismiss()
        dialogBuilder.singleChoiceDialog?.dismiss()
        dialogBuilder.sliderDialog?.dismiss()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)
        // Moved viewModel/prefs/dialogBuilder initialization to onCreateView to
        // ensure Compose content has access to ViewModel state early.
        // Keep other initializations here.

        deviceManager =
            context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
    }

    override fun onStop() {
        super.onStop()
        dismissDialogs()
    }
}