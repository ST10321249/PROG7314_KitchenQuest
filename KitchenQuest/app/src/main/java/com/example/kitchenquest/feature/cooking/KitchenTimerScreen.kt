package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

private val TimerPresets = listOf(1, 3, 5, 10, 15, 20, 30, 45)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KitchenTimerScreen(
    onBack: () -> Unit,
    onStartTimer: (minutes: Int, label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMinutes by rememberSaveable { mutableStateOf(10) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KitchenQuestDimens.ScreenPadding)
    ) {
        KitchenQuestTopBar(
            title = stringResource(R.string.kitchentimer_kitchen_timer_829429),
            onBack = onBack
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        Box(
            modifier = Modifier
                .size(194.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                strokeWidth = 10.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "%02d:00".format(selectedMinutes),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(KitchenQuestDimens.TouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.kitchentimer_edit_timer_duration_3c558b),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        Text(
            text = stringResource(R.string.kitchentimer_quick_presets_4fe651),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                KitchenQuestDimens.TinySpacing
            ),
            verticalArrangement = Arrangement.spacedBy(
                KitchenQuestDimens.SmallSpacing
            )
        ) {
            TimerPresets.forEach { minutes ->
                KitchenQuestChoiceChip(
                    text = stringResource(R.string.ktimer_minutes, minutes),
                    selected = selectedMinutes == minutes,
                    onClick = { selectedMinutes = minutes }
                )
            }
        }

        Spacer(modifier = Modifier.height(KitchenQuestDimens.LargeSpacing))

        KitchenQuestPrimaryButton(
            text = stringResource(R.string.kitchentimer_start_timer_54a06d),
            onClick = {
                onStartTimer(
                    selectedMinutes,
                    "Kitchen Timer"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showEditDialog) {
        EditTimerDialog(
            currentMinutes = selectedMinutes,
            onDismiss = { showEditDialog = false },
            onConfirm = { minutes ->
                selectedMinutes = minutes
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditTimerDialog(
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by rememberSaveable(currentMinutes) {
        mutableStateOf(currentMinutes.toString())
    }

    val value = text.toIntOrNull()
    val valid = value != null && value in 1..180

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.kitchentimer_set_timer_84719a))
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        text = input.filter(Char::isDigit).take(3)
                    },
                    label = { Text(stringResource(R.string.kitchentimer_minutes_f670ea)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(KitchenQuestDimens.SmallSpacing))

                Text(
                    text = stringResource(R.string.kitchentimer_choose_between_1_and_180_minutes_3e42b0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value ?: currentMinutes) },
                enabled = valid
            ) {
                Text(stringResource(R.string.kitchentimer_set_5d5b78))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.kitchentimer_cancel_ea4788))
            }
        }
    )
}
