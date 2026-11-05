package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.R
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
import androidx.compose.ui.res.stringResource
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
                    message = stringResource(R.string.cookingmode_loading_cooking_mode_7ca796),
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
                        text = stringResource(R.string.cookingmode_cooking_mode_couldn_t_be_opened_5db184),
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
                        text = stringResource(R.string.cookingmode_go_back_22b653),
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
                                    contentDescription = stringResource(R.string.cookingmode_exit_cooking_mode_767fd4)
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
                        text = stringResource(R.string.cooking_step_of, state.currentStepIndex + 1, recipe.steps.size),
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
                                text = stringResource(R.string.cookingmode_timer_for_this_step_536fe7),
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                text = stringResource(R.string.cookingmode_choose_a_duration_then_keep_cooking_while_it_runs_b72b7f),
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
                                        text = stringResource(R.string.cooking_minutes, minutes),
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
                            text = stringResource(R.string.cookingmode_previous_995e9d),
                            onClick = onPreviousStep,
                            enabled = state.currentStepIndex > 0,
                            modifier = Modifier.weight(1f)
                        )

                        KitchenQuestPrimaryButton(
                            text = if (isLastStep) {
                                stringResource(R.string.cookingmode_finish_6767d5)
                            } else {
                                stringResource(R.string.cookingmode_next_step_68397f)
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
                    text = stringResource(R.string.cookingmode_this_recipe_doesn_t_have_step_by_step_instructions_b2dae0),
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
            title = { Text(stringResource(R.string.cookingmode_exit_cooking_mode_893356)) },
            text = {
                Text(
                    stringResource(R.string.cookingmode_your_current_step_and_timers_will_stay_available_so_you_c_d09bb6)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        onExit()
                    }
                ) {
                    Text(stringResource(R.string.cookingmode_exit_fef46e))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(stringResource(R.string.cookingmode_keep_cooking_ee11d2))
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
                text = stringResource(R.string.cooking_active_timers, totalCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(onClick = onViewAll) {
                Text(stringResource(R.string.cookingmode_view_all_0b4db2))
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
