package com.github.gezimos.inkos.ui.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SignalCellularOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.AudioWidgetHelper
import com.github.gezimos.inkos.helper.IconShape
import com.github.gezimos.inkos.helper.IconShapeUtility
import com.github.gezimos.inkos.helper.IconUtility
import com.github.gezimos.inkos.helper.utils.BrightnessHelper
import com.github.gezimos.inkos.helper.utils.VibrationHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.services.NotificationService
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.compose.AppIconBox
import com.github.gezimos.inkos.ui.compose.rememberAppIconBitmap
import com.github.gezimos.inkos.ui.compose.EditModeOverlay
import com.github.gezimos.inkos.ui.compose.HubFocusZone
import com.github.gezimos.inkos.ui.compose.NavHelper
import com.github.gezimos.inkos.ui.compose.SimpleTrayFocusZone
import com.github.gezimos.inkos.ui.compose.gestureHelper
import com.github.gezimos.inkos.ui.compose.inkOsSafeDrawingPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ================================================================================
// SHARED DATA TYPES
// ================================================================================

internal data class HubNotification(
    val packageName: String,
    val notif: NotificationManager.ConversationNotification,
    val sbn: StatusBarNotification
)

internal data class CategoryTab(
    val label: String,
    val count: Int,
    val icon: ImageVector? = null,
    val filter: (HubNotification) -> Boolean
)

internal class SimplePagerState(current: Int = 0, count: Int = 1) {
    private var _current = mutableIntStateOf(current)
    private var _count = mutableIntStateOf(count)
    var currentPage: Int
        get() = _current.intValue
        set(v) { _current.intValue = v }
    var pageCount: Int
        get() = _count.intValue
        set(v) { _count.intValue = v }
    fun scrollToPage(page: Int) {
        currentPage = page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    }
}

internal fun vibratePaging(context: Context) {
    VibrationHelper.trigger(VibrationHelper.Effect.PAGE)
}

internal fun openNotificationSettingsForPackage(context: Context, packageName: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

// ================================================================================
// HUB CATEGORY HELPERS
// ================================================================================

private val messagingPackages = setOf(
    "com.whatsapp", "com.whatsapp.w4b",
    "org.telegram.messenger", "org.thunderdog.chalern",
    "com.facebook.orca", "com.facebook.mlite",
    "com.discord", "com.Slack",
    "com.viber.voip", "com.google.android.apps.messaging",
    "com.samsung.android.messaging", "com.oneplus.mms",
    "org.thoughtcrime.securesms", "im.vector.app",
    "com.beeper.chat", "com.snapchat.android"
)

private fun isMessageNotification(hn: HubNotification): Boolean {
    return hn.notif.category == Notification.CATEGORY_MESSAGE
        || hn.notif.category == Notification.CATEGORY_SOCIAL
        || hn.packageName in messagingPackages
}

private fun buildCategoryTabs(
    context: Context,
    notifications: List<HubNotification>
): List<CategoryTab> {
    val knownCategories = setOf(
        Notification.CATEGORY_MESSAGE,
        Notification.CATEGORY_SOCIAL,
        Notification.CATEGORY_CALL,
        Notification.CATEGORY_EMAIL,
        Notification.CATEGORY_EVENT
    )
    val tabs = mutableListOf(
        CategoryTab(context.getString(R.string.hub_all), notifications.size, icon = Icons.Rounded.Notifications) { true }
    )
    val msgCount = notifications.count { isMessageNotification(it) }
    tabs.add(CategoryTab(context.getString(R.string.hub_messages), msgCount, icon = Icons.AutoMirrored.Rounded.Chat) { isMessageNotification(it) })
    val categoryDefs = listOf(
        Triple(context.getString(R.string.hub_calls), Notification.CATEGORY_CALL, Icons.Rounded.Phone),
        Triple(context.getString(R.string.hub_email), Notification.CATEGORY_EMAIL, Icons.Rounded.Email),
        Triple(context.getString(R.string.hub_events), Notification.CATEGORY_EVENT, Icons.Rounded.Event)
    )
    for ((label, cat, icon) in categoryDefs) {
        val count = notifications.count { it.notif.category == cat }
        tabs.add(CategoryTab(label, count, icon = icon) { it.notif.category == cat })
    }
    val otherCount = notifications.count { !isMessageNotification(it) && it.notif.category !in knownCategories }
    tabs.add(CategoryTab(context.getString(R.string.hub_other), otherCount, icon = Icons.Rounded.MoreHoriz) { !isMessageNotification(it) && it.notif.category !in knownCategories })
    return tabs
}

private val actionIconMap = mapOf(
    "delete" to Icons.Rounded.Delete,
    "trash" to Icons.Rounded.Delete,
    "remove" to Icons.Rounded.Delete
)

// Hub container size cache (used for itemsPerPage calculation across recompositions)
internal object HubLayoutCache {
    var cachedItemsPerPage: Int? = null
    var cachedContainerHeight: Int = 0
    fun clear() { cachedItemsPerPage = null; cachedContainerHeight = 0 }
}

internal fun convertSbnToHubConversationNotification(
    ctx: Context,
    prefsLocal: Prefs,
    sbn: StatusBarNotification
): NotificationManager.ConversationNotification {
    val packageName = sbn.packageName
    val extras = sbn.notification.extras
    val showGroup = prefsLocal.showNotificationGroupName

    val conversationTitleRaw = if (showGroup) {
        extras?.getCharSequence("android.conversationTitle")?.toString()
            ?: extras?.getString("android.conversationTitle")
    } else null

    val titleRaw = extras?.getCharSequence("android.title")?.toString()
        ?: extras?.getString("android.title")

    val senderRaw = if (!titleRaw.isNullOrBlank() && titleRaw.contains(": ")) {
        titleRaw.split(": ", limit = 2).firstOrNull()?.trim()
    } else titleRaw
        ?: extras?.getCharSequence("android.subText")?.toString()
        ?: extras?.getString("android.subText")

    val messageRaw = when {
        extras?.getCharSequence("android.bigText") != null ->
            extras.getCharSequence("android.bigText")?.toString()
        extras?.getCharSequence("android.text") != null ->
            extras.getCharSequence("android.text")?.toString()
        extras?.getCharSequenceArray("android.textLines") != null ->
            extras.getCharSequenceArray("android.textLines")?.lastOrNull()?.toString()
        extras?.getCharSequence("android.summaryText") != null ->
            extras.getCharSequence("android.summaryText")?.toString()
        extras?.getCharSequence("android.infoText") != null ->
            extras.getCharSequence("android.infoText")?.toString()
        sbn.notification.tickerText != null ->
            sbn.notification.tickerText?.toString()
        else -> null
    }

    val message = if (!messageRaw.isNullOrBlank() &&
        !conversationTitleRaw.isNullOrBlank() &&
        messageRaw.trim().equals(conversationTitleRaw.trim(), ignoreCase = true)) {
        null
    } else messageRaw

    val notificationGroup = sbn.notification.group
    val conversationId = when {
        !notificationGroup.isNullOrBlank() -> "group_${packageName}_$notificationGroup"
        !conversationTitleRaw.isNullOrBlank() -> conversationTitleRaw
        sbn.notification.category == Notification.CATEGORY_MESSAGE -> {
            val phoneNumber = extras?.getString("android.people")?.firstOrNull()?.toString()
                ?: extras?.getString("android.subText")
                ?: extras?.getString("android.summaryText")
                ?: senderRaw
            phoneNumber?.let { "sms_$it" } ?: senderRaw ?: "default"
        }
        !senderRaw.isNullOrBlank() -> senderRaw
        else -> sbn.key
    }

    var conversationTitle = if (showGroup) conversationTitleRaw else null
    var sender = senderRaw
    var finalMessage = message?.replace("\n", " ")
        ?.replace("\r", " ")?.trim()
        ?.replace(Regex("\\s+"), " ")?.take(300)

    if ((conversationTitle.isNullOrBlank() && sender.isNullOrBlank()) && finalMessage.isNullOrBlank()) {
        val pm = ctx.packageManager
        val appLabel = try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) { packageName }
        conversationTitle = appLabel
        sender = appLabel
        finalMessage = when (sbn.notification.category) {
            Notification.CATEGORY_MESSAGE -> "New message"
            Notification.CATEGORY_EMAIL -> "New email"
            Notification.CATEGORY_CALL -> "Missed call"
            Notification.CATEGORY_ALARM -> "Alarm"
            Notification.CATEGORY_REMINDER -> "Reminder"
            Notification.CATEGORY_EVENT -> "Event"
            Notification.CATEGORY_PROMO -> "Promotion"
            Notification.CATEGORY_SYSTEM -> "System notification"
            Notification.CATEGORY_SERVICE -> "Service notification"
            Notification.CATEGORY_ERROR -> "Error"
            Notification.CATEGORY_PROGRESS -> "Progress update"
            Notification.CATEGORY_SOCIAL -> "Social update"
            Notification.CATEGORY_STATUS -> "Status update"
            Notification.CATEGORY_RECOMMENDATION -> "Recommendation"
            else -> null
        }
    }

    return NotificationManager.ConversationNotification(
        conversationId = conversationId,
        conversationTitle = conversationTitle,
        sender = sender,
        message = finalMessage,
        timestamp = sbn.postTime,
        category = sbn.notification.category,
        notificationKey = sbn.key
    )
}

// ================================================================================
// HUB SCREEN
// ================================================================================

