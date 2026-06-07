package com.github.gezimos.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.github.gezimos.inkos.CrashReportActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    companion object {
        const val CRASH_LOOP_PREFS = "crash_loop"
        const val KEY_SAFE_MODE = "safe_mode"
        const val KEY_CRASH_TIMESTAMPS = "crash_timestamps"
        private const val CRASH_WINDOW_MS = 30_000L
        private const val CRASH_THRESHOLD = 3

        private val userActions = LinkedBlockingQueue<String>(50) // Stores last 50 user actions

        fun logUserAction(action: String) {
            val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            userActions.offer("$timeStamp - $action")
            if (userActions.size > 50) userActions.poll() // Remove oldest if over limit
        }

        fun customReportSender(context: Context): Uri? {
            val logFile: File = try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)

                // Use internal storage for saving the crash log
                val crashDir = File(context.filesDir, "crash_logs")  // Internal storage
                if (!crashDir.exists()) crashDir.mkdirs()

                val crashFile = File(crashDir, "${packageInfo.packageName}-crash-report.txt")

                // Check if the file exists before attempting to read
                if (crashFile.exists()) {
                    // Read the content of the file
                    val fileInputStream = FileInputStream(crashFile)
                    val inputStreamReader = InputStreamReader(fileInputStream)
                    val stringBuilder = StringBuilder()

                    // Read the file line by line
                    inputStreamReader.forEachLine { stringBuilder.append(it).append("\n") }

                    // Log the content of the crash report file
                    Log.d("CrashHandler", "Crash Report Content:\n${stringBuilder}")
                } else {
                    Log.e("CrashHandler", "Crash report file does not exist.")
                }

                File(crashDir, "${packageInfo.packageName}-crash-report.txt")
            } catch (e: Exception) {
                Log.e("CrashHandler", "Error determining crash log file location: ${e.message}")
                return null // Return null if something goes wrong
            }

            // Ensure the file exists
            if (!logFile.exists()) {
                return null
            }

            // Use FileProvider to get a content Uri for the file
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e("CrashHandler", "Caught exception: ${exception.message}", exception)

        // Step 1: Save custom crash log
        saveCrashLog(exception)

        // Step 2: Record crash timestamp for crash-loop detection
        recordCrashTimestamp()

        // Step 3: Start CrashReportActivity with the crash details
        val intent = Intent(context, CrashReportActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        // Kill the process
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }

    /**
     * Records crash timestamps to detect crash loops.
     * If 3+ crashes occur within 30 seconds, sets a safe_mode flag so
     * MainActivity can skip normal UI and show the crash report directly.
     * Uses commit() instead of apply() since the process is about to die.
     */
    private fun recordCrashTimestamp() {
        try {
            val sp = context.getSharedPreferences(CRASH_LOOP_PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()

            val existing = sp.getString(KEY_CRASH_TIMESTAMPS, "")
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { it.toLongOrNull() }
                ?.filter { now - it < CRASH_WINDOW_MS }
                ?: emptyList()

            val updated = existing + now

            val editor = sp.edit()
            editor.putString(KEY_CRASH_TIMESTAMPS, updated.joinToString(","))
            if (updated.size >= CRASH_THRESHOLD) {
                editor.putBoolean(KEY_SAFE_MODE, true)
            }
            editor.commit()
        } catch (_: Exception) {}
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun saveCrashLog(exception: Throwable): File {
        val logFile: File = try {
            val packageManager = context.packageManager
            val packageInfo: PackageInfo = packageManager.getPackageInfo(context.packageName, 0)

            // Use internal storage for saving the crash log
            val crashDir = File(context.filesDir, "crash_logs")  // This is internal storage
            if (!crashDir.exists()) crashDir.mkdirs()

            File(crashDir, "${packageInfo.packageName}-crash-report.txt")
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error determining crash log file location: ${e.message}")
            // In case of error, use a default file name
            File(context.filesDir, "default-crash-report.txt")
        }

        try {
            FileWriter(logFile).use { writer ->
                PrintWriter(writer).use { printWriter ->
                    printWriter.println("Crash Report - ${Date()}")
                    printWriter.println("Thread: ${Thread.currentThread().name}")
                    printWriter.println("\n=== Device Info ===")
                    printWriter.println(getDeviceInfo())
                    printWriter.println("\n=== Recent User Actions ===")
                    userActions.forEach { printWriter.println(it) }
                    printWriter.println("\n=== Crash Stack Trace ===")
                    exception.printStackTrace(printWriter)
                }
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error writing crash log: ${e.message}")
        }
        return logFile
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getDeviceInfo(): String {
        return try {
            val packageManager = context.packageManager
            val packageInfo: PackageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val installSource = getInstallSource(packageManager, context.packageName)

            """
                App Version: ${packageInfo.versionName} (${packageInfo.longVersionCode})
                Installed From: $installSource
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                CPU: ${Build.SUPPORTED_ABIS.joinToString()}
            """.trimIndent()
        } catch (e: Exception) {
            "Device Info Unavailable: ${e.message}"
        }
    }

    private fun getInstallSource(packageManager: PackageManager, packageName: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName ?: "Unknown"
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName) ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
}