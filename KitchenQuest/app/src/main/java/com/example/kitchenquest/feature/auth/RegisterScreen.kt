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
import androidx.compose.material3.HorizontalDivider
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
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun RegisterScreen(
    onRegister: (displayName: String, email: String, password: String) -> Unit,
    onBackToLogin: () -> Unit,
    onGoogleSignIn: () -> Unit,
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
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }

    val displayNameIsValid = isValidDisplayName(displayName)
    val emailIsValid = isValidEmail(email)
    val passwordIsValid = isStrongPassword(password)
    val confirmationIsValid = passwordsMatch(password, confirmPassword)
    val formIsValid = displayNameIsValid && emailIsValid && passwordIsValid && confirmationIsValid && acceptedTerms

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KitchenQuestDimens.ScreenPadding)
    ) {
        KitchenQuestTopBar(title = "Create account", onBack = onBackToLogin)

        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

        KitchenQuestTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = "Display name",
            modifier = Modifier.fillMaxWidth(),
            isError = attemptedSubmit && !displayNameIsValid,
            supportingText = if (attemptedSubmit && !displayNameIsValid) "Display name is required" else null
        )

        Spacer(Modifier.height(KitchenQuestDimens.FieldSpacing))

        KitchenQuestTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email address",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Email,
            isError = attemptedSubmit && !emailIsValid,
            supportingText = if (attemptedSubmit && !emailIsValid) "Enter a valid email address" else null
        )

        Spacer(Modifier.height(KitchenQuestDimens.FieldSpacing))

        KitchenQuestTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Hide" else "Show")
                }
            },
            isError = attemptedSubmit && !passwordIsValid,
            supportingText = if (attemptedSubmit && !passwordIsValid) {
                "Use at least 8 characters with an uppercase letter, number and symbol"
            } else {
                "At least 8 characters, with an uppercase letter, number and symbol."
            }
        )

        Spacer(Modifier.height(KitchenQuestDimens.FieldSpacing))

        KitchenQuestTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm password",
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            isError = attemptedSubmit && !confirmationIsValid,
            supportingText = if (attemptedSubmit && !confirmationIsValid) "Passwords must match" else null
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = { acceptedTerms = it }
            )
            Text(
                text = "I agree to the Terms of Use and Privacy Policy.",
                style = MaterialTheme.typography.labelMedium
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
        }

        KitchenQuestPrimaryButton(
            text = if (isLoading) "Creating account..." else "Create account",
            onClick = {
                attemptedSubmit = true
                if (formIsValid) {
                    onRegister(displayName.trim(), email.trim(), password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.MediumSpacing)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("or", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        KitchenQuestSecondaryButton(
            text = "Sign up with Google",
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        if (onEditDietaryPreferences != null) {
            TextButton(
                onClick = onEditDietaryPreferences,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Edit dietary preferences")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onBackToLogin, enabled = !isLoading) {
                Text("Sign in")
            }
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))
    }
}
