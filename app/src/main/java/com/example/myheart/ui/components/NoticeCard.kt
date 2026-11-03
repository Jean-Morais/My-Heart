package com.example.myheart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myheart.R
import com.example.myheart.ui.theme.AppColors
import com.example.myheart.ui.theme.MyHeartTheme

/**
 * Aviso de falha em cartão rosado (mesmas cores do aviso de dispositivo desconectado).
 * Se [actionText] e [onActionClick] forem informados, mostra um botão de nova tentativa.
 * É uma região "viva": o leitor de tela anuncia o aviso quando ele aparece.
 */
@Composable
fun NoticeCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.AlertBackground)
            .border(1.dp, AppColors.AlertBorder, shape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AppColors.AlertIcon,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(text = body, fontSize = 14.sp, lineHeight = 20.sp, color = AppColors.TextMuted)
            }
        }
        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(12.dp))
            FormButton(
                text = actionText,
                onClick = onActionClick,
                outlined = true,
                borderColor = AppColors.AlertBorder,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoticeCardPreview() {
    MyHeartTheme {
        NoticeCard(
            title = stringResource(R.string.save_error_title),
            body = stringResource(R.string.save_error_body),
            modifier = Modifier.padding(24.dp),
            actionText = stringResource(R.string.save_error_retry),
            onActionClick = {},
        )
    }
}
