package com.example.myheart.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myheart.R
import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.ui.components.AppCard
import com.example.myheart.ui.components.AppHeader
import com.example.myheart.ui.components.BackButton
import com.example.myheart.ui.components.SelectableChip
import com.example.myheart.ui.components.StatusDot
import com.example.myheart.ui.components.color
import com.example.myheart.ui.components.labelRes
import com.example.myheart.ui.components.palette
import com.example.myheart.ui.format.formatClock
import com.example.myheart.ui.format.formatDateRange
import com.example.myheart.ui.format.formatLongDate
import com.example.myheart.ui.format.localDateOf
import com.example.myheart.ui.theme.AppColors
import com.example.myheart.ui.theme.MyHeartTheme
import java.time.LocalDate

/** Rota: obtém o estado do ViewModel e entrega o conteúdo, que não conhece o ViewModel. */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(
        uiState = uiState,
        onPeriodSelected = viewModel::onPeriodSelected,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    onPeriodSelected: (HistoryPeriod) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSimulated = uiState.readings.any { it.reading.isSimulated }
    val hasReal = uiState.readings.any { !it.reading.isSimulated }

    Column(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        AppHeader(title = stringResource(R.string.app_name))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            TitleRow(onBackClick = onBackClick)
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.history_period),
                fontSize = 14.sp,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(8.dp))
            PeriodChips(selected = uiState.period, onSelected = onPeriodSelected)

            Spacer(Modifier.height(24.dp))
            LegendCard(simulated = hasSimulated)
            Spacer(Modifier.height(24.dp))

            when {
                uiState.isLoading -> Unit
                uiState.hasError -> MessageCard(
                    title = stringResource(R.string.history_error_title),
                    body = stringResource(R.string.history_error_body),
                )
                uiState.readings.isEmpty() -> MessageCard(
                    title = stringResource(R.string.history_empty_title),
                    body = stringResource(R.string.history_empty_body),
                )
                else -> ReadingsList(
                    uiState = uiState,
                    hasSimulated = hasSimulated,
                    // A marca em cada cartão só importa quando a lista mistura dados reais e ilustrativos.
                    tagSimulated = hasSimulated && hasReal,
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Topo e filtros

@Composable
private fun TitleRow(onBackClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BackButton(onClick = onBackClick)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.history_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Burgundy,
            )
            Text(
                text = stringResource(R.string.history_back_subtitle),
                fontSize = 13.sp,
                color = AppColors.TextMuted,
            )
        }
    }
}

@Composable
private fun PeriodChips(selected: HistoryPeriod, onSelected: (HistoryPeriod) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectableChip(
            text = stringResource(R.string.period_7_days),
            selected = selected == HistoryPeriod.Week,
            onClick = { onSelected(HistoryPeriod.Week) },
            modifier = Modifier.weight(1f),
        )
        SelectableChip(
            text = stringResource(R.string.period_30_days),
            selected = selected == HistoryPeriod.Month,
            onClick = { onSelected(HistoryPeriod.Month) },
            modifier = Modifier.weight(1f),
        )
        SelectableChip(
            text = stringResource(R.string.period_all),
            selected = selected == HistoryPeriod.All,
            onClick = { onSelected(HistoryPeriod.All) },
            modifier = Modifier.weight(1.6f),
        )
    }
}

/** Aviso de dados ilustrativos (rosado) com a legenda de cores; sem dados simulados, só a legenda. */
@Composable
private fun LegendCard(simulated: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (simulated) AppColors.AlertBackground else AppColors.Surface)
            .border(1.dp, if (simulated) AppColors.AlertBorder else AppColors.Border, shape)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(if (simulated) R.string.history_sample_title else R.string.history_legend_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(if (simulated) R.string.history_sample_body else R.string.history_legend_body),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = AppColors.TextMuted,
        )
        Spacer(Modifier.height(12.dp))
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
}

@Composable
private fun MessageCard(title: String, body: String) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.Burgundy)
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = AppColors.TextMuted,
            )
        }
    }
}

// ---------------------------------------------------------------- Lista

