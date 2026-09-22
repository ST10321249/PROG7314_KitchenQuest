package com.example.kitchenquest.feature.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRedLight

@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onOpenKitchen: () -> Unit,
    onOpenShoppingList: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        KitchenQuestTopBar(
            title = "Notifications",
            onBack = onBack,
            modifier = Modifier.padding(horizontal = KitchenQuestDimens.ScreenPadding)
        )

        when {
            state.isLoading -> {
                KitchenQuestLoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KitchenQuestDimens.ScreenPadding),
                    message = "Checking your kitchen..."
                )
            }

            state.errorMessage != null -> {
                KitchenQuestErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            state.expiringCount == 0 && state.shoppingCount == 0 -> {
                KitchenQuestEmptyState(
                    title = "You're all caught up",
                    message = "Kitchen reminders and shopping updates will appear here.",
                    modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(KitchenQuestDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.MediumSpacing)
                ) {
                    item {
                        Text(
                            text = "TODAY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.expiringCount > 0) {
                        item {
                            NotificationCard(
                                emoji = "🥕",
                                title = "${state.expiringCount} ingredient${if (state.expiringCount == 1) "" else "s"} expiring soon",
                                message = "Open My Kitchen to use them before they expire.",
                                containerColor = KitchenRedLight,
                                onClick = onOpenKitchen
                            )
                        }
                    }

                    if (state.shoppingCount > 0) {
                        item {
                            NotificationCard(
                                emoji = "🛒",
                                title = "Shopping list reminder",
                                message = "You still have ${state.shoppingCount} item${if (state.shoppingCount == 1) "" else "s"} to buy.",
                                containerColor = KitchenOrangeLight,
                                onClick = onOpenShoppingList
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    emoji: String,
    title: String,
    message: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
                }
            }

            Spacer(modifier = Modifier.size(KitchenQuestDimens.MediumSpacing))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
