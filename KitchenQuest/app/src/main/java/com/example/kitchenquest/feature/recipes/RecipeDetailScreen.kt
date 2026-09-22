package com.example.kitchenquest.feature.recipes

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.data.recipes.RecipeIngredientDto
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    onBack: () -> Unit,
    onIncreaseServings: () -> Unit,
    onDecreaseServings: () -> Unit,
    onAddMissingToList: () -> Unit,
    onStartCooking: () -> Unit,
    onToggleFavourite: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        KitchenQuestTopBar(
            title = state.recipe?.title ?: "Recipe",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding),
            actions = {
                if (state.recipe != null) {
                    IconButton(onClick = onToggleFavourite) {
                        Icon(
                            imageVector = if (state.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (state.isFavourite) "Remove from saved" else "Save recipe"
                        )
                    }
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.recipe == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.errorMessage != null && state.recipe == null -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding)
                    ) {
                        KitchenQuestErrorState(message = state.errorMessage, onRetry = onRetry)
                    }
                }

                state.recipe != null -> {
                    val recipe = state.recipe
                    val baseServings = recipe.servings?.takeIf { it > 0 } ?: state.servings
                    val scaleFactor = state.servings.toDouble() / baseServings

                    val heldCount = recipe.ingredients.count { it.name.lowercase() in state.heldIngredientNames }
                    val missingCount = recipe.ingredients.size - heldCount

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = KitchenQuestDimens.ScreenPadding
                            )
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)) {
                                recipe.readyInMinutes?.let {
                                    Text(text = "$it min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "You have $heldCount of ${recipe.ingredients.size}" +
                                            if (missingCount > 0) " · $missingCount missing" else "",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Servings", style = MaterialTheme.typography.titleMedium)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onDecreaseServings) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Fewer servings")
                                    }
                                    Text(text = "${state.servings}", style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = onIncreaseServings) {
                                        Icon(Icons.Filled.Add, contentDescription = "More servings")
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = KitchenQuestDimens.ScreenPadding),
                            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                        ) {
                            items(recipe.ingredients, key = { it.name }) { ingredient ->
                                IngredientLine(
                                    ingredient = ingredient,
                                    scaleFactor = scaleFactor,
                                    held = ingredient.name.lowercase() in state.heldIngredientNames
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)) {
                            if (missingCount > 0) {
                                KitchenQuestSecondaryButton(
                                    text = "Add missing to list",
                                    onClick = onAddMissingToList,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))
                            }

                            KitchenQuestPrimaryButton(
                                text = "Start cooking",
                                onClick = onStartCooking,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientLine(
    ingredient: RecipeIngredientDto,
    scaleFactor: Double,
    held: Boolean
) {
    val color = if (held) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val scaledAmount = ingredient.amount * scaleFactor

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (held) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "In My Kitchen",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(KitchenQuestDimens.SmallSpacing))
            }
            Text(text = ingredient.name, color = color)
        }

        Text(text = "${"%.1f".format(scaledAmount)} ${ingredient.unit}", color = color)
    }
}
