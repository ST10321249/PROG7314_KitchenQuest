package com.example.kitchenquest.data.recipes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.data.favourites.FavouriteRepository
import com.example.kitchenquest.data.favourites.OfflineFavouriteRepository
import com.example.kitchenquest.data.local.KitchenQuestDatabase
import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OfflineRecipeRepositoryTest {

    // Pretends to be the API. "online" decides whether calls work or fail like a lost connection.
    private class FakeRemoteRecipes : RecipeRepository {
        var online = true
        var failure: ApiException = ApiException(ApiErrorType.NO_CONNECTION, "No connection")
        val recipes = mutableMapOf<String, RecipeDetailDto>()

        override suspend fun searchRecipes(query: String?, diet: String?, cuisine: String?, maxReadyTime: Int?) =
            if (online) Result.success(emptyList<RecipeSearchResultDto>()) else Result.failure(failure)

        override suspend fun getRecipeById(id: String): Result<RecipeDetailDto> {
            if (!online) return Result.failure(failure)
            val recipe = recipes[id]
                ?: return Result.failure(ApiException(ApiErrorType.NOT_FOUND, "Recipe not found"))
            return Result.success(recipe)
        }

        override suspend fun getRecommendations() = Result.success(emptyList<RecipeRecommendationDto>())
    }

    private class FakeRemoteFavourites : FavouriteRepository {
        var online = true
        val favourites = mutableListOf<FavouriteRecipeDto>()
        private val offline = ApiException(ApiErrorType.NO_CONNECTION, "No connection")

        override suspend fun getFavourites() =
            if (online) Result.success(favourites.toList()) else Result.failure(offline)

        override suspend fun addFavourite(recipeSourceId: String, recipeTitle: String, imageUrl: String?): Result<FavouriteRecipeDto> {
            if (!online) return Result.failure(offline)
            val favourite = FavouriteRecipeDto("fav-$recipeSourceId", recipeSourceId, recipeTitle, imageUrl)
            favourites.add(favourite)
            return Result.success(favourite)
        }

        override suspend fun removeFavourite(recipeSourceId: String): Result<Unit> {
            if (!online) return Result.failure(offline)
            favourites.removeAll { it.recipeSourceId == recipeSourceId }
            return Result.success(Unit)
        }
    }

    private lateinit var database: KitchenQuestDatabase
    private lateinit var remoteRecipes: FakeRemoteRecipes
    private lateinit var remoteFavourites: FakeRemoteFavourites
    private var signedInUser: String? = "user-1"
    private var now = 1_000L
    private lateinit var store: RecipeOfflineStore
    private lateinit var recipes: OfflineRecipeRepository
    private lateinit var favourites: OfflineFavouriteRepository

    private val alfredo = RecipeDetailDto(
        recipeSourceId = "101",
        title = "Chicken Alfredo",
        servings = 2,
        ingredients = listOf(RecipeIngredientDto("Cream", 100.0, "ml")),
        steps = listOf(RecipeStepDto(1, "Boil the pasta"), RecipeStepDto(2, "Add the cream"))
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KitchenQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        remoteRecipes = FakeRemoteRecipes().apply { recipes[alfredo.recipeSourceId] = alfredo }
        remoteFavourites = FakeRemoteFavourites()
        store = RecipeOfflineStore(
            dao = database.recipeCacheDao(),
            currentUserId = { signedInUser },
            maxRecipes = 2,
            clock = { now++ }
        )
        recipes = OfflineRecipeRepository(remoteRecipes, store)
        favourites = OfflineFavouriteRepository(remoteFavourites, store)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun aRecipeLoadedOnlineIsNotMarkedAsAnOfflineCopy() = runBlocking {
        val loaded = recipes.getRecipeWithSource("101").getOrThrow()

        assertEquals("Chicken Alfredo", loaded.recipe.title)
        assertFalse(loaded.isOfflineCopy)
    }

    @Test
    fun aRecipeOpenedOnceCanBeReadOfflineWithAllItsSteps() = runBlocking {
        recipes.getRecipeById("101")
        remoteRecipes.online = false

        val loaded = recipes.getRecipeWithSource("101").getOrThrow()

        assertTrue(loaded.isOfflineCopy)
        assertNotNull(loaded.savedAt)
        assertEquals(alfredo, loaded.recipe)
        assertEquals(2, loaded.recipe.steps.size)
    }

    @Test
    fun getRecipeByIdAlsoFallsBackToTheCopySoCookingModeWorksOffline() = runBlocking {
        recipes.getRecipeById("101")
        remoteRecipes.online = false

        assertEquals("Chicken Alfredo", recipes.getRecipeById("101").getOrThrow().title)
    }

    @Test
    fun aRecipeNeverOpenedGivesTheConnectionErrorWhenOffline() = runBlocking {
        remoteRecipes.online = false

        val error = recipes.getRecipeWithSource("101").exceptionOrNull()

        assertTrue(error is ApiException)
        assertTrue(error!!.isConnectionProblem())
    }

    @Test
    fun aServerTimeoutAlsoShowsTheSavedCopy() = runBlocking {
        recipes.getRecipeById("101")
        remoteRecipes.online = false
        remoteRecipes.failure = ApiException(ApiErrorType.TIMEOUT, "Too slow")

        assertTrue(recipes.getRecipeWithSource("101").getOrThrow().isOfflineCopy)
    }

    @Test
    fun aRealAnswerFromTheServerIsNotHiddenByAnOldCopy() = runBlocking {
        recipes.getRecipeById("101")
        remoteRecipes.recipes.clear()

        val error = recipes.getRecipeWithSource("101").exceptionOrNull() as ApiException

        assertEquals(ApiErrorType.NOT_FOUND, error.type)
        assertFalse(error.isConnectionProblem())
    }

    @Test
    fun anotherAccountCannotSeeTheFirstAccountsCopies() = runBlocking {
        recipes.getRecipeById("101")
        signedInUser = "user-2"
        remoteRecipes.online = false

        assertTrue(recipes.getRecipeWithSource("101").isFailure)
    }

    @Test
    fun nothingIsSavedWhenNobodyIsSignedIn() = runBlocking {
        signedInUser = null
        recipes.getRecipeById("101")
        signedInUser = "user-1"

        assertNull(store.getRecipe("101"))
    }

    @Test
    fun searchStillNeedsTheInternet() = runBlocking {
        remoteRecipes.online = false

        assertTrue(recipes.searchRecipes(query = "pasta").isFailure)
    }

    @Test
    fun oldCopiesAreRemovedButSavedRecipesAreAlwaysKept() = runBlocking {
        // maxRecipes is 2 in this test.
        remoteRecipes.recipes["1"] = alfredo.copy(recipeSourceId = "1", title = "Saved one")
        remoteRecipes.recipes["2"] = alfredo.copy(recipeSourceId = "2", title = "Two")
        remoteRecipes.recipes["3"] = alfredo.copy(recipeSourceId = "3", title = "Three")
        favourites.addFavourite("1", "Saved one", null)

        recipes.getRecipeById("1")
        recipes.getRecipeById("101")
        recipes.getRecipeById("2")
        recipes.getRecipeById("3")

        assertNotNull("A saved recipe is never removed", store.getRecipe("1"))
        assertNull("The oldest unsaved copy is removed", store.getRecipe("101"))
        assertNotNull(store.getRecipe("2"))
        assertNotNull(store.getRecipe("3"))
    }

    @Test
    fun theSavedRecipesListIsShownOfflineInTheSameOrder() = runBlocking {
        remoteFavourites.favourites += FavouriteRecipeDto("a", "1", "Bobotie")
        remoteFavourites.favourites += FavouriteRecipeDto("b", "2", "Bunny chow")
        favourites.getFavouritesWithSource()
        remoteFavourites.online = false

        val loaded = favourites.getFavouritesWithSource().getOrThrow()

        assertTrue(loaded.isOfflineCopy)
        assertEquals(listOf("Bobotie", "Bunny chow"), loaded.favourites.map { it.recipeTitle })
    }

    @Test
    fun addingAndRemovingOnlineUpdatesTheOfflineList() = runBlocking {
        favourites.getFavouritesWithSource()
        favourites.addFavourite("7", "Malva pudding", null)
        favourites.addFavourite("8", "Koeksisters", null)
        favourites.removeFavourite("7")
        remoteFavourites.online = false

        val offlineTitles = favourites.getFavourites().getOrThrow().map { it.recipeTitle }

        assertEquals(listOf("Koeksisters"), offlineTitles)
    }

    @Test
    fun withNoSavedListTheOfflineErrorIsReturned() = runBlocking {
        remoteFavourites.online = false

        assertTrue(favourites.getFavouritesWithSource().isFailure)
    }
}