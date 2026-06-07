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
import com.github.gezimos.inkos.ui.compose.inkOsSafeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.EditModeHelper
import com.github.gezimos.inkos.services.NotificationManager
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled
import com.github.gezimos.inkos.ui.compose.EditModeOverlay
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import kotlinx.coroutines.launch

class LettersFragment : Fragment() {
    private lateinit var prefs: Prefs
    private var viewModel: com.github.gezimos.inkos.MainViewModel? = null
    private lateinit var vibrator: Vibrator
    private var wasPaused = false
    private var focusRestoreTriggerState: androidx.compose.runtime.MutableState<Int>? = null
    private var permissionExplanationSheet: com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost? = null

    private fun showPermissionExplanationDialog(
        title: String,
        message: String,
        onContinue: () -> Unit
    ) {
        permissionExplanationSheet?.dismiss()
        val host = com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost(requireActivity())
        permissionExplanationSheet = host
        host.show {
            val screenScale = rememberScreenScale()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp.scaled(screenScale))
            ) {
                com.github.gezimos.inkos.ui.dialogs.SheetTitle(title)
                Text(text = message, style = SettingsTheme.typography.item, color = Theme.colors.text)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = {
                        try { com.github.gezimos.inkos.helper.utils.VibrationHelper.trigger(com.github.gezimos.inkos.helper.utils.VibrationHelper.Effect.CLICK) } catch (_: Exception) {}
                        host.dismiss()
                        onContinue()
                    }) {
                        Text(text = getString(android.R.string.ok), style = SettingsTheme.typography.button, color = Theme.colors.text)
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val vm = try { androidx.lifecycle.ViewModelProvider(requireActivity())[com.github.gezimos.inkos.MainViewModel::class.java] } catch (_: Exception) { null }
        viewModel = vm
        prefs = vm?.getPrefs() ?: Prefs(requireContext())
        vibrator = requireContext().getSystemService(Vibrator::class.java)
        val composeView = ComposeView(requireContext())
        composeView.setContent {
            val isDark = prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
            val triggerState = remember { mutableStateOf(0) }
            focusRestoreTriggerState = triggerState
            SettingsTheme(isDark = isDark) {
                NotificationsScreen(
                    composeView = composeView,
                    focusRestoreTrigger = triggerState.value,
                    viewModel = viewModel
                )
            }
        }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.defaultFocusHighlightEnabled = false
        }
        view.requestFocus()

        view.setOnKeyListener { v, keyCode, event ->


            val mapped = com.github.gezimos.inkos.ui.compose.NavHelper.mapNotificationsKey(prefs, keyCode, event)
            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.None) return@setOnKeyListener false

            if (mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageUp || mapped == com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.PageDown) {
                return@setOnKeyListener false
            }

            val composeView = v as? ComposeView
            val pagerState = composeView?.getTag(0xdeadbeef.toInt()) as? androidx.compose.foundation.pager.PagerState
            val coroutineScope = composeView?.getTag(0xcafebabe.toInt()) as? kotlinx.coroutines.CoroutineScope
            val validNotifications = (composeView?.getTag(0xabcdef01.toInt()) as? List<*>)
                ?.filterIsInstance<Pair<String, NotificationManager.ConversationNotification>>()

            if (pagerState == null || coroutineScope == null || validNotifications == null) {
                android.util.Log.d("LettersFragment", "pagerState, coroutineScope, or validNotifications is null")
                return@setOnKeyListener false
            }

