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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestSearchField
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

private val recipeFilters = listOf("All", "Under 30 min", "Chicken", "Vegetarian")
private val recipeCategories = listOf(
    Triple("Breakfast", "🍳", Color(0xFFFFEDD9)),
    Triple("Lunch", "🥗", Color(0xFFE7F0FA)),
    Triple("Dinner", "🍲", Color(0xFFF9E8E8)),
    Triple("Healthy", "🥑", Color(0xFFE5F3EC))
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
    // Saved recipes remain accessible from Profile and from the heart on recipe
    // details. Keeping the Recipes header uncluttered matches the supplied design.
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Recipes",
            modifier = Modifier.padding(
                start = KitchenQuestDimens.ScreenPadding,
                end = KitchenQuestDimens.ScreenPadding,
                top = KitchenQuestDimens.MediumSpacing
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

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
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(KitchenQuestDimens.TinySpacing))
                        Text(
                            text = "Find recipes from the ingredients in My Kitchen",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
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

            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
            ) {
                recipeCategories.forEach { (category, emoji, background) ->
                    Surface(
                        onClick = { onCategorySelected(category) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                        color = background
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = KitchenQuestDimens.TinySpacing,
                                vertical = KitchenQuestDimens.MediumSpacing
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(emoji, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(KitchenQuestDimens.TinySpacing))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
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
                        contentPadding = PaddingValues(
                            start = KitchenQuestDimens.ScreenPadding,
                            end = KitchenQuestDimens.ScreenPadding,
                            top = KitchenQuestDimens.SmallSpacing,
                            bottom = KitchenQuestDimens.SectionSpacing
                        ),
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
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                color = KitchenOrangeLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🍲", style = MaterialTheme.typography.titleLarge)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KitchenQuestDimens.MediumSpacing)
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(KitchenQuestDimens.TinySpacing))
                Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)) {
                    recipe.readyInMinutes?.let {
                        Text(
                            text = "$it min",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    recipe.servings?.let {
                        Text(
                            text = "$it servings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
