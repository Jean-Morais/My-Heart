package com.example.myheart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modelo de banco da leitura. Diferente do perfil, aqui há várias linhas (uma por leitura),
 * então o identificador é gerado pelo banco.
 *
 * Se mudar qualquer campo, o comando SQL em MIGRATION_1_2 também precisa mudar (ou criar nova migração).
 */
@Entity(tableName = "reading")
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bpm: Int,
    val spo2: Int,
    val timestamp: Long,
    val isSimulated: Boolean,
)
