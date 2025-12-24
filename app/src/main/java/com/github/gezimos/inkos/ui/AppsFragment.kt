package com.github.gezimos.inkos.ui

import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.isSystemApp
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.ui.compose.AZSidebar
import com.github.gezimos.inkos.ui.compose.AppsDrawerLayout
import com.github.gezimos.inkos.ui.compose.NavHelper
import com.github.gezimos.inkos.ui.compose.SearchHelper
import com.github.gezimos.inkos.ui.compose.gestureHelper
import kotlinx.coroutines.delay


/**
 * Fragment that handles all logic for the Apps Drawer
 */
class AppsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var flag: Constants.AppDrawerFlag
    private var appPosition: Int = 0
    private var showSearchArg: Boolean = false

    private var pendingUninstallPackage: String? = null
    private val uninstallLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
            pendingUninstallPackage?.let { pkg ->
                viewModel.refreshAppListAfterUninstall(includeHiddenApps = false)
                pendingUninstallPackage = null
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        prefs = viewModel.getPrefs()

        val flagString = arguments?.getString("flag", Constants.AppDrawerFlag.LaunchApp.toString())
            ?: Constants.AppDrawerFlag.LaunchApp.toString()
        flag = Constants.AppDrawerFlag.valueOf(flagString)
        appPosition = arguments?.getInt("n", 0) ?: 0
        showSearchArg = arguments?.getBoolean("showSearch", false) ?: false

        val includeHidden = flag == Constants.AppDrawerFlag.SetHomeApp || flag == Constants.AppDrawerFlag.HiddenApps
        
        // Only reload if we need hidden apps (SetHomeApp or HiddenApps flag)
        // For normal LaunchApp flag, the ViewModel already preloads the list in init
        // This avoids expensive reload on every fragment open, making it instant like SimpleTrayFragment
        if (includeHidden || flag == Constants.AppDrawerFlag.HiddenApps) {
            // Need to load with hidden apps, so reload
            viewModel.getAppList(includeHiddenApps = includeHidden, flag = flag)
        }
        // Otherwise, use the cached list from ViewModel preload - no reload needed!
        // The ViewModel.init already calls getAppList(includeHiddenApps = false) for instant opening
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
                remember {
                    try { context.getSystemService(Vibrator::class.java) } catch (_: Exception) { null }
                }
                
                // Use consolidated State from ViewModel instead of individual observeAsState calls
                val appsDrawer by viewModel.appsDrawerUiState.collectAsState()
                
                // Safety check: if list is empty and we're not loading hidden apps, trigger a load
                // This handles edge cases where ViewModel init hasn't completed yet
                LaunchedEffect(Unit) {
                    val includeHidden = flag == Constants.AppDrawerFlag.SetHomeApp || flag == Constants.AppDrawerFlag.HiddenApps
                    if (!includeHidden && appsDrawer.appList.isEmpty()) {
                        viewModel.getAppList(includeHiddenApps = false, flag = flag)
                    }
                }
                
                val baseAppList = when (flag) {
                    Constants.AppDrawerFlag.HiddenApps -> appsDrawer.hiddenApps
                    else -> appsDrawer.appList
                }
                val appsForDrawer = remember(baseAppList, flag) {
                    baseAppList
                }
                
                val appList = remember(baseAppList, flag) {
                    if (flag == Constants.AppDrawerFlag.HiddenApps) {
                        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
                        val hiddenSystemShortcuts = appsRepo.getFilteredSystemShortcuts(
                            includeHidden = false,
                            onlyHidden = true
                        )
                        baseAppList.toMutableList().apply { addAll(hiddenSystemShortcuts) }
                    } else {
                        baseAppList
                    }
                }
                
                // Create state once and update apps when list changes
                // Deprecated: `initialSearch` nav-arg removed to avoid duplicate input.
                val state = remember {
                    AppsDrawerState(
                        flag = flag,
                        position = appPosition,
                        apps = appsForDrawer,
                        searchQuery = androidx.compose.ui.text.input.TextFieldValue("")
                    )
                }
                
                // Update search enabled state based on preference
                // Disable search when in SetHomeApp flag mode
                LaunchedEffect(appsDrawer.appDrawerSearchEnabled, flag) {
                    state.searchEnabled = appsDrawer.appDrawerSearchEnabled && flag != Constants.AppDrawerFlag.SetHomeApp
                }
                
                // Update MainActivity's suppressKeyForwarding based on rename overlay or context menu state
                LaunchedEffect(state.showRenameOverlay, state.showContextMenu) {
                    val act = context as? com.github.gezimos.inkos.MainActivity
                    act?.suppressKeyForwarding = state.showRenameOverlay || state.showContextMenu
                }
                
                // Update apps list when it changes from ViewModel
                LaunchedEffect(appsForDrawer) {
                    state.apps = appsForDrawer
                }
                
                // Collect typed-character and backspace events from MainActivity/ViewModel
                LaunchedEffect(viewModel) {
                    try {
                        viewModel.typedCharEvents.collect { ch ->
                            if (state.searchEnabled) {
                                state.searchQuery = SearchHelper.appendCharToSearch(state.searchQuery, ch)
                            }
                        }
                    } catch (_: Exception) {}
                }

                LaunchedEffect(viewModel) {
                    try {
                        viewModel.backspaceEvents.collect {
                            if (state.searchEnabled && state.searchQuery.text.isNotEmpty()) {
                                state.searchQuery = SearchHelper.backspaceSearch(state.searchQuery)
                            }
                        }
                    } catch (_: Exception) {}
                }
                
                var containerSize by remember { mutableStateOf(IntSize.Zero) }
                
                // Check cache first - use it if available for instant rendering
                // Check cache directly (not in remember) so it's fresh on every composition
                val cachedAppsPerPage = viewModel.getCachedAppsPerPage()
                var isCalculated by remember { 
                    mutableStateOf(cachedAppsPerPage != null) 
                }
                
                // Initialize state with cached value immediately if available
                // This runs during composition, so it's synchronous
                // If we have cache, use it immediately and mark as calculated - don't wait for containerSize
                if (cachedAppsPerPage != null) {
                    if (state.appsPerPage != cachedAppsPerPage) {
                        state.appsPerPage = cachedAppsPerPage
                    }
                    // Force isCalculated to true when cache exists - don't let calculation override it
                    if (!isCalculated) {
                        isCalculated = true
                    }
                }
                
                // Only do expensive calculations if we don't have a cache
                // This prevents janky swiping during calculation
                if (cachedAppsPerPage == null) {
                    // Robust appsPerPage calculation:
                    // - Measure actual text height with the active font + size
                    // - Include the vertical padding used by each row (gap)
                    // - Subtract fixed header rows (search) so bottom items never clip
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
                    // Row height = text height + top padding + bottom padding (each AppItem has vertical padding)
                    // Use 0.95 multiplier to account for text measurement including line-height that may not be fully used
                    // This allows fitting more items, and SpaceBetween will distribute any leftover space evenly
                    val rowHeightPx = remember(rowTextHeightPx, gapPx) {
                        ((rowTextHeightPx * 0.95f) + (gapPx * 2)).toInt().coerceAtLeast(1)
                    }
                    // Search field and title also use the same gap for vertical padding, so they're roughly the same height
                    val headerRows = remember(appsDrawer.appDrawerSearchEnabled, state.searchEnabled, state.isHiddenAppsMode, flag) {
                        var rows = 0
                        // Account for title row in SetHomeApp and HiddenApps modes
                        if (flag == Constants.AppDrawerFlag.SetHomeApp || flag == Constants.AppDrawerFlag.HiddenApps) {
                            rows++
                        }
                        // Account for search field if enabled and not hidden apps mode
                        if (!state.isHiddenAppsMode && appsDrawer.appDrawerSearchEnabled && state.searchEnabled) {
                            rows++
                        }
                        rows
                    }
                    val appsPerPageCalculated = remember(containerSize.height, rowHeightPx, headerRows) {
                        val h = containerSize.height
                        if (h <= 0) 1
                        else {
                            // Subtract header row height (search field) from available space
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
                            // Cache the result for next time
                            viewModel.cacheAppsPerPage(appsPerPageCalculated)
                            isCalculated = true
                        } else {
                            // Container not measured yet - keep isCalculated false
                            isCalculated = false
                        }
                    }
                }
                
                // Cache sorted apps to avoid multiple sorts per recomposition (performance optimization)
                val sortedApps by remember {
                    derivedStateOf { state.apps.sorted() }
                }
                
                // Use derivedStateOf for calculated values to optimize recomposition
                val filteredApps by remember {
                    derivedStateOf {
                        if (state.isHiddenAppsMode) {
                            sortedApps
                        } else if (state.azFilterLetter != null) {
                            val prefix = state.azFilterLetter!!.toString()
                            sortedApps.filter { app ->
                                try {
                                    SearchHelper.startsWith(app.label, prefix)
                                } catch (_: Exception) { false }
                            }
                        } else if (state.searchQuery.text.isBlank()) {
                            sortedApps
                        } else {
                            sortedApps
                                .mapNotNull { app ->
                                    val score = SearchHelper.scoreApp(app, state.searchQuery.text, 100)
                                    if (score > 0) app to score else null
                                }
                                .sortedByDescending { (_, score) -> score }
                                .map { (app, _) -> app }
                        }
                    }
                }
                
                // derivedStateOf for total pages to prevent redundant calculations
                val totalPages by remember {
                    derivedStateOf {
                        if (state.appsPerPage > 0) {
                            ((filteredApps.size + state.appsPerPage - 1) / state.appsPerPage).coerceAtLeast(1)
                        } else {
                            1
                        }
                    }
                }

                // Safe setter for currentPage that always clamps into valid range.
                val setCurrentPageSafe: (Int) -> Unit = { newPage ->
                    val maxPage = (totalPages - 1).coerceAtLeast(0)
                    val clamped = newPage.coerceIn(0, maxPage)
                    if (state.currentPage != clamped) state.currentPage = clamped
                }

                // Reset page when search changes and clear A→Z filter when user starts typing
                LaunchedEffect(state.searchQuery.text) {
                    setCurrentPageSafe(0)
                    state.selectedItemIndex = 0
                    if (state.searchQuery.text.isNotBlank()) {
                        // User started a text search — exit A→Z filtering so search works normally
                        state.azFilterLetter = null
                    }
                }
                

                val clampedNow = state.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                if (state.currentPage != clampedNow) {
                    state.currentPage = clampedNow
                }

                // Adjust current page if needed whenever the filtered list or page size changes.
                // This clamps `state.currentPage` immediately so rapid swipes can't leave it out-of-range
                LaunchedEffect(filteredApps.size, state.appsPerPage) {
                    if (state.currentPage >= totalPages) {
                        setCurrentPageSafe((totalPages - 1).coerceAtLeast(0))
                    }
                }
                
                // Get display apps for current page (use a clamped page to avoid transient empty pages)
                // Compute pages locally from `filteredApps` and `state.appsPerPage` to avoid depending
                // on `totalPages` and causing stale memoization when navigated with an initial search.
                val displayApps = remember(filteredApps, state.currentPage, state.appsPerPage) {
                    val safePage = state.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                    val startIndex = safePage * state.appsPerPage
                    val endIndex = (startIndex + state.appsPerPage).coerceAtMost(filteredApps.size)
                    if (startIndex < filteredApps.size) {
                        filteredApps.subList(startIndex, endIndex)
                    } else {
                        emptyList()
                    }
                }
                
                // Ensure selection is within bounds
                LaunchedEffect(displayApps.size) {
                    if (state.selectedItemIndex >= displayApps.size) {
                        state.selectedItemIndex = (displayApps.size - 1).coerceAtLeast(0)
                    }
                }
                
                // Pad display apps with null placeholders to fill the page
                // This ensures SpaceBetween always has the same number of items, creating consistent spacing
                val paddedDisplayApps = remember(displayApps, state.appsPerPage) {
                    val missing = (state.appsPerPage - displayApps.size).coerceAtLeast(0)
                    if (missing == 0) displayApps else displayApps + List(missing) { null }
                }
                
                fun vibratePage() {
                    com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.PAGE)
                }

                fun vibrateFeedback() {
                    // Use a stronger haptic for long-press/context-menu feedback
                    com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.LONG_PRESS)
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
                
                // Auto-launch logic
                LaunchedEffect(state.searchQuery.text, displayApps.size) {
                    if (appsDrawer.appDrawerAutoLaunch && state.searchQuery.text.isNotBlank() && filteredApps.size == 1) {
                        delay(300)
                        handleAppClick(filteredApps[0])
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
                val searchFocusRequester = remember { FocusRequester() }
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                val keyPressTracker = remember { com.github.gezimos.inkos.ui.compose.KeyPressTracker() }

                // Auto-show keyboard when preference enabled and search is present/enabled
                LaunchedEffect(appsDrawer.appDrawerAutoShowKeyboard, appsDrawer.appDrawerSearchEnabled, state.searchEnabled) {
                    try {
                        if (appsDrawer.appDrawerAutoShowKeyboard && appsDrawer.appDrawerSearchEnabled && state.searchEnabled) {
                            // Small delay to ensure the search field is composed
                            delay(120)
                            try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                            try {
                                // Prefer Compose keyboard controller when available
                                try {
                                    keyboardController?.show()
                                } catch (_: Exception) {}

                                // No additional fallback: prefer Compose keyboard controller only
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                }
                
                // Reset key tracker when context menu or rename overlay state changes to prevent stuck state
                LaunchedEffect(state.showRenameOverlay, state.showContextMenu) {
                    keyPressTracker.reset()
                }
                
                // Handle search focus changes - clear app selection and A-Z filter focus when search is focused
                LaunchedEffect(state.searchFocused) {
                    if (state.searchFocused) {
                        state.isDpadMode = false
                        state.selectedItemIndex = -1
                        state.azFilterFocused = false
                    }
                }

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
                        // Visual size used for the A–Z sidebar width (owned by AppsUI)
                        val pageDotSize = com.github.gezimos.inkos.ui.compose.azSidebarWidth

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                                .onSizeChanged { containerSize = it }
                                .focusRequester(columnFocusRequester)
                                .focusable()
                                .onPreviewKeyEvent { keyEvent ->
                                    // Skip all key handling when rename overlay or context menu is visible
                                    // to allow their internal key handling to work
                                    if (state.showRenameOverlay || state.showContextMenu) {
                                        return@onPreviewKeyEvent false
                                    }
                                    
                                    // Fragment-level key handling (DPAD/page/search)
                                    try {
                                        val handled = NavHelper.handleAppsKeyEvent(
                                            keyEvent = keyEvent,
                                            keyPressTracker = keyPressTracker,
                                            isDpadModeSetter = { v -> state.isDpadMode = v },
                                            selectedIndexGetter = { state.selectedItemIndex },
                                            selectedIndexSetter = { v -> state.selectedItemIndex = v },
                                            currentPage = state.currentPage,
                                            totalPages = totalPages,
                                            displayAppsSize = displayApps.size,
                                            onPreviousPage = { previousPage() },
                                            onNextPage = { nextPage() },
                                            onAppClick = { index ->
                                                val selectedApp = displayApps.getOrNull(index)
                                                if (selectedApp != null) {
                                                    handleAppClick(selectedApp)
                                                }
                                            },
                                            onAppLongClick = { index ->
                                                val selectedApp = displayApps.getOrNull(index)
                                                if (selectedApp != null) {
                                                    vibrateFeedback()
                                                    when (flag) {
                                                        Constants.AppDrawerFlag.SetHomeApp,
                                                        Constants.AppDrawerFlag.SetSwipeLeft,
                                                        Constants.AppDrawerFlag.SetSwipeRight,
                                                        Constants.AppDrawerFlag.SetSwipeUp,
                                                        Constants.AppDrawerFlag.SetSwipeDown,
                                                        Constants.AppDrawerFlag.SetClickClock,
                                                        Constants.AppDrawerFlag.SetClickDate,
                                                        Constants.AppDrawerFlag.SetQuoteWidget,
                                                        Constants.AppDrawerFlag.SetDoubleTap -> {
                                                            viewModel.selectAppForFlag(selectedApp, flag, appPosition)
                                                            findNavController().popBackStack()
                                                        }
                                                        else -> {
                                                            state.contextMenuApp = selectedApp
                                                            state.showContextMenu = true
                                                        }
                                                    }
                                                }
                                            },
                                            onNavigateBack = { findNavController().popBackStack() },
                                            // New parameters for A-Z filter and search
                                            showAzFilter = appsDrawer.appDrawerAzFilter && !state.showContextMenu && !state.showRenameOverlay && !state.isHiddenAppsMode,
                                            azFilterFocused = state.azFilterFocused,
                                            onAzFilterFocusChange = { focused ->
                                                state.azFilterFocused = focused
                                                if (focused) {
                                                    // Reset to first item when entering filter
                                                    state.azFilterSelectedIndex = 0
                                                }
                                            },
                                            azFilterSelectedIndex = state.azFilterSelectedIndex,
                                            onAzFilterIndexChange = { index ->
                                                state.azFilterSelectedIndex = index
                                            },
                                            azFilterSize = appsDrawer.azLetters.size,
                                            onAzFilterActivate = {
                                                // Apply the currently selected filter letter and move back to app list
                                                val letter = appsDrawer.azLetters.getOrNull(state.azFilterSelectedIndex)?.toString()
                                                if (letter != null) {
                                                    try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                                                    val newFilter = if (letter == "★") null else letter.firstOrNull()?.uppercaseChar()
                                                    state.azFilterLetter = newFilter
                                                    setCurrentPageSafe(0)
                                                    // Keep focus in A-Z filter after activation until user explicitly moves
                                                }
                                            },
                                            showSearch = appsDrawer.appDrawerSearchEnabled && state.searchEnabled && !state.isHiddenAppsMode,
                                            onSearchFocus = {
                                                state.searchFocused = true
                                                // Request keyboard focus on search field
                                                searchFocusRequester.requestFocus()
                                            }
                                        )
                                        if (handled) return@onPreviewKeyEvent true
                                    } catch (_: Exception) {}
                                    
                                    // Handle DOWN key when search field is focused to move back to app list
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown && state.searchFocused) {
                                        state.searchFocused = false
                                        state.isDpadMode = true
                                        state.selectedItemIndex = 0
                                        columnFocusRequester.requestFocus()
                                        return@onPreviewKeyEvent true
                                    }

                                    // Fallback: handle Enter (open first result) and search input/backspace
                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        // If search is active, Enter or DPAD center should open the first filtered app
                                        // But we need to track press duration, so only handle on KeyUp
                                        when (keyEvent.key) {
                                            Key.Backspace, Key.Delete -> {
                                                if (state.searchEnabled && state.searchQuery.text.isNotEmpty()) {
                                                    try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                                                    val newText = state.searchQuery.text.dropLast(1).lowercase()
                                                    state.searchQuery = state.searchQuery.copy(
                                                        text = newText,
                                                        selection = androidx.compose.ui.text.TextRange(newText.length)
                                                    )
                                                    return@onPreviewKeyEvent true
                                                }
                                                return@onPreviewKeyEvent false
                                            }
                                            else -> {
                                                if (state.searchEnabled) {
                                                    val char = SearchHelper.keyToChar(keyEvent.key)
                                                    if (char != null) {
                                                        try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                                                        state.searchQuery = SearchHelper.appendCharToSearch(state.searchQuery, char)
                                                        return@onPreviewKeyEvent true
                                                    }
                                                }
                                            }
                                        }
                                    }

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
                            AppsDrawerLayout(
                                state = state,
                                uiState = appsDrawer,
                                displayApps = displayApps,
                                paddedDisplayApps = paddedDisplayApps,
                                totalPages = totalPages,
                                isCalculated = isCalculated,
                                searchFocusRequester = searchFocusRequester,
                                onAppClick = { app ->
                                    state.selectedItemIndex = displayApps.indexOf(app)
                                    state.isDpadMode = false
                                    handleAppClick(app)
                                },
                                onAppLongClick = { app ->
                                    state.selectedItemIndex = displayApps.indexOf(app)
                                    state.isDpadMode = false
                                    vibrateFeedback()
                                    when (flag) {
                                        Constants.AppDrawerFlag.SetHomeApp,
                                        Constants.AppDrawerFlag.SetSwipeLeft,
                                        Constants.AppDrawerFlag.SetSwipeRight,
                                        Constants.AppDrawerFlag.SetSwipeUp,
                                        Constants.AppDrawerFlag.SetSwipeDown,
                                        Constants.AppDrawerFlag.SetClickClock,
                                        Constants.AppDrawerFlag.SetClickDate,
                                        Constants.AppDrawerFlag.SetQuoteWidget,
                                        Constants.AppDrawerFlag.SetDoubleTap -> {
                                            viewModel.selectAppForFlag(app, flag, appPosition)
                                            findNavController().popBackStack()
                                        }
                                        else -> {
                                            state.contextMenuApp = app
                                            state.showContextMenu = true
                                            state.showRenameOverlay = false
                                        }
                                    }
                                },
                                onDelete = { app ->
                                    handleAppDelete(app)
                                    state.showContextMenu = false
                                    state.contextMenuApp = null
                                },
                                onRename = { pkg, newName ->
                                    try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                                    if (newName.isEmpty()) {
                                        val app = displayApps.find { it.activityPackage == pkg }
                                        if (app != null) {
                                            state.renameApp = app
                                            state.showRenameOverlay = true
                                            state.showContextMenu = false
                                            state.contextMenuApp = null
                                        }
                                    } else {
                                        handleRenameApp(pkg, newName)
                                        state.showRenameOverlay = false
                                        state.renameApp = null
                                    }
                                },
                                onHideShow = { f, app ->
                                    handleHideShowApp(f, app)
                                    state.showContextMenu = false
                                    state.contextMenuApp = null
                                },
                                onLock = { app ->
                                    handleLockApp(app) {
                                        state.showContextMenu = false
                                        state.contextMenuApp = null
                                    }
                                },
                                onInfo = { app -> 
                                    handleInfoApp(app)
                                    state.showContextMenu = false
                                    state.contextMenuApp = null
                                    findNavController().popBackStack(R.id.mainFragment, false)
                                },
                                onSearchQueryChange = { newQuery ->
                                    // Normalize to lowercase
                                    val lower = newQuery.text.lowercase()
                                    val sel = newQuery.selection
                                    val newSel = androidx.compose.ui.text.TextRange(
                                        sel.min.coerceAtMost(lower.length),
                                        sel.max.coerceAtMost(lower.length)
                                    )
                                    state.searchQuery = newQuery.copy(text = lower, selection = newSel)
                                },
                                onSearchAction = {
                                    try {
                                        val first = displayApps.firstOrNull()
                                        if (first != null) handleAppClick(first)
                                    } catch (_: Exception) {}
                                },
                                onInteract = { state.isDpadMode = false },
                                onCloseMenu = {
                                    state.showContextMenu = false
                                    state.showRenameOverlay = false
                                    state.contextMenuApp = null
                                    state.renameApp = null
                                }
                            )
                            // Use precomputed AZ letters from ViewModel so we only show
                            // letters that have apps (avoids the "no apps available" column)
                            val azLetters = appsDrawer.azLetters

                            // A-Z sidebar for quick filtering (Niagara-like)
                            // Only show AZ sidebar when the preference is enabled and no overlay is active.
                            // Hide during context menu or rename to prevent interference and stale updates.
                            // Wait for isCalculated so it appears at the same time as the apps list.
                            if (isCalculated && appsDrawer.appDrawerAzFilter && !state.showContextMenu && !state.showRenameOverlay && !state.isHiddenAppsMode) {
                                Theme.colors.text
                                    AZSidebar(
                                    modifier = Modifier
                                        .align(if (appsDrawer.appDrawerAlignment == 2) Alignment.CenterStart else Alignment.CenterEnd),
                                    edgeWidth = pageDotSize,
                                    letters = azLetters,
                                    appTextColor = Theme.colors.text,
                                    containerHeightPx = containerSize.height,
                                    selectedLetter = state.azFilterLetter,
                                    onLetterSelected = { letter ->
                                        try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.AZ_FILTER) } catch (_: Exception) {}
                                        try {
                                            val newFilter = if (letter == "★") null else letter.firstOrNull()?.uppercaseChar()
                                            if (newFilter != state.azFilterLetter) {
                                                state.azFilterLetter = newFilter
                                                setCurrentPageSafe(0)
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    onTouchStart = { /* no-op */ },
                                    onTouchEnd = { finalLetter ->
                                        try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                                        try {
                                            val newFilter = if (finalLetter == null || finalLetter == "★") null else finalLetter.firstOrNull()?.uppercaseChar()
                                            if (newFilter != state.azFilterLetter) {
                                                state.azFilterLetter = newFilter
                                            }
                                            // If user explicitly released on the star, ensure we go to page 0.
                                            if (finalLetter == "★") {
                                                setCurrentPageSafe(0)
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    // DPAD navigation parameters
                                    dpadSelectedIndex = if (state.azFilterFocused) state.azFilterSelectedIndex else null,
                                    onDpadIndexChange = { index ->
                                        state.azFilterSelectedIndex = index
                                        // Apply the filter for the selected letter
                                        val letter = azLetters.getOrNull(index)?.toString()
                                        if (letter != null) {
                                            try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                                            val newFilter = if (letter == "★") null else letter.firstOrNull()?.uppercaseChar()
                                            if (newFilter != state.azFilterLetter) {
                                                state.azFilterLetter = newFilter
                                                setCurrentPageSafe(0)
                                            }
                                        }
                                    },
                                    textIslandsShape = appsDrawer.textIslandsShape
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleAppClick(app: AppListItem) {
        // For flag-based actions, select and navigate back
        when (flag) {
            Constants.AppDrawerFlag.SetHomeApp,
            Constants.AppDrawerFlag.SetSwipeLeft,
            Constants.AppDrawerFlag.SetSwipeRight,
            Constants.AppDrawerFlag.SetSwipeUp,
            Constants.AppDrawerFlag.SetSwipeDown,
            Constants.AppDrawerFlag.SetClickClock,
            Constants.AppDrawerFlag.SetClickDate,
            Constants.AppDrawerFlag.SetQuoteWidget,
            Constants.AppDrawerFlag.SetDoubleTap -> {
                viewModel.selectAppForFlag(app, flag, appPosition)
                findNavController().popBackStack()
                return
            }
            else -> { /* Launch app */ }
        }
        
        // Handle synthetic apps and system shortcuts
        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
        if (appsRepo.launchSyntheticOrSystemApp(
                requireContext(),
                app.activityPackage,
                this
            )
        ) {
            return
        }
        
        // Check if app is locked
        if (viewModel.isAppLocked(app.activityPackage)) {
            promptBiometric(
                onSuccess = { viewModel.launchApp(app) },
                title = "Authenticate to open ${app.label}"
            )
        } else {
            viewModel.launchApp(app)
        }
    }

    private fun handleAppLongClick(app: AppListItem) {
        // Long click handled by UI state
    }

    private fun handleAppDelete(app: AppListItem) {
        if (requireContext().isSystemApp(app.activityPackage)) {
            showShortToast(getString(R.string.can_not_delete_system_apps))
        } else {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:${app.activityPackage}")
            }
            pendingUninstallPackage = app.activityPackage
            uninstallLauncher.launch(intent)
        }
    }

    private fun handleRenameApp(pkg: String, newName: String) {
        if (newName == "RESET_TO_ORIGINAL") {
            viewModel.renameApp(pkg, "", flag)
        } else {
            viewModel.renameApp(pkg, newName, flag)
        }
    }

    private fun handleHideShowApp(f: Constants.AppDrawerFlag, app: AppListItem) {
        viewModel.hideOrShowApp(f, app)
        if (f == Constants.AppDrawerFlag.HiddenApps && viewModel.getPrefs().hiddenApps.isEmpty()) {
            findNavController().popBackStack()
        }
    }

    private fun handleLockApp(app: AppListItem, onSuccess: () -> Unit) {
        promptBiometric(
            onSuccess = {
                viewModel.toggleAppLock(app.activityPackage)
                onSuccess()
            },
            title = if (prefs.lockedApps.contains(app.activityPackage)) "Authenticate to unlock" else "Authenticate to lock"
        )
    }

    private fun handleInfoApp(app: AppListItem) {
        openAppInfo(requireContext(), app.user, app.activityPackage)
    }

    private fun promptBiometric(onSuccess: () -> Unit, title: String = "Authenticate") {
        val biometricManager = BiometricManager.from(requireContext())
        val canAuth = biometricManager.canAuthenticate()
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(), "Biometric authentication not available", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    onSuccess()
                } catch (_: Exception) {}
            }

        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onResume() {
        super.onResume()
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
        try {
            // Clear any buffered typed characters so reopening the drawer doesn't reuse old input
            viewModel.clearTypedCharReplay()
        } catch (_: Exception) {}
        act.suppressKeyForwarding = false
    }
}
