package com.github.gezimos.inkos.ui.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Constants.Theme.Light
import com.github.gezimos.inkos.data.Constants.Theme.System
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.hideNavigationBar
import com.github.gezimos.inkos.helper.hideStatusBar
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.helper.setThemeMode
import com.github.gezimos.inkos.helper.showNavigationBar
import com.github.gezimos.inkos.helper.showStatusBar
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelectWithColorPreview
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SolidSeparator
import com.github.gezimos.inkos.ui.dialogs.DialogManager

class LookFeelFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var dialogBuilder: DialogManager

    // Callbacks for updating UI state
    private var onHomeImageChanged: ((String?) -> Unit)? = null
    private var onHomeImageOpacityChanged: ((Int) -> Unit)? = null

    // Activity result launchers for image picking
    private val homeBackgroundImagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Check if image needs optimization before setting
            try {
                val contentResolver = requireContext().contentResolver
                val dimensionOptions = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                contentResolver.openInputStream(it)?.use { inputStream ->
                    android.graphics.BitmapFactory.decodeStream(inputStream, null, dimensionOptions)
                }

                val originalWidth = dimensionOptions.outWidth
                val originalHeight = dimensionOptions.outHeight

                // Calculate screen size limits (2x resolution max)
                val display = requireActivity().windowManager.defaultDisplay
                val displayMetrics = android.util.DisplayMetrics()
                display.getMetrics(displayMetrics)
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                val maxWidth = screenWidth * 2
                val maxHeight = screenHeight * 2

                // Show optimization notice if image will be downsampled
                if (originalWidth > maxWidth || originalHeight > maxHeight) {
                    showShortToast("Large image will be optimized for performance (${originalWidth}×${originalHeight} → ~${maxWidth}×${maxHeight})")
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "LookFeelFragment",
                    "Error checking image dimensions: ${e.message}"
                )
            }

            // Persist URI permission
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                prefs.homeBackgroundImageUri = it.toString()
                // Trigger MainViewModel LiveData update
                viewModel.homeBackgroundImageUri.postValue(it.toString())
                onHomeImageChanged?.invoke(it.toString())
            } catch (e: Exception) {
                android.util.Log.e(
                    "LookFeelFragment",
                    "Error persisting home background URI: ${e.message}"
                )
            }
        }
    }

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
            // Remove bottomInsetDp from header
            SettingsTheme(isDark) {
                Column(Modifier.fillMaxWidth()) {
                    PageHeader(
                        iconRes = R.drawable.ic_back,
                        title = stringResource(R.string.look_feel_settings_title),
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
                }
            }
        }

        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent { HeaderContent() }
        }
        rootLayout.addView(headerView)

        val nestedScrollView = androidx.core.widget.NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                androidx.compose.ui.platform.ComposeView(context).apply {
                    setContent {
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
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
        val toggledShowStatusBar = remember { mutableStateOf(prefs.showStatusBar) }
        val toggledShowNavigationBar = remember { mutableStateOf(prefs.showNavigationBar) }
        val selectedTheme = remember { mutableStateOf(prefs.appTheme) }
        val selectedBackgroundColor = remember { mutableStateOf(prefs.backgroundColor) }
        val selectedAppColor = remember { mutableStateOf(prefs.appColor) }
        val selectedClockColor = remember { mutableStateOf(prefs.clockColor) }
        val selectedBatteryColor = remember { mutableStateOf(prefs.batteryColor) }
        val selectedDateColor = remember { mutableStateOf(prefs.dateColor) }
        val selectedQuoteColor = remember { mutableStateOf(prefs.quoteColor) }
        val selectedAudioWidgetColor = remember { mutableStateOf<Int>(prefs.audioWidgetColor) }
        val vibrationForPaging = remember { mutableStateOf(prefs.useVibrationForPaging) }
        val homeBackgroundImageUri = remember { mutableStateOf(prefs.homeBackgroundImageUri) }
        val homeBackgroundImageOpacity =
            remember { mutableStateOf(prefs.homeBackgroundImageOpacity) }

        // Set up callbacks for updating state
        onHomeImageChanged = { uri -> homeBackgroundImageUri.value = uri }
        onHomeImageOpacityChanged = { opacity -> homeBackgroundImageOpacity.value = opacity }

        // Clean up callbacks when composable is disposed
        androidx.compose.runtime.DisposableEffect(Unit) {
            onDispose {
                onHomeImageChanged = null
                onHomeImageOpacityChanged = null
            }
        }

        Constants.updateMaxHomePages(requireContext())
        Column(modifier = Modifier.fillMaxSize()) {
            // Theme Mode
            DashedSeparator(isDark)

            // Visibility & Display
            SettingsTitle(
                text = stringResource(R.string.visibility_display),
                fontSize = titleFontSize,
            )
            DashedSeparator(isDark)
            SettingsSelect(
                title = stringResource(id = R.string.theme_mode),
                option = when (selectedTheme.value) {
                    Constants.Theme.System -> "System"
                    Constants.Theme.Light -> "Light"
                    Constants.Theme.Dark -> "Dark"
                    else -> "System"
                },
                fontSize = titleFontSize,
                onClick = {
                    selectedTheme.value = when (selectedTheme.value) {
                        Constants.Theme.System -> Constants.Theme.Light
                        Constants.Theme.Light -> Constants.Theme.Dark
                        Constants.Theme.Dark -> Constants.Theme.System
                        else -> Constants.Theme.System
                    }
                    prefs.appTheme = selectedTheme.value
                    val isDark = when (selectedTheme.value) {
                        Constants.Theme.Light -> false
                        Constants.Theme.Dark -> true
                        Constants.Theme.System -> isSystemInDarkMode(requireContext())
                        else -> false
                    }
                    selectedBackgroundColor.value =
                        if (isDark) Color.Black.toArgb() else Color.White.toArgb()
                    selectedAppColor.value =
                        if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                    selectedClockColor.value =
                        if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                    selectedBatteryColor.value =
                        if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                    selectedDateColor.value =
                        if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                    selectedQuoteColor.value =
                        if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                    selectedAudioWidgetColor.value =
                        if (isDark) Color.White.toArgb() else Color.Black.toArgb()
                    prefs.backgroundColor = selectedBackgroundColor.value
                    prefs.appColor = selectedAppColor.value
                    prefs.clockColor = selectedClockColor.value
                    prefs.batteryColor = selectedBatteryColor.value
                    prefs.dateColor = selectedDateColor.value
                    prefs.quoteColor = selectedQuoteColor.value
                    prefs.audioWidgetColor = selectedAudioWidgetColor.value
                    setThemeMode(
                        requireContext(),
                        isDark,
                        requireActivity().window.decorView
                    )
                    requireActivity().recreate()
                }
            )
            DashedSeparator(isDark)
            SettingsSwitch(
                text = "Vibration Feedback",
                fontSize = titleFontSize,
                defaultState = vibrationForPaging.value,
                onCheckedChange = {
                    vibrationForPaging.value = it
                    prefs.useVibrationForPaging = it
                }
            )
            DashedSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.show_status_bar),
                fontSize = titleFontSize,
                defaultState = toggledShowStatusBar.value,
                onCheckedChange = {
                    toggledShowStatusBar.value = !prefs.showStatusBar
                    prefs.showStatusBar = toggledShowStatusBar.value
                    if (toggledShowStatusBar.value) showStatusBar(requireActivity()) else hideStatusBar(
                        requireActivity()
                    )
                }
            )
            DashedSeparator(isDark)
            SettingsSwitch(
                text = stringResource(R.string.show_navigation_bar),
                fontSize = titleFontSize,
                defaultState = toggledShowNavigationBar.value,
                onCheckedChange = {
                    toggledShowNavigationBar.value = !prefs.showNavigationBar
                    prefs.showNavigationBar = toggledShowNavigationBar.value
                    if (toggledShowNavigationBar.value) showNavigationBar(requireActivity()) else hideNavigationBar(
                        requireActivity()
                    )
                }
            )

            DashedSeparator(isDark)
            // Background Images
            SettingsTitle(
                text = "Background Images",
                fontSize = titleFontSize,
            )
            DashedSeparator(isDark)

            SettingsSelect(
                title = "Home Image",
                option = if (homeBackgroundImageUri.value != null) "Clear" else "Set",
                fontSize = titleFontSize,
                onClick = {
                    if (homeBackgroundImageUri.value != null) {
                        // Clear the image
                        prefs.homeBackgroundImageUri = null
                        homeBackgroundImageUri.value = null
                        // Trigger MainViewModel LiveData update
                        viewModel.homeBackgroundImageUri.postValue(null)
                    } else {
                        // Set a new image
                        homeBackgroundImagePicker.launch(arrayOf("image/*"))
                    }
                }
            )

            if (homeBackgroundImageUri.value != null) {
                DashedSeparator(isDark)
                SettingsSelect(
                    title = "Image Opacity",
                    option = "${homeBackgroundImageOpacity.value}%",
                    fontSize = titleFontSize,
                    onClick = {
                        dialogBuilder.showSliderDialog(
                            context = requireContext(),
                            title = "Image Opacity",
                            minValue = 10,
                            maxValue = 100,
                            currentValue = homeBackgroundImageOpacity.value,
                            onValueSelected = { newOpacity ->
                                homeBackgroundImageOpacity.value = newOpacity
                                prefs.homeBackgroundImageOpacity = newOpacity
                                // Trigger MainViewModel LiveData update
                                viewModel.homeBackgroundImageOpacity.postValue(newOpacity)
                                onHomeImageOpacityChanged?.invoke(newOpacity)
                            }
                        )
                    }
                )
            }

        }


        // Element Colors
    DashedSeparator(isDark)
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
                        val isDarkMode = when (prefs.appTheme) {
                            Dark -> true
                            Light -> false
                            System -> isSystemInDarkMode(requireContext())
                        }
                        selectedBackgroundColor.value =
                            if (isDarkMode) Color.Black.toArgb() else Color.White.toArgb()
                        selectedAppColor.value =
                            if (isDarkMode) Color.White.toArgb() else Color.Black.toArgb()
                        selectedClockColor.value =
                            if (isDarkMode) Color.White.toArgb() else Color.Black.toArgb()
                        selectedBatteryColor.value =
                            if (isDarkMode) Color.White.toArgb() else Color.Black.toArgb()
                        selectedDateColor.value =
                            if (isDarkMode) Color.White.toArgb() else Color.Black.toArgb()
                        selectedQuoteColor.value =
                            if (isDarkMode) Color.White.toArgb() else Color.Black.toArgb()
                        selectedAudioWidgetColor.value =
                            if (isDarkMode) Color.White.toArgb() else Color.Black.toArgb()
                        prefs.backgroundColor = selectedBackgroundColor.value
                        prefs.appColor = selectedAppColor.value
                        prefs.clockColor = selectedClockColor.value
                        prefs.batteryColor = selectedBatteryColor.value
                        prefs.dateColor = selectedDateColor.value
                        prefs.quoteColor = selectedQuoteColor.value
                        prefs.audioWidgetColor = selectedAudioWidgetColor.value
                    }
            )
        }
    DashedSeparator(isDark)
        val hexBackgroundColor =
            String.format("#%06X", (0xFFFFFF and selectedBackgroundColor.value))
        SettingsSelectWithColorPreview(
            title = stringResource(R.string.background_color),
            hexColor = hexBackgroundColor,
            previewColor = Color(selectedBackgroundColor.value),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showColorPickerDialog(
                    context = requireContext(),
                    color = selectedBackgroundColor.value,
                    titleResId = R.string.background_color,
                    onItemSelected = { selectedColor ->
                        selectedBackgroundColor.value = selectedColor
                        prefs.backgroundColor = selectedColor
                    })
            }
        )
        DashedSeparator(isDark)
        val hexAppColor = String.format("#%06X", (0xFFFFFF and selectedAppColor.value))
        SettingsSelectWithColorPreview(
            title = stringResource(R.string.app_color),
            hexColor = hexAppColor,
            previewColor = Color(selectedAppColor.value),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showColorPickerDialog(
                    context = requireContext(),
                    color = selectedAppColor.value,
                    titleResId = R.string.app_color,
                    onItemSelected = { selectedColor ->
                        selectedAppColor.value = selectedColor
                        prefs.appColor = selectedColor
                    })
            }
        )

        DashedSeparator(isDark)
        val hexClockColor = String.format("#%06X", (0xFFFFFF and selectedClockColor.value))
        SettingsSelectWithColorPreview(
            title = stringResource(R.string.clock_color),
            hexColor = hexClockColor,
            previewColor = Color(selectedClockColor.value),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showColorPickerDialog(
                    context = requireContext(),
                    color = selectedClockColor.value,
                    titleResId = R.string.clock_color,
                    onItemSelected = { selectedColor ->
                        selectedClockColor.value = selectedColor
                        prefs.clockColor = selectedColor
                    })
            }
        )
        DashedSeparator(isDark)
        val hexBatteryColor = String.format("#%06X", (0xFFFFFF and selectedBatteryColor.value))
        SettingsSelectWithColorPreview(
            title = stringResource(R.string.battery_color),
            hexColor = hexBatteryColor,
            previewColor = Color(selectedBatteryColor.value),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showColorPickerDialog(
                    context = requireContext(),
                    color = selectedBatteryColor.value,
                    titleResId = R.string.battery_color,
                    onItemSelected = { selectedColor ->
                        selectedBatteryColor.value = selectedColor
                        prefs.batteryColor = selectedColor
                    })
            }
        )
        DashedSeparator(isDark)
        val hexDateColor = String.format("#%06X", (0xFFFFFF and selectedDateColor.value))
        SettingsSelectWithColorPreview(
            title = stringResource(R.string.date_color),
            hexColor = hexDateColor,
            previewColor = Color(selectedDateColor.value),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showColorPickerDialog(
                    context = requireContext(),
                    color = selectedDateColor.value,
                    titleResId = R.string.date_color,
                    onItemSelected = { selectedColor ->
                        selectedDateColor.value = selectedColor
                        prefs.dateColor = selectedColor
                    })
            }
        )
        DashedSeparator(isDark)
        val hexQuoteColor = String.format("#%06X", (0xFFFFFF and selectedQuoteColor.value))
        SettingsSelectWithColorPreview(
            title = stringResource(R.string.quote_color),
            hexColor = hexQuoteColor,
            previewColor = Color(selectedQuoteColor.value),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showColorPickerDialog(
                    context = requireContext(),
                    color = selectedQuoteColor.value,
                    titleResId = R.string.quote_color,
                    onItemSelected = { selectedColor ->
                        selectedQuoteColor.value = selectedColor
                        prefs.quoteColor = selectedColor
                    }
                )
            }
        )            // Audio Widget Color
        DashedSeparator(isDark)
        val hexAudioWidgetColor =
            String.format("#%06X", (0xFFFFFF and selectedAudioWidgetColor.value))
        SettingsSelectWithColorPreview(
            title = "Audio widget",
            hexColor = hexAudioWidgetColor,
            previewColor = Color(selectedAudioWidgetColor.value),
            fontSize = titleFontSize,
            onClick = {
                dialogBuilder.showColorPickerDialog(
                    context = requireContext(),
                    color = selectedAudioWidgetColor.value,
                    titleResId = R.string.quote_color, // Reuse existing string for now
                    onItemSelected = { selectedColor ->
                        selectedAudioWidgetColor.value = selectedColor
                        prefs.audioWidgetColor = selectedColor
                    }
                )
            }
        )
    DashedSeparator(isDark)


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

    override fun onStop() {
        super.onStop()
        dismissDialogs()
    }
}