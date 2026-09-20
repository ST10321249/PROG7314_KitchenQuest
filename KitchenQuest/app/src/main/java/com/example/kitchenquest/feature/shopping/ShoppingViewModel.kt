package com.example.kitchenquest.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.shopping.DefaultShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.shopping.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val isLoading: Boolean = false,
    val items: List<ShoppingItemDto> = emptyList(),
    val errorMessage: String? = null
)

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository = DefaultShoppingRepository(),
    private val pantryRepository: PantryRepository = DefaultPantryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    fun loadList() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            shoppingRepository.getShoppingItems().fold(
                onSuccess = { items -> _uiState.update { it.copy(isLoading = false, items = items) } },
                onFailure = { failure -> _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) } }
            )
        }
    }

    fun addItem(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            shoppingRepository.addShoppingItem(name.trim(), null, null).onSuccess { item ->
                _uiState.update { it.copy(items = it.items + item) }
            }
        }
    }

    // Ticking an item transfers it into the pantry, per the connected pantry/shopping workflow.
    fun togglePurchased(item: ShoppingItemDto) {
        viewModelScope.launch {
            shoppingRepository.setPurchased(item.id, !item.isPurchased).onSuccess { updated ->
                _uiState.update { state -> state.copy(items = state.items.map { if (it.id == updated.id) updated else it }) }

                if (updated.isPurchased) {
                    pantryRepository.addPantryItem(
                        ingredientName = updated.ingredientName,
                        quantity = updated.quantity ?: 1.0,
                        unit = updated.unit.ifBlank { "unit" },
                        category = "Uncategorised",
                        expiryDate = null
                    ).onSuccess {
                        shoppingRepository.deleteShoppingItem(updated.id).onSuccess {
                            _uiState.update { state -> state.copy(items = state.items.filterNot { it.id == updated.id }) }
                        }
                    }
                }
            }
        }
    }

    fun removeItem(id: String) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingItem(id).onSuccess {
                _uiState.update { it.copy(items = it.items.filterNot { item -> item.id == id }) }
            }
        }
    }
}