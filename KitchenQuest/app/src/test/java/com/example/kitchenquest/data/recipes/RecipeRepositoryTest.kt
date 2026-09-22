package com.example.kitchenquest.data.recipes

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeRepositoryTest {

    private class FakeRecipeApi : RecipeApi {
        var searchResults: List<RecipeSearchResultDto> = emptyList()
        var detail: RecipeDetailDto? = null
        var recommendations: List<RecipeRecommendationDto> = emptyList()
        var lastSearchQuery: String? = null

        override suspend fun searchRecipes(
            query: String?,
            diet: String?,
            cuisine: String?,
            maxReadyTime: Int?,
            number: Int?
        ): List<RecipeSearchResultDto> {
            lastSearchQuery = query
            return searchResults
        }

        override suspend fun getRecipeById(id: String): RecipeDetailDto {
            return detail ?: throw NoSuchElementException("No recipe with id $id")
        }

        override suspend fun getRecommendations(number: Int?): RecipeRecommendationsResponseDto {
            return RecipeRecommendationsResponseDto(recommendations)
        }
    }

    @Test
    fun searchRecipesPassesTheQueryThrough() = runBlocking {
        val api = FakeRecipeApi().apply {
            searchResults = listOf(RecipeSearchResultDto(recipeSourceId = "1", title = "Chicken Soup"))
        }
        val repository = DefaultRecipeRepository(api)

        val result = repository.searchRecipes(query = "chicken")

        assertEquals("chicken", api.lastSearchQuery)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("Chicken Soup", result.getOrThrow().first().title)
    }

    @Test
    fun getRecipeByIdReturnsTheDetail() = runBlocking {
        val api = FakeRecipeApi().apply {
            detail = RecipeDetailDto(recipeSourceId = "42", title = "Pasta Bake")
        }
        val repository = DefaultRecipeRepository(api)

        val result = repository.getRecipeById("42")

        assertEquals("Pasta Bake", result.getOrThrow().title)
    }

    @Test
    fun getRecommendationsUnwrapsTheRecipesList() = runBlocking {
        val api = FakeRecipeApi().apply {
            recommendations = listOf(
                RecipeRecommendationDto(recipeSourceId = "1", title = "A", matchRatio = 0.8),
                RecipeRecommendationDto(recipeSourceId = "2", title = "B", matchRatio = 0.5)
            )
        }
        val repository = DefaultRecipeRepository(api)

        val result = repository.getRecommendations()

        assertEquals(2, result.getOrThrow().size)
        assertEquals(0.8, result.getOrThrow().first().matchRatio, 0.0001)
    }
}