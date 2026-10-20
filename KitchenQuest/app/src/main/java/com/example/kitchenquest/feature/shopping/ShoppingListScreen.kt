package com.example.kitchenquest.feature.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenGreenLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun ShoppingListScreen(
    state: ShoppingUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onTogglePurchased: (ShoppingItemDto) -> Unit,
    onMoveToPantry: (ShoppingItemDto) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit
) {
    var newItemText by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        KitchenQuestTopBar(
            title = "Shopping list",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding),
            actions = {
                if (state.items.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KitchenQuestDimens.ScreenPadding),
            shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier.padding(
                    start = KitchenQuestDimens.MediumSpacing,
                    end = KitchenQuestDimens.SmallSpacing,
                    top = 11.dp,
                    bottom = 11.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (newItemText.isBlank()) {
                                Text(
                                    text = "+ Add an item...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                TextButton(
                    onClick = {
                        val item = newItemText.trim()
                        if (item.isNotEmpty()) {
                            onAdd(item)
                            newItemText = ""
                        }
                    },
                    enabled = newItemText.isNotBlank()
                ) {
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (newItemText.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        }
                    )
                }
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(
                    horizontal = KitchenQuestDimens.ScreenPadding,
                    vertical = KitchenQuestDimens.SmallSpacing
                ),
                style = MaterialTheme.typography.labelMedium,
                color = KitchenGreen
            )
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(
                    horizontal = KitchenQuestDimens.ScreenPadding,
                    vertical = KitchenQuestDimens.SmallSpacing
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        if (state.items.isEmpty() && !state.isLoading) {
            KitchenQuestEmptyState(
                title = "Your shopping list is empty",
                message = "Add items manually or send missing recipe ingredients here.",
                modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = KitchenQuestDimens.ScreenPadding,
                    end = KitchenQuestDimens.ScreenPadding,
                    top = KitchenQuestDimens.SmallSpacing,
                    bottom = KitchenQuestDimens.SectionSpacing
                )
            ) {
                itemsIndexed(
                    items = state.items,
                    key = { _, item -> item.id }
                ) { index, item ->
                    ShoppingRow(
                        item = item,
                        onTogglePurchased = { onTogglePurchased(item) },
                        onMoveToPantry = { onMoveToPantry(item) },
                        onRemove = { onRemove(item.id) }
                    )
                    if (index != state.items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 44.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear shopping list?") },
            text = { Text("This will remove every item currently on your shopping list.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItemDto,
    onTogglePurchased: () -> Unit,
    onMoveToPantry: () -> Unit,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = KitchenQuestDimens.TinySpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isPurchased,
                onCheckedChange = { onTogglePurchased() },
                colors = CheckboxDefaults.colors(
                    checkedColor = KitchenGreen,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            Text(
                text = item.ingredientName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                color = if (item.isPurchased) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (item.isPurchased) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 44.dp, bottom = KitchenQuestDimens.SmallSpacing),
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                color = KitchenGreenLight
            ) {
                Row(
                    modifier = Modifier.padding(KitchenQuestDimens.SmallSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                ) {
                    Text(
                        text = "Purchased",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = KitchenGreen
                    )
                    KitchenQuestPrimaryButton(
                        text = "Add to My Kitchen",
                        onClick = onMoveToPantry
                    )
                }
            }
        }
    }
}
