package com.github.gezimos.inkos.helper.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.ImageViewCompat
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatteryReceiver(private val onBatteryUpdate: (String, String, Boolean) -> Unit = { _, _, _ -> }) : BroadcastReceiver() {

    private lateinit var prefs: Prefs

    override fun onReceive(context: Context, intent: Intent) {
        prefs = Prefs(context)
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val batteryLevel = level * 100 / scale.toFloat()
        val batteryLevelInt = batteryLevel.toInt()

        // Compute date text (without battery)
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val newDateText = if (prefs.showDate) {
            currentDate
        } else {
            ""
        }

        // Compute battery text (independent of date)
        val newBatteryText = if (prefs.showDateBatteryCombo) {
            "$batteryLevelInt%"
        } else {
            ""
        }

        // Call the callback with date text, battery text, and charging status
        onBatteryUpdate(newDateText, newBatteryText, isCharging)
    }
}

