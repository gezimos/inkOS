package com.github.gezimos.inkos

import com.github.gezimos.inkos.ui.dialogs.LockedBottomSheetDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.gezimos.common.CrashHandler
import com.github.gezimos.inkos.helper.utils.SimpleEmailSender

class CrashReportActivity : AppCompatActivity() {
    private var pkgName: String = ""
    private var pkgVersion: String = ""
    private var crashDialog: LockedBottomSheetDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pkgName = getString(R.string.app_name)
        pkgVersion = this.packageManager.getPackageInfo(
            this.packageName,
            0
        ).versionName.toString()

        // Show a bottom-sheet dialog to ask if the user wants to report the crash
        val content = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val pad = (12 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)

            val titleView = android.widget.TextView(context).apply {
                text = getString(R.string.acra_crash)
                gravity = android.view.Gravity.CENTER
                textSize = 18f
            }
            addView(titleView)

            val msg = android.widget.TextView(context).apply {
                text = getString(R.string.acra_dialog_text).format(pkgName)
                gravity = android.view.Gravity.CENTER
                textSize = 14f
                val mPad = (8 * resources.displayMetrics.density).toInt()
                setPadding(mPad, mPad, mPad, mPad)
            }
            addView(msg)

            val btnRow = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.END
                val params = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (12 * resources.displayMetrics.density).toInt()
                }
                layoutParams = params

                val btnPadding = (8 * resources.displayMetrics.density).toInt()

                val dontSend = android.widget.Button(context).apply {
                    text = getString(R.string.acra_dont_send)
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                }

                val send = android.widget.Button(context).apply {
                    text = getString(R.string.acra_send_report)
                    setPadding(btnPadding, btnPadding, btnPadding, btnPadding)
                    minWidth = 0
                    minimumWidth = 0
                    minHeight = 0
                    minimumHeight = 0
                }

                // Style buttons using Prefs if available (best-effort)
                try {
                    val p = com.github.gezimos.inkos.data.Prefs(this@CrashReportActivity)
                    val density = resources.displayMetrics.density
                    val radius = (6 * density)
                    val strokeWidth = (3f * density).toInt()
                    val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = radius
                        setColor(p.backgroundColor)
                        setStroke(strokeWidth, p.appColor)
                    }
                    dontSend.background = bgDrawable
                    send.background = bgDrawable.constantState?.newDrawable()?.mutate()
                    dontSend.setTextColor(p.appColor)
                    send.setTextColor(p.appColor)
                } catch (_: Exception) {}

                val spacing = (8 * resources.displayMetrics.density).toInt()
                dontSend.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = spacing }
                send.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = spacing }

                addView(dontSend)
                addView(send)

                dontSend.setOnClickListener {
                    restartApp()
                    crashDialog?.dismiss()
                }
                send.setOnClickListener {
                    sendCrashReport(this@CrashReportActivity)
                    crashDialog?.dismiss()
                }
            }

            addView(btnRow)
        }

        val dialog = LockedBottomSheetDialog(this)
        dialog.setContentView(content)
        try {
            val p = com.github.gezimos.inkos.data.Prefs(this)
            content.setBackgroundColor(p.backgroundColor)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        } catch (_: Exception) {}
        dialog.setLocked(true)
        dialog.show()
        crashDialog = dialog
    }

    override fun onDestroy() {
        crashDialog?.dismiss()
        super.onDestroy()
    }

    private fun sendCrashReport(context: Context) {
        val crashFileUri: Uri? = CrashHandler.customReportSender(applicationContext)
        val crashFileUris: List<Uri> = crashFileUri?.let { listOf(it) } ?: emptyList()

        val emailSender = SimpleEmailSender() // Create an instance
        val crashReportContent = getString(R.string.acra_mail_body)
        val subject = String.format("Crash Report %s - %s", pkgName, pkgVersion)
        val recipient = getString(R.string.acra_email) // Replace with your email

        emailSender.sendCrashReport(context, crashReportContent, crashFileUris, subject, recipient)
    }


    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }
}
