package com.example.myheart.domain.usecase

import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.FamilyAlert
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.repository.ProfileRepository

/**
 * Decide se, depois de uma leitura, o app deve oferecer ao usuário avisar o familiar.
 *
 * Só oferece quando as três condições valem: a leitura não é Normal, existe um contato de
 * familiar salvo e o usuário deu a autorização expressa (consentimento) no Perfil. O envio em si
 * é sempre manual: o app só prepara o texto e o usuário confirma no app de e-mail ou de mensagens.
 *
 * Não depende do switch "Avisar em caso de anormalidade", que comanda só a notificação local.
 * Falhas do banco são lançadas: quem chama decide como registrá-las (sem oferta, o app segue normal).
 */
class PrepareFamilyAlertUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(reading: AnalyzedReading): FamilyAlert? {
        if (reading.status == ReadingStatus.Normal) return null
        val contact = profileRepository.getRelativeContact() ?: return null
        if (!contact.consent) return null
        val userName = profileRepository.getProfile()?.name?.trim()?.takeIf { it.isNotEmpty() }
        return FamilyAlert(
            userName = userName,
            relativeName = contact.name.trim(),
            channel = contact.channel,
            contact = contact.contact.trim(),
            reading = reading,
        )
    }
}
