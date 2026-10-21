package com.example.kitchenquest.feature.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Keeps a verified session unlocked across locale/configuration changes.
 * This ViewModel does NOT survive process death: a fresh launch must verify again.
 * We never persist a biometric success flag to preferences or saved instance state.
 */
class BiometricGateViewModel : ViewModel() {
    var unlockedUid by mutableStateOf<String?>(null)
    var signedOutSinceLaunch by mutableStateOf(false)
        private set
    var passwordFallbackRequested by mutableStateOf(false)

    // Show the branded startup screen once, before first cold-launch biometric prompt.
    var startupSplashShown by mutableStateOf(false)
        private set

    fun finishStartupSplash() { startupSplashShown = true }

    /** Call only after Firebase has finished its initial auth check. */
    fun sessionResolved(uid: String?) {
        if (uid == null) {
            unlockedUid = null
            signedOutSinceLaunch = true
        } else if (signedOutSinceLaunch) {
            // The user signed in interactively after an observed signed-out state.
            unlockedUid = uid
            signedOutSinceLaunch = false
        } else if (unlockedUid != null && unlockedUid != uid) {
            // Never reuse verification between different Firebase accounts.
            unlockedUid = null
        }
    }

    fun lock() { unlockedUid = null }
    fun verified(uid: String) { unlockedUid = uid }
}
