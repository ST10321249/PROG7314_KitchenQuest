package com.example.kitchenquest.feature.pantry

import java.time.LocalDate

data class IngredientFormErrors(
    val name: String? = null,
    val quantity: String? = null,
    val expiryDate: String? = null
) {
    val hasErrors get() = name != null || quantity != null || expiryDate != null
}

fun validateIngredientForm(
    name: String,
    quantityText: String,
    expiryDate: String?
): IngredientFormErrors {
    val nameError = if (name.isBlank()) "Ingredient name is required" else null

    val quantity = quantityText.toDoubleOrNull()
    val quantityError = when {
        quantityText.isBlank() -> "Quantity is required"
        quantity == null -> "Enter a valid number"
        quantity <= 0 -> "Quantity must be greater than zero"
        else -> null
    }

    val expiryError = if (expiryDate != null) {
        try {
            if (LocalDate.parse(expiryDate).isBefore(LocalDate.now())) "Expiry date can't be in the past" else null
        } catch (error: Exception) {
            "Invalid date"
        }
    } else null

    return IngredientFormErrors(nameError, quantityError, expiryError)
}