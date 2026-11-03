package com.example.myheart.data.sensor.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

class BleParsersTest {

    // --- SFLOAT ---

    @Test
    fun sfloat_decodificaInteirosPositivos() {
        assertEquals(98.0, Sfloat.decode(0x0062)!!, 0.0)
        assertEquals(72.0, Sfloat.decode(0x0048)!!, 0.0)
    }

    @Test
    fun sfloat_decodificaNegativoEExpoente() {
        assertEquals(-1.0, Sfloat.decode(0x0FFF)!!, 0.0)
        assertEquals(10.0, Sfloat.decode(0x1001)!!, 0.0)
        assertEquals(0.1, Sfloat.decode(0xF001)!!, 1e-9)
    }

    @Test
    fun sfloat_valoresEspeciaisViramNulo() {
        listOf(0x07FF, 0x0800, 0x0801, 0x07FE, 0x0802).forEach { special ->
            assertNull("0x" + special.toString(16), Sfloat.decode(special))
        }
    }

    @Test
    fun sfloat_decodeAtLeLittleEndianEIgnoraBytesFaltando() {
        assertEquals(98.0, Sfloat.decodeAt(bytes(0x01, 0x62, 0x00), 1)!!, 0.0)
        assertNull(Sfloat.decodeAt(bytes(0x62), 0))
        assertNull(Sfloat.decodeAt(bytes(0x01, 0x62), 1))
    }

    // --- Heart Rate Measurement ---

    @Test
    fun heartRate_bpmDe8BitsComContatoDetectado() {
        val sample = HeartRateParser.parse(bytes(0x06, 0x48))!!
        assertEquals(72, sample.bpm)
        assertEquals(true, sample.fingerPresent)
    }

    @Test
    fun heartRate_dedoRemovidoNaoTemBpmEIndicaSemContato() {
        val sample = HeartRateParser.parse(bytes(0x04, 0x00))!!
        assertNull(sample.bpm)
        assertEquals(false, sample.fingerPresent)
    }

    @Test
    fun heartRate_bpmDe16Bits() {
        val sample = HeartRateParser.parse(bytes(0x07, 0xC8, 0x00))!!
        assertEquals(200, sample.bpm)
        assertEquals(true, sample.fingerPresent)
    }

    @Test
    fun heartRate_semSuporteAContatoNaoInformaDedo() {
        val sample = HeartRateParser.parse(bytes(0x00, 0x48))!!
        assertEquals(72, sample.bpm)
        assertNull(sample.fingerPresent)
    }

    @Test
    fun heartRate_pacoteTruncadoDevolveNulo() {
        assertNull(HeartRateParser.parse(bytes()))
        assertNull(HeartRateParser.parse(bytes(0x06)))
        assertNull(HeartRateParser.parse(bytes(0x01, 0x48)))
    }

    // --- Pulse Oximeter ---

    @Test
    fun pulseOximeter_continuoComValoresValidos() {
        val sample = PulseOximeterParser.parse(bytes(0x04, 0x62, 0x00, 0x48, 0x00, 0x00, 0x00))!!
        assertEquals(98, sample.spo2)
        assertEquals(72, sample.bpm)
    }

    @Test
    fun pulseOximeter_semDedoTemOsDoisValoresNulos() {
        val sample = PulseOximeterParser.parse(bytes(0x04, 0xFF, 0x07, 0xFF, 0x07, 0x00, 0x20))
        assertNotNull(sample)
        assertNull(sample!!.spo2)
        assertNull(sample.bpm)
    }

    @Test
    fun pulseOximeter_apenasUmValorValido() {
        val sample = PulseOximeterParser.parse(bytes(0x04, 0x62, 0x00, 0xFF, 0x07, 0x00, 0x40))!!
        assertEquals(98, sample.spo2)
        assertNull(sample.bpm)
    }

    @Test
    fun pulseOximeter_spotCheck() {
        val sample = PulseOximeterParser.parse(bytes(0x00, 0x62, 0x00, 0x48, 0x00))!!
        assertEquals(98, sample.spo2)
        assertEquals(72, sample.bpm)
    }

    @Test
    fun pulseOximeter_pacoteTruncadoDevolveNulo() {
        assertNull(PulseOximeterParser.parse(bytes(0x04, 0x62, 0x00, 0x48)))
    }

    // --- UUIDs ---

    @Test
    fun uuids_expandemOsIdentificadoresDe16Bits() {
        assertEquals("0000180d-0000-1000-8000-00805f9b34fb", BleUuids.HEART_RATE_SERVICE.toString())
        assertEquals("00001822-0000-1000-8000-00805f9b34fb", BleUuids.PULSE_OXIMETER_SERVICE.toString())
        assertEquals("00002a5f-0000-1000-8000-00805f9b34fb", BleUuids.PLX_CONTINUOUS_MEASUREMENT.toString())
        assertEquals("00002902-0000-1000-8000-00805f9b34fb", BleUuids.CLIENT_CHARACTERISTIC_CONFIG.toString())
    }
}
