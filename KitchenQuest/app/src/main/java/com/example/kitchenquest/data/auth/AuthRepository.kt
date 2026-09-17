package com.example.kitchenquest.data.auth

interface AuthRepository {

    val currentUser: AuthUser?

    suspend fun register(
        displayName: String,
        email: String,
        password: String
    ): Result<AuthUser>

    suspend fun login(
        email: String,
        password: String
    ): Result<AuthUser>

    suspend fun signInWithGoogle(
        idToken: String
    ): Result<AuthUser>

    suspend fun sendPasswordReset(
        email: String
    ): Result<Unit>

    fun signOut()
}