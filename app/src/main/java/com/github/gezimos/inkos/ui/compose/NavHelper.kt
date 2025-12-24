package com.github.gezimos.inkos.ui.compose

import androidx.compose.runtime.MutableState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Tracks key press duration to detect long-press gestures.
 */
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
            // Return duration, or 1 if duration is 0 but we were tracking this key
            // This handles edge cases where KeyUp comes immediately after KeyDown
            return if (duration > 0) duration else if (wasTracking) 1L else 0L
        }
        // Even if key doesn't match, reset to avoid stuck state
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

/**
 * Focus zones for Home screen navigation
 */
enum class FocusZone {
    CLOCK, DATE, APPS, MEDIA_WIDGET, QUOTE
}

/**
 * Focus zones for SimpleTray navigation
 */
enum class SimpleTrayFocusZone {
    QUICK_SETTINGS,
    BRIGHTNESS_SLIDER,
    CLEAR_ALL,
    NOTIFICATIONS,
    BOTTOM_NAV
}

object NavHelper {
    // Increase DPAD long-press threshold to match touch long-press behaviour
    const val LONG_PRESS_THRESHOLD_MS = 700L
    /**
     * Handle DPAD/key events for Home screen with multi-zone navigation.
     * Returns true if the event was handled.
     */
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
        onQuoteClick: (() -> Unit)? = null
    ): Boolean {
        // Handle activation keys (Enter, DPAD Center) with zone-aware activation
        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.repeatCount == 0) {
                dpadMode.value = true
                // For zones other than APPS, activate immediately on KeyDown
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
        
        // For other keys, only handle on KeyDown and only treat navigation/selection
        // keys as DPAD input. This avoids turning on DPAD mode for Back/Swipe inputs.
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
                                    // At last app on last page - try to move to media or quote
                                    if (showMediaWidget) {
                                        focusZone.value = FocusZone.MEDIA_WIDGET
                                        selectedMediaButton?.value = 0
                                    } else if (showQuote) {
                                        focusZone.value = FocusZone.QUOTE
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
                                }
                            } else if (showQuote) {
                                focusZone.value = FocusZone.QUOTE
                            }
                        }
                        FocusZone.QUOTE -> {
                            // Stay at quote (boundary)
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
                            // Stay at clock (boundary)
                        }
                        FocusZone.DATE -> {
                            if (showClock) focusZone.value = FocusZone.CLOCK
                        }
                        FocusZone.APPS -> {
                            if (appsOnPageSize > 0) {
                                if (selectedIndex.value > 0) {
                                    selectedIndex.value = selectedIndex.value - 1
                                } else if (currentPage > 0) {
                                    adjustPageBy(-1)
                                    selectedIndex.value = appsPerPage - 1
                                } else {
                                    // At first app on first page - try to move to date or clock
                                    if (showDate) {
                                        focusZone.value = FocusZone.DATE
                                    } else if (showClock) {
                                        focusZone.value = FocusZone.CLOCK
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
                // Left/Right no longer navigate media buttons - they trigger gestures
                if (!disableSwipeGestures) {
                    try { onSwipeLeft?.invoke() } catch (_: Exception) {}
                }
                true
            }
            Key.DirectionRight -> {
                dpadMode.value = true
                // Left/Right no longer navigate media buttons - they trigger gestures
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
            // Standard navigation keys for item-by-item movement
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

    /**
     * Handle DPAD/key events for Apps drawer with A-Z filter and search support.
     */
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
        onSearchFocus: (() -> Unit)? = null
    ): Boolean {
        // Handle activation keys (Enter, DPAD Center) with long-press detection
        // When A-Z filter is focused, ENTER activates the selected filter letter
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
                        // Return true to consume the event and prevent default action
                        true
                    }
                    KeyEventType.KeyUp -> {
                        if (keyPressTracker.longPressHandled) {
                            keyPressTracker.reset()
                        } else {
                            val duration = keyPressTracker.onKeyUp(keyEvent.key)
                            if (duration > 0) {
                                onAppClick(selectedIndexGetter())
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        
        // For other keys, only handle on KeyDown and only treat navigation/selection
        // keys as DPAD input to avoid enabling DPAD mode for Back/Swipe keys.
        if (keyEvent.type != KeyEventType.KeyDown) return false

        return when (keyEvent.key) {
            Key.DirectionUp -> {
                isDpadModeSetter(true)
                
                if (azFilterFocused) {
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
                    }
                }
                true
            }
            Key.DirectionDown -> {
                isDpadModeSetter(true)
                
                if (azFilterFocused) {
                    // Navigate down in A-Z filter
                    val newIndex = (azFilterSelectedIndex + 1).coerceAtMost(azFilterSize - 1)
                    onAzFilterIndexChange?.invoke(newIndex)
                } else {
                    // If in search field, move back to app list
                    // This will be handled by the fragment when search field loses focus
                    
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
                // Move from A-Z filter back to app list
                if (azFilterFocused) {
                    onAzFilterFocusChange?.invoke(false)
                }
                true
            }
            Key.DirectionRight -> {
                isDpadModeSetter(true)
                // Move from app list to A-Z filter (if visible and not already focused)
                if (!azFilterFocused && showAzFilter) {
                    onAzFilterFocusChange?.invoke(true)
                }
                true
            }
            Key.PageUp -> {
                isDpadModeSetter(true)
                onPreviousPage()
                selectedIndexSetter(0)
                true
            }
            Key.PageDown -> {
                isDpadModeSetter(true)
                onNextPage()
                selectedIndexSetter(0)
                true
            }
            Key.Backspace, Key.Delete -> {
                // Let caller handle search focus/backspace behavior; return false to let existing logic act.
                false
            }
            else -> false
        }
    }

    /**
     * Handle DPAD/key events for SimpleTray with multi-zone navigation.
     * Returns true if the event was handled.
     */
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
                            // Skip to bottom nav if no notifications and bottom nav is enabled
                            focusZone.value = SimpleTrayFocusZone.BOTTOM_NAV
                            selectedBottomNavIndex.value = 0
                        }
                        // If no notifications and bottom nav disabled, stay in CLEAR_ALL
                    }
                    SimpleTrayFocusZone.NOTIFICATIONS -> {
                        // Move to next notification, change page at boundary
                        if (notificationsOnPageSize > 0) {
                            if (selectedNotificationIndex.value < notificationsOnPageSize - 1) {
                                selectedNotificationIndex.value = selectedNotificationIndex.value + 1
                            } else if (currentPage < totalPages - 1) {
                                // At last notification on page, go to next page
                                onNextPage()
                                selectedNotificationIndex.value = 0
                            } else if (bottomNavEnabled) {
                                // At last notification on last page, move to bottom nav if enabled
                                focusZone.value = SimpleTrayFocusZone.BOTTOM_NAV
                                selectedBottomNavIndex.value = 0
                            }
                            // If bottom nav disabled, stay at last notification
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
                        // Move to previous notification, change page at boundary
                        if (notificationsOnPageSize > 0) {
                            if (selectedNotificationIndex.value > 0) {
                                selectedNotificationIndex.value = selectedNotificationIndex.value - 1
                            } else if (currentPage > 0) {
                                // At first notification on page, go to previous page
                                onPreviousPage()
                                selectedNotificationIndex.value = pageSize - 1 // Will be clamped by UI
                            } else {
                                // At first notification on first page, move to clear all
                                focusZone.value = SimpleTrayFocusZone.CLEAR_ALL
                            }
                        }
                    }
                    SimpleTrayFocusZone.BOTTOM_NAV -> {
                        // Move to previous bottom nav button
                        if (selectedBottomNavIndex.value > 0) {
                            selectedBottomNavIndex.value = selectedBottomNavIndex.value - 1
                        } else {
                            // At first button, move back to notifications or clear all
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
                        // Navigate within quick settings (5 buttons) - left
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
                        // Navigate within quick settings (5 buttons) - right
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
}
