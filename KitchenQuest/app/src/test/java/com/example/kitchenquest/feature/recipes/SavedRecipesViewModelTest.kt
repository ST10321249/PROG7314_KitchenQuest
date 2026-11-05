package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.data.favourites.FavouriteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedRecipesViewModelTest {

    private class FakeFavouriteRepository(
        private val favourites: List<FavouriteRecipeDto>
    ) : FavouriteRepository {
        override suspend fun getFavourites() = Result.success(favourites)
        override suspend fun addFavourite(recipeSourceId: String, recipeTitle: String, imageUrl: String?) =
            error("not used")

        override suspend fun removeFavourite(recipeSourceId: String) = Result.success(Unit)
    }

    private fun favourite(id: String, title: String) =
        FavouriteRecipeDto(id = id, recipeSourceId = id, recipeTitle = title, imageUrl = null)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun searchFiltersByTitleCaseInsensitively() {
        val viewModel = SavedRecipesViewModel(
            FakeFavouriteRepository(
                listOf(favourite("1", "Chicken Alfredo"), favourite("2", "Beef Tacos"))
            )
        )

        viewModel.load()
        viewModel.onQueryChange("chicken")

        assertEquals(listOf("Chicken Alfredo"), viewModel.uiState.value.filteredFavourites.map { it.recipeTitle })
    }

    @Test
    fun aBlankQueryShowsEverything() {
        val viewModel = SavedRecipesViewModel(
            FakeFavouriteRepository(listOf(favourite("1", "Chicken Alfredo"), favourite("2", "Beef Tacos")))
        )

        viewModel.load()

        assertEquals(2, viewModel.uiState.value.filteredFavourites.size)
    }

    @Test
    fun removingAFavouriteDropsItFromTheList() {
        val viewModel = SavedRecipesViewModel(
            FakeFavouriteRepository(listOf(favourite("1", "Chicken Alfredo")))
        )

        viewModel.load()
        viewModel.removeFavourite("1")

        assertEquals(0, viewModel.uiState.value.favourites.size)
    }
}
