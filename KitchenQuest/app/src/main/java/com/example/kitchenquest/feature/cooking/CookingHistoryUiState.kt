package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto

data class CookingHistoryUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val entries: List<CookingHistoryDto> = emptyList(),
    val errorMessage: String? = null
)