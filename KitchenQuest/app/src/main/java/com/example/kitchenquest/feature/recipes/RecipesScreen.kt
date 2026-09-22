package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun RecipesScreen(
    state: RecipesUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
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
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                label = { Text("Search recipes or ingredients") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            Surface(
                onClick = onWhatCanIMake,
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing)) {
                    Text(
                        text = "What Can I Make?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Find recipes using ingredients in My Kitchen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.errorMessage)
                        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))
                        KitchenQuestPrimaryButton(text = "Retry", onClick = onSearch)
                    }
                }

                state.results.isEmpty() -> {
                    Text(
                        text = if (state.searchQuery.isBlank())
                            "Search for a recipe to get started."
                        else
                            "No recipes found for \"${state.searchQuery}\".",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KitchenQuestDimens.MediumSpacing)
        ) {
            Text(text = recipe.title, style = MaterialTheme.typography.bodyLarge)

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
}