package com.example.myheart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Banco local do app. Qualquer mudança nas tabelas exige aumentar [version] e escrever uma migração
 * (ver Migrations.kt).
 *
 * Histórico de versões:
 *  1 - perfil e contato do familiar (Etapa 1)
 *  2 - tabela de leituras (Etapa 2)
 *  3 - switch de aviso no contato do familiar (Etapa 5)
 *  4 - o switch passa para o perfil do usuário (Etapa 5)
 *
 * exportSchema = false: não gera o arquivo JSON de schema. É aceitável aqui porque o app ainda não
 * foi publicado; as migrações continuam funcionando normalmente.
 */
@Database(
    entities = [ProfileEntity::class, RelativeContactEntity::class, ReadingEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class MyHeartDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun readingDao(): ReadingDao
}
