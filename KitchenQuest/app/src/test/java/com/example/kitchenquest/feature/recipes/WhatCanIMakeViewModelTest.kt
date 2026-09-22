package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.shopping.ShoppingRepository
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
class WhatCanIMakeViewModelTest {

    private class FakeRecipeRepository(
        private val result: Result<List<RecipeRecommendationDto>>
    ) : RecipeRepository {
        override suspend fun searchRecipes(query: String?, diet: String?, cuisine: String?, maxReadyTime: Int?) =
            error("not used")

        override suspend fun getRecipeById(id: String) = error("not used")
        override suspend fun getRecommendations() = result
    }

    private class FakePantryRepository(
        private val names: List<String>
    ) : PantryRepository {
        override suspend fun getPantryItems() = Result.success(
            names.map { PantryItemDto(id = it, ingredientName = it, quantity = 1.0, unit = "unit", category = "Meat") }
        )

        override suspend fun addPantryItem(
            ingredientName: String, quantity: Double, unit: String, category: String, expiryDate: String?
        ) = error("not used")

        override suspend fun updatePantryItem(
            id: String, ingredientName: String?, quantity: Double?, unit: String?, category: String?, expiryDate: String?
        ) = error("not used")

        override suspend fun deletePantryItem(id: String) = error("not used")
    }

    private class FakeShoppingRepository : ShoppingRepository {
        val added = mutableListOf<String>()

        override suspend fun getShoppingItems() = error("not used")

        override suspend fun addShoppingItem(name: String, quantity: Double?, unit: String?): Result<ShoppingItemDto> {
            added.add(name)
            return Result.success(ShoppingItemDto(id = name, ingredientName = name))
        }

        override suspend fun setPurchased(id: String, purchased: Boolean) = error("not used")
        override suspend fun deleteShoppingItem(id: String) = error("not used")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun recommendation(missing: List<String>) = RecipeRecommendationDto(
        recipeSourceId = "recipe-1",
        title = "Pasta",
        imageUrl = null,
        matchRatio = 0.5,
        missingIngredients = missing
    )

    @Test
    fun loadPopulatesBothPantryNamesAndRecommendations() {
        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = FakeRecipeRepository(Result.success(listOf(recommendation(listOf("Cream"))))),
            pantryRepository = FakePantryRepository(listOf("Milk", "Eggs")),
            shoppingRepository = FakeShoppingRepository()
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertEquals(listOf("Milk", "Eggs"), state.pantryIngredientNames)
        assertEquals(1, state.recommendations.size)
        assertTrue(state.hasLoaded)
    }

    @Test
    fun addingMissingIngredientsSendsEachOneToTheShoppingList() {
        val shoppingRepository = FakeShoppingRepository()

        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = FakeRecipeRepository(Result.success(emptyList())),
            pantryRepository = FakePantryRepository(emptyList()),
            shoppingRepository = shoppingRepository
        )

        viewModel.addMissingToShoppingList(recommendation(listOf("Cream", "Garlic")))

        assertEquals(listOf("Cream", "Garlic"), shoppingRepository.added)
        assertTrue("recipe-1" in viewModel.uiState.value.addedToShoppingList)
    }

    @Test
    fun addingTheSameRecipesMissingIngredientsTwiceOnlySendsThemOnce() {
        val shoppingRepository = FakeShoppingRepository()

        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = FakeRecipeRepository(Result.success(emptyList())),
            pantryRepository = FakePantryRepository(emptyList()),
            shoppingRepository = shoppingRepository
        )

        val recipe = recommendation(listOf("Cream"))
        viewModel.addMissingToShoppingList(recipe)
        viewModel.addMissingToShoppingList(recipe)

        assertEquals(listOf("Cream"), shoppingRepository.added)
    }
}
