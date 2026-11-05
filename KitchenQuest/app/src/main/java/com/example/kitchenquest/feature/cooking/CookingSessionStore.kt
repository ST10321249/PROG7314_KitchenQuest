package com.example.kitchenquest.feature.cooking

import android.content.Context
import android.util.Log
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.recipes.RecipeDetailDto
import kotlinx.serialization.Serializable

@Serializable
data class CookingSession(
    val recipe: RecipeDetailDto? = null,
    val currentStepIndex: Int = 0,
    val timers: List<CookingTimer> = emptyList(),
    val savedAtMillis: Long = 0
)

interface CookingSessionStore {
    fun load(): CookingSession?
    fun save(session: CookingSession)
    fun clear()
}

object NoOpCookingSessionStore : CookingSessionStore {
    override fun load(): CookingSession? = null
    override fun save(session: CookingSession) = Unit
    override fun clear() = Unit
}

class SharedPreferencesCookingSessionStore(
    context: Context,
    private val currentUserId: () -> String?
) : CookingSessionStore {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    override fun load(): CookingSession? {
        val key = key() ?: return null
        val json = preferences.getString(key, null) ?: return null
        return try {
            ApiClient.json.decodeFromString(CookingSession.serializer(), json).also {
                Log.d(TAG, "Restored cooking session with ${it.timers.size} timers")
            }
        } catch (error: Exception) {
            Log.w(TAG, "Saved cooking session could not be read, discarding it", error)
            preferences.edit().remove(key).apply()
            null
        }
    }

    override fun save(session: CookingSession) {
        val key = key() ?: return
        val json = ApiClient.json.encodeToString(CookingSession.serializer(), session)
        preferences.edit().putString(key, json).apply()
    }

    override fun clear() {
        val key = key() ?: return
        preferences.edit().remove(key).apply()
        Log.d(TAG, "Cleared cooking session")
    }

    private fun key(): String? = currentUserId()?.takeIf { it.isNotBlank() }?.let { "session_$it" }

    private companion object {
        const val TAG = "CookingSessionStore"
        const val PREFERENCES_NAME = "kitchenquest_cooking_session"
    }
}