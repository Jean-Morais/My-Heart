package com.example.myheart.data.sensor

import com.example.myheart.domain.model.Device

data class SensorSample(val bpm: Int, val spo2: Int)

/**
 * Contrato da fonte de dados do sensor.
 * Há duas implementações: a simulada (FakeSensorDataSource, usada em testes e demonstrações) e a
 * Bluetooth real (BleSensorDataSource). As telas e os ViewModels só conhecem esta interface.
 */
interface SensorDataSource {
    // Falhas: connect e readOnce lançam exceção quando a conexão ou a leitura não dão certo
    // (sensor desligado, fora de alcance, sinal ruim). O ViewModel trata qualquer exceção que
    // não seja cancelamento e mostra o estado de erro; nada aqui precisa conhecer a interface.
    // Quando o motivo é conhecido, lance SensorException com o SensorFailure: a tela explica o que fazer.

    /** true quando os valores são inventados (demonstração); a UI avisa o usuário. */
    val isSimulated: Boolean

    suspend fun connect(): Device

    /** Faz uma leitura completa. [onProgress] recebe de 0 a 100 enquanto o sinal é capturado. */
    suspend fun readOnce(onProgress: (Int) -> Unit): SensorSample

    /** Encerra a conexão com o dispositivo. Deve ser seguro chamar mesmo se a conexão já caiu. */
    suspend fun disconnect()

    /**
     * Libera tudo de forma imediata e sem suspender; chamada quando o ViewModel é destruído, para
     * a conexão não ficar presa ao app. Por padrão não há nada a liberar.
     */
    fun release() {}
}
