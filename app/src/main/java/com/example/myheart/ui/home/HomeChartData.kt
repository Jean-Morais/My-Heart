package com.example.myheart.ui.home

import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.ui.format.formatClock
import kotlin.math.roundToInt

enum class HomeMetric { Bpm, Spo2 }

/** Uma barra do gráfico (uma leitura). [level] define a cor: Normal=verde, Attention=amarelo, Critical=vermelho. */
data class ChartBar(
    val time: String,
    val value: Int,
    val level: ReadingStatus,
)

/** Tudo o que o gráfico e os cartões de estatística precisam para uma das medidas. */
data class ChartSeries(
    val metric: HomeMetric,
    val axisMin: Int,
    val axisMax: Int,
    /** Em ordem cronológica: a leitura mais recente é a última (fica à direita). */
    val bars: List<ChartBar>,
    val average: Int?,
    val max: Int?,
    val min: Int?,
) {
    val axisTicks: List<Int> get() = listOf(axisMax, (axisMin + axisMax) / 2, axisMin)
}

/**
 * Monta a série do gráfico a partir das leituras do dia, que devem estar em ordem cronológica.
 *
 * A escala do eixo é fixa (BPM de 40 a 140, SpO₂ de 80 a 100) para a altura das barras ser comparável
 * entre os dias; só se amplia, de 10 em 10, se algum valor sair dessa faixa.
 * Média, máxima e mínima são null quando não há leituras.
 */
fun buildChartSeries(metric: HomeMetric, readings: List<AnalyzedReading>): ChartSeries {
    val bars = readings.map { item ->
        when (metric) {
            HomeMetric.Bpm -> ChartBar(formatClock(item.reading.timestamp), item.reading.bpm, item.bpmStatus)
            HomeMetric.Spo2 -> ChartBar(formatClock(item.reading.timestamp), item.reading.spo2, item.spo2Status)
        }
    }
    val values = bars.map { it.value }
    val (defaultMin, defaultMax) = when (metric) {
        HomeMetric.Bpm -> 40 to 140
        HomeMetric.Spo2 -> 80 to 100
    }
    val axisMin = minOf(defaultMin, floorToTen(values.minOrNull() ?: defaultMin))
    val axisMax = maxOf(defaultMax, ceilToTen(values.maxOrNull() ?: defaultMax))

    return ChartSeries(
        metric = metric,
        axisMin = axisMin,
        axisMax = axisMax,
        bars = bars,
        average = if (values.isEmpty()) null else values.average().roundToInt(),
        max = values.maxOrNull(),
        min = values.minOrNull(),
    )
}

private fun floorToTen(value: Int): Int = Math.floorDiv(value, 10) * 10

private fun ceilToTen(value: Int): Int = -Math.floorDiv(-value, 10) * 10
