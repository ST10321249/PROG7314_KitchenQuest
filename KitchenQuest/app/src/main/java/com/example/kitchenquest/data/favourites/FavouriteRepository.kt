package com.example.kitchenquest.data.favourites

import android.util.Log
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall
import com.example.kitchenquest.data.recipes.RecipeOfflineStore
import com.example.kitchenquest.data.recipes.isConnectionProblem

// The saved-recipes list plus where it came from. isOfflineCopy is true when the server
// could not be reached and the list saved on the phone is being shown instead.
data class FavouritesLoad(
    val favourites: List<FavouriteRecipeDto>,
    val isOfflineCopy: Boolean = false
)

interface FavouriteRepository {
    suspend fun getFavourites(): Result<List<FavouriteRecipeDto>>
    suspend fun addFavourite(recipeSourceId: String, recipeTitle: String, imageUrl: String?): Result<FavouriteRecipeDto>
    suspend fun removeFavourite(recipeSourceId: String): Result<Unit>

    // Same as getFavourites, but also says whether the list is an offline copy.
    // Repositories without an offline copy simply report the online result.
    suspend fun getFavouritesWithSource(): Result<FavouritesLoad> =
        getFavourites().map { FavouritesLoad(favourites = it) }
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

// Wraps the online repository so the saved-recipes list keeps a copy on the phone.
// Adding and removing still need the server (they are not queued offline); the phone
// copy is only updated after the server says the change worked.
class OfflineFavouriteRepository(
    private val remote: FavouriteRepository,
    private val store: RecipeOfflineStore
) : FavouriteRepository {

    override suspend fun getFavourites(): Result<List<FavouriteRecipeDto>> =
        getFavouritesWithSource().map { it.favourites }

    override suspend fun getFavouritesWithSource(): Result<FavouritesLoad> {
        val online = remote.getFavourites()

        online.onSuccess { favourites ->
            store.replaceFavourites(favourites)
            return Result.success(FavouritesLoad(favourites = favourites))
        }

        val failure = online.exceptionOrNull() ?: return online.map { FavouritesLoad(it) }
        if (!failure.isConnectionProblem()) return Result.failure(failure)

        val stored = store.getFavourites()
        if (stored.isEmpty()) return Result.failure(failure)

        Log.i(TAG, "Offline: showing ${stored.size} saved recipes from the phone")
        return Result.success(FavouritesLoad(favourites = stored, isOfflineCopy = true))
    }

    override suspend fun addFavourite(
        recipeSourceId: String,
        recipeTitle: String,
        imageUrl: String?
    ): Result<FavouriteRecipeDto> =
        remote.addFavourite(recipeSourceId, recipeTitle, imageUrl)
            .onSuccess { store.addFavourite(it) }

    override suspend fun removeFavourite(recipeSourceId: String): Result<Unit> =
        remote.removeFavourite(recipeSourceId)
            .onSuccess { store.removeFavourite(recipeSourceId) }

    private companion object {
        const val TAG = "OfflineFavouriteRepo"
    }
}