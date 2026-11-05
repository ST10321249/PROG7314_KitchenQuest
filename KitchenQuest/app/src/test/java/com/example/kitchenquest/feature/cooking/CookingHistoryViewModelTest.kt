package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.history.RecipeNoteDto
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
import java.time.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CookingHistoryViewModelTest {

    private class FakeHistoryRepository : HistoryRepository {
        var entries: List<CookingHistoryDto> = emptyList()
        var notes: Map<String, String> = emptyMap()

        override suspend fun getHistory(): Result<List<CookingHistoryDto>> =
            Result.success(entries)

        override suspend fun addHistoryEntry(
            recipeSourceId: String,
            recipeTitle: String,
            servings: Int?,
            rating: Int?,
            difficultyFeedback: String?
        ): Result<CookingHistoryDto> = Result.success(
            CookingHistoryDto(
                recipeSourceId = recipeSourceId,
                recipeTitle = recipeTitle,
                servings = servings,
                rating = rating,
                difficultyFeedback = difficultyFeedback
            )
        )

        override suspend fun getNote(recipeId: String): Result<RecipeNoteDto?> =
            Result.success(
                notes[recipeId]?.let {
                    RecipeNoteDto(
                        recipeSourceId = recipeId,
                        note = it
                    )
                }
            )

        override suspend fun saveNote(
            recipeSourceId: String,
            note: String
        ): Result<RecipeNoteDto> = Result.success(
            RecipeNoteDto(
                recipeSourceId = recipeSourceId,
                note = note
            )
        )
    }

    private lateinit var repository: FakeHistoryRepository
    private lateinit var viewModel: CookingHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeHistoryRepository()
        viewModel = CookingHistoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadFetchesHistoryAndAvailableNotes() {
        repository.entries = listOf(
            entry("1", "Soup", 5),
            entry("2", "Pasta", 3)
        )
        repository.notes = mapOf("1" to "Use less salt")

        viewModel.load()

        val state = viewModel.uiState.value
        assertTrue(state.hasLoaded)
        assertFalse(state.isLoading)
        assertEquals(2, state.entries.size)
        assertEquals("Use less salt", state.notesByRecipeId["1"])
    }

    @Test
    fun ratedFiveFilterOnlyShowsFiveStarMeals() {
        repository.entries = listOf(
            entry("1", "Soup", 5),
            entry("2", "Pasta", 4)
        )
        viewModel.load()

        viewModel.selectFilter(CookingHistoryFilter.RATED_FIVE)

        assertEquals(
            listOf("Soup"),
            viewModel.uiState.value.filteredEntries.map { it.recipeTitle }
        )
    }

    @Test
    fun withNotesFilterOnlyShowsMealsWithSavedNotes() {
        repository.entries = listOf(
            entry("1", "Soup", 5),
            entry("2", "Pasta", 4)
        )
        repository.notes = mapOf("2" to "Add chilli next time")
        viewModel.load()

        viewModel.selectFilter(CookingHistoryFilter.WITH_NOTES)

        assertEquals(
            listOf("Pasta"),
            viewModel.uiState.value.filteredEntries.map { it.recipeTitle }
        )
    }

    @Test
    fun thisMonthFilterExcludesOlderMeals() {
        repository.entries = listOf(
            entry(
                id = "1",
                title = "Current",
                rating = 5,
                cookedAt = OffsetDateTime.now().toString()
            ),
            entry(
                id = "2",
                title = "Older",
                rating = 5,
                cookedAt = OffsetDateTime.now().minusMonths(2).toString()
            )
        )
        viewModel.load()

        viewModel.selectFilter(CookingHistoryFilter.THIS_MONTH)

        assertEquals(
            listOf("Current"),
            viewModel.uiState.value.filteredEntries.map { it.recipeTitle }
        )
    }

    private fun entry(
        id: String,
        title: String,
        rating: Int,
        cookedAt: String = OffsetDateTime.now().toString()
    ) = CookingHistoryDto(
        id = id,
        recipeSourceId = id,
        recipeTitle = title,
        cookedAt = cookedAt,
        rating = rating,
        difficultyFeedback = "Easy"
    )
}
