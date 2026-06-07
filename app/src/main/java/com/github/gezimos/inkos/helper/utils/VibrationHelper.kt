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
        return true
    }

    fun trigger(effect: Effect = Effect.DEFAULT) {
        val p = prefs ?: return
        if (!enabled) return
        if (!shouldAllowEffect(effect)) return

        val scale = (p.vibrationScale.coerceIn(0, 500)) / 100f
        if (scale <= 0f) return

        val now = System.currentTimeMillis()
        val last = lastTrigger[effect] ?: 0L
        val wait = throttleMs(effect)
        if (now - last < wait) return
        lastTrigger[effect] = now

        fun scaledAmp(base: Int): Int = (base * scale).toInt().coerceIn(1, 255)
        fun scaledDur(base: Long): Long = (base * scale).toLong().coerceAtLeast(1L)
        val isDefaultScale = scale == 1.0f

        try {
            val ve = when (effect) {
                Effect.CLICK -> if (isDefaultScale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                } else {
                    VibrationEffect.createOneShot(scaledDur(30), scaledAmp(180))
                }
                Effect.PAGE -> VibrationEffect.createOneShot(scaledDur(30), scaledAmp(180))
                Effect.SOFT -> VibrationEffect.createOneShot(scaledDur(8), scaledAmp(40))
                Effect.AZ_FILTER -> if (isDefaultScale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    } catch (_: Exception) {
                        VibrationEffect.createOneShot(scaledDur(45), scaledAmp(180))
                    }
                } else {
                    VibrationEffect.createOneShot(scaledDur(45), scaledAmp(180))
                }
                Effect.LONG_PRESS -> if (isDefaultScale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    } catch (_: Exception) {
                        VibrationEffect.createOneShot(scaledDur(70), scaledAmp(200))
                    }
                } else {
                    VibrationEffect.createOneShot(scaledDur(70), scaledAmp(200))
                }
                else -> VibrationEffect.createOneShot(scaledDur(30), scaledAmp(180))
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
