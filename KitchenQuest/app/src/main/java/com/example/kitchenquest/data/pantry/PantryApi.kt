package com.example.kitchenquest.data.pantry

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface PantryApi {

    @GET("api/pantry")
    suspend fun getPantryItems(): List<PantryItemDto>

    @retrofit2.http.POST("api/pantry")
    suspend fun addPantryItem(
        @Body request: CreatePantryItemRequest
    ): PantryItemDto

    @PATCH("api/pantry/{id}")
    suspend fun updatePantryItem(
        @Path("id") id: String,
        @Body request: UpdatePantryItemRequest
    ): PantryItemDto

    @DELETE("api/pantry/{id}")
    suspend fun deletePantryItem(
        @Path("id") id: String
    )
}