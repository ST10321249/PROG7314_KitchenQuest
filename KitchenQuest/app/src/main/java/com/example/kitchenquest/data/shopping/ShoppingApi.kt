package com.example.kitchenquest.data.shopping

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ShoppingApi {

    @GET("api/shopping-list")
    suspend fun getShoppingItems(): List<ShoppingItemDto>

    @POST("api/shopping-list")
    suspend fun addShoppingItem(@Body request: CreateShoppingItemRequest): ShoppingItemDto

    @PATCH("api/shopping-list/{id}")
    suspend fun updateShoppingItem(
        @Path("id") id: String,
        @Body request: UpdateShoppingItemRequest
    ): ShoppingItemDto

    @DELETE("api/shopping-list/{id}")
    suspend fun deleteShoppingItem(@Path("id") id: String)
}