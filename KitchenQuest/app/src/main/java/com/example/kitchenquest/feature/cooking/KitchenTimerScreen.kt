package com.example.kitchenquest.feature.cooking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

private val PRESETS = listOf(1, 3, 5, 10, 15, 20, 30, 45)

@Composable
fun KitchenTimerScreen(
    onStartTimer: (minutes: Int, label: String) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(10) }

    Column(
        modifier = Modifier.fillMaxSize().padding(KitchenQuestDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Kitchen timer", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text(text = "$selectedMinutes:00", style = MaterialTheme.typography.displayLarge)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text(text = "QUICK PRESETS", style = MaterialTheme.typography.labelMedium)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing),
            verticalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
        ) {
            items(PRESETS) { minutes ->
                KitchenQuestChoiceChip(
                    text = "$minutes min",
                    selected = selectedMinutes == minutes,
                    onClick = { selectedMinutes = minutes }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        KitchenQuestPrimaryButton(
            text = "Start timer",
            onClick = { onStartTimer(selectedMinutes, "Kitchen Timer") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}