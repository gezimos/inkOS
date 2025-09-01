package com.github.gezimos.inkos.helper.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.appcompat.widget.AppCompatTextView
import com.github.gezimos.inkos.R
import com.github.gezimos.inkos.data.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BatteryReceiver : BroadcastReceiver() {

    private lateinit var prefs: Prefs

    override fun onReceive(context: Context, intent: Intent) {
        prefs = Prefs(context)
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

    val contextBattery = context as? Activity
    // batteryTextView removed
        val dateTextView = (contextBattery)?.findViewById<AppCompatTextView>(R.id.date)

        val batteryLevel = level * 100 / scale.toFloat()
        val batteryLevelInt = batteryLevel.toInt()

    // Update bottom battery widget removed

        // Update date+battery combo if enabled
        if (prefs.showDate && prefs.showDateBatteryCombo) {
            val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            dateTextView?.text = buildString {
                append(currentDate)
                append("  ·  ")
                append(batteryLevelInt)
                append("%")
            }
        } else if (prefs.showDate) {
            // Show just date if combo is disabled
            val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
            dateTextView?.text = dateFormat.format(Date())
        }
    }
}

