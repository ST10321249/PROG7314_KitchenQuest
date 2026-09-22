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
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun CookingHistoryScreen(
    state: CookingHistoryUiState,
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

            state.entries.isEmpty() -> {
                Text(
                    text = "You haven't finished any recipes yet.",
                    modifier = Modifier.align(Alignment.Center).padding(KitchenQuestDimens.ScreenPadding),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Cooking history",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = KitchenQuestDimens.ScreenPadding),
                        verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                    ) {
                        items(state.entries, key = { it.id }) { entry ->
                            HistoryRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: CookingHistoryDto) {
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