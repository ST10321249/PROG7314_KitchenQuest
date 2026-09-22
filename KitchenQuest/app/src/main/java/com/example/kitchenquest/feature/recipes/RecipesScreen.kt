package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestSearchField
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipesScreen(
    state: RecipesUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDietSelected: (String) -> Unit,
    onCuisineSelected: (String) -> Unit,
    onMaxReadyTimeSelected: (Int) -> Unit,
    onWhatCanIMake: () -> Unit,
    onSavedRecipes: () -> Unit,
    onRecipeClick: (RecipeSearchResultDto) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KitchenQuestDimens.ScreenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Recipes", style = MaterialTheme.typography.headlineSmall)

            IconButton(onClick = onSavedRecipes) {
                Icon(Icons.Filled.Favorite, contentDescription = "Saved recipes")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KitchenQuestDimens.ScreenPadding)
        ) {
            KitchenQuestSearchField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                placeholder = "Search recipes or ingredients",
                onSearch = onSearch
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

            KitchenQuestSectionTitle(title = "FILTERS")

            Spacer(modifier = Modifier.height(KitchenQuestDimens.TinySpacing))

            FlowRow {
                RecipeFilterOptions.diets.forEach { diet ->
                    KitchenQuestChoiceChip(
                        text = diet,
                        selected = state.selectedDiet == diet,
                        onClick = { onDietSelected(diet) }
                    )
                }
            }

            FlowRow {
                RecipeFilterOptions.cuisines.forEach { cuisine ->
                    KitchenQuestChoiceChip(
                        text = cuisine,
                        selected = state.selectedCuisine == cuisine,
                        onClick = { onCuisineSelected(cuisine) }
                    )
                }
            }

            FlowRow {
                RecipeFilterOptions.readyTimes.forEach { minutes ->
                    KitchenQuestChoiceChip(
                        text = "Under $minutes min",
                        selected = state.selectedMaxReadyTime == minutes,
                        onClick = { onMaxReadyTimeSelected(minutes) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            KitchenQuestCard(onClick = onWhatCanIMake) {
                Text(
                    text = "What Can I Make?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Find recipes using ingredients in My Kitchen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding)
                    ) {
                        KitchenQuestErrorState(message = state.errorMessage, onRetry = onSearch)
                    }
                }

                !state.hasSearched -> {
                    KitchenQuestEmptyState(
                        title = "Find something to cook",
                        message = "Search by name or ingredient, or tap a filter above.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                state.results.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "No recipes found",
                        message = "Try a different search or fewer filters.",
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
                        items(state.results, key = { it.recipeSourceId }) { recipe ->
                            RecipeResultRow(recipe = recipe, onClick = { onRecipeClick(recipe) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeResultRow(
    recipe: RecipeSearchResultDto,
    onClick: () -> Unit
) {
    KitchenQuestCard(onClick = onClick) {
        Text(
            text = recipe.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)) {
            recipe.readyInMinutes?.let {
                Text(
                    text = "$it min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            recipe.servings?.let {
                Text(
                    text = "$it servings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
