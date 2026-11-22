package dev.daniilborovskiy.yandextvremote.remote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class NecEncoderTest {

    @Test
    fun `power frame has NEC leader and 67 entries`() {
        val pattern = NecEncoder.encode(0x52)

        assertEquals(NecEncoder.PATTERN_SIZE, pattern.size)
        assertEquals(NecEncoder.LEADER_MARK_US, pattern[0])
        assertEquals(NecEncoder.LEADER_SPACE_US, pattern[1])
        assertEquals(NecEncoder.BIT_MARK_US, pattern.last())
    }

    @Test
    fun `power frame encodes address command and inverse LSB first`() {
        val bytes = decode(NecEncoder.encode(0x52))

        assertArrayEquals(intArrayOf(0xDF, 0x20, 0x52, 0xAD), bytes)
    }

    @Test
    fun `zero command has 0xFF inverse`() {
        val bytes = decode(NecEncoder.encode(0x00))

        assertArrayEquals(intArrayOf(0xDF, 0x20, 0x00, 0xFF), bytes)
    }

    @Test
    fun `custom address is encoded little endian`() {
        val bytes = decode(NecEncoder.encode(0x10, address = 0x1234))

        assertArrayEquals(intArrayOf(0x34, 0x12, 0x10, 0xEF), bytes)
    }

    @Test
    fun `every IR key in RemoteKey produces a valid NEC frame`() {
        RemoteKey.entries.mapNotNull { it.ir }.forEach { command ->
            val bytes = decode(NecEncoder.encode(command))

            assertEquals("address low for 0x$command", 0xDF, bytes[0])
            assertEquals("address high for 0x$command", 0x20, bytes[1])
            assertEquals("command for 0x$command", command, bytes[2])
            assertEquals("inverse command for 0x$command", command xor 0xFF, bytes[3])
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `command wider than one byte is rejected`() {
        NecEncoder.encode(0x100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative command is rejected`() {
        NecEncoder.encode(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `address wider than two bytes is rejected`() {
        NecEncoder.encode(0x52, address = 0x10000)
    }

    private fun decode(pattern: IntArray): IntArray {
        assertEquals(NecEncoder.PATTERN_SIZE, pattern.size)
        assertEquals(NecEncoder.LEADER_MARK_US, pattern[0])
        assertEquals(NecEncoder.LEADER_SPACE_US, pattern[1])

        val bytes = IntArray(4)
        var index = 2

        for (byteIndex in bytes.indices) {
            var value = 0
            for (bitIndex in 0 until 8) {
                assertEquals("bit mark at $index", NecEncoder.BIT_MARK_US, pattern[index++])
                when (val space = pattern[index++]) {
                    NecEncoder.ZERO_SPACE_US -> Unit
                    NecEncoder.ONE_SPACE_US -> value = value or (1 shl bitIndex)
                    else -> fail("Unexpected space duration $space at ${index - 1}")
                }
            }
            bytes[byteIndex] = value
        }

        assertEquals("final mark index", NecEncoder.PATTERN_SIZE - 1, index)
        assertEquals(NecEncoder.BIT_MARK_US, pattern[index])
        return bytes
    }
}
