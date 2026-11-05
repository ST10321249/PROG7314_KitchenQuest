package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// This utility preserves its stable English output for existing reports/tests.
// CookingHistoryScreen renders localized month headings for the selected app language.
private val MONTH_LABEL_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

// Backend already returns entries sorted newest-first (cookedAt: -1), so grouping
// by insertion order keeps that ordering without needing to re-sort here.
fun groupHistoryByMonth(entries: List<CookingHistoryDto>): List<Pair<String, List<CookingHistoryDto>>> {
    val groups = LinkedHashMap<String, MutableList<CookingHistoryDto>>()

    entries.forEach { entry ->
        val label = monthLabel(entry.cookedAt)
        groups.getOrPut(label) { mutableListOf() }.add(entry)
    }

    return groups.map { (label, items) -> label to items }
}

private fun monthLabel(cookedAt: String): String {
    val parsed = runCatching { Instant.parse(cookedAt) }.getOrNull()
        ?: return "Undated"

    return parsed.atZone(ZoneOffset.UTC).toLocalDate().format(MONTH_LABEL_FORMATTER)
}