            when (mapped) {
                com.github.gezimos.inkos.ui.compose.NavHelper.NotificationKeyAction.Dismiss -> {
                    // Dismiss current notification
                    val (pkg, notif) = validNotifications.getOrNull(pagerState.currentPage) ?: return@setOnKeyListener true
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
        val act = activity as? com.github.gezimos.inkos.MainActivity ?: return
        
        if (wasPaused) {
            wasPaused = false
            // Restore focus to fragment view
            view?.postDelayed({
                val currentView = view
                if (isAdded && !isDetached && currentView != null) {
                    try {
                        currentView.isFocusable = true
                        currentView.isFocusableInTouchMode = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentView.defaultFocusHighlightEnabled = false
                        }
                        currentView.requestFocus()
                    } catch (_: Exception) {}
                }
            }, 350) // Delay to let MainActivity's onResume and window focus changes complete
            
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
    fun NotificationsScreen(
        composeView: ComposeView,
        focusRestoreTrigger: Int = 0,
        viewModel: com.github.gezimos.inkos.MainViewModel? = null
    ) {
        val ctx = androidx.compose.ui.platform.LocalContext.current

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
                showPermissionExplanationDialog(
                    title = getString(R.string.perm_notification_title),
                    message = "inkOS needs notification access to show your notifications here. You will be taken to Android settings to grant this permission.",
                    onContinue = {
                        try {
                            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        } catch (_: Exception) {
                            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", ctx.packageName, null)
                            })
                        }
                    }
                )
            }
        }

        // One-time tooltip for Letters (only after notification permission granted)
        com.github.gezimos.inkos.ui.compose.OneTimeTooltip(
            key = "tooltip_letters_shown",
            title = "Letters",
            lines = listOf(
                "Swipe up and down to move between notifications.",
                "Long press dismiss to dismiss all notifications."
            ),
            trigger = hasNotifPermission
        )

        val notificationsMap by rememberNotifications()
        val isEditMode by EditModeHelper.isEditModeFlow.collectAsState()
        val defaultState = remember { com.github.gezimos.inkos.HomeUiState() }
        val uiState by (viewModel?.homeUiState?.collectAsState() ?: remember { mutableStateOf<com.github.gezimos.inkos.HomeUiState>(defaultState) })

        val notifTitle = uiState.lettersTitle
        val screenScale = rememberScreenScale()
        val notifTitleSize = uiState.lettersTitleSize.sp.scaled(screenScale)
        val notifTextSize = uiState.notificationsTextSize.sp.scaled(screenScale)

        val notifFontFamily = remember(uiState.notificationsFont) {
            val notifFont = prefs.getFontForContext("notifications")
                .getFont(requireContext(), prefs.getCustomFontPathForContext("notifications"))
            notifFont?.let { FontFamily(it) } ?: FontFamily.Default
        }
        
        val notifTitleFontFamily = remember(uiState.lettersTitleFont) {
            val notifTitleFont = prefs.lettersTitleFont.getFont(
                requireContext(),
                prefs.getCustomFontPath("lettersTitle")
            )
            notifTitleFont?.let { FontFamily(it) } ?: FontFamily.Default
        }

        remember { prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark }

        // Check if notifications are enabled
        if (!prefs.notificationsEnabled) {
            findNavController().popBackStack()
            return
        }

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

        val contentBottomPadding = 0.dp
        val actionBarBottomPadding = 0.dp
        val contentTopPadding = 0.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.colors.background)
                .inkOsSafeDrawingPadding()
        ) {

            if (validNotifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = contentTopPadding, bottom = contentBottomPadding)
                        .then(
                            if (isEditMode) Modifier.clickable {
                                EditModeHelper.showNotificationScreenSettings(
                                    requireContext(),
                                    this@LettersFragment,
                                    prefs
                                ) { }
                            } else Modifier
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_foreground),
                        colorFilter = ColorFilter.tint(Theme.colors.text),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp.scaled(screenScale))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.no_notifications),
                        color = Theme.colors.text,
                        fontSize = 18.sp.scaled(screenScale),
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
                            .padding(top = contentTopPadding, bottom = contentBottomPadding)
                            .then(
                                if (isEditMode) Modifier.clickable {
                                    EditModeHelper.showNotificationScreenSettings(
                                        requireContext(),
                                        this@LettersFragment,
                                        prefs
                                    ) { }
                                } else Modifier
                            ),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Title row at the top of the content
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp.scaled(screenScale), vertical = 24.dp.scaled(screenScale))
                                .then(
                                    if (isEditMode) Modifier.clickable {
                                        EditModeHelper.showNotificationScreenSettings(
                                            requireContext(),
                                            this@LettersFragment,
                                            prefs
                                        ) { }
                                    } else Modifier
                                ),
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
                                .pointerInput(validNotifications, isEditMode) {
                                    detectTapGestures(
                                        onTap = {
                                            if (isEditMode) {
                                                EditModeHelper.showNotificationScreenSettings(
                                                    requireContext(),
                                                    this@LettersFragment,
                                                    prefs
                                                ) { }
                                                return@detectTapGestures
                                            }
                                            if (canDismiss) {
                                                val (pkg, notif) = validNotifications.getOrNull(pagerState.currentPage)
                                                    ?: return@detectTapGestures
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
                                            if (!isEditMode && canDismiss) {
                                                dismissAllNotifications()
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = stringResource(R.string.cd_dismiss).uppercase(),
                                style = SettingsTheme.typography.title,
                                fontSize = notifTextSize,
                                fontWeight = FontWeight.Bold,
                                fontFamily = notifFontFamily,
                                color = if (canDismiss) SettingsTheme.typography.title.color else Color.Gray,
                                modifier = Modifier.padding(start = 24.dp.scaled(screenScale), end = 24.dp.scaled(screenScale))
                            )
                        }
                        // Open button with matching hitbox
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clickable {
                                    if (isEditMode) {
                                        EditModeHelper.showNotificationScreenSettings(
                                            requireContext(),
                                            this@LettersFragment,
                                            prefs
                                        ) { }
                                        return@clickable
                                    }
                                    val (pkg, notif) = validNotifications.getOrNull(pagerState.currentPage)
                                        ?: return@clickable
                                    NotificationManager.getInstance(requireContext())
                                        .openNotification(pkg, notif.notificationKey, notif.conversationId, removeAfterOpen = true)
                                    vibratePaging() // vibrate on open tap if enabled
                                },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = stringResource(R.string.hub_open).uppercase(),
                                style = SettingsTheme.typography.title,
                                fontSize = notifTextSize,
                                fontWeight = FontWeight.Bold,
                                fontFamily = notifFontFamily,
                                color = SettingsTheme.typography.title.color,
                                modifier = Modifier.padding(start = 24.dp.scaled(screenScale), end = 24.dp.scaled(screenScale))
                            )
                        }
                    }
                }
            }
            if (isEditMode) {
                EditModeOverlay()
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
                if (notif.notificationKey != null) {
                    com.github.gezimos.inkos.services.NotificationService.dismissNotification(notif.notificationKey)
                }
                // Remove from conversation notifications
                notificationManager.removeConversationNotification(packageName, notif.conversationId)
            }
        }
        vibratePaging() // vibrate on dismiss all if enabled
        showShortToast(getString(R.string.toast_all_dismissed))
    }


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
        val screenScale = rememberScreenScale()
        val context = requireContext()
        
        fun openNotificationSettingsForPackage(context: Context, packageName: String) {
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
                    .padding(horizontal = 24.dp.scaled(screenScale), vertical = 16.dp),
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

            DashedSeparator()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp.scaled(screenScale)),
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

            message?.let {
                Text(
                    text = it,
                    style = SettingsTheme.typography.title,
                    fontSize = descriptionFontSize,
                    fontWeight = FontWeight.Normal,
                    fontFamily = notifFontFamily,
                    lineHeight = descriptionFontSize * 1.3, // Increased line height
                    modifier = Modifier
                        .padding(start = 24.dp.scaled(screenScale), end = 24.dp.scaled(screenScale), bottom = 8.dp)
                )
            }
        }
    }
}
