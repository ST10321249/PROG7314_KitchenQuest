package com.example.kitchenquest.feature.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun IngredientEditorScreen(
    initialName: String = "",
    initialQuantity: String = "",
    initialUnit: String = "",
    initialCategory: String = "",
    initialExpiryDate: String? = null,
    isEditing: Boolean = false,
    isSaving: Boolean = false,
    onSave: (name: String, quantity: Double, unit: String, category: String, expiryDate: String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var quantity by remember { mutableStateOf(initialQuantity) }
    var unit by remember { mutableStateOf(initialUnit) }
    var category by remember { mutableStateOf(initialCategory) }
    var expiryDate by remember { mutableStateOf(initialExpiryDate ?: "") }
    var errors by remember { mutableStateOf(IngredientFormErrors()) }

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

        KitchenQuestTextField(
            value = name,
            onValueChange = { name = it },
            label = "Ingredient name",
            isError = errors.name != null,
            supportingText = errors.name
        )

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
            KitchenQuestTextField(
                value = unit,
                onValueChange = { unit = it },
                label = "Unit",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

        KitchenQuestTextField(
            value = category,
            onValueChange = { category = it },
            label = "Category"
        )

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
                    onSave(name.trim(), quantity.toDouble(), unit.trim(), category.trim(), trimmedExpiry)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}