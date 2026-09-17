package com.example.kitchenquest.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

@Composable
fun KitchenQuestPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(
                KitchenQuestDimens.ButtonHeight
            ),
        enabled = enabled,
        shape = RoundedCornerShape(
            KitchenQuestDimens.MediumCorner
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun KitchenQuestSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(
                KitchenQuestDimens.ButtonHeight
            ),
        enabled = enabled,
        shape = RoundedCornerShape(
            KitchenQuestDimens.MediumCorner
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun KitchenQuestTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation =
        VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = {
            Text(
                text = label
            )
        },
        singleLine = true,
        isError = isError,
        supportingText =
            if (supportingText != null) {
                {
                    Text(
                        text = supportingText
                    )
                }
            } else {
                null
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = keyboardType
            ),
        visualTransformation =
            visualTransformation,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(
            KitchenQuestDimens.MediumCorner
        )
    )
}

@Composable
fun KitchenQuestChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text
            )
        },
        modifier = modifier.padding(
            end = KitchenQuestDimens.SmallSpacing
        ),
        shape = RoundedCornerShape(
            KitchenQuestDimens.ChipCorner
        ),
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor =
                    MaterialTheme.colorScheme
                        .primaryContainer,
                selectedLabelColor =
                    MaterialTheme.colorScheme
                        .onPrimaryContainer
            )
    )
}