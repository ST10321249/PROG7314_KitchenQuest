package com.example.kitchenquest.feature.cooking

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestCard
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCompleteScreen(
    recipeTitle: String,
    readyInMinutes: Int?,
    onSave: (rating: Int?, difficulty: String?, note: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var difficulty by rememberSaveable { mutableStateOf<String?>(null) }
    var note by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KitchenQuestDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        Surface(
            shape = RoundedCornerShape(KitchenQuestDimens.ExtraLargeCorner),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(78.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        Text(
            text = "Meal complete!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        val subtitle = buildString {
            append(recipeTitle)
            readyInMinutes?.takeIf { it > 0 }?.let {
                append(" · ")
                append(it)
                append(" minutes")
            }
        }

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        KitchenQuestCard {
            Text(
                text = "How was it?",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = "Optional",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = {
                            rating = if (rating == star) 0 else star
                        },
                        modifier = Modifier.size(KitchenQuestDimens.TouchTarget)
                    ) {
                        Icon(
                            imageVector = if (star <= rating) {
                                Icons.Filled.Star
                            } else {
                                Icons.Filled.StarBorder
                            },
                            contentDescription = "$star star rating",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

            Text(
                text = "HOW DIFFICULT WAS IT?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    KitchenQuestDimens.TinySpacing
                )
            ) {
                listOf("Easy", "Medium", "Hard").forEach { level ->
                    KitchenQuestChoiceChip(
                        text = level,
                        selected = difficulty == level,
                        onClick = {
                            difficulty = if (difficulty == level) {
                                null
                            } else {
                                level
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

            Text(
                text = "PRIVATE NOTE FOR NEXT TIME",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 6,
                placeholder = {
                    Text(
                        "What would you change next time?"
                    )
                },
                shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text(
            text = "Rating, difficulty and notes are all optional.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        KitchenQuestPrimaryButton(
            text = "Save & finish",
            onClick = {
                onSave(
                    rating.takeIf { it > 0 },
                    difficulty,
                    note.trim().takeIf { it.isNotBlank() }
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))
    }
}
