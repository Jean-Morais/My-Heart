package com.example.myheart.data.sensor.ble

import android.content.Context

/**
 * Guarda o último medidor nas preferências privadas do app (arquivo próprio, fora do banco: não
 * exige migração). O backup automático e a transferência entre aparelhos estão desligados, então
 * o endereço nunca sai do celular.
 */
class SharedPreferencesLastDeviceStore(context: Context) : LastDeviceStore {

    private val preferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun get(): RememberedDevice? {
        val address = preferences.getString(KEY_ADDRESS, null) ?: return null
        return RememberedDevice(address = address, name = preferences.getString(KEY_NAME, null))
    }

    override fun save(device: RememberedDevice) {
        preferences.edit()
            .putString(KEY_ADDRESS, device.address)
            .putString(KEY_NAME, device.name)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "last_sensor_device"
        const val KEY_ADDRESS = "address"
        const val KEY_NAME = "name"
    }
}
