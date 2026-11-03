package com.example.myheart.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myheart.data.sensor.SensorDataSource
import com.example.myheart.data.sensor.SensorException
import com.example.myheart.data.sensor.SensorFailure
import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.Device
import com.example.myheart.domain.model.FamilyAlert
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.repository.ReadingRepository
import com.example.myheart.domain.usecase.AlertOnAbnormalReadingUseCase
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.domain.usecase.PrepareFamilyAlertUseCase
import com.example.myheart.ui.format.localDateOf
import com.example.myheart.ui.format.startOfDayMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Estado único da UI, compartilhado entre Início e Leitura. */
data class MyHeartUiState(
    val device: Device? = null,
    val lastReading: Reading? = null,
    val status: ReadingStatus? = null,
    val isReading: Boolean = false,
    /** Progresso da captura, de 0 a 100. */
    val readingProgress: Int = 0,
    /** true quando os dados exibidos vêm do sensor simulado. */
    val isSimulatedData: Boolean = false,
    /**
     * Dia mostrado no gráfico da Início: o dia da leitura mais recente (hoje, se já houve leitura hoje).
     * null enquanto não existir nenhuma leitura.
     */
    val chartDay: LocalDate? = null,
    /** Leituras de [chartDay] com os níveis calculados, em ordem cronológica (a mais recente é a última). */
    val chartReadings: List<AnalyzedReading> = emptyList(),
    /** true quando as leituras salvas não puderam ser carregadas do banco. */
    val loadError: Boolean = false,
    /** true enquanto a conexão com o dispositivo está sendo feita (impede toques repetidos). */
    val isConnecting: Boolean = false,
    /** true quando a última tentativa de conexão falhou. */
    val connectionError: Boolean = false,
    /** Motivo conhecido da falha de conexão (null = motivo desconhecido, mensagem genérica). */
    val connectionFailure: SensorFailure? = null,
    /** true quando a leitura falhou no meio do caminho; a tela de Leitura mostra o estado de erro. */
    val readingError: Boolean = false,
    /** Motivo conhecido da falha de leitura (null = motivo desconhecido, mensagem genérica). */
    val readingFailure: SensorFailure? = null,
    /** true quando a leitura exibida não foi gravada no banco (o resultado continua na tela). */
    val saveError: Boolean = false,
    /**
     * Aviso ao familiar oferecido depois de uma leitura fora do normal (null = nada a oferecer).
     * O envio é sempre manual; some ao começar outra leitura ou ao sair da tela de Leitura.
     */
    val familyAlert: FamilyAlert? = null,
) {
    val canRead: Boolean get() = device != null && !isReading
}

private const val TAG = "MyHeartViewModel"

/** Resultado intermediário da observação do dia do gráfico. */
private data class DayReadings(val day: LocalDate?, val readings: List<Reading>)

