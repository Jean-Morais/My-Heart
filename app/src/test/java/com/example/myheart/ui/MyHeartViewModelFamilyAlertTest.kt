package com.example.myheart.ui

import com.example.myheart.data.sensor.SensorSample
import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.domain.model.RelativeContact
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Oferta de aviso ao familiar no ViewModel principal (o envio em si é manual e fica fora do ViewModel). */
@OptIn(ExperimentalCoroutinesApi::class)
class MyHeartViewModelFamilyAlertTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val abnormal = SensorSample(bpm = 72, spo2 = 85)
    private val normal = SensorSample(bpm = 72, spo2 = 98)
    private val authorizedContact = RelativeContact("Ana", NotifyChannel.Phone, "(88) 99999-8888", consent = true)

    private class Setup(
        val viewModel: MyHeartViewModel,
        val sensor: FakeSensor,
        val readings: FakeReadingRepository,
        val profile: FakeProfileRepository,
    )

    private fun TestScope.setup(
        sample: SensorSample = abnormal,
        contact: RelativeContact? = authorizedContact,
    ): Setup {
        val sensor = FakeSensor(sample = sample)
        val readings = FakeReadingRepository()
        val profile = FakeProfileRepository(
            profile = UserProfile(name = "Maria", email = "maria@exemplo.com", notificationsEnabled = false),
            contact = contact,
        )
        val viewModel = MyHeartViewModel(
            sensor = sensor,
            analyzeReading = AnalyzeReadingUseCase(),
            repository = readings,
            alertOnAbnormalReading = AlertOnAbnormalReadingUseCase(profile, FakeAlertNotifier()),
            prepareFamilyAlert = PrepareFamilyAlertUseCase(profile),
        )
        advanceUntilIdle()
        viewModel.onConnectClicked()
        advanceUntilIdle()
        return Setup(viewModel, sensor, readings, profile)
    }

    private fun TestScope.read(s: Setup) {
        s.viewModel.onReadClicked()
        advanceUntilIdle()
    }

    @Test
    fun `leitura fora do normal com contato autorizado oferece o aviso`() = runTest {
        val s = setup()
        read(s)

        val alert = s.viewModel.uiState.value.familyAlert
        assertNotNull(alert)
        assertEquals("Ana", alert!!.relativeName)
        assertEquals(NotifyChannel.Phone, alert.channel)
        assertEquals("Maria", alert.userName)
        assertEquals(85, alert.reading.reading.spo2)
    }

    @Test
    fun `leitura normal nao oferece o aviso`() = runTest {
        val s = setup(sample = normal)
        read(s)

        assertNull(s.viewModel.uiState.value.familyAlert)
    }

    @Test
    fun `sem autorizacao ou sem contato nao oferece o aviso`() = runTest {
        val semAutorizacao = setup(contact = authorizedContact.copy(consent = false))
        read(semAutorizacao)
        assertNull(semAutorizacao.viewModel.uiState.value.familyAlert)

        val semContato = setup(contact = null)
        read(semContato)
        assertNull(semContato.viewModel.uiState.value.familyAlert)
    }

    @Test
    fun `falha ao ler o contato nao oferece o aviso mas a leitura e gravada`() = runTest {
        val s = setup()
        s.profile.failLoad = true
        read(s)

        val state = s.viewModel.uiState.value
        assertNull(state.familyAlert)
        assertNotNull(state.lastReading)
        assertEquals(1, s.readings.saved.size)
    }

    @Test
    fun `comecar outra leitura descarta a oferta anterior`() = runTest {
        val s = setup()
        read(s)
        assertNotNull(s.viewModel.uiState.value.familyAlert)

        s.sensor.sample = normal
        read(s)

        assertNull(s.viewModel.uiState.value.familyAlert)
    }

    @Test
    fun `sair da tela de leitura descarta a oferta`() = runTest {
        val s = setup()
        read(s)
        assertNotNull(s.viewModel.uiState.value.familyAlert)

        s.viewModel.onCancelReading()

        assertNull(s.viewModel.uiState.value.familyAlert)
        // A leitura em si continua exibida e gravada.
        assertNotNull(s.viewModel.uiState.value.lastReading)
        assertEquals(1, s.readings.saved.size)
    }

    @Test
    fun `a oferta aparece mesmo com o switch de notificacao desligado`() = runTest {
        val s = setup()
        // O perfil do setup tem notificationsEnabled = false.
        read(s)

        assertNotNull(s.viewModel.uiState.value.familyAlert)
    }
}
