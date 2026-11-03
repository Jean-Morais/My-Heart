package com.example.myheart.domain

import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.UserProfile
import com.example.myheart.domain.usecase.AlertOnAbnormalReadingUseCase
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.testing.FakeAlertNotifier
import com.example.myheart.testing.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Quando uma leitura gera aviso: nível da leitura e switch do perfil. */
class AlertOnAbnormalReadingUseCaseTest {

    private fun analyzed(bpm: Int, spo2: Int): AnalyzedReading =
        AnalyzeReadingUseCase().analyze(Reading(bpm = bpm, spo2 = spo2, timestamp = 0L))

    private fun profile(enabled: Boolean = true) =
        UserProfile(name = "Maria", email = "maria@exemplo.com", notificationsEnabled = enabled)

    private val attention = analyzed(bpm = 110, spo2 = 98)
    private val critical = analyzed(bpm = 72, spo2 = 85)
    private val normal = analyzed(bpm = 72, spo2 = 98)

    @Test
    fun `leitura normal nao avisa`() = runTest {
        val notifier = FakeAlertNotifier()
        val useCase = AlertOnAbnormalReadingUseCase(FakeProfileRepository(profile = profile()), notifier)

        assertFalse(useCase(normal))
        assertTrue(notifier.requested.isEmpty())
    }

    @Test
    fun `leitura em atencao avisa quando o aviso esta ligado`() = runTest {
        val notifier = FakeAlertNotifier()
        val useCase = AlertOnAbnormalReadingUseCase(FakeProfileRepository(profile = profile()), notifier)

        assertTrue(useCase(attention))
        assertEquals(listOf(attention), notifier.requested)
    }

    @Test
    fun `leitura critica avisa quando o aviso esta ligado`() = runTest {
        val notifier = FakeAlertNotifier()
        val useCase = AlertOnAbnormalReadingUseCase(FakeProfileRepository(profile = profile()), notifier)

        assertTrue(useCase(critical))
        assertEquals(listOf(critical), notifier.requested)
    }

    @Test
    fun `avisa mesmo sem contato de familiar cadastrado`() = runTest {
        val notifier = FakeAlertNotifier()
        val repository = FakeProfileRepository(profile = profile(), contact = null)
        val useCase = AlertOnAbnormalReadingUseCase(repository, notifier)

        assertTrue(useCase(critical))
        assertEquals(1, notifier.requested.size)
    }

    @Test
    fun `sem perfil salvo nao avisa`() = runTest {
        val notifier = FakeAlertNotifier()
        val useCase = AlertOnAbnormalReadingUseCase(FakeProfileRepository(profile = null), notifier)

        assertFalse(useCase(critical))
        assertTrue(notifier.requested.isEmpty())
    }

    @Test
    fun `aviso desligado nao avisa`() = runTest {
        val notifier = FakeAlertNotifier()
        val useCase = AlertOnAbnormalReadingUseCase(FakeProfileRepository(profile = profile(enabled = false)), notifier)

        assertFalse(useCase(critical))
        assertTrue(notifier.requested.isEmpty())
    }

    @Test
    fun `devolve false quando o aparelho nao deixa publicar a notificacao`() = runTest {
        val notifier = FakeAlertNotifier().apply { published = false }
        val useCase = AlertOnAbnormalReadingUseCase(FakeProfileRepository(profile = profile()), notifier)

        assertFalse(useCase(critical))
        assertEquals(1, notifier.requested.size)
    }
}
