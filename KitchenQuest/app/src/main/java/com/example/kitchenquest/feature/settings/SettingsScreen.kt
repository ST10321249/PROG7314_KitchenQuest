package com.example.kitchenquest.feature.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
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
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.kitchenquest.data.auth.BiometricAvailability
import com.example.kitchenquest.data.auth.BiometricSettings
import com.example.kitchenquest.data.auth.BiometricSupport
import com.example.kitchenquest.feature.onboarding.PreferenceOptions
import com.example.kitchenquest.ui.components.KitchenQuestChoiceChip
import com.example.kitchenquest.ui.components.KitchenQuestErrorState
import com.example.kitchenquest.ui.components.KitchenQuestLoadingState
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.components.KitchenQuestTextField
import com.example.kitchenquest.ui.components.KitchenQuestTopBar
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

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
    onBiometricsEnabled: () -> Unit = {},
    userId: String,
    modifier: Modifier = Modifier
) {
    var showSignOutConfirmation by rememberSaveable {
        mutableStateOf(false)
    }
    // The preference belongs to this signed-in user on this device only.
    val context = LocalContext.current
    val biometricSettings = remember(context) { BiometricSettings(context) }
    var biometricAvailability by remember(userId) {
        mutableStateOf(BiometricSupport.check(context))
    }
    var biometricEnabled by remember(userId) {
        mutableStateOf(biometricSettings.isEnabled(userId))
    }
    var biometricFeedback by rememberSaveable(userId) { mutableStateOf<String?>(null) }
    var biometricFeedbackIsError by rememberSaveable(userId) { mutableStateOf(false) }

    val refreshBiometricStatus = {
        biometricAvailability = BiometricSupport.check(context)
        if (biometricAvailability != BiometricAvailability.READY) {
            biometricEnabled = false
            biometricSettings.setEnabled(userId, false)
        }
    }
    val changeBiometricEnabled: (Boolean) -> Unit = { enabled ->
        biometricFeedback = null
        biometricFeedbackIsError = false
        if (!enabled) {
            biometricSettings.setEnabled(userId, false)
            biometricEnabled = false
            biometricFeedback = "Biometric unlock preference turned off."
        } else if (userId.isBlank() || biometricAvailability != BiometricAvailability.READY) {
            biometricFeedback = "Sign in and set up biometrics on your device first."
            biometricFeedbackIsError = true
        } else {
            val activity = context as? FragmentActivity
            if (activity == null) {
                biometricFeedback = "Biometric setup is unavailable on this device."
                biometricFeedbackIsError = true
            } else {
                BiometricSetup(activity).confirmEnable { verified, message ->
                    if (verified) {
                        biometricSettings.setEnabled(userId, true)
                        biometricEnabled = true
                        onBiometricsEnabled()
                        biometricFeedback = "Biometrics enabled for this account."
                    } else {
                        biometricFeedback = message
                        biometricFeedbackIsError = true
                    }
                }
            }
        }
    }
    val openDeviceBiometricSettings = {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            context.startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(KitchenQuestDimens.ScreenPadding)
    ) {
        KitchenQuestTopBar(
            title = "Settings",
            onBack = onBack
        )

        when {
            !state.hasLoaded && state.isLoading -> {
                KitchenQuestLoadingState(
                    message = "Loading your settings...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            !state.hasLoaded && state.errorMessage != null -> {
                KitchenQuestErrorState(
                    message = state.errorMessage,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            !state.hasLoaded -> {
                KitchenQuestLoadingState(
                    message = "Loading your settings...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                SettingsContent(
                    state = state,
                    onDisplayNameChange = onDisplayNameChange,
                    onDietaryPreferencesChange = onDietaryPreferencesChange,
                    onAvoidedIngredientsChange = onAvoidedIngredientsChange,
                    onSave = onSave,
                    onSignOut = { showSignOutConfirmation = true },
                    biometricEnabled = biometricEnabled,
                    biometricAvailability = biometricAvailability,
                    biometricFeedback = biometricFeedback,
                    biometricFeedbackIsError = biometricFeedbackIsError,
                    onBiometricChange = changeBiometricEnabled,
                    onCheckBiometrics = refreshBiometricStatus,
                    onOpenDeviceBiometrics = openDeviceBiometricSettings
                )
            }
        }
    }

    if (showSignOutConfirmation) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmation = false },
            title = { Text("Sign out?") },
            text = {
                Text("You can sign back in at any time. Your saved KitchenQuest data will remain in your account.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmation = false
                        onSignOut()
                    }
                ) {
                    Text(
                        text = "Sign out",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmation = false }) {
                    Text("Cancel")
                }
            }
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
    onSave: () -> Unit,
    onSignOut: () -> Unit,
    biometricEnabled: Boolean,
    biometricAvailability: BiometricAvailability,
    biometricFeedback: String?,
    biometricFeedbackIsError: Boolean,
    onBiometricChange: (Boolean) -> Unit,
    onCheckBiometrics: () -> Unit,
    onOpenDeviceBiometrics: () -> Unit
) {
    var openDialog by rememberSaveable {
        mutableStateOf<SettingsDialog?>(null)
    }

    SectionHeader("ACCOUNT")

    SettingsGroup {
        SettingsRow(
            icon = Icons.Outlined.Person,
            title = state.displayName.ifBlank { "Add your name" },
            subtitle = state.email.ifBlank { "No email available" },
            onClick = { openDialog = SettingsDialog.Name }
        )
    }

    SectionHeader("SECURITY")

    SettingsGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .padding(KitchenQuestDimens.MediumSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(KitchenQuestDimens.SmallCorner),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
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
                Text("Biometric unlock", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when (biometricAvailability) {
                        BiometricAvailability.READY ->
                            if (biometricEnabled) "Enabled for this account" else "Verify with your fingerprint or face"
                        BiometricAvailability.NOT_ENROLLED -> "Add a fingerprint or face in your phone's settings first"
                        BiometricAvailability.NO_HARDWARE -> "This phone doesn't support biometric unlock"
                        BiometricAvailability.TEMPORARILY_UNAVAILABLE -> "Biometric sensor is temporarily unavailable"
                        BiometricAvailability.SECURITY_UPDATE_REQUIRED -> "Device security update needed"
                        BiometricAvailability.UNSUPPORTED -> "Biometric unlock isn't supported here"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = biometricEnabled,
                enabled = biometricEnabled || biometricAvailability == BiometricAvailability.READY,
                onCheckedChange = onBiometricChange
            )
        }

        if (biometricAvailability != BiometricAvailability.READY) {
            Row(
                modifier = Modifier.padding(
                    start = KitchenQuestDimens.MediumSpacing,
                    end = KitchenQuestDimens.MediumSpacing,
                    bottom = KitchenQuestDimens.SmallSpacing
                ),
                horizontalArrangement = Arrangement.spacedBy(KitchenQuestDimens.SmallSpacing)
            ) {
                if (biometricAvailability == BiometricAvailability.NOT_ENROLLED) {
                    TextButton(onClick = onOpenDeviceBiometrics) { Text("Set up on phone") }
                }
                TextButton(onClick = onCheckBiometrics) { Text("Check again") }
            }
        }
    }

    if (biometricFeedback != null) {
        FeedbackMessage(
            text = biometricFeedback,
            isError = biometricFeedbackIsError
        )
    }

    SectionHeader("PREFERENCES")

    SettingsGroup {
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

    Text(
        text = "These choices are stored with your account and are used as the default recipe preferences.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            top = KitchenQuestDimens.SmallSpacing
        )
    )

    when {
        state.errorMessage != null -> {
            FeedbackMessage(
                text = state.errorMessage,
                isError = true
            )
        }

        state.saveSucceeded -> {
            FeedbackMessage(
                text = "Changes saved",
                isError = false
            )
        }
    }

    if (state.hasUnsavedChanges || state.isSaving) {
        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        KitchenQuestPrimaryButton(
            text = if (state.isSaving) "Saving..." else "Save changes",
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(KitchenQuestDimens.LargeSpacing))

    SectionHeader("ACCOUNT ACTION")

    OutlinedButton(
        onClick = onSignOut,
        modifier = Modifier
            .fillMaxWidth()
            .height(KitchenQuestDimens.ButtonHeight),
        shape = RoundedCornerShape(KitchenQuestDimens.MediumCorner),
        border = BorderStroke(
            KitchenQuestDimens.CardBorderWidth,
            MaterialTheme.colorScheme.error
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.size(KitchenQuestDimens.SmallSpacing))

        Text("Sign out")
    }

    Spacer(modifier = Modifier.height(KitchenQuestDimens.SectionSpacing))

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
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            top = KitchenQuestDimens.SectionSpacing,
            bottom = KitchenQuestDimens.SmallSpacing
        )
    )
}

@Composable
private fun SettingsGroup(
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KitchenQuestDimens.LargeCorner),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            KitchenQuestDimens.CardBorderWidth,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Column(content = { content() })
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
            .heightIn(min = 72.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick
            )
            .padding(KitchenQuestDimens.MediumSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(KitchenQuestDimens.SmallCorner),
            color = MaterialTheme.colorScheme.surfaceVariant
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
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
private fun FeedbackMessage(
    text: String,
    isError: Boolean
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.secondary
        },
        modifier = Modifier
            .padding(top = KitchenQuestDimens.MediumSpacing)
            .semantics {
                liveRegion = LiveRegionMode.Polite
            }
    )
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    KitchenQuestDimens.TinySpacing
                )
            ) {
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
