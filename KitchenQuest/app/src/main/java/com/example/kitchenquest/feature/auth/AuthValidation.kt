package com.example.kitchenquest.feature.auth

import android.util.Patterns

fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
}

fun isValidLoginPassword(password: String): Boolean {
    return password.isNotBlank()
}

fun isValidDisplayName(displayName: String): Boolean {
    return displayName.trim().isNotEmpty()
}

fun isStrongPassword(password: String): Boolean {
    return password.length >= 8 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isDigit() } &&
            password.any { !it.isLetterOrDigit() }
}

fun passwordsMatch(
    password: String,
    confirmPassword: String
): Boolean {
    return confirmPassword.isNotEmpty() &&
            password == confirmPassword
}