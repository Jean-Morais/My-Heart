package com.example.myheart.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.myheart.R
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.ui.theme.AppColors

/** Cor do indicador (bolinha) do nível. */
fun ReadingStatus.color(): Color = when (this) {
    ReadingStatus.Normal -> AppColors.LevelLow
    ReadingStatus.Attention -> AppColors.LevelAttention
    ReadingStatus.Critical -> AppColors.LevelHigh
}

@StringRes
fun ReadingStatus.labelRes(): Int = when (this) {
    ReadingStatus.Normal -> R.string.status_normal
    ReadingStatus.Attention -> R.string.status_attention
    ReadingStatus.Critical -> R.string.status_critical
}

@StringRes
fun ReadingStatus.messageRes(): Int = when (this) {
    ReadingStatus.Normal -> R.string.result_normal_message
    ReadingStatus.Attention -> R.string.result_attention_message
    ReadingStatus.Critical -> R.string.result_critical_message
}

/** Cores do cartão de análise. */
data class StatusPalette(val background: Color, val border: Color, val content: Color)

fun ReadingStatus.palette(): StatusPalette = when (this) {
    ReadingStatus.Normal -> StatusPalette(
        AppColors.SuccessBackground, AppColors.SuccessBorder, AppColors.StatusNormalText,
    )
    ReadingStatus.Attention -> StatusPalette(
        AppColors.StatusAttentionBackground, AppColors.StatusAttentionBorder, AppColors.StatusAttentionText,
    )
    ReadingStatus.Critical -> StatusPalette(
        AppColors.AlertBackground, AppColors.AlertBorder, AppColors.StatusCriticalText,
    )
}
