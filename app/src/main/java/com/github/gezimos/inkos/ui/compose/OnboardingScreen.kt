package com.github.gezimos.inkos.ui.compose

import android.content.Intent
import androidx.compose.ui.res.stringResource
import com.github.gezimos.inkos.R
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import com.github.gezimos.inkos.ui.compose.inkOsSafeDrawingPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.ShapeHelper
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.style.rememberScreenScale
import com.github.gezimos.inkos.style.scaled

class OnboardingFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val context = LocalContext.current
                val prefs = remember { Prefs(context) }
                val viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
                OnboardingScreen.Show(
                    onFinish = {
                        prefs.commitFirstOpen(false)
                        prefs.onboardingPage = 0
                        viewModel.setFirstOpen(false)
                        findNavController().popBackStack()
                    },
                    onEditFavorites = {
                        viewModel.getAppList(includeHiddenApps = true, flag = Constants.AppDrawerFlag.EditFavorites)
                        findNavController().navigate(
                            R.id.appsFragment,
                            androidx.core.os.bundleOf(
                                "flag" to Constants.AppDrawerFlag.EditFavorites.toString(),
                                "showSearch" to false
                            )
                        )
                    }
                )
            }
        }
    }
}

object OnboardingScreen {

    private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    @Composable
    fun Show(
        onFinish: () -> Unit = {},
        onEditFavorites: () -> Unit = {}
    ) {
        val context = LocalContext.current
        val prefs = remember { Prefs(context) }
        val activity = LocalActivity.current
        val viewModel = activity?.let { act ->
            if (act is androidx.lifecycle.ViewModelStoreOwner) {
                ViewModelProvider(act)[MainViewModel::class.java]
            } else null
        }

        val screenScaleRaw = com.github.gezimos.inkos.style.detectScaleMode(context).let { mode ->
            if (prefs.uiScaleMode != 0) com.github.gezimos.inkos.style.UiScaleMode.fromId(prefs.uiScaleMode).scale
            else mode.scale
        }
        val settingsSize = (prefs.settingsSize - 3)
        val titleFontSize = (settingsSize * 1.5f * screenScaleRaw).sp
        val bodyFontSize = (settingsSize * 1.2f * screenScaleRaw).sp
        val bigTitleSize = (settingsSize * 3f * screenScaleRaw).sp

        val systemDark = isSystemInDarkTheme()
        var themeMode by remember { mutableStateOf(prefs.appTheme) }
        val isDark = when (themeMode) {
            Constants.Theme.Dark -> true
            Constants.Theme.Light -> false
            Constants.Theme.System -> systemDark
        }
        var page by remember { mutableStateOf(prefs.onboardingPage.coerceIn(0, 6)) }
        var showThemePicker by remember { mutableStateOf(false) }
        var privacyAgreed by remember { mutableStateOf(false) }
        var privacyToastShown by remember { mutableStateOf(false) }
        var settingsConfirmed by remember { mutableStateOf(false) }
        var settingsToastShown by remember { mutableStateOf(false) }
        val totalPages = 7

        // Persist page so onboarding resumes
        LaunchedEffect(page) {
            prefs.onboardingPage = page
        }

        var hasPermission by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
        LaunchedEffect(page) {
            if (page != 2 || hasPermission) return@LaunchedEffect
            while (!hasPermission) {
                kotlinx.coroutines.delay(1000)
                val current = isNotificationListenerEnabled(context)
                if (current) { hasPermission = true; return@LaunchedEffect }
            }
        }

        var isDefaultLauncher by remember { mutableStateOf(com.github.gezimos.inkos.helper.isinkosDefault(context)) }
        LaunchedEffect(page) {
            if (page != 3 || isDefaultLauncher) return@LaunchedEffect
            while (!isDefaultLauncher) {
                kotlinx.coroutines.delay(1000)
                val current = com.github.gezimos.inkos.helper.isinkosDefault(context)
                if (current) { isDefaultLauncher = true; return@LaunchedEffect }
            }
        }

        SettingsTheme(isDark = isDark) {
            val textColor = Theme.colors.text
            val bgColor = Theme.colors.background
            val screenScale = rememberScreenScale()
            val buttonShape = remember(prefs.textIslandsShape) {
                ShapeHelper.getRoundedCornerShape(textIslandsShape = prefs.textIslandsShape, pillRadius = 16.dp)
            }

            androidx.activity.compose.BackHandler(enabled = true) {
                if (page == 5 && showThemePicker) showThemePicker = false
                else if (page > 0) page--
                // page 0: swallow back, don't exit onboarding
            }

            var einkMode by remember { mutableStateOf(com.github.gezimos.inkos.helper.device.DeviceHelper.isEinkDevice()) }
            val themePickerIndex = remember { androidx.compose.runtime.mutableIntStateOf(0) }
            val themeSelectAction = remember { mutableStateOf<(() -> Unit)?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .inkOsSafeDrawingPadding()
            ) {
                if (page == 5 && viewModel != null && showThemePicker) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp.scaled(screenScale))
                            .padding(top = 32.dp.scaled(screenScale))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_theme),
                                style = SettingsTheme.typography.title,
                                fontSize = bigTitleSize,
                                fontWeight = FontWeight.Black,
                                color = textColor
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                            ) {
                                // Mono toggle
                                val monoActive = einkMode
                                Text(
                                    text = "Mono",
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    color = if (monoActive) bgColor else textColor,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(buttonShape)
                                        .background(if (monoActive) textColor else bgColor)
                                        .then(if (!monoActive) Modifier.border(2.dp, textColor, buttonShape) else Modifier)
                                        .clickable { einkMode = !einkMode }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                // Theme mode icon cycle
                                val modeIcon = when (themeMode) {
                                    Constants.Theme.Light -> Icons.Rounded.LightMode
                                    Constants.Theme.Dark -> Icons.Rounded.DarkMode
                                    Constants.Theme.System -> Icons.Rounded.Android
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clip(buttonShape)
                                        .border(2.dp, textColor, buttonShape)
                                        .clickable {
                                            val newMode = when (themeMode) {
                                                Constants.Theme.Light -> Constants.Theme.Dark
                                                Constants.Theme.Dark -> Constants.Theme.System
                                                Constants.Theme.System -> Constants.Theme.Light
                                            }
                                            themeMode = newMode
                                            viewModel.setAppTheme(newMode)
                                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                                when (newMode) {
                                                    Constants.Theme.Light -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                                    Constants.Theme.Dark -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                                    Constants.Theme.System -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                                }
                                            )
                                        }
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = themeMode.name,
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp.scaled(screenScale))
                                    )
                                }
                            }
                        }
                    }
                    ThemePresetPicker(
                        isDark = isDark,
                        themeMode = themeMode,
                        onThemeModeChange = {
                            themeMode = it
                            viewModel.setAppTheme(it)
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                when (it) {
                                    Constants.Theme.Light -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                    Constants.Theme.Dark -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                                    Constants.Theme.System -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                }
                            )
                        },
                        viewModel = viewModel,
                        prefs = prefs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        applyOnFirstComposition = false,
                        titleFontSize = titleFontSize,
                        showThemeModeToggleUnderCards = false,
                        requireConfirmation = true,
                        applyFontImmediately = true,
                        einkMode = einkMode,
                        currentIndexState = themePickerIndex,
                        selectAction = themeSelectAction
                    )
                } else {
                    // All other pages: normal content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 32.dp.scaled(screenScale), vertical = 32.dp.scaled(screenScale))
                    ) {
                        // Big title (top-left, uppercase)
                        Text(
                            text = when (page) {
                                0 -> stringResource(R.string.onboarding_welcome)
                                1 -> stringResource(R.string.onboarding_privacy)
                                2 -> stringResource(R.string.onboarding_notifications)
                                3 -> stringResource(R.string.onboarding_default_launcher)
                                4 -> stringResource(R.string.onboarding_settings)
                                5 -> stringResource(R.string.onboarding_theme)
                                6 -> stringResource(R.string.onboarding_home_apps)
                                else -> ""
                            },
                            style = SettingsTheme.typography.title,
                            fontSize = bigTitleSize,
                            fontWeight = FontWeight.Black,
                            color = textColor,
                            lineHeight = (bigTitleSize.value * 1.05f).sp
                        )

                        // Thick bar
                        Spacer(modifier = Modifier.height(16.dp.scaled(screenScale)))
                        Box(
                            modifier = Modifier
                                .width(60.dp.scaled(screenScale))
                                .height(5.dp)
                                .background(textColor)
                        )

                        // Body text
                        Spacer(modifier = Modifier.height(24.dp.scaled(screenScale)))
                        Text(
                            text = when (page) {
                                0 -> "A distraction-free interface for clarity and focus. Optimized for e-ink devices, QWERTY and T9 phones."
                                1 -> "inkOS does not collect, store, or share any data. There are no server connections and no tracking. Everything stays on your device."
                                2 -> "To get the most out of inkOS, grant notification permissions so inkOS can forward you Android notifications in Home, Letters or Simple Tray."
                                3 -> "To make inkOS your new app home, set it as a default launcher. You can change this later in Settings."
                                4 -> "To access settings, Pinch (zoom out) in home (empty areas) to open the quick settings. That will allow you to access All Settings or enable Edit Mode for the front-end editor."
                                5 -> "Choose a color preset for your launcher, or skip to keep your current theme and start from scratch."
                                6 -> "Select your home shortcuts and order them using Edit Favorites. You can still longpress on single apps in home to replace them individually like before."
                                else -> ""
                            },
                            style = SettingsTheme.typography.body,
                            fontSize = bodyFontSize,
                            lineHeight = (bodyFontSize.value * 1.6f).sp,
                            color = textColor
                        )

                        // Pinch animation on settings page
                        if (page == 4) {
                            Spacer(modifier = Modifier.height(32.dp.scaled(screenScale)))
                            var pinchFrame by remember { mutableIntStateOf(0) }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    kotlinx.coroutines.delay(600)
                                    pinchFrame = (pinchFrame + 1) % 2
                                }
                            }
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    if (pinchFrame == 0) R.drawable.pinch1 else R.drawable.pinch2
                                ),
                                contentDescription = null,
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(textColor),
                                modifier = Modifier.size(48.dp.scaled(screenScale))
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            val confirmInteraction = remember { MutableInteractionSource() }
                            val confirmFocused = confirmInteraction.collectIsFocusedAsState().value
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled(screenScale)),
                                modifier = Modifier
                                    .then(with(SettingsComposable) {
                                        Modifier.pillHighlight(
                                            isHighlighted = confirmFocused,
                                            color = textColor,
                                            outerHorizontal = 4.dp,
                                            outerVertical = 4.dp
                                        )
                                    })
                                    .clip(buttonShape)
                                    .background(if (confirmFocused) textColor else bgColor)
                                    .clickable(
                                        interactionSource = confirmInteraction,
                                        indication = null
                                    ) { settingsConfirmed = !settingsConfirmed }
                                    .padding(horizontal = 12.dp.scaled(screenScale), vertical = 8.dp.scaled(screenScale))
                            ) {
                                val confirmFg = if (confirmFocused) bgColor else textColor
                                val confirmBg = if (confirmFocused) textColor else bgColor
                                androidx.compose.runtime.CompositionLocalProvider(
                                    androidx.compose.material3.LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = settingsConfirmed,
                                        onCheckedChange = null,
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = confirmFg,
                                            uncheckedColor = confirmFg,
                                            checkmarkColor = confirmBg
                                        ),
                                        modifier = Modifier.size(18.dp.scaled(screenScale))
                                    )
                                }
                                Text(
                                    text = "I CONFIRM THAT I'VE READ",
                                    style = SettingsTheme.typography.title,
                                    fontSize = (titleFontSize.value * 0.7f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = confirmFg,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // App version info on welcome page
                        if (page == 0) {
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled(screenScale)),
                                modifier = Modifier.padding(
                                    horizontal = 12.dp.scaled(screenScale),
                                    vertical = 8.dp.scaled(screenScale)
                                )
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_inkos),
                                    contentDescription = null,
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(textColor),
                                    modifier = Modifier.size(18.dp.scaled(screenScale))
                                )
                                Text(
                                    text = stringResource(R.string.onboarding_app_version),
                                    style = SettingsTheme.typography.title,
                                    fontSize = (titleFontSize.value * 0.7f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "v${com.github.gezimos.inkos.BuildConfig.VERSION_NAME}",
                                    style = SettingsTheme.typography.body,
                                    fontSize = bodyFontSize,
                                    color = textColor
                                )
                            }
                        }

                        // Action button for page 1 (Privacy Policy)
                        if (page == 1) {
                            Spacer(modifier = Modifier.height(32.dp.scaled(screenScale)))
                            val privacyInteraction = remember { MutableInteractionSource() }
                            val privacyFocused = privacyInteraction.collectIsFocusedAsState().value
                            Box(
                                modifier = Modifier
                                    .then(with(SettingsComposable) {
                                        Modifier.pillHighlight(
                                            isHighlighted = privacyFocused,
                                            color = textColor,
                                            outerHorizontal = 4.dp,
                                            outerVertical = 4.dp
                                        )
                                    })
                                    .clip(buttonShape)
                                    .background(if (privacyFocused) textColor else bgColor)
                                    .then(if (!privacyFocused) Modifier.border(1.5.dp.scaled(screenScale), textColor, buttonShape) else Modifier)
                                    .clickable(
                                        interactionSource = privacyInteraction,
                                        indication = null
                                    ) {
                                        com.github.gezimos.inkos.ui.dialogs.ComposeDialogManager(context, activity ?: return@clickable)
                                            .showPrivacyPolicySheet()
                                    }
                                    .padding(horizontal = 24.dp.scaled(screenScale), vertical = 12.dp.scaled(screenScale))
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_read_policy),
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    color = if (privacyFocused) bgColor else textColor
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            val agreeInteraction = remember { MutableInteractionSource() }
                            val agreeFocused = agreeInteraction.collectIsFocusedAsState().value
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp.scaled(screenScale)),
                                modifier = Modifier
                                    .then(with(SettingsComposable) {
                                        Modifier.pillHighlight(
                                            isHighlighted = agreeFocused,
                                            color = textColor,
                                            outerHorizontal = 4.dp,
                                            outerVertical = 4.dp
                                        )
                                    })
                                    .clip(buttonShape)
                                    .background(if (agreeFocused) textColor else bgColor)
                                    .clickable(
                                        interactionSource = agreeInteraction,
                                        indication = null
                                    ) { privacyAgreed = !privacyAgreed }
                                    .padding(horizontal = 12.dp.scaled(screenScale), vertical = 8.dp.scaled(screenScale))
                            ) {
                                val agreeFg = if (agreeFocused) bgColor else textColor
                                val agreeBg = if (agreeFocused) textColor else bgColor
                                androidx.compose.runtime.CompositionLocalProvider(
                                    androidx.compose.material3.LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = privacyAgreed,
                                        onCheckedChange = null,
                                        colors = androidx.compose.material3.CheckboxDefaults.colors(
                                            checkedColor = agreeFg,
                                            uncheckedColor = agreeFg,
                                            checkmarkColor = agreeBg
                                        ),
                                        modifier = Modifier.size(18.dp.scaled(screenScale))
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.onboarding_agree_policy),
                                    style = SettingsTheme.typography.title,
                                    fontSize = (titleFontSize.value * 0.7f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = agreeFg,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Action button for page 5 intro (Choose Preset)
                        if (page == 5) {
                            Spacer(modifier = Modifier.height(32.dp.scaled(screenScale)))
                            val presetInteraction = remember { MutableInteractionSource() }
                            val presetFocused = presetInteraction.collectIsFocusedAsState().value
                            Box(
                                modifier = Modifier
                                    .then(with(SettingsComposable) {
                                        Modifier.pillHighlight(
                                            isHighlighted = presetFocused,
                                            color = textColor,
                                            outerHorizontal = 4.dp,
                                            outerVertical = 4.dp
                                        )
                                    })
                                    .clip(buttonShape)
                                    .background(if (presetFocused) textColor else bgColor)
                                    .then(if (!presetFocused) Modifier.border(1.5.dp.scaled(screenScale), textColor, buttonShape) else Modifier)
                                    .clickable(
                                        interactionSource = presetInteraction,
                                        indication = null
                                    ) { showThemePicker = true }
                                    .padding(horizontal = 24.dp.scaled(screenScale), vertical = 12.dp.scaled(screenScale))
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_choose_preset),
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    color = if (presetFocused) bgColor else textColor
                                )
                            }
                        }

                        // Action button for page 6 (Edit Favorites)
                        if (page == 6) {
                            Spacer(modifier = Modifier.height(32.dp.scaled(screenScale)))
                            val specialInteraction = remember { MutableInteractionSource() }
                            val specialFocused = specialInteraction.collectIsFocusedAsState().value
                            Box(
                                modifier = Modifier
                                    .then(with(SettingsComposable) {
                                        Modifier.pillHighlight(
                                            isHighlighted = specialFocused,
                                            color = textColor,
                                            outerHorizontal = 4.dp,
                                            outerVertical = 4.dp
                                        )
                                    })
                                    .clip(buttonShape)
                                    .background(if (specialFocused) textColor else bgColor)
                                    .then(if (!specialFocused) Modifier.border(1.5.dp.scaled(screenScale), textColor, buttonShape) else Modifier)
                                    .clickable(
                                        interactionSource = specialInteraction,
                                        indication = null
                                    ) { onEditFavorites() }
                                    .padding(horizontal = 24.dp.scaled(screenScale), vertical = 12.dp.scaled(screenScale))
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_edit_favorites),
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    color = if (specialFocused) bgColor else textColor
                                )
                            }
                        }

                        // Action button for pages 2 and 3
                        if (page == 2 || page == 3) {
                            Spacer(modifier = Modifier.height(32.dp.scaled(screenScale)))
                            val actionInteraction = remember { MutableInteractionSource() }
                            val actionFocused = actionInteraction.collectIsFocusedAsState().value
                            Box(
                                modifier = Modifier
                                    .then(with(SettingsComposable) {
                                        Modifier.pillHighlight(
                                            isHighlighted = actionFocused,
                                            color = textColor,
                                            outerHorizontal = 4.dp,
                                            outerVertical = 4.dp
                                        )
                                    })
                                    .clip(buttonShape)
                                    .background(if (actionFocused) textColor else bgColor)
                                    .then(if (!actionFocused) Modifier.border(1.5.dp.scaled(screenScale), textColor, buttonShape) else Modifier)
                                    .clickable(
                                        interactionSource = actionInteraction,
                                        indication = null
                                    ) {
                                        val intent = if (page == 2) {
                                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        } else {
                                            Intent(Settings.ACTION_HOME_SETTINGS)
                                        }
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 24.dp.scaled(screenScale), vertical = 12.dp.scaled(screenScale))
                            ) {
                                Text(
                                    text = when {
                                        page == 2 && hasPermission -> stringResource(R.string.onboarding_permission_granted)
                                        page == 2 -> stringResource(R.string.onboarding_grant_permission)
                                        page == 3 && isDefaultLauncher -> stringResource(R.string.onboarding_default_set)
                                        else -> stringResource(R.string.onboarding_set_default)
                                    },
                                    style = SettingsTheme.typography.title,
                                    fontSize = titleFontSize,
                                    color = if (actionFocused) bgColor else textColor
                                )
                            }
                        }
                    }
                }

                // Separator line above footer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp.scaled(screenScale))
                        .background(textColor)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp.scaled(screenScale)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // BACK button
                    val backInteraction = remember { MutableInteractionSource() }
                    val backFocused = backInteraction.collectIsFocusedAsState().value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (backFocused) textColor else bgColor)
                            .clickable(
                                enabled = page > 0,
                                interactionSource = backInteraction,
                                indication = null
                            ) { if (page == 5 && showThemePicker) showThemePicker = false else if (page > 0) page-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.btn_back_upper),
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (backFocused) bgColor else textColor,
                            textDecoration = if (page == 0) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    }

                    // Vertical separator
                    Box(
                        modifier = Modifier
                            .width(1.5.dp.scaled(screenScale))
                            .fillMaxHeight()
                            .background(textColor)
                    )

                    // Page indicator
                    Box(
                        modifier = Modifier.width(54.dp.scaled(screenScale)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${page + 1}/$totalPages",
                            style = SettingsTheme.typography.body,
                            fontSize = (titleFontSize.value * 0.7f).sp,
                            color = textColor
                        )
                    }

                    // Vertical separator
                    Box(
                        modifier = Modifier
                            .width(1.5.dp.scaled(screenScale))
                            .fillMaxHeight()
                            .background(textColor)
                    )

                    // NEXT button
                    val nextInteraction = remember { MutableInteractionSource() }
                    val nextFocused = nextInteraction.collectIsFocusedAsState().value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (nextFocused) textColor else bgColor)
                            .clickable(
                                interactionSource = nextInteraction,
                                indication = null
                            ) {
                                if (page == 1 && !privacyAgreed) {
                                    if (!privacyToastShown) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "In order to continue you'll need to agree to the privacy policy",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        privacyToastShown = true
                                    }
                                    return@clickable
                                }
                                if (page == 4 && !settingsConfirmed) {
                                    if (!settingsToastShown) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Please confirm you've read the settings instructions",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        settingsToastShown = true
                                    }
                                    return@clickable
                                }
                                if (page == 5 && showThemePicker) {
                                    themeSelectAction.value?.invoke()
                                    showThemePicker = false
                                }
                                if (page < totalPages - 1) page++ else onFinish()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (page == 5 && !showThemePicker) stringResource(R.string.btn_skip) else if (page == 5 && showThemePicker) stringResource(R.string.btn_select) else if (page < totalPages - 1) stringResource(R.string.btn_next) else stringResource(R.string.btn_finish),
                            style = SettingsTheme.typography.title,
                            fontSize = titleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (nextFocused) bgColor else textColor,
                            textDecoration = if ((page == 1 && !privacyAgreed) || (page == 4 && !settingsConfirmed)) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    }
                }

                // Bottom border when nav bar is visible
                if (WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() > 0.dp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp.scaled(screenScale))
                            .background(textColor)
                    )
                }
            }
        }
    }
}
