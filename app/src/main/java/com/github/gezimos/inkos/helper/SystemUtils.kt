package com.github.gezimos.inkos.helper

import android.app.Activity
import android.app.UiModeManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.openAccessibilitySettings
import com.github.gezimos.common.showLongToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.services.ActionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Get synthetic apps (App Drawer, Notifications, Empty Space, System Shortcuts) based on context
 */
fun getSyntheticApps(
    prefs: Prefs,
    flag: Constants.AppDrawerFlag?,
    includeHiddenApps: Boolean = false
): List<AppListItem> {
    val syntheticApps = mutableListOf<AppListItem>()

    // Add internal synthetic apps (App Drawer, Notifications)
    if (flag == Constants.AppDrawerFlag.SetHomeApp ||
        flag == Constants.AppDrawerFlag.LaunchApp ||
        flag == Constants.AppDrawerFlag.HiddenApps ||
        flag == null
    ) {

        syntheticApps.addAll(getInternalSyntheticApps(prefs, flag, includeHiddenApps))
    }

    // Add Empty Space for SetHomeApp context only
    if (flag == Constants.AppDrawerFlag.SetHomeApp) {
        syntheticApps.add(getEmptySpaceApp())
    }

    // Add system shortcuts if enabled
    if (prefs.systemShortcutsEnabled &&
        (flag == Constants.AppDrawerFlag.SetHomeApp ||
                flag == Constants.AppDrawerFlag.LaunchApp ||
                flag == Constants.AppDrawerFlag.HiddenApps ||
                flag == null)
    ) {

        syntheticApps.addAll(getSystemShortcutsForContext(prefs, flag, includeHiddenApps))
    }

    return syntheticApps
}

/**
 * Get internal synthetic apps (App Drawer, Notifications)
 */
private fun getInternalSyntheticApps(
    prefs: Prefs,
    flag: Constants.AppDrawerFlag?,
    includeHiddenApps: Boolean
): List<AppListItem> {
    val apps = mutableListOf<AppListItem>()
    val hiddenApps = prefs.hiddenApps

    // App Drawer synthetic app
    val appDrawerPackage = "com.inkos.internal.app_drawer"
    val isAppDrawerHidden = hiddenApps.contains("${appDrawerPackage}|${Process.myUserHandle()}")

    if (shouldIncludeSyntheticApp(flag, isAppDrawerHidden, includeHiddenApps)) {
        val customLabel = prefs.getAppAlias("app_alias_$appDrawerPackage")
        apps.add(
            AppListItem(
                activityLabel = "App Drawer",
                activityPackage = appDrawerPackage,
                activityClass = "com.github.gezimos.inkos.ui.AppDrawerFragment",
                user = Process.myUserHandle(),
                customLabel = customLabel
            )
        )
    }

    // Notifications synthetic app (if enabled)
    if (prefs.notificationsEnabled) {
        val notificationsPackage = "com.inkos.internal.notifications"
        val isNotificationsHidden =
            hiddenApps.contains("${notificationsPackage}|${Process.myUserHandle()}")

        if (shouldIncludeSyntheticApp(flag, isNotificationsHidden, includeHiddenApps)) {
            val customLabel = prefs.getAppAlias("app_alias_$notificationsPackage")
            apps.add(
                AppListItem(
                    activityLabel = "Notifications",
                    activityPackage = notificationsPackage,
                    activityClass = "com.github.gezimos.inkos.ui.notifications.NotificationsFragment",
                    user = Process.myUserHandle(),
                    customLabel = customLabel
                )
            )
        }
    }

    return apps
}

/**
 * Get Empty Space synthetic app
 */
private fun getEmptySpaceApp(): AppListItem {
    return AppListItem(
        activityLabel = "Empty Space",
        activityPackage = "com.inkos.internal.empty_space",
        activityClass = "",
        user = Process.myUserHandle(),
        customLabel = ""
    )
}

/**
 * Get system shortcuts for the given context
 */
private fun getSystemShortcutsForContext(
    prefs: Prefs,
    flag: Constants.AppDrawerFlag?,
    includeHiddenApps: Boolean
): List<AppListItem> {
    return when (flag) {
        Constants.AppDrawerFlag.HiddenApps -> {
            SystemShortcutHelper.getFilteredSystemShortcuts(
                prefs, includeHidden = false, onlyHidden = true
            )
        }

        Constants.AppDrawerFlag.SetHomeApp -> {
            SystemShortcutHelper.getFilteredSystemShortcuts(
                prefs, includeHidden = false, onlyHidden = false
            )
        }

        else -> {
            SystemShortcutHelper.getFilteredSystemShortcuts(
                prefs, includeHidden = includeHiddenApps, onlyHidden = false
            )
        }
    }
}

