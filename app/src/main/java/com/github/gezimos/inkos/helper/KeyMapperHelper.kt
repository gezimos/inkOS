package com.github.gezimos.inkos.helper

import android.view.KeyEvent
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Prefs

object KeyMapperHelper {
    sealed class HomeKeyAction {
        object None : HomeKeyAction()
        object MoveSelectionUp : HomeKeyAction()
        object MoveSelectionDown : HomeKeyAction()
        object PageUp : HomeKeyAction()
        object PageDown : HomeKeyAction()
        data class GestureLeft(val action: Action) : HomeKeyAction()
        data class GestureRight(val action: Action) : HomeKeyAction()
        object LongPressSelected : HomeKeyAction()
        object OpenSettings : HomeKeyAction()
        object ClickClock : HomeKeyAction()
        object ClickDate : HomeKeyAction()
        object ClickQuote : HomeKeyAction()
        object DoubleTap : HomeKeyAction()
    }
    object GestureKeyCodes {
        const val CLOCK = KeyEvent.KEYCODE_6
        const val DATE = KeyEvent.KEYCODE_7
        const val QUOTE = KeyEvent.KEYCODE_8
        const val DOUBLETAP = KeyEvent.KEYCODE_2
    }

    fun mapHomeKey(prefs: Prefs, keyCode: Int, event: KeyEvent): HomeKeyAction {
        if (event.action != KeyEvent.ACTION_DOWN) return HomeKeyAction.None

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> HomeKeyAction.MoveSelectionUp
            KeyEvent.KEYCODE_DPAD_DOWN -> HomeKeyAction.MoveSelectionDown
            
            // Volume keys for page navigation (if enabled)
            KeyEvent.KEYCODE_VOLUME_UP -> if (prefs.useVolumeKeysForPages) HomeKeyAction.PageUp else HomeKeyAction.None
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (prefs.useVolumeKeysForPages) HomeKeyAction.PageDown else HomeKeyAction.None
            
            // GestureKeyCodes (clock/date/quote/doubletap) - only map if the user configured an action
            GestureKeyCodes.CLOCK -> if (prefs.clickClockAction != Action.Disabled) HomeKeyAction.ClickClock else HomeKeyAction.None
            GestureKeyCodes.DATE -> if (prefs.clickDateAction != Action.Disabled) HomeKeyAction.ClickDate else HomeKeyAction.None
            GestureKeyCodes.QUOTE -> if (prefs.quoteAction != Action.Disabled) HomeKeyAction.ClickQuote else HomeKeyAction.None
            GestureKeyCodes.DOUBLETAP -> if (prefs.doubleTapAction != Action.Disabled) HomeKeyAction.DoubleTap else HomeKeyAction.None
            // If the user has disabled these gestures (Action.Disabled), fall back to default key behavior.
            KeyEvent.KEYCODE_DPAD_LEFT -> if (prefs.swipeRightAction != Action.Disabled) HomeKeyAction.GestureRight(prefs.swipeRightAction) else HomeKeyAction.None
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (prefs.swipeLeftAction != Action.Disabled) HomeKeyAction.GestureLeft(prefs.swipeLeftAction) else HomeKeyAction.None
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (event.isLongPress) HomeKeyAction.LongPressSelected else HomeKeyAction.None
            }

            KeyEvent.KEYCODE_9 -> {
                if (event.isLongPress) HomeKeyAction.OpenSettings else HomeKeyAction.None
            }

            else -> HomeKeyAction.None
        }
    }

    /**
     * Same mapping semantics for an individual app button (per-button listener).
     */
    fun mapAppButtonKey(prefs: Prefs, keyCode: Int, event: KeyEvent): HomeKeyAction {
        return mapHomeKey(prefs, keyCode, event)
    }

    // --- Notifications-specific key mapping ---
    sealed class NotificationKeyAction {
        object None : NotificationKeyAction()
        object PageUp : NotificationKeyAction()
        object PageDown : NotificationKeyAction()
        object Dismiss : NotificationKeyAction()
        object Open : NotificationKeyAction()
    }

    // --- AppDrawer-specific key mapping ---
    sealed class AppDrawerKeyAction {
        object None : AppDrawerKeyAction()
        object PageUp : AppDrawerKeyAction()
        object PageDown : AppDrawerKeyAction()
        object MoveSelectionUp : AppDrawerKeyAction()
        object MoveSelectionDown : AppDrawerKeyAction()
        object SelectItem : AppDrawerKeyAction()
        object LongPressItem : AppDrawerKeyAction()
    }

    fun mapNotificationsKey(prefs: Prefs, keyCode: Int, event: KeyEvent): NotificationKeyAction {
        if (event.action != KeyEvent.ACTION_DOWN) return NotificationKeyAction.None

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> NotificationKeyAction.PageUp
            KeyEvent.KEYCODE_DPAD_DOWN -> NotificationKeyAction.PageDown
            KeyEvent.KEYCODE_PAGE_UP -> NotificationKeyAction.PageUp
            KeyEvent.KEYCODE_PAGE_DOWN -> NotificationKeyAction.PageDown
            
            // Volume keys for page navigation (if enabled)
            KeyEvent.KEYCODE_VOLUME_UP -> if (prefs.useVolumeKeysForPages) NotificationKeyAction.PageUp else NotificationKeyAction.None
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (prefs.useVolumeKeysForPages) NotificationKeyAction.PageDown else NotificationKeyAction.None
            
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_1 -> NotificationKeyAction.Dismiss
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_3 -> NotificationKeyAction.Open
            else -> NotificationKeyAction.None
        }
    }

    fun mapAppDrawerKey(prefs: Prefs, keyCode: Int, event: KeyEvent): AppDrawerKeyAction {
        if (event.action != KeyEvent.ACTION_DOWN) return AppDrawerKeyAction.None

        return when (keyCode) {
            // Standard navigation keys for item-by-item movement
            KeyEvent.KEYCODE_DPAD_UP -> AppDrawerKeyAction.MoveSelectionUp
            KeyEvent.KEYCODE_DPAD_DOWN -> AppDrawerKeyAction.MoveSelectionDown
            
            // Page navigation keys
            KeyEvent.KEYCODE_PAGE_UP -> AppDrawerKeyAction.PageUp
            KeyEvent.KEYCODE_PAGE_DOWN -> AppDrawerKeyAction.PageDown
            
            // Selection keys
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (event.isLongPress) AppDrawerKeyAction.LongPressItem else AppDrawerKeyAction.SelectItem
            }
            
            // Volume keys for page navigation (if enabled)
            KeyEvent.KEYCODE_VOLUME_UP -> if (prefs.useVolumeKeysForPages) AppDrawerKeyAction.PageUp else AppDrawerKeyAction.None
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (prefs.useVolumeKeysForPages) AppDrawerKeyAction.PageDown else AppDrawerKeyAction.None
            
            else -> AppDrawerKeyAction.None
        }
    }
}
