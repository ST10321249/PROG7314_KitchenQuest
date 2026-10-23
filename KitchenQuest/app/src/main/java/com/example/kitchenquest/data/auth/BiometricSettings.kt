package com.example.kitchenquest.data.auth

import android.content.Context
import androidx.biometric.BiometricManager

/** Device-only biometric preference. No fingerprints, faces or passwords are saved here. */
class BiometricSettings(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "kitchenquest_biometric_settings",
        Context.MODE_PRIVATE
    )

    // Include the Firebase uid so one account cannot inherit another user's choice.
    fun isEnabled(uid: String): Boolean =
        uid.isNotBlank() && preferences.getBoolean("enabled_$uid", false)

    fun setEnabled(uid: String, enabled: Boolean) {
        if (uid.isBlank()) return
        preferences.edit().putBoolean("enabled_$uid", enabled).apply()
    }
}

enum class BiometricAvailability {
    READY,
    NOT_ENROLLED,
    NO_HARDWARE,
    TEMPORARILY_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

/** We use biometric-only verification; normal Firebase login stays the fallback. */
object BiometricSupport {
    val authenticators: Int = BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun check(context: Context): BiometricAvailability = fromResult(
        BiometricManager.from(context).canAuthenticate(authenticators)
    )

    fun fromResult(result: Int): BiometricAvailability = when (result) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.READY
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            BiometricAvailability.TEMPORARILY_UNAVAILABLE
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
            BiometricAvailability.SECURITY_UPDATE_REQUIRED
        else -> BiometricAvailability.UNSUPPORTED
    }
}
