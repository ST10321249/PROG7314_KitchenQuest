package com.example.kitchenquest.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.user.DefaultUserRepository
import com.example.kitchenquest.data.user.UserProfileDto
import com.example.kitchenquest.data.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userRepository: UserRepository =
        DefaultUserRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> =
        _uiState.asStateFlow()

    private var savedProfile: UserProfileDto? = null

    fun loadProfile() {
        // The screen asks again after rotation; reloading would discard unsaved edits.
        if (_uiState.value.isLoading || _uiState.value.hasLoaded) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            var result = userRepository.getProfile()

            // The profile is created on sign-in; if that never happened, create it now.
            val error = result.exceptionOrNull()
            if (error is ApiException && error.type == ApiErrorType.NOT_FOUND) {
                result = userRepository.syncUser()
            }

            result.fold(
                onSuccess = { profile ->
                    savedProfile = profile
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            displayName = profile.displayName,
                            email = profile.email,
                            dietaryPreferences = profile.dietaryPreferences.toSet(),
                            avoidedIngredients = profile.avoidedIngredients.toSet(),
                            hasUnsavedChanges = false
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = failure.message
                        )
                    }
                }
            )
        }
    }

    fun onDisplayNameChange(name: String) {
        edit { it.copy(displayName = name) }
    }

    fun onDietaryPreferencesChange(preferences: Set<String>) {
        edit { it.copy(dietaryPreferences = preferences) }
    }

    fun onAvoidedIngredientsChange(ingredients: Set<String>) {
        edit { it.copy(avoidedIngredients = ingredients) }
    }

    fun save() {
        val state = _uiState.value

        if (state.isSaving || !state.hasUnsavedChanges) {
            return
        }

        val name = state.displayName.trim()

        if (name.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Display name can't be empty.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessage = null,
                saveSucceeded = false
            )
        }

        viewModelScope.launch {
            userRepository.updateProfile(
                displayName = name,
                dietaryPreferences = state.dietaryPreferences.toList(),
                avoidedIngredients = state.avoidedIngredients.toList()
            ).fold(
                onSuccess = { profile ->
                    savedProfile = profile
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            displayName = profile.displayName,
                            dietaryPreferences = profile.dietaryPreferences.toSet(),
                            avoidedIngredients = profile.avoidedIngredients.toSet(),
                            hasUnsavedChanges = false,
                            saveSucceeded = true
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = failure.message
                        )
                    }
                }
            )
        }
    }

    fun clearFeedback() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                saveSucceeded = false
            )
        }
    }

    private fun edit(change: (SettingsUiState) -> SettingsUiState) {
        _uiState.update { current ->
            val updated = change(current).copy(
                errorMessage = null,
                saveSucceeded = false
            )

            updated.copy(hasUnsavedChanges = differsFromSaved(updated))
        }
    }

    private fun differsFromSaved(state: SettingsUiState): Boolean {
        val saved = savedProfile ?: return false

        return state.displayName != saved.displayName ||
                state.dietaryPreferences != saved.dietaryPreferences.toSet() ||
                state.avoidedIngredients != saved.avoidedIngredients.toSet()
    }
}
