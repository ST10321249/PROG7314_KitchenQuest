package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onEditDietaryPreferences: (() -> Unit)? = null,
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
            .padding(
                KitchenQuestDimens.ScreenPadding
            ),
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = "Welcome back",
            style =
                MaterialTheme.typography
                    .headlineMedium
        )

        if (
            onEditDietaryPreferences != null
        ) {

            TextButton(
                onClick =
                    onEditDietaryPreferences,
                enabled =
                    !isLoading
            ) {
                Text(
                    text =
                        "Edit dietary preferences"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SectionSpacing
            )
        )

        KitchenQuestTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = "Email address",
            modifier =
                Modifier.fillMaxWidth(),
            isError =
                emailError != null,
            supportingText =
                emailError,
            keyboardType =
                KeyboardType.Email
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .FieldSpacing
            )
        )

        KitchenQuestTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = "Password",
            modifier =
                Modifier.fillMaxWidth(),
            isError =
                passwordError != null,
            supportingText =
                passwordError,
            keyboardType =
                KeyboardType.Password,
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
            }
        )

        TextButton(
            onClick =
                onForgotPassword,
            enabled =
                !isLoading
        ) {

            Text(
                text =
                    "Forgot password?"
            )
        }

        if (errorMessage != null) {

            Text(
                text =
                    errorMessage,
                color =
                    MaterialTheme
                        .colorScheme
                        .error,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )

            Spacer(
                modifier =
                    Modifier.height(
                        KitchenQuestDimens
                            .SmallSpacing
                    )
            )
        }

        KitchenQuestPrimaryButton(
            text =
                if (isLoading) {
                    "Signing in..."
                } else {
                    "Sign in"
                },
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
            enabled =
                !isLoading
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .FieldSpacing
            )
        )

        KitchenQuestSecondaryButton(
            text =
                "Continue with Google",
            onClick =
                onGoogleSignIn,
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !isLoading
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .FieldSpacing
            )
        )

        TextButton(
            onClick =
                onCreateAccount,
            modifier =
                Modifier.fillMaxWidth(),
            enabled =
                !isLoading
        ) {

            Text(
                text =
                    "Create an account"
            )
        }
    }
}