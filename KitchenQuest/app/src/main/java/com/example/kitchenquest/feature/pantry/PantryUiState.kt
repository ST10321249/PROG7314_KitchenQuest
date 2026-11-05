package com.example.kitchenquest.feature.pantry

import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.sync.SyncFailure
import com.example.kitchenquest.data.sync.SyncOverview
import java.time.LocalDate

data class PantryUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val hasRefreshed: Boolean = false,
    val items: List<PantryItemDto> = emptyList(),
    val errorMessage: String? = null,
    // Online or offline, and how many changes are waiting to be sent.
    val sync: SyncOverview = SyncOverview(),
    // A failed item the user tapped, with the reason the server gave.
    val failure: SyncFailure? = null

) {
    val showLoading: Boolean
        get() = items.isEmpty() && !hasRefreshed && errorMessage == null

    val expiringSoon: List<PantryItemDto>
        get() = items.filter { daysUntilExpiry(it) != null && daysUntilExpiry(it)!! <= 3 }
            .sortedBy { daysUntilExpiry(it) }

    val everythingElse: List<PantryItemDto>
        get() = items - expiringSoon.toSet()
}


fun daysUntilExpiry(item: PantryItemDto): Long? {
    val expiryDate = item.expiryDate ?: return null
    return try {
        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(expiryDate))
    } catch (error: Exception) {
        null
    }
}