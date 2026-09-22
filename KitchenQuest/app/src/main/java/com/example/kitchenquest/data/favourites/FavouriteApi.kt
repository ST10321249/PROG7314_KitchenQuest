package com.example.kitchenquest.data.favourites

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FavouriteApi {

    @GET("api/favourites")
    suspend fun getFavourites(): List<FavouriteRecipeDto>

    @POST("api/favourites")
    suspend fun addFavourite(@Body request: AddFavouriteRequest): FavouriteRecipeDto

    @DELETE("api/favourites/{recipeSourceId}")
    suspend fun removeFavourite(@Path("recipeSourceId") recipeSourceId: String)
}