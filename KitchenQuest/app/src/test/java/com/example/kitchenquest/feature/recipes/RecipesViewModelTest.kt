package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.recipes.RecipeDetailDto
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModelTest {

    private class FakeRecipeRepository : RecipeRepository {
        var lastQuery: String? = "unset"
        var lastDiet: String? = "unset"
        var lastCuisine: String? = "unset"
        var lastMaxReadyTime: Int? = -1
        var callCount = 0
        var result: Result<List<RecipeSearchResultDto>> = Result.success(
            listOf(
                RecipeSearchResultDto(
                    recipeSourceId = "1",
                    title = "Result"
                )
            )
        )

        override suspend fun searchRecipes(
            query: String?,
            diet: String?,
            cuisine: String?,
            maxReadyTime: Int?
        ): Result<List<RecipeSearchResultDto>> {
            callCount++
            lastQuery = query
            lastDiet = diet
            lastCuisine = cuisine
            lastMaxReadyTime = maxReadyTime
            return result
        }

        override suspend fun getRecipeById(id: String): Result<RecipeDetailDto> = error("not used")
        override suspend fun getRecommendations(): Result<List<RecipeRecommendationDto>> = error("not used")
    }

    private lateinit var repository: FakeRecipeRepository
    private lateinit var viewModel: RecipesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeRecipeRepository()
        viewModel = RecipesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun aPlainSearchWithNoQueryUsesNoFilters() {
        viewModel.search()

        assertEquals(1, repository.callCount)
        assertNull(repository.lastQuery)
        assertNull(repository.lastDiet)
        assertNull(repository.lastCuisine)
        assertNull(repository.lastMaxReadyTime)
        assertTrue(state.hasLoaded)
        assertEquals(1, state.results.size)
    }

    @Test
    fun vegetarianFilterTriggersADietSearch() {
        viewModel.onFilterSelected("Vegetarian")

        assertEquals(1, repository.callCount)
        assertNull(repository.lastQuery)
        assertEquals("vegetarian", repository.lastDiet)
        assertEquals("Vegetarian", state.selectedFilter)
    }

    @Test
    fun underThirtyMinutesFilterIsForwarded() {
        viewModel.onFilterSelected("Under 30 min")

        assertEquals(1, repository.callCount)
        assertEquals(30, repository.lastMaxReadyTime)
    }

    @Test
    fun chickenFilterUsesChickenAsTheSearchQuery() {
        viewModel.onFilterSelected("Chicken")

        assertEquals(1, repository.callCount)
        assertEquals("chicken", repository.lastQuery)
    }

    @Test
    fun typedQueryTakesPriorityOverTheChickenFilterQuery() {
        viewModel.onFilterSelected("Chicken")
        viewModel.onQueryChange("pasta")
        viewModel.search()

        assertEquals("pasta", repository.lastQuery)
    }

    @Test
    fun searchCategorySetsTheQueryAndResetsTheFilter() {
        viewModel.onFilterSelected("Vegetarian")
        viewModel.searchCategory("Breakfast")

        assertEquals("Breakfast", state.searchQuery)
        assertEquals("All", state.selectedFilter)
        assertEquals("Breakfast", repository.lastQuery)
    }

    @Test
    fun failedSearchStoresTheErrorAndStopsLoading() {
        repository.result = Result.failure(RuntimeException("offline"))

        viewModel.search()

        assertEquals("offline", state.errorMessage)
        assertTrue(state.hasLoaded)
        assertEquals(false, state.isLoading)
    }
}
