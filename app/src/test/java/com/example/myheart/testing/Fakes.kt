package com.example.myheart.testing

import com.example.myheart.data.sensor.SensorDataSource
import com.example.myheart.data.sensor.SensorException
import com.example.myheart.data.sensor.SensorFailure
import com.example.myheart.data.sensor.SensorSample
import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.Device
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.RelativeContact
import com.example.myheart.domain.model.UserProfile
import com.example.myheart.domain.notification.AlertNotifier
import com.example.myheart.domain.repository.ProfileRepository
import com.example.myheart.domain.repository.ReadingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Repositório de perfil em memória, com contadores para os testes conferirem o que foi gravado. */
class FakeProfileRepository(
    var profile: UserProfile? = null,
    var contact: RelativeContact? = null,
) : ProfileRepository {

    /** Quando true, as leituras e as gravações lançam exceção (simula falha do banco). */
    var failLoad = false
    var failSave = false

    var saveProfileCalls = 0
        private set
    var saveContactCalls = 0
        private set

    override suspend fun getProfile(): UserProfile? {
        if (failLoad) throw IOException("falha simulada ao ler o perfil")
        return profile
    }

    override suspend fun saveProfile(profile: UserProfile) {
        saveProfileCalls++
        if (failSave) throw IOException("falha simulada ao gravar o perfil")
        this.profile = profile
    }

    override suspend fun getRelativeContact(): RelativeContact? {
        if (failLoad) throw IOException("falha simulada ao ler o contato")
        return contact
    }

    override suspend fun saveRelativeContact(contact: RelativeContact) {
        saveContactCalls++
        if (failSave) throw IOException("falha simulada ao gravar o contato")
        this.contact = contact
    }
}

/** Repositório de leituras em memória; as consultas observáveis reagem às gravações, como no Room. */
class FakeReadingRepository(initial: List<Reading> = emptyList()) : ReadingRepository {

    private val all = MutableStateFlow(initial)

    /** Quando true, gravar lança exceção (simula falha do banco). */
    var failSave = false

    /** Quando true, as consultas lançam exceção (simula falha do banco). */
    var failLoad = false

    var saveCalls = 0
        private set

    /** Leituras gravadas, da mais antiga para a mais recente. */
    val saved: List<Reading> get() = all.value

    override suspend fun saveReading(reading: Reading) {
        saveCalls++
        if (failSave) throw IOException("falha simulada ao gravar a leitura")
        all.value = all.value + reading.copy(id = all.value.size + 1L)
    }

    override suspend fun getLastReading(): Reading? {
        if (failLoad) throw IOException("falha simulada ao ler a última leitura")
        return all.value.maxByOrNull { it.timestamp }
    }

    override fun observeLastReading(): Flow<Reading?> =
        if (failLoad) flow { throw IOException("falha simulada ao observar a última leitura") }
        else all.map { list -> list.maxByOrNull { it.timestamp } }

    override fun observeReadings(fromInclusive: Long?, toExclusive: Long?): Flow<List<Reading>> =
        all.map { list ->
            list
                .filter { (fromInclusive == null || it.timestamp >= fromInclusive) && (toExclusive == null || it.timestamp < toExclusive) }
                .sortedByDescending { it.timestamp }
        }
}

/**
 * Sensor controlável: conectar leva [connectDurationMillis] e a leitura leva [readDurationMillis]
 * de tempo virtual (os testes avançam o relógio) e devolve [sample]. Conta as chamadas para os
 * testes verificarem toques repetidos e pode ser mandado falhar ([failConnect] e [failRead]).
 */
class FakeSensor(
    override val isSimulated: Boolean = true,
    var sample: SensorSample = SensorSample(bpm = 72, spo2 = 98),
    private val readDurationMillis: Long = 1_000,
    private val connectDurationMillis: Long = 0,
) : SensorDataSource {

    var failConnect = false
    var failRead = false

    /** Quando definido, a falha é uma SensorException com este motivo (em vez da IOException genérica). */
    var connectFailure: SensorFailure? = null
    var readFailure: SensorFailure? = null

    var connectCalls = 0
        private set
    var readCalls = 0
        private set
    var disconnectCalls = 0
        private set

    override suspend fun connect(): Device {
        connectCalls++
        delay(connectDurationMillis)
        connectFailure?.let { throw SensorException(it) }
        if (failConnect) throw IOException("falha simulada ao conectar")
        return Device(name = "Sensor de teste", address = "AA:BB:CC:DD:EE:FF")
    }

    override suspend fun readOnce(onProgress: (Int) -> Unit): SensorSample {
        readCalls++
        onProgress(50)
        delay(readDurationMillis)
        readFailure?.let { throw SensorException(it) }
        if (failRead) throw IOException("falha simulada na leitura")
        onProgress(100)
        return sample
    }

    override suspend fun disconnect() {
        disconnectCalls++
    }
}

/** Publicador de avisos que só anota o que recebeu; pode devolver false (permissão negada) ou falhar. */
class FakeAlertNotifier : AlertNotifier {

    /** O que [notifyAbnormal] devolve: true = publicou; false = sem permissão ou notificações desligadas. */
    var published = true

    /** Quando true, publicar lança exceção. */
    var fail = false

    /** Leituras para as quais um aviso foi pedido, na ordem. */
    val requested = mutableListOf<AnalyzedReading>()

    override fun notifyAbnormal(reading: AnalyzedReading): Boolean {
        if (fail) throw IllegalStateException("falha simulada ao notificar")
        requested += reading
        return published
    }
}
