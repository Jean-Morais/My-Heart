package com.example.myheart.domain.model

/** Dispositivo Bluetooth pareado (por enquanto, simulado). */
data class Device(
    val name: String,
    val address: String,
)

/**
 * Uma leitura cardíaca. [timestamp] em milissegundos (epoch).
 *
 * [isSimulated] registra se o valor veio do sensor simulado, para o histórico nunca passar
 * demonstração por medição real. [id] é gerado pelo banco; vale 0 enquanto a leitura ainda não foi gravada.
 */
data class Reading(
    val bpm: Int,
    val spo2: Int,
    val timestamp: Long,
    val isSimulated: Boolean = false,
    val id: Long = 0,
)

enum class ReadingStatus { Normal, Attention, Critical }

/**
 * Uma leitura com os níveis já calculados: [status] é o nível geral (o pior entre as duas medidas)
 * e [bpmStatus] e [spo2Status] são o nível de cada medida, usados para colorir o gráfico.
 */
data class AnalyzedReading(
    val reading: Reading,
    val status: ReadingStatus,
    val bpmStatus: ReadingStatus,
    val spo2Status: ReadingStatus,
)
