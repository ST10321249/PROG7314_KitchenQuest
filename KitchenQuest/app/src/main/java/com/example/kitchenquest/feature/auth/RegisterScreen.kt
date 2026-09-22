package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun RegisterScreen(
    onRegister: (
        displayName: String,
        email: String,
        password: String
    ) -> Unit,
    onBackToLogin: () -> Unit,
    onEditDietaryPreferences: (() -> Unit)? = null,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    val displayNameIsValid = isValidDisplayName(displayName)
    val emailIsValid = isValidEmail(email)
    val passwordIsValid = isStrongPassword(password)
    val confirmationIsValid = passwordsMatch(password = password, confirmPassword = confirmPassword)

    val formIsValid = displayNameIsValid &&
            emailIsValid &&
            passwordIsValid &&
            confirmationIsValid &&
            acceptedTerms

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        KitchenQuestTopBar(
            title = "Create account",
            onBack = onBackToLogin,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KitchenQuestDimens.ScreenPadding)
        ) {

            if (onEditDietaryPreferences != null) {
                TextButton(
                    onClick = onEditDietaryPreferences,
                    enabled = !isLoading
                ) {
                    Text(text = "Edit dietary preferences")
                }

                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
            }

            KitchenQuestCard {

                KitchenQuestTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Display name",
                    modifier = Modifier.fillMaxWidth(),
                    isError = displayName.isNotEmpty() && !displayNameIsValid
                )

                Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

                KitchenQuestTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email address",
                    modifier = Modifier.fillMaxWidth(),
                    isError = email.isNotEmpty() && !emailIsValid,
                    supportingText = if (email.isNotEmpty() && !emailIsValid) {
                        "Enter a valid email address"
                    } else {
                        null
                    },
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

                KitchenQuestTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    modifier = Modifier.fillMaxWidth(),
                    isError = password.isNotEmpty() && !passwordIsValid,
                    supportingText = when {
                        password.isEmpty() -> "At least 8 characters with one capital letter, one number and one special character"
                        !passwordIsValid -> "Password must have at least 8 characters, one capital letter, one number and one special character"
                        else -> null
                    },
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(text = if (showPassword) "Hide" else "Show")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

                KitchenQuestTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm password",
                    modifier = Modifier.fillMaxWidth(),
                    isError = confirmPassword.isNotEmpty() && !confirmationIsValid,
                    supportingText = if (confirmPassword.isNotEmpty() && !confirmationIsValid) {
                        "Passwords do not match"
                    } else {
                        null
                    },
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showConfirmPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Text(text = if (showConfirmPassword) "Hide" else "Show")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it }
                    )

                    Text(
                        text = "I agree to the Terms of Use and Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

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
                    text = if (isLoading) "Creating account..." else "Create account",
                    onClick = {
                        onRegister(displayName.trim(), email.trim(), password)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = formIsValid && !isLoading
                )
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Already have an account?")

                TextButton(
                    onClick = onBackToLogin,
                    enabled = !isLoading
                ) {
                    Text(text = "Sign in")
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))
        }
    }
}
