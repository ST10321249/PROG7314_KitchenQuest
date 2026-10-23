package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.recipes.RecipeSearchResultDto

data class RecipesUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val results: List<RecipeSearchResultDto> = emptyList(),
    val errorMessage: String? = null
)
