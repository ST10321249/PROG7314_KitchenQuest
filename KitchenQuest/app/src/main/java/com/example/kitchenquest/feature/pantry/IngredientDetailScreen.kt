package com.example.kitchenquest.feature.pantry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.theme.KitchenGreen
import com.example.kitchenquest.ui.theme.KitchenOrangeLight
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRed

@Composable
fun IngredientDetailScreen(
    item: PantryItemDto,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMarkFinished: () -> Unit,
    onFindRecipes: () -> Unit
) {
    val days = daysUntilExpiry(item)
    val expiryStatus = when {
        item.expiryDate == null -> "No expiry set"
        days == null -> item.expiryDate
        days < 0 -> "Expired"
        days == 0L -> "Expires today"
        days == 1L -> "Expires tomorrow"
        else -> "$days days left"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit ingredient")
                        }
                    }
                }

                Spacer(Modifier.height(KitchenQuestDimens.MediumSpacing))

                Surface(
                    modifier = Modifier
                        .size(88.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = KitchenOrangeLight
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("🥕", style = MaterialTheme.typography.displayMedium)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(KitchenQuestDimens.ScreenPadding)
        ) {
            Text(item.ingredientName, style = MaterialTheme.typography.headlineMedium)
            Text(item.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(KitchenQuestDimens.SectionSpacing))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(KitchenQuestDimens.MediumSpacing)) {
                    DetailRow("Quantity", "${formatQuantity(item.quantity)} ${item.unit}")
                    DetailRow("Expiry date", item.expiryDate ?: "Not set")
                    DetailRow(
                        "Expiry reminder",
                        expiryStatus,
                        if (days != null && days <= 3) KitchenRed else KitchenGreen
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.FieldSpacing)) {
                KitchenQuestSecondaryButton(
                    text = "Mark finished",
                    onClick = onMarkFinished,
                    modifier = Modifier.weight(1f)
                )
                KitchenQuestPrimaryButton(
                    text = "Find recipes",
                    onClick = onFindRecipes,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = KitchenQuestDimens.SmallSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
