package com.github.gezimos.inkos.ui.compose

import androidx.compose.runtime.MutableState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
class KeyPressTracker {
    private var keyDownTime: Long = 0L
    private var trackedKey: Key? = null
    private var isHolding: Boolean = false
    var longPressHandled: Boolean = false
    
    fun onKeyDown(key: Key) {
        // Always start fresh tracking for the key
        // This ensures we don't get stuck in a stale state
        trackedKey = key
        keyDownTime = System.currentTimeMillis()
        isHolding = true
        longPressHandled = false
    }
    
    fun isKeyHeld(key: Key): Boolean {
        return isHolding && trackedKey == key
    }

    fun getDuration(key: Key): Long {
        if (trackedKey == key && isHolding) {
            return System.currentTimeMillis() - keyDownTime
        }
        return 0L
    }
    
    fun onKeyUp(key: Key): Long {
        if (trackedKey == key && isHolding) {
            val duration = System.currentTimeMillis() - keyDownTime
            val wasTracking = true
            reset()
            return if (duration > 0) duration else if (wasTracking) 1L else 0L
        }
        reset()
        return 0L
    }
    
    fun reset() {
        trackedKey = null
        keyDownTime = 0L
        isHolding = false
        longPressHandled = false
    }
}
enum class FocusZone {
    CLOCK, DATE, APPS, MEDIA_WIDGET, QUOTE
}
enum class SimpleTrayFocusZone {
    QUICK_SETTINGS,
    BRIGHTNESS_SLIDER,
    CLEAR_ALL,
    NOTIFICATIONS,
    BOTTOM_NAV
}

enum class HubFocusZone {
    CATEGORY_TABS,
    CLEAR_ALL,
    NOTIFICATIONS
}

enum class ThemePresetFocusZone {
    HEADER,
    CARD,
    SKIP_ROW,
    BOTTOM_ROW
}

enum class ColorEditorFocusZone {
    HEADER,      // back(0), save(1)
    TABS,        // light(0), dark(1)
    COLOR_ROWS   // background(0), text(1)
}

