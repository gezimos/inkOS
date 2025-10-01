package com.github.gezimos.inkos.ui

import android.annotation.SuppressLint
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

    private var appPosition: Int = 0

    // Paging state
    private var currentPage = 0
    private var appsPerPage = 0
    private var totalPages = 1
    private var vibrator: Vibrator? = null

    private var selectedItemIndex = 0
    private var isInitializing = true

    private var appDrawerPrefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    // Uninstall launcher and package tracking
    private var pendingUninstallPackage: String? = null
    private val uninstallLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
            pendingUninstallPackage?.let {
                clearMeasurementCache()
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

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]

        return binding.root
    }

    private fun focusAdapterPosition(adapterPos: Int) {
        try {
            val pageStart = currentPage * appsPerPage
            val indexInPage = adapterPos - pageStart
            if (indexInPage < 0) return
            val rv = binding.recyclerView
            val vh = rv.findViewHolderForAdapterPosition(indexInPage)
            if (vh != null) {
                vh.itemView.requestFocus()
                try {
                    val title = vh.itemView.findViewById<View>(R.id.appTitle)
                    title?.requestFocus()
                } catch (_: Exception) {}
            } else {
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

    private fun moveSelectionUp() {
        if (selectedItemIndex > 0) {
            selectedItemIndex--
            focusSelectedItem()
        } else {
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

    private fun moveSelectionDown() {
        val currentPageAppCount = getCurrentPageAppCount()
        if (selectedItemIndex < currentPageAppCount - 1) {
            selectedItemIndex++
            focusSelectedItem()
        } else {
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

    private fun focusSelectedItem() {
        try {
            val rv = binding.recyclerView
            val vh = rv.findViewHolderForAdapterPosition(selectedItemIndex)
            if (vh != null) {
                vh.itemView.requestFocus()
                try {
                    val title = vh.itemView.findViewById<View>(R.id.appTitle)
                    title?.requestFocus()
                } catch (_: Exception) {}
            } else {
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
                appClickListener(viewModel, flag, appPosition)(selectedApp)
            }
        } catch (_: Exception) {}
    }

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

        var bottomInsetPx = 0
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainLayout) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        binding.mainLayout.post {
            val vPad = resources.getDimensionPixelSize(R.dimen.app_drawer_vertical_padding)
            binding.mainLayout.setPadding(0, vPad, 0, bottomInsetPx + vPad)
            binding.mainLayout.clipToPadding = false
        }

        val flagString = arguments?.getString("flag", AppDrawerFlag.LaunchApp.toString())
            ?: AppDrawerFlag.LaunchApp.toString()
        flag = AppDrawerFlag.valueOf(flagString)
        appPosition = arguments?.getInt("n", 0) ?: 0

        val includeHidden = flag == AppDrawerFlag.SetHomeApp || flag == AppDrawerFlag.HiddenApps
        viewModel.getAppList(includeHiddenApps = includeHidden, flag = flag)

        val alignmentPref = prefs.appDrawerAlignment
    val gravity = when (alignmentPref) {
        1 -> Gravity.CENTER
        2 -> Gravity.END
        else -> Gravity.START
    }

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
                appClickListener(viewModel, flag, appPosition),
                appDeleteListener(),
                this.appRenameListener(),
                appShowHideListener(),
                appInfoListener(),
                { keyCode, adapterPos ->
                    val absolutePos = currentPage * appsPerPage + adapterPos
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            val pageStart = currentPage * appsPerPage
                            val pageEnd = (pageStart + appsPerPage).coerceAtMost(fullAppsList.size) - 1
                            if (absolutePos >= pageEnd) {
                                if (currentPage < totalPages - 1) {
                                    currentPage++
                                    updatePagedList(fullAppsList, adapter)
                                    updatePageIndicator()
                                    vibratePaging()
                                    binding.recyclerView.post {
                                        focusAdapterPosition(currentPage * appsPerPage)
                                    }
                                    return@AppDrawerAdapter true
                                }
                            }
                            false
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
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
            val diffX = e2.x - (e1?.x ?: e2.x)
            // Only treat horizontal fling as back when the gesture STARTS near the left or right edge of the screen
            val startX = e1?.x ?: e2.x
            val screenWidth = resources.displayMetrics.widthPixels
            val edgeThresholdPx = (48 * density) // 48dp edge region
            if (kotlin.math.abs(diffX) > flingThreshold && kotlin.math.abs(velocityX) > flingVelocity) {
                if (startX <= edgeThresholdPx || startX >= (screenWidth - edgeThresholdPx)) {
                    try {
                        findNavController().popBackStack()
                    } catch (_: Exception) {}
                    vibratePaging()
                    return true
                }
            }
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

            var handledByRecycler = false
            try {
                val forwarded = MotionEvent.obtain(event)
                // Compute overlay (touchArea) location on screen so we can convert
                // overlay-local coordinates into RecyclerView-local coordinates.
                val overlayLocation = IntArray(2)
                try { v.getLocationOnScreen(overlayLocation) } catch (_: Exception) { overlayLocation[0] = 0; overlayLocation[1] = 0 }
                // Offset forwarded event by (overlayOnScreen - rvOnScreen) so forwarded.x == rawX - rvX
                forwarded.offsetLocation(
                    (overlayLocation[0] - rvLocation[0]).toFloat(),
                    (overlayLocation[1] - rvLocation[1]).toFloat()
                )
                handledByRecycler = try {
                    rv.dispatchTouchEvent(forwarded)
                } catch (_: IllegalArgumentException) {
                    // Some devices may still be unhappy; fall back below
                    false
                }
                forwarded.recycle()
            } catch (_: IllegalArgumentException) {
                // Worst-case fallback: send a simple ACTION_CANCEL to clear touch state
                try {
                    val cancel = MotionEvent.obtain(
                        event.downTime,
                        event.eventTime,
                        MotionEvent.ACTION_CANCEL,
                        0f,
                        0f,
                        0
                    )
                    rv.dispatchTouchEvent(cancel)
                    cancel.recycle()
                } catch (_: Exception) {}
                handledByRecycler = false
            }

            // Call performClick when touch is completed
            if (event.actionMasked == MotionEvent.ACTION_UP && !handledByRecycler) {
                @Suppress("ClickableViewAccessibility")
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

    private var fullAppsList: List<AppListItem> = emptyList()
    private var lastDisplayedPage = -1
    private var lastRecyclerHeight = 0
    private var lastAppTextSize = -1
    private var lastAppTextPadding = -1
    private var cachedPages: List<List<AppListItem>> = emptyList()

    private fun clearMeasurementCache() {
        lastRecyclerHeight = 0
        lastAppTextSize = -1
        lastAppTextPadding = -1
        cachedPages = emptyList()
    }

    private fun populateAppList(apps: List<AppListItem>, appAdapter: AppDrawerAdapter) {
        if (!isAdded || _binding == null) return
        binding.recyclerView.layoutAnimation = null
            fullAppsList = apps.sorted()
        cachedPages = emptyList()
        selectedItemIndex = 0
        
        val b = _binding ?: return
        b.recyclerView.post {
        b.recyclerView.post {
            val recyclerHeight = binding.recyclerView.height

            val prefs = Prefs(requireContext())
            val appTextSize = prefs.appDrawerSize
            val appTextPadding = prefs.appDrawerGap

            val needMeasure = (lastAppTextSize != appTextSize) || (lastAppTextPadding != appTextPadding) || (recyclerHeight != lastRecyclerHeight)

            var itemHeight = resources.getDimensionPixelSize(R.dimen.app_drawer_item_height)

            if (needMeasure && recyclerHeight > 0) {
                try {
                    val textPaint = android.text.TextPaint()
                    @Suppress("DEPRECATION")
                    val scaled = resources.displayMetrics.scaledDensity
                    textPaint.textSize = appTextSize * scaled
                    val fm = textPaint.fontMetrics
                    val textHeight = (Math.ceil((fm.descent - fm.ascent).toDouble()).toInt())
                    val total = textHeight + (2 * appTextPadding)
                    if (total > 0) itemHeight = total
                } catch (_: Exception) {
                    itemHeight = resources.getDimensionPixelSize(R.dimen.app_drawer_item_height)
                }

                lastAppTextSize = appTextSize
                lastAppTextPadding = appTextPadding
                lastRecyclerHeight = recyclerHeight

                // RecyclerView is constrained between guidelines (5%-95% of screen),
                // so its full height is available. Add small buffer (1% of height) to ensure
                // last item doesn't get clipped at bottom due to rounding or padding differences.
                val safeHeight = (recyclerHeight * 0.99).toInt()
                appsPerPage = if (itemHeight > 0) {
                    (safeHeight / itemHeight).coerceAtLeast(1)
                } else {
                    8 // fallback
                }

                try {
                    binding.recyclerView.setItemViewCacheSize(appsPerPage + 1)
                    binding.recyclerView.setHasFixedSize(true)
                } catch (_: Exception) {}
            }

            totalPages = ((fullAppsList.size + appsPerPage - 1) / appsPerPage).coerceAtLeast(1)
            if (currentPage >= totalPages) {
                currentPage = (totalPages - 1).coerceAtLeast(0)
            }
            lastDisplayedPage = -1

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
                    if (!isAdded || _binding == null) return@withContext
                    cachedPages = pages
                    updatePagedList(fullAppsList, appAdapter)
                    updatePageIndicator()
                }
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

        appAdapter.setPageAppsWithDiff(pageApps)
        lastDisplayedPage = currentPage

        if (binding.recyclerView.visibility != View.VISIBLE) binding.recyclerView.visibility = View.VISIBLE
        binding.listEmptyHint.visibility = if (pageApps.isEmpty()) View.VISIBLE else View.GONE
        
        // Ensure selected item index is within bounds for this page
        if (selectedItemIndex >= pageApps.size) {
            selectedItemIndex = kotlin.math.max(0, pageApps.size - 1)
        }
        
        if (flag == AppDrawerFlag.LaunchApp) {
            binding.recyclerView.post {
                focusSelectedItem()
            }
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

        val density = requireContext().resources.displayMetrics.density
        val swipeThreshold = (100 * density)
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
                    vibratePaging()
                    return true
                }
                return false
            }
        })

        val itemTouchListener = object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
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

        try { binding.recyclerView.removeOnItemTouchListener(itemTouchListener) } catch (_: Exception) {}
        binding.recyclerView.addOnItemTouchListener(itemTouchListener)

        binding.mainLayout.isFocusableInTouchMode = true
        binding.mainLayout.requestFocus()
        binding.mainLayout.setOnKeyListener { _, keyCode, event ->
            if (isInitializing) {
                return@setOnKeyListener true
            }
            
            val action = KeyMapperHelper.mapAppDrawerKey(prefs, keyCode, event)
            when (action) {
                KeyMapperHelper.AppDrawerKeyAction.PageUp -> {
                    return@setOnKeyListener false
                }
                KeyMapperHelper.AppDrawerKeyAction.PageDown -> {
                    return@setOnKeyListener false
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
        binding.recyclerView.layoutAnimation = null
        
        val delay = if (flag == AppDrawerFlag.SetHomeApp) 800 else 300
        binding.root.postDelayed({
            isInitializing = false
        }, delay.toLong())
    }

    override fun onResume() {
        super.onResume()
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        act.pageNavigationHandler = object : com.github.gezimos.inkos.MainActivity.PageNavigationHandler {
            override val handleDpadAsPage: Boolean = false

            override fun pageUp() {
                if (isInitializing) return
                if (currentPage > 0) {
                    currentPage--
                    updatePagedList(fullAppsList, adapter)
                    updatePageIndicator()
                    selectedItemIndex = 0
                    focusSelectedItem()
                    vibratePaging()
                }
            }

            override fun pageDown() {
                if (isInitializing) return
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
    }

    override fun onPause() {
        super.onPause()
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        if (act.pageNavigationHandler != null) act.pageNavigationHandler = null
    }

    private fun vibratePaging() {
        if (!::prefs.isInitialized) return
        if (!prefs.useVibrationForPaging) return
        try {
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
            }
        }

    private fun appRenameListener(): (String, String) -> Unit = { packageName, newName ->
        clearMeasurementCache()
        viewModel.renameApp(packageName, newName, flag)
    }


    private fun appShowHideListener(): (flag: AppDrawerFlag, appListItem: AppListItem) -> Unit =
        { flag, appModel ->
            clearMeasurementCache()
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
        try {
            if (::adapter.isInitialized) {
                adapter.cancelBackgroundWork()
                binding.recyclerView.adapter = null
            }
        } catch (_: Exception) {}
        
        try {
            appDrawerPrefListener?.let { prefs.sharedPrefs.unregisterOnSharedPreferenceChangeListener(it) }
            appDrawerPrefListener = null
        } catch (_: Exception) {}

        _binding = null
    }
}