package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun CookScreen(
    recipeTitle: String?,
    activeTimerCount: Int,
    onContinueCooking: () -> Unit,
    onKitchenTimer: () -> Unit,
    onActiveTimers: () -> Unit,
    onCookingHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KitchenQuestDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(
            KitchenQuestDimens.MediumSpacing
        )
    ) {
        KitchenQuestTopBar(
            title = "Cook"
        )

        Text(
            text = "Keep your cooking tools and recent activity in one place.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (recipeTitle != null) {
            KitchenQuestCard {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.height(
                        KitchenQuestDimens.SmallSpacing
                    )
                )

                Text(
                    text = "Continue cooking",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = recipeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(
                        KitchenQuestDimens.MediumSpacing
                    )
                )

                KitchenQuestPrimaryButton(
                    text = "Continue recipe",
                    onClick = onContinueCooking,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        KitchenQuestCard(
            onClick = onKitchenTimer
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(
                    KitchenQuestDimens.SmallSpacing
                )
            )

            Text(
                text = "Kitchen timer",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Start a standalone timer while you cook.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (activeTimerCount > 0) {
            KitchenQuestSecondaryButton(
                text = "Active timers ($activeTimerCount)",
                onClick = onActiveTimers,
                modifier = Modifier.fillMaxWidth()
            )
        }

        KitchenQuestCard(
            onClick = onCookingHistory
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(
                    KitchenQuestDimens.SmallSpacing
                )
            )

            Text(
                text = "Cooking history",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Review meals you have completed and your saved feedback.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