/**
 * Determine if a synthetic app should be included based on context and hidden status
 */
private fun shouldIncludeSyntheticApp(
    flag: Constants.AppDrawerFlag?,
    isHidden: Boolean,
    includeHiddenApps: Boolean
): Boolean {
    return when (flag) {
        Constants.AppDrawerFlag.SetHomeApp -> !isHidden || includeHiddenApps
        Constants.AppDrawerFlag.LaunchApp -> !isHidden
        Constants.AppDrawerFlag.HiddenApps -> isHidden
        null -> !isHidden || includeHiddenApps
        else -> false
    }
}

/**
 * Get all hidden apps (both synthetic and regular apps) for hidden apps management
 */
suspend fun getHiddenApps(
    context: Context,
    prefs: Prefs,
    hiddenAppsSet: Set<String>
): List<AppListItem> {
    val hiddenApps = mutableListOf<AppListItem>()

    // Get all installed apps to match against
    val allApps = getAppsList(context, includeRegularApps = true, includeHiddenApps = true)

    for (hiddenApp in hiddenAppsSet) {
        try {
            val parts = hiddenApp.split("|")
            val packageName = parts[0]

            when {
                // Handle internal synthetic apps
                packageName == "com.inkos.internal.app_drawer" -> {
                    val customLabel = prefs.getAppAlias("app_alias_$packageName")
                    hiddenApps.add(
                        AppListItem(
                            activityLabel = "App Drawer",
                            activityPackage = packageName,
                            activityClass = "com.github.gezimos.inkos.ui.AppDrawerFragment",
                            user = Process.myUserHandle(),
                            customLabel = customLabel
                        )
                    )
                }

                packageName == "com.inkos.internal.notifications" -> {
                    val customLabel = prefs.getAppAlias("app_alias_$packageName")
                    hiddenApps.add(
                        AppListItem(
                            activityLabel = "Notifications",
                            activityPackage = packageName,
                            activityClass = "com.github.gezimos.inkos.ui.notifications.NotificationsFragment",
                            user = Process.myUserHandle(),
                            customLabel = customLabel
                        )
                    )
                }

                // Handle system shortcuts (if enabled)
                prefs.systemShortcutsEnabled && SystemShortcutHelper.isSystemShortcut(packageName) -> {
                    val shortcut = SystemShortcutHelper.getSystemShortcut(packageName)
                    if (shortcut != null) {
                        val customLabel = prefs.getAppAlias("app_alias_$packageName")
                        val item = SystemShortcutHelper.createAppListItem(shortcut, customLabel)
                        hiddenApps.add(item)
                    }
                }

                // Handle regular apps
                else -> {
                    val app = if (parts.size > 1) {
                        allApps.find { app ->
                            app.activityPackage == packageName &&
                                    app.user.toString() == parts[1]
                        }
                    } else {
                        allApps.find { app ->
                            app.activityPackage == packageName
                        }
                    }

                    app?.let {
                        val customLabel = prefs.getAppAlias("app_alias_${it.activityPackage}")
                        if (customLabel.isNotEmpty()) {
                            it.customLabel = customLabel
                        }
                        hiddenApps.add(it)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SystemUtils", "Error processing hidden app: $hiddenApp", e)
            continue
        }
    }

    return hiddenApps
}

/**
 * Handle launching synthetic and system apps
 * Returns true if the app was handled (launched or is a special case), false if it should be handled normally
 */
fun launchSyntheticOrSystemApp(
    context: Context,
    packageName: String,
    fragment: Fragment
): Boolean {
    when {
        // Handle synthetic "App Drawer" item
        packageName == "com.inkos.internal.app_drawer" -> {
            try {
                if (fragment.findNavController().currentDestination?.id == R.id.mainFragment) {
                    fragment.findNavController()
                        .navigate(R.id.action_mainFragment_to_appListFragment)
                }
            } catch (_: Exception) {
                fragment.findNavController().navigate(R.id.appListFragment)
            }
            return true
        }

        // Handle synthetic "Notifications" item
        packageName == "com.inkos.internal.notifications" -> {
            try {
                fragment.findNavController().navigate(R.id.notificationsFragment)
            } catch (_: Exception) {
                // fallback: try direct navigation
                fragment.findNavController().navigate(R.id.notificationsFragment)
            }
            return true
        }

        // Handle synthetic "Empty Space" item - do nothing
        packageName == "com.inkos.internal.empty_space" -> {
            return true
        }

        // Handle system shortcuts
        SystemShortcutHelper.isSystemShortcut(packageName) -> {
            return SystemShortcutHelper.launchSystemShortcut(context, packageName)
        }

        else -> return false
    }
}

suspend fun getAppsList(
    context: Context,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
): MutableList<AppListItem> {
    return withContext(Dispatchers.Main) {
        val appList: MutableList<AppListItem> = mutableListOf()
        val combinedList: MutableList<AppListItem> = mutableListOf()

        try {
            val hiddenApps = Prefs(context).hiddenApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            for (profile in userManager.userProfiles) {
                for (launcherActivityInfo in launcherApps.getActivityList(null, profile)) {
                    val activityName = launcherActivityInfo.name
                    val appPackage = launcherActivityInfo.applicationInfo.packageName
                    val label = launcherActivityInfo.label.toString()

                    if (includeHiddenApps && hiddenApps.contains(appPackage) ||
                        includeRegularApps && !hiddenApps.contains(appPackage)
                    ) {
                        val customLabel = Prefs(context).getAppAlias("app_alias_$appPackage")
                        appList.add(
                            AppListItem(
                                activityLabel = label,
                                activityPackage = appPackage,
                                activityClass = activityName,
                                user = profile,
                                customLabel = customLabel
                            )
                        )
                    }
                }
            }
            appList.sort()
            combinedList.addAll(appList)
        } catch (_: Exception) {
            Log.d("appList", "error retrieving app list")
        }

        combinedList
    }
}

fun getUserHandleFromString(context: Context, userHandleString: String): UserHandle {
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    for (userHandle in userManager.userProfiles) {
        if (userHandle.toString() == userHandleString) {
            return userHandle
        }
    }
    return Process.myUserHandle()
}

fun isinkosDefault(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    } else {
        val testIntent = Intent(Intent.ACTION_MAIN)
        testIntent.addCategory(Intent.CATEGORY_HOME)
        val defaultHome = testIntent.resolveActivity(context.packageManager)?.packageName
        return defaultHome == context.packageName
    }
}

fun setDefaultHomeScreen(context: Context, checkDefault: Boolean = false) {
    val isDefault = isinkosDefault(context)
    if (checkDefault && isDefault) {
        // Launcher is already the default home app
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && context is Activity
        && !isDefault // using role manager only works when µLauncher is not already the default.
    ) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        context.startActivityForResult(
            roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
            Constants.REQUEST_SET_DEFAULT_HOME
        )
        return
    }

    val intent = Intent(Settings.ACTION_HOME_SETTINGS)
    context.startActivity(intent)
}


fun openAppInfo(context: Context, userHandle: UserHandle, packageName: String) {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        launcher.startAppDetailsActivity(intent.component, userHandle, null, null)
    } ?: context.showLongToast("Unable to to open app info")
}


