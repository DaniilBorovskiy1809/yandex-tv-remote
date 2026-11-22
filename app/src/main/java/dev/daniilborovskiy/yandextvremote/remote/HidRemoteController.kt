package dev.daniilborovskiy.yandextvremote.remote

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dev.daniilborovskiy.yandextvremote.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registers the phone as a Bluetooth HID device (keyboard + consumer control).
 *
 * The TV's Bluetooth stack already supports the HID host profile and HOGP.
 * No Yandex-specific protocol is needed for button presses: the TV receives
 * ordinary HID reports and maps them through its stock Generic.kl.
 */
class HidRemoteController(context: Context) {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val _state = MutableStateFlow<HidState>(HidState.Idle)
    val state: StateFlow<HidState> = _state.asStateFlow()

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sendMutex = Mutex()
    private var discoverableJob: Job? = null

    private var started = false
    private var registered = false
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            val hid = proxy as? BluetoothHidDevice ?: return
            hidDevice = hid

            val sdp = BluetoothHidDeviceAppSdpSettings(
                APP_NAME,
                APP_DESCRIPTION,
                APP_PROVIDER,
                HID_SUBCLASS,
                HID_REPORT_DESCRIPTOR,
            )

            val accepted = try {
                hid.registerApp(sdp, null, null, appContext.mainExecutor, callback)
            } catch (_: SecurityException) {
                false
            }

