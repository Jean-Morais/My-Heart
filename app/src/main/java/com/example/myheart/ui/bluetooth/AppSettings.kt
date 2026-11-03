package com.example.myheart.ui.bluetooth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/** Abre a tela de informações do app nas configurações (onde se liberam as permissões depois de uma negação definitiva). */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Aparelho sem essa tela: não há o que fazer além de não derrubar o app.
    }
}
