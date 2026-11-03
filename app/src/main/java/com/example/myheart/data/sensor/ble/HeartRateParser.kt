package com.example.myheart.data.sensor.ble

import com.example.myheart.data.sensor.DecodedSample

/** Decodifica a característica Heart Rate Measurement (0x2A37) do Heart Rate Service. */
object HeartRateParser {
    private const val FLAG_VALUE_16_BITS = 0x01
    private const val FLAG_CONTACT_DETECTED = 0x02
    private const val FLAG_CONTACT_SUPPORTED = 0x04

    /** Devolve nulo se o pacote estiver truncado. BPM igual a zero vira "sem valor". */
    fun parse(data: ByteArray): DecodedSample? {
        if (data.size < 2) return null
        val flags = data[0].toInt() and 0xFF

        val is16Bits = (flags and FLAG_VALUE_16_BITS) != 0
        val bpm = if (is16Bits) {
            if (data.size < 3) return null
            (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        } else {
            data[1].toInt() and 0xFF
        }

        // Energia gasta e intervalos RR, quando presentes, vêm depois do BPM e não são usados.
        val contactSupported = (flags and FLAG_CONTACT_SUPPORTED) != 0
        val contact = if (contactSupported) (flags and FLAG_CONTACT_DETECTED) != 0 else null

        return DecodedSample(bpm = bpm.takeIf { it > 0 }, fingerPresent = contact)
    }
}
