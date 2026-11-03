package com.example.myheart.ui.familyalert

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import com.example.myheart.domain.model.NotifyChannel

/**
 * Deixa só o que o app de mensagens precisa: dígitos e, se o número começar com "+", o "+".
 * "(88) 99999-8888" vira "88999998888"; "+55 88 99999-8888" vira "+5588999998888".
 */
fun sanitizePhoneForSms(raw: String): String {
    val trimmed = raw.trim()
    val digits = trimmed.filter { it.isDigit() }
    return if (trimmed.startsWith("+")) "+$digits" else digits
}

/**
 * Abre o app de e-mail ou de mensagens com o destinatário e o texto já preenchidos. O app NÃO envia
 * nada: o usuário confere e confirma o envio lá (por isso não precisa de nenhuma permissão).
 *
 * Devolve false se o aparelho não tem app para isso (sem e-mail configurado, por exemplo).
 */
fun openFamilyAlert(
    context: Context,
    channel: NotifyChannel,
    contact: String,
    subject: String,
    body: String,
): Boolean {
    val intent = when (channel) {
        // "mailto:" sem endereço na URI: o destinatário vai no extra, o que evita problemas de codificação.
        NotifyChannel.Email -> Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contact))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        NotifyChannel.Phone -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + sanitizePhoneForSms(contact))).apply {
            putExtra("sms_body", body)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/**
 * Copia a mensagem para a área de transferência (plano B quando não há app de e-mail ou de
 * mensagens). Marcada como sensível: o Android 13 ou superior não mostra o conteúdo na prévia.
 */
fun copyFamilyAlertText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText("My Heart", text)
    clip.description.extras = PersistableBundle().apply {
        putBoolean("android.content.extra.IS_SENSITIVE", true)
    }
    clipboard.setPrimaryClip(clip)
}
