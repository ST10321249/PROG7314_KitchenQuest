package com.example.kitchenquest.data.recipes

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall

interface RecipeRepository {

    suspend fun searchRecipes(
        query: String? = null,
        diet: String? = null,
        cuisine: String? = null,
        maxReadyTime: Int? = null
    ): Result<List<RecipeSearchResultDto>>

    suspend fun getRecipeById(id: String): Result<RecipeDetailDto>

    // Keeps the original Part 2 repository contract intact for screens and
    // tests that simply want recommendations from the user's saved pantry.
    suspend fun getRecommendations(): Result<List<RecipeRecommendationDto>>

    // What Can I Make? can temporarily exclude ingredient chips without
    // modifying the pantry. Implementations that do not need this behaviour
    // can safely fall back to the normal recommendation request.
    suspend fun getRecommendations(
        ingredients: List<String>
    ): Result<List<RecipeRecommendationDto>> = getRecommendations()
}

class DefaultRecipeRepository(
    private val recipeApi: RecipeApi = ApiClient.create(RecipeApi::class.java)
) : RecipeRepository {

    override suspend fun searchRecipes(
        query: String?,
        diet: String?,
        cuisine: String?,
        maxReadyTime: Int?
    ): Result<List<RecipeSearchResultDto>> = apiCall {
        recipeApi.searchRecipes(
            query = query,
            diet = diet,
            cuisine = cuisine,
            maxReadyTime = maxReadyTime
        )
    }

    override suspend fun getRecipeById(id: String): Result<RecipeDetailDto> = apiCall {
        recipeApi.getRecipeById(id)
    }

    override suspend fun getRecommendations(): Result<List<RecipeRecommendationDto>> = apiCall {
        recipeApi.getRecommendations().recipes
    }

    override suspend fun getRecommendations(
        ingredients: List<String>
    ): Result<List<RecipeRecommendationDto>> = apiCall {
        recipeApi.getRecommendations(
            ingredients = ingredients
                .takeIf { it.isNotEmpty() }
                ?.joinToString(",")
        ).recipes
    }
}
