package com.example.kitchenquest.data.auth

import androidx.biometric.BiometricManager
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricSupportTest {
    @Test
    fun availableDeviceCanUseBiometricSetup() {
        assertEquals(
            BiometricAvailability.READY,
            BiometricSupport.fromResult(BiometricManager.BIOMETRIC_SUCCESS)
        )
    }

    @Test
    fun missingEnrollmentDoesNotEnableBiometrics() {
        assertEquals(
            BiometricAvailability.NOT_ENROLLED,
            BiometricSupport.fromResult(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED)
        )
    }

    @Test
    fun devicesWithoutHardwareCannotOfferBiometrics() {
        assertEquals(
            BiometricAvailability.NO_HARDWARE,
            BiometricSupport.fromResult(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
        )
    }

    @Test
    fun temporaryHardwareFailureDoesNotLookReady() {
        assertEquals(
            BiometricAvailability.TEMPORARILY_UNAVAILABLE,
            BiometricSupport.fromResult(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE)
        )
    }

    @Test
    fun unknownStatusRemainsUnavailable() {
        assertEquals(BiometricAvailability.UNSUPPORTED, BiometricSupport.fromResult(-999))
    }
}
