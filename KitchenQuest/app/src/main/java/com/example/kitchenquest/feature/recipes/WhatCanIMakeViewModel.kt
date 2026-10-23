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
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                actionMessage = null
            )
        }

        viewModelScope.launch {
            pantryRepository.getPantryItems().fold(
                onSuccess = { items ->
                    val ingredients = items
                        .map { it.ingredientName }
                        .distinctBy { it.lowercase() }

                    _uiState.update {
                        it.copy(
                            pantryIngredients = ingredients,
                            selectedIngredients = ingredients.toSet()
                        )
                    }

                    loadRecommendations(ingredients)
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            errorMessage = failure.message
                        )
                    }
                }
            )
        }
    }

    fun toggleIngredient(name: String) {
        val selected = _uiState.value.selectedIngredients.toMutableSet()
        if (!selected.add(name)) selected.remove(name)

        _uiState.update {
            it.copy(
                selectedIngredients = selected,
                actionMessage = null
            )
        }

        if (selected.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasLoaded = true,
                    recommendations = emptyList()
                )
            }
        } else {
            loadRecommendations(selected.toList())
        }
    }

    fun resetIngredients() {
        val ingredients = _uiState.value.pantryIngredients
        _uiState.update { it.copy(selectedIngredients = ingredients.toSet()) }
        loadRecommendations(ingredients)
    }

    fun addMissingToShoppingList(recommendation: RecipeRecommendationDto) {
        if (recommendation.missingIngredients.isEmpty()) return

        viewModelScope.launch {
            var added = 0
            recommendation.missingIngredients.distinct().forEach { ingredient ->
                shoppingRepository
                    .addShoppingItem(ingredient, null, null)
                    .onSuccess { added += 1 }
            }

            _uiState.update {
                it.copy(
                    actionMessage = if (added > 0) {
                        "Added $added missing ingredient${if (added == 1) "" else "s"} to your shopping list."
                    } else {
                        "We couldn't add those ingredients right now."
                    }
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    private fun loadRecommendations(ingredients: List<String>) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            recipeRepository.getRecommendations(ingredients).fold(
                onSuccess = { recommendations ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            recommendations = recommendations
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            errorMessage = failure.message
                        )
                    }
                }
            )
        }
    }
}
