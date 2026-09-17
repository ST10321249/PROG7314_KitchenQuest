package com.example.kitchenquest.data.preferences

import android.content.Context

class OnboardingPreferences(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun isOnboardingComplete(): Boolean {
        return preferences.getBoolean(
            KEY_ONBOARDING_COMPLETE,
            false
        )
    }

    fun setOnboardingComplete() {
        preferences
            .edit()
            .putBoolean(
                KEY_ONBOARDING_COMPLETE,
                true
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "kitchenquest_preferences"

        private const val KEY_ONBOARDING_COMPLETE =
            "onboarding_complete"
    }
}