@Composable
fun HubScreen(
    prefs: Prefs,
    composeView: ComposeView,
    focusRestoreTrigger: Int = 0,
    onShowPermissionExplanationDialog: (title: String, message: String, onContinue: () -> Unit) -> Unit,
    onEditModeClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val ctx = LocalContext.current

    var hasNotifPermission by remember {
        mutableStateOf(androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName))
    }
    var hasShownNotifDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasNotifPermission = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    if (!hasNotifPermission && !hasShownNotifDialog) {
        LaunchedEffect(Unit) {
            hasShownNotifDialog = true
            onShowPermissionExplanationDialog(
                ctx.getString(R.string.perm_notification_title),
                "inkOS needs notification access to show your notifications here. You will be taken to Android settings to grant this permission."
            ) {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Exception) {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", ctx.packageName, null)
                    })
                }
            }
        }
    }

    val isEditMode by com.github.gezimos.inkos.helper.EditModeHelper.isEditModeFlow.collectAsState(
        initial = com.github.gezimos.inkos.helper.EditModeHelper.isEditMode()
    )
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRestoreTrigger) {
        kotlinx.coroutines.delay(400)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val screenScale = rememberScreenScale()

    val resolvedIconShape = remember(prefs.iconShape) { IconShape.fromPreference(prefs.iconShape) }
    val iconSourceMode = prefs.iconSourceMode
    val selectedIconPack = prefs.selectedIconPackPackage
    val tintColor = Theme.colors.text
    val tintArgb = remember(tintColor, iconSourceMode) {
        if (IconShapeUtility.isTintedMode(iconSourceMode)) tintColor.toArgb() else 0
    }

    val rawNotifications by NotificationService.sbnState.collectAsState(initial = emptyList())
    val prefsLocal = Prefs(ctx)
    val notificationManager = NotificationManager.getInstance(ctx)

    val allowed = prefsLocal.allowedSimpleTrayApps
    val validNotifications = rawNotifications
        .filter { sbn ->
            if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) return@filter false
            !notificationManager.isNotificationSummary(sbn) &&
                (allowed.isEmpty() || allowed.contains(sbn.packageName))
        }
        .map { sbn -> HubNotification(sbn.packageName, convertSbnToHubConversationNotification(ctx, prefsLocal, sbn), sbn) }
        .sortedByDescending { it.notif.timestamp }

    val selectedCategoryIndex = remember { mutableIntStateOf(0) }

    val categoryTabs = remember(validNotifications.size) {
        buildCategoryTabs(ctx, validNotifications)
    }

    LaunchedEffect(categoryTabs.size) {
        if (selectedCategoryIndex.intValue >= categoryTabs.size) {
            selectedCategoryIndex.intValue = 0
        }
    }

    val filteredNotifications = remember(validNotifications, selectedCategoryIndex.intValue, categoryTabs) {
        val tab = categoryTabs.getOrNull(selectedCategoryIndex.intValue) ?: return@remember validNotifications
        validNotifications.filter(tab.filter)
    }

    val isDpadModeState = remember { mutableStateOf(false) }
    var isDpadMode by isDpadModeState
    val focusZone = remember { mutableStateOf(HubFocusZone.CATEGORY_TABS) }
    val selectedNotificationIndex = remember { mutableIntStateOf(0) }
    var expandedNotificationKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(filteredNotifications.size) {
        if (filteredNotifications.isEmpty() && focusZone.value == HubFocusZone.NOTIFICATIONS) {
            focusZone.value = HubFocusZone.CATEGORY_TABS
        }
    }

    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(
        if (HubLayoutCache.cachedContainerHeight > 0) IntSize(0, HubLayoutCache.cachedContainerHeight) else IntSize.Zero
    ) }
    val itemHeightPx = remember(density) { with(density) { 68.dp.toPx() }.toInt() }

    val itemsPerPage = remember(containerSize.height, itemHeightPx) {
        val h = containerSize.height
        if (h > 0 && h == HubLayoutCache.cachedContainerHeight && HubLayoutCache.cachedItemsPerPage != null) {
            HubLayoutCache.cachedItemsPerPage!!
        } else {
            val overhead = with(density) { 30.dp.toPx() }.toInt()
            val usableH = (h - overhead).coerceAtLeast(0)
            val result = if (usableH <= 0) 4
            else (usableH / itemHeightPx).coerceAtLeast(1)
            if (h > 0) { HubLayoutCache.cachedContainerHeight = h; HubLayoutCache.cachedItemsPerPage = result }
            result
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val pages = ((filteredNotifications.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    val pagerState = remember { SimplePagerState(0, pages) }

    LaunchedEffect(filteredNotifications.size, itemsPerPage) {
        val oldPages = pagerState.pageCount
        pagerState.pageCount = pages
        if (pages != oldPages) {
            pagerState.currentPage = 0
        } else if (pagerState.currentPage >= pages) {
            pagerState.currentPage = (pages - 1).coerceAtLeast(0)
        }
    }

    val displayNotifications = run {
        val safePage = pagerState.currentPage.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
        val startIndex = safePage * itemsPerPage
        val totalSize = filteredNotifications.size
        val endIndex = (startIndex + itemsPerPage).coerceAtMost(totalSize)
        if (startIndex < totalSize) filteredNotifications.subList(startIndex, endIndex) else emptyList()
    }

    val notifTypefaceGlobal = remember(prefs.notificationsFont, prefs.getCustomFontPathForContext("notifications")) {
        try { prefs.getFontForContext("notifications").getFont(ctx, prefs.getCustomFontPathForContext("notifications")) }
        catch (_: Exception) { null }
    }
    val notifFontFamilyGlobal = remember(notifTypefaceGlobal) { notifTypefaceGlobal?.let { FontFamily(it) } ?: FontFamily.Default }

    val pageDragThreshold = with(density) { 48.dp.toPx() }
    val keyPressTracker = remember { com.github.gezimos.inkos.ui.compose.KeyPressTracker() }

    DisposableEffect(pagerState, coroutineScope, filteredNotifications, composeView) {
        composeView.setTag(0xdeadbeef.toInt(), pagerState)
        composeView.setTag(0xcafebabe.toInt(), coroutineScope)
        composeView.setTag(0xabcdef01.toInt(), filteredNotifications)
        onDispose {
            composeView.setTag(0xdeadbeef.toInt(), null)
            composeView.setTag(0xcafebabe.toInt(), null)
            composeView.setTag(0xabcdef01.toInt(), null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { keyEvent ->
                NavHelper.handleHubKeyEvent(
                    keyEvent = keyEvent,
                    keyPressTracker = keyPressTracker,
                    isDpadModeSetter = { isDpadMode = it },
                    focusZone = focusZone,
                    selectedCategoryIndex = selectedCategoryIndex,
                    categoryTabCount = categoryTabs.size,
                    onCategorySelected = { },
                    onClearAll = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                        filteredNotifications.forEach { hn ->
                            try {
                                if (hn.notif.notificationKey != null) {
                                    NotificationService.dismissNotification(hn.notif.notificationKey)
                                }
                                NotificationManager.getInstance(ctx).removeConversationNotification(hn.packageName, hn.notif.conversationId)
                            } catch (_: Exception) {}
                        }
                    },
                    selectedNotificationIndex = selectedNotificationIndex,
                    notificationsOnPageSize = displayNotifications.size,
                    pageSize = itemsPerPage,
                    currentPage = pagerState.currentPage,
                    totalPages = pagerState.pageCount,
                    onNextPage = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < pagerState.pageCount - 1) {
                                pagerState.scrollToPage(pagerState.currentPage + 1)
                                vibratePaging(ctx)
                            }
                        }
                    },
                    onPreviousPage = {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.scrollToPage(pagerState.currentPage - 1)
                                vibratePaging(ctx)
                            }
                        }
                    },
                    onNotificationClick = { index ->
                        displayNotifications.getOrNull(index)?.let { hn ->
                            expandedNotificationKey = null
                            NotificationManager.getInstance(ctx)
                                .openNotification(hn.packageName, hn.notif.notificationKey, hn.notif.conversationId, removeAfterOpen = true)
                        }
                    },
                    onNotificationLongClick = { index ->
                        displayNotifications.getOrNull(index)?.let { hn ->
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                            expandedNotificationKey = if (expandedNotificationKey == hn.notif.notificationKey) null
                                else hn.notif.notificationKey
                        }
                    },
                    bottomNavEnabled = false
                )
            }
            .gestureHelper(onSwipeUp = {
                try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                onNavigateBack()
            })
            .background(Theme.colors.background)
            .inkOsSafeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 24.dp.scaled(screenScale), end = 24.dp.scaled(screenScale),
                    top = 24.dp.scaled(screenScale),
                    bottom = 16.dp
                )
                .pointerInput(pagerState.currentPage, isDpadMode) {
                    var dragSum = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            isDpadModeState.value = false
                            dragSum += dragAmount
                        },
                        onDragEnd = {
                            if (dragSum < -pageDragThreshold) {
                                if (pagerState.currentPage < pagerState.pageCount - 1) {
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                    vibratePaging(ctx)
                                }
                            } else if (dragSum > pageDragThreshold) {
                                if (pagerState.currentPage > 0) {
                                    pagerState.scrollToPage(pagerState.currentPage - 1)
                                    vibratePaging(ctx)
                                }
                            }
                            dragSum = 0f
                        }
                    )
                }
                .pointerInput(categoryTabs.size, selectedCategoryIndex.intValue) {
                    var dragSum = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount -> dragSum += dragAmount },
                        onDragEnd = {
                            val threshold = with(density) { 48.dp.toPx() }
                            val idx = selectedCategoryIndex.intValue
                            if (dragSum < -threshold && idx < categoryTabs.size - 1) {
                                selectedCategoryIndex.intValue = idx + 1
                            } else if (dragSum > threshold && idx > 0) {
                                selectedCategoryIndex.intValue = idx - 1
                            }
                            dragSum = 0f
                        }
                    )
                }
        ) {
            HubCategoryTabsRow(
                prefs = prefs,
                tabs = categoryTabs,
                selectedIndex = selectedCategoryIndex.intValue,
                onTabSelected = { selectedCategoryIndex.intValue = it },
                isDpadMode = isDpadMode,
                isFocused = isDpadMode && focusZone.value == HubFocusZone.CATEGORY_TABS,
                selectedDpadIndex = selectedCategoryIndex.intValue
            )

            Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))

            if (filteredNotifications.isNotEmpty()) {
                HubClearAllRow(
                    prefs = prefs,
                    isFocused = isDpadMode && focusZone.value == HubFocusZone.CLEAR_ALL,
                    total = filteredNotifications.size,
                    isAllTab = selectedCategoryIndex.intValue == 0,
                    categoryLabel = categoryTabs.getOrNull(selectedCategoryIndex.intValue)?.label ?: "All",
                    onClearAll = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                        filteredNotifications.forEach { hn ->
                            try {
                                if (hn.notif.notificationKey != null) {
                                    NotificationService.dismissNotification(hn.notif.notificationKey)
                                }
                                NotificationManager.getInstance(ctx).removeConversationNotification(hn.packageName, hn.notif.conversationId)
                            } catch (_: Exception) {}
                        }
                    },
                    fontFamily = notifFontFamilyGlobal
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth().onSizeChanged { containerSize = it }) {
                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_foreground),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Theme.colors.text),
                                modifier = Modifier.size(36.dp.scaled(screenScale))
                            )
                            Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                            Text(
                                text = stringResource(id = R.string.no_notifications),
                                color = Theme.colors.text,
                                fontSize = prefs.notificationsTextSize.sp.scaled(screenScale),
                                fontFamily = notifFontFamilyGlobal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    HubNotificationList(
                        prefs = prefs,
                        notifications = displayNotifications,
                        isDpadMode = isDpadMode,
                        focusZone = focusZone.value,
                        selectedNotificationIndex = selectedNotificationIndex.intValue,
                        iconShape = resolvedIconShape,
                        iconSourceMode = iconSourceMode,
                        tintArgb = tintArgb,
                        selectedIconPack = selectedIconPack,
                        expandedNotificationKey = expandedNotificationKey,
                        onExpandToggle = { key ->
                            expandedNotificationKey = if (expandedNotificationKey == key) null else key
                        },
                        onActionClick = { notifKey, actionIndex ->
                            val executed = NotificationService.executeNotificationAction(notifKey, actionIndex)
                            if (!executed) {
                                displayNotifications.find { it.notif.notificationKey == notifKey }?.let { hn ->
                                    NotificationManager.getInstance(ctx)
                                        .openNotification(hn.packageName, hn.notif.notificationKey, hn.notif.conversationId, removeAfterOpen = true)
                                }
                            }
                            expandedNotificationKey = null
                        },
                        onDismiss = { hn ->
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                            if (hn.notif.notificationKey != null) {
                                NotificationService.dismissNotification(hn.notif.notificationKey)
                            }
                            NotificationManager.getInstance(ctx).removeConversationNotification(hn.packageName, hn.notif.conversationId)
                            expandedNotificationKey = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onEditModeClick() }
            )
            EditModeOverlay()
        }
    }
}

