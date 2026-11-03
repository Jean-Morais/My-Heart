package com.example.myheart.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myheart.R
import com.example.myheart.domain.model.NotifyChannel
import com.example.myheart.ui.components.AppCard
import com.example.myheart.ui.components.AppHeader
import com.example.myheart.ui.components.AppTextField
import com.example.myheart.ui.components.FormButton
import com.example.myheart.ui.components.NoticeCard
import com.example.myheart.ui.notification.notificationsAllowed
import com.example.myheart.ui.notification.openNotificationSettings
import com.example.myheart.ui.theme.AppColors
import com.example.myheart.ui.theme.MyHeartTheme

/** Eventos que a tela envia ao ViewModel (valores padrão vazios facilitam o @Preview). */
data class ProfileActions(
    val onNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onSaveAccountClick: () -> Unit = {},
    val onRelativeNameChange: (String) -> Unit = {},
    val onChannelChange: (NotifyChannel) -> Unit = {},
    val onRelativeEmailChange: (String) -> Unit = {},
    val onRelativePhoneChange: (String) -> Unit = {},
    val onConsentChange: (Boolean) -> Unit = {},
    val onNotificationsChange: (Boolean) -> Unit = {},
    val onOpenNotificationSettingsClick: () -> Unit = {},
    val onSaveContactClick: () -> Unit = {},
)

/** Composable de rota: é o único que conhece o ViewModel. */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Situação das notificações no aparelho. Reavaliada ao voltar das configurações do sistema.
    var allowed by remember { mutableStateOf(notificationsAllowed(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        allowed = notificationsAllowed(context)
    }
    LifecycleResumeEffect(Unit) {
        allowed = notificationsAllowed(context)
        onPauseOrDispose { }
    }

    ProfileContent(
        state = state,
        // O aviso está ligado, mas o aparelho não deixa o app publicar notificações.
        notificationsBlocked = state.notificationsEnabled && !allowed,
        actions = ProfileActions(
            onNameChange = viewModel::onNameChange,
            onEmailChange = viewModel::onEmailChange,
            onSaveAccountClick = viewModel::onSaveAccountClick,
            onRelativeNameChange = viewModel::onRelativeNameChange,
            onChannelChange = viewModel::onChannelChange,
            onRelativeEmailChange = viewModel::onRelativeEmailChange,
            onRelativePhoneChange = viewModel::onRelativePhoneChange,
            onConsentChange = viewModel::onConsentChange,
            onNotificationsChange = { enabled ->
                viewModel.onNotificationsChange(enabled)
                // A permissão é pedida no momento em que ela faz sentido: ao ligar o aviso (Android 13+).
                if (enabled && !allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onOpenNotificationSettingsClick = { openNotificationSettings(context) },
            onSaveContactClick = viewModel::onSaveContactClick,
        ),
        modifier = modifier,
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
    notificationsBlocked: Boolean = false,
) {
    Column(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        AppHeader(title = stringResource(R.string.app_name))

        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Burgundy,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.profile_subtitle),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = AppColors.TextMuted,
            )

            if (state.loadError) {
                Spacer(Modifier.height(16.dp))
                NoticeCard(
                    title = stringResource(R.string.profile_load_error_title),
                    body = stringResource(R.string.profile_load_error_body),
                )
            }

            Spacer(Modifier.height(20.dp))
            AccountCard(state, actions, notificationsBlocked)

            Spacer(Modifier.height(24.dp))
            RelativeCard(state, actions)

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AppColors.TextMuted,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.profile_disclaimer),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = AppColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun AccountCard(state: ProfileUiState, actions: ProfileActions, notificationsBlocked: Boolean) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CardTitle(stringResource(R.string.account_title))

            AppTextField(
                label = stringResource(R.string.account_name_label),
                value = state.name,
                onValueChange = actions.onNameChange,
                placeholder = stringResource(R.string.account_name_hint),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                errorText = state.nameError?.let { stringResource(it.messageRes()) },
            )
            AppTextField(
                label = stringResource(R.string.account_email_label),
                value = state.email,
                onValueChange = actions.onEmailChange,
                placeholder = stringResource(R.string.account_email_hint),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                errorText = state.emailError?.let { stringResource(it.messageRes()) },
            )
            NotificationsSwitch(
                checked = state.notificationsEnabled,
                onCheckedChange = actions.onNotificationsChange,
            )
            if (notificationsBlocked) {
                NoticeCard(
                    title = stringResource(R.string.notifications_blocked_title),
                    body = stringResource(R.string.notifications_blocked_body),
                    actionText = stringResource(R.string.notifications_blocked_action),
                    onActionClick = actions.onOpenNotificationSettingsClick,
                )
            }

            FormButton(
                text = stringResource(R.string.save_account),
                onClick = actions.onSaveAccountClick,
            )
            if (state.accountSaved) SuccessMessage(stringResource(R.string.account_saved))
            if (state.accountSaveError) SaveErrorNotice()
        }
    }
}