@Composable
private fun ReadingsList(
    uiState: HistoryUiState,
    hasSimulated: Boolean,
    tagSimulated: Boolean,
) {
    // groupBy mantém a ordem de chegada: dias e leituras seguem da mais recente para a mais antiga.
    val groups = remember(uiState.readings) {
        uiState.readings.groupBy { localDateOf(it.reading.timestamp) }.toList()
    }
    val count = uiState.readings.size
    val start = uiState.rangeStart
    val end = uiState.rangeEnd

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pluralStringResource(R.plurals.history_count, count, count),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Burgundy,
        )
        Text(text = stringResource(R.string.history_sort), fontSize = 13.sp, color = AppColors.TextMuted)
    }
    if (start != null && end != null) {
        Spacer(Modifier.height(4.dp))
        Text(text = formatDateRange(start, end), fontSize = 13.sp, color = AppColors.TextMuted)
    }

    groups.forEach { (date, items) ->
        Spacer(Modifier.height(24.dp))
        DayHeader(date)
        items.forEach { item ->
            Spacer(Modifier.height(12.dp))
            ReadingCard(item = item, showSimulatedTag = tagSimulated && item.reading.isSimulated)
        }
    }

    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.history_end),
        modifier = Modifier.fillMaxWidth(),
        fontSize = 13.sp,
        color = AppColors.TextMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(
            if (hasSimulated) R.string.history_disclaimer_sample else R.string.instructions_disclaimer,
        ),
        fontSize = 13.sp,
        lineHeight = 20.sp,
        color = AppColors.TextMuted,
    )
}

@Composable
private fun DayHeader(date: LocalDate) {
    Text(
        text = formatLongDate(date),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.Burgundy,
    )
}

@Composable
private fun ReadingCard(item: AnalyzedReading, showSimulatedTag: Boolean) {
    val reading = item.reading
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatClock(reading.timestamp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Burgundy,
                    )
                    if (showSimulatedTag) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sample_data_tag),
                            fontSize = 12.sp,
                            color = AppColors.TextMuted,
                        )
                    }
                }
                StatusBadge(item.status)
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                MetricColumn(
                    icon = Icons.Outlined.MonitorHeart,
                    label = stringResource(R.string.heart_rate_short_label),
                    value = reading.bpm,
                    unit = stringResource(R.string.unit_bpm),
                    modifier = Modifier.weight(1f).padding(end = 16.dp),
                )
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(AppColors.Border))
                MetricColumn(
                    icon = Icons.Outlined.WaterDrop,
                    label = stringResource(R.string.oxygen_label),
                    value = reading.spo2,
                    unit = stringResource(R.string.unit_spo2),
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = AppColors.TextMuted)
            Spacer(Modifier.width(6.dp))
            Text(text = label, fontSize = 13.sp, color = AppColors.TextMuted)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.toString(),
                fontSize = 32.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = unit,
                fontSize = 13.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }
    }
}

/** Etiqueta do nível: fundo e texto na cor do nível, com a bolinha. */
@Composable
private fun StatusBadge(status: ReadingStatus) {
    val palette = status.palette()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(palette.background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(status.color())
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(status.labelRes()),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.content,
        )
    }
}

// ---------------------------------------------------------------- Previews

private fun previewReadings(): List<AnalyzedReading> {
    val analyze = AnalyzeReadingUseCase()
    val now = System.currentTimeMillis()
    val hour = 60 * 60 * 1000L
    val day = 24 * hour
    return listOf(
        Reading(bpm = 72, spo2 = 98, timestamp = now, isSimulated = true),
        Reading(bpm = 96, spo2 = 94, timestamp = now - 6 * hour, isSimulated = true),
        Reading(bpm = 112, spo2 = 93, timestamp = now - day, isSimulated = true),
        Reading(bpm = 76, spo2 = 98, timestamp = now - 3 * day, isSimulated = true),
    ).map(analyze::analyze)
}

@Preview(showBackground = true, heightDp = 1300)
@Composable
private fun HistoryWithReadingsPreview() {
    MyHeartTheme {
        HistoryContent(
            uiState = HistoryUiState(
                isLoading = false,
                readings = previewReadings(),
                rangeStart = LocalDate.now().minusDays(6),
                rangeEnd = LocalDate.now(),
            ),
            onPeriodSelected = {},
            onBackClick = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
private fun HistoryEmptyPreview() {
    MyHeartTheme {
        HistoryContent(
            uiState = HistoryUiState(isLoading = false),
            onPeriodSelected = {},
            onBackClick = {},
        )
    }
}
