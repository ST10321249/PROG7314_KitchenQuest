package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.AppRepositories
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.favourites.FavouriteRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.recipes.isConnectionProblem
import com.example.kitchenquest.data.shopping.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    // The app-wide repositories keep an offline copy of each recipe that is opened.
    private val recipeRepository: RecipeRepository = AppRepositories.recipes,
    private val pantryRepository: PantryRepository = AppRepositories.pantry,
    private val shoppingRepository: ShoppingRepository = AppRepositories.shopping,
    private val favouriteRepository: FavouriteRepository = AppRepositories.favourites
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    fun load(recipeId: String) {
        _uiState.update {
            it.copy(isLoading = true, errorMessage = null, isOfflineWithoutCopy = false)
        }

        viewModelScope.launch {
            recipeRepository.getRecipeWithSource(recipeId).fold(
                onSuccess = { loaded ->
                    val recipe = loaded.recipe
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recipe = recipe,
                            servings = recipe.servings?.takeIf { s -> s > 0 } ?: 1,
                            isOfflineCopy = loaded.isOfflineCopy
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = failure.message,
                            // No connection and no saved copy: the screen explains this
                            // instead of showing a general error.
                            isOfflineWithoutCopy = failure.isConnectionProblem()
                        )
                    }
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
            favouriteRepository.getFavouritesWithSource().onSuccess { loaded ->
                _uiState.update {
                    it.copy(isFavourite = loaded.favourites.any { fav -> fav.recipeSourceId == recipeId })
                }
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