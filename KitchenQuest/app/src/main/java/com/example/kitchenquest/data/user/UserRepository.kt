package com.example.kitchenquest.data.user

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall

interface UserRepository {

    suspend fun syncUser(
        displayName: String? = null,
        dietaryPreferences: List<String>? = null,
        avoidedIngredients: List<String>? = null
    ): Result<UserProfileDto>

    suspend fun getProfile(): Result<UserProfileDto>

    suspend fun updateProfile(
        displayName: String? = null,
        dietaryPreferences: List<String>? = null,
        avoidedIngredients: List<String>? = null
    ): Result<UserProfileDto>
}

class DefaultUserRepository(
    private val userApi: UserApi =
        ApiClient.create(UserApi::class.java)
) : UserRepository {

    override suspend fun syncUser(
        displayName: String?,
        dietaryPreferences: List<String>?,
        avoidedIngredients: List<String>?
    ): Result<UserProfileDto> = apiCall {
        userApi.syncUser(
            SyncUserRequest(
                displayName = displayName,
                dietaryPreferences = dietaryPreferences,
                avoidedIngredients = avoidedIngredients
            )
        )
    }

    override suspend fun getProfile(): Result<UserProfileDto> = apiCall {
        userApi.getMe()
    }

    override suspend fun updateProfile(
        displayName: String?,
        dietaryPreferences: List<String>?,
        avoidedIngredients: List<String>?
    ): Result<UserProfileDto> = apiCall {
        userApi.updateMe(
            UpdateProfileRequest(
                displayName = displayName,
                dietaryPreferences = dietaryPreferences,
                avoidedIngredients = avoidedIngredients
            )
        )
    }
}
