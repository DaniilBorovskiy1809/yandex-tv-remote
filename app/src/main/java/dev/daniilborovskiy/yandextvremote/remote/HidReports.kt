package dev.daniilborovskiy.yandextvremote.remote

/**
 * Encoders for the two HID reports used by the remote.
 *
 * Report 1 is a boot-compatible keyboard report. Report 2 is a 16-bit
 * Consumer Control array. Both encoders are pure and unit-testable.
 */
object HidReports {

    const val KEYBOARD_REPORT_ID = 1
    const val CONSUMER_REPORT_ID = 2

    /** Logical maximum from the keyboard collection in the HID descriptor. */
    const val KEYBOARD_USAGE_MAX = 0x65

    /** Logical maximum from the consumer collection in the HID descriptor. */
    const val CONSUMER_USAGE_MAX = 0x3FF

    val KEYBOARD_RELEASE: ByteArray = ByteArray(8)
    val CONSUMER_RELEASE: ByteArray = byteArrayOf(0, 0)

    fun keyboard(usage: Int): ByteArray {
        require(usage in 0x00..KEYBOARD_USAGE_MAX) {
            "Keyboard usage must be in 0..$KEYBOARD_USAGE_MAX: $usage"
        }
        return byteArrayOf(
            0,
            0,
            usage.toByte(),
            0,
            0,
            0,
            0,
            0,
        )
    }

    fun consumer(usage: Int): ByteArray {
        require(usage in 0x0000..CONSUMER_USAGE_MAX) {
            "Consumer usage must be in 0..$CONSUMER_USAGE_MAX: $usage"
        }
        return byteArrayOf(
            (usage and 0xFF).toByte(),
            ((usage shr 8) and 0xFF).toByte(),
        )
    }
}
