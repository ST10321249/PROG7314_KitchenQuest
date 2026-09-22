package com.example.kitchenquest.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.DefaultRecipeRepository
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.shopping.DefaultShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WhatCanIMakeViewModel(
    private val recipeRepository: RecipeRepository = DefaultRecipeRepository(),
    private val pantryRepository: PantryRepository = DefaultPantryRepository(),
    private val shoppingRepository: ShoppingRepository = DefaultShoppingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatCanIMakeUiState())
    val uiState: StateFlow<WhatCanIMakeUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            pantryRepository.getPantryItems().onSuccess { items ->
                _uiState.update {
                    it.copy(pantryIngredientNames = items.map { item -> item.ingredientName })
                }
            }

            recipeRepository.getRecommendations().fold(
                onSuccess = { recommendations ->
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, recommendations = recommendations)
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, errorMessage = failure.message)
                    }
                }
            )
        }
    }

    fun addMissingToShoppingList(recommendation: RecipeRecommendationDto) {
        if (recommendation.recipeSourceId in _uiState.value.addedToShoppingList) {
            return
        }

        viewModelScope.launch {
            recommendation.missingIngredients.forEach { ingredientName ->
                shoppingRepository.addShoppingItem(ingredientName, null, null)
            }

            _uiState.update {
                it.copy(addedToShoppingList = it.addedToShoppingList + recommendation.recipeSourceId)
            }
        }
    }
}
