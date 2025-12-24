package com.github.gezimos.inkos.ui.notifications

import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {
    private lateinit var prefs: Prefs
    private lateinit var vibrator: Vibrator
    private var wasPaused = false
    private var focusRestoreTriggerState: androidx.compose.runtime.MutableState<Int>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = try { androidx.lifecycle.ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java] } catch (_: Exception) { null }
        prefs = vm?.getPrefs() ?: Prefs(requireContext())
        vibrator = requireContext().getSystemService(Vibrator::class.java)
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            val isDark = prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
            // Create state for focus restoration trigger that can be observed by Compose
            val triggerState = remember { mutableStateOf(0) }
            focusRestoreTriggerState = triggerState
            SettingsTheme(isDark = isDark) {
                NotificationsScreen(composeView, focusRestoreTrigger = triggerState.value)
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // E-Ink refresh centralized in MainActivity; fragment-level call removed
        // Keep view focus so other key events can be received by the fragment if needed
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        // Disable default focus highlight to prevent transparent white hue/ripple when accessed via D-pad
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.setDefaultFocusHighlightEnabled(false)
        }
        view.requestFocus()

        // Local key listener remains responsible only for actions other than page up/down.
        // Page navigation (volume keys and optionally DPAD/pages) is forwarded from the Activity.
        view.setOnKeyListener { v, keyCode, event ->
            // Show quick hint when user presses left/right dpad keys to explain actions
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        showShortToast("Dismiss: Del, C, #1 | Long press to dismiss all")
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        showShortToast("Open: Enter, Dpad Center, #3")
                        return@setOnKeyListener true
                    }
                }
            }

            val mapped = com.github.gezimos.inkos.ui.compose.NavHelper.mapNotificationsKey(prefs, keyCode, event)
            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.None) return@setOnKeyListener false

            // If the mapped action is PageUp/PageDown, let the Activity handle it via the centralized handler.
            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageUp || mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageDown) {
                return@setOnKeyListener false
            }

            val composeView = v as? ComposeView
            val pagerState = composeView?.getTag(0xdeadbeef.toInt()) as? androidx.compose.foundation.pager.PagerState
            val coroutineScope = composeView?.getTag(0xcafebabe.toInt()) as? kotlinx.coroutines.CoroutineScope
            val validNotifications = (composeView?.getTag(0xabcdef01.toInt()) as? List<*>)
                ?.filterIsInstance<Pair<String, NotificationManager.ConversationNotification>>()

            if (pagerState == null || coroutineScope == null || validNotifications == null) {
                android.util.Log.d("NotificationsFragment", "pagerState, coroutineScope, or validNotifications is null")
                return@setOnKeyListener false
            }

            when (mapped) {
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Dismiss -> {
                    // Dismiss current notification
                    val (pkg, notif) = validNotifications.getOrNull(pagerState.currentPage) ?: return@setOnKeyListener true
                    // Dismiss notification from system tray (mark as read/snooze)
                    if (notif.notificationKey != null) {
                        com.github.gezimos.inkos.services.NotificationService.dismissNotification(notif.notificationKey)
                    }
                    // Remove from conversation notifications
                    NotificationManager.getInstance(requireContext())
                        .removeConversationNotification(pkg, notif.conversationId)
                    coroutineScope.launch {
                        val nextPage = when {
                            pagerState.currentPage == validNotifications.lastIndex && pagerState.currentPage > 0 -> pagerState.currentPage - 1
                            pagerState.currentPage < validNotifications.lastIndex -> pagerState.currentPage
                            else -> 0
                        }
                        kotlinx.coroutines.delay(150)
                        if (validNotifications.size > 1) {
                            pagerState.scrollToPage(nextPage)
                        }
                    }
                    true
                }
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Open -> {
                    val (pkg, notif) = validNotifications.getOrNull(pagerState.currentPage) ?: return@setOnKeyListener true
                    NotificationManager.getInstance(requireContext())
                        .openNotification(pkg, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                    // Navigate to previous page if needed
                    coroutineScope.launch {
                        val currentPage = pagerState.currentPage
                        if (currentPage >= validNotifications.size - 1 && currentPage > 0) {
                            pagerState.scrollToPage(currentPage - 1)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Register Activity-level page navigation handler so volume and page/DPAD keys are forwarded
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        
        // Always restore focus after screen unlock (when wasPaused is true)
        // This fixes the issue where D-pad stops working after closing/reopening flip phone
        if (wasPaused) {
            wasPaused = false
            // Restore focus to fragment view
            view?.postDelayed({
                val currentView = view
                if (isAdded && !isDetached && currentView != null) {
                    try {
                        currentView.isFocusable = true
                        currentView.isFocusableInTouchMode = true
                        // Disable default focus highlight to prevent transparent white hue/ripple
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentView.setDefaultFocusHighlightEnabled(false)
                        }
                        currentView.requestFocus()
                    } catch (_: Exception) {}
                }
            }, 350) // Delay to let MainActivity's onResume and window focus changes complete
            
            // Also trigger Compose focus restoration by incrementing a counter
            // This will be passed to NotificationsScreen to re-request Compose focus if needed
            focusRestoreTriggerState?.value = (focusRestoreTriggerState?.value ?: 0) + 1
        }
        
        act.pageNavigationHandler = object : com.github.gezimos.inkos.MainActivity.PageNavigationHandler {
            override val handleDpadAsPage: Boolean = true

            override fun pageUp() {
                val composeView = view as? ComposeView ?: return
                val pagerState = composeView.getTag(0xdeadbeef.toInt()) as? androidx.compose.foundation.pager.PagerState
                val coroutineScope = composeView.getTag(0xcafebabe.toInt()) as? kotlinx.coroutines.CoroutineScope
                (composeView.getTag(0xabcdef01.toInt()) as? List<*>)
                    ?.filterIsInstance<Pair<String, NotificationManager.ConversationNotification>>() ?: return
                if (pagerState == null || coroutineScope == null) return

                coroutineScope.launch {
                    if (pagerState.currentPage > 0) {
                        pagerState.scrollToPage(pagerState.currentPage - 1)
                        vibratePaging()
                    } else if (pagerState.pageCount > 0) {
                        pagerState.scrollToPage(pagerState.pageCount - 1)
                        vibratePaging()
                    }
                }
            }

            override fun pageDown() {
                val composeView = view as? ComposeView ?: return
                val pagerState = composeView.getTag(0xdeadbeef.toInt()) as? androidx.compose.foundation.pager.PagerState
                val coroutineScope = composeView.getTag(0xcafebabe.toInt()) as? kotlinx.coroutines.CoroutineScope
                (composeView.getTag(0xabcdef01.toInt()) as? List<*>)
                    ?.filterIsInstance<Pair<String, NotificationManager.ConversationNotification>>() ?: return
                if (pagerState == null || coroutineScope == null) return

                coroutineScope.launch {
                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                        pagerState.scrollToPage(pagerState.currentPage + 1)
                        vibratePaging()
                    } else if (pagerState.pageCount > 0) {
                        pagerState.scrollToPage(0)
                        vibratePaging()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        wasPaused = true
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        if (act.pageNavigationHandler != null) act.pageNavigationHandler = null
    }

    @Composable
    fun NotificationsScreen(composeView: ComposeView, focusRestoreTrigger: Int = 0) {
        val notificationsMap by rememberNotifications()
        
        // Cache font loading to avoid repeated operations
        val notifFontFamily = remember {
            val notifFont = prefs.getFontForContext("notifications")
                .getFont(requireContext(), prefs.getCustomFontPathForContext("notifications"))
            notifFont?.let { FontFamily(it) } ?: FontFamily.Default
        }
        
        val notifTextSize = remember { prefs.notificationsTextSize.sp }
        val notifTitle = remember { prefs.lettersTitle }
        
        val notifTitleFontFamily = remember {
            val notifTitleFont = prefs.lettersTitleFont.getFont(
                requireContext(),
                prefs.getCustomFontPath("lettersTitle")
            )
            notifTitleFont?.let { FontFamily(it) } ?: FontFamily.Default
        }
        
        val notifTitleSize = remember { prefs.lettersTitleSize.sp }

        remember { prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark }

        // Check if notifications are enabled
        if (!prefs.notificationsEnabled) {
            findNavController().popBackStack()
            return
        }

        // Flatten all notifications into a single list (one per notification)
        val validNotifications = remember(notificationsMap) {
            notificationsMap.flatMap { (packageName, conversations) ->
                conversations.map { notif -> packageName to notif }
            }
        }
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { validNotifications.size }
        )
        val coroutineScope = rememberCoroutineScope()

        // Optimize: Single DisposableEffect for all ComposeView tags to reduce overhead
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

        // Custom swipe handling for instant page change
        val pagerModifier = Modifier
            .fillMaxSize()
            .pointerInput(validNotifications.size) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (totalDrag < -30) { // Swipe up
                            coroutineScope.launch {
                                if (pagerState.currentPage < validNotifications.lastIndex) {
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                    vibratePaging()
                                } else if (validNotifications.isNotEmpty()) {
                                    pagerState.scrollToPage(0) // Loop to first notification
                                    vibratePaging()
                                }
                            }
                        } else if (totalDrag > 30) { // Swipe down
                            coroutineScope.launch {
                                if (pagerState.currentPage > 0) {
                                    pagerState.scrollToPage(pagerState.currentPage - 1)
                                    vibratePaging()
                                } else if (validNotifications.isNotEmpty()) {
                                    pagerState.scrollToPage(validNotifications.lastIndex) // Loop to last notification
                                    vibratePaging()
                                }
                            }
                        }
                        totalDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume() // Use consume() as recommended by Compose deprecation warning
                        totalDrag += dragAmount
                    }
                )
            }

        // Detect navigation bar height for padding
        val view = LocalView.current
        val density = LocalDensity.current
        LocalConfiguration.current
        var navBarPadding by remember { mutableStateOf(0.dp) }
        var statusBarPadding by remember { mutableStateOf(0.dp) }
        LaunchedEffect(view) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.rootWindowInsets?.let { insets ->
                    val navBar = insets.getInsets(android.view.WindowInsets.Type.navigationBars())
                    navBarPadding = with(density) { navBar.bottom.toDp() }
                    val statusBar = insets.getInsets(android.view.WindowInsets.Type.statusBars())
                    statusBarPadding = with(density) { statusBar.top.toDp() }
                }
            } else {
                ViewCompat.setOnApplyWindowInsetsListener(view) { v: View, insets: WindowInsetsCompat ->
                    val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    navBarPadding = with(density) { navBar.bottom.toDp() }
                    val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                    statusBarPadding = with(density) { statusBar.top.toDp() }
                    insets
                }
            }
        }

        // Apply navigation bar padding to bottom content
        val contentBottomPadding = navBarPadding
        val actionBarBottomPadding = navBarPadding
        val contentTopPadding = statusBarPadding

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background)
        ) {

            if (validNotifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = contentTopPadding, bottom = contentBottomPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Show decorative panda drawable above the empty state text
                    Image(
                        painter = painterResource(id = R.drawable.ic_foreground),
                        colorFilter = ColorFilter.tint(Theme.colors.text),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.no_notifications),
                        color = Theme.colors.text,
                        fontSize = 18.sp,
                        fontFamily = notifFontFamily,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = pagerModifier,
                    userScrollEnabled = false,
                    pageSpacing = 0.dp
                ) { page ->
                    val (packageName, notif) = validNotifications[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = contentTopPadding, bottom = contentBottomPadding),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Title row at the top of the content
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = notifTitle,
                                style = SettingsTheme.typography.title,
                                fontSize = notifTitleSize,
                                fontWeight = FontWeight.Bold,
                                fontFamily = notifTitleFontFamily,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                        ConversationNotificationItem(
                            packageName = packageName,
                            notif = notif,
                            notifFontFamily = notifFontFamily,
                            titleFontSize = notifTextSize, // Use body font size for sender/group name
                            descriptionFontSize = notifTextSize
                        )
                    }
                }
                // Sticky bottom action bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = actionBarBottomPadding),
                    contentAlignment = Alignment.Center
                ) {
                    val canDismiss = validNotifications.isNotEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dismiss button with matching hitbox
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .pointerInput(validNotifications) {
                                    detectTapGestures(
                                        onTap = {
                                            if (canDismiss) {
                                                val (pkg, notif) = validNotifications.getOrNull(pagerState.currentPage)
                                                    ?: return@detectTapGestures
                                                // Dismiss notification from system tray (mark as read/snooze)
                                                if (notif.notificationKey != null) {
                                                    com.github.gezimos.inkos.services.NotificationService.dismissNotification(notif.notificationKey)
                                                }
                                                // Remove from conversation notifications
                                                NotificationManager.getInstance(requireContext())
                                                    .removeConversationNotification(pkg, notif.conversationId)
                                                coroutineScope.launch {
                                                    val nextPage = when {
                                                        pagerState.currentPage == validNotifications.lastIndex && pagerState.currentPage > 0 -> pagerState.currentPage - 1
                                                        pagerState.currentPage < validNotifications.lastIndex -> pagerState.currentPage
                                                        else -> 0
                                                    }
                                                    kotlinx.coroutines.delay(150)
                                                    if (validNotifications.size > 1) {
                                                        pagerState.scrollToPage(nextPage)
                                                    }
                                                }
                                                vibratePaging() // vibrate on dismiss tap if enabled
                                            }
                                        },
                                        onLongPress = {
                                            if (canDismiss) {
                                                dismissAllNotifications()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "DISMISS",
                                style = SettingsTheme.typography.title,
                                fontSize = notifTextSize,
                                fontWeight = FontWeight.Bold,
                                fontFamily = notifFontFamily,
                                color = if (canDismiss) SettingsTheme.typography.title.color else Color.Gray,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp)
                            )
                        }
                        // Open button with matching hitbox
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clickable {
                                    val (pkg, notif) = validNotifications.getOrNull(pagerState.currentPage)
                                        ?: return@clickable
                                    NotificationManager.getInstance(requireContext())
                                        .openNotification(pkg, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                                    vibratePaging() // vibrate on open tap if enabled
                                },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "OPEN",
                                style = SettingsTheme.typography.title,
                                fontSize = notifTextSize,
                                fontWeight = FontWeight.Bold,
                                fontFamily = notifFontFamily,
                                color = SettingsTheme.typography.title.color,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun vibratePaging() {
        com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.PAGE)
    }

    private fun dismissAllNotifications() {
        val notificationManager = NotificationManager.getInstance(requireContext())
        // Get all current notifications and dismiss them
        val allNotifications = notificationManager.conversationNotificationsState.value
        allNotifications.forEach { (packageName, conversations) ->
            conversations.forEach { notif ->
                // Dismiss notification from system tray (mark as read/snooze)
                if (notif.notificationKey != null) {
                    com.github.gezimos.inkos.services.NotificationService.dismissNotification(notif.notificationKey)
                }
                // Remove from conversation notifications
                notificationManager.removeConversationNotification(packageName, notif.conversationId)
            }
        }
        vibratePaging() // vibrate on dismiss all if enabled
        showShortToast("All notifications dismissed")
    }

    // settings shortcut intentionally not supported from notifications

    @Composable
    private fun rememberNotifications(): State<Map<String, List<NotificationManager.ConversationNotification>>> {
        val stateFlow = NotificationManager.getInstance(requireContext()).conversationNotificationsState
        val state = stateFlow.collectAsState(initial = emptyMap())
        return state
    }

    @Composable
    fun ConversationNotificationItem(
        packageName: String,
        notif: NotificationManager.ConversationNotification,
        notifFontFamily: FontFamily,
        titleFontSize: TextUnit,
        descriptionFontSize: TextUnit
    ) {
        val context = requireContext()
        
        fun openNotificationSettingsForPackage(context: Context, packageName: String) {
            try {
                // Use ACTION_APP_NOTIFICATION_SETTINGS for Android 8.0+ to open notification settings for specific app
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } else {
                    // Fallback for older Android versions: open app info page
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (_: Exception) {
                // If notification settings intent fails, try app info as fallback
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
        
        // Cache expensive operations with remember
        val appLabelInfo = remember(packageName) {
            val prefs = Prefs(context)
            val alias = prefs.getAppAlias("app_alias_${packageName}")
            if (alias.isNotEmpty()) {
                alias
            } else {
                try {
                    context.packageManager.getApplicationLabel(
                        context.packageManager.getApplicationInfo(packageName, 0)
                    ).toString()
                } catch (_: Exception) {
                    packageName
                }
            }
        }
        
        // Improved title logic for group conversations
        val title = remember(notif.conversationTitle, notif.sender) {
            when {
                !notif.conversationTitle.isNullOrBlank() && !notif.sender.isNullOrBlank() && notif.conversationTitle != notif.sender -> notif.sender
                !notif.conversationTitle.isNullOrBlank() -> notif.conversationTitle
                !notif.sender.isNullOrBlank() -> notif.sender
                else -> appLabelInfo
            }
        }
        
        val timeString = remember(notif.timestamp) {
            val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            timeFormat.format(notif.timestamp)
        }

        // Only use the single message field, since ConversationNotification does not have a messages list
        // Backend now provides category-specific fallbacks, so we don't need generic fallback here
        val message = remember(notif.message) {
            notif.message?.takeIf { it.isNotBlank() }
        }
        DashedSeparator()

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = SettingsTheme.typography.title,
                    fontSize = titleFontSize * 1.2f, // Increased name size to 1.2x
                    fontWeight = FontWeight.Bold,
                    fontFamily = notifFontFamily,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Settings icon - opens notification settings for this package
                val isSyntheticPackage = packageName == "com.inkos.internal.app_drawer" ||
                        packageName == "com.inkos.internal.notifications" ||
                        packageName == "com.inkos.internal.search"
                if (!isSyntheticPackage) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Notification settings",
                        tint = Theme.colors.text,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                try {
                                    openNotificationSettingsForPackage(context, packageName)
                                } catch (_: Exception) {}
                            }
                    )
                }
            }

            DashedSeparator()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appLabelInfo,
                    style = SettingsTheme.typography.title,
                    fontSize = descriptionFontSize * 0.8f, // Reduced app alias size
                    fontWeight = FontWeight.Normal,
                    fontFamily = notifFontFamily,
                    color = SettingsTheme.typography.title.color
                )
                Text(
                    text = timeString,
                    style = SettingsTheme.typography.title,
                    fontSize = descriptionFontSize * 0.8f, // Reduced time size
                    fontWeight = FontWeight.Normal,
                    fontFamily = notifFontFamily,
                    color = SettingsTheme.typography.title.color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            DashedSeparator()
            Spacer(modifier = Modifier.height(24.dp))

            // Only show message if available - backend now provides category-specific text or null
            message?.let {
                Text(
                    text = it,
                    style = SettingsTheme.typography.title,
                    fontSize = descriptionFontSize,
                    fontWeight = FontWeight.Normal,
                    fontFamily = notifFontFamily,
                    lineHeight = descriptionFontSize * 1.3, // Increased line height
                    modifier = Modifier
                        .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                )
            }
        }
    }
}
