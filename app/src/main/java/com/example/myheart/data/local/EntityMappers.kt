package com.example.myheart.data.local

import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.RelativeContact
import com.example.myheart.domain.model.UserProfile

// Conversão entre o modelo de banco (Entity) e o de domínio. O repositório converte antes de
// devolver, então o Room nunca vaza para o ViewModel ou para a tela.

fun ProfileEntity.toDomain() = UserProfile(name = name, email = email, notificationsEnabled = notificationsEnabled)

fun UserProfile.toEntity() = ProfileEntity(name = name, email = email, notificationsEnabled = notificationsEnabled)

fun RelativeContactEntity.toDomain() = RelativeContact(
    name = name,
    // Se o texto gravado não corresponder a nenhum canal, cai para E-mail em vez de quebrar o app.
    channel = NotifyChannel.entries.firstOrNull { it.name == channel } ?: NotifyChannel.Email,
    contact = contact,
    consent = consent,
)

fun RelativeContact.toEntity() = RelativeContactEntity(
    name = name,
    channel = channel.name,
    contact = contact,
    consent = consent,
)

fun ReadingEntity.toDomain() = Reading(
    bpm = bpm,
    spo2 = spo2,
    timestamp = timestamp,
    isSimulated = isSimulated,
    id = id,
)

fun Reading.toEntity() = ReadingEntity(
    id = id,
    bpm = bpm,
    spo2 = spo2,
    timestamp = timestamp,
    isSimulated = isSimulated,
)
