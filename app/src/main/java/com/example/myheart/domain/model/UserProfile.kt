package com.example.myheart.domain.model

/**
 * Dados do próprio usuário do app. Não há login nem senha: o perfil só identifica quem usa o app.
 *
 * [notificationsEnabled] é o switch "Avisar em caso de anormalidade": liga a notificação neste
 * celular quando uma leitura não é Normal. Começa desligado.
 */
data class UserProfile(
    val name: String,
    val email: String,
    val notificationsEnabled: Boolean = false,
)
