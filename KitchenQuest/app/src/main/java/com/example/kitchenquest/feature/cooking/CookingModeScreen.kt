package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

private val StepTimerPresets = listOf(1, 5, 10, 15)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CookingModeScreen(
    state: CookingUiState,
    onExit: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onStartStepTimer: (Int) -> Unit,
    onViewAllTimers: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    var selectedTimerMinutes by rememberSaveable { mutableIntStateOf(10) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.recipe == null -> {
                KitchenQuestLoadingState(
                    message = "Loading cooking mode...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            state.errorMessage != null && state.recipe == null -> {
                KitchenQuestCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KitchenQuestDimens.ScreenPadding)
                ) {
                    Text(
                        text = "Cooking mode couldn't be opened",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                    KitchenQuestSecondaryButton(
                        text = "Go back",
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            state.recipe != null && state.recipe.steps.isNotEmpty() -> {
                val recipe = state.recipe
                val step = recipe.steps[state.currentStepIndex]
                val isLastStep = state.currentStepIndex == recipe.steps.lastIndex
                val featuredTimer = state.timers
                    .firstOrNull { it.sourceType == TimerSource.RECIPE && !it.isFinished }
                    ?: state.timers.firstOrNull { !it.isFinished }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(KitchenQuestDimens.ScreenPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            IconButton(
                                onClick = { showExitConfirmation = true },
                                modifier = Modifier.size(KitchenQuestDimens.TouchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Exit cooking mode"
                                )
                            }
                        }

                        Text(
                            text = recipe.title,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = KitchenQuestDimens.SmallSpacing),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                    StepProgress(
                        currentStep = state.currentStepIndex,
                        totalSteps = recipe.steps.size
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                    Text(
                        text = "STEP ${state.currentStepIndex + 1} OF ${recipe.steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                    Text(
                        text = step.instruction,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

                    Surface(
                        shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            KitchenQuestDimens.CardBorderWidth,
                            MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(KitchenQuestDimens.CardPadding)
                        ) {
                            Text(
                                text = "Timer for this step",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                text = "Choose a duration, then keep cooking while it runs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(
                                    KitchenQuestDimens.TinySpacing
                                )
                            ) {
                                StepTimerPresets.forEach { minutes ->
                                    KitchenQuestChoiceChip(
                                        text = "$minutes min",
                                        selected = selectedTimerMinutes == minutes,
                                        onClick = { selectedTimerMinutes = minutes }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                            Button(
                                onClick = {
                                    onStartStepTimer(selectedTimerMinutes)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(KitchenQuestDimens.ButtonHeight),
                                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text(
                                    text = "Start %02d:00 timer".format(selectedTimerMinutes),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.LargeSpacing))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            KitchenQuestDimens.SmallSpacing
                        )
                    ) {
                        KitchenQuestSecondaryButton(
                            text = "‹ Previous",
                            onClick = onPreviousStep,
                            enabled = state.currentStepIndex > 0,
                            modifier = Modifier.weight(1f)
                        )

                        KitchenQuestPrimaryButton(
                            text = if (isLastStep) {
                                "Finish"
                            } else {
                                "Next step ›"
                            },
                            onClick = {
                                if (isLastStep) {
                                    onFinish()
                                } else {
                                    onNextStep()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (featuredTimer != null) {
                        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

                        ActiveTimerStrip(
                            timer = featuredTimer,
                            totalCount = state.timers.count { !it.isFinished },
                            onViewAll = onViewAllTimers
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = "This recipe doesn't have step-by-step instructions.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(KitchenQuestDimens.ScreenPadding),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Exit cooking mode?") },
            text = {
                Text(
                    "Your current step and timers will stay available so you can continue later."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        onExit()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Keep cooking")
                }
            }
        )
    }
}

@Composable
private fun StepProgress(
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.TinySpacing)
    ) {
        repeat(totalSteps.coerceAtLeast(1)) { index ->
            val color = when {
                index < currentStep -> MaterialTheme.colorScheme.primary
                index == currentStep -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        color = color,
                        shape = RoundedCornerShape(50.dp)
                    )
            )
        }
    }
}

@Composable
private fun ActiveTimerStrip(
    timer: CookingTimer,
    totalCount: Int,
    onViewAll: () -> Unit
) {
    val progress = if (timer.totalSeconds > 0) {
        timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat()
    } else {
        0f
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE TIMERS · $totalCount",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onViewAll) {
                Text("View all")
            }
        }

        Surface(
            onClick = onViewAll,
            shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = KitchenQuestDimens.SmallSpacing)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = timer.label,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = formatCookingTimer(timer.remainingSeconds),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

private fun formatCookingTimer(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
