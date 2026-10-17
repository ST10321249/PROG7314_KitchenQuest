package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CookingHistoryScreen(
    state: CookingHistoryUiState,
    onBack: () -> Unit,
    onFilterSelected: (CookingHistoryFilter) -> Unit,
    onRecipeClick: (CookingHistoryDto) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        KitchenQuestTopBar(
            title = "Cooking history",
            onBack = onBack,
            modifier = Modifier.padding(
                horizontal = KitchenQuestDimens.ScreenPadding
            )
        )

        when {
            state.isLoading && !state.hasLoaded -> {
                KitchenQuestLoadingState(
                    message = "Loading your cooking history...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            state.errorMessage != null && !state.hasLoaded -> {
                KitchenQuestErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            else -> {
                HistoryContent(
                    state = state,
                    onFilterSelected = onFilterSelected,
                    onRecipeClick = onRecipeClick
                )
            }
        }
    }
}

@Composable
private fun HistoryContent(
    state: CookingHistoryUiState,
    onFilterSelected: (CookingHistoryFilter) -> Unit,
    onRecipeClick: (CookingHistoryDto) -> Unit
) {
    val ratedEntries = state.entries.mapNotNull { it.rating }
    val averageRating = ratedEntries
        .takeIf { it.isNotEmpty() }
        ?.average()

    val thisMonthCount = state.entries.count { entry ->
        isCurrentMonth(entry.cookedAt)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = KitchenQuestDimens.ScreenPadding,
                    vertical = KitchenQuestDimens.SmallSpacing
                ),
            horizontalArrangement = Arrangement.spacedBy(
                KitchenQuestDimens.SmallSpacing
            )
        ) {
            HistoryMetric(
                value = state.entries.size.toString(),
                label = "Total meals",
                modifier = Modifier.weight(1f)
            )

            HistoryMetric(
                value = thisMonthCount.toString(),
                label = "This month",
                modifier = Modifier.weight(1f)
            )

            HistoryMetric(
                value = averageRating?.let {
                    String.format(Locale.US, "%.1f", it)
                } ?: "—",
                label = "Avg rating",
                modifier = Modifier.weight(1f)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(
                horizontal = KitchenQuestDimens.ScreenPadding
            )
        ) {
            items(CookingHistoryFilter.entries) { filter ->
                KitchenQuestChoiceChip(
                    text = filter.label,
                    selected = state.selectedFilter == filter,
                    onClick = { onFilterSelected(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        if (state.filteredEntries.isEmpty()) {
            KitchenQuestEmptyState(
                title = if (state.entries.isEmpty()) {
                    "No cooked meals yet"
                } else {
                    "No meals match this filter"
                },
                message = if (state.entries.isEmpty()) {
                    "Completed recipes will appear here with your rating and notes."
                } else {
                    "Try another filter to see more of your cooking history."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KitchenQuestDimens.ScreenPadding)
            )
        } else {
            val grouped = state.filteredEntries.groupBy { entry ->
                monthLabel(entry.cookedAt)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = KitchenQuestDimens.ScreenPadding,
                    end = KitchenQuestDimens.ScreenPadding,
                    bottom = KitchenQuestDimens.SectionSpacing
                ),
                verticalArrangement = Arrangement.spacedBy(
                    KitchenQuestDimens.MediumSpacing
                )
            ) {
                grouped.forEach { (month, entries) ->
                    item(key = "month-$month") {
                        Text(
                            text = month.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item(key = "card-$month") {
                        KitchenQuestCard {
                            entries.forEachIndexed { index, entry ->
                                HistoryRow(
                                    entry = entry,
                                    note = state.notesByRecipeId[entry.recipeSourceId],
                                    onClick = { onRecipeClick(entry) }
                                )

                                if (index != entries.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(
                                            vertical = KitchenQuestDimens.FieldSpacing
                                        ),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            KitchenQuestDimens.CardBorderWidth,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = KitchenQuestDimens.SmallSpacing,
                vertical = KitchenQuestDimens.MediumSpacing
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: CookingHistoryDto,
    note: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = KitchenQuestDimens.TinySpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.recipeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = shortDate(entry.cookedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entry.rating?.let { rating ->
                Text(
                    text = "★".repeat(rating.coerceIn(1, 5)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = KitchenQuestDimens.SmallSpacing),
            horizontalArrangement = Arrangement.spacedBy(
                KitchenQuestDimens.SmallSpacing
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entry.difficultyFeedback?.takeIf { it.isNotBlank() }?.let { difficulty ->
                Surface(
                    shape = RoundedCornerShape(KitchenQuestDimens.ChipCorner),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(
                            horizontal = KitchenQuestDimens.SmallSpacing,
                            vertical = KitchenQuestDimens.TinySpacing
                        )
                    )
                }
            }
        }

        note?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = KitchenQuestDimens.SmallSpacing)
            )
        }
    }
}

private fun monthLabel(value: String): String {
    val parsed = runCatching {
        OffsetDateTime.parse(value)
    }.getOrNull() ?: return "Earlier"

    return parsed.format(
        DateTimeFormatter.ofPattern("MMMM yyyy")
    )
}

private fun shortDate(value: String): String {
    val parsed = runCatching {
        OffsetDateTime.parse(value)
    }.getOrNull() ?: return "Completed"

    return parsed.format(
        DateTimeFormatter.ofPattern("dd MMM")
    )
}

private fun isCurrentMonth(value: String): Boolean {
    val parsed = runCatching {
        OffsetDateTime.parse(value)
    }.getOrNull() ?: return false

    val now = OffsetDateTime.now()

    return parsed.year == now.year &&
        parsed.monthValue == now.monthValue
}
