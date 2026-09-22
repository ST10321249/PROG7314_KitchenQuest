package com.example.kitchenquest.feature.auth

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
        KitchenQuestTopBar(title = "Reset password", onBack = onBackToLogin)

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
            text = if (resetRequested) "Check your inbox" else "Check your inbox",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        Text(
            text = if (resetRequested) {
                "If an account exists for that email, a password reset link has been sent."
            } else {
                "Enter the email you registered with and we'll send a link to set a new password."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        KitchenQuestTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email address",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Email,
            isError = attemptedSubmit && !emailValid,
            supportingText = if (attemptedSubmit && !emailValid) "Enter a valid email address" else null
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

        KitchenQuestPrimaryButton(
            text = if (isLoading) "Sending..." else "Send reset link",
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
            Text("Back to sign in")
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))
    }
}
