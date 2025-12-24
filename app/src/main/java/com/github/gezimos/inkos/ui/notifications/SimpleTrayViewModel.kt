package com.github.gezimos.inkos.ui.notifications

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.gezimos.inkos.data.Prefs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SimpleTrayUiEvent {
    object OpenWifiPanel : SimpleTrayUiEvent
    object OpenInternetPanel : SimpleTrayUiEvent
    object OpenBluetoothSettings : SimpleTrayUiEvent
}

sealed interface SimpleTrayPermissionRequest {
    object BluetoothConnect : SimpleTrayPermissionRequest
    object Camera : SimpleTrayPermissionRequest
    object ReadPhoneState : SimpleTrayPermissionRequest
}

class SimpleTrayViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext
    private val prefs = Prefs(appContext)

    private val _wifiEnabled = MutableStateFlow(false)
    val wifiEnabled: StateFlow<Boolean> = _wifiEnabled.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(false)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _mobileDataEnabled = MutableStateFlow(false)
    val mobileDataEnabled: StateFlow<Boolean> = _mobileDataEnabled.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    private val _brightnessLevel = MutableStateFlow(prefs.brightnessLevel.coerceIn(0, 255))
    val brightnessLevel: StateFlow<Int> = _brightnessLevel.asStateFlow()

    private val _uiEvents = MutableSharedFlow<SimpleTrayUiEvent>(extraBufferCapacity = 4)
    val uiEvents = _uiEvents.asSharedFlow()

    private val _permissionRequests = MutableSharedFlow<SimpleTrayPermissionRequest>(extraBufferCapacity = 4)
    val permissionRequests = _permissionRequests.asSharedFlow()

    private val cameraManager = appContext.getSystemService(CameraManager::class.java)

    init {
        refreshStates()
        registerReceiversAndCallbacks()
    }

    fun refreshStates() {
        viewModelScope.launch {
            _wifiEnabled.value = try {
                val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wm?.isWifiEnabled == true
            } catch (_: Exception) { false }

            _bluetoothEnabled.value = try {
                val btManager = appContext.getSystemService(BluetoothManager::class.java)
                val adapter = btManager?.adapter
                adapter?.isEnabled == true
            } catch (_: Exception) { false }

            _mobileDataEnabled.value = try {
                // If airplane mode is on, treat mobile data as OFF
                val airplane = try { android.provider.Settings.Global.getInt(appContext.contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) } catch (_: Exception) { 0 }
                if (airplane == 1) {
                    false
                } else {
                    // If there are no active SIM subscriptions, treat mobile data as OFF
                    val sm = try { appContext.getSystemService(android.telephony.SubscriptionManager::class.java) } catch (_: Exception) { null }
                    val activeCount = try { sm?.activeSubscriptionInfoCount ?: 0 } catch (_: Exception) { 0 }
                    if (activeCount <= 0) {
                        false
                    } else {
                        val tm = appContext.getSystemService(TelephonyManager::class.java)
                        // Prefer TelephonyManager.isDataEnabled when permission is available. Otherwise fall back
                        // to Settings.Global.MOBILE_DATA if available, then to currentCellState(). This avoids
                        // treating a wifi-only connectivity change as mobile data being on.
                        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            try { tm.isDataEnabled } catch (_: Exception) {
                                try { android.provider.Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentCellState() }
                            }
                        } else {
                            try { android.provider.Settings.Global.getInt(appContext.contentResolver, "mobile_data", 0) == 1 } catch (_: Exception) { currentCellState() }
                        }
                    }
                }
            } catch (_: Exception) { false }

            // torch state will be updated via TorchCallback when available; set false if not available
            _flashlightEnabled.value = false
        }
    }

    private fun currentCellState(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (_: Exception) { false }
    }

    private fun registerReceiversAndCallbacks() {
        try {
            // WiFi
            val wifiReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    viewModelScope.launch { _wifiEnabled.value = try {
                        val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        wm?.isWifiEnabled == true
                    } catch (_: Exception) { false } }
                }
            }
            appContext.registerReceiver(wifiReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))

            // Bluetooth
            val bluetoothReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    viewModelScope.launch {
                        _bluetoothEnabled.value = try {
                            val btManager = appContext.getSystemService(BluetoothManager::class.java)
                            btManager?.adapter?.isEnabled == true
                        } catch (_: Exception) { false }
                    }
                }
            }
            appContext.registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

            // Connectivity callback
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    // Recompute full state when network capabilities change; this avoids mixing
                    // wifi connectivity events with mobile-data toggle state.
                    viewModelScope.launch {
                        try { refreshStates() } catch (_: Exception) {}
                    }
                }

                override fun onLost(network: Network) {
                    viewModelScope.launch {
                        try { refreshStates() } catch (_: Exception) {}
                    }
                }
            }
            try {
                cm?.registerDefaultNetworkCallback(networkCallback)
            } catch (_: Exception) {
                try {
                    val req = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
                    cm?.registerNetworkCallback(req, networkCallback)
                } catch (_: Exception) {}
            }

            // Torch callback
            try {
                val torchCb = object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        viewModelScope.launch { _flashlightEnabled.value = enabled }
                    }
                }
                cameraManager?.registerTorchCallback(torchCb, Handler(Looper.getMainLooper()))
            } catch (_: Exception) {}

            // Airplane mode: update mobile data state when airplane toggles
            try {
                val airplaneReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try { viewModelScope.launch { refreshStates() } } catch (_: Exception) {}
                    }
                }
                appContext.registerReceiver(airplaneReceiver, IntentFilter(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED))
            } catch (_: Exception) {}

            // SIM state changes: update mobile data state when SIMs inserted/removed
            try {
                val simReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        try { viewModelScope.launch { refreshStates() } } catch (_: Exception) {}
                    }
                }
                appContext.registerReceiver(simReceiver, IntentFilter("android.intent.action.SIM_STATE_CHANGED"))
            } catch (_: Exception) {}

        } catch (_: Exception) {}
    }

    // UI intent emitters
    fun openWifiPanel() { viewModelScope.launch { _uiEvents.emit(SimpleTrayUiEvent.OpenWifiPanel) } }
    fun openInternetPanel() { viewModelScope.launch { _uiEvents.emit(SimpleTrayUiEvent.OpenInternetPanel) } }
    fun openBluetoothSettings() { viewModelScope.launch { _uiEvents.emit(SimpleTrayUiEvent.OpenBluetoothSettings) } }

    // Toggle actions (permission checks may cause permissionRequests to be emitted)
    fun requestToggleBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModelScope.launch { _permissionRequests.emit(SimpleTrayPermissionRequest.BluetoothConnect) }
        } else {
            toggleBluetooth()
        }
    }

    fun toggleBluetooth() {
        viewModelScope.launch {
            try {
                // Android 15+ (API 35) restricts BluetoothAdapter.enable()/disable() for third-party apps
                // Open Settings directly instead of attempting deprecated API calls
                if (Build.VERSION.SDK_INT >= 35) {
                    _uiEvents.emit(SimpleTrayUiEvent.OpenBluetoothSettings)
                    return@launch
                }
                
                val btManager = appContext.getSystemService(BluetoothManager::class.java)
                val adapter = btManager?.adapter
                if (adapter != null) {
                    val wasEnabled = adapter.isEnabled
                    // attempt to toggle
                    try {
                        if (wasEnabled) adapter.disable() else adapter.enable()
                    } catch (_: Exception) {}
                    // give the system a moment to apply the change
                    kotlinx.coroutines.delay(800)
                    val nowEnabled = try { btManager?.adapter?.isEnabled == true } catch (_: Exception) { wasEnabled }
                    if (nowEnabled == wasEnabled) {
                        // Toggle had no effect (restricted on some devices/OS); prompt user to toggle in settings
                        viewModelScope.launch { _uiEvents.emit(SimpleTrayUiEvent.OpenBluetoothSettings) }
                    }
                    _bluetoothEnabled.value = nowEnabled
                }
            } catch (_: Exception) {}
        }
    }

    fun requestToggleFlashlight() {
        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModelScope.launch { _permissionRequests.emit(SimpleTrayPermissionRequest.Camera) }
        } else {
            toggleFlashlight()
        }
    }

    fun toggleFlashlight() {
        viewModelScope.launch {
            try {
                val cm = cameraManager ?: return@launch
                val id = cm.cameraIdList.firstOrNull { id0 ->
                    try { cm.getCameraCharacteristics(id0).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } catch (_: Exception) { false }
                } ?: return@launch
                val current = _flashlightEnabled.value
                cm.setTorchMode(id, !current)
                _flashlightEnabled.value = !current
            } catch (_: Exception) {}
        }
    }

    fun setBrightness(level: Int) {
        viewModelScope.launch {
            val v = level.coerceIn(0, 255)
            try {
                prefs.brightnessLevel = v
                // Save non-zero values to lastBrightnessLevel for restoration
                if (v > 0) {
                    prefs.lastBrightnessLevel = v
                }
                if (Settings.System.canWrite(appContext)) {
                    try { Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v) } catch (_: Exception) {}
                }
                _brightnessLevel.value = v
            } catch (_: Exception) {}
        }
    }

    // Called by Fragment when permission resolved — retry pending actions as needed
    fun onPermissionResult(request: SimpleTrayPermissionRequest, granted: Boolean) {
        viewModelScope.launch {
            if (!granted) return@launch
            when (request) {
                SimpleTrayPermissionRequest.BluetoothConnect -> toggleBluetooth()
                SimpleTrayPermissionRequest.Camera -> toggleFlashlight()
                SimpleTrayPermissionRequest.ReadPhoneState -> refreshStates()
            }
        }
    }
}
