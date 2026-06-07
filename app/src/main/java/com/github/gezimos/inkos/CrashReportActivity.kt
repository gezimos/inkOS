package com.github.gezimos.inkos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.inkos.style.SettingsTheme
import com.github.gezimos.inkos.style.Theme
import com.github.gezimos.inkos.ui.dialogs.SheetTitle
import com.github.gezimos.inkos.helper.utils.SimpleEmailSender
import com.github.gezimos.inkos.ui.dialogs.ComposeBottomSheetHost

class CrashReportActivity : AppCompatActivity() {
    private var pkgName: String = ""
    private var pkgVersion: String = ""
    private var crashSheet: ComposeBottomSheetHost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        })

        pkgName = getString(R.string.app_name)
        pkgVersion = this.packageManager.getPackageInfo(this.packageName, 0).versionName.toString()

        crashSheet = ComposeBottomSheetHost(this)
        crashSheet?.show {
            val prefs = com.github.gezimos.inkos.data.Prefs(this@CrashReportActivity)
            val isDark = prefs.appTheme == com.github.gezimos.inkos.data.Constants.Theme.Dark
            SettingsTheme(isDark) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SheetTitle(getString(R.string.acra_crash))
                    Text(
                        text = getString(R.string.acra_dialog_text).format(pkgName),
                        style = SettingsTheme.typography.item,
                        color = Theme.colors.text,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { restartApp(); crashSheet?.dismiss() }) {
                            Text(getString(R.string.acra_dont_send), color = Theme.colors.text)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { sendCrashReport(this@CrashReportActivity); crashSheet?.dismiss() }) {
                            Text(getString(R.string.acra_send_report), color = Theme.colors.text)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        crashSheet?.dismiss()
        super.onDestroy()
    }

    private fun sendCrashReport(context: Context) {
        val crashFileUri: Uri? = CrashHandler.customReportSender(applicationContext)
        val crashFileUris: List<Uri> = crashFileUri?.let { listOf(it) } ?: emptyList()

        val emailSender = SimpleEmailSender()
        val crashReportContent = getString(R.string.acra_mail_body)
        val subject = String.format("Crash Report %s - %s", pkgName, pkgVersion)
        val recipient = getString(R.string.acra_email)

        emailSender.sendCrashReport(context, crashReportContent, crashFileUris, subject, recipient)
    }

    private fun restartApp() {
        try {
            getSharedPreferences(CrashHandler.CRASH_LOOP_PREFS, MODE_PRIVATE)
                .edit().clear().apply()
        } catch (_: Exception) {}

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }
}
