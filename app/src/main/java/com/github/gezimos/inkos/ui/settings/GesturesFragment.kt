package com.github.gezimos.inkos.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
// removed unused import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Constants.AppDrawerFlag
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Constants.Theme.Light
import com.github.gezimos.inkos.data.Constants.Theme.System
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.isSystemInDarkMode
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.helper.utils.PrivateSpaceManager
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle
import com.github.gezimos.inkos.ui.dialogs.DialogManager

class GesturesFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var dialogBuilder: DialogManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        prefs = Prefs(requireContext())
        dialogBuilder = DialogManager(requireContext(), requireActivity())
        val isDark = when (prefs.appTheme) {
            Light -> false
            Dark -> true
            System -> isSystemInDarkMode(requireContext())
        }
        val settingsSize = (prefs.settingsSize - 3)
        val backgroundColor = getHexForOpacity(prefs)
        val context = requireContext()
        val currentPage = intArrayOf(0)
        val pageCount = intArrayOf(1)

        // Create a vertical LinearLayout to hold sticky header and scrollable content
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        var bottomInsetPx = 0
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // Add sticky header ComposeView
        val headerView = ComposeView(context).apply {
            setContent {
                val density = LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.gestures_settings_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        
                        if (bottomInsetDp > 0.dp) {
                            Spacer(modifier = Modifier.height(bottomInsetDp))
                        }
                    }
                }
            }
        }
        rootLayout.addView(headerView)

        // Add scrollable settings content
        val nestedScrollView = NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                ComposeView(context).apply {
                    setContent {
                        val density = LocalDensity.current
                        val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    GesturesSettingsAllInOne(settingsSize.sp)
                                    if (bottomInsetDp > 0.dp) {
                                        Spacer(modifier = Modifier.height(bottomInsetDp))
                                    }
                                }
                            }
                        }
                    }
                },
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        EinkScrollBehavior(context).attachToScrollView(nestedScrollView)
        rootLayout.addView(
            nestedScrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Apply bottom padding to the root layout to prevent scroll view from going under navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

    // Use EinkScrollBehavior callback to update page indicator reliably
    val scrollBehavior = EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            headerView.setContent {
                val density = LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = stringResource(R.string.gestures_settings_title),
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                        
                        if (bottomInsetDp > 0.dp) Spacer(modifier = Modifier.height(bottomInsetDp))
                    }
                }
            }
        }
        scrollBehavior.attachToScrollView(nestedScrollView)
        return rootLayout
    }

    /**
     * Return raw app label (may be empty). Use openAppDisplay() to format for UI.
     */
    private fun getOpenAppLabel(label: String): String = label

    // Format the display text for an OpenApp option: "Open App" when no app is chosen,
    // otherwise just show the app name without "Open" prefix.
    private fun openAppDisplay(label: String): String = if (label.isEmpty()) getString(R.string.open_app) else label

    @Composable
    fun GesturesSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified) {
        val navController = findNavController()
        val isDark = isSystemInDarkMode(requireContext())
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        var selectedDoubleTapAction by remember { mutableStateOf(prefs.doubleTapAction) }
        var selectedClickClockAction by remember { mutableStateOf(prefs.clickClockAction) }
        var selectedClickDateAction by remember { mutableStateOf(prefs.clickDateAction) }
    var selectedSwipeLeftAction by remember { mutableStateOf(prefs.swipeLeftAction) }
    var selectedSwipeRightAction by remember { mutableStateOf(prefs.swipeRightAction) }
        var selectedQuoteAction by remember { mutableStateOf(prefs.quoteAction) }
        val actions = Action.entries
        val filteredActions =
            if (!PrivateSpaceManager(requireContext()).isPrivateSpaceSupported()) {
                actions.filter { it != Action.TogglePrivateSpace }
            } else actions
        val doubleTapGestureActions = filteredActions.filter { action ->
        action != Action.OpenApp &&
            when (action) {
                        Action.OpenNotificationsScreen -> prefs.notificationsEnabled
                        else -> true
                    }
        }.toMutableList().apply {
            if (!contains(Action.Brightness)) add(Action.Brightness)
        }
    val clickClockGestureActions = filteredActions.filter { action ->
            when (action) {
                        Action.OpenNotificationsScreen -> prefs.notificationsEnabled
                        else -> true
                    }
        }.toMutableList().apply {
            if (!contains(Action.Brightness)) add(Action.Brightness)
        }
    val clickDateGestureActions = filteredActions.filter { action ->
            when (action) {
                        Action.OpenNotificationsScreen -> prefs.notificationsEnabled
                        else -> true
                    }
        }.toMutableList().apply {
            if (!contains(Action.Brightness)) add(Action.Brightness)
        }
        val gestureActions = filteredActions.filter { action ->
            when (action) {
                Action.OpenNotificationsScreen -> prefs.notificationsEnabled
                else -> true
            }
        }.toMutableList()
        if (!gestureActions.contains(Action.OpenAppDrawer)) gestureActions.add(Action.OpenAppDrawer)
        if (!clickClockGestureActions.contains(Action.OpenAppDrawer)) clickClockGestureActions.add(
            Action.OpenAppDrawer
        )
        if (!doubleTapGestureActions.contains(Action.OpenAppDrawer)) doubleTapGestureActions.add(
            Action.OpenAppDrawer
        )
        if (!clickClockGestureActions.contains(Action.ExitLauncher)) clickClockGestureActions.add(
            Action.ExitLauncher
        )
        if (!clickDateGestureActions.contains(Action.ExitLauncher)) clickDateGestureActions.add(
            Action.ExitLauncher
        )
        if (!doubleTapGestureActions.contains(Action.ExitLauncher)) doubleTapGestureActions.add(
            Action.ExitLauncher
        )
        if (!clickClockGestureActions.contains(Action.LockScreen)) clickClockGestureActions.add(
            Action.LockScreen
        )
        if (!clickClockGestureActions.contains(Action.ShowRecents)) clickClockGestureActions.add(
            Action.ShowRecents
        )
        if (!clickClockGestureActions.contains(Action.OpenQuickSettings)) clickClockGestureActions.add(
            Action.OpenQuickSettings
        )
        if (!clickClockGestureActions.contains(Action.OpenPowerDialog)) clickClockGestureActions.add(
            Action.OpenPowerDialog
        )
        if (!clickDateGestureActions.contains(Action.LockScreen)) clickDateGestureActions.add(Action.LockScreen)
        if (!clickDateGestureActions.contains(Action.ShowRecents)) clickDateGestureActions.add(
            Action.ShowRecents
        )
        if (!clickDateGestureActions.contains(Action.OpenQuickSettings)) clickDateGestureActions.add(
            Action.OpenQuickSettings
        )
        if (!clickDateGestureActions.contains(Action.OpenPowerDialog)) clickDateGestureActions.add(
            Action.OpenPowerDialog
        )
        if (!doubleTapGestureActions.contains(Action.LockScreen)) doubleTapGestureActions.add(Action.LockScreen)
        if (!doubleTapGestureActions.contains(Action.ShowRecents)) doubleTapGestureActions.add(
            Action.ShowRecents
        )
        if (!doubleTapGestureActions.contains(Action.OpenQuickSettings)) doubleTapGestureActions.add(
            Action.OpenQuickSettings
        )
        if (!doubleTapGestureActions.contains(Action.OpenPowerDialog)) doubleTapGestureActions.add(
            Action.OpenPowerDialog
        )
        val doubleTapActionStrings =
            doubleTapGestureActions.map { it.getString(requireContext()) }.toTypedArray()
        val clickClockActionStrings =
            clickClockGestureActions.map {
                if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) else it.getString(
                    requireContext()
                )
            }.toTypedArray()
        val clickDateActionStrings =
            clickDateGestureActions.map {
                if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) else it.getString(
                    requireContext()
                )
            }.toTypedArray()
        val actionStrings = gestureActions.map {
            if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) else it.getString(
                requireContext()
            )
        }.toTypedArray()
        val quoteGestureActions = filteredActions.filter { action ->
                    when (action) {
                Action.OpenNotificationsScreen -> prefs.notificationsEnabled
                else -> true
            }
        }.toMutableList().apply {
            if (!contains(Action.Brightness)) add(Action.Brightness)
        }
        if (!quoteGestureActions.contains(Action.OpenAppDrawer)) quoteGestureActions.add(Action.OpenAppDrawer)
        if (!quoteGestureActions.contains(Action.ExitLauncher)) quoteGestureActions.add(Action.ExitLauncher)
        if (!quoteGestureActions.contains(Action.LockScreen)) quoteGestureActions.add(Action.LockScreen)
        if (!quoteGestureActions.contains(Action.ShowRecents)) quoteGestureActions.add(Action.ShowRecents)
        if (!quoteGestureActions.contains(Action.OpenQuickSettings)) quoteGestureActions.add(Action.OpenQuickSettings)
        if (!quoteGestureActions.contains(Action.OpenPowerDialog)) quoteGestureActions.add(Action.OpenPowerDialog)
        val quoteActionStrings = quoteGestureActions.map {
            if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) else it.getString(
                requireContext()
            )
        }.toTypedArray()
        val appLabelDoubleTapAction = getOpenAppLabel(prefs.appDoubleTap.activityLabel)
        val appLabelClickClockAction = getOpenAppLabel(prefs.appClickClock.activityLabel)
        val appLabelClickDateAction = getOpenAppLabel(prefs.appClickDate.activityLabel)
        val appLabelQuoteAction = getOpenAppLabel(prefs.appQuoteWidget.activityLabel)
        val appLabelSwipeLeftAction = getOpenAppLabel(prefs.appSwipeLeft.activityLabel)
        val appLabelSwipeRightAction = getOpenAppLabel(prefs.appSwipeRight.activityLabel)

        Column(modifier = Modifier.fillMaxSize()) {
            DashedSeparator()
            SettingsTitle(
                text = stringResource(R.string.tap_click_actions),
                fontSize = titleFontSize,
            )
            DashedSeparator()
            SettingsSelect(
                title = "${stringResource(R.string.double_tap)} (2)",
                option = if (selectedDoubleTapAction == Action.OpenApp) {
                    openAppDisplay(appLabelDoubleTapAction)
                } else {
                    selectedDoubleTapAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    // compute selectedIndex so the dialog shows the checked radio
                    val currentDoubleTapIndex = doubleTapGestureActions.indexOf(selectedDoubleTapAction)
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = doubleTapActionStrings,
                        titleResId = R.string.double_tap,
                        selectedIndex = if (currentDoubleTapIndex >= 0) currentDoubleTapIndex else null,
                        onItemSelected = { newDoubleTapAction: String ->
                            val selectedAction =
                                doubleTapGestureActions.firstOrNull { it.getString(requireContext()) == newDoubleTapAction }
                            if (selectedAction != null) {
                                selectedDoubleTapAction = selectedAction
                                setGesture(AppDrawerFlag.SetDoubleTap, selectedAction)
                            }
                        },
                        showButtons = false
                    )
                }
            )
            DashedSeparator()
            SettingsSelect(
                title = "${stringResource(R.string.clock_click_app)} (6)",
                option = when (selectedClickClockAction) {
                    Action.OpenApp -> openAppDisplay(appLabelClickClockAction)
                    Action.OpenAppDrawer -> getString(R.string.app_drawer)
                    else -> selectedClickClockAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    val currentClickClockIndex = clickClockGestureActions.indexOf(selectedClickClockAction)
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = clickClockActionStrings,
                        titleResId = R.string.clock_click_app,
                        selectedIndex = if (currentClickClockIndex >= 0) currentClickClockIndex else null,
                        onItemSelected = { newClickClock: String ->
                            val selectedAction =
                                clickClockGestureActions.firstOrNull {
                                    if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) == newClickClock
                                    else it.getString(requireContext()) == newClickClock
                                }
                            if (selectedAction != null) {
                                if (selectedAction == Action.OpenApp) {
                                    navController.navigate(R.id.appListFragment, Bundle().apply {
                                        putString("flag", AppDrawerFlag.SetClickClock.name)
                                    })
                                } else {
                                    selectedClickClockAction = selectedAction
                                    setGesture(AppDrawerFlag.SetClickClock, selectedAction)
                                }
                            }
                        },
                        showButtons = false
                    )
                }
            )
            DashedSeparator()
            SettingsSelect(
                title = "${stringResource(R.string.date_click_app)} (7)",
                option = when (selectedClickDateAction) {
                    Action.OpenApp -> openAppDisplay(appLabelClickDateAction)
                    Action.OpenAppDrawer -> getString(R.string.app_drawer)
                    else -> selectedClickDateAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    val currentClickDateIndex = clickDateGestureActions.indexOf(selectedClickDateAction)
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = clickDateActionStrings,
                        titleResId = R.string.date_click_app,
                        selectedIndex = if (currentClickDateIndex >= 0) currentClickDateIndex else null,
                        onItemSelected = { newClickDate: String ->
                            val selectedAction =
                                clickDateGestureActions.firstOrNull {
                                    if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) == newClickDate
                                    else it.getString(requireContext()) == newClickDate
                                }
                            if (selectedAction != null) {
                                if (selectedAction == Action.OpenApp) {
                                    navController.navigate(R.id.appListFragment, Bundle().apply {
                                        putString("flag", AppDrawerFlag.SetClickDate.name)
                                    })
                                } else {
                                    prefs.clickDateAction = selectedAction
                                    selectedClickDateAction = selectedAction
                                    setGesture(AppDrawerFlag.SetClickDate, selectedAction)
                                }
                            }
                        },
                        showButtons = false
                    )
                }
            )
            DashedSeparator()
            SettingsSelect(
                title = "${stringResource(R.string.quote_click_app)} (8)",
                option = when (selectedQuoteAction) {
                    Action.OpenApp -> openAppDisplay(appLabelQuoteAction)
                    Action.OpenAppDrawer -> getString(R.string.app_drawer)
                    else -> selectedQuoteAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    val currentQuoteIndex = quoteGestureActions.indexOf(selectedQuoteAction)
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = quoteActionStrings,
                        titleResId = R.string.quote_click_app,
                        selectedIndex = if (currentQuoteIndex >= 0) currentQuoteIndex else null,
                        onItemSelected = { newQuoteAction: String ->
                            val selectedAction = quoteGestureActions.firstOrNull {
                                if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) == newQuoteAction
                                else it.getString(requireContext()) == newQuoteAction
                            }
                            if (selectedAction != null) {
                                if (selectedAction == Action.OpenApp) {
                                    navController.navigate(R.id.appListFragment, Bundle().apply {
                                        putString("flag", AppDrawerFlag.SetQuoteWidget.name)
                                    })
                                    } else {
                                    prefs.appQuoteWidget = AppListItem(
                                        activityLabel = "",
                                        activityPackage = "",
                                        activityClass = "",
                                        user = prefs.appClickClock.user,
                                        customLabel = ""
                                    )
                                    prefs.quoteAction = selectedAction
                                    selectedQuoteAction = selectedAction
                                    setGesture(AppDrawerFlag.SetQuoteWidget, selectedAction)
                                }
                            }
                        },
                        showButtons = false
                    )
                }
            )
            DashedSeparator()
            SettingsTitle(
                text = stringResource(R.string.swipe_movement),
                fontSize = titleFontSize,
            )
            DashedSeparator()
            SettingsSelect(
                title = "${stringResource(R.string.swipe_left_app)} (>)",
                option = when (selectedSwipeLeftAction) {
                    Action.OpenApp -> openAppDisplay(appLabelSwipeLeftAction)
                    Action.OpenAppDrawer -> getString(R.string.app_drawer)
                    else -> selectedSwipeLeftAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    val currentSwipeLeftIndex = gestureActions.indexOf(selectedSwipeLeftAction)
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = actionStrings,
                        titleResId = R.string.swipe_left_app,
                        selectedIndex = if (currentSwipeLeftIndex >= 0) currentSwipeLeftIndex else null,
                        onItemSelected = { newAction: String ->
                            val selectedAction =
                                gestureActions.firstOrNull {
                                    if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) == newAction
                                    else it.getString(requireContext()) == newAction
                                }
                            if (selectedAction != null) {
                                if (selectedAction == Action.OpenApp) {
                                    navController.navigate(R.id.appListFragment, Bundle().apply {
                                        putString("flag", AppDrawerFlag.SetSwipeLeft.name)
                                    })
                                } else {
                                    selectedSwipeLeftAction = selectedAction
                                    setGesture(AppDrawerFlag.SetSwipeLeft, selectedAction)
                                }
                            }
                        },
                        showButtons = false
                    )
                }
            )
            DashedSeparator()
            SettingsSelect(
                title = "${stringResource(R.string.swipe_right_app)} (<)",
                option = when (selectedSwipeRightAction) {
                    Action.OpenApp -> openAppDisplay(appLabelSwipeRightAction)
                    Action.OpenAppDrawer -> getString(R.string.app_drawer)
                    Action.Disabled -> stringResource(R.string.disabled)
                    else -> selectedSwipeRightAction.string()
                },
                fontSize = titleFontSize,
                onClick = {
                    val currentSwipeRightIndex = gestureActions.indexOf(selectedSwipeRightAction)
                    dialogBuilder.showSingleChoiceDialog(
                        context = requireContext(),
                        options = actionStrings,
                        titleResId = R.string.swipe_right_app,
                        selectedIndex = if (currentSwipeRightIndex >= 0) currentSwipeRightIndex else null,
                        onItemSelected = { newAction: String ->
                            val selectedAction =
                                gestureActions.firstOrNull {
                                    if (it == Action.OpenAppDrawer) getString(R.string.app_drawer) == newAction
                                    else it.getString(requireContext()) == newAction
                                }
                            if (selectedAction != null) {
                                if (selectedAction == Action.OpenApp) {
                                    navController.navigate(R.id.appListFragment, Bundle().apply {
                                        putString("flag", AppDrawerFlag.SetSwipeRight.name)
                                    })
                                } else {
                                    selectedSwipeRightAction = selectedAction
                                    setGesture(AppDrawerFlag.SetSwipeRight, selectedAction)
                                }
                            }
                        },
                        showButtons = false
                    )
                }
            )
            DashedSeparator()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    private fun setGesture(flag: AppDrawerFlag, action: Action) {
        when (flag) {
            AppDrawerFlag.SetDoubleTap -> prefs.doubleTapAction = action
            AppDrawerFlag.SetClickClock -> prefs.clickClockAction = action
            AppDrawerFlag.SetClickDate -> prefs.clickDateAction = action
            AppDrawerFlag.SetSwipeLeft -> prefs.swipeLeftAction = action
            AppDrawerFlag.SetSwipeRight -> prefs.swipeRightAction = action
            AppDrawerFlag.SetQuoteWidget -> prefs.quoteAction = action
            AppDrawerFlag.LaunchApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.PrivateApps,
            AppDrawerFlag.SetHomeApp -> {
            }

            else -> {
            }
        }
    }
}