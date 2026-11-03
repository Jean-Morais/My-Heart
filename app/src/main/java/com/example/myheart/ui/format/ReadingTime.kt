package com.example.myheart.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.myheart.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Em que dia, em relação a hoje, caiu um instante. */
enum class RelativeDay { Today, Yesterday, Other }

/**
 * Compara o dia civil de [timestamp] com o de [now] no fuso [zone]. Compara datas, não "24 horas":
 * uma leitura de 23h50 de ontem, vista às 00h10, ainda é "Ontem". Parâmetros com valor padrão
 * existem para permitir testes com data fixa.
 */
fun relativeDay(
    timestamp: Long,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): RelativeDay {
    val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return when (day) {
        today -> RelativeDay.Today
        today.minusDays(1) -> RelativeDay.Yesterday
        else -> RelativeDay.Other
    }
}

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", ptBr)
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", ptBr)

/** "Hoje, 14:32", "Ontem, 14:32" ou "28/10/2026, 14:32". Usada por todas as telas que mostram leituras. */
@Composable
fun formatReadingMoment(timestamp: Long): String {
    val moment = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val clock = timeFormatter.format(moment)
    return when (relativeDay(timestamp)) {
        RelativeDay.Today -> stringResource(R.string.reading_moment_today, clock)
        RelativeDay.Yesterday -> stringResource(R.string.reading_moment_yesterday, clock)
        RelativeDay.Other -> stringResource(R.string.reading_moment_date, dateFormatter.format(moment), clock)
    }
}

// ---------------------------------------------------------------- Funções puras (sem Compose)

private val longDateFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ptBr)
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", ptBr)

/** Dia civil de um instante no fuso [zone]. */
fun localDateOf(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

/** Instante (em milissegundos) em que [date] começa no fuso [zone]. */
fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

/** "14:32". */
fun formatClock(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    timeFormatter.format(Instant.ofEpochMilli(timestamp).atZone(zone))

/** "30/10/2026". */
fun formatShortDate(date: LocalDate): String = dateFormatter.format(date)

/** "30 de outubro de 2026". */
fun formatLongDate(date: LocalDate): String = longDateFormatter.format(date)

/**
 * Intervalo de datas por extenso: "24 a 30 de outubro de 2026" (mesmo mês),
 * "28 de setembro a 30 de outubro de 2026" (meses diferentes) ou a data única quando são iguais.
 */
fun formatDateRange(start: LocalDate, end: LocalDate): String = when {
    start == end -> longDateFormatter.format(end)
    start.year == end.year && start.month == end.month ->
        "${start.dayOfMonth} a ${longDateFormatter.format(end)}"
    start.year == end.year ->
        "${dayMonthFormatter.format(start)} a ${longDateFormatter.format(end)}"
    else -> "${longDateFormatter.format(start)} a ${longDateFormatter.format(end)}"
}
