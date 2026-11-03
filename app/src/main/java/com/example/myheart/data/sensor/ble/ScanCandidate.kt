package com.example.myheart.data.sensor.ble

/** Dispositivo visto na busca, já reduzido ao que importa para escolher. */
data class ScanCandidate(
    val address: String,
    val name: String?,
    /** Intensidade do sinal em dBm; quanto mais perto de zero, mais perto o aparelho. */
    val rssi: Int,
    val advertisesPulseOximeter: Boolean,
    val advertisesHeartRate: Boolean,
)

/** Regras de escolha entre os dispositivos encontrados (puras, sem Bluetooth). */
object DeviceSelector {

    /**
     * Melhor candidato que mede oxigenação (anuncia o Pulse Oximeter Service): o de sinal mais
     * forte. Nulo se nenhum serve.
     */
    fun pick(candidates: Collection<ScanCandidate>): ScanCandidate? =
        candidates.filter { it.advertisesPulseOximeter }.maxByOrNull { it.rssi }

    /**
     * true quando apareceram dispositivos, mas nenhum mede oxigenação (uma cinta de frequência
     * cardíaca, por exemplo): o app pode dizer que o aparelho não é compatível, em vez de "não achei".
     */
    fun onlyIncompatible(candidates: Collection<ScanCandidate>): Boolean =
        candidates.isNotEmpty() && candidates.none { it.advertisesPulseOximeter }
}
