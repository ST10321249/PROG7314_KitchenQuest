package com.example.kitchenquest.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.recipes.DefaultRecipeRepository
import com.example.kitchenquest.data.recipes.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WhatCanIMakeViewModel(
    private val recipeRepository: RecipeRepository = DefaultRecipeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatCanIMakeUiState())
    val uiState: StateFlow<WhatCanIMakeUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            recipeRepository.getRecommendations().fold(
                onSuccess = { recommendations ->
                    _uiState.update { it.copy(isLoading = false, hasLoaded = true, recommendations = recommendations) }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) }
                }
            )
        }
    }
}