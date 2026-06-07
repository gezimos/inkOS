package com.github.gezimos.inkos.data

import android.graphics.Typeface
import android.os.UserHandle
import androidx.annotation.ColorInt
import com.github.gezimos.inkos.services.NotificationManager
data class HomeAppUiState(
    val id: Int,
    val label: String,
    val font: Typeface?,
    @ColorInt val color: Int,
    val notificationInfo: NotificationManager.NotificationInfo? = null,
    val activityPackage: String,
    val activityClass: String = "",
    val user: UserHandle? = null,
    val shortcutId: String? = null
)

