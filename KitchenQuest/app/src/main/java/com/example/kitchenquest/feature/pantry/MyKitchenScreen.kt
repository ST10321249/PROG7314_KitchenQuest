package com.example.kitchenquest.feature.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRed

@Composable
fun MyKitchenScreen(
    state: PantryUiState,
    onAddIngredient: () -> Unit,
    onIngredientClick: (PantryItemDto) -> Unit,
    onFindRecipes: () -> Unit,
    onRetry: () -> Unit
) {
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    fun matches(item: PantryItemDto) =
        searchQuery.isBlank() || item.ingredientName.contains(searchQuery, ignoreCase = true)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && !state.hasLoaded -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.errorMessage != null && !state.hasLoaded -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(KitchenQuestDimens.ScreenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.errorMessage)
                    Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(KitchenQuestDimens.ScreenPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "My Kitchen", style = MaterialTheme.typography.headlineSmall)

                        IconButton(onClick = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = if (searchActive) "Close search" else "Search ingredients"
                            )
                        }
                    }

                    if (searchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search ingredients") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = KitchenQuestDimens.ScreenPadding)
                        )
                        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
                    }

                    val filteredExpiringSoon = state.expiringSoon.filter(::matches)
                    val filteredEverythingElse = state.everythingElse.filter(::matches)
                    val noResults = searchQuery.isNotBlank() &&
                            filteredExpiringSoon.isEmpty() && filteredEverythingElse.isEmpty()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                    ) {
                        if (noResults) {
                            item {
                                Text(
                                    text = "No ingredients match \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (filteredExpiringSoon.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Expiring soon",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            items(filteredExpiringSoon, key = { it.id }) { ingredient ->
                                IngredientRow(ingredient, highlighted = true, onClick = { onIngredientClick(ingredient) })
                            }
                            item { Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing)) }
                        }

                        if (filteredEverythingElse.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Everything else",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            items(filteredEverythingElse, key = { it.id }) { ingredient ->
                                IngredientRow(ingredient, highlighted = false, onClick = { onIngredientClick(ingredient) })
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)) {
                        Button(
                            onClick = onFindRecipes,
                            modifier = Modifier.fillMaxWidth().height(KitchenQuestDimens.ButtonHeight)
                        ) {
                            Text("Find recipes using my ingredients")
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onAddIngredient,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(KitchenQuestDimens.ScreenPadding)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add ingredient")
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(
    ingredient: PantryItemDto,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val days = daysUntilExpiry(ingredient)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = if (highlighted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KitchenQuestDimens.MediumSpacing),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = ingredient.ingredientName, style = MaterialTheme.typography.bodyLarge)

            Text(
                text = when {
                    days == null -> "No expiry set"
                    days < 0 -> "Expired"
                    days == 0L -> "Expires today"
                    days == 1L -> "Expires tomorrow"
                    else -> "$days days left"
                },
                color = if (highlighted) KitchenRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}