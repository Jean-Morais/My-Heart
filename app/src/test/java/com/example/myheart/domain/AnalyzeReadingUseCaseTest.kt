package com.example.myheart.domain

import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyzeReadingUseCaseTest {

    private val analyze = AnalyzeReadingUseCase()

    private fun reading(bpm: Int, spo2: Int) = Reading(bpm = bpm, spo2 = spo2, timestamp = 0L)

    @Test
    fun `valores habituais sao Normal`() {
        assertEquals(ReadingStatus.Normal, analyze(reading(bpm = 72, spo2 = 98)))
    }

    @Test
    fun `limites de batimentos`() {
        assertEquals(ReadingStatus.Normal, analyze.bpmStatus(60))
        assertEquals(ReadingStatus.Normal, analyze.bpmStatus(100))
        assertEquals(ReadingStatus.Attention, analyze.bpmStatus(59))
        assertEquals(ReadingStatus.Attention, analyze.bpmStatus(101))
        assertEquals(ReadingStatus.Attention, analyze.bpmStatus(40))
        assertEquals(ReadingStatus.Attention, analyze.bpmStatus(130))
        assertEquals(ReadingStatus.Critical, analyze.bpmStatus(39))
        assertEquals(ReadingStatus.Critical, analyze.bpmStatus(131))
    }

    @Test
    fun `limites de oxigenacao`() {
        assertEquals(ReadingStatus.Normal, analyze.spo2Status(95))
        assertEquals(ReadingStatus.Attention, analyze.spo2Status(94))
        assertEquals(ReadingStatus.Attention, analyze.spo2Status(90))
        assertEquals(ReadingStatus.Critical, analyze.spo2Status(89))
    }

    @Test
    fun `nivel geral e o pior entre as duas medidas`() {
        assertEquals(ReadingStatus.Attention, analyze(reading(bpm = 72, spo2 = 93)))
        assertEquals(ReadingStatus.Critical, analyze(reading(bpm = 72, spo2 = 88)))
        assertEquals(ReadingStatus.Critical, analyze(reading(bpm = 135, spo2 = 98)))
        assertEquals(ReadingStatus.Critical, analyze(reading(bpm = 112, spo2 = 88)))
    }

    @Test
    fun `analyze devolve o nivel de cada medida`() {
        val result = analyze.analyze(reading(bpm = 112, spo2 = 98))
        assertEquals(ReadingStatus.Attention, result.status)
        assertEquals(ReadingStatus.Attention, result.bpmStatus)
        assertEquals(ReadingStatus.Normal, result.spo2Status)
    }
}
