package com.example.kitchenquest.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth =
        FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    override suspend fun register(
        displayName: String,
        email: String,
        password: String
    ): Result<AuthUser> {
        return runCatching {

            val result = firebaseAuth
                .createUserWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val firebaseUser = result.user
                ?: throw IllegalStateException(
                    "Firebase did not return a user after registration."
                )

            val cleanDisplayName =
                displayName.trim()

            val profileUpdate =
                UserProfileChangeRequest.Builder()
                    .setDisplayName(
                        cleanDisplayName
                    )
                    .build()

            firebaseUser
                .updateProfile(profileUpdate)
                .await()

            AuthUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = cleanDisplayName
            )
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthUser> {
        return runCatching {

            val result = firebaseAuth
                .signInWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val firebaseUser = result.user
                ?: throw IllegalStateException(
                    "Firebase did not return a user after login."
                )

            firebaseUser.toAuthUser()
        }
    }

    override suspend fun signInWithGoogle(
        idToken: String
    ): Result<AuthUser> {
        return runCatching {

            val credential =
                GoogleAuthProvider.getCredential(
                    idToken,
                    null
                )

            val result = firebaseAuth
                .signInWithCredential(
                    credential
                )
                .await()

            val firebaseUser = result.user
                ?: throw IllegalStateException(
                    "Firebase did not return a user after Google sign-in."
                )

            firebaseUser.toAuthUser()
        }
    }

    override suspend fun sendPasswordReset(
        email: String
    ): Result<Unit> {
        return runCatching {

            firebaseAuth
                .sendPasswordResetEmail(
                    email.trim()
                )
                .await()

            Unit
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName
        )
    }
}