package com.example.myheart.ui.reading

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myheart.R
import com.example.myheart.data.sensor.SensorFailure
import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.Device
import com.example.myheart.domain.model.FamilyAlert
import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.ui.MyHeartUiState
import com.example.myheart.ui.bluetooth.readingErrorBodyRes
import com.example.myheart.ui.format.formatReadingMoment
import com.example.myheart.ui.components.AppCard
import com.example.myheart.ui.components.AppHeader
import com.example.myheart.ui.components.BackButton
import com.example.myheart.ui.components.FormButton
import com.example.myheart.ui.components.NoticeCard
import com.example.myheart.ui.components.PrimaryActionButton
import com.example.myheart.ui.components.ReadingValueCard
import com.example.myheart.ui.components.StatusDot
import com.example.myheart.ui.components.color
import com.example.myheart.ui.components.labelRes
import com.example.myheart.ui.components.messageRes
import com.example.myheart.ui.components.palette
import com.example.myheart.ui.theme.AppColors
import com.example.myheart.ui.theme.MyHeartTheme

/** Tela única com dois estados: leitura em andamento e resultado. */
@Composable
fun ReadingScreen(
    uiState: MyHeartUiState,
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit,
    onNewReadingClick: () -> Unit,
    onRetrySaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        AppHeader(title = stringResource(R.string.app_name))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            val reading = uiState.lastReading
            val status = uiState.status
            when {
                uiState.isReading -> InProgressContent(
                    progress = uiState.readingProgress,
                    simulated = uiState.isSimulatedData,
                    onBackClick = onBackClick,
                    onCancelClick = onCancelClick,
                )
                uiState.readingError -> ErrorContent(
                    failure = uiState.readingFailure,
                    simulated = uiState.isSimulatedData,
                    canReadAgain = uiState.canRead,
                    onBackClick = onBackClick,
                    onBackToHomeClick = onCancelClick,
                    onRetryClick = onNewReadingClick,
                )
                reading != null && status != null -> ResultContent(
                    reading = reading,
                    status = status,
                    simulated = uiState.isSimulatedData,
                    canReadAgain = uiState.canRead,
                    saveError = uiState.saveError,
                    familyAlert = uiState.familyAlert,
                    onBackClick = onBackClick,
                    onNewReadingClick = onNewReadingClick,
                    onRetrySaveClick = onRetrySaveClick,
                )
                else -> TitleRow(subtitle = "", onBackClick = onBackClick)
            }
        }
    }
}

// ---------------------------------------------------------------- Em andamento

@Composable
private fun InProgressContent(
    progress: Int,
    simulated: Boolean,
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "readingProgress",
    )

    TitleRow(subtitle = stringResource(R.string.reading_status_in_progress), onBackClick = onBackClick)
    Spacer(Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppColors.TextMuted,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.reading_device_connected),
            fontSize = 14.sp,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.width(8.dp))
        StatusDot(AppColors.LevelLow)
    }

    Spacer(Modifier.height(16.dp))

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressRing(progress = animatedProgress)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.reading_now_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Burgundy,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reading_now_description),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reading_capturing),
                    fontSize = 13.sp,
                    color = AppColors.TextMuted,
                )
                Text(
                    text = stringResource(R.string.reading_percent, progress),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Burgundy,
                )
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(progress = animatedProgress)
        }
    }

    Spacer(Modifier.height(16.dp))
    PendingValueRow(Icons.Outlined.MonitorHeart, stringResource(R.string.heart_rate_label), stringResource(R.string.unit_bpm))
    Spacer(Modifier.height(12.dp))
    PendingValueRow(Icons.Outlined.WaterDrop, stringResource(R.string.oxygen_label), stringResource(R.string.unit_spo2))

    Spacer(Modifier.height(16.dp))
    FormButton(
        text = stringResource(R.string.cancel_reading),
        onClick = onCancelClick,
        outlined = true,
        borderColor = AppColors.Border,
    )
    Spacer(Modifier.height(16.dp))
    Disclaimer(simulated)
}

/** Anel de progresso: começa no topo e avança no sentido horário. */
@Composable
private fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(104.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val inset = strokeWidth / 2
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = AppColors.StepBadge,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = AppColors.Burgundy,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Icon(
            imageVector = Icons.Outlined.MonitorHeart,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = AppColors.Burgundy,
        )
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(AppColors.StepBadge),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(AppColors.Burgundy),
        )
    }
}

