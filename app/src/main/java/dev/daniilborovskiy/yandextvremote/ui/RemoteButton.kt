package dev.daniilborovskiy.yandextvremote.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.daniilborovskiy.yandextvremote.remote.KeyTone
import dev.daniilborovskiy.yandextvremote.remote.RemoteKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun RemoteKeyButton(
    key: RemoteKey,
    onClick: (RemoteKey) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 56.dp,
    shape: Shape = CircleShape,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(key.labelRes)

    var pressed by remember { mutableStateOf(false) }
    var activePress by remember { mutableStateOf<PressInteraction.Press?>(null) }
    var repeatJob by remember { mutableStateOf<Job?>(null) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "remote-key-scale",
    )

    val containerColor = when (key.tone) {
        KeyTone.Normal -> MaterialTheme.colorScheme.surfaceContainerHighest
        KeyTone.Accent -> MaterialTheme.colorScheme.primaryContainer
        KeyTone.Danger -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (key.tone) {
        KeyTone.Normal -> MaterialTheme.colorScheme.onSurface
        KeyTone.Accent -> MaterialTheme.colorScheme.onPrimaryContainer
        KeyTone.Danger -> MaterialTheme.colorScheme.onErrorContainer
    }

    val rippleIndication = ripple()

    Surface(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .indication(interactionSource, rippleIndication)
            .pointerInput(key, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    pressed = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                    val press = PressInteraction.Press(down.position)
                    activePress = press
                    interactionSource.emit(press)

                    repeatJob?.cancel()
                    repeatJob = scope.launch {
                        onClick(key)
                        if (key.repeatable) {
                            delay(REPEAT_DELAY_MS)
                            while (isActive) {
                                onClick(key)
                                delay(REPEAT_INTERVAL_MS)
                            }
                        }
                    }

                    waitForUpOrCancellation()

                    repeatJob?.cancel()
                    repeatJob = null
                    pressed = false
                    activePress?.let { interactionSource.emit(PressInteraction.Release(it)) }
                    activePress = null
                }
            }
            .semantics {
                contentDescription = label
                role = Role.Button
                if (enabled) {
                    onClick(label = label) {
                        onClick(key)
                        true
                    }
                } else {
                    disabled()
                }
            },
        shape = shape,
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
        border = if (enabled) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        } else {
            null
        },
        shadowElevation = if (pressed) 0.dp else 2.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                key.text != null -> Text(
                    text = key.text,
                    style = if (key.text.length > 1) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                )

                key.icon != null -> Icon(
                    imageVector = key.icon,
                    contentDescription = label,
                    modifier = Modifier.size(size * 0.42f),
                )
            }
        }
    }
}

private const val REPEAT_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 110L
