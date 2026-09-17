package com.example.kitchenquest.feature.onboarding

data class OnboardingSelection(
    val dietaryPreferences: Set<String> =
        setOf("No restrictions"),
    val avoidedIngredients: Set<String> =
        emptySet()
)