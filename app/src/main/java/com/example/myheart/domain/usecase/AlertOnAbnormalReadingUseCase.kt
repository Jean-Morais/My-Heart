package com.example.myheart.domain.usecase

import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.notification.AlertNotifier
import com.example.myheart.domain.repository.ProfileRepository

/**
 * Decide se uma leitura concluída deve gerar aviso e, se deve, pede ao [AlertNotifier] que publique.
 *
 * Só avisa quando as duas condições valem: a leitura não é Normal e o usuário ligou "Avisar em
 * caso de anormalidade" em "Seus dados" (e salvou). A notificação é local, então não depende de
 * contato de familiar. A configuração é lida do banco a cada leitura, então desligar o aviso vale
 * já na próxima leitura.
 *
 * Falhas (banco ou notificação) são lançadas: quem chama decide como registrá-las. Devolve true
 * somente se a notificação foi de fato publicada.
 */
class AlertOnAbnormalReadingUseCase(
    private val profileRepository: ProfileRepository,
    private val notifier: AlertNotifier,
) {
    suspend operator fun invoke(reading: AnalyzedReading): Boolean {
        if (reading.status == ReadingStatus.Normal) return false
        val profile = profileRepository.getProfile() ?: return false
        if (!profile.notificationsEnabled) return false
        return notifier.notifyAbnormal(reading)
    }
}
