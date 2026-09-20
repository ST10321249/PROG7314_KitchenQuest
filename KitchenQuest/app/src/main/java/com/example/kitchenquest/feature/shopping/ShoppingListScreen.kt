package com.example.kitchenquest.feature.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import androidx.compose.ui.Alignment

@Composable
fun ShoppingListScreen(
    state: ShoppingUiState,
    onAdd: (String) -> Unit,
    onTogglePurchased: (ShoppingItemDto) -> Unit,
    onRemove: (String) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(KitchenQuestDimens.ScreenPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = { newItemText = it },
                label = { Text("Add an item") },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                onAdd(newItemText)
                newItemText = ""
            }) { Text("Add") }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        if (state.errorMessage != null) {
            Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)) {
            items(state.items, key = { it.id }) { shoppingItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = shoppingItem.isPurchased,
                            onCheckedChange = { onTogglePurchased(shoppingItem) }
                        )
                        Text(
                            text = shoppingItem.ingredientName,
                            textDecoration = if (shoppingItem.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    IconButton(onClick = { onRemove(shoppingItem.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove item")
                    }
                }
            }
        }
    }
}