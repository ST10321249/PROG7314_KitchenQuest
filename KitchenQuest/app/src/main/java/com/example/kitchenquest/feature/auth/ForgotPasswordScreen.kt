package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun ForgotPasswordScreen(
    onSendResetLink: (String) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }

    var attemptedSubmit by rememberSaveable {
        mutableStateOf(false)
    }

    var resetRequested by rememberSaveable {
        mutableStateOf(false)
    }

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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Reset password"
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        if (resetRequested) {

            Text(
                text = "Check your inbox"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "If an account exists for this email, a password reset link has been sent."
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Back to sign in"
                )
            }

        } else {

            Text(
                text = "Enter the email address registered with your account."
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = "Email address"
                    )
                },
                singleLine = true,
                isError = emailError != null,
                supportingText = {
                    if (emailError != null) {
                        Text(
                            text = emailError
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Button(
                onClick = {
                    attemptedSubmit = true

                    if (emailIsValid) {
                        onSendResetLink(
                            email.trim()
                        )

                        resetRequested = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Send reset link"
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            TextButton(
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Back to sign in"
                )
            }
        }
    }
}