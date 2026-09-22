package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
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

    val emailIsValid = isValidEmail(email)

    val emailError = when {
        !attemptedSubmit -> null
        email.isBlank() -> "Email is required"
        !emailIsValid -> "Enter a valid email address"
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        KitchenQuestTopBar(
            title = "Reset password",
            onBack = onBackToLogin,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KitchenQuestDimens.ScreenPadding)
        ) {

            KitchenQuestCard {

                if (resetRequested) {

                    Text(
                        text = "Check your inbox",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                    Text(
                        text = "If an account exists for this email, a password reset link has been sent.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

                    TextButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Back to sign in")
                    }

                } else {

                    Text(
                        text = "Enter the email address registered with your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

                    KitchenQuestTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email address",
                        modifier = Modifier.fillMaxWidth(),
                        isError = emailError != null,
                        supportingText = emailError,
                        keyboardType = KeyboardType.Email
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                    KitchenQuestPrimaryButton(
                        text = if (isLoading) "Sending..." else "Send reset link",
                        onClick = {
                            attemptedSubmit = true

                            if (emailIsValid) {
                                onSendResetLink(email.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

                    TextButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text(text = "Back to sign in")
                    }
                }
            }
        }
    }
}
