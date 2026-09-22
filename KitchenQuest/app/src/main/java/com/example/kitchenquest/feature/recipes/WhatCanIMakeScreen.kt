package com.example.kitchenquest.feature.recipes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun WhatCanIMakeScreen(
    state: WhatCanIMakeUiState,
    onRecipeClick: (RecipeRecommendationDto) -> Unit,
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

            state.recommendations.isEmpty() -> {
                Text(
                    text = "Add some ingredients to My Kitchen to see what you can make.",
                    modifier = Modifier.align(Alignment.Center).padding(KitchenQuestDimens.ScreenPadding),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            RecommendationRow(recommendation, onClick = { onRecipeClick(recommendation) })
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(KitchenQuestDimens.MediumSpacing)) {
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
            }
        }
    }
}