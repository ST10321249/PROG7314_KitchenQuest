package com.example.kitchenquest.feature.settings

import android.app.Activity
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
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.ui.res.stringResource
import com.example.kitchenquest.R
import com.example.kitchenquest.data.preferences.AppLanguage
import com.example.kitchenquest.data.preferences.AppLanguagePreferences
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
    Avoided,
    Language
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
    val languagePreferences = remember(context) { AppLanguagePreferences(context) }
    var selectedLanguage by remember(context) {
        mutableStateOf(languagePreferences.getLanguage())
    }
    val changeLanguage: (String) -> Unit = { code ->
        // Saving first ensures attachBaseContext loads the correct resources.
        if (languagePreferences.setLanguage(code)) {
            selectedLanguage = languagePreferences.getLanguage()
            (context as? Activity)?.recreate()
        }
    }
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
            biometricFeedback = context.getString(R.string.settings_biometric_disabled_feedback)
        } else if (userId.isBlank() || biometricAvailability != BiometricAvailability.READY) {
            biometricFeedback = context.getString(R.string.settings_biometric_enroll_feedback)
            biometricFeedbackIsError = true
        } else {
            val activity = context as? FragmentActivity
            if (activity == null) {
                biometricFeedback = context.getString(R.string.settings_biometric_unavailable_feedback)
                biometricFeedbackIsError = true
            } else {
                BiometricSetup(activity).confirmEnable { verified, message ->
                    if (verified) {
                        biometricSettings.setEnabled(userId, true)
                        biometricEnabled = true
                        onBiometricsEnabled()
                        biometricFeedback = context.getString(R.string.settings_biometric_success_feedback)
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
            title = stringResource(R.string.settings_title),
            onBack = onBack
        )

        when {
            !state.hasLoaded && state.isLoading -> {
                KitchenQuestLoadingState(
                    message = stringResource(R.string.settings_loading),
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
                    message = stringResource(R.string.settings_loading),
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
                    languageCode = selectedLanguage,
                    onLanguageChange = changeLanguage,
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
            title = { Text(stringResource(R.string.settings_sign_out_question)) },
            text = {
                Text(stringResource(R.string.settings_sign_out_explanation))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirmation = false
                        onSignOut()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_sign_out),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmation = false }) {
                    Text(stringResource(R.string.settings_cancel))
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
    languageCode: String,
    onLanguageChange: (String) -> Unit,
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

    SectionHeader(stringResource(R.string.settings_account))

    SettingsGroup {
        SettingsRow(
            icon = Icons.Outlined.Person,
            title = state.displayName.ifBlank { stringResource(R.string.settings_add_name) },
            subtitle = state.email.ifBlank { stringResource(R.string.settings_no_email) },
            onClick = { openDialog = SettingsDialog.Name }
        )
    }

    SectionHeader(stringResource(R.string.settings_security))

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
                Text(stringResource(R.string.settings_biometric_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when (biometricAvailability) {
                        BiometricAvailability.READY ->
                            if (biometricEnabled) stringResource(R.string.settings_biometric_enabled) else stringResource(R.string.settings_biometric_verify)
                        BiometricAvailability.NOT_ENROLLED -> stringResource(R.string.settings_biometric_enroll)
                        BiometricAvailability.NO_HARDWARE -> stringResource(R.string.settings_biometric_missing)
                        BiometricAvailability.TEMPORARILY_UNAVAILABLE -> stringResource(R.string.settings_biometric_unavailable)
                        BiometricAvailability.SECURITY_UPDATE_REQUIRED -> stringResource(R.string.settings_biometric_security_update)
                        BiometricAvailability.UNSUPPORTED -> stringResource(R.string.settings_biometric_unsupported)
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
                    TextButton(onClick = onOpenDeviceBiometrics) { Text(stringResource(R.string.settings_biometric_phone_setup)) }
                }
                TextButton(onClick = onCheckBiometrics) { Text(stringResource(R.string.settings_biometric_check_again)) }
            }
        }
    }

    if (biometricFeedback != null) {
        FeedbackMessage(
            text = biometricFeedback,
            isError = biometricFeedbackIsError
        )
    }

    SectionHeader(stringResource(R.string.settings_preferences))

    // Interface language remains a local preference; server preference sync is R8.
    SettingsGroup {
        SettingsRow(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.settings_language),
            subtitle = if (languageCode == AppLanguage.AFRIKAANS) {
                stringResource(R.string.settings_language_afrikaans)
            } else {
                stringResource(R.string.settings_language_english)
            },
            onClick = { openDialog = SettingsDialog.Language }
        )
    }

    Text(
        text = stringResource(R.string.settings_language_device_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = KitchenQuestDimens.SmallSpacing)
    )

    TranslationModelsPanel(
        modifier = Modifier.padding(top = KitchenQuestDimens.MediumSpacing)
    )

    SettingsGroup {
        SettingsRow(
            icon = Icons.Outlined.Restaurant,
            title = stringResource(R.string.settings_dietary),
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
            title = stringResource(R.string.settings_avoided),
            subtitle = PreferenceOptions.summary(
                state.avoidedIngredients,
                stringResource(R.string.settings_no_avoided)
            ),
            onClick = { openDialog = SettingsDialog.Avoided }
        )
    }

    Text(
        text = stringResource(R.string.settings_preferences_description),
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
                text = stringResource(R.string.settings_saved),
                isError = false
            )
        }
    }

    if (state.hasUnsavedChanges || state.isSaving) {
        Spacer(modifier = Modifier.height(KitchenQuestDimens.MediumSpacing))

        KitchenQuestPrimaryButton(
            text = if (state.isSaving) stringResource(R.string.settings_saving) else stringResource(R.string.settings_save),
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(KitchenQuestDimens.LargeSpacing))

    SectionHeader(stringResource(R.string.settings_account_action))

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

        Text(stringResource(R.string.settings_sign_out))
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
            title = stringResource(R.string.settings_dietary),
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
            title = stringResource(R.string.settings_avoided),
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

        SettingsDialog.Language -> AlertDialog(
            onDismissRequest = { openDialog = null },
            title = { Text(stringResource(R.string.settings_language_dialog)) },
            text = {
                Column {
                    TextButton(onClick = {
                        openDialog = null
                        onLanguageChange(AppLanguage.ENGLISH)
                    }) {
                        Text(stringResource(R.string.settings_language_english))
                    }
                    TextButton(onClick = {
                        openDialog = null
                        onLanguageChange(AppLanguage.AFRIKAANS)
                    }) {
                        Text(stringResource(R.string.settings_language_afrikaans))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openDialog = null }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
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
        title = { Text(stringResource(R.string.settings_display_name)) },
        text = {
            KitchenQuestTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.settings_display_name),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.settings_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel))
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
                Text(stringResource(R.string.settings_done))
            }
        }
    )
}
