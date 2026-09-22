package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.favourites.FavouriteRecipeDto

data class SavedRecipesUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val searchQuery: String = "",
    val favourites: List<FavouriteRecipeDto> = emptyList(),
    val errorMessage: String? = null
) {
    val filteredFavourites: List<FavouriteRecipeDto>
        get() = if (searchQuery.isBlank()) {
            favourites
        } else {
            favourites.filter { it.recipeTitle.contains(searchQuery, ignoreCase = true) }
        }
}
