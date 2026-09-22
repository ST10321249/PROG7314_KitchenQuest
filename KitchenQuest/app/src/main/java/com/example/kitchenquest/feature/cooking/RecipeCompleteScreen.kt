package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun RecipeCompleteScreen(
    recipeTitle: String,
    onSave: (rating: Int?, difficulty: String?, note: String?) -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    var difficulty by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(KitchenQuestDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Meal complete!", style = MaterialTheme.typography.headlineSmall)
        Text(text = recipeTitle, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text(text = "How was it?", style = MaterialTheme.typography.titleSmall)

        Row {
            (1..5).forEach { star ->
                IconButton(onClick = { rating = star }) {
                    Icon(
                        imageVector = if (star <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Rate $star stars"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        Text(text = "How difficult was it?", style = MaterialTheme.typography.titleSmall)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        Row {
            listOf("Easy", "Medium", "Hard").forEach { level ->
                KitchenQuestChoiceChip(
                    text = level,
                    selected = difficulty == level,
                    onClick = { difficulty = level }
                )
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Private note for next time") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        KitchenQuestPrimaryButton(
            text = "Save",
            onClick = {
                onSave(
                    rating.takeIf { it > 0 },
                    difficulty,
                    note.trim().takeIf { it.isNotBlank() }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}