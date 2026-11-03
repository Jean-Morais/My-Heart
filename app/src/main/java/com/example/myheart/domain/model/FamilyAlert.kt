package com.example.myheart.domain.model

/**
 * Aviso que o app oferece ao usuário enviar a um familiar depois de uma leitura fora do normal.
 *
 * O app não envia nada sozinho: este modelo só reúne o que a tela precisa para abrir o app de
 * e-mail ou de mensagens com o texto pronto. [userName] é null quando o perfil não tem nome.
 * [contact] é o e-mail ou o telefone, conforme [channel].
 */
data class FamilyAlert(
    val userName: String?,
    val relativeName: String,
    val channel: NotifyChannel,
    val contact: String,
    val reading: AnalyzedReading,
)
