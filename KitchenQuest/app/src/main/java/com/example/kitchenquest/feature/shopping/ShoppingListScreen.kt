package com.example.kitchenquest.feature.shopping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import androidx.compose.ui.Alignment

@Composable
fun ShoppingListScreen(
    state: ShoppingUiState,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onTogglePurchased: (ShoppingItemDto) -> Unit,
    onRemove: (String) -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {

        KitchenQuestTopBar(
            title = "Shopping List",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KitchenQuestDimens.ScreenPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KitchenQuestTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    label = "Add an item",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(KitchenQuestDimens.SmallSpacing))

                FilledIconButton(onClick = {
                    onAdd(newItemText)
                    newItemText = ""
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add item")
                }
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
                Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        if (state.items.isEmpty()) {
            KitchenQuestEmptyState(
                title = "Your list is empty",
                message = "Add items above, or add missing ingredients from a recipe.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(KitchenQuestDimens.ScreenPadding)
            )
        } else {
            val outstanding = state.items.filterNot { it.isPurchased }
            val purchased = state.items.filter { it.isPurchased }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = KitchenQuestDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
            ) {
                if (outstanding.isNotEmpty()) {
                    item { KitchenQuestSectionTitle(title = "TO BUY") }
                    items(outstanding, key = { it.id }) { shoppingItem ->
                        ShoppingRow(shoppingItem, onTogglePurchased, onRemove)
                    }
                }

                if (purchased.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
                        KitchenQuestSectionTitle(title = "IN YOUR KITCHEN")
                    }
                    items(purchased, key = { it.id }) { shoppingItem ->
                        ShoppingRow(shoppingItem, onTogglePurchased, onRemove)
                    }
                }

                item { Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing)) }
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    shoppingItem: ShoppingItemDto,
    onTogglePurchased: (ShoppingItemDto) -> Unit,
    onRemove: (String) -> Unit
) {
    KitchenQuestCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = shoppingItem.isPurchased,
                    onCheckedChange = { onTogglePurchased(shoppingItem) }
                )
                Text(
                    text = shoppingItem.ingredientName,
                    textDecoration = if (shoppingItem.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (shoppingItem.isPurchased) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            IconButton(onClick = { onRemove(shoppingItem.id) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove item")
            }
        }
    }
}
