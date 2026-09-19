package com.example.kitchenquest.data.network

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

interface TokenProvider {

    // Called on an OkHttp background thread, so blocking is acceptable.
    fun getToken(): String?
}

class FirebaseTokenProvider : TokenProvider {

    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun getToken(): String? {
        val user = firebaseAuth.currentUser ?: return null

        return try {
            // Returns the cached token and refreshes it automatically once expired.
            Tasks.await(user.getIdToken(false)).token
        } catch (error: Exception) {
            null
        }
    }
}
