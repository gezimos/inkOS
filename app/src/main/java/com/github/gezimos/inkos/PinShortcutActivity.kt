package com.github.gezimos.inkos

import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.gezimos.common.showShortToast
import com.github.gezimos.inkos.helper.PinnedShortcutUtility

/**
 * Activity that handles incoming shortcut pin requests from other apps.
 * 
 * This handles two types of requests:
 * 1. Legacy ACTION_CREATE_SHORTCUT intents (deprecated but still used)
 * 2. LauncherApps.PinItemRequest (API 26+) for modern pinning
 */
class PinShortcutActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PinShortcutActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Process the intent and finish immediately
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        if (intent == null) {
            finish()
            return
        }
        
        Log.d(TAG, "Received intent: ${intent.action}")
        
        when {
            // Handle modern pin request (API 26+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            intent.action == LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT -> {
                handlePinShortcutRequest()
            }
            
            // Handle legacy shortcut creation
            intent.action == Intent.ACTION_CREATE_SHORTCUT -> {
                handleLegacyShortcut(intent)
            }
            
            intent.action == "com.android.launcher.action.INSTALL_SHORTCUT" -> {
                handleInstallShortcut(intent)
            }
            
            else -> {
                Log.w(TAG, "Unknown intent action: ${intent.action}")
                finish()
            }
        }
    }
    
    /**
     * Handle modern pin shortcut request (API 26+).
     * The system provides a PinItemRequest that we can accept or reject.
     */
    private fun handlePinShortcutRequest() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            finish()
            return
        }
        
        try {
            val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
            val request = launcherApps.getPinItemRequest(intent)
            
            if (request == null) {
                Log.e(TAG, "No pin request found in intent")
                finish()
                return
            }
            
            when (request.requestType) {
                LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT -> {
                    val shortcutInfo = request.shortcutInfo
                    if (shortcutInfo != null) {
                        val label = shortcutInfo.shortLabel?.toString()
                            ?: shortcutInfo.longLabel?.toString()
                            ?: shortcutInfo.id
                        
                        Log.d(TAG, "Pin request for shortcut: ${shortcutInfo.`package`}/${shortcutInfo.id} ($label)")
                        
                        // Accept the pin request
                        val accepted = PinnedShortcutUtility.acceptPinRequest(this, request)
                        
                        if (accepted) {
                            showShortToast("Shortcut pinned: $label")
                            
                            // Notify the apps repository to refresh
                            try {
                                val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
                                appsRepo.onPackageChanged()
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to refresh app list", e)
                            }
                        } else {
                            showShortToast("Failed to pin shortcut")
                        }
                    } else {
                        Log.e(TAG, "ShortcutInfo is null in pin request")
                    }
                }
                
                LauncherApps.PinItemRequest.REQUEST_TYPE_APPWIDGET -> {
                    // We don't support widget pinning yet
                    Log.d(TAG, "Widget pin request - not supported")
                    showShortToast("Widget pinning not supported")
                }
                
                else -> {
                    Log.w(TAG, "Unknown pin request type: ${request.requestType}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling pin shortcut request", e)
            showShortToast("Error pinning shortcut")
        }
        
        finish()
    }
    
    /**
     * Handle legacy ACTION_CREATE_SHORTCUT intent.
     * This is the old way apps requested shortcuts before API 26.
     */
    private fun handleLegacyShortcut(intent: Intent) {
        try {
            // Extract shortcut info from the intent extras
            val shortcutIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)
            }
            
            val shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
            
            if (shortcutIntent != null) {
                val packageName = shortcutIntent.`package` 
                    ?: shortcutIntent.component?.packageName
                    ?: "unknown"
                
                // Generate a unique ID for this shortcut
                val shortcutId = "legacy_${System.currentTimeMillis()}"
                
                Log.d(TAG, "Legacy shortcut creation: $packageName/$shortcutId ($shortcutName)")
                
                // Store as a pinned shortcut
                val added = PinnedShortcutUtility.addPinnedShortcut(
                    context = this,
                    packageName = packageName,
                    shortcutId = shortcutId,
                    label = shortcutName
                )
                
                if (added) {
                    showShortToast("Shortcut added: $shortcutName")
                    
                    // Notify refresh
                    try {
                        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
                        appsRepo.onPackageChanged()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh app list", e)
                    }
                    
                    // Set result OK for the requesting app
                    setResult(RESULT_OK)
                } else {
                    showShortToast("Shortcut already exists")
                    setResult(RESULT_CANCELED)
                }
            } else {
                Log.w(TAG, "No shortcut intent found in legacy shortcut request")
                setResult(RESULT_CANCELED)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling legacy shortcut", e)
            showShortToast("Error creating shortcut")
            setResult(RESULT_CANCELED)
        }
        
        finish()
    }
    
    /**
     * Handle INSTALL_SHORTCUT broadcast (converted to activity start).
     * This is another legacy mechanism for shortcut installation.
     */
    private fun handleInstallShortcut(intent: Intent) {
        try {
            val shortcutIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)
            }
            
            val shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME) ?: "Shortcut"
            
            if (shortcutIntent != null) {
                val packageName = shortcutIntent.`package` 
                    ?: shortcutIntent.component?.packageName
                    ?: "unknown"
                
                val shortcutId = "install_${System.currentTimeMillis()}"
                
                Log.d(TAG, "Install shortcut: $packageName/$shortcutId ($shortcutName)")
                
                val added = PinnedShortcutUtility.addPinnedShortcut(
                    context = this,
                    packageName = packageName,
                    shortcutId = shortcutId,
                    label = shortcutName
                )
                
                if (added) {
                    showShortToast("Shortcut installed: $shortcutName")
                    
                    try {
                        val appsRepo = com.github.gezimos.inkos.data.repository.AppsRepository.getInstance(application)
                        appsRepo.onPackageChanged()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh app list", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling install shortcut", e)
        }
        
        finish()
    }
}
