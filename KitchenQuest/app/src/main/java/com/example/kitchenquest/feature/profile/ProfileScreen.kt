package com.example.kitchenquest.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestIconButton
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenGreenLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onSettings: () -> Unit,
    onSavedRecipes: () -> Unit,
    onCookingHistory: () -> Unit,
    onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = KitchenQuestDimens.SectionSpacing)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = KitchenQuestDimens.ScreenPadding,
                        end = KitchenQuestDimens.ScreenPadding,
                        top = KitchenQuestDimens.SectionSpacing,
                        bottom = KitchenQuestDimens.LargeSpacing
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        KitchenQuestIconButton(
                            icon = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            onClick = onSettings
                        )
                    }

                    Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

                    Surface(
                        modifier = Modifier
                            .size(76.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🍳", style = MaterialTheme.typography.headlineLarge)
                        }
                    }

                    Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

                    Text(
                        text = state.displayName.ifBlank { "KitchenQuest cook" },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    if (state.email.isNotBlank()) {
                        Text(
                            text = state.email,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.MediumSpacing)
            ) {
                if (state.errorMessage != null && !state.hasLoaded) {
                    KitchenQuestErrorState(
                        message = state.errorMessage,
                        onRetry = onRetry
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
                ) {
                    StatCard("${state.mealsCooked}", "Meals cooked", Modifier.weight(1f))
                    StatCard("${state.savedRecipes}", "Saved recipes", Modifier.weight(1f))
                    StatCard("${state.expiringSoon}", "Expiring soon", Modifier.weight(1f))
                }

                state.averageRating?.let { rating ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                        color = KitchenGreenLight
                    ) {
                        Row(
                            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⭐", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.size(KitchenQuestDimens.MediumSpacing))
                            Column {
                                Text(
                                    text = "${"%.1f".format(rating)} average rating",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KitchenGreen
                                )
                                Text(
                                    text = "Based on recipes you've rated after cooking.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                ProfileLink(
                    emoji = "❤️",
                    title = "Saved recipes",
                    onClick = onSavedRecipes
                )

                ProfileLink(
                    emoji = "📖",
                    title = "Cooking history",
                    onClick = onCookingHistory
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(vertical = KitchenQuestDimens.MediumSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileLink(
    emoji: String,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge)
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = KitchenQuestDimens.MediumSpacing),
                style = MaterialTheme.typography.titleSmall
            )
            Text(text = "›", style = MaterialTheme.typography.titleLarge)
        }
    }
}
