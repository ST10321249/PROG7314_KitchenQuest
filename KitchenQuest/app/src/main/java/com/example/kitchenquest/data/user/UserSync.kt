package com.example.kitchenquest.data.user

import com.example.kitchenquest.data.auth.AuthUser
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.feature.onboarding.OnboardingSelection

// Reports whether the server profile had to be created. This lets the app
// distinguish a genuinely new KitchenQuest user from a returning user whose
// local app data was cleared or restored on another installation.
data class UserSyncResult(
    val profile: UserProfileDto,
    val created: Boolean
)

// Existing server profiles are authoritative. Local onboarding choices are
// only used when the authenticated Firebase user does not yet have a backend
// profile. This prevents stale or freshly-reset local data from overwriting a
// returning user's saved preferences.
suspend fun UserRepository.syncSignedInUser(
    user: AuthUser,
    local: OnboardingSelection?
): Result<UserSyncResult> {
    val existingProfile = getProfile()

    existingProfile.getOrNull()?.let { profile ->
        return Result.success(
            UserSyncResult(
                profile = profile,
                created = false
            )
        )
    }

    val lookupFailure = existingProfile.exceptionOrNull()

    if (
        lookupFailure !is ApiException ||
        lookupFailure.type != ApiErrorType.NOT_FOUND
    ) {
        return Result.failure(
            lookupFailure
                ?: IllegalStateException(
                    "Unable to determine whether the user profile exists."
                )
        )
    }

    return syncUser(
        displayName = user.displayName?.takeIf { it.isNotBlank() },
        dietaryPreferences = local?.dietaryPreferences?.toList(),
        avoidedIngredients = local?.avoidedIngredients?.toList()
    ).map { profile ->
        UserSyncResult(
            profile = profile,
            created = true
        )
    }
}

fun UserProfileDto.toOnboardingSelection(): OnboardingSelection {
    val dietary = dietaryPreferences
        .toSet()
        .ifEmpty {
            setOf("No restrictions")
        }

    return OnboardingSelection(
        dietaryPreferences = dietary,
        avoidedIngredients = avoidedIngredients.toSet()
    )
}