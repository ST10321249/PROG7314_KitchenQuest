package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// The app is English-only for now (see README - Afrikaans is planned, not built), so this
// is pinned to English rather than the device locale, to match the rest of the UI.
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
