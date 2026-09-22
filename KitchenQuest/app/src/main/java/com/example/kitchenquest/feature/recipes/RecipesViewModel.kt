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

class RecipesViewModel(
    private val recipeRepository: RecipeRepository =
        DefaultRecipeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            recipeRepository.searchRecipes(query = query).fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(isLoading = false, results = results) }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}