package com.github.gezimos.inkos.helper.utils

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri

class SimpleEmailSender {

    fun sendCrashReport(
        context: Context,
        crashReportContent: String,
        crashReportAttachments: List<Uri>,
        subject: String,
        emailRecipient: String
    ) {
        sendWithSelector(
            subject,
            crashReportContent,
            crashReportAttachments,
            context,
            emailRecipient
        )
    }

    private fun resolveAndSend(
        subject: String,
        body: String,
        attachments: List<Uri>,
        context: Context,
        emailRecipient: String
    ) {
        val pm = context.packageManager
        val resolveIntent = buildResolveIntent()
        val resolveActivity = resolveIntent.resolveActivity(pm)

        if (resolveActivity != null) {
            if (attachments.isEmpty()) {
                // No attachments, send directly
                context.startActivity(buildFallbackIntent(subject, body, emailRecipient))
            } else {
                val attachmentIntent =
                    buildAttachmentIntent(subject, body, attachments, emailRecipient)
                val altAttachmentIntent = Intent(attachmentIntent).apply {
                    type = "*/*"
                } // To match Gmail's attachment expectations
                val initialIntents = buildInitialIntents(pm, resolveIntent, attachmentIntent)
                val packageName = getPackageName(resolveActivity, initialIntents)

                attachmentIntent.setPackage(packageName)
                altAttachmentIntent.setPackage(packageName)

                when {
                    packageName == null -> {
                        // Let user choose email client
                        showChooser(context, initialIntents)
                    }

                    attachmentIntent.resolveActivity(pm) != null -> {
                        // Use default email client
                        context.startActivity(attachmentIntent)
                    }

                    altAttachmentIntent.resolveActivity(pm) != null -> {
                        context.startActivity(altAttachmentIntent)
                    }

                    else -> {
                        // No email client found, fallback
                        context.startActivity(buildFallbackIntent(subject, body, emailRecipient))
                    }
                }
            }
        } else {
            throw ActivityNotFoundException("No email client found")
        }
    }

    private fun sendWithSelector(
        subject: String,
        body: String,
        attachments: List<Uri>,
        context: Context,
        emailRecipient: String
    ) {
        val intent = if (attachments.size == 1) {
            buildSingleAttachmentIntent(subject, body, attachments.first(), emailRecipient)
        } else {
            buildAttachmentIntent(subject, body, attachments, emailRecipient)
        }
        intent.selector = buildResolveIntent()
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            resolveAndSend(subject, body, attachments, context, emailRecipient)
        }
    }

    private fun buildAttachmentIntent(
        subject: String,
        body: String,
        attachments: List<Uri>,
        emailRecipient: String
    ): Intent {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailRecipient))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments))
        intent.putExtra(Intent.EXTRA_TEXT, body)
        return intent
    }

    private fun buildSingleAttachmentIntent(
        subject: String,
        body: String,
        attachment: Uri,
        emailRecipient: String
    ): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailRecipient))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_STREAM, attachment)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        return intent
    }

    private fun buildResolveIntent(): Intent {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = "mailto:".toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun buildFallbackIntent(subject: String, body: String, emailRecipient: String): Intent {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data =
            "mailto:${emailRecipient}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}".toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        return intent
    }

    private fun buildInitialIntents(
        pm: PackageManager,
        resolveIntent: Intent,
        emailIntent: Intent
    ): List<Intent> {
        val resolveInfoList =
            pm.queryIntentActivities(resolveIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val initialIntents = mutableListOf<Intent>()
        for (info in resolveInfoList) {
            val packageSpecificIntent = Intent(emailIntent)
            packageSpecificIntent.setPackage(info.activityInfo.packageName)
            if (packageSpecificIntent.resolveActivity(pm) != null) {
                initialIntents.add(packageSpecificIntent)
                continue
            }
            packageSpecificIntent.setClassName(
                info.activityInfo.packageName,
                info.activityInfo.name
            )
            if (packageSpecificIntent.resolveActivity(pm) != null) {
                initialIntents.add(packageSpecificIntent)
            }
        }
        return initialIntents
    }

    private fun showChooser(context: Context, initialIntents: List<Intent>) {
        val chooser = Intent(Intent.ACTION_CHOOSER)
        chooser.putExtra(Intent.EXTRA_INTENT, initialIntents.first())
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents.toTypedArray())
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun getPackageName(
        resolveActivity: ComponentName,
        initialIntents: List<Intent>
    ): String? {
        var packageName: String? = resolveActivity.packageName
        if (packageName == "android") {
            if (initialIntents.size > 1) {
                packageName = null
            } else if (initialIntents.size == 1) {
                packageName = initialIntents[0].`package`
            }
        }
        return packageName
    }
}