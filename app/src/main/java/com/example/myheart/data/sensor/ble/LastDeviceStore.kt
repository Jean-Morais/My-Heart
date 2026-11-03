package com.example.myheart.data.sensor.ble

/** Último medidor com que o app conectou: o endereço Bluetooth e, se o aparelho anunciou, o nome. */
data class RememberedDevice(val address: String, val name: String?)

/** Guarda qual foi o último medidor, para a próxima conexão tentar esse primeiro, sem buscar. */
interface LastDeviceStore {
    fun get(): RememberedDevice?
    fun save(device: RememberedDevice)
    fun clear()
}
