package com.example.myheart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    @Insert
    suspend fun insert(reading: ReadingEntity): Long

    /** Desempata por id caso duas leituras tenham o mesmo horário. */
    @Query("SELECT * FROM reading ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLast(): ReadingEntity?

    @Query("SELECT * FROM reading ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun observeLast(): Flow<ReadingEntity?>

    /** Leituras com horário em [fromInclusive, toExclusive), da mais recente para a mais antiga. */
    @Query(
        "SELECT * FROM reading WHERE timestamp >= :fromInclusive AND timestamp < :toExclusive " +
            "ORDER BY timestamp DESC, id DESC",
    )
    fun observeBetween(fromInclusive: Long, toExclusive: Long): Flow<List<ReadingEntity>>
}
