package com.example.myheart.data.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementCollectorTest {

    private fun MeasurementCollector.addPairs(vararg pairs: Pair<Int, Int>) =
        pairs.forEach { (bpm, spo2) -> add(DecodedSample(bpm = bpm, spo2 = spo2)) }

    @Test
    fun semAmostras_naoEstaCompleta() {
        val collector = MeasurementCollector()
        assertFalse(collector.isComplete)
        assertNull(collector.result())
        assertEquals(0f, collector.progress, 0.0001f)
    }

    @Test
    fun completaComCincoAmostrasEUsaAMediana() {
        val collector = MeasurementCollector()
        collector.addPairs(70 to 97, 72 to 98, 71 to 98, 73 to 97, 72 to 98)

        assertTrue(collector.isComplete)
        assertEquals(CollectedMeasurement(bpm = 72, spo2 = 98), collector.result())
    }

    @Test
    fun valoresForaDaFaixaSaoIgnorados() {
        val collector = MeasurementCollector()
        collector.add(DecodedSample(bpm = 0, spo2 = 0))
        collector.add(DecodedSample(bpm = 500, spo2 = 101))

        assertEquals(0f, collector.progress, 0.0001f)
        assertNull(collector.result())
    }

    @Test
    fun usaApenasAsUltimasAmostras() {
        val collector = MeasurementCollector()
        // Três valores de estabilização, depois cinco bons: só os cinco bons contam.
        collector.addPairs(150 to 90, 140 to 91, 130 to 92)
        collector.addPairs(70 to 97, 72 to 98, 71 to 98, 73 to 97, 72 to 98)

        assertEquals(CollectedMeasurement(bpm = 72, spo2 = 98), collector.result())
    }

    @Test
    fun aceitaBatimentosEOxigenacaoEmPacotesSeparados() {
        val collector = MeasurementCollector()
        repeat(5) { collector.add(DecodedSample(bpm = 72)) }
        assertFalse(collector.isComplete)

        repeat(5) { collector.add(DecodedSample(spo2 = 98)) }
        assertEquals(CollectedMeasurement(bpm = 72, spo2 = 98), collector.result())
    }

    @Test
    fun progressoSegueAMedidaComMenosAmostras() {
        val collector = MeasurementCollector()
        collector.addPairs(72 to 98, 72 to 98)
        assertEquals(0.4f, collector.progress, 0.0001f)

        repeat(2) { collector.add(DecodedSample(bpm = 72)) }
        // Batimentos adiantados não aceleram o progresso: a oxigenação ainda tem 2 amostras.
        assertEquals(0.4f, collector.progress, 0.0001f)
    }

    @Test
    fun dedoRemovidoReiniciaAColeta() {
        val collector = MeasurementCollector()
        collector.addPairs(72 to 98, 72 to 98, 72 to 98)

        collector.add(DecodedSample(fingerPresent = false))

        assertEquals(0f, collector.progress, 0.0001f)
        assertNull(collector.result())
    }

    @Test
    fun semOxigenacaoNaoCompleta() {
        val collector = MeasurementCollector()
        repeat(10) { collector.add(DecodedSample(bpm = 72)) }

        assertFalse(collector.isComplete)
        assertNull(collector.result())
        assertEquals(0f, collector.progress, 0.0001f)
    }

    @Test
    fun medianaComQuantidadeParCalculaAMediaDosDoisDoMeio() {
        val collector = MeasurementCollector(requiredSamples = 4)
        collector.addPairs(70 to 97, 72 to 97, 74 to 97, 76 to 99)

        assertEquals(CollectedMeasurement(bpm = 73, spo2 = 97), collector.result())
    }

    @Test
    fun resetLimpaTudo() {
        val collector = MeasurementCollector(requiredSamples = 2)
        collector.addPairs(72 to 98, 72 to 98)
        assertTrue(collector.isComplete)

        collector.reset()

        assertFalse(collector.isComplete)
        assertNull(collector.result())
    }

    @Test(expected = IllegalArgumentException::class)
    fun quantidadeInvalidaFalha() {
        MeasurementCollector(requiredSamples = 0)
    }
}
