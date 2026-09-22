package com.example.kitchenquest.feature.pantry

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestIconButton
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSearchField
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRed
import com.example.kitchenquest.ui.theme.KitchenRedLight

@Composable
fun MyKitchenScreen(
    state: PantryUiState,
    onAddIngredient: () -> Unit,
    onShoppingList: () -> Unit,
    onIngredientClick: (PantryItemDto) -> Unit,
    onFindRecipes: () -> Unit,
    onRetry: () -> Unit
) {
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    fun matches(item: PantryItemDto): Boolean =
        searchQuery.isBlank() || item.ingredientName.contains(searchQuery, ignoreCase = true)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && !state.hasLoaded -> {
                KitchenQuestLoadingState(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(KitchenQuestDimens.ScreenPadding),
                    message = "Loading your kitchen..."
                )
            }

            state.errorMessage != null && !state.hasLoaded -> {
                KitchenQuestErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = KitchenQuestDimens.ScreenPadding,
                                end = KitchenQuestDimens.ScreenPadding,
                                top = KitchenQuestDimens.MediumSpacing
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Kitchen",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium
                        )

                        KitchenQuestIconButton(
                            icon = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searchActive) "Close search" else "Search ingredients",
                            onClick = {
                                searchActive = !searchActive
                                if (!searchActive) searchQuery = ""
                            }
                        )

                        Spacer(Modifier.size(KitchenQuestDimens.SmallSpacing))

                        KitchenQuestIconButton(
                            icon = Icons.Filled.ShoppingCart,
                            contentDescription = "Shopping list",
                            onClick = onShoppingList
                        )
                    }

                    if (searchActive) {
                        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                        KitchenQuestSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search ingredients",
                            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding)
                        )
                    }

                    if (state.expiringSoon.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = KitchenQuestDimens.ScreenPadding,
                                    vertical = KitchenQuestDimens.MediumSpacing
                                ),
                            shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                            color = KitchenRedLight,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠", style = MaterialTheme.typography.titleLarge, color = KitchenRed)
                                Text(
                                    text = "${state.expiringSoon.size} item${if (state.expiringSoon.size == 1) "" else "s"} expiring within 3 days",
                                    modifier = Modifier.padding(start = KitchenQuestDimens.MediumSpacing),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = KitchenRed
                                )
                            }
                        }
                    }

                    val filteredExpiring = state.expiringSoon.filter(::matches)
                    val filteredElse = state.everythingElse.filter(::matches)
                    val noResults = filteredExpiring.isEmpty() && filteredElse.isEmpty()

                    if (state.items.isEmpty()) {
                        KitchenQuestEmptyState(
                            title = "Your kitchen is empty",
                            message = "Add ingredients to start tracking expiry dates and finding recipes.",
                            actionText = "Add ingredient",
                            onAction = onAddIngredient,
                            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                        )
                    } else if (noResults) {
                        KitchenQuestEmptyState(
                            title = "No matching ingredients",
                            message = "Try another search.",
                            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                start = KitchenQuestDimens.ScreenPadding,
                                end = KitchenQuestDimens.ScreenPadding,
                                bottom = 96.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                        ) {
                            if (filteredExpiring.isNotEmpty()) {
                                item {
                                    Text("Expiring soon", style = MaterialTheme.typography.titleMedium)
                                }
                                items(filteredExpiring, key = { it.id }) { ingredient ->
                                    IngredientRow(ingredient, highlighted = true, onClick = { onIngredientClick(ingredient) })
                                }
                                item { Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing)) }
                            }

                            if (filteredElse.isNotEmpty()) {
                                item {
                                    Text("Everything else", style = MaterialTheme.typography.titleMedium)
                                }
                                items(filteredElse, key = { it.id }) { ingredient ->
                                    IngredientRow(ingredient, highlighted = false, onClick = { onIngredientClick(ingredient) })
                                }
                            }
                        }
                    }

                    KitchenQuestPrimaryButton(
                        text = "Find recipes using my ingredients",
                        onClick = onFindRecipes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                FloatingActionButton(
                    onClick = onAddIngredient,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = KitchenQuestDimens.ScreenPadding,
                            bottom = 92.dp
                        ),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
    val status = when {
        days == null -> "No expiry set"
        days < 0 -> "Expired"
        days == 0L -> "Expires today"
        days == 1L -> "Expires tomorrow"
        else -> "$days days left"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🥕", style = MaterialTheme.typography.titleLarge)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KitchenQuestDimens.MediumSpacing)
            ) {
                Text(ingredient.ingredientName, style = MaterialTheme.typography.titleSmall)
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (highlighted) KitchenRed else KitchenGreen
                )
            }
            Text("⋮", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
