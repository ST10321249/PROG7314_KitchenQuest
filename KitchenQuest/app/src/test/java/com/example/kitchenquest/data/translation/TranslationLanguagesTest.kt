package com.example.kitchenquest.data.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationLanguagesTest {
    @Test fun languageNormalizationSupportsOurTwoLocales() {
        assertEquals("en", TranslationLanguages.normalized(" EN "))
        assertEquals("af", TranslationLanguages.normalized("af"))
        assertEquals(null, TranslationLanguages.normalized("fr"))
    }
    @Test fun doesNotTranslateBlankOrAlreadyLocalizedContent() {
        assertFalse(TranslationLanguages.requiresTranslation("", "en", "af"))
        assertFalse(TranslationLanguages.requiresTranslation("rice", "en", "en"))
        assertFalse(TranslationLanguages.requiresTranslation("rice", "fr", "af"))
    }
    @Test fun translatesBetweenEnglishAndAfrikaans() {
        assertTrue(TranslationLanguages.requiresTranslation("Rice", "en", "af"))
        assertTrue(TranslationLanguages.requiresTranslation("Rys", "af", "en"))
    }
    @Test fun cacheEntriesDifferBySourceAndTarget() {
        val a = TranslationLanguages.cacheKey("Bread", "en", "af")
        val b = TranslationLanguages.cacheKey("Bread", "af", "en")
        assertTrue(a != b)
    }
}
