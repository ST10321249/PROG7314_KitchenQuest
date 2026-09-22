package com.example.kitchenquest.feature.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestSearchField
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
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    fun matches(item: PantryItemDto): Boolean {
        val matchesQuery = searchQuery.isBlank() || item.ingredientName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || item.category == selectedCategory
        return matchesQuery && matchesCategory
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && !state.hasLoaded -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.errorMessage != null && !state.hasLoaded -> {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(KitchenQuestDimens.ScreenPadding)
                ) {
                    KitchenQuestErrorState(message = state.errorMessage, onRetry = onRetry)
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KitchenQuestDimens.ScreenPadding)
                    ) {
                        if (searchActive) {
                            KitchenQuestSearchField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search ingredients"
                            )
                            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.TinySpacing)) {
                            KitchenQuestChoiceChip(
                                text = "All",
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null }
                            )
                            IngredientOptions.ingredientTypes.forEach { category ->
                                KitchenQuestChoiceChip(
                                    text = category,
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == category) null else category
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                    val filteredExpiringSoon = state.expiringSoon.filter(::matches)
                    val filteredEverythingElse = state.everythingElse.filter(::matches)
                    val noResults = filteredExpiringSoon.isEmpty() && filteredEverythingElse.isEmpty()

                    if (noResults) {
                        KitchenQuestEmptyState(
                            title = if (state.expiringSoon.isEmpty() && state.everythingElse.isEmpty()) {
                                "Your kitchen is empty"
                            } else {
                                "No matches"
                            },
                            message = if (state.expiringSoon.isEmpty() && state.everythingElse.isEmpty()) {
                                "Tap + to add your first ingredient."
                            } else {
                                "Try a different search or category."
                            },
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterHorizontally)
                                .padding(KitchenQuestDimens.ScreenPadding)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                        ) {
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

    KitchenQuestCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
