package com.example.kitchenquest.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.shopping.DefaultShoppingRepository
import com.example.kitchenquest.data.shopping.ShoppingRepository
import com.example.kitchenquest.feature.pantry.daysUntilExpiry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val pantryRepository: PantryRepository = DefaultPantryRepository(),
    private val shoppingRepository: ShoppingRepository = DefaultShoppingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val pantryResult = pantryRepository.getPantryItems()
            val shoppingResult = shoppingRepository.getShoppingItems()

            val pantry = pantryResult.getOrDefault(emptyList())
            val shopping = shoppingResult.getOrDefault(emptyList())
            val error = pantryResult.exceptionOrNull() ?: shoppingResult.exceptionOrNull()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    expiringCount = pantry.count { item ->
                        val days = daysUntilExpiry(item)
                        days != null && days in 0L..3L
                    },
                    shoppingCount = shopping.count { item -> !item.isPurchased },
                    errorMessage = error?.message
                )
            }
        }
    }
}
