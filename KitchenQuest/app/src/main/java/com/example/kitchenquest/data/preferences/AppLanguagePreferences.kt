package com.example.kitchenquest.data.preferences

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Device-local UI language with an account-scoped pending server update. */
class AppLanguagePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getLanguage(): String = AppLanguage.normalized(
        preferences.getString(KEY_LANGUAGE, AppLanguage.ENGLISH)
    )

    /** Save before recreating the Activity so the new locale loads immediately. */
    fun setLanguage(code: String): Boolean {
        if (!AppLanguage.isSupported(code)) return false
        val next = AppLanguage.normalized(code)
        if (next == getLanguage()) return false
        return preferences.edit().putString(KEY_LANGUAGE, next).commit()
    }

    /** Store a pending profile update before Activity recreation or a connection loss. */
    fun queueProfileUpdate(userId: String, language: String) {
        if (userId.isBlank() || !AppLanguage.isSupported(language)) return
        preferences.edit().putString(pendingKey(userId), AppLanguage.normalized(language)).apply()
    }

    fun pendingProfileUpdate(userId: String): String? =
        if (userId.isBlank()) null else preferences.getString(pendingKey(userId), null)

    /** Only clear the exact value that the server accepted. */
    fun acknowledgeProfileUpdate(userId: String, language: String) {
        if (pendingProfileUpdate(userId) == language) {
            preferences.edit().remove(pendingKey(userId)).apply()
        }
    }

    private fun pendingKey(userId: String) = "pending_language_$userId"

    companion object {
        private const val PREFS_NAME = "kitchenquest_app_language"
        private const val KEY_LANGUAGE = "language"

        /** A localized Context used before the Activity loads its resources. */
        fun localizedContext(base: Context): Context {
            val code = AppLanguagePreferences(base).getLanguage()
            val locale = Locale.forLanguageTag(code)
            val updated = Configuration(base.resources.configuration).apply {
                setLocale(locale)
                setLayoutDirection(locale)
            }
            return base.createConfigurationContext(updated)
        }
    }
}
