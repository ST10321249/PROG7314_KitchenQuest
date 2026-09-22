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
            return Result.success(listOf(RecipeSearchResultDto(recipeSourceId = "1", title = "Result")))
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
    fun searchingWithNoQueryAndNoFiltersDoesNothing() {
        viewModel.search()

        assertEquals(0, repository.callCount)
        assertEquals(false, state.hasSearched)
    }

    @Test
    fun selectingADietTriggersASearchWithNoTextQuery() {
        viewModel.onDietSelected("Vegan")

        assertEquals(1, repository.callCount)
        assertNull(repository.lastQuery)
        assertEquals("vegan", repository.lastDiet)
        assertEquals("Vegan", state.selectedDiet)
    }

    @Test
    fun clearingTheOnlyActiveFilterDropsStaleResultsInsteadOfSearchingAgain() {
        viewModel.onCuisineSelected("Italian")
        assertEquals(1, repository.callCount)
        assertTrue(state.hasSearched)

        viewModel.onCuisineSelected("Italian") // toggling the same chip clears it

        assertNull(state.selectedCuisine)
        assertEquals(1, repository.callCount) // no point searching with zero criteria
        assertTrue(state.results.isEmpty())
        assertEquals(false, state.hasSearched) // back to the "search for something" prompt
    }

    @Test
    fun allThreeFilterKindsAreForwardedTogether() {
        viewModel.onDietSelected("Vegetarian")
        viewModel.onCuisineSelected("Mexican")
        viewModel.onMaxReadyTimeSelected(30)

        assertEquals("vegetarian", repository.lastDiet)
        assertEquals("mexican", repository.lastCuisine)
        assertEquals(30, repository.lastMaxReadyTime)
        assertEquals(3, repository.callCount)
    }

    @Test
    fun textQueryAndFiltersCanCombine() {
        viewModel.onQueryChange("pasta")
        viewModel.onDietSelected("Vegan")

        assertEquals("pasta", repository.lastQuery)
        assertEquals("vegan", repository.lastDiet)
    }

    @Test
    fun aSuccessfulSearchMarksHasSearchedAndStoresResults() {
        viewModel.onQueryChange("pasta")
        viewModel.search()

        assertTrue(state.hasSearched)
        assertEquals(1, state.results.size)
        assertEquals(false, state.isLoading)
    }
}
