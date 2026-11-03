package com.example.myheart.ui.profile

import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.domain.model.RelativeContact
import com.example.myheart.domain.model.UserProfile
import com.example.myheart.testing.FakeProfileRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Cria o ViewModel e deixa a carga inicial terminar. */
    private fun TestScope.createViewModel(repository: FakeProfileRepository): ProfileViewModel {
        val viewModel = ProfileViewModel(repository)
        advanceUntilIdle()
        return viewModel
    }

    // ---------------------------------------------------------------- Carga

    @Test
    fun `sem dados salvos os campos comecam vazios`() = runTest {
        val viewModel = createViewModel(FakeProfileRepository())
        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.relativeName)
        assertFalse(state.consent)
    }

    @Test
    fun `dados salvos preenchem os campos ao abrir`() = runTest {
        val repository = FakeProfileRepository(
            profile = UserProfile(name = "Maria", email = "maria@exemplo.com"),
            contact = RelativeContact("Joao", NotifyChannel.Phone, "(88) 99999-1234", consent = true),
        )
        val state = createViewModel(repository).uiState.value
        assertEquals("Maria", state.name)
        assertEquals("maria@exemplo.com", state.email)
        assertEquals("Joao", state.relativeName)
        assertEquals(NotifyChannel.Phone, state.channel)
        // O dado salvo vai só para o campo do canal a que pertence.
        assertEquals("(88) 99999-1234", state.relativePhone)
        assertEquals("", state.relativeEmail)
        assertTrue(state.consent)
    }

    // ---------------------------------------------------------------- Dados do usuário

    @Test
    fun `nome vazio nao grava e mostra erro`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onNameChange("   ")
        viewModel.onEmailChange("maria@exemplo.com")
        viewModel.onSaveAccountClick()
        advanceUntilIdle()

        assertEquals(FieldError.Required, viewModel.uiState.value.nameError)
        assertNull(viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.accountSaved)
        assertEquals(0, repository.saveProfileCalls)
    }

    @Test
    fun `email vazio e email invalido tem erros diferentes`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onNameChange("Maria")

        viewModel.onSaveAccountClick()
        assertEquals(FieldError.Required, viewModel.uiState.value.emailError)

        viewModel.onEmailChange("maria@")
        viewModel.onSaveAccountClick()
        assertEquals(FieldError.InvalidEmail, viewModel.uiState.value.emailError)

        advanceUntilIdle()
        assertEquals(0, repository.saveProfileCalls)
    }

    @Test
    fun `perfil valido grava sem espacos nas pontas e confirma`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onNameChange("  Maria  ")
        viewModel.onEmailChange(" maria@exemplo.com ")
        viewModel.onSaveAccountClick()
        advanceUntilIdle()

        assertEquals(UserProfile(name = "Maria", email = "maria@exemplo.com"), repository.profile)
        val state = viewModel.uiState.value
        assertEquals("Maria", state.name)
        assertEquals("maria@exemplo.com", state.email)
        assertTrue(state.accountSaved)
    }

    @Test
    fun `editar um campo limpa o erro e a confirmacao`() = runTest {
        val viewModel = createViewModel(FakeProfileRepository())
        viewModel.onSaveAccountClick()
        assertEquals(FieldError.Required, viewModel.uiState.value.nameError)

        viewModel.onNameChange("M")
        assertNull(viewModel.uiState.value.nameError)
        assertFalse(viewModel.uiState.value.accountSaved)
    }

    // ---------------------------------------------------------------- Contato do familiar

    @Test
    fun `contato sem autorizacao nao grava`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onRelativeNameChange("Joao")
        viewModel.onRelativeEmailChange("joao@exemplo.com")
        viewModel.onSaveContactClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.consentError)
        assertFalse(viewModel.uiState.value.contactSaved)
        assertEquals(0, repository.saveContactCalls)
    }

    @Test
    fun `contato por email grava so o email`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onRelativeNameChange(" Joao ")
        viewModel.onChannelChange(NotifyChannel.Email)
        viewModel.onRelativeEmailChange(" joao@exemplo.com ")
        viewModel.onRelativePhoneChange("123") // dado do outro canal: não deve ser validado nem gravado
        viewModel.onConsentChange(true)
        viewModel.onSaveContactClick()
        advanceUntilIdle()

        assertEquals(
            RelativeContact("Joao", NotifyChannel.Email, "joao@exemplo.com", consent = true),
            repository.contact,
        )
        assertTrue(viewModel.uiState.value.contactSaved)
    }

    @Test
    fun `telefone com 9 digitos e invalido e com 11 e valido mesmo formatado`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onRelativeNameChange("Joao")
        viewModel.onChannelChange(NotifyChannel.Phone)
        viewModel.onConsentChange(true)

        viewModel.onRelativePhoneChange("999991234")
        viewModel.onSaveContactClick()
        assertEquals(FieldError.InvalidPhone, viewModel.uiState.value.relativeContactError)

        viewModel.onRelativePhoneChange("(88) 99999-1234")
        viewModel.onSaveContactClick()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.relativeContactError)
        assertEquals(NotifyChannel.Phone, repository.contact?.channel)
        assertEquals("(88) 99999-1234", repository.contact?.contact)
    }

    @Test
    fun `telefone vazio pede o preenchimento`() = runTest {
        val viewModel = createViewModel(FakeProfileRepository())
        viewModel.onRelativeNameChange("Joao")
        viewModel.onChannelChange(NotifyChannel.Phone)
        viewModel.onConsentChange(true)
        viewModel.onSaveContactClick()
        assertEquals(FieldError.Required, viewModel.uiState.value.relativeContactError)
    }

    // ---------------------------------------------------------------- Falhas do banco

    @Test
    fun `falha ao carregar avisa e deixa os campos vazios`() = runTest {
        val repository = FakeProfileRepository(
            profile = UserProfile(name = "Maria", email = "maria@exemplo.com"),
        ).apply { failLoad = true }
        val state = createViewModel(repository).uiState.value

        assertTrue(state.loadError)
        assertEquals("", state.name)
        assertEquals("", state.email)
    }

    @Test
    fun `falha ao salvar o perfil avisa e nao confirma`() = runTest {
        val repository = FakeProfileRepository().apply { failSave = true }
        val viewModel = createViewModel(repository)
        viewModel.onNameChange("Maria")
        viewModel.onEmailChange("maria@exemplo.com")

        viewModel.onSaveAccountClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.accountSaveError)
        assertFalse(viewModel.uiState.value.accountSaved)
        assertNull(repository.profile)
    }

    @Test
    fun `depois da falha editar limpa o aviso e salvar de novo confirma`() = runTest {
        val repository = FakeProfileRepository().apply { failSave = true }
        val viewModel = createViewModel(repository)
        viewModel.onNameChange("Maria")
        viewModel.onEmailChange("maria@exemplo.com")
        viewModel.onSaveAccountClick()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.accountSaveError)

        viewModel.onNameChange("Maria S.")
        assertFalse(viewModel.uiState.value.accountSaveError)

        repository.failSave = false
        viewModel.onSaveAccountClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.accountSaved)
        assertFalse(viewModel.uiState.value.accountSaveError)
        assertEquals("Maria S.", repository.profile?.name)
    }

    @Test
    fun `falha ao salvar o contato avisa e nao confirma`() = runTest {
        val repository = FakeProfileRepository().apply { failSave = true }
        val viewModel = createViewModel(repository)
        viewModel.onRelativeNameChange("Joao")
        viewModel.onRelativeEmailChange("joao@exemplo.com")
        viewModel.onConsentChange(true)

        viewModel.onSaveContactClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contactSaveError)
        assertFalse(viewModel.uiState.value.contactSaved)
        assertNull(repository.contact)

        repository.failSave = false
        viewModel.onSaveContactClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.contactSaved)
        assertFalse(viewModel.uiState.value.contactSaveError)
        assertEquals("joao@exemplo.com", repository.contact?.contact)
    }

    // ---------------------------------------------------------------- Aviso em caso de anormalidade

    @Test
    fun `o aviso comeca desligado`() = runTest {
        val viewModel = createViewModel(FakeProfileRepository())

        assertFalse(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun `perfil salvo guarda o aviso ligado`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onNameChange("Joao")
        viewModel.onEmailChange("joao@exemplo.com")
        viewModel.onNotificationsChange(true)
        viewModel.onSaveAccountClick()
        advanceUntilIdle()

        assertEquals(UserProfile("Joao", "joao@exemplo.com", notificationsEnabled = true), repository.profile)
        assertTrue(viewModel.uiState.value.accountSaved)
    }

    @Test
    fun `perfil salvo com o aviso ligado preenche o switch ao abrir`() = runTest {
        val repository = FakeProfileRepository(
            profile = UserProfile("Joao", "joao@exemplo.com", notificationsEnabled = true),
        )
        val viewModel = createViewModel(repository)

        assertTrue(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun `mexer no switch limpa a confirmacao do perfil`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onNameChange("Joao")
        viewModel.onEmailChange("joao@exemplo.com")
        viewModel.onSaveAccountClick()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.accountSaved)

        viewModel.onNotificationsChange(true)

        assertFalse(viewModel.uiState.value.accountSaved)
        assertTrue(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun `o switch do aviso nao altera nem depende do contato do familiar`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        viewModel.onNotificationsChange(true)

        assertFalse(viewModel.uiState.value.consent)
        assertEquals(0, repository.saveContactCalls)
    }
}