/** Linha compacta com "— unidade", mostrada enquanto o valor ainda não existe. */
@Composable
private fun PendingValueRow(icon: ImageVector, label: String, unit: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = AppColors.TextMuted)
                Spacer(Modifier.width(8.dp))
                Text(text = label, fontSize = 14.sp, color = AppColors.TextMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.placeholder_dash),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.width(8.dp))
                Text(text = unit, fontSize = 13.sp, color = AppColors.TextMuted)
            }
        }
    }
}

// ---------------------------------------------------------------- Erro

/**
 * Leitura interrompida. Mantém a estrutura da tela em andamento (título, valores pendentes, aviso
 * legal) e troca o anel de progresso pelo aviso de erro, com "Tentar de novo" e "Voltar ao início".
 */
@Composable
private fun ErrorContent(
    failure: SensorFailure?,
    simulated: Boolean,
    canReadAgain: Boolean,
    onBackClick: () -> Unit,
    onBackToHomeClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    TitleRow(subtitle = stringResource(R.string.reading_status_error), onBackClick = onBackClick)
    Spacer(Modifier.height(16.dp))

    NoticeCard(
        title = stringResource(R.string.reading_error_title),
        body = stringResource(readingErrorBodyRes(failure)),
    )

    Spacer(Modifier.height(16.dp))
    PendingValueRow(Icons.Outlined.MonitorHeart, stringResource(R.string.heart_rate_label), stringResource(R.string.unit_bpm))
    Spacer(Modifier.height(12.dp))
    PendingValueRow(Icons.Outlined.WaterDrop, stringResource(R.string.oxygen_label), stringResource(R.string.unit_spo2))

    Spacer(Modifier.height(24.dp))
    PrimaryActionButton(
        text = stringResource(R.string.reading_error_retry),
        icon = Icons.Outlined.Replay,
        enabled = canReadAgain,
        onClick = onRetryClick,
    )
    Spacer(Modifier.height(12.dp))
    FormButton(
        text = stringResource(R.string.reading_error_back),
        onClick = onBackToHomeClick,
        outlined = true,
        borderColor = AppColors.Border,
    )
    Spacer(Modifier.height(16.dp))
    Disclaimer(simulated)
}

// ---------------------------------------------------------------- Resultado

@Composable
private fun ResultContent(
    reading: Reading,
    status: ReadingStatus,
    simulated: Boolean,
    canReadAgain: Boolean,
    saveError: Boolean,
    familyAlert: FamilyAlert?,
    onBackClick: () -> Unit,
    onNewReadingClick: () -> Unit,
    onRetrySaveClick: () -> Unit,
) {
    TitleRow(
        subtitle = stringResource(R.string.reading_status_done, formatReadingMoment(reading.timestamp)),
        onBackClick = onBackClick,
    )
    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AppColors.StatusNormalText,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.reading_done),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.StatusNormalText,
            )
        }
        if (simulated) {
            Text(
                text = stringResource(R.string.sample_data_tag),
                fontSize = 13.sp,
                color = AppColors.TextMuted,
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    ReadingValueCard(
        icon = Icons.Outlined.MonitorHeart,
        label = stringResource(R.string.heart_rate_label),
        value = reading.bpm.toString(),
        unit = stringResource(R.string.unit_bpm),
    )
    Spacer(Modifier.height(12.dp))
    ReadingValueCard(
        icon = Icons.Outlined.WaterDrop,
        label = stringResource(R.string.oxygen_label),
        value = reading.spo2.toString(),
        unit = stringResource(R.string.unit_spo2),
    )

    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.analysis_result_title),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.Burgundy,
    )
    Spacer(Modifier.height(12.dp))
    AnalysisCard(status)

    Spacer(Modifier.height(12.dp))
    Legend()

    if (familyAlert != null) {
        Spacer(Modifier.height(16.dp))
        FamilyAlertCard(alert = familyAlert, simulated = simulated)
    }

    Spacer(Modifier.height(16.dp))
    Disclaimer(simulated)

    if (saveError) {
        Spacer(Modifier.height(16.dp))
        NoticeCard(
            title = stringResource(R.string.save_error_title),
            body = stringResource(R.string.save_error_body),
            actionText = stringResource(R.string.save_error_retry),
            onActionClick = onRetrySaveClick,
        )
    }

    Spacer(Modifier.height(16.dp))
    PrimaryActionButton(
        text = stringResource(R.string.new_reading),
        icon = Icons.Outlined.Replay,
        enabled = canReadAgain,
        onClick = onNewReadingClick,
    )
}

