package com.example.myheart.data.sensor.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permissões de execução necessárias para procurar e conectar ao medidor, que mudam com a versão
 * do Android:
 * - Android 12 ou superior: "Dispositivos próximos" (busca e conexão);
 * - Android 9 a 11: Localização (exigida pelo sistema para a busca BLE).
 * As permissões BLUETOOTH e BLUETOOTH_ADMIN das versões antigas são liberadas na instalação.
 */
object BluetoothPermissions {

    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun granted(context: Context): Boolean =
        required().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
}
