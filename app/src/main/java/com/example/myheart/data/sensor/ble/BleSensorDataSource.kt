package com.example.myheart.data.sensor.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import com.example.myheart.R
import com.example.myheart.data.sensor.CollectedMeasurement
import com.example.myheart.data.sensor.DecodedSample
import com.example.myheart.data.sensor.MeasurementCollector
import com.example.myheart.data.sensor.SensorDataSource
import com.example.myheart.data.sensor.SensorException
import com.example.myheart.data.sensor.SensorFailure
import com.example.myheart.data.sensor.SensorSample
import com.example.myheart.domain.model.Device
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Sensor real por Bluetooth de baixa energia (BLE), falando os perfis padrão da Bluetooth SIG:
 * Pulse Oximeter Service (SpO2 e pulso) e, quando existir, Heart Rate Service (BPM).
 *
 * Fluxo:
 * - [connect]: confere permissões e Bluetooth; se houver um último medidor guardado, tenta conectar
 *   direto nele (sem buscar); se não der, procura por poucos segundos um dispositivo que
 *   anuncia o Pulse Oximeter Service (se houver vários, o de sinal mais forte), conecta, descobre
 *   os serviços e confirma que o dispositivo realmente oferece a medição contínua. Não há
 *   pareamento: o próprio app procura e conecta.
 * - [readOnce]: assina as notificações, junta as amostras num [MeasurementCollector] até ter
 *   amostras suficientes de BPM e SpO2, e cancela as assinaturas ao terminar (ou ao ser cancelado).
 *
 * Falhas são [SensorException] com o motivo (contrato A33). As operações GATT são feitas uma de
 * cada vez, como o Android exige.
 *
 * Observação: a conexão pode cair sem aviso enquanto o app está parado; isso só é percebido na
 * próxima leitura, que falha com [SensorFailure.ConnectionLost].
 */
