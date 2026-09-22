package com.example.kitchenquest.feature.recipes

// Display labels and the Spoonacular query values they map to (recipeApiClient.js
// forwards diet/cuisine straight through as query params, so these must match
// Spoonacular's accepted values, e.g. "gluten free" with a space, lowercase).
object RecipeFilterOptions {

    val diets = listOf("Vegetarian", "Vegan", "Gluten Free", "Ketogenic", "Paleo")
    val cuisines = listOf("Italian", "Mexican", "Indian", "Chinese", "Mediterranean", "American")
    val readyTimes = listOf(15, 30, 45, 60)

    fun dietParam(displayLabel: String): String = displayLabel.lowercase()

    fun cuisineParam(displayLabel: String): String = displayLabel.lowercase()
}
