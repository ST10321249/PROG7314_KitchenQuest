package com.example.kitchenquest.notifications

import com.example.kitchenquest.data.pantry.PantryItemDto
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

object ExpiryAlertRules {

    // Matches the "expiring within three days" rule used on My Kitchen.
    const val WINDOW_DAYS = 3L

    const val ALERT_HOUR = 8

    // Items expiring today up to WINDOW_DAYS from now, soonest first. Items that have already
    // expired are left out so the same old item does not trigger an alert every day.
    fun expiringSoon(items: List<PantryItemDto>, today: LocalDate): List<PantryItemDto> =
        items
            .mapNotNull { item ->
                daysLeft(item, today)
                    ?.takeIf { it in 0..WINDOW_DAYS }
                    ?.let { item to it }
            }
            .sortedWith(compareBy({ it.second }, { it.first.ingredientName.lowercase() }))
            .map { it.first }

    fun minutesUntilNextAlert(now: LocalDateTime): Long {
        var next = now.toLocalDate().atTime(ALERT_HOUR, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMinutes()
    }

    private fun daysLeft(item: PantryItemDto, today: LocalDate): Long? {
        val date = item.expiryDate ?: return null
        return try {
            ChronoUnit.DAYS.between(today, LocalDate.parse(date))
        } catch (error: DateTimeParseException) {
            null
        }
    }
}