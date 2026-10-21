package com.example.kitchenquest.feature.pantry

import androidx.compose.ui.res.stringResource
import com.example.kitchenquest.R
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
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
                    message = stringResource(R.string.home_loading)
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
                            text = stringResource(R.string.kitchen_title),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        KitchenQuestIconButton(
                            icon = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = if (searchActive) stringResource(R.string.kitchen_close_search) else stringResource(R.string.kitchen_search),
                            onClick = {
                                searchActive = !searchActive
                                if (!searchActive) searchQuery = ""
                            }
                        )

                        Spacer(Modifier.size(KitchenQuestDimens.SmallSpacing))

                        KitchenQuestIconButton(
                            icon = Icons.Filled.ShoppingCart,
                            contentDescription = stringResource(R.string.kitchen_shopping),
                            onClick = onShoppingList
                        )
                    }

                    if (searchActive) {
                        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                        KitchenQuestSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = stringResource(R.string.kitchen_search),
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
                            border = BorderStroke(1.dp, KitchenRed.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠", style = MaterialTheme.typography.titleLarge, color = KitchenRed)
                                Text(
                                    text = if (state.expiringSoon.size == 1) stringResource(R.string.kitchen_expiring_one) else stringResource(R.string.kitchen_expiring_count, state.expiringSoon.size),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = KitchenQuestDimens.MediumSpacing),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KitchenRed
                                )
                                Text("›", color = KitchenRed, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    val filteredExpiring = state.expiringSoon.filter(::matches)
                    val filteredElse = state.everythingElse.filter(::matches)
                    val noResults = filteredExpiring.isEmpty() && filteredElse.isEmpty()

                    if (state.items.isEmpty()) {
                        KitchenQuestEmptyState(
                            title = stringResource(R.string.kitchen_empty_title),
                            message = stringResource(R.string.kitchen_empty_help),
                            actionText = stringResource(R.string.kitchen_add),
                            onAction = onAddIngredient,
                            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                        )
                    } else if (noResults) {
                        KitchenQuestEmptyState(
                            title = stringResource(R.string.kitchen_no_results),
                            message = stringResource(R.string.kitchen_try_search),
                            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                start = KitchenQuestDimens.ScreenPadding,
                                end = KitchenQuestDimens.ScreenPadding,
                                bottom = 88.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                        ) {
                            if (filteredExpiring.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.home_expiring),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                item {
                                    IngredientGroup(
                                        items = filteredExpiring,
                                        highlighted = true,
                                        onIngredientClick = onIngredientClick
                                    )
                                }
                                item { Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing)) }
                            }

                            if (filteredElse.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.kitchen_else),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                item {
                                    IngredientGroup(
                                        items = filteredElse,
                                        highlighted = false,
                                        onIngredientClick = onIngredientClick
                                    )
                                }
                            }
                        }
                    }

                    KitchenQuestPrimaryButton(
                        text = stringResource(R.string.kitchen_find),
                        onClick = onFindRecipes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = KitchenQuestDimens.ScreenPadding,
                                end = KitchenQuestDimens.ScreenPadding,
                                bottom = KitchenQuestDimens.SmallSpacing
                            )
                    )
                }

                FloatingActionButton(
                    onClick = onAddIngredient,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = KitchenQuestDimens.ScreenPadding,
                            bottom = 86.dp
                        ),
                    shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.kitchen_add))
                }
            }
        }
    }
}

@Composable
private fun IngredientGroup(
    items: List<PantryItemDto>,
    highlighted: Boolean,
    onIngredientClick: (PantryItemDto) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (highlighted) KitchenRed.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column {
            items.forEachIndexed { index, ingredient ->
                IngredientRow(
                    ingredient = ingredient,
                    highlighted = highlighted,
                    onClick = { onIngredientClick(ingredient) }
                )
                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
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
        days == null -> stringResource(R.string.kitchen_no_expiry)
        days < 0 -> stringResource(R.string.kitchen_expired)
        days == 0L -> stringResource(R.string.kitchen_today)
        days == 1L -> stringResource(R.string.kitchen_tomorrow)
        else -> stringResource(R.string.kitchen_days_left, days)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = KitchenQuestDimens.MediumSpacing,
                vertical = 11.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(ingredientEmoji(ingredient.ingredientName), style = MaterialTheme.typography.titleLarge)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KitchenQuestDimens.MediumSpacing)
            ) {
                Text(
                    text = ingredient.ingredientName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlighted) KitchenRed else KitchenGreen
                )
            }
            Text(
                text = "⋮",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun ingredientEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        "chicken" in lower -> "🍗"
        "milk" in lower -> "🥛"
        "spinach" in lower || "lettuce" in lower -> "🥬"
        "tomato" in lower -> "🍅"
        "egg" in lower -> "🥚"
        "rice" in lower -> "🍚"
        "pasta" in lower -> "🍝"
        "cheese" in lower -> "🧀"
        "onion" in lower -> "🧅"
        "garlic" in lower -> "🧄"
        else -> "🥕"
    }
}
