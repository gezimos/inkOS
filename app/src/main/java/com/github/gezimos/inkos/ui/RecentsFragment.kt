package com.github.gezimos.inkos.ui

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.TextFieldValue
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
import com.github.gezimos.inkos.style.resolveThemeColors
import com.github.gezimos.inkos.ui.compose.NavHelper
import com.github.gezimos.inkos.ui.compose.RecentsLayout
import com.github.gezimos.inkos.ui.compose.RecentAppItem
import com.github.gezimos.inkos.ui.compose.gestureHelper
import com.github.gezimos.inkos.ui.dialogs.LockedBottomSheetDialog

/**
 * Fragment that shows recently used apps based on usage stats
 */
class RecentsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    
    // Add a trigger to refresh recent apps when fragment resumes
    // Using a simple counter that gets incremented in onResume
    private var refreshTriggerCounter = 0
    
    // Key to trigger permission re-check when fragment resumes
    private var permissionCheckKey by mutableStateOf(0)
    
    private var permissionExplanationDialog: LockedBottomSheetDialog? = null
    
    /**
     * Shows an informational bottom sheet dialog explaining why usage stats permission is needed before requesting it.
     */
    private fun showPermissionExplanationDialog(
        title: String,
        message: String,
        onContinue: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val context = requireContext()
        
        // Dismiss any existing permission dialog
        permissionExplanationDialog?.dismiss()
        
        // Grab prefs and resolve theme colors
        Prefs(context)
        val (dlgTextColor, dlgBackground) = resolveThemeColors(context)
        
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            
            // Title
            val titleView = TextView(context).apply {
                text = title.uppercase()
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 16f
                try { setTextColor(dlgTextColor) } catch (_: Exception) {}
            }
            addView(titleView)
            
            // Message
            val messageView = TextView(context).apply {
                text = message
                gravity = Gravity.START
                textAlignment = View.TEXT_ALIGNMENT_TEXT_START
                textSize = 14f
                val mPad = (8 * context.resources.displayMetrics.density).toInt()
                setPadding(mPad, mPad, mPad, mPad)
                try { setTextColor(dlgTextColor) } catch (_: Exception) {}
            }
            addView(messageView)
            
            // Buttons row
            val buttonsRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                val btnMargin = (8 * context.resources.displayMetrics.density).toInt()
                val btnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(btnMargin, btnMargin, btnMargin, 0)
                }
                
                // Cancel button (if onCancel is provided)
                if (onCancel != null) {
                    val cancelBtn = Button(context).apply {
                        text = context.getString(android.R.string.cancel)
                        val btnPadding = (10 * context.resources.displayMetrics.density).toInt()
                        setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                        minWidth = 0
                        minimumWidth = 0
                        minHeight = 0
                        minimumHeight = 0
                        setOnClickListener {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                            onCancel()
                            permissionExplanationDialog?.dismiss()
                        }
                    }
                    // Style button
                    try {
                        val density = context.resources.displayMetrics.density
                        val radius = (6 * density)
                        val strokeWidth = (3f * density).toInt()
                        val bgDrawable = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = radius
                            setColor(dlgBackground)
                            setStroke(strokeWidth, dlgTextColor)
                        }
                        cancelBtn.background = bgDrawable
                        cancelBtn.setTextColor(dlgTextColor)
                    } catch (_: Exception) {}
                    cancelBtn.layoutParams = btnParams
                    addView(cancelBtn)
                }
                
                // Continue/OK button
                val continueBtn = Button(context).apply {
                    text = context.getString(android.R.string.ok)
                    val btnPadding = (10 * context.resources.displayMetrics.density).toInt()
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                    setOnClickListener {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        permissionExplanationDialog?.dismiss()
                        onContinue()
                    }
                }
                // Style button
                try {
                    val density = context.resources.displayMetrics.density
                    val radius = (6 * density)
                    (3f * density).toInt()
                    val bgDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(dlgTextColor)
                    }
                    continueBtn.background = bgDrawable
                    continueBtn.setTextColor(dlgBackground)
                } catch (_: Exception) {}
                continueBtn.layoutParams = btnParams
                addView(continueBtn)
            }
            addView(buttonsRow)
        }
        
        // Apply fonts
        try {
            val fontFamily = prefs.getFontForContext("settings")
            val customFontPath = prefs.getCustomFontPathForContext("settings")
            val typeface = fontFamily.getFont(context, customFontPath)
            val textSize = prefs.settingsSize.toFloat()
            
            fun applyFont(view: View) {
                if (view is TextView) {
                    typeface?.let { view.typeface = it }
                    view.textSize = textSize
                } else if (view is ViewGroup) {
                    for (i in 0 until view.childCount) {
                        applyFont(view.getChildAt(i))
                    }
                }
            }
            applyFont(content)
        } catch (_: Exception) {}
        
        // Create and show bottom sheet dialog
        val dialog = LockedBottomSheetDialog(context)
        dialog.setContentView(content)
        try {
            content.setBackgroundColor(dlgBackground)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        
        dialog.setLocked(true)
        dialog.show()
        permissionExplanationDialog = dialog
        
        // WindowInsets padding for navigation bar
        try {
            val baseBottom = content.paddingBottom
            dialog.window?.decorView?.let { decor ->
                ViewCompat.setOnApplyWindowInsetsListener(decor) { _, insets ->
                    try {
                        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                        val extra = (8 * context.resources.displayMetrics.density).toInt()
                        content.setPadding(content.paddingLeft, content.paddingTop, content.paddingRight, baseBottom + navBarInset + extra)
                    } catch (_: Exception) {}
                    insets
                }
                ViewCompat.requestApplyInsets(decor)
            }
        } catch (_: Exception) {}
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
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val context = LocalContext.current
                val density = LocalDensity.current
                
                // Check for usage stats permission - make it reactive so it updates when permission is granted
                var hasPermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
                var hasShownDialog by remember { mutableStateOf(false) }
                
                // Re-check permission when fragment resumes (triggered by permissionCheckKey from onResume)
                // Read the class-level permissionCheckKey value - since it's mutableStateOf, reading it will trigger recomposition
                val currentPermissionCheckKey = permissionCheckKey
                LaunchedEffect(currentPermissionCheckKey) {
                    val newPermissionStatus = hasUsageStatsPermission(context)
                    if (newPermissionStatus != hasPermission) {
                        hasPermission = newPermissionStatus
                        hasShownDialog = false // Reset dialog flag when permission changes
                    }
                }
                
                if (!hasPermission) {
                    // Show permission required screen
                    SettingsTheme(isDark = prefs.appTheme == Constants.Theme.Dark) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(getHexForOpacity(context))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Usage access permission required",
                                color = com.github.gezimos.inkos.style.Theme.colors.text,
                                fontSize = 20.sp
                            )
                        }
                    }
                    
                    // Show explanation dialog before requesting permission (only once per permission state)
                    LaunchedEffect(hasShownDialog) {
                        if (!hasShownDialog) {
                            hasShownDialog = true
                            showPermissionExplanationDialog(
                                title = "Usage Stats Permission",
                                message = "This app needs usage stats permission to show your recently used apps. You will be taken to Android settings to grant this permission.",
                                onContinue = {
                                    requestUsageStatsPermission()
                                }
                            )
                        }
                    }
                    return@setContent
                }
                
                // Use consolidated State from ViewModel
                val appsDrawer by viewModel.appsDrawerUiState.collectAsState()
                
                // Stats mode state
                var showStats by remember { mutableStateOf(false) }
                
                // Get recent apps from usage stats - refresh when refreshTriggerCounter changes (onResume)
                var localRefreshTrigger by remember { mutableStateOf(0) }
                val recentApps by remember(localRefreshTrigger, showStats) {
                    derivedStateOf {
                        getRecentApps(context, appsDrawer.appList, showStats)
                    }
                }
                
                // Sync with class-level refreshTriggerCounter to refresh when fragment resumes
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
                
                // Reset to first page when switching between "most recent" and "most used"
                LaunchedEffect(showStats) {
                    state.currentPage = 0
                    state.selectedItemIndex = 0
                }
                
                var containerSize by remember { mutableStateOf(IntSize.Zero) }
                var isCalculated by remember { mutableStateOf(false) }
                
                // Robust appsPerPage calculation for recents:
                // - Measure actual text height with the active font + size
                // - Include vertical padding used by each row (gap)
                // - Subtract the fixed header row ("RECENTS"/"MOST USED") so bottom items never clip
                val textMeasurer = rememberTextMeasurer()
                val appTypefaceNullable = remember(appsDrawer.appsFont, appsDrawer.customFontPath) {
                    try { appsDrawer.appsFont.getFont(context, appsDrawer.customFontPath) } catch (_: Exception) { null }
                }
                val appFontFamily = remember(appTypefaceNullable) {
                    if (appTypefaceNullable != null) FontFamily(appTypefaceNullable) else FontFamily.Default
                }
                val gapPx = remember(appsDrawer.appDrawerGap, density) {
                    try { with(density) { appsDrawer.appDrawerGap.dp.roundToPx() } } catch (_: Exception) { 0 }
                }.coerceAtLeast(0)
                val rowTextHeightPx = remember(appsDrawer.appDrawerSize, appFontFamily, textMeasurer) {
                    try {
                        textMeasurer
                            .measure(
                                text = AnnotatedString("Ag"),
                                style = TextStyle(
                                    fontSize = appsDrawer.appDrawerSize.sp,
                                    fontFamily = appFontFamily
                                )
                            )
                            .size
                            .height
                    } catch (_: Exception) {
                        1
                    }
                }.coerceAtLeast(1)
                // Row height = text height + top padding + bottom padding
                // Use 0.95 multiplier to account for text measurement including line-height that may not be fully used
                // This allows fitting more items, and SpaceBetween will distribute any leftover space evenly
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
                        // Subtract one extra row as safety margin to prevent bottom clipping
                        // This ensures the last item always fits, and SpaceBetween will distribute any leftover space
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
                
                // Pad display apps with null placeholders to fill the page
                // This ensures SpaceBetween always has the same number of items, creating consistent spacing
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
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .onSizeChanged { containerSize = it }
                                .focusRequester(columnFocusRequester)
                                .focusable()
                                .onPreviewKeyEvent { keyEvent ->
                                    try {
                                        val handled = NavHelper.handleAppsKeyEvent(
                                            keyEvent = keyEvent,
                                            keyPressTracker = keyPressTracker,
                                            isDpadModeSetter = { v -> state.isDpadMode = v },
                                            selectedIndexGetter = { state.selectedItemIndex },
                                            selectedIndexSetter = { v -> state.selectedItemIndex = v },
                                            currentPage = state.currentPage,
                                            totalPages = totalPages,
                                            displayAppsSize = displayRecentApps.size,
                                            onPreviousPage = { previousPage() },
                                            onNextPage = { nextPage() },
                                            onAppClick = { index ->
                                                val selectedApp = displayRecentApps.getOrNull(index)
                                                if (selectedApp != null) {
                                                    handleAppClick(selectedApp)
                                                }
                                            },
                                            onAppLongClick = { index ->
                                                val selectedApp = displayRecentApps.getOrNull(index)
                                                if (selectedApp != null) {
                                                    vibrateFeedback()
                                                    handleInfoApp(selectedApp.app)
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
                                                if (state.currentPage == 0 && steps > 0) {
                                                    vibratePage()
                                                    findNavController().popBackStack()
                                                } else {
                                                    val newPage = (state.currentPage - steps).coerceAtLeast(0)
                                                    if (newPage != state.currentPage) {
                                                        setCurrentPageSafe(newPage)
                                                        vibratePage()
                                                    }
                                                }
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
                                totalPages = totalPages,
                                isCalculated = isCalculated,
                                showStats = showStats,
                                onStatsToggle = { showStats = !showStats },
                                onAppClick = { recentApp ->
                                    state.selectedItemIndex = displayRecentApps.indexOf(recentApp)
                                    state.isDpadMode = false
                                    handleAppClick(recentApp)
                                },
                                onAppLongClick = { recentApp ->
                                    state.selectedItemIndex = displayRecentApps.indexOf(recentApp)
                                    state.isDpadMode = false
                                    vibrateFeedback()
                                    handleInfoApp(recentApp.app)
                                }
                            )
                        }
                    }
                }
            }
        }
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

    private fun getRecentApps(context: Context, installedApps: List<AppListItem>, showStats: Boolean = false): List<RecentAppItem> {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (7 * 24 * 60 * 60 * 1000L) // Last 7 days
            
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
            
            // Match with installed apps and create RecentAppItem list
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
        // Refresh recent apps and check permission when fragment resumes
        // This triggers recomposition and permission re-check when returning from settings
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
