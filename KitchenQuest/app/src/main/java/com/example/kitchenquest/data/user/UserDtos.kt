package com.example.kitchenquest.data.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val displayName: String = "",
    val email: String = "",
    val dietaryPreferences: List<String> = emptyList(),
    val avoidedIngredients: List<String> = emptyList()
)

// Null fields are left out of the JSON so the API only sees what was set.
@Serializable
data class SyncUserRequest(
    val displayName: String? = null,
    val dietaryPreferences: List<String>? = null,
    val avoidedIngredients: List<String>? = null
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val dietaryPreferences: List<String>? = null,
    val avoidedIngredients: List<String>? = null
)
