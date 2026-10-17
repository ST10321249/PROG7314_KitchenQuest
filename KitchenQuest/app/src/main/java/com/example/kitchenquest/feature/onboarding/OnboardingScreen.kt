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
import com.example.kitchenquest.ui.theme.KitchenGreenLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    initialSelection: OnboardingSelection = OnboardingSelection(),
    existingPreferencesMessage: String? = null,
    onContinue: (OnboardingSelection) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dietary by rememberSaveable {
        mutableStateOf(initialSelection.dietaryPreferences.toList())
    }
    var avoided by rememberSaveable {
        mutableStateOf(initialSelection.avoidedIngredients.toList())
    }

    val dietarySet = dietary.toSet()
    val avoidedSet = avoided.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(KitchenQuestDimens.ScreenPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.TinySpacing)
            ) {
                Surface(
                    modifier = Modifier
                        .height(KitchenQuestDimens.TinySpacing)
                        .weight(1f),
                    color = MaterialTheme.colorScheme.outline
                ) {}
                Surface(
                    modifier = Modifier
                        .height(KitchenQuestDimens.TinySpacing)
                        .weight(1f),
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }

            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text(
            text = "Tell us how you eat",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        Text(
            text = "We'll filter every recipe and recommendation to match. You can change this any time in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        existingPreferencesMessage?.let { message ->
            Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                color = KitchenGreenLight
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text("Dietary preference", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing),
            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
        ) {
            PreferenceOptions.optionsWith(PreferenceOptions.dietary, dietarySet).forEach { option ->
                KitchenQuestChoiceChip(
                    text = option,
                    selected = option in dietarySet,
                    onClick = {
                        dietary = PreferenceOptions
                            .toggleDietary(dietarySet, option)
                            .toList()
                    }
                )
            }
        }

        Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text("Avoid these ingredients", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(KitchenQuestDimens.SmallSpacing))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing),
            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
        ) {
            PreferenceOptions.optionsWith(PreferenceOptions.avoidedIngredients, avoidedSet).forEach { option ->
                KitchenQuestChoiceChip(
                    text = option,
                    selected = option in avoidedSet,
                    onClick = {
                        avoided = PreferenceOptions
                            .toggleAvoided(avoidedSet, option)
                            .toList()
                    }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        KitchenQuestPrimaryButton(
            text = "Continue",
            onClick = {
                onContinue(
                    OnboardingSelection(
                        dietaryPreferences = dietarySet,
                        avoidedIngredients = avoidedSet
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
