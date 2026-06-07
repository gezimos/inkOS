package com.github.gezimos.inkos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager.LayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
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
import com.github.gezimos.inkos.helper.EditModeHelper
import com.github.gezimos.inkos.helper.hideNavigationBar
import com.github.gezimos.inkos.helper.isTablet
import com.github.gezimos.inkos.helper.showNavigationBar
import com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager
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
    private var einkHelper: EinkHelper? = null
    private var wasInBackground: Boolean = false
    var allowEdgeSwipeBack: Boolean = true
    private var backGesture: com.github.gezimos.inkos.helper.BackGesture? = null

    // --- Activity Result Launchers ---

    val defaultHomeLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        }

    val backupWriteLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.also { uri ->
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

    val backupReadLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.also { uri ->
                    val fileName = uri.lastPathSegment ?: ""
                    val isJsonExtension = fileName.endsWith(".json", ignoreCase = true) ||
                            applicationContext.contentResolver.getType(uri)
                                ?.contains("json") == true
                    if (!isJsonExtension) {
                        ComposeDialogManager(this, this).showErrorDialog(
                            this,
                            getString(R.string.error_invalid_file_title),
                            getString(R.string.error_invalid_file_message)
                        )
                        return@registerForActivityResult
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
                        val jsonObj = try {
                            org.json.JSONObject(string)
                        } catch (_: Exception) {
                            ComposeDialogManager(this, this).showErrorDialog(
                                this,
                                getString(R.string.error_invalid_file_title),
                                getString(R.string.error_invalid_file_message)
                            )
                            return@registerForActivityResult
                        }
                        val prefs = Prefs(applicationContext)
                        if (jsonObj.optString("type") == "inkos_theme") {
                            try {
                                prefs.loadThemeFromJson(string)
                                android.widget.Toast.makeText(this, getString(R.string.theme_imported), android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("ThemeImport", "Import failed", e)
                                ComposeDialogManager(this, this).showErrorDialog(this, "Import failed", e.message ?: "Unknown error")
                                return@registerForActivityResult
                            }
                        } else {
                            prefs.clear()
                            prefs.loadFromString(string)
                        }
                    }
                    startActivity(Intent.makeRestartActivityTask(this.intent?.component))
                }
            }
        }

    val themeBackupWriteLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.also { uri ->
                    try {
                        val prefs = Prefs(applicationContext)
                        val json = prefs.saveThemeToJson()
                        applicationContext.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        }
                        android.widget.Toast.makeText(this, getString(R.string.theme_exported), android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("ThemeExport", "Export failed", e)
                        ComposeDialogManager(this, this).showErrorDialog(this, "Export failed", e.message ?: "Unknown error")
                    }
                }
            }
        }

    val themeBackupReadLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.also { uri ->
                    try {
                        val prefs = Prefs(applicationContext)
                        val json = applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                            BufferedReader(InputStreamReader(input)).use { it.readText() }
                        } ?: throw IllegalArgumentException("Cannot read file")
                        prefs.loadThemeFromJson(json)
                        android.widget.Toast.makeText(this, getString(R.string.theme_imported), android.widget.Toast.LENGTH_SHORT).show()
                        startActivity(Intent.makeRestartActivityTask(this.intent?.component))
                    } catch (e: Exception) {
                        Log.e("ThemeImport", "Import failed", e)
                        ComposeDialogManager(this, this).showErrorDialog(this, "Import failed", e.message ?: "Unknown error")
                    }
                }
            }
        }

    companion object {
        private const val TAG = "MainActivity"
    }

    private fun isOnboarding(): Boolean {
        if (!::navController.isInitialized) return false
        val destId = navController.currentDestination?.id
        return destId == R.id.onboardingFragment
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!::navController.isInitialized) return
            val destId = navController.currentDestination?.id
            if (destId == R.id.onboardingFragment) return
            if (destId != R.id.mainFragment) {
                navController.popBackStack()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isOnboarding()) return true
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
                return super.onKeyDown(keyCode, event)
            }

            else -> {
                return super.onKeyDown(keyCode, event)
            }
        }
    }

    interface PageNavigationHandler {
        val handleDpadAsPage: Boolean
        fun pageUp()
        fun pageDown()
    }

    var pageNavigationHandler: PageNavigationHandler? = null

    interface FragmentKeyHandler {
        fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean
    }

    var fragmentKeyHandler: FragmentKeyHandler? = null

    var suppressKeyForwarding: Boolean = false

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isOnboarding()) return super.dispatchKeyEvent(event)

        // --- Fragment key handler ---
        val fragHandler = fragmentKeyHandler
        if (event.action == KeyEvent.ACTION_DOWN && fragHandler != null) {
            try {
                if (fragHandler.handleKeyEvent(event.keyCode, event)) return true
            } catch (_: Exception) {}
        }

        val handler = pageNavigationHandler
        if (handler != null) {
            // --- Volume keys for page navigation ---
            if (prefs.useVolumeKeysForPages) {
                val audioWidgetHelper = AudioWidgetHelper.getInstance(this)
                val currentMediaPlayer = audioWidgetHelper.getCurrentMediaPlayer()
                val isAudioPlaying = currentMediaPlayer?.isPlaying == true
                
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            if (!isAudioPlaying) {
                                handler.pageUp()
                            } else {
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
                                return super.dispatchKeyEvent(event)
                            }
                        }
                        return true
                    }
                }
            }

            // --- DPAD / PAGE keys ---
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

        val superHandled = super.dispatchKeyEvent(event)
        if (superHandled) return true

        // --- Forward unhandled keys to app drawer search ---
        try {
            if (event.action == KeyEvent.ACTION_DOWN && !suppressKeyForwarding && prefs.appDrawerSearchEnabled) {
                try {
                    val ch = com.github.gezimos.inkos.helper.SearchHelper.keyEventToChar(event)
                    if (ch != null) {
                        try { viewModel.emitTypedChar(ch) } catch (_: Exception) {}
                        try {
                            if (::navController.isInitialized && navController.currentDestination?.id == R.id.mainFragment) {
                                navController.navigate(
                                    R.id.action_mainFragment_to_appListFragment,
                                    androidx.core.os.bundleOf("searchMode" to 0)
                                )
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
        if (isOnboarding()) return
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("OPEN_SIMPLE_TRAY", false)) {
            navigateToSimpleTray()
            return
        }
        if (!::navController.isInitialized) return
        when (intent.action) {
            Constants.ACTION_OPEN_APP_DRAWER -> {
                try { navController.navigate(R.id.appListFragment) } catch (_: Exception) {}
                return
            }
            Constants.ACTION_OPEN_NOTIFICATIONS -> {
                try { navController.navigate(R.id.lettersFragment) } catch (_: Exception) {}
                return
            }
            Constants.ACTION_OPEN_SIMPLE_TRAY -> {
                try { navController.navigate(R.id.simpleTrayFragment) } catch (_: Exception) {}
                return
            }
            Constants.ACTION_OPEN_HUB -> {
                try { navController.navigate(R.id.hubFragment) } catch (_: Exception) {}
                return
            }
            Constants.ACTION_OPEN_RECENTS -> {
                try { navController.navigate(R.id.recentsFragment) } catch (_: Exception) {}
                return
            }
            Constants.ACTION_OPEN_SETTINGS -> {
                try { navController.navigate(R.id.settingsFragment) } catch (_: Exception) {}
                return
            }
        }
        if (intent.action == Intent.ACTION_MAIN) {
            backToHomeScreen()
        }
    }

    private fun navigateToSimpleTray() {
        if (!::navController.isInitialized) return
        try {
            navController.popBackStack(R.id.mainFragment, false)
            navController.navigate(R.id.simpleTrayFragment)
        } catch (_: Exception) {}
    }

    private fun backToHomeScreen() {
        if (!::navController.isInitialized) return
        navController.popBackStack(R.id.mainFragment, false)
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

        // --- Crash loop detection ---
        try {
            val crashPrefs = getSharedPreferences(CrashHandler.CRASH_LOOP_PREFS, MODE_PRIVATE)
            if (crashPrefs.getBoolean(CrashHandler.KEY_SAFE_MODE, false)) {
                crashPrefs.edit().remove(CrashHandler.KEY_SAFE_MODE).remove(CrashHandler.KEY_CRASH_TIMESTAMPS).apply()
                val intent = Intent(this, CrashReportActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                finish()
                return
            }
        } catch (_: Exception) {}

        // Register back pressed callback
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        try {
            VH.init(applicationContext, prefs)
        } catch (_: Exception) {}
        migration = Migration(this)
        migration.migratePreferencesOnVersionUpdate(prefs)

        if (EinkHelper.isMuditaKompakt()) {
            einkHelper = EinkHelper(packageName)
            lifecycle.addObserver(einkHelper!!)
            if (prefs.einkHelperMode != EinkHelper.MEINK_MODE_DISABLED) {
                einkHelper!!.setMeinkMode(prefs.einkHelperMode)
            }
        }

        // --- Centralized insets listener ---
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            if (EinkHelper.isMuditaKompakt()) {
                val imeVisible = insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
                einkHelper?.let { helper ->
                    val userMode = prefs.einkHelperMode
                    if (imeVisible && userMode != EinkHelper.MEINK_MODE_DISABLED && userMode != EinkHelper.MEINK_MODE_CONTRAST) {
                        helper.setMeinkMode(EinkHelper.MEINK_MODE_CONTRAST)
                    } else if (!imeVisible) {
                        helper.setMeinkMode(userMode)
                    }
                }
            }

            androidx.core.view.WindowInsetsCompat.Builder(insets).apply {
                if (!prefs.showStatusBar) {
                    setInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars(), androidx.core.graphics.Insets.NONE)
                }
                if (!prefs.showNavigationBar) {
                    setInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars(), androidx.core.graphics.Insets.NONE)
                }
            }.build()
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        val themeMode = when (prefs.appTheme) {
            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isDarkTheme = prefs.getResolvedTheme() == Constants.Theme.Dark
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
        windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme

        // --- System bar colors ---
        try {
            val rawBg = try { com.github.gezimos.inkos.style.resolveThemeColors(this).second } catch (_: Exception) { prefs.backgroundColor }
            val opaqueBg = if ((rawBg ushr 24) == 0) rawBg or (0xFF shl 24) else rawBg

            try {
                @Suppress("DEPRECATION")
                try {
                    window.statusBarColor = opaqueBg
                    window.navigationBarColor = opaqueBg
                } catch (_: Exception) {}

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    try { window.navigationBarDividerColor = opaqueBg } catch (_: Exception) {}
                }

            } catch (_: Exception) {}
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


        navController = this.findNavController(R.id.nav_host_fragment)

        if (prefs.firstOpen) {
            if (com.github.gezimos.inkos.helper.device.DeviceHelper.isEinkDevice()) {
                prefs.appTheme = Constants.Theme.Light
            }
            navController.navigate(R.id.onboardingFragment)
        }



        try {
            backGesture = com.github.gezimos.inkos.helper.BackGesture(
                binding.root,
                navController,
                prefs
            ) { allowEdgeSwipeBack && prefs.edgeSwipeBackEnabled }
        } catch (_: Exception) {}
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.getAppList(includeHiddenApps = true)
        
        try {
            val appsRepository = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
            appsRepository.registerPackageReceiver(this)
        } catch (_: Exception) {
            // Silently ignore registration errors
        }
        
        setupOrientation()
        // --- E-Ink refresh on navigation ---
        try {
            val skipIds = emptySet<Int>()
            navController.addOnDestinationChangedListener { _: NavController, destination: androidx.navigation.NavDestination, _: Bundle? ->
                try {
                    if (destination.id in skipIds) return@addOnDestinationChangedListener
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

    }

    override fun onResume() {
        super.onResume()

        // --- Home-only E-Ink refresh on resume ---
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

        window.decorView.postDelayed({
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
                EditModeHelper.exitEditMode()
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

        prefs.onSystemThemeChanged()

        // --- Update AppCompat night mode for System theme ---
        if (prefs.appTheme == Constants.Theme.System) {
            val resolvedTheme = prefs.getResolvedTheme()
            val themeMode = when (resolvedTheme) {
                Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
                Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(themeMode)
            
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            val isDarkTheme = resolvedTheme == Constants.Theme.Dark
            windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
            
            try {
                val rawBg = try { com.github.gezimos.inkos.style.resolveThemeColors(this).second } catch (_: Exception) { prefs.backgroundColor }
                val opaqueBg = if ((rawBg ushr 24) == 0) rawBg or (0xFF shl 24) else rawBg
                window.statusBarColor = opaqueBg
                window.navigationBarColor = opaqueBg
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    @Suppress("DEPRECATION")
                    try { window.navigationBarDividerColor = opaqueBg } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        // --- Update fragment backgrounds ---
        try {
            val fragments = supportFragmentManager.fragments
            for (frag in fragments) {
                if (frag is com.github.gezimos.inkos.ui.HomeFragmentCompose) continue

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
            einkHelper?.let {
                val currentMode = it.getCurrentMeinkMode()
                if (currentMode != -1) {
                    window.decorView.postDelayed({
                        it.setMeinkMode(currentMode)
                    }, 200)
                }
            }

            // --- Re-apply bar visibility ---
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
            
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        einkHelper?.let {
            lifecycle.removeObserver(it)
        }
        try {
            val appsRepository = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
            appsRepository.unregisterPackageReceiver(this)
        } catch (_: Exception) {
            // Silently ignore unregistration errors
        }
    }

    @Suppress("unused")
    fun setMeinkMode(mode: Int) {
        einkHelper?.setMeinkMode(mode)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
    }


    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this)) return
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}