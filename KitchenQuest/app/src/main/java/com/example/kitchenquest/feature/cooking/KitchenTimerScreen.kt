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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

private val PRESETS = listOf(1, 3, 5, 10, 15, 20, 30, 45)
private const val MAX_DIAL_MINUTES = 60

@Composable
fun KitchenTimerScreen(
    onBack: () -> Unit,
    onStartTimer: (minutes: Int, label: String) -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(10) }
    var customMinutesText by remember { mutableStateOf("10") }

    fun applyMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        selectedMinutes = clamped
        customMinutesText = clamped.toString()
    }

    Column(modifier = Modifier.fillMaxSize()) {

        KitchenQuestTopBar(
            title = "Kitchen Timer",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KitchenQuestDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                CircularProgressIndicator(
                    progress = { (selectedMinutes.toFloat() / MAX_DIAL_MINUTES).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 14.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$selectedMinutes", style = MaterialTheme.typography.displayLarge)
                    Text(
                        text = if (selectedMinutes == 1) "minute" else "minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

            KitchenQuestTextField(
                value = customMinutesText,
                onValueChange = { text ->
                    customMinutesText = text
                    text.toIntOrNull()?.let { selectedMinutes = it.coerceIn(1, 180) }
                },
                label = "Custom minutes",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth()
            )

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
                        onClick = { applyMinutes(minutes) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            KitchenQuestPrimaryButton(
                text = "Start timer",
                onClick = { onStartTimer(selectedMinutes, "Kitchen Timer") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))
        }
    }
}
