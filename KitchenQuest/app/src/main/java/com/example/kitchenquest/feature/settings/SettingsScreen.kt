package com.example.kitchenquest.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kitchenquest.feature.onboarding.PreferenceOptions
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestSecondaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens
import com.example.kitchenquest.ui.theme.KitchenQuestTheme

private enum class SettingsDialog {
    Name,
    Dietary,
    Avoided
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onDietaryPreferencesChange: (Set<String>) -> Unit,
    onAvoidedIngredientsChange: (Set<String>) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        KitchenQuestTopBar(
            title = "Settings",
            onBack = onBack,
            modifier = Modifier.padding(KitchenQuestDimens.ScreenPadding)
        )

        when {
            // Also covers the first frame, before loading has started.
            !state.hasLoaded && (state.isLoading || state.errorMessage == null) -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            !state.hasLoaded -> {
                LoadFailed(
                    message = state.errorMessage,
                    onRetry = onRetry
                )
            }

            else -> {
                SettingsContent(
                    state = state,
                    onDisplayNameChange = onDisplayNameChange,
                    onDietaryPreferencesChange = onDietaryPreferencesChange,
                    onAvoidedIngredientsChange = onAvoidedIngredientsChange,
                    onSave = onSave
                )
            }
        }

        SignOutButton(
            onClick = onSignOut,
            modifier = Modifier.padding(
                horizontal = KitchenQuestDimens.MediumSpacing,
                vertical = KitchenQuestDimens.SectionSpacing
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onDisplayNameChange: (String) -> Unit,
    onDietaryPreferencesChange: (Set<String>) -> Unit,
    onAvoidedIngredientsChange: (Set<String>) -> Unit,
    onSave: () -> Unit
) {
    var openDialog by rememberSaveable {
        mutableStateOf<SettingsDialog?>(null)
    }

    Column(
        modifier = Modifier.padding(
            horizontal = KitchenQuestDimens.MediumSpacing
        )
    ) {

        SectionHeader("ACCOUNT")

        SettingsCard {
            SettingsRow(
                icon = Icons.Outlined.Person,
                title = state.displayName.ifBlank { "Add your name" },
                subtitle = state.email.ifBlank { "No email on this account" },
                onClick = { openDialog = SettingsDialog.Name }
            )
        }

        SectionHeader("PREFERENCES")

        SettingsCard {
            SettingsRow(
                icon = Icons.Outlined.Restaurant,
                title = "Dietary preferences",
                subtitle = PreferenceOptions.summary(
                    state.dietaryPreferences,
                    PreferenceOptions.NO_RESTRICTIONS
                ),
                onClick = { openDialog = SettingsDialog.Dietary }
            )

            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = KitchenQuestDimens.MediumSpacing
                ),
                color = MaterialTheme.colorScheme.outline
            )

            SettingsRow(
                icon = Icons.Outlined.Block,
                title = "Avoided ingredients",
                subtitle = PreferenceOptions.summary(
                    state.avoidedIngredients,
                    "None"
                ),
                onClick = { openDialog = SettingsDialog.Avoided }
            )
        }

        Feedback(state)

        if (state.hasUnsavedChanges || state.isSaving) {
            KitchenQuestPrimaryButton(
                text = if (state.isSaving) "Saving..." else "Save changes",
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KitchenQuestDimens.MediumSpacing)
            )
        }
    }

    when (openDialog) {
        SettingsDialog.Name -> NameDialog(
            initial = state.displayName,
            onConfirm = {
                onDisplayNameChange(it)
                openDialog = null
            },
            onDismiss = { openDialog = null }
        )

        SettingsDialog.Dietary -> ChoiceDialog(
            title = "Dietary preferences",
            options = PreferenceOptions.optionsWith(
                PreferenceOptions.dietary,
                state.dietaryPreferences
            ),
            selected = state.dietaryPreferences,
            onToggle = {
                onDietaryPreferencesChange(
                    PreferenceOptions.toggleDietary(
                        state.dietaryPreferences,
                        it
                    )
                )
            },
            onDismiss = { openDialog = null }
        )

        SettingsDialog.Avoided -> ChoiceDialog(
            title = "Avoided ingredients",
            options = PreferenceOptions.optionsWith(
                PreferenceOptions.avoidedIngredients,
                state.avoidedIngredients
            ),
            selected = state.avoidedIngredients,
            onToggle = {
                onAvoidedIngredientsChange(
                    PreferenceOptions.toggleAvoided(
                        state.avoidedIngredients,
                        it
                    )
                )
            },
            onDismiss = { openDialog = null }
        )

        null -> Unit
    }
}

