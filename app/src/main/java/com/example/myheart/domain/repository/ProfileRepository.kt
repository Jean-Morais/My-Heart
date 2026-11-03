package com.example.myheart.domain.repository

import com.example.myheart.domain.model.RelativeContact
import com.example.myheart.domain.model.UserProfile

/**
 * Contrato de acesso aos dados de perfil. Vive no domínio: a tela e o ViewModel só conhecem esta
 * interface, nunca o Room. Assim o banco pode mudar (ou ser trocado por um falso nos testes)
 * sem tocar na interface do app.
 *
 * Os "get" devolvem null quando nada foi salvo ainda.
 */
interface ProfileRepository {
    suspend fun getProfile(): UserProfile?
    suspend fun saveProfile(profile: UserProfile)

    suspend fun getRelativeContact(): RelativeContact?
    suspend fun saveRelativeContact(contact: RelativeContact)
}
