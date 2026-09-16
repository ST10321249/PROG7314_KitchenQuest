package com.example.kitchenquest.feature.auth

import android.util.Patterns

fun isValidLoginEmail(email: String): Boolean {
    return email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
}

fun isValidLoginPassword(password: String): Boolean {
    return password.isNotBlank()
}