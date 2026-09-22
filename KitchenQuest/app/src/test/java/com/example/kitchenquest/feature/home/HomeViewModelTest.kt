package com.example.kitchenquest.feature.home

import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.user.UserProfileDto
import com.example.kitchenquest.data.user.UserRepository
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private class FakeUserRepository(
        private val result: Result<UserProfileDto> = Result.success(UserProfileDto(displayName = "Akeev"))
    ) : UserRepository {
        override suspend fun syncUser(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ) = result

        override suspend fun getProfile() = result

        override suspend fun updateProfile(
            displayName: String?,
            dietaryPreferences: List<String>?,
            avoidedIngredients: List<String>?
        ) = result
    }

    private class FakePantryRepository(
        private val result: Result<List<PantryItemDto>>,
        private val gate: CompletableDeferred<Unit>? = null
    ) : PantryRepository {
        var fetchCount = 0

        override suspend fun getPantryItems(): Result<List<PantryItemDto>> {
            fetchCount++
            gate?.await()
            return result
        }
        override suspend fun addPantryItem(
            ingredientName: String, quantity: Double, unit: String, category: String, expiryDate: String?
        ) = error("not used")

        override suspend fun updatePantryItem(
            id: String, ingredientName: String?, quantity: Double?, unit: String?, category: String?, expiryDate: String?
        ) = error("not used")

        override suspend fun deletePantryItem(id: String) = error("not used")
    }

    private class FakeRecipeRepository(
        private val result: Result<List<RecipeRecommendationDto>> = Result.success(emptyList())
    ) : RecipeRepository {
        override suspend fun searchRecipes(query: String?, diet: String?, cuisine: String?, maxReadyTime: Int?) =
            error("not used")

        override suspend fun getRecipeById(id: String) = error("not used")
        override suspend fun getRecommendations() = result
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun pantryItem(days: Long?) = PantryItemDto(
        id = "id",
        ingredientName = "Milk",
        quantity = 1.0,
        unit = "l",
        category = "Dairy",
        expiryDate = days?.let { LocalDate.now().plusDays(it).toString() }
    )

    @Test
    fun loadingComputesPantryCountsAndDisplayNameFromRealRepositories() {
        val pantryItems = listOf(pantryItem(days = 1), pantryItem(days = 10), pantryItem(days = null))

        val viewModel = HomeViewModel(
            userRepository = FakeUserRepository(),
            pantryRepository = FakePantryRepository(Result.success(pantryItems)),
            recipeRepository = FakeRecipeRepository()
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertEquals("Akeev", state.displayName)
        assertEquals(3, state.pantryItemCount)
        assertEquals(1, state.expiringSoonCount)
        assertTrue(state.hasLoaded)
        assertFalse(state.isLoading)
    }

    @Test
    fun recommendationsLoadIndependentlyOfPantryStats() {
        val recommendations = listOf(
            RecipeRecommendationDto(
                recipeSourceId = "1",
                title = "Pasta",
                imageUrl = null,
                matchRatio = 0.75,
                missingIngredients = listOf("Cream")
            )
        )

        val viewModel = HomeViewModel(
            userRepository = FakeUserRepository(),
            pantryRepository = FakePantryRepository(Result.success(emptyList())),
            recipeRepository = FakeRecipeRepository(Result.success(recommendations))
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertFalse(state.recommendationsLoading)
        assertEquals(recommendations, state.recommendations)
    }

    @Test
    fun aFailedPantryLoadShowsAnErrorButDoesNotCrash() {
        val viewModel = HomeViewModel(
            userRepository = FakeUserRepository(),
            pantryRepository = FakePantryRepository(Result.failure(RuntimeException("offline"))),
            recipeRepository = FakeRecipeRepository()
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertTrue(state.hasLoaded)
        assertEquals("offline", state.errorMessage)
        assertEquals(0, state.pantryItemCount)
    }

    @Test
    fun aSecondLoadCallWhileTheFirstIsStillInFlightIsIgnored() {
        val gate = CompletableDeferred<Unit>()
        val pantryRepository = FakePantryRepository(Result.success(emptyList()), gate)

        val viewModel = HomeViewModel(
            userRepository = FakeUserRepository(),
            pantryRepository = pantryRepository,
            recipeRepository = FakeRecipeRepository()
        )

        viewModel.load() // suspends inside getPantryItems(), still "loading"
        viewModel.load() // should be a no-op: isLoading is already true

        gate.complete(Unit) // let the first call finish

        assertEquals(1, pantryRepository.fetchCount)
        assertTrue(viewModel.uiState.value.hasLoaded)
    }

    @Test
    fun callingLoadAgainAfterCompletionRefreshesTheData() {
        val pantryRepository = FakePantryRepository(Result.success(listOf(pantryItem(days = 1))))

        val viewModel = HomeViewModel(
            userRepository = FakeUserRepository(),
            pantryRepository = pantryRepository,
            recipeRepository = FakeRecipeRepository()
        )

        viewModel.load()
        viewModel.load() // Home refreshes on re-entry, unlike Settings

        assertEquals(2, pantryRepository.fetchCount)
    }
}
