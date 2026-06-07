package com.github.gezimos.inkos.ui

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.github.gezimos.inkos.ui.compose.inkOsSafeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.helper.EditModeHelper
import com.github.gezimos.inkos.ui.compose.EditModeOverlay
import com.github.gezimos.inkos.ui.compose.NavHelper
import com.github.gezimos.inkos.ui.dialogs.SheetTitle
import com.github.gezimos.inkos.ui.compose.RecentAppItem
import com.github.gezimos.inkos.ui.compose.RecentsLayout
import com.github.gezimos.inkos.ui.compose.gestureHelper
import com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost
class RecentsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    
    private var refreshTriggerCounter = 0
    
    private var permissionCheckKey by mutableStateOf(0)
    
    private var permissionExplanationSheet: ComposeBottomSheetHost? = null
    private fun showPermissionExplanationDialog(
        title: String,
        message: String,
        onContinue: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        requireContext()
        
        permissionExplanationSheet?.dismiss()
        val host = ComposeBottomSheetHost(requireActivity())
        permissionExplanationSheet = host

        host.show {
            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp.scaled(rememberScreenScale()))
            ) {
                SheetTitle(title)
                Text(
                    text = message,
                    style = SettingsTheme.typography.item,
                    color = Theme.colors.text
                )
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    if (onCancel != null) {
                        TextButton(onClick = {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onCancel()
                            host.dismiss()
                        }) {
                            Text(text = getString(android.R.string.cancel), style = SettingsTheme.typography.button, color = Theme.colors.text)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        host.dismiss()
                        onContinue()
                    }) {
                        Text(text = getString(android.R.string.ok), style = SettingsTheme.typography.button, color = Theme.colors.text)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        prefs = viewModel.getPrefs()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        val backgroundColor = getHexForOpacity(ctx)
        val root = android.widget.FrameLayout(ctx).apply {
            setBackgroundColor(backgroundColor)
            clipToPadding = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val composeView = ComposeView(ctx).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val context = LocalContext.current
                val density = LocalDensity.current
                
                var hasPermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
                var hasShownDialog by remember { mutableStateOf(false) }
                
                val currentPermissionCheckKey = permissionCheckKey
                LaunchedEffect(currentPermissionCheckKey) {
                    val newPermissionStatus = hasUsageStatsPermission(context)
                    if (newPermissionStatus != hasPermission) {
                        hasPermission = newPermissionStatus
                        hasShownDialog = false // Reset dialog flag when permission changes
                    }
                }
                
                if (!hasPermission) {
                    LaunchedEffect(hasShownDialog) {
                        if (!hasShownDialog) {
                            hasShownDialog = true
                            showPermissionExplanationDialog(
                                title = "Usage Stats Permission",
                                message = "This app needs usage stats permission to show your recently used apps. You will be taken to Android settings to grant this permission.",
                                onContinue = { requestUsageStatsPermission() }
                            )
                        }
                    }
                }
                
                // Use consolidated State from ViewModel
                val appsDrawer by viewModel.appsDrawerUiState.collectAsState()
                
                val isEditMode by EditModeHelper.isEditModeFlow.collectAsState()

                var showStats by remember { mutableStateOf(appsDrawer.recentsDefaultView == 1) }
                LaunchedEffect(appsDrawer.recentsDefaultView) {
                    showStats = appsDrawer.recentsDefaultView == 1
                }

                // Header DPAD state
                var headerFocused by remember { mutableStateOf(false) }
                var headerSelectedIndex by remember { mutableStateOf(0) } // 0=Title, 1=Cog
                
                var localRefreshTrigger by remember { mutableStateOf(0) }
                val recentApps by remember(localRefreshTrigger, showStats, appsDrawer.recentsUsageFilter, appsDrawer.recentsUsageUnit, appsDrawer.recentsUnitCost, appsDrawer.recentsUnitCoffeePrice, appsDrawer.recentsUnitEmojiChar) {
                    derivedStateOf {
                        getRecentApps(context, appsDrawer.appList, showStats, appsDrawer.recentsUsageFilter)
                    }
                }
                
                LaunchedEffect(refreshTriggerCounter) {
                    localRefreshTrigger++
                }
                
                // Create state for recents
                val state = remember {
                    AppsDrawerState(
                        flag = Constants.AppDrawerFlag.LaunchApp,
                        position = 0,
                        apps = recentApps.map { it.app },
                        searchQuery = TextFieldValue("")
                    )
                }
                
                // Update apps list when recent apps change
                LaunchedEffect(recentApps) {
                    state.apps = recentApps.map { it.app }
                }
                
                LaunchedEffect(showStats) {
                    state.currentPage = 0
                    state.selectedItemIndex = 0
                }
                
                var containerSize by remember { mutableStateOf(IntSize.Zero) }
                var isCalculated by remember { mutableStateOf(false) }
                
                // Robust appsPerPage calculation for recents:
                val textMeasurer = rememberTextMeasurer()
                val appTypefaceNullable = remember(appsDrawer.appsFont, appsDrawer.customFontPath) {
                    try { appsDrawer.appsFont.getFont(context, appsDrawer.customFontPath) } catch (_: Exception) { null }
                }
                val appFontFamily = remember(appTypefaceNullable) {
                    if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
                }
                val drawerScreenScale = rememberScreenScale()
                val gapPx = remember(appsDrawer.appDrawerGap, density) {
                    try { with(density) { appsDrawer.appDrawerGap.dp.roundToPx() } } catch (_: Exception) { 0 }
                }.coerceAtLeast(0)
                val rowTextHeightPx = remember(appsDrawer.appDrawerSize, appFontFamily, textMeasurer, drawerScreenScale) {
                    try {
                        textMeasurer
                            .measure(
                                text = AnnotatedString("Ag"),
                                style = TextStyle(
                                    fontSize = appsDrawer.appDrawerSize.sp.scaled(drawerScreenScale),
                                    fontFamily = appFontFamily
                                )
                            )
                            .size
                            .height
                    } catch (_: Exception) {
                        1
                    }
                }.coerceAtLeast(1)
                val rowHeightPx = remember(rowTextHeightPx, gapPx) {
                    ((rowTextHeightPx * 0.95f) + (gapPx * 2)).toInt().coerceAtLeast(1)
                }
                val headerRows = 1
                val appsPerPageCalculated = remember(containerSize.height, rowHeightPx) {
                    val h = containerSize.height
                    if (h <= 0) 1
                    else {
                        // Subtract title row height from available space
                        val headerHeight = headerRows * rowHeightPx
                        val available = (h - headerHeight - rowHeightPx).coerceAtLeast(rowHeightPx)
                        (available / rowHeightPx).coerceAtLeast(1)
                    }
                }
                LaunchedEffect(appsPerPageCalculated, containerSize.height) {
                    if (containerSize.height > 0) {
                        state.appsPerPage = appsPerPageCalculated
                        isCalculated = true
                    } else {
                        isCalculated = false
                    }
                }
                
                val totalPages by remember {
                    derivedStateOf {
                        if (state.appsPerPage > 0) {
                            ((recentApps.size + state.appsPerPage - 1) / state.appsPerPage).coerceAtLeast(1)
                        } else {
                            1
                        }
                    }
                }

                val setCurrentPageSafe: (Int) -> Unit = { newPage ->
                    val maxPage = (totalPages - 1).coerceAtLeast(0)
                    val clamped = newPage.coerceIn(0, maxPage)
                    if (state.currentPage != clamped) state.currentPage = clamped
                }

                val clampedNow = state.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                if (state.currentPage != clampedNow) {
                    state.currentPage = clampedNow
                }

                LaunchedEffect(recentApps.size, state.appsPerPage) {
                    if (state.currentPage >= totalPages) {
                        setCurrentPageSafe((totalPages - 1).coerceAtLeast(0))
                    }
                }
                
                val displayRecentApps = remember(recentApps, state.currentPage, state.appsPerPage) {
                    val safePage = state.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                    val startIndex = safePage * state.appsPerPage
                    val endIndex = (startIndex + state.appsPerPage).coerceAtMost(recentApps.size)
                    if (startIndex < recentApps.size) {
                        recentApps.subList(startIndex, endIndex)
                    } else {
                        emptyList()
                    }
                }
                
                LaunchedEffect(displayRecentApps.size) {
                    if (state.selectedItemIndex >= displayRecentApps.size) {
                        state.selectedItemIndex = (displayRecentApps.size - 1).coerceAtLeast(0)
                    }
                }
                
                val paddedDisplayRecentApps = remember(displayRecentApps, state.appsPerPage) {
                    val base: List<RecentAppItem?> = displayRecentApps
                    val missing = (state.appsPerPage - displayRecentApps.size).coerceAtLeast(0)
                    if (missing == 0) base else base + List(missing) { null }
                }
                
                fun vibratePage() {
                    VibrationHelper.trigger(VibrationHelper.Effect.PAGE)
                }

                fun vibrateFeedback() {
                    VibrationHelper.trigger(VibrationHelper.Effect.LONG_PRESS)
                }
                
                fun nextPage() {
                    val newPage = state.currentPage + 1
                    if (newPage <= totalPages - 1) {
                        setCurrentPageSafe(newPage)
                        vibratePage()
                    }
                }

                fun previousPage() {
                    val newPage = state.currentPage - 1
                    if (newPage >= 0) {
                        setCurrentPageSafe(newPage)
                        vibratePage()
                    }
                }
                
                // Handle volume key page requests from ViewModel
                LaunchedEffect(viewModel) {
                    try {
                        viewModel.appDrawerPageRequests.collect { delta ->
                            try {
                                if (delta > 0) previousPage() else nextPage()
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                }
                
                val isDark = appsDrawer.appTheme == Constants.Theme.Dark
                val backgroundColor = getHexForOpacity(requireContext())
                val columnFocusRequester = remember { FocusRequester() }
                val keyPressTracker = remember { com.github.gezimos.inkos.ui.compose.KeyPressTracker() }

                LaunchedEffect(Unit) {
                    try {
                        columnFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
                
                SettingsTheme(isDark = isDark) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(backgroundColor))
                            .inkOsSafeDrawingPadding()
                    ) {
                        if (isEditMode) {
                            EditModeOverlay()
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp.scaled(rememberScreenScale()))
                                .onSizeChanged { containerSize = it }
                                .focusRequester(columnFocusRequester)
                                .focusable()
                                .onPreviewKeyEvent { keyEvent ->
                                    // Header DPAD navigation
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        if (headerFocused) {
                                            when (keyEvent.key) {
                                                Key.DirectionDown -> {
                                                    headerFocused = false
                                                    state.isDpadMode = true
                                                    state.selectedItemIndex = 0
                                                    return@onPreviewKeyEvent true
                                                }
                                                Key.DirectionLeft -> {
                                                    if (headerSelectedIndex > 0) {
                                                        headerSelectedIndex--
                                                    }
                                                    return@onPreviewKeyEvent true
                                                }
                                                Key.DirectionRight -> {
                                                    if (headerSelectedIndex < 1) {
                                                        headerSelectedIndex++
                                                    }
                                                    return@onPreviewKeyEvent true
                                                }
                                                Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                                                    when (headerSelectedIndex) {
                                                        0 -> { showStats = !showStats }
                                                        1 -> {
                                                            EditModeHelper.showRecentsSettings(
                                                                requireContext(), this@RecentsFragment, prefs
                                                            ) {}
                                                        }
                                                    }
                                                    return@onPreviewKeyEvent true
                                                }
                                                Key.Back, Key.Escape -> {
                                                    headerFocused = false
                                                    state.isDpadMode = true
                                                    state.selectedItemIndex = 0
                                                    return@onPreviewKeyEvent true
                                                }
                                                Key.DirectionUp -> {
                                                    // Already at top, do nothing
                                                    return@onPreviewKeyEvent true
                                                }
                                            }
                                        } else if (keyEvent.key == Key.DirectionUp &&
                                            state.selectedItemIndex == 0 && state.currentPage == 0 && state.isDpadMode
                                        ) {
                                            // Move from first app item to header
                                            headerFocused = true
                                            state.isDpadMode = false
                                            return@onPreviewKeyEvent true
                                        }
                                    }
                                    if (keyEvent.type == KeyEventType.KeyUp && headerFocused) {
                                        return@onPreviewKeyEvent true
                                    }

                                    try {
                                        val handled = NavHelper.handleAppsKeyEvent(
                                            keyEvent = keyEvent,
                                            keyPressTracker = keyPressTracker,
                                            isDpadModeSetter = { v -> state.isDpadMode = v },
                                            selectedIndexGetter = { state.selectedItemIndex },
                                            selectedIndexSetter = { v ->
                                                state.selectedItemIndex = v
                                                headerFocused = false
                                            },
                                            currentPage = state.currentPage,
                                            totalPages = totalPages,
                                            displayAppsSize = displayRecentApps.size,
                                            onPreviousPage = { previousPage() },
                                            onNextPage = { nextPage() },
                                            onAppClick = { index ->
                                                val selectedApp = displayRecentApps.getOrNull(index)
                                                if (selectedApp != null) {
                                                    if (isEditMode) {
                                                        EditModeHelper.showRecentsSettings(requireContext(), this@RecentsFragment, prefs) {}
                                                    } else {
                                                        handleAppClick(selectedApp)
                                                    }
                                                }
                                            },
                                            onAppLongClick = { index ->
                                                val selectedApp = displayRecentApps.getOrNull(index)
                                                if (selectedApp != null) {
                                                    if (isEditMode) {
                                                        EditModeHelper.showRecentsSettings(requireContext(), this@RecentsFragment, prefs) {}
                                                    } else {
                                                        vibrateFeedback()
                                                        handleInfoApp(selectedApp.app)
                                                    }
                                                }
                                            },
                                            onNavigateBack = { findNavController().popBackStack() },
                                            showAzFilter = false,
                                            azFilterFocused = false,
                                            onAzFilterFocusChange = {},
                                            azFilterSelectedIndex = 0,
                                            onAzFilterIndexChange = {},
                                            azFilterSize = 0,
                                            onAzFilterActivate = {},
                                            showSearch = false,
                                            onSearchFocus = {}
                                        )
                                        if (handled) return@onPreviewKeyEvent true
                                    } catch (_: Exception) {}

                                    false
                                }
                                .gestureHelper(
                                    shortSwipeRatio = prefs.shortSwipeThresholdRatio,
                                    longSwipeRatio = prefs.longSwipeThresholdRatio,
                                    onVerticalPageMove = { delta ->
                                        try {
                                            if (totalPages <= 1) return@gestureHelper
                                            if (delta > 0) {
                                                val newPage = (state.currentPage + delta).coerceAtMost(totalPages - 1)
                                                if (newPage != state.currentPage) {
                                                    setCurrentPageSafe(newPage)
                                                    vibratePage()
                                                }
                                            } else if (delta < 0) {
                                                val steps = -delta
                                                val newPage = (state.currentPage - steps).coerceAtLeast(0)
                                                if (newPage != state.currentPage) {
                                                    setCurrentPageSafe(newPage)
                                                    vibratePage()
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    onLongSwipeDown = {
                                        try {
                                            if (state.currentPage == 0) {
                                                vibratePage()
                                                findNavController().popBackStack()
                                            }
                                        } catch (_: Exception) {}
                                    }
                                )
                        ) {
                            RecentsLayout(
                                state = state,
                                uiState = appsDrawer,
                                recentApps = displayRecentApps,
                                paddedRecentApps = paddedDisplayRecentApps,
                                isCalculated = isCalculated,
                                showStats = showStats,
                                hasPermission = hasPermission,
                                onStatsToggle = {
                                    if (isEditMode) {
                                        EditModeHelper.showRecentsSettings(requireContext(), this@RecentsFragment, prefs) {}
                                    } else {
                                        showStats = !showStats
                                    }
                                },
                                onSettingsClick = {
                                    EditModeHelper.showRecentsSettings(requireContext(), this@RecentsFragment, prefs) {}
                                },
                                headerFocused = headerFocused,
                                headerSelectedIndex = headerSelectedIndex,
                                onAppClick = { recentApp ->
                                    if (isEditMode) {
                                        EditModeHelper.showRecentsSettings(requireContext(), this@RecentsFragment, prefs) {}
                                    } else {
                                        state.selectedItemIndex = displayRecentApps.indexOf(recentApp)
                                        state.isDpadMode = false
                                        handleAppClick(recentApp)
                                    }
                                },
                                onAppLongClick = { recentApp ->
                                    if (isEditMode) {
                                        EditModeHelper.showRecentsSettings(requireContext(), this@RecentsFragment, prefs) {}
                                    } else {
                                        state.selectedItemIndex = displayRecentApps.indexOf(recentApp)
                                        state.isDpadMode = false
                                        vibrateFeedback()
                                        handleInfoApp(recentApp.app)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        root.addView(composeView)
        return root
    }

    override fun onDestroyView() {
        permissionExplanationSheet?.dismiss()
        permissionExplanationSheet = null
        super.onDestroyView()
    }

    private fun handleAppClick(recentApp: RecentAppItem) {
        viewModel.launchApp(recentApp.app)
    }

    private fun handleInfoApp(app: AppListItem) {
        openAppInfo(requireContext(), app.user, app.activityPackage)
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private fun requestUsageStatsPermission() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        } catch (_: Exception) {
            showShortToast("Unable to open settings")
        }
    }

    private fun getRecentApps(context: Context, installedApps: List<AppListItem>, showStats: Boolean = false, usageFilter: Int = 1): List<RecentAppItem> {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = when (usageFilter) {
                0 -> { // Today — from midnight
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                1 -> endTime - (7L * 24 * 60 * 60 * 1000)   // This Week (7 days)
                2 -> endTime - (30L * 24 * 60 * 60 * 1000)  // This Month (30 days)
                else -> endTime - (365L * 24 * 60 * 60 * 1000) // All Time (~1 year)
            }

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            // Create a map of package name to usage stats
            val usageMap = usageStatsList
                .groupBy { it.packageName }
                .mapValues { (_, stats) ->
                    // Sum up usage across all intervals
                    stats.fold(Pair(0L, 0L)) { acc, stat ->
                        Pair(
                            maxOf(acc.first, stat.lastTimeUsed),
                            acc.second + stat.totalTimeInForeground
                        )
                    }
                }
                .filterValues { (lastUsed, totalTime) -> lastUsed > 0 && totalTime > 0 }

            installedApps
                .filterNot { app ->
                    // Exclude inkOS and inkOS debug from recents
                    app.activityPackage == "app.inkos" ||
                    app.activityPackage == "app.inkos.debug"
                }
                .mapNotNull { app ->
                    usageMap[app.activityPackage]?.let { (lastUsed, totalTime) ->
                        RecentAppItem(
                            app = app,
                            lastUsedTime = lastUsed,
                            totalUsageTime = totalTime
                        )
                    }
                }
                .sortedByDescending { if (showStats) it.totalUsageTime else it.lastUsedTime }
                .take(20) // Top 20 most recently used apps
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshTriggerCounter++
        permissionCheckKey++
        
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        act.pageNavigationHandler = object : com.github.gezimos.inkos.MainActivity.PageNavigationHandler {
            override val handleDpadAsPage: Boolean = false

            override fun pageUp() {
                try {
                    viewModel.requestAppDrawerPageUp()
                } catch (_: Exception) {}
            }

            override fun pageDown() {
                try {
                    viewModel.requestAppDrawerPageDown()
                } catch (_: Exception) {}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        if (act.pageNavigationHandler != null) act.pageNavigationHandler = null
    }
}
