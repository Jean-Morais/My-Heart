package com.example.myheart.domain.usecase

import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus

/**
 * Classifica uma leitura em Normal, Atenção ou Crítico.
 * Os limiares são ilustrativos (projeto acadêmico), não indicação clínica.
 *
 * O nível geral é o pior entre o nível dos batimentos e o da oxigenação. As regras são as mesmas
 * de antes; só foram separadas por medida para o gráfico poder colorir cada barra pelo valor exibido.
 */
class AnalyzeReadingUseCase {

    /** Nível geral da leitura. */
    operator fun invoke(reading: Reading): ReadingStatus =
        // ReadingStatus está declarado do melhor para o pior (Normal, Attention, Critical).
        maxOf(bpmStatus(reading.bpm), spo2Status(reading.spo2))

    fun bpmStatus(bpm: Int): ReadingStatus = when {
        bpm !in 40..130 -> ReadingStatus.Critical
        bpm !in 60..100 -> ReadingStatus.Attention
        else -> ReadingStatus.Normal
    }

    fun spo2Status(spo2: Int): ReadingStatus = when {
        spo2 < 90 -> ReadingStatus.Critical
        spo2 < 95 -> ReadingStatus.Attention
        else -> ReadingStatus.Normal
    }

    /** Leitura com o nível geral e o de cada medida. */
    fun analyze(reading: Reading): AnalyzedReading = AnalyzedReading(
        reading = reading,
        status = invoke(reading),
        bpmStatus = bpmStatus(reading.bpm),
        spo2Status = spo2Status(reading.spo2),
    )
}