object NavHelper {
    const val LONG_PRESS_THRESHOLD_MS = 700L
    fun handleHomeKeyEvent(
        keyEvent: KeyEvent,
        keyPressTracker: KeyPressTracker,
        dpadMode: MutableState<Boolean>,
        selectedIndex: MutableState<Int>,
        appsOnPageSize: Int,
        currentPage: Int,
        totalPages: Int,
        appsPerPage: Int,
        adjustPageBy: (Int) -> Unit,
        onAppClick: (index: Int) -> Unit,
        onAppLongClick: (index: Int) -> Unit,
        onNavigateBack: () -> Unit,
        onSwipeLeft: (() -> Unit)? = null,
        onSwipeRight: (() -> Unit)? = null,
        disableSwipeGestures: Boolean = false,
        // Multi-zone navigation parameters
        focusZone: MutableState<FocusZone>? = null,
        selectedMediaButton: MutableState<Int>? = null,
        showClock: Boolean = false,
        showDate: Boolean = false,
        showMediaWidget: Boolean = false,
        showQuote: Boolean = false,
        onClockClick: (() -> Unit)? = null,
        onDateClick: (() -> Unit)? = null,
        onMediaAction: ((Int) -> Unit)? = null,
        onQuoteClick: (() -> Unit)? = null,
        onSwipeUp: (() -> Unit)? = null,
        onSwipeDown: (() -> Unit)? = null
    ): Boolean {
        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.repeatCount == 0) {
                dpadMode.value = true
                if (focusZone != null && focusZone.value != FocusZone.APPS) {
                    when (focusZone.value) {
                        FocusZone.CLOCK -> onClockClick?.invoke()
                        FocusZone.DATE -> onDateClick?.invoke()
                        FocusZone.MEDIA_WIDGET -> selectedMediaButton?.value?.let { onMediaAction?.invoke(it) }
                        FocusZone.QUOTE -> onQuoteClick?.invoke()
                        else -> {}
                    }
                    return true
                }
            }
            // For APPS zone, use long-press detection
            if (focusZone == null || focusZone.value == FocusZone.APPS) {
                return when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        dpadMode.value = true
                        if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                            keyPressTracker.onKeyDown(keyEvent.key)
                        } else {
                            if (!keyPressTracker.longPressHandled && keyPressTracker.getDuration(keyEvent.key) >= LONG_PRESS_THRESHOLD_MS) {
                                onAppLongClick(selectedIndex.value)
                                keyPressTracker.longPressHandled = true
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (keyPressTracker.longPressHandled) {
                            keyPressTracker.reset()
                        } else {
                            val duration = keyPressTracker.onKeyUp(keyEvent.key)
                            if (duration > 0) {
                                onAppClick(selectedIndex.value)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            return true
        }
        
        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.DirectionDown -> {
                dpadMode.value = true
                if (focusZone != null) {
                    when (focusZone.value) {
                        FocusZone.CLOCK -> {
                            if (showDate) focusZone.value = FocusZone.DATE
                            else if (appsOnPageSize > 0) focusZone.value = FocusZone.APPS
                        }
                        FocusZone.DATE -> {
                            if (appsOnPageSize > 0) focusZone.value = FocusZone.APPS
                        }
                        FocusZone.APPS -> {
                            if (appsOnPageSize > 0) {
                                if (selectedIndex.value < appsOnPageSize - 1) {
                                    selectedIndex.value = selectedIndex.value + 1
                                } else if (currentPage < totalPages - 1) {
                                    adjustPageBy(1)
                                    selectedIndex.value = 0
                                } else {
                                    if (showMediaWidget) {
                                        focusZone.value = FocusZone.MEDIA_WIDGET
                                        selectedMediaButton?.value = 0
                                    } else if (showQuote) {
                                        focusZone.value = FocusZone.QUOTE
                                    } else {
                                        onSwipeDown?.invoke()
                                    }
                                }
                            }
                        }
                        FocusZone.MEDIA_WIDGET -> {
                            // Navigate down through media buttons
                            if (selectedMediaButton != null) {
                                if (selectedMediaButton.value < 4) {
                                    selectedMediaButton.value = selectedMediaButton.value + 1
                                } else if (showQuote) {
                                    // Move to quote from last button
                                    focusZone.value = FocusZone.QUOTE
                                } else {
                                    onSwipeDown?.invoke()
                                }
                            } else if (showQuote) {
                                focusZone.value = FocusZone.QUOTE
                            }
                        }
                        FocusZone.QUOTE -> {
                            // At bottom boundary - trigger swipe down action
                            onSwipeDown?.invoke()
                        }
                    }
                } else {
                    // Legacy behavior when focusZone is null
                    if (appsOnPageSize > 0) {
                        if (selectedIndex.value < appsOnPageSize - 1) {
                            selectedIndex.value = selectedIndex.value + 1
                        } else if (currentPage < totalPages - 1) {
                            adjustPageBy(1)
                            selectedIndex.value = 0
                        }
                    }
                }
                true
            }
            Key.DirectionUp -> {
                dpadMode.value = true
                if (focusZone != null) {
                    when (focusZone.value) {
                        FocusZone.CLOCK -> {
                            // At top boundary - trigger swipe up action
                            onSwipeUp?.invoke()
                        }
                        FocusZone.DATE -> {
                            if (showClock) focusZone.value = FocusZone.CLOCK
                            else onSwipeUp?.invoke()
                        }
                        FocusZone.APPS -> {
                            if (appsOnPageSize > 0) {
                                if (selectedIndex.value > 0) {
                                    selectedIndex.value = selectedIndex.value - 1
                                } else if (currentPage > 0) {
                                    adjustPageBy(-1)
                                    selectedIndex.value = appsPerPage - 1
                                } else {
                                    if (showDate) {
                                        focusZone.value = FocusZone.DATE
                                    } else if (showClock) {
                                        focusZone.value = FocusZone.CLOCK
                                    } else {
                                        onSwipeUp?.invoke()
                                    }
                                }
                            }
                        }
                        FocusZone.MEDIA_WIDGET -> {
                            // Navigate up through media buttons
                            if (selectedMediaButton != null && selectedMediaButton.value > 0) {
                                selectedMediaButton.value = selectedMediaButton.value - 1
                            } else if (appsOnPageSize > 0) {
                                focusZone.value = FocusZone.APPS
                                selectedIndex.value = appsOnPageSize - 1
                            }
                        }
                        FocusZone.QUOTE -> {
                            if (showMediaWidget) {
                                focusZone.value = FocusZone.MEDIA_WIDGET
                                selectedMediaButton?.value = 4 // Last button (Stop)
                            } else if (appsOnPageSize > 0) {
                                focusZone.value = FocusZone.APPS
                                selectedIndex.value = appsOnPageSize - 1
                            }
                        }
                    }
                } else {
                    // Legacy behavior when focusZone is null
                    if (appsOnPageSize > 0) {
                        if (selectedIndex.value > 0) {
                            selectedIndex.value = selectedIndex.value - 1
                        } else if (currentPage > 0) {
                            adjustPageBy(-1)
                            selectedIndex.value = appsPerPage - 1
                        }
                    }
                }
                true
            }
            Key.DirectionLeft -> {
                dpadMode.value = true
                if (!disableSwipeGestures) {
                    try { onSwipeLeft?.invoke() } catch (_: Exception) {}
                }
                true
            }
            Key.DirectionRight -> {
                dpadMode.value = true
                if (!disableSwipeGestures) {
                    try { onSwipeRight?.invoke() } catch (_: Exception) {}
                }
                true
            }
            Key.PageUp -> {
                dpadMode.value = true
                adjustPageBy(-1)
                true
            }
            Key.PageDown -> {
                dpadMode.value = true
                adjustPageBy(1)
                true
            }
            else -> false
        }
    }

    // --- Notifications-specific key mapping (migrated here) ---
    sealed class NotificationKeyAction {
        object None : NotificationKeyAction()
        object PageUp : NotificationKeyAction()
        object PageDown : NotificationKeyAction()
        object Dismiss : NotificationKeyAction()
        object Open : NotificationKeyAction()
    }

    fun mapNotificationsKey(prefs: com.github.gezimos.inkos.data.Prefs, keyCode: Int, event: android.view.KeyEvent): NotificationKeyAction {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return NotificationKeyAction.None

        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_UP -> NotificationKeyAction.PageUp
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> NotificationKeyAction.PageDown
            android.view.KeyEvent.KEYCODE_PAGE_UP -> NotificationKeyAction.PageUp
            android.view.KeyEvent.KEYCODE_PAGE_DOWN -> NotificationKeyAction.PageDown

            // Volume keys for page navigation (if enabled)
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> if (prefs.useVolumeKeysForPages) NotificationKeyAction.PageUp else NotificationKeyAction.None
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> if (prefs.useVolumeKeysForPages) NotificationKeyAction.PageDown else NotificationKeyAction.None

            android.view.KeyEvent.KEYCODE_DEL, android.view.KeyEvent.KEYCODE_MENU, android.view.KeyEvent.KEYCODE_1 -> NotificationKeyAction.Dismiss
            android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER, android.view.KeyEvent.KEYCODE_3 -> NotificationKeyAction.Open
            else -> NotificationKeyAction.None
        }
    }

