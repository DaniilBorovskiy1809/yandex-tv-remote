package dev.daniilborovskiy.yandextvremote.remote

/**
 * Pure NEC infrared frame encoder.
 *
 * Keeping this separate from [IrRemoteController] makes the timing logic
 * testable on the JVM without an Android device or IR hardware.
 */
object NecEncoder {

    const val CARRIER_HZ = 38_000
    const val DEFAULT_ADDRESS = 0x20DF

    const val LEADER_MARK_US = 9_000
    const val LEADER_SPACE_US = 4_500
    const val BIT_MARK_US = 560
    const val ZERO_SPACE_US = 560
    const val ONE_SPACE_US = 1_690

    const val FRAME_BITS = 32
    const val PATTERN_SIZE = 2 + FRAME_BITS * 2 + 1

    /**
     * Encodes one NEC frame as mark/space durations in microseconds.
     *
     * Byte order is address low, address high, command, inverse command.
     * Every byte is transmitted LSB first.
     */
    fun encode(command: Int, address: Int = DEFAULT_ADDRESS): IntArray {
        require(command in 0x00..0xFF) { "NEC command must be one byte: $command" }
        require(address in 0x0000..0xFFFF) { "NEC address must be two bytes: $address" }

        val bytes = intArrayOf(
            address and 0xFF,
            (address shr 8) and 0xFF,
            command and 0xFF,
            command.inv() and 0xFF,
        )

        val pattern = IntArray(PATTERN_SIZE)
        pattern[0] = LEADER_MARK_US
        pattern[1] = LEADER_SPACE_US

        var index = 2
        for (byteIndex in bytes.indices) {
            val value = bytes[byteIndex]
            for (bitIndex in 0 until 8) {
                val bit = (value shr bitIndex) and 1
                pattern[index++] = BIT_MARK_US
                pattern[index++] = if (bit == 1) ONE_SPACE_US else ZERO_SPACE_US
            }
        }
        pattern[index] = BIT_MARK_US
        return pattern
    }
}
