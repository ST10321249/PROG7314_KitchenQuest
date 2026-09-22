package com.example.kitchenquest.feature.profile

data class ProfileUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val mealsCooked: Int = 0,
    val savedRecipesCount: Int = 0,
    val pantryItemCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val errorMessage: String? = null
)
