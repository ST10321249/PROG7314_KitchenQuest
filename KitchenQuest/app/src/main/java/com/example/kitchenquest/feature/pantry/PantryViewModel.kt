package com.example.kitchenquest.feature.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PantryViewModel(
    private val pantryRepository: PantryRepository =
        DefaultPantryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    fun loadPantry() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            pantryRepository.getPantryItems().fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, items = items)
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = failure.message)
                    }
                }
            )
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            pantryRepository.deletePantryItem(id).onSuccess {
                _uiState.update { state ->
                    state.copy(items = state.items.filterNot { it.id == id })
                }
            }
        }
    }

    fun markFinished(id: String) = deleteItem(id)

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun addItem(
        name: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ) {
        viewModelScope.launch {
            pantryRepository.addPantryItem(name, quantity, unit, category, expiryDate).onSuccess { item ->
                _uiState.update { it.copy(items = it.items + item) }
            }
        }
    }
}