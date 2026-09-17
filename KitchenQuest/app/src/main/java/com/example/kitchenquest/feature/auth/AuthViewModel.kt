package com.example.kitchenquest.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.auth.AuthRepository
import com.example.kitchenquest.data.auth.FirebaseAuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository =
        FirebaseAuthRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            AuthUiState(
                isAuthChecked = true,
                user = authRepository.currentUser
            )
        )

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    fun login(
        email: String,
        password: String
    ) {
        setLoadingState()

        viewModelScope.launch {

            val result =
                authRepository.login(
                    email = email,
                    password = password
                )

            handleAuthResult(result)
        }
    }

    fun register(
        displayName: String,
        email: String,
        password: String
    ) {
        setLoadingState()

        viewModelScope.launch {

            val result =
                authRepository.register(
                    displayName = displayName,
                    email = email,
                    password = password
                )

            handleAuthResult(result)
        }
    }

    fun startGoogleSignIn() {
        setLoadingState()
    }

    fun signInWithGoogle(
        idToken: String
    ) {
        setLoadingState()

        viewModelScope.launch {

            val result =
                authRepository.signInWithGoogle(
                    idToken
                )

            handleAuthResult(result)
        }
    }

    fun googleSignInFailed(
        message: String
    ) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message
            )
        }
    }

    fun sendPasswordReset(
        email: String
    ) {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                passwordResetSent = false
            )
        }

        viewModelScope.launch {

            val result =
                authRepository.sendPasswordReset(
                    email
                )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordResetSent = true,
                            errorMessage = null
                        )
                    }
                },

                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage =
                                getResetErrorMessage(
                                    error
                                )
                        )
                    }
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                passwordResetSent = false
            )
        }
    }

    fun signOut() {

        authRepository.signOut()

        _uiState.update {
            AuthUiState(
                isAuthChecked = true
            )
        }
    }

    private fun setLoadingState() {
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }
    }

    private fun handleAuthResult(
        result:
        Result<com.example.kitchenquest.data.auth.AuthUser>
    ) {
        result.fold(
            onSuccess = { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        errorMessage = null
                    )
                }
            },

            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage =
                            getAuthErrorMessage(
                                error
                            )
                    )
                }
            }
        )
    }

    private fun getAuthErrorMessage(
        error: Throwable
    ): String {
        return when (error) {

            is FirebaseAuthUserCollisionException ->
                "An account already exists with this email."

            is FirebaseAuthInvalidUserException ->
                "Invalid email or password."

            is FirebaseAuthInvalidCredentialsException ->
                "Invalid email or password."

            is FirebaseAuthWeakPasswordException ->
                "The password does not meet the required security rules."

            is FirebaseNetworkException ->
                "Network error. Check your connection and try again."

            else ->
                "Authentication failed. Please try again."
        }
    }

    private fun getResetErrorMessage(
        error: Throwable
    ): String {
        return when (error) {

            is FirebaseNetworkException ->
                "Network error. Check your connection and try again."

            else ->
                "Unable to send the reset email. Please try again."
        }
    }
}