package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestSearchField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun SavedRecipesScreen(
    state: SavedRecipesUiState,
    onBack: () -> Unit,
    onBrowseRecipes: () -> Unit,
    onRecipeClick: (FavouriteRecipeDto) -> Unit,
    onRemove: (FavouriteRecipeDto) -> Unit,
    onRetry: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<FavouriteRecipeDto?>(null) }

    val filtered = state.favourites.filter {
        query.isBlank() || it.recipeTitle.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        KitchenQuestTopBar(
            title = "Saved recipes",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding)
        )

        KitchenQuestSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search saved recipes",
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding)
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading && !state.hasLoaded -> {
                    KitchenQuestLoadingState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding),
                        message = "Loading saved recipes..."
                    )
                }

                state.errorMessage != null && !state.hasLoaded -> {
                    KitchenQuestErrorState(
                        message = state.errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                state.favourites.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "No saved recipes yet",
                        message = "Tap the heart on a recipe to keep it here.",
                        actionText = "Browse recipes",
                        onAction = onBrowseRecipes,
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                filtered.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "No matches",
                        message = "No saved recipes match \"$query\".",
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                    ) {
                        items(filtered, key = { it.recipeSourceId }) { favourite ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onRecipeClick(favourite) },
                                shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                                        color = KitchenOrangeLight
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🍲", style = MaterialTheme.typography.headlineSmall)
                                        }
                                    }

                                    Text(
                                        text = favourite.recipeTitle,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = KitchenQuestDimens.MediumSpacing),
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    IconButton(onClick = { pendingRemoval = favourite }) {
                                        Icon(
                                            Icons.Filled.Favorite,
                                            contentDescription = "Remove from saved",
                                            tint = MaterialTheme.colorScheme.primary
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

    pendingRemoval?.let { favourite ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove saved recipe?") },
            text = { Text("${favourite.recipeTitle} will be removed from Saved Recipes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(favourite)
                        pendingRemoval = null
                    }
                ) {
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
