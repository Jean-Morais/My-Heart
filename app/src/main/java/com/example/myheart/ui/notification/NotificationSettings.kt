package com.example.myheart.ui.notification

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * true quando o app pode publicar notificações agora. No Android 13 ou superior isso inclui a
 * permissão de notificação; nas versões anteriores só diz se o usuário não as desligou.
 */
fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/** Abre a tela de notificações do app nas configurações do aparelho (único caminho depois de uma negação definitiva). */
fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Aparelho sem essa tela: não há o que fazer além de não derrubar o app.
    }
}
