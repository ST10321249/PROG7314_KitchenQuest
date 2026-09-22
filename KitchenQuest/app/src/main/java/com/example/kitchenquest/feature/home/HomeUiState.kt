package com.example.kitchenquest.feature.home

import com.example.kitchenquest.data.recipes.RecipeRecommendationDto

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val displayName: String = "",
    val pantryItemCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val recommendationsLoading: Boolean = false,
    val recommendations: List<RecipeRecommendationDto> = emptyList(),
    val errorMessage: String? = null
)
