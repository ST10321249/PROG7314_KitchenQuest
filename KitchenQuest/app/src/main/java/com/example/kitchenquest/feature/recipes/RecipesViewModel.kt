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
    private val recipeRepository: RecipeRepository = DefaultRecipeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
        search()
    }

    fun searchCategory(category: String) {
        _uiState.update {
            it.copy(
                searchQuery = category,
                selectedFilter = "All"
            )
        }
        search()
    }

    fun search() {
        val state = _uiState.value

        val query = when {
            state.searchQuery.isNotBlank() -> state.searchQuery.trim()
            state.selectedFilter == "Chicken" -> "chicken"
            else -> null
        }

        val diet = if (state.selectedFilter == "Vegetarian") "vegetarian" else null
        val maxReadyTime = if (state.selectedFilter == "Under 30 min") 30 else null

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            recipeRepository.searchRecipes(
                query = query,
                diet = diet,
                maxReadyTime = maxReadyTime
            ).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            results = results
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
