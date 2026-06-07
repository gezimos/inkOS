package com.github.gezimos.inkos.helper.utils

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppReloader {
    @OptIn(DelicateCoroutinesApi::class)
    fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)

        GlobalScope.launch(Dispatchers.Main) {
            delay(500) // Suspend function, avoids `Handler`
            context.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }
}

