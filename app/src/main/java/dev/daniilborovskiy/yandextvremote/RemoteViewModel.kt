package dev.daniilborovskiy.yandextvremote

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.daniilborovskiy.yandextvremote.remote.HidRemoteController
import dev.daniilborovskiy.yandextvremote.remote.HidState
import dev.daniilborovskiy.yandextvremote.remote.IrRemoteController
import dev.daniilborovskiy.yandextvremote.remote.RemoteKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RemoteMode {
    BLUETOOTH,
    INFRARED,
}

class RemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val hid = HidRemoteController(application)
    private val ir = IrRemoteController(application)

    val hidState: StateFlow<HidState> = hid.state
    val irAvailable: Boolean = ir.isAvailable

    var mode by mutableStateOf(RemoteMode.BLUETOOTH)
        private set

    var lastMessage by mutableStateOf<String?>(null)
        private set

    private var bluetoothPermissionGranted = false

    fun onBluetoothPermissionChanged(granted: Boolean) {
        bluetoothPermissionGranted = granted
        if (granted && mode == RemoteMode.BLUETOOTH) {
            hid.start()
        } else if (!granted) {
            hid.stop()
        }
    }

    fun selectMode(newMode: RemoteMode) {
        if (mode == newMode) return
        mode = newMode
        lastMessage = null

        when (newMode) {
            RemoteMode.BLUETOOTH -> if (bluetoothPermissionGranted) hid.start()
            RemoteMode.INFRARED -> hid.stop()
        }
    }

    fun onDiscoverableResult(granted: Boolean) {
        hid.onDiscoverableResult(granted)
    }

    fun retryBluetooth() {
        if (mode == RemoteMode.BLUETOOTH && bluetoothPermissionGranted) {
            hid.restart()
        }
    }

    fun send(key: RemoteKey) {
        when (mode) {
            RemoteMode.BLUETOOTH -> {
                if (hidState.value !is HidState.Connected) {
                    lastMessage = getApplication<Application>().getString(R.string.msg_connect_first)
                    return
                }
                hid.send(key) {
                    lastMessage = getApplication<Application>().getString(R.string.msg_hid_send_failed)
                }
            }

            RemoteMode.INFRARED -> {
                val command = key.ir
                if (command == null) {
                    lastMessage = getApplication<Application>().getString(R.string.msg_ir_unsupported_key)
                    return
                }
                viewModelScope.launch {
                    val sent = withContext(Dispatchers.IO) { ir.send(command) }
                    if (!sent) {
                        lastMessage = getApplication<Application>().getString(R.string.msg_ir_send_failed)
                    }
                }
            }
        }
    }

    fun clearMessage() {
        lastMessage = null
    }

    override fun onCleared() {
        hid.stop()
        super.onCleared()
    }
}
