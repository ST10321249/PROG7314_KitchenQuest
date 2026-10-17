package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestIconButton
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenAmber
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatCanIMakeScreen(
    state: WhatCanIMakeUiState,
    onBack: () -> Unit,
    onSavedRecipes: () -> Unit,
    onToggleIngredient: (String) -> Unit,
    onAddIngredient: () -> Unit,
    onAddMissingToList: (RecipeRecommendationDto) -> Unit,
    onRecipeClick: (RecipeRecommendationDto) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        KitchenQuestTopBar(
            title = "What Can I Make?",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding),
            actions = {
                KitchenQuestIconButton(
                    icon = Icons.Filled.Favorite,
                    contentDescription = "Saved recipes",
                    onClick = onSavedRecipes
                )
            }
        )

        FlowRow(
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing),
            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
        ) {
            state.pantryIngredients.forEach { ingredient ->
                KitchenQuestChoiceChip(
                    text = if (ingredient in state.selectedIngredients) "$ingredient ×" else ingredient,
                    selected = ingredient in state.selectedIngredients,
                    onClick = { onToggleIngredient(ingredient) }
                )
            }

            KitchenQuestSecondaryButton(
                text = "+ Add ingredient",
                onClick = onAddIngredient
            )
        }

        state.actionMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(
                    horizontal = KitchenQuestDimens.ScreenPadding,
                    vertical = KitchenQuestDimens.SmallSpacing
                ),
                style = MaterialTheme.typography.labelMedium,
                color = KitchenGreen
            )
        }

        Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading && !state.hasLoaded -> {
                    KitchenQuestLoadingState(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding),
                        message = "Matching recipes to your kitchen..."
                    )
                }

                state.errorMessage != null -> {
                    KitchenQuestErrorState(
                        message = state.errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                state.selectedIngredients.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "Choose at least one ingredient",
                        message = "Tap an ingredient above to include it in recipe matching.",
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                state.recommendations.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "No matches yet",
                        message = "Add more ingredients to My Kitchen or try a different combination.",
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding),
                        actionText = "Add ingredient",
                        onAction = onAddIngredient
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = KitchenQuestDimens.ScreenPadding,
                            end = KitchenQuestDimens.ScreenPadding,
                            bottom = KitchenQuestDimens.SectionSpacing
                        ),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.MediumSpacing)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${state.recommendations.size} matches",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Best match",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(state.recommendations, key = { it.recipeSourceId }) { recommendation ->
                            RecommendationCard(
                                recommendation = recommendation,
                                onClick = { onRecipeClick(recommendation) },
                                onAddMissing = { onAddMissingToList(recommendation) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: RecipeRecommendationDto,
    onClick: () -> Unit,
    onAddMissing: () -> Unit
) {
    val percent = (recommendation.matchRatio * 100).toInt()
    val ratioColor = if (recommendation.matchRatio >= 0.75) KitchenGreen else KitchenAmber

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(recommendation.title, style = MaterialTheme.typography.titleSmall)
                    Text("$percent% ingredient match", style = MaterialTheme.typography.labelMedium, color = ratioColor)
                }
            }

            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

            LinearProgressIndicator(
                progress = { recommendation.matchRatio.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = ratioColor
            )

            if (recommendation.missingIngredients.isNotEmpty()) {
                Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Missing: ${recommendation.missingIngredients.joinToString(", ")}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add to list",
                        modifier = Modifier
                            .clickable(onClick = onAddMissing)
                            .padding(KitchenQuestDimens.SmallSpacing),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
