package com.example.kitchenquest.feature.recipes

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeIngredientDto
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenGreenLight
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
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
    when {
        state.isLoading && state.recipe == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                KitchenQuestLoadingState(message = stringResource(R.string.recipedetail_loading_recipe_1d53c5))
            }
        }

        state.errorMessage != null && state.recipe == null -> {
            KitchenQuestErrorState(
                message = state.errorMessage,
                onRetry = onRetry,
                modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
            )
        }

        state.recipe != null -> {
            val recipe = state.recipe
            val baseServings = recipe.servings?.takeIf { it > 0 } ?: state.servings
            val scaleFactor = state.servings.toDouble() / baseServings
            val heldCount = recipe.ingredients.count { it.name.lowercase() in state.heldIngredientNames }
            val missingCount = recipe.ingredients.size - heldCount

            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = KitchenQuestDimens.ScreenPadding,
                            end = KitchenQuestDimens.ScreenPadding,
                            top = KitchenQuestDimens.SmallSpacing,
                            bottom = KitchenQuestDimens.SectionSpacing
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.recipedetail_back_0557fa))
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Surface(
                                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                IconButton(onClick = onToggleFavourite) {
                                    Icon(
                                        imageVector = if (state.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = if (state.isFavourite) stringResource(R.string.recipedetail_remove_from_saved_f5b42c) else stringResource(R.string.recipedetail_save_recipe_ca579a),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = KitchenOrangeLight
                            ) {}
                            Text("🍝", style = MaterialTheme.typography.displayMedium)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.MediumSpacing)
                ) {
                    item {
                        Text(recipe.title, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                        Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.MediumSpacing)) {
                            recipe.readyInMinutes?.let {
                                Text(stringResource(R.string.detail_minutes, it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            recipe.servings?.let {
                                Text(stringResource(R.string.detail_servings, it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (recipe.ingredients.isNotEmpty()) {
                            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                            Surface(
                                shape = RoundedCornerShape(KitchenQuestDimens.ChipCorner),
                                color = KitchenGreenLight
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_owned_format, heldCount, recipe.ingredients.size),
                                    modifier = Modifier.padding(
                                        horizontal = KitchenQuestDimens.MediumSpacing,
                                        vertical = KitchenQuestDimens.SmallSpacing
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = KitchenGreen
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.recipedetail_servings_4349e0), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(R.string.recipedetail_quantities_update_automatically_d087c6),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = onDecreaseServings) {
                                            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.recipedetail_fewer_servings_20d364))
                                        }
                                        Text("${state.servings}", style = MaterialTheme.typography.titleSmall)
                                        IconButton(onClick = onIncreaseServings) {
                                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recipedetail_more_servings_5d4fc5))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.recipedetail_ingredients_210c9e), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (missingCount > 0) {
                                Text(
                                    stringResource(R.string.recipedetail_add_missing_to_list_f56f15),
                                    modifier = Modifier.padding(KitchenQuestDimens.SmallSpacing),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    items(recipe.ingredients, key = { it.name }) { ingredient ->
                        IngredientLine(
                            ingredient = ingredient,
                            scaleFactor = scaleFactor,
                            held = ingredient.name.lowercase() in state.heldIngredientNames
                        )
                    }

                    if (missingCount > 0) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onAddMissingToList,
                                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                                color = KitchenOrangeLight
                            ) {
                                Text(
                                    text = if (missingCount == 1) stringResource(R.string.detail_missing_one) else stringResource(R.string.detail_missing_many, missingCount),
                                    modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                KitchenQuestPrimaryButton(
                    text = stringResource(R.string.recipedetail_start_cooking_ee58ff),
                    onClick = onStartCooking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KitchenQuestDimens.ScreenPadding)
                )
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
    val scaledAmount = ingredient.amount * scaleFactor

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                shape = RoundedCornerShape(7.dp),
                color = if (held) KitchenGreen else Color.Transparent,
                border = if (held) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                if (held) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.recipedetail_in_my_kitchen_a4d4ae),
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Text(
                text = ingredient.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KitchenQuestDimens.MediumSpacing),
                color = if (held) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "${formatAmount(scaledAmount)} ${ingredient.unit}",
                color = if (held) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}
