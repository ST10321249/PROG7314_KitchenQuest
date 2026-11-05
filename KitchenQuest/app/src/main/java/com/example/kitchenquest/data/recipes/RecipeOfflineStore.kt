package com.example.kitchenquest.data.recipes

import android.util.Log
import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.data.local.CachedFavouriteEntity
import com.example.kitchenquest.data.local.CachedRecipeEntity
import com.example.kitchenquest.data.local.RecipeCacheDao
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import kotlinx.coroutines.CancellationException

// A recipe copy read back from the phone, with the time it was saved.
data class StoredRecipe(
    val recipe: RecipeDetailDto,
    val savedAt: Long
)

// Keeps copies of recipes and of the saved-recipes list on the phone for the signed-in
// user. Searching for new recipes still needs the internet (Spoonacular is reached
// through our API), so only recipes that were already opened can be read offline.
//
// Saving a copy must never break the screen that is showing the recipe, so every write
// here catches its own errors and only logs them.
class RecipeOfflineStore(
    private val dao: RecipeCacheDao,
    private val currentUserId: () -> String?,
    private val maxRecipes: Int = DEFAULT_MAX_RECIPES,
    private val clock: () -> Long = System::currentTimeMillis
) {

    suspend fun saveRecipe(recipe: RecipeDetailDto) {
        val userId = currentUserId() ?: return
        if (recipe.recipeSourceId.isBlank()) return

        safely("save recipe ${recipe.recipeSourceId}") {
            dao.putRecipe(
                CachedRecipeEntity(
                    userId = userId,
                    recipeSourceId = recipe.recipeSourceId,
                    title = recipe.title,
                    recipeJson = ApiClient.json.encodeToString(RecipeDetailDto.serializer(), recipe),
                    savedAt = clock()
                )
            )
            dao.trimRecipes(userId, maxRecipes)
            Log.d(TAG, "Saved an offline copy of recipe ${recipe.recipeSourceId}")
        }
    }

    suspend fun getRecipe(recipeSourceId: String): StoredRecipe? {
        val userId = currentUserId() ?: return null
        val row = try {
            dao.getRecipe(userId, recipeSourceId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not read the offline copy of recipe $recipeSourceId", error)
            null
        } ?: return null

        return try {
            StoredRecipe(
                recipe = ApiClient.json.decodeFromString(RecipeDetailDto.serializer(), row.recipeJson),
                savedAt = row.savedAt
            )
        } catch (error: Exception) {
            // A copy written by an older version of the app that no longer reads cleanly.
            Log.w(TAG, "The offline copy of recipe $recipeSourceId could not be read", error)
            null
        }
    }

    suspend fun replaceFavourites(favourites: List<FavouriteRecipeDto>) {
        val userId = currentUserId() ?: return
        safely("save the saved-recipes list") {
            dao.replaceFavourites(
                userId,
                favourites.mapIndexed { index, favourite -> favourite.toEntity(userId, index) }
            )
            Log.d(TAG, "Saved an offline copy of ${favourites.size} saved recipes")
        }
    }

    suspend fun getFavourites(): List<FavouriteRecipeDto> {
        val userId = currentUserId() ?: return emptyList()
        return try {
            dao.getFavourites(userId).map { it.toDto() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not read the offline saved-recipes list", error)
            emptyList()
        }
    }

    suspend fun addFavourite(favourite: FavouriteRecipeDto) {
        val userId = currentUserId() ?: return
        safely("add ${favourite.recipeSourceId} to the saved list") {
            val nextOrder = dao.lastFavouriteOrder(userId) + 1
            dao.putFavourites(listOf(favourite.toEntity(userId, nextOrder)))
        }
    }

    suspend fun removeFavourite(recipeSourceId: String) {
        val userId = currentUserId() ?: return
        safely("remove $recipeSourceId from the saved list") {
            dao.removeFavourite(userId, recipeSourceId)
        }
    }

    private suspend fun safely(action: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not $action for offline use", error)
        }
    }

    private fun FavouriteRecipeDto.toEntity(userId: String, order: Int) = CachedFavouriteEntity(
        userId = userId,
        recipeSourceId = recipeSourceId,
        favouriteId = id,
        recipeTitle = recipeTitle,
        imageUrl = imageUrl,
        listOrder = order
    )

    private fun CachedFavouriteEntity.toDto() = FavouriteRecipeDto(
        id = favouriteId,
        recipeSourceId = recipeSourceId,
        recipeTitle = recipeTitle,
        imageUrl = imageUrl
    )

    companion object {
        private const val TAG = "RecipeOfflineStore"

        // Saved recipes are always kept. This only limits other recipes that were opened.
        const val DEFAULT_MAX_RECIPES = 50
    }
}

// True when the failure means "the server could not be reached right now", which is when
// showing the copy saved on the phone makes sense. Errors like "not found" or "please sign
// in again" are real answers from the server, so they are shown as they are.
fun Throwable.isConnectionProblem(): Boolean {
    val type = (this as? ApiException)?.type ?: return false
    return type == ApiErrorType.NO_CONNECTION ||
            type == ApiErrorType.TIMEOUT ||
            type == ApiErrorType.SERVER
}