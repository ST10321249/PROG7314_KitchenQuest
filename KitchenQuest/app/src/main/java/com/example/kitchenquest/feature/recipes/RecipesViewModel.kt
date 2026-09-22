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

    // Tapping a selected chip again clears that filter.
    fun onDietSelected(diet: String) {
        _uiState.update {
            it.copy(selectedDiet = if (it.selectedDiet == diet) null else diet)
        }
        search()
    }

    fun onCuisineSelected(cuisine: String) {
        _uiState.update {
            it.copy(selectedCuisine = if (it.selectedCuisine == cuisine) null else cuisine)
        }
        search()
    }

    fun onMaxReadyTimeSelected(minutes: Int) {
        _uiState.update {
            it.copy(selectedMaxReadyTime = if (it.selectedMaxReadyTime == minutes) null else minutes)
        }
        search()
    }

    fun search() {
        val state = _uiState.value

        // A filter alone (no text) is a valid search - e.g. "show me vegetarian recipes".
        val hasAnyCriteria = state.searchQuery.isNotBlank() ||
                state.selectedDiet != null ||
                state.selectedCuisine != null ||
                state.selectedMaxReadyTime != null

        if (!hasAnyCriteria) {
            // The last active filter/query was just cleared - drop stale results
            // rather than leaving them on screen with nothing selected.
            _uiState.update {
                it.copy(isLoading = false, hasSearched = false, results = emptyList(), errorMessage = null)
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            recipeRepository.searchRecipes(
                query = state.searchQuery.ifBlank { null },
                diet = state.selectedDiet?.let(RecipeFilterOptions::dietParam),
                cuisine = state.selectedCuisine?.let(RecipeFilterOptions::cuisineParam),
                maxReadyTime = state.selectedMaxReadyTime
            ).fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(isLoading = false, hasSearched = true, results = results) }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(isLoading = false, hasSearched = true, errorMessage = failure.message)
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
