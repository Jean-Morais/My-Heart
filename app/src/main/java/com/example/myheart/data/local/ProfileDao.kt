package com.example.myheart.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * Operações no banco. As funções são "suspend": o Room as executa fora da thread principal,
 * então o ViewModel pode chamá-las direto de uma coroutine sem travar a tela.
 *
 * "Upsert" insere se a linha não existe e atualiza se já existe.
 */
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile LIMIT 1")
    suspend fun getProfile(): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM relative_contact LIMIT 1")
    suspend fun getRelativeContact(): RelativeContactEntity?

    @Upsert
    suspend fun upsertRelativeContact(contact: RelativeContactEntity)
}
