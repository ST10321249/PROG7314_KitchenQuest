package com.example.kitchenquest.data.history

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HistoryApi {

    @GET("api/history")
    suspend fun getHistory(): List<CookingHistoryDto>

    @POST("api/history")
    suspend fun addHistoryEntry(@Body request: AddHistoryRequest): CookingHistoryDto

    @GET("api/recipe-notes/{recipeId}")
    suspend fun getNote(@Path("recipeId") recipeId: String): RecipeNoteDto?

    @POST("api/recipe-notes")
    suspend fun saveNote(@Body request: SaveRecipeNoteRequest): RecipeNoteDto
}