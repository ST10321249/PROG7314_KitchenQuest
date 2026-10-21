package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.R
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
import androidx.compose.ui.res.stringResource
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
            title = stringResource(R.string.cook_cook_1bf70a)
        )

        Text(
            text = stringResource(R.string.cook_keep_your_cooking_tools_and_recent_activity_in_one_place_8a58d1),
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
                    text = stringResource(R.string.cook_continue_cooking_dfd1a2),
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
                    text = stringResource(R.string.cook_continue_recipe_cbe219),
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
                text = stringResource(R.string.cook_kitchen_timer_829429),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.cook_start_a_standalone_timer_while_you_cook_a220c2),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (activeTimerCount > 0) {
            KitchenQuestSecondaryButton(
                text = stringResource(R.string.cook_active_timers, activeTimerCount),
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
                text = stringResource(R.string.cook_cooking_history_4eae4d),
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.cook_review_meals_you_have_completed_and_your_saved_feedback_2ad36a),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
