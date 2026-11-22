package dev.daniilborovskiy.yandextvremote.remote

import android.content.Context
import android.hardware.ConsumerIrManager

/**
 * Sends NEC IR frames using the phone's built-in IR blaster.
 *
 * The Yandex monet firmware has two IR tables enabled in remote.sum:
 *   - RCA tables (active, custom_code = 0x0f)
 *   - NEC tables (fallback, custom_code = 0xdf20)
 *
 * The RCA frame format is not publicly documented, but the NEC table is
 * included in the same active configuration. The NEC address below is derived
 * from the wake-up code 0xad52df20: 0xAD (~0x52) 0x52 (command) 0xDF 0x20
 * (address, LSB first).
 */
class IrRemoteController(context: Context) {

    private val manager: ConsumerIrManager? =
        context.applicationContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val isAvailable: Boolean = try {
        manager?.hasIrEmitter() == true
    } catch (_: Exception) {
        false
    }

    fun send(command: Int): Boolean {
        val ir = manager ?: return false
        if (!isAvailable) return false

        return try {
            ir.transmit(CARRIER_HZ, necPattern(NEC_ADDRESS, command))
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun necPattern(address: Int, command: Int): IntArray {
        val bytes = intArrayOf(
            address and 0xff,
            (address shr 8) and 0xff,
            command and 0xff,
            command.inv() and 0xff,
        )

        val pattern = IntArray(2 + 32 * 2 + 1)
        pattern[0] = NEC_LEADER_MARK_US
        pattern[1] = NEC_LEADER_SPACE_US

        var index = 2
        for (byteIndex in 0 until 4) {
            val value = bytes[byteIndex]
            for (bitIndex in 0 until 8) {
                val bit = (value shr bitIndex) and 1
                pattern[index++] = NEC_BIT_MARK_US
                pattern[index++] = if (bit == 1) NEC_ONE_SPACE_US else NEC_ZERO_SPACE_US
            }
        }
        pattern[index] = NEC_BIT_MARK_US
        return pattern
    }

    companion object {
        private const val CARRIER_HZ = 38_000
        private const val NEC_ADDRESS = 0x20df
        private const val NEC_LEADER_MARK_US = 9_000
        private const val NEC_LEADER_SPACE_US = 4_500
        private const val NEC_BIT_MARK_US = 560
        private const val NEC_ZERO_SPACE_US = 560
        private const val NEC_ONE_SPACE_US = 1_690
    }
}
