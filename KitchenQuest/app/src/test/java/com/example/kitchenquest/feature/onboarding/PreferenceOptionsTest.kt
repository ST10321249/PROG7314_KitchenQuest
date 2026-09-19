package com.example.kitchenquest.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceOptionsTest {

    private val none = PreferenceOptions.NO_RESTRICTIONS

    @Test
    fun choosingNoRestrictionsClearsEveryOtherChoice() {
        val result = PreferenceOptions.toggleDietary(
            setOf("Vegan", "Halal"),
            none
        )

        assertEquals(setOf(none), result)
    }

    @Test
    fun choosingARealOptionRemovesNoRestrictions() {
        val result = PreferenceOptions.toggleDietary(setOf(none), "Vegan")

        assertEquals(setOf("Vegan"), result)
    }

    @Test
    fun severalDietaryOptionsCanBeSelectedAtOnce() {
        var selected = setOf(none)

        selected = PreferenceOptions.toggleDietary(selected, "Vegan")
        selected = PreferenceOptions.toggleDietary(selected, "Halal")

        assertEquals(setOf("Vegan", "Halal"), selected)
    }

    @Test
    fun deselectingAnOptionKeepsTheOthers() {
        val result = PreferenceOptions.toggleDietary(
            setOf("Vegan", "Halal"),
            "Vegan"
        )

        assertEquals(setOf("Halal"), result)
    }

    @Test
    fun deselectingTheLastDietaryOptionFallsBackToNoRestrictions() {
        val result = PreferenceOptions.toggleDietary(setOf("Vegan"), "Vegan")

        assertEquals(setOf(none), result)
    }

    @Test
    fun avoidedIngredientsToggleOnAndOffAndCanBeEmpty() {
        var selected = emptySet<String>()

        selected = PreferenceOptions.toggleAvoided(selected, "Nuts")
        selected = PreferenceOptions.toggleAvoided(selected, "Soy")
        assertEquals(setOf("Nuts", "Soy"), selected)

        selected = PreferenceOptions.toggleAvoided(selected, "Nuts")
        selected = PreferenceOptions.toggleAvoided(selected, "Soy")
        assertTrue(selected.isEmpty())
    }

    @Test
    fun valuesFromTheServerThatAreNotStandardOptionsStayVisible() {
        val options = PreferenceOptions.optionsWith(
            PreferenceOptions.avoidedIngredients,
            setOf("Nuts", "Peanuts")
        )

        assertEquals(PreferenceOptions.avoidedIngredients + "Peanuts", options)
    }

    @Test
    fun summaryListsTheChoicesOrFallsBackToTheEmptyText() {
        assertEquals("None", PreferenceOptions.summary(emptySet(), "None"))
        assertEquals(
            "Vegan, Halal",
            PreferenceOptions.summary(linkedSetOf("Vegan", "Halal"), "None")
        )
    }
}
