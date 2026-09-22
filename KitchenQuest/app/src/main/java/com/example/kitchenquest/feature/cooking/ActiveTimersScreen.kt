package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun ActiveTimersScreen(
    state: CookingUiState,
    onAddMinute: (String) -> Unit,
    onTogglePause: (String) -> Unit,
    onCancel: (String) -> Unit,
    onStopAll: () -> Unit,
    onBackToCooking: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(KitchenQuestDimens.ScreenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Timers", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onStopAll, enabled = state.timers.isNotEmpty()) {
                Text("Stop all")
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        val featured = state.timers.firstOrNull { it.isRunning && !it.isFinished } ?: state.timers.firstOrNull()

        if (featured != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = formatSeconds(featured.remainingSeconds), style = MaterialTheme.typography.displayMedium)
                Text(text = featured.label, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)) {
                    KitchenQuestSecondaryButton(text = "+1 min", onClick = { onAddMinute(featured.id) })
                    KitchenQuestSecondaryButton(
                        text = if (featured.isRunning) "Pause" else "Resume",
                        onClick = { onTogglePause(featured.id) }
                    )
                    KitchenQuestSecondaryButton(text = "Cancel", onClick = { onCancel(featured.id) })
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))
        }

        Text(text = "All timers", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        if (state.timers.isEmpty()) {
            Text(
                text = "No timers running. Start one from a recipe step or the Kitchen Timer.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
            ) {
                items(state.timers, key = { it.id }) { timer ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = timer.label)
                            Text(
                                text = if (timer.sourceType == TimerSource.RECIPE) "From recipe" else "Standalone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (timer.isFinished) "Done" else formatSeconds(timer.remainingSeconds),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        KitchenQuestPrimaryButton(
            text = "Back to cooking",
            onClick = onBackToCooking,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}