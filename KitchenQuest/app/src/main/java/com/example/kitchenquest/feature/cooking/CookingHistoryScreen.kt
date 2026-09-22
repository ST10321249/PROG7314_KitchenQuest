package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestSectionTitle
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun CookingHistoryScreen(
    state: CookingHistoryUiState,
    onBack: () -> Unit,
    onRecipeClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        KitchenQuestTopBar(
            title = "Cooking History",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        Box(modifier = Modifier.fillMaxSize()) {
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

                state.entries.isEmpty() -> {
                    KitchenQuestEmptyState(
                        title = "No cooks yet",
                        message = "Finish a recipe in Cooking Mode and it'll show up here.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(KitchenQuestDimens.ScreenPadding)
                    )
                }

                else -> {
                    val groups = groupHistoryByMonth(state.entries)

                    LazyColumn(
                        contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                    ) {
                        groups.forEach { (monthLabel, entries) ->
                            item { KitchenQuestSectionTitle(title = monthLabel.uppercase()) }
                            items(entries, key = { it.id }) { entry ->
                                HistoryRow(entry, onClick = { onRecipeClick(entry.recipeSourceId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: CookingHistoryDto,
    onClick: () -> Unit
) {
    KitchenQuestCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = entry.recipeTitle, style = MaterialTheme.typography.bodyLarge)
                entry.difficultyFeedback?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (entry.rating != null) {
                Row {
                    repeat(entry.rating) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
