package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestEmptyState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun ActiveTimersScreen(
    state: CookingUiState,
    onAddMinute: (String) -> Unit,
    onTogglePause: (String) -> Unit,
    onCancel: (String) -> Unit,
    onStopAll: () -> Unit,
    onBackToCooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmStopAll by rememberSaveable { mutableStateOf(false) }

    val featured = state.timers
        .firstOrNull { it.isRunning && !it.isFinished }
        ?: state.timers.firstOrNull { !it.isFinished }
        ?: state.timers.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KitchenQuestDimens.ScreenPadding)
    ) {
        KitchenQuestTopBar(
            title = "Timers",
            onBack = onBackToCooking,
            actions = {
                TextButton(
                    onClick = { confirmStopAll = true },
                    enabled = state.timers.isNotEmpty()
                ) {
                    Text(
                        text = "Stop all",
                        color = if (state.timers.isNotEmpty()) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        if (featured == null) {
            KitchenQuestEmptyState(
                title = "No active timers",
                message = "Start a timer from a recipe step or from the Kitchen Timer."
            )

            Spacer(modifier = Modifier.weight(1f))

            KitchenQuestPrimaryButton(
                text = if (state.recipe != null) "Back to cooking" else "Back",
                onClick = onBackToCooking,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            FeaturedTimer(
                timer = featured,
                onAddMinute = { onAddMinute(featured.id) },
                onTogglePause = { onTogglePause(featured.id) },
                onCancel = { onCancel(featured.id) }
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

            Text(
                text = "All timers",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    KitchenQuestDimens.SmallSpacing
                )
            ) {
                items(
                    items = state.timers,
                    key = { it.id }
                ) { timer ->
                    TimerCard(timer = timer)
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

            KitchenQuestPrimaryButton(
                text = if (state.recipe != null) "Back to cooking" else "Back",
                onClick = onBackToCooking,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (confirmStopAll) {
        AlertDialog(
            onDismissRequest = { confirmStopAll = false },
            title = { Text("Stop all timers?") },
            text = {
                Text("All active and completed timers will be removed from this list.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmStopAll = false
                        onStopAll()
                    }
                ) {
                    Text(
                        text = "Stop all",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStopAll = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FeaturedTimer(
    timer: CookingTimer,
    onAddMinute: () -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val progress = if (timer.totalSeconds > 0) {
            timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
        } else {
            0f
        }

        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeWidth = 10.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (timer.isFinished) {
                        "00:00"
                    } else {
                        formatSeconds(timer.remainingSeconds)
                    },
                    style = MaterialTheme.typography.displayMedium
                )

                Text(
                    text = timer.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                KitchenQuestDimens.SmallSpacing
            )
        ) {
            KitchenQuestSecondaryButton(
                text = "+1 min",
                onClick = onAddMinute,
                enabled = !timer.isFinished,
                modifier = Modifier.weight(1f)
            )

            KitchenQuestSecondaryButton(
                text = when {
                    timer.isFinished -> "Finished"
                    timer.isRunning -> "Pause"
                    else -> "Resume"
                },
                onClick = onTogglePause,
                enabled = !timer.isFinished,
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(KitchenQuestDimens.ButtonHeight),
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                border = BorderStroke(
                    KitchenQuestDimens.CardBorderWidth,
                    MaterialTheme.colorScheme.error
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: CookingTimer
) {
    val progress = if (timer.totalSeconds > 0) {
        timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
    } else {
        0f
    }

    KitchenQuestCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.label,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = when {
                        timer.isFinished -> "Finished"
                        timer.sourceType == TimerSource.RECIPE -> "From recipe"
                        else -> "Standalone"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (timer.isFinished) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = if (timer.isFinished) {
                    "00:00"
                } else {
                    formatSeconds(timer.remainingSeconds)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (timer.isFinished) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = if (timer.isFinished) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
