package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.github.gezimos.inkos.data.Prefs

object VibrationHelper {
    enum class Effect { CLICK, PAGE, SELECT, SAVE, SOFT, DEFAULT, LONG_PRESS, AZ_FILTER }

    private var vibrator: Vibrator? = null
    private var prefs: Prefs? = null
    private val lastTrigger = mutableMapOf<Effect, Long>()
    private var enabled: Boolean = true

    fun init(context: Context, p: Prefs) {
        prefs = p
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun throttleMs(effect: Effect): Long = when (effect) {
        Effect.PAGE -> 120L
        Effect.CLICK -> 40L
        Effect.SELECT -> 60L
        Effect.SOFT -> 40L
        Effect.SAVE -> 80L
        Effect.LONG_PRESS -> 300L
        Effect.AZ_FILTER -> 50L
        else -> 50L
    }

    private fun shouldAllowEffect(effect: Effect): Boolean {
        val p = prefs ?: return false
        // Respect global haptic toggle first
        if (!p.hapticFeedback) return false
        // Paging now follows the global haptic setting; no separate paging pref.
        return true
    }

    fun trigger(effect: Effect = Effect.DEFAULT) {
        val p = prefs ?: return
        if (!enabled) return
        if (!shouldAllowEffect(effect)) return

        val now = System.currentTimeMillis()
        val last = lastTrigger[effect] ?: 0L
        val wait = throttleMs(effect)
        if (now - last < wait) return
        lastTrigger[effect] = now

        try {
            val ve = when (effect) {
                Effect.CLICK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                                else VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                Effect.PAGE -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                Effect.SOFT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) VibrationEffect.createOneShot(8, 20) else VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE)
                Effect.AZ_FILTER -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    } catch (_: Exception) {
                        // Fallback: stronger than SOFT but not as strong as LONG_PRESS
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            VibrationEffect.createOneShot(45, 180)
                        } else {
                            VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Stronger vibration for AZ filtering: 45ms duration, 180 amplitude (more noticeable than SOFT)
                    VibrationEffect.createOneShot(45, 180)
                } else {
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                Effect.LONG_PRESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    } catch (_: Exception) {
                        VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect.createOneShot(70, 255)
                } else {
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                else -> VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator?.vibrate(ve)
        } catch (_: Exception) {
        }
    }

    fun release() {
        vibrator = null
        prefs = null
        lastTrigger.clear()
    }

    fun setEnabled(on: Boolean) {
        enabled = on
    }

    fun isEnabled(): Boolean = enabled
}
