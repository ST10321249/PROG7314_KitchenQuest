package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.history.RecipeNoteDto
import com.example.kitchenquest.data.recipes.RecipeDetailDto
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import com.example.kitchenquest.data.recipes.RecipeStepDto
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
class CookingViewModelTest {

    private class FakeRecipeRepository(val recipe: RecipeDetailDto) : RecipeRepository {
        override suspend fun searchRecipes(query: String?, diet: String?, cuisine: String?, maxReadyTime: Int?) =
            Result.success(emptyList<RecipeSearchResultDto>())

        override suspend fun getRecipeById(id: String) = Result.success(recipe)

        override suspend fun getRecommendations() = Result.success(emptyList<RecipeRecommendationDto>())
    }

    private class FakeHistoryRepository : HistoryRepository {
        var addHistoryCalls = 0
        var saveNoteCalls = 0
        var lastRating: Int? = null
        var lastDifficulty: String? = null
        var lastNote: String? = null

        override suspend fun getHistory() = Result.success(emptyList<CookingHistoryDto>())

        override suspend fun addHistoryEntry(
            recipeSourceId: String,
            recipeTitle: String,
            servings: Int?,
            rating: Int?,
            difficultyFeedback: String?
        ): Result<CookingHistoryDto> {
            addHistoryCalls++
            lastRating = rating
            lastDifficulty = difficultyFeedback
            return Result.success(CookingHistoryDto(recipeSourceId = recipeSourceId, recipeTitle = recipeTitle))
        }

        override suspend fun getNote(recipeId: String) = Result.success<RecipeNoteDto?>(null)

        override suspend fun saveNote(recipeSourceId: String, note: String): Result<RecipeNoteDto> {
            saveNoteCalls++
            lastNote = note
            return Result.success(RecipeNoteDto(recipeSourceId = recipeSourceId, note = note))
        }
    }

    private lateinit var recipeRepository: FakeRecipeRepository
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var viewModel: CookingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        recipeRepository = FakeRecipeRepository(sampleRecipe())
        historyRepository = FakeHistoryRepository()
        viewModel = CookingViewModel(recipeRepository, historyRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun loadingARecipeStartsOnTheFirstStep() {
        viewModel.loadRecipe("123")

        assertEquals(0, state.currentStepIndex)
        assertEquals("Test Recipe", state.recipe?.title)
    }

    @Test
    fun nextStepDoesNotGoPastTheLastStep() {
        viewModel.loadRecipe("123")

        repeat(5) { viewModel.nextStep() }

        assertEquals(recipeRepository.recipe.steps.size - 1, state.currentStepIndex)
    }

    @Test
    fun previousStepDoesNotGoBeforeTheFirstStep() {
        viewModel.loadRecipe("123")

        viewModel.previousStep()

        assertEquals(0, state.currentStepIndex)
    }

    @Test
    fun startingAStepTimerLabelsItByStepNumber() {
        viewModel.loadRecipe("123")
        viewModel.nextStep()

        viewModel.startTimerForCurrentStep(10)

        assertEquals(1, state.timers.size)
        assertEquals("Step 2", state.timers.first().label)
        assertEquals(TimerSource.RECIPE, state.timers.first().sourceType)
        assertEquals(600, state.timers.first().remainingSeconds)
    }

    @Test
    fun recipeAndStandaloneTimersShareOneList() {
        viewModel.loadRecipe("123")
        viewModel.startTimerForCurrentStep(10)
        viewModel.startStandaloneTimer(5, "Kitchen Timer")

        assertEquals(2, state.timers.size)
        assertTrue(state.timers.any { it.sourceType == TimerSource.RECIPE })
        assertTrue(state.timers.any { it.sourceType == TimerSource.STANDALONE })
    }

    @Test
    fun addingAMinuteExtendsTheTimer() {
        viewModel.startStandaloneTimer(1, "Kitchen Timer")
        val timerId = state.timers.first().id

        viewModel.addMinuteToTimer(timerId)

        assertEquals(120, state.timers.first().remainingSeconds)
    }

    @Test
    fun togglingPauseFlipsTheRunningState() {
        viewModel.startStandaloneTimer(1, "Kitchen Timer")
        val timerId = state.timers.first().id

        viewModel.togglePauseTimer(timerId)
        assertFalse(state.timers.first().isRunning)

        viewModel.togglePauseTimer(timerId)
        assertTrue(state.timers.first().isRunning)
    }

    @Test
    fun cancellingATimerRemovesItFromTheList() {
        viewModel.startStandaloneTimer(1, "Kitchen Timer")
        val timerId = state.timers.first().id

        viewModel.cancelTimer(timerId)

        assertTrue(state.timers.isEmpty())
    }

    @Test
    fun completingCookingRecordsHistoryAndSavesANonBlankNote() {
        viewModel.loadRecipe("123")

        var completed = false
        viewModel.completeCooking(rating = 4, difficulty = "Medium", note = "Add more salt") {
            completed = true
        }

        assertTrue(completed)
        assertEquals(1, historyRepository.addHistoryCalls)
        assertEquals(4, historyRepository.lastRating)
        assertEquals(1, historyRepository.saveNoteCalls)
        assertEquals("Add more salt", historyRepository.lastNote)
    }

    @Test
    fun completingCookingSkipsSavingABlankNote() {
        viewModel.loadRecipe("123")

        viewModel.completeCooking(rating = null, difficulty = null, note = "   ") {}

        assertEquals(1, historyRepository.addHistoryCalls)
        assertEquals(0, historyRepository.saveNoteCalls)
    }

    @Test
    fun completingCookingStopsAllTimers() {
        viewModel.loadRecipe("123")
        viewModel.startTimerForCurrentStep(10)

        viewModel.completeCooking(rating = null, difficulty = null, note = null) {}

        assertTrue(state.timers.isEmpty())
    }

    private companion object {
        fun sampleRecipe() = RecipeDetailDto(
            recipeSourceId = "123",
            title = "Test Recipe",
            steps = listOf(
                RecipeStepDto(1, "Step one"),
                RecipeStepDto(2, "Step two"),
                RecipeStepDto(3, "Step three")
            )
        )
    }
}