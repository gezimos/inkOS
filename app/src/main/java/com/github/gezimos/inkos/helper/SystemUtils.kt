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
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowCompat
import com.github.gezimos.common.openAccessibilitySettings
import com.github.gezimos.common.showLongToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.services.ActionService
import com.github.gezimos.inkos.style.resolveThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

// Synthetic app helpers were moved to `helper/SyntheticApps.kt` to keep SystemUtils focused.
// See `SyntheticApps.kt` in the same package for implementations of:
// - getSyntheticApps
// - getInternalSyntheticApps
// - getEmptySpaceApp
// - getSystemShortcutsForContext
// - shouldIncludeSyntheticApp
// - getHiddenApps
// - launchSyntheticOrSystemApp

suspend fun getAppsList(
    context: Context,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
): MutableList<AppListItem> {
    return withContext(Dispatchers.IO) {
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

                    // Check hidden apps using the same format as hideOrShowApp(): "packageName|userHandle.toString()"
                    // Also check old format (package only) for backwards compatibility
                    val hiddenKey = "$appPackage|${profile.toString()}"
                    val isHidden = hiddenApps.contains(hiddenKey) || hiddenApps.contains(appPackage)
                    if (includeHiddenApps && isHidden ||
                        includeRegularApps && !isHidden
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
        val prefs = Prefs(activity)
        val isDarkTheme = prefs.appTheme == Constants.Theme.Dark
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            activity.window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (_: Exception) {}
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}

fun showNavigationBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(WindowInsets.Type.navigationBars())

        // Set correct navigation bar icon appearance based on current theme
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val prefs = Prefs(activity)
        val isDarkTheme = prefs.appTheme == Constants.Theme.Dark
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            activity.window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } catch (_: Exception) {}
        activity.window.insetsController?.hide(WindowInsets.Type.navigationBars())
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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

/**
 * Variant that resolves the current theme colors via the theme bridge and returns
 * the background color as an ARGB int. Prefer this from View code when a Context
 * is available so the centralized theme resolver is used.
 */
fun getHexForOpacity(context: Context): Int {
    return try {
        val (_, bg) = resolveThemeColors(context)
        bg
    } catch (e: Exception) {
        // Fallback to prefs if resolve fails
        try {
            Prefs(context).backgroundColor
        } catch (_: Exception) {
            0xFFFFFFFF.toInt()
        }
    }
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

