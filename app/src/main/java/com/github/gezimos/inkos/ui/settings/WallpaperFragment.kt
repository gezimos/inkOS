package com.github.gezimos.inkos.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.ui.compose.WallpaperUI

class WallpaperFragment : Fragment() {

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
        
        val backgroundColor = getHexForOpacity(requireContext())
        val isDark = prefs.appTheme == Constants.Theme.Dark
        val context = requireContext()
        
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val composeView = ComposeView(context).apply {
            setContent {
                SettingsTheme(isDark) {
                    // Observe settingsSize from ViewModel inside Compose
                    val homeUiStateValue by viewModel.homeUiState.collectAsState()
                    val settingsSize = (homeUiStateValue.settingsSize - 3)
                    
                    WallpaperUI(
                        onBackClick = { findNavController().popBackStack() },
                        fontSize = if (settingsSize > 0) settingsSize.sp else TextUnit.Unspecified,
                        isDark = isDark,
                        showStatusBar = homeUiStateValue.showStatusBar,
                        onExternalWallpaperClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER)
                                startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Wallpaper settings not available", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWallpaperSet = {
                            findNavController().popBackStack(com.github.gezimos.inkos.R.id.mainFragment, false)
                        }
                    )
                }
            }
        }

        rootLayout.addView(composeView)
        return rootLayout
    }
}