// As permissões são conferidas em requireReadyAdapter() antes de qualquer chamada protegida.
@SuppressLint("MissingPermission")
class BleSensorDataSource(
    context: Context,
    private val lastDeviceStore: LastDeviceStore? = null,
) : SensorDataSource {

    private val appContext: Context = context.applicationContext

    override val isSimulated: Boolean = false

    // Os callbacks do Bluetooth chegam em threads do sistema; por isso os campos são @Volatile.
    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var incoming: Channel<DecodedSample>? = null
    @Volatile private var pendingConnection: CompletableDeferred<Unit>? = null
    @Volatile private var pendingServices: CompletableDeferred<Int>? = null
    @Volatile private var pendingDescriptor: CompletableDeferred<Int>? = null

    /** Garante uma operação GATT por vez (escrita de descritor). */
    private val gattMutex = Mutex()

    // ------------------------------------------------------------------ Contrato

    override suspend fun connect(): Device {
        closeGatt() // sobra de uma conexão anterior, se houver
        val adapter = requireReadyAdapter()

        connectToRemembered(adapter)?.let { return it }

        val candidate = findDevice(adapter)
        val remote = adapter.getRemoteDevice(candidate.address)
        try {
            openGatt(remote, CONNECT_ATTEMPTS, CONNECT_TIMEOUT_MS)
            discoverAndValidate()
        } catch (t: Throwable) {
            closeGatt()
            throw t
        }
        lastDeviceStore?.save(RememberedDevice(address = candidate.address, name = candidate.name))
        return Device(
            name = candidate.name ?: appContext.getString(R.string.ble_default_device_name),
            address = candidate.address,
        )
    }

    /**
     * Tenta conectar direto no último medidor usado, com uma só tentativa curta. Devolve nulo se não
     * houver medidor guardado ou se ele não responder (desligado, longe, trocado por outro aparelho):
     * o chamador então faz a busca normal. Só falhas conhecidas caem para a busca; cancelamento passa.
     */
    private suspend fun connectToRemembered(adapter: BluetoothAdapter): Device? {
        val store = lastDeviceStore ?: return null
        val remembered = withContext(Dispatchers.IO) { store.get() } ?: return null
        val remote = try {
            adapter.getRemoteDevice(remembered.address)
        } catch (e: IllegalArgumentException) {
            store.clear() // endereço inválido guardado: esquece
            return null
        }
        try {
            openGatt(remote, REMEMBERED_ATTEMPTS, REMEMBERED_CONNECT_TIMEOUT_MS)
            discoverAndValidate()
        } catch (e: SensorException) {
            closeGatt()
            return null
        } catch (t: Throwable) {
            closeGatt()
            throw t
        }
        return Device(
            name = remembered.name ?: appContext.getString(R.string.ble_default_device_name),
            address = remembered.address,
        )
    }

    override suspend fun readOnce(onProgress: (Int) -> Unit): SensorSample {
        val g = gatt ?: throw SensorException(SensorFailure.ConnectionLost)
        val oximeter = oximeterCharacteristic(g) ?: throw SensorException(SensorFailure.IncompatibleDevice)
        val heartRate = heartRateCharacteristic(g)

        val samples = Channel<DecodedSample>(Channel.UNLIMITED)
        incoming = samples
        try {
            setNotifications(g, oximeter, enable = true)
            if (heartRate != null) {
                // O BPM também vem junto com a oxigenação; a assinatura extra não é obrigatória.
                try {
                    setNotifications(g, heartRate, enable = true)
                } catch (e: SensorException) {
                    // segue só com o pulso do oxímetro
                }
            }
            return collectMeasurement(samples, onProgress)
        } finally {
            if (incoming === samples) incoming = null
            samples.close()
            // Cancela as assinaturas mesmo se a leitura foi cancelada; sem isso o sensor continuaria enviando.
            withContext(NonCancellable) {
                unsubscribeQuietly(g, oximeter)
                if (heartRate != null) unsubscribeQuietly(g, heartRate)
            }
        }
    }

    override suspend fun disconnect() {
        closeGatt()
    }

    override fun release() {
        closeGatt()
    }

    // ------------------------------------------------------------------ Preparação e busca

    private fun requireReadyAdapter(): BluetoothAdapter {
        if (!BluetoothPermissions.granted(appContext)) {
            throw SensorException(SensorFailure.PermissionDenied)
        }
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            throw SensorException(SensorFailure.BluetoothUnavailable)
        }
        val adapter = appContext.getSystemService(BluetoothManager::class.java)?.adapter
            ?: throw SensorException(SensorFailure.BluetoothUnavailable)
        if (!adapter.isEnabled) throw SensorException(SensorFailure.BluetoothDisabled)
        return adapter
    }

    /**
     * Busca por até [SCAN_TIMEOUT_MS]. Ao achar o primeiro medidor, espera mais [SCAN_SETTLE_MS]
     * para ver se há outros e escolhe o de sinal mais forte.
     */
    private suspend fun findDevice(adapter: BluetoothAdapter): ScanCandidate {
        val scanner = adapter.bluetoothLeScanner ?: throw SensorException(SensorFailure.BluetoothDisabled)
        val found = LinkedHashMap<String, ScanCandidate>()
        var settleDeadline = Long.MAX_VALUE

        withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            scanResults(scanner)
                .takeWhile { SystemClock.elapsedRealtime() < settleDeadline }
                .collect { candidate ->
                    found[candidate.address] = candidate
                    if (candidate.advertisesPulseOximeter && settleDeadline == Long.MAX_VALUE) {
                        settleDeadline = SystemClock.elapsedRealtime() + SCAN_SETTLE_MS
                    }
                }
        }

        DeviceSelector.pick(found.values)?.let { return it }
        if (DeviceSelector.onlyIncompatible(found.values)) {
            throw SensorException(SensorFailure.IncompatibleDevice)
        }
        throw SensorException(SensorFailure.DeviceNotFound)
    }

    private fun scanResults(scanner: BluetoothLeScanner): Flow<ScanCandidate> = callbackFlow {
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(result.toCandidate())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { trySend(it.toCandidate()) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(SensorException(SensorFailure.ConnectionFailed))
            }
        }
        // Filtra pelos dois serviços: o Pulse Oximeter mede; o Heart Rate só serve para dizer "incompatível".
        val filters = listOf(BleUuids.PULSE_OXIMETER_SERVICE, BleUuids.HEART_RATE_SERVICE).map {
            ScanFilter.Builder().setServiceUuid(ParcelUuid(it)).build()
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanner.startScan(filters, settings, callback)
        } catch (e: SecurityException) {
            close(SensorException(SensorFailure.PermissionDenied, e))
        } catch (e: IllegalStateException) {
            close(SensorException(SensorFailure.BluetoothDisabled, e))
        }
        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (e: Exception) {
                // Bluetooth desligado no meio da busca: nada a parar.
            }
        }
    }

    private fun ScanResult.toCandidate(): ScanCandidate {
        val uuids: List<UUID> = scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()
        return ScanCandidate(
            address = device.address,
            name = scanRecord?.deviceName,
            rssi = rssi,
            advertisesPulseOximeter = BleUuids.PULSE_OXIMETER_SERVICE in uuids,
            advertisesHeartRate = BleUuids.HEART_RATE_SERVICE in uuids,
        )
    }

    // ------------------------------------------------------------------ Conexão

    /** Abre a conexão GATT; a primeira tentativa costuma falhar (erro 133), por isso a busca normal tenta duas vezes. */
    private suspend fun openGatt(remote: BluetoothDevice, attempts: Int, timeoutMillis: Long) {
        repeat(attempts) { attempt ->
            val connected = CompletableDeferred<Unit>()
            pendingConnection = connected
            val g = remote.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
                ?: throw SensorException(SensorFailure.ConnectionFailed)
            gatt = g

            val ok = withTimeoutOrNull(timeoutMillis) {
                try {
                    connected.await()
                    true
                } catch (e: SensorException) {
                    false
                }
            } ?: false
            if (ok) return

            closeGatt()
            if (attempt == attempts - 1) throw SensorException(SensorFailure.ConnectionFailed)
        }
    }

    private suspend fun discoverAndValidate() {
        val g = gatt ?: throw SensorException(SensorFailure.ConnectionLost)
        val discovered = CompletableDeferred<Int>()
        pendingServices = discovered
        if (!g.discoverServices()) throw SensorException(SensorFailure.ConnectionFailed)

        val status = withTimeoutOrNull(OPERATION_TIMEOUT_MS) { discovered.await() }
            ?: throw SensorException(SensorFailure.ConnectionFailed)
        if (status != BluetoothGatt.GATT_SUCCESS) throw SensorException(SensorFailure.ConnectionFailed)

        // Reconhecimento do dispositivo: sem a medição contínua de oxigenação, não serve.
        oximeterCharacteristic(g) ?: throw SensorException(SensorFailure.IncompatibleDevice)
    }

    private fun oximeterCharacteristic(g: BluetoothGatt): BluetoothGattCharacteristic? =
        g.getService(BleUuids.PULSE_OXIMETER_SERVICE)
            ?.getCharacteristic(BleUuids.PLX_CONTINUOUS_MEASUREMENT)
            ?.takeIf { it.canNotify() }

    private fun heartRateCharacteristic(g: BluetoothGatt): BluetoothGattCharacteristic? =
        g.getService(BleUuids.HEART_RATE_SERVICE)
            ?.getCharacteristic(BleUuids.HEART_RATE_MEASUREMENT)
            ?.takeIf { it.canNotify() }

    private fun BluetoothGattCharacteristic.canNotify(): Boolean =
        (properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0

    private fun closeGatt() {
        val g = gatt ?: return
        gatt = null
        try {
            g.disconnect()
        } catch (e: Exception) {
            // Já desconectado ou sem permissão: o close() abaixo libera o que sobrou.
        }
        try {
            g.close()
        } catch (e: Exception) {
            // Nada mais a fazer.
        }
    }

    // ------------------------------------------------------------------ Assinaturas e coleta

    /** Liga ou desliga as notificações de uma característica (duas etapas: local e descritor CCCD). */
    private suspend fun setNotifications(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, enable: Boolean) {
        val descriptor = characteristic.getDescriptor(BleUuids.CLIENT_CHARACTERISTIC_CONFIG)
            ?: throw SensorException(SensorFailure.IncompatibleDevice)
        val value = when {
            !enable -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ->
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        }

        gattMutex.withLock {
            if (!g.setCharacteristicNotification(characteristic, enable)) {
                throw SensorException(SensorFailure.ConnectionFailed)
            }
            val written = CompletableDeferred<Int>()
            pendingDescriptor = written
            if (!writeDescriptorCompat(g, descriptor, value)) {
                pendingDescriptor = null
                throw SensorException(SensorFailure.ConnectionFailed)
            }
            val status = withTimeoutOrNull(OPERATION_TIMEOUT_MS) { written.await() }
            pendingDescriptor = null
            if (status != BluetoothGatt.GATT_SUCCESS) throw SensorException(SensorFailure.ConnectionFailed)
        }
    }

    private suspend fun unsubscribeQuietly(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        try {
            setNotifications(g, characteristic, enable = false)
        } catch (e: SensorException) {
            // Conexão já caiu ou o aparelho não respondeu: não há mais nada a cancelar.
        }
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptorCompat(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.setValue(value)
            g.writeDescriptor(descriptor)
        }

    /**
     * Junta as amostras até a medição ficar completa. Se o sensor ficar [SIGNAL_GAP_MS] sem enviar
     * nada, o que já foi coletado é descartado (o dedo pode ter saído e voltado em outra posição).
     * Passado [READ_TIMEOUT_MS] sem completar, falha com [SensorFailure.NoSignal].
     */
    private suspend fun collectMeasurement(
        samples: ReceiveChannel<DecodedSample>,
        onProgress: (Int) -> Unit,
    ): SensorSample {
        val collector = MeasurementCollector()
        val measurement: CollectedMeasurement = withTimeoutOrNull(READ_TIMEOUT_MS) {
            var done: CollectedMeasurement? = null
            while (done == null) {
                val sample = withTimeoutOrNull(SIGNAL_GAP_MS) { samples.receive() }
                if (sample == null) {
                    if (collector.progress > 0f) {
                        collector.reset()
                        onProgress(0)
                    }
                    continue
                }
                collector.add(sample)
                onProgress((collector.progress * 100).toInt())
                done = collector.result()
            }
            done
        } ?: throw SensorException(SensorFailure.NoSignal)

        return SensorSample(bpm = measurement.bpm, spo2 = measurement.spo2)
    }

    private fun handleNotification(uuid: UUID, value: ByteArray) {
        val sample = when (uuid) {
            BleUuids.PLX_CONTINUOUS_MEASUREMENT -> PulseOximeterParser.parse(value)
            BleUuids.HEART_RATE_MEASUREMENT -> HeartRateParser.parse(value)
            else -> null
        } ?: return
        incoming?.trySend(sample)
    }

    // ------------------------------------------------------------------ Callbacks do sistema

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val connected = newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS
            if (connected) {
                pendingConnection?.complete(Unit)
                return
            }
            // Desconectou ou falhou: acorda quem estiver esperando e avisa a leitura em curso.
            val lost = SensorException(SensorFailure.ConnectionLost)
            pendingConnection?.completeExceptionally(SensorException(SensorFailure.ConnectionFailed))
            pendingServices?.completeExceptionally(lost)
            pendingDescriptor?.completeExceptionally(lost)
            incoming?.close(lost)
            if (this@BleSensorDataSource.gatt === gatt) {
                this@BleSensorDataSource.gatt = null
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            pendingServices?.complete(status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            pendingDescriptor?.complete(status)
        }

        // Android 13 ou superior: o valor vem junto com a chamada.
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            handleNotification(characteristic.uuid, value)
        }

        // Android 12 ou anterior: o valor é lido da característica.
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.getValue() ?: return
            handleNotification(characteristic.uuid, value)
        }
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 8_000L
        const val SCAN_SETTLE_MS = 1_500L
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val CONNECT_ATTEMPTS = 2
        const val REMEMBERED_CONNECT_TIMEOUT_MS = 4_000L
        const val REMEMBERED_ATTEMPTS = 1
        const val OPERATION_TIMEOUT_MS = 5_000L
        const val READ_TIMEOUT_MS = 30_000L
        const val SIGNAL_GAP_MS = 5_000L
    }
}
