package com.example.myheart.data.sensor.ble

import java.util.Locale
import java.util.UUID

/** UUIDs dos perfis padrão da Bluetooth SIG usados pelo app. */
object BleUuids {
    val HEART_RATE_SERVICE: UUID = fromShort(0x180D)
    val HEART_RATE_MEASUREMENT: UUID = fromShort(0x2A37)

    val PULSE_OXIMETER_SERVICE: UUID = fromShort(0x1822)
    val PLX_SPOT_CHECK_MEASUREMENT: UUID = fromShort(0x2A5E)
    val PLX_CONTINUOUS_MEASUREMENT: UUID = fromShort(0x2A5F)

    /** Descritor que liga notificações e indicações (CCCD). */
    val CLIENT_CHARACTERISTIC_CONFIG: UUID = fromShort(0x2902)

    private fun fromShort(id: Int): UUID =
        UUID.fromString(String.format(Locale.ROOT, "0000%04x-0000-1000-8000-00805f9b34fb", id))
}
