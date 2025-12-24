package com.github.gezimos.inkos.ui

import androidx.compose.runtime.*
import androidx.compose.ui.text.input.TextFieldValue
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants

/**
 * State holder for Apps Drawer - manages all UI state
 */
@Stable
class AppsDrawerState(
    val flag: Constants.AppDrawerFlag,
    val position: Int,
    apps: List<AppListItem>,
    searchQuery: TextFieldValue = TextFieldValue(""),
    currentPage: Int = 0,
    selectedItemIndex: Int = 0,
    isDpadMode: Boolean = false,
    showContextMenu: Boolean = false,
    contextMenuApp: AppListItem? = null,
    showRenameOverlay: Boolean = false,
    renameApp: AppListItem? = null,
    appsPerPage: Int = 1,
    containerHeightPx: Int = 0
) {
    var apps by mutableStateOf(apps)
    var searchQuery by mutableStateOf(searchQuery)
    var currentPage by mutableStateOf(currentPage)
    var selectedItemIndex by mutableStateOf(selectedItemIndex)
    var isDpadMode by mutableStateOf(isDpadMode)
    var showContextMenu by mutableStateOf(showContextMenu)
    var contextMenuApp by mutableStateOf(contextMenuApp)
    var showRenameOverlay by mutableStateOf(showRenameOverlay)
    var renameApp by mutableStateOf(renameApp)
    var appsPerPage by mutableStateOf(appsPerPage)
    var azFilterLetter by mutableStateOf<Char?>(null)
    var azFilterFocused by mutableStateOf(false)
    var azFilterSelectedIndex by mutableStateOf(0)
    var searchFocused by mutableStateOf(false)
    
    val isHiddenAppsMode = flag == Constants.AppDrawerFlag.HiddenApps
    
    // Search is now a UI element, not an app - check if enabled via parameter
    // This will be set from AppsFragment based on prefs.appDrawerSearchEnabled
    var searchEnabled: Boolean = false
    
    val appsWithoutSearch: List<AppListItem>
        get() = apps.sorted()
}