fun isTablet(context: Context): Boolean {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    windowManager.defaultDisplay.getMetrics(metrics)
    val widthInches = metrics.widthPixels / metrics.xdpi
    val heightInches = metrics.heightPixels / metrics.ydpi
    val diagonalInches = sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))
    if (diagonalInches >= 7.0) return true
    return false
}

fun initActionService(context: Context): ActionService? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val actionService = ActionService.instance()
        if (actionService != null) {
            return actionService
        } else {
            context.openAccessibilitySettings()
        }
    } else {
        context.showLongToast("This action requires Android P (9) or higher")
    }

    return null
}


fun showStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(WindowInsets.Type.statusBars())

        // Set correct status bar icon appearance based on current theme
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val prefs = com.github.gezimos.inkos.data.Prefs(activity)
        val isDarkTheme = when (prefs.appTheme) {
                Constants.Theme.Dark -> true
                Constants.Theme.Light -> false
                Constants.Theme.System -> {
                val uiMode =
                    activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
    } else {
        @Suppress("DEPRECATION", "InlinedApi")
        activity.window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }
}

fun hideStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}

fun showNavigationBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(WindowInsets.Type.navigationBars())

        // Set correct navigation bar icon appearance based on current theme
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val prefs = com.github.gezimos.inkos.data.Prefs(activity)
        val isDarkTheme = when (prefs.appTheme) {
            Constants.Theme.Dark -> true
            Constants.Theme.Light -> false
            Constants.Theme.System -> {
                val uiMode =
                    activity.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
    } else {
        @Suppress("DEPRECATION", "InlinedApi")
        activity.window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }
}

