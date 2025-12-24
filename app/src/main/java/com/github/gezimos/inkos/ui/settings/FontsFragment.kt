package com.github.gezimos.inkos.ui.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getTrueSystemFont
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FontsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var viewModel: com.github.gezimos.inkos.MainViewModel
    private lateinit var dialogBuilder: DialogManager
    private val PICK_FONT_FILE_REQUEST_CODE = 1001
    private var onCustomFontSelected: ((Typeface, String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = androidx.lifecycle.ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val backgroundColor = com.github.gezimos.inkos.helper.getHexForOpacity(requireContext())
        val isDark = prefs.appTheme == Dark

        val context = requireContext()
    // --- Dot indicator state ---
    val currentPage = intArrayOf(0)
    val pageCount = intArrayOf(1)

        // Track bottom inset for padding in Compose header/content
        var bottomInsetPx = 0

        // Create sticky header ComposeView (we'll update its content from the scroll behavior callback)
        val headerView = ComposeView(context).apply {
            setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                val density = LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.fonts_settings_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
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

        // Add scrollable settings content
        val nestedScrollView = NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                ComposeView(context).apply {
                    setContent {
                        val homeUiState by viewModel.homeUiState.collectAsState()
                        val settingsSize = (homeUiState.settingsSize - 3)
                        val density = LocalDensity.current
                        val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    FontsSettingsAllInOne(settingsSize.sp, isDark)
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

        // Create a vertical LinearLayout to hold sticky header and scrollable content
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            // Match LookFeelFragment: set root background so header isn't transparent
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(headerView)
            addView(
                nestedScrollView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Create behavior with a callback to update page indicator reliably
        val scrollBehavior = EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            // Update header compose content to reflect the new page indicator state
            headerView.setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.fonts_settings_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
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
        }

        scrollBehavior.attachToScrollView(nestedScrollView)

        // Apply bottom padding to the root layout to prevent scroll view from going under navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

    // Page calculation and indicator updating are handled by EinkScrollBehavior callback

        return rootLayout
    }

    @Composable
    fun FontsSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified, isDark: Boolean) {
        findNavController()
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        
        // Observe ViewModel state
        val homeUiState by viewModel.homeUiState.collectAsState()

        // Universal Font Section State (Not in HomeUiState)
        var universalFontState by remember { mutableStateOf(prefs.universalFont) }
        var universalFontEnabledState by remember { mutableStateOf(prefs.universalFontEnabled) }
        var settingsFontState by remember { mutableStateOf(prefs.fontFamily) }
        
        // Notification Fonts Section State (Sizes/Titles not in HomeUiState)
        var labelnotificationsFontSize by remember { mutableStateOf(prefs.labelnotificationsTextSize) }
        var notificationsTitle by remember { mutableStateOf(prefs.lettersTitle) }
        var notificationsTitleSize by remember { mutableStateOf(prefs.lettersTitleSize) }
        var notificationsTextSize by remember { mutableStateOf(prefs.notificationsTextSize) }

        // Derived state for app name mode
        val appNameMode = when {
            homeUiState.allCapsApps -> 2
            homeUiState.smallCapsApps -> 1
            else -> 0
        }

        // Use Column instead of LazyColumn (let parent NestedScrollView handle scrolling)
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Universal Custom Font Section (top, with Reset All on right) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsTitle(
                    text = stringResource(R.string.universal_custom_font),
                    fontSize = titleFontSize,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Reset All",
                    style = SettingsTheme.typography.button,
                    fontSize = if (titleFontSize.isSpecified) (titleFontSize.value * 0.7).sp else 14.sp,
                    modifier = Modifier
                        .padding(end = SettingsTheme.color.horizontalPadding)
                        .clickable {
                                                // Reset fonts centrally in ViewModel
                                                viewModel.resetFontsToDefault()
                                
                                // Reset sizes to defaults - using values from Prefs.kt getInt defaults
                                // settingsSize default: 16 (from TEXT_SIZE_SETTINGS)
                                viewModel.setSettingsSize(16)
                                // appSize default: 27 (from APP_SIZE_TEXT)
                                viewModel.setAppSize(27)
                                // clockSize default: 48 (from CLOCK_SIZE_TEXT, catch returns 64 but default is 48)
                                viewModel.setClockSize(48)
                                // labelnotificationsTextSize default: 16 (from "notificationTextSize")
                                viewModel.setLabelNotificationsTextSize(16)
                                // dateSize default: 18 (from Constants.PrefKeys.DATE_SIZE_TEXT)
                                viewModel.setDateSize(18)
                                // quoteSize default: 18 (from QUOTE_TEXT_SIZE)
                                viewModel.setQuoteSize(18)
                                // lettersTextSize default: 18 (from LETTERS_TEXT_SIZE)
                                viewModel.setLettersTextSize(18)
                                // lettersTitleSize default: 36 (from LETTERS_TITLE_SIZE)
                                viewModel.setLettersTitleSize(36)
                                // notificationsTextSize default: 18 (from NOTIFICATIONS_TEXT_SIZE)
                                viewModel.setNotificationsTextSize(18)
                                // lettersTitle default: "Letters" (from LETTERS_TITLE)
                                viewModel.setLettersTitle("Letters")
                                
                                // Update local state
                                universalFontState = Constants.FontFamily.PublicSans
                                universalFontEnabledState = false
                                settingsFontState = Constants.FontFamily.PublicSans
                                labelnotificationsFontSize = 16
                                notificationsTitle = "Letters"
                                notificationsTitleSize = 36
                                notificationsTextSize = 18
                            }
                )
            }
            SettingsSwitch(
                text = stringResource(R.string.universal_custom_font),
                fontSize = titleFontSize,
                defaultState = universalFontEnabledState,
                onCheckedChange = { enabled ->
                    viewModel.setUniversalFontEnabled(enabled)
                    universalFontEnabledState = enabled
                    if (enabled) {
                        val font = prefs.universalFont
                        val fontPath = if (font == Constants.FontFamily.Custom) prefs.getCustomFontPath("universal") else null
                        viewModel.setFontFamily(font)
                        viewModel.setAppsFont(font)
                        viewModel.setClockFont(font)
                        viewModel.setStatusFont(font)
                        viewModel.setLabelNotificationsFont(font)
                        viewModel.setNotificationsFont(font)
                        viewModel.setLettersTitleFont(font)
                        viewModel.setDateFont(font)
                        viewModel.setQuoteFont(font)
                        viewModel.setLettersFont(font)
                        if (font == Constants.FontFamily.Custom && fontPath != null) {
                            val keys = listOf(
                                "universal",
                                "settings",
                                "apps",
                                "clock",
                                "status",
                                "notification",
                                "notifications",
                                "lettersTitle",
                                "date",
                                "quote",
                                "letters"
                            )
                            for (key in keys) viewModel.setCustomFontPath(key, fontPath)
                        }
                        settingsFontState = font
                    }
                }
            )
            SettingsSelect(
                title = stringResource(R.string.universal_custom_font_selector),
                option = getFontDisplayName(universalFontState, "universal"),
                fontSize = titleFontSize,
                onClick = {
                    showFontSelectionDialogWithCustoms(
                        R.string.universal_custom_font_selector,
                        "universal"
                        ) { newFont, customPath ->
                        viewModel.setUniversalFont(newFont)
                        universalFontState = newFont
                        val fontPath = if (newFont == Constants.FontFamily.Custom) customPath else null
                        if (prefs.universalFontEnabled) {
                            viewModel.setFontFamily(newFont)
                            viewModel.setAppsFont(newFont)
                            viewModel.setClockFont(newFont)
                            viewModel.setStatusFont(newFont)
                            viewModel.setLabelNotificationsFont(newFont)
                            viewModel.setNotificationsFont(newFont)
                            viewModel.setLettersTitleFont(newFont)
                            viewModel.setDateFont(newFont)
                            viewModel.setQuoteFont(newFont)
                            viewModel.setLettersFont(newFont)
                            if (newFont == Constants.FontFamily.Custom && fontPath != null) {
                                val keys = listOf(
                                    "universal",
                                    "settings",
                                    "apps",
                                    "clock",
                                    "status",
                                    "notification",
                                    "notifications",
                                    "lettersTitle",
                                    "date",
                                    "quote",
                                    "letters"
                                )
                                for (key in keys) viewModel.setCustomFontPath(key, fontPath)
                            }
                            settingsFontState = newFont
                        }
                        // Refresh the fragment to show changes immediately
                        activity?.recreate()
                    }
                },
                enabled = prefs.universalFontEnabled
            )
            SettingsSelect(
                title = stringResource(R.string.settings_font_section),
                option = getFontDisplayName(settingsFontState, "settings"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.settings_font_section,
                            "settings"
                        ) { newFont, customPath ->
                            viewModel.setFontFamily(newFont)
                            settingsFontState = newFont
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            SettingsSelect(
                title = stringResource(R.string.settings_text_size),
                option = homeUiState.settingsSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = getString(R.string.settings_text_size),
                        minValue = Constants.MIN_SETTINGS_TEXT_SIZE,
                        maxValue = Constants.MAX_SETTINGS_TEXT_SIZE,
                        currentValue = homeUiState.settingsSize,
                        onValueSelected = { newSize ->
                            viewModel.setSettingsSize(newSize)
                        }
                    )
                }
            )

            // --- Home Fonts Section ---
            
            SettingsTitle(
                text = "Home Fonts",
                fontSize = titleFontSize
            )
            

            // Apps Font
            SettingsSelect(
                title = stringResource(R.string.apps_font),
                option = getFontDisplayName(homeUiState.appsFont, "apps"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.apps_font,
                            "apps"
                        ) { newFont, customPath ->
                            viewModel.setAppsFont(newFont)
                            customPath?.let { viewModel.setCustomFontPath("apps", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            SettingsSelect(
                title = stringResource(R.string.app_text_size),
                option = homeUiState.appSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.app_text_size),
                        minValue = Constants.MIN_APP_SIZE,
                        maxValue = Constants.MAX_APP_SIZE,
                        currentValue = homeUiState.appSize,
                        onValueSelected = { newAppSize ->
                            viewModel.setAppSize(newAppSize)
                        }
                    )
                }
            )
            val appNameModeLabels = listOf(
                stringResource(R.string.app_name_mode_normal),
                stringResource(R.string.small_caps_apps),
                stringResource(R.string.app_name_mode_all_caps)
            )
            SettingsSelect(
                title = stringResource(R.string.app_name_mode),
                option = appNameModeLabels[appNameMode],
                fontSize = titleFontSize,
                onClick = {
                    val nextMode = (appNameMode + 1) % 3
                    viewModel.setSmallCapsApps(nextMode == 1)
                    viewModel.setAllCapsApps(nextMode == 2)
                }
            )

            // Clock Font
            SettingsSelect(
                title = stringResource(R.string.clock_font),
                option = getFontDisplayName(homeUiState.clockFont, "clock"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.clock_font,
                            "clock"
                        ) { newFont, customPath ->
                            viewModel.setClockFont(newFont)
                            customPath?.let { viewModel.setCustomFontPath("clock", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            SettingsSelect(
                title = stringResource(R.string.clock_text_size),
                option = homeUiState.clockSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.clock_text_size),
                        minValue = Constants.MIN_CLOCK_SIZE,
                        maxValue = Constants.MAX_CLOCK_SIZE,
                        currentValue = homeUiState.clockSize,
                        onValueSelected = { newClockSize ->
                            viewModel.setClockSize(newClockSize)
                        }
                    )
                }
            )
            // Date Font
            SettingsSelect(
                title = stringResource(R.string.date_font),
                option = getFontDisplayName(homeUiState.dateFont, "date"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.date_font,
                            "date"
                        ) { newFont, customPath ->
                            viewModel.setDateFont(newFont)
                            customPath?.let { viewModel.setCustomFontPath("date", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            SettingsSelect(
                title = stringResource(R.string.date_text_size),
                option = homeUiState.dateSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.date_text_size),
                        minValue = 10,
                        maxValue = 64,
                        currentValue = homeUiState.dateSize,
                        onValueSelected = { newSize ->
                            viewModel.setDateSize(newSize)
                        }
                    )
                }
            )
            




            // Quote Font Section
            SettingsSelect(
                title = stringResource(R.string.quote_font),
                option = getFontDisplayName(homeUiState.quoteFont, "quote"),
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.quote_font,
                            "quote"
                        ) { newFont, customPath ->
                            viewModel.setQuoteFont(newFont)
                            customPath?.let { viewModel.setCustomFontPath("quote", it) }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            
            SettingsSelect(
                title = stringResource(R.string.quote_text_size),
                option = homeUiState.quoteSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = requireContext().getString(R.string.quote_text_size),
                        minValue = 10,
                        maxValue = 64,
                        currentValue = homeUiState.quoteSize,
                        onValueSelected = { newQuoteSize ->
                            viewModel.setQuoteSize(newQuoteSize)
                        }
                    )
                }
            )
            

            SettingsTitle(
                text = "Label Notifications",
                fontSize = titleFontSize
            )
            
            SettingsSelect(
                title = "Label Notifications Font",
                option = if (homeUiState.notificationFont == Constants.FontFamily.Custom)
                    getFontDisplayName(homeUiState.notificationFont, "notification")
                else homeUiState.notificationFont.name,
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.app_notification_font,
                            "notification"
                        ) { newFont, customPath ->
                            viewModel.setLabelNotificationsFont(newFont)
                            if (newFont == Constants.FontFamily.Custom && customPath != null) {
                                viewModel.setCustomFontPath("notification", customPath)
                            }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            SettingsSelect(
                title = "Label Notifications Size",
                option = labelnotificationsFontSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "Label Notifications Size",
                        minValue = Constants.MIN_LABEL_NOTIFICATION_TEXT_SIZE,
                        maxValue = Constants.MAX_LABEL_NOTIFICATION_TEXT_SIZE,
                        currentValue = prefs.labelnotificationsTextSize,
                        onValueSelected = { newSize ->
                            viewModel.setLabelNotificationsTextSize(newSize)
                            labelnotificationsFontSize = newSize
                        }
                    )
                }
            )

            SettingsTitle(
                text = "Notifications Window",
                fontSize = titleFontSize
            )
            
            SettingsSelect(
                title = "Window Title",
                option = notificationsTitle,
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showInputDialog(
                        context = requireContext(),
                        title = "Title",
                        initialValue = notificationsTitle,
                            onValueEntered = { newTitle ->
                            // Remove any newline characters to enforce single line
                            val singleLineTitle = newTitle.replace("\n", "")
                            viewModel.setLettersTitle(singleLineTitle)
                            notificationsTitle = singleLineTitle
                        }
                    )
                }
            )
            SettingsSelect(
                title = "Title Font",
                option = if (homeUiState.lettersTitleFont == Constants.FontFamily.Custom)
                    getFontDisplayName(homeUiState.lettersTitleFont, "lettersTitle")
                else homeUiState.lettersTitleFont.name,
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.notifications_font,
                            "lettersTitle"
                        ) { newFont, customPath ->
                            viewModel.setLettersTitleFont(newFont)
                            if (newFont == Constants.FontFamily.Custom && customPath != null) {
                                viewModel.setCustomFontPath("lettersTitle", customPath)
                            }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            SettingsSelect(
                title = "Title Size",
                option = notificationsTitleSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "Title Size",
                        minValue = 10,
                        maxValue = 60,
                        currentValue = notificationsTitleSize,
                            onValueSelected = { newSize ->
                            viewModel.setLettersTitleSize(newSize)
                            notificationsTitleSize = newSize
                        }
                    )
                }
            )
            SettingsSelect(
                title = "Body Font",
                option = if (universalFontEnabledState) {
                    val universalFont = prefs.universalFont
                    if (universalFont == Constants.FontFamily.Custom)
                        getFontDisplayName(universalFont, "universal")
                    else universalFont.name
                } else if (homeUiState.notificationsFont == Constants.FontFamily.Custom)
                    getFontDisplayName(homeUiState.notificationsFont, "notifications")
                else homeUiState.notificationsFont.name,
                fontSize = titleFontSize,
                onClick = {
                    if (!universalFontEnabledState) {
                        showFontSelectionDialogWithCustoms(
                            R.string.notifications_font,
                            "notifications"
                        ) { newFont, customPath ->
                            viewModel.setNotificationsFont(newFont)
                            if (newFont == Constants.FontFamily.Custom && customPath != null) {
                                viewModel.setCustomFontPath("notifications", customPath)
                            }
                        }
                    }
                },
                fontColor = if (!universalFontEnabledState)
                    SettingsTheme.typography.title.color
                else Color.Gray,
                enabled = !universalFontEnabledState
            )
            SettingsSelect(
                title = "Body Text Size",
                option = notificationsTextSize.toString(),
                fontSize = titleFontSize,
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(),
                        title = "Body Text Size",
                        minValue = Constants.MIN_LABEL_NOTIFICATION_TEXT_SIZE,
                        maxValue = Constants.MAX_LABEL_NOTIFICATION_TEXT_SIZE,
                        currentValue = notificationsTextSize,
                            onValueSelected = { newSize ->
                            viewModel.setNotificationsTextSize(newSize)
                            notificationsTextSize = newSize
                        }
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    private fun getFontDisplayName(font: Constants.FontFamily, contextKey: String): String {
        val fontName = if (font == Constants.FontFamily.Custom) {
            val path = if (contextKey == "notifications") {
                // Use the correct custom font path for notifications
                prefs.getCustomFontPath("notifications")
                    ?: prefs.getCustomFontPath("universal")
            } else {
                prefs.getCustomFontPathForContext(contextKey)
            }
            path?.let { File(it).name } ?: font.name
        } else {
            font.name
        }

        // Truncate font names to 12 characters with "..." if longer
        return if (fontName.length > 12) "${fontName.take(12)}..." else fontName
    }

    private fun showFontSelectionDialogWithCustoms(
        titleResId: Int,
        contextKey: String,
        onFontSelected: (Constants.FontFamily, String?) -> Unit
    ) {
        val fontFamilyEntries = Constants.FontFamily.entries
            .filter { it != Constants.FontFamily.Custom }
        val context = requireContext()
        val prefs = Prefs(context)

        val builtInFontOptions = fontFamilyEntries.map { it.getString(context) }
        val builtInFonts = fontFamilyEntries.map { it.getFont(context) ?: getTrueSystemFont() }

        val customFonts = Constants.FontFamily.getAllCustomFonts(context)
        val customFontOptions = customFonts.map { it.first }
        val customFontPaths = customFonts.map { it.second }
        val customFontTypefaces = customFontPaths.map { path ->
            Constants.FontFamily.Custom.getFont(context, path) ?: getTrueSystemFont()
        }

        val addCustomFontOption = "Add Custom Font..." // Ensure this is capitalized

        val options = listOf(addCustomFontOption) + builtInFontOptions + customFontOptions
        val fonts =
            listOf(getTrueSystemFont()) + builtInFonts + customFontTypefaces // Add placeholder font for "Add Custom Font..."

        // Determine currently selected index so the dialog shows the radio for current choice
        var selectedIndex: Int? = null
        try {
            // If the current context uses custom font, match by path
            val currentFont = when (contextKey) {
                "universal" -> prefs.universalFont
                else -> prefs.getFontForContext(contextKey)
            }
            if (currentFont == Constants.FontFamily.Custom) {
                val path = if (contextKey == "notifications") prefs.getCustomFontPath("notifications") ?: prefs.getCustomFontPath("universal") else prefs.getCustomFontPathForContext(contextKey)
                if (path != null) {
                    val idx = customFontPaths.indexOf(path)
                    if (idx != -1) selectedIndex = 1 + builtInFontOptions.size + idx
                }
            } else {
                val idx = fontFamilyEntries.indexOf(currentFont)
                if (idx != -1) selectedIndex = 1 + idx
            }
        } catch (_: Exception) {}

        dialogBuilder.showSingleChoiceDialog(
            context = context,
            options = options.toTypedArray(),
            fonts = fonts,
            titleResId = titleResId,
            selectedIndex = selectedIndex,
            showButtons = false,
            isCustomFont = { option ->
                customFontOptions.contains(option)
            },
            nonSelectable = { option -> option.toString() == addCustomFontOption },
            maxHeightRatio = 0.50f, // Make font picker taller (50% of screen)
            onItemSelected = { selectedName ->
                // Use string comparison to handle reordered options
                if (selectedName.toString() == addCustomFontOption) {
                    pickCustomFontFile { typeface, path ->
                        viewModel.setCustomFontPath(contextKey, path)
                        viewModel.addCustomFontPath(path)
                        onFontSelected(Constants.FontFamily.Custom, path)
                        activity?.recreate()
                    }
                } else {
                    val builtInIndex = builtInFontOptions.indexOf(selectedName)
                    if (builtInIndex != -1) {
                        onFontSelected(fontFamilyEntries[builtInIndex], null)
                        return@showSingleChoiceDialog
                    }
                    val customIndex = customFontOptions.indexOf(selectedName)
                    if (customIndex != -1) {
                        val path = customFontPaths[customIndex]
                        viewModel.setCustomFontPath(contextKey, path)
                        onFontSelected(Constants.FontFamily.Custom, path)
                        return@showSingleChoiceDialog
                    }
                }
            },
            onItemDeleted = { deletedName ->
                val customIndex = customFontOptions.indexOf(deletedName)
                if (customIndex != -1) {
                    val path = customFontPaths[customIndex]

                    // Remove the custom font from storage
                    viewModel.removeCustomFontPathByPath(path)

                    // Find all contexts using this font and reset them to System font
                    val allKeys = prefs.customFontPathMap.filterValues { it == path }.keys
                    for (key in allKeys) {
                        viewModel.setCustomFontPath(key, null)
                        // Reset the font setting for this context to System
                        when (key) {
                            "universal" -> viewModel.setUniversalFont(Constants.FontFamily.System)
                            "settings" -> viewModel.setFontFamily(Constants.FontFamily.System)
                            "apps" -> viewModel.setAppsFont(Constants.FontFamily.System)
                            "clock" -> viewModel.setClockFont(Constants.FontFamily.System)
                            "date" -> viewModel.setDateFont(Constants.FontFamily.System)
                            "quote" -> viewModel.setQuoteFont(Constants.FontFamily.System)
                            "notification" -> viewModel.setLabelNotificationsFont(Constants.FontFamily.System)
                            "notifications" -> viewModel.setNotificationsFont(Constants.FontFamily.System)
                            "lettersTitle" -> viewModel.setLettersTitleFont(Constants.FontFamily.System)
                        }
                    }

                }
            }
        )
    }

    @Suppress("DEPRECATION")
    private fun pickCustomFontFile(onFontPicked: (Typeface, String) -> Unit) {
        onCustomFontSelected = onFontPicked
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "font/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "font/ttf",
                    "font/otf",
                    "application/x-font-ttf",
                    "application/x-font-opentype",
                    "application/octet-stream"
                )
            )
        }
        startActivityForResult(intent, PICK_FONT_FILE_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FONT_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val fontFile = copyFontToInternalStorage(uri)
                if (fontFile != null) {
                    try {
                        val typeface = Typeface.createFromFile(fontFile)
                        // Try to access a property to force load
                        typeface.style
                        onCustomFontSelected?.invoke(typeface, fontFile.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Show error dialog to user
                        dialogBuilder.showErrorDialog(
                            requireContext(),
                            title = "Invalid Font File",
                            message = "The selected file could not be loaded as a font. Please choose a valid font file."
                        )
                    }
                } else {
                    dialogBuilder.showErrorDialog(
                        requireContext(),
                        title = "File Error",
                        message = "Could not copy the selected file. Please try again."
                    )
                }
            }
        }
    }

    private fun copyFontToInternalStorage(uri: Uri): File? {
        val fileName = getFileName(uri) ?: "custom_font.ttf"
        val file = File(requireContext().filesDir, fileName)
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = it.getString(index)
            }
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // No binding to clean up in Compose
    }

    override fun onStop() {
        super.onStop()
        dialogBuilder.singleChoiceDialog?.dismiss()
    }
}