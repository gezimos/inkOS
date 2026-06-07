package com.github.gezimos.inkos.helper

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
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

suspend fun getAppsList(
    context: Context,
    includeRegularApps: Boolean = true,
    includeHiddenApps: Boolean = false,
    includeShortcuts: Boolean = true,
): MutableList<AppListItem> {
    return withContext(Dispatchers.IO) {
        val appList: MutableList<AppListItem> = mutableListOf()
        val shortcutList: MutableList<AppListItem> = mutableListOf()
        val combinedList: MutableList<AppListItem> = mutableListOf()

        try {
            val prefs = Prefs(context)
            val hiddenApps = prefs.hiddenApps
            val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            for (profile in userManager.userProfiles) {
                for (launcherActivityInfo in launcherApps.getActivityList(null, profile)) {
                    val activityName = launcherActivityInfo.name
                    val appPackage = launcherActivityInfo.applicationInfo.packageName
                    val label = launcherActivityInfo.label.toString()

                    val hiddenKey = "$appPackage|${profile.toString()}"
                    val isHidden = hiddenApps.contains(hiddenKey) || hiddenApps.contains(appPackage)
                    if (includeHiddenApps && isHidden ||
                        includeRegularApps && !isHidden
                    ) {
                        val customLabel = prefs.getAppAlias("app_alias_$appPackage")
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
                
                if (includeShortcuts) {
                    try {
                        if (launcherApps.hasShortcutHostPermission()) {
                            val shortcutQuery = ShortcutQuery().apply {
                                setQueryFlags(
                                    ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                    ShortcutQuery.FLAG_MATCH_MANIFEST
                                )
                            }
                            
                            val shortcuts = launcherApps.getShortcuts(shortcutQuery, profile)
                            shortcuts?.forEach { shortcut ->
                                val shortcutId = shortcut.id
                                val appPackage = shortcut.`package`
                                val label = shortcut.shortLabel?.toString() 
                                    ?: shortcut.longLabel?.toString() 
                                    ?: shortcutId
                                
                                val shortcutHiddenKey = "$appPackage|$shortcutId|${profile.toString()}"
                                val regularHiddenKey = "$appPackage|${profile.toString()}"
                                val isHidden = hiddenApps.contains(shortcutHiddenKey) ||
                                    hiddenApps.contains(regularHiddenKey) ||
                                    hiddenApps.contains(appPackage)
                                
                                if (includeHiddenApps && isHidden ||
                                    includeRegularApps && !isHidden
                                ) {
                                    val customLabel = prefs.getAppAlias("app_alias_${appPackage}_$shortcutId")
                                    shortcutList.add(
                                        AppListItem(
                                            activityLabel = label,
                                            activityPackage = appPackage,
                                            activityClass = "", // Shortcuts don't have activity classes
                                            user = profile,
                                            customLabel = customLabel,
                                            shortcutId = shortcutId
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("appList", "error retrieving shortcuts: ${e.message}")
                    }
                }
            }
            appList.sort()
            shortcutList.sort()
            combinedList.addAll(appList)
            combinedList.addAll(shortcutList)
            combinedList.sort()
        } catch (_: Exception) {
            Log.d("appList", "error retrieving app list")
        }

        combinedList
    }
}
data class AppShortcutInfo(
    val packageName: String,
    val shortcutId: String,
    val label: String,
    val userHandle: String,
    val isPinned: Boolean
) {
    val key: String get() = "$packageName|$shortcutId|$userHandle"
}
fun getAllAppShortcuts(context: Context): List<AppShortcutInfo> {
    val shortcuts = mutableListOf<AppShortcutInfo>()
    val seenKeys = mutableSetOf<String>()
    
    try {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        
        if (launcherApps.hasShortcutHostPermission()) {
            for (profile in userManager.userProfiles) {
                val query = ShortcutQuery().apply {
                    setQueryFlags(
                        ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        ShortcutQuery.FLAG_MATCH_MANIFEST
                    )
                }
                
                val systemShortcuts = launcherApps.getShortcuts(query, profile)
                systemShortcuts?.forEach { shortcut ->
                    val key = "${shortcut.`package`}|${shortcut.id}|${profile.toString()}"
                    if (!seenKeys.contains(key)) {
                        seenKeys.add(key)
                        shortcuts.add(AppShortcutInfo(
                            packageName = shortcut.`package`,
                            shortcutId = shortcut.id,
                            label = shortcut.shortLabel?.toString() 
                                ?: shortcut.longLabel?.toString() 
                                ?: shortcut.id,
                            userHandle = profile.toString(),
                            isPinned = false
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.d("SystemUtils", "Error getting system shortcuts: ${e.message}")
    }
    
    try {
        val pinnedShortcuts = PinnedShortcutUtility.getPinnedShortcuts(context)
        for (pinned in pinnedShortcuts) {
            val key = "${pinned.packageName}|${pinned.shortcutId}|${pinned.userHandle}"
            if (!seenKeys.contains(key)) {
                seenKeys.add(key)
                shortcuts.add(AppShortcutInfo(
                    packageName = pinned.packageName,
                    shortcutId = pinned.shortcutId,
                    label = pinned.label,
                    userHandle = pinned.userHandle,
                    isPinned = true
                ))
            }
        }
    } catch (e: Exception) {
        Log.d("SystemUtils", "Error getting pinned shortcuts: ${e.message}")
    }
    
    return shortcuts.sortedBy { it.label.lowercase() }
}
fun computeDefaultShortcutSelection(allShortcuts: List<AppShortcutInfo>, applicationId: String): Set<String> {
    val pinnedKeys = allShortcuts
        .filter { it.isPinned && it.packageName != applicationId }
        .map { it.key }.toSet()
    val inkosKeys = allShortcuts
        .filter { it.packageName == applicationId && it.shortcutId.startsWith("inkos_") }
        .map { it.key }.toSet()
    return pinnedKeys + inkosKeys
}
fun getShortcutAsAppListItem(
    context: Context,
    packageName: String,
    shortcutId: String,
    userHandle: UserHandle
): AppListItem? {
    return try {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        if (!launcherApps.hasShortcutHostPermission()) {
            return null
        }
        
        val query = ShortcutQuery().apply {
            setPackage(packageName)
            setShortcutIds(listOf(shortcutId))
            setQueryFlags(
                ShortcutQuery.FLAG_MATCH_DYNAMIC or
                ShortcutQuery.FLAG_MATCH_PINNED or
                ShortcutQuery.FLAG_MATCH_MANIFEST
            )
        }
        
        val shortcuts = launcherApps.getShortcuts(query, userHandle)
        val shortcut = shortcuts?.firstOrNull() ?: return null
        
        val label = shortcut.shortLabel?.toString() 
            ?: shortcut.longLabel?.toString() 
            ?: shortcutId
            
        val customLabel = Prefs(context).getAppAlias("app_alias_${packageName}_$shortcutId")
        
        AppListItem(
            activityLabel = label,
            activityPackage = packageName,
            activityClass = "",
            user = userHandle,
            customLabel = customLabel,
            shortcutId = shortcutId
        )
    } catch (e: Exception) {
        Log.d("SystemUtils", "Error getting shortcut: ${e.message}")
        null
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

fun setDefaultHomeScreen(
    context: Context,
    checkDefault: Boolean = false,
    launcher: ActivityResultLauncher<Intent>? = null
) {
    val isDefault = isinkosDefault(context)
    if (checkDefault && isDefault) {
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        && !isDefault
    ) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        if (launcher != null) {
            launcher.launch(roleIntent)
            return
        }
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

        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val prefs = Prefs(activity)
        val isDarkTheme = when (prefs.appTheme) {
            Constants.Theme.Dark -> true
            Constants.Theme.Light -> false
            Constants.Theme.System -> isSystemInDarkMode(activity)
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

        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val prefs = Prefs(activity)
        val isDarkTheme = when (prefs.appTheme) {
            Constants.Theme.Dark -> true
            Constants.Theme.Light -> false
            Constants.Theme.System -> isSystemInDarkMode(activity)
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

fun storeFile(launcher: ActivityResultLauncher<Intent>, backupType: Constants.BackupType) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "application/json"
        putExtra(Intent.EXTRA_TITLE, when (backupType) {
            Constants.BackupType.FullSystem -> "backup_$timeStamp.json"
            Constants.BackupType.Theme -> "inkos_theme_$timeStamp.json"
        })
    }
    launcher.launch(intent)
}

fun loadFile(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
    }
    if (intent.resolveActivity(activity.packageManager) == null) {
        try {
            com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager(activity, activity).showErrorDialog(
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
    launcher.launch(intent)
}

fun getHexForOpacity(context: Context): Int {
    return try {
        val (_, bg) = resolveThemeColors(context)
        bg
    } catch (_: Exception) {
        try {
            Prefs(context).backgroundColor
        } catch (_: Exception) {
            0xFFFFFFFF.toInt()
        }
    }
}

fun isSystemInDarkMode(context: Context): Boolean {
    return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
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

    return Typeface.DEFAULT
}

