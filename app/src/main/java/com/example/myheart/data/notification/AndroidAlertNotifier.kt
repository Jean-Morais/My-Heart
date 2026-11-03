package com.example.myheart.data.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myheart.MainActivity
import com.example.myheart.R
import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.domain.notification.AlertNotifier

/**
 * Publica o aviso como notificação do Android.
 *
 * Privacidade: o texto com BPM e SpO2 só aparece com o aparelho desbloqueado. Na tela de bloqueio
 * a notificação mostra uma versão genérica (sem valores de saúde). Existe uma única notificação de
 * aviso por vez: uma leitura nova substitui a anterior em vez de empilhar.
 */
class AndroidAlertNotifier(context: Context) : AlertNotifier {

    private val appContext = context.applicationContext

    /** Cria o canal de notificações. É seguro chamar várias vezes (o Android ignora repetições). */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = appContext.getString(R.string.alert_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // A permissão POST_NOTIFICATIONS (Android 13+) é conferida logo abaixo por areNotificationsEnabled().
    @SuppressLint("MissingPermission")
    override fun notifyAbnormal(reading: AnalyzedReading): Boolean {
        val manager = NotificationManagerCompat.from(appContext)
        // Sem permissão, ou com as notificações do app desligadas nas configurações: não publica.
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel()
        if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) {
            return false
        }

        val sample = reading.reading
        val title = appContext.getString(
            if (reading.status == ReadingStatus.Critical) R.string.alert_title_critical
            else R.string.alert_title_attention,
        )
        var text = appContext.getString(R.string.alert_text, sample.bpm, sample.spo2)
        if (sample.isSimulated) text += " • " + appContext.getString(R.string.alert_illustrative)

        val publicVersion = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_heart)
            .setContentTitle(appContext.getString(R.string.app_name))
            .setContentText(appContext.getString(R.string.alert_public_text))
            .build()

        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_heart)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setWhen(sample.timestamp)
            .setShowWhen(true)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            // A permissão foi retirada entre a conferência e a publicação.
            false
        }
    }

    private companion object {
        const val CHANNEL_ID = "abnormal_reading_alerts"
        const val NOTIFICATION_ID = 1001
    }
}
