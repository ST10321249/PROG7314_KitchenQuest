package com.example.kitchenquest.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onSettings: () -> Unit,
    onCookingHistory: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KitchenQuestDimens.ScreenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Profile", style = MaterialTheme.typography.headlineSmall)

            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

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

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = KitchenQuestDimens.ScreenPadding)
                    ) {
                        KitchenQuestCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(KitchenQuestDimens.MediumSpacing))

                                Column {
                                    Text(
                                        text = state.displayName.ifBlank { "KitchenQuest cook" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = state.email.ifBlank { "No email on this account" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                value = state.mealsCooked.toString(),
                                label = "Meals cooked",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(KitchenQuestDimens.SmallSpacing))
                            StatCard(
                                icon = Icons.Filled.History,
                                value = state.savedRecipesCount.toString(),
                                label = "Saved recipes",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            StatCard(
                                icon = Icons.Filled.Kitchen,
                                value = state.pantryItemCount.toString(),
                                label = "Pantry items",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(KitchenQuestDimens.SmallSpacing))
                            StatCard(
                                icon = Icons.Filled.Warning,
                                value = state.expiringSoonCount.toString(),
                                label = "Expiring soon",
                                highlight = state.expiringSoonCount > 0,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                        KitchenQuestCard(onClick = onCookingHistory) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.History, contentDescription = null)
                                    Spacer(modifier = Modifier.width(KitchenQuestDimens.SmallSpacing))
                                    Text(text = "Cooking history", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    KitchenQuestCard(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
