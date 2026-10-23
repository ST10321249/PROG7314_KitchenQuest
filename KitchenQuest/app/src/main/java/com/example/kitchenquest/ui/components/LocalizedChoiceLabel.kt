package com.example.kitchenquest.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.kitchenquest.R

/** Localises a displayed option without changing the canonical value stored by the API. */
@Composable
fun localizedChoiceLabel(option: String): String = when (option) {
    "No restrictions" -> stringResource(R.string.choice_none)
    "Vegetarian" -> stringResource(R.string.choice_vegetarian)
    "Vegan" -> stringResource(R.string.choice_vegan)
    "Halal" -> stringResource(R.string.choice_halal)
    "Gluten-free" -> stringResource(R.string.choice_gluten_free)
    "Dairy-free" -> stringResource(R.string.choice_dairy_free)
    "Nuts" -> stringResource(R.string.choice_nuts)
    "Shellfish" -> stringResource(R.string.choice_shellfish)
    "Eggs" -> stringResource(R.string.choice_eggs)
    "Milk" -> stringResource(R.string.choice_milk)
    "Soy" -> stringResource(R.string.choice_soy)
    "Meat" -> stringResource(R.string.choice_meat)
    "Vegetables" -> stringResource(R.string.choice_vegetables)
    "Dairy" -> stringResource(R.string.choice_dairy)
    else -> option // Custom or API-provided values must be preserved until ML Kit is added.
}
