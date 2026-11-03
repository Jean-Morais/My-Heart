package com.example.myheart.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** O app tem um único perfil: a linha sempre usa este identificador, então salvar substitui a anterior. */
const val SINGLE_ROW_ID = 1

/** Modelo de banco do perfil. É diferente do modelo de domínio de propósito (ver EntityMappers). */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val name: String,
    val email: String,
    // Coluna acrescentada na versão 4 (migração 3 para 4); o padrão 0 vale para perfis já salvos.
    @ColumnInfo(defaultValue = "0") val notificationsEnabled: Boolean = false,
)
