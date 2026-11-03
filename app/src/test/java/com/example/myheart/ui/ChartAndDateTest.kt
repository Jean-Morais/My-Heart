package com.example.myheart.ui

import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.ui.format.formatDateRange
import com.example.myheart.ui.home.HomeMetric
import com.example.myheart.ui.home.buildChartSeries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ChartAndDateTest {

    private val analyze = AnalyzeReadingUseCase()

    private fun analyzed(bpm: Int, spo2: Int, timestamp: Long) =
        analyze.analyze(Reading(bpm = bpm, spo2 = spo2, timestamp = timestamp))

    @Test
    fun `serie vazia nao tem estatisticas`() {
        val series = buildChartSeries(HomeMetric.Bpm, emptyList())
        assertEquals(emptyList<Any>(), series.bars)
        assertNull(series.average)
        assertNull(series.max)
        assertNull(series.min)
        assertEquals(40, series.axisMin)
        assertEquals(140, series.axisMax)
    }

    @Test
    fun `estatisticas e niveis seguem a medida escolhida`() {
        val readings = listOf(
            analyzed(bpm = 72, spo2 = 98, timestamp = 1_000L),
            analyzed(bpm = 112, spo2 = 88, timestamp = 2_000L),
        )

        val bpm = buildChartSeries(HomeMetric.Bpm, readings)
        assertEquals(listOf(72, 112), bpm.bars.map { it.value })
        assertEquals(listOf(ReadingStatus.Normal, ReadingStatus.Attention), bpm.bars.map { it.level })
        assertEquals(92, bpm.average)
        assertEquals(112, bpm.max)
        assertEquals(72, bpm.min)

        val spo2 = buildChartSeries(HomeMetric.Spo2, readings)
        assertEquals(listOf(ReadingStatus.Normal, ReadingStatus.Critical), spo2.bars.map { it.level })
        assertEquals(80, spo2.axisMin)
        assertEquals(100, spo2.axisMax)
    }

    @Test
    fun `eixo amplia quando um valor sai da faixa`() {
        val readings = listOf(analyzed(bpm = 151, spo2 = 98, timestamp = 1_000L))
        val series = buildChartSeries(HomeMetric.Bpm, readings)
        assertEquals(160, series.axisMax)
        assertEquals(40, series.axisMin)
    }

    @Test
    fun `intervalo de datas`() {
        assertEquals(
            "24 a 30 de outubro de 2026",
            formatDateRange(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 30)),
        )
        assertEquals(
            "28 de setembro a 30 de outubro de 2026",
            formatDateRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 30)),
        )
        assertEquals(
            "30 de dezembro de 2025 a 2 de janeiro de 2026",
            formatDateRange(LocalDate.of(2025, 12, 30), LocalDate.of(2026, 1, 2)),
        )
        assertEquals(
            "30 de outubro de 2026",
            formatDateRange(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 30)),
        )
    }
}
