package com.example.kitchenquest.feature.pantry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onBack: () -> Unit,
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
    var showExpiryDatePicker by remember { mutableStateOf(false) }

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = KitchenQuestDimens.ScreenPadding)
    ) {
        KitchenQuestTopBar(
            title = if (isEditing) "Edit ingredient" else "Add ingredient",
            onBack = onBack
        )

        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

        Text("INGREDIENT NAME", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

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
                supportingText = errors.name ?: "Start typing to match a known ingredient, or keep your own name.",
                trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .fillMaxWidth()
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

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("QUANTITY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                KitchenQuestTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = "Quantity",
                    keyboardType = KeyboardType.Decimal,
                    isError = errors.quantity != null,
                    supportingText = errors.quantity,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("UNIT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
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
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text("INGREDIENT TYPE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing),
            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
        ) {
            IngredientOptions.ingredientTypes.forEach { option ->
                KitchenQuestChoiceChip(
                    text = option,
                    selected = ingredientType == option,
                    onClick = { ingredientType = option }
                )
            }
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text("EXPIRY DATE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        Box(modifier = Modifier.fillMaxWidth()) {
            KitchenQuestTextField(
                value = expiryDate,
                onValueChange = {},
                label = if (expiryDate.isBlank()) "Select expiry date" else "Expiry date",
                isError = errors.expiryDate != null,
                supportingText = errors.expiryDate ?: "Tap to choose a date",
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Choose expiry date"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // The text field stays visually consistent with the rest of the form, while
            // this overlay makes the whole field open the date picker instead of asking
            // the user to type an ISO date manually.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showExpiryDatePicker = true }
            )
        }

        if (showExpiryDatePicker) {
            val initialDateMillis = expiryDate
                .takeIf { it.isNotBlank() }
                ?.let { value ->
                    runCatching {
                        LocalDate.parse(value)
                            .atStartOfDay(ZoneOffset.UTC)
                            .toInstant()
                            .toEpochMilli()
                    }.getOrNull()
                }

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialDateMillis
            )

            DatePickerDialog(
                onDismissRequest = { showExpiryDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { selectedMillis ->
                                expiryDate = Instant.ofEpochMilli(selectedMillis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .toString()
                                errors = errors.copy(expiryDate = null)
                            }
                            showExpiryDatePicker = false
                        },
                        enabled = datePickerState.selectedDateMillis != null
                    ) {
                        Text("Use date")
                    }
                },
                dismissButton = {
                    Row {
                        if (expiryDate.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    expiryDate = ""
                                    errors = errors.copy(expiryDate = null)
                                    showExpiryDatePicker = false
                                }
                            ) {
                                Text("Clear")
                            }
                        }

                        TextButton(
                            onClick = { showExpiryDatePicker = false }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(Modifier.height(KitchenQuestDimens.ExtraLargeSpacing))

        Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)) {
            if (isEditing && onDelete != null) {
                KitchenQuestSecondaryButton(
                    text = "Mark finished",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                )
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
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))
    }
}
