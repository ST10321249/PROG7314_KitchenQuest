package com.example.kitchenquest.data.preferences

import android.content.Context
import com.example.kitchenquest.feature.onboarding.OnboardingSelection

class OnboardingPreferences(
    context: Context
) {

    private val flowPreferences =
        context.getSharedPreferences(
            FLOW_PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private val userPreferences =
        context.getSharedPreferences(
            USER_PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun hasAuthenticatedBefore(): Boolean {
        return flowPreferences
            .getBoolean(
                KEY_HAS_AUTHENTICATED_BEFORE,
                false
            )
    }

    fun markAuthenticatedBefore() {
        flowPreferences
            .edit()
            .putBoolean(
                KEY_HAS_AUTHENTICATED_BEFORE,
                true
            )
            .apply()
    }

    fun savePendingSelection(
        selection: OnboardingSelection
    ) {
        flowPreferences
            .edit()
            .putBoolean(
                KEY_HAS_PENDING_SELECTION,
                true
            )
            .putStringSet(
                KEY_PENDING_DIETARY,
                selection.dietaryPreferences.toSet()
            )
            .putStringSet(
                KEY_PENDING_AVOIDED,
                selection.avoidedIngredients.toSet()
            )
            .apply()
    }

    fun getPendingSelection(): OnboardingSelection? {
        val hasPending =
            flowPreferences
                .getBoolean(
                    KEY_HAS_PENDING_SELECTION,
                    false
                )

        if (!hasPending) {
            return null
        }

        val dietary =
            flowPreferences
                .getStringSet(
                    KEY_PENDING_DIETARY,
                    setOf("No restrictions")
                )
                ?.toSet()
                ?: setOf("No restrictions")

        val avoided =
            flowPreferences
                .getStringSet(
                    KEY_PENDING_AVOIDED,
                    emptySet()
                )
                ?.toSet()
                ?: emptySet()

        return OnboardingSelection(
            dietaryPreferences = dietary,
            avoidedIngredients = avoided
        )
    }

    fun clearPendingSelection() {
        flowPreferences
            .edit()
            .remove(KEY_HAS_PENDING_SELECTION)
            .remove(KEY_PENDING_DIETARY)
            .remove(KEY_PENDING_AVOIDED)
            .apply()
    }

    fun hasCompletedPreferences(
        uid: String
    ): Boolean {
        return userPreferences
            .getBoolean(
                userKey(uid, "complete"),
                false
            )
    }

    fun saveUserPreferences(
        uid: String,
        selection: OnboardingSelection
    ) {
        val existing = getUserPreferences(uid)

        if (existing == selection) {
            return
        }

        userPreferences
            .edit()
            .putStringSet(
                userKey(uid, "dietary"),
                selection.dietaryPreferences.toSet()
            )
            .putStringSet(
                userKey(uid, "avoided"),
                selection.avoidedIngredients.toSet()
            )
            .putBoolean(
                userKey(uid, "complete"),
                true
            )
            .apply()
    }

    fun getUserPreferences(
        uid: String
    ): OnboardingSelection? {
        if (!hasCompletedPreferences(uid)) {
            return null
        }

        val dietary =
            userPreferences
                .getStringSet(
                    userKey(uid, "dietary"),
                    setOf("No restrictions")
                )
                ?.toSet()
                ?: setOf("No restrictions")

        val avoided =
            userPreferences
                .getStringSet(
                    userKey(uid, "avoided"),
                    emptySet()
                )
                ?.toSet()
                ?: emptySet()

        return OnboardingSelection(
            dietaryPreferences = dietary,
            avoidedIngredients = avoided
        )
    }

    private fun userKey(
        uid: String,
        property: String
    ): String {
        return "user_${uid}_$property"
    }

    companion object {
        private const val FLOW_PREFS_NAME =
            "kitchenquest_flow_preferences"

        private const val USER_PREFS_NAME =
            "kitchenquest_user_preferences"

        private const val KEY_HAS_AUTHENTICATED_BEFORE =
            "has_authenticated_before"

        private const val KEY_HAS_PENDING_SELECTION =
            "has_pending_selection"

        private const val KEY_PENDING_DIETARY =
            "pending_dietary"

        private const val KEY_PENDING_AVOIDED =
            "pending_avoided"
    }
}