package com.example.kitchenquest.feature.auth

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kitchenquest.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.kitchenquest.data.auth.BiometricAvailability
import com.example.kitchenquest.data.auth.BiometricSupport
import com.example.kitchenquest.ui.components.KitchenQuestPrimaryButton
import com.example.kitchenquest.ui.theme.KitchenQuestDimens

/** Full-screen gate. Protected navigation is not composed until verification. */
@Composable
fun BiometricUnlockScreen(
    userId: String,
    onUnlocked: () -> Unit,
    onUseNormalLogin: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canAuthenticate = BiometricSupport.check(context) == BiometricAvailability.READY
    val onUnlockedLatest by rememberUpdatedState(onUnlocked)

    var message by remember(userId) { mutableStateOf<String?>(null) }
    var autoPromptStarted by remember(userId) { mutableStateOf(false) }
    var isResumed by remember(activity) {
        mutableStateOf(activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true)
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE -> isResumed = false
                else -> Unit
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    val prompt = remember(activity) {
        activity?.let { host ->
            BiometricPrompt(
                host,
                ContextCompat.getMainExecutor(host),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        message = null
                        onUnlockedLatest()
                    }

                    override fun onAuthenticationFailed() {
                        message = context.getString(R.string.auth_bio_failed)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        message = when (errorCode) {
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_CANCELED ->
                                context.getString(R.string.auth_bio_cancelled)
                            BiometricPrompt.ERROR_LOCKOUT,
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                                context.getString(R.string.auth_bio_locked)
                            else -> context.getString(R.string.auth_bio_unavailable_format, errString)
                        }
                    }
                }
            )
        }
    }

    val startPrompt: () -> Unit = {
        if (prompt != null && canAuthenticate && isResumed) {
            message = null
            try {
                prompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(context.getString(R.string.auth_bio_prompt_title))
                        .setSubtitle(context.getString(R.string.auth_bio_prompt_subtitle))
                        .setAllowedAuthenticators(BiometricSupport.authenticators)
                        .setNegativeButtonText(context.getString(R.string.settings_cancel))
                        .build()
                )
            } catch (_: Exception) {
                message = context.getString(R.string.auth_bio_cant_start)
            }
        } else {
            message = context.getString(R.string.auth_bio_device_missing)
        }
    }

    // Auto-prompt once per lock; cancellation leaves a visible retry option.
    // Wait for RESUMED so the FragmentManager can display the system prompt.
    LaunchedEffect(isResumed, userId) {
        if (isResumed && !autoPromptStarted) {
            autoPromptStarted = true
            startPrompt()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(KitchenQuestDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(76.dp).fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.auth_locked_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.auth_locked_instructions),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            if (!message.isNullOrBlank() || !canAuthenticate) {
                Spacer(Modifier.height(16.dp))
                Text(
                    message ?: stringResource(R.string.auth_bio_not_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            if (canAuthenticate) {
                KitchenQuestPrimaryButton(
                    text = stringResource(R.string.auth_try_biometric),
                    onClick = startPrompt,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            TextButton(onClick = onUseNormalLogin) {
                Text(stringResource(R.string.auth_use_normal_login))
            }
        }
    }
}
