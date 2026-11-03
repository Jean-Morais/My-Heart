package com.example.myheart.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.domain.model.RelativeContact
import com.example.myheart.domain.model.UserProfile
import com.example.myheart.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FieldError { Required, InvalidEmail, InvalidPhone }

data class ProfileUiState(
    // Dados do usuário
    val name: String = "",
    val email: String = "",
    val nameError: FieldError? = null,
    val emailError: FieldError? = null,
    val accountSaved: Boolean = false,
    /** Switch "Avisar em caso de anormalidade"; só vale depois de salvar os dados. */
    val notificationsEnabled: Boolean = false,
    // Contato do familiar
    val relativeName: String = "",
    val channel: NotifyChannel = NotifyChannel.Email,
    val relativeEmail: String = "",
    val relativePhone: String = "",
    val consent: Boolean = false,
    val relativeNameError: FieldError? = null,
    val relativeContactError: FieldError? = null,
    val consentError: Boolean = false,
    val contactSaved: Boolean = false,
    // Falhas do banco
    /** Os dados salvos não puderam ser carregados; os campos podem estar vazios. */
    val loadError: Boolean = false,
    val accountSaveError: Boolean = false,
    val contactSaveError: Boolean = false,
)

private const val TAG = "ProfileViewModel"

/**
 * Estado e validação dos formulários do Perfil, agora com persistência.
 *
 * Fluxo: ao criar o ViewModel, os dados salvos são carregados uma vez para preencher os campos.
 * Ao tocar em "salvar", valida primeiro; só grava no banco (via repositório) se estiver tudo certo.
 * A carga é pontual (e não um fluxo contínuo) de propósito: assim o banco nunca sobrescreve o que
 * o usuário está digitando.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSavedData()
    }

    private fun loadSavedData() {
        viewModelScope.launch {
            val saved = try {
                repository.getProfile() to repository.getRelativeContact()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao carregar o perfil", e)
                _uiState.update { it.copy(loadError = true) }
                return@launch
            }
            val (profile, contact) = saved
            _uiState.update { state ->
                var next = state
                if (profile != null) {
                    next = next.copy(
                        name = profile.name,
                        email = profile.email,
                        notificationsEnabled = profile.notificationsEnabled,
                    )
                }
                if (contact != null) {
                    next = next.copy(
                        relativeName = contact.name,
                        channel = contact.channel,
                        // O dado salvo vai para o campo do canal a que ele pertence.
                        relativeEmail = if (contact.channel == NotifyChannel.Email) contact.contact else "",
                        relativePhone = if (contact.channel == NotifyChannel.Phone) contact.contact else "",
                        consent = contact.consent,
                    )
                }
                next
            }
        }
    }

    // ---- Dados do usuário ----
    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null, accountSaved = false, accountSaveError = false) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, accountSaved = false, accountSaveError = false) }

    fun onNotificationsChange(value: Boolean) =
        _uiState.update { it.copy(notificationsEnabled = value, accountSaved = false, accountSaveError = false) }

    fun onSaveAccountClick() {
        val s = _uiState.value
        val nameError = if (s.name.isBlank()) FieldError.Required else null
        val emailError = validateEmail(s.email)

        if (nameError != null || emailError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    accountSaved = false,
                    accountSaveError = false,
                )
            }
            return
        }

        val name = s.name.trim()
        val email = s.email.trim()
        viewModelScope.launch {
            try {
                repository.saveProfile(
                    UserProfile(name = name, email = email, notificationsEnabled = s.notificationsEnabled),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao gravar o perfil", e)
                _uiState.update { it.copy(accountSaved = false, accountSaveError = true) }
                return@launch
            }
            // Mostra na tela exatamente o que foi gravado (sem espaços nas pontas).
            _uiState.update {
                it.copy(
                    name = name,
                    email = email,
                    nameError = null,
                    emailError = null,
                    accountSaved = true,
                    accountSaveError = false,
                )
            }
        }
    }

    // ---- Contato do familiar ----
    fun onRelativeNameChange(value: String) =
        _uiState.update { it.copy(relativeName = value, relativeNameError = null, contactSaved = false, contactSaveError = false) }

    fun onChannelChange(channel: NotifyChannel) =
        _uiState.update { it.copy(channel = channel, relativeContactError = null, contactSaved = false, contactSaveError = false) }

    fun onRelativeEmailChange(value: String) =
        _uiState.update { it.copy(relativeEmail = value, relativeContactError = null, contactSaved = false, contactSaveError = false) }

    fun onRelativePhoneChange(value: String) =
        _uiState.update { it.copy(relativePhone = value, relativeContactError = null, contactSaved = false, contactSaveError = false) }

    fun onConsentChange(value: Boolean) =
        _uiState.update { it.copy(consent = value, consentError = false, contactSaved = false, contactSaveError = false) }

    fun onSaveContactClick() {
        val s = _uiState.value
        val nameError = if (s.relativeName.isBlank()) FieldError.Required else null
        val contactError = when (s.channel) {
            NotifyChannel.Email -> validateEmail(s.relativeEmail)
            NotifyChannel.Phone -> validatePhone(s.relativePhone)
        }
        val consentError = !s.consent

        if (nameError != null || contactError != null || consentError) {
            _uiState.update {
                it.copy(
                    relativeNameError = nameError,
                    relativeContactError = contactError,
                    consentError = consentError,
                    contactSaved = false,
                    contactSaveError = false,
                )
            }
            return
        }

        val name = s.relativeName.trim()
        // Só o dado do canal escolhido é guardado; o do outro canal não vai para o banco.
        val contact = when (s.channel) {
            NotifyChannel.Email -> s.relativeEmail.trim()
            NotifyChannel.Phone -> s.relativePhone.trim()
        }
        viewModelScope.launch {
            try {
                repository.saveRelativeContact(
                    RelativeContact(
                        name = name,
                        channel = s.channel,
                        contact = contact,
                        consent = s.consent,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao gravar o contato do familiar", e)
                _uiState.update { it.copy(contactSaved = false, contactSaveError = true) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    relativeName = name,
                    relativeEmail = if (s.channel == NotifyChannel.Email) contact else it.relativeEmail,
                    relativePhone = if (s.channel == NotifyChannel.Phone) contact else it.relativePhone,
                    relativeNameError = null,
                    relativeContactError = null,
                    consentError = false,
                    contactSaved = true,
                    contactSaveError = false,
                )
            }
        }
    }

    private fun validateEmail(value: String): FieldError? = when {
        value.isBlank() -> FieldError.Required
        !EMAIL_REGEX.matches(value.trim()) -> FieldError.InvalidEmail
        else -> null
    }

    private fun validatePhone(value: String): FieldError? {
        if (value.isBlank()) return FieldError.Required
        val digits = value.count { it.isDigit() }
        return if (digits in 10..11) null else FieldError.InvalidPhone // DDD + 8 ou 9 dígitos
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