class MyHeartViewModel(
    private val sensor: SensorDataSource,
    private val analyzeReading: AnalyzeReadingUseCase,
    private val repository: ReadingRepository,
    private val alertOnAbnormalReading: AlertOnAbnormalReadingUseCase,
    private val prepareFamilyAlert: PrepareFamilyAlertUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyHeartUiState(isSimulatedData = sensor.isSimulated))
    val uiState: StateFlow<MyHeartUiState> = _uiState.asStateFlow()

    private var readingJob: Job? = null
    private var chartJob: Job? = null

    /** Leitura exibida cuja gravação falhou; guardada para "Salvar de novo". */
    private var unsavedReading: Reading? = null

    init {
        loadLastReading()
        observeChartDay()
    }

    /**
     * Mantém [MyHeartUiState.chartDay] e [MyHeartUiState.chartReadings] atualizados: segue a leitura
     * mais recente para saber o dia e observa as leituras desse dia. Assim, uma leitura nova
     * (ou o primeiro uso do app) aparece no gráfico sem ninguém pedir.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeChartDay() {
        chartJob?.cancel()
        chartJob = viewModelScope.launch {
            repository.observeLastReading()
                .flatMapLatest { last ->
                    if (last == null) {
                        flowOf(DayReadings(day = null, readings = emptyList()))
                    } else {
                        val zone = ZoneId.systemDefault()
                        val day = localDateOf(last.timestamp, zone)
                        repository
                            .observeReadings(
                                fromInclusive = startOfDayMillis(day, zone),
                                toExclusive = startOfDayMillis(day.plusDays(1), zone),
                            )
                            .map { readings -> DayReadings(day = day, readings = readings) }
                    }
                }
                .catch { e ->
                    Log.e(TAG, "Falha ao observar as leituras do dia", e)
                    _uiState.update { it.copy(loadError = true) }
                }
                .collect { dayReadings ->
                    _uiState.update { state ->
                        state.copy(
                            chartDay = dayReadings.day,
                            // O banco devolve da mais recente para a mais antiga; o gráfico quer o inverso.
                            chartReadings = dayReadings.readings.asReversed().map(analyzeReading::analyze),
                        )
                    }
                }
        }
    }

    /**
     * Carga pontual da última leitura gravada, para o app não "esquecer" ao ser reaberto ou quando
     * o Android encerra o processo. Não sobrescreve nada se uma leitura nova já chegou.
     */
    private fun loadLastReading() {
        viewModelScope.launch {
            val saved = try {
                repository.getLastReading()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao carregar a última leitura", e)
                _uiState.update { it.copy(loadError = true) }
                null
            } ?: return@launch

            _uiState.update { state ->
                if (state.lastReading != null || state.isReading) {
                    state
                } else {
                    state.copy(
                        lastReading = saved,
                        status = analyzeReading(saved),
                        isSimulatedData = saved.isSimulated,
                    )
                }
            }
        }
    }

    /** Tenta carregar de novo as leituras salvas depois de uma falha. */
    fun onRetryLoad() {
        _uiState.update { it.copy(loadError = false) }
        loadLastReading()
        observeChartDay()
    }

    fun onConnectClicked() {
        val state = _uiState.value
        // Já conectado ou conectando: um novo toque não abre outra conexão.
        if (state.device != null || state.isConnecting) return
        _uiState.update { it.copy(isConnecting = true, connectionError = false, connectionFailure = null) }
        viewModelScope.launch {
            try {
                val device = sensor.connect()
                _uiState.update { it.copy(device = device, isConnecting = false) }
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isConnecting = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao conectar ao dispositivo", e)
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        connectionError = true,
                        connectionFailure = (e as? SensorException)?.failure,
                    )
                }
            }
        }
    }

    /** O usuário não liberou as permissões de Bluetooth pedidas ao tocar em conectar. */
    fun onBluetoothPermissionDenied() {
        _uiState.update { it.copy(connectionError = true, connectionFailure = SensorFailure.PermissionDenied) }
    }

    /** Desconecta do dispositivo; uma leitura em andamento é cancelada. */
    fun onDisconnectClicked() {
        if (_uiState.value.device == null) return
        readingJob?.cancel()
        readingJob = null
        _uiState.update {
            it.copy(
                device = null,
                isReading = false,
                readingProgress = 0,
                readingError = false,
                readingFailure = null,
                connectionError = false,
                connectionFailure = null,
                isSimulatedData = it.lastReading?.isSimulated ?: sensor.isSimulated,
            )
        }
        viewModelScope.launch {
            try {
                sensor.disconnect()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // O app já se considera desconectado; só registra.
                Log.e(TAG, "Falha ao desconectar do dispositivo", e)
            }
        }
    }

    fun onReadClicked() {
        if (!_uiState.value.canRead) return
        // Atualiza já, antes de navegar, para a tela de Leitura nunca mostrar o resultado antigo.
        _uiState.update {
            it.copy(
                isReading = true,
                readingProgress = 0,
                isSimulatedData = sensor.isSimulated,
                readingError = false,
                readingFailure = null,
                // Uma leitura anterior que não foi gravada é descartada ao começar outra.
                saveError = false,
                // A oferta de aviso ao familiar vale só para a leitura que a gerou.
                familyAlert = null,
            )
        }
        unsavedReading = null
        readingJob = viewModelScope.launch {
            val sample = try {
                sensor.readOnce { progress ->
                    _uiState.update { it.copy(readingProgress = progress) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha durante a leitura", e)
                val failure = (e as? SensorException)?.failure
                val connectionLost = failure == SensorFailure.ConnectionLost
                _uiState.update {
                    it.copy(
                        isReading = false,
                        readingProgress = 0,
                        readingError = true,
                        readingFailure = failure,
                        // Conexão perdida: o aparelho deixa de contar como conectado e o usuário reconecta.
                        device = if (connectionLost) null else it.device,
                        connectionError = it.connectionError || connectionLost,
                        connectionFailure = if (connectionLost) failure else it.connectionFailure,
                        isSimulatedData = it.lastReading?.isSimulated ?: sensor.isSimulated,
                    )
                }
                return@launch
            }
            val reading = Reading(
                bpm = sample.bpm,
                spo2 = sample.spo2,
                timestamp = System.currentTimeMillis(),
                isSimulated = sensor.isSimulated,
            )
            // Mostra o resultado primeiro; a gravação vem depois e não atrasa a tela.
            _uiState.update {
                it.copy(
                    isReading = false,
                    readingProgress = 100,
                    lastReading = reading,
                    status = analyzeReading(reading),
                    isSimulatedData = reading.isSimulated,
                )
            }
            alertIfAbnormal(reading)
            offerFamilyAlert(reading)
            saveReading(reading)
        }
    }

    /**
     * Avisa o usuário (notificação) se a leitura não for Normal e o aviso estiver ligado no Perfil.
     * Roda só ao concluir uma leitura, nunca em "Salvar de novo", para não avisar duas vezes.
     * Uma falha aqui é registrada e não impede a gravação nem derruba o app. Se o perfil não puder
     * ser lido, o aviso não é enviado: sem saber a escolha do usuário, o padrão é respeitar o "desligado".
     */
    private suspend fun alertIfAbnormal(reading: Reading) {
        try {
            alertOnAbnormalReading(analyzeReading.analyze(reading))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao avisar sobre a leitura fora do normal", e)
        }
    }

    /**
     * Prepara a oferta de aviso ao familiar (leitura fora do normal, contato salvo e autorização
     * dada). Uma falha ao ler o banco é registrada e só deixa de mostrar a oferta: não impede a
     * gravação nem derruba o app.
     */
    private suspend fun offerFamilyAlert(reading: Reading) {
        try {
            val alert = prepareFamilyAlert(analyzeReading.analyze(reading))
            _uiState.update { it.copy(familyAlert = alert) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao preparar o aviso ao familiar", e)
        }
    }

    /**
     * Cancela a leitura em andamento. Sem leitura em andamento, só descarta o estado de erro da
     * leitura e a oferta de aviso ao familiar (é o que acontece ao sair da tela de Leitura).
     */
    fun onCancelReading() {
        if (!_uiState.value.isReading) {
            val state = _uiState.value
            if (state.readingError || state.familyAlert != null) {
                _uiState.update { it.copy(readingError = false, readingFailure = null, familyAlert = null) }
            }
            return
        }
        readingJob?.cancel()
        readingJob = null
        _uiState.update {
            it.copy(
                isReading = false,
                readingProgress = 0,
                isSimulatedData = it.lastReading?.isSimulated ?: sensor.isSimulated,
            )
        }
    }

    /** Grava de novo a leitura exibida quando a primeira gravação falhou (não faz nada se não houver pendência). */
    fun onRetrySave() {
        val pending = unsavedReading ?: return
        viewModelScope.launch { saveReading(pending) }
    }

    /**
     * Uma falha de gravação não pode derrubar o app: o resultado já está na tela, mas o usuário é
     * avisado e pode tentar de novo. [unsavedReading] fica vazio durante a tentativa, o que
     * também impede que toques repetidos gravem a mesma leitura duas vezes.
     */
    private suspend fun saveReading(reading: Reading) {
        unsavedReading = null
        try {
            repository.saveReading(reading)
            _uiState.update { it.copy(saveError = false) }
        } catch (e: CancellationException) {
            unsavedReading = reading
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao gravar a leitura", e)
            unsavedReading = reading
            _uiState.update { it.copy(saveError = true) }
        }
    }

    /** Ao destruir o ViewModel, libera a conexão com o sensor para não prender o medidor. */
    override fun onCleared() {
        sensor.release()
        super.onCleared()
    }
}
