package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.favourites.FavouriteRecipeDto

data class SavedRecipesUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val favourites: List<FavouriteRecipeDto> = emptyList(),
    val errorMessage: String? = null
)