@Composable
private fun AnalysisCard(status: ReadingStatus) {
    val palette = status.palette()
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.background)
            .border(1.dp, palette.border, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(status.color())
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(status.labelRes()),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.content,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(status.messageRes()),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = palette.content,
        )
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ReadingStatus.entries.forEach { level ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(level.color())
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(level.labelRes()), fontSize = 12.sp, color = AppColors.TextMuted)
            }
        }
    }
}

// ---------------------------------------------------------------- Comuns

@Composable
private fun TitleRow(subtitle: String, onBackClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BackButton(onClick = onBackClick)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.reading_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Burgundy,
            )
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 13.sp, color = AppColors.TextMuted)
            }
        }
    }
}

@Composable
private fun Disclaimer(simulated: Boolean) {
    Text(
        text = stringResource(if (simulated) R.string.reading_disclaimer_sample else R.string.reading_disclaimer),
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = AppColors.TextMuted,
    )
}

// ---------------------------------------------------------------- Previews

private val previewDevice = Device("Sensor Simulado", "00:00")

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ReadingInProgressPreview() {
    MyHeartTheme {
        ReadingScreen(
            uiState = MyHeartUiState(device = previewDevice, isReading = true, readingProgress = 65, isSimulatedData = true),
            onBackClick = {}, onCancelClick = {}, onNewReadingClick = {}, onRetrySaveClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ReadingErrorPreview() {
    MyHeartTheme {
        ReadingScreen(
            uiState = MyHeartUiState(device = previewDevice, readingError = true, isSimulatedData = true),
            onBackClick = {}, onCancelClick = {}, onNewReadingClick = {}, onRetrySaveClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ReadingResultPreview() {
    MyHeartTheme {
        ReadingScreen(
            uiState = MyHeartUiState(
                device = previewDevice,
                lastReading = Reading(bpm = 72, spo2 = 98, timestamp = System.currentTimeMillis()),
                status = ReadingStatus.Normal,
                isSimulatedData = true,
            ),
            onBackClick = {}, onCancelClick = {}, onNewReadingClick = {}, onRetrySaveClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun ReadingSaveErrorPreview() {
    MyHeartTheme {
        ReadingScreen(
            uiState = MyHeartUiState(
                device = previewDevice,
                lastReading = Reading(bpm = 72, spo2 = 98, timestamp = System.currentTimeMillis()),
                status = ReadingStatus.Normal,
                saveError = true,
            ),
            onBackClick = {}, onCancelClick = {}, onNewReadingClick = {}, onRetrySaveClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ReadingCriticalPreview() {
    MyHeartTheme {
        ReadingScreen(
            uiState = MyHeartUiState(
                device = previewDevice,
                lastReading = Reading(bpm = 128, spo2 = 88, timestamp = System.currentTimeMillis()),
                status = ReadingStatus.Critical,
            ),
            onBackClick = {}, onCancelClick = {}, onNewReadingClick = {}, onRetrySaveClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun ReadingFamilyAlertPreview() {
    MyHeartTheme {
        val reading = Reading(bpm = 72, spo2 = 85, timestamp = System.currentTimeMillis())
        ReadingScreen(
            uiState = MyHeartUiState(
                device = previewDevice,
                lastReading = reading,
                status = ReadingStatus.Critical,
                familyAlert = FamilyAlert(
                    userName = "Maria",
                    relativeName = "Ana",
                    channel = NotifyChannel.Email,
                    contact = "ana@exemplo.com",
                    reading = AnalyzedReading(
                        reading = reading,
                        status = ReadingStatus.Critical,
                        bpmStatus = ReadingStatus.Normal,
                        spo2Status = ReadingStatus.Critical,
                    ),
                ),
            ),
            onBackClick = {}, onCancelClick = {}, onNewReadingClick = {}, onRetrySaveClick = {},
        )
    }
}
