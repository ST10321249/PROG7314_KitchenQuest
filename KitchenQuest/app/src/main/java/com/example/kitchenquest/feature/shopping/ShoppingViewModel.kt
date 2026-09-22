package com.example.kitchenquest.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
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
    val errorMessage: String? = null,
    val message: String? = null
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
                onSuccess = { items ->
                    _uiState.update { it.copy(isLoading = false, items = items) }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) }
                }
            )
        }
    }

    fun addItem(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            shoppingRepository.addShoppingItem(name.trim(), null, null).fold(
                onSuccess = { item ->
                    _uiState.update {
                        it.copy(
                            items = it.items + item,
                            message = "Added ${item.ingredientName}."
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(errorMessage = failure.message) }
                }
            )
        }
    }

    // A checked item remains visible so the user can choose when to move it
    // into the pantry, matching the Part 1 shopping workflow.
    fun togglePurchased(item: ShoppingItemDto) {
        viewModelScope.launch {
            shoppingRepository.setPurchased(item.id, !item.isPurchased).fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(
                            items = state.items.map { if (it.id == updated.id) updated else it },
                            message = null
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(errorMessage = failure.message) }
                }
            )
        }
    }

    fun moveToPantry(item: ShoppingItemDto) {
        viewModelScope.launch {
            pantryRepository.addPantryItem(
                ingredientName = item.ingredientName,
                quantity = item.quantity ?: 1.0,
                unit = item.unit.ifBlank { "unit" },
                category = "Uncategorised",
                expiryDate = null
            ).fold(
                onSuccess = {
                    shoppingRepository.deleteShoppingItem(item.id).onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                items = state.items.filterNot { it.id == item.id },
                                message = "Moved ${item.ingredientName} to My Kitchen."
                            )
                        }
                    }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(errorMessage = failure.message) }
                }
            )
        }
    }

    fun removeItem(id: String) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingItem(id).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(items = state.items.filterNot { item -> item.id == id })
                    }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(errorMessage = failure.message) }
                }
            )
        }
    }

    fun clearList() {
        val ids = _uiState.value.items.map { it.id }
        if (ids.isEmpty()) return

        viewModelScope.launch {
            var failed = false
            ids.forEach { id ->
                shoppingRepository.deleteShoppingItem(id).onFailure { failed = true }
            }

            if (failed) {
                loadList()
                _uiState.update { it.copy(errorMessage = "Some items couldn't be removed.") }
            } else {
                _uiState.update {
                    it.copy(
                        items = emptyList(),
                        message = "Shopping list cleared."
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }
}
