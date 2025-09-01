/**
 * The view for the list of all the installed applications.
 */

package com.github.gezimos.inkos.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.GestureDetector
import android.content.SharedPreferences
import android.view.MotionEvent
import android.os.Vibrator
import android.content.Context.VIBRATOR_SERVICE
import android.widget.ImageView
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.github.gezimos.common.isSystemApp
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.databinding.FragmentAppDrawerBinding
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.KeyMapperHelper
import com.github.gezimos.inkos.helper.SystemShortcutHelper

class AppDrawerFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var flag: AppDrawerFlag

    // Paging state
    private var currentPage = 0
    private var appsPerPage = 0
    private var totalPages = 1
    private var vibrator: Vibrator? = null

    // Item selection state for keyboard navigation
    private var selectedItemIndex = 0 // Index within the current page

    // Listener for app-drawer-specific preference changes
    private var appDrawerPrefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    // --- Add uninstall launcher and package tracking ---
    private var pendingUninstallPackage: String? = null
    private val uninstallLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (pendingUninstallPackage != null) {
                viewModel.refreshAppListAfterUninstall(includeHiddenApps = false)
                pendingUninstallPackage = null
            }
        }

    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
    @Suppress("DEPRECATION")
    vibrator = requireContext().getSystemService(VIBRATOR_SERVICE) as? Vibrator

        // Initialize viewModel
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        return binding.root
    }

    // Focus the view at the given absolute adapter position (within fullAppsList).
    private fun focusAdapterPosition(adapterPos: Int) {
        try {
            // Since RecyclerView holds only the current page, compute the index within current page
            val pageStart = currentPage * appsPerPage
            val indexInPage = adapterPos - pageStart
            if (indexInPage < 0) return
            val rv = binding.recyclerView
            val vh = rv.findViewHolderForAdapterPosition(indexInPage)
            if (vh != null) {
                vh.itemView.requestFocus()
                // Try to focus the title TextView if possible
                try {
                    val title = vh.itemView.findViewById<View>(R.id.appTitle)
                    title?.requestFocus()
                } catch (_: Exception) {}
            } else {
                // If not attached yet, scrollToPosition then post focus
                rv.scrollToPosition(indexInPage)
                rv.post {
                    val vh2 = rv.findViewHolderForAdapterPosition(indexInPage)
                    vh2?.itemView?.requestFocus()
                    try {
                        val title = vh2?.itemView?.findViewById<View>(R.id.appTitle)
                        title?.requestFocus()
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    // Move selection up within the current page
    private fun moveSelectionUp() {
        if (selectedItemIndex > 0) {
            selectedItemIndex--
            focusSelectedItem()
        } else {
            // If at top of page, go to previous page (last item)
            if (currentPage > 0) {
                currentPage--
                updatePagedList(fullAppsList, adapter)
                updatePageIndicator()
                selectedItemIndex = kotlin.math.max(0, getCurrentPageAppCount() - 1)
                focusSelectedItem()
                vibratePaging()
            }
        }
    }

    // Move selection down within the current page
    private fun moveSelectionDown() {
        val currentPageAppCount = getCurrentPageAppCount()
        if (selectedItemIndex < currentPageAppCount - 1) {
            selectedItemIndex++
            focusSelectedItem()
        } else {
            // If at bottom of page, go to next page (first item)
            if (currentPage < totalPages - 1) {
                currentPage++
                updatePagedList(fullAppsList, adapter)
                updatePageIndicator()
                selectedItemIndex = 0
                focusSelectedItem()
                vibratePaging()
            }
        }
    }

    // Focus the currently selected item
    private fun focusSelectedItem() {
        try {
            val rv = binding.recyclerView
            val vh = rv.findViewHolderForAdapterPosition(selectedItemIndex)
            if (vh != null) {
                vh.itemView.requestFocus()
                // Try to focus the title TextView if possible
                try {
                    val title = vh.itemView.findViewById<View>(R.id.appTitle)
                    title?.requestFocus()
                } catch (_: Exception) {}
            } else {
                // If not attached yet, scrollToPosition then post focus
                rv.scrollToPosition(selectedItemIndex)
                rv.post {
                    val vh2 = rv.findViewHolderForAdapterPosition(selectedItemIndex)
                    vh2?.itemView?.requestFocus()
                    try {
                        val title = vh2?.itemView?.findViewById<View>(R.id.appTitle)
                        title?.requestFocus()
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    // Get the number of apps on the current page
    private fun getCurrentPageAppCount(): Int {
        val pageApps: List<AppListItem> = if (cachedPages.isNotEmpty()) {
            if (currentPage in cachedPages.indices) cachedPages[currentPage] else emptyList()
        } else {
            val startIndex = currentPage * appsPerPage
            val endIndex = kotlin.math.min(startIndex + appsPerPage, fullAppsList.size)
            if (startIndex < fullAppsList.size) fullAppsList.subList(startIndex, endIndex) else emptyList()
        }
        return pageApps.size
    }

    // Select the currently focused item
    private fun selectCurrentItem() {
        try {
            val pageApps: List<AppListItem> = if (cachedPages.isNotEmpty()) {
                if (currentPage in cachedPages.indices) cachedPages[currentPage] else emptyList()
            } else {
                val startIndex = currentPage * appsPerPage
                val endIndex = kotlin.math.min(startIndex + appsPerPage, fullAppsList.size)
                if (startIndex < fullAppsList.size) fullAppsList.subList(startIndex, endIndex) else emptyList()
            }
            
            if (selectedItemIndex in pageApps.indices) {
                val selectedApp = pageApps[selectedItemIndex]
                appClickListener(viewModel, flag, 0)(selectedApp)
            }
        } catch (_: Exception) {}
    }

    // Long press the currently focused item
    private fun longPressCurrentItem() {
        try {
            val rv = binding.recyclerView
            val vh = rv.findViewHolderForAdapterPosition(selectedItemIndex)
            vh?.itemView?.performLongClick()
        } catch (_: Exception) {}
    }

    @SuppressLint("RtlHardcoded")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (prefs.firstSettingsOpen) {
            prefs.firstSettingsOpen = false
        }

        arguments?.getInt("letterKeyCode", -1)

        val backgroundColor = getHexForOpacity(prefs)
        binding.mainLayout.setBackgroundColor(backgroundColor)

        // Set up window insets listener for navigation bar padding
        var bottomInsetPx = 0
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLayout) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Apply bottom padding to prevent content from going under navigation bar
        // and limit the drawer content to 90% of the available height so
        // prefs-driven padding won't clip app names.
        binding.mainLayout.post {
            val vPad = resources.getDimensionPixelSize(R.dimen.app_drawer_vertical_padding)
            binding.mainLayout.setPadding(0, vPad, 0, bottomInsetPx + vPad)
            binding.mainLayout.clipToPadding = false

            // If the root is a ConstraintLayout, the XML guidelines keep
            // the recyclerView/touchArea centered at 90% height; skip runtime
            // resizing to avoid fighting the layout. Otherwise, apply the
            // fallback 90% sizing so older layouts still behave.
            try {
                // mainLayout is always ConstraintLayout based on XML
                // ConstraintLayout handles centering via XML; nothing to do.
            } catch (_: Exception) {}
        }

        val flagString = arguments?.getString("flag", AppDrawerFlag.LaunchApp.toString())
            ?: AppDrawerFlag.LaunchApp.toString()
        flag = AppDrawerFlag.valueOf(flagString)
        val n = arguments?.getInt("n", 0) ?: 0

        // Include hidden apps only for SetHomeApp flag or HiddenApps flag
        val includeHidden = flag == AppDrawerFlag.SetHomeApp || flag == AppDrawerFlag.HiddenApps
        viewModel.getAppList(includeHiddenApps = includeHidden, flag = flag)

    // No drawer button in layout; navigation handled via other UI actions.

    // Align app names based on app-drawer-specific preference
    val alignmentPref = prefs.appDrawerAlignment
    val gravity = when (alignmentPref) {
        1 -> Gravity.CENTER
        2 -> Gravity.END
        else -> Gravity.START
    }

    // Position page indicator opposite to app name alignment: when names are
    // right-aligned (2), place the pager on the left (horizontalBias=0f).
    try {
        val pager = binding.appDrawerPager
        val lp = pager.layoutParams
        if (lp is androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
            lp.horizontalBias = if (alignmentPref == 2) 0f else 1f
            pager.layoutParams = lp
        }
    } catch (_: Exception) {}

        val appAdapter = context?.let {
            AppDrawerAdapter(
                it,
                flag,
                gravity,
                appClickListener(viewModel, flag, n),
                appDeleteListener(),
                this.appRenameListener(),
                appShowHideListener(),
                appInfoListener(),
                // key navigation listener: return true if handled
                { keyCode, adapterPos ->
                    // adapterPos here is page-relative (0..pageSize-1). Convert to absolute index.
                    val absolutePos = currentPage * appsPerPage + adapterPos
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            // If this is the last visible item in the current page,
                            // move to next page (if available) and focus first item.
                            val pageStart = currentPage * appsPerPage
                            val pageEnd = (pageStart + appsPerPage).coerceAtMost(fullAppsList.size) - 1
                            if (absolutePos >= pageEnd) {
                                if (currentPage < totalPages - 1) {
                                    currentPage++
                                    updatePagedList(fullAppsList, adapter)
                                    updatePageIndicator()
                                    vibratePaging()
                                    // focus first item of new page after layout applied
                                    binding.recyclerView.post {
                                        focusAdapterPosition(currentPage * appsPerPage)
                                    }
                                    return@AppDrawerAdapter true
                                }
                            }
                            false
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            // If this is the first visible item in the current page,
                            // move to previous page (if available) and focus last item.
                            val pageStart = currentPage * appsPerPage
                            if (absolutePos == pageStart) {
                                if (currentPage > 0) {
                                    currentPage--
                                    updatePagedList(fullAppsList, adapter)
                                    updatePageIndicator()
                                    vibratePaging()
                                    // focus last item of new page after layout applied
                                    binding.recyclerView.post {
                                        val newPageStart = currentPage * appsPerPage
                                        val newPageEnd = (newPageStart + appsPerPage).coerceAtMost(fullAppsList.size) - 1
                                        focusAdapterPosition(newPageEnd)
                                    }
                                    return@AppDrawerAdapter true
                                }
                            }
                            false
                        }

                        else -> false
                    }
                }
            )
        }
        if (appAdapter != null) {
            adapter = appAdapter
        }

        binding.listEmptyHint.typeface = prefs.appsFont.getFont(requireContext())
        if (appAdapter != null) {
            initViewModel(flag, viewModel, appAdapter)
        }

        // Observe runtime changes to app text size or padding and recompute pages
        try {
            viewModel.appSize.observe(viewLifecycleOwner) { _ ->
                if (this::adapter.isInitialized) populateAppList(fullAppsList, adapter)
            }
            viewModel.textPaddingSize.observe(viewLifecycleOwner) { _ ->
                if (this::adapter.isInitialized) populateAppList(fullAppsList, adapter)
            }
        } catch (_: Exception) {}

        // Register SharedPreferences listener for app-drawer-specific keys
        try {
            val shared = prefs.sharedPrefs
            appDrawerPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "APP_DRAWER_SIZE" || key == "APP_DRAWER_GAP" || key == "APP_DRAWER_ALIGNMENT") {
                    if (this::adapter.isInitialized) populateAppList(fullAppsList, adapter)
                }
                if (key == "APP_DRAWER_ALIGNMENT") {
                    // Update page indicator placement to opposite side when alignment changes
                    try {
                        val newAlignment = prefs.appDrawerAlignment
                        val pager = binding.appDrawerPager
                        val lp = pager.layoutParams
                        if (lp is androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
                            lp.horizontalBias = if (newAlignment == 2) 0f else 1f
                            pager.layoutParams = lp
                        }
                    } catch (_: Exception) {}
                }
            }
            appDrawerPrefListener?.let { shared.registerOnSharedPreferenceChangeListener(it) }
        } catch (_: Exception) {}

    // Use a LayoutManager that disables vertical scrolling so a "page"
    // can't be scrolled internally when font sizes or item heights grow.
    binding.recyclerView.layoutManager = object : LinearLayoutManager(requireContext()) {
        override fun canScrollVertically(): Boolean = false
    }
    binding.recyclerView.adapter = appAdapter
    // Disable RecyclerView item animator to prevent DiffUtil from animating
    // item moves/changes — we prefer instant page swaps for the drawer.
    binding.recyclerView.itemAnimator = null
    // Keep the RecyclerView invisible until the first paged list is computed
    // to avoid a flash where all apps appear then split into pages.
    binding.recyclerView.visibility = View.INVISIBLE
    // Disable all scrolling and animations
    binding.recyclerView.isNestedScrollingEnabled = false
    binding.recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
    binding.recyclerView.layoutAnimation = null
    // touchArea is a visual overlay used for swipe gestures and page UI.
    // We'll let it sit on top (bringToFront) but implement an explicit
    // onTouch handler that only consumes vertical flings (paging). All
    // other touch events (taps, long-press) are forwarded to the
    // RecyclerView so item clicks and context menus work as expected.
    binding.touchArea.bringToFront()
    binding.touchArea.isClickable = true
    binding.touchArea.isFocusable = false

    // Create a small gesture detector used only to detect vertical flings
    // for page navigation. It sets a flag when a fling is detected and
    // performs the page change immediately.
    val density = requireContext().resources.displayMetrics.density
    // Lowered thresholds so vertical swipes are easier to trigger.
    val flingThreshold = (48 * density)
    val flingVelocity = 600
    val flingDetected = java.util.concurrent.atomic.AtomicBoolean(false)

    val overlayDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val diffY = e2.y - (e1?.y ?: e2.y)
            if (kotlin.math.abs(diffY) > flingThreshold && kotlin.math.abs(velocityY) > flingVelocity) {
                flingDetected.set(true)
                if (diffY < 0) {
                    if (currentPage < totalPages - 1) currentPage++
                } else {
                    if (currentPage > 0) currentPage--
                }
                updatePagedList(fullAppsList, adapter)
                updatePageIndicator()
                // Provide haptic feedback when the page changes (user-configurable)
                vibratePaging()
                // Clear RecyclerView's touch state so the next tap registers
                // immediately (sends an ACTION_CANCEL with coordinates
                // translated into RecyclerView's local space).
                try {
                    val rv = binding.recyclerView
                    val rvLoc = IntArray(2)
                    rv.getLocationOnScreen(rvLoc)
                    val rawX = if (e2.rawX.isNaN()) e2.x else e2.rawX
                    val rawY = if (e2.rawY.isNaN()) e2.y else e2.rawY
                    val cancel = MotionEvent.obtain(
                        e2.downTime,
                        e2.eventTime,
                        MotionEvent.ACTION_CANCEL,
                        rawX - rvLoc[0],
                        rawY - rvLoc[1],
                        0
                    )
                    rv.dispatchTouchEvent(cancel)
                    cancel.recycle()
                } catch (_: Exception) {}

                // Reset the fling flag so further taps are forwarded normally
                flingDetected.set(false)
                return true
            }
            return false
        }
    })

    binding.touchArea.setOnTouchListener { v, event ->
        // Let detector inspect the event first to find flings
        overlayDetector.onTouchEvent(event)

        if (flingDetected.get()) {
            // We handled a fling — consume the event and reset flag on UP/CANCEL.
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                flingDetected.set(false)
            }
            true
        } else {
            // Forward the MotionEvent to the RecyclerView so taps/long-press
            // are received by items. Translate coordinates from the overlay
            // (screen) to the RecyclerView local coordinates.
            val rv = binding.recyclerView
            val rvLocation = IntArray(2)
            rv.getLocationOnScreen(rvLocation)

            val rawX = event.rawX
            val rawY = event.rawY
            val translatedX = rawX - rvLocation[0]
            val translatedY = rawY - rvLocation[1]

            val forwarded = MotionEvent.obtain(
                event.downTime,
                event.eventTime,
                event.action,
                translatedX,
                translatedY,
                event.metaState
            )
            val handledByRecycler = rv.dispatchTouchEvent(forwarded)
            forwarded.recycle()

            // Call performClick when touch is completed
            if (event.actionMasked == MotionEvent.ACTION_UP && !handledByRecycler) {
                v.performClick()
            }

            // If recycler handled it, return true to indicate we forwarded
            // and it's handled. Otherwise return false to allow normal
            // propagation.
            handledByRecycler
        }
    }

        binding.listEmptyHint.text =
            applyTextColor(getString(R.string.drawer_list_empty_hint), prefs.appColor)

        // Paging: swipe and volume key listeners
        setupPagingListeners()
    }

    private fun applyTextColor(text: String, color: Int): SpannableString {
        val spannableString = SpannableString(text)
        spannableString.setSpan(
            ForegroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableString
    }

    private fun initViewModel(
        flag: AppDrawerFlag,
        viewModel: MainViewModel,
        appAdapter: AppDrawerAdapter
    ) {
        viewModel.hiddenApps.observe(viewLifecycleOwner, Observer {
            if (flag != AppDrawerFlag.HiddenApps) return@Observer
            it?.let { appList ->
                // Merge hidden system shortcuts with regular hidden apps
                val prefs = Prefs(requireContext())
                val hiddenSystemShortcuts = SystemShortcutHelper.getFilteredSystemShortcuts(
                    prefs,
                    includeHidden = false,
                    onlyHidden = true
                )
                val mergedList = appList.toMutableList().apply { addAll(hiddenSystemShortcuts) }
                binding.listEmptyHint.visibility =
                    if (mergedList.isEmpty()) View.VISIBLE else View.GONE
                populateAppList(mergedList, appAdapter)
            }
        })

        viewModel.appList.observe(viewLifecycleOwner, Observer {
            if (flag == AppDrawerFlag.HiddenApps) return@Observer
            if (it == appAdapter.appsList) return@Observer
            it?.let { appList ->
                binding.listEmptyHint.visibility =
                    if (appList.isEmpty()) View.VISIBLE else View.GONE
                populateAppList(appList, appAdapter)
            }
        })
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    // Store the full app list for paging
    private var fullAppsList: List<AppListItem> = emptyList()
    // Cache the last displayed page to avoid redundant updates
    private var lastDisplayedPage: Int = -1
    // Cache the last measured recycler height so we don't recalc appsPerPage unnecessarily
    private var lastRecyclerHeight: Int = 0
    // Cache prefs-derived sizing values so we can detect when they change
    private var lastAppTextSize: Int = -1
    private var lastAppTextPadding: Int = -1
    // Pre-sliced pages cache to avoid repeated subList allocations
    private var cachedPages: List<List<AppListItem>> = emptyList()

    private fun populateAppList(apps: List<AppListItem>, appAdapter: AppDrawerAdapter) {
    // If view is destroyed, avoid scheduling any work that touches binding
    if (!isAdded || _binding == null) return
    binding.recyclerView.layoutAnimation = null
    // Ensure the full app list is sorted alphabetically by custom label (if present)
    // or by the normal label. Adapter-side sorting exists, but paging is computed
    // from the fragment's `fullAppsList`, so we must sort here to ensure pages
    // (including HiddenApps mode) are alphabetically ordered.
    fullAppsList = apps.sortedWith(compareBy { it.customLabel.ifEmpty { it.label }.lowercase() })
    
    // Reset selection to the first item when app list changes
    selectedItemIndex = 0
    // Use a local reference to binding to reduce nullable access inside lambdas
    val b = _binding ?: return
    b.recyclerView.post {
            val recyclerHeight = binding.recyclerView.height
            val margin = resources.getDimensionPixelSize(R.dimen.app_drawer_vertical_padding)

            // Read prefs-derived visual values that affect item height (app-drawer specific)
            val prefs = Prefs(requireContext())
            val appTextSize = prefs.appDrawerSize
            val appTextPadding = prefs.appDrawerGap

            // If these values changed since last measurement, force re-measure
            val needMeasure = (lastAppTextSize != appTextSize) || (lastAppTextPadding != appTextPadding) || (recyclerHeight != lastRecyclerHeight)

            var itemHeight = resources.getDimensionPixelSize(R.dimen.app_drawer_item_height)

            if (needMeasure && recyclerHeight > 0) {
                // Estimate item height using TextPaint and font metrics instead of inflating a view.
                try {
                    val textPaint = android.text.TextPaint()
                    // textSize in prefs is provided as 'sp' numeric; set in pixels via scaledDensity
                    @Suppress("DEPRECATION")
                    val scaled = resources.displayMetrics.scaledDensity
                    textPaint.textSize = appTextSize * scaled
                    val fm = textPaint.fontMetrics
                    val textHeight = (Math.ceil((fm.descent - fm.ascent).toDouble()).toInt())
                    // Add vertical padding from prefs (assumed px)
                    val total = textHeight + (2 * appTextPadding)
                    if (total > 0) itemHeight = total
                } catch (_: Exception) {
                    itemHeight = resources.getDimensionPixelSize(R.dimen.app_drawer_item_height)
                }

                // Update cached prefs values
                lastAppTextSize = appTextSize
                lastAppTextPadding = appTextPadding
                lastRecyclerHeight = recyclerHeight

                appsPerPage = if (itemHeight > 0) {
                    ((recyclerHeight - 2 * margin) / itemHeight).coerceAtLeast(1)
                } else {
                    8 // fallback
                }

                // Configure RecyclerView caching for faster page swaps
                try {
                    binding.recyclerView.setItemViewCacheSize(appsPerPage + 1)
                    // If item heights are stable, enable fixed size optimizations
                    binding.recyclerView.setHasFixedSize(true)
                } catch (_: Exception) {}
            }

            totalPages = ((fullAppsList.size + appsPerPage - 1) / appsPerPage).coerceAtLeast(1)
            currentPage = 0
            // Force update the displayed page after a full list change
            lastDisplayedPage = -1

            // Compute page slices off the UI thread to avoid stalls on large app lists
            // Use the viewLifecycleOwner's scope so work is cancelled when view is destroyed.
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val pages = if (appsPerPage > 0 && fullAppsList.isNotEmpty()) {
                    (0 until totalPages).map { pageIndex ->
                        val start = pageIndex * appsPerPage
                        val end = (start + appsPerPage).coerceAtMost(fullAppsList.size)
                        fullAppsList.subList(start, end)
                    }
                } else {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    // UI may have been destroyed while computing pages
                    if (!isAdded || _binding == null) return@withContext
                    cachedPages = pages
                    updatePagedList(fullAppsList, appAdapter)
                    updatePageIndicator()
                }
            }
        }
    }

    private fun updatePagedList(apps: List<AppListItem>, appAdapter: AppDrawerAdapter) {
        val pageApps: List<AppListItem> = if (cachedPages.isNotEmpty()) {
            if (currentPage in cachedPages.indices) cachedPages[currentPage] else emptyList()
        } else {
            val startIdx = currentPage * appsPerPage
            val endIdx = (startIdx + appsPerPage).coerceAtMost(apps.size)
            if (apps.isNotEmpty()) apps.subList(startIdx, endIdx) else emptyList()
        }
        // Avoid reapplying the same page repeatedly
        if (currentPage == lastDisplayedPage) {
            // Still ensure list-empty hint/visibility are correct
            binding.listEmptyHint.visibility = if (pageApps.isEmpty()) View.VISIBLE else View.GONE
            if (binding.recyclerView.visibility != View.VISIBLE) binding.recyclerView.visibility = View.VISIBLE
            return
        }

    // Use DiffUtil-backed update to minimize UI work
    appAdapter.setPageAppsWithDiff(pageApps)
        lastDisplayedPage = currentPage

        // Reveal the RecyclerView once the first page has been applied.
        if (binding.recyclerView.visibility != View.VISIBLE) binding.recyclerView.visibility = View.VISIBLE
        binding.listEmptyHint.visibility = if (pageApps.isEmpty()) View.VISIBLE else View.GONE
        
        // Ensure selected item index is within bounds for this page
        if (selectedItemIndex >= pageApps.size) {
            selectedItemIndex = kotlin.math.max(0, pageApps.size - 1)
        }
        
        // Focus the selected item after a brief delay to ensure RecyclerView is ready
        binding.recyclerView.post {
            focusSelectedItem()
        }
    }

    private fun updatePageIndicator() {
        val pager = binding.appDrawerPager
        if (!prefs.appDrawerPager) {
            pager.removeAllViews()
            pager.visibility = View.GONE
            return
        }
        pager.removeAllViews()
        if (totalPages <= 1) {
            pager.visibility = View.GONE
            return
        }
        pager.visibility = View.VISIBLE
        val sizeInDp = 12
        val density = requireContext().resources.displayMetrics.density
        val sizeInPx = (sizeInDp * density).toInt()
        val spacingInPx = (12 * density).toInt()
        for (pageIndex in 0 until totalPages) {
            val imageView = ImageView(requireContext())
            val drawableRes = if (pageIndex == currentPage) R.drawable.ic_current_page else R.drawable.ic_new_page
            val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)?.apply {
                val colorFilterColor = PorterDuffColorFilter(prefs.appColor, PorterDuff.Mode.SRC_IN)
                colorFilter = colorFilterColor
            }
            imageView.setImageDrawable(drawable)
            val params = ViewGroup.MarginLayoutParams(sizeInPx, sizeInPx).apply {
                topMargin = if (pageIndex > 0) spacingInPx else 0
            }
            imageView.layoutParams = params
            pager.addView(imageView)
        }
    }

    private fun setupPagingListeners() {
    // Install an OnItemTouchListener on the RecyclerView to detect
    // vertical fling gestures and perform paging. The listener only
    // intercepts when a fling is detected (flingFlag=true), so normal
    // taps/long-press gestures are handled by RecyclerView / ViewHolders.
        val density = requireContext().resources.displayMetrics.density
        val swipeThreshold = (100 * density) // tuned threshold
        val swipeVelocityThreshold = 800

        val flingFlag = java.util.concurrent.atomic.AtomicBoolean(false)

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val diffY = e2.y - (e1?.y ?: e2.y)
                if (kotlin.math.abs(diffY) > swipeThreshold && kotlin.math.abs(velocityY) > swipeVelocityThreshold) {
                    flingFlag.set(true)
                    if (diffY < 0) {
                        if (currentPage < totalPages - 1) currentPage++
                    } else {
                        if (currentPage > 0) currentPage--
                    }
                    updatePagedList(fullAppsList, adapter)
                    updatePageIndicator()
                    // Haptic feedback on page change
                    vibratePaging()
                    return true
                }
                return false
            }
        })

        val itemTouchListener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Feed the detector so it can set flingFlag when a fling is detected.
                gestureDetector.onTouchEvent(e)

                return when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> false
                    MotionEvent.ACTION_MOVE -> flingFlag.get()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val consumed = flingFlag.get()
                        flingFlag.set(false)
                        consumed
                    }
                    else -> false
                }
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        }

        // Remove previous listeners of same type to avoid duplicates
        try { binding.recyclerView.removeOnItemTouchListener(itemTouchListener) } catch (_: Exception) {}
        binding.recyclerView.addOnItemTouchListener(itemTouchListener)

        // Move volume key listener to main layout to avoid conflicts with touchArea overlay
        binding.mainLayout.isFocusableInTouchMode = true
        binding.mainLayout.requestFocus()
        binding.mainLayout.setOnKeyListener { _, keyCode, event ->
            val action = KeyMapperHelper.mapAppDrawerKey(prefs, keyCode, event)
            when (action) {
                KeyMapperHelper.AppDrawerKeyAction.PageUp -> {
                    if (currentPage > 0) {
                        currentPage--
                        updatePagedList(fullAppsList, adapter)
                        updatePageIndicator()
                        selectedItemIndex = 0 // Reset selection to first item on new page
                        focusSelectedItem()
                        vibratePaging()
                    }
                    true
                }
                KeyMapperHelper.AppDrawerKeyAction.PageDown -> {
                    if (currentPage < totalPages - 1) {
                        currentPage++
                        updatePagedList(fullAppsList, adapter)
                        updatePageIndicator()
                        selectedItemIndex = 0 // Reset selection to first item on new page
                        focusSelectedItem()
                        vibratePaging()
                    }
                    true
                }
                KeyMapperHelper.AppDrawerKeyAction.MoveSelectionUp -> {
                    moveSelectionUp()
                    true
                }
                KeyMapperHelper.AppDrawerKeyAction.MoveSelectionDown -> {
                    moveSelectionDown()
                    true
                }
                KeyMapperHelper.AppDrawerKeyAction.SelectItem -> {
                    selectCurrentItem()
                    true
                }
                KeyMapperHelper.AppDrawerKeyAction.LongPressItem -> {
                    longPressCurrentItem()
                    true
                }
                else -> false
            }
        }
        // Ensure zero animations
        binding.recyclerView.layoutAnimation = null
    }

    private fun vibratePaging() {
        if (!::prefs.isInitialized) return
        if (!prefs.useVibrationForPaging) return
        try {
            // Use VibrationEffect (API 26+)
            val effect = android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(effect)
        } catch (_: Exception) {}
    }

    private fun appClickListener(
        viewModel: MainViewModel,
        flag: AppDrawerFlag,
        n: Int = 0
    ): (appListItem: AppListItem) -> Unit =
        { appModel ->
            when (flag) {
                AppDrawerFlag.SetSwipeLeft -> {
                    prefs.appSwipeLeft = appModel
                    prefs.swipeLeftAction = Action.OpenApp
                    findNavController().popBackStack()
                }

                AppDrawerFlag.SetSwipeRight -> {
                    prefs.appSwipeRight = appModel
                    prefs.swipeRightAction = Action.OpenApp
                    findNavController().popBackStack()
                }

                AppDrawerFlag.SetClickClock -> {
                    prefs.appClickClock = appModel
                    prefs.clickClockAction = Action.OpenApp
                    findNavController().popBackStack()
                }

                AppDrawerFlag.SetQuoteWidget -> {
                    prefs.appQuoteWidget = appModel
                    prefs.quoteAction = Action.OpenApp
                    findNavController().popBackStack()
                }

                else -> {
                    viewModel.selectedApp(this, appModel, flag, n)
                }
            }
        }

    private fun appDeleteListener(): (appListItem: AppListItem) -> Unit =
        { appModel ->
            if (requireContext().isSystemApp(appModel.activityPackage))
                showShortToast(getString(R.string.can_not_delete_system_apps))
            else {
                val appPackage = appModel.activityPackage
                val intent = Intent(Intent.ACTION_DELETE)
                intent.data = "package:$appPackage".toUri()
                pendingUninstallPackage = appPackage
                uninstallLauncher.launch(intent)
                // Do NOT refresh here; refresh will happen in the result callback
            }
        }

    private fun appRenameListener(): (String, String) -> Unit = { packageName, newName ->
        viewModel.renameApp(packageName, newName, flag)
        @Suppress("NotifyDataSetChanged")
        adapter.notifyDataSetChanged()
    }


    private fun appShowHideListener(): (flag: AppDrawerFlag, appListItem: AppListItem) -> Unit =
        { flag, appModel ->
            viewModel.hideOrShowApp(flag, appModel)
            if (flag == AppDrawerFlag.HiddenApps && Prefs(requireContext()).hiddenApps.isEmpty()) {
                findNavController().popBackStack()
            }
        }

    private fun appInfoListener(): (appListItem: AppListItem) -> Unit =
        { appModel ->
            openAppInfo(
                requireContext(),
                appModel.user,
                appModel.activityPackage
            )
            findNavController().popBackStack(R.id.mainFragment, false)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any running background work tied to the adapter so it won't
        // try to touch views after the view is destroyed. The adapter's
        // instance-scoped job ensures cancelling only affects this adapter.
        try {
            if (this::adapter.isInitialized) {
                adapter.cancelBackgroundWork()
                // Detach adapter from RecyclerView to avoid leaks.
                try { binding.recyclerView.adapter = null } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        // Unregister SharedPreferences listener
        try {
            val shared = prefs.sharedPrefs
            appDrawerPrefListener?.let { shared.unregisterOnSharedPreferenceChangeListener(it) }
            appDrawerPrefListener = null
        } catch (_: Exception) {}

        // Clear binding reference to avoid leaking the view.
        _binding = null
    }
}