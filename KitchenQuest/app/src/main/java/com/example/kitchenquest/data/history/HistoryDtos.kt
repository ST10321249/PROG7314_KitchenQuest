package com.example.kitchenquest.data.history

import kotlinx.serialization.Serializable

@Serializable
data class CookingHistoryDto(
    val id: String = "",
    val recipeSourceId: String = "",
    val recipeTitle: String = "",
    val cookedAt: String = "",
    val servings: Int? = null,
    val rating: Int? = null,
    val difficultyFeedback: String? = null
)

@Serializable
data class AddHistoryRequest(
    val recipeSourceId: String,
    val recipeTitle: String,
    val servings: Int? = null,
    val rating: Int? = null,
    val difficultyFeedback: String? = null
)

@Serializable
data class RecipeNoteDto(
    val recipeSourceId: String = "",
    val note: String = "",
    val updatedAt: String = ""
)

@Serializable
data class SaveRecipeNoteRequest(
    val recipeSourceId: String,
    val note: String
)