package com.example.kitchenquest.feature.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun validEmail_returnsTrue() {

        assertTrue(
            isValidEmail(
                "user@example.com"
            )
        )

        assertTrue(
            isValidEmail(
                "raheel.test123@gmail.com"
            )
        )

        assertTrue(
            isValidEmail(
                "test-user+app@outlook.co.za"
            )
        )
    }

    @Test
    fun invalidEmail_returnsFalse() {

        assertFalse(
            isValidEmail("")
        )

        assertFalse(
            isValidEmail("raheel")
        )

        assertFalse(
            isValidEmail(
                "raheel@"
            )
        )

        assertFalse(
            isValidEmail(
                "@example.com"
            )
        )

        assertFalse(
            isValidEmail(
                "raheel@example"
            )
        )
    }

    @Test
    fun loginPassword_requiresNonBlankPassword() {

        assertTrue(
            isValidLoginPassword(
                "Password123!"
            )
        )

        assertFalse(
            isValidLoginPassword("")
        )

        assertFalse(
            isValidLoginPassword("   ")
        )
    }

    @Test
    fun displayName_requiresNonBlankName() {

        assertTrue(
            isValidDisplayName(
                "Raheel"
            )
        )

        assertTrue(
            isValidDisplayName(
                "  Raheel  "
            )
        )

        assertFalse(
            isValidDisplayName("")
        )

        assertFalse(
            isValidDisplayName("   ")
        )
    }

    @Test
    fun strongPassword_acceptsValidPassword() {

        assertTrue(
            isStrongPassword(
                "Abcdefg1!"
            )
        )
    }

    @Test
    fun strongPassword_rejectsPasswordWithoutUppercase() {

        assertFalse(
            isStrongPassword(
                "abcdefg1!"
            )
        )
    }

    @Test
    fun strongPassword_rejectsPasswordWithoutNumber() {

        assertFalse(
            isStrongPassword(
                "Abcdefgh!"
            )
        )
    }

    @Test
    fun strongPassword_rejectsPasswordWithoutSpecialCharacter() {

        assertFalse(
            isStrongPassword(
                "Abcdefg1"
            )
        )
    }

    @Test
    fun strongPassword_rejectsPasswordThatIsTooShort() {

        assertFalse(
            isStrongPassword(
                "Ab1!"
            )
        )
    }

    @Test
    fun passwordConfirmation_requiresMatchingPasswords() {

        assertTrue(
            passwordsMatch(
                password = "Abcdefg1!",
                confirmPassword = "Abcdefg1!"
            )
        )

        assertFalse(
            passwordsMatch(
                password = "Abcdefg1!",
                confirmPassword = "Different1!"
            )
        )

        assertFalse(
            passwordsMatch(
                password = "Abcdefg1!",
                confirmPassword = ""
            )
        )
    }
}