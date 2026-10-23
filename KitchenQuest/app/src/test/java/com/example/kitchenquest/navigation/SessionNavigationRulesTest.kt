package com.example.kitchenquest.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionNavigationRulesTest {
    @Test fun languageRecreationKeepsSettingsOpen() {
        assertTrue(SessionNavigationRules.isAlreadyInsideApp(AppDestinations.Settings))
    }
    @Test fun languageRecreationKeepsRecipeDetailsOpen() {
        assertTrue(SessionNavigationRules.isAlreadyInsideApp(AppDestinations.RecipeDetails))
    }
    @Test fun splashAndAuthStillNeedInitialRouting() {
        assertFalse(SessionNavigationRules.isAlreadyInsideApp(AppDestinations.Splash))
        assertFalse(SessionNavigationRules.isAlreadyInsideApp(AppDestinations.Login))
        assertFalse(SessionNavigationRules.isAlreadyInsideApp(AppDestinations.Onboarding))
        assertFalse(SessionNavigationRules.isAlreadyInsideApp(null))
    }
}
