package com.example.kitchenquest.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialSelection: OnboardingSelection =
        OnboardingSelection(),
    existingPreferencesMessage: String? = null,
    onContinue: (
        OnboardingSelection
    ) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {

    val dietaryOptions =
        PreferenceOptions.dietary

    val avoidedIngredientOptions =
        PreferenceOptions.avoidedIngredients

    var selectedDietaryPreferences
            by rememberSaveable {
                mutableStateOf(
                    initialSelection
                        .dietaryPreferences
                )
            }

    var selectedAvoidedIngredients
            by rememberSaveable {
                mutableStateOf(
                    initialSelection
                        .avoidedIngredients
                )
            }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                KitchenQuestDimens
                    .ScreenPadding
            )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "KitchenQuest",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            TextButton(
                onClick = onSkip
            ) {
                Text(
                    text = "Skip"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SmallSpacing
            )
        )

        Text(
            text =
                "Tell us how you eat",
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SmallSpacing
            )
        )

        Text(
            text =
                "We'll filter recipe and recommendation results to match your preferences.",
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        if (
            existingPreferencesMessage != null
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        KitchenQuestDimens
                            .MediumSpacing
                    )
            )

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer,
                shape =
                    RoundedCornerShape(
                        KitchenQuestDimens
                            .MediumCorner
                    )
            ) {

                Text(
                    text =
                        existingPreferencesMessage,
                    modifier =
                        Modifier.padding(
                            KitchenQuestDimens
                                .MediumSpacing
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onPrimaryContainer
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SectionSpacing
            )
        )

        Text(
            text =
                "Dietary preference",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SmallSpacing
            )
        )

        FlowRow {

            dietaryOptions.forEach {
                    option ->

                KitchenQuestChoiceChip(
                    text = option,
                    selected =
                        option in
                                selectedDietaryPreferences,
                    onClick = {

                        selectedDietaryPreferences =
                            PreferenceOptions
                                .toggleDietary(
                                    selectedDietaryPreferences,
                                    option
                                )
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SectionSpacing
            )
        )

        Text(
            text =
                "Avoid these ingredients",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier = Modifier.height(
                KitchenQuestDimens
                    .SmallSpacing
            )
        )

        FlowRow {

            avoidedIngredientOptions
                .forEach {
                        ingredient ->

                    KitchenQuestChoiceChip(
                        text =
                            ingredient,
                        selected =
                            ingredient in
                                    selectedAvoidedIngredients,
                        onClick = {

                            selectedAvoidedIngredients =
                                PreferenceOptions
                                    .toggleAvoided(
                                        selectedAvoidedIngredients,
                                        ingredient
                                    )
                        }
                    )
                }
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        KitchenQuestPrimaryButton(
            text = "Continue",
            onClick = {
                onContinue(
                    OnboardingSelection(
                        dietaryPreferences =
                            selectedDietaryPreferences,
                        avoidedIngredients =
                            selectedAvoidedIngredients
                    )
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )
    }
}