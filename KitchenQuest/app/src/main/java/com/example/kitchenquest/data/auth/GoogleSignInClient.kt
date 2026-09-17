package com.example.kitchenquest.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.kitchenquest.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInClient {

    companion object {
        private const val TAG =
            "GoogleSignInClient"
    }

    suspend fun getGoogleIdToken(
        activity: Activity
    ): String {

        Log.d(
            TAG,
            "Requesting Google credential."
        )

        val credentialManager =
            CredentialManager.create(
                activity
            )

        val googleSignInOption =
            GetSignInWithGoogleOption.Builder(
                activity.getString(
                    R.string.default_web_client_id
                )
            ).build()

        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(
                    googleSignInOption
                )
                .build()

        val result =
            credentialManager.getCredential(
                context = activity,
                request = request
            )

        Log.d(
            TAG,
            "Credential Manager returned a credential."
        )

        val credential =
            result.credential

        if (
            credential is CustomCredential &&
            credential.type ==
            GoogleIdTokenCredential
                .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            val googleCredential =
                GoogleIdTokenCredential.createFrom(
                    credential.data
                )

            Log.d(
                TAG,
                "Valid Google ID credential received."
            )

            return googleCredential.idToken
        }

        Log.w(
            TAG,
            "Credential Manager returned an unsupported credential type."
        )

        throw IllegalStateException(
            "Google sign-in returned an unsupported credential."
        )
    }

    suspend fun clearCredentialState(
        context: Context
    ) {

        Log.d(
            TAG,
            "Clearing Credential Manager state."
        )

        val credentialManager =
            CredentialManager.create(
                context
            )

        credentialManager
            .clearCredentialState(
                ClearCredentialStateRequest()
            )

        Log.d(
            TAG,
            "Credential Manager state cleared."
        )
    }
}