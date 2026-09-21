package com.example.kitchenquest.feature.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientEditorScreen(
    initialName: String = "",
    initialQuantity: String = "",
    initialUnit: String = "",
    initialCategory: String = "",
    initialExpiryDate: String? = null,
    isEditing: Boolean = false,
    isSaving: Boolean = false,
    knownIngredients: List<PantryItemDto> = emptyList(),
    onSave: (name: String, quantity: Double, unit: String, category: String, expiryDate: String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var quantity by remember { mutableStateOf(initialQuantity) }
    var unit by remember { mutableStateOf(initialUnit.ifBlank { IngredientOptions.units.first() }) }
    var ingredientType by remember { mutableStateOf(initialCategory.ifBlank { IngredientOptions.ingredientTypes.first() }) }
    var expiryDate by remember { mutableStateOf(initialExpiryDate ?: "") }
    var errors by remember { mutableStateOf(IngredientFormErrors()) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var nameMenuExpanded by remember { mutableStateOf(false) }

    // Suggests from ingredients already in the user's kitchen; picking one fills every field below.
    val suggestions = remember(name, knownIngredients) {
        if (name.isBlank()) {
            emptyList()
        } else {
            knownIngredients
                .distinctBy { it.ingredientName.lowercase() }
                .filter { it.ingredientName.contains(name, ignoreCase = true) }
                .take(5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KitchenQuestDimens.ScreenPadding)
    ) {
        Text(
            text = if (isEditing) "Edit ingredient" else "Add ingredient",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        ExposedDropdownMenuBox(
            expanded = nameMenuExpanded && suggestions.isNotEmpty(),
            onExpandedChange = { nameMenuExpanded = it }
        ) {
            KitchenQuestTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameMenuExpanded = true
                },
                label = "Ingredient name",
                isError = errors.name != null,
                supportingText = errors.name
                    ?: "Start typing to match a known ingredient, or keep your own name",
                trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = nameMenuExpanded && suggestions.isNotEmpty(),
                onDismissRequest = { nameMenuExpanded = false }
            ) {
                suggestions.forEach { match ->
                    DropdownMenuItem(
                        text = { Text(match.ingredientName) },
                        onClick = {
                            name = match.ingredientName
                            quantity = match.quantity.toString()
                            unit = match.unit
                            ingredientType = match.category
                            expiryDate = match.expiryDate ?: ""
                            nameMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

        Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)) {
            KitchenQuestTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = "Quantity",
                keyboardType = KeyboardType.Decimal,
                isError = errors.quantity != null,
                supportingText = errors.quantity,
                modifier = Modifier.weight(1f)
            )

            ExposedDropdownMenuBox(
                expanded = unitMenuExpanded,
                onExpandedChange = { unitMenuExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false }
                ) {
                    IngredientOptions.units.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                unit = option
                                unitMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

        Text(text = "Ingredient type", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
        Row {
            IngredientOptions.ingredientTypes.forEach { option ->
                KitchenQuestChoiceChip(
                    text = option,
                    selected = ingredientType == option,
                    onClick = { ingredientType = option }
                )
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

        KitchenQuestTextField(
            value = expiryDate,
            onValueChange = { expiryDate = it },
            label = "Expiry date (YYYY-MM-DD)",
            isError = errors.expiryDate != null,
            supportingText = errors.expiryDate
        )

        Spacer(modifier = Modifier.weight(1f))

        if (isEditing && onDelete != null) {
            KitchenQuestSecondaryButton(
                text = "Mark finished",
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))
        }

        KitchenQuestPrimaryButton(
            text = if (isEditing) "Save changes" else "Add to kitchen",
            enabled = !isSaving,
            onClick = {
                val trimmedExpiry = expiryDate.trim().ifBlank { null }
                val validation = validateIngredientForm(name, quantity, trimmedExpiry)
                errors = validation

                if (!validation.hasErrors) {
                    onSave(name.trim(), quantity.toDouble(), unit, ingredientType, trimmedExpiry)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}