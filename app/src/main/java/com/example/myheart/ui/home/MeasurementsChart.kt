package com.example.myheart.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myheart.R
import com.example.myheart.ui.components.AppCard
import com.example.myheart.ui.components.color
import com.example.myheart.ui.components.labelRes
import com.example.myheart.ui.theme.AppColors

private val PlotHeight = 200.dp
private val AxisWidth = 36.dp
private val ValueLabelHeight = 20.dp
private val SlotWidth = 48.dp
private val BarWidth = 28.dp

/** "96 bpm" ou "96%". */
@Composable
fun metricValueText(metric: HomeMetric, value: Int): String = when (metric) {
    HomeMetric.Bpm -> stringResource(R.string.value_bpm, value)
    HomeMetric.Spo2 -> stringResource(R.string.value_percent, value)
}

/** Unidade mostrada no canto do cartão do gráfico. */
@Composable
private fun metricUnitLabel(metric: HomeMetric): String = when (metric) {
    HomeMetric.Bpm -> stringResource(R.string.unit_bpm)
    HomeMetric.Spo2 -> stringResource(R.string.unit_percent)
}

/**
 * Gráfico de barras das leituras de um dia: uma barra por leitura, altura pelo valor, cor pelo nível
 * e horário embaixo. Com muitas leituras rola para os lados e abre já no fim, onde está a mais recente.
 */
@Composable
fun MeasurementsChartCard(
    title: String,
    series: ChartSeries,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Burgundy,
                )
                Text(text = metricUnitLabel(series.metric), fontSize = 13.sp, color = AppColors.TextMuted)
            }

            Spacer(Modifier.height(16.dp))

            if (series.bars.isEmpty()) {
                EmptyChartMessage()
            } else {
                ChartPlot(series)
            }
        }
    }
}

@Composable
private fun ChartPlot(series: ChartSeries) {
    val scrollState = rememberScrollState()
    // Quando a largura do conteúdo muda (primeira medição da tela ou leitura nova), vai para o fim.
    LaunchedEffect(scrollState.maxValue) { scrollState.scrollTo(scrollState.maxValue) }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            YAxis(series)
            Row(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
                series.bars.forEach { bar -> BarSlot(bar = bar, series = series) }
            }
        }
        if (scrollState.maxValue > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chart_scroll_hint),
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                color = AppColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Eixo Y fixo: o centro de cada rótulo fica exatamente sobre o valor. */
@Composable
private fun YAxis(series: ChartSeries) {
    Column(modifier = Modifier.width(AxisWidth)) {
        // Mesmo espaço que as barras reservam para o valor acima delas.
        Spacer(Modifier.height(ValueLabelHeight))
        Box(modifier = Modifier.width(AxisWidth).height(PlotHeight)) {
            Text(
                text = series.axisTicks[0].toString(),
                fontSize = 12.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.align(Alignment.TopStart).offset(y = (-8).dp),
            )
            Text(
                text = series.axisTicks[1].toString(),
                fontSize = 12.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = series.axisTicks[2].toString(),
                fontSize = 12.sp,
                color = AppColors.TextMuted,
                modifier = Modifier.align(Alignment.BottomStart).offset(y = 8.dp),
            )
        }
    }
}

@Composable
private fun BarSlot(bar: ChartBar, series: ChartSeries) {
    val fraction = ((bar.value - series.axisMin).toFloat() /
        (series.axisMax - series.axisMin)).coerceIn(0.02f, 1f)
    val description = stringResource(
        R.string.chart_bar_description,
        bar.time,
        metricValueText(series.metric, bar.value),
        stringResource(bar.level.labelRes()),
    )

    Column(
        modifier = Modifier
            .width(SlotWidth)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Valor logo acima da barra; a barra mais alta ainda cabe porque há ValueLabelHeight de folga.
        Column(
            modifier = Modifier.height(ValueLabelHeight + PlotHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(ValueLabelHeight),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    text = bar.value.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                )
            }
            Box(
                modifier = Modifier
                    .width(BarWidth)
                    .height(PlotHeight * fraction)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bar.level.color()),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = bar.time, fontSize = 12.sp, color = AppColors.TextMuted, maxLines = 1)
    }
}

@Composable
private fun EmptyChartMessage() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.chart_empty_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.Burgundy,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.chart_empty_body),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = AppColors.TextMuted,
        )
    }
}
