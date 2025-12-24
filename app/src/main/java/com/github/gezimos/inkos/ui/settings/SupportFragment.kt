package com.github.gezimos.inkos.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.BuildConfig
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Constants.Theme.Dark
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.helper.openAppInfo
import com.github.gezimos.inkos.helper.utils.EinkScrollBehavior
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect
import com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle

class SupportFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    // Paging state
    private var currentPage = intArrayOf(0)
    private var pageCount = intArrayOf(1)
    private var bottomInsetPx = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        prefs = viewModel.getPrefs()
        val isDark = prefs.appTheme == Dark
        val backgroundColor = getHexForOpacity(requireContext())
        val context = requireContext()

        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Sticky header ComposeView
        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = "Support",
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                    }
                }
            }
        }
        rootLayout.addView(headerView)

        val nestedScrollView = NestedScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(
                androidx.compose.ui.platform.ComposeView(context).apply {
                    setContent {
                        val homeUiState by viewModel.homeUiState.collectAsState()
                        val settingsSize = (homeUiState.settingsSize - 3)
                        SettingsTheme(isDark) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    SupportSettingsAllInOne(settingsSize.sp)
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
        
        // Handle bottom insets for navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        rootLayout.addView(
            nestedScrollView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Apply bottom padding to prevent scroll view from going under navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

        // Use EinkScrollBehavior callback to update page indicator reliably
        val scrollBehavior = EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            headerView.setContent {
                val homeUiState by viewModel.homeUiState.collectAsState()
                val settingsSize = (homeUiState.settingsSize - 3)
                SettingsTheme(isDark) {
                    Column(Modifier.fillMaxWidth()) {
                        PageHeader(
                            iconRes = R.drawable.ic_back,
                            title = "Support",
                            onClick = { findNavController().popBackStack() },
                            showStatusBar = homeUiState.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified
                        )
                    }
                }
            }
        }
        scrollBehavior.attachToScrollView(nestedScrollView)
        return rootLayout
    }

    @Composable
    fun SupportSettingsAllInOne(fontSize: TextUnit = TextUnit.Unspecified) {
        val titleFontSize = if (fontSize.isSpecified) (fontSize.value * 1.5).sp else fontSize
        val context = LocalContext.current

        Column(modifier = Modifier.fillMaxSize()) {
            // Donate/Support section
            SettingsTitle(
                text = "Donate/Support",
                fontSize = titleFontSize,
            )

            // Donate - Buy Me a Coffee
            SettingsSelect(
                title = "buymeacoffee.com/",
                option = "gezimos",
                fontSize = titleFontSize,
                enabled = true,
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/gezimos".toUri())
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Ignore failures to open browser
                    }
                }
            )

            // GitHub Sponsor
            SettingsSelect(
                title = "Sponsor",
                option = "github.com/gezimos",
                fontSize = titleFontSize,
                enabled = true,
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/gezimos".toUri())
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Ignore failures to open browser
                    }
                }
            )

            // Support section
            SettingsTitle(
                text = "Support",
                fontSize = titleFontSize,
            )

            // Report Issues - Github
            SettingsSelect(
                title = "Report Issues",
                option = "Github",
                fontSize = titleFontSize,
                enabled = true,
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/gezimos/inkOS/issues".toUri())
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Ignore failures to open browser
                    }
                }
            )

            // Reddit
            SettingsSelect(
                title = "Reddit",
                option = "r/inkos",
                fontSize = titleFontSize,
                enabled = true,
                onClick = {
                    try {
                        // Try reddit:// deep link first (opens app directly to subreddit)
                        val redditDeepLink = Intent(Intent.ACTION_VIEW, "reddit://r/inkos".toUri())
                        try {
                            context.startActivity(redditDeepLink)
                        } catch (_: Exception) {
                            // If reddit:// fails, try https:// which will open in app if installed
                            val webIntent = Intent(Intent.ACTION_VIEW, "https://reddit.com/r/inkos".toUri())
                            context.startActivity(webIntent)
                        }
                    } catch (_: Exception) {
                        // Ignore failures
                    }
                }
            )

            // Other section
            SettingsTitle(
                text = "Other",
                fontSize = titleFontSize,
            )

            // App version
            SettingsSelect(
                title = stringResource(R.string.app_version),
                option = "v0.4",
                fontSize = titleFontSize,
                enabled = true,
                onClick = {
                    openAppInfo(
                        context,
                        android.os.Process.myUserHandle(),
                        BuildConfig.APPLICATION_ID
                    )
                }
            )

            // Credits
            SettingsSelect(
                title = "Credits",
                option = "mLauncher, oLauncher",
                fontSize = titleFontSize,
                enabled = true,
                onClick = {
                    // Credits are informational, no action needed
                }
            )
        }
    }

}
