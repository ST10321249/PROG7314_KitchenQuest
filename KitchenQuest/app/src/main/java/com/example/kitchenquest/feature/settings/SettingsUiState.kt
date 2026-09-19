package com.example.kitchenquest.feature.settings

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasLoaded: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val dietaryPreferences: Set<String> = emptySet(),
    val avoidedIngredients: Set<String> = emptySet(),
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null,
    val saveSucceeded: Boolean = false
)
