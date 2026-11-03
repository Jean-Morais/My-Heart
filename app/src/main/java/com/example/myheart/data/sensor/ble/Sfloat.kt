package com.example.myheart.data.sensor.ble

import kotlin.math.pow

/**
 * SFLOAT do IEEE 11073: ponto flutuante de 16 bits, com 4 bits de expoente e 12 bits de
 * mantissa, ambos com sinal. Valor = mantissa x 10^expoente.
 */
object Sfloat {
    // NaN, NRes, reservado, +INF e -INF: nenhum deles é uma medida utilizável.
    private val SPECIAL_VALUES = setOf(0x07FF, 0x0800, 0x0801, 0x07FE, 0x0802)

    /** Decodifica os 16 bits; devolve nulo para os valores especiais ("sem leitura válida"). */
    fun decode(raw: Int): Double? {
        val value = raw and 0xFFFF
        if (value in SPECIAL_VALUES) return null

        var mantissa = value and 0x0FFF
        if (mantissa >= 0x0800) mantissa -= 0x1000
        var exponent = value ushr 12
        if (exponent >= 0x8) exponent -= 0x10

        return mantissa * 10.0.pow(exponent)
    }

    /** Lê dois bytes em little-endian a partir de [offset] e decodifica; nulo se faltarem bytes. */
    fun decodeAt(data: ByteArray, offset: Int): Double? {
        if (offset < 0 || offset + 1 >= data.size) return null
        val raw = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        return decode(raw)
    }
}
