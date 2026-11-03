package com.example.myheart.data.sensor

import com.example.myheart.domain.model.Device
import kotlinx.coroutines.delay

/** Simula o hardware: progresso gradual e, no fim, valores aleatórios. */
class FakeSensorDataSource : SensorDataSource {

    override val isSimulated: Boolean = true

    override suspend fun connect(): Device {
        delay(500) // simula o tempo de conexão
        return Device(name = "Sensor Simulado", address = "00:00:00:00:00:00")
    }

    override suspend fun readOnce(onProgress: (Int) -> Unit): SensorSample {
        val steps = 60 // 60 passos de 100 ms = 6 s de captura
        repeat(steps) { step ->
            delay(100)
            onProgress((step + 1) * 100 / steps)
        }
        return SensorSample(
            bpm = (55..120).random(),
            spo2 = (88..100).random(),
        )
    }

    override suspend fun disconnect() {
        // Nada a encerrar: o sensor simulado não mantém conexão.
    }
}