    // --- AppDrawer-specific key mapping (migrated here) ---
    sealed class AppDrawerKeyAction {
        object None : AppDrawerKeyAction()
        object PageUp : AppDrawerKeyAction()
        object PageDown : AppDrawerKeyAction()
        object MoveSelectionUp : AppDrawerKeyAction()
        object MoveSelectionDown : AppDrawerKeyAction()
        object SelectItem : AppDrawerKeyAction()
        object LongPressItem : AppDrawerKeyAction()
    }

    fun mapAppDrawerKey(prefs: com.github.gezimos.inkos.data.Prefs, keyCode: Int, event: android.view.KeyEvent): AppDrawerKeyAction {
        if (event.action != android.view.KeyEvent.ACTION_DOWN) return AppDrawerKeyAction.None

        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_UP -> AppDrawerKeyAction.MoveSelectionUp
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> AppDrawerKeyAction.MoveSelectionDown

            // Page navigation keys
            android.view.KeyEvent.KEYCODE_PAGE_UP -> AppDrawerKeyAction.PageUp
            android.view.KeyEvent.KEYCODE_PAGE_DOWN -> AppDrawerKeyAction.PageDown

            // Selection keys
            android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                if (event.isLongPress) AppDrawerKeyAction.LongPressItem else AppDrawerKeyAction.SelectItem
            }

            // Volume keys for page navigation (if enabled)
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> if (prefs.useVolumeKeysForPages) AppDrawerKeyAction.PageUp else AppDrawerKeyAction.None
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> if (prefs.useVolumeKeysForPages) AppDrawerKeyAction.PageDown else AppDrawerKeyAction.None

