package com.example.myheart.ui.reading

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myheart.R
import com.example.myheart.domain.model.AnalyzedReading
import com.example.myheart.domain.model.FamilyAlert
import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.domain.model.Reading
import com.example.myheart.domain.model.ReadingStatus
import com.example.myheart.ui.components.AppCard
import com.example.myheart.ui.components.NoticeCard
import com.example.myheart.ui.components.PrimaryActionButton
import com.example.myheart.ui.components.labelRes
import com.example.myheart.ui.familyalert.copyFamilyAlertText
import com.example.myheart.ui.familyalert.openFamilyAlert
import com.example.myheart.ui.format.formatReadingMoment
import com.example.myheart.ui.theme.AppColors
import com.example.myheart.ui.theme.MyHeartTheme

/** O que aconteceu depois do toque no botão do cartão. */
private enum class FamilyAlertOutcome { Idle, NoApp, Copied }

/**
 * Oferta de aviso ao familiar, mostrada no resultado de uma leitura fora do normal. Abre o app de
 * e-mail ou de mensagens com o texto pronto; o usuário confere e confirma o envio. Nada sai do
 * celular sem essa confirmação. Se não houver app para isso, oferece copiar a mensagem.
 */
@Composable
fun FamilyAlertCard(
    alert: FamilyAlert,
    simulated: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var outcome by remember { mutableStateOf(FamilyAlertOutcome.Idle) }

    val reading = alert.reading.reading
    val moment = formatReadingMoment(reading.timestamp)
    val bpmLevel = stringResource(alert.reading.bpmStatus.labelRes())
    val spo2Level = stringResource(alert.reading.spo2Status.labelRes())
    val who = alert.userName ?: stringResource(R.string.family_alert_unknown_person)
    val sampleNote = if (simulated) stringResource(R.string.family_alert_sample_note) else ""

    val subject = stringResource(R.string.family_alert_subject)
    val body = when (alert.channel) {
        NotifyChannel.Email -> stringResource(
            R.string.family_alert_email_body,
            alert.relativeName, who, moment, reading.bpm, bpmLevel, reading.spo2, spo2Level,
        )
        NotifyChannel.Phone -> stringResource(
            R.string.family_alert_sms_body,
            who, moment, reading.bpm, bpmLevel, reading.spo2, spo2Level,
        )
    } + sampleNote

    val isEmail = alert.channel == NotifyChannel.Email

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.family_alert_title, alert.relativeName),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Burgundy,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (isEmail) R.string.family_alert_body_email else R.string.family_alert_body_sms,
                ),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.family_alert_privacy),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AppColors.TextMuted,
            )
            Spacer(Modifier.height(14.dp))
            PrimaryActionButton(
                text = stringResource(if (isEmail) R.string.family_alert_action_email else R.string.family_alert_action_sms),
                icon = Icons.AutoMirrored.Outlined.Send,
                enabled = true,
                onClick = {
                    outcome = if (openFamilyAlert(context, alert.channel, alert.contact, subject, body)) {
                        FamilyAlertOutcome.Idle
                    } else {
                        FamilyAlertOutcome.NoApp
                    }
                },
            )

            when (outcome) {
                FamilyAlertOutcome.NoApp -> {
                    Spacer(Modifier.height(12.dp))
                    NoticeCard(
                        title = stringResource(R.string.family_alert_no_app_title),
                        body = stringResource(
                            if (isEmail) R.string.family_alert_no_app_email else R.string.family_alert_no_app_sms,
                        ),
                        actionText = stringResource(R.string.family_alert_copy),
                        onActionClick = {
                            copyFamilyAlertText(context, body)
                            outcome = FamilyAlertOutcome.Copied
                        },
                    )
                }
                FamilyAlertOutcome.Copied -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.family_alert_copied),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.StatusNormalText,
                    )
                }
                FamilyAlertOutcome.Idle -> Unit
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FamilyAlertCardPreview() {
    MyHeartTheme {
        val reading = Reading(bpm = 72, spo2 = 85, timestamp = System.currentTimeMillis())
        FamilyAlertCard(
            alert = FamilyAlert(
                userName = "Maria",
                relativeName = "Ana",
                channel = NotifyChannel.Phone,
                contact = "(88) 99999-8888",
                reading = AnalyzedReading(
                    reading = reading,
                    status = ReadingStatus.Critical,
                    bpmStatus = ReadingStatus.Normal,
                    spo2Status = ReadingStatus.Critical,
                ),
            ),
            simulated = false,
            modifier = Modifier.padding(24.dp),
        )
    }
}
