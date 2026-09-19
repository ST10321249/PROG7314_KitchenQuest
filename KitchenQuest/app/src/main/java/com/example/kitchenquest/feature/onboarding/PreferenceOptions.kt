package com.example.kitchenquest.feature.onboarding

object PreferenceOptions {

    const val NO_RESTRICTIONS = "No restrictions"

    val dietary = listOf(
        NO_RESTRICTIONS,
        "Vegetarian",
        "Vegan",
        "Halal",
        "Gluten-free",
        "Dairy-free"
    )

    val avoidedIngredients = listOf(
        "Nuts",
        "Shellfish",
        "Eggs",
        "Milk",
        "Soy"
    )

    // "No restrictions" excludes every other choice, and clearing the
    // last choice falls back to it.
    fun toggleDietary(
        current: Set<String>,
        option: String
    ): Set<String> {
        if (option == NO_RESTRICTIONS) {
            return setOf(NO_RESTRICTIONS)
        }

        val updated = current.toMutableSet()
        updated.remove(NO_RESTRICTIONS)

        if (!updated.add(option)) {
            updated.remove(option)
        }

        return if (updated.isEmpty()) {
            setOf(NO_RESTRICTIONS)
        } else {
            updated
        }
    }

    fun toggleAvoided(
        current: Set<String>,
        ingredient: String
    ): Set<String> {
        val updated = current.toMutableSet()

        if (!updated.add(ingredient)) {
            updated.remove(ingredient)
        }

        return updated
    }

    // Keeps values that came from elsewhere (for example the server)
    // visible even when they are not one of the standard options.
    fun optionsWith(
        standard: List<String>,
        selected: Set<String>
    ): List<String> {
        return standard + selected.filter { it !in standard }
    }

    fun summary(
        selected: Set<String>,
        emptyText: String
    ): String {
        return if (selected.isEmpty()) {
            emptyText
        } else {
            selected.joinToString(", ")
        }
    }
}
