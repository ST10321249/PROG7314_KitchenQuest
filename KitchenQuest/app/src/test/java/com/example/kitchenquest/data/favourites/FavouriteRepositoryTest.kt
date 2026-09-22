package com.example.kitchenquest.data.favourites

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavouriteRepositoryTest {

    private class FakeFavouriteApi : FavouriteApi {
        val favourites = mutableListOf<FavouriteRecipeDto>()

        override suspend fun getFavourites(): List<FavouriteRecipeDto> = favourites.toList()

        override suspend fun addFavourite(request: AddFavouriteRequest): FavouriteRecipeDto {
            val favourite = FavouriteRecipeDto(
                id = "id-${favourites.size}",
                recipeSourceId = request.recipeSourceId,
                recipeTitle = request.recipeTitle,
                imageUrl = request.imageUrl
            )
            favourites.add(favourite)
            return favourite
        }

        override suspend fun removeFavourite(recipeSourceId: String) {
            favourites.removeAll { it.recipeSourceId == recipeSourceId }
        }
    }

    @Test
    fun addFavouriteAppendsTheRecipe() = runBlocking {
        val api = FakeFavouriteApi()
        val repository = DefaultFavouriteRepository(api)

        repository.addFavourite("123", "Chicken Soup", null)

        assertEquals(1, api.favourites.size)
        assertEquals("Chicken Soup", api.favourites.first().recipeTitle)
    }

    @Test
    fun removeFavouriteDeletesByRecipeSourceId() = runBlocking {
        val api = FakeFavouriteApi()
        val repository = DefaultFavouriteRepository(api)
        repository.addFavourite("123", "Chicken Soup", null)

        repository.removeFavourite("123")

        assertTrue(api.favourites.isEmpty())
    }

    @Test
    fun getFavouritesReturnsEverythingStored() = runBlocking {
        val api = FakeFavouriteApi()
        val repository = DefaultFavouriteRepository(api)
        repository.addFavourite("1", "A", null)
        repository.addFavourite("2", "B", null)

        val result = repository.getFavourites()

        assertEquals(2, result.getOrThrow().size)
    }
}