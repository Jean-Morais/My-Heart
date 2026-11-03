package com.example.myheart.data.sensor.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSelectorTest {

    private fun oximeter(address: String, rssi: Int) = ScanCandidate(
        address = address, name = null, rssi = rssi,
        advertisesPulseOximeter = true, advertisesHeartRate = true,
    )

    private fun heartRateOnly(address: String, rssi: Int) = ScanCandidate(
        address = address, name = null, rssi = rssi,
        advertisesPulseOximeter = false, advertisesHeartRate = true,
    )

    @Test
    fun semCandidatosNaoEscolheNada() {
        assertNull(DeviceSelector.pick(emptyList()))
        assertFalse(DeviceSelector.onlyIncompatible(emptyList()))
    }

    @Test
    fun escolheOMedidorDeOxigenacao() {
        val chosen = DeviceSelector.pick(listOf(oximeter("A", -60)))
        assertEquals("A", chosen?.address)
    }

    @Test
    fun comVariosMedidoresEscolheOMaisProximo() {
        val chosen = DeviceSelector.pick(listOf(oximeter("A", -80), oximeter("B", -45), oximeter("C", -70)))
        assertEquals("B", chosen?.address)
    }

    @Test
    fun dispositivoSoDeFrequenciaCardiacaNaoEEscolhido() {
        val candidates = listOf(heartRateOnly("H", -40))
        assertNull(DeviceSelector.pick(candidates))
        assertTrue(DeviceSelector.onlyIncompatible(candidates))
    }

    @Test
    fun medidorGanhaMesmoQuandoACintaEstaMaisPerto() {
        val chosen = DeviceSelector.pick(listOf(heartRateOnly("H", -30), oximeter("A", -75)))
        assertEquals("A", chosen?.address)
        assertFalse(DeviceSelector.onlyIncompatible(listOf(heartRateOnly("H", -30), oximeter("A", -75))))
    }
}
