package com.example.myheart.ui.bluetooth

import androidx.annotation.StringRes
import com.example.myheart.R
import com.example.myheart.data.sensor.SensorFailure

/** Texto do aviso de conexão para cada motivo de falha (nulo = motivo desconhecido, texto genérico). */
@StringRes
fun connectionErrorBodyRes(failure: SensorFailure?): Int = when (failure) {
    SensorFailure.PermissionDenied -> R.string.ble_error_permission
    SensorFailure.BluetoothUnavailable -> R.string.ble_error_unavailable
    SensorFailure.BluetoothDisabled -> R.string.ble_error_disabled
    SensorFailure.DeviceNotFound -> R.string.ble_error_not_found
    SensorFailure.IncompatibleDevice -> R.string.ble_error_incompatible
    SensorFailure.ConnectionFailed -> R.string.ble_error_connection_failed
    SensorFailure.ConnectionLost -> R.string.ble_error_connection_lost
    SensorFailure.NoSignal, null -> R.string.banner_error_body
}

/** Texto do estado de erro da Leitura para cada motivo de falha. */
@StringRes
fun readingErrorBodyRes(failure: SensorFailure?): Int = when (failure) {
    SensorFailure.NoSignal -> R.string.reading_error_no_signal
    SensorFailure.ConnectionLost -> R.string.reading_error_connection_lost
    else -> R.string.reading_error_body
}
