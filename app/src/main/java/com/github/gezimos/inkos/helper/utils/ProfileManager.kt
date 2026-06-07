package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.github.gezimos.common.showLongToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.helper.isinkosDefault
import com.github.gezimos.inkos.helper.setDefaultHomeScreen

class ProfileManager(private val context: Context) {


    fun isPrivateSpaceSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }

    fun getPrivateSpaceUser(): UserHandle? {
        if (!isPrivateSpaceSupported()) return null
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return launcherApps.profiles.firstOrNull { u ->
            launcherApps.getLauncherUserInfo(u)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE
        }
    }

    fun isPrivateSpaceSetUp(
        showToast: Boolean = false,
        launchSettings: Boolean = false
    ): Boolean {
        if (!isPrivateSpaceSupported()) {
            if (showToast) {
                context.showLongToast(context.getString(R.string.alert_requires_android_v))
            }
            return false
        }
        val privateSpaceUser = getPrivateSpaceUser()
        if (privateSpaceUser != null) {
            return true
        }
        if (!isinkosDefault(context)) {
            if (showToast) {
                context.showLongToast(context.getString(R.string.toast_private_space_default_home_screen))
            }
            if (launchSettings) {
                setDefaultHomeScreen(context)
            }
        } else {
            if (showToast) {
                context.showLongToast(context.getString(R.string.toast_private_space_not_available))
            }
        }
        return false
    }

    fun isPrivateSpaceLocked(): Boolean {
        if (!isPrivateSpaceSupported()) return false
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val privateSpaceUser = getPrivateSpaceUser() ?: return false
        return userManager.isQuietModeEnabled(privateSpaceUser)
    }

    private fun lockPrivateSpace(lock: Boolean) {
        if (!isPrivateSpaceSupported()) return
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val privateSpaceUser = getPrivateSpaceUser() ?: return
        userManager.requestQuietModeEnabled(lock, privateSpaceUser)
    }

    fun togglePrivateSpaceLock(showToast: Boolean, launchSettings: Boolean) {
        if (!isPrivateSpaceSetUp(showToast, launchSettings)) return
        if (!isinkosDefault(context)) {
            context.showLongToast(context.getString(R.string.toast_private_space_default_home_screen))
            return
        }

        val wasLocked = isPrivateSpaceLocked()
        lockPrivateSpace(!wasLocked)

        if (showToast) {
            val msgRes = if (wasLocked) R.string.toast_private_space_unlocked
                         else R.string.toast_private_space_locked
            context.showLongToast(context.getString(msgRes))
        }
    }


    fun isWorkProfileSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun getWorkProfileUser(): UserHandle? {
        if (!isWorkProfileSupported()) return null
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        return userManager.userProfiles.firstOrNull { u ->
            u != Process.myUserHandle() &&
            launcherApps.getLauncherUserInfo(u)?.userType == UserManager.USER_TYPE_PROFILE_MANAGED
        }
    }

    fun hasWorkProfile(): Boolean {
        return getWorkProfileUser() != null
    }

    fun isWorkProfilePaused(): Boolean {
        if (!isWorkProfileSupported()) return false
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val workUser = getWorkProfileUser() ?: return false
        return userManager.isQuietModeEnabled(workUser)
    }

    fun toggleWorkProfile(showToast: Boolean) {
        if (!isWorkProfileSupported()) {
            if (showToast) {
                context.showLongToast(context.getString(R.string.toast_work_profile_not_available))
            }
            return
        }
        val workUser = getWorkProfileUser()
        if (workUser == null) {
            if (showToast) {
                context.showLongToast(context.getString(R.string.toast_work_profile_not_available))
            }
            return
        }

        val wasPaused = isWorkProfilePaused()
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        userManager.requestQuietModeEnabled(!wasPaused, workUser)

        if (showToast) {
            val msgRes = if (wasPaused) R.string.toast_work_profile_resumed
                         else R.string.toast_work_profile_paused
            context.showLongToast(context.getString(msgRes))
        }
    }
}
