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

    suspend fun getRecommendations(): Result<List<RecipeRecommendationDto>>
}

class DefaultRecipeRepository(
    private val recipeApi: RecipeApi =
        ApiClient.create(RecipeApi::class.java)
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
}