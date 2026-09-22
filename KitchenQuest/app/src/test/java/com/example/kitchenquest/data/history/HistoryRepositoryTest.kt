package com.example.kitchenquest.data.history

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryRepositoryTest {

    private class FakeHistoryApi : HistoryApi {
        val entries = mutableListOf<CookingHistoryDto>()
        val notes = mutableMapOf<String, String>()

        override suspend fun getHistory(): List<CookingHistoryDto> = entries.toList()

        override suspend fun addHistoryEntry(request: AddHistoryRequest): CookingHistoryDto {
            val entry = CookingHistoryDto(
                id = "id-${entries.size}",
                recipeSourceId = request.recipeSourceId,
                recipeTitle = request.recipeTitle,
                servings = request.servings,
                rating = request.rating,
                difficultyFeedback = request.difficultyFeedback
            )
            entries.add(entry)
            return entry
        }

        override suspend fun getNote(recipeId: String): RecipeNoteDto? {
            val note = notes[recipeId] ?: return null
            return RecipeNoteDto(recipeSourceId = recipeId, note = note)
        }

        override suspend fun saveNote(request: SaveRecipeNoteRequest): RecipeNoteDto {
            notes[request.recipeSourceId] = request.note
            return RecipeNoteDto(recipeSourceId = request.recipeSourceId, note = request.note)
        }
    }

    @Test
    fun addHistoryEntryStoresTheRatingAndDifficulty() = runBlocking {
        val api = FakeHistoryApi()
        val repository = DefaultHistoryRepository(api)

        repository.addHistoryEntry("1", "Chicken Soup", null, 5, "Easy")

        assertEquals(1, api.entries.size)
        assertEquals(5, api.entries.first().rating)
        assertEquals("Easy", api.entries.first().difficultyFeedback)
    }

    @Test
    fun getNoteReturnsNullWhenNoneSaved() = runBlocking {
        val api = FakeHistoryApi()
        val repository = DefaultHistoryRepository(api)

        val result = repository.getNote("1")

        assertNull(result.getOrThrow())
    }

    @Test
    fun saveNoteThenGetNoteReturnsWhatWasSaved() = runBlocking {
        val api = FakeHistoryApi()
        val repository = DefaultHistoryRepository(api)

        repository.saveNote("1", "Add more salt next time")
        val result = repository.getNote("1")

        assertEquals("Add more salt next time", result.getOrThrow()?.note)
    }
}