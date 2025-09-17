package com.github.gezimos.inkos

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.drawable.toDrawable
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Migration
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.databinding.ActivityMainBinding
import com.github.gezimos.inkos.helper.isTablet
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.ui.dialogs.DialogManager
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var migration: Migration
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private var isOnboarding = false

    // Eink helper instance
    private var einkHelper: EinkHelper? = null

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
        if (isOnboarding) {
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

            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P,
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_Z -> {
                when (navController.currentDestination?.id) {
                    R.id.mainFragment -> {
                        val bundle = Bundle()
                        bundle.putInt("letterKeyCode", keyCode)
                        this.findNavController(R.id.nav_host_fragment)
                            .navigate(R.id.action_mainFragment_to_appListFragment, bundle)
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
                com.github.gezimos.inkos.ui.HomeFragment.sendGoToFirstPageSignal()
                backToHomeScreen()
                true
            }

            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                // Let fragments handle volume keys directly through their own key listeners
                return super.onKeyDown(keyCode, event)
            }

            else -> super.onKeyDown(keyCode, event)
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If onboarding or no handler, use default dispatch
        if (isOnboarding) return super.dispatchKeyEvent(event)

        // First give the visible fragment a chance to handle arbitrary keys
        val fragHandler = fragmentKeyHandler
        if (event.action == KeyEvent.ACTION_DOWN && fragHandler != null) {
            try {
                if (fragHandler.handleKeyEvent(event.keyCode, event)) return true
            } catch (_: Exception) {}
        }

        val handler = pageNavigationHandler
        // Only handle ACTION_DOWN to mirror existing behaviour
        if (event.action == KeyEvent.ACTION_DOWN && handler != null) {
            // Volume keys always considered for page navigation if user enabled the pref
            if (prefs.useVolumeKeysForPages) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        handler.pageUp()
                        return true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        handler.pageDown()
                        return true
                    }
                }
            }

            // Forward DPAD / PAGE keys only if the fragment opted in
            if (handler.handleDpadAsPage) {
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

        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        if (isOnboarding) return
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) {
            backToHomeScreen()
        }
    }

    private fun backToHomeScreen() {
        if (!::navController.isInitialized) return
        navController.popBackStack(R.id.mainFragment, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register back pressed callback
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        migration = Migration(this)

        // Initialize EinkHelper only if enabled
        if (prefs.einkHelperEnabled) {
            einkHelper = EinkHelper()
            lifecycle.addObserver(einkHelper!!)
            einkHelper!!.initializeMeinkService()
        }

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))

        if (prefs.firstOpen) {
            isOnboarding = true
            val composeView = androidx.compose.ui.platform.ComposeView(this)
            setContentView(composeView)
            composeView.setContent {
                com.github.gezimos.inkos.ui.compose.OnboardingScreen.Show(
                    onFinish = {
                        prefs.firstOpen = false
                        isOnboarding = false
                        // Add delay to ensure theme changes are fully applied before recreate
                        window.decorView.postDelayed({
                            recreate()
                        }, 150)
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                requestPermissions(
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    1001
                                )
                            }
                        } else {
                            val areEnabled =
                                NotificationManagerCompat.from(this).areNotificationsEnabled()
                            if (!areEnabled) {
                                val intent =
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                    }
                                startActivity(intent)
                            }
                        }
                    }
                )
            }
            return
        }

        val themeMode = when (prefs.appTheme) {
            Constants.Theme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.Theme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            Constants.Theme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(themeMode)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isDarkTheme = when (prefs.appTheme) {
            Constants.Theme.Dark -> true
            Constants.Theme.Light -> false
            Constants.Theme.System -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
        windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme

        if (prefs.showNavigationBar) {
            com.github.gezimos.inkos.helper.showNavigationBar(this)
        } else {
            com.github.gezimos.inkos.helper.hideNavigationBar(this)
        }

        migration.migratePreferencesOnVersionUpdate(prefs)

        window.setBackgroundDrawable(prefs.backgroundColor.toDrawable())

        navController = this.findNavController(R.id.nav_host_fragment)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        viewModel.getAppList(includeHiddenApps = true)
        setupOrientation()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)

        if (!isNotificationServiceEnabled()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (prefs.showNavigationBar) {
            com.github.gezimos.inkos.helper.showNavigationBar(this)
        } else {
            com.github.gezimos.inkos.helper.hideNavigationBar(this)
        }

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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed, reinitializing MeInk service")
        einkHelper?.reinitializeMeinkService()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val homeFragment =
                fragment?.childFragmentManager?.fragments?.find { it is com.github.gezimos.inkos.ui.HomeFragment } as? com.github.gezimos.inkos.ui.HomeFragment
            if (homeFragment != null && com.github.gezimos.inkos.ui.HomeFragment.isHomeVisible) {
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        einkHelper?.let {
            lifecycle.removeObserver(it)
            it.cleanup()
        }
    }

    /**
     * Public method to set MeInk mode from anywhere in the app
     */
    @Suppress("unused")
    fun setMeinkMode(mode: Int) {
        einkHelper?.setMeinkMode(mode)
    }

    private fun isNotificationServiceEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constants.REQUEST_SET_DEFAULT_HOME -> {
                val isDefault = isinkosDefault(this)
                if (isDefault) {
                    viewModel.setDefaultLauncher(false)
                } else {
                    viewModel.setDefaultLauncher(true)
                }
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