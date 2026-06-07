package com.github.gezimos.inkos.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.github.gezimos.inkos.ui.compose.gestureHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.isBiometricEnabled
import com.github.gezimos.inkos.BuildConfig
import com.github.gezimos.inkos.EinkHelper
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.CalendarEventsHelper
import com.github.gezimos.inkos.helper.IconPackUtility
import com.github.gezimos.inkos.helper.IconUtility
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.getTrueSystemFont
import com.github.gezimos.inkos.helper.hideNavigationBar
import com.github.gezimos.inkos.helper.hideStatusBar
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.showNavigationBar
import com.github.gezimos.inkos.helper.showStatusBar
import com.github.gezimos.inkos.helper.utils.AppReloader
import com.github.gezimos.inkos.helper.utils.ProfileManager
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.listener.DeviceAdmin
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.compose.SettingsComposable
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsHomeItem
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelectWithDualColorPreview
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSwitch
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.ui.dialogs.SheetTitle
import com.github.gezimos.inkos.ui.compose.getChosenThemeDisplayName
import com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost
import com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager
import com.github.gezimos.inkos.ui.dialogs.SearchFoldersSheet
import com.github.gezimos.inkos.services.ActionService
import com.github.gezimos.common.openAccessibilitySettings
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.Collator
import androidx.core.net.toUri

// ---------------------------------------------------------------------------
// Route
// ---------------------------------------------------------------------------

sealed interface SettingsRoute {
    data object Home : SettingsRoute
    data object Features : SettingsRoute
    data object Drawer : SettingsRoute
    data object LookFeel : SettingsRoute
    data object Gestures : SettingsRoute
    data object Fonts : SettingsRoute
    data object Notifications : SettingsRoute
    data object Advanced : SettingsRoute
    data object Support : SettingsRoute
    data object Extras : SettingsRoute

    fun title(context: Context): String = when (this) {
        Home -> context.getString(R.string.settings_name)
        Features -> context.getString(R.string.settings_home_title)
        Drawer -> context.getString(R.string.app_drawer)
        LookFeel -> context.getString(R.string.look_feel_settings_title)
        Gestures -> context.getString(R.string.gestures_settings_title)
        Fonts -> context.getString(R.string.fonts_settings_title)
        Notifications -> context.getString(R.string.notification_section)
        Advanced -> context.getString(R.string.advanced_settings_title)
        Support -> "Support"
        Extras -> "Extras"
    }
}

