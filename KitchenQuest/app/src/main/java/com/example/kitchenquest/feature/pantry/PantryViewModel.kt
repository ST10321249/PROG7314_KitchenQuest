package com.example.kitchenquest.feature.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.sync.SyncEntity
import com.example.kitchenquest.data.sync.SyncFailure
import com.example.kitchenquest.data.sync.SyncOverviewSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PantryViewModel(
    private val pantryRepository: PantryRepository = AppRepositories.pantry,
    private val syncOverview: SyncOverviewSource = AppRepositories.syncOverview
) : ViewModel() {

    private var watchingSync: Job? = null
    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    private var observing: Job? = null

    // True once the repository has delivered the saved list. From then on the list on
    // screen follows the phone's database, so changes appear without being patched in.
    private var following = false

    fun loadPantry() {
        follow()
        watchSync()
        refresh()
    }

    private fun watchSync() {
        if (watchingSync?.isActive == true) return
        watchingSync = viewModelScope.launch {
            syncOverview.observe().collect { overview ->
                _uiState.update { it.copy(sync = overview) }
            }
        }
    }

    // Looks up why the server refused this item so the screen can explain it.
    fun showFailure(item: PantryItemDto) {
        viewModelScope.launch {
            val reason = syncOverview.failureReason(SyncEntity.PANTRY_ITEM, item.id)
            _uiState.update { it.copy(failure = SyncFailure(item.id, item.ingredientName, reason, syncOverview.isFailedTransfer(item.id))) }
        }
    }

    fun dismissFailure() {
        _uiState.update { it.copy(failure = null) }
    }

    fun retryFailedTransfer(itemId: String) {
        viewModelScope.launch {
            val retried = syncOverview.retryFailedTransfer(itemId)
            _uiState.update { state ->
                state.copy(
                    failure = null,
                    errorMessage = if (retried) null else "The transfer could not be retried. Please refresh and try again."
                )
            }
        }
    }

    // Shows what is saved on the phone straight away.
    private fun follow() {
        if (observing?.isActive == true) return
        observing = viewModelScope.launch {
            pantryRepository.observePantryItems().collect { items ->
                following = true
                _uiState.update { it.copy(items = items, hasLoaded = true) }
            }
        }
    }

    // Brings the phone up to date in the background. It never blocks the list.
    private fun refresh() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            pantryRepository.getPantryItems().fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoaded = true,
                            hasRefreshed = true,
                            items = if (following) it.items else items
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(isLoading = false, hasRefreshed = true, errorMessage = failure.message)
                    }
                }
            )
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            pantryRepository.deletePantryItem(id).onSuccess {
                patchList { list -> list.filterNot { it.id == id } }
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
                patchList { list -> list + item }
            }
        }
    }

    fun findItem(id: String): PantryItemDto? =
        _uiState.value.items.firstOrNull { it.id == id }

    fun updateItem(
        id: String,
        name: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ) {
        viewModelScope.launch {
            pantryRepository.updatePantryItem(id, name, quantity, unit, category, expiryDate).onSuccess { updated ->
                patchList { list -> list.map { if (it.id == updated.id) updated else it } }
            }
        }
    }

    // While the list follows the database the change arrives by itself. A repository
    // with no live list (a test fake) still gets the change patched in.
    private fun patchList(change: (List<PantryItemDto>) -> List<PantryItemDto>) {
        if (following) return
        _uiState.update { it.copy(items = change(it.items)) }
    }
}