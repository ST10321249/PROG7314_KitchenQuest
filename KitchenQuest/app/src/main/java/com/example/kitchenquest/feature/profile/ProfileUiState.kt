package com.example.kitchenquest.feature.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val mealsCooked: Int = 0,
    val savedRecipes: Int = 0,
    val expiringSoon: Int = 0,
    val averageRating: Double? = null,
    val errorMessage: String? = null
)
