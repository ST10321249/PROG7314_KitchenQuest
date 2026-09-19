package com.example.kitchenquest.data.user

import com.example.kitchenquest.data.auth.AuthUser
import com.example.kitchenquest.feature.onboarding.OnboardingSelection

// Makes sure the signed-in user has a server profile that matches the
// choices saved on this device. The server only applies preferences when
// it creates a profile, so an existing profile that differs is updated.
suspend fun UserRepository.syncSignedInUser(
    user: AuthUser,
    local: OnboardingSelection?
): Result<UserProfileDto> {
    val synced = syncUser(
        displayName = user.displayName?.takeIf { it.isNotBlank() },
        dietaryPreferences = local?.dietaryPreferences?.toList(),
        avoidedIngredients = local?.avoidedIngredients?.toList()
    )

    val profile = synced.getOrElse { return synced }

    if (local == null) {
        return synced
    }

    val matches =
        profile.dietaryPreferences.toSet() == local.dietaryPreferences &&
                profile.avoidedIngredients.toSet() == local.avoidedIngredients

    if (matches) {
        return synced
    }

    return updateProfile(
        dietaryPreferences = local.dietaryPreferences.toList(),
        avoidedIngredients = local.avoidedIngredients.toList()
    )
}
