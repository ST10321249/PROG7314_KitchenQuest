package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.recipes.RecipeSearchResultDto

data class RecipesUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedDiet: String? = null,
    val selectedCuisine: String? = null,
    val selectedMaxReadyTime: Int? = null,
    val results: List<RecipeSearchResultDto> = emptyList(),
    val hasSearched: Boolean = false,
    val errorMessage: String? = null
)