fun hideNavigationBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        activity.window.insetsController?.hide(WindowInsets.Type.navigationBars())
    else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}

fun dp2px(resources: Resources, dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

fun storeFile(activity: Activity, backupType: Constants.BackupType) {
    // Generate a unique filename with a timestamp
    when (backupType) {
        Constants.BackupType.FullSystem -> {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "backup_$timeStamp.json"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            activity.startActivityForResult(intent, Constants.BACKUP_WRITE, null)
        }

        Constants.BackupType.Theme -> {
            val timeStamp = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val fileName = "theme_$timeStamp.mtheme"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            activity.startActivityForResult(intent, Constants.THEME_BACKUP_WRITE, null)
        }

    }

}

fun loadFile(activity: Activity, backupType: Constants.BackupType) {
    when (backupType) {
        Constants.BackupType.FullSystem -> {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Use generic type for compatibility
            }
            if (intent.resolveActivity(activity.packageManager) == null) {
                // Show error dialog if no file picker is available
                try {
                    val dialogManagerClass =
                        Class.forName("com.github.gezimos.inkos.ui.dialogs.DialogManager")
                    val constructor =
                        dialogManagerClass.getConstructor(Context::class.java, Activity::class.java)
                    val dialogManager = constructor.newInstance(activity, activity)
                    val showErrorDialog = dialogManagerClass.getMethod(
                        "showErrorDialog",
                        Context::class.java,
                        String::class.java,
                        String::class.java
                    )
                    showErrorDialog.invoke(
                        dialogManager,
                        activity,
                        activity.getString(R.string.error_no_file_picker_title),
                        activity.getString(R.string.error_no_file_picker_message)
                    )
                        } catch (_: Exception) {
                            // fallback: show toast
                            android.widget.Toast.makeText(
                                activity,
                                activity.getString(R.string.error_no_file_picker_message),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                return
            }
            activity.startActivityForResult(intent, Constants.BACKUP_READ, null)
        }

        Constants.BackupType.Theme -> {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
            }
            if (intent.resolveActivity(activity.packageManager) == null) {
                try {
                    val dialogManagerClass =
                        Class.forName("com.github.gezimos.inkos.ui.dialogs.DialogManager")
                    val constructor =
                        dialogManagerClass.getConstructor(Context::class.java, Activity::class.java)
                    val dialogManager = constructor.newInstance(activity, activity)
                    val showErrorDialog = dialogManagerClass.getMethod(
                        "showErrorDialog",
                        Context::class.java,
                        String::class.java,
                        String::class.java
                    )
                    showErrorDialog.invoke(
                        dialogManager,
                        activity,
                        activity.getString(R.string.error_no_file_picker_title),
                        activity.getString(R.string.error_no_file_picker_message)
                    )
                } catch (_: Exception) {
                    android.widget.Toast.makeText(
                        activity,
                        activity.getString(R.string.error_no_file_picker_message),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
            activity.startActivityForResult(intent, Constants.THEME_BACKUP_READ, null)
        }
    }

}

fun getHexForOpacity(prefs: Prefs): Int {
    val backgroundColor = prefs.backgroundColor
    return backgroundColor // Just return the background color without opacity modification
}

fun isSystemInDarkMode(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
}

fun setThemeMode(context: Context, isDark: Boolean, view: View) {
    // Retrieve background color based on the theme
    val backgroundAttr = if (isDark) R.attr.backgroundDark else R.attr.backgroundLight

    val typedValue = TypedValue()
    val theme: Resources.Theme = context.theme
    theme.resolveAttribute(backgroundAttr, typedValue, true)

    // Apply the background color from styles.xml
    view.setBackgroundResource(typedValue.resourceId)
}

fun getTrueSystemFont(): Typeface {
    val possibleSystemFonts = listOf(
        "/system/fonts/Roboto-Regular.ttf",      // Stock Android (Pixel, AOSP)
        "/system/fonts/NotoSans-Regular.ttf",    // Some Android One devices
        "/system/fonts/SamsungOne-Regular.ttf",  // Samsung
        "/system/fonts/MiSans-Regular.ttf",      // Xiaomi MIUI
        "/system/fonts/OPSans-Regular.ttf"       // OnePlus
    )

    for (fontPath in possibleSystemFonts) {
        val fontFile = File(fontPath)
        if (fontFile.exists()) {
            return Typeface.createFromFile(fontFile)
        }
    }

    // Fallback to Roboto as a default if no system font is found
    return Typeface.DEFAULT
}

