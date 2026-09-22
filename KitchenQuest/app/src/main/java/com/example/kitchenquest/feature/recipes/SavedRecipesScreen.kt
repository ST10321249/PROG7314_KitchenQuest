package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun SavedRecipesScreen(
    state: SavedRecipesUiState,
    onRecipeClick: (FavouriteRecipeDto) -> Unit,
    onRemove: (FavouriteRecipeDto) -> Unit,
    onRetry: () -> Unit
) {
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

            state.favourites.isEmpty() -> {
                Text(
                    text = "No saved recipes yet. Tap the heart on a recipe to save it here.",
                    modifier = Modifier.align(Alignment.Center).padding(KitchenQuestDimens.ScreenPadding),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Saved recipes",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = KitchenQuestDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                    ) {
                        items(state.favourites, key = { it.recipeSourceId }) { favourite ->
                            Surface(
                                onClick = { onRecipeClick(favourite) },
                                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(KitchenQuestDimens.MediumSpacing),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = favourite.recipeTitle, style = MaterialTheme.typography.bodyLarge)

                                    IconButton(onClick = { onRemove(favourite) }) {
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
}