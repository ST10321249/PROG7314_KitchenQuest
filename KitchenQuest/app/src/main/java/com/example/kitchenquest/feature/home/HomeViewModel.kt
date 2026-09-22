package com.example.kitchenquest.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.DefaultRecipeRepository
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.shopping.DefaultShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingRepository
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
    private val shoppingRepository: ShoppingRepository = DefaultShoppingRepository(),
    private val recipeRepository: RecipeRepository = DefaultRecipeRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load(force: Boolean = false) {
        if (_uiState.value.isLoading || (_uiState.value.hasLoaded && !force)) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val profileResult = userRepository.getProfile()
            val pantryResult = pantryRepository.getPantryItems()
            val shoppingResult = shoppingRepository.getShoppingItems()
            val recommendationResult = recipeRepository.getRecommendations()

            val pantryItems = pantryResult.getOrDefault(emptyList())
            val expiringSoon = pantryItems
                .filter { item ->
                    val days = daysUntilExpiry(item)
                    days != null && days in 0L..3L
                }
                .sortedBy { daysUntilExpiry(it) }

            val firstFailure = listOf(
                profileResult.exceptionOrNull(),
                pantryResult.exceptionOrNull(),
                shoppingResult.exceptionOrNull(),
                recommendationResult.exceptionOrNull()
            ).firstOrNull { it != null }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasLoaded = true,
                    displayName = profileResult.getOrNull()?.displayName.orEmpty(),
                    pantryCount = pantryItems.size,
                    shoppingCount = shoppingResult.getOrDefault(emptyList()).count { item -> !item.isPurchased },
                    expiringSoon = expiringSoon,
                    recommendations = recommendationResult.getOrDefault(emptyList()),
                    errorMessage = firstFailure?.message
                )
            }
        }
    }

    fun refresh() = load(force = true)
}
