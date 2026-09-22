package com.example.kitchenquest.feature.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNotifications: () -> Unit,
    onKitchenTimer: () -> Unit,
    onWhatCanIMake: () -> Unit,
    onShoppingList: () -> Unit,
    onRecipes: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KitchenQuest",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (state.displayName.isNotBlank()) {
                            "Hello, ${state.displayName}"
                        } else {
                            "Hello"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                IconButton(onClick = onNotifications) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications"
                    )
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))
        }

        if (state.isLoading && !state.hasLoaded) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.errorMessage != null && !state.hasLoaded) {
            item {
                KitchenQuestErrorState(message = state.errorMessage, onRetry = onRetry)
            }
        } else {

            item {
                KitchenQuestSectionTitle(title = "QUICK ACTIONS")
                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)
                ) {
                    QuickAction(
                        icon = Icons.Filled.Timer,
                        label = "Kitchen Timer",
                        onClick = onKitchenTimer,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAction(
                        icon = Icons.Filled.Search,
                        label = "What Can I Make?",
                        onClick = onWhatCanIMake,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)
                ) {
                    QuickAction(
                        icon = Icons.Filled.ListAlt,
                        label = "Shopping List",
                        onClick = onShoppingList,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAction(
                        icon = Icons.Filled.RestaurantMenu,
                        label = "Recipes",
                        onClick = onRecipes,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))
            }

            item {
                KitchenQuestSectionTitle(title = "YOUR KITCHEN")
                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                KitchenQuestCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatTile(
                            value = state.pantryItemCount.toString(),
                            label = "Pantry items",
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            value = state.expiringSoonCount.toString(),
                            label = "Expiring soon",
                            highlight = state.expiringSoonCount > 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))
            }

            item {
                KitchenQuestSectionTitle(title = "RECIPES YOU CAN MAKE")
                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))
            }

            when {
                state.recommendationsLoading -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.recommendations.isEmpty() -> item {
                    KitchenQuestEmptyState(
                        title = "No matches yet",
                        message = "Add ingredients to My Kitchen and recipe recommendations will show up here."
                    )
                }

                else -> item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)) {
                        items(state.recommendations) { recommendation ->
                            RecommendationCard(
                                recommendation = recommendation,
                                onClick = { onRecipeClick(recommendation.recipeSourceId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            KitchenQuestDimens.CardBorderWidth,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KitchenQuestDimens.MediumSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier.padding(KitchenQuestDimens.SmallSpacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecommendationCard(
    recommendation: RecipeRecommendationDto,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 200.dp, height = 140.dp),
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            KitchenQuestDimens.CardBorderWidth,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(KitchenQuestDimens.MediumSpacing)) {
            Text(
                text = recommendation.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            if (recommendation.missingIngredients.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${recommendation.missingIngredients.size} missing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            LinearProgressIndicator(
                progress = { recommendation.matchRatio.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${(recommendation.matchRatio * 100).toInt()}% match",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
