package com.example.kitchenquest.data.preferences

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Device-local app language; server/profile preference synchronization comes later. */
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
