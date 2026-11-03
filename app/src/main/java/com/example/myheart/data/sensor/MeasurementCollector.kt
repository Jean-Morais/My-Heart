package com.example.myheart.data.sensor

import kotlin.math.min
import kotlin.math.roundToInt

/** Resultado consolidado de uma coleta: um par de valores pronto para virar uma leitura. */
data class CollectedMeasurement(val bpm: Int, val spo2: Int)

/**
 * Consolida o fluxo de amostras de um sensor em uma única medição.
 *
 * Regras:
 * - valores fora da faixa plausível são descartados (não são erro, o sensor pode estar acomodando);
 * - batimentos e oxigenação são contados em separado, pois podem chegar em pacotes diferentes;
 * - só as últimas [requiredSamples] amostras de cada medida contam, o que descarta a fase
 *   inicial de estabilização do sensor;
 * - a coleta termina quando as duas medidas têm [requiredSamples] amostras; o resultado é a
 *   mediana de cada uma, que resiste a valores isolados fora do padrão;
 * - aviso de dedo removido zera a coleta, para não misturar duas posições do dedo.
 *
 * Classe pura: sem Android e sem tempo, o que a torna simples de testar.
 */
class MeasurementCollector(
    private val requiredSamples: Int = DEFAULT_REQUIRED_SAMPLES,
) {
    init {
        require(requiredSamples > 0) { "requiredSamples deve ser maior que zero" }
    }

    private val bpmValues = mutableListOf<Int>()
    private val spo2Values = mutableListOf<Int>()

    /** Avanço da coleta, de 0 a 1: limitado pela medida que tem menos amostras. */
    val progress: Float
        get() = min(bpmValues.size, spo2Values.size).toFloat() / requiredSamples

    val isComplete: Boolean
        get() = bpmValues.size >= requiredSamples && spo2Values.size >= requiredSamples

    fun add(sample: DecodedSample) {
        if (sample.fingerPresent == false) {
            reset()
            return
        }
        sample.bpm?.takeIf { it in BPM_RANGE }?.let { push(bpmValues, it) }
        sample.spo2?.takeIf { it in SPO2_RANGE }?.let { push(spo2Values, it) }
    }

    /** Medição consolidada, ou nulo enquanto a coleta não estiver completa. */
    fun result(): CollectedMeasurement? =
        if (isComplete) CollectedMeasurement(median(bpmValues), median(spo2Values)) else null

    fun reset() {
        bpmValues.clear()
        spo2Values.clear()
    }

    private fun push(values: MutableList<Int>, value: Int) {
        values.add(value)
        while (values.size > requiredSamples) values.removeAt(0)
    }

    private fun median(values: List<Int>): Int {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            ((sorted[middle - 1] + sorted[middle]) / 2.0).roundToInt()
        }
    }

    companion object {
        const val DEFAULT_REQUIRED_SAMPLES = 5

        // Faixas amplas e genéricas, só para filtrar lixo; a classificação clínica ilustrativa
        // continua sendo feita pelo AnalyzeReadingUseCase.
        private val BPM_RANGE = 30..250
        private val SPO2_RANGE = 50..100
    }
}
