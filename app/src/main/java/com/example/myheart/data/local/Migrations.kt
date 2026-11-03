package com.example.myheart.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versão 1 -> 2: cria a tabela de leituras. Perfil e familiar não são tocados, então os dados
 * já salvos nos aparelhos continuam lá.
 *
 * O texto SQL precisa descrever exatamente o que o Room espera de [ReadingEntity] (nomes, tipos e
 * NOT NULL). Se divergir, o app falha ao abrir o banco com "Migration didn't properly handle".
 * Nunca usar fallbackToDestructiveMigration: ele apagaria os dados do usuário em silêncio.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reading` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`bpm` INTEGER NOT NULL, " +
                "`spo2` INTEGER NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`isSimulated` INTEGER NOT NULL)"
        )
    }
}

/**
 * Versão 2 -> 3: acrescentou ao familiar o switch de aviso em caso de anormalidade.
 * (O switch mudou para o perfil na versão 4; esta migração continua aqui porque aparelhos na
 * versão 2 ainda passam por ela antes da 3 -> 4.)
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `relative_contact` ADD COLUMN `notificationsEnabled` INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * Versão 3 -> 4: o switch de aviso passa do familiar para o perfil do usuário (a notificação é
 * local, então a configuração é de quem usa o app).
 *
 *  1. Acrescenta `notificationsEnabled` ao perfil (padrão 0 = desligado).
 *  2. Copia a escolha que já estava no contato, para o usuário não perder o que ligou.
 *  3. Recria `relative_contact` sem a coluna antiga: o SQLite dos aparelhos com Android 9 não
 *     remove colunas, e o Room recusa um banco com colunas a mais.
 *
 * O DEFAULT 0 precisa bater com @ColumnInfo(defaultValue = "0") em [ProfileEntity], e a tabela
 * recriada com o que o Room espera de [RelativeContactEntity].
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `profile` ADD COLUMN `notificationsEnabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "UPDATE `profile` SET `notificationsEnabled` = " +
                "COALESCE((SELECT `notificationsEnabled` FROM `relative_contact` LIMIT 1), 0)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `relative_contact_new` (" +
                "`id` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`channel` TEXT NOT NULL, " +
                "`contact` TEXT NOT NULL, " +
                "`consent` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "INSERT INTO `relative_contact_new` (`id`, `name`, `channel`, `contact`, `consent`) " +
                "SELECT `id`, `name`, `channel`, `contact`, `consent` FROM `relative_contact`"
        )
        db.execSQL("DROP TABLE `relative_contact`")
        db.execSQL("ALTER TABLE `relative_contact_new` RENAME TO `relative_contact`")
    }
}
