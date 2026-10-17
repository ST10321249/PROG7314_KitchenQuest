package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestIconButton
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestSearchField
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

private val recipeFilters = listOf("All", "Under 30 min", "Chicken", "Vegetarian")
private val recipeCategories = listOf(
    "Breakfast" to "🍳",
    "Lunch" to "🥗",
    "Dinner" to "🍲",
    "Healthy" to "🥑"
)

@Composable
fun RecipesScreen(
    state: RecipesUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilterSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onWhatCanIMake: () -> Unit,
    onSavedRecipes: () -> Unit,
    onRecipeClick: (RecipeSearchResultDto) -> Unit
) {
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
                text = "Recipes",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium
            )
            KitchenQuestIconButton(
                icon = Icons.Filled.Favorite,
                contentDescription = "Saved recipes",
                onClick = onSavedRecipes
            )
        }

        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

        Column(
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding)
        ) {
            KitchenQuestSearchField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                placeholder = "Search recipes or ingredients",
                onSearch = onSearch
            )

            Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = onWhatCanIMake,
                shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                color = KitchenGreen
            ) {
                Row(
                    modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "What Can I Make?",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text(
                            text = "Find recipes using ingredients in My Kitchen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = androidx.compose.ui.graphics.Color.White)
                }
            }

            Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                recipeFilters.forEach { filter ->
                    KitchenQuestChoiceChip(
                        text = filter,
                        selected = state.selectedFilter == filter,
                        onClick = { onFilterSelected(filter) }
                    )
                }
            }

            Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

            Text("Categories", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
            ) {
                recipeCategories.forEach { (category, emoji) ->
                    Surface(
                        onClick = { onCategorySelected(category) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                        color = KitchenOrangeLight
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = KitchenQuestDimens.MediumSpacing),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                            Text(category, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

            KitchenQuestSectionTitle(title = "Popular this week")
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading && !state.hasLoaded -> {
                    KitchenQuestLoadingState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding),
                        message = "Finding recipes..."
                    )
                }

                state.errorMessage != null -> {
                    KitchenQuestErrorState(
                        message = state.errorMessage,
                        onRetry = onSearch,
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                state.results.isEmpty() && state.hasLoaded -> {
                    KitchenQuestEmptyState(
                        title = "No recipes found",
                        message = "Try another search or remove a filter.",
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                !state.hasLoaded -> {
                    KitchenQuestEmptyState(
                        title = "Find something to cook",
                        message = "Search for a recipe, choose a category, or browse recipes based on your kitchen.",
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding),
                        actionText = "Browse recipes",
                        onAction = onSearch
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                    ) {
                        items(state.results, key = { it.recipeSourceId }) { recipe ->
                            RecipeResultRow(recipe, onClick = { onRecipeClick(recipe) })
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
        modifier = Modifier.fillMaxWidth(),
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KitchenQuestDimens.MediumSpacing)
            ) {
                Text(recipe.title, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)) {
                    recipe.readyInMinutes?.let {
                        Text("$it min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    recipe.servings?.let {
                        Text("$it servings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
}
