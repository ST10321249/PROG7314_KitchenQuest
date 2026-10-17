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
        if (_uiState.value.isLoading) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            historyRepository.getHistory().fold(
                onSuccess = { entries ->
                    val notes = buildMap {
                        entries.forEach { entry ->
                            historyRepository
                                .getNote(entry.recipeSourceId)
                                .getOrNull()
                                ?.note
                                ?.takeIf { it.isNotBlank() }
                                ?.let { note ->
                                    put(entry.recipeSourceId, note)
                                }
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            entries = entries,
                            notesByRecipeId = notes,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = failure.message
                                ?: "Your cooking history couldn't be loaded."
                        )
                    }
                }
            )
        }
    }

    fun selectFilter(filter: CookingHistoryFilter) {
        _uiState.update {
            it.copy(selectedFilter = filter)
        }
    }
}
