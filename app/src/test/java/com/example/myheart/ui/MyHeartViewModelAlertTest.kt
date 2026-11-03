package com.example.myheart.ui

import com.example.myheart.data.sensor.SensorSample
import com.example.myheart.domain.model.UserProfile
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Aviso (notificação) ao concluir uma leitura: o ViewModel principal com os falsos de perfil e de notificação. */
@OptIn(ExperimentalCoroutinesApi::class)
class MyHeartViewModelAlertTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Sem nenhum contato de familiar: a notificação local só depende do switch do perfil.
    private val enabledProfile = UserProfile(name = "Maria", email = "maria@exemplo.com", notificationsEnabled = true)

    private class Setup(
        val viewModel: MyHeartViewModel,
        val readings: FakeReadingRepository,
        val profile: FakeProfileRepository,
        val notifier: FakeAlertNotifier,
    )

    /** Cria o ViewModel já conectado ao sensor que devolve [sample]. */
    private fun TestScope.setup(
        sample: SensorSample,
        profile: UserProfile? = enabledProfile,
    ): Setup {
        val readings = FakeReadingRepository()
        val profileRepository = FakeProfileRepository(profile = profile)
        val notifier = FakeAlertNotifier()
        val viewModel = MyHeartViewModel(
            sensor = FakeSensor(sample = sample),
            analyzeReading = AnalyzeReadingUseCase(),
            repository = readings,
            alertOnAbnormalReading = AlertOnAbnormalReadingUseCase(profileRepository, notifier),
            prepareFamilyAlert = PrepareFamilyAlertUseCase(profileRepository),
        )
        advanceUntilIdle()
        viewModel.onConnectClicked()
        advanceUntilIdle()
        return Setup(viewModel, readings, profileRepository, notifier)
    }

    private fun TestScope.read(setup: Setup) {
        setup.viewModel.onReadClicked()
        advanceUntilIdle()
    }

    @Test
    fun `leitura fora do normal com o aviso ligado notifica uma vez e grava`() = runTest {
        val s = setup(SensorSample(bpm = 72, spo2 = 85))
        read(s)

        assertEquals(1, s.notifier.requested.size)
        assertEquals(85, s.notifier.requested.single().reading.spo2)
        assertEquals(1, s.readings.saved.size)
    }

    @Test
    fun `leitura normal nao notifica`() = runTest {
        val s = setup(SensorSample(bpm = 72, spo2 = 98))
        read(s)

        assertTrue(s.notifier.requested.isEmpty())
        assertEquals(1, s.readings.saved.size)
    }

    @Test
    fun `aviso desligado nao notifica`() = runTest {
        val s = setup(SensorSample(bpm = 72, spo2 = 85), profile = enabledProfile.copy(notificationsEnabled = false))
        read(s)

        assertTrue(s.notifier.requested.isEmpty())
        assertEquals(1, s.readings.saved.size)
    }

    @Test
    fun `falha ao notificar nao derruba a leitura nem impede a gravacao`() = runTest {
        val s = setup(SensorSample(bpm = 130 + 10, spo2 = 98))
        s.notifier.fail = true
        read(s)

        val state = s.viewModel.uiState.value
        assertNotNull(state.lastReading)
        assertFalse(state.saveError)
        assertFalse(state.readingError)
        assertEquals(1, s.readings.saved.size)
    }

    @Test
    fun `falha ao ler o perfil nao notifica mas a leitura e gravada`() = runTest {
        val s = setup(SensorSample(bpm = 72, spo2 = 85))
        s.profile.failLoad = true
        read(s)

        assertTrue(s.notifier.requested.isEmpty())
        assertFalse(s.viewModel.uiState.value.saveError)
        assertEquals(1, s.readings.saved.size)
    }

    @Test
    fun `salvar de novo nao repete o aviso`() = runTest {
        val s = setup(SensorSample(bpm = 72, spo2 = 85))
        s.readings.failSave = true
        read(s)
        assertTrue(s.viewModel.uiState.value.saveError)
        assertEquals(1, s.notifier.requested.size)

        s.readings.failSave = false
        s.viewModel.onRetrySave()
        advanceUntilIdle()

        assertEquals(1, s.notifier.requested.size)
        assertEquals(1, s.readings.saved.size)
        assertFalse(s.viewModel.uiState.value.saveError)
    }
}
