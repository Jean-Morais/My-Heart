package com.example.myheart.data.repository

import com.example.myheart.data.local.ProfileDao
import com.example.myheart.data.local.toDomain
import com.example.myheart.data.local.toEntity
import com.example.myheart.domain.model.RelativeContact
import com.example.myheart.domain.model.UserProfile
import com.example.myheart.domain.repository.ProfileRepository

/** Implementação do repositório sobre o Room: delega ao DAO e converte entre os dois modelos. */
class ProfileRepositoryImpl(
    private val dao: ProfileDao,
) : ProfileRepository {

    override suspend fun getProfile(): UserProfile? = dao.getProfile()?.toDomain()

    override suspend fun saveProfile(profile: UserProfile) = dao.upsertProfile(profile.toEntity())

    override suspend fun getRelativeContact(): RelativeContact? = dao.getRelativeContact()?.toDomain()

    override suspend fun saveRelativeContact(contact: RelativeContact) =
        dao.upsertRelativeContact(contact.toEntity())
}
