package com.example.myheart.ui.navigation

import kotlinx.serialization.Serializable

// Rotas tipadas (Navigation Compose 2.8+). Sufixo Route evita conflito com os modelos.
@Serializable data object HomeRoute
@Serializable data object ReadingRoute
@Serializable data object HistoryRoute
@Serializable data object InstructionsRoute
@Serializable data object ProfileRoute
