package com.example.kitchenquest.feature.cooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.history.DefaultHistoryRepository
import com.example.kitchenquest.data.history.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CookingHistoryViewModel(
    private val historyRepository: HistoryRepository = DefaultHistoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookingHistoryUiState())
    val uiState: StateFlow<CookingHistoryUiState> = _uiState.asStateFlow()

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            historyRepository.getHistory().fold(
                onSuccess = { entries ->
                    _uiState.update { it.copy(isLoading = false, hasLoaded = true, entries = entries) }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) }
                }
            )
        }
    }
}