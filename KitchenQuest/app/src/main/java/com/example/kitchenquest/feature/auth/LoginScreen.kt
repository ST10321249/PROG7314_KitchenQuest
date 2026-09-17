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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var showPassword by rememberSaveable {
        mutableStateOf(false)
    }

    var attemptedSubmit by rememberSaveable {
        mutableStateOf(false)
    }

    val emailIsValid =
        isValidEmail(email)

    val passwordIsValid =
        isValidLoginPassword(password)

    val emailError = when {
        !attemptedSubmit -> null

        email.isBlank() ->
            "Email is required"

        !emailIsValid ->
            "Enter a valid email address"

        else -> null
    }

    val passwordError = when {
        !attemptedSubmit -> null

        password.isBlank() ->
            "Password is required"

        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "Welcome back"
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text("Email address")
            },
            singleLine = true,
            isError = emailError != null,
            supportingText = {
                if (emailError != null) {
                    Text(emailError)
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Email
                )
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            singleLine = true,
            isError =
                passwordError != null,
            supportingText = {
                if (passwordError != null) {
                    Text(passwordError)
                }
            },
            visualTransformation =
                if (showPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
            trailingIcon = {
                TextButton(
                    onClick = {
                        showPassword =
                            !showPassword
                    }
                ) {
                    Text(
                        text =
                            if (showPassword) {
                                "Hide"
                            } else {
                                "Show"
                            }
                    )
                }
            },
            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Password
                )
        )

        TextButton(
            onClick = onForgotPassword,
            enabled = !isLoading
        ) {
            Text(
                text = "Forgot password?"
            )
        }

        if (errorMessage != null) {

            Text(
                text = errorMessage
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )
        }

        Button(
            onClick = {
                attemptedSubmit = true

                if (
                    emailIsValid &&
                    passwordIsValid
                ) {
                    onLogin(
                        email.trim(),
                        password
                    )
                }
            },
            modifier =
                Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                text =
                    if (isLoading) {
                        "Signing in..."
                    } else {
                        "Sign in"
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onGoogleSignIn,
            modifier =
                Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                text = "Continue with Google"
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        TextButton(
            onClick = onCreateAccount,
            modifier =
                Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(
                text = "Create an account"
            )
        }
    }
}