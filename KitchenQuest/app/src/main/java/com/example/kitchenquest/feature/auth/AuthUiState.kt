package com.example.kitchenquest.feature.auth

import com.example.kitchenquest.data.auth.AuthUser

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: AuthUser? = null,
    val errorMessage: String? = null,
    val passwordResetSent: Boolean = false
)