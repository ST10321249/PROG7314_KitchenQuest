package com.example.kitchenquest.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.DefaultRecipeRepository
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.user.DefaultUserRepository
import com.example.kitchenquest.data.user.UserRepository
import com.example.kitchenquest.feature.pantry.daysUntilExpiry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val userRepository: UserRepository = DefaultUserRepository(),
    private val pantryRepository: PantryRepository = DefaultPantryRepository(),
    private val recipeRepository: RecipeRepository = DefaultRecipeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load() {
        if (_uiState.value.isLoading) {
            return
        }

        _uiState.update {
            it.copy(isLoading = true, recommendationsLoading = true, errorMessage = null)
        }

        viewModelScope.launch {
            userRepository.getProfile().fold(
                onSuccess = { profile ->
                    _uiState.update { it.copy(displayName = profile.displayName) }
                },
                onFailure = { /* the top bar just shows a generic greeting without a name */ }
            )

            pantryRepository.getPantryItems().fold(
                onSuccess = { items ->
                    val expiringSoon = items.count { item ->
                        val days = daysUntilExpiry(item)
                        days != null && days <= 3
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            pantryItemCount = items.size,
                            expiringSoonCount = expiringSoon
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, errorMessage = failure.message)
                    }
                }
            )
        }

        viewModelScope.launch {
            recipeRepository.getRecommendations().fold(
                onSuccess = { recommendations ->
                    _uiState.update {
                        it.copy(recommendationsLoading = false, recommendations = recommendations)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(recommendationsLoading = false, recommendations = emptyList()) }
                }
            )
        }
    }
}
