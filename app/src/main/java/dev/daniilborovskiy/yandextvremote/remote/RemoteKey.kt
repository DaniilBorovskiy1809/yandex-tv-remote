package dev.daniilborovskiy.yandextvremote.remote

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector
import dev.daniilborovskiy.yandextvremote.R

/**
 * A command understood by the phone remote.
 *
 * HID commands use the standard USB HID Consumer Page / Keyboard Page so the
 * TV can decode them with its stock Generic.kl. IR commands are NEC command
 * bytes from /vendor/etc/remote_nec_ircus1.tab (custom code 0xdf20).
 */
enum class RemoteKey(
    @StringRes val labelRes: Int,
    val icon: ImageVector? = null,
    val text: String? = null,
    val hid: HidCommand? = null,
    val ir: Int? = null,
    val repeatable: Boolean = false,
    val tone: KeyTone = KeyTone.Normal,
) {
    POWER(
        labelRes = R.string.key_power,
        icon = Icons.Rounded.PowerSettingsNew,
        hid = HidCommand.Consumer(0x30),
        ir = 0x52,
        tone = KeyTone.Danger,
    ),
    TV(
        labelRes = R.string.key_tv,
        icon = Icons.Rounded.Tv,
        hid = HidCommand.Consumer(0x89),
    ),
    SETTINGS(
        labelRes = R.string.key_settings,
        icon = Icons.Rounded.Settings,
        hid = HidCommand.Consumer(0x40),
        ir = 0x0d,
    ),
    VOICE(
        labelRes = R.string.key_voice,
        icon = Icons.Rounded.Mic,
        hid = HidCommand.Consumer(0xcf),
        tone = KeyTone.Accent,
    ),

    DPAD_UP(
        labelRes = R.string.key_up,
        icon = Icons.Rounded.KeyboardArrowUp,
        hid = HidCommand.Consumer(0x42),
        ir = 0x47,
        repeatable = true,
    ),
    DPAD_DOWN(
        labelRes = R.string.key_down,
        icon = Icons.Rounded.KeyboardArrowDown,
        hid = HidCommand.Consumer(0x43),
        ir = 0x4d,
        repeatable = true,
    ),
    DPAD_LEFT(
        labelRes = R.string.key_left,
        icon = Icons.Rounded.KeyboardArrowLeft,
        hid = HidCommand.Consumer(0x44),
        ir = 0x49,
        repeatable = true,
    ),
    DPAD_RIGHT(
        labelRes = R.string.key_right,
        icon = Icons.Rounded.KeyboardArrowRight,
        hid = HidCommand.Consumer(0x45),
        ir = 0x4b,
        repeatable = true,
    ),
    OK(
        labelRes = R.string.key_ok,
        text = "OK",
        hid = HidCommand.Consumer(0x41),
        ir = 0x4a,
        tone = KeyTone.Accent,
    ),

    BACK(
        labelRes = R.string.key_back,
        icon = Icons.Rounded.ArrowBack,
        hid = HidCommand.Consumer(0x224),
        ir = 0x40,
    ),
    HOME(
        labelRes = R.string.key_home,
        icon = Icons.Rounded.Home,
        hid = HidCommand.Consumer(0x223),
        ir = 0x48,
    ),
    MENU(
        labelRes = R.string.key_menu,
        icon = Icons.Rounded.Menu,
        hid = HidCommand.Consumer(0x40),
        ir = 0x0d,
    ),

    VOLUME_UP(
        labelRes = R.string.key_volume_up,
        icon = Icons.Rounded.VolumeUp,
        hid = HidCommand.Consumer(0xe9),
        ir = 0x03,
        repeatable = true,
    ),
    VOLUME_DOWN(
        labelRes = R.string.key_volume_down,
        icon = Icons.Rounded.VolumeDown,
        hid = HidCommand.Consumer(0xea),
        ir = 0x41,
        repeatable = true,
    ),
    MUTE(
        labelRes = R.string.key_mute,
        icon = Icons.Rounded.VolumeOff,
        hid = HidCommand.Consumer(0xe2),
        ir = 0x53,
    ),
    CHANNEL_UP(
        labelRes = R.string.key_channel_up,
        icon = Icons.Rounded.Add,
        hid = HidCommand.Consumer(0x9c),
        ir = 0x02,
        repeatable = true,
    ),
    CHANNEL_DOWN(
        labelRes = R.string.key_channel_down,
        icon = Icons.Rounded.Remove,
        hid = HidCommand.Consumer(0x9d),
        ir = 0x09,
        repeatable = true,
    ),

    PLAY_PAUSE(
        labelRes = R.string.key_play_pause,
        icon = Icons.Rounded.PlayArrow,
        hid = HidCommand.Consumer(0xcd),
    ),
    PREVIOUS(
        labelRes = R.string.key_previous,
        icon = Icons.Rounded.SkipPrevious,
        hid = HidCommand.Consumer(0xb6),
        repeatable = true,
    ),
    NEXT(
        labelRes = R.string.key_next,
        icon = Icons.Rounded.SkipNext,
        hid = HidCommand.Consumer(0xb5),
        repeatable = true,
    ),
    REWIND(
        labelRes = R.string.key_rewind,
        icon = Icons.Rounded.FastRewind,
        hid = HidCommand.Consumer(0xb4),
        repeatable = true,
    ),
    FORWARD(
        labelRes = R.string.key_forward,
        icon = Icons.Rounded.FastForward,
        hid = HidCommand.Consumer(0xb3),
        repeatable = true,
    ),
    STOP(
        labelRes = R.string.key_stop,
        icon = Icons.Rounded.Stop,
        hid = HidCommand.Consumer(0xb7),
    ),
    RECORD(
        labelRes = R.string.key_record,
        icon = Icons.Rounded.FiberManualRecord,
        hid = HidCommand.Consumer(0xb2),
    ),
    GUIDE(
        labelRes = R.string.key_guide,
        icon = Icons.Rounded.LiveTv,
        hid = HidCommand.Consumer(0x8d),
    ),
    SEARCH(
        labelRes = R.string.key_search,
        icon = Icons.Rounded.Search,
        hid = HidCommand.Consumer(0x221),
        ir = 0x1c,
    ),

    DIGIT_0(
        labelRes = R.string.key_digit_0,
        text = "0",
        hid = HidCommand.Keyboard(0x27),
        ir = 0x1b,
    ),
    DIGIT_1(
        labelRes = R.string.key_digit_1,
        text = "1",
        hid = HidCommand.Keyboard(0x1e),
        ir = 0x00,
    ),
    DIGIT_2(
        labelRes = R.string.key_digit_2,
        text = "2",
        hid = HidCommand.Keyboard(0x1f),
        ir = 0x10,
    ),
    DIGIT_3(
        labelRes = R.string.key_digit_3,
        text = "3",
        hid = HidCommand.Keyboard(0x20),
        ir = 0x11,
    ),
    DIGIT_4(
        labelRes = R.string.key_digit_4,
        text = "4",
        hid = HidCommand.Keyboard(0x21),
        ir = 0x13,
    ),
    DIGIT_5(
        labelRes = R.string.key_digit_5,
        text = "5",
        hid = HidCommand.Keyboard(0x22),
        ir = 0x14,
    ),
    DIGIT_6(
        labelRes = R.string.key_digit_6,
        text = "6",
        hid = HidCommand.Keyboard(0x23),
        ir = 0x15,
    ),
    DIGIT_7(
        labelRes = R.string.key_digit_7,
        text = "7",
        hid = HidCommand.Keyboard(0x24),
        ir = 0x17,
    ),
    DIGIT_8(
        labelRes = R.string.key_digit_8,
        text = "8",
        hid = HidCommand.Keyboard(0x25),
        ir = 0x18,
    ),
    DIGIT_9(
        labelRes = R.string.key_digit_9,
        text = "9",
        hid = HidCommand.Keyboard(0x26),
        ir = 0x19,
    ),
}

sealed interface HidCommand {
    /** A 16-bit usage from the HID Consumer Page (0x0C). */
    data class Consumer(val usage: Int) : HidCommand

    /** An 8-bit usage from the HID Keyboard/Keypad Page (0x07). */
    data class Keyboard(val usage: Int) : HidCommand
}

enum class KeyTone {
    Normal,
    Accent,
    Danger,
}
