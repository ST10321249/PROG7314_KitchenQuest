package com.example.kitchenquest.data.recipes

import android.util.Log
import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall

// A loaded recipe plus where it came from. isOfflineCopy is true when the server could
// not be reached and the copy saved on the phone is being shown instead.
data class RecipeLoad(
    val recipe: RecipeDetailDto,
    val isOfflineCopy: Boolean = false,
    val savedAt: Long? = null
)

interface RecipeRepository {

    suspend fun searchRecipes(
        query: String? = null,
        diet: String? = null,
        cuisine: String? = null,
        maxReadyTime: Int? = null
    ): Result<List<RecipeSearchResultDto>>

    suspend fun getRecipeById(id: String): Result<RecipeDetailDto>

    // Same as getRecipeById, but also says whether the recipe is an offline copy.
    // Repositories without an offline copy simply report the online result.
    suspend fun getRecipeWithSource(id: String): Result<RecipeLoad> =
        getRecipeById(id).map { RecipeLoad(recipe = it) }

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

// Wraps the online repository: every recipe that loads is saved on the phone, and when
// the server cannot be reached the saved copy is returned instead. Search and
// recommendations are passed straight through because they need fresh data from the API.
class OfflineRecipeRepository(
    private val remote: RecipeRepository,
    private val store: RecipeOfflineStore
) : RecipeRepository by remote {

    override suspend fun getRecipeById(id: String): Result<RecipeDetailDto> =
        getRecipeWithSource(id).map { it.recipe }

    override suspend fun getRecipeWithSource(id: String): Result<RecipeLoad> {
        val online = remote.getRecipeById(id)

        online.onSuccess { recipe ->
            store.saveRecipe(recipe)
            return Result.success(RecipeLoad(recipe = recipe))
        }

        val failure = online.exceptionOrNull() ?: return online.map { RecipeLoad(recipe = it) }
        if (!failure.isConnectionProblem()) return Result.failure(failure)

        val stored = store.getRecipe(id) ?: return Result.failure(failure)
        Log.i(TAG, "Offline: showing the saved copy of recipe $id")
        return Result.success(
            RecipeLoad(recipe = stored.recipe, isOfflineCopy = true, savedAt = stored.savedAt)
        )
    }

    private companion object {
        const val TAG = "OfflineRecipeRepo"
    }
}