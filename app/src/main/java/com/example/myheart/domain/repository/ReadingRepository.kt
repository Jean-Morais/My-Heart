package com.example.myheart.domain.repository

import com.example.myheart.domain.model.Reading
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso às leituras gravadas. Mesmo papel do ProfileRepository: o ViewModel só conhece
 * esta interface, nunca o Room.
 *
 * Diferente do perfil (carga pontual, decisão A16), as consultas do histórico são observáveis:
 * quando uma leitura nova é gravada, o gráfico e o histórico se atualizam sozinhos.
 */
interface ReadingRepository {
    suspend fun saveReading(reading: Reading)

    /** A leitura mais recente, ou null se ainda não houver nenhuma. */
    suspend fun getLastReading(): Reading?

    /** Observa a leitura mais recente (null enquanto não houver nenhuma). */
    fun observeLastReading(): Flow<Reading?>

    /**
     * Observa as leituras do intervalo, da mais recente para a mais antiga.
     * [fromInclusive] e [toExclusive] são instantes em milissegundos; null deixa o lado aberto.
     */
    fun observeReadings(fromInclusive: Long? = null, toExclusive: Long? = null): Flow<List<Reading>>
}