@Composable
private fun SectionHeader(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .padding(
                top = KitchenQuestDimens.SectionSpacing,
                bottom = KitchenQuestDimens.SmallSpacing
            )
            .semantics { heading() }
    )
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(
                horizontal = KitchenQuestDimens.MediumSpacing,
                vertical = KitchenQuestDimens.FieldSpacing
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            shape = RoundedCornerShape(11.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = KitchenQuestDimens.FieldSpacing)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Feedback(
    state: SettingsUiState
) {
    val message = state.errorMessage

    when {
        message != null -> Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .padding(top = KitchenQuestDimens.MediumSpacing)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )

        state.saveSucceeded -> Text(
            text = "Changes saved",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(top = KitchenQuestDimens.MediumSpacing)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
private fun LoadFailed(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(KitchenQuestDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            KitchenQuestDimens.MediumSpacing
        )
    ) {
        Text(
            text = message ?: "Your settings couldn't be loaded.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
            }
        )

        KitchenQuestSecondaryButton(
            text = "Try again",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(KitchenQuestDimens.ButtonHeight),
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Text(
            text = "Sign out",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun NameDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display name") },
        text = {
            KitchenQuestTextField(
                value = text,
                onValueChange = { text = it },
                label = "Display name",
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow {
                options.forEach { option ->
                    KitchenQuestChoiceChip(
                        text = option,
                        selected = option in selected,
                        onClick = { onToggle(option) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

private val previewState = SettingsUiState(
    hasLoaded = true,
    displayName = "Akeev",
    email = "akeev@example.com",
    dietaryPreferences = setOf("Vegetarian", "Gluten-free"),
    avoidedIngredients = setOf("Nuts", "Shellfish")
)

@Composable
private fun PreviewScreen(state: SettingsUiState) {
    KitchenQuestTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                state = state,
                onBack = {},
                onDisplayNameChange = {},
                onDietaryPreferencesChange = {},
                onAvoidedIngredientsChange = {},
                onSave = {},
                onRetry = {},
                onSignOut = {}
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Loaded")
@Composable
private fun SettingsScreenLoadedPreview() {
    PreviewScreen(previewState)
}

@Preview(showBackground = true, heightDp = 800, name = "Unsaved changes")
@Composable
private fun SettingsScreenUnsavedPreview() {
    PreviewScreen(previewState.copy(hasUnsavedChanges = true))
}

@Preview(showBackground = true, heightDp = 800, name = "Save failed")
@Composable
private fun SettingsScreenSaveFailedPreview() {
    PreviewScreen(
        previewState.copy(
            hasUnsavedChanges = true,
            errorMessage = "Can't reach the server. Check your connection and try again."
        )
    )
}

@Preview(showBackground = true, heightDp = 800, name = "Saved")
@Composable
private fun SettingsScreenSavedPreview() {
    PreviewScreen(previewState.copy(saveSucceeded = true))
}

@Preview(showBackground = true, heightDp = 800, name = "Loading")
@Composable
private fun SettingsScreenLoadingPreview() {
    PreviewScreen(SettingsUiState(isLoading = true))
}

@Preview(showBackground = true, heightDp = 800, name = "Load failed")
@Composable
private fun SettingsScreenLoadFailedPreview() {
    PreviewScreen(
        SettingsUiState(
            errorMessage = "Can't reach the server. Check your connection and try again."
        )
    )
}

@Preview(showBackground = true, heightDp = 800, name = "Dark")
@Composable
private fun SettingsScreenDarkPreview() {
    KitchenQuestTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                state = previewState,
                onBack = {},
                onDisplayNameChange = {},
                onDietaryPreferencesChange = {},
                onAvoidedIngredientsChange = {},
                onSave = {},
                onRetry = {},
                onSignOut = {}
            )
        }
    }
}
