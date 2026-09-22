package com.example.kitchenquest.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.favourites.DefaultFavouriteRepository
import com.example.kitchenquest.data.favourites.FavouriteRepository
import com.example.kitchenquest.data.history.DefaultHistoryRepository
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.pantry.DefaultPantryRepository
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.user.DefaultUserRepository
import com.example.kitchenquest.data.user.UserRepository
import com.example.kitchenquest.feature.pantry.daysUntilExpiry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = DefaultUserRepository(),
    private val historyRepository: HistoryRepository = DefaultHistoryRepository(),
    private val favouriteRepository: FavouriteRepository = DefaultFavouriteRepository(),
    private val pantryRepository: PantryRepository = DefaultPantryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load() {
        if (_uiState.value.isLoading) {
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            userRepository.getProfile().onSuccess { profile ->
                _uiState.update { it.copy(displayName = profile.displayName, email = profile.email) }
            }

            historyRepository.getHistory().onSuccess { history ->
                _uiState.update { it.copy(mealsCooked = history.size) }
            }

            favouriteRepository.getFavourites().onSuccess { favourites ->
                _uiState.update { it.copy(savedRecipesCount = favourites.size) }
            }

            pantryRepository.getPantryItems().fold(
                onSuccess = { items ->
                    val expiringSoon = items.count { item ->
                        val days = daysUntilExpiry(item)
                        days != null && days <= 3
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            pantryItemCount = items.size,
                            expiringSoonCount = expiringSoon
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(isLoading = false, hasLoaded = true, errorMessage = failure.message)
                    }
                }
            )
        }
    }
}
