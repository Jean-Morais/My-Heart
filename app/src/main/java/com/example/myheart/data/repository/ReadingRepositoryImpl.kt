package com.example.myheart.data.repository

import com.example.myheart.data.local.ReadingDao
import com.example.myheart.data.local.toDomain
import com.example.myheart.data.local.toEntity
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.repository.ReadingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Implementação do repositório de leituras sobre o Room. */
class ReadingRepositoryImpl(
    private val dao: ReadingDao,
) : ReadingRepository {

    override suspend fun saveReading(reading: Reading) {
        dao.insert(reading.toEntity())
    }

    override suspend fun getLastReading(): Reading? = dao.getLast()?.toDomain()

    override fun observeLastReading(): Flow<Reading?> =
        dao.observeLast().map { it?.toDomain() }

    override fun observeReadings(fromInclusive: Long?, toExclusive: Long?): Flow<List<Reading>> =
        dao.observeBetween(
            fromInclusive = fromInclusive ?: Long.MIN_VALUE,
            toExclusive = toExclusive ?: Long.MAX_VALUE,
        ).map { list -> list.map { it.toDomain() } }
}