@Composable
private fun HubCategoryTabsRow(
    prefs: Prefs,
    tabs: List<CategoryTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    isDpadMode: Boolean,
    isFocused: Boolean,
    selectedDpadIndex: Int
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tabCount = tabs.size
        val minGap = 6.dp
        val screenScale = rememberScreenScale()
        val maxButtonSize = 48.dp.scaled(screenScale)
        val calculatedSize = (maxWidth - minGap * (tabCount - 1)) / tabCount
        val buttonSize = calculatedSize.coerceAtMost(maxButtonSize)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                HubCategoryIconButton(
                    prefs = prefs,
                    tab = tab,
                    isSelected = index == selectedIndex,
                    isDpadFocused = isFocused && index == selectedDpadIndex,
                    onClick = { onTabSelected(index) },
                    buttonSize = buttonSize
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HubCategoryIconButton(
    prefs: Prefs,
    tab: CategoryTab,
    isSelected: Boolean,
    isDpadFocused: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp = 40.dp
) {
    val textIslandsShape = prefs.textIslandsShape
    val buttonShape = remember(textIslandsShape) {
        when (textIslandsShape) {
            0 -> CircleShape
            1 -> RoundedCornerShape(8.dp)
            else -> RoundedCornerShape(0.dp)
        }
    }
    val bgColor = if (isSelected) Theme.colors.text else Theme.colors.background
    val iconTint = if (isSelected) Theme.colors.background else Theme.colors.text
    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val iconSize = buttonSize * 0.5f
    val highlightSizePx = with(density) { (buttonSize + 8.dp).toPx() }

    Box(
        modifier = Modifier
            .size(buttonSize)
            .then(
                if (isDpadFocused) {
                    Modifier.drawBehind {
                        val cornerRadius = when (textIslandsShape) {
                            0 -> highlightSizePx / 2f
                            1 -> with(density) { 8.dp.toPx() }
                            else -> 0f
                        }
                        drawRoundRect(
                            color = highlightColor,
                            topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                            size = Size(highlightSizePx, highlightSizePx),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                } else Modifier
            )
            .combinedClickable(
                onClick = {
                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                    onClick()
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(color = bgColor, shape = buttonShape)
            .then(
                if (!isSelected) Modifier.border(width = 1.5.dp, color = Theme.colors.text, shape = buttonShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (tab.icon != null) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun HubNotificationList(
    prefs: Prefs,
    notifications: List<HubNotification>,
    isDpadMode: Boolean,
    focusZone: HubFocusZone,
    selectedNotificationIndex: Int,
    iconShape: IconShape = IconShape.PILL,
    iconSourceMode: Int = 2,
    tintArgb: Int = 0,
    selectedIconPack: String = "",
    expandedNotificationKey: String? = null,
    onExpandToggle: (String) -> Unit = {},
    onActionClick: (notifKey: String, actionIndex: Int) -> Unit = { _, _ -> },
    onDismiss: (HubNotification) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val notificationBoxShape = remember(prefs.textIslandsShape) {
        when (prefs.textIslandsShape) {
            0 -> RoundedCornerShape(12.dp)
            1 -> RoundedCornerShape(8.dp)
            else -> RoundedCornerShape(0.dp)
        }
    }
    val ctx = LocalContext.current

    Column(
        modifier = modifier
            .padding(top = 12.dp, bottom = 16.dp)
            .border(1.5.dp, Theme.colors.text, notificationBoxShape)
    ) {
        notifications.forEachIndexed { index, hn ->
            key(hn.notif.conversationId) {
                val actions = hn.sbn.notification.actions?.toList() ?: emptyList()
                HubNotificationItem(
                    prefs = prefs,
                    packageName = hn.packageName,
                    notif = hn.notif,
                    showTopDivider = index > 0,
                    iconShape = iconShape,
                    iconSourceMode = iconSourceMode,
                    tintArgb = tintArgb,
                    selectedIconPack = selectedIconPack,
                    isDpadMode = isDpadMode,
                    isFocused = focusZone == HubFocusZone.NOTIFICATIONS && index == selectedNotificationIndex,
                    isExpanded = expandedNotificationKey == hn.notif.notificationKey,
                    actions = actions,
                    onLongPress = { onExpandToggle(hn.notif.notificationKey ?: "") },
                    onTap = {
                        if (expandedNotificationKey == hn.notif.notificationKey) {
                            onExpandToggle(hn.notif.notificationKey ?: "")
                        } else {
                            if (expandedNotificationKey != null) onExpandToggle("")
                            NotificationManager.getInstance(ctx)
                                .openNotification(hn.packageName, hn.notif.notificationKey, hn.notif.conversationId, removeAfterOpen = true)
                        }
                    },
                    onActionClick = { actionIndex -> onActionClick(hn.notif.notificationKey ?: "", actionIndex) },
                    onDismiss = { onDismiss(hn) },
                    onIconTap = {
                        try { openNotificationSettingsForPackage(ctx, hn.packageName) } catch (_: Exception) {}
                    },
                    isFirst = index == 0,
                    isLast = index == notifications.lastIndex
                )
            }
        }
    }
}

@Composable
private fun HubNotificationItem(
    prefs: Prefs,
    packageName: String,
    notif: NotificationManager.ConversationNotification,
    showTopDivider: Boolean,
    iconShape: IconShape = IconShape.PILL,
    iconSourceMode: Int = 2,
    tintArgb: Int = 0,
    selectedIconPack: String = "",
    isDpadMode: Boolean = false,
    isFocused: Boolean = false,
    isExpanded: Boolean = false,
    actions: List<Notification.Action> = emptyList(),
    onLongPress: () -> Unit = {},
    onTap: () -> Unit = {},
    onActionClick: (Int) -> Unit = {},
    onDismiss: () -> Unit = {},
    onIconTap: () -> Unit = {},
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val context = LocalContext.current
    val screenScale = rememberScreenScale()

    val appLabelInfo = remember(packageName) {
        try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) { packageName }
    }

    val title: String = remember(notif.conversationTitle, notif.sender, prefs.showNotificationGroupName) {
        when {
            !notif.sender.isNullOrBlank() -> notif.sender
            prefs.showNotificationGroupName && !notif.conversationTitle.isNullOrBlank() -> notif.conversationTitle
            else -> null
        } ?: appLabelInfo
    }

    val timeString = remember(notif.timestamp) {
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        timeFormat.format(notif.timestamp)
    }

    val message: String? = remember(notif.message) { notif.message?.takeIf { it.isNotBlank() } }

    val notifTypeface = remember(prefs.notificationsFont, prefs.getCustomFontPathForContext("notifications")) {
        try { prefs.getFontForContext("notifications").getFont(context, prefs.getCustomFontPathForContext("notifications")) }
        catch (_: Exception) { null }
    }
    val notifFontFamily = remember(notifTypeface) { notifTypeface?.let { FontFamily(it) } ?: FontFamily.Default }

    val appsTypeface = remember(prefs.appsFont, prefs.getCustomFontPathForContext("apps")) {
        try { prefs.getFontForContext("apps").getFont(context, prefs.getCustomFontPathForContext("apps")) }
        catch (_: Exception) { null }
    }
    val appsFontFamily = remember(appsTypeface) { appsTypeface?.let { FontFamily(it) } ?: FontFamily.Default }

    val iconSizePx = with(LocalDensity.current) { 32.dp.toPx().toInt() }
    val imageBitmap = if (iconSourceMode != 0) {
        rememberAppIconBitmap(
            context, packageName, iconSourceMode, selectedIconPack, iconSizePx,
            tintArgb = tintArgb, iconShapeId = prefs.iconShape,
            iconTintContrast = prefs.iconTintContrast,
            bgArgb = if (iconSourceMode == 6) Theme.colors.background.toArgb() else 0
        )
    } else null

    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val textIslandsShape = prefs.textIslandsShape
    val highlightShape = remember(textIslandsShape, isFirst, isLast) {
        val r = when (textIslandsShape) {
            0 -> 12.dp
            1 -> 8.dp
            else -> 0.dp
        }
        RoundedCornerShape(
            topStart = if (isFirst) r else 0.dp,
            topEnd = if (isFirst) r else 0.dp,
            bottomStart = if (isLast) r else 0.dp,
            bottomEnd = if (isLast) r else 0.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDpadMode && isFocused) {
                    Modifier.background(color = highlightColor, shape = highlightShape)
                } else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                        onLongPress()
                    }
                )
            }
            .pointerInput(Unit) {
                var dragSum = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount -> dragSum += dragAmount },
                    onDragEnd = {
                        val threshold = with(density) { 60.dp.toPx() }
                        if (kotlin.math.abs(dragSum) > threshold) {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                            onDismiss()
                        }
                        dragSum = 0f
                    }
                )
            }
    ) {
        if (showTopDivider) {
            val dividerColor = Theme.colors.text
            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                val dashWidth = 4f
                val gapWidth = 4f
                var x = 0f
                val y = size.height / 2
                while (x < size.width) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(x, y),
                        end = Offset((x + dashWidth).coerceAtMost(size.width), y),
                        strokeWidth = size.height
                    )
                    x += dashWidth + gapWidth
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconBox(
                iconSourceMode = iconSourceMode,
                iconShape = iconShape,
                size = 32.dp,
                showBackground = false,
                backgroundColor = Theme.colors.background,
                showBorder = IconShapeUtility.borderWidthForMode(iconSourceMode) != null,
                borderColor = Theme.colors.text,
                modifier = if (isExpanded) Modifier.clickable { onIconTap() } else Modifier
            ) {
                if (iconSourceMode == 0) {
                    val letterCode = remember(appLabelInfo) { IconUtility.generateCodeForLabel(appLabelInfo) }
                    Text(
                        text = letterCode,
                        color = Theme.colors.text,
                        fontSize = if (letterCode.length > 1) (prefs.notificationsTextSize - 3).sp.scaled(screenScale) else prefs.notificationsTextSize.sp.scaled(screenScale),
                        fontWeight = FontWeight.Bold,
                        fontFamily = appsFontFamily,
                        textAlign = TextAlign.Center
                    )
                } else if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = appLabelInfo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp.scaled(screenScale)))

            Box(modifier = Modifier.weight(1f)) {
                val textAlpha = if (isExpanded) 0f else 1f
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Theme.colors.text.copy(alpha = textAlpha),
                            fontSize = prefs.notificationsTextSize.sp.scaled(screenScale),
                            fontWeight = FontWeight.Bold,
                            fontFamily = notifFontFamily,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeString,
                            color = Theme.colors.text.copy(alpha = textAlpha),
                            fontSize = (prefs.notificationsTextSize - 4).sp.scaled(screenScale),
                            fontFamily = notifFontFamily
                        )
                    }
                    Text(
                        text = message ?: "",
                        color = Theme.colors.text.copy(alpha = textAlpha * 0.7f),
                        fontSize = (prefs.notificationsTextSize - 2).sp.scaled(screenScale),
                        fontFamily = notifFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isExpanded) {
                    Row(
                        modifier = Modifier.matchParentSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        actions.take(3).forEachIndexed { idx, action ->
                            HubActionPill(
                                prefs = prefs,
                                label = action.title?.toString() ?: stringResource(R.string.hub_action),
                                onClick = { onActionClick(idx) },
                                fontFamily = notifFontFamily
                            )
                        }
                        if (actions.isEmpty()) {
                            HubActionPill(
                                prefs = prefs,
                                label = stringResource(R.string.hub_open),
                                onClick = onTap,
                                fontFamily = notifFontFamily
                            )
                        }
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_dismiss),
                    tint = Theme.colors.text,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onDismiss)
                )
            }
        }
    }
}

