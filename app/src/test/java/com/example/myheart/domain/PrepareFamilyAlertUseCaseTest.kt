package com.example.myheart.domain

import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.RelativeContact
import com.example.myheart.domain.model.UserProfile
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.domain.usecase.PrepareFamilyAlertUseCase
import com.example.myheart.testing.FakeProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/** Regras da oferta de aviso ao familiar: leitura fora do normal, contato salvo e autorização dada. */
class PrepareFamilyAlertUseCaseTest {

    private val analyze = AnalyzeReadingUseCase()

    private fun normal() = analyze.analyze(Reading(bpm = 72, spo2 = 98, timestamp = 1_000L))
    private fun attention() = analyze.analyze(Reading(bpm = 105, spo2 = 98, timestamp = 1_000L))
    private fun critical() = analyze.analyze(Reading(bpm = 72, spo2 = 85, timestamp = 1_000L))

    private fun contact(consent: Boolean = true, channel: NotifyChannel = NotifyChannel.Email) =
        RelativeContact(name = "Ana", channel = channel, contact = "ana@exemplo.com", consent = consent)

    @Test
    fun `leitura normal nao gera oferta`() = runTest {
        val repo = FakeProfileRepository(contact = contact())
        assertNull(PrepareFamilyAlertUseCase(repo)(normal()))
    }

    @Test
    fun `sem contato salvo nao gera oferta`() = runTest {
        val repo = FakeProfileRepository(contact = null)
        assertNull(PrepareFamilyAlertUseCase(repo)(critical()))
    }

    @Test
    fun `sem autorizacao do usuario nao gera oferta`() = runTest {
        val repo = FakeProfileRepository(contact = contact(consent = false))
        assertNull(PrepareFamilyAlertUseCase(repo)(critical()))
    }

    @Test
    fun `leitura em atencao com contato autorizado gera oferta com os dados`() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(name = "Maria", email = "maria@exemplo.com"),
            contact = contact(),
        )
        val alert = PrepareFamilyAlertUseCase(repo)(attention())

        assertNotNull(alert)
        assertEquals("Maria", alert!!.userName)
        assertEquals("Ana", alert.relativeName)
        assertEquals(NotifyChannel.Email, alert.channel)
        assertEquals("ana@exemplo.com", alert.contact)
        assertEquals(105, alert.reading.reading.bpm)
    }

    @Test
    fun `leitura critica com contato por telefone gera oferta`() = runTest {
        val repo = FakeProfileRepository(
            contact = RelativeContact("Ana", NotifyChannel.Phone, "(88) 99999-8888", consent = true),
        )
        val alert = PrepareFamilyAlertUseCase(repo)(critical())

        assertEquals(NotifyChannel.Phone, alert!!.channel)
        assertEquals("(88) 99999-8888", alert.contact)
    }

    @Test
    fun `a oferta nao depende do switch de notificacao do perfil`() = runTest {
        val repo = FakeProfileRepository(
            profile = UserProfile(name = "Maria", email = "m@exemplo.com", notificationsEnabled = false),
            contact = contact(),
        )
        assertNotNull(PrepareFamilyAlertUseCase(repo)(critical()))
    }

    @Test
    fun `perfil sem nome ou nao salvo deixa o nome do usuario vazio`() = runTest {
        val semPerfil = FakeProfileRepository(profile = null, contact = contact())
        assertNull(PrepareFamilyAlertUseCase(semPerfil)(critical())!!.userName)

        val nomeEmBranco = FakeProfileRepository(profile = UserProfile(name = "   ", email = "m@exemplo.com"), contact = contact())
        assertNull(PrepareFamilyAlertUseCase(nomeEmBranco)(critical())!!.userName)
    }

    @Test
    fun `falha ao ler o banco e lancada para quem chama decidir`() = runTest {
        val repo = FakeProfileRepository(contact = contact()).apply { failLoad = true }
        try {
            PrepareFamilyAlertUseCase(repo)(critical())
            fail("era esperada uma exceção")
        } catch (e: IOException) {
            assertTrue(e.message!!.isNotEmpty())
        }
    }
}
