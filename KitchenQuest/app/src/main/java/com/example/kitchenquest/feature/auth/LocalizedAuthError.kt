package com.example.kitchenquest.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.kitchenquest.R

/** Keeps Firebase error mapping unchanged while localising known user-facing messages. */
@Composable
fun localizedAuthError(error: String?): String? = when (error) {
    null -> null
    "Google sign-in is unavailable." -> stringResource(R.string.auth_google_unavailable)
    "Google sign-in was cancelled." -> stringResource(R.string.auth_google_cancelled)
    "Google sign-in failed. Please try again." -> stringResource(R.string.auth_google_failed)
    "Google sign-in did not complete." -> stringResource(R.string.auth_google_incomplete)
    "An account already exists with this email." -> stringResource(R.string.auth_server_account_exists)
    "Invalid email or password." -> stringResource(R.string.auth_server_invalid)
    "The password does not meet the required security rules." -> stringResource(R.string.auth_server_weak_password)
    "Network error. Check your connection and try again." -> stringResource(R.string.auth_server_network)
    "Authentication failed. Please try again." -> stringResource(R.string.auth_server_failed)
    "Unable to send the reset email. Please try again." -> stringResource(R.string.auth_server_reset_failed)
    else -> error
}
