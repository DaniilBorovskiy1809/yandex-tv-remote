package dev.daniilborovskiy.yandextvremote.remote

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HidReportsTest {

    @Test
    fun `keyboard report puts usage in first key slot`() {
        assertArrayEquals(
            byteArrayOf(0, 0, 0x1E, 0, 0, 0, 0, 0),
            HidReports.keyboard(0x1E),
        )
    }

    @Test
    fun `consumer report is 16-bit little endian`() {
        assertArrayEquals(byteArrayOf(0x30, 0x00), HidReports.consumer(0x30))
        assertArrayEquals(byteArrayOf(0x24, 0x02), HidReports.consumer(0x224))
        assertArrayEquals(byteArrayOf(0xFF.toByte(), 0x03), HidReports.consumer(0x3FF))
    }

    @Test
    fun `release reports are all zero`() {
        assertArrayEquals(ByteArray(8), HidReports.KEYBOARD_RELEASE)
        assertArrayEquals(byteArrayOf(0, 0), HidReports.CONSUMER_RELEASE)
    }

    @Test
    fun `report ids match the HID descriptor`() {
        assertEquals(1, HidReports.KEYBOARD_REPORT_ID)
        assertEquals(2, HidReports.CONSUMER_REPORT_ID)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `keyboard usage above descriptor maximum is rejected`() {
        HidReports.keyboard(HidReports.KEYBOARD_USAGE_MAX + 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `consumer usage above descriptor maximum is rejected`() {
        HidReports.consumer(HidReports.CONSUMER_USAGE_MAX + 1)
    }

    @Test
    fun `descriptor maximum usages are accepted`() {
        assertArrayEquals(
            byteArrayOf(0, 0, 0x65, 0, 0, 0, 0, 0),
            HidReports.keyboard(HidReports.KEYBOARD_USAGE_MAX),
        )
        assertArrayEquals(
            byteArrayOf(0xFF.toByte(), 0x03),
            HidReports.consumer(HidReports.CONSUMER_USAGE_MAX),
        )
    }
}
