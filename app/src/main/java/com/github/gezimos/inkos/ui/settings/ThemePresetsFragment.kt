package com.github.gezimos.inkos.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.github.gezimos.inkos.ui.compose.inkOsSafeDrawingPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.github.gezimos.inkos.MainViewModel
import com.github.gezimos.inkos.data.Constants
import com.github.gezimos.inkos.data.Prefs
import com.github.gezimos.inkos.helper.getHexForOpacity
import com.github.gezimos.inkos.style.LocalPrefs
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.ThemePresetsPage

class ThemePresetsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        prefs = viewModel.getPrefs()

        val context = requireContext()

        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val composeView = ComposeView(context).apply {
            setContent {
                val homeUiStateValue by viewModel.homeUiState.collectAsState()
                val currentIsDark = homeUiStateValue.appTheme == Constants.Theme.Dark ||
                    (homeUiStateValue.appTheme == Constants.Theme.System &&
                        androidx.compose.foundation.isSystemInDarkTheme())

                androidx.compose.runtime.LaunchedEffect(homeUiStateValue.backgroundColor) {
                    val bgColor = getHexForOpacity(context)
                    rootLayout.setBackgroundColor(bgColor)
                }

                androidx.compose.runtime.CompositionLocalProvider(
                    LocalPrefs provides prefs
                ) {
                    Box(Modifier.fillMaxSize().inkOsSafeDrawingPadding()) {
                        val settingsSize = (homeUiStateValue.settingsSize - 3)

                        ThemePresetsPage(
                            onBackClick = { findNavController().popBackStack() },
                            isDark = currentIsDark,
                            showStatusBar = homeUiStateValue.showStatusBar,
                            viewModel = viewModel,
                            prefs = prefs,
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else TextUnit.Unspecified,
                            onConfirmSelect = { findNavController().popBackStack() }
                        )
                    }
                }
            }
        }

        rootLayout.addView(composeView)
        return rootLayout
    }
}
