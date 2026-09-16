package com.example.kitchenquest.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KitchenQuestOrange = Color(0xFFFF7A1A)
private val SelectedGreen = Color(0xFFE4F4EB)

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDietaryPreferences by remember {
        mutableStateOf(setOf("No restrictions"))
    }

    var avoidedIngredients by remember {
        mutableStateOf(emptySet<String>())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .width(40.dp)
                    .height(3.dp)
                    .background(
                        color = KitchenQuestOrange,
                        shape = RoundedCornerShape(50)
                    )
            )

            TextButton(
                onClick = onSkip
            ) {
                Text(
                    text = "Skip",
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tell us how you eat",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll filter every recipe and recommendation to match. You can change this any time in Settings.",
            fontSize = 14.sp,
            color = Color.Gray,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Dietary preference",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DietaryChip(
                text = "No restrictions",
                selected = "No restrictions" in selectedDietaryPreferences,
                onClick = {
                    selectedDietaryPreferences = setOf("No restrictions")
                }
            )

            DietaryChip(
                text = "Vegetarian",
                selected = "Vegetarian" in selectedDietaryPreferences,
                onClick = {
                    selectedDietaryPreferences =
                        updateDietarySelection(
                            currentSelection = selectedDietaryPreferences,
                            preference = "Vegetarian"
                        )
                }
            )

            DietaryChip(
                text = "Vegan",
                selected = "Vegan" in selectedDietaryPreferences,
                onClick = {
                    selectedDietaryPreferences =
                        updateDietarySelection(
                            currentSelection = selectedDietaryPreferences,
                            preference = "Vegan"
                        )
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DietaryChip(
                text = "Halal",
                selected = "Halal" in selectedDietaryPreferences,
                onClick = {
                    selectedDietaryPreferences =
                        updateDietarySelection(
                            currentSelection = selectedDietaryPreferences,
                            preference = "Halal"
                        )
                }
            )

            DietaryChip(
                text = "Gluten-free",
                selected = "Gluten-free" in selectedDietaryPreferences,
                onClick = {
                    selectedDietaryPreferences =
                        updateDietarySelection(
                            currentSelection = selectedDietaryPreferences,
                            preference = "Gluten-free"
                        )
                }
            )

            DietaryChip(
                text = "Dairy-free",
                selected = "Dairy-free" in selectedDietaryPreferences,
                onClick = {
                    selectedDietaryPreferences =
                        updateDietarySelection(
                            currentSelection = selectedDietaryPreferences,
                            preference = "Dairy-free"
                        )
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Avoid these ingredients",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvoidIngredientChip(
                text = "Nuts",
                selected = "Nuts" in avoidedIngredients,
                onClick = {
                    avoidedIngredients =
                        toggleSelection(avoidedIngredients, "Nuts")
                }
            )

            AvoidIngredientChip(
                text = "Shellfish",
                selected = "Shellfish" in avoidedIngredients,
                onClick = {
                    avoidedIngredients =
                        toggleSelection(avoidedIngredients, "Shellfish")
                }
            )

            AvoidIngredientChip(
                text = "Eggs",
                selected = "Eggs" in avoidedIngredients,
                onClick = {
                    avoidedIngredients =
                        toggleSelection(avoidedIngredients, "Eggs")
                }
            )

            AvoidIngredientChip(
                text = "Milk",
                selected = "Milk" in avoidedIngredients,
                onClick = {
                    avoidedIngredients =
                        toggleSelection(avoidedIngredients, "Milk")
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AvoidIngredientChip(
                text = "Soy",
                selected = "Soy" in avoidedIngredients,
                onClick = {
                    avoidedIngredients =
                        toggleSelection(avoidedIngredients, "Soy")
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KitchenQuestOrange
            )
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DietaryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(text = text)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFFEEE2),
            selectedLabelColor = KitchenQuestOrange
        )
    )
}

@Composable
private fun AvoidIngredientChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(text = text)
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SelectedGreen,
            selectedLabelColor = Color(0xFF347A55)
        )
    )
}

private fun updateDietarySelection(
    currentSelection: Set<String>,
    preference: String
): Set<String> {
    val updatedSelection = currentSelection - "No restrictions"

    return if (preference in updatedSelection) {
        val result = updatedSelection - preference

        if (result.isEmpty()) {
            setOf("No restrictions")
        } else {
            result
        }
    } else {
        updatedSelection + preference
    }
}

private fun toggleSelection(
    currentSelection: Set<String>,
    item: String
): Set<String> {
    return if (item in currentSelection) {
        currentSelection - item
    } else {
        currentSelection + item
    }
}