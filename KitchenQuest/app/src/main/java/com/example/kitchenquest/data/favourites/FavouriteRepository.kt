package com.example.kitchenquest.data.favourites

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall

interface FavouriteRepository {
    suspend fun getFavourites(): Result<List<FavouriteRecipeDto>>
    suspend fun addFavourite(recipeSourceId: String, recipeTitle: String, imageUrl: String?): Result<FavouriteRecipeDto>
    suspend fun removeFavourite(recipeSourceId: String): Result<Unit>
}

class DefaultFavouriteRepository(
    private val favouriteApi: FavouriteApi = ApiClient.create(FavouriteApi::class.java)
) : FavouriteRepository {

    override suspend fun getFavourites(): Result<List<FavouriteRecipeDto>> = apiCall {
        favouriteApi.getFavourites()
    }

    override suspend fun addFavourite(
        recipeSourceId: String,
        recipeTitle: String,
        imageUrl: String?
    ): Result<FavouriteRecipeDto> = apiCall {
        favouriteApi.addFavourite(AddFavouriteRequest(recipeSourceId, recipeTitle, imageUrl))
    }

    override suspend fun removeFavourite(recipeSourceId: String): Result<Unit> = apiCall {
        favouriteApi.removeFavourite(recipeSourceId)
    }
}