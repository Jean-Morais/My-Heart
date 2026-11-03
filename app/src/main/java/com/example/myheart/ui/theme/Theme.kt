package com.example.myheart.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema do Material alinhado à paleta do app ([AppColors]). O design é claro e fixo (limitação
 * documentada: não há tema escuro nem cores dinâmicas do sistema), então não há escolha de tema aqui.
 */
private val AppColorScheme = lightColorScheme(
    primary = AppColors.Burgundy,
    onPrimary = AppColors.Surface,
    secondary = AppColors.Burgundy,
    onSecondary = AppColors.Surface,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.StepBadge,
    onSurfaceVariant = AppColors.TextMuted,
    outline = AppColors.Border,
    error = AppColors.StatusCriticalText,
    onError = AppColors.Surface,
)

@Composable
fun MyHeartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
