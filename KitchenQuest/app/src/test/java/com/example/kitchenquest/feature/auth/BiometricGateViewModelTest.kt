package com.example.kitchenquest.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricGateViewModelTest {
    @Test fun restoredSessionStartsLocked() {
        val gate = BiometricGateViewModel()
        gate.sessionResolved("restored-user")
        assertNull(gate.unlockedUid)
        assertFalse(gate.signedOutSinceLaunch)
    }

    @Test fun newInteractiveLoginCanOpenExistingSession() {
        val gate = BiometricGateViewModel()
        gate.sessionResolved(null)
        assertTrue(gate.signedOutSinceLaunch)
        gate.sessionResolved("new-user")
        assertEquals("new-user", gate.unlockedUid)
        assertFalse(gate.signedOutSinceLaunch)
    }

    @Test fun verifiedSessionSurvivesConfigurationRecreation() {
        val gate = BiometricGateViewModel()
        gate.sessionResolved("user")
        gate.verified("user")
        gate.sessionResolved("user")
        assertEquals("user", gate.unlockedUid)
    }

    @Test fun backgroundingRelocksSession() {
        val gate = BiometricGateViewModel()
        gate.sessionResolved("user")
        gate.verified("user")
        gate.lock()
        assertNull(gate.unlockedUid)
    }

    @Test fun coldLaunchShowsSplashOnceBeforeBiometrics() {
        val gate = BiometricGateViewModel()
        assertFalse(gate.startupSplashShown)
        gate.finishStartupSplash()
        assertTrue(gate.startupSplashShown)
        gate.verified("user")
        gate.lock()
        // Re-locking on background return must not replay the splash.
        assertTrue(gate.startupSplashShown)
    }

    @Test fun changingFirebaseAccountNeverReusesUnlock() {
        val gate = BiometricGateViewModel()
        gate.sessionResolved("first")
        gate.verified("first")
        gate.sessionResolved("second")
        assertNull(gate.unlockedUid)
    }
}
