package com.example.myheart.ui.theme

import androidx.compose.ui.graphics.Color

/** Paleta do design do My Heart (tema claro). */
object AppColors {
    val Burgundy = Color(0xFF5E1A2D)
    val Background = Color(0xFFF7F1EF)
    val Surface = Color.White
    val Border = Color(0xFFE9DEDC)

    val TextPrimary = Color(0xFF1E1A1B)
    val TextMuted = Color(0xFF6B5E60)

    val DisabledContainer = Color(0xFFEFE6E4)
    val DisabledContent = Color(0xFF8F8284)

    val StepBadge = Color(0xFFEFE6E4)
    val Placeholder = Color(0xFF9A8E90)

    val AlertBackground = Color(0xFFFFF1F0)
    val AlertBorder = Color(0xFFF6D5D2)
    val AlertIcon = Color(0xFFC0392B)

    val SuccessBackground = Color(0xFFEFF8F1)
    val SuccessBorder = Color(0xFFCDE8D3)
    val SuccessIcon = Color(0xFF2E7D32)

    // Cartão de análise do resultado (fundo e borda do Normal e do Crítico reaproveitam Success* e Alert*)
    val StatusNormalText = Color(0xFF1B6B34)
    val StatusAttentionBackground = Color(0xFFFFF8E5)
    val StatusAttentionBorder = Color(0xFFF3E1A6)
    val StatusAttentionText = Color(0xFF7A5200)
    val StatusCriticalText = Color(0xFFB3261E)

    // Níveis de criticidade do gráfico
    val LevelLow = Color(0xFF22C55E)
    val LevelAttention = Color(0xFFFBBF24)
    val LevelHigh = Color(0xFFEF4444)
}
