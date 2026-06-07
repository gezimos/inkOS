package com.github.gezimos.inkos.ui

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.text.input.TextFieldValue
import com.github.gezimos.inkos.data.AppListItem
import com.github.gezimos.inkos.data.Constants
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
    appsPerPage: Int = 4,
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
    var privateSpaceFilter by mutableStateOf(false)
    var searchFocused by mutableStateOf(false)
    
    val isHiddenAppsMode = flag == Constants.AppDrawerFlag.HiddenApps
    val isEditFavoritesMode = flag == Constants.AppDrawerFlag.EditFavorites
    val isEditHiddenMode = flag == Constants.AppDrawerFlag.EditHiddenApps

    var searchEnabled: Boolean = false

    val selectedFavorites: SnapshotStateList<AppListItem> = mutableStateListOf()
    val selectedHidden: SnapshotStateList<AppListItem> = mutableStateListOf()
    
    // Done button focus state (DPAD navigation)
    var doneFocused by mutableStateOf(false)

    var titleFocused by mutableStateOf(false)

    // DPAD drag handle focus and grab state
    var dragHandleFocused by mutableStateOf(false)
    var dpadGrabbedIndex by mutableStateOf<Int?>(null)

    var draggingIndex by mutableStateOf<Int?>(null)
    var dragOffsetY by mutableStateOf(0f)
}
