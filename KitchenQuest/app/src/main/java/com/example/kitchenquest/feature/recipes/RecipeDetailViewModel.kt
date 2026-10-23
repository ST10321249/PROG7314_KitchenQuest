package com.example.kitchenquest.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.favourites.DefaultFavouriteRepository
import com.example.kitchenquest.data.favourites.FavouriteRepository
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.DefaultRecipeRepository
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.shopping.DefaultShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository = DefaultRecipeRepository(),
    private val pantryRepository: PantryRepository = DefaultPantryRepository(),
    private val shoppingRepository: ShoppingRepository = DefaultShoppingRepository(),
    private val favouriteRepository: FavouriteRepository = DefaultFavouriteRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    fun load(recipeId: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            recipeRepository.getRecipeById(recipeId).fold(
                onSuccess = { recipe ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recipe = recipe,
                            servings = recipe.servings?.takeIf { s -> s > 0 } ?: 1
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) }
                }
            )
        }

        viewModelScope.launch {
            pantryRepository.getPantryItems().onSuccess { items ->
                _uiState.update {
                    it.copy(heldIngredientNames = items.map { item -> item.ingredientName.lowercase() }.toSet())
                }
            }
        }

        viewModelScope.launch {
            favouriteRepository.getFavourites().onSuccess { favourites ->
                _uiState.update { it.copy(isFavourite = favourites.any { fav -> fav.recipeSourceId == recipeId }) }
            }
        }
    }

    fun increaseServings() {
        _uiState.update { it.copy(servings = it.servings + 1) }
    }

    fun decreaseServings() {
        _uiState.update { it.copy(servings = (it.servings - 1).coerceAtLeast(1)) }
    }

    fun toggleFavourite() {
        val recipe = _uiState.value.recipe ?: return
        val currentlyFavourite = _uiState.value.isFavourite

        // Flips immediately so the heart feels responsive; rolls back only if the call fails.
        _uiState.update { it.copy(isFavourite = !currentlyFavourite) }

        viewModelScope.launch {
            val result = if (currentlyFavourite) {
                favouriteRepository.removeFavourite(recipe.recipeSourceId)
            } else {
                favouriteRepository.addFavourite(recipe.recipeSourceId, recipe.title, recipe.imageUrl)
            }

            result.onFailure {
                _uiState.update { state -> state.copy(isFavourite = currentlyFavourite) }
            }
        }
    }

    fun addMissingIngredientsToList() {
        val state = _uiState.value
        val recipe = state.recipe ?: return

        val missing = recipe.ingredients.filter {
            it.name.lowercase() !in state.heldIngredientNames
        }

        viewModelScope.launch {
            missing.forEach { ingredient ->
                shoppingRepository.addShoppingItem(ingredient.name, ingredient.amount, ingredient.unit)
            }
        }
    }
}