package com.example.myheart.data.sensor

/** Motivo de uma falha do sensor, para a interface explicar o que aconteceu e o que fazer. */
enum class SensorFailure {
    /** O usuário não liberou as permissões de Bluetooth. */
    PermissionDenied,

    /** O aparelho não tem Bluetooth de baixa energia. */
    BluetoothUnavailable,

    /** O Bluetooth do celular está desligado. */
    BluetoothDisabled,

    /** A busca terminou sem encontrar um medidor compatível. */
    DeviceNotFound,

    /** Encontrou um dispositivo, mas ele não mede oxigenação (ou não oferece o serviço esperado). */
    IncompatibleDevice,

    /** Não foi possível abrir a conexão com o medidor. */
    ConnectionFailed,

    /** A conexão existia e caiu. */
    ConnectionLost,

    /** Conectado, mas o medidor não enviou uma leitura estável a tempo (dedo mal posicionado, por exemplo). */
    NoSignal,
}

/**
 * Falha com motivo conhecido. As fontes de dados lançam esta exceção (contrato A33); qualquer
 * outra exceção continua valendo e a interface mostra a mensagem genérica.
 */
class SensorException(
    val failure: SensorFailure,
    cause: Throwable? = null,
) : Exception(failure.name, cause)