            else -> AppDrawerKeyAction.None
        }
    }
    fun handleAppsKeyEvent(
        keyEvent: KeyEvent,
        keyPressTracker: KeyPressTracker,
        isDpadModeSetter: (Boolean) -> Unit,
        selectedIndexGetter: () -> Int,
        selectedIndexSetter: (Int) -> Unit,
        currentPage: Int,
        totalPages: Int,
        displayAppsSize: Int,
        onPreviousPage: () -> Unit,
        onNextPage: () -> Unit,
        onAppClick: (index: Int) -> Unit,
        onAppLongClick: (index: Int) -> Unit,
        onNavigateBack: () -> Unit,
        // New parameters for A-Z filter and search
        showAzFilter: Boolean = false,
        azFilterFocused: Boolean = false,
        onAzFilterFocusChange: ((Boolean) -> Unit)? = null,
        azFilterSelectedIndex: Int = 0,
        onAzFilterIndexChange: ((Int) -> Unit)? = null,
        azFilterSize: Int = 27,
        onAzFilterActivate: (() -> Unit)? = null,
        showSearch: Boolean = false,
        onSearchFocus: (() -> Unit)? = null,
        // Edit Favorites parameters
        isEditFavoritesMode: Boolean = false,
        allowDragReorder: Boolean = false,
        titleFocusable: Boolean = false,
        hasDoneButton: Boolean = false,
        doneFocused: Boolean = false,
        onDoneFocusChange: ((Boolean) -> Unit)? = null,
        onDoneActivate: (() -> Unit)? = null,
        onFavoriteToggle: ((index: Int) -> Unit)? = null,
        // Edit Favorites title focus
        titleFocused: Boolean = false,
        onTitleFocusChange: ((Boolean) -> Unit)? = null,
        onTitleActivate: (() -> Unit)? = null,
        // DPAD drag reorder parameters
        dragHandleFocused: Boolean = false,
        onDragHandleFocusChange: ((Boolean) -> Unit)? = null,
        dpadGrabbedIndex: Int? = null,
        onDpadGrab: (() -> Unit)? = null,
        onDpadDrop: (() -> Unit)? = null,
        onDpadMoveUp: (() -> Unit)? = null,
        onDpadMoveDown: (() -> Unit)? = null,
        isItemChecked: ((Int) -> Boolean)? = null
    ): Boolean {
        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (dpadGrabbedIndex != null) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    isDpadModeSetter(true)
                    onDpadDrop?.invoke()
                }
                return true
            }
            if (dragHandleFocused) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    isDpadModeSetter(true)
                    onDpadGrab?.invoke()
                }
                return true
            }
            // Done button in edit favorites mode
            if (doneFocused) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    isDpadModeSetter(true)
                    onDoneActivate?.invoke()
                }
                return true
            }
            // Title in edit favorites mode
            if (titleFocused) {
                if (keyEvent.type == KeyEventType.KeyDown) {
                    isDpadModeSetter(true)
                    onTitleActivate?.invoke()
                }
                return true
            }
        }

        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (azFilterFocused) {
                // Activate the A-Z filter selection
                if (keyEvent.type == KeyEventType.KeyDown) {
                    isDpadModeSetter(true)
                    onAzFilterActivate?.invoke()
                    return true
                }
                return true
            } else {
                // Normal app selection handling
                return when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        isDpadModeSetter(true)
                        if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                            keyPressTracker.onKeyDown(keyEvent.key)
                        } else {
                            if (!keyPressTracker.longPressHandled && keyPressTracker.getDuration(keyEvent.key) >= LONG_PRESS_THRESHOLD_MS) {
                                onAppLongClick(selectedIndexGetter())
                                keyPressTracker.longPressHandled = true
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (keyPressTracker.longPressHandled) {
                            keyPressTracker.reset()
                        } else {
                            val duration = keyPressTracker.onKeyUp(keyEvent.key)
                            if (duration > 0) {
                                if (isEditFavoritesMode && onFavoriteToggle != null) {
                                    onFavoriteToggle(selectedIndexGetter())
                                } else {
                                    onAppClick(selectedIndexGetter())
                                }
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.DirectionUp -> {
                isDpadModeSetter(true)

                if (dpadGrabbedIndex != null) {
                    onDpadMoveUp?.invoke()
                } else if (doneFocused || titleFocused) {
                    // Already at header row, nowhere to go up
                } else if (azFilterFocused) {
                    // Navigate up in A-Z filter
                    val newIndex = (azFilterSelectedIndex - 1).coerceAtLeast(0)
                    onAzFilterIndexChange?.invoke(newIndex)
                } else {
                    val selected = selectedIndexGetter()

                    if (selected > 0) {
                        selectedIndexSetter(selected - 1)
                    } else if (currentPage > 0) {
                        onPreviousPage()
                        selectedIndexSetter(displayAppsSize - 1)
                    } else if (isEditFavoritesMode || titleFocusable) {
                        // At first item on first page — focus the title/header action
                        onTitleFocusChange?.invoke(true)
                    } else if (showSearch) {
                        onSearchFocus?.invoke()
                    }
                }
                true
            }
            Key.DirectionDown -> {
                isDpadModeSetter(true)

                if (dpadGrabbedIndex != null) {
                    onDpadMoveDown?.invoke()
                } else if (doneFocused) {
                    // Move from Done button back to app list
                    onDoneFocusChange?.invoke(false)
                    selectedIndexSetter(0)
                } else if (titleFocused) {
                    // Move from title back to app list
                    onTitleFocusChange?.invoke(false)
                    selectedIndexSetter(0)
                } else if (azFilterFocused) {
                    // Navigate down in A-Z filter
                    val newIndex = (azFilterSelectedIndex + 1).coerceAtMost(azFilterSize - 1)
                    onAzFilterIndexChange?.invoke(newIndex)
                } else {
                    // If in search field, move back to app list

                    val selected = selectedIndexGetter()
                    if (selected < displayAppsSize - 1) {
                        selectedIndexSetter(selected + 1)
                    } else if (currentPage < totalPages - 1) {
                        onNextPage()
                        selectedIndexSetter(0)
                    }
                }
                true
            }
            Key.DirectionLeft -> {
                isDpadModeSetter(true)
                if (dpadGrabbedIndex != null) {
                    // Block Left while grabbed
                } else if (titleFocused && hasDoneButton) {
                    // Move from title to Done button
                    onTitleFocusChange?.invoke(false)
                    onDoneFocusChange?.invoke(true)
                } else if (doneFocused) {
                    // Move from Done to title
                    onDoneFocusChange?.invoke(false)
                    onTitleFocusChange?.invoke(true)
                } else if (dragHandleFocused) {
                    // Move focus back from drag handle to app label
                    onDragHandleFocusChange?.invoke(false)
                } else if (azFilterFocused) {
                    // Move from A-Z filter back to app list
                    onAzFilterFocusChange?.invoke(false)
                }
                true
            }
            Key.DirectionRight -> {
                isDpadModeSetter(true)
                if (dpadGrabbedIndex != null) {
                    // Block Right while grabbed
                } else if (titleFocused && hasDoneButton) {
                    // Move from title to Done button
                    onTitleFocusChange?.invoke(false)
                    onDoneFocusChange?.invoke(true)
                } else if (doneFocused) {
                    // Move from Done to title
                    onDoneFocusChange?.invoke(false)
                    onTitleFocusChange?.invoke(true)
                } else if (isEditFavoritesMode && allowDragReorder && !dragHandleFocused && !doneFocused) {
                    val idx = selectedIndexGetter()
                    if (isItemChecked?.invoke(idx) == true) {
                        onDragHandleFocusChange?.invoke(true)
                    }
                } else if (!azFilterFocused && showAzFilter) {
                    onAzFilterFocusChange?.invoke(true)
                }
                true
            }
            Key.PageUp -> {
                isDpadModeSetter(true)
                if (dpadGrabbedIndex == null) {
                    onPreviousPage()
                    selectedIndexSetter(0)
                }
                true
            }
            Key.PageDown -> {
                isDpadModeSetter(true)
                if (dpadGrabbedIndex == null) {
                    onNextPage()
                    selectedIndexSetter(0)
                }
                true
            }
            Key.Escape, Key.Back -> {
                // Cancel grab if active
                if (dpadGrabbedIndex != null) {
                    onDpadDrop?.invoke()
                    return true
                }
                false
            }
            Key.Backspace, Key.Delete -> {
                false
            }
            else -> false
        }
    }
    fun handleSimpleTrayKeyEvent(
        keyEvent: KeyEvent,
        keyPressTracker: KeyPressTracker,
        isDpadModeSetter: (Boolean) -> Unit,
        focusZone: MutableState<SimpleTrayFocusZone>,
        // Quick Settings (5 buttons)
        selectedQuickSettingIndex: MutableState<Int>,
        onQuickSettingActivate: (index: Int) -> Unit,
        // Brightness Slider
        onBrightnessAdjust: (delta: Float) -> Unit,
        // Clear All
        onClearAll: () -> Unit,
        // Notifications (up to 3 per page)
        selectedNotificationIndex: MutableState<Int>,
        notificationsOnPageSize: Int,
        pageSize: Int,
        currentPage: Int,
        totalPages: Int,
        onNextPage: () -> Unit,
        onPreviousPage: () -> Unit,
        onNotificationClick: (index: Int) -> Unit,
        onNotificationLongClick: (index: Int) -> Unit,
        // Bottom Nav (2 buttons)
        selectedBottomNavIndex: MutableState<Int>,
        onBottomNavActivate: (index: Int) -> Unit,
        bottomNavEnabled: Boolean = true
    ): Boolean {
        // Handle activation keys (Enter, DPAD Center)
        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.repeatCount == 0) {
                isDpadModeSetter(true)
                // For notifications zone, use long-press detection
                if (focusZone.value == SimpleTrayFocusZone.NOTIFICATIONS) {
                    keyPressTracker.onKeyDown(keyEvent.key)
                    return true
                } else {
                    // For other zones, activate immediately
                    when (focusZone.value) {
                        SimpleTrayFocusZone.QUICK_SETTINGS -> onQuickSettingActivate(selectedQuickSettingIndex.value)
                        SimpleTrayFocusZone.BRIGHTNESS_SLIDER -> {} // No activation for slider
                        SimpleTrayFocusZone.CLEAR_ALL -> onClearAll()
                        SimpleTrayFocusZone.BOTTOM_NAV -> onBottomNavActivate(selectedBottomNavIndex.value)
                        else -> {}
                    }
                    return true
                }
            }
            
            // Handle long-press for notifications zone
            if (focusZone.value == SimpleTrayFocusZone.NOTIFICATIONS) {
                return when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        isDpadModeSetter(true)
                        if (keyEvent.nativeKeyEvent.repeatCount > 0) {
                            if (!keyPressTracker.longPressHandled && keyPressTracker.getDuration(keyEvent.key) >= LONG_PRESS_THRESHOLD_MS) {
                                onNotificationLongClick(selectedNotificationIndex.value)
                                keyPressTracker.longPressHandled = true
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (keyPressTracker.longPressHandled) {
                            keyPressTracker.reset()
                        } else {
                            val duration = keyPressTracker.onKeyUp(keyEvent.key)
                            if (duration > 0) {
                                onNotificationClick(selectedNotificationIndex.value)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            return true
        }
        
        // For other keys, only handle on KeyDown
        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.DirectionDown -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    SimpleTrayFocusZone.QUICK_SETTINGS -> {
                        // Move to next quick setting button
                        if (selectedQuickSettingIndex.value < 4) {
                            selectedQuickSettingIndex.value = selectedQuickSettingIndex.value + 1
                        } else {
                            // At last button, move to brightness zone
                            focusZone.value = SimpleTrayFocusZone.BRIGHTNESS_SLIDER
                        }
                    }
                    SimpleTrayFocusZone.BRIGHTNESS_SLIDER -> {
                        // Move to clear all zone
                        focusZone.value = SimpleTrayFocusZone.CLEAR_ALL
                    }
                    SimpleTrayFocusZone.CLEAR_ALL -> {
                        // Move to notifications zone if any exist
                        if (notificationsOnPageSize > 0) {
                            focusZone.value = SimpleTrayFocusZone.NOTIFICATIONS
                            selectedNotificationIndex.value = 0
                        } else if (bottomNavEnabled) {
                            focusZone.value = SimpleTrayFocusZone.BOTTOM_NAV
                            selectedBottomNavIndex.value = 0
                        }
                    }
                    SimpleTrayFocusZone.NOTIFICATIONS -> {
                        if (notificationsOnPageSize > 0) {
                            if (selectedNotificationIndex.value < notificationsOnPageSize - 1) {
                                selectedNotificationIndex.value = selectedNotificationIndex.value + 1
                            } else if (currentPage < totalPages - 1) {
                                // At last notification on page, go to next page
                                onNextPage()
                                selectedNotificationIndex.value = 0
                            } else if (bottomNavEnabled) {
                                focusZone.value = SimpleTrayFocusZone.BOTTOM_NAV
                                selectedBottomNavIndex.value = 0
                            }
                        }
                    }
                    SimpleTrayFocusZone.BOTTOM_NAV -> {
                        // Move to next bottom nav button
                        if (selectedBottomNavIndex.value < 1) {
                            selectedBottomNavIndex.value = selectedBottomNavIndex.value + 1
                        }
                        // Stay at boundary if already at last button
                    }
                }
                true
            }
            Key.DirectionUp -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    SimpleTrayFocusZone.QUICK_SETTINGS -> {
                        // Move to previous quick setting button
                        if (selectedQuickSettingIndex.value > 0) {
                            selectedQuickSettingIndex.value = selectedQuickSettingIndex.value - 1
                        }
                        // Stay at boundary if already at first button
                    }
                    SimpleTrayFocusZone.BRIGHTNESS_SLIDER -> {
                        // Move back to quick settings zone (last button)
                        focusZone.value = SimpleTrayFocusZone.QUICK_SETTINGS
                        selectedQuickSettingIndex.value = 4
                    }
                    SimpleTrayFocusZone.CLEAR_ALL -> {
                        // Move back to brightness zone
                        focusZone.value = SimpleTrayFocusZone.BRIGHTNESS_SLIDER
                    }
                    SimpleTrayFocusZone.NOTIFICATIONS -> {
                        if (notificationsOnPageSize > 0) {
                            if (selectedNotificationIndex.value > 0) {
                                selectedNotificationIndex.value = selectedNotificationIndex.value - 1
                            } else if (currentPage > 0) {
                                onPreviousPage()
                                selectedNotificationIndex.value = pageSize - 1 // Will be clamped by UI
                            } else {
                                focusZone.value = SimpleTrayFocusZone.CLEAR_ALL
                            }
                        }
                    }
                    SimpleTrayFocusZone.BOTTOM_NAV -> {
                        // Move to previous bottom nav button
                        if (selectedBottomNavIndex.value > 0) {
                            selectedBottomNavIndex.value = selectedBottomNavIndex.value - 1
                        } else {
                            if (notificationsOnPageSize > 0) {
                                focusZone.value = SimpleTrayFocusZone.NOTIFICATIONS
                                selectedNotificationIndex.value = notificationsOnPageSize - 1
                            } else {
                                focusZone.value = SimpleTrayFocusZone.CLEAR_ALL
                            }
                        }
                    }
                }
                true
            }
            Key.DirectionLeft -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    SimpleTrayFocusZone.QUICK_SETTINGS -> {
                        if (selectedQuickSettingIndex.value > 0) {
                            selectedQuickSettingIndex.value = selectedQuickSettingIndex.value - 1
                        }
                    }
                    SimpleTrayFocusZone.BRIGHTNESS_SLIDER -> {
                        // Decrease brightness
                        onBrightnessAdjust(-0.05f)
                    }
                    SimpleTrayFocusZone.CLEAR_ALL, SimpleTrayFocusZone.NOTIFICATIONS, SimpleTrayFocusZone.BOTTOM_NAV -> {
                        // No action for left in these zones
                    }
                }
                true
            }
            Key.DirectionRight -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    SimpleTrayFocusZone.QUICK_SETTINGS -> {
                        if (selectedQuickSettingIndex.value < 4) {
                            selectedQuickSettingIndex.value = selectedQuickSettingIndex.value + 1
                        }
                    }
                    SimpleTrayFocusZone.BRIGHTNESS_SLIDER -> {
                        // Increase brightness
                        onBrightnessAdjust(0.05f)
                    }
                    SimpleTrayFocusZone.CLEAR_ALL, SimpleTrayFocusZone.NOTIFICATIONS, SimpleTrayFocusZone.BOTTOM_NAV -> {
                        // No action for right in these zones
                    }
                }
                true
            }
            Key.PageUp -> {
                isDpadModeSetter(true)
                if (currentPage > 0) {
                    onPreviousPage()
                    selectedNotificationIndex.value = 0
                }
                true
            }
            Key.PageDown -> {
                isDpadModeSetter(true)
                if (currentPage < totalPages - 1) {
                    onNextPage()
                    selectedNotificationIndex.value = 0
                }
                true
            }
            else -> false
        }
    }
    fun handleHubKeyEvent(
        keyEvent: KeyEvent,
        keyPressTracker: KeyPressTracker,
        isDpadModeSetter: (Boolean) -> Unit,
        focusZone: MutableState<HubFocusZone>,
        // Category Tabs
        selectedCategoryIndex: MutableState<Int>,
        categoryTabCount: Int,
        onCategorySelected: (Int) -> Unit,
        // Clear All
        onClearAll: () -> Unit,
        // Notifications
        selectedNotificationIndex: MutableState<Int>,
        notificationsOnPageSize: Int,
        pageSize: Int,
        currentPage: Int,
        totalPages: Int,
        onNextPage: () -> Unit,
        onPreviousPage: () -> Unit,
        onNotificationClick: (index: Int) -> Unit,
        onNotificationLongClick: (index: Int) -> Unit,
        // Bottom Nav (kept for API compat, ignored)
        bottomNavEnabled: Boolean = false
    ): Boolean {
        // Handle activation keys (Enter, DPAD Center)
        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.repeatCount == 0) {
                isDpadModeSetter(true)
                if (focusZone.value == HubFocusZone.NOTIFICATIONS) {
                    keyPressTracker.onKeyDown(keyEvent.key)
                    return true
                } else {
                    when (focusZone.value) {
                        HubFocusZone.CATEGORY_TABS -> onCategorySelected(selectedCategoryIndex.value)
                        HubFocusZone.CLEAR_ALL -> onClearAll()
                        else -> {}
                    }
                    return true
                }
            }

            // Handle long-press for notifications zone
            if (focusZone.value == HubFocusZone.NOTIFICATIONS) {
                return when (keyEvent.type) {
                    KeyEventType.KeyDown -> {
                        isDpadModeSetter(true)
                        if (keyEvent.nativeKeyEvent.repeatCount > 0) {
                            if (!keyPressTracker.longPressHandled && keyPressTracker.getDuration(keyEvent.key) >= LONG_PRESS_THRESHOLD_MS) {
                                onNotificationLongClick(selectedNotificationIndex.value)
                                keyPressTracker.longPressHandled = true
                            }
                        }
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (keyPressTracker.longPressHandled) {
                            keyPressTracker.reset()
                        } else {
                            val duration = keyPressTracker.onKeyUp(keyEvent.key)
                            if (duration > 0) {
                                onNotificationClick(selectedNotificationIndex.value)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            return true
        }

        // For other keys, only handle on KeyDown
        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.DirectionDown -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    HubFocusZone.CATEGORY_TABS -> {
                        if (notificationsOnPageSize > 0) {
                            focusZone.value = HubFocusZone.CLEAR_ALL
                        }
                    }
                    HubFocusZone.CLEAR_ALL -> {
                        if (notificationsOnPageSize > 0) {
                            focusZone.value = HubFocusZone.NOTIFICATIONS
                            selectedNotificationIndex.value = 0
                        }
                    }
                    HubFocusZone.NOTIFICATIONS -> {
                        if (notificationsOnPageSize > 0) {
                            if (selectedNotificationIndex.value < notificationsOnPageSize - 1) {
                                selectedNotificationIndex.value = selectedNotificationIndex.value + 1
                            } else if (currentPage < totalPages - 1) {
                                onNextPage()
                                selectedNotificationIndex.value = 0
                            }
                            // At bottom of last page — stay put
                        }
                    }
                }
                true
            }
            Key.DirectionUp -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    HubFocusZone.CATEGORY_TABS -> {
                        // At top — do nothing
                    }
                    HubFocusZone.CLEAR_ALL -> {
                        focusZone.value = HubFocusZone.CATEGORY_TABS
                    }
                    HubFocusZone.NOTIFICATIONS -> {
                        if (notificationsOnPageSize > 0) {
                            if (selectedNotificationIndex.value > 0) {
                                selectedNotificationIndex.value = selectedNotificationIndex.value - 1
                            } else if (currentPage > 0) {
                                onPreviousPage()
                                selectedNotificationIndex.value = pageSize - 1
                            } else {
                                focusZone.value = HubFocusZone.CLEAR_ALL
                            }
                        }
                    }
                }
                true
            }
            Key.DirectionLeft -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    HubFocusZone.CATEGORY_TABS -> {
                        if (selectedCategoryIndex.value > 0) {
                            selectedCategoryIndex.value = selectedCategoryIndex.value - 1
                            onCategorySelected(selectedCategoryIndex.value)
                        }
                    }
                    else -> {}
                }
                true
            }
            Key.DirectionRight -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    HubFocusZone.CATEGORY_TABS -> {
                        if (selectedCategoryIndex.value < categoryTabCount - 1) {
                            selectedCategoryIndex.value = selectedCategoryIndex.value + 1
                            onCategorySelected(selectedCategoryIndex.value)
                        }
                    }
                    else -> {}
                }
                true
            }
            Key.PageUp -> {
                isDpadModeSetter(true)
                if (currentPage > 0) {
                    onPreviousPage()
                    selectedNotificationIndex.value = 0
                }
                true
            }
            Key.PageDown -> {
                isDpadModeSetter(true)
                if (currentPage < totalPages - 1) {
                    onNextPage()
                    selectedNotificationIndex.value = 0
                }
                true
            }
            else -> false
        }
    }
    fun handleColorEditorKeyEvent(
        keyEvent: KeyEvent,
        isDpadModeSetter: (Boolean) -> Unit,
        focusZone: MutableState<ColorEditorFocusZone>,
        headerIndex: MutableState<Int>,
        tabIndex: MutableState<Int>,
        colorRowIndex: MutableState<Int>,
        onBackClick: () -> Unit,
        onSave: () -> Unit,
        onTabSelect: (Int) -> Unit,
        onColorRowClick: (Int) -> Unit
    ): Boolean {
        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (keyEvent.type == KeyEventType.KeyDown) {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ColorEditorFocusZone.HEADER -> {
                        when (headerIndex.value) {
                            0 -> onBackClick()
                            1 -> onSave()
                        }
                    }
                    ColorEditorFocusZone.TABS -> {
                        onTabSelect(tabIndex.value)
                    }
                    ColorEditorFocusZone.COLOR_ROWS -> {
                        onColorRowClick(colorRowIndex.value)
                    }
                }
                return true
            }
            return true
        }

        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.DirectionLeft -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ColorEditorFocusZone.HEADER -> {
                        if (headerIndex.value > 0) headerIndex.value--
                    }
                    ColorEditorFocusZone.TABS -> {
                        if (tabIndex.value > 0) tabIndex.value--
                    }
                    ColorEditorFocusZone.COLOR_ROWS -> {
                        // no left/right in color rows
                    }
                }
                true
            }
            Key.DirectionRight -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ColorEditorFocusZone.HEADER -> {
                        if (headerIndex.value < 1) headerIndex.value++
                    }
                    ColorEditorFocusZone.TABS -> {
                        if (tabIndex.value < 1) tabIndex.value++
                    }
                    ColorEditorFocusZone.COLOR_ROWS -> {
                        // no left/right in color rows
                    }
                }
                true
            }
            Key.DirectionDown -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ColorEditorFocusZone.HEADER -> {
                        focusZone.value = ColorEditorFocusZone.TABS
                    }
                    ColorEditorFocusZone.TABS -> {
                        focusZone.value = ColorEditorFocusZone.COLOR_ROWS
                        colorRowIndex.value = 0
                    }
                    ColorEditorFocusZone.COLOR_ROWS -> {
                        if (colorRowIndex.value < 1) colorRowIndex.value++
                    }
                }
                true
            }
            Key.DirectionUp -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ColorEditorFocusZone.COLOR_ROWS -> {
                        if (colorRowIndex.value > 0) {
                            colorRowIndex.value--
                        } else {
                            focusZone.value = ColorEditorFocusZone.TABS
                        }
                    }
                    ColorEditorFocusZone.TABS -> {
                        focusZone.value = ColorEditorFocusZone.HEADER
                    }
                    ColorEditorFocusZone.HEADER -> { /* at top */ }
                }
                true
            }
            else -> false
        }
    }

    fun handleThemePresetKeyEvent(
        keyEvent: KeyEvent,
        isDpadModeSetter: (Boolean) -> Unit,
        focusZone: MutableState<ThemePresetFocusZone>,
        headerIndex: MutableState<Int>,
        bottomRowIndex: MutableState<Int>,
        currentIndex: MutableState<Int>,
        presetsLastIndex: Int,
        hasSelectButton: Boolean,
        headerMaxIndex: Int = 0,
        onBackClick: () -> Unit,
        onHeaderAction: (Int) -> Unit = {},
        onModeToggle: () -> Unit,
        onSelect: () -> Unit,
        skipRowIndex: MutableState<Int>? = null,
        skipRowCount: Int = 0,
        onSkipToggle: (Int) -> Unit = {}
    ): Boolean {
        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (keyEvent.type == KeyEventType.KeyDown) {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ThemePresetFocusZone.HEADER -> {
                        if (headerIndex.value == 0) onBackClick()
                        else onHeaderAction(headerIndex.value)
                    }
                    ThemePresetFocusZone.BOTTOM_ROW -> {
                        when (bottomRowIndex.value) {
                            0 -> onModeToggle()
                            1 -> onSelect()
                        }
                    }
                    ThemePresetFocusZone.CARD -> {
                        if (hasSelectButton) onSelect() else onModeToggle()
                    }
                    ThemePresetFocusZone.SKIP_ROW -> {
                        onSkipToggle(skipRowIndex?.value ?: 0)
                    }
                }
                return true
            }
            return true
        }

        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.DirectionLeft -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ThemePresetFocusZone.HEADER -> {
                        if (headerIndex.value > 0) headerIndex.value--
                    }
                    ThemePresetFocusZone.CARD -> {
                        if (currentIndex.value > 0) currentIndex.value--
                    }
                    ThemePresetFocusZone.BOTTOM_ROW -> {
                        if (bottomRowIndex.value > 0) bottomRowIndex.value--
                    }
                    ThemePresetFocusZone.SKIP_ROW -> {
                        if ((skipRowIndex?.value ?: 0) > 0) skipRowIndex?.let { it.value-- }
                    }
                }
                true
            }
            Key.DirectionRight -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ThemePresetFocusZone.HEADER -> {
                        if (headerIndex.value < headerMaxIndex) headerIndex.value++
                    }
                    ThemePresetFocusZone.CARD -> {
                        if (currentIndex.value < presetsLastIndex) currentIndex.value++
                    }
                    ThemePresetFocusZone.BOTTOM_ROW -> {
                        val maxIndex = if (hasSelectButton) 1 else 0
                        if (bottomRowIndex.value < maxIndex) bottomRowIndex.value++
                    }
                    ThemePresetFocusZone.SKIP_ROW -> {
                        if ((skipRowIndex?.value ?: 0) < skipRowCount - 1) skipRowIndex?.let { it.value++ }
                    }
                }
                true
            }
            Key.DirectionDown -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ThemePresetFocusZone.HEADER -> {
                        focusZone.value = ThemePresetFocusZone.CARD
                    }
                    ThemePresetFocusZone.CARD -> {
                        focusZone.value = if (skipRowCount > 0) ThemePresetFocusZone.SKIP_ROW else ThemePresetFocusZone.BOTTOM_ROW
                    }
                    ThemePresetFocusZone.SKIP_ROW -> {
                        focusZone.value = ThemePresetFocusZone.BOTTOM_ROW
                    }
                    ThemePresetFocusZone.BOTTOM_ROW -> { /* at bottom */ }
                }
                true
            }
            Key.DirectionUp -> {
                isDpadModeSetter(true)
                when (focusZone.value) {
                    ThemePresetFocusZone.BOTTOM_ROW -> {
                        focusZone.value = if (skipRowCount > 0) ThemePresetFocusZone.SKIP_ROW else ThemePresetFocusZone.CARD
                    }
                    ThemePresetFocusZone.SKIP_ROW -> {
                        focusZone.value = ThemePresetFocusZone.CARD
                    }
                    ThemePresetFocusZone.CARD -> {
                        focusZone.value = ThemePresetFocusZone.HEADER
                    }
                    ThemePresetFocusZone.HEADER -> { /* at top */ }
                }
                true
            }
            else -> false
        }
    }
}