@Composable
private fun HubActionPill(
    prefs: Prefs,
    label: String,
    onClick: () -> Unit,
    fontFamily: FontFamily = FontFamily.Default
) {
    val screenScale = rememberScreenScale()
    val textIslandsShape = prefs.textIslandsShape
    val pillShape = remember(textIslandsShape) {
        when (textIslandsShape) {
            0 -> RoundedCornerShape(12.dp)
            1 -> RoundedCornerShape(6.dp)
            else -> RoundedCornerShape(0.dp)
        }
    }
    val icon = actionIconMap[label.lowercase()]
    if (icon != null) {
        Box(
            modifier = Modifier
                .border(1.5.dp, Theme.colors.text, pillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Theme.colors.text,
                modifier = Modifier.size(16.dp)
            )
        }
    } else {
        Text(
            text = label,
            color = Theme.colors.text,
            fontSize = (prefs.notificationsTextSize - 4).sp.scaled(screenScale),
            fontWeight = FontWeight.Medium,
            fontFamily = fontFamily,
            maxLines = 1,
            modifier = Modifier
                .border(1.5.dp, Theme.colors.text, pillShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HubClearAllRow(
    prefs: Prefs,
    isFocused: Boolean = false,
    total: Int? = null,
    isAllTab: Boolean = true,
    categoryLabel: String = "All",
    onClearAll: () -> Unit,
    fontFamily: FontFamily = FontFamily.Default
) {
    val textColor = Theme.colors.text
    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val textIslandsShape = prefs.textIslandsShape
    val screenScale = rememberScreenScale()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (total != null) {
            Text(
                text = "$categoryLabel ($total)",
                color = Theme.colors.text,
                fontSize = (prefs.notificationsTextSize - 2).sp.scaled(screenScale),
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamily
            )
        }
        Text(
            text = if (isAllTab) stringResource(R.string.hub_clear_all) else stringResource(R.string.hub_clear),
            color = textColor,
            fontSize = prefs.notificationsTextSize.sp.scaled(screenScale),
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            modifier = Modifier
                .then(
                    if (isFocused) {
                        Modifier.drawBehind {
                            val cornerRadius = when (textIslandsShape) {
                                0 -> size.height / 2f
                                1 -> with(density) { 8.dp.toPx() }
                                else -> 0f
                            }
                            drawRoundRect(
                                color = highlightColor,
                                topLeft = Offset(-8.dp.toPx(), -4.dp.toPx()),
                                size = Size(size.width + 16.dp.toPx(), size.height + 8.dp.toPx()),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    } else Modifier
                )
                .clickable(onClick = onClearAll)
        )
    }
}

// ================================================================================
// SIMPLE TRAY HELPERS
// ================================================================================

internal fun convertSbnToSimpleTrayConversationNotification(
    ctx: Context,
    prefsLocal: Prefs,
    sbn: StatusBarNotification
): NotificationManager.ConversationNotification {
    val packageName = sbn.packageName
    val extras = sbn.notification.extras
    val showGroup = prefsLocal.showNotificationGroupName

    val conversationTitleRaw = if (showGroup) {
        extras?.getCharSequence("android.conversationTitle")?.toString()
            ?: extras?.getString("android.conversationTitle")
    } else null

    val titleRaw = extras?.getCharSequence("android.title")?.toString()
        ?: extras?.getString("android.title")

    val senderRaw = if (!titleRaw.isNullOrBlank() && titleRaw.contains(": ")) {
        titleRaw.split(": ", limit = 2).firstOrNull()?.trim()
    } else titleRaw
        ?: extras?.getCharSequence("android.subText")?.toString()
        ?: extras?.getString("android.subText")

    val messageRaw = when {
        extras?.getCharSequence("android.bigText") != null ->
            extras.getCharSequence("android.bigText")?.toString()
        extras?.getCharSequence("android.text") != null ->
            extras.getCharSequence("android.text")?.toString()
        extras?.getCharSequenceArray("android.textLines") != null ->
            extras.getCharSequenceArray("android.textLines")?.lastOrNull()?.toString()
        extras?.getCharSequence("android.summaryText") != null ->
            extras.getCharSequence("android.summaryText")?.toString()
        extras?.getCharSequence("android.infoText") != null ->
            extras.getCharSequence("android.infoText")?.toString()
        sbn.notification.tickerText != null ->
            sbn.notification.tickerText?.toString()
        else -> null
    }

    val message = if (!messageRaw.isNullOrBlank() &&
        !conversationTitleRaw.isNullOrBlank() &&
        messageRaw.trim().equals(conversationTitleRaw.trim(), ignoreCase = true)) {
        null
    } else messageRaw

    val notificationGroup = sbn.notification.group
    val conversationId = when {
        !notificationGroup.isNullOrBlank() -> "group_${packageName}_$notificationGroup"
        !conversationTitleRaw.isNullOrBlank() -> conversationTitleRaw
        sbn.notification.category == Notification.CATEGORY_MESSAGE -> {
            val phoneNumber = extras?.getString("android.people")?.firstOrNull()?.toString()
                ?: extras?.getString("android.subText")
                ?: extras?.getString("android.summaryText")
                ?: senderRaw
            phoneNumber?.let { "sms_$it" } ?: senderRaw ?: "default"
        }
        !senderRaw.isNullOrBlank() -> senderRaw
        else -> sbn.key
    }

    var conversationTitle = if (showGroup) conversationTitleRaw else null
    var sender = senderRaw
    var finalMessage = message?.replace("\n", " ")
        ?.replace("\r", " ")?.trim()
        ?.replace(Regex("\\s+"), " ")?.take(300)

    if ((conversationTitle.isNullOrBlank() && sender.isNullOrBlank()) && finalMessage.isNullOrBlank()) {
        val pm = ctx.packageManager
        val appLabel = try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) { packageName }
        conversationTitle = appLabel
        sender = appLabel
        finalMessage = when (sbn.notification.category) {
            Notification.CATEGORY_MESSAGE -> "New message"
            Notification.CATEGORY_EMAIL -> "New email"
            Notification.CATEGORY_CALL -> "Missed call"
            Notification.CATEGORY_ALARM -> "Alarm"
            Notification.CATEGORY_REMINDER -> "Reminder"
            Notification.CATEGORY_EVENT -> "Event"
            Notification.CATEGORY_PROMO -> "Promotion"
            Notification.CATEGORY_TRANSPORT -> "Media playing"
            Notification.CATEGORY_SYSTEM -> "System notification"
            Notification.CATEGORY_SERVICE -> "Service notification"
            Notification.CATEGORY_ERROR -> "Error"
            Notification.CATEGORY_PROGRESS -> "Progress update"
            Notification.CATEGORY_SOCIAL -> "Social update"
            Notification.CATEGORY_STATUS -> "Status update"
            Notification.CATEGORY_RECOMMENDATION -> "Recommendation"
            else -> null
        }
    }

    return NotificationManager.ConversationNotification(
        conversationId = conversationId,
        conversationTitle = conversationTitle,
        sender = sender,
        message = finalMessage,
        timestamp = sbn.postTime,
        category = sbn.notification.category,
        notificationKey = sbn.key
    )
}

// ================================================================================
// SIMPLE TRAY SCREEN
// ================================================================================

@Composable
fun SimpleTrayScreen(
    prefs: Prefs,
    composeView: ComposeView,
    simpleTrayViewModel: SimpleTrayViewModel?,
    requestBluetoothPermission: (onGranted: () -> Unit) -> Unit,
    requestPhoneStatePermission: (onGranted: () -> Unit) -> Unit,
    requestCameraPermission: (onGranted: () -> Unit) -> Unit,
    onShowPermissionExplanationDialog: (title: String, message: String, onContinue: () -> Unit) -> Unit,
    onLaunchBluetoothPermission: () -> Unit,
    onLaunchCameraPermission: () -> Unit,
    onLaunchPhoneStatePermission: () -> Unit,
    onRequestWriteSettingsPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    focusRestoreTrigger: Int = 0,
    isEditMode: Boolean = false,
    onNotificationEditModeClick: () -> Unit = {},
    onBackgroundEditModeClick: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val wifiPanelAction = "android.settings.panel.action.WIFI"
    val internetPanelAction = "android.settings.panel.action.INTERNET_CONNECTIVITY"

    var hasNotifPermission by remember {
        mutableStateOf(androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName))
    }
    var hasShownNotifDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasNotifPermission = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    if (!hasNotifPermission && !hasShownNotifDialog) {
        LaunchedEffect(Unit) {
            hasShownNotifDialog = true
            onShowPermissionExplanationDialog(
                ctx.getString(R.string.perm_notification_title),
                ctx.getString(R.string.perm_notification_body)
            ) {
                try {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } catch (_: Exception) {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", ctx.packageName, null)
                    })
                }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRestoreTrigger) {
        kotlinx.coroutines.delay(400)
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    val rawNotifications by NotificationService.sbnState.collectAsState(initial = emptyList())
    val prefsLocal = Prefs(ctx)
    val notificationManager = NotificationManager.getInstance(ctx)
    val screenScale = rememberScreenScale()

    val allowed = prefsLocal.allowedSimpleTrayApps

    val notificationsFromSbn = rawNotifications
        .filter { sbn ->
            if (sbn.notification.category == Notification.CATEGORY_TRANSPORT) return@filter false
            !notificationManager.isNotificationSummary(sbn) &&
                (allowed.isEmpty() || allowed.contains(sbn.packageName))
        }
        .map { sbn -> sbn.packageName to convertSbnToSimpleTrayConversationNotification(ctx, prefsLocal, sbn) }

    val audioWidgetHelper = AudioWidgetHelper.getInstance(ctx)
    val currentMediaPlayer by audioWidgetHelper.mediaPlayerState.collectAsState()
    val mediaNotifications: List<Pair<String, NotificationManager.ConversationNotification>> =
        currentMediaPlayer?.let { player ->
            if (allowed.isNotEmpty() && !allowed.contains(player.packageName)) {
                emptyList()
            } else {
                val mediaNotif = NotificationManager.ConversationNotification(
                    conversationId = "media_${player.packageName}",
                    conversationTitle = null,
                    sender = player.title ?: "Media playing",
                    message = player.artist,
                    timestamp = System.currentTimeMillis(),
                    category = Notification.CATEGORY_TRANSPORT,
                    notificationKey = null
                )
                listOf(player.packageName to mediaNotif)
            }
        } ?: emptyList()

    val validNotifications = (notificationsFromSbn + mediaNotifications)
        .sortedByDescending { (_, notif) -> notif.timestamp }

    val isDpadModeState = remember { mutableStateOf(false) }
    var isDpadMode by isDpadModeState
    val focusZone = remember(validNotifications.size) {
        mutableStateOf(
            if (validNotifications.isEmpty()) SimpleTrayFocusZone.CLEAR_ALL
            else SimpleTrayFocusZone.NOTIFICATIONS
        )
    }
    val selectedQuickSettingIndex = remember { mutableIntStateOf(0) }
    val selectedNotificationIndex = remember { mutableIntStateOf(0) }
    val selectedBottomNavIndex = remember { mutableIntStateOf(0) }

    LaunchedEffect(validNotifications.size) {
        if (validNotifications.isEmpty() && focusZone.value == SimpleTrayFocusZone.NOTIFICATIONS) {
            focusZone.value = SimpleTrayFocusZone.CLEAR_ALL
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val pageSize = prefsLocal.notificationsPerPage
    val pages = ((validNotifications.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val pagerState = remember { SimplePagerState(0, pages) }

    LaunchedEffect(validNotifications.size) {
        val oldPages = pagerState.pageCount
        pagerState.pageCount = pages
        if (pages != oldPages) {
            pagerState.currentPage = 0
        } else if (pagerState.currentPage >= pages) {
            pagerState.currentPage = (pages - 1).coerceAtLeast(0)
        }
    }

    val displayNotifications = run {
        val safePage = pagerState.currentPage.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
        val startIndex = safePage * pageSize
        val totalSize = validNotifications.size
        val endIndex = (startIndex + pageSize).coerceAtMost(totalSize)
        if (startIndex < totalSize) validNotifications.subList(startIndex, endIndex) else emptyList()
    }

    val appContext = remember(ctx) { ctx.applicationContext }
    val telephonyManager = remember(appContext) {
        appContext.getSystemService(TelephonyManager::class.java)
    }

    fun currentWifiState(): Boolean {
        return try {
            val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                false
            } else {
                wm?.isWifiEnabled == true
            }
        } catch (_: Exception) { false }
    }

    fun currentBluetoothState(): Boolean {
        return try {
            val btManager = appContext.getSystemService(BluetoothManager::class.java)
            val adapter = btManager?.adapter ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
            try { adapter.isEnabled } catch (_: SecurityException) { false }
        } catch (_: Exception) { false }
    }

    fun currentCellState(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val capabilities = cm?.getNetworkCapabilities(cm.activeNetwork)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (_: Exception) { false }
    }

    fun currentMobileDataState(): Boolean {
        try {
            val airplane = try { Settings.Global.getInt(appContext.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) } catch (_: Exception) { 0 }
            if (airplane == 1) return false
        } catch (_: Exception) {}

        try {
            val sm = try { appContext.getSystemService(android.telephony.SubscriptionManager::class.java) } catch (_: Exception) { null }
            val activeCount = try { sm?.activeSubscriptionInfoCount ?: 0 } catch (_: Exception) { 0 }
            if (activeCount <= 0) return false
        } catch (_: Exception) {}

        val tm = telephonyManager ?: return currentCellState()
        return if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try { tm.isDataEnabled } catch (_: Exception) { currentCellState() }
        } else currentCellState()
    }

    fun readMobileToggleState(): Boolean {
        return try {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    telephonyManager?.isDataEnabled ?: try { Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentMobileDataState() }
                } catch (_: Exception) {
                    try { Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentMobileDataState() }
                }
            } else {
                try { Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentMobileDataState() }
            }
        } catch (_: Exception) { currentMobileDataState() }
    }

    val cameraManager = remember(appContext) { appContext.getSystemService(CameraManager::class.java) }
    val torchCameraId = remember(cameraManager) {
        cameraManager?.cameraIdList?.firstOrNull { id ->
            val characteristics = try { cameraManager.getCameraCharacteristics(id) } catch (_: Exception) { null }
            characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    val vmLocal = simpleTrayViewModel
    val brightnessState = vmLocal?.brightnessLevel?.collectAsState() ?: remember { mutableIntStateOf(prefs.brightnessLevel.coerceIn(0, 255)) }
    val brightnessLevel by brightnessState
    val brightness = if (brightnessLevel == 0) 0f else kotlin.math.sqrt((brightnessLevel / 255f).coerceIn(0f, 1f))

    val sliderToSystem: (Float) -> Int = { s ->
        val v = (s * s * 255f).toInt().coerceIn(0, 255)
        v
    }

    val onBrightnessChange: (Float) -> Unit = { newBrightness ->
        val systemValue = sliderToSystem(newBrightness)

        if (!Settings.System.canWrite(ctx)) {
            onRequestWriteSettingsPermission()
        }

        if (simpleTrayViewModel != null) {
            try { simpleTrayViewModel.setBrightness(systemValue) } catch (_: Exception) {}
        } else {
            prefs.brightnessLevel = systemValue
            if (systemValue > 0) {
                prefs.lastBrightnessLevel = systemValue
            }
            if (Settings.System.canWrite(ctx)) {
                try {
                    Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, systemValue)
                } catch (_: Exception) {}
            }
        }
    }

    val wifiState = vmLocal?.wifiEnabled?.collectAsState(initial = currentWifiState()) ?: remember { mutableStateOf(currentWifiState()) }
    val wifiEnabled by wifiState
    val bluetoothState = vmLocal?.bluetoothEnabled?.collectAsState(initial = currentBluetoothState()) ?: remember { mutableStateOf(currentBluetoothState()) }
    val bluetoothEnabled by bluetoothState
    val signalState = vmLocal?.mobileDataEnabled?.collectAsState(initial = run {
        try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }
    }) ?: remember { mutableStateOf(currentMobileDataState()) }
    val signalEnabled by signalState
    val flashlightState = vmLocal?.flashlightEnabled?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val flashlightEnabled by flashlightState
    val brightnessEnabled = brightnessLevel > 1

    val phonePermissionGranted = ContextCompat.checkSelfPermission(
        ctx,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPhoneStatePermission {
                try { simpleTrayViewModel?.refreshStates() } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(Unit) {
        if (vmLocal == null) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPhoneStatePermission { /* fallback refresh */ }
            }
        }
    }

    DisposableEffect(Unit) {
        simpleTrayViewModel?.refreshStates()
        onDispose { }
    }

    @SuppressLint("MissingPermission")
    DisposableEffect(telephonyManager, phonePermissionGranted) {
        simpleTrayViewModel?.refreshStates()
        onDispose { }
    }

    DisposableEffect(cameraManager, torchCameraId) {
        simpleTrayViewModel?.refreshStates()
        onDispose { }
    }

    fun openWifiPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(wifiPanelAction)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        try { ctx.startActivity(intent) } catch (_: Exception) {}
    }

    fun openInternetConnectivityPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(internetPanelAction)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }
        try { ctx.startActivity(intent) } catch (_: Exception) {}
    }

    fun openBluetoothSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 35) {
                ctx.showShortToast(ctx.getString(R.string.toast_android15_bluetooth))
            }
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT < 35) {
                Intent("android.settings.panel.action.BLUETOOTH")
            } else {
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun openWifiSettingsPage() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openNetworkSettingsPage() {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    LaunchedEffect(vmLocal) {
        if (vmLocal == null) return@LaunchedEffect
        launch {
            vmLocal.permissionRequests.collect { req ->
                when (req) {
                    SimpleTrayPermissionRequest.BluetoothConnect -> {
                        onShowPermissionExplanationDialog(
                            ctx.getString(R.string.perm_bluetooth_title),
                            ctx.getString(R.string.perm_bluetooth_body)
                        ) { onLaunchBluetoothPermission() }
                    }
                    SimpleTrayPermissionRequest.Camera -> {
                        onShowPermissionExplanationDialog(
                            ctx.getString(R.string.perm_camera_title),
                            ctx.getString(R.string.perm_camera_body)
                        ) { onLaunchCameraPermission() }
                    }
                    SimpleTrayPermissionRequest.ReadPhoneState -> {
                        onShowPermissionExplanationDialog(
                            ctx.getString(R.string.perm_phone_title),
                            ctx.getString(R.string.perm_phone_body)
                        ) { onLaunchPhoneStatePermission() }
                    }
                }
            }
        }
        launch {
            vmLocal.uiEvents.collect { ev ->
                when (ev) {
                    SimpleTrayUiEvent.OpenWifiPanel -> openWifiPanel()
                    SimpleTrayUiEvent.OpenInternetPanel -> openInternetConnectivityPanel()
                    SimpleTrayUiEvent.OpenBluetoothSettings -> openBluetoothSettings()
                }
            }
        }
    }

    fun toggleBluetoothState(): Boolean {
        if (Build.VERSION.SDK_INT >= 35) {
            openBluetoothSettings()
            return bluetoothEnabled
        }
        val btManager = appContext.getSystemService(BluetoothManager::class.java)
        val adapter = btManager?.adapter ?: return bluetoothEnabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ctx.showShortToast(ctx.getString(R.string.toast_bluetooth_permission))
                return bluetoothEnabled
            }
        }
        return try {
            @Suppress("DEPRECATION")
            if (adapter.isEnabled) {
                adapter.disable()
                false
            } else {
                adapter.enable()
                true
            }
        } catch (_: SecurityException) {
            ctx.showShortToast(ctx.getString(R.string.toast_allow_bluetooth))
            bluetoothEnabled
        } catch (_: Exception) { bluetoothEnabled }
    }

    fun toggleFlashlight(): Boolean {
        val id = torchCameraId ?: run {
            ctx.showShortToast(ctx.getString(R.string.toast_no_flashlight))
            return flashlightEnabled
        }
        val manager = cameraManager ?: return flashlightEnabled
        return try {
            manager.setTorchMode(id, !flashlightEnabled)
            !flashlightEnabled
        } catch (_: SecurityException) {
            ctx.showShortToast(ctx.getString(R.string.toast_allow_camera))
            flashlightEnabled
        } catch (_: Exception) { flashlightEnabled }
    }

    fun toggleBrightness(): Boolean {
        val newBrightness = if (brightnessEnabled) {
            val currentSystemValue = sliderToSystem(brightness)
            if (currentSystemValue > 1) {
                prefs.lastBrightnessLevel = currentSystemValue
            }
            0f
        } else {
            val savedPref = prefs.lastBrightnessLevel.coerceIn(2, 255)
            kotlin.math.sqrt((savedPref / 255f).coerceIn(0f, 1f))
        }
        onBrightnessChange(newBrightness)
        return newBrightness > 0f
    }

    fun openBrightnessSettings() {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }

    DisposableEffect(pagerState, coroutineScope, validNotifications, composeView) {
        composeView.setTag(0xdeadbeef.toInt(), pagerState)
        composeView.setTag(0xcafebabe.toInt(), coroutineScope)
        composeView.setTag(0xabcdef01.toInt(), validNotifications)
        onDispose {
            composeView.setTag(0xdeadbeef.toInt(), null)
            composeView.setTag(0xcafebabe.toInt(), null)
            composeView.setTag(0xabcdef01.toInt(), null)
        }
    }

    val density = LocalDensity.current

    val notifTypefaceGlobal = remember(prefs.notificationsFont, prefs.getCustomFontPathForContext("notifications")) {
        try { prefs.getFontForContext("notifications").getFont(ctx, prefs.getCustomFontPathForContext("notifications")) }
        catch (_: Exception) { null }
    }
    val notifFontFamilyGlobal = remember(notifTypefaceGlobal) { notifTypefaceGlobal?.let { FontFamily(it) } ?: FontFamily.Default }
    val baseFontSize = prefs.notificationsTextSize * screenScale

    val pageDragThreshold = with(density) { 48.dp.toPx() }
    val keyPressTracker = remember { com.github.gezimos.inkos.ui.compose.KeyPressTracker() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusTarget()
            .onPreviewKeyEvent { keyEvent ->
                NavHelper.handleSimpleTrayKeyEvent(
                    keyEvent = keyEvent,
                    keyPressTracker = keyPressTracker,
                    isDpadModeSetter = { isDpadMode = it },
                    focusZone = focusZone,
                    selectedQuickSettingIndex = selectedQuickSettingIndex,
                    onQuickSettingActivate = { index ->
                        when (index) {
                            0 -> simpleTrayViewModel?.openWifiPanel() ?: openWifiPanel()
                            1 -> simpleTrayViewModel?.requestToggleBluetooth() ?: run {
                                val performToggle = {
                                    toggleBluetoothState()
                                    Unit
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                    requestBluetoothPermission(performToggle)
                                } else {
                                    performToggle()
                                }
                            }
                            2 -> {
                                simpleTrayViewModel?.openInternetPanel() ?: openInternetConnectivityPanel()
                                val refreshState = {
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(600)
                                        simpleTrayViewModel?.refreshStates() ?: run { }
                                    }
                                }
                                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                                    refreshState()
                                } else {
                                    requestPhoneStatePermission { refreshState() }
                                }
                            }
                            3 -> {
                                if (simpleTrayViewModel != null) {
                                    simpleTrayViewModel.requestToggleFlashlight()
                                } else {
                                    val toggle = {
                                        (flashlightState as? MutableState<Boolean>)?.value = toggleFlashlight()
                                    }
                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        toggle()
                                    } else {
                                        requestCameraPermission(toggle)
                                    }
                                }
                            }
                            4 -> { toggleBrightness() }
                        }
                    },
                    onBrightnessAdjust = { delta ->
                        onBrightnessChange((brightness + delta).coerceIn(0f, 1f))
                    },
                    onClearAll = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                        validNotifications.forEach { (pkg, notif) ->
                            try {
                                val isMedia = notif.category == Notification.CATEGORY_TRANSPORT
                                if (isMedia) {
                                    AudioWidgetHelper.getInstance(ctx).stopMedia()
                                } else {
                                    if (notif.notificationKey != null) {
                                        NotificationService.dismissNotification(notif.notificationKey)
                                    }
                                    NotificationManager.getInstance(ctx).removeConversationNotification(pkg, notif.conversationId)
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    selectedNotificationIndex = selectedNotificationIndex,
                    notificationsOnPageSize = displayNotifications.size,
                    pageSize = pageSize,
                    currentPage = pagerState.currentPage,
                    totalPages = pagerState.pageCount,
                    onNextPage = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < pagerState.pageCount - 1) {
                                pagerState.scrollToPage(pagerState.currentPage + 1)
                                vibratePaging(ctx)
                            }
                        }
                    },
                    onPreviousPage = {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.scrollToPage(pagerState.currentPage - 1)
                                vibratePaging(ctx)
                            }
                        }
                    },
                    onNotificationClick = { index ->
                        if (isEditMode) {
                            onNotificationEditModeClick()
                        } else {
                            displayNotifications.getOrNull(index)?.let { (pkg, notif) ->
                                NotificationManager.getInstance(ctx)
                                    .openNotification(pkg, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                            }
                        }
                    },
                    onNotificationLongClick = { index ->
                        displayNotifications.getOrNull(index)?.let { (pkg, notif) ->
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                            val isMedia = notif.category == Notification.CATEGORY_TRANSPORT
                            if (isMedia) {
                                AudioWidgetHelper.getInstance(ctx).stopMedia()
                            } else {
                                if (notif.notificationKey != null) {
                                    NotificationService.dismissNotification(notif.notificationKey)
                                }
                                NotificationManager.getInstance(ctx).removeConversationNotification(pkg, notif.conversationId)
                            }
                        }
                    },
                    selectedBottomNavIndex = selectedBottomNavIndex,
                    onBottomNavActivate = { index ->
                        if (isEditMode) {
                            onBackgroundEditModeClick()
                        } else {
                            when (index) {
                                0 -> onNavigateBack()
                                1 -> onNavigateToSettings()
                            }
                        }
                    },
                    bottomNavEnabled = prefsLocal.enableBottomNav
                )
            }
            .gestureHelper(onSwipeUp = {
                try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                onNavigateBack()
            })
            .then(
                if (isEditMode) {
                    Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        onBackgroundEditModeClick()
                    }
                } else Modifier
            )
            .background(Theme.colors.background)
            .inkOsSafeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp.scaled(screenScale),
                    end = 24.dp.scaled(screenScale),
                    top = 24.dp.scaled(screenScale),
                    bottom = 80.dp
                )
                .pointerInput(pagerState.currentPage, isDpadMode) {
                    var dragSum = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            isDpadModeState.value = false
                            dragSum += dragAmount
                        },
                        onDragEnd = {
                            if (dragSum < -pageDragThreshold) {
                                if (pagerState.currentPage < pagerState.pageCount - 1) {
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                    vibratePaging(ctx)
                                }
                            } else if (dragSum > pageDragThreshold) {
                                if (pagerState.currentPage > 0) {
                                    pagerState.scrollToPage(pagerState.currentPage - 1)
                                    vibratePaging(ctx)
                                }
                            }
                            dragSum = 0f
                        }
                    )
                }
        ) {
            QuickSettingsRow(
                prefs = prefs,
                isDpadMode = isDpadMode,
                focusZone = focusZone.value,
                selectedIndex = selectedQuickSettingIndex.intValue,
                wifiEnabled = wifiEnabled,
                onWifiToggle = { simpleTrayViewModel?.openWifiPanel() ?: openWifiPanel() },
                onWifiLongPress = { openWifiSettingsPage() },
                bluetoothEnabled = bluetoothEnabled,
                onBluetoothToggle = {
                    if (simpleTrayViewModel != null) {
                        simpleTrayViewModel.requestToggleBluetooth()
                    } else {
                        val performToggle = {
                            toggleBluetoothState()
                            Unit
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            requestBluetoothPermission(performToggle)
                        } else {
                            performToggle()
                        }
                    }
                },
                onBluetoothLongPress = { openBluetoothSettings() },
                signalEnabled = signalEnabled,
                onSignalToggle = {
                    if (simpleTrayViewModel != null) {
                        simpleTrayViewModel.openInternetPanel()
                        coroutineScope.launch {
                            val initial = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }
                            val attempts = 15
                            repeat(attempts) {
                                kotlinx.coroutines.delay(300)
                                val now = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }
                                if (now != initial) {
                                    try { simpleTrayViewModel.refreshStates() } catch (_: Exception) {}
                                    return@repeat
                                }
                            }
                        }
                    } else {
                        openInternetConnectivityPanel()
                        coroutineScope.launch {
                            val initial = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }
                            val attempts = 15
                            repeat(attempts) {
                                kotlinx.coroutines.delay(300)
                                val now = try { readMobileToggleState() } catch (_: Exception) { currentMobileDataState() }
                                if (now != initial) {
                                    (signalState as? MutableState<Boolean>)?.value = now
                                    return@repeat
                                }
                            }
                        }
                    }
                },
                onSignalLongPress = { openNetworkSettingsPage() },
                flashlightEnabled = flashlightEnabled,
                onFlashlightToggle = {
                    if (simpleTrayViewModel != null) {
                        simpleTrayViewModel.requestToggleFlashlight()
                    } else {
                        val toggle = {
                            (flashlightState as? MutableState<Boolean>)?.value = toggleFlashlight()
                        }
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            toggle()
                        } else {
                            requestCameraPermission(toggle)
                        }
                    }
                },
                brightnessEnabled = brightnessEnabled,
                onBrightnessToggle = { toggleBrightness() },
                onBrightnessLongPress = { openBrightnessSettings() }
            )

            Box(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
                BrightnessSlider(
                    prefs = prefs,
                    brightness = brightness,
                    onBrightnessChange = onBrightnessChange,
                    isDpadMode = isDpadModeState,
                    isFocused = isDpadMode && focusZone.value == SimpleTrayFocusZone.BRIGHTNESS_SLIDER
                )
            }

            if (validNotifications.isNotEmpty()) {
                SimpleTrayClearAllRow(
                    prefs = prefs,
                    isFocused = isDpadMode && focusZone.value == SimpleTrayFocusZone.CLEAR_ALL,
                    total = validNotifications.size,
                    onClearAll = {
                        try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                        validNotifications.forEach { (pkg, notif) ->
                            try {
                                if (notif.notificationKey != null) {
                                    NotificationService.dismissNotification(notif.notificationKey)
                                }
                                NotificationManager.getInstance(ctx).removeConversationNotification(pkg, notif.conversationId)
                            } catch (_: Exception) {}
                        }
                    },
                    fontFamily = notifFontFamilyGlobal
                )
            }

            if (validNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(
                            if (isEditMode) {
                                Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                    try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                                    onBackgroundEditModeClick()
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_foreground),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Theme.colors.text),
                            modifier = Modifier.size(36.dp.scaled(screenScale))
                        )
                        Spacer(modifier = Modifier.height(12.dp.scaled(screenScale)))
                        Text(
                            text = stringResource(id = R.string.no_notifications),
                            color = Theme.colors.text,
                            fontSize = baseFontSize.sp,
                            fontFamily = notifFontFamilyGlobal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                MergedNotificationCards(
                    prefs = prefs,
                    notifications = displayNotifications,
                    isDpadMode = isDpadMode,
                    focusZone = focusZone.value,
                    selectedNotificationIndex = selectedNotificationIndex.intValue,
                    isEditMode = isEditMode,
                    onEditModeTap = onNotificationEditModeClick
                )
            }
        }

        if (isEditMode) {
            EditModeOverlay()
        }

        BottomNavigationBar(
            prefs = prefs,
            isDpadMode = isDpadMode,
            focusZone = focusZone.value,
            selectedIndex = selectedBottomNavIndex.intValue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 0.dp),
            enabled = prefsLocal.enableBottomNav,
            totalNotifications = validNotifications.size,
            isEditMode = isEditMode,
            onEditModeClick = onBackgroundEditModeClick,
            onHomeClick = { onNavigateBack() },
            onSettingsClick = {
                try {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                } catch (_: Exception) {
                    onNavigateToSettings()
                }
            },
            centerContent = {}
        )
    }
}

