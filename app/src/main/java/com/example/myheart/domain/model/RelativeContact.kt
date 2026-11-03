package com.example.myheart.domain.model

/**
 * Familiar que receberá os avisos.
 *
 * [contact] guarda apenas o dado do canal escolhido (o e-mail se [channel] for Email,
 * o telefone se for Phone). O dado do canal não usado não é guardado.
 * [consent] registra que o usuário autorizou expressamente o envio de avisos.
 */
data class RelativeContact(
    val name: String,
    val channel: NotifyChannel,
    val contact: String,
    val consent: Boolean,
)
