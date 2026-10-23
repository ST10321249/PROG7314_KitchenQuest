package com.example.kitchenquest.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test fun englishIsSupported() {
        assertTrue(AppLanguage.isSupported("en"))
    }

    @Test fun afrikaansIsSupported() {
        assertTrue(AppLanguage.isSupported("af"))
    }

    @Test fun languageCodesAreNormalized() {
        assertEquals("af", AppLanguage.normalized(" AF "))
        assertEquals("en", AppLanguage.normalized("EN"))
    }

    @Test fun unsupportedCodesFallBackToEnglish() {
        assertFalse(AppLanguage.isSupported("fr"))
        assertEquals("en", AppLanguage.normalized("fr"))
    }

    @Test fun missingLanguageFallsBackToEnglish() {
        assertFalse(AppLanguage.isSupported(null))
        assertEquals("en", AppLanguage.normalized(null))
    }
}
