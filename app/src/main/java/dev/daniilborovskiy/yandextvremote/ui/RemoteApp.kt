package dev.daniilborovskiy.yandextvremote.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.daniilborovskiy.yandextvremote.RemoteViewModel

@Composable
fun RemoteApp(viewModel: RemoteViewModel) {
    val context = LocalContext.current
    val adapter = remember(context) {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    val requiredPermissions = remember { bluetoothPermissions() }

    var permissionGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }
    var bluetoothEnabled by remember { mutableStateOf(isBluetoothEnabled(adapter)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionGranted = requiredPermissions.all {
            result[it] == true ||
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.onBluetoothPermissionChanged(permissionGranted)
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        bluetoothEnabled = isBluetoothEnabled(adapter)
        viewModel.onBluetoothPermissionChanged(permissionGranted)
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        // ACTION_REQUEST_DISCOVERABLE returns the granted duration (for
        // example 120) on success and RESULT_CANCELED when declined.
        viewModel.onDiscoverableResult(result.resultCode != Activity.RESULT_CANCELED)
    }

    LaunchedEffect(Unit) {
        if (requiredPermissions.isNotEmpty() && !permissionGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            viewModel.onBluetoothPermissionChanged(true)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        permissionGranted = granted
        bluetoothEnabled = isBluetoothEnabled(adapter)
        viewModel.onBluetoothPermissionChanged(granted)
    }

    val hidState by viewModel.hidState.collectAsStateWithLifecycle()

    RemoteScreen(
        mode = viewModel.mode,
        hidState = hidState,
        irAvailable = viewModel.irAvailable,
        permissionGranted = permissionGranted,
        bluetoothEnabled = bluetoothEnabled,
        message = viewModel.lastMessage,
        onModeChange = viewModel::setMode,
        onSend = viewModel::send,
        onMakeDiscoverable = {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)
            }
            discoverableLauncher.launch(intent)
        },
        onRequestPermissions = { permissionLauncher.launch(requiredPermissions) },
        onEnableBluetooth = {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        },
        onRetry = viewModel::retryBluetooth,
        onMessageShown = viewModel::clearMessage,
    )
}

private const val DISCOVERABLE_SECONDS = 120

private fun bluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        )
    } else {
        emptyArray()
    }

private fun isBluetoothEnabled(adapter: BluetoothAdapter?): Boolean = try {
    adapter?.isEnabled == true
} catch (_: SecurityException) {
    false
}
