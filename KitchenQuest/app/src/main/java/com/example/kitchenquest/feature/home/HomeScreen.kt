package com.example.kitchenquest.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Notifications
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
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.feature.pantry.daysUntilExpiry
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestIconButton
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenGreenLight
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRed
import com.example.kitchenquest.ui.theme.KitchenRedLight
import java.time.LocalTime

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNotifications: () -> Unit,
    onKitchenTimer: () -> Unit,
    onWhatCanIMake: () -> Unit,
    onShoppingList: () -> Unit,
    onSearchRecipes: () -> Unit,
    onMyKitchen: () -> Unit,
    onRecipeClick: (RecipeRecommendationDto) -> Unit,
    onRetry: () -> Unit
) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = KitchenQuestDimens.ScreenPadding,
            end = KitchenQuestDimens.ScreenPadding,
            top = KitchenQuestDimens.MediumSpacing,
            bottom = KitchenQuestDimens.SectionSpacing
        ),
        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SectionSpacing)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.displayName.ifBlank { "KitchenQuest" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                KitchenQuestIconButton(
                    icon = Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    onClick = onNotifications
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
            ) {
                QuickAction(
                    emoji = "⏱",
                    label = "Timer",
                    iconBackground = Color(0xFFFFEEE0),
                    onClick = onKitchenTimer,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    emoji = "🍳",
                    label = "What can\nI make?",
                    iconBackground = Color(0xFFE5F4EE),
                    onClick = onWhatCanIMake,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    emoji = "🛒",
                    label = "Shopping\nlist",
                    iconBackground = Color(0xFFE9EDF5),
                    onClick = onShoppingList,
                    modifier = Modifier.weight(1f)
                )
                QuickAction(
                    emoji = "🔎",
                    label = "Search\nrecipes",
                    iconBackground = Color(0xFFF0EAF9),
                    onClick = onSearchRecipes,
                    modifier = Modifier.weight(1f)
                )
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
                    modifier = Modifier.padding(vertical = KitchenQuestDimens.MediumSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryValue("${state.pantryCount}", "In my kitchen", Modifier.weight(1f))
                    SummaryDivider()
                    SummaryValue(
                        "${state.expiringSoon.size}",
                        "Expiring soon",
                        Modifier.weight(1f),
                        KitchenRed
                    )
                    SummaryDivider()
                    SummaryValue("${state.shoppingCount}", "To buy", Modifier.weight(1f))
                }
            }
        }

        if (state.errorMessage != null && !state.hasLoaded) {
            item {
                KitchenQuestErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry
                )
            }
        }

        if (state.expiringSoon.isNotEmpty()) {
            item {
                KitchenQuestSectionTitle(
                    title = "Use soon"
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                ) {
                    items(state.expiringSoon.take(6), key = { it.id }) { item ->
                        ExpiringCard(item)
                    }
                }
            }
        }

        item {
            KitchenQuestSectionTitle(
                title = "Quick Picks"
            )
        }

        if (state.isLoading && !state.hasLoaded) {
            item {
                Text(
                    text = "Loading your kitchen...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (state.recommendations.isEmpty()) {
            item {
                Surface(
                    onClick = onWhatCanIMake,
                    shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                    color = KitchenGreenLight,
                    border = BorderStroke(1.dp, KitchenGreen.copy(alpha = 0.16f))
                ) {
                    Column(modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing)) {
                        Text(
                            text = "Find recipes from your ingredients",
                            style = MaterialTheme.typography.titleSmall,
                            color = KitchenGreen
                        )
                        Spacer(Modifier.height(KitchenQuestDimens.TinySpacing))
                        Text(
                            text = "Add ingredients to My Kitchen and KitchenQuest will rank recipes by what you already have.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(state.recommendations.take(4), key = { it.recipeSourceId }) { recipe ->
                RecommendationCard(recipe = recipe, onClick = { onRecipeClick(recipe) })
            }
        }
    }
}

@Composable
private fun QuickAction(
    emoji: String,
    label: String,
    iconBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = KitchenQuestDimens.TinySpacing,
                vertical = KitchenQuestDimens.MediumSpacing
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(KitchenQuestDimens.SmallCorner),
                color = iconBackground
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(38.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

@Composable
private fun SummaryValue(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExpiringCard(item: PantryItemDto) {
    val days = daysUntilExpiry(item)
    val message = when (days) {
        0L -> "Expires today"
        1L -> "Expires tomorrow"
        null -> "Expiry soon"
        else -> "$days days left"
    }

    Surface(
        modifier = Modifier.size(width = 104.dp, height = 108.dp),
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = KitchenRedLight,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = ingredientEmoji(item.ingredientName), style = MaterialTheme.typography.titleLarge)
            Text(
                text = item.ingredientName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelSmall,
                color = KitchenRed,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    recipe: RecipeRecommendationDto,
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
                    Text(text = "🍲", style = MaterialTheme.typography.titleLarge)
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
                Text(
                    text = "${(recipe.matchRatio * 100).toInt()}% ingredient match",
                    style = MaterialTheme.typography.labelMedium,
                    color = KitchenGreen
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun ingredientEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        "chicken" in lower -> "🍗"
        "milk" in lower -> "🥛"
        "spinach" in lower || "lettuce" in lower -> "🥬"
        "tomato" in lower -> "🍅"
        "egg" in lower -> "🥚"
        "rice" in lower -> "🍚"
        "pasta" in lower -> "🍝"
        "cheese" in lower -> "🧀"
        "onion" in lower -> "🧅"
        "garlic" in lower -> "🧄"
        else -> "🥕"
    }
}
