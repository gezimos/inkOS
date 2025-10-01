package com.github.gezimos.inkos.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment

class BluetoothFragment : Fragment() {

    @Deprecated("Deprecated in Android API")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_CONNECT_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            updateDeviceState()
            val prefs = com.github.gezimos.inkos.data.Prefs(requireContext())
            val settingsSize = (prefs.settingsSize - 3)
            val isDark = when (prefs.appTheme) {
                com.github.gezimos.inkos.data.Constants.Theme.Light -> false
                com.github.gezimos.inkos.data.Constants.Theme.Dark -> true
                com.github.gezimos.inkos.data.Constants.Theme.System -> com.github.gezimos.inkos.helper.isSystemInDarkMode(
                    requireContext()
                )

                else -> false
            }
            composeView?.setContent {
                BluetoothFragmentContent(
                    fontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                    isDark = isDark
                )
            }
        }
    }

    private val BLUETOOTH_CONNECT_REQUEST_CODE = 1001

    companion object {
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }

    // Store battery levels for devices
    private val batteryLevels: androidx.compose.runtime.MutableState<Map<String, Int>> =
        androidx.compose.runtime.mutableStateOf(emptyMap())

    // Battery receiver property for unregistering
    private var batteryReceiver: android.content.BroadcastReceiver? = null

    // ...existing code...
    private var a2dpReceiver: android.content.BroadcastReceiver? = null
    private var lastA2dpConnectedAddress: String? = null

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceState =
        androidx.compose.runtime.mutableStateOf<List<Pair<BluetoothDevice, String>>>(emptyList())

    private var composeView: androidx.compose.ui.platform.ComposeView? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun getDeviceStatus(device: BluetoothDevice): String {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && device.type == BluetoothDevice.DEVICE_TYPE_LE -> "Bonded (BLE)"
            device.address == lastA2dpConnectedAddress -> "Connected (A2DP)"
            else -> "Bonded"
        }
    }

    private fun updateDeviceState() {
        val bonded = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                emptyList()
            } else {
                bluetoothAdapter.bondedDevices.toList()
            }
        } catch (e: SecurityException) {
            emptyList()
        }
        val stateList = bonded.map { device ->
            val status = getDeviceStatus(device)
            device to status
        }
        deviceState.value = stateList
    }

    private fun refreshDeviceList() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                lastA2dpConnectedAddress = null
                updateDeviceState()
                return
            }
            adapter?.getProfileProxy(
                requireContext().applicationContext,
                object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(
                        profile: Int,
                        proxy: BluetoothProfile?
                    ) {
                        if (profile == BluetoothProfile.A2DP && proxy != null) {
                            try {
                                val devices = proxy.javaClass.getMethod("getConnectedDevices")
                                    .invoke(proxy) as? List<*>
                                if (!devices.isNullOrEmpty()) {
                                    val connectedDevice = devices.firstOrNull() as? BluetoothDevice
                                    lastA2dpConnectedAddress = connectedDevice?.address
                                } else {
                                    lastA2dpConnectedAddress = null
                                }
                            } catch (_: Exception) {
                                lastA2dpConnectedAddress = null
                            }
                            adapter.closeProfileProxy(
                                BluetoothProfile.A2DP,
                                proxy
                            )
                            updateDeviceState()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {}
                },
                BluetoothProfile.A2DP
            )
        } catch (_: Exception) {
            lastA2dpConnectedAddress = null
            updateDeviceState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        // Request BLUETOOTH_CONNECT permission at runtime for Android 12+
        val missingPermissions = mutableListOf<String>()
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add("BLUETOOTH_CONNECT")
        }
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add("BLUETOOTH")
        }

        // Root layout: vertical LinearLayout
        val rootLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        var bottomInsetPx = 0
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val navBarInset =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom
            bottomInsetPx = navBarInset
            insets
        }

        // --- Use AdvancedFragment header logic ---
        val prefs = com.github.gezimos.inkos.data.Prefs(requireContext())
        com.github.gezimos.inkos.helper.getHexForOpacity(prefs)
        val isDark = when (prefs.appTheme) {
            com.github.gezimos.inkos.data.Constants.Theme.Light -> false
            com.github.gezimos.inkos.data.Constants.Theme.Dark -> true
            com.github.gezimos.inkos.data.Constants.Theme.System -> com.github.gezimos.inkos.helper.isSystemInDarkMode(
                requireContext()
            )

            else -> false
        }
        val settingsSize = (prefs.settingsSize - 3)
        val currentPage = intArrayOf(0)
        val pageCount = intArrayOf(1)
        val headerView = androidx.compose.ui.platform.ComposeView(context).apply {
            setContent {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                com.github.gezimos.inkos.style.SettingsTheme(isDark) {
                    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                        com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader(
                            iconRes = com.github.gezimos.inkos.R.drawable.ic_back,
                            title = "mKompakt Bluetooth",
                            onClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                        com.github.gezimos.inkos.ui.compose.SettingsComposable.SolidSeparator()
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.height(
                                com.github.gezimos.inkos.style.SettingsTheme.color.horizontalPadding
                            )
                        )
                        if (bottomInsetDp > 0.dp) {
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.height(
                                    bottomInsetDp
                                )
                            )
                        }
                    }
                }
            }
        }
        rootLayout.addView(headerView)

        // Scrollable content ComposeView (BluetoothFragmentContent or permissions message)
        val contentComposeView = androidx.compose.ui.platform.ComposeView(context)

        if (missingPermissions.isNotEmpty()) {
            requestPermissions(
                missingPermissions.map { "android.permission.$it" }.toTypedArray(),
                BLUETOOTH_CONNECT_REQUEST_CODE
            )
            contentComposeView.setContent {
                com.github.gezimos.inkos.style.SettingsTheme(isDark) {
                    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                        val titleFontSize =
                            if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                        com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsTitle(
                            text = getString(com.github.gezimos.inkos.R.string.bluetooth_permission_required_title),
                            fontSize = titleFontSize
                        )
                        com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsItem(
                            text = getString(
                                com.github.gezimos.inkos.R.string.bluetooth_permission_required_body,
                                missingPermissions.joinToString(", ")
                            ),
                            fontSize = titleFontSize
                        )
                    }
                }
            }
        } else {
            // Register receiver for battery level changes
            batteryReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(
                    context: android.content.Context?,
                    intent: android.content.Intent?
                ) {
                    if (intent?.action == ACTION_BATTERY_LEVEL_CHANGED) {
                        val device =
                            intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                        if (device != null && level >= 0) {
                            val updated = batteryLevels.value.toMutableMap()
                            updated[device.address] = level
                            batteryLevels.value = updated
                        }
                    }
                }
            }
            context.registerReceiver(
                batteryReceiver,
                android.content.IntentFilter(ACTION_BATTERY_LEVEL_CHANGED)
            )
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            // Query A2DP profile for currently connected devices
            bluetoothAdapter.getProfileProxy(
                context.applicationContext,
                object : BluetoothProfile.ServiceListener {
                    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        if (profile == BluetoothProfile.A2DP && proxy != null) {
                            val connectedDevices: List<BluetoothDevice> = try {
                                @Suppress("UNCHECKED_CAST")
                                proxy.javaClass.getMethod("getConnectedDevices")
                                    .invoke(proxy) as? List<BluetoothDevice> ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                            val bonded = bluetoothAdapter.bondedDevices.toList()
                            val stateList = bonded.map { device ->
                                val status =
                                    if (connectedDevices.any { it.address == device.address }) "Connected (A2DP)" else "Paired"
                                device to status
                            }
                            deviceState.value = stateList
                            contentComposeView.setContent {
                                BluetoothFragmentContent(
                                    fontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                                    isDark = isDark
                                )
                            }
                            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, proxy)
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {}
                },
                BluetoothProfile.A2DP
            )

            contentComposeView.setContent {
                BluetoothFragmentContent(
                    fontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                    isDark = isDark
                )
            }
        }

        // ScrollView for settings content
        val scrollView = androidx.core.widget.NestedScrollView(context).apply {
            isFillViewport = true
            addView(
                contentComposeView, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        // Use callback-enabled EinkScrollBehavior to update header page indicator reliably
        com.github.gezimos.inkos.helper.utils.EinkScrollBehavior(context) { page, pages ->
            pageCount[0] = pages
            currentPage[0] = page
            // Re-render header with updated page indicator
            headerView.setContent {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val bottomInsetDp = with(density) { bottomInsetPx.toDp() }
                com.github.gezimos.inkos.style.SettingsTheme(isDark) {
                    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                        com.github.gezimos.inkos.ui.compose.SettingsComposable.PageHeader(
                            iconRes = com.github.gezimos.inkos.R.drawable.ic_back,
                            title = "mKompakt Bluetooth",
                            onClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                            showStatusBar = prefs.showStatusBar,
                            pageIndicator = {
                                com.github.gezimos.inkos.ui.compose.SettingsComposable.PageIndicator(
                                    currentPage = currentPage[0],
                                    pageCount = pageCount[0]
                                )
                            },
                            titleFontSize = if (settingsSize > 0) (settingsSize * 1.5).sp else androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                        com.github.gezimos.inkos.ui.compose.SettingsComposable.SolidSeparator()
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.height(
                                com.github.gezimos.inkos.style.SettingsTheme.color.horizontalPadding
                            )
                        )
                        if (bottomInsetDp > 0.dp) {
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier.height(
                                    bottomInsetDp
                                )
                            )
                        }
                    }
                }
            }
        }.attachToScrollView(scrollView)
        rootLayout.addView(
            scrollView, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Register A2DP receiver
        a2dpReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(
                context: android.content.Context?,
                intent: android.content.Intent?
            ) {
                if (intent?.action == "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED") {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1)
                    if (state == BluetoothProfile.STATE_CONNECTED && device != null) {
                        lastA2dpConnectedAddress = device.address
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED && device != null) {
                        if (lastA2dpConnectedAddress == device.address) lastA2dpConnectedAddress =
                            null
                    }
                    updateDeviceState()
                }
            }
        }
        val filter =
            android.content.IntentFilter("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
        context.registerReceiver(a2dpReceiver, filter)

        // Apply bottom padding to root layout to avoid overlap with navbar
        rootLayout.post {
            rootLayout.setPadding(0, 0, 0, bottomInsetPx)
            rootLayout.clipToPadding = false
        }

        return rootLayout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        a2dpReceiver?.let {
            requireContext().unregisterReceiver(it)
            a2dpReceiver = null
        }
        // Unregister battery receiver
        batteryReceiver?.let {
            try {
                requireContext().unregisterReceiver(it)
            } catch (_: Exception) {
            }
            batteryReceiver = null
        }
        composeView = null
    }

    @androidx.compose.runtime.Composable
    private fun BluetoothFragmentContent(
        fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
        isDark: Boolean
    ) {
        com.github.gezimos.inkos.style.SettingsTheme(isDark) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                deviceState.value.forEach { (device, status) ->
                    val battery = batteryLevels.value[device.address]
                    val emoji = when (device.bluetoothClass?.deviceClass) {
                        android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
                        android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                        android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
                        android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
                        android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
                        android.bluetooth.BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "🎧"

                        android.bluetooth.BluetoothClass.Device.PHONE_SMART,
                        android.bluetooth.BluetoothClass.Device.PHONE_CELLULAR,
                        android.bluetooth.BluetoothClass.Device.PHONE_CORDLESS,
                        android.bluetooth.BluetoothClass.Device.PHONE_ISDN -> "📱"

                        android.bluetooth.BluetoothClass.Device.COMPUTER_LAPTOP,
                        android.bluetooth.BluetoothClass.Device.COMPUTER_DESKTOP,
                        android.bluetooth.BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA,
                        android.bluetooth.BluetoothClass.Device.COMPUTER_SERVER,
                        android.bluetooth.BluetoothClass.Device.COMPUTER_UNCATEGORIZED -> "💻"

                        android.bluetooth.BluetoothClass.Device.PERIPHERAL_KEYBOARD -> "⌨️"
                        0x580 -> "🖱️"
                        else -> "❓"
                    }
                    val title = if (battery != null) {
                        "$emoji ${device.name ?: "Unknown"} - ${battery}%"
                    } else {
                        "$emoji ${device.name ?: "Unknown"}"
                    }
                    com.github.gezimos.inkos.ui.compose.SettingsComposable.SettingsSelect(
                        title = title,
                        option = if (status == "Connected (A2DP)") "Connected" else "Paired",
                        fontSize = fontSize,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                                device.type == BluetoothDevice.DEVICE_TYPE_LE
                            ) {
                                if (isAdded) {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(
                                            requireContext(),
                                            "BLE device. Use a dedicated app to connect.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                return@SettingsSelect
                            }
                            val context = requireContext().applicationContext
                            val adapter = BluetoothAdapter.getDefaultAdapter()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                Toast.makeText(
                                    requireContext(),
                                    "BLUETOOTH_CONNECT permission required",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@SettingsSelect
                            }
                            adapter?.getProfileProxy(
                                context,
                                object : BluetoothProfile.ServiceListener {
                                    override fun onServiceConnected(
                                        profile: Int,
                                        proxy: BluetoothProfile?
                                    ) {
                                        if (profile == BluetoothProfile.A2DP && proxy != null) {
                                            try {
                                                if (status == "Connected (A2DP)") {
                                                    try {
                                                        val disconnectMethod =
                                                            proxy.javaClass.getMethod(
                                                                "disconnect",
                                                                BluetoothDevice::class.java
                                                            )
                                                        disconnectMethod.invoke(proxy, device)
                                                        if (isAdded) {
                                                            requireActivity().runOnUiThread @RequiresPermission(
                                                                Manifest.permission.BLUETOOTH_CONNECT
                                                            ) {
                                                                Toast.makeText(
                                                                    requireContext(),
                                                                    "Disconnecting from ${device.name} (A2DP)",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        if (isAdded) {
                                                            requireActivity().runOnUiThread @RequiresPermission(
                                                                Manifest.permission.BLUETOOTH_CONNECT
                                                            ) {
                                                                Toast.makeText(
                                                                    requireContext(),
                                                                    "Disconnect failed for ${device.name}",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                        e.printStackTrace()
                                                    }
                                                } else {
                                                    val connectMethod = proxy.javaClass.getMethod(
                                                        "connect",
                                                        BluetoothDevice::class.java
                                                    )
                                                    connectMethod.invoke(proxy, device)
                                                    if (isAdded) {
                                                        requireActivity().runOnUiThread @RequiresPermission(
                                                            Manifest.permission.BLUETOOTH_CONNECT
                                                        ) {
                                                            Toast.makeText(
                                                                requireContext(),
                                                                "Connecting to ${device.name} (A2DP)",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                if (isAdded) {
                                                    requireActivity().runOnUiThread @RequiresPermission(
                                                        Manifest.permission.BLUETOOTH_CONNECT
                                                    ) {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            "A2DP operation failed: ${device.name}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                e.printStackTrace()
                                            }
                                            adapter.closeProfileProxy(
                                                BluetoothProfile.A2DP,
                                                proxy
                                            )
                                            android.os.Handler(android.os.Looper.getMainLooper())
                                                .postDelayed({
                                                    updateDeviceState()
                                                }, 1000)
                                        }
                                    }

                                    override fun onServiceDisconnected(profile: Int) {}
                                },
                                BluetoothProfile.A2DP
                            )
                        }
                    )
                    com.github.gezimos.inkos.ui.compose.SettingsComposable.DashedSeparator()
                }
            }
        }
    }
}