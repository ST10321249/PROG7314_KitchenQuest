package com.example.kitchenquest.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.favourites.DefaultFavouriteRepository
import com.example.kitchenquest.data.favourites.FavouriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavedRecipesViewModel(
    private val favouriteRepository: FavouriteRepository = DefaultFavouriteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedRecipesUiState())
    val uiState: StateFlow<SavedRecipesUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            favouriteRepository.getFavourites().fold(
                onSuccess = { favourites ->
                    _uiState.update { it.copy(isLoading = false, hasLoaded = true, favourites = favourites) }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) }
                }
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun removeFavourite(recipeSourceId: String) {
        viewModelScope.launch {
            favouriteRepository.removeFavourite(recipeSourceId).onSuccess {
                _uiState.update { state -> state.copy(favourites = state.favourites.filterNot { it.recipeSourceId == recipeSourceId }) }
            }
        }
    }
}