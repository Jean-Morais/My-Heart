package com.example.myheart.ui

import com.example.myheart.data.sensor.SensorSample
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.usecase.AlertOnAbnormalReadingUseCase
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.domain.usecase.PrepareFamilyAlertUseCase
import com.example.myheart.testing.FakeAlertNotifier
import com.example.myheart.testing.FakeProfileRepository
import com.example.myheart.testing.FakeReadingRepository
import com.example.myheart.testing.FakeSensor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fluxo de leitura do ViewModel principal: conexão, leitura concluída, cancelamento e recuperação
 * da última leitura. O tempo é virtual: os testes avançam o relógio em vez de esperar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyHeartViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(
        sensor: FakeSensor = FakeSensor(),
        repository: FakeReadingRepository = FakeReadingRepository(),
    ): MyHeartViewModel {
        val viewModel = MyHeartViewModel(
            sensor,
            AnalyzeReadingUseCase(),
            repository,
            AlertOnAbnormalReadingUseCase(FakeProfileRepository(), FakeAlertNotifier()),
            PrepareFamilyAlertUseCase(FakeProfileRepository()),
        )
        advanceUntilIdle()
        return viewModel
    }

    private fun TestScope.connected(viewModel: MyHeartViewModel) {
        viewModel.onConnectClicked()
        advanceUntilIdle()
    }

    // ---------------------------------------------------------------- Conexão

    @Test
    fun `sem dispositivo nao e possivel ler`() = runTest {
        val sensor = FakeSensor()
        val viewModel = createViewModel(sensor)

        assertFalse(viewModel.uiState.value.canRead)
        viewModel.onReadClicked()
        advanceUntilIdle()

        assertEquals(0, sensor.readCalls)
        assertFalse(viewModel.uiState.value.isReading)
    }

    @Test
    fun `conectar guarda o dispositivo e habilita a leitura`() = runTest {
        val viewModel = createViewModel()
        connected(viewModel)

        assertNotNull(viewModel.uiState.value.device)
        assertTrue(viewModel.uiState.value.canRead)
    }

    // ---------------------------------------------------------------- Leitura concluída

    @Test
    fun `leitura concluida mostra o resultado classifica e grava`() = runTest {
        val sensor = FakeSensor(sample = SensorSample(bpm = 72, spo2 = 98))
        val repository = FakeReadingRepository()
        val viewModel = createViewModel(sensor, repository)
        connected(viewModel)

        viewModel.onReadClicked()
        // A tela de Leitura precisa ver "em andamento" já na primeira atualização.
        assertTrue(viewModel.uiState.value.isReading)
        assertEquals(0, viewModel.uiState.value.readingProgress)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReading)
        assertEquals(100, state.readingProgress)
        assertEquals(72, state.lastReading?.bpm)
        assertEquals(98, state.lastReading?.spo2)
        assertEquals(ReadingStatus.Normal, state.status)
        assertEquals(1, repository.saved.size)
        assertEquals(72, repository.saved.first().bpm)
    }

    @Test
    fun `leitura fora da faixa e classificada como critica`() = runTest {
        val sensor = FakeSensor(sample = SensorSample(bpm = 140, spo2 = 98))
        val viewModel = createViewModel(sensor)
        connected(viewModel)

        viewModel.onReadClicked()
        advanceUntilIdle()

        assertEquals(ReadingStatus.Critical, viewModel.uiState.value.status)
    }

    @Test
    fun `a leitura grava se o sensor e simulado`() = runTest {
        val repository = FakeReadingRepository()
        val viewModel = createViewModel(FakeSensor(isSimulated = false), repository)
        connected(viewModel)

        viewModel.onReadClicked()
        advanceUntilIdle()

        assertFalse(repository.saved.single().isSimulated)
        assertFalse(viewModel.uiState.value.isSimulatedData)
    }

    @Test
    fun `leitura nova aparece no grafico do dia`() = runTest {
        val viewModel = createViewModel()
        connected(viewModel)
        assertTrue(viewModel.uiState.value.chartReadings.isEmpty())
        assertNull(viewModel.uiState.value.chartDay)

        viewModel.onReadClicked()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.chartReadings.size)
        assertNotNull(viewModel.uiState.value.chartDay)
    }

    @Test
    fun `toque repetido durante a leitura nao inicia outra`() = runTest {
        val sensor = FakeSensor()
        val viewModel = createViewModel(sensor)
        connected(viewModel)

        viewModel.onReadClicked()
        viewModel.onReadClicked()
        advanceUntilIdle()

        assertEquals(1, sensor.readCalls)
    }

    // ---------------------------------------------------------------- Cancelamento

    @Test
    fun `cancelar interrompe a leitura sem gravar nada`() = runTest {
        val repository = FakeReadingRepository()
        val viewModel = createViewModel(FakeSensor(), repository)
        connected(viewModel)

        viewModel.onReadClicked()
        advanceTimeBy(500)
        runCurrent()
        assertTrue(viewModel.uiState.value.isReading)
        assertEquals(50, viewModel.uiState.value.readingProgress)

        viewModel.onCancelReading()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isReading)
        assertEquals(0, state.readingProgress)
        assertNull(state.lastReading)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `cancelar sem leitura em andamento nao muda nada`() = runTest {
        val viewModel = createViewModel()
        connected(viewModel)
        val before = viewModel.uiState.value

        viewModel.onCancelReading()

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `depois de cancelar e possivel ler de novo`() = runTest {
        val repository = FakeReadingRepository()
        val viewModel = createViewModel(FakeSensor(), repository)
        connected(viewModel)

        viewModel.onReadClicked()
        advanceTimeBy(300)
        viewModel.onCancelReading()
        advanceUntilIdle()

        viewModel.onReadClicked()
        advanceUntilIdle()

        assertEquals(1, repository.saved.size)
        assertNotNull(viewModel.uiState.value.lastReading)
    }

    // ---------------------------------------------------------------- Recuperação ao abrir o app

    @Test
    fun `ao abrir recupera a ultima leitura gravada`() = runTest {
        val saved = Reading(bpm = 88, spo2 = 93, timestamp = 1_700_000_000_000, isSimulated = true, id = 1)
        val viewModel = createViewModel(repository = FakeReadingRepository(listOf(saved)))

        val state = viewModel.uiState.value
        assertEquals(saved, state.lastReading)
        // 93% de SpO2 é Atenção pela regra de classificação.
        assertEquals(ReadingStatus.Attention, state.status)
        assertTrue(state.isSimulatedData)
    }

    // ---------------------------------------------------------------- Falhas do banco

    @Test
    fun `falha ao gravar mantem o resultado na tela e avisa o usuario`() = runTest {
        val repository = FakeReadingRepository().apply { failSave = true }
        val viewModel = createViewModel(FakeSensor(sample = SensorSample(bpm = 72, spo2 = 98)), repository)
        connected(viewModel)

        viewModel.onReadClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(72, state.lastReading?.bpm)
        assertEquals(ReadingStatus.Normal, state.status)
        assertFalse(state.isReading)
        assertTrue(state.saveError)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `salvar de novo grava a leitura e limpa o aviso`() = runTest {
        val repository = FakeReadingRepository().apply { failSave = true }
        val viewModel = createViewModel(repository = repository)
        connected(viewModel)
        viewModel.onReadClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saveError)

        repository.failSave = false
        viewModel.onRetrySave()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saveError)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `salvar de novo que falha de novo mantem o aviso`() = runTest {
        val repository = FakeReadingRepository().apply { failSave = true }
        val viewModel = createViewModel(repository = repository)
        connected(viewModel)
        viewModel.onReadClicked()
        advanceUntilIdle()

        viewModel.onRetrySave()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveError)
        assertEquals(2, repository.saveCalls)
        // Ainda dá para tentar mais uma vez.
        repository.failSave = false
        viewModel.onRetrySave()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.saveError)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `salvar de novo sem pendencia nao grava nada`() = runTest {
        val repository = FakeReadingRepository()
        val viewModel = createViewModel(repository = repository)
        connected(viewModel)
        viewModel.onReadClicked()
        advanceUntilIdle()
        assertEquals(1, repository.saveCalls)

        viewModel.onRetrySave()
        advanceUntilIdle()

        assertEquals(1, repository.saveCalls)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `nova leitura descarta a pendencia da anterior`() = runTest {
        val repository = FakeReadingRepository().apply { failSave = true }
        val viewModel = createViewModel(repository = repository)
        connected(viewModel)
        viewModel.onReadClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saveError)

        repository.failSave = false
        viewModel.onReadClicked()
        assertFalse(viewModel.uiState.value.saveError)
        advanceUntilIdle()
        viewModel.onRetrySave() // não há mais nada pendente

        assertFalse(viewModel.uiState.value.saveError)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `falha ao carregar avisa e tentar de novo recupera as leituras`() = runTest {
        val saved = Reading(bpm = 88, spo2 = 93, timestamp = 1_700_000_000_000, isSimulated = true, id = 1)
        val repository = FakeReadingRepository(listOf(saved)).apply { failLoad = true }
        val viewModel = createViewModel(repository = repository)

        assertTrue(viewModel.uiState.value.loadError)
        assertNull(viewModel.uiState.value.lastReading)

        repository.failLoad = false
        viewModel.onRetryLoad()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loadError)
        assertEquals(saved, state.lastReading)
        assertEquals(1, state.chartReadings.size)
    }

    // ---------------------------------------------------------------- Conexão: toques repetidos e falhas

    @Test
    fun `toque repetido em conectar abre uma unica conexao`() = runTest {
        val sensor = FakeSensor(connectDurationMillis = 500)
        val viewModel = createViewModel(sensor)

        viewModel.onConnectClicked()
        assertTrue(viewModel.uiState.value.isConnecting)
        viewModel.onConnectClicked()
        advanceUntilIdle()

        assertEquals(1, sensor.connectCalls)
        assertFalse(viewModel.uiState.value.isConnecting)
        assertNotNull(viewModel.uiState.value.device)
    }

    @Test
    fun `conectar com dispositivo ja conectado nao reconecta`() = runTest {
        val sensor = FakeSensor()
        val viewModel = createViewModel(sensor)
        connected(viewModel)

        viewModel.onConnectClicked()
        advanceUntilIdle()

        assertEquals(1, sensor.connectCalls)
    }

    @Test
    fun `falha ao conectar mostra o erro e permite tentar de novo`() = runTest {
        val sensor = FakeSensor().apply { failConnect = true }
        val viewModel = createViewModel(sensor)

        viewModel.onConnectClicked()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertNull(state.device)
        assertFalse(state.isConnecting)
        assertTrue(state.connectionError)
        assertFalse(state.canRead)

        sensor.failConnect = false
        viewModel.onConnectClicked()
        assertFalse(viewModel.uiState.value.connectionError) // o aviso some assim que a nova tentativa começa
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertNotNull(state.device)
        assertFalse(state.connectionError)
        assertEquals(2, sensor.connectCalls)
    }

    // ---------------------------------------------------------------- Desconexão

    @Test
    fun `desconectar limpa o dispositivo e desabilita a leitura`() = runTest {
        val sensor = FakeSensor()
        val viewModel = createViewModel(sensor)
        connected(viewModel)

        viewModel.onDisconnectClicked()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.device)
        assertFalse(viewModel.uiState.value.canRead)
        assertEquals(1, sensor.disconnectCalls)
    }

    @Test
    fun `desconectar sem dispositivo nao chama o sensor`() = runTest {
        val sensor = FakeSensor()
        val viewModel = createViewModel(sensor)

        viewModel.onDisconnectClicked()
        advanceUntilIdle()

        assertEquals(0, sensor.disconnectCalls)
    }

    @Test
    fun `desconectar durante a leitura cancela sem gravar nada`() = runTest {
        val sensor = FakeSensor()
        val repository = FakeReadingRepository()
        val viewModel = createViewModel(sensor, repository)
        connected(viewModel)
        viewModel.onReadClicked()
        advanceTimeBy(400)
        runCurrent()
        assertTrue(viewModel.uiState.value.isReading)

        viewModel.onDisconnectClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.device)
        assertFalse(state.isReading)
        assertEquals(0, state.readingProgress)
        assertFalse(state.readingError)
        assertTrue(repository.saved.isEmpty())
        assertEquals(1, sensor.disconnectCalls)
    }

    @Test
    fun `depois de desconectar e possivel conectar de novo`() = runTest {
        val sensor = FakeSensor()
        val viewModel = createViewModel(sensor)
        connected(viewModel)
        viewModel.onDisconnectClicked()
        advanceUntilIdle()

        connected(viewModel)

        assertNotNull(viewModel.uiState.value.device)
        assertEquals(2, sensor.connectCalls)
    }

    // ---------------------------------------------------------------- Falha na leitura

    @Test
    fun `falha na leitura mostra o erro sem gravar e mantem a ultima leitura`() = runTest {
        val previous = Reading(bpm = 80, spo2 = 97, timestamp = 1_700_000_000_000, isSimulated = true, id = 1)
        val repository = FakeReadingRepository(listOf(previous))
        val sensor = FakeSensor().apply { failRead = true }
        val viewModel = createViewModel(sensor, repository)
        connected(viewModel)

        viewModel.onReadClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.readingError)
        assertFalse(state.isReading)
        assertEquals(0, state.readingProgress)
        assertEquals(previous, state.lastReading)
        assertEquals(1, repository.saved.size)
        // O dispositivo continua conectado: dá para tentar de novo.
        assertNotNull(state.device)
        assertTrue(state.canRead)
    }

    @Test
    fun `tentar de novo depois da falha conclui a leitura e limpa o erro`() = runTest {
        val repository = FakeReadingRepository()
        val sensor = FakeSensor().apply { failRead = true }
        val viewModel = createViewModel(sensor, repository)
        connected(viewModel)
        viewModel.onReadClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.readingError)

        sensor.failRead = false
        viewModel.onReadClicked()
        assertFalse(viewModel.uiState.value.readingError)
        assertTrue(viewModel.uiState.value.isReading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.readingError)
        assertEquals(72, state.lastReading?.bpm)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun `sair da tela de leitura descarta o erro`() = runTest {
        val sensor = FakeSensor().apply { failRead = true }
        val viewModel = createViewModel(sensor)
        connected(viewModel)
        viewModel.onReadClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.readingError)

        viewModel.onCancelReading()

        assertFalse(viewModel.uiState.value.readingError)
    }
}
