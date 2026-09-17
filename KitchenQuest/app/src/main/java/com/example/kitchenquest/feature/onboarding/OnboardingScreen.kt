package com.example.kitchenquest.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {

    val dietaryOptions = listOf(
        "No restrictions",
        "Vegetarian",
        "Vegan",
        "Halal",
        "Gluten-free",
        "Dairy-free"
    )

    val avoidedIngredientOptions = listOf(
        "Nuts",
        "Shellfish",
        "Eggs",
        "Milk",
        "Soy"
    )

    var selectedDietaryPreferences
            by rememberSaveable {
                mutableStateOf(
                    setOf("No restrictions")
                )
            }

    var selectedAvoidedIngredients
            by rememberSaveable {
                mutableStateOf(
                    emptySet<String>()
                )
            }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                KitchenQuestDimens.ScreenPadding
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
                    MaterialTheme.typography
                        .titleMedium,
                color =
                    MaterialTheme.colorScheme
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
            modifier = Modifier.padding(
                KitchenQuestDimens
                    .SmallSpacing
            )
        )

        Text(
            text = "Tell us how you eat",
            style =
                MaterialTheme.typography
                    .headlineMedium
        )

        Text(
            text =
                "We'll filter recipe and recommendation results to match your preferences.",
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.padding(
                KitchenQuestDimens
                    .MediumSpacing
            )
        )

        Text(
            text = "Dietary preference",
            style =
                MaterialTheme.typography
                    .titleMedium
        )

        Spacer(
            modifier = Modifier.padding(
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

                        if (
                            option ==
                            "No restrictions"
                        ) {

                            selectedDietaryPreferences =
                                setOf(
                                    "No restrictions"
                                )

                        } else {

                            val updated =
                                selectedDietaryPreferences
                                    .toMutableSet()

                            updated.remove(
                                "No restrictions"
                            )

                            if (
                                option in updated
                            ) {
                                updated.remove(
                                    option
                                )
                            } else {
                                updated.add(
                                    option
                                )
                            }

                            selectedDietaryPreferences =
                                if (
                                    updated.isEmpty()
                                ) {
                                    setOf(
                                        "No restrictions"
                                    )
                                } else {
                                    updated
                                }
                        }
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.padding(
                KitchenQuestDimens
                    .MediumSpacing
            )
        )

        Text(
            text = "Avoid these ingredients",
            style =
                MaterialTheme.typography
                    .titleMedium
        )

        Spacer(
            modifier = Modifier.padding(
                KitchenQuestDimens
                    .SmallSpacing
            )
        )

        FlowRow {

            avoidedIngredientOptions.forEach {
                    ingredient ->

                KitchenQuestChoiceChip(
                    text = ingredient,
                    selected =
                        ingredient in
                                selectedAvoidedIngredients,
                    onClick = {

                        val updated =
                            selectedAvoidedIngredients
                                .toMutableSet()

                        if (
                            ingredient in updated
                        ) {
                            updated.remove(
                                ingredient
                            )
                        } else {
                            updated.add(
                                ingredient
                            )
                        }

                        selectedAvoidedIngredients =
                            updated
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        KitchenQuestPrimaryButton(
            text = "Continue",
            onClick = onContinue,
            modifier =
                Modifier.fillMaxWidth()
        )
    }
}