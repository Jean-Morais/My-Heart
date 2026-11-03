package com.example.myheart.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myheart.R
import com.example.myheart.data.sensor.SensorFailure
import com.example.myheart.domain.model.Device
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.ui.MyHeartUiState
import com.example.myheart.ui.bluetooth.connectionErrorBodyRes
import com.example.myheart.ui.format.RelativeDay
import com.example.myheart.ui.format.formatReadingMoment
import com.example.myheart.ui.format.formatShortDate
import com.example.myheart.ui.format.relativeDay
import com.example.myheart.ui.format.startOfDayMillis
import com.example.myheart.ui.components.AppCard
import com.example.myheart.ui.components.AppHeader
import com.example.myheart.ui.components.PrimaryActionButton
import com.example.myheart.ui.components.ReadingValueCard
import com.example.myheart.ui.components.NoticeCard
import com.example.myheart.ui.components.SelectableChip
import com.example.myheart.ui.theme.AppColors
import com.example.myheart.ui.theme.MyHeartTheme
import java.time.LocalDate

@Composable
fun HomeScreen(
    uiState: MyHeartUiState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onReadClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onRetryLoadClick: () -> Unit,
    onOpenAppSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMetric by rememberSaveable { mutableStateOf(HomeMetric.Bpm) }
    val series = remember(selectedMetric, uiState.chartReadings) {
        buildChartSeries(selectedMetric, uiState.chartReadings)
    }
    val showSampleNote = uiState.chartReadings.any { it.reading.isSimulated }
    val reading = uiState.lastReading
    val dash = stringResource(R.string.placeholder_dash)

    Column(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        AppHeader(title = stringResource(R.string.app_name))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            ConnectionBanner(
                device = uiState.device,
                isConnecting = uiState.isConnecting,
                hasError = uiState.connectionError,
                failure = uiState.connectionFailure,
                onConnectClick = onConnectClick,
                onDisconnectClick = onDisconnectClick,
            )

            if (uiState.connectionError && uiState.connectionFailure == SensorFailure.PermissionDenied) {
                Spacer(Modifier.height(16.dp))
                NoticeCard(
                    title = stringResource(R.string.ble_permission_title),
                    body = stringResource(R.string.ble_permission_body),
                    actionText = stringResource(R.string.ble_permission_action),
                    onActionClick = onOpenAppSettingsClick,
                )
            }

            if (uiState.loadError) {
                Spacer(Modifier.height(16.dp))
                NoticeCard(
                    title = stringResource(R.string.load_error_title),
                    body = stringResource(R.string.load_error_body),
                    actionText = stringResource(R.string.load_error_retry),
                    onActionClick = onRetryLoadClick,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Última leitura
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.last_reading),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Burgundy,
                )
                Text(
                    text = reading?.let { formatReadingMoment(it.timestamp) }
                        ?: stringResource(R.string.no_reading),
                    fontSize = 13.sp,
                    color = AppColors.TextMuted,
                )
            }
            Spacer(Modifier.height(12.dp))

            ReadingValueCard(
                icon = Icons.Outlined.MonitorHeart,
                label = stringResource(R.string.heart_rate_label),
                value = (reading?.bpm ?: 0).toString(),
                unit = stringResource(R.string.unit_bpm),
            )
            Spacer(Modifier.height(12.dp))
            ReadingValueCard(
                icon = Icons.Outlined.WaterDrop,
                label = stringResource(R.string.oxygen_label),
                value = (reading?.spo2 ?: 0).toString(),
                unit = stringResource(R.string.unit_spo2),
            )

            Spacer(Modifier.height(24.dp))

            PrimaryActionButton(
                text = stringResource(R.string.take_reading_now),
                icon = Icons.AutoMirrored.Outlined.ShowChart,
                enabled = uiState.canRead,
                onClick = onReadClick,
            )
            Spacer(Modifier.height(12.dp))
            CaptionText(
                stringResource(
                    if (uiState.device == null) R.string.connect_to_enable_reading
                    else R.string.reading_ready,
                ),
            )

            Spacer(Modifier.height(28.dp))

            // Análise de medições
            Text(
                text = stringResource(R.string.analysis_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Burgundy,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (showSampleNote) R.string.analysis_description_sample else R.string.analysis_description,
                ),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AppColors.TextMuted,
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectableChip(
                    text = stringResource(R.string.tab_bpm),
                    selected = selectedMetric == HomeMetric.Bpm,
                    onClick = { selectedMetric = HomeMetric.Bpm },
                )
                SelectableChip(
                    text = stringResource(R.string.tab_oxygen),
                    selected = selectedMetric == HomeMetric.Spo2,
                    onClick = { selectedMetric = HomeMetric.Spo2 },
                )
            }

            Spacer(Modifier.height(16.dp))

            MeasurementsChartCard(
                title = chartTitle(uiState.chartDay),
                series = series,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    stringResource(R.string.stat_average),
                    series.average?.let { metricValueText(selectedMetric, it) } ?: dash,
                    Modifier.weight(1f),
                )
                StatCard(
                    stringResource(R.string.stat_max),
                    series.max?.let { metricValueText(selectedMetric, it) } ?: dash,
                    Modifier.weight(1f),
                )
                StatCard(
                    stringResource(R.string.stat_min),
                    series.min?.let { metricValueText(selectedMetric, it) } ?: dash,
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))

            PrimaryActionButton(
                text = stringResource(R.string.view_history),
                icon = Icons.AutoMirrored.Outlined.ShowChart,
                enabled = true,
                onClick = onHistoryClick,
            )
        }
    }
}

