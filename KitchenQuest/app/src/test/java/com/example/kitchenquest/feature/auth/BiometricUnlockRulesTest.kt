package com.example.kitchenquest.feature.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricUnlockRulesTest {
    @Test fun returningOptedInUserMustUnlock() {
        assertTrue(BiometricUnlockRules.shouldLock("alice", true, null, false))
    }

    @Test fun verifiedSessionCanAccessApp() {
        assertFalse(BiometricUnlockRules.shouldLock("alice", true, "alice", false))
    }

    @Test fun authenticatedPasswordLoginNeedsNoSecondPrompt() {
        assertFalse(BiometricUnlockRules.shouldLock("alice", true, null, true))
    }

    @Test fun noBiometricPreferenceMeansNoLock() {
        assertFalse(BiometricUnlockRules.shouldLock("alice", false, null, false))
    }

    @Test fun loggedOutUserNeedsNoBiometricGate() {
        assertFalse(BiometricUnlockRules.shouldLock(null, true, null, false))
    }

    @Test fun unlockingDifferentAccountDoesNotGrantAccess() {
        assertTrue(BiometricUnlockRules.shouldLock("bob", true, "alice", false))
    }

    @Test fun backgroundResetRequiresAnotherUnlock() {
        assertTrue(BiometricUnlockRules.shouldLock("alice", true, null, false))
    }
}
