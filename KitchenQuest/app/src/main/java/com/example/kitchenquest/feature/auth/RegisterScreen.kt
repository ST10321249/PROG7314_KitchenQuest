package com.example.kitchenquest.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun RegisterScreen(
    onRegister: (
        displayName: String,
        email: String,
        password: String
    ) -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by rememberSaveable {
        mutableStateOf("")
    }

    var email by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var showPassword by rememberSaveable {
        mutableStateOf(false)
    }

    var showConfirmPassword by rememberSaveable {
        mutableStateOf(false)
    }

    var acceptedTerms by rememberSaveable {
        mutableStateOf(false)
    }

    val displayNameIsValid = isValidDisplayName(displayName)
    val emailIsValid = isValidEmail(email)
    val passwordIsValid = isStrongPassword(password)

    val confirmationIsValid = passwordsMatch(
        password = password,
        confirmPassword = confirmPassword
    )

    val formIsValid =
        displayNameIsValid &&
                emailIsValid &&
                passwordIsValid &&
                confirmationIsValid &&
                acceptedTerms

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Create account"
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Display name")
            },
            singleLine = true,
            isError = displayName.isNotEmpty() &&
                    !displayNameIsValid
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Email address")
            },
            singleLine = true,
            isError = email.isNotEmpty() &&
                    !emailIsValid,
            supportingText = {
                if (email.isNotEmpty() && !emailIsValid) {
                    Text(
                        text = "Enter a valid email address"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Password")
            },
            singleLine = true,
            isError = password.isNotEmpty() &&
                    !passwordIsValid,
            supportingText = {
                if (password.isEmpty()) {
                    Text(
                        text = "At least 8 characters with one capital letter, one number and one special character"
                    )
                } else if (!passwordIsValid) {
                    Text(
                        text = "Password must have at least 8 characters, one capital letter, one number and one special character"
                    )
                }
            },
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = {
                        showPassword = !showPassword
                    }
                ) {
                    Text(
                        text = if (showPassword) {
                            "Hide"
                        } else {
                            "Show"
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Confirm password")
            },
            singleLine = true,
            isError = confirmPassword.isNotEmpty() &&
                    !confirmationIsValid,
            supportingText = {
                if (
                    confirmPassword.isNotEmpty() &&
                    !confirmationIsValid
                ) {
                    Text(
                        text = "Passwords do not match"
                    )
                }
            },
            visualTransformation = if (showConfirmPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = {
                        showConfirmPassword =
                            !showConfirmPassword
                    }
                ) {
                    Text(
                        text = if (showConfirmPassword) {
                            "Hide"
                        } else {
                            "Show"
                        }
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = acceptedTerms,
                onCheckedChange = {
                    acceptedTerms = it
                }
            )

            Text(
                text = "I agree to the Terms of Use and Privacy Policy"
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = {
                onRegister(
                    displayName.trim(),
                    email.trim(),
                    password
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = formIsValid
        ) {
            Text(
                text = "Create account"
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Already have an account?"
            )

            TextButton(
                onClick = onBackToLogin
            ) {
                Text(
                    text = "Sign in"
                )
            }
        }
    }
}