package com.example.kitchenquest.feature.profile

import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.data.favourites.FavouriteRepository
import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.user.UserProfileDto
import com.example.kitchenquest.data.user.UserRepository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private class FakeUserRepository(
        private val result: Result<UserProfileDto>
    ) : UserRepository {
        override suspend fun syncUser(displayName: String?, dietaryPreferences: List<String>?, avoidedIngredients: List<String>?) = result
        override suspend fun getProfile() = result
        override suspend fun updateProfile(displayName: String?, dietaryPreferences: List<String>?, avoidedIngredients: List<String>?) = result
    }

    private class FakeHistoryRepository(private val count: Int) : HistoryRepository {
        override suspend fun getHistory() = Result.success(List(count) { CookingHistoryDto(id = "$it") })
        override suspend fun addHistoryEntry(recipeSourceId: String, recipeTitle: String, servings: Int?, rating: Int?, difficultyFeedback: String?) =
            error("not used")
        override suspend fun getNote(recipeId: String) = error("not used")
        override suspend fun saveNote(recipeSourceId: String, note: String) = error("not used")
    }

    private class FakeFavouriteRepository(private val count: Int) : FavouriteRepository {
        override suspend fun getFavourites() = Result.success(
            List(count) { FavouriteRecipeDto(id = "$it", recipeSourceId = "$it", recipeTitle = "Recipe $it") }
        )
        override suspend fun addFavourite(recipeSourceId: String, recipeTitle: String, imageUrl: String?) = error("not used")
        override suspend fun removeFavourite(recipeSourceId: String) = error("not used")
    }

    private class FakePantryRepository(
        private val result: Result<List<PantryItemDto>>
    ) : PantryRepository {
        override suspend fun getPantryItems() = result
        override suspend fun addPantryItem(ingredientName: String, quantity: Double, unit: String, category: String, expiryDate: String?) =
            error("not used")
        override suspend fun updatePantryItem(id: String, ingredientName: String?, quantity: Double?, unit: String?, category: String?, expiryDate: String?) =
            error("not used")
        override suspend fun deletePantryItem(id: String) = error("not used")
    }

    private fun pantryItem(days: Long?) = PantryItemDto(
        id = "id-$days",
        ingredientName = "Milk",
        quantity = 1.0,
        unit = "l",
        category = "Dairy",
        expiryDate = days?.let { LocalDate.now().plusDays(it).toString() }
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPullsRealCountsFromEachRepositoryRatherThanFabricatingThem() {
        val viewModel = ProfileViewModel(
            userRepository = FakeUserRepository(Result.success(UserProfileDto(displayName = "Akeev", email = "a@example.com"))),
            historyRepository = FakeHistoryRepository(count = 4),
            favouriteRepository = FakeFavouriteRepository(count = 7),
            pantryRepository = FakePantryRepository(Result.success(listOf(pantryItem(1), pantryItem(30))))
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertEquals("Akeev", state.displayName)
        assertEquals("a@example.com", state.email)
        assertEquals(4, state.mealsCooked)
        assertEquals(7, state.savedRecipesCount)
        assertEquals(2, state.pantryItemCount)
        assertEquals(1, state.expiringSoonCount)
        assertTrue(state.hasLoaded)
    }

    @Test
    fun zeroActivityShowsZeroesNotPlaceholders() {
        val viewModel = ProfileViewModel(
            userRepository = FakeUserRepository(Result.success(UserProfileDto())),
            historyRepository = FakeHistoryRepository(count = 0),
            favouriteRepository = FakeFavouriteRepository(count = 0),
            pantryRepository = FakePantryRepository(Result.success(emptyList()))
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertEquals(0, state.mealsCooked)
        assertEquals(0, state.savedRecipesCount)
        assertEquals(0, state.pantryItemCount)
        assertEquals(0, state.expiringSoonCount)
    }

    @Test
    fun aFailedPantryLoadStillShowsWhateverElseSucceeded() {
        val viewModel = ProfileViewModel(
            userRepository = FakeUserRepository(Result.success(UserProfileDto(displayName = "Akeev"))),
            historyRepository = FakeHistoryRepository(count = 2),
            favouriteRepository = FakeFavouriteRepository(count = 1),
            pantryRepository = FakePantryRepository(Result.failure(RuntimeException("offline")))
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertEquals("Akeev", state.displayName)
        assertEquals(2, state.mealsCooked)
        assertEquals("offline", state.errorMessage)
        assertTrue(state.hasLoaded)
    }
}
