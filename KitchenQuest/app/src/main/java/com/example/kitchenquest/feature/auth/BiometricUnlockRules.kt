package com.example.kitchenquest.feature.auth

/** Pure lock decision, so restored-session behavior can be unit tested. */
internal object BiometricUnlockRules {
    fun shouldLock(
        signedInUid: String?,
        biometricEnabled: Boolean,
        unlockedUid: String?,
        justSignedIn: Boolean
    ): Boolean = !signedInUid.isNullOrBlank() &&
        biometricEnabled &&
        unlockedUid != signedInUid &&
        !justSignedIn
}
