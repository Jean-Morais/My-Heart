package com.example.myheart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modelo de banco do familiar. O canal é guardado como texto (nome do enum), o que dispensa um
 * conversor de tipos e mantém o banco legível.
 */
@Entity(tableName = "relative_contact")
data class RelativeContactEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val name: String,
    val channel: String,
    val contact: String,
    val consent: Boolean,
)
