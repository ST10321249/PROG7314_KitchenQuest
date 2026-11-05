package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto
import java.time.OffsetDateTime
import java.time.YearMonth

enum class CookingHistoryFilter(
    val label: String
) {
    ALL("All"),
    THIS_MONTH("This month"),
    RATED_FIVE("Rated 5★"),
    WITH_NOTES("With notes")
}

data class CookingHistoryUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val entries: List<CookingHistoryDto> = emptyList(),
    val notesByRecipeId: Map<String, String> = emptyMap(),
    val selectedFilter: CookingHistoryFilter = CookingHistoryFilter.ALL,
    val errorMessage: String? = null
) {
    val filteredEntries: List<CookingHistoryDto>
        get() = entries.filter { entry ->
            when (selectedFilter) {
                CookingHistoryFilter.ALL -> true
                CookingHistoryFilter.THIS_MONTH -> entry.cookedAt.isInCurrentMonth()
                CookingHistoryFilter.RATED_FIVE -> entry.rating == 5
                CookingHistoryFilter.WITH_NOTES ->
                    !notesByRecipeId[entry.recipeSourceId].isNullOrBlank()
            }
        }
}

private fun String.isInCurrentMonth(): Boolean {
    val cookedMonth = runCatching {
        YearMonth.from(OffsetDateTime.parse(this))
    }.getOrNull() ?: return false

    return cookedMonth == YearMonth.now()
}
