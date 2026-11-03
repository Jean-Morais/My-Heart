package com.example.myheart.data.sensor.ble

import com.example.myheart.data.sensor.DecodedSample
import kotlin.math.roundToInt

/**
 * Decodifica as medidas do Pulse Oximeter Service: PLX Continuous (0x2A5F) e
 * PLX Spot-check (0x2A5E). Nas duas, os bytes 1 a 4 trazem SpO2 e pulso em SFLOAT; os campos
 * opcionais que vêm depois (timestamp, status etc.) não são usados.
 *
 * NaN em qualquer dos dois valores significa "sem leitura válida" e vira nulo.
 */
object PulseOximeterParser {
    private const val MIN_SIZE = 5

    /** Devolve nulo se o pacote estiver truncado. */
    fun parse(data: ByteArray): DecodedSample? {
        if (data.size < MIN_SIZE) return null
        val spo2 = Sfloat.decodeAt(data, 1)
        val pulse = Sfloat.decodeAt(data, 3)
        return DecodedSample(
            bpm = pulse?.roundToInt(),
            spo2 = spo2?.roundToInt(),
        )
    }
}
