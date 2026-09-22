package com.example.kitchenquest.data.recipes

import kotlinx.serialization.Serializable

@Serializable
data class RecipeSearchResultDto(
    val recipeSourceId: String = "",
    val title: String = "",
    val imageUrl: String? = null,
    val readyInMinutes: Int? = null,
    val servings: Int? = null
)

@Serializable
data class RecipeIngredientDto(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String = ""
)

@Serializable
data class RecipeStepDto(
    val number: Int = 0,
    val instruction: String = ""
)

@Serializable
data class RecipeDetailDto(
    val recipeSourceId: String = "",
    val title: String = "",
    val imageUrl: String? = null,
    val readyInMinutes: Int? = null,
    val servings: Int? = null,
    val ingredients: List<RecipeIngredientDto> = emptyList(),
    val steps: List<RecipeStepDto> = emptyList()
)

@Serializable
data class RecipeRecommendationDto(
    val recipeSourceId: String = "",
    val title: String = "",
    val imageUrl: String? = null,
    val matchRatio: Double = 0.0,
    val missingIngredients: List<String> = emptyList()
)

@Serializable
data class RecipeRecommendationsResponseDto(
    val recipes: List<RecipeRecommendationDto> = emptyList()
)