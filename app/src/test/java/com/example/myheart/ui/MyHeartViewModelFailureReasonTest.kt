package com.example.myheart.ui

import com.example.myheart.data.sensor.SensorFailure
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
 * Motivos de falha do sensor (Etapa 6): o ViewModel guarda o motivo conhecido para a tela explicar
 * o que fazer, e a conexão perdida durante a leitura derruba o dispositivo para o usuário reconectar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MyHeartViewModelFailureReasonTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createViewModel(
        sensor: FakeSensor,
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

    private fun TestScope.connect(viewModel: MyHeartViewModel) {
        viewModel.onConnectClicked()
        advanceUntilIdle()
    }

    private fun TestScope.read(viewModel: MyHeartViewModel) {
        viewModel.onReadClicked()
        advanceUntilIdle()
    }

    // ---------------------------------------------------------------- Conexão

    @Test
    fun `falha de conexao com motivo guarda o motivo`() = runTest {
        val sensor = FakeSensor().apply { connectFailure = SensorFailure.DeviceNotFound }
        val viewModel = createViewModel(sensor)

        connect(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.connectionError)
        assertEquals(SensorFailure.DeviceNotFound, state.connectionFailure)
        assertNull(state.device)
        assertFalse(state.isConnecting)
    }

    @Test
    fun `falha generica de conexao nao tem motivo`() = runTest {
        val sensor = FakeSensor().apply { failConnect = true }
        val viewModel = createViewModel(sensor)

        connect(viewModel)

        assertTrue(viewModel.uiState.value.connectionError)
        assertNull(viewModel.uiState.value.connectionFailure)
    }

    @Test
    fun `nova tentativa limpa o motivo anterior`() = runTest {
        val sensor = FakeSensor().apply { connectFailure = SensorFailure.BluetoothDisabled }
        val viewModel = createViewModel(sensor)
        connect(viewModel)
        assertEquals(SensorFailure.BluetoothDisabled, viewModel.uiState.value.connectionFailure)

        sensor.connectFailure = null
        connect(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.connectionError)
        assertNull(state.connectionFailure)
        assertNotNull(state.device)
    }

    @Test
    fun `permissao negada mostra o erro de conexao sem tentar conectar`() = runTest {
        val sensor = FakeSensor()
        val viewModel = createViewModel(sensor)

        viewModel.onBluetoothPermissionDenied()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.connectionError)
        assertEquals(SensorFailure.PermissionDenied, state.connectionFailure)
        assertFalse(state.isConnecting)
        assertEquals(0, sensor.connectCalls)
    }

    // ---------------------------------------------------------------- Leitura

    @Test
    fun `sem sinal na leitura mantem o dispositivo e guarda o motivo`() = runTest {
        val sensor = FakeSensor().apply { readFailure = SensorFailure.NoSignal }
        val repository = FakeReadingRepository()
        val viewModel = createViewModel(sensor, repository)
        connect(viewModel)

        read(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.readingError)
        assertEquals(SensorFailure.NoSignal, state.readingFailure)
        assertNotNull(state.device)
        assertTrue(state.canRead)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `conexao perdida na leitura derruba o dispositivo`() = runTest {
        val sensor = FakeSensor().apply { readFailure = SensorFailure.ConnectionLost }
        val viewModel = createViewModel(sensor)
        connect(viewModel)

        read(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.readingError)
        assertEquals(SensorFailure.ConnectionLost, state.readingFailure)
        assertNull(state.device)
        assertFalse(state.canRead)
        assertTrue(state.connectionError)
        assertEquals(SensorFailure.ConnectionLost, state.connectionFailure)
    }

    @Test
    fun `sair do estado de erro limpa o motivo da leitura`() = runTest {
        val sensor = FakeSensor().apply { readFailure = SensorFailure.NoSignal }
        val viewModel = createViewModel(sensor)
        connect(viewModel)
        read(viewModel)

        viewModel.onCancelReading()

        assertFalse(viewModel.uiState.value.readingError)
        assertNull(viewModel.uiState.value.readingFailure)
    }

    @Test
    fun `nova leitura limpa o motivo da falha anterior`() = runTest {
        val sensor = FakeSensor().apply { readFailure = SensorFailure.NoSignal }
        val viewModel = createViewModel(sensor)
        connect(viewModel)
        read(viewModel)
        assertEquals(SensorFailure.NoSignal, viewModel.uiState.value.readingFailure)

        sensor.readFailure = null
        read(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.readingError)
        assertNull(state.readingFailure)
        assertNotNull(state.lastReading)
    }

    @Test
    fun `desconectar limpa os motivos de falha`() = runTest {
        val sensor = FakeSensor().apply { readFailure = SensorFailure.NoSignal }
        val viewModel = createViewModel(sensor)
        connect(viewModel)
        read(viewModel)

        viewModel.onDisconnectClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.device)
        assertNull(state.readingFailure)
        assertNull(state.connectionFailure)
        assertFalse(state.readingError)
    }
}
