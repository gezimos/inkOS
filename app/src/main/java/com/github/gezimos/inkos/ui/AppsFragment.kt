package com.github.gezimos.inkos.ui

import android.content.Intent
import android.os.Bundle
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
import com.github.gezimos.inkos.ui.compose.inkOsSafeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.github.gezimos.common.hideKeyboard
import com.github.gezimos.common.isSystemApp
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.BuildConfig
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.EditModeHelper
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.compose.AZSidebar
import com.github.gezimos.inkos.ui.compose.AppsDrawerLayout
import com.github.gezimos.inkos.ui.compose.EditModeOverlay
import com.github.gezimos.inkos.ui.compose.NavHelper
import com.github.gezimos.inkos.helper.SearchHelper
import com.github.gezimos.inkos.ui.compose.gestureHelper
import com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager
import androidx.core.net.toUri
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AppsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var flag: Constants.AppDrawerFlag
    private val dialogManager: ComposeDialogManager by lazy {
        ComposeDialogManager(requireContext(), requireActivity())
    }
    private var appPosition: Int = 0
    private var currentSearchQuery: String = ""
    private var showSearchArg: Boolean = false
    private var searchMode: Int = -1 // -1=normal, 0=search (forces search bar + keyboard), 1=contacts (pre-loads all contacts)

    private var pendingUninstallPackage: String? = null
    private val uninstallLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
            pendingUninstallPackage?.let {
                viewModel.refreshAppListAfterUninstall(includeHiddenApps = false)
                pendingUninstallPackage = null
            }
        }

    val folderPickerLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
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
        searchMode = arguments?.getInt("searchMode", -1) ?: -1

        val includeHidden = flag == Constants.AppDrawerFlag.SetHomeApp || flag == Constants.AppDrawerFlag.HiddenApps || flag == Constants.AppDrawerFlag.EditFavorites || flag == Constants.AppDrawerFlag.EditHiddenApps

        if (includeHidden || flag == Constants.AppDrawerFlag.HiddenApps) {
            viewModel.getAppList(includeHiddenApps = includeHidden, flag = flag)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragment = this
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
                remember {
                    try { context.getSystemService(Vibrator::class.java) } catch (_: Exception) { null }
                }
                
                // Consolidated ViewModel state
                val appsDrawer by viewModel.appsDrawerUiState.collectAsState()
                
                LaunchedEffect(Unit) {
                    if (appsDrawer.appList.isEmpty()) {
                        viewModel.getAppList(includeHiddenApps = false, flag = flag)
                    }
                    if ((flag == Constants.AppDrawerFlag.HiddenApps || flag == Constants.AppDrawerFlag.EditHiddenApps) && appsDrawer.hiddenApps.isEmpty()) {
                        viewModel.getHiddenApps()
                    }
                }

                val baseAppList = when (flag) {
                    Constants.AppDrawerFlag.HiddenApps -> appsDrawer.hiddenApps
                    Constants.AppDrawerFlag.EditHiddenApps -> appsDrawer.appList + appsDrawer.hiddenApps
                    else -> appsDrawer.appList
                }
                val appsForDrawer = remember(baseAppList, flag) {
                    baseAppList
                }
                
                val state = remember {
                    AppsDrawerState(
                        flag = flag,
                        position = appPosition,
                        apps = appsForDrawer
                    ).also {
                        if (searchMode >= 0) {
                            it.searchEnabled = true
                        }
                    }
                }
                
                LaunchedEffect(flag) {
                    if (flag == Constants.AppDrawerFlag.EditFavorites) {
                        state.selectedFavorites.clear()
                        val homeAppsCount = prefs.homeAppsNum
                        for (i in 0 until homeAppsCount) {
                            val homeApp = prefs.getHomeAppModel(i)
                            if (homeApp.activityPackage.isNotEmpty()) {
                                state.selectedFavorites.add(homeApp)
                            }
                        }
                    }
                }

                // Initialize hidden-apps selection once the full app list is available
                var hiddenInitialized by remember { mutableStateOf(false) }
                LaunchedEffect(flag, appsForDrawer) {
                    if (flag == Constants.AppDrawerFlag.EditHiddenApps && !hiddenInitialized && appsForDrawer.isNotEmpty()) {
                        val hiddenSet = prefs.hiddenApps
                        state.selectedHidden.clear()
                        appsForDrawer.forEach { app ->
                            val key = if (app.shortcutId != null) {
                                "${app.activityPackage}|${app.shortcutId}|${app.user}"
                            } else {
                                "${app.activityPackage}|${app.user}"
                            }
                            val matched = hiddenSet.contains(key) ||
                                hiddenSet.contains(app.activityPackage) ||
                                hiddenSet.contains("${app.activityPackage}|${app.user}")
                            if (matched) state.selectedHidden.add(app)
                        }
                        hiddenInitialized = true
                    }
                }
                
                // Disable search for SetHomeApp/EditFavorites
                LaunchedEffect(appsDrawer.appDrawerSearchEnabled, flag) {
                    state.searchEnabled = (appsDrawer.appDrawerSearchEnabled || searchMode >= 0) && flag != Constants.AppDrawerFlag.SetHomeApp && flag != Constants.AppDrawerFlag.EditFavorites && flag != Constants.AppDrawerFlag.EditHiddenApps
                }
                
                LaunchedEffect(state.showRenameOverlay) {
                    val act = context as? com.github.gezimos.inkos.MainActivity
                    act?.suppressKeyForwarding = state.showRenameOverlay
                }
                
                LaunchedEffect(appsForDrawer) {
                    state.apps = appsForDrawer
                }
                
                LaunchedEffect(Unit) {
                    try {
                        viewModel.typedCharEvents.collect { ch ->
                            if (state.searchEnabled) {
                                state.searchQuery = SearchHelper.appendCharToSearch(state.searchQuery, ch)
                            }
                        }
                    } catch (_: Exception) {}
                }

                LaunchedEffect(Unit) {
                    try {
                        viewModel.backspaceEvents.collect {
                            if (state.searchEnabled && state.searchQuery.text.isNotEmpty()) {
                                state.searchQuery = SearchHelper.backspaceSearch(state.searchQuery)
                            }
                        }
                    } catch (_: Exception) {}
                }
                
                var containerSize by remember { mutableStateOf(IntSize.Zero) }

                // Use persisted cache for instant rendering
                val cachedAppsPerPage = viewModel.getCachedAppsPerPage()
                var isCalculated by remember {
                    mutableStateOf(cachedAppsPerPage != null)
                }

                if (cachedAppsPerPage != null) {
                    if (state.appsPerPage != cachedAppsPerPage) {
                        state.appsPerPage = cachedAppsPerPage
                    }
                    if (!isCalculated) {
                        isCalculated = true
                    }
                }

                val iconTextMeasurer = rememberTextMeasurer()
                val iconTypefaceNullable = remember(appsDrawer.appsFont, appsDrawer.customFontPath) {
                    try { appsDrawer.appsFont.getFont(context, appsDrawer.customFontPath) } catch (_: Exception) { null }
                }
                val iconFontFamily = remember(iconTypefaceNullable) {
                    if (iconTypefaceNullable != null) FontFamily(iconTypefaceNullable) else FontFamily.Default
                }
                val drawerScreenScale = rememberScreenScale()
                val iconSizePx = remember(appsDrawer.appDrawerSize, iconFontFamily, iconTextMeasurer, drawerScreenScale) {
                    try {
                        iconTextMeasurer.measure(
                            text = AnnotatedString("Ag"),
                            style = TextStyle(fontSize = appsDrawer.appDrawerSize.sp.scaled(drawerScreenScale), fontFamily = iconFontFamily)
                        ).size.height.coerceAtLeast(1)
                    } catch (_: Exception) {
                        with(density) { appsDrawer.appDrawerSize.dp.scaled(drawerScreenScale).toPx().toInt() }.coerceAtLeast(1)
                    }
                }

                if (cachedAppsPerPage == null) {
                    val gapPx = remember(appsDrawer.appDrawerGap, density) {
                        try { with(density) { appsDrawer.appDrawerGap.dp.roundToPx() } } catch (_: Exception) { 0 }
                    }.coerceAtLeast(0)
                    val rowTextHeightPx = iconSizePx
                    val rowHeightPx = remember(rowTextHeightPx, gapPx) {
                        ((rowTextHeightPx * 0.95f) + (gapPx * 2)).toInt().coerceAtLeast(1)
                    }
                    val headerRows = remember(appsDrawer.appDrawerSearchEnabled, state.searchEnabled, state.isHiddenAppsMode, flag) {
                        var rows = 0
                        if (flag == Constants.AppDrawerFlag.SetHomeApp || flag == Constants.AppDrawerFlag.HiddenApps || flag == Constants.AppDrawerFlag.EditFavorites || flag == Constants.AppDrawerFlag.EditHiddenApps) {
                            rows++
                        }
                        if (!state.isHiddenAppsMode && state.searchEnabled) {
                            rows++
                        }
                        rows
                    }
                    val appsPerPageCalculated = remember(containerSize.height, rowHeightPx, headerRows) {
                        val h = containerSize.height
                        if (h <= 0) 1
                        else {
                            val headerHeight = headerRows * rowHeightPx
                            val available = (h - headerHeight - rowHeightPx).coerceAtLeast(rowHeightPx)
                            (available / rowHeightPx).coerceAtLeast(1)
                        }
                    }
                    LaunchedEffect(appsPerPageCalculated, containerSize.height) {
                        if (containerSize.height > 0) {
                            state.appsPerPage = appsPerPageCalculated
                            viewModel.cacheAppsPerPage(appsPerPageCalculated)
                            isCalculated = true
                        } else {
                            isCalculated = false
                        }
                    }
                }
                
                val usageStatsMap by viewModel.appUsageStats.collectAsState()

                val sortedApps by remember {
                    derivedStateOf {
                        if (state.isEditFavoritesMode) {
                            val favSet = state.selectedFavorites.map { it.activityPackage to it.activityClass }.toSet()
                            val remaining = state.apps.filter { app ->
                                (app.activityPackage to app.activityClass) !in favSet
                            }.sorted()
                            state.selectedFavorites.toList() + remaining
                        } else if (state.isEditHiddenMode) {
                            val (checked, unchecked) = state.apps.partition { app ->
                                state.selectedHidden.any { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass }
                            }
                            checked.sorted() + unchecked.sorted()
                        } else {
                            when (appsDrawer.appDrawerSortOrder) {
                                1 -> { // Most Used
                                    if (usageStatsMap.isEmpty()) state.apps.sorted()
                                    else {
                                        val isInkOS = { app: AppListItem -> app.activityPackage == BuildConfig.APPLICATION_ID }
                                        val (withStats, noStats) = state.apps.partition { !isInkOS(it) && usageStatsMap.containsKey(it.activityPackage) }
                                        withStats.sortedByDescending { usageStatsMap[it.activityPackage]!!.second } + noStats.sorted()
                                    }
                                }
                                2 -> { // Last Used
                                    if (usageStatsMap.isEmpty()) state.apps.sorted()
                                    else {
                                        val isInkOS = { app: AppListItem -> app.activityPackage == BuildConfig.APPLICATION_ID }
                                        val (withStats, noStats) = state.apps.partition { !isInkOS(it) && usageStatsMap.containsKey(it.activityPackage) }
                                        withStats.sortedByDescending { usageStatsMap[it.activityPackage]!!.first } + noStats.sorted()
                                    }
                                }
                                else -> state.apps.sorted() // Alphabetical
                            }
                        }
                    }
                }
                

                var searchVersion by remember { mutableIntStateOf(0) }
                LaunchedEffect(state.searchQuery.text) { searchVersion++; currentSearchQuery = state.searchQuery.text }
                val musicCache = remember(appsDrawer.appDrawerSearchMusicEnabled) { android.util.LruCache<String, List<AppListItem>>(16) }
                val fileCache = remember(appsDrawer.appDrawerSearchFilesEnabled) { android.util.LruCache<String, List<AppListItem>>(16) }
                var allContacts by remember { mutableStateOf<List<AppListItem>>(emptyList()) }
                LaunchedEffect(appsDrawer.appDrawerSearchContactsEnabled, appsDrawer.appDrawerSearchContactAccounts) {
                    if (appsDrawer.appDrawerSearchContactsEnabled) {
                        allContacts = withContext(Dispatchers.IO) {
                            if (com.github.gezimos.inkos.helper.ContactsHelper.hasContactsPermission(requireContext())) {
                                com.github.gezimos.inkos.helper.ContactsHelper.searchContacts(requireContext(), "", listAll = true, allowedAccounts = appsDrawer.appDrawerSearchContactAccounts)
                                    .map { com.github.gezimos.inkos.helper.ContactsHelper.convertToAppListItem(it) }
                            } else emptyList()
                        }
                    } else {
                        allContacts = emptyList()
                    }
                }

                var musicSearchResults by remember { mutableStateOf<List<AppListItem>>(emptyList()) }
                var fileSearchResults by remember { mutableStateOf<List<AppListItem>>(emptyList()) }
                var asyncSearchDone by remember { mutableStateOf(true) }
                val settingsSearchResults by remember {
                    derivedStateOf {
                        val q = state.searchQuery.text
                        if (q.isBlank()) emptyList()
                        else com.github.gezimos.inkos.helper.SettingsSearchHelper.search(q)
                    }
                }
                LaunchedEffect(state.searchQuery.text) {
                    val query = state.searchQuery.text
                    val version = searchVersion

                    val wantMusic = query.isNotBlank() && appsDrawer.appDrawerSearchMusicEnabled
                    val wantFiles = query.isNotBlank() && appsDrawer.appDrawerSearchFilesEnabled

                    if (!wantMusic && !wantFiles) {
                        if (musicSearchResults.isNotEmpty()) musicSearchResults = emptyList()
                        if (fileSearchResults.isNotEmpty()) fileSearchResults = emptyList()
                        asyncSearchDone = true
                        return@LaunchedEffect
                    }
                    asyncSearchDone = false

                    val cachedMusic = if (wantMusic) musicCache.get(query) else emptyList()
                    val cachedFiles = if (wantFiles) fileCache.get(query) else emptyList()
                    if (cachedMusic != null && cachedFiles != null) {
                        musicSearchResults = cachedMusic
                        fileSearchResults = cachedFiles
                        asyncSearchDone = true
                        return@LaunchedEffect
                    }

                    delay(if (wantFiles && cachedFiles == null) 200L else 100L)
                    if (version != searchVersion) return@LaunchedEffect

                    val musicDeferred: Deferred<List<AppListItem>>? = if (wantMusic && cachedMusic == null) {
                        async(Dispatchers.IO) {
                            if (com.github.gezimos.inkos.helper.MusicSearchHelper.hasAudioPermission(requireContext())) {
                                com.github.gezimos.inkos.helper.MusicSearchHelper.search(requireContext(), query)
                            } else emptyList()
                        }
                    } else null

                    val filesDeferred: Deferred<List<AppListItem>>? = if (wantFiles && cachedFiles == null) {
                        async(Dispatchers.IO) {
                            withTimeoutOrNull(2000) {
                                if (com.github.gezimos.inkos.helper.FileSearchHelper.hasFolderAccess(requireContext())) {
                                    com.github.gezimos.inkos.helper.FileSearchHelper.search(requireContext(), query)
                                } else emptyList()
                            } ?: emptyList()
                        }
                    } else null

                    val music = cachedMusic ?: musicDeferred?.await() ?: emptyList()
                    val files = cachedFiles ?: filesDeferred?.await() ?: emptyList()

                    if (version != searchVersion) return@LaunchedEffect

                    if (cachedMusic == null) musicCache.put(query, music)
                    if (cachedFiles == null) fileCache.put(query, files)

                    musicSearchResults = music
                    fileSearchResults = files
                    asyncSearchDone = true
                }

                val filteredApps by remember {
                    derivedStateOf {
                        val query = state.searchQuery.text
                        val isSearchActive = query.isNotBlank() && !state.isHiddenAppsMode && state.azFilterLetter == null

                        if (state.privateSpaceFilter) {
                            appsDrawer.privateSpaceApps.sorted()
                        } else if (state.isHiddenAppsMode) {
                            sortedApps
                        } else if (state.azFilterLetter != null) {
                            val filterChar = state.azFilterLetter!!
                            sortedApps.filter { app ->
                                app.normalizedFirstChar == filterChar
                            }
                        } else if (!isSearchActive) {
                            sortedApps
                        } else {
                            buildList {
                                val normalizedQuery = SearchHelper.normalizeString(query)
                                val ctx = requireContext()
                                val scored = ArrayList<Pair<AppListItem, Int>>()

                                fun scoreApp(app: AppListItem): Int {
                                    val aliasScore = SearchHelper.scorePreNormalized(app.normalizedLabel, normalizedQuery, 100, app.label)
                                    val origScore = app.normalizedOriginalLabel?.let {
                                        SearchHelper.scorePreNormalized(it, normalizedQuery, 100, app.activityLabel)
                                    } ?: 0
                                    return maxOf(aliasScore, origScore)
                                }

                                for (app in sortedApps) {
                                    var score = scoreApp(app)
                                    if (score > 0) {
                                        score += SearchHelper.getFrequencyBoost(ctx, normalizedQuery, app.activityPackage)
                                        scored.add(app to score)
                                    }
                                }
                                if (appsDrawer.appDrawerSearchHiddenAppsEnabled) {
                                    for (app in appsDrawer.hiddenApps) {
                                        var score = scoreApp(app)
                                        if (score > 0) {
                                            score += SearchHelper.getFrequencyBoost(ctx, normalizedQuery, app.activityPackage)
                                            scored.add(app to score)
                                        }
                                    }
                                }

                                val contactScored = ArrayList<Pair<AppListItem, Int>>()
                                for (app in allContacts) {
                                    val score = SearchHelper.scorePreNormalized(app.normalizedLabel, normalizedQuery, 75, app.label)
                                    if (score > 0) contactScored.add(app to score)
                                }
                                contactScored.sortByDescending { it.second }
                                for (i in 0 until contactScored.size.coerceAtMost(10)) {
                                    scored.add(contactScored[i])
                                }

                                for ((index, app) in settingsSearchResults.withIndex()) {
                                    scored.add(app to (45 - index))
                                }

                                for (app in musicSearchResults) {
                                    val score = SearchHelper.scoreStrict(app.normalizedLabel, normalizedQuery, 40)
                                    if (score > 0) scored.add(app to score)
                                }

                                for (app in fileSearchResults) {
                                    val score = SearchHelper.scoreStrict(app.normalizedLabel, normalizedQuery, 40)
                                    if (score > 0) scored.add(app to score)
                                }

                                scored.sortByDescending { it.second }
                                for ((app, _) in scored) add(app)

                                if (appsDrawer.appDrawerSearchWebEnabled) {
                                    add(SearchHelper.createWebSearchItem(query))
                                }
                            }
                        }
                    }
                }
                
                val appsWithSeparators by remember {
                    derivedStateOf {
                        val showSeparators = (flag == Constants.AppDrawerFlag.SetHomeApp || flag == Constants.AppDrawerFlag.EditFavorites)
                            && state.azFilterLetter == null
                            && state.searchQuery.text.isBlank()
                            && !state.privateSpaceFilter
                        if (showSeparators) {
                            val u = android.os.Process.myUserHandle()
                            val separators = listOf(
                                AppListItem(activityLabel = " ", activityPackage = Constants.SEPARATOR_EMPTY, activityClass = "", user = u, customLabel = context.getString(R.string.separator_empty)),
                                AppListItem(activityLabel = "\u2014", activityPackage = Constants.SEPARATOR_EM_DASH, activityClass = "", user = u, customLabel = context.getString(R.string.separator_em_dash)),
                                AppListItem(activityLabel = "\u00B7 \u00B7 \u00B7", activityPackage = Constants.SEPARATOR_DOTS, activityClass = "", user = u, customLabel = context.getString(R.string.separator_dots))
                            )
                            val selectedPkgs = state.selectedFavorites.map { it.activityPackage }.toSet()
                            filteredApps + separators.filter { it.activityPackage !in selectedPkgs }
                        } else filteredApps
                    }
                }

                var profileSwitchedThisGesture by remember { mutableStateOf(false) }
                val totalPages by remember {
                    derivedStateOf {
                        if (state.appsPerPage > 0) {
                            ((appsWithSeparators.size + state.appsPerPage - 1) / state.appsPerPage).coerceAtLeast(1)
                        } else {
                            1
                        }
                    }
                }

                var maxFavoritesState by remember { mutableStateOf(prefs.homeAppsNum) }
                LaunchedEffect(flag) {
                    if (flag == Constants.AppDrawerFlag.EditFavorites) maxFavoritesState = prefs.homeAppsNum
                }

                // Clamp page into valid range
                val setCurrentPageSafe: (Int) -> Unit = remember {
                    { newPage: Int ->
                        val maxPage = (totalPages - 1).coerceAtLeast(0)
                        val clamped = newPage.coerceIn(0, maxPage)
                        if (state.currentPage != clamped) state.currentPage = clamped
                    }
                }

                LaunchedEffect(state.searchQuery.text) {
                    setCurrentPageSafe(0)
                    state.selectedItemIndex = 0
                    if (state.searchQuery.text.isNotBlank()) {
                        state.azFilterLetter = null
                    }
                }
                

                LaunchedEffect(state.currentPage, totalPages) {
                    val maxPage = (totalPages - 1).coerceAtLeast(0)
                    val clamped = state.currentPage.coerceIn(0, maxPage)
                    if (state.currentPage != clamped) {
                        state.currentPage = clamped
                    }
                }
                
                val displayApps = remember(appsWithSeparators, state.currentPage, state.appsPerPage) {
                    val safePage = state.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                    val startIndex = safePage * state.appsPerPage
                    val endIndex = (startIndex + state.appsPerPage).coerceAtMost(appsWithSeparators.size)
                    if (startIndex < appsWithSeparators.size) {
                        appsWithSeparators.subList(startIndex, endIndex)
                    } else {
                        emptyList()
                    }
                }
                
                LaunchedEffect(displayApps.size) {
                    if (state.selectedItemIndex >= displayApps.size) {
                        state.selectedItemIndex = (displayApps.size - 1).coerceAtLeast(0)
                    }
                }
                
                val paddedDisplayApps = remember(displayApps, state.appsPerPage) {
                    val missing = (state.appsPerPage - displayApps.size).coerceAtLeast(0)
                    if (missing == 0) displayApps
                    else displayApps + List(missing) { null }
                }
                
                fun vibratePage() {
                    com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.PAGE)
                }

                fun vibrateFeedback() {
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
                
                // aren't missed and the wrong app isn't launched.
                LaunchedEffect(state.searchQuery.text, displayApps.size, asyncSearchDone) {
                    if (appsDrawer.appDrawerAutoLaunch && state.searchQuery.text.isNotBlank() && asyncSearchDone) {
                        val launchableApps = filteredApps.filter { app ->
                            app.activityPackage != Constants.INTERNAL_WEB_SEARCH
                        }
                        if (launchableApps.size == 1) {
                            delay(150)
                            handleAppClick(launchableApps[0])
                        }
                    }
                }
                
                LaunchedEffect(Unit) {
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

                LaunchedEffect(state.searchQuery.text) {
                    if (state.searchQuery.text.isNotBlank() && state.searchEnabled) {
                        delay(300)
                        try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                    }
                }

                LaunchedEffect(appsDrawer.appDrawerAutoShowKeyboard, appsDrawer.appDrawerSearchEnabled, state.searchEnabled) {
                    try {
                        if ((appsDrawer.appDrawerAutoShowKeyboard || searchMode >= 0) && state.searchEnabled) {
                            // the window where space-as-home-button can fire.
                            delay(if (searchMode >= 0) 10L else 50L)
                            try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                            try {
                                try {
                                    keyboardController?.show()
                                } catch (_: Exception) {}

                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                }
                
                LaunchedEffect(state.showRenameOverlay) {
                    keyPressTracker.reset()
                }
                
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
                            .inkOsSafeDrawingPadding()
                    ) {
                        val screenScale = rememberScreenScale()
                        val pageDotSize = com.github.gezimos.inkos.ui.compose.azSidebarWidth.scaled(screenScale)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp.scaled(screenScale))
                                .onSizeChanged { containerSize = it }
                                .focusRequester(columnFocusRequester)
                                .focusable()
                                .onPreviewKeyEvent { keyEvent ->
                                    if (state.showRenameOverlay) {
                                        return@onPreviewKeyEvent false
                                    }

                                    if (keyEvent.type == KeyEventType.KeyDown && (keyEvent.key == Key.Back || keyEvent.key == Key.Escape)) {
                                        if (state.azFilterLetter != null) {
                                            state.azFilterLetter = null
                                            state.azFilterSelectedIndex = 0
                                            state.azFilterFocused = false
                                            setCurrentPageSafe(0)
                                            return@onPreviewKeyEvent true
                                        }
                                        if (state.searchEnabled && state.searchQuery.text.isNotEmpty()) {
                                            state.searchQuery = androidx.compose.ui.text.input.TextFieldValue("")
                                            setCurrentPageSafe(0)
                                            return@onPreviewKeyEvent true
                                        }
                                        if (state.searchFocused) {
                                            state.searchFocused = false
                                            state.isDpadMode = true
                                            state.selectedItemIndex = 0
                                            columnFocusRequester.requestFocus()
                                            return@onPreviewKeyEvent true
                                        }
                                    }

                                    try {
                                        val handled = NavHelper.handleAppsKeyEvent(
                                            keyEvent = keyEvent,
                                            keyPressTracker = keyPressTracker,
                                            isDpadModeSetter = { v -> state.isDpadMode = v },
                                            selectedIndexGetter = { state.selectedItemIndex },
                                            selectedIndexSetter = { v ->
                                                state.selectedItemIndex = v
                                                state.dragHandleFocused = false
                                            },
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
                                                            dialogManager.showAppContextMenu(
                                                                app = selectedApp,
                                                                flag = flag,
                                                                isLocked = viewModel.isAppLocked(selectedApp),
                                                                onDelete = { handleAppDelete(it) },
                                                                onRename = { pkg, newName ->
                                                                    if (newName.isEmpty()) {
                                                                        val parts = pkg.split("|", limit = 2)
                                                                        val app = displayApps.find { it.activityPackage == parts[0] && (parts.size < 2 || it.shortcutId == parts[1]) }
                                                                        if (app != null) {
                                                                            state.renameApp = app
                                                                            state.showRenameOverlay = true
                                                                        }
                                                                    } else {
                                                                        val appToRename = state.renameApp
                                                                        if (appToRename != null) handleRenameApp(appToRename, newName)
                                                                        state.showRenameOverlay = false
                                                                        state.renameApp = null
                                                                    }
                                                                },
                                                                onHideShow = { f, app ->
                                                                    handleHideShowApp(f, app)
                                                                    if (f == Constants.AppDrawerFlag.HiddenApps && viewModel.getPrefs().hiddenApps.isEmpty()) {
                                                                        findNavController().popBackStack()
                                                                    }
                                                                },
                                                                onLock = { app -> handleLockApp(app) {} },
                                                                onInfo = { app ->
                                                                    handleInfoApp(app)
                                                                    findNavController().popBackStack(R.id.mainFragment, false)
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            onNavigateBack = { findNavController().popBackStack() },
                                            showAzFilter = appsDrawer.appDrawerAzFilter && !state.showRenameOverlay && !state.isHiddenAppsMode && !state.isEditFavoritesMode && !state.isEditHiddenMode,
                                            azFilterFocused = state.azFilterFocused,
                                            onAzFilterFocusChange = { focused ->
                                                state.azFilterFocused = focused
                                                if (focused) {
                                                    state.azFilterSelectedIndex = 0
                                                }
                                            },
                                            azFilterSelectedIndex = state.azFilterSelectedIndex,
                                            onAzFilterIndexChange = { index ->
                                                state.azFilterSelectedIndex = index
                                            },
                                            azFilterSize = appsDrawer.azLetters.size,
                                            onAzFilterActivate = {
                                                val letter = appsDrawer.azLetters.getOrNull(state.azFilterSelectedIndex)?.toString()
                                                if (letter != null) {
                                                    try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.SOFT) } catch (_: Exception) {}
                                                    val newFilter = if (letter == "★") null else letter.firstOrNull()?.uppercaseChar()
                                                    state.azFilterLetter = newFilter
                                                    setCurrentPageSafe(0)
                                                }
                                            },
                                            showSearch = state.searchEnabled && !state.isHiddenAppsMode,
                                            onSearchFocus = {
                                                state.searchFocused = true
                                                try {
                                                    searchFocusRequester.requestFocus()
                                                    keyboardController?.show()
                                                } catch (_: Exception) {}
                                            },
                                            // Selection-mode DPAD support (Edit Favorites / Edit Hidden Apps)
                                            isEditFavoritesMode = state.isEditFavoritesMode || state.isEditHiddenMode,
                                            allowDragReorder = state.isEditFavoritesMode,
                                            titleFocusable = flag == Constants.AppDrawerFlag.HiddenApps,
                                            hasDoneButton = state.isEditFavoritesMode || state.isEditHiddenMode,
                                            doneFocused = state.doneFocused,
                                            onDoneFocusChange = { focused ->
                                                state.doneFocused = focused
                                                if (focused) state.titleFocused = false
                                            },
                                            onDoneActivate = {
                                                when {
                                                    state.isEditHiddenMode -> {
                                                        viewModel.saveEditHiddenApps(state.selectedHidden.toList())
                                                        findNavController().popBackStack()
                                                    }
                                                    state.isEditFavoritesMode -> {
                                                        viewModel.saveEditFavorites(state.selectedFavorites.toList())
                                                        findNavController().popBackStack()
                                                    }
                                                }
                                            },
                                            titleFocused = state.titleFocused,
                                            onTitleFocusChange = { focused ->
                                                state.titleFocused = focused
                                                if (focused) state.doneFocused = false
                                            },
                                            onTitleActivate = {
                                                when (flag) {
                                                    Constants.AppDrawerFlag.EditFavorites -> {
                                                        EditModeHelper.showHomeAppsAndPagesOnly(requireContext(), this@AppsFragment, prefs) {
                                                            maxFavoritesState = prefs.homeAppsNum
                                                        }
                                                    }
                                                    Constants.AppDrawerFlag.HiddenApps -> {
                                                        findNavController().navigate(
                                                            R.id.appsFragment,
                                                            androidx.core.os.bundleOf("flag" to Constants.AppDrawerFlag.EditHiddenApps.toString())
                                                        )
                                                    }
                                                    else -> {}
                                                }
                                            },
                                            onFavoriteToggle = if (state.isEditFavoritesMode || state.isEditHiddenMode) { { index ->
                                                val app = displayApps.getOrNull(index)
                                                if (app != null) {
                                                    if (state.isEditHiddenMode) {
                                                        val existingIndex = state.selectedHidden.indexOfFirst { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass }
                                                        if (existingIndex >= 0) state.selectedHidden.removeAt(existingIndex)
                                                        else state.selectedHidden.add(app)
                                                    } else {
                                                        val existingIndex = state.selectedFavorites.indexOfFirst { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass }
                                                        if (existingIndex >= 0) {
                                                            state.selectedFavorites.removeAt(existingIndex)
                                                        } else if (state.selectedFavorites.size < prefs.homeAppsNum) {
                                                            state.selectedFavorites.add(app)
                                                        } else {
                                                            Toast.makeText(requireContext(), getString(R.string.max_favorites_reached, prefs.homeAppsNum), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            } } else null,
                                            // DPAD drag reorder
                                            dragHandleFocused = state.dragHandleFocused,
                                            onDragHandleFocusChange = { focused -> state.dragHandleFocused = focused },
                                            dpadGrabbedIndex = state.dpadGrabbedIndex,
                                            onDpadGrab = {
                                                val app = displayApps.getOrNull(state.selectedItemIndex)
                                                if (app != null) {
                                                    val favIndex = state.selectedFavorites.indexOfFirst {
                                                        it.activityPackage == app.activityPackage && it.activityClass == app.activityClass
                                                    }
                                                    if (favIndex >= 0) {
                                                        state.dpadGrabbedIndex = favIndex
                                                        state.draggingIndex = favIndex
                                                    }
                                                }
                                            },
                                            onDpadDrop = {
                                                state.dpadGrabbedIndex = null
                                                state.draggingIndex = null
                                                state.dragOffsetY = 0f
                                            },
                                            onDpadMoveUp = {
                                                val idx = state.dpadGrabbedIndex
                                                if (idx != null && idx > 0) {
                                                    val item = state.selectedFavorites.removeAt(idx)
                                                    state.selectedFavorites.add(idx - 1, item)
                                                    state.dpadGrabbedIndex = idx - 1
                                                    state.draggingIndex = idx - 1
                                                    state.dragOffsetY = 0f
                                                    if (state.selectedItemIndex > 0) {
                                                        state.selectedItemIndex = state.selectedItemIndex - 1
                                                    }
                                                }
                                            },
                                            onDpadMoveDown = {
                                                val idx = state.dpadGrabbedIndex
                                                if (idx != null && idx < state.selectedFavorites.size - 1) {
                                                    val item = state.selectedFavorites.removeAt(idx)
                                                    state.selectedFavorites.add(idx + 1, item)
                                                    state.dpadGrabbedIndex = idx + 1
                                                    state.draggingIndex = idx + 1
                                                    state.dragOffsetY = 0f
                                                    if (state.selectedItemIndex < displayApps.size - 1) {
                                                        state.selectedItemIndex = state.selectedItemIndex + 1
                                                    }
                                                }
                                            },
                                            isItemChecked = if (state.isEditFavoritesMode || state.isEditHiddenMode) { { index ->
                                                val app = displayApps.getOrNull(index)
                                                val targetList = if (state.isEditHiddenMode) state.selectedHidden else state.selectedFavorites
                                                app != null && targetList.any {
                                                    it.activityPackage == app.activityPackage && it.activityClass == app.activityClass
                                                }
                                            } } else null
                                        )
                                        if (handled) return@onPreviewKeyEvent true
                                    } catch (_: Exception) {}
                                    
                                    if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.DirectionDown && state.searchFocused) {
                                        state.searchFocused = false
                                        state.isDpadMode = true
                                        state.selectedItemIndex = 0
                                        columnFocusRequester.requestFocus()
                                        return@onPreviewKeyEvent true
                                    }

                                    if (keyEvent.type == KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            Key.Backspace, Key.Delete -> {
                                                if (state.searchEnabled && state.searchQuery.text.isNotEmpty()) {
                                                    state.searchQuery = SearchHelper.backspaceSearch(state.searchQuery)
                                                    return@onPreviewKeyEvent true
                                                }
                                                return@onPreviewKeyEvent false
                                            }
                                            else -> {
                                                if (state.searchEnabled) {
                                                    val char = SearchHelper.keyToChar(keyEvent.key)
                                                    if (char != null) {
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
                                    scrollPageMoveMultiplier = if (state.azFilterLetter != null) 1.5f else 3.0f,
                                    onAnyTouch = { profileSwitchedThisGesture = false },
                                    onVerticalPageMove = { delta ->
                                        try {
                                            if (state.draggingIndex != null) return@gestureHelper
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
                                                } else if (state.currentPage == 0 && !profileSwitchedThisGesture) {
                                                    profileSwitchedThisGesture = true
                                                    if (state.privateSpaceFilter) {
                                                        state.privateSpaceFilter = false
                                                    } else if (appsDrawer.hasPrivateSpace) {
                                                        state.privateSpaceFilter = true
                                                    }
                                                    state.azFilterLetter = null
                                                    setCurrentPageSafe(0)
                                                    try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    onLongSwipeDown = {
                                        try {
                                            if (state.draggingIndex != null) return@gestureHelper
                                            if (state.currentPage == 0 && flag != Constants.AppDrawerFlag.EditFavorites && flag != Constants.AppDrawerFlag.EditHiddenApps) {
                                                vibratePage()
                                                findNavController().popBackStack()
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
                                isCalculated = isCalculated,
                                searchFocusRequester = searchFocusRequester,
                                onAppClick = { app ->
                                    state.selectedItemIndex = displayApps.indexOf(app)
                                    state.isDpadMode = false
                                    handleAppClick(app)
                                },
                                onAppLongClick = { app ->
                                    if (EditModeHelper.isEditMode()) {
                                        EditModeHelper.showAppDrawerSettings(context, fragment, prefs, { }, folderPickerLauncher)
                                    } else {
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
                                                dialogManager.showAppContextMenu(
                                                    app = app,
                                                    flag = flag,
                                                    isLocked = viewModel.isAppLocked(app),
                                                    onDelete = { handleAppDelete(it) },
                                                    onRename = { pkg, newName ->
                                                        if (newName.isEmpty()) {
                                                            val rParts = pkg.split("|", limit = 2)
                                                            val a = displayApps.find { it.activityPackage == rParts[0] && (rParts.size < 2 || it.shortcutId == rParts[1]) }
                                                            if (a != null) {
                                                                state.renameApp = a
                                                                state.showRenameOverlay = true
                                                            }
                                                        } else {
                                                            val appToRename = state.renameApp
                                                            if (appToRename != null) handleRenameApp(appToRename, newName)
                                                            state.showRenameOverlay = false
                                                            state.renameApp = null
                                                        }
                                                    },
                                                    onHideShow = { f, a ->
                                                        handleHideShowApp(f, a)
                                                        if (f == Constants.AppDrawerFlag.HiddenApps && viewModel.getPrefs().hiddenApps.isEmpty()) {
                                                            findNavController().popBackStack()
                                                        }
                                                    },
                                                    onLock = { a -> handleLockApp(a) {} },
                                                    onInfo = { a ->
                                                        handleInfoApp(a)
                                                        findNavController().popBackStack(R.id.mainFragment, false)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                },
                                onRename = { pkg, newName ->
                                    try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                                    if (newName.isEmpty()) {
                                        val parts = pkg.split("|", limit = 2)
                                        val app = displayApps.find { it.activityPackage == parts[0] && (parts.size < 2 || it.shortcutId == parts[1]) }
                                        if (app != null) {
                                            state.renameApp = app
                                            state.showRenameOverlay = true
                                        }
                                    } else {
                                        val appToRename = state.renameApp
                                        if (appToRename != null) handleRenameApp(appToRename, newName)
                                        state.showRenameOverlay = false
                                        state.renameApp = null
                                    }
                                },
                                onSearchQueryChange = { newQuery ->
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
                                },
                                // Selection mode parameters (Edit Favorites / Edit Hidden Apps)
                                onFavoriteToggle = when (flag) {
                                    Constants.AppDrawerFlag.EditFavorites -> { app ->
                                        val existingIndex = state.selectedFavorites.indexOfFirst { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass }
                                        if (existingIndex >= 0) {
                                            state.selectedFavorites.removeAt(existingIndex)
                                        } else if (state.selectedFavorites.size < prefs.homeAppsNum) {
                                            state.selectedFavorites.add(app)
                                        } else {
                                            Toast.makeText(requireContext(), getString(R.string.max_favorites_reached, prefs.homeAppsNum), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    Constants.AppDrawerFlag.EditHiddenApps -> { app ->
                                        val existingIndex = state.selectedHidden.indexOfFirst { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass }
                                        if (existingIndex >= 0) {
                                            state.selectedHidden.removeAt(existingIndex)
                                        } else {
                                            state.selectedHidden.add(app)
                                        }
                                    }
                                    else -> null
                                },
                                onDone = when (flag) {
                                    Constants.AppDrawerFlag.EditFavorites -> {
                                        {
                                            viewModel.saveEditFavorites(state.selectedFavorites.toList())
                                            findNavController().popBackStack()
                                        }
                                    }
                                    Constants.AppDrawerFlag.EditHiddenApps -> {
                                        {
                                            viewModel.saveEditHiddenApps(state.selectedHidden.toList())
                                            findNavController().popBackStack()
                                        }
                                    }
                                    else -> null
                                },
                                selectedFavoritesCount = when (flag) {
                                    Constants.AppDrawerFlag.EditFavorites -> state.selectedFavorites.size
                                    Constants.AppDrawerFlag.EditHiddenApps -> state.selectedHidden.size
                                    else -> 0
                                },
                                maxFavorites = if (flag == Constants.AppDrawerFlag.EditFavorites) maxFavoritesState else 0,
                                isFavoriteChecked = when (flag) {
                                    Constants.AppDrawerFlag.EditFavorites -> { app ->
                                        state.selectedFavorites.any { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass }
                                    }
                                    Constants.AppDrawerFlag.EditHiddenApps -> { app ->
                                        state.selectedHidden.any { it.activityPackage == app.activityPackage && it.activityClass == app.activityClass }
                                    }
                                    else -> null
                                },
                                onTitleClick = when (flag) {
                                    Constants.AppDrawerFlag.EditFavorites -> {
                                        {
                                            EditModeHelper.showHomeAppsAndPagesOnly(context, fragment, prefs) {
                                                maxFavoritesState = prefs.homeAppsNum
                                            }
                                        }
                                    }
                                    Constants.AppDrawerFlag.HiddenApps -> {
                                        {
                                            findNavController().navigate(
                                                R.id.appsFragment,
                                                androidx.core.os.bundleOf("flag" to Constants.AppDrawerFlag.EditHiddenApps.toString())
                                            )
                                        }
                                    }
                                    else -> null
                                },
                                homePagesNum = prefs.homePagesNum
                            )

                            val azLetters = appsDrawer.azLetters

                            if (isCalculated && appsDrawer.appDrawerAzFilter && !state.showRenameOverlay && !state.isHiddenAppsMode && !state.isEditFavoritesMode && !state.isEditHiddenMode) {
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
                                            state.privateSpaceFilter = false
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
                                            if (finalLetter == "★") {
                                                setCurrentPageSafe(0)
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    dpadSelectedIndex = if (state.azFilterFocused) state.azFilterSelectedIndex else null,
                                    onDpadIndexChange = { index ->
                                        state.azFilterSelectedIndex = index
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
                                    textIslandsShape = appsDrawer.textIslandsShape,
                                    fontFamily = iconFontFamily,
                                    showPrivateSpace = appsDrawer.hasPrivateSpace,
                                    isPrivateSpaceActive = state.privateSpaceFilter,
                                    onPrivateSpaceTap = {
                                        try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                                        state.privateSpaceFilter = !state.privateSpaceFilter
                                        state.azFilterLetter = null
                                        setCurrentPageSafe(0)
                                    },
                                    onPrivateSpaceLongPress = {
                                        try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                                        try {
                                            com.github.gezimos.inkos.helper.utils.ProfileManager(requireContext())
                                                .togglePrivateSpaceLock(showToast = true, launchSettings = false)
                                        } catch (_: Exception) {}
                                        state.privateSpaceFilter = false
                                        state.azFilterLetter = null
                                        setCurrentPageSafe(0)
                                    }
                                )
                            }
                        }
                        if (EditModeHelper.isEditMode()) {
                            EditModeOverlay()
                        }
                    }
                }
            }
        }

        root.addView(composeView)
        return root
    }

    private fun handleAppClick(app: AppListItem) {
        if (flag == Constants.AppDrawerFlag.EditFavorites || flag == Constants.AppDrawerFlag.EditHiddenApps) return

        // Record launch for frequency boosting
        if (currentSearchQuery.isNotBlank()) {
            SearchHelper.recordLaunch(requireContext(), currentSearchQuery, app.activityPackage)
        }
        
        if (EditModeHelper.isEditMode()) {
            EditModeHelper.showAppDrawerSettings(requireContext(), this, prefs, { }, folderPickerLauncher)
            return
        }
        if (app.activityPackage.startsWith(Constants.INTERNAL_CONTACT_PREFIX)) {
            val contactId = app.activityPackage.removePrefix(Constants.INTERNAL_CONTACT_PREFIX)
            val lookupKey = com.github.gezimos.inkos.helper.ContactsHelper.getLookupKey(requireContext(), contactId) ?: contactId
            com.github.gezimos.inkos.helper.ContactsHelper.launchContactQuickActions(
                requireContext(),
                contactId,
                lookupKey
            )
            return
        }

        if (app.activityPackage == Constants.INTERNAL_WEB_SEARCH) {
            // Query is stored in activityClass
            val query = app.activityClass
            SearchHelper.launchWebSearch(requireContext(), query)
            return
        }

        if (app.activityPackage == Constants.INTERNAL_SETTINGS) {
            // Action string is stored in activityClass
            com.github.gezimos.inkos.helper.SettingsSearchHelper.launch(requireContext(), app.activityClass)
            return
        }

        if (app.activityPackage == Constants.INTERNAL_MUSIC) {
            // Content URI is stored in activityClass
            com.github.gezimos.inkos.helper.MusicSearchHelper.launch(requireContext(), app.activityClass)
            return
        }

        if (app.activityPackage == Constants.INTERNAL_FILES) {
            com.github.gezimos.inkos.helper.FileSearchHelper.launch(requireContext(), app.activityClass)
            return
        }

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
        
        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
        if (viewModel.isAppLocked(app)) {
            promptBiometric(
                onSuccess = {
                    if (appsRepo.launchSyntheticOrSystemApp(requireContext(), app.activityPackage, this, app.shortcutId)) return@promptBiometric
                    viewModel.launchApp(app)
                },
                title = "Authenticate to open ${app.label}"
            )
        } else {
            if (appsRepo.launchSyntheticOrSystemApp(requireContext(), app.activityPackage, this, app.shortcutId)) return
            viewModel.launchApp(app)
        }
    }

    private fun handleAppDelete(app: AppListItem) {
        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(requireContext().applicationContext as android.app.Application)
        if (appsRepo.isPinnedShortcut(app)) {
            val unpinned = appsRepo.unpinShortcut(app)
            if (unpinned) {
                showShortToast(getString(R.string.toast_shortcut_unpinned))
                viewModel.refreshAppListAfterUninstall(includeHiddenApps = false)
            } else {
                showShortToast(getString(R.string.toast_unpin_failed))
            }
            return
        }

        if (app.shortcutId != null) {
            val key = "${app.activityPackage}|${app.shortcutId}|${app.user}"
            val allShortcuts = try {
                com.github.gezimos.inkos.helper.getAllAppShortcuts(requireContext())
            } catch (_: Exception) { emptyList() }
            val effectiveSelected = if (!prefs.hasSelectedAppShortcutsBeenSet()) {
                com.github.gezimos.inkos.helper.computeDefaultShortcutSelection(allShortcuts, BuildConfig.APPLICATION_ID).toMutableSet()
            } else {
                prefs.selectedAppShortcuts.toMutableSet()
            }
            effectiveSelected.remove(key)
            viewModel.setSelectedAppShortcuts(effectiveSelected)
            viewModel.refreshAppListAfterUninstall(includeHiddenApps = false)
            showShortToast(getString(R.string.toast_shortcut_removed))
            return
        }

        if (requireContext().isSystemApp(app.activityPackage)) {
            showShortToast(getString(R.string.can_not_delete_system_apps))
        } else {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = "package:${app.activityPackage}".toUri()
                if (app.user != android.os.Process.myUserHandle()) {
                    putExtra(Intent.EXTRA_USER, app.user)
                }
            }
            pendingUninstallPackage = app.activityPackage
            uninstallLauncher.launch(intent)
        }
    }

    private fun handleRenameApp(app: AppListItem, newName: String) {
        if (newName == "RESET_TO_ORIGINAL") {
            viewModel.renameApp(app.activityPackage, "", flag, app.shortcutId)
        } else {
            viewModel.renameApp(app.activityPackage, newName, flag, app.shortcutId)
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
                viewModel.toggleAppLock(app)
                onSuccess()
            },
            title = if (viewModel.isAppLocked(app)) "Authenticate to unlock" else "Authenticate to lock"
        )
    }

    private fun handleInfoApp(app: AppListItem) {
        openAppInfo(requireContext(), app.user, app.activityPackage)
    }

    private fun promptBiometric(onSuccess: () -> Unit, title: String = "Authenticate") {
        val biometricManager = BiometricManager.from(requireContext())
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(), getString(R.string.toast_biometric_unavailable), Toast.LENGTH_SHORT).show()
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
        if (flag == Constants.AppDrawerFlag.SetHomeApp ||
            flag == Constants.AppDrawerFlag.SetSwipeLeft ||
            flag == Constants.AppDrawerFlag.SetSwipeRight ||
            flag == Constants.AppDrawerFlag.SetSwipeUp ||
            flag == Constants.AppDrawerFlag.SetSwipeDown ||
            flag == Constants.AppDrawerFlag.SetClickClock ||
            flag == Constants.AppDrawerFlag.SetClickDate ||
            flag == Constants.AppDrawerFlag.SetQuoteWidget ||
            flag == Constants.AppDrawerFlag.SetDoubleTap ||
            flag == Constants.AppDrawerFlag.EditFavorites ||
            flag == Constants.AppDrawerFlag.EditHiddenApps
        ) {
            EditModeHelper.exitEditMode()
        }
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
        try { hideKeyboard() } catch (_: Exception) {}
        super.onPause()
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        if (act.pageNavigationHandler != null) act.pageNavigationHandler = null
        try {
            viewModel.clearTypedCharReplay()
        } catch (_: Exception) {}
        act.suppressKeyForwarding = false
    }
}
