package com.example.kitchenquest.feature.settings

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.kitchenquest.R
import com.example.kitchenquest.data.auth.BiometricSupport

/** Confirm the user’s biometric identity before enabling quick unlock for this account. */
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
                        BiometricPrompt.ERROR_CANCELED -> activity.getString(R.string.settings_biometric_setup_cancelled)
                        else -> activity.getString(R.string.settings_biometric_setup_failed, errString)
                    })
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.settings_biometric_setup_title))
            .setSubtitle(activity.getString(R.string.settings_biometric_setup_subtitle))
            .setAllowedAuthenticators(BiometricSupport.authenticators)
            .setNegativeButtonText(activity.getString(R.string.settings_cancel))
            .build()

        prompt.authenticate(info)
    }
}
