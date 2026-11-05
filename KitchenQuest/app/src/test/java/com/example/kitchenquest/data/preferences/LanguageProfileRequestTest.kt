package com.example.kitchenquest.data.preferences

import com.example.kitchenquest.data.user.UserProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageProfileRequestTest {
    @Test fun `profile language is optional for existing records`() {
        assertNull(UserProfileDto().language)
    }

    @Test fun `profile updates contain the selected language`() {
        assertEquals("af", LanguageUpdateRequest(AppLanguage.normalized("AF")).language)
        assertEquals("en", LanguageUpdateRequest(AppLanguage.normalized("en")).language)
    }
}