// ---------------------------------------------------------------------------
// Fragment
// ---------------------------------------------------------------------------

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName
    private lateinit var dialogBuilder: ComposeDialogManager

    private var currentRoute by mutableStateOf<SettingsRoute>(SettingsRoute.Home)
    private val backStack = mutableListOf<SettingsRoute>()

    // Page indicator state
    private var currentPage by mutableStateOf(0)
    private var pageCount by mutableStateOf(1)

    // Scroll infrastructure
    private lateinit var contentComposeView: ComposeView

    private var onCustomFontSelected: ((Typeface, String) -> Unit)? = null
    private val fontPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fontFile = copyFontToInternalStorage(uri)
            if (fontFile != null) {
                try {
                    val typeface = Typeface.createFromFile(fontFile)
                    typeface.style
                    onCustomFontSelected?.invoke(typeface, fontFile.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
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

    // Calendar permission
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshEvents(requireContext())
        }
    }

    // Contacts permission
    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setAppDrawerSearchContactsEnabled(true)
        }
    }

    // Audio/music permission
    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setAppDrawerSearchMusicEnabled(true)
        }
    }

    // File search folder picker
    private val settingsFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    // ------------------------------------------------------------------
    // Navigation helpers
    // ------------------------------------------------------------------

    private fun navigateTo(route: SettingsRoute) {
        backStack.add(currentRoute)
        currentRoute = route
        resetScroll()
    }

    private fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            currentRoute = backStack.removeAt(backStack.lastIndex)
            resetScroll()
            return true
        }
        return false
    }

    // Signal to Compose to reset scroll position
    private var scrollResetTrigger by mutableStateOf(0)

    private fun resetScroll() {
        currentPage = 0
        pageCount = 1
        scrollResetTrigger++
    }

    // ------------------------------------------------------------------
    // onCreateView
    // ------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        viewModel.isinkosDefault()
        arguments?.getString("initialRoute")?.let { routeName ->
            val route = when (routeName) {
                "Notifications" -> SettingsRoute.Notifications
                "LookFeel" -> SettingsRoute.LookFeel
                "Drawer" -> SettingsRoute.Drawer
                "Gestures" -> SettingsRoute.Gestures
                "Fonts" -> SettingsRoute.Fonts
                "Advanced" -> SettingsRoute.Advanced
                "Support" -> SettingsRoute.Support
                else -> null
            }
            if (route != null) {
                backStack.add(SettingsRoute.Home)
                currentRoute = route
            }
        }
        val context = requireContext()
        deviceManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(context, DeviceAdmin::class.java)
        checkAdminPermission()
        dialogBuilder = ComposeDialogManager(context, requireActivity())

        val backgroundColor = getHexForOpacity(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        // Back handling
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!navigateBack()) {
                        try { findNavController().popBackStack() } catch (_: Exception) {}
                    }
                }
            }
        )

        // --- Sticky header ---
        val headerView = ComposeView(context)
        fun updateHeader() {
            headerView.setContent {
                val uiState by viewModel.homeUiState.collectAsState()
                val isDark = uiState.appTheme == Dark
                val settingsSize = (uiState.settingsSize - 3)
                val route = currentRoute
                val navController = findNavController()

                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        if (route is SettingsRoute.Home) {
                            val textIslandsShape = uiState.textIslandsShape
                            val supportButtonShape = remember(textIslandsShape) {
                                ShapeHelper.getRoundedCornerShape(
                                    textIslandsShape = textIslandsShape,
                                    pillRadius = 50.dp
                                )
                            }
                            PageHeader(
                                iconRes = R.drawable.ic_inkos,
                                title = stringResource(R.string.settings_name),
                                onClick = {
                                    val popped = try {
                                        navController.popBackStack(R.id.mainFragment, false)
                                    } catch (_: Exception) { false }
                                    if (!popped) {
                                        try {
                                            val navOptions = androidx.navigation.NavOptions.Builder()
                                                .setPopUpTo(navController.graph.startDestinationId, true)
                                                .build()
                                            navController.navigate(R.id.mainFragment, null, navOptions)
                                        } catch (_: Exception) {
                                            navController.navigate(R.id.mainFragment)
                                        }
                                    }
                                },
                                showStatusBar = uiState.showStatusBar,
                                pageIndicator = {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isFocused = interactionSource.collectIsFocusedAsState().value
                                    val textColor = Theme.colors.text
                                    val bgColor = Theme.colors.background
                                    Text(
                                        text = stringResource(R.string.buy_me_a_coffee),
                                        style = SettingsTheme.typography.title,
                                        fontSize = if (settingsSize > 0) (settingsSize * 1.2).sp else TextUnit.Unspecified,
                                        color = if (isFocused) bgColor else textColor,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clip(supportButtonShape)
                                            .background(if (isFocused) textColor else Color.Transparent)
                                            .border(
                                                width = 2.dp,
                                                color = textColor,
                                                shape = supportButtonShape
                                            )
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null
                                            ) {
                                                try { context.startActivity(Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/gezimos".toUri())) } catch (_: Exception) {}
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                },
                                titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                            )
                        } else {
                            PageHeader(
                                iconRes = R.drawable.ic_back,
                                title = route.title(requireContext()),
                                onClick = { navigateBack() },
                                showStatusBar = uiState.showStatusBar,
                                pageIndicator = {
                                    PageIndicator(currentPage = currentPage, pageCount = pageCount)
                                },
                                titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                            )
                        }
                    }
                }
            }
        }
        updateHeader()
        root.addView(headerView)

        // --- Scrollable content (Compose-level paged scrolling via gestureHelper) ---
        contentComposeView = ComposeView(context).apply {
            setContent {
                val uiState by viewModel.homeUiState.collectAsState()
                val isDark = if (currentRoute is SettingsRoute.LookFeel)
                    prefs.getResolvedTheme() == Dark
                else uiState.appTheme == Dark
                val settingsSize = (uiState.settingsSize - 3)

                SettingsTheme(isDark) {
                    val scrollState = rememberScrollState()
                    val scope = rememberCoroutineScope()
                    val overlapFraction = 0.2f

                    // Reset scroll when route changes
                    val resetTrigger = scrollResetTrigger
                    LaunchedEffect(resetTrigger) {
                        scrollState.scrollTo(0)
                    }

                    // Page calculation from scroll state
                    val viewportHeightPx = remember { mutableIntStateOf(0) }
                    LaunchedEffect(scrollState.value, scrollState.maxValue, viewportHeightPx.intValue) {
                        val vp = viewportHeightPx.intValue
                        if (vp <= 0) return@LaunchedEffect
                        val overlap = (vp * overlapFraction).toInt()
                        val step = (vp - overlap).coerceAtLeast(1)
                        val maxScroll = scrollState.maxValue.coerceAtLeast(0)
                        val pages = if (maxScroll <= 0) 1 else (1 + ((maxScroll + step - 1) / step))
                        var bestPage = 0
                        var bestDist = kotlin.math.abs(scrollState.value)
                        for (i in 1 until pages) {
                            val pageStart = (i * step).coerceAtMost(maxScroll)
                            val dist = kotlin.math.abs(scrollState.value - pageStart)
                            if (dist < bestDist) { bestPage = i; bestDist = dist }
                        }
                        if (bestPage != currentPage || pages != pageCount) {
                            currentPage = bestPage
                            pageCount = pages
                            updateHeader()
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewportHeightPx.intValue = it.height }
                            .gestureHelper(
                                onVerticalPageMove = { delta ->
                                    val vp = viewportHeightPx.intValue
                                    if (vp <= 0) return@gestureHelper
                                    val overlap = (vp * overlapFraction).toInt()
                                    val step = (vp - overlap).coerceAtLeast(1)
                                    val maxScroll = scrollState.maxValue.coerceAtLeast(0)
                                    val pages = if (maxScroll <= 0) 1 else (1 + ((maxScroll + step - 1) / step))
                                    val targetPage = (currentPage + delta).coerceIn(0, pages - 1)
                                    if (targetPage != currentPage) {
                                        val targetScroll = (targetPage * step).coerceAtMost(maxScroll)
                                        scope.launch { scrollState.scrollTo(targetScroll) }
                                    }
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState, enabled = false)
                        ) {
                            SettingsRouteContent(settingsSize.sp)
                        }
                    }
                }
            }
        }

        root.addView(
            contentComposeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        root.post {
            root.clipToPadding = false
        }

        return root
    }

    // ------------------------------------------------------------------
    // Content router
    // ------------------------------------------------------------------

    @Composable
    private fun SettingsRouteContent(fontSize: TextUnit) {
        when (currentRoute) {
            SettingsRoute.Home -> SettingsHomeContent(fontSize)
            SettingsRoute.Features -> FeaturesContent(fontSize)
            SettingsRoute.Drawer -> DrawerContent(fontSize)
            SettingsRoute.LookFeel -> LookFeelContent(fontSize)
            SettingsRoute.Gestures -> GesturesContent(fontSize)
            SettingsRoute.Fonts -> FontsContent(fontSize)
            SettingsRoute.Notifications -> NotificationsContent(fontSize)
            SettingsRoute.Advanced -> AdvancedContent(fontSize)
            SettingsRoute.Support -> SupportContent(fontSize)
            SettingsRoute.Extras -> ExtrasContent(fontSize)
        }
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            viewModel.setLockModeOn(isAdmin)
    }

    // ==================================================================
    // HOME
    // ==================================================================

    @Composable
    private fun SettingsHomeContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val privateSpaceManager = remember { ProfileManager(requireContext()) }
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsHomeItem(
                title = stringResource(R.string.settings_home_title),
                imageVector = Icons.Rounded.Widgets,
                description = stringResource(R.string.desc_home_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Features) },
            )
            SettingsHomeItem(
                title = stringResource(R.string.app_drawer),
                imageVector = Icons.Rounded.FilterList,
                description = stringResource(R.string.desc_drawer_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Drawer) },
            )
            SettingsHomeItem(
                title = stringResource(R.string.fonts_settings_title),
                imageVector = Icons.Rounded.FormatSize,
                description = stringResource(R.string.desc_fonts_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Fonts) },
            )
            SettingsHomeItem(
                title = stringResource(R.string.settings_look_feel_title),
                imageVector = Icons.Filled.InvertColors,
                description = stringResource(R.string.desc_look_feel_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.LookFeel) },
            )
            SettingsHomeItem(
                title = stringResource(R.string.settings_gestures_title),
                imageVector = Icons.Rounded.SwipeVertical,
                description = stringResource(R.string.desc_gestures_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Gestures) },
            )
            SettingsHomeItem(
                title = stringResource(R.string.notification_section),
                imageVector = Icons.Rounded.Notifications,
                description = stringResource(R.string.desc_notifications_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Notifications) },
            )
            if (privateSpaceManager.isPrivateSpaceSupported() &&
                privateSpaceManager.isPrivateSpaceSetUp(showToast = false, launchSettings = false)
            ) {
                SettingsHomeItem(
                    title = stringResource(R.string.private_space),
                    imageVector = Icons.Rounded.Lock,
                    description = stringResource(R.string.desc_private_space),
                    titleFontSize = titleFontSize,
                    onClick = {
                        privateSpaceManager.togglePrivateSpaceLock(
                            showToast = true,
                            launchSettings = true
                        )
                    }
                )
            }
            if (privateSpaceManager.hasWorkProfile()) {
                SettingsHomeItem(
                    title = stringResource(R.string.work_profile) +
                            if (privateSpaceManager.isWorkProfilePaused()) " (paused)" else "",
                    imageVector = Icons.Rounded.Star,
                    description = stringResource(R.string.desc_work_profile),
                    titleFontSize = titleFontSize,
                    onClick = {
                        privateSpaceManager.toggleWorkProfile(showToast = true)
                    }
                )
            }
            SettingsHomeItem(
                title = stringResource(R.string.settings_advanced_title) +
                        if (!isinkosDefault(requireContext())) "*" else "",
                imageVector = Icons.Rounded.Settings,
                description = stringResource(R.string.desc_advanced_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Advanced) },
            )
            SettingsHomeItem(
                title = stringResource(R.string.extra_features),
                imageVector = Icons.Rounded.OfflineBolt,
                description = stringResource(R.string.desc_extras_settings),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Extras) },
            )
            SettingsHomeItem(
                title = stringResource(R.string.about),
                imageVector = Icons.Rounded.Info,
                description = stringResource(R.string.desc_about),
                titleFontSize = titleFontSize,
                onClick = { navigateTo(SettingsRoute.Support) },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ==================================================================
    // SUPPORT
    // ==================================================================

    @Composable
    private fun SupportContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val context = androidx.compose.ui.platform.LocalContext.current

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTitle(text = stringResource(R.string.donate), fontSize = titleFontSize)
            SettingsHomeItem(
                title = stringResource(R.string.buy_me_a_coffee),
                imageVector = Icons.Rounded.Coffee,
                description = stringResource(R.string.desc_buy_coffee),
                titleFontSize = titleFontSize,
                onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/gezimos".toUri())) } catch (_: Exception) {}
                }
            )
            SettingsHomeItem(
                title = stringResource(R.string.github_sponsors),
                imageVector = Icons.Rounded.Favorite,
                description = stringResource(R.string.desc_github_sponsor),
                titleFontSize = titleFontSize,
                onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/gezimos".toUri())) } catch (_: Exception) {}
                }
            )
            SettingsTitle(text = stringResource(R.string.community), fontSize = titleFontSize)
            SettingsHomeItem(
                title = stringResource(R.string.report_issues),
                imageVector = Icons.Rounded.BugReport,
                description = stringResource(R.string.desc_report_bugs),
                titleFontSize = titleFontSize,
                onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/gezimos/inkOS/issues".toUri())) } catch (_: Exception) {}
                }
            )
            SettingsHomeItem(
                title = stringResource(R.string.reddit_inkos),
                imageVector = Icons.Rounded.Forum,
                description = stringResource(R.string.desc_reddit_community),
                titleFontSize = titleFontSize,
                onClick = {
                    try {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, "reddit://r/inkos".toUri())) }
                        catch (_: Exception) { context.startActivity(Intent(Intent.ACTION_VIEW, "https://reddit.com/r/inkos".toUri())) }
                    } catch (_: Exception) {}
                }
            )
            SettingsTitle(text = stringResource(R.string.about), fontSize = titleFontSize)
            SettingsHomeItem(
                title = "${stringResource(R.string.app_version)} v${BuildConfig.VERSION_NAME}",
                imageVector = Icons.Rounded.Info,
                description = stringResource(R.string.desc_app_version),
                titleFontSize = titleFontSize,
                onClick = { openAppInfo(context, android.os.Process.myUserHandle(), BuildConfig.APPLICATION_ID) }
            )
            SettingsHomeItem(
                title = stringResource(R.string.font_license_ofl),
                imageVector = Icons.Rounded.TextFields,
                description = stringResource(R.string.desc_font_license),
                titleFontSize = titleFontSize,
                onClick = { dialogBuilder.showFontLicenseSheet() }
            )
            SettingsHomeItem(
                title = stringResource(R.string.credits),
                imageVector = Icons.Rounded.Code,
                description = "mLauncher, oLauncher — ${stringResource(R.string.desc_open_source)}",
                titleFontSize = titleFontSize,
                onClick = {}
            )
        }
    }

    // ==================================================================
    // EXTRAS
    // ==================================================================

    @Composable
    private fun ExtrasContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val uiState by viewModel.homeUiState.collectAsState()
        val isMuditaDevice = remember { EinkHelper.isMuditaKompakt() }
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMuditaDevice) {
                SettingsTitle(text = stringResource(R.string.eink_auto_mode), fontSize = titleFontSize)
                SettingsSelect(
                    title = stringResource(R.string.eink_auto_mode),
                    option = EinkHelper.modeName(uiState.einkHelperMode),
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_eink_mode),
                    onClick = {
                        val newMode = EinkHelper.nextMode(uiState.einkHelperMode)
                        viewModel.setEinkHelperMode(newMode)
                        (requireActivity() as? com.github.gezimos.inkos.MainActivity)?.setMeinkMode(newMode)
                    }
                )
            }
            SettingsTitle(text = stringResource(R.string.extra_features), fontSize = titleFontSize)
            SettingsSwitch(
                text = stringResource(R.string.auto_eink_refresh), fontSize = titleFontSize,
                description = stringResource(R.string.desc_eink_auto_refresh),
                defaultState = uiState.einkRefreshEnabled,
                onCheckedChange = { viewModel.setEinkRefreshEnabled(it) }
            )
            if (uiState.einkRefreshEnabled) {
                SettingsSwitch(
                    text = stringResource(R.string.auto_refresh_home_only), fontSize = titleFontSize,
                    description = stringResource(R.string.desc_eink_home_only),
                    defaultState = uiState.einkRefreshHomeButtonOnly,
                    onCheckedChange = { viewModel.setEinkRefreshHomeButtonOnly(it) }
                )
            }
            SettingsSelect(
                title = stringResource(R.string.eink_refresh_delay), option = "${uiState.einkRefreshDelay} ms", fontSize = titleFontSize,
                description = stringResource(R.string.desc_eink_interval),
                onClick = {
                    dialogBuilder.showSliderDialog(
                        context = requireContext(), title = getString(R.string.eink_refresh_delay),
                        minValue = Constants.MIN_EINK_REFRESH_DELAY, maxValue = Constants.MAX_EINK_REFRESH_DELAY,
                        currentValue = uiState.einkRefreshDelay.toInt(),
                        onValueSelected = { newDelay -> viewModel.setEinkRefreshDelay(((newDelay + 12) / 25) * 25) }
                    )
                }
            )
            SettingsSwitch(
                text = getString(R.string.use_volume_keys_for_pages), fontSize = titleFontSize,
                description = stringResource(R.string.desc_volume_keys),
                defaultState = uiState.useVolumeKeysForPages,
                onCheckedChange = { viewModel.setUseVolumeKeysForPages(it) }
            )
        }
    }

    // ==================================================================
    // ADVANCED
    // ==================================================================

    @Composable
    private fun AdvancedContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val changeLauncherText = if (!isinkosDefault(requireContext())) {
            R.string.advanced_settings_set_as_default_launcher
        } else {
            R.string.advanced_settings_change_default_launcher
        }
        val uiState by viewModel.homeUiState.collectAsState()
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSwitch(
                text = stringResource(R.string.lock_home_apps), fontSize = titleFontSize,
                description = stringResource(R.string.desc_lock_home_apps),
                defaultState = uiState.homeLocked,
                onCheckedChange = { checked ->
                    viewModel.setHomeLocked(checked)
                    if (!checked) viewModel.setLongPressAppInfoEnabled(false)
                }
            )
            SettingsSwitch(
                text = stringResource(R.string.longpress_app_info), fontSize = titleFontSize,
                description = stringResource(R.string.desc_longpress_app_info),
                defaultState = uiState.longPressAppInfoEnabled, enabled = uiState.homeLocked,
                onCheckedChange = { viewModel.setLongPressAppInfoEnabled(it) }
            )
            if (requireContext().isBiometricEnabled()) {
                SettingsSwitch(
                    text = stringResource(R.string.lock_settings), fontSize = titleFontSize,
                    description = stringResource(R.string.desc_lock_settings),
                    defaultState = uiState.settingsLocked,
                    onCheckedChange = { viewModel.setSettingsLocked(it) }
                )
            }
            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_backup_restore_title),
                description = stringResource(R.string.desc_backup_restore),
                titleFontSize = titleFontSize,
                onClick = { dialogBuilder.showBackupRestoreDialog() }
            )
            SettingsHomeItem(
                title = stringResource(R.string.theme_export_import),
                description = stringResource(R.string.desc_theme_export_import),
                titleFontSize = titleFontSize,
                onClick = { dialogBuilder.showThemeDialog() }
            )
            SettingsHomeItem(
                title = stringResource(changeLauncherText) + if (!isinkosDefault(requireContext())) "*" else "",
                description = "Choose which launcher to use as your home screen",
                titleFontSize = titleFontSize,
                onClick = { requireContext().startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS)) }
            )
            SettingsHomeItem(
                title = stringResource(R.string.onboarding),
                description = "View the setup wizard again",
                titleFontSize = titleFontSize,
                onClick = {
                    prefs.commitFirstOpen(true)
                    prefs.commitGuideShown(false)
                    prefs.commitThemePickerShown(false)
                    AppReloader.restartApp(requireContext())
                }
            )
            SettingsHomeItem(
                title = stringResource(R.string.guide),
                description = "Learn gestures and interactions",
                titleFontSize = titleFontSize,
                onClick = { findNavController().navigate(R.id.guideFragment) }
            )
            SettingsHomeItem(
                title = stringResource(R.string.advanced_settings_restart_title),
                description = "Restart inkOS without rebooting the device",
                titleFontSize = titleFontSize,
                onClick = { AppReloader.restartApp(requireContext()) }
            )
            SettingsHomeItem(
                title = stringResource(R.string.settings_exit_inkos_title),
                description = "Switch to a different launcher",
                titleFontSize = titleFontSize,
                onClick = { exitLauncher() }
            )
        }
    }

    private fun exitLauncher() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(intent, "Choose your launcher"))
    }

    // ==================================================================
    // FEATURES
    // ==================================================================

    @Composable
    private fun FeaturesContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val homeUiState by viewModel.homeUiState.collectAsState()
        when {
            homeUiState.allCapsApps -> 2
            homeUiState.smallCapsApps -> 1
            else -> 0
        }
        val alignmentLabels = listOf(
            stringResource(R.string.left), stringResource(R.string.center), stringResource(R.string.right)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsTitle(text = stringResource(R.string.layout_positioning), fontSize = titleFontSize)
            SettingsSelect(
                title = stringResource(R.string.app_padding_size), option = homeUiState.textPaddingSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_app_padding),
                onClick = {
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.app_padding_size),
                        Constants.MIN_TEXT_PADDING, Constants.MAX_TEXT_PADDING, homeUiState.textPaddingSize) { viewModel.setTextPaddingSize(it) }
                }
            )
            SettingsSelect(
                title = stringResource(R.string.home_apps_y_offset), option = homeUiState.homeAppsYOffset.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_home_y_offset),
                onClick = {
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.home_apps_y_offset),
                        Constants.MIN_HOME_APPS_Y_OFFSET, homeUiState.maxHomeAppsYOffset, homeUiState.homeAppsYOffset) { viewModel.setHomeAppsYOffset(it) }
                }
            )
            SettingsSelect(
                title = stringResource(R.string.top_widget_margin), option = homeUiState.topWidgetMargin.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_top_widget_margin),
                onClick = {
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.top_widget_margin),
                        0, Constants.MAX_TOP_WIDGET_MARGIN, homeUiState.topWidgetMargin) { viewModel.setTopWidgetMargin(it) }
                }
            )
            SettingsSelect(
                title = stringResource(R.string.bottom_widget_margin), option = homeUiState.bottomWidgetMargin.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_bottom_widget_margin),
                onClick = {
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.bottom_widget_margin),
                        0, Constants.MAX_BOTTOM_WIDGET_MARGIN, homeUiState.bottomWidgetMargin) { viewModel.setBottomWidgetMargin(it) }
                }
            )
            SettingsTitle(text = stringResource(R.string.home_apps), fontSize = titleFontSize)
            SettingsSelect(
                title = stringResource(R.string.apps_on_home_screen), option = homeUiState.homeAppsNum.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_home_apps_num),
                onClick = {
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.apps_on_home_screen),
                        Constants.MIN_HOME_APPS, Constants.MAX_HOME_APPS, homeUiState.homeAppsNum) { newHomeAppsNum ->
                        viewModel.setHomeAppsNum(newHomeAppsNum)
                        Constants.updateMaxHomePages(requireContext())
                        if (homeUiState.homePagesNum > Constants.MAX_HOME_PAGES) viewModel.setHomePagesNum(Constants.MAX_HOME_PAGES)
                    }
                }
            )
            SettingsSelect(
                title = stringResource(R.string.pages_on_home_screen), option = homeUiState.homePagesNum.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_home_pages_num),
                onClick = {
                    Constants.updateMaxHomePages(requireContext())
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.pages_on_home_screen),
                        Constants.MIN_HOME_PAGES, Constants.MAX_HOME_PAGES, homeUiState.homePagesNum) { viewModel.setHomePagesNum(it) }
                }
            )
            SettingsSwitch(text = stringResource(R.string.enable_home_pager), fontSize = titleFontSize,
                description = stringResource(R.string.desc_page_indicator),
                defaultState = homeUiState.pageIndicatorVisible, onCheckedChange = { viewModel.setPageIndicatorVisible(it) })
            SettingsSwitch(text = stringResource(R.string.home_page_reset), fontSize = titleFontSize,
                description = stringResource(R.string.desc_page_reset),
                defaultState = homeUiState.homeReset, onCheckedChange = { viewModel.setHomeReset(it) })
            SettingsSelect(
                title = stringResource(R.string.home_apps_alignment),
                option = alignmentLabels.getOrElse(homeUiState.homeAlignment) { stringResource(R.string.left) },
                optionAlignment = homeUiState.homeAlignment,
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_home_alignment),
                onClick = { viewModel.setHomeAlignment((homeUiState.homeAlignment + 1) % 3) }
            )
            // Clock Section
            SettingsTitle(text = stringResource(R.string.clock_section), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.show_clock), fontSize = titleFontSize,
                description = stringResource(R.string.desc_show_clock),
                defaultState = homeUiState.showClock, onCheckedChange = { viewModel.setShowClock(it) })
            val clockModeLabels = listOf(
                stringResource(R.string.option_system), stringResource(R.string.option_24_hour),
                stringResource(R.string.option_12_hour), stringResource(R.string.option_12_hour_alt)
            )
            SettingsSelect(title = stringResource(R.string.clock_format),
                option = clockModeLabels.getOrElse(homeUiState.clockMode) { stringResource(R.string.option_system) },
                description = stringResource(R.string.desc_clock_format),
                fontSize = titleFontSize, onClick = { viewModel.setClockMode((homeUiState.clockMode + 1) % 4) })
            val clockStyleOrder = intArrayOf(0, 2, 6, 1, 3, 8, 9, 10, 4, 5, 7)
            val clockStyleNameByValue = mapOf(
                0 to stringResource(R.string.option_default),
                1 to stringResource(R.string.option_flip),
                2 to stringResource(R.string.option_box_solid),
                3 to stringResource(R.string.option_round),
                4 to stringResource(R.string.option_split),
                5 to stringResource(R.string.option_horizontal),
                6 to stringResource(R.string.option_box_outline),
                8 to stringResource(R.string.option_analog),
                9 to stringResource(R.string.option_digital),
                10 to stringResource(R.string.option_matrix),
                7 to stringResource(R.string.option_stacked)
            )
            val currentStyleDisplayIndex = clockStyleOrder.indexOf(homeUiState.clockStyle).coerceAtLeast(0)
            SettingsSelect(title = stringResource(R.string.clock_style),
                option = clockStyleNameByValue[homeUiState.clockStyle] ?: stringResource(R.string.option_default),
                description = stringResource(R.string.desc_clock_style),
                fontSize = titleFontSize, onClick = {
                    val nextDisplayIndex = (currentStyleDisplayIndex + 1) % clockStyleOrder.size
                    viewModel.setClockStyle(clockStyleOrder[nextDisplayIndex])
                })
            SettingsSwitch(text = stringResource(R.string.show_am_pm), fontSize = titleFontSize,
                description = stringResource(R.string.desc_show_am_pm),
                defaultState = homeUiState.showAmPm, onCheckedChange = { viewModel.setShowAmPm(it) })
            if (homeUiState.clockStyle in intArrayOf(0, 2, 6)) {
            SettingsSwitch(text = stringResource(R.string.dual_clocks), fontSize = titleFontSize,
                description = stringResource(R.string.desc_dual_clocks),
                defaultState = homeUiState.showSecondClock, onCheckedChange = { viewModel.setShowSecondClock(it) })
            }
            if (homeUiState.showSecondClock && homeUiState.clockStyle in intArrayOf(0, 2, 6)) {
                SettingsSelect(
                    title = stringResource(R.string.second_clock_offset),
                    option = if (homeUiState.secondClockOffsetHours >= 0) "+${homeUiState.secondClockOffsetHours}" else homeUiState.secondClockOffsetHours.toString(),
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_second_clock_offset),
                    onClick = {
                        dialogBuilder.showSliderDialog(requireContext(), getString(R.string.second_clock_offset),
                            -12, 14, homeUiState.secondClockOffsetHours) { viewModel.setSecondClockOffsetHours(it) }
                    }
                )
            }
            SettingsSelect(title = stringResource(R.string.home_clock_alignment),
                option = alignmentLabels.getOrElse(homeUiState.clockAlignment) { stringResource(R.string.left) },
                optionAlignment = homeUiState.clockAlignment,
                description = stringResource(R.string.desc_clock_alignment),
                fontSize = titleFontSize, onClick = { viewModel.setHomeClockAlignment((homeUiState.clockAlignment + 1) % 3) })
            // Date Row Section
            SettingsTitle(text = stringResource(R.string.date_row_section), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.show_date), fontSize = titleFontSize,
                description = stringResource(R.string.desc_show_date),
                defaultState = homeUiState.showDate, onCheckedChange = { viewModel.setShowDate(it) })
            val dateFormatLabels = listOf(stringResource(R.string.option_date_format_1), stringResource(R.string.option_date_format_2), stringResource(R.string.option_date_format_3), stringResource(R.string.option_date_format_4), stringResource(R.string.option_date_format_5))
            SettingsSelect(title = stringResource(R.string.date_format),
                option = dateFormatLabels.getOrElse(homeUiState.dateFormatStyle) { dateFormatLabels[0] },
                description = stringResource(R.string.desc_date_format),
                fontSize = titleFontSize, onClick = { viewModel.setDateFormatStyle((homeUiState.dateFormatStyle + 1) % 5) })
            SettingsSwitch(text = stringResource(R.string.show_battery), fontSize = titleFontSize,
                description = stringResource(R.string.desc_show_battery),
                defaultState = homeUiState.showDateBatteryCombo, onCheckedChange = { viewModel.setShowDateBatteryCombo(it) })
            SettingsSwitch(text = stringResource(R.string.show_notification_count), fontSize = titleFontSize,
                description = stringResource(R.string.desc_notification_count),
                defaultState = homeUiState.showNotificationCount, onCheckedChange = { viewModel.setShowNotificationCount(it) })
            if (homeUiState.showNotificationCount) {
                val countSourceLabels = arrayOf(stringResource(R.string.option_simple_tray), stringResource(R.string.option_letters), stringResource(R.string.option_hub))
                SettingsSelect(title = stringResource(R.string.desc_notification_style),
                    option = countSourceLabels.getOrElse(homeUiState.notificationCountSource) { "SimpleTray" },
                    description = stringResource(R.string.desc_notification_style),
                    fontSize = titleFontSize, onClick = { viewModel.setNotificationCountSource((homeUiState.notificationCountSource + 1) % 3) })
            }
            SettingsSelect(title = stringResource(R.string.home_date_alignment),
                option = alignmentLabels.getOrElse(homeUiState.dateAlignment) { stringResource(R.string.left) },
                optionAlignment = homeUiState.dateAlignment,
                description = stringResource(R.string.desc_date_alignment),
                fontSize = titleFontSize, onClick = { viewModel.setHomeDateAlignment((homeUiState.dateAlignment + 1) % 3) })
            // Bottom Widgets Section
            SettingsTitle(text = stringResource(R.string.bottom_widgets), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.show_audio_widget), fontSize = titleFontSize,
                description = stringResource(R.string.desc_audio_widget),
                defaultState = homeUiState.showAudioWidget, onCheckedChange = { viewModel.setShowAudioWidget(it) })
            val bottomWidgetLabels = listOf(
                stringResource(R.string.bottom_widget_disabled), stringResource(R.string.bottom_widget_quote),
                stringResource(R.string.bottom_widget_events), stringResource(R.string.bottom_widget_android),
                stringResource(R.string.bottom_widget_shortcuts), stringResource(R.string.bottom_widget_total_usage),
                stringResource(R.string.bottom_widget_page_dots)
            )
            val bottomWidgetValues = listOf(
                Constants.BottomWidgetType.Disabled.value, Constants.BottomWidgetType.Quote.value,
                Constants.BottomWidgetType.Events.value, Constants.BottomWidgetType.AndroidWidget.value,
                Constants.BottomWidgetType.Shortcuts.value, Constants.BottomWidgetType.TotalUsage.value,
                Constants.BottomWidgetType.PageDots.value
            )
            val currentBottomWidgetIndex = bottomWidgetValues.indexOf(homeUiState.bottomWidgetType).coerceAtLeast(0)
            SettingsSelect(
                title = stringResource(R.string.bottom_widget),
                option = bottomWidgetLabels.getOrElse(currentBottomWidgetIndex) { stringResource(R.string.bottom_widget_quote) },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_bottom_widget),
                onClick = {
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(), options = bottomWidgetLabels.toTypedArray(),
                        titleResId = R.string.bottom_widget, selectedIndex = currentBottomWidgetIndex,
                        onItemSelected = { selectedLabel ->
                            val idx = bottomWidgetLabels.indexOf(selectedLabel)
                            if (idx >= 0) viewModel.setBottomWidgetType(bottomWidgetValues[idx])
                        }
                    )
                }
            )
            if (homeUiState.bottomWidgetType == Constants.BottomWidgetType.Shortcuts.value) {

                val shortcutActions = remember {
                    val psm = ProfileManager(requireContext())
                    Action.entries.filter {
                        (psm.isPrivateSpaceSetUp() || it != Action.TogglePrivateSpace) &&
                        (psm.hasWorkProfile() || it != Action.ToggleWorkProfile) &&
                        when (it) { Action.OpenLettersScreen -> prefs.notificationsEnabled; else -> true }
                    }.toMutableList().apply {
                        if (!contains(Action.Brightness)) add(Action.Brightness)
                        if (!contains(Action.OpenAppDrawer)) add(Action.OpenAppDrawer)
                    }
                }
                val iconLabels = Constants.ShortcutIcon.entries.map { it.name }
                SettingsSelect(title = stringResource(R.string.shortcut_left_icon), option = prefs.shortcutLeftIcon.name, fontSize = titleFontSize, onClick = {
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(), options = iconLabels.toTypedArray(),
                        titleResId = R.string.bottom_widget,
                        selectedIndex = Constants.ShortcutIcon.entries.indexOf(prefs.shortcutLeftIcon),
                        onItemSelected = { selected ->
                            viewModel.setShortcutLeftIcon(Constants.ShortcutIcon.valueOf(selected))
                        }
                    )
                })
                val appLabelLeft = prefs.appShortcutLeft.activityLabel
                val leftDisplay = when (homeUiState.shortcutLeftAction) {
                    Action.OpenApp -> appLabelLeft.ifEmpty { getString(R.string.open_app) }
                    else -> homeUiState.shortcutLeftAction.getString(requireContext())
                }
                SettingsSelect(title = stringResource(R.string.shortcut_left_action), option = leftDisplay, fontSize = titleFontSize, onClick = {
                    dialogBuilder.showGestureActionPicker(
                        requireContext(), R.string.bottom_widget, AppDrawerFlag.SetShortcutLeft,
                        shortcutActions, homeUiState.shortcutLeftAction, appLabelLeft, viewModel
                    ) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetShortcutLeft)
                        else viewModel.setShortcutLeftAction(action)
                    }
                })
                SettingsSelect(title = stringResource(R.string.shortcut_right_icon), option = prefs.shortcutRightIcon.name, fontSize = titleFontSize, onClick = {
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(), options = iconLabels.toTypedArray(),
                        titleResId = R.string.bottom_widget,
                        selectedIndex = Constants.ShortcutIcon.entries.indexOf(prefs.shortcutRightIcon),
                        onItemSelected = { selected ->
                            viewModel.setShortcutRightIcon(Constants.ShortcutIcon.valueOf(selected))
                        }
                    )
                })
                val appLabelRight = prefs.appShortcutRight.activityLabel
                val rightDisplay = when (homeUiState.shortcutRightAction) {
                    Action.OpenApp -> appLabelRight.ifEmpty { getString(R.string.open_app) }
                    else -> homeUiState.shortcutRightAction.getString(requireContext())
                }
                SettingsSelect(title = stringResource(R.string.shortcut_right_action), option = rightDisplay, fontSize = titleFontSize, onClick = {
                    dialogBuilder.showGestureActionPicker(
                        requireContext(), R.string.bottom_widget, AppDrawerFlag.SetShortcutRight,
                        shortcutActions, homeUiState.shortcutRightAction, appLabelRight, viewModel
                    ) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetShortcutRight)
                        else viewModel.setShortcutRightAction(action)
                    }
                })
            }
            if (homeUiState.bottomWidgetType == Constants.BottomWidgetType.Events.value) {
                val calendarDisplayName = when {
                    prefs.eventsCalendarId == CalendarEventsHelper.ALL_CALENDARS_ID -> getString(R.string.events_all_calendars)
                    prefs.eventsCalendarName.isNotBlank() -> prefs.eventsCalendarName
                    else -> getString(R.string.events_no_calendar)
                }
                SettingsSelect(
                    title = stringResource(R.string.events_choose_calendar), option = calendarDisplayName, fontSize = titleFontSize,
                    description = stringResource(R.string.desc_events_calendar),
                    onClick = {
                        if (!CalendarEventsHelper.hasCalendarPermission(requireContext())) {
                            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            return@SettingsSelect
                        }
                        com.github.gezimos.inkos.helper.EditModeHelper.showEventsCalendarPicker(
                            requireContext(), dialogBuilder, viewModel, prefs
                        ) { viewModel.refreshEvents(requireContext()) }
                    }
                )
                val filterLabels = listOf(
                    getString(R.string.events_filter_24h), getString(R.string.events_filter_1week),
                    getString(R.string.events_filter_2weeks), getString(R.string.events_filter_1month)
                )
                SettingsSelect(
                    title = stringResource(R.string.events_filter),
                    option = filterLabels.getOrElse(prefs.eventsFilter) { getString(R.string.events_filter_1week) },
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_events_filter),
                    onClick = {
                        dialogBuilder.showSingleChoiceDialog(
                            context = requireContext(), options = filterLabels.toTypedArray(),
                            titleResId = R.string.events_filter, selectedIndex = prefs.eventsFilter,
                            onItemSelected = { selectedLabel ->
                                val idx = filterLabels.indexOf(selectedLabel)
                                if (idx >= 0) viewModel.setEventsFilter(idx)
                            }
                        )
                    }
                )
            }
            if (homeUiState.bottomWidgetType == Constants.BottomWidgetType.Quote.value) {
                SettingsSelect(
                    title = stringResource(R.string.quote_text),
                    option = if (homeUiState.quoteText.length > 12) "${homeUiState.quoteText.take(12)}..." else homeUiState.quoteText,
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_quote_text),
                    onClick = {
                        dialogBuilder.showInputDialog(requireContext(), getString(R.string.quote_text), homeUiState.quoteText) { viewModel.setQuoteText(it) }
                    }
                )
            }
            if (homeUiState.bottomWidgetType != Constants.BottomWidgetType.Disabled.value && homeUiState.bottomWidgetType != Constants.BottomWidgetType.AndroidWidget.value) {
                SettingsSelect(title = stringResource(R.string.home_quote_alignment),
                    option = alignmentLabels.getOrElse(homeUiState.quoteAlignment) { stringResource(R.string.left) },
                    optionAlignment = homeUiState.quoteAlignment,
                    description = stringResource(R.string.desc_quote_alignment),
                    fontSize = titleFontSize, onClick = { viewModel.setHomeQuoteAlignment((homeUiState.quoteAlignment + 1) % 3) })
            }
            if (homeUiState.bottomWidgetType == Constants.BottomWidgetType.AndroidWidget.value) {
                val widgetLabel = remember(homeUiState.androidWidgetId) {
                    com.github.gezimos.inkos.helper.AppWidgetHelper.getInstance(requireContext())
                        .getCurrentWidgetLabel() ?: "None"
                }
                SettingsSelect(
                    title = stringResource(R.string.choose_android_widget), option = widgetLabel, fontSize = titleFontSize,
                    description = stringResource(R.string.desc_choose_widget),
                    onClick = {
                        val appWidgetHelper = com.github.gezimos.inkos.helper.AppWidgetHelper.getInstance(requireContext())
                        val widgetApps = appWidgetHelper.getInstalledWidgetsByApp()
                        if (widgetApps.isEmpty()) {
                            android.widget.Toast.makeText(requireContext(), "No widgets available", android.widget.Toast.LENGTH_SHORT).show()
                            return@SettingsSelect
                        }
                        val ctx = requireContext()
                        val navController = findNavController()
                        dialogBuilder.showSheet {
                            val selectedApp = remember { mutableStateOf<com.github.gezimos.inkos.helper.AppWidgetHelper.WidgetApp?>(null) }
                            val app = selectedApp.value
                            if (app == null) {
                                com.github.gezimos.inkos.helper.WidgetAppListContent(
                                    context = ctx, widgetApps = widgetApps,
                                    onAppSelected = { selectedApp.value = it },
                                    onClose = { dialogBuilder.dismissAll() }
                                )
                            } else {
                                com.github.gezimos.inkos.helper.WidgetListContent(
                                    app = app, appWidgetHelper = appWidgetHelper,
                                    onBack = { selectedApp.value = null },
                                    onWidgetPicked = { providerInfo ->
                                        dialogBuilder.dismissAll()
                                        appWidgetHelper.pickWidgetFromProvider(
                                            providerInfo = providerInfo,
                                            onSuccess = { widgetId -> viewModel.setAndroidWidgetId(widgetId) },
                                            onNeedsPermission = { _, _ ->
                                                HomeFragmentCompose.pendingWidgetPickerSignal = true
                                                try { navController.popBackStack(R.id.mainFragment, false) } catch (_: Exception) {}
                                            },
                                            onNeedsConfigure = { _, _ ->
                                                HomeFragmentCompose.pendingWidgetPickerSignal = false
                                                try { navController.popBackStack(R.id.mainFragment, false) } catch (_: Exception) {}
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                )
                SettingsSelect(
                    title = stringResource(R.string.android_widget_height), option = "${homeUiState.androidWidgetHeight}dp", fontSize = titleFontSize,
                    description = stringResource(R.string.desc_widget_height),
                    onClick = {
                        dialogBuilder.showSliderDialog(requireContext(), getString(R.string.android_widget_height),
                            Constants.MIN_ANDROID_WIDGET_HEIGHT, Constants.MAX_ANDROID_WIDGET_HEIGHT, homeUiState.androidWidgetHeight,
                            liveUpdate = true) { viewModel.setAndroidWidgetHeight(it) }
                    }
                )
                SettingsSelect(
                    title = stringResource(R.string.android_widget_margin_start), option = "${homeUiState.androidWidgetMarginStart}%", fontSize = titleFontSize,
                    description = stringResource(R.string.desc_widget_margin_start),
                    onClick = {
                        dialogBuilder.showSliderDialog(requireContext(), getString(R.string.android_widget_margin_start),
                            0, Constants.MAX_ANDROID_WIDGET_MARGIN, homeUiState.androidWidgetMarginStart,
                            liveUpdate = true) { viewModel.setAndroidWidgetMarginStart(it) }
                    }
                )
                SettingsSelect(
                    title = stringResource(R.string.android_widget_margin_end), option = "${homeUiState.androidWidgetMarginEnd}%", fontSize = titleFontSize,
                    description = stringResource(R.string.desc_widget_margin_end),
                    onClick = {
                        dialogBuilder.showSliderDialog(requireContext(), getString(R.string.android_widget_margin_end),
                            0, Constants.MAX_ANDROID_WIDGET_MARGIN, homeUiState.androidWidgetMarginEnd,
                            liveUpdate = true) { viewModel.setAndroidWidgetMarginEnd(it) }
                    }
                )
                if (homeUiState.androidWidgetId != -1) {
                    SettingsSelect(
                        title = stringResource(R.string.remove_android_widget), option = "", fontSize = titleFontSize,
                        description = stringResource(R.string.desc_remove_widget),
                        onClick = {
                            com.github.gezimos.inkos.helper.AppWidgetHelper.getInstance(requireContext()).removeWidget()
                            viewModel.setAndroidWidgetId(-1)
                        }
                    )
                }
            }
            SettingsTitle(text = stringResource(R.string.other_functions), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.extend_home_apps_area), fontSize = titleFontSize,
                description = stringResource(R.string.desc_extend_area),
                defaultState = homeUiState.extendHomeAppsArea, onCheckedChange = { viewModel.setExtendHomeAppsArea(it) })
        }
    }

    // ==================================================================
    // DRAWER
    // ==================================================================

    @Composable
    private fun DrawerContent(fontSize: TextUnit) {
        val navController = findNavController()
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val uiState by viewModel.homeUiState.collectAsState()
        val drawerState by viewModel.appsDrawerUiState.collectAsState()
        val alignmentLabels = listOf(
            stringResource(R.string.left), stringResource(R.string.center), stringResource(R.string.right)
        )

        Column {
            SettingsSelect(
                title = stringResource(R.string.app_drawer),
                option = if (drawerState.appList.isEmpty()) "None" else "${drawerState.appList.size} apps",
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_all_apps),
                onClick = { navController.navigate(R.id.appsFragment, bundleOf("flag" to AppDrawerFlag.LaunchApp.toString())) }
            )
            SettingsSelect(
                title = stringResource(R.string.settings_hidden_apps_title),
                option = if (drawerState.hiddenApps.isEmpty()) "None" else "${drawerState.hiddenApps.size} hidden",
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_hidden_apps),
                onClick = {
                    viewModel.getHiddenApps()
                    navController.navigate(R.id.appsFragment, bundleOf("flag" to AppDrawerFlag.HiddenApps.toString()))
                }
            )
            SettingsSelect(
                title = getString(R.string.app_shortcuts),
                description = stringResource(R.string.desc_app_shortcuts),
                option = run {
                    val allShortcuts = com.github.gezimos.inkos.helper.getAllAppShortcuts(requireContext())
                    val validKeys = allShortcuts.map { it.key }.toSet()
                    val selectedRaw = prefs.selectedAppShortcuts
                    val selectionBeenSet = prefs.hasSelectedAppShortcutsBeenSet()
                    val effectiveSelection = if (!selectionBeenSet) {
                        com.github.gezimos.inkos.helper.computeDefaultShortcutSelection(allShortcuts, BuildConfig.APPLICATION_ID)
                    } else {
                        selectedRaw.filter { validKeys.contains(it) }.toSet()
                    }
                    if (allShortcuts.isEmpty()) "None"
                    else if (effectiveSelection.isEmpty()) "None"
                    else "${effectiveSelection.size} selected"
                },
                fontSize = titleFontSize,
                onClick = {
                    val allShortcuts = com.github.gezimos.inkos.helper.getAllAppShortcuts(requireContext())
                    if (allShortcuts.isEmpty()) {
                        android.widget.Toast.makeText(requireContext(), "No app shortcuts available", android.widget.Toast.LENGTH_SHORT).show()
                        return@SettingsSelect
                    }
                    val inkosShortcuts = allShortcuts.filter { it.packageName == BuildConfig.APPLICATION_ID && it.shortcutId.startsWith("inkos_") }
                    val pinnedShortcuts = allShortcuts.filter { it.isPinned && it.packageName != BuildConfig.APPLICATION_ID }
                    val appShortcuts = allShortcuts.filter { !it.isPinned && it.packageName != BuildConfig.APPLICATION_ID }
                    val inkosItems = inkosShortcuts.map { com.github.gezimos.inkos.ui.dialogs.ShortcutItem(it.key, it.label) }
                    val pinnedItems = pinnedShortcuts.map { com.github.gezimos.inkos.ui.dialogs.ShortcutItem(it.key, it.label) }
                    val pm = requireContext().packageManager
                    val appGroups = appShortcuts.groupBy { it.packageName }.map { (pkg, shortcuts) ->
                        val appName = try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { pkg.substringAfterLast('.') }
                        com.github.gezimos.inkos.ui.dialogs.ShortcutGroup(
                            groupName = appName,
                            items = shortcuts.map { com.github.gezimos.inkos.ui.dialogs.ShortcutItem(it.key, it.label) }
                        )
                    }.sortedBy { it.groupName.lowercase() }
                    val validKeys = allShortcuts.map { it.key }.toSet()
                    val selectedRaw = prefs.selectedAppShortcuts
                    val selectionBeenSet = prefs.hasSelectedAppShortcutsBeenSet()
                    val effectiveSelection = if (!selectionBeenSet) {
                        com.github.gezimos.inkos.helper.computeDefaultShortcutSelection(allShortcuts, BuildConfig.APPLICATION_ID)
                    } else {
                        selectedRaw.filter { validKeys.contains(it) }.toSet()
                    }
                    if (selectionBeenSet && effectiveSelection.size < selectedRaw.size) {
                        viewModel.setSelectedAppShortcuts(effectiveSelection)
                    }
                    dialogBuilder.showTabbedShortcutsDialog(
                        title = getString(R.string.app_shortcuts), appGroups = appGroups,
                        inkosItems = inkosItems, pinnedItems = pinnedItems,
                        selectedKeys = effectiveSelection,
                        onSelectionChanged = { selected -> viewModel.setSelectedAppShortcuts(selected) }
                    )
                }
            )
            SettingsTitle(text = stringResource(R.string.customizations), fontSize = titleFontSize)
            SettingsSelect(
                title = stringResource(R.string.app_padding_size), option = uiState.appDrawerGap.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_drawer_gap),
                onClick = {
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.app_padding_size),
                        Constants.MIN_TEXT_PADDING, Constants.MAX_TEXT_PADDING, uiState.appDrawerGap) { viewModel.setAppDrawerGap(it) }
                }
            )
            SettingsSelect(
                title = stringResource(R.string.app_drawer_alignment),
                option = alignmentLabels.getOrElse(uiState.appDrawerAlignment) { stringResource(R.string.left) },
                optionAlignment = uiState.appDrawerAlignment,
                fontSize = titleFontSize, description = stringResource(R.string.desc_drawer_alignment),
                onClick = { viewModel.setAppDrawerAlignment((uiState.appDrawerAlignment + 1) % 3) }
            )
            SettingsSwitch(text = stringResource(R.string.show_icons), fontSize = titleFontSize,
                description = stringResource(R.string.desc_drawer_icons),
                defaultState = drawerState.drawerShowIcons, onCheckedChange = { viewModel.setDrawerShowIcons(it) })
            SettingsTitle(text = stringResource(R.string.filtring), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.az_filter), fontSize = titleFontSize,
                description = stringResource(R.string.desc_az_filter),
                defaultState = uiState.appDrawerAzFilter, onCheckedChange = { viewModel.setAppDrawerAzFilter(it) })
            val sortOrderLabels = listOf(stringResource(R.string.option_az), stringResource(R.string.option_most_used), stringResource(R.string.option_last_used))
            SettingsSelect(title = stringResource(R.string.sort_order),
                option = sortOrderLabels.getOrElse(drawerState.appDrawerSortOrder.coerceIn(0, 2)) { "A-Z" },
                description = stringResource(R.string.desc_drawer_sort),
                fontSize = titleFontSize, onClick = { viewModel.setAppDrawerSortOrder((drawerState.appDrawerSortOrder + 1) % 3) })
            SettingsSwitch(text = stringResource(R.string.hide_home_apps), fontSize = titleFontSize,
                description = stringResource(R.string.desc_hide_home_apps),
                defaultState = uiState.hideHomeApps, onCheckedChange = { viewModel.setHideHomeApps(!uiState.hideHomeApps) })
            SettingsTitle(text = stringResource(R.string.search), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.search), fontSize = titleFontSize,
                description = stringResource(R.string.desc_show_search_bar),
                defaultState = uiState.appDrawerSearchEnabled,
                onCheckedChange = { viewModel.setAppDrawerSearchEnabled(it) })
            SettingsSwitch(text = stringResource(R.string.auto_show_keyboard), fontSize = titleFontSize,
                description = stringResource(R.string.desc_auto_keyboard),
                defaultState = uiState.appDrawerAutoShowKeyboard, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { viewModel.setAppDrawerAutoShowKeyboard(it) })
            SettingsSwitch(text = stringResource(R.string.auto_launch_result), fontSize = titleFontSize,
                description = stringResource(R.string.desc_auto_open_result),
                defaultState = uiState.appDrawerAutoLaunch, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { viewModel.setAppDrawerAutoLaunch(it) })
            SettingsSwitch(text = stringResource(R.string.search_hidden_apps), fontSize = titleFontSize,
                description = stringResource(R.string.desc_search_hidden),
                defaultState = uiState.appDrawerSearchHiddenAppsEnabled, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { viewModel.setAppDrawerSearchHiddenAppsEnabled(it) })
            SettingsSwitch(text = stringResource(R.string.search_contacts), fontSize = titleFontSize,
                description = stringResource(R.string.desc_search_contacts),
                defaultState = uiState.appDrawerSearchContactsEnabled, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !com.github.gezimos.inkos.helper.ContactsHelper.hasContactsPermission(requireContext())) {
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    } else {
                        viewModel.setAppDrawerSearchContactsEnabled(enabled)
                    }
                })
            if (uiState.appDrawerSearchContactsEnabled && uiState.appDrawerSearchEnabled) {
                val accounts = remember { com.github.gezimos.inkos.helper.ContactsHelper.getAvailableAccounts(requireContext()) }
                if (accounts.size > 1) {
                    val selectedAccounts = uiState.appDrawerSearchContactAccounts
                    val displayOption = if (selectedAccounts == null) "All" else "${selectedAccounts.size}/${accounts.size}"
                    SettingsSelect(
                        title = stringResource(R.string.contacts_to_show),
                        option = displayOption,
                        fontSize = titleFontSize,
                        description = stringResource(R.string.desc_contact_lists),
                        onClick = {
                            val items = accounts.map { it.second }.toTypedArray()
                            val initialChecked = BooleanArray(accounts.size) { idx ->
                                selectedAccounts == null || accounts[idx].first in selectedAccounts
                            }
                            dialogBuilder.showMultiChoiceDialog(
                                context = requireContext(),
                                title = getString(R.string.contacts_to_show),
                                items = items,
                                initialChecked = initialChecked,
                                onConfirm = { selectedIndices ->
                                    val selected = selectedIndices.map { accounts[it].first }.toSet()
                                    // If all selected, store null (= all)
                                    viewModel.setAppDrawerSearchContactAccounts(
                                        if (selected.size == accounts.size) null else selected
                                    )
                                }
                            )
                        }
                    )
                }
            }
            SettingsSwitch(text = stringResource(R.string.search_web), fontSize = titleFontSize,
                description = stringResource(R.string.desc_search_web),
                defaultState = uiState.appDrawerSearchWebEnabled, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { viewModel.setAppDrawerSearchWebEnabled(it) })
            SettingsSwitch(text = stringResource(R.string.search_settings), fontSize = titleFontSize,
                description = stringResource(R.string.desc_search_settings),
                defaultState = uiState.appDrawerSearchSettingsEnabled, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { viewModel.setAppDrawerSearchSettingsEnabled(it) })
            SettingsSwitch(text = stringResource(R.string.search_music), fontSize = titleFontSize,
                description = stringResource(R.string.desc_search_music),
                defaultState = uiState.appDrawerSearchMusicEnabled, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !com.github.gezimos.inkos.helper.MusicSearchHelper.hasAudioPermission(requireContext())) {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_AUDIO
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        audioPermissionLauncher.launch(permission)
                    } else {
                        viewModel.setAppDrawerSearchMusicEnabled(enabled)
                    }
                })
            SettingsSwitch(text = stringResource(R.string.search_files), fontSize = titleFontSize,
                description = stringResource(R.string.desc_search_files),
                defaultState = uiState.appDrawerSearchFilesEnabled, enabled = uiState.appDrawerSearchEnabled,
                onCheckedChange = {
                    viewModel.setAppDrawerSearchFilesEnabled(it)
                    if (it && com.github.gezimos.inkos.helper.FileSearchHelper.getPersistedFolders(requireContext()).isEmpty()) {
                        settingsFolderPickerLauncher.launch(null)
                    }
                })
            if (uiState.appDrawerSearchFilesEnabled && uiState.appDrawerSearchEnabled) {
                var folderCount by remember {
                    mutableIntStateOf(
                        com.github.gezimos.inkos.helper.FileSearchHelper.getPersistedFolders(requireContext()).size
                    )
                }
                SettingsSelect(
                    title = stringResource(R.string.search_folders),
                    option = if (folderCount == 0) "Add" else folderCount.toString(),
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_search_folders),
                    onClick = {
                        showSearchFoldersSheet { folderCount = it }
                    }
                )
            }
        }
    }

    // ==================================================================
    // LOOK & FEEL
    // ==================================================================

    @Composable
    private fun LookFeelContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val uiState by viewModel.homeUiState.collectAsState()
        val isDarkParam = prefs.getResolvedTheme() == Dark
        val navController = findNavController()

        Constants.updateMaxHomePages(requireContext())
        SettingsTheme(isDarkParam) {
            Column(modifier = Modifier.fillMaxSize()) {
                SettingsTitle(text = stringResource(R.string.visibility_display), fontSize = titleFontSize)

                // UI Scale
                val detected = remember { com.github.gezimos.inkos.style.detectScaleMode(requireContext()) }
                val currentScaleMode = remember { mutableStateOf(prefs.uiScaleMode) }
                val activeMode = com.github.gezimos.inkos.style.UiScaleMode.fromId(currentScaleMode.value)
                val allModes = com.github.gezimos.inkos.style.UiScaleMode.entries.filter { it != com.github.gezimos.inkos.style.UiScaleMode.AUTO }
                val displayLabel = if (activeMode == com.github.gezimos.inkos.style.UiScaleMode.AUTO) "${detected.label} (Auto)" else activeMode.label
                val selectedIdx = if (activeMode == com.github.gezimos.inkos.style.UiScaleMode.AUTO) {
                    allModes.indexOf(detected)
                } else {
                    allModes.indexOf(activeMode)
                }
                SettingsSelect(
                    title = stringResource(R.string.ui_scale), option = displayLabel, fontSize = titleFontSize,
                    description = "All in one UI Scaling",
                    onClick = {
                        dialogBuilder.showSheet {
                            val baseSettingsSize = 16f
                            SheetTitle("UI Scale")
                            allModes.forEachIndexed { index, mode ->
                                val label = if (mode == detected) "${mode.label} (Auto)" else mode.label
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused = interactionSource.collectIsFocusedAsState().value
                                val prefTextColor = Theme.colors.text
                                val prefBgColor = Theme.colors.background
                                val optionFontSize = ((baseSettingsSize - 3) * 1.5f * mode.scale).sp
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .then(with(SettingsComposable) {
                                            Modifier.pillHighlight(isFocused, 6.dp, prefTextColor)
                                        })
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            val modeId = if (mode == detected) 0 else mode.id
                                            prefs.uiScaleMode = modeId
                                            currentScaleMode.value = modeId
                                            val s = mode.scale
                                            viewModel.setSettingsSize((16 * s).toInt())
                                            viewModel.setClockSize((48 * s).toInt())
                                            viewModel.setAppSize((27 * s).toInt())
                                            viewModel.setDateSize((18 * s).toInt())
                                            viewModel.setQuoteSize((18 * s).toInt())
                                            viewModel.setNotificationsTextSize((18 * s).toInt())
                                            viewModel.setLettersTextSize((18 * s).toInt())
                                            viewModel.setLettersTitleSize((36 * s).toInt())
                                            viewModel.setAppDrawerSize((24 * s).toInt())
                                            viewModel.setTextPaddingSize((10 * s).toInt())
                                            viewModel.setAppDrawerGap((8 * s).toInt())
                                            viewModel.setTopWidgetMargin((32 * s).toInt())
                                            viewModel.setBottomWidgetMargin((32 * s).toInt())
                                            requireActivity().recreate()
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedIdx == index,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = if (isFocused) prefBgColor else prefTextColor,
                                            unselectedColor = if (isFocused) prefBgColor else prefTextColor
                                        )
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        style = SettingsTheme.typography.item,
                                        fontSize = optionFontSize,
                                        color = if (isFocused) prefBgColor else prefTextColor
                                    )
                                }
                            }
                        }
                    }
                )

                SettingsSelect(
                    title = stringResource(R.string.themes),
                    option = remember(uiState.textColor, uiState.backgroundColor) { getChosenThemeDisplayName(requireContext()) },
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_theme_preset),
                    onClick = { navController.navigate(R.id.themePresetsFragment) }
                )
                SettingsSelect(
                    title = stringResource(id = R.string.theme_mode),
                    option = when (uiState.appTheme) { Dark -> "Dark"; Constants.Theme.Light -> "Light"; Constants.Theme.System -> "System" },
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_theme_mode),
                    onClick = {
                        val themeOptions = listOf(Constants.Theme.Light, Dark, Constants.Theme.System)
                        val currentIndex = themeOptions.indexOf(uiState.appTheme)
                        dialogBuilder.showSingleChoiceDialog(
                            context = requireContext(), options = themeOptions.toTypedArray(),
                            titleResId = R.string.theme_mode, selectedIndex = currentIndex,
                            onItemSelected = { newTheme ->
                                viewModel.setAppTheme(newTheme)
                                AppCompatDelegate.setDefaultNightMode(when (newTheme) {
                                    Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                                    Dark -> AppCompatDelegate.MODE_NIGHT_YES
                                    Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                })
                                requireActivity().recreate()
                            }
                        )
                    }
                )
                SettingsSelectWithDualColorPreview(
                    title = stringResource(R.string.element_colors),
                    color1 = Color(uiState.backgroundColor),
                    color2 = Color(uiState.textColor),
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_element_colors),
                    onClick = { navController.navigate(R.id.colorEditorFragment) }
                )
                SettingsSelect(title = stringResource(R.string.wallpaper), option = stringResource(R.string.open), fontSize = titleFontSize,
                    description = stringResource(R.string.desc_wallpaper),
                    onClick = { navController.navigate(R.id.wallpaperFragment) })
                val wallpaperVisibility = 100 - (uiState.backgroundOpacity.coerceIn(0, 255) * 100 / 255)
                SettingsSelect(title = stringResource(R.string.wallpaper_visibility), option = "$wallpaperVisibility%", fontSize = titleFontSize,
                    description = stringResource(R.string.desc_wallpaper_opacity),
                    onClick = {
                        dialogBuilder.showSliderDialog(requireContext(), "Wallpaper Visibility", 0, 100, wallpaperVisibility) { value ->
                            viewModel.setBackgroundOpacity(255 - (value * 255 / 100))
                        }
                    })
                SettingsTitle(text = stringResource(R.string.icons_buttons), fontSize = titleFontSize)
                SettingsSwitch(text = stringResource(R.string.show_icons), fontSize = titleFontSize, description = stringResource(R.string.desc_show_icons), defaultState = uiState.showIcons, onCheckedChange = { viewModel.setShowIcons(it) })
                val currentStyleName = when (uiState.iconSourceMode) {
                    0 -> "Letters"; 2 -> "System"
                    3 -> { val packs = IconPackUtility.getInstalledIconPacks(requireContext()); packs.firstOrNull { it.first == uiState.selectedIconPackPackage }?.second ?: "Icon Pack" }
                    4 -> stringResource(R.string.icon_inkos); 5 -> stringResource(R.string.icon_minimal); 6 -> stringResource(R.string.icon_filled); else -> "Letters"
                }
                SettingsSelect(title = stringResource(R.string.icon_style), option = currentStyleName, fontSize = titleFontSize,
                    description = stringResource(R.string.desc_icon_pack),
                    onClick = {
                        val packs = IconPackUtility.getInstalledIconPacks(requireContext())
                        val allOptions = mutableListOf(getString(R.string.icon_letters), getString(R.string.icon_inkos), getString(R.string.icon_minimal), getString(R.string.icon_filled), getString(R.string.icon_system))
                        packs.forEach { allOptions.add(it.second) }
                        val selectedIdx = when (uiState.iconSourceMode) {
                            0 -> 0; 4 -> 1; 5 -> 2; 6 -> 3; 2 -> 4
                            3 -> { val packIdx = packs.indexOfFirst { it.first == uiState.selectedIconPackPackage }; if (packIdx >= 0) 5 + packIdx else 0 }
                            else -> 0
                        }
                        dialogBuilder.showSingleChoiceDialog(
                            context = requireContext(), options = allOptions.toTypedArray(),
                            titleResId = R.string.show_icons, selectedIndex = selectedIdx,
                            onItemSelected = { selected ->
                                val idx = allOptions.indexOf(selected)
                                when {
                                    idx == 0 -> viewModel.setIconSourceMode(0)
                                    idx == 1 -> viewModel.setIconSourceMode(4)
                                    idx == 2 -> viewModel.setIconSourceMode(5)
                                    idx == 3 -> viewModel.setIconSourceMode(6)
                                    idx == 4 -> viewModel.setIconSourceMode(2)
                                    idx >= 5 && packs.isNotEmpty() -> {
                                        val packIdx = idx - 5
                                        if (packIdx in packs.indices) { viewModel.setIconSourceMode(3); viewModel.setSelectedIconPackPackage(packs[packIdx].first) }
                                    }
                                }
                            }
                        )
                    })
                val iconShapeLabels = listOf(stringResource(R.string.option_pill), stringResource(R.string.option_rounded), stringResource(R.string.option_square))
                SettingsSelect(title = stringResource(R.string.icon_shape), option = iconShapeLabels.getOrElse(uiState.iconShape) { "Pill" },
                    description = stringResource(R.string.desc_icon_shape),
                    fontSize = titleFontSize, onClick = { viewModel.setIconShape((uiState.iconShape + 1) % 3) })
                if (uiState.iconSourceMode == 4) {
                    SettingsSelect(
                        title = stringResource(R.string.icon_tint_contrast),
                        option = String.format("%.1f×", uiState.iconTintContrast / 10f),
                        description = stringResource(R.string.desc_icon_tint_contrast),
                        fontSize = titleFontSize,
                        onClick = {
                            dialogBuilder.showSliderDialog(
                                context = requireContext(),
                                title = getString(R.string.icon_tint_contrast),
                                minValue = 0, maxValue = 30,
                                currentValue = uiState.iconTintContrast,
                                onValueSelected = { v -> viewModel.setIconTintContrast(v) }
                            )
                        }
                    )
                }
                SettingsSelect(title = stringResource(R.string.button_corners), option = iconShapeLabels.getOrElse(uiState.textIslandsShape) { "Pill" },
                    description = stringResource(R.string.desc_button_corners),
                    fontSize = titleFontSize, onClick = { viewModel.setTextIslandsShape((uiState.textIslandsShape + 1) % 3) })
                SettingsTitle(text = stringResource(R.string.text_islands_section), fontSize = titleFontSize)
                SettingsSwitch(text = stringResource(R.string.text_islands), fontSize = titleFontSize, description = stringResource(R.string.desc_text_islands), defaultState = uiState.textIslands, onCheckedChange = { viewModel.setTextIslands(it) })
                SettingsSwitch(text = stringResource(R.string.invert_islands), fontSize = titleFontSize, description = stringResource(R.string.desc_invert_islands), defaultState = uiState.textIslandsInverted, onCheckedChange = { viewModel.setTextIslandsInverted(it) })
                SettingsTitle(text = stringResource(R.string.system_section), fontSize = titleFontSize)
                SettingsSwitch(text = stringResource(R.string.vibration_feedback), fontSize = titleFontSize, description = stringResource(R.string.desc_vibration), defaultState = uiState.hapticFeedback,
                    onCheckedChange = { checked -> viewModel.setHapticFeedback(checked); try { VibrationHelper.setEnabled(checked) } catch (_: Exception) {} })
                val vibrationFormatter: (Int) -> String = { v ->
                    if (v == 0) "0x" else {
                        val s = "%.2f".format(v / 100f).trimEnd('0').trimEnd('.')
                        "${s}x"
                    }
                }
                SettingsSelect(
                    title = stringResource(R.string.vibration_strength),
                    option = vibrationFormatter(uiState.vibrationScale),
                    fontSize = titleFontSize,
                    description = stringResource(R.string.desc_vibration_strength),
                    onClick = {
                        dialogBuilder.showSliderDialog(
                            requireContext(),
                            getString(R.string.vibration_strength),
                            0,
                            500,
                            uiState.vibrationScale,
                            liveUpdate = true,
                            step = 25,
                            valueFormatter = vibrationFormatter
                        ) { value ->
                            viewModel.setVibrationScale(value)
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                        }
                    }
                )
                SettingsSwitch(text = stringResource(R.string.show_status_bar), fontSize = titleFontSize, description = stringResource(R.string.desc_status_bar), defaultState = uiState.showStatusBar,
                    onCheckedChange = { checked -> viewModel.setShowStatusBar(checked); if (checked) showStatusBar(requireActivity()) else hideStatusBar(requireActivity()) })
                SettingsSwitch(text = stringResource(R.string.show_navigation_bar), fontSize = titleFontSize, description = stringResource(R.string.desc_navigation_bar), defaultState = uiState.showNavigationBar,
                    onCheckedChange = { checked -> viewModel.setShowNavigationBar(checked); if (checked) showNavigationBar(requireActivity()) else hideNavigationBar(requireActivity()) })
            }
        }
    }

    // ==================================================================
    // GESTURES
    // ==================================================================

    private fun getOpenAppLabel(label: String): String = label
    private fun openAppDisplay(label: String): String = if (label.isEmpty()) getString(R.string.open_app) else label

    private fun setGesture(flag: AppDrawerFlag, action: Action) {
        when (flag) {
            AppDrawerFlag.SetDoubleTap -> viewModel.setDoubleTapAction(action)
            AppDrawerFlag.SetClickClock -> viewModel.setClickClockAction(action)
            AppDrawerFlag.SetClickDate -> viewModel.setClickDateAction(action)
            AppDrawerFlag.SetSwipeLeft -> viewModel.setSwipeLeftAction(action)
            AppDrawerFlag.SetSwipeRight -> viewModel.setSwipeRightAction(action)
            AppDrawerFlag.SetSwipeUp -> viewModel.setSwipeUpAction(action)
            AppDrawerFlag.SetSwipeDown -> viewModel.setSwipeDownAction(action)
            AppDrawerFlag.SetQuoteWidget -> viewModel.setQuoteAction(action)
            else -> {}
        }
        // Delay permission check so the gesture picker sheet can dismiss first
        view?.postDelayed({ checkGesturePermission(action) }, 300)
    }

    private var permissionSheet: ComposeBottomSheetHost? = null
    private fun checkGesturePermission(action: Action) {
        val ctx = context ?: return
        when (action) {
            Action.LockScreen, Action.ShowRecents, Action.OpenQuickSettings, Action.OpenPowerDialog -> {
                if (ActionService.instance() == null) {
                    showGesturePermissionSheet(
                        title = getString(R.string.accessibility_permission_title),
                        message = getString(R.string.accessibility_permission_message),
                        onContinue = { ctx.openAccessibilitySettings() }
                    )
                }
            }
            Action.Brightness -> {
                if (!android.provider.Settings.System.canWrite(ctx)) {
                    showGesturePermissionSheet(
                        title = getString(R.string.write_settings_permission_title),
                        message = getString(R.string.write_settings_permission_message),
                        onContinue = {
                            startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS, android.net.Uri.parse("package:${ctx.packageName}")))
                        }
                    )
                }
            }
            else -> {}
        }
    }

    private fun showGesturePermissionSheet(title: String, message: String, onContinue: () -> Unit) {
        permissionSheet?.dismiss()
        val host = ComposeBottomSheetHost(requireActivity())
        permissionSheet = host
        host.show {
            val screenScale = rememberScreenScale()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))) {
                SheetTitle(title)
                Text(text = message, style = SettingsTheme.typography.item, color = Theme.colors.text)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { host.dismiss() }) {
                        Text(text = getString(R.string.btn_deny), style = SettingsTheme.typography.button, color = Theme.colors.text)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        host.dismiss()
                        onContinue()
                    }) {
                        Text(text = getString(R.string.btn_allow), style = SettingsTheme.typography.button, color = Theme.colors.text)
                    }
                }
            }
        }
    }

    private fun showSwipeRatioSlider(titleRes: Int, currentValue: Float, minValue: Float, maxValue: Float, onValueSelected: (Float) -> Unit) {
        val multiplier = 100
        dialogBuilder.showSliderDialog(
            context = requireContext(), title = getString(titleRes),
            minValue = (minValue * multiplier).toInt(), maxValue = (maxValue * multiplier).toInt(),
            currentValue = (currentValue * multiplier).toInt(),
            onValueSelected = { newValue -> onValueSelected(newValue / multiplier.toFloat()) }
        )
    }

    @Composable
    private fun GesturesContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val uiState by viewModel.homeUiState.collectAsState()

        val actions = Action.entries
        val psm = remember { ProfileManager(requireContext()) }
        val filteredActions = actions.filter {
            (psm.isPrivateSpaceSetUp() || it != Action.TogglePrivateSpace) &&
            (psm.hasWorkProfile() || it != Action.ToggleWorkProfile)
        }
        val doubleTapGestureActions = filteredActions.filter { action ->
            action != Action.OpenApp && when (action) { Action.OpenLettersScreen -> uiState.notificationsEnabled; else -> true }
        }.toMutableList().apply { if (!contains(Action.Brightness)) add(Action.Brightness) }
        val clickClockGestureActions = filteredActions.filter { action ->
            when (action) { Action.OpenLettersScreen -> uiState.notificationsEnabled; else -> true }
        }.toMutableList().apply { if (!contains(Action.Brightness)) add(Action.Brightness) }
        val clickDateGestureActions = filteredActions.filter { action ->
            when (action) { Action.OpenLettersScreen -> uiState.notificationsEnabled; else -> true }
        }.toMutableList().apply { if (!contains(Action.Brightness)) add(Action.Brightness) }
        val gestureActions = filteredActions.filter { action ->
            when (action) { Action.OpenLettersScreen -> uiState.notificationsEnabled; else -> true }
        }.toMutableList()
        if (!gestureActions.contains(Action.OpenAppDrawer)) gestureActions.add(Action.OpenAppDrawer)
        if (!clickClockGestureActions.contains(Action.OpenAppDrawer)) clickClockGestureActions.add(Action.OpenAppDrawer)
        if (!doubleTapGestureActions.contains(Action.OpenAppDrawer)) doubleTapGestureActions.add(Action.OpenAppDrawer)
        if (!clickClockGestureActions.contains(Action.ExitLauncher)) clickClockGestureActions.add(Action.ExitLauncher)
        if (!clickDateGestureActions.contains(Action.ExitLauncher)) clickDateGestureActions.add(Action.ExitLauncher)
        if (!doubleTapGestureActions.contains(Action.ExitLauncher)) doubleTapGestureActions.add(Action.ExitLauncher)
        if (!clickClockGestureActions.contains(Action.LockScreen)) clickClockGestureActions.add(Action.LockScreen)
        if (!clickClockGestureActions.contains(Action.ShowRecents)) clickClockGestureActions.add(Action.ShowRecents)
        if (!clickClockGestureActions.contains(Action.OpenQuickSettings)) clickClockGestureActions.add(Action.OpenQuickSettings)
        if (!clickClockGestureActions.contains(Action.OpenPowerDialog)) clickClockGestureActions.add(Action.OpenPowerDialog)
        if (!clickDateGestureActions.contains(Action.LockScreen)) clickDateGestureActions.add(Action.LockScreen)
        if (!clickDateGestureActions.contains(Action.ShowRecents)) clickDateGestureActions.add(Action.ShowRecents)
        if (!clickDateGestureActions.contains(Action.OpenQuickSettings)) clickDateGestureActions.add(Action.OpenQuickSettings)
        if (!clickDateGestureActions.contains(Action.OpenPowerDialog)) clickDateGestureActions.add(Action.OpenPowerDialog)
        if (!doubleTapGestureActions.contains(Action.LockScreen)) doubleTapGestureActions.add(Action.LockScreen)
        if (!doubleTapGestureActions.contains(Action.ShowRecents)) doubleTapGestureActions.add(Action.ShowRecents)
        if (!doubleTapGestureActions.contains(Action.OpenQuickSettings)) doubleTapGestureActions.add(Action.OpenQuickSettings)
        if (!doubleTapGestureActions.contains(Action.OpenPowerDialog)) doubleTapGestureActions.add(Action.OpenPowerDialog)
        val quoteGestureActions = filteredActions.filter { action ->
            when (action) { Action.OpenLettersScreen -> prefs.notificationsEnabled; else -> true }
        }.toMutableList().apply { if (!contains(Action.Brightness)) add(Action.Brightness) }
        if (!quoteGestureActions.contains(Action.OpenAppDrawer)) quoteGestureActions.add(Action.OpenAppDrawer)
        if (!quoteGestureActions.contains(Action.ExitLauncher)) quoteGestureActions.add(Action.ExitLauncher)
        if (!quoteGestureActions.contains(Action.LockScreen)) quoteGestureActions.add(Action.LockScreen)
        if (!quoteGestureActions.contains(Action.ShowRecents)) quoteGestureActions.add(Action.ShowRecents)
        if (!quoteGestureActions.contains(Action.OpenQuickSettings)) quoteGestureActions.add(Action.OpenQuickSettings)
        if (!quoteGestureActions.contains(Action.OpenPowerDialog)) quoteGestureActions.add(Action.OpenPowerDialog)

        val appLabelDoubleTapAction = getOpenAppLabel(prefs.appDoubleTap.activityLabel)
        val appLabelClickClockAction = getOpenAppLabel(prefs.appClickClock.activityLabel)
        val appLabelClickDateAction = getOpenAppLabel(prefs.appClickDate.activityLabel)
        val appLabelQuoteAction = getOpenAppLabel(prefs.appQuoteWidget.activityLabel)
        val appLabelSwipeLeftAction = getOpenAppLabel(prefs.appSwipeLeft.activityLabel)
        val appLabelSwipeRightAction = getOpenAppLabel(prefs.appSwipeRight.activityLabel)
        val appLabelSwipeUpAction = getOpenAppLabel(prefs.appSwipeUp.activityLabel)
        val appLabelSwipeDownAction = getOpenAppLabel(prefs.appSwipeDown.activityLabel)

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTitle(text = stringResource(R.string.tap_click_actions), fontSize = titleFontSize)
            SettingsSelect(
                title = "${stringResource(R.string.double_tap)} (2)",
                option = if (uiState.doubleTapAction == Action.OpenApp) openAppDisplay(appLabelDoubleTapAction) else uiState.doubleTapAction.string(),
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_double_tap),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.double_tap, AppDrawerFlag.SetDoubleTap,
                        doubleTapGestureActions, uiState.doubleTapAction, appLabelDoubleTapAction, viewModel) { action, _ -> setGesture(AppDrawerFlag.SetDoubleTap, action) }
                }
            )
            SettingsSelect(
                title = "${stringResource(R.string.clock_click_app)} (6)",
                option = when (uiState.clickClockAction) { Action.OpenApp -> openAppDisplay(appLabelClickClockAction); Action.OpenAppDrawer -> getString(R.string.app_drawer); else -> uiState.clickClockAction.string() },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_clock_tap),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.clock_click_app, AppDrawerFlag.SetClickClock,
                        clickClockGestureActions, uiState.clickClockAction, appLabelClickClockAction, viewModel) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetClickClock) else setGesture(AppDrawerFlag.SetClickClock, action)
                    }
                }
            )
            SettingsSelect(
                title = "${stringResource(R.string.date_click_app)} (7)",
                option = when (uiState.clickDateAction) { Action.OpenApp -> openAppDisplay(appLabelClickDateAction); Action.OpenAppDrawer -> getString(R.string.app_drawer); else -> uiState.clickDateAction.string() },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_date_tap),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.date_click_app, AppDrawerFlag.SetClickDate,
                        clickDateGestureActions, uiState.clickDateAction, appLabelClickDateAction, viewModel) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetClickDate) else setGesture(AppDrawerFlag.SetClickDate, action)
                    }
                }
            )
            SettingsSelect(
                title = "${stringResource(R.string.quote_click_app)} (8)",
                option = when (uiState.quoteAction) { Action.OpenApp -> openAppDisplay(appLabelQuoteAction); Action.OpenAppDrawer -> getString(R.string.app_drawer); else -> uiState.quoteAction.string() },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_quote_tap),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.quote_click_app, AppDrawerFlag.SetQuoteWidget,
                        quoteGestureActions, uiState.quoteAction, appLabelQuoteAction, viewModel) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetQuoteWidget) else setGesture(AppDrawerFlag.SetQuoteWidget, action)
                    }
                }
            )
            SettingsTitle(text = stringResource(R.string.swipe_movement), fontSize = titleFontSize)
            SettingsSelect(
                title = "${stringResource(R.string.swipe_left_app)}",
                option = when (uiState.swipeLeftAction) { Action.OpenApp -> openAppDisplay(appLabelSwipeLeftAction); Action.OpenAppDrawer -> getString(R.string.app_drawer); else -> uiState.swipeLeftAction.string() },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_swipe_left),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.swipe_left_app, AppDrawerFlag.SetSwipeLeft,
                        gestureActions, uiState.swipeLeftAction, appLabelSwipeLeftAction, viewModel) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetSwipeLeft) else setGesture(AppDrawerFlag.SetSwipeLeft, action)
                    }
                }
            )
            SettingsSelect(
                title = "${stringResource(R.string.swipe_right_app)} (<)",
                option = when (uiState.swipeRightAction) { Action.OpenApp -> openAppDisplay(appLabelSwipeRightAction); Action.OpenAppDrawer -> getString(R.string.app_drawer); Action.Disabled -> stringResource(R.string.disabled); else -> uiState.swipeRightAction.string() },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_swipe_right),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.swipe_right_app, AppDrawerFlag.SetSwipeRight,
                        gestureActions, uiState.swipeRightAction, appLabelSwipeRightAction, viewModel) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetSwipeRight) else setGesture(AppDrawerFlag.SetSwipeRight, action)
                    }
                }
            )
            SettingsSelect(
                title = "Gesture vs page conflict",
                option = "Read more",
                fontSize = titleFontSize,
                description = "How short vs long swipes work with home pages",
                onClick = { dialogBuilder.showGestureVsPageConflictSheet() }
            )
            SettingsSelect(
                title = "${stringResource(R.string.swipe_up_app)} (^)",
                option = when (uiState.swipeUpAction) { Action.OpenApp -> openAppDisplay(appLabelSwipeUpAction); Action.OpenAppDrawer -> getString(R.string.app_drawer); Action.Disabled -> stringResource(R.string.disabled); else -> uiState.swipeUpAction.string() },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_swipe_up),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.swipe_up_app, AppDrawerFlag.SetSwipeUp,
                        gestureActions, uiState.swipeUpAction, appLabelSwipeUpAction, viewModel) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetSwipeUp) else setGesture(AppDrawerFlag.SetSwipeUp, action)
                    }
                }
            )
            SettingsSelect(
                title = "${stringResource(R.string.swipe_down_app)} (v)",
                option = when (uiState.swipeDownAction) { Action.OpenApp -> openAppDisplay(appLabelSwipeDownAction); Action.OpenAppDrawer -> getString(R.string.app_drawer); Action.Disabled -> stringResource(R.string.disabled); else -> uiState.swipeDownAction.string() },
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_swipe_down),
                onClick = {
                    dialogBuilder.showGestureActionPicker(requireContext(), R.string.swipe_down_app, AppDrawerFlag.SetSwipeDown,
                        gestureActions, uiState.swipeDownAction, appLabelSwipeDownAction, viewModel) { action, app ->
                        if (action == Action.OpenApp && app != null) viewModel.selectAppForFlag(app, AppDrawerFlag.SetSwipeDown) else setGesture(AppDrawerFlag.SetSwipeDown, action)
                    }
                }
            )
            SettingsTitle(text = stringResource(R.string.swipe_threshold_ratios), fontSize = titleFontSize)
            SettingsSelect(
                title = stringResource(R.string.short_swipe_ratio),
                option = stringResource(R.string.swipe_ratio_display_format, uiState.shortSwipeThresholdRatio),
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_short_swipe),
                onClick = { showSwipeRatioSlider(R.string.short_swipe_ratio, uiState.shortSwipeThresholdRatio, Constants.MIN_SHORT_SWIPE_RATIO, Constants.MAX_SHORT_SWIPE_RATIO) { viewModel.setShortSwipeThresholdRatio(it) } }
            )
            SettingsSelect(
                title = stringResource(R.string.long_swipe_ratio),
                option = stringResource(R.string.swipe_ratio_display_format, uiState.longSwipeThresholdRatio),
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_long_swipe),
                onClick = { showSwipeRatioSlider(R.string.long_swipe_ratio, uiState.longSwipeThresholdRatio, Constants.MIN_LONG_SWIPE_RATIO, Constants.MAX_LONG_SWIPE_RATIO) { viewModel.setLongSwipeThresholdRatio(it) } }
            )
            SettingsSwitch(text = stringResource(R.string.edge_swipe_back), fontSize = titleFontSize,
                description = stringResource(R.string.desc_edge_swipe),
                defaultState = uiState.edgeSwipeBackEnabled, onCheckedChange = { viewModel.setEdgeSwipeBackEnabled(it) })
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ==================================================================
    // FONTS
    // ==================================================================

    @Composable
    private fun FontsContent(fontSize: TextUnit) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val homeUiState by viewModel.homeUiState.collectAsState()
        var universalFontState by remember { mutableStateOf(prefs.universalFont) }
        var universalFontEnabledState by remember { mutableStateOf(prefs.universalFontEnabled) }
        var settingsFontState by remember { mutableStateOf(prefs.fontFamily) }
        var labelnotificationsFontSize by remember { mutableStateOf(prefs.labelnotificationsTextSize) }
        var notificationsTitle by remember { mutableStateOf(prefs.lettersTitle) }
        var notificationsTitleSize by remember { mutableStateOf(prefs.lettersTitleSize) }
        var notificationsTextSize by remember { mutableStateOf(prefs.notificationsTextSize) }
        val appNameMode = when { homeUiState.allCapsApps -> 2; homeUiState.smallCapsApps -> 1; else -> 0 }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SettingsTitle(text = stringResource(R.string.universal_custom_font), fontSize = titleFontSize, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.reset_all),
                    style = SettingsTheme.typography.button,
                    fontSize = if (titleFontSize.isSpecified) (titleFontSize.value * 0.7).sp else 14.sp,
                    modifier = Modifier.padding(end = SettingsTheme.color.horizontalPadding).clickable {
                        viewModel.resetFontsToDefault()
                        viewModel.setSettingsSize(16); viewModel.setAppSize(27); viewModel.setClockSize(48)
                        viewModel.setLabelNotificationsTextSize(16); viewModel.setDateSize(18); viewModel.setQuoteSize(18)
                        viewModel.setLettersTextSize(18); viewModel.setLettersTitleSize(36); viewModel.setNotificationsTextSize(18)
                        viewModel.setLettersTitle("Letters")
                        universalFontState = Constants.FontFamily.PublicSans; universalFontEnabledState = false
                        settingsFontState = Constants.FontFamily.PublicSans; labelnotificationsFontSize = 16
                        notificationsTitle = "Letters"; notificationsTitleSize = 36; notificationsTextSize = 18
                    }
                )
            }
            SettingsSwitch(text = stringResource(R.string.universal_custom_font), fontSize = titleFontSize,
                description = stringResource(R.string.desc_universal_font),
                defaultState = universalFontEnabledState,
                onCheckedChange = { enabled ->
                    viewModel.setUniversalFontEnabled(enabled); universalFontEnabledState = enabled
                    if (enabled) {
                        val font = prefs.universalFont
                        val fontPath = if (font == Constants.FontFamily.Custom) prefs.getCustomFontPath("universal") else null
                        viewModel.setFontFamily(font); viewModel.setAppsFont(font); viewModel.setClockFont(font)
                        viewModel.setStatusFont(font); viewModel.setLabelNotificationsFont(font); viewModel.setNotificationsFont(font)
                        viewModel.setLettersTitleFont(font); viewModel.setDateFont(font); viewModel.setQuoteFont(font); viewModel.setLettersFont(font)
                        if (font == Constants.FontFamily.Custom && fontPath != null) {
                            for (key in listOf("universal","settings","apps","clock","status","notification","notifications","lettersTitle","date","quote","letters")) viewModel.setCustomFontPath(key, fontPath)
                        }
                        settingsFontState = font
                    }
                })
            SettingsSelect(title = stringResource(R.string.universal_custom_font),
                option = getFontDisplayName(universalFontState, "universal"), fontSize = titleFontSize,
                description = stringResource(R.string.desc_universal_font_select),
                onClick = {
                    showFontSelectionDialogWithCustoms(R.string.universal_custom_font, "universal") { newFont, customPath ->
                        viewModel.setUniversalFont(newFont); universalFontState = newFont
                        val fontPath = if (newFont == Constants.FontFamily.Custom) customPath else null
                        if (prefs.universalFontEnabled) {
                            viewModel.setFontFamily(newFont); viewModel.setAppsFont(newFont); viewModel.setClockFont(newFont)
                            viewModel.setStatusFont(newFont); viewModel.setLabelNotificationsFont(newFont); viewModel.setNotificationsFont(newFont)
                            viewModel.setLettersTitleFont(newFont); viewModel.setDateFont(newFont); viewModel.setQuoteFont(newFont); viewModel.setLettersFont(newFont)
                            if (newFont == Constants.FontFamily.Custom && fontPath != null) {
                                for (key in listOf("universal","settings","apps","clock","status","notification","notifications","lettersTitle","date","quote","letters")) viewModel.setCustomFontPath(key, fontPath)
                            }
                            settingsFontState = newFont
                        }
                        activity?.recreate()
                    }
                }, enabled = prefs.universalFontEnabled)
            SettingsSelect(title = stringResource(R.string.settings_font_section), option = getFontDisplayName(settingsFontState, "settings"), fontSize = titleFontSize,
                description = stringResource(R.string.desc_settings_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.settings_font_section, "settings") { newFont, _ -> viewModel.setFontFamily(newFont); settingsFontState = newFont } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.settings_text_size), option = homeUiState.settingsSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_settings_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), getString(R.string.settings_text_size), Constants.MIN_SETTINGS_TEXT_SIZE, Constants.MAX_SETTINGS_TEXT_SIZE, homeUiState.settingsSize) { viewModel.setSettingsSize(it) } })
            SettingsTitle(text = stringResource(R.string.home_fonts), fontSize = titleFontSize)
            SettingsSelect(title = stringResource(R.string.apps_font), option = getFontDisplayName(homeUiState.appsFont, "apps"), fontSize = titleFontSize,
                description = stringResource(R.string.desc_apps_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.apps_font, "apps") { newFont, customPath -> viewModel.setAppsFont(newFont); customPath?.let { viewModel.setCustomFontPath("apps", it) } } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.app_text_size), option = homeUiState.appSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_apps_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), requireContext().getString(R.string.app_text_size), Constants.MIN_APP_SIZE, Constants.MAX_APP_SIZE, homeUiState.appSize) { viewModel.setAppSize(it) } })
            SettingsSelect(title = stringResource(R.string.app_drawer_size), option = homeUiState.appDrawerSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_drawer_font_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), getString(R.string.app_drawer_size), Constants.MIN_APP_SIZE, Constants.MAX_APP_SIZE, homeUiState.appDrawerSize) { viewModel.setAppDrawerSize(it) } })
            val appNameModeLabels = listOf(stringResource(R.string.app_name_mode_normal), stringResource(R.string.small_caps_apps), stringResource(R.string.app_name_mode_all_caps))
            SettingsSelect(title = stringResource(R.string.app_name_mode), option = appNameModeLabels[appNameMode], fontSize = titleFontSize,
                description = stringResource(R.string.desc_app_name_mode),
                onClick = { val nextMode = (appNameMode + 1) % 3; viewModel.setSmallCapsApps(nextMode == 1); viewModel.setAllCapsApps(nextMode == 2) })
            SettingsSelect(title = stringResource(R.string.clock_font), option = getFontDisplayName(homeUiState.clockFont, "clock"), fontSize = titleFontSize,
                description = stringResource(R.string.desc_clock_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.clock_font, "clock") { newFont, customPath -> viewModel.setClockFont(newFont); customPath?.let { viewModel.setCustomFontPath("clock", it) } } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.clock_text_size), option = homeUiState.clockSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_clock_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), requireContext().getString(R.string.clock_text_size), Constants.MIN_CLOCK_SIZE, Constants.MAX_CLOCK_SIZE, homeUiState.clockSize) { viewModel.setClockSize(it) } })
            SettingsSelect(title = stringResource(R.string.date_font), option = getFontDisplayName(homeUiState.dateFont, "date"), fontSize = titleFontSize,
                description = stringResource(R.string.desc_date_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.date_font, "date") { newFont, customPath -> viewModel.setDateFont(newFont); customPath?.let { viewModel.setCustomFontPath("date", it) } } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.date_text_size), option = homeUiState.dateSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_date_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), requireContext().getString(R.string.date_text_size), 10, 64, homeUiState.dateSize) { viewModel.setDateSize(it) } })
            SettingsSelect(title = stringResource(R.string.quote_font), option = getFontDisplayName(homeUiState.quoteFont, "quote"), fontSize = titleFontSize,
                description = stringResource(R.string.desc_quote_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.quote_font, "quote") { newFont, customPath -> viewModel.setQuoteFont(newFont); customPath?.let { viewModel.setCustomFontPath("quote", it) } } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.quote_text_size), option = homeUiState.quoteSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_quote_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), requireContext().getString(R.string.quote_text_size), 10, 64, homeUiState.quoteSize) { viewModel.setQuoteSize(it) } })
            SettingsTitle(text = stringResource(R.string.label_notifications), fontSize = titleFontSize)
            SettingsSelect(title = stringResource(R.string.label_notifications_font),
                option = if (homeUiState.notificationFont == Constants.FontFamily.Custom) getFontDisplayName(homeUiState.notificationFont, "notification") else homeUiState.notificationFont.name,
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_noti_label_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.app_notification_font, "notification") { newFont, customPath -> viewModel.setLabelNotificationsFont(newFont); if (newFont == Constants.FontFamily.Custom && customPath != null) viewModel.setCustomFontPath("notification", customPath) } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.label_notifications_size), option = labelnotificationsFontSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_noti_label_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), getString(R.string.label_notifications_size), Constants.MIN_LABEL_NOTIFICATION_TEXT_SIZE, Constants.MAX_LABEL_NOTIFICATION_TEXT_SIZE, prefs.labelnotificationsTextSize) { newSize -> viewModel.setLabelNotificationsTextSize(newSize); labelnotificationsFontSize = newSize } })
            SettingsTitle(text = stringResource(R.string.letters_window), fontSize = titleFontSize)
            SettingsSelect(title = stringResource(R.string.letters_window), option = notificationsTitle, fontSize = titleFontSize,
                description = stringResource(R.string.desc_letters_title),
                onClick = { dialogBuilder.showInputDialog(requireContext(), getString(R.string.title_label), notificationsTitle) { newTitle -> val singleLineTitle = newTitle.replace("\n", ""); viewModel.setLettersTitle(singleLineTitle); notificationsTitle = singleLineTitle } })
            SettingsSelect(title = stringResource(R.string.title_font),
                option = if (homeUiState.lettersTitleFont == Constants.FontFamily.Custom) getFontDisplayName(homeUiState.lettersTitleFont, "lettersTitle") else homeUiState.lettersTitleFont.name,
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_letters_title_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.letters_font, "lettersTitle") { newFont, customPath -> viewModel.setLettersTitleFont(newFont); if (newFont == Constants.FontFamily.Custom && customPath != null) viewModel.setCustomFontPath("lettersTitle", customPath) } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.title_size), option = notificationsTitleSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_letters_title_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), getString(R.string.title_size), 10, 60, notificationsTitleSize) { newSize -> viewModel.setLettersTitleSize(newSize); notificationsTitleSize = newSize } })
            SettingsSelect(title = stringResource(R.string.body_font),
                option = if (universalFontEnabledState) { val uf = prefs.universalFont; if (uf == Constants.FontFamily.Custom) getFontDisplayName(uf, "universal") else uf.name }
                else if (homeUiState.notificationsFont == Constants.FontFamily.Custom) getFontDisplayName(homeUiState.notificationsFont, "notifications") else homeUiState.notificationsFont.name,
                fontSize = titleFontSize,
                description = stringResource(R.string.desc_noti_body_font),
                onClick = { if (!universalFontEnabledState) { showFontSelectionDialogWithCustoms(R.string.letters_font, "notifications") { newFont, customPath -> viewModel.setNotificationsFont(newFont); if (newFont == Constants.FontFamily.Custom && customPath != null) viewModel.setCustomFontPath("notifications", customPath) } } },
                fontColor = if (!universalFontEnabledState) SettingsTheme.typography.title.color else SettingsTheme.typography.title.color.copy(alpha = 0.4f), enabled = !universalFontEnabledState)
            SettingsSelect(title = stringResource(R.string.body_text_size), option = notificationsTextSize.toString(), fontSize = titleFontSize,
                description = stringResource(R.string.desc_noti_body_size),
                onClick = { dialogBuilder.showSliderDialog(requireContext(), getString(R.string.body_text_size), Constants.MIN_LABEL_NOTIFICATION_TEXT_SIZE, Constants.MAX_LABEL_NOTIFICATION_TEXT_SIZE, notificationsTextSize) { newSize -> viewModel.setNotificationsTextSize(newSize); notificationsTextSize = newSize } })
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- Font helpers ---

    private fun getFontDisplayName(font: Constants.FontFamily, contextKey: String): String {
        val fontName = if (font == Constants.FontFamily.Custom) {
            val path = if (contextKey == "notifications") {
                prefs.getCustomFontPath("notifications") ?: prefs.getCustomFontPath("universal")
            } else { prefs.getCustomFontPathForContext(contextKey) }
            path?.let { File(it).name } ?: font.name
        } else { font.name }
        return if (fontName.length > 12) "${fontName.take(12)}..." else fontName
    }

    private fun showSearchFoldersSheet(onFolderCountChanged: (Int) -> Unit) {
        dialogBuilder.showSheet {
            SearchFoldersSheet(
                context = requireContext(),
                onAddFolder = {
                    dialogBuilder.dismissAll()
                    settingsFolderPickerLauncher.launch(null)
                },
                onRemoveFolder = { uri ->
                    com.github.gezimos.inkos.helper.FileSearchHelper.removeFolderAccess(requireContext(), uri)
                },
                onFolderCountChanged = onFolderCountChanged,
                onDismiss = { dialogBuilder.dismissAll() }
            )
        }
    }

    private fun showFontSelectionDialogWithCustoms(titleResId: Int, contextKey: String, onFontSelected: (Constants.FontFamily, String?) -> Unit) {
        val fontFamilyEntries = Constants.FontFamily.entries.filter { it != Constants.FontFamily.Custom }
        val context = requireContext()
        val prefs = Prefs(context)
        val builtInFontOptions = fontFamilyEntries.map { it.getString(context) }
        val builtInFonts = fontFamilyEntries.map { it.getFont(context) ?: getTrueSystemFont() }
        val oflFontOptions = fontFamilyEntries.filter { it != Constants.FontFamily.System }.map { it.getString(context) }
        val customFonts = Constants.FontFamily.getAllCustomFonts(context)
        val customFontOptions = customFonts.map { it.first }
        val customFontPaths = customFonts.map { it.second }
        val customFontTypefaces = customFontPaths.map { path -> Constants.FontFamily.Custom.getFont(context, path) ?: getTrueSystemFont() }
        var selectedIndex: Int? = null
        try {
            val currentFont = when (contextKey) { "universal" -> prefs.universalFont; else -> prefs.getFontForContext(contextKey) }
            if (currentFont == Constants.FontFamily.Custom) {
                val path = if (contextKey == "notifications") prefs.getCustomFontPath("notifications") ?: prefs.getCustomFontPath("universal") else prefs.getCustomFontPathForContext(contextKey)
                if (path != null) { val idx = customFontPaths.indexOf(path); if (idx != -1) selectedIndex = builtInFontOptions.size + idx }
            } else { val idx = fontFamilyEntries.indexOf(currentFont); if (idx != -1) selectedIndex = idx }
        } catch (_: Exception) {}
        dialogBuilder.showTabbedFontPicker(
            context = context, titleResId = titleResId,
            builtInOptions = builtInFontOptions, builtInFonts = builtInFonts,
            customOptions = customFontOptions, customFonts = customFontTypefaces,
            selectedIndex = selectedIndex,
            isBuiltInFont = { option -> oflFontOptions.contains(option) },
            onInfoClick = { dialogBuilder.dismissAll(); dialogBuilder.showFontLicenseSheet() },
            addCustomFontLabel = context.getString(R.string.add_custom_font),
            onAddCustomFont = {
                pickCustomFontFile { _, path ->
                    viewModel.setCustomFontPath(contextKey, path); viewModel.addCustomFontPath(path)
                    onFontSelected(Constants.FontFamily.Custom, path); dialogBuilder.dismissAll(); activity?.recreate()
                }
            },
            onItemSelected = { selectedName, isCustom ->
                if (!isCustom) {
                    val builtInIndex = builtInFontOptions.indexOf(selectedName)
                    if (builtInIndex != -1) { onFontSelected(fontFamilyEntries[builtInIndex], null) }
                } else {
                    val customIndex = customFontOptions.indexOf(selectedName)
                    if (customIndex != -1) { val path = customFontPaths[customIndex]; viewModel.setCustomFontPath(contextKey, path); onFontSelected(Constants.FontFamily.Custom, path) }
                }
            },
            onItemDeleted = { deletedName, dismiss ->
                val customIndex = customFontOptions.indexOf(deletedName)
                if (customIndex != -1) {
                    val path = customFontPaths[customIndex]
                    viewModel.removeCustomFontPathByPath(path)
                    val allKeys = prefs.customFontPathMap.filterValues { it == path }.keys
                    for (key in allKeys) {
                        viewModel.setCustomFontPath(key, null)
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
                    dismiss()
                    showFontSelectionDialogWithCustoms(titleResId, contextKey, onFontSelected)
                }
            }
        )
    }

    private fun pickCustomFontFile(onFontPicked: (Typeface, String) -> Unit) {
        onCustomFontSelected = onFontPicked
        fontPickerLauncher.launch(arrayOf(
            "font/ttf", "font/otf", "application/x-font-ttf",
            "application/x-font-opentype", "application/octet-stream"
        ))
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
        } catch (e: Exception) { e.printStackTrace() }
        return null
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use { if (it.moveToFirst()) { val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (index >= 0) name = it.getString(index) } }
        return name
    }

    // ==================================================================
    // NOTIFICATIONS
    // ==================================================================

    private data class AppInfo(val label: String, val packageName: String, val user: String? = null)

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    @Composable
    private fun NotificationsContent(fontSize: TextUnit) {
        val context = requireContext()
        val titleFontSize = if (fontSize != TextUnit.Unspecified) (fontSize.value * 1.5).sp else fontSize
        val uiState by viewModel.homeUiState.collectAsState()
        val hasNotificationListener = remember(context) { isNotificationListenerEnabled(context) }
        var pushNotificationsEnabled by remember { mutableStateOf(hasNotificationListener) }

        LaunchedEffect(context) {
            val initialListener = isNotificationListenerEnabled(context)
            pushNotificationsEnabled = initialListener
            viewModel.setPushNotificationsEnabled(initialListener)
            while (true) {
                kotlinx.coroutines.delay(1000)
                val hasListener = isNotificationListenerEnabled(context)
                if (pushNotificationsEnabled != hasListener) {
                    pushNotificationsEnabled = hasListener
                    viewModel.setPushNotificationsEnabled(hasListener)
                }
            }
        }

        fun openNotificationListenerSettings() {
            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            try { context.startActivity(intent) }
            catch (_: Exception) {
                context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                })
            }
        }

        fun onPushNotificationsToggle(requestedState: Boolean) {
            if (requestedState) {
                val hasListener = isNotificationListenerEnabled(context)
                if (hasListener) { pushNotificationsEnabled = true; viewModel.setPushNotificationsEnabled(true); return }
                openNotificationListenerSettings(); pushNotificationsEnabled = false
            } else { openNotificationListenerSettings() }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsSwitch(text = stringResource(R.string.push_notifications), fontSize = titleFontSize,
                description = stringResource(R.string.desc_noti_access),
                defaultState = pushNotificationsEnabled, onCheckedChange = { onPushNotificationsToggle(it) })
            SettingsTitle(text = stringResource(R.string.notification_home), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.show_notification_badge), fontSize = titleFontSize,
                description = stringResource(R.string.desc_noti_dots),
                defaultState = uiState.showNotificationBadge, enabled = pushNotificationsEnabled, onCheckedChange = { viewModel.setShowNotificationBadge(it) })
            if (uiState.showNotificationBadge) {
                val indicators = Constants.NotificationIndicator.entries
                val currentIdx = uiState.notificationIndicatorStyle.coerceIn(0, indicators.lastIndex)
                val current = indicators[currentIdx]
                SettingsSelect(
                    title = stringResource(R.string.badge_indicator),
                    option = current.symbol,
                    fontSize = titleFontSize,
                    enabled = pushNotificationsEnabled,
                    onClick = {
                        val nextIdx = (currentIdx + 1) % indicators.size
                        viewModel.setNotificationIndicatorStyle(nextIdx)
                    }
                )
            }
            SettingsSwitch(text = stringResource(R.string.label_notifications), fontSize = titleFontSize,
                description = stringResource(R.string.desc_noti_text_home),
                defaultState = uiState.showNotificationText, enabled = pushNotificationsEnabled, onCheckedChange = { viewModel.setShowNotificationText(it) })
            SettingsSwitch(text = stringResource(R.string.show_media_playing_indicator), fontSize = titleFontSize,
                description = stringResource(R.string.desc_media_indicator),
                defaultState = uiState.showMediaIndicator, enabled = pushNotificationsEnabled, onCheckedChange = { viewModel.setShowMediaIndicator(it) })
            SettingsSwitch(text = stringResource(R.string.show_media_playing_name), fontSize = titleFontSize,
                description = stringResource(R.string.desc_media_track_name),
                defaultState = uiState.showMediaName, enabled = pushNotificationsEnabled, onCheckedChange = { viewModel.setShowMediaName(it) })

            var showBadgeDialog by remember { mutableStateOf(false) }
            SettingsSelect(title = stringResource(R.string.home_notifications_allowlist), option = uiState.allowedBadgeNotificationApps.size.toString(),
                fontSize = titleFontSize, description = stringResource(R.string.desc_noti_app_filter), onClick = { showBadgeDialog = true }, enabled = pushNotificationsEnabled)
            if (showBadgeDialog) {
                LaunchedEffect(Unit) {
                    showBadgeDialog = false
                    showAppAllowlistDialog("Label Notification Apps", uiState.allowedBadgeNotificationApps) { viewModel.setAllowedBadgeNotificationApps(it) }
                }
            }
            SettingsTitle(text = stringResource(R.string.chat_notifications_section), fontSize = titleFontSize)
            SettingsSwitch(text = stringResource(R.string.show_sender_name), fontSize = titleFontSize,
                description = stringResource(R.string.desc_sender_name),
                defaultState = uiState.showNotificationSenderName, onCheckedChange = { viewModel.setShowNotificationSenderName(it) }, enabled = pushNotificationsEnabled)
            SettingsSwitch(text = stringResource(R.string.show_conversation_group_name), fontSize = titleFontSize,
                description = stringResource(R.string.desc_group_name),
                defaultState = uiState.showNotificationGroupName, onCheckedChange = { viewModel.setShowNotificationGroupName(it) }, enabled = pushNotificationsEnabled)
            SettingsSwitch(text = stringResource(R.string.show_message), fontSize = titleFontSize,
                description = stringResource(R.string.desc_message_preview),
                defaultState = uiState.showNotificationMessage, onCheckedChange = { viewModel.setShowNotificationMessage(it) }, enabled = pushNotificationsEnabled)
            SettingsSelect(title = stringResource(R.string.badge_character_limit), option = uiState.homeAppCharLimit.toString(),
                fontSize = titleFontSize, enabled = pushNotificationsEnabled,
                description = stringResource(R.string.desc_noti_char_limit),
                onClick = {
                    dialogBuilder.showSliderDialog(requireContext(), getString(R.string.badge_character_limit), 5, 50, uiState.homeAppCharLimit) { viewModel.setHomeAppCharLimit(it) }
                })
            SettingsTitle(text = stringResource(R.string.letters_window), fontSize = titleFontSize)

            var showAllowlistDialog by remember { mutableStateOf(false) }
            SettingsSwitch(text = stringResource(R.string.letters_window), fontSize = titleFontSize,
                description = stringResource(R.string.desc_enable_letters),
                defaultState = uiState.notificationsEnabled, onCheckedChange = { viewModel.setNotificationsEnabled(it) }, enabled = pushNotificationsEnabled)
            SettingsSwitch(text = stringResource(R.string.clear_conversation_on_app_open), fontSize = titleFontSize,
                description = stringResource(R.string.desc_clear_on_open),
                defaultState = uiState.clearConversationOnAppOpen, enabled = pushNotificationsEnabled, onCheckedChange = { viewModel.setClearConversationOnAppOpen(it) })
            SettingsSelect(title = stringResource(R.string.letters_allowlist), option = uiState.allowedNotificationApps.size.toString(),
                fontSize = titleFontSize, description = stringResource(R.string.desc_letters_filter), onClick = { showAllowlistDialog = true }, enabled = pushNotificationsEnabled)
            if (showAllowlistDialog) {
                LaunchedEffect(Unit) {
                    showAllowlistDialog = false
                    showAppAllowlistDialog(getString(R.string.letters_allowlist), uiState.allowedNotificationApps) { viewModel.setAllowedNotificationApps(it) }
                }
            }
            SettingsTitle(text = stringResource(R.string.simple_tray_title), fontSize = titleFontSize)
            SettingsSelect(title = stringResource(R.string.notifications_per_page), option = uiState.notificationsPerPage.toString(),
                fontSize = titleFontSize, description = stringResource(R.string.desc_noti_per_page), onClick = { viewModel.setNotificationsPerPage((uiState.notificationsPerPage % 5) + 1) }, enabled = pushNotificationsEnabled)
            SettingsSwitch(text = stringResource(R.string.enable_bottom_navigation), fontSize = titleFontSize,
                description = stringResource(R.string.desc_bottom_nav),
                defaultState = uiState.enableBottomNav, onCheckedChange = { viewModel.setEnableBottomNav(it) }, enabled = pushNotificationsEnabled)
            var showSimpleTrayAllowlistDialog by remember { mutableStateOf(false) }
            SettingsSelect(title = stringResource(R.string.simple_tray_allowlist), option = uiState.allowedSimpleTrayApps.size.toString(),
                fontSize = titleFontSize, description = stringResource(R.string.desc_simple_tray_filter), onClick = { showSimpleTrayAllowlistDialog = true }, enabled = pushNotificationsEnabled)
            if (showSimpleTrayAllowlistDialog) {
                LaunchedEffect(Unit) {
                    showSimpleTrayAllowlistDialog = false
                    showAppAllowlistDialog("Simple Tray Apps", uiState.allowedSimpleTrayApps) { viewModel.setAllowedSimpleTrayApps(it) }
                }
            }
        }
    }

    private fun showAppAllowlistDialog(title: String, initialSelected: Set<String>, onConfirm: (Set<String>) -> Unit) {
        val appListFlow = viewModel.getAppList(includeHiddenApps = true)
        viewLifecycleOwner.lifecycleScope.launch {
            appListFlow.collectLatest { appListItems ->
                if (appListItems.isEmpty()) return@collectLatest
                val filteredApps = appListItems.filter {
                    val pkg = it.activityPackage
                    pkg.isNotBlank() && !IconUtility.isSyntheticPackage(pkg) &&
                        !(it.isShortcut && it.shortcutId != null && IconUtility.isInkOSInternalShortcut(it.shortcutId))
                }
                val allApps = filteredApps.map {
                    AppInfo(
                        label = it.customLabel.takeIf { l -> l.isNotEmpty() } ?: it.activityLabel,
                        packageName = it.activityPackage,
                        user = it.user.toString()
                    )
                }
                val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
                val sortedApps = allApps.sortedWith(
                    compareByDescending<AppInfo> { initialSelected.contains(it.packageName) }
                        .then(compareBy(collator) { it.label })
                )
                val appLabels = sortedApps.map { it.label }
                val appPackages = sortedApps.map { it.packageName }
                val checkedItems = appPackages.mapIndexed { idx, pkg ->
                    val userStr = sortedApps[idx].user
                    val pkgWithUser = if (!userStr.isNullOrBlank()) "$pkg|$userStr" else pkg
                    initialSelected.contains(pkg) || initialSelected.contains(pkgWithUser)
                }.toBooleanArray()
                dialogBuilder.showMultiChoiceDialog(
                    context = requireContext(), title = title,
                    items = appLabels.toTypedArray(), initialChecked = checkedItems, maxHeightRatio = 0.60f,
                    showSelectAll = true,
                    onConfirm = { selectedIndices ->
                        val selectedPkgs = selectedIndices.map { idx ->
                            val pkg = appPackages[idx]
                            val userStr = sortedApps[idx].user
                            val pkgWithUser = if (!userStr.isNullOrBlank()) "$pkg|$userStr" else pkg
                            if (initialSelected.contains(pkgWithUser)) pkgWithUser else pkg
                        }.toSet()
                        onConfirm(selectedPkgs)
                    }
                )
            }
        }
    }
}
