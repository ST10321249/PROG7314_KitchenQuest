package com.example.kitchenquest.feature.auth

import androidx.compose.ui.res.stringResource
import com.example.kitchenquest.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun ForgotPasswordScreen(
    onSendResetLink: (String) -> Unit,
    onBackToLogin: () -> Unit,
    isLoading: Boolean = false,
    resetRequested: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }
    val emailValid = isValidEmail(email)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = KitchenQuestDimens.ScreenPadding)
    ) {
        KitchenQuestTopBar(title = stringResource(R.string.auth_reset_password), onBack = onBackToLogin)

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Surface(
            modifier = Modifier.align(Alignment.Start),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KitchenQuestDimens.LargeCorner),
            color = KitchenOrangeLight
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing)
            )
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text(
            text = if (resetRequested) stringResource(R.string.auth_check_inbox) else stringResource(R.string.auth_check_inbox),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        Text(
            text = if (resetRequested) {
                stringResource(R.string.auth_reset_sent)
            } else {
                stringResource(R.string.auth_reset_instructions)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        KitchenQuestTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(R.string.auth_email),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Email,
            isError = attemptedSubmit && !emailValid,
            supportingText = if (attemptedSubmit && !emailValid) stringResource(R.string.auth_email_invalid) else null
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
            Text(text = localizedAuthError(errorMessage) ?: errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

        KitchenQuestPrimaryButton(
            text = if (isLoading) stringResource(R.string.auth_sending) else stringResource(R.string.auth_send_reset),
            onClick = {
                attemptedSubmit = true
                if (emailValid) onSendResetLink(email.trim())
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(Modifier.weight(1f))

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.auth_back_login))
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))
    }
}
