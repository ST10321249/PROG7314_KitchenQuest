package com.example.kitchenquest.feature.pantry

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.kitchenquest.R

/** UI-only translation of existing form validation results. */
@Composable
fun localizedIngredientError(error: String?): String? = when (error) {
    null -> null
    "Ingredient name is required" -> stringResource(R.string.validation_name_required)
    "Quantity is required" -> stringResource(R.string.validation_quantity_required)
    "Enter a valid number" -> stringResource(R.string.validation_number_invalid)
    "Quantity must be greater than zero" -> stringResource(R.string.validation_quantity_positive)
    "Expiry date can't be in the past" -> stringResource(R.string.validation_expiry_past)
    "Invalid date" -> stringResource(R.string.validation_date_invalid)
    else -> error
}