            if (!accepted) {
                _state.value = HidState.Error(appContext.getString(R.string.msg_hid_register_failed))
                closeProxy()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            connectedDevice = null
            registered = false
            if (started) {
                _state.value = HidState.Error(appContext.getString(R.string.msg_hid_register_failed))
            }
        }
    }

    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            this@HidRemoteController.registered = registered
            if (!registered) {
                if (started) {
                    _state.value = HidState.Error(appContext.getString(R.string.msg_hid_register_failed))
                }
                return
            }

            _state.value = HidState.Ready
            makeDiscoverable()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    _state.value = HidState.Connected(deviceName(device))
                }

                BluetoothProfile.STATE_CONNECTING -> {
                    _state.value = HidState.Connecting(deviceName(device))
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedDevice == device) connectedDevice = null
                    if (started) _state.value = HidState.Ready
                }

                BluetoothProfile.STATE_DISCONNECTING -> {
                    if (started) _state.value = HidState.Ready
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Int, id: Int, bufferSize: Int) {
            val hid = hidDevice ?: return
            try {
                hid.sendReport(device, id, ByteArray(bufferSize.coerceAtLeast(0)))
            } catch (_: SecurityException) {
                // The host will retry or disconnect; no user action needed here.
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Int, id: Int, data: ByteArray) {
            // The TV may send keyboard LED output reports. We do not have LEDs.
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Int) {
            // Boot/report protocol is irrelevant for our short reports.
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Int, data: ByteArray) {
            // No interrupt channel is used by this app.
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            connectedDevice = null
            if (started) _state.value = HidState.Ready
        }
    }

    fun start() {
        if (started) return
        if (!hasConnectPermission() || !hasAdvertisePermission()) {
            _state.value = HidState.PermissionRequired
            return
        }

        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            _state.value = HidState.Unsupported
            return
        }

        val enabled = try {
            bluetoothAdapter.isEnabled
        } catch (_: SecurityException) {
            false
        }
        if (!enabled) {
            _state.value = HidState.BluetoothOff
            return
        }

        if (!bluetoothAdapter.isProfileSupported(BluetoothProfile.HID_DEVICE)) {
            _state.value = HidState.Unsupported
            return
        }

        started = true
        _state.value = HidState.Registering

        val requested = try {
            bluetoothAdapter.getProfileProxy(appContext, serviceListener, BluetoothProfile.HID_DEVICE)
        } catch (_: SecurityException) {
            false
        }

        if (!requested) {
            started = false
            _state.value = HidState.Error(appContext.getString(R.string.msg_hid_register_failed))
        }
    }

    fun restart() {
        stop()
        start()
    }

    fun stop() {
        started = false
        discoverableJob?.cancel()
        discoverableJob = null
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        closeProxy()
        connectedDevice = null
        _state.value = HidState.Idle
    }

    fun makeDiscoverable() {
        if (!started || connectedDevice != null) return
        if (!hasAdvertisePermission()) {
            _state.value = HidState.PermissionRequired
            return
        }

        val bluetoothAdapter = adapter ?: return
        val accepted = try {
            bluetoothAdapter.setScanMode(
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
                DISCOVERABLE_SECONDS,
            )
        } catch (_: SecurityException) {
            false
        }

        if (!accepted) {
            _state.value = HidState.Error(appContext.getString(R.string.msg_discoverable_failed))
            return
        }

        _state.value = HidState.Discoverable
        discoverableJob?.cancel()
        discoverableJob = scope.launch {
            delay(DISCOVERABLE_SECONDS * 1_000L)
            if (_state.value is HidState.Discoverable) {
                _state.value = HidState.Ready
            }
        }
    }

    fun send(key: RemoteKey) {
        val command = key.hid ?: return
        val device = connectedDevice ?: return
        val hid = hidDevice ?: return

        scope.launch {
            sendMutex.withLock {
                val success = try {
                    when (command) {
                        is HidCommand.Consumer -> {
                            val down = hid.sendReport(
                                device,
                                REPORT_ID_CONSUMER,
                                command.usage.toLittleEndian(),
                            )
                            delay(KEY_HOLD_MS)
                            val up = hid.sendReport(device, REPORT_ID_CONSUMER, CONSUMER_RELEASE)
                            down && up
                        }

                        is HidCommand.Keyboard -> {
                            val down = hid.sendReport(
                                device,
                                REPORT_ID_KEYBOARD,
                                byteArrayOf(
                                    0,
                                    0,
                                    command.usage.toByte(),
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                ),
                            )
                            delay(KEY_HOLD_MS)
                            val up = hid.sendReport(device, REPORT_ID_KEYBOARD, KEYBOARD_RELEASE)
                            down && up
                        }
                    }
                } catch (_: SecurityException) {
                    false
                }

                if (!success) {
                    _state.value = HidState.Error(appContext.getString(R.string.msg_hid_send_failed))
                } else if (_state.value is HidState.Error) {
                    _state.value = HidState.Connected(deviceName(device))
                }
            }
        }
    }

    private fun closeProxy() {
        val hid = hidDevice
        if (hid != null) {
            if (registered) {
                try {
                    hid.unregisterApp()
                } catch (_: SecurityException) {
                    // Profile proxy may already be gone.
                }
            }
            try {
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            } catch (_: SecurityException) {
                // Nothing else to release.
            }
        }
        hidDevice = null
        registered = false
    }

    private fun deviceName(device: BluetoothDevice): String = try {
        device.name ?: device.address
    } catch (_: SecurityException) {
        device.address
    }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED

    private fun hasAdvertisePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            ) == PackageManager.PERMISSION_GRANTED

    private fun Int.toLittleEndian(): ByteArray = byteArrayOf(
        (this and 0xff).toByte(),
        ((this shr 8) and 0xff).toByte(),
    )

    companion object {
        private const val APP_NAME = "Yandex TV Remote"
        private const val APP_DESCRIPTION = "Bluetooth HID remote for Yandex TV"
        private const val APP_PROVIDER = "Daniil Borovskiy"
        private const val HID_SUBCLASS: Byte = 0xC0.toByte()
        private const val REPORT_ID_KEYBOARD = 1
        private const val REPORT_ID_CONSUMER = 2
        private const val KEY_HOLD_MS = 30L
        private const val DISCOVERABLE_SECONDS = 120

        private val CONSUMER_RELEASE = byteArrayOf(0, 0)
        private val KEYBOARD_RELEASE = ByteArray(8)

        /**
         * Composite HID descriptor:
         *   Report ID 1: boot-compatible keyboard with 6-key rollover.
         *   Report ID 2: 16-bit consumer control array.
         *
         * Consumer usages are deliberately standard (Home, Back, Menu, D-pad,
         * media, volume, channel, voice) so the TV's Generic.kl maps them
         * without any Yandex-specific keylayout.
         */
        private val HID_REPORT_DESCRIPTOR: ByteArray = descriptor(
            0x05, 0x01,             // Usage Page (Generic Desktop)
            0x09, 0x06,             // Usage (Keyboard)
            0xA1, 0x01,             // Collection (Application)
            0x85, 0x01,             //   Report ID (1)
            0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
            0x19, 0xE0,             //   Usage Minimum (224)
            0x29, 0xE7,             //   Usage Maximum (231)
            0x15, 0x00,             //   Logical Minimum (0)
            0x25, 0x01,             //   Logical Maximum (1)
            0x75, 0x01,             //   Report Size (1)
            0x95, 0x08,             //   Report Count (8)
            0x81, 0x02,             //   Input (Data,Var,Abs)
            0x75, 0x08,             //   Report Size (8)
            0x95, 0x01,             //   Report Count (1)
            0x81, 0x01,             //   Input (Const)
            0x05, 0x08,             //   Usage Page (LEDs)
            0x19, 0x01,             //   Usage Minimum (1)
            0x29, 0x05,             //   Usage Maximum (5)
            0x75, 0x01,             //   Report Size (1)
            0x95, 0x05,             //   Report Count (5)
            0x91, 0x02,             //   Output (Data,Var,Abs)
            0x75, 0x03,             //   Report Size (3)
            0x95, 0x01,             //   Report Count (1)
            0x91, 0x01,             //   Output (Const)
            0x05, 0x07,             //   Usage Page (Keyboard/Keypad)
            0x19, 0x00,             //   Usage Minimum (0)
            0x29, 0x65,             //   Usage Maximum (101)
            0x15, 0x00,             //   Logical Minimum (0)
            0x25, 0x65,             //   Logical Maximum (101)
            0x75, 0x08,             //   Report Size (8)
            0x95, 0x06,             //   Report Count (6)
            0x81, 0x00,             //   Input (Data,Array,Abs)
            0xC0,                   // End Collection
            0x05, 0x0C,             // Usage Page (Consumer)
            0x09, 0x01,             // Usage (Consumer Control)
            0xA1, 0x01,             // Collection (Application)
            0x85, 0x02,             //   Report ID (2)
            0x15, 0x00,             //   Logical Minimum (0)
            0x26, 0xFF, 0x03,       //   Logical Maximum (1023)
            0x19, 0x00,             //   Usage Minimum (0)
            0x2A, 0xFF, 0x03,       //   Usage Maximum (1023)
            0x75, 0x10,             //   Report Size (16)
            0x95, 0x01,             //   Report Count (1)
            0x81, 0x00,             //   Input (Data,Array,Abs)
            0xC0,                   // End Collection
        )

        private fun descriptor(vararg values: Int): ByteArray =
            ByteArray(values.size) { values[it].toByte() }
    }
}

sealed interface HidState {
    data object Idle : HidState
    data object BluetoothOff : HidState
    data object Unsupported : HidState
    data object PermissionRequired : HidState
    data object Registering : HidState
    data object Ready : HidState
    data object Discoverable : HidState
    data class Connecting(val name: String) : HidState
    data class Connected(val name: String) : HidState
    data class Error(val message: String) : HidState
}
