package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CookingHistoryGroupingTest {

    private fun entry(id: String, cookedAt: String) =
        CookingHistoryDto(id = id, recipeTitle = "Recipe $id", cookedAt = cookedAt)

    @Test
    fun entriesInTheSameMonthAreGroupedTogether() {
        val groups = groupHistoryByMonth(
            listOf(
                entry("1", "2026-09-20T14:30:00.000Z"),
                entry("2", "2026-09-05T09:00:00.000Z")
            )
        )

        assertEquals(1, groups.size)
        assertEquals(listOf("1", "2"), groups.first().second.map { it.id })
    }

    @Test
    fun entriesFromDifferentMonthsGetSeparateGroupsInInputOrder() {
        // Backend already sorts newest-first, so September should stay before August.
        val groups = groupHistoryByMonth(
            listOf(
                entry("1", "2026-09-20T14:30:00.000Z"),
                entry("2", "2026-08-05T09:00:00.000Z")
            )
        )

        assertEquals(2, groups.size)
        assertEquals(listOf("1"), groups[0].second.map { it.id })
        assertEquals(listOf("2"), groups[1].second.map { it.id })
        assertEquals("September 2026", groups[0].first)
        assertEquals("August 2026", groups[1].first)
    }

    @Test
    fun anUnparsableDateFallsBackToAnUndatedGroupInsteadOfCrashing() {
        val groups = groupHistoryByMonth(listOf(entry("1", "not-a-date")))

        assertEquals("Undated", groups.first().first)
        assertEquals(listOf("1"), groups.first().second.map { it.id })
    }

    @Test
    fun anEmptyListProducesNoGroups() {
        assertEquals(emptyList<Pair<String, List<CookingHistoryDto>>>(), groupHistoryByMonth(emptyList()))
    }
}
