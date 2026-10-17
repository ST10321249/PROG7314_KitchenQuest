package com.example.kitchenquest.feature.home

import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto

data class HomeUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val displayName: String = "",
    val pantryCount: Int = 0,
    val shoppingCount: Int = 0,
    val expiringSoon: List<PantryItemDto> = emptyList(),
    val recommendations: List<RecipeRecommendationDto> = emptyList(),
    val errorMessage: String? = null
)
