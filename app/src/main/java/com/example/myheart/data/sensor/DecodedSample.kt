package com.example.myheart.data.sensor

/**
 * Amostra já decodificada de um pacote do sensor, independente do perfil ou do fabricante.
 *
 * Cada parser (Bluetooth padrão hoje; Samsung, Apple e Xiaomi no futuro) converte os bytes do seu
 * protocolo para este modelo. Campo nulo significa "sem valor válido". Não confundir com
 * [SensorSample], que é o resultado final de uma leitura completa.
 */
data class DecodedSample(
    val bpm: Int? = null,
    val spo2: Int? = null,
    /** Verdadeiro/falso quando o aparelho informa o contato com a pele; nulo quando não informa. */
    val fingerPresent: Boolean? = null,
)
