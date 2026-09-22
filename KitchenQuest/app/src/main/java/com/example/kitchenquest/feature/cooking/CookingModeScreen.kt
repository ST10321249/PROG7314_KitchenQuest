package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun CookingModeScreen(
    state: CookingUiState,
    onExit: () -> Unit,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onStartStepTimer: (Int) -> Unit,
    onViewAllTimers: () -> Unit,
    onFinish: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.recipe == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.errorMessage != null && state.recipe == null -> {
                Text(
                    text = state.errorMessage,
                    modifier = Modifier.align(Alignment.Center).padding(KitchenQuestDimens.ScreenPadding)
                )
            }

            state.recipe != null && state.recipe.steps.isNotEmpty() -> {
                val recipe = state.recipe
                val step = recipe.steps[state.currentStepIndex]
                val isLastStep = state.currentStepIndex == recipe.steps.size - 1

                Column(modifier = Modifier.fillMaxSize().padding(KitchenQuestDimens.ScreenPadding)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onExit) {
                            Icon(Icons.Filled.Close, contentDescription = "Exit cooking mode")
                        }
                        Text(text = recipe.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

                    Text(
                        text = "STEP ${state.currentStepIndex + 1} OF ${recipe.steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                    Text(text = step.instruction, style = MaterialTheme.typography.headlineSmall)

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

                    Text(text = "Start a timer for this step", style = MaterialTheme.typography.titleSmall)

                    Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                    Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)) {
                        listOf(1, 5, 10, 15).forEach { minutes ->
                            KitchenQuestSecondaryButton(
                                text = "$minutes min",
                                onClick = { onStartStepTimer(minutes) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (state.timers.isNotEmpty()) {
                        Surface(
                            onClick = onViewAllTimers,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(KitchenQuestDimens.MediumSpacing),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${state.timers.size} active timer(s)")
                                Text(text = "View all")
                            }
                        }
                        Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)) {
                        KitchenQuestSecondaryButton(
                            text = "Previous",
                            onClick = onPreviousStep,
                            modifier = Modifier.weight(1f),
                            enabled = state.currentStepIndex > 0
                        )
                        KitchenQuestPrimaryButton(
                            text = if (isLastStep) "Finish" else "Next step",
                            onClick = { if (isLastStep) onFinish() else onNextStep() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = "This recipe doesn't have step-by-step instructions.",
                    modifier = Modifier.align(Alignment.Center).padding(KitchenQuestDimens.ScreenPadding)
                )
            }
        }
    }
}