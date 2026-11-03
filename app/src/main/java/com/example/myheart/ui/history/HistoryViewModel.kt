package com.example.myheart.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.repository.ReadingRepository
import com.example.myheart.domain.usecase.AnalyzeReadingUseCase
import com.example.myheart.ui.format.localDateOf
import com.example.myheart.ui.format.startOfDayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

/** Filtro de período da tela de Histórico. */
enum class HistoryPeriod { Week, Month, All }

data class HistoryUiState(
    val period: HistoryPeriod = HistoryPeriod.Week,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    /** Da mais recente para a mais antiga. */
    val readings: List<AnalyzedReading> = emptyList(),
    /** Primeiro e último dia do período mostrado, para o texto "24 a 30 de outubro de 2026". */
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate? = null,
)

private const val TAG = "HistoryViewModel"

class HistoryViewModel(
    private val repository: ReadingRepository,
    private val analyzeReading: AnalyzeReadingUseCase,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(HistoryPeriod.Week)

    /**
     * Refaz a consulta sempre que o período muda e se atualiza sozinho quando uma leitura é gravada.
     * WhileSubscribed: sem a tela aberta, o banco deixa de ser observado após 5 s.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = selectedPeriod
        .flatMapLatest { period -> observe(period) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    fun onPeriodSelected(period: HistoryPeriod) {
        selectedPeriod.value = period
    }

    private fun observe(period: HistoryPeriod): Flow<HistoryUiState> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        // "7 dias" e "30 dias" contam o dia de hoje: de hoje menos 6 (ou 29) até hoje.
        val firstDay = when (period) {
            HistoryPeriod.Week -> today.minusDays(6)
            HistoryPeriod.Month -> today.minusDays(29)
            HistoryPeriod.All -> null
        }

        return repository
            .observeReadings(fromInclusive = firstDay?.let { startOfDayMillis(it, zone) })
            .map { readings ->
                HistoryUiState(
                    period = period,
                    isLoading = false,
                    readings = readings.map(analyzeReading::analyze),
                    rangeStart = firstDay ?: readings.lastOrNull()?.let { localDateOf(it.timestamp, zone) },
                    rangeEnd = if (firstDay != null) today else readings.firstOrNull()?.let { localDateOf(it.timestamp, zone) },
                )
            }
            .catch { e ->
                Log.e(TAG, "Falha ao carregar o histórico", e)
                emit(HistoryUiState(period = period, isLoading = false, hasError = true))
            }
    }
}
