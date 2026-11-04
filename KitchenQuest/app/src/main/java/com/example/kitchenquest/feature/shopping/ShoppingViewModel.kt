package com.example.kitchenquest.feature.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.shopping.ShoppingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShoppingUiState(
    // A refresh from the server is running.
    val isLoading: Boolean = false,
    val items: List<ShoppingItemDto> = emptyList(),
    val errorMessage: String? = null,
    val message: String? = null
)

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository = AppRepositories.shopping,
    private val pantryRepository: PantryRepository = AppRepositories.pantry
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private var observing: Job? = null

    // True once the repository has delivered the saved list. From then on the list on
    // screen follows the phone's database.
    private var following = false

    fun loadList() {
        follow()
        refresh()
    }

    private fun follow() {
        if (observing?.isActive == true) return
        observing = viewModelScope.launch {
            shoppingRepository.observeShoppingItems().collect { items ->
                following = true
                _uiState.update { it.copy(items = items) }
            }
        }
    }

    private fun refresh() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            shoppingRepository.getShoppingItems().fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(isLoading = false, items = if (following) it.items else items)
                    }
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
                    apply(message = "Added ${item.ingredientName}.") { list -> list + item }
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
                    apply { list -> list.map { if (it.id == updated.id) updated else it } }
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
                        apply(message = "Moved ${item.ingredientName} to My Kitchen.") { list ->
                            list.filterNot { it.id == item.id }
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
                onSuccess = { apply { list -> list.filterNot { it.id == id } } },
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
                apply(message = "Shopping list cleared.") { emptyList() }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    // While the list follows the database the change arrives by itself. A repository
    // with no live list (a test fake) still gets the change patched in.
    private fun apply(
        message: String? = null,
        change: (List<ShoppingItemDto>) -> List<ShoppingItemDto> = { it }
    ) {
        _uiState.update { state ->
            state.copy(
                items = if (following) state.items else change(state.items),
                message = message
            )
        }
    }
}