package com.example.myheart

import android.app.Application
import androidx.room.Room
import com.example.myheart.data.local.MIGRATION_1_2
import com.example.myheart.data.local.MIGRATION_2_3
import com.example.myheart.data.local.MIGRATION_3_4
import com.example.myheart.data.local.MyHeartDatabase
import com.example.myheart.data.notification.AndroidAlertNotifier
import com.example.myheart.data.repository.ProfileRepositoryImpl
import com.example.myheart.data.repository.ReadingRepositoryImpl
import com.example.myheart.domain.notification.AlertNotifier
import com.example.myheart.domain.repository.ProfileRepository
import com.example.myheart.domain.repository.ReadingRepository

/**
 * Hospeda o que precisa existir uma única vez durante a vida do app: o banco, os repositórios e o
 * publicador de notificações (cujo canal é criado ao iniciar o app).
 * É criada pelo Android antes de qualquer tela (registrada no manifesto) e a MainActivity
 * pega os repositórios daqui para montar os ViewModels.
 *
 * "by lazy": o banco só é aberto na primeira vez que alguém o usa.
 */
class MyHeartApplication : Application() {

    private val androidAlertNotifier: AndroidAlertNotifier by lazy { AndroidAlertNotifier(this) }

    val alertNotifier: AlertNotifier get() = androidAlertNotifier

    override fun onCreate() {
        super.onCreate()
        // O canal precisa existir antes da primeira notificação; criar de novo é inofensivo.
        androidAlertNotifier.ensureChannel()
    }

    private val database: MyHeartDatabase by lazy {
        Room.databaseBuilder(this, MyHeartDatabase::class.java, DATABASE_NAME)
            // Cada mudança de versão do banco precisa de uma migração explícita aqui.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(database.profileDao())
    }

    val readingRepository: ReadingRepository by lazy {
        ReadingRepositoryImpl(database.readingDao())
    }

    private companion object {
        const val DATABASE_NAME = "myheart.db"
    }
}
