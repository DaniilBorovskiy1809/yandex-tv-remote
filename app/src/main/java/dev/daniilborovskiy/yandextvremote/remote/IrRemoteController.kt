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
            ir.transmit(NecEncoder.CARRIER_HZ, NecEncoder.encode(command))
            true
        } catch (_: Exception) {
            false
        }
    }
}
