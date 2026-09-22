package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestSearchField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun SavedRecipesScreen(
    state: SavedRecipesUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRecipeClick: (FavouriteRecipeDto) -> Unit,
    onRemove: (FavouriteRecipeDto) -> Unit,
    onRetry: () -> Unit
) {
    var pendingRemoval by remember { mutableStateOf<FavouriteRecipeDto?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {

        KitchenQuestTopBar(
            title = "Saved recipes",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        if (state.favourites.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KitchenQuestDimens.ScreenPadding)
            ) {
                KitchenQuestSearchField(
                    value = state.searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = "Search saved recipes"
                )
            }
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

                state.favourites.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "No saved recipes yet",
                        message = "Tap the heart on a recipe to save it here.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                state.filteredFavourites.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "No matches",
                        message = "No saved recipes match \"${state.searchQuery}\".",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                    ) {
                        items(state.filteredFavourites, key = { it.recipeSourceId }) { favourite ->
                            KitchenQuestCard(onClick = { onRecipeClick(favourite) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = favourite.recipeTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = { pendingRemoval = favourite }) {
                                        Icon(Icons.Filled.Favorite, contentDescription = "Remove from saved")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val toRemove = pendingRemoval
    if (toRemove != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove saved recipe?") },
            text = { Text("\"${toRemove.recipeTitle}\" will be removed from your saved recipes.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(toRemove)
                    pendingRemoval = null
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
