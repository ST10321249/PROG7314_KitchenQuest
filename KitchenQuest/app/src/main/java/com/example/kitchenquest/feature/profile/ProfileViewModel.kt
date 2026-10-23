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

    fun load(force: Boolean = false) {
        if (_uiState.value.isLoading || (_uiState.value.hasLoaded && !force)) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val profileResult = userRepository.getProfile()
            val historyResult = historyRepository.getHistory()
            val favouritesResult = favouriteRepository.getFavourites()
            val pantryResult = pantryRepository.getPantryItems()

            val history = historyResult.getOrDefault(emptyList())
            val pantry = pantryResult.getOrDefault(emptyList())
            val ratings = history.mapNotNull { it.rating }
            val error = listOf(
                profileResult.exceptionOrNull(),
                historyResult.exceptionOrNull(),
                favouritesResult.exceptionOrNull(),
                pantryResult.exceptionOrNull()
            ).firstOrNull { it != null }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasLoaded = true,
                    displayName = profileResult.getOrNull()?.displayName.orEmpty(),
                    email = profileResult.getOrNull()?.email.orEmpty(),
                    mealsCooked = history.size,
                    savedRecipes = favouritesResult.getOrDefault(emptyList()).size,
                    expiringSoon = pantry.count { item ->
                        val days = daysUntilExpiry(item)
                        days != null && days in 0L..3L
                    },
                    averageRating = ratings.takeIf { list -> list.isNotEmpty() }?.average(),
                    errorMessage = error?.message
                )
            }
        }
    }

    fun refresh() = load(force = true)
}
