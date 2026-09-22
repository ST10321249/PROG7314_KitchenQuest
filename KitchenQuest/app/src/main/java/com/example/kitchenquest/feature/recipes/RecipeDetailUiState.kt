package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.recipes.RecipeDetailDto

data class RecipeDetailUiState(
    val isLoading: Boolean = false,
    val recipe: RecipeDetailDto? = null,
    val servings: Int = 1,
    val heldIngredientNames: Set<String> = emptySet(),
    val isFavourite: Boolean = false,
    val errorMessage: String? = null
)