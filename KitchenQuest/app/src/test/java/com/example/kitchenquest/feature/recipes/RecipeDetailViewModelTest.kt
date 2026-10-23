package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.favourites.FavouriteRecipeDto
import com.example.kitchenquest.data.favourites.FavouriteRepository
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.PantryRepository
import com.example.kitchenquest.data.recipes.RecipeDetailDto
import com.example.kitchenquest.data.recipes.RecipeIngredientDto
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
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
class RecipeDetailViewModelTest {

    private class FakeRecipeRepository(private val recipe: RecipeDetailDto) : RecipeRepository {
        override suspend fun searchRecipes(query: String?, diet: String?, cuisine: String?, maxReadyTime: Int?) =
            Result.success(emptyList<RecipeSearchResultDto>())

        override suspend fun getRecipeById(id: String) = Result.success(recipe)

        override suspend fun getRecommendations() = Result.success(emptyList<RecipeRecommendationDto>())
    }

    private class FakePantryRepository(private val heldNames: List<String>) : PantryRepository {
        override suspend fun getPantryItems() = Result.success(
            heldNames.map { name ->
                PantryItemDto(id = name, ingredientName = name, quantity = 1.0, unit = "unit", category = "General", expiryDate = null)
            }
        )

        override suspend fun addPantryItem(ingredientName: String, quantity: Double, unit: String, category: String, expiryDate: String?) =
            Result.success(PantryItemDto(id = "x", ingredientName = ingredientName, quantity = quantity, unit = unit, category = category, expiryDate = expiryDate))

        override suspend fun updatePantryItem(id: String, ingredientName: String?, quantity: Double?, unit: String?, category: String?, expiryDate: String?) =
            Result.success(PantryItemDto(id = id, ingredientName = ingredientName ?: "", quantity = quantity ?: 0.0, unit = unit ?: "", category = category ?: "", expiryDate = expiryDate))

        override suspend fun deletePantryItem(id: String) = Result.success(Unit)
    }

    private class FakeShoppingRepository : ShoppingRepository {
        val addedItems = mutableListOf<String>()

        override suspend fun getShoppingItems() = Result.success(emptyList<ShoppingItemDto>())

        override suspend fun addShoppingItem(name: String, quantity: Double?, unit: String?): Result<ShoppingItemDto> {
            addedItems.add(name)
            return Result.success(ShoppingItemDto(id = "x", ingredientName = name, quantity = quantity, unit = unit ?: "", isPurchased = false))
        }

        override suspend fun setPurchased(id: String, purchased: Boolean) =
            Result.success(ShoppingItemDto(id = id, isPurchased = purchased))

        override suspend fun deleteShoppingItem(id: String) = Result.success(Unit)
    }

    private class FakeFavouriteRepository : FavouriteRepository {
        val favourites = mutableListOf<FavouriteRecipeDto>()
        var addCalls = 0
        var removeCalls = 0

        override suspend fun getFavourites() = Result.success(favourites.toList())

        override suspend fun addFavourite(recipeSourceId: String, recipeTitle: String, imageUrl: String?): Result<FavouriteRecipeDto> {
            addCalls++
            val favourite = FavouriteRecipeDto(id = "x", recipeSourceId = recipeSourceId, recipeTitle = recipeTitle, imageUrl = imageUrl)
            favourites.add(favourite)
            return Result.success(favourite)
        }

        override suspend fun removeFavourite(recipeSourceId: String): Result<Unit> {
            removeCalls++
            favourites.removeAll { it.recipeSourceId == recipeSourceId }
            return Result.success(Unit)
        }
    }

    private lateinit var shoppingRepository: FakeShoppingRepository
    private lateinit var favouriteRepository: FakeFavouriteRepository
    private lateinit var viewModel: RecipeDetailViewModel

    private val recipe = RecipeDetailDto(
        recipeSourceId = "1",
        title = "Chicken Alfredo",
        servings = 2,
        ingredients = listOf(
            RecipeIngredientDto("Chicken breast", 200.0, "g"),
            RecipeIngredientDto("Cream", 100.0, "ml")
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        shoppingRepository = FakeShoppingRepository()
        favouriteRepository = FakeFavouriteRepository()
        viewModel = RecipeDetailViewModel(
            recipeRepository = FakeRecipeRepository(recipe),
            pantryRepository = FakePantryRepository(heldNames = listOf("Chicken breast")),
            shoppingRepository = shoppingRepository,
            favouriteRepository = favouriteRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun loadingMarksHeldIngredientsFromThePantry() {
        viewModel.load("1")

        assertTrue(state.heldIngredientNames.contains("chicken breast"))
        assertFalse(state.heldIngredientNames.contains("cream"))
    }

    @Test
    fun increasingServingsScalesUpFromTheRecipeBase() {
        viewModel.load("1")

        viewModel.increaseServings()

        assertEquals(3, state.servings)
    }

    @Test
    fun servingsNeverGoBelowOne() {
        viewModel.load("1")

        repeat(5) { viewModel.decreaseServings() }

        assertEquals(1, state.servings)
    }

    @Test
    fun addingMissingIngredientsOnlySendsWhatIsNotHeld() {
        viewModel.load("1")

        viewModel.addMissingIngredientsToList()

        assertEquals(listOf("Cream"), shoppingRepository.addedItems)
    }

    @Test
    fun togglingFavouriteAddsThenRemoves() {
        viewModel.load("1")

        viewModel.toggleFavourite()
        assertTrue(state.isFavourite)
        assertEquals(1, favouriteRepository.addCalls)

        viewModel.toggleFavourite()
        assertFalse(state.isFavourite)
        assertEquals(1, favouriteRepository.removeCalls)
    }
}