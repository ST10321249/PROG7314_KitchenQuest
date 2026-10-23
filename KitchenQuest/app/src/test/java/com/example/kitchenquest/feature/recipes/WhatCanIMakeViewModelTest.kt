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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WhatCanIMakeViewModelTest {

    private class FakeRecipeRepository(
        private val result: Result<List<RecipeRecommendationDto>>
    ) : RecipeRepository {
        val recommendationRequests = mutableListOf<List<String>>()

        override suspend fun searchRecipes(
            query: String?,
            diet: String?,
            cuisine: String?,
            maxReadyTime: Int?
        ) = error("not used")

        override suspend fun getRecipeById(id: String) = error("not used")
        override suspend fun getRecommendations() = result

        override suspend fun getRecommendations(
            ingredients: List<String>
        ): Result<List<RecipeRecommendationDto>> {
            recommendationRequests += ingredients
            return result
        }
    }

    private class FakePantryRepository(
        private val names: List<String>
    ) : PantryRepository {
        override suspend fun getPantryItems() = Result.success(
            names.map {
                PantryItemDto(
                    id = it,
                    ingredientName = it,
                    quantity = 1.0,
                    unit = "unit",
                    category = "Other"
                )
            }
        )

        override suspend fun addPantryItem(
            ingredientName: String,
            quantity: Double,
            unit: String,
            category: String,
            expiryDate: String?
        ) = error("not used")

        override suspend fun updatePantryItem(
            id: String,
            ingredientName: String?,
            quantity: Double?,
            unit: String?,
            category: String?,
            expiryDate: String?
        ) = error("not used")

        override suspend fun deletePantryItem(id: String) = error("not used")
    }

    private class FakeShoppingRepository : ShoppingRepository {
        val added = mutableListOf<String>()

        override suspend fun getShoppingItems() = error("not used")

        override suspend fun addShoppingItem(
            name: String,
            quantity: Double?,
            unit: String?
        ): Result<ShoppingItemDto> {
            added.add(name)
            return Result.success(
                ShoppingItemDto(
                    id = name,
                    ingredientName = name
                )
            )
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
    fun loadPopulatesPantrySelectionsAndRecommendations() {
        val recipeRepository = FakeRecipeRepository(
            Result.success(listOf(recommendation(listOf("Cream"))))
        )
        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = recipeRepository,
            pantryRepository = FakePantryRepository(listOf("Milk", "Eggs")),
            shoppingRepository = FakeShoppingRepository()
        )

        viewModel.load()
        val state = viewModel.uiState.value

        assertEquals(listOf("Milk", "Eggs"), state.pantryIngredients)
        assertEquals(setOf("Milk", "Eggs"), state.selectedIngredients)
        assertEquals(listOf("Milk", "Eggs"), recipeRepository.recommendationRequests.single())
        assertEquals(1, state.recommendations.size)
        assertTrue(state.hasLoaded)
    }

    @Test
    fun togglingAnIngredientRefreshesRecommendationsWithTheNewSelection() {
        val recipeRepository = FakeRecipeRepository(Result.success(emptyList()))
        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = recipeRepository,
            pantryRepository = FakePantryRepository(listOf("Milk", "Eggs")),
            shoppingRepository = FakeShoppingRepository()
        )

        viewModel.load()
        viewModel.toggleIngredient("Eggs")

        assertEquals(setOf("Milk"), viewModel.uiState.value.selectedIngredients)
        assertEquals(listOf("Milk"), recipeRepository.recommendationRequests.last())
    }

    @Test
    fun removingTheLastSelectedIngredientClearsRecommendationsWithoutAnotherApiCall() {
        val recipeRepository = FakeRecipeRepository(
            Result.success(listOf(recommendation(emptyList())))
        )
        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = recipeRepository,
            pantryRepository = FakePantryRepository(listOf("Milk")),
            shoppingRepository = FakeShoppingRepository()
        )

        viewModel.load()
        val callsBeforeToggle = recipeRepository.recommendationRequests.size

        viewModel.toggleIngredient("Milk")

        assertTrue(viewModel.uiState.value.selectedIngredients.isEmpty())
        assertTrue(viewModel.uiState.value.recommendations.isEmpty())
        assertEquals(callsBeforeToggle, recipeRepository.recommendationRequests.size)
    }

    @Test
    fun addingMissingIngredientsAddsDistinctItemsAndShowsAConfirmationMessage() {
        val shoppingRepository = FakeShoppingRepository()
        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = FakeRecipeRepository(Result.success(emptyList())),
            pantryRepository = FakePantryRepository(emptyList()),
            shoppingRepository = shoppingRepository
        )

        viewModel.addMissingToShoppingList(
            recommendation(listOf("Cream", "Garlic", "Cream"))
        )

        assertEquals(listOf("Cream", "Garlic"), shoppingRepository.added)
        assertEquals(
            "Added 2 missing ingredients to your shopping list.",
            viewModel.uiState.value.actionMessage
        )
    }

    @Test
    fun clearActionMessageRemovesThePreviousConfirmation() {
        val shoppingRepository = FakeShoppingRepository()
        val viewModel = WhatCanIMakeViewModel(
            recipeRepository = FakeRecipeRepository(Result.success(emptyList())),
            pantryRepository = FakePantryRepository(emptyList()),
            shoppingRepository = shoppingRepository
        )

        viewModel.addMissingToShoppingList(recommendation(listOf("Cream")))
        assertFalse(viewModel.uiState.value.actionMessage.isNullOrBlank())

        viewModel.clearActionMessage()
        assertTrue(viewModel.uiState.value.actionMessage == null)
    }
}
