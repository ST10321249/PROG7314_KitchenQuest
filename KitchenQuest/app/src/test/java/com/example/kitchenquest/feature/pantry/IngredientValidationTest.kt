package com.example.kitchenquest.feature.pantry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IngredientValidationTest {

    @Test
    fun blankNameIsRejected() {
        val errors = validateIngredientForm("", "1", null)
        assertEquals("Ingredient name is required", errors.name)
    }

    @Test
    fun nonPositiveQuantityIsRejected() {
        assertEquals("Quantity must be greater than zero", validateIngredientForm("Milk", "0", null).quantity)
        assertEquals("Enter a valid number", validateIngredientForm("Milk", "abc", null).quantity)
    }

    @Test
    fun pastExpiryDateIsRejected() {
        val errors = validateIngredientForm("Milk", "1", "2000-01-01")
        assertEquals("Expiry date can't be in the past", errors.expiryDate)
    }

    @Test
    fun validFormHasNoErrors() {
        val errors = validateIngredientForm("Milk", "2", null)
        assertNull(errors.name)
        assertNull(errors.quantity)
        assertNull(errors.expiryDate)
    }
}