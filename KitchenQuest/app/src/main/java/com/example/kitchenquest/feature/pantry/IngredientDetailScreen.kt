package com.example.kitchenquest.feature.pantry

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenRed

@Composable
fun IngredientDetailScreen(
    item: PantryItemDto,
    onEdit: () -> Unit,
    onMarkFinished: () -> Unit,
    onFindRecipes: () -> Unit
) {
    val days = daysUntilExpiry(item)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(KitchenQuestDimens.ScreenPadding)
    ) {
        Text(text = item.ingredientName, style = MaterialTheme.typography.headlineSmall)
        Text(text = item.category, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

        DetailRow(label = "Quantity", value = "${item.quantity} ${item.unit}")

        val expiryText = when {
            item.expiryDate == null -> "No expiry set"
            days == null -> item.expiryDate
            days < 0 -> "Expired"
            days == 0L -> "Expires today"
            days == 1L -> "Expires tomorrow"
            else -> "$days days left"
        }
        DetailRow(
            label = "Expiry date",
            value = item.expiryDate ?: "Not set",
            valueColor = if (days != null && days <= 3) KitchenRed else MaterialTheme.colorScheme.onSurface
        )
        DetailRow(label = "Status", value = expiryText)

        Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

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

        Spacer(modifier = Modifier.height(KitchenQuestDimens.FieldSpacing))

        KitchenQuestSecondaryButton(
            text = "Edit ingredient",
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, color = valueColor)
    }
}