package com.example.kitchenquest.notifications

import com.example.kitchenquest.data.pantry.PantryItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ExpiryAlertRulesTest {

    private val today = LocalDate.of(2026, 11, 3)

    private fun item(name: String, expiry: String?) = PantryItemDto(
        id = name, ingredientName = name, quantity = 1.0, unit = "pcs", category = "Dairy", expiryDate = expiry
    )

    private fun names(vararg items: PantryItemDto) =
        ExpiryAlertRules.expiringSoon(items.toList(), today).map { it.ingredientName }

    @Test
    fun anItemExpiringTomorrowIsIncluded() {
        assertEquals(listOf("Milk"), names(item("Milk", "2026-11-04")))
    }

    @Test
    fun todayAndThreeDaysAwayAreIncluded() {
        assertEquals(listOf("A", "B"), names(item("B", "2026-11-06"), item("A", "2026-11-03")))
    }

    @Test
    fun fourDaysAwayIsLeftOut() {
        assertTrue(names(item("Rice", "2026-11-07")).isEmpty())
    }

    @Test
    fun expiredItemsAndItemsWithNoDateAreLeftOut() {
        assertTrue(names(item("Old", "2026-11-02"), item("Pasta", null)).isEmpty())
    }

    @Test
    fun aDateThatCannotBeReadIsIgnored() {
        assertTrue(names(item("Odd", "tomorrow")).isEmpty())
    }

    @Test
    fun theSoonestExpiryComesFirst() {
        assertEquals(
            listOf("Spinach", "Milk", "Chicken"),
            names(item("Chicken", "2026-11-06"), item("Spinach", "2026-11-03"), item("Milk", "2026-11-05"))
        )
    }

    @Test
    fun alertsAreDueAtEightOClock() {
        assertEquals(60, ExpiryAlertRules.minutesUntilNextAlert(LocalDateTime.of(2026, 11, 3, 7, 0)))
        assertEquals(24 * 60, ExpiryAlertRules.minutesUntilNextAlert(LocalDateTime.of(2026, 11, 3, 8, 0)))
        assertEquals(22 * 60 + 30, ExpiryAlertRules.minutesUntilNextAlert(LocalDateTime.of(2026, 11, 3, 9, 30)))
    }
}