@Composable
private fun RelativeCard(state: ProfileUiState, actions: ProfileActions) {
    val byEmail = state.channel == NotifyChannel.Email

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CardTitle(stringResource(R.string.relative_title))
                Text(
                    text = stringResource(R.string.relative_description),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = AppColors.TextMuted,
                )
            }

            AppTextField(
                label = stringResource(R.string.relative_name_label),
                value = state.relativeName,
                onValueChange = actions.onRelativeNameChange,
                placeholder = stringResource(R.string.relative_name_hint),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                errorText = state.relativeNameError?.let { stringResource(it.messageRes()) },
            )

            Column {
                Text(
                    text = stringResource(R.string.channel_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Burgundy,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChannelOption(
                        text = stringResource(R.string.channel_email),
                        selected = byEmail,
                        onClick = { actions.onChannelChange(NotifyChannel.Email) },
                        modifier = Modifier.weight(1f),
                    )
                    ChannelOption(
                        text = stringResource(R.string.channel_phone),
                        selected = !byEmail,
                        onClick = { actions.onChannelChange(NotifyChannel.Phone) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // O mesmo campo muda de rótulo, teclado e valor conforme o canal escolhido.
            if (byEmail) {
                AppTextField(
                    label = stringResource(R.string.relative_email_label),
                    value = state.relativeEmail,
                    onValueChange = actions.onRelativeEmailChange,
                    placeholder = stringResource(R.string.relative_email_hint),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    errorText = state.relativeContactError?.let { stringResource(it.messageRes()) },
                )
            } else {
                AppTextField(
                    label = stringResource(R.string.relative_phone_label),
                    value = state.relativePhone,
                    onValueChange = actions.onRelativePhoneChange,
                    placeholder = stringResource(R.string.relative_phone_hint),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                    errorText = state.relativeContactError?.let { stringResource(it.messageRes()) },
                )
            }

            Column {
                ConsentBox(checked = state.consent, onCheckedChange = actions.onConsentChange)
                if (state.consentError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.error_consent),
                        fontSize = 12.sp,
                        color = AppColors.AlertIcon,
                    )
                }
            }

            FormButton(
                text = stringResource(R.string.save_contact),
                onClick = actions.onSaveContactClick,
                outlined = true,
            )
            if (state.contactSaved) SuccessMessage(stringResource(R.string.contact_saved))
            if (state.contactSaveError) SaveErrorNotice()
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.Burgundy,
    )
}

@Composable
private fun ChannelOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) AppColors.Burgundy else AppColors.Surface)
            .then(if (selected) Modifier else Modifier.border(1.dp, AppColors.Border, shape))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.White,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else AppColors.Burgundy,
        )
    }
}

@Composable
private fun ConsentBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.AlertBackground)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // onCheckedChange = null: quem trata o clique é o Row inteiro (área de toque maior).
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = AppColors.Burgundy,
                uncheckedColor = AppColors.Burgundy,
                checkmarkColor = Color.White,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.consent_text),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = AppColors.TextMuted,
        )
    }
}

@Composable
private fun NotificationsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.notifications_switch_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Burgundy,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.notifications_switch_description),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AppColors.TextMuted,
            )
        }
        Spacer(Modifier.width(12.dp))
        // onCheckedChange = null: quem trata o toque é o Row inteiro (área de toque maior).
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppColors.Burgundy,
            ),
        )
    }
}

@Composable
private fun SaveErrorNotice() {
    NoticeCard(
        title = stringResource(R.string.profile_save_error_title),
        body = stringResource(R.string.profile_save_error_body),
    )
}

@Composable
private fun SuccessMessage(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppColors.SuccessIcon,
        )
        Spacer(Modifier.width(6.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.SuccessIcon)
    }
}

private fun FieldError.messageRes(): Int = when (this) {
    FieldError.Required -> R.string.error_required
    FieldError.InvalidEmail -> R.string.error_invalid_email
    FieldError.InvalidPhone -> R.string.error_invalid_phone
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ProfileContentEmptyPreview() {
    MyHeartTheme {
        ProfileContent(state = ProfileUiState(), actions = ProfileActions())
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun ProfileContentDatabaseErrorsPreview() {
    MyHeartTheme {
        ProfileContent(
            state = ProfileUiState(loadError = true, accountSaveError = true, contactSaveError = true),
            actions = ProfileActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun ProfileContentErrorsPreview() {
    MyHeartTheme {
        ProfileContent(
            state = ProfileUiState(
                email = "joao@",
                nameError = FieldError.Required,
                emailError = FieldError.InvalidEmail,
                channel = NotifyChannel.Phone,
                relativeContactError = FieldError.InvalidPhone,
                consentError = true,
            ),
            actions = ProfileActions(),
        )
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun ProfileContentNotificationsBlockedPreview() {
    MyHeartTheme {
        ProfileContent(
            state = ProfileUiState(
                name = "Maria",
                email = "maria@exemplo.com",
                notificationsEnabled = true,
            ),
            actions = ProfileActions(),
            notificationsBlocked = true,
        )
    }
}
