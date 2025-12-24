package com.github.gezimos.inkos.helper

import android.view.KeyEvent
import com.github.gezimos.inkos.data.Constants.Action
import com.github.gezimos.inkos.data.Prefs

/**
 * Maps hardware key events to semantic actions for Home/Apps screens.
 * Actual gesture execution is handled by GestureHelper.
 */
object KeyMapperHelper {
    sealed class HomeKeyAction {
        object None : HomeKeyAction()
        object MoveSelectionUp : HomeKeyAction()
        object MoveSelectionDown : HomeKeyAction()
        object PageUp : HomeKeyAction()
        object PageDown : HomeKeyAction()
        object SwipeLeft : HomeKeyAction()
        object SwipeRight : HomeKeyAction()
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
            // Volume keys for page navigation (if enabled)
            KeyEvent.KEYCODE_VOLUME_UP -> if (prefs.useVolumeKeysForPages) HomeKeyAction.PageUp else HomeKeyAction.None
            KeyEvent.KEYCODE_VOLUME_DOWN -> if (prefs.useVolumeKeysForPages) HomeKeyAction.PageDown else HomeKeyAction.None

            // GestureKeyCodes (clock/date/quote/doubletap) - only map if the user configured an action
            GestureKeyCodes.CLOCK -> if (prefs.clickClockAction != Action.Disabled) HomeKeyAction.ClickClock else HomeKeyAction.None
            GestureKeyCodes.DATE -> if (prefs.clickDateAction != Action.Disabled) HomeKeyAction.ClickDate else HomeKeyAction.None
            GestureKeyCodes.QUOTE -> if (prefs.quoteAction != Action.Disabled) HomeKeyAction.ClickQuote else HomeKeyAction.None
            GestureKeyCodes.DOUBLETAP -> if (prefs.doubleTapAction != Action.Disabled) HomeKeyAction.DoubleTap else HomeKeyAction.None

            // DPAD left/right map to swipe gestures for Home (respect user prefs)
            KeyEvent.KEYCODE_DPAD_LEFT -> if (prefs.swipeRightAction != Action.Disabled) HomeKeyAction.SwipeRight else HomeKeyAction.None
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (prefs.swipeLeftAction != Action.Disabled) HomeKeyAction.SwipeLeft else HomeKeyAction.None

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
}
