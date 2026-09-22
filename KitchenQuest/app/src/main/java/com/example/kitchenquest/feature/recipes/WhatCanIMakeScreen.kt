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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatCanIMakeScreen(
    state: WhatCanIMakeUiState,
    onBack: () -> Unit,
    onRecipeClick: (RecipeRecommendationDto) -> Unit,
    onAddMissingToShoppingList: (RecipeRecommendationDto) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        KitchenQuestTopBar(
            title = "What Can I Make?",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        if (state.pantryIngredientNames.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KitchenQuestDimens.ScreenPadding)
            ) {
                KitchenQuestSectionTitle(title = "MATCHING AGAINST YOUR KITCHEN")
                Spacer(modifier = Modifier.height(KitchenQuestDimens.TinySpacing))

                FlowRow {
                    state.pantryIngredientNames.forEach { ingredient ->
                        KitchenQuestChoiceChip(
                            text = ingredient,
                            selected = true,
                            onClick = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
            }
        }

        Box(modifier = Modifier.weight(1f)) {
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

                state.recommendations.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "Nothing to show yet",
                        message = "Add some ingredients to My Kitchen to see what you can make.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "${state.recommendations.size} matches",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                        )

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = KitchenQuestDimens.ScreenPadding),
                            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                        ) {
                            items(state.recommendations, key = { it.recipeSourceId }) { recommendation ->
                                RecommendationRow(
                                    recommendation = recommendation,
                                    alreadyAddedToList = recommendation.recipeSourceId in state.addedToShoppingList,
                                    onClick = { onRecipeClick(recommendation) },
                                    onAddMissingToShoppingList = { onAddMissingToShoppingList(recommendation) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationRow(
    recommendation: RecipeRecommendationDto,
    alreadyAddedToList: Boolean,
    onClick: () -> Unit,
    onAddMissingToShoppingList: () -> Unit
) {
    KitchenQuestCard(onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = recommendation.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${(recommendation.matchRatio * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { recommendation.matchRatio.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )

        if (recommendation.missingIngredients.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Missing: ${recommendation.missingIngredients.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            KitchenQuestSecondaryButton(
                text = if (alreadyAddedToList) "Added to shopping list" else "Add missing to shopping list",
                onClick = onAddMissingToShoppingList,
                enabled = !alreadyAddedToList,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
