package com.example.kitchenquest.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.kitchenquest.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSignInClient {

    suspend fun getGoogleIdToken(
        activity: Activity
    ): String {

        val credentialManager =
            CredentialManager.create(activity)

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

            return googleCredential.idToken
        }

        throw IllegalStateException(
            "Google sign-in returned an unsupported credential."
        )
    }

    suspend fun clearCredentialState(
        context: Context
    ) {
        val credentialManager =
            CredentialManager.create(context)

        credentialManager.clearCredentialState(
            ClearCredentialStateRequest()
        )
    }
}