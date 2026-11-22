package dev.daniilborovskiy.yandextvremote.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.daniilborovskiy.yandextvremote.R
import dev.daniilborovskiy.yandextvremote.RemoteMode
import dev.daniilborovskiy.yandextvremote.remote.HidState
import dev.daniilborovskiy.yandextvremote.remote.RemoteKey

@Composable
fun RemoteScreen(
    mode: RemoteMode,
    hidState: HidState,
    irAvailable: Boolean,
    permissionGranted: Boolean,
    bluetoothEnabled: Boolean,
    message: String?,
    onModeChange: (RemoteMode) -> Unit,
    onSend: (RemoteKey) -> Unit,
    onMakeDiscoverable: () -> Unit,
    onCancelDiscoverable: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onRetry: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Заголовок — обычный элемент списка, а не TopAppBar: при
                // прокрутке он уезжает вместе с контентом и не «плавает»
                // поверх экрана.
                item {
                    ScreenHeader()
                }
                item {
                    StatusCard(
                        mode = mode,
                        hidState = hidState,
                        irAvailable = irAvailable,
                        permissionGranted = permissionGranted,
                        bluetoothEnabled = bluetoothEnabled,
                        onMakeDiscoverable = onMakeDiscoverable,
                        onCancelDiscoverable = onCancelDiscoverable,
                        onRequestPermissions = onRequestPermissions,
                        onEnableBluetooth = onEnableBluetooth,
                        onRetry = onRetry,
                        onSend = onSend,
                    )
                }
                item {
                    ModeCard(
                        mode = mode,
                        irAvailable = irAvailable,
                        onModeChange = onModeChange,
                    )
                }
                item {
                    RemotePad(mode = mode, onSend = onSend)
                }
                item {
                    HelpCard(mode = mode, irAvailable = irAvailable)
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusCard(
    mode: RemoteMode,
    hidState: HidState,
    irAvailable: Boolean,
    permissionGranted: Boolean,
    bluetoothEnabled: Boolean,
    onMakeDiscoverable: () -> Unit,
    onCancelDiscoverable: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onRetry: () -> Unit,
    onSend: (RemoteKey) -> Unit,
) {
    val isBluetooth = mode == RemoteMode.BLUETOOTH
    val title = if (isBluetooth) "Bluetooth HID" else "ИК-пульт"

    val status = if (isBluetooth) {
        when (hidState) {
            HidState.Idle -> stringResource(R.string.status_idle)
            HidState.BluetoothOff -> stringResource(R.string.status_bluetooth_off)
            HidState.Unsupported -> stringResource(R.string.status_unsupported)
            HidState.PermissionRequired -> stringResource(R.string.status_permission)
            HidState.Registering -> stringResource(R.string.status_registering)
            HidState.Ready -> stringResource(R.string.status_ready)
            HidState.Discoverable -> stringResource(R.string.status_discoverable)
            is HidState.Connecting -> stringResource(R.string.status_connecting, hidState.name)
            is HidState.Connected -> stringResource(R.string.status_connected, hidState.name)
            is HidState.Error -> stringResource(R.string.status_error, hidState.message)
        }
    } else {
        if (irAvailable) {
            stringResource(R.string.status_ir_ready)
        } else {
            stringResource(R.string.status_ir_unavailable)
        }
    }

    val icon = if (isBluetooth) Icons.Rounded.Bluetooth else Icons.Rounded.Lightbulb
    val iconContainer = if (isBluetooth) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val iconColor = if (isBluetooth) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    val isBusy = isBluetooth &&
        (hidState is HidState.Registering || hidState is HidState.Connecting)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconContainer,
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = if (isBluetooth) "HID" else "IR",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    AnimatedContent(
                        targetState = status,
                        label = "hid-status",
                    ) { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            when {
                !isBluetooth -> {
                    if (irAvailable) {
                        FilledTonalButton(
                            onClick = { onSend(RemoteKey.POWER) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_test_ir))
                        }
                    }
                }

                !permissionGranted -> {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_allow))
                    }
                }

                hidState is HidState.Unsupported -> Unit

                !bluetoothEnabled || hidState is HidState.BluetoothOff -> {
                    Button(
                        onClick = onEnableBluetooth,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_enable_bluetooth))
                    }
                }

                hidState is HidState.PermissionRequired -> {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_allow))
                    }
                }

                hidState is HidState.Error -> {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_retry))
                    }
                }

                hidState is HidState.Ready -> {
                    FilledTonalButton(
                        onClick = onMakeDiscoverable,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_make_discoverable))
                    }
                }

                hidState is HidState.Discoverable -> {
                    FilledTonalButton(
                        onClick = onCancelDiscoverable,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_cancel_discoverable))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeCard(
    mode: RemoteMode,
    irAvailable: Boolean,
    onModeChange: (RemoteMode) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.mode_title),
                style = MaterialTheme.typography.titleMedium,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == RemoteMode.BLUETOOTH,
                    onClick = { onModeChange(RemoteMode.BLUETOOTH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = { Text(stringResource(R.string.mode_bluetooth)) },
                )
                SegmentedButton(
                    selected = mode == RemoteMode.INFRARED,
                    onClick = { onModeChange(RemoteMode.INFRARED) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    enabled = irAvailable,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = { Text(stringResource(R.string.mode_ir)) },
                )
            }

            Text(
                text = if (mode == RemoteMode.BLUETOOTH) {
                    stringResource(R.string.mode_bluetooth_description)
                } else {
                    stringResource(R.string.mode_ir_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (mode == RemoteMode.INFRARED && !irAvailable) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.mode_ir_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (mode == RemoteMode.INFRARED && irAvailable) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.mode_ir_limited),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RemotePad(
    mode: RemoteMode,
    onSend: (RemoteKey) -> Unit,
) {
    val isIr = mode == RemoteMode.INFRARED

    fun enabled(key: RemoteKey): Boolean = !isIr || key.ir != null

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.remote_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = if (isIr) {
                        stringResource(R.string.remote_subtitle_ir)
                    } else {
                        stringResource(R.string.remote_subtitle_hid)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteKeyButton(
                    key = RemoteKey.POWER,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.POWER),
                    size = 64.dp,
                )
                RemoteKeyButton(
                    key = RemoteKey.TV,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.TV),
                )
                RemoteKeyButton(
                    key = RemoteKey.SETTINGS,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.SETTINGS),
                )
                RemoteKeyButton(
                    key = RemoteKey.VOICE,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.VOICE),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.volume),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RemoteKeyButton(
                        key = RemoteKey.VOLUME_UP,
                        onClick = onSend,
                        enabled = enabled(RemoteKey.VOLUME_UP),
                        size = 46.dp,
                    )
                    RemoteKeyButton(
                        key = RemoteKey.MUTE,
                        onClick = onSend,
                        enabled = enabled(RemoteKey.MUTE),
                        size = 38.dp,
                    )
                    RemoteKeyButton(
                        key = RemoteKey.VOLUME_DOWN,
                        onClick = onSend,
                        enabled = enabled(RemoteKey.VOLUME_DOWN),
                        size = 46.dp,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RemoteKeyButton(
                        key = RemoteKey.DPAD_UP,
                        onClick = onSend,
                        enabled = enabled(RemoteKey.DPAD_UP),
                        size = 54.dp,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RemoteKeyButton(
                            key = RemoteKey.DPAD_LEFT,
                            onClick = onSend,
                            enabled = enabled(RemoteKey.DPAD_LEFT),
                            size = 54.dp,
                        )
                        RemoteKeyButton(
                            key = RemoteKey.OK,
                            onClick = onSend,
                            enabled = enabled(RemoteKey.OK),
                            size = 66.dp,
                        )
                        RemoteKeyButton(
                            key = RemoteKey.DPAD_RIGHT,
                            onClick = onSend,
                            enabled = enabled(RemoteKey.DPAD_RIGHT),
                            size = 54.dp,
                        )
                    }
                    RemoteKeyButton(
                        key = RemoteKey.DPAD_DOWN,
                        onClick = onSend,
                        enabled = enabled(RemoteKey.DPAD_DOWN),
                        size = 54.dp,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.channel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RemoteKeyButton(
                        key = RemoteKey.CHANNEL_UP,
                        onClick = onSend,
                        enabled = enabled(RemoteKey.CHANNEL_UP),
                        size = 46.dp,
                    )
                    RemoteKeyButton(
                        key = RemoteKey.CHANNEL_DOWN,
                        onClick = onSend,
                        enabled = enabled(RemoteKey.CHANNEL_DOWN),
                        size = 46.dp,
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = stringResource(R.string.section_navigation),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteKeyButton(
                    key = RemoteKey.BACK,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.BACK),
                )
                RemoteKeyButton(
                    key = RemoteKey.HOME,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.HOME),
                )
                RemoteKeyButton(
                    key = RemoteKey.MENU,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.MENU),
                )
                RemoteKeyButton(
                    key = RemoteKey.SEARCH,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.SEARCH),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = stringResource(R.string.section_media),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteKeyButton(
                    key = RemoteKey.REWIND,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.REWIND),
                    size = 50.dp,
                )
                RemoteKeyButton(
                    key = RemoteKey.PREVIOUS,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.PREVIOUS),
                    size = 50.dp,
                )
                RemoteKeyButton(
                    key = RemoteKey.PLAY_PAUSE,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.PLAY_PAUSE),
                    size = 56.dp,
                )
                RemoteKeyButton(
                    key = RemoteKey.NEXT,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.NEXT),
                    size = 50.dp,
                )
                RemoteKeyButton(
                    key = RemoteKey.FORWARD,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.FORWARD),
                    size = 50.dp,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteKeyButton(
                    key = RemoteKey.STOP,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.STOP),
                    size = 50.dp,
                )
                Spacer(Modifier.width(12.dp))
                RemoteKeyButton(
                    key = RemoteKey.RECORD,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.RECORD),
                    size = 50.dp,
                )
                Spacer(Modifier.width(12.dp))
                RemoteKeyButton(
                    key = RemoteKey.GUIDE,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.GUIDE),
                    size = 50.dp,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = stringResource(R.string.section_digits),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DigitRow(
                    digits = listOf(
                        RemoteKey.DIGIT_1,
                        RemoteKey.DIGIT_2,
                        RemoteKey.DIGIT_3,
                    ),
                    enabled = { key -> enabled(key) },
                    onSend = onSend,
                )
                DigitRow(
                    digits = listOf(
                        RemoteKey.DIGIT_4,
                        RemoteKey.DIGIT_5,
                        RemoteKey.DIGIT_6,
                    ),
                    enabled = { key -> enabled(key) },
                    onSend = onSend,
                )
                DigitRow(
                    digits = listOf(
                        RemoteKey.DIGIT_7,
                        RemoteKey.DIGIT_8,
                        RemoteKey.DIGIT_9,
                    ),
                    enabled = { key -> enabled(key) },
                    onSend = onSend,
                )
                RemoteKeyButton(
                    key = RemoteKey.DIGIT_0,
                    onClick = onSend,
                    enabled = enabled(RemoteKey.DIGIT_0),
                    size = 60.dp,
                    shape = RoundedCornerShape(20.dp),
                )
            }

            if (isIr) {
                Text(
                    text = stringResource(R.string.mode_ir_limited),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DigitRow(
    digits: List<RemoteKey>,
    enabled: (RemoteKey) -> Boolean,
    onSend: (RemoteKey) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        digits.forEach { digit ->
            RemoteKeyButton(
                key = digit,
                onClick = onSend,
                enabled = enabled(digit),
                size = 60.dp,
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

@Composable
private fun HelpCard(
    mode: RemoteMode,
    irAvailable: Boolean,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.help_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = if (expanded) {
                            stringResource(R.string.action_hide_help)
                        } else {
                            stringResource(R.string.action_help)
                        },
                    )
                }
            }

            if (mode == RemoteMode.BLUETOOTH) {
                Text(
                    text = stringResource(R.string.help_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.ir_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mode == RemoteMode.BLUETOOTH) {
                        HelpStep(1, stringResource(R.string.help_step_1))
                        HelpStep(2, stringResource(R.string.help_step_2))
                        HelpStep(3, stringResource(R.string.help_step_3))
                        HelpStep(4, stringResource(R.string.help_step_4))
                        HelpStep(5, stringResource(R.string.help_step_5))
                    } else if (!irAvailable) {
                        Text(
                            text = stringResource(R.string.mode_ir_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        HelpStep(1, stringResource(R.string.ir_step_1))
                        HelpStep(2, stringResource(R.string.ir_step_2))
                        HelpStep(3, stringResource(R.string.ir_step_3))
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpStep(number: Int, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
