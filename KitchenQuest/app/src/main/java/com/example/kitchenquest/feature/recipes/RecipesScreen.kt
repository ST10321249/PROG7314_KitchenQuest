package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.R
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
import androidx.compose.ui.res.stringResource
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
            text = stringResource(R.string.recipes_recipes_0153a1),
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
                placeholder = stringResource(R.string.recipes_search_recipes_or_ingredients_d98b98),
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
                            text = stringResource(R.string.recipes_what_can_i_make_726bfa),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(KitchenQuestDimens.TinySpacing))
                        Text(
                            text = stringResource(R.string.recipes_find_recipes_from_the_ingredients_in_my_kitchen_409201),
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
                        text = recipeFilterLabel(filter),
                        selected = state.selectedFilter == filter,
                        onClick = { onFilterSelected(filter) }
                    )
                }
            }

            Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

            Text(
                text = stringResource(R.string.recipes_categories_af1b98),
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
                                text = recipeCategoryLabel(category),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

            KitchenQuestSectionTitle(title = stringResource(R.string.recipes_popular_this_week_201ff2))
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading && !state.hasLoaded -> {
                    KitchenQuestLoadingState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding),
                        message = stringResource(R.string.recipes_finding_recipes_51cf34)
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
                        title = stringResource(R.string.recipes_no_recipes_found_c0aa6e),
                        message = stringResource(R.string.recipes_try_another_search_or_remove_a_filter_9e9c48),
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                !state.hasLoaded -> {
                    KitchenQuestEmptyState(
                        title = stringResource(R.string.recipes_find_something_to_cook_5ed09c),
                        message = stringResource(R.string.recipes_search_for_a_recipe_choose_a_category_or_browse_recipes_b_28a2fe),
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding),
                        actionText = stringResource(R.string.recipes_browse_recipes_c60b98),
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
                            text = stringResource(R.string.recipes_minutes_format, it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    recipe.servings?.let {
                        Text(
                            text = stringResource(R.string.recipes_servings_format, it),
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

/** Translate display labels only; recipe filters and categories stay in their API format. */
@Composable
private fun recipeFilterLabel(value: String): String = when (value) {
    "All" -> stringResource(R.string.recipe_filter_all)
    "Under 30 min" -> stringResource(R.string.recipe_filter_under30)
    "Chicken" -> stringResource(R.string.recipe_filter_chicken)
    "Vegetarian" -> stringResource(R.string.recipe_filter_vegetarian)
    else -> value
}

@Composable
private fun recipeCategoryLabel(value: String): String = when (value) {
    "Breakfast" -> stringResource(R.string.recipe_category_breakfast)
    "Lunch" -> stringResource(R.string.recipe_category_lunch)
    "Dinner" -> stringResource(R.string.recipe_category_dinner)
    "Healthy" -> stringResource(R.string.recipe_category_healthy)
    else -> value
}
