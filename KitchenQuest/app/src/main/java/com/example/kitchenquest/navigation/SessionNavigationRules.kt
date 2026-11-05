package com.example.kitchenquest.navigation

/** Recreating the activity for a language change must not navigate back Home. */
internal object SessionNavigationRules {
    fun isAlreadyInsideApp(route: String?): Boolean = route != null &&
        route != AppDestinations.Splash &&
        route != AppDestinations.Login &&
        route != AppDestinations.Register &&
        route != AppDestinations.ForgotPassword &&
        route != AppDestinations.Onboarding
}
