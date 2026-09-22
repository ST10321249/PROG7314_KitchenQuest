package com.example.kitchenquest.data.recipes

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApi {

    @GET("api/recipes/search")
    suspend fun searchRecipes(
        @Query("query") query: String? = null,
        @Query("diet") diet: String? = null,
        @Query("cuisine") cuisine: String? = null,
        @Query("maxReadyTime") maxReadyTime: Int? = null,
        @Query("number") number: Int? = null
    ): List<RecipeSearchResultDto>

    @GET("api/recipes/{id}")
    suspend fun getRecipeById(
        @Path("id") id: String
    ): RecipeDetailDto

    @GET("api/recipes/recommendations")
    suspend fun getRecommendations(
        @Query("number") number: Int? = null
    ): RecipeRecommendationsResponseDto
}