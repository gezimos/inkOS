package com.github.gezimos.inkos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager.LayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Migration
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.databinding.ActivityMainBinding
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.hideNavigationBar
import com.github.gezimos.inkos.helper.isTablet
import com.github.gezimos.inkos.helper.showNavigationBar
import com.github.gezimos.inkos.ui.compose.GuideScreen
import com.github.gezimos.inkos.ui.compose.OnboardingScreen
import com.github.gezimos.inkos.ui.dialogs.DialogManager
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import com.github.gezimos.inkos.helper.utils.VibrationHelper as VH

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var migration: Migration
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private var isOnboarding = false
    private var isGuide = false

    
    // Eink helper instance
    private var einkHelper: EinkHelper? = null
    // Track whether Activity was backgrounded so we can detect returns
    private var wasInBackground: Boolean = false
    // Edge-swipe back support (app-wide). Fragments can opt-out via `allowEdgeSwipeBack`.
    var allowEdgeSwipeBack: Boolean = true
    private var backGesture: com.github.gezimos.inkos.helper.BackGesture? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isOnboarding) {
                return
            }
            if (navController.currentDestination?.id != R.id.mainFragment) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isOnboarding || isGuide) {
            return true
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                when (navController.currentDestination?.id) {
                    R.id.mainFragment -> {
                        this.findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.action_mainFragment_to_appListFragment)
                        true
                    }

                    else -> false
                }
            }

            KeyEvent.KEYCODE_ESCAPE -> {
                backToHomeScreen()
                true
            }

            KeyEvent.KEYCODE_HOME -> {
                com.github.gezimos.inkos.ui.HomeFragmentCompose.sendGoToFirstPageSignal()
                backToHomeScreen()
                true
            }

            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Let fragments handle volume keys directly through their own key listeners
                return super.onKeyDown(keyCode, event)
            }

            else -> {
                // Default: leave non-system keys to the normal dispatch path.
                // Home-specific printable/gesture shortcuts are handled by the
                // visible fragment (e.g. HomeFragmentCompose) via the
                // Activity.fragmentKeyHandler or Compose onPreviewKeyEvent.
                return super.onKeyDown(keyCode, event)
            }
        }
    }

    /**
     * Interface for fragments to handle page navigation when Activity forwards key events.
     * Fragments that provide page navigation should register an instance in the Activity
     * so key events (volume, page keys, optional DPAD) are handled regardless of view focus.
     */
    interface PageNavigationHandler {
        /**
         * When true the Activity will forward DPAD_UP/DPAD_DOWN and PAGE_UP/PAGE_DOWN keys as page navigation.
         * When false only volume keys (if enabled) will be forwarded.
         */
        val handleDpadAsPage: Boolean
        fun pageUp()
        fun pageDown()
    }

    // Currently registered page navigation handler (null when none). Fragments must set/unset
    // this in onResume/onPause to receive forwarded key events only while visible.
    var pageNavigationHandler: PageNavigationHandler? = null

    /**
     * Optional fragment-level key handler for non-navigation keys (e.g. keypad gestures).
     * If set, Activity will forward key events to it before attempting page navigation so
     * fragments receive keys regardless of view focus.
     */
    interface FragmentKeyHandler {
        fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean
    }

    var fragmentKeyHandler: FragmentKeyHandler? = null

    // When true, Activity will NOT intercept keys for search forwarding (e.g. when rename overlay is visible)
    var suppressKeyForwarding: Boolean = false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If onboarding/guide or no handler, use default dispatch
        if (isOnboarding || isGuide) return super.dispatchKeyEvent(event)

        // First give the visible fragment a chance to handle arbitrary keys
        // (HomeFragment uses this for volume keys and special gesture keys 6/7/8/9)
        val fragHandler = fragmentKeyHandler
        if (event.action == KeyEvent.ACTION_DOWN && fragHandler != null) {
            try {
                if (fragHandler.handleKeyEvent(event.keyCode, event)) return true
            } catch (_: Exception) {}
        }

        val handler = pageNavigationHandler
        if (handler != null) {
            // Volume keys: if user enabled pref, consume ALL volume key actions
            // (ACTION_DOWN, ACTION_UP, ACTION_MULTIPLE) so long-presses/repeats
            // don't fall through to the system and show the volume dialog.
            if (prefs.useVolumeKeysForPages) {
                // Check if audio is playing before handling volume keys for navigation
                val audioWidgetHelper = AudioWidgetHelper.getInstance(this)
                val currentMediaPlayer = audioWidgetHelper.getCurrentMediaPlayer()
                val isAudioPlaying = currentMediaPlayer?.isPlaying == true
                
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            if (!isAudioPlaying) {
                                handler.pageUp()
                            } else {
                                // Let system handle volume control when audio is playing
                                return super.dispatchKeyEvent(event)
                            }
                        }
                        return true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            if (!isAudioPlaying) {
                                handler.pageDown()
                            } else {
                                // Let system handle volume control when audio is playing
                                return super.dispatchKeyEvent(event)
                            }
                        }
                        return true
                    }
                }
            }

            // Only handle DPAD / PAGE keys on ACTION_DOWN to mirror existing behaviour
            // (Used by NotificationsFragment for DPAD-as-page navigation)
            if (event.action == KeyEvent.ACTION_DOWN && handler.handleDpadAsPage) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP -> {
                        handler.pageUp()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> {
                        handler.pageDown()
                        return true
                    }
                }
            }
        }

        // Give the default dispatch a chance first (fragments/Compose can consume keys).
        val superHandled = super.dispatchKeyEvent(event)
        if (superHandled) return true

        // If unhandled and forwarding allowed, convert to printable char and forward
        // to ViewModel so the app drawer can receive it when ready. Also navigate
        // from Home to the app drawer when typing.
        try {
            if (event.action == KeyEvent.ACTION_DOWN && !suppressKeyForwarding) {
                try {
                    val ch = com.github.gezimos.inkos.ui.compose.SearchHelper.keyEventToChar(event)
                    if (ch != null) {
                        try { viewModel.emitTypedChar(ch) } catch (_: Exception) {}
                        // If we're on the Home screen, open the apps drawer so search appears
                        try {
                            if (::navController.isInitialized && navController.currentDestination?.id == R.id.mainFragment) {
                                navController.navigate(R.id.action_mainFragment_to_appListFragment)
                            }
                        } catch (_: Exception) {}
                        return true
                    }
                } catch (_: Exception) {}

                if (event.keyCode == KeyEvent.KEYCODE_DEL) {
                    try { viewModel.emitBackspaceEvent() } catch (_: Exception) {}
                    try {
                        if (::navController.isInitialized && navController.currentDestination?.id == R.id.mainFragment) {
                            navController.navigate(R.id.action_mainFragment_to_appListFragment)
                        }
                    } catch (_: Exception) {}
                    return true
                }
            }
        } catch (_: Exception) {}

        return false
    }

    override fun onNewIntent(intent: Intent) {
        if (isOnboarding || isGuide) return
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) {
            backToHomeScreen()
        }
    }

    private fun backToHomeScreen() {
        if (!::navController.isInitialized) return
        navController.popBackStack(R.id.mainFragment, false)
        // Trigger an E-Ink refresh when returning to home from other apps
        try {
            com.github.gezimos.inkos.helper.utils.EinkRefreshHelper.refreshEink(
                this,
                prefs,
                null,
                prefs.einkRefreshDelay,
                useActivityRoot = true
            )
        } catch (_: Exception) {}
    }

    @RequiresPermission(anyOf = ["android.permission.READ_WALLPAPER_INTERNAL", Manifest.permission.MANAGE_EXTERNAL_STORAGE])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register back pressed callback
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        // Initialize centralized vibration helper
        try {
            VH.init(applicationContext, prefs)
        } catch (_: Exception) {}
        migration = Migration(this)

        // Initialize EinkHelper only if enabled
        if (prefs.einkHelperEnabled) {
            einkHelper = EinkHelper()
            lifecycle.addObserver(einkHelper!!)
            einkHelper!!.initializeMeinkService()
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        if (prefs.firstOpen) {
            isOnboarding = true
            val composeView = ComposeView(this)
            setContentView(composeView)
            composeView.setContent {
                OnboardingScreen.Show(
                    onFinish = {
                        viewModel.setFirstOpen(false)
                        isOnboarding = false
                        // After onboarding, check if guide should be shown
                        if (!prefs.guideShown) {
                            isGuide = true
                            composeView.setContent {
                                GuideScreen.Show(
                                    onFinish = {
                                        prefs.guideShown = true
                                        isGuide = false
                                        // Add delay to ensure theme changes are fully applied before recreate
                                        window.decorView.postDelayed({
                                            recreate()
                                        }, 150)
                                    }
                                )
                            }
                        } else {
                            // Add delay to ensure theme changes are fully applied before recreate
                            window.decorView.postDelayed({
                                recreate()
                            }, 150)
                        }
                    }
                )
            }
            return
        }
        
        // Check if guide should be shown (onboarding completed but guide not shown)
        if (!prefs.guideShown) {
            isGuide = true
            val composeView = ComposeView(this)
            setContentView(composeView)
            composeView.setContent {
                GuideScreen.Show(
                    onFinish = {
                        prefs.guideShown = true
                        isGuide = false
                        // Add delay to ensure theme changes are fully applied before recreate
                        window.decorView.postDelayed({
                            recreate()
                        }, 150)
                    }
                )
            }
            return
        }

        val themeMode = if (prefs.appTheme == Constants.Theme.Light) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(themeMode)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isDarkTheme = prefs.appTheme == Constants.Theme.Dark
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
        windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme

        // Ensure status/navigation bars use the app background color (opaque) so they
        // don't appear as translucent black/gray in light mode. Some OEMs or
        // system settings render these bars translucent when content is drawn
        // behind them; explicitly set opaque colors to match the app background.
        try {
            val rawBg = try { com.github.gezimos.inkos.style.resolveThemeColors(this).second } catch (_: Exception) { prefs.backgroundColor }
            // Force full opacity for system bars to avoid translucency artifacts
            val opaqueBg = if ((rawBg ushr 24) == 0) rawBg or (0xFF shl 24) else rawBg

            // Use modern WindowInsetsController for appearance and avoid legacy translucent flags.
            try {
                // Set system bar colors (keep opaque). These assignments may be deprecated
                // on some toolchains; scope suppression narrowly so lint is satisfied.
                @Suppress("DEPRECATION")
                try {
                    window.statusBarColor = opaqueBg
                    window.navigationBarColor = opaqueBg
                } catch (_: Exception) {}

                // On API levels that support a nav divider color, set it as well.
                // Note: navigationBarDividerColor is deprecated in API 35+, but still functional on older APIs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    try { window.navigationBarDividerColor = opaqueBg } catch (_: Exception) {}
                }

                // Rely on WindowCompat.getInsetsController(...) (already set above)
                // to control light/dark appearance and behavior. Avoid using
                // deprecated contrast-enforcement flags.
            } catch (_: Exception) {}
        } catch (_: Exception) {}

        if (prefs.showNavigationBar) {
            showNavigationBar(this)
        } else {
            hideNavigationBar(this)
        }

        migration.migratePreferencesOnVersionUpdate(prefs)

        // Wallpaper is handled automatically by android:windowShowWallpaper in styles.xml
        // No manual processing needed - system handles it efficiently

        navController = this.findNavController(R.id.nav_host_fragment)
        // Initialize BackGesture helper to centralize edge-swipe detection.
        try {
            backGesture = com.github.gezimos.inkos.helper.BackGesture(
                binding.root,
                navController,
                prefs
            ) { allowEdgeSwipeBack && prefs.edgeSwipeBackEnabled }
        } catch (_: Exception) {}
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.getAppList(includeHiddenApps = true)
        
        // Register package install receiver to detect new app installations
        // This is more efficient than refreshing on every fragment resume
        try {
            val appsRepository = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
            appsRepository.registerPackageReceiver(this)
        } catch (_: Exception) {
            // Silently ignore registration errors
        }
        
        setupOrientation()
        // Central E-Ink refresh on navigation: attach to decorView so it covers Compose roots.
        // Uses `refreshEink` so user preference (`einkRefreshEnabled`) is respected.
        try {
            val skipIds = emptySet<Int>()
            navController.addOnDestinationChangedListener { _: NavController, destination: androidx.navigation.NavDestination, _: Bundle? ->
                try {
                    if (destination.id in skipIds) return@addOnDestinationChangedListener
                    // Only perform auto refresh on navigation when Auto mode enabled and Home-only mode is NOT active
                    if (prefs.einkRefreshEnabled && !prefs.einkRefreshHomeButtonOnly) {
                        com.github.gezimos.inkos.helper.utils.EinkRefreshHelper.refreshEink(
                            this,
                            prefs,
                            null,
                            prefs.einkRefreshDelay,
                            useActivityRoot = true
                        )
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        window.addFlags(LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // Don't automatically open notification listener settings
        // Only request permission when user explicitly enables notifications in onboarding
    }

    override fun onResume() {
        super.onResume()

        // If we are returning from background and Home-only refresh is enabled,
        // trigger a single refresh when arriving at the Home destination.
        try {
            if (wasInBackground) {
                wasInBackground = false
                try {
                    if (prefs.einkRefreshHomeButtonOnly) {
                        if (::navController.isInitialized && navController.currentDestination?.id == R.id.mainFragment) {
                            com.github.gezimos.inkos.helper.utils.EinkRefreshHelper.refreshEink(
                                this,
                                prefs,
                                null,
                                prefs.einkRefreshDelay,
                                useActivityRoot = true
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        if (prefs.showNavigationBar) {
            showNavigationBar(this)
        } else {
            hideNavigationBar(this)
        }

        if (prefs.showStatusBar) {
            com.github.gezimos.inkos.helper.showStatusBar(this)
        } else {
            com.github.gezimos.inkos.helper.hideStatusBar(this)
        }

        // Wallpaper updates automatically via windowShowWallpaper - no manual refresh needed

        window.decorView.postDelayed({
            // Defensive: avoid touching window/UI when activity is finishing/destroyed/stopped
            if (isFinishing || isDestroyed) return@postDelayed
            try {
                window.decorView.requestFocus()
                val imm =
                    getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
                currentFocus?.let { view ->
                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ignored exception during onResume UI operations", e)
            }

            // Re-apply MeInk mode when resuming
            einkHelper?.let {
                val currentMode = it.getCurrentMeinkMode()
                if (currentMode != -1) {
                    it.setMeinkMode(currentMode)
                }
            }
        }, 100)
    }

    override fun onPause() {
        super.onPause()
        try {
            wasInBackground = true
        } catch (_: Exception) {}
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        try {
            if (backGesture?.onTouchEvent(ev) == true) return true
        } catch (_: Exception) {}
        return super.dispatchTouchEvent(ev)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed, reinitializing MeInk service")
        einkHelper?.reinitializeMeinkService()

        // If the user chose System theme, update window/system bar appearances
        // and notify viewmodel so Views/Compose that rely on resolved colors
        // can refresh without requiring a full process restart.
        // No-op: System theme removed; fragment backgrounds and viewmodel state
        // will be updated below for general configuration changes.

        // Additionally, walk fragments and update their root view backgrounds so
        // view-based fragments that set background once at creation will reflect
        // the new system-derived theme color immediately.
        try {
            val fragments = supportFragmentManager.fragments
            for (frag in fragments) {
                // Skip HomeFragmentCompose as it handles its own background/transparency
                if (frag is com.github.gezimos.inkos.ui.HomeFragmentCompose) continue

                // Skip NavHostFragment root view to avoid blocking wallpaper with opaque background
                // (NavHostFragment is just a container; we only want to color its children)
                if (frag !is androidx.navigation.fragment.NavHostFragment) {
                    try {
                        val fview = frag.view
                        if (fview != null) {
                            val bg = try { com.github.gezimos.inkos.style.resolveThemeColors(frag.requireContext()).second } catch (_: Exception) { null }
                            if (bg != null) {
                                try { fview.setBackgroundColor(bg) } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Also update any child fragments
                try {
                    val childFrags = frag.childFragmentManager.fragments
                    for (cf in childFrags) {
                        // Skip nested HomeFragmentCompose
                        if (cf is com.github.gezimos.inkos.ui.HomeFragmentCompose) continue

                        val cv = cf.view
                        if (cv != null) {
                            val cbg = try { com.github.gezimos.inkos.style.resolveThemeColors(cf.requireContext()).second } catch (_: Exception) { null }
                            if (cbg != null) try { cv.setBackgroundColor(cbg) } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val homeFragment =
                fragment?.childFragmentManager?.fragments?.find { it is com.github.gezimos.inkos.ui.HomeFragmentCompose } as? com.github.gezimos.inkos.ui.HomeFragmentCompose
            if (homeFragment != null && com.github.gezimos.inkos.ui.HomeFragmentCompose.isHomeVisible) {
                homeFragment.onWindowFocusGained()
            }
            // Re-apply MeInk mode when gaining focus
            einkHelper?.let {
                val currentMode = it.getCurrentMeinkMode()
                if (currentMode != -1) {
                    window.decorView.postDelayed({
                        it.setMeinkMode(currentMode)
                    }, 200)
                }
            }

            // Defensive: when the window regains focus system bars may become
            // visible due to user taps or gesture navigation — re-apply the
            // user's preference for hiding/showing the status and nav bars.
            window.decorView.postDelayed({
                try {
                    if (prefs.showNavigationBar) {
                        showNavigationBar(this)
                    } else {
                        hideNavigationBar(this)
                    }

                    if (prefs.showStatusBar) {
                        com.github.gezimos.inkos.helper.showStatusBar(this)
                    } else {
                        com.github.gezimos.inkos.helper.hideStatusBar(this)
                    }
                } catch (_: Exception) {}
            }, 150)
            
            // Wallpaper updates automatically via windowShowWallpaper - no manual refresh needed
        }
    }
    

    override fun onDestroy() {
        super.onDestroy()
        einkHelper?.let {
            lifecycle.removeObserver(it)
            it.cleanup()
        }
        // Unregister package install receiver
        try {
            val appsRepository = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
            appsRepository.unregisterPackageReceiver(this)
        } catch (_: Exception) {
            // Silently ignore unregistration errors
        }
    }

    /**
     * Public method to set MeInk mode from anywhere in the app
     */
    @Suppress("unused")
    fun setMeinkMode(mode: Int) {
        einkHelper?.setMeinkMode(mode)
    }

    // Fragments may toggle the public `allowEdgeSwipeBack` property directly when needed.

    private fun isNotificationServiceEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // No permission results to handle - we only use Notification Listener Service
        // which is enabled via system settings, not runtime permissions
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.REQUEST_SET_DEFAULT_HOME -> {
                // Returned from default-home chooser. No action required here;
                // UI updates are handled elsewhere (no-op removed from ViewModel).
            }

            Constants.BACKUP_READ -> {
                data?.data?.also { uri ->
                    val fileName = uri.lastPathSegment ?: ""
                    val isJsonExtension = fileName.endsWith(".json", ignoreCase = true) ||
                            applicationContext.contentResolver.getType(uri)
                                ?.contains("json") == true
                    if (!isJsonExtension) {
                        DialogManager(this, this).showErrorDialog(
                            this,
                            getString(R.string.error_invalid_file_title),
                            getString(R.string.error_invalid_file_message)
                        )
                        return
                    }
                    applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
                        val stringBuilder = StringBuilder()
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                        }
                        val string = stringBuilder.toString()
                        try {
                            org.json.JSONObject(string)
                        } catch (_: Exception) {
                            DialogManager(this, this).showErrorDialog(
                                this,
                                getString(R.string.error_invalid_file_title),
                                getString(R.string.error_invalid_file_message)
                            )
                            return
                        }
                        val prefs = Prefs(applicationContext)
                        prefs.clear()
                        prefs.loadFromString(string)
                    }
                }
                startActivity(Intent.makeRestartActivityTask(this.intent?.component))
            }

            Constants.BACKUP_WRITE -> {
                data?.data?.also { uri ->
                    applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use { file ->
                        FileOutputStream(file.fileDescriptor).use { stream ->
                            val prefs = Prefs(applicationContext).saveToString()
                            stream.channel.truncate(0)
                            stream.write(prefs.toByteArray())
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this)) return
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}