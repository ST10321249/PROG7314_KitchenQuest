package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.recipes.RecipeRecommendationDto

data class WhatCanIMakeUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val pantryIngredientNames: List<String> = emptyList(),
    val recommendations: List<RecipeRecommendationDto> = emptyList(),
    // recipeSourceIds whose missing ingredients have already been added to the shopping list
    val addedToShoppingList: Set<String> = emptySet(),
    val errorMessage: String? = null
)
