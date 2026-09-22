package com.example.kitchenquest.feature.shopping

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
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
                        Text("Clear", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        )

        Row(
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
        ) {
            KitchenQuestTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                label = "Add an item...",
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    val item = newItemText.trim()
                    if (item.isNotEmpty()) {
                        onAdd(item)
                        newItemText = ""
                    }
                }
            ) {
                Text("Add")
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
                contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
            ) {
                items(state.items, key = { it.id }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(KitchenQuestDimens.SmallSpacing)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isPurchased,
                                    onCheckedChange = { onTogglePurchased(item) }
                                )
                                Text(
                                    text = item.ingredientName,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleSmall,
                                    textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                                )
                                IconButton(onClick = { onRemove(item.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove item")
                                }
                            }

                            if (item.isPurchased) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                                    color = KitchenGreenLight
                                ) {
                                    Row(
                                        modifier = Modifier.padding(KitchenQuestDimens.SmallSpacing),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Purchased",
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = KitchenGreen
                                        )
                                        KitchenQuestPrimaryButton(
                                            text = "Add to My Kitchen",
                                            onClick = { onMoveToPantry(item) }
                                        )
                                    }
                                }
                            }
                        }
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