@Composable
private fun QuickSettingsRow(
    prefs: Prefs,
    isDpadMode: Boolean,
    focusZone: SimpleTrayFocusZone,
    selectedIndex: Int,
    wifiEnabled: Boolean,
    onWifiToggle: () -> Unit,
    onWifiLongPress: (() -> Unit)? = null,
    bluetoothEnabled: Boolean,
    onBluetoothToggle: () -> Unit,
    onBluetoothLongPress: (() -> Unit)? = null,
    signalEnabled: Boolean,
    onSignalToggle: () -> Unit,
    onSignalLongPress: (() -> Unit)? = null,
    flashlightEnabled: Boolean,
    onFlashlightToggle: () -> Unit,
    brightnessEnabled: Boolean,
    onBrightnessToggle: () -> Unit,
    onBrightnessLongPress: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickSettingButton(
            prefs = prefs,
            icon = if (wifiEnabled) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
            enabled = wifiEnabled,
            onClick = onWifiToggle,
            onLongClick = onWifiLongPress,
            isFocused = isDpadMode && focusZone == SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 0
        )
        QuickSettingButton(
            prefs = prefs,
            icon = if (bluetoothEnabled) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
            enabled = bluetoothEnabled,
            onClick = onBluetoothToggle,
            onLongClick = onBluetoothLongPress,
            isFocused = isDpadMode && focusZone == SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 1
        )
        QuickSettingButton(
            prefs = prefs,
            icon = if (signalEnabled) Icons.Rounded.SignalCellularAlt else Icons.Rounded.SignalCellularOff,
            enabled = signalEnabled,
            onClick = onSignalToggle,
            onLongClick = onSignalLongPress,
            isFocused = isDpadMode && focusZone == SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 2
        )
        QuickSettingButton(
            prefs = prefs,
            icon = if (flashlightEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
            enabled = flashlightEnabled,
            onClick = onFlashlightToggle,
            isFocused = isDpadMode && focusZone == SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 3
        )
        QuickSettingButton(
            prefs = prefs,
            icon = if (brightnessEnabled) Icons.Rounded.BrightnessHigh else Icons.Rounded.BrightnessLow,
            enabled = true,
            isActive = brightnessEnabled,
            onClick = onBrightnessToggle,
            onLongClick = onBrightnessLongPress,
            isFocused = isDpadMode && focusZone == SimpleTrayFocusZone.QUICK_SETTINGS && selectedIndex == 4
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickSettingButton(
    prefs: Prefs,
    icon: ImageVector,
    enabled: Boolean,
    isActive: Boolean = enabled,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isFocused: Boolean = false
) {
    val handleClick: () -> Unit = {
        try { VibrationHelper.trigger(VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
        try { onClick() } catch (_: Exception) {}
    }
    val handleLong: (() -> Unit)? = if (onLongClick != null) {
        {
            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
            try { onLongClick() } catch (_: Exception) {}
        }
    } else null
    val bgColor = if (isActive) Theme.colors.text else Theme.colors.background
    val iconTint = if (isActive) Theme.colors.background else Theme.colors.text
    val borderColor = Theme.colors.text
    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val textIslandsShape = prefs.textIslandsShape
    val screenScale = rememberScreenScale()

    val buttonShape = remember(textIslandsShape) {
        when (textIslandsShape) {
            0 -> CircleShape
            1 -> RoundedCornerShape(8.dp)
            else -> RoundedCornerShape(0.dp)
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp.scaled(screenScale))
            .then(
                if (isFocused) {
                    Modifier.drawBehind {
                        val pad = 4.dp.toPx()
                        val highlightSize = size.width + pad * 2
                        val cornerRadius = when (textIslandsShape) {
                            0 -> highlightSize / 2f
                            1 -> with(density) { 8.dp.toPx() }
                            else -> 0f
                        }
                        drawRoundRect(
                            color = highlightColor,
                            topLeft = Offset(-pad, -pad),
                            size = Size(highlightSize, highlightSize),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }
                } else Modifier
            )
            .combinedClickable(
                onClick = handleClick,
                onLongClick = handleLong,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .background(color = bgColor, shape = buttonShape)
            .border(width = 1.5.dp, color = borderColor, shape = buttonShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp.scaled(screenScale))
        )
    }
}

@Composable
private fun BrightnessSlider(
    prefs: Prefs,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    isDpadMode: MutableState<Boolean>,
    isFocused: Boolean = false
) {
    val ctx = LocalContext.current
    fun sliderToSystem(s: Float): Int {
        val v = (s.coerceIn(0f, 1f) * s.coerceIn(0f, 1f) * 255f).toInt()
        return v.coerceIn(0, 255)
    }

    var internalBrightness by remember(brightness) { mutableFloatStateOf(brightness) }

    LaunchedEffect(brightness) {
        val newValue = brightness.coerceIn(0f, 1f)
        if (kotlin.math.abs(newValue - internalBrightness) > 0.02f) {
            internalBrightness = newValue
        }
    }

    BrightnessSliderBar(
        prefs = prefs,
        internalBrightness = internalBrightness,
        onBrightnessChanged = { newBrightness ->
            internalBrightness = newBrightness
            onBrightnessChange(newBrightness)
        },
        ctx = ctx,
        sliderToSystem = ::sliderToSystem,
        isDpadMode = isDpadMode,
        isFocused = isFocused
    )
}

@Composable
private fun BrightnessSliderBar(
    prefs: Prefs,
    internalBrightness: Float,
    onBrightnessChanged: (Float) -> Unit,
    ctx: Context,
    sliderToSystem: (Float) -> Int,
    isDpadMode: MutableState<Boolean>,
    isFocused: Boolean = false
) {
    var localLastWriteTime by remember { mutableLongStateOf(0L) }
    var localBrightness by remember { mutableFloatStateOf(internalBrightness) }

    LaunchedEffect(internalBrightness) {
        if (kotlin.math.abs(internalBrightness - localBrightness) > 0.01f) {
            localBrightness = internalBrightness
        }
    }

    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val textIslandsShape = prefs.textIslandsShape

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val totalDp = maxWidth

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(totalDp)
                    .height(32.dp)
                    .then(
                        if (isFocused) {
                            Modifier.drawBehind {
                                val cornerRadius = when (textIslandsShape) {
                                    0 -> size.height / 2f
                                    1 -> with(density) { 8.dp.toPx() }
                                    else -> 0f
                                }
                                drawRoundRect(
                                    color = highlightColor,
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )
                            }
                        } else Modifier
                    )
            ) {
                val sliderShape = remember(textIslandsShape) {
                    when (textIslandsShape) {
                        0 -> RoundedCornerShape(18.dp)
                        1 -> RoundedCornerShape(8.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(width = 1.5.dp, color = Theme.colors.text, shape = sliderShape)
                ) {
                    val totalBarHeight = 32.dp
                    val innerTrackHeight = 20.dp
                    val innerPadding = (totalBarHeight - innerTrackHeight) / 2
                    val innerTrackShape = remember(textIslandsShape) {
                        when (textIslandsShape) {
                            0 -> RoundedCornerShape(12.dp)
                            1 -> RoundedCornerShape(8.dp)
                            else -> RoundedCornerShape(0.dp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(color = Color.Transparent, shape = innerTrackShape)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    isDpadMode.value = false
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    var frac = (offset.x / w).coerceIn(0f, 1f)
                                    if (frac <= 0.05f) frac = 0f

                                    localBrightness = frac

                                    val intVal = if (frac == 0f) 0 else sliderToSystem(frac)
                                    if (Settings.System.canWrite(ctx)) {
                                        BrightnessHelper.setBrightness(ctx, prefs, intVal)
                                        localLastWriteTime = System.currentTimeMillis()
                                    }

                                    onBrightnessChanged(frac)
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, _ ->
                                        isDpadMode.value = false
                                        val w = size.width.toFloat().coerceAtLeast(1f)
                                        var frac = (change.position.x / w).coerceIn(0f, 1f)
                                        if (frac <= 0.03f) frac = 0f

                                        localBrightness = frac

                                        val intVal = if (frac == 0f) 0 else sliderToSystem(frac)
                                        if (Settings.System.canWrite(ctx)) {
                                            val now = System.currentTimeMillis()
                                            if (now - localLastWriteTime >= 80L || intVal == 0) {
                                                if (BrightnessHelper.setBrightness(ctx, prefs, intVal)) {
                                                    localLastWriteTime = now
                                                }
                                            }
                                        }

                                        onBrightnessChanged(frac)
                                    },
                                    onDragEnd = {
                                        val finalInt = if (localBrightness == 0f) 0 else sliderToSystem(localBrightness)
                                        if (Settings.System.canWrite(ctx)) {
                                            BrightnessHelper.setBrightness(ctx, prefs, finalInt)
                                            localLastWriteTime = System.currentTimeMillis()
                                        }
                                    }
                                )
                            }
                    ) {
                        val innerWidth = totalDp - (innerPadding * 2)

                        if (localBrightness <= 0f) {
                            val emptyIndicatorShape = remember(textIslandsShape) {
                                when (textIslandsShape) {
                                    0 -> CircleShape
                                    1 -> RoundedCornerShape(4.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(20.dp.scaled(rememberScreenScale()))
                                    .background(color = Theme.colors.text, shape = emptyIndicatorShape)
                                    .align(Alignment.CenterStart)
                            )
                        } else {
                            val fillDp = (innerWidth * localBrightness).coerceAtLeast(20.dp)
                            val filledBarShape = remember(textIslandsShape) {
                                when (textIslandsShape) {
                                    0 -> RoundedCornerShape(12.dp)
                                    1 -> RoundedCornerShape(4.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(fillDp)
                                    .background(color = Theme.colors.text, shape = filledBarShape)
                                    .align(Alignment.CenterStart)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    prefs: Prefs,
    isDpadMode: Boolean,
    focusZone: SimpleTrayFocusZone,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    totalNotifications: Int,
    isEditMode: Boolean = false,
    onEditModeClick: () -> Unit = {},
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    centerContent: (@Composable () -> Unit)? = null
) {
    if (!enabled) return

    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val textIslandsShape = prefs.textIslandsShape
    val screenScale = rememberScreenScale()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp.scaled(screenScale), vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp.scaled(screenScale))
                .then(
                    if (isDpadMode && focusZone == SimpleTrayFocusZone.BOTTOM_NAV && selectedIndex == 0) {
                        Modifier.drawBehind {
                            val pad = 4.dp.toPx()
                            val highlightSize = size.width + pad * 2
                            val cornerRadius = when (textIslandsShape) {
                                0 -> highlightSize / 2f
                                1 -> with(density) { 8.dp.toPx() }
                                else -> 0f
                            }
                            drawRoundRect(
                                color = highlightColor,
                                topLeft = Offset(-pad, -pad),
                                size = Size(highlightSize, highlightSize),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    } else Modifier
                )
                .clickable(onClick = {
                    if (isEditMode) onEditModeClick() else onHomeClick()
                }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = "Home",
                tint = Theme.colors.text,
                modifier = Modifier.size(24.dp.scaled(screenScale))
            )
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (centerContent != null) {
                centerContent()
            } else if (totalNotifications > 0) {
                Text(
                    text = "$totalNotifications",
                    color = Theme.colors.text,
                    fontSize = (prefs.notificationsTextSize - 4).sp.scaled(screenScale),
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp.scaled(screenScale))
                .then(
                    if (isDpadMode && focusZone == SimpleTrayFocusZone.BOTTOM_NAV && selectedIndex == 1) {
                        Modifier.drawBehind {
                            val pad = 4.dp.toPx()
                            val highlightSize = size.width + pad * 2
                            val cornerRadius = when (textIslandsShape) {
                                0 -> highlightSize / 2f
                                1 -> with(density) { 8.dp.toPx() }
                                else -> 0f
                            }
                            drawRoundRect(
                                color = highlightColor,
                                topLeft = Offset(-pad, -pad),
                                size = Size(highlightSize, highlightSize),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    } else Modifier
                )
                .clickable(onClick = {
                    if (isEditMode) onEditModeClick() else onSettingsClick()
                }),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = Theme.colors.text,
                modifier = Modifier.size(24.dp.scaled(screenScale))
            )
        }
    }
}

@Composable
private fun SimpleTrayClearAllRow(
    prefs: Prefs,
    isFocused: Boolean = false,
    total: Int? = null,
    onClearAll: () -> Unit,
    fontFamily: FontFamily = FontFamily.Default
) {
    val textColor = Theme.colors.text
    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val textIslandsShape = prefs.textIslandsShape
    val screenScale = rememberScreenScale()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Clear All",
            color = textColor,
            fontSize = prefs.notificationsTextSize.sp.scaled(screenScale),
            fontWeight = FontWeight.Bold,
            fontFamily = fontFamily,
            modifier = Modifier
                .then(
                    if (isFocused) {
                        Modifier.drawBehind {
                            val cornerRadius = when (textIslandsShape) {
                                0 -> size.height / 2f
                                1 -> with(density) { 8.dp.toPx() }
                                else -> 0f
                            }
                            drawRoundRect(
                                color = highlightColor,
                                topLeft = Offset(-8.dp.toPx(), -4.dp.toPx()),
                                size = Size(size.width + 16.dp.toPx(), size.height + 8.dp.toPx()),
                                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                            )
                        }
                    } else Modifier
                )
                .clickable(onClick = onClearAll)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (total != null) {
                Text(
                    text = "$total",
                    color = Theme.colors.text,
                    fontSize = (prefs.notificationsTextSize - 2).sp.scaled(screenScale),
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            }
        }
    }
}

@Composable
private fun MergedNotificationCards(
    prefs: Prefs,
    notifications: List<Pair<String, NotificationManager.ConversationNotification>>,
    isDpadMode: Boolean,
    focusZone: SimpleTrayFocusZone,
    selectedNotificationIndex: Int,
    isEditMode: Boolean = false,
    onEditModeTap: () -> Unit = {}
) {
    val notificationBoxShape = remember(prefs.textIslandsShape) {
        when (prefs.textIslandsShape) {
            0 -> RoundedCornerShape(12.dp)
            1 -> RoundedCornerShape(8.dp)
            else -> RoundedCornerShape(0.dp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 24.dp)
            .border(width = 1.5.dp, color = Theme.colors.text, shape = notificationBoxShape)
    ) {
        notifications.forEachIndexed { index, (packageName, notif) ->
            key(notif.conversationId) {
                NotificationCardItem(
                    prefs = prefs,
                    packageName = packageName,
                    notif = notif,
                    showTopDivider = index > 0,
                    isDpadMode = isDpadMode,
                    isFocused = focusZone == SimpleTrayFocusZone.NOTIFICATIONS && index == selectedNotificationIndex,
                    isEditMode = isEditMode,
                    onEditModeTap = onEditModeTap,
                    isFirst = index == 0,
                    isLast = index == notifications.lastIndex
                )
            }
        }
    }
}

@Composable
private fun NotificationCardItem(
    prefs: Prefs,
    packageName: String,
    notif: NotificationManager.ConversationNotification,
    showTopDivider: Boolean,
    isDpadMode: Boolean = false,
    isFocused: Boolean = false,
    isEditMode: Boolean = false,
    onEditModeTap: () -> Unit = {},
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val context = LocalContext.current
    val screenScale = rememberScreenScale()
    val appLabelInfo = remember(packageName) {
        try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) { packageName }
    }

    val title: String = remember(notif.conversationTitle, notif.sender, prefs.showNotificationGroupName) {
        when {
            !notif.sender.isNullOrBlank() -> notif.sender
            prefs.showNotificationGroupName && !notif.conversationTitle.isNullOrBlank() -> notif.conversationTitle
            else -> null
        } ?: appLabelInfo
    }

    val isMediaNotification = notif.category == Notification.CATEGORY_TRANSPORT

    val mediaController = remember(notif.notificationKey, packageName, isMediaNotification) {
        if (isMediaNotification) {
            try {
                val audioHelper = AudioWidgetHelper.getInstance(context)
                val mediaPlayer = audioHelper.getCurrentMediaPlayer()
                if (mediaPlayer?.packageName == packageName) mediaPlayer.controller else null
            } catch (_: Exception) { null }
        } else null
    }

    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isMediaNotification, packageName) {
        if (isMediaNotification) {
            val audioHelper = AudioWidgetHelper.getInstance(context)
            audioHelper.mediaPlayerState.collect { mediaPlayer ->
                isPlaying = mediaPlayer?.packageName == packageName && mediaPlayer.isPlaying
            }
        }
    }

    var mediaTimeString by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaController, isMediaNotification) {
        if (isMediaNotification && mediaController != null) {
            while (true) {
                try {
                    val playbackState = mediaController.playbackState
                    val position = playbackState?.position ?: 0L
                    val metadata = mediaController.metadata
                    val duration = metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L

                    if (duration > 0) {
                        val elapsed = position / 1000
                        val total = duration / 1000
                        val remaining = (total - elapsed).coerceAtLeast(0)

                        fun formatTime(seconds: Long): String {
                            val mins = seconds / 60
                            val secs = seconds % 60
                            return String.format("%d:%02d", mins, secs)
                        }

                        mediaTimeString = if (remaining > 0) {
                            "-${formatTime(remaining)}"
                        } else {
                            "${formatTime(elapsed)}/${formatTime(total)}"
                        }
                    } else {
                        mediaTimeString = null
                    }
                } catch (_: Exception) {
                    mediaTimeString = null
                }
                kotlinx.coroutines.delay(if (isPlaying) 1000L else 2000L)
            }
        } else {
            mediaTimeString = null
        }
    }

    val defaultTimeString = remember(notif.timestamp) {
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        timeFormat.format(notif.timestamp)
    }
    val timeString = if (isMediaNotification && mediaTimeString != null) {
        mediaTimeString ?: defaultTimeString
    } else defaultTimeString

    val message: String? = remember(notif.message) { notif.message?.takeIf { it.isNotBlank() } }

    val notifTypeface = remember(prefs.notificationsFont, prefs.getCustomFontPathForContext("notifications")) {
        try { prefs.getFontForContext("notifications").getFont(context, prefs.getCustomFontPathForContext("notifications")) }
        catch (_: Exception) { null }
    }
    val notifFontFamily = remember(notifTypeface) { notifTypeface?.let { FontFamily(it) } ?: FontFamily.Default }
    val highlightColor = Theme.colors.text.copy(alpha = 0.3f)
    val textIslandsShape = prefs.textIslandsShape

    val highlightShape = remember(textIslandsShape, isFirst, isLast) {
        val r = when (textIslandsShape) {
            0 -> 12.dp
            1 -> 8.dp
            else -> 0.dp
        }
        RoundedCornerShape(
            topStart = if (isFirst) r else 0.dp,
            topEnd = if (isFirst) r else 0.dp,
            bottomStart = if (isLast) r else 0.dp,
            bottomEnd = if (isLast) r else 0.dp
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDpadMode && isFocused) {
                    Modifier.background(color = highlightColor, shape = highlightShape)
                } else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (isEditMode) {
                            onEditModeTap()
                        } else {
                            NotificationManager.getInstance(context)
                                .openNotification(packageName, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                        }
                    },
                    onLongPress = {
                        try {
                            try { VibrationHelper.trigger(VibrationHelper.Effect.SELECT) } catch (_: Exception) {}
                            if (isMediaNotification) {
                                AudioWidgetHelper.getInstance(context).stopMedia()
                            } else {
                                if (notif.notificationKey != null) {
                                    NotificationService.dismissNotification(notif.notificationKey)
                                }
                                NotificationManager.getInstance(context).removeConversationNotification(packageName, notif.conversationId)
                            }
                        } catch (_: Exception) {}
                    }
                )
            }
    ) {
        if (showTopDivider) {
            val dividerColor = Theme.colors.text
            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                val dashWidth = 4f
                val gapWidth = 4f
                var x = 0f
                val y = size.height / 2
                while (x < size.width) {
                    drawLine(
                        color = dividerColor,
                        start = Offset(x, y),
                        end = Offset((x + dashWidth).coerceAtMost(size.width), y),
                        strokeWidth = size.height
                    )
                    x += dashWidth + gapWidth
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Theme.colors.text,
                    fontSize = prefs.notificationsTextSize.sp.scaled(screenScale),
                    fontWeight = FontWeight.Bold,
                    fontFamily = notifFontFamily,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMediaNotification && mediaController != null) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Theme.colors.text,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    try { mediaController.transportControls.skipToPrevious() } catch (_: Exception) {}
                                }
                        )
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Theme.colors.text,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    try {
                                        if (isPlaying) mediaController.transportControls.pause()
                                        else mediaController.transportControls.play()
                                    } catch (_: Exception) {}
                                }
                        )
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Theme.colors.text,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    try { mediaController.transportControls.skipToNext() } catch (_: Exception) {}
                                }
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Notification settings",
                        tint = Theme.colors.text,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                try { openNotificationSettingsForPackage(context, packageName) } catch (_: Exception) {}
                            }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = appLabelInfo,
                        color = Theme.colors.text,
                        fontSize = (prefs.notificationsTextSize - 4).sp.scaled(screenScale),
                        fontFamily = notifFontFamily
                    )
                    Text(
                        text = "•",
                        color = Theme.colors.text,
                        fontSize = (prefs.notificationsTextSize - 4).sp.scaled(screenScale),
                        fontFamily = notifFontFamily
                    )
                    Text(
                        text = timeString,
                        color = Theme.colors.text,
                        fontSize = (prefs.notificationsTextSize - 4).sp.scaled(screenScale),
                        fontFamily = notifFontFamily
                    )
                }
            }

            Text(
                text = message ?: "",
                color = Theme.colors.text,
                fontSize = (prefs.notificationsTextSize - 2).sp.scaled(screenScale),
                fontFamily = notifFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
