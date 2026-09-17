package com.example.kitchenquest.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.auth.AuthRepository
import com.example.kitchenquest.data.auth.AuthUser
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

    companion object {
        private const val TAG =
            "KitchenQuestAuth"
    }

    private val initialUser =
        authRepository.currentUser

    private val _uiState =
        MutableStateFlow(
            AuthUiState(
                isAuthChecked = true,
                user = initialUser
            )
        )

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    init {
        if (initialUser != null) {
            Log.d(
                TAG,
                "Existing Firebase session found."
            )
        } else {
            Log.d(
                TAG,
                "No existing Firebase session found."
            )
        }
    }

    fun login(
        email: String,
        password: String
    ) {
        Log.d(
            TAG,
            "Email and password sign-in started."
        )

        setLoadingState()

        viewModelScope.launch {

            val result =
                authRepository.login(
                    email = email,
                    password = password
                )

            handleAuthResult(
                result = result,
                actionName =
                    "Email and password sign-in"
            )
        }
    }

    fun register(
        displayName: String,
        email: String,
        password: String
    ) {
        Log.d(
            TAG,
            "Account registration started."
        )

        setLoadingState()

        viewModelScope.launch {

            val result =
                authRepository.register(
                    displayName = displayName,
                    email = email,
                    password = password
                )

            handleAuthResult(
                result = result,
                actionName =
                    "Account registration"
            )
        }
    }

    fun startGoogleSignIn() {
        Log.d(
            TAG,
            "Google sign-in started."
        )

        setLoadingState()
    }

    fun signInWithGoogle(
        idToken: String
    ) {
        Log.d(
            TAG,
            "Google credential received. Starting Firebase authentication."
        )

        setLoadingState()

        viewModelScope.launch {

            val result =
                authRepository.signInWithGoogle(
                    idToken
                )

            handleAuthResult(
                result = result,
                actionName =
                    "Google sign-in"
            )
        }
    }

    fun googleSignInFailed(
        message: String
    ) {
        Log.w(
            TAG,
            "Google sign-in did not complete."
        )

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
        Log.d(
            TAG,
            "Password reset request started."
        )

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                passwordResetSent = false
            )
        }

        viewModelScope.launch {

            val result =
                authRepository
                    .sendPasswordReset(
                        email
                    )

            result.fold(
                onSuccess = {

                    Log.d(
                        TAG,
                        "Password reset request completed."
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordResetSent = true,
                            errorMessage = null
                        )
                    }
                },

                onFailure = { error ->

                    Log.e(
                        TAG,
                        "Password reset request failed: " +
                                error.javaClass.simpleName
                    )

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

    fun signOut() {

        Log.d(
            TAG,
            "Firebase sign-out started."
        )

        authRepository.signOut()

        _uiState.update {
            AuthUiState(
                isAuthChecked = true
            )
        }

        Log.d(
            TAG,
            "Firebase sign-out completed."
        )
    }

    fun clearFeedback() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                passwordResetSent = false
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
        result: Result<AuthUser>,
        actionName: String
    ) {
        result.fold(
            onSuccess = { user ->

                Log.d(
                    TAG,
                    "$actionName completed successfully."
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        errorMessage = null
                    )
                }
            },

            onFailure = { error ->

                Log.e(
                    TAG,
                    "$actionName failed: " +
                            error.javaClass.simpleName
                )

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