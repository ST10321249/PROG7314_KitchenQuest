package com.example.kitchenquest.feature.settings

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.kitchenquest.data.auth.BiometricSupport

/** Verify the person before opting in. Unlocking a returning session follows in R3. */
class BiometricSetup(private val activity: FragmentActivity) {
    fun confirmEnable(onFinished: (success: Boolean, message: String?) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onFinished(true, null)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFinished(false, when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> "Biometric setup was cancelled."
                        else -> "Biometric setup could not finish: $errString"
                    })
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable biometric unlock")
            .setSubtitle("Confirm your fingerprint or face for this KitchenQuest account")
            .setAllowedAuthenticators(BiometricSupport.authenticators)
            .setNegativeButtonText("Cancel")
            .build()

        prompt.authenticate(info)
    }
}
