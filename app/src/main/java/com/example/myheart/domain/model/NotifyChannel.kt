package com.example.myheart.domain.model

/** Canal pelo qual o familiar será avisado. Fica no domínio porque o banco e a tela usam o mesmo conceito. */
enum class NotifyChannel { Email, Phone }