/**
 * Aviso de conexão, com quatro estados:
 * desconectado (rosado, clicável), conectando (neutro, com indicador de progresso),
 * erro de conexão (rosado, clicável, pede nova tentativa) e conectado (verde, com o botão "Desconectar").
 */
@Composable
private fun ConnectionBanner(
    device: Device?,
    isConnecting: Boolean,
    hasError: Boolean,
    failure: SensorFailure?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connected = device != null
    val clickable = !connected && !isConnecting
    val background = when {
        connected -> AppColors.SuccessBackground
        isConnecting -> AppColors.Surface
        else -> AppColors.AlertBackground
    }
    val borderColor = when {
        connected -> AppColors.SuccessBorder
        isConnecting -> AppColors.Border
        else -> AppColors.AlertBorder
    }
    val titleRes = when {
        connected -> R.string.banner_connected_title
        isConnecting -> R.string.banner_connecting_title
        hasError -> R.string.banner_error_title
        else -> R.string.banner_disconnected_title
    }
    val bodyRes = when {
        isConnecting -> R.string.banner_connecting_body
        hasError -> connectionErrorBodyRes(failure)
        else -> R.string.banner_disconnected_body
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .then(if (clickable) Modifier.clickable(role = Role.Button, onClick = onConnectClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        when {
            isConnecting -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = AppColors.Burgundy,
                strokeWidth = 2.dp,
            )
            else -> Icon(
                imageVector = if (connected) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (connected) AppColors.SuccessIcon else AppColors.AlertIcon,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = device?.name ?: stringResource(bodyRes),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = AppColors.TextMuted,
            )
        }
        if (connected) {
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onDisconnectClick,
                colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Burgundy),
            ) {
                Text(
                    text = stringResource(R.string.disconnect),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Título do gráfico: "Medições de hoje", "de ontem" ou da data, conforme o dia mostrado. */
@Composable
private fun chartTitle(day: LocalDate?): String {
    if (day == null) return stringResource(R.string.chart_title_today)
    return when (relativeDay(startOfDayMillis(day))) {
        RelativeDay.Today -> stringResource(R.string.chart_title_today)
        RelativeDay.Yesterday -> stringResource(R.string.chart_title_yesterday)
        RelativeDay.Other -> stringResource(R.string.chart_title_date, formatShortDate(day))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Burgundy,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Burgundy,
            )
        }
    }
}

@Composable
private fun CaptionText(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 12.sp,
        color = AppColors.TextMuted,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun HomeScreenDisconnectedPreview() {
    MyHeartTheme {
        HomeScreen(
            uiState = MyHeartUiState(),
            onConnectClick = {},
            onDisconnectClick = {},
            onReadClick = {},
            onHistoryClick = {},
            onRetryLoadClick = {},
            onOpenAppSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun HomeScreenConnectingPreview() {
    MyHeartTheme {
        HomeScreen(
            uiState = MyHeartUiState(isConnecting = true),
            onConnectClick = {},
            onDisconnectClick = {},
            onReadClick = {},
            onHistoryClick = {},
            onRetryLoadClick = {},
            onOpenAppSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 500)
@Composable
private fun HomeScreenConnectionErrorPreview() {
    MyHeartTheme {
        HomeScreen(
            uiState = MyHeartUiState(connectionError = true),
            onConnectClick = {},
            onDisconnectClick = {},
            onReadClick = {},
            onHistoryClick = {},
            onRetryLoadClick = {},
            onOpenAppSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun HomeScreenLoadErrorPreview() {
    MyHeartTheme {
        HomeScreen(
            uiState = MyHeartUiState(loadError = true),
            onConnectClick = {},
            onDisconnectClick = {},
            onReadClick = {},
            onHistoryClick = {},
            onRetryLoadClick = {},
            onOpenAppSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun HomeScreenConnectedPreview() {
    MyHeartTheme {
        HomeScreen(
            uiState = MyHeartUiState(
                device = Device("Sensor Simulado", "00:00"),
                lastReading = Reading(bpm = 72, spo2 = 98, timestamp = System.currentTimeMillis()),
                chartDay = LocalDate.now(),
                chartReadings = previewChartReadings(),
            ),
            onConnectClick = {},
            onDisconnectClick = {},
            onReadClick = {},
            onHistoryClick = {},
            onRetryLoadClick = {},
            onOpenAppSettingsClick = {},
        )
    }
}

/** Leituras de exemplo só para a pré-visualização do gráfico (nunca vão para o app). */
private fun previewChartReadings(): List<com.example.myheart.domain.model.AnalyzedReading> {
    val analyze = AnalyzeReadingUseCase()
    val now = System.currentTimeMillis()
    val hour = 60 * 60 * 1000L
    return listOf(
        Reading(bpm = 72, spo2 = 98, timestamp = now - 6 * hour, isSimulated = true),
        Reading(bpm = 96, spo2 = 94, timestamp = now - 4 * hour, isSimulated = true),
        Reading(bpm = 112, spo2 = 93, timestamp = now - 2 * hour, isSimulated = true),
        Reading(bpm = 128, spo2 = 88, timestamp = now - hour, isSimulated = true),
        Reading(bpm = 78, spo2 = 97, timestamp = now, isSimulated = true),
    ).map(analyze::analyze)
}
