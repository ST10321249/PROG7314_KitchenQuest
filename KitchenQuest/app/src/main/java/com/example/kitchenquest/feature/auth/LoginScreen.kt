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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
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
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var attemptedSubmit by rememberSaveable { mutableStateOf(false) }

    val emailIsValid = isValidEmail(email)
    val passwordIsValid = isValidLoginPassword(password)

    val emailError = when {
        !attemptedSubmit -> null
        email.isBlank() -> "Email is required"
        !emailIsValid -> "Enter a valid email address"
        else -> null
    }

    val passwordError = when {
        !attemptedSubmit -> null
        password.isBlank() -> "Password is required"
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KitchenQuestDimens.ScreenPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
        ) {
            Surface(
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(KitchenQuestDimens.SmallSpacing)
                )
            }
            Text(text = "KitchenQuest", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(KitchenQuestDimens.ExtraLargeSpacing))

        Text(text = "Welcome back", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
        Text(
            text = "Sign in to reach your kitchen, saved recipes and cooking progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        KitchenQuestTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email address",
            modifier = Modifier.fillMaxWidth(),
            isError = emailError != null,
            supportingText = emailError,
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(KitchenQuestDimens.FieldSpacing))

        KitchenQuestTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            isError = passwordError != null,
            supportingText = passwordError,
            keyboardType = KeyboardType.Password,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Hide" else "Show")
                }
            }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onForgotPassword, enabled = !isLoading) {
                Text("Forgot password?", color = MaterialTheme.colorScheme.primary)
            }
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
            text = if (isLoading) "Signing in..." else "Sign in",
            onClick = {
                attemptedSubmit = true
                if (emailIsValid && passwordIsValid) {
                    onLogin(email.trim(), password)
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
            text = "Continue with Google",
            onClick = onGoogleSignIn,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        if (onEditDietaryPreferences != null) {
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
            TextButton(
                onClick = onEditDietaryPreferences,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Edit dietary preferences")
            }
        }

        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCreateAccount, enabled = !isLoading) {
                Text("Create account")
            }
        }
    }
}
