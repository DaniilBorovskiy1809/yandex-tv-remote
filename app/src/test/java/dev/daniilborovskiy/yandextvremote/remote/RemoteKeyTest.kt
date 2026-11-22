package dev.daniilborovskiy.yandextvremote.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeyTest {

    @Test
    fun `every key has a label and a HID command`() {
        RemoteKey.entries.forEach { key ->
            assertTrue("${key.name} has no label", key.labelRes != 0)
            assertNotNull("${key.name} has no HID command", key.hid)
        }
    }

    @Test
    fun `IR commands are one-byte NEC values with good coverage`() {
        val irCodes = RemoteKey.entries.mapNotNull { it.ir }

        assertTrue("expected at least 20 IR codes, got ${irCodes.size}", irCodes.size >= 20)
        assertTrue("expected at least 20 distinct IR codes", irCodes.toSet().size >= 20)
        irCodes.forEach { command ->
            assertTrue("IR command 0x${command.toString(16)} is not a byte", command in 0x00..0xFF)
        }
    }

    @Test
    fun `consumer usages fit the 10-bit descriptor range`() {
        RemoteKey.entries
            .mapNotNull { it.hid }
            .filterIsInstance<HidCommand.Consumer>()
            .forEach { command ->
                assertTrue(
                    "${command.usage} is outside 0..${HidReports.CONSUMER_USAGE_MAX}",
                    command.usage in 0x0000..HidReports.CONSUMER_USAGE_MAX,
                )
            }
    }

    @Test
    fun `keyboard usages fit the descriptor range`() {
        RemoteKey.entries
            .mapNotNull { it.hid }
            .filterIsInstance<HidCommand.Keyboard>()
            .forEach { command ->
                assertTrue(
                    "${command.usage} is outside 0..${HidReports.KEYBOARD_USAGE_MAX}",
                    command.usage in 0x00..HidReports.KEYBOARD_USAGE_MAX,
                )
            }
    }

    @Test
    fun `critical mappings match the firmware tables`() {
        assertEquals(HidCommand.Consumer(0x30), RemoteKey.POWER.hid)
        assertEquals(0x52, RemoteKey.POWER.ir)

        assertEquals(HidCommand.Consumer(0x224), RemoteKey.BACK.hid)
        assertEquals(HidCommand.Consumer(0x223), RemoteKey.HOME.hid)
        assertEquals(HidCommand.Consumer(0x41), RemoteKey.OK.hid)
        assertEquals(0x4A, RemoteKey.OK.ir)

        assertEquals(HidCommand.Consumer(0x9C), RemoteKey.CHANNEL_UP.hid)
        assertEquals(0x02, RemoteKey.CHANNEL_UP.ir)
        assertEquals(HidCommand.Consumer(0x9D), RemoteKey.CHANNEL_DOWN.hid)
        assertEquals(0x09, RemoteKey.CHANNEL_DOWN.ir)

        assertEquals(HidCommand.Keyboard(0x1E), RemoteKey.DIGIT_1.hid)
        assertEquals(0x00, RemoteKey.DIGIT_1.ir)
        assertEquals(HidCommand.Keyboard(0x27), RemoteKey.DIGIT_0.hid)
        assertEquals(0x1B, RemoteKey.DIGIT_0.ir)
    }

    @Test
    fun `keys without an IR code are explicitly marked`() {
        assertNull(RemoteKey.TV.ir)
        assertNull(RemoteKey.VOICE.ir)
        assertNull(RemoteKey.PLAY_PAUSE.ir)
        assertNull(RemoteKey.GUIDE.ir)
    }

    @Test
    fun `repeatable set matches the remote UX`() {
        val expected = setOf(
            RemoteKey.DPAD_UP,
            RemoteKey.DPAD_DOWN,
            RemoteKey.DPAD_LEFT,
            RemoteKey.DPAD_RIGHT,
            RemoteKey.VOLUME_UP,
            RemoteKey.VOLUME_DOWN,
            RemoteKey.CHANNEL_UP,
            RemoteKey.CHANNEL_DOWN,
            RemoteKey.PREVIOUS,
            RemoteKey.NEXT,
            RemoteKey.REWIND,
            RemoteKey.FORWARD,
        )

        assertEquals(expected, RemoteKey.entries.filter { it.repeatable }.toSet())
    }

    @Test
    fun `all digits use keyboard page`() {
        val digits = listOf(
            RemoteKey.DIGIT_0,
            RemoteKey.DIGIT_1,
            RemoteKey.DIGIT_2,
            RemoteKey.DIGIT_3,
            RemoteKey.DIGIT_4,
            RemoteKey.DIGIT_5,
            RemoteKey.DIGIT_6,
            RemoteKey.DIGIT_7,
            RemoteKey.DIGIT_8,
            RemoteKey.DIGIT_9,
        )

        digits.forEach { digit ->
            assertTrue("${digit.name} is not a keyboard usage", digit.hid is HidCommand.Keyboard)
            assertNotNull("${digit.name} has no IR code", digit.ir)
        }
    }
}
