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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CookingSessionRestoreTest {

    private class FakeRecipeRepository(val recipe: RecipeDetailDto) : RecipeRepository {
        override suspend fun searchRecipes(query: String?, diet: String?, cuisine: String?, maxReadyTime: Int?) =
            Result.success(emptyList<RecipeSearchResultDto>())

        override suspend fun getRecipeById(id: String) = Result.success(recipe)

        override suspend fun getRecommendations() = Result.success(emptyList<RecipeRecommendationDto>())
    }

    private class FakeHistoryRepository : HistoryRepository {
        override suspend fun getHistory() = Result.success(emptyList<CookingHistoryDto>())

        override suspend fun addHistoryEntry(
            recipeSourceId: String,
            recipeTitle: String,
            servings: Int?,
            rating: Int?,
            difficultyFeedback: String?
        ) = Result.success(CookingHistoryDto(recipeSourceId = recipeSourceId, recipeTitle = recipeTitle))

        override suspend fun getNote(recipeId: String) = Result.success<RecipeNoteDto?>(null)

        override suspend fun saveNote(recipeSourceId: String, note: String) =
            Result.success(RecipeNoteDto(recipeSourceId = recipeSourceId, note = note))
    }

    private class MemorySessionStore : CookingSessionStore {
        var session: CookingSession? = null
        override fun load() = session
        override fun save(session: CookingSession) {
            this.session = session
        }
        override fun clear() {
            session = null
        }
    }

    private val recipe = RecipeDetailDto(
        recipeSourceId = "123",
        title = "Bobotie",
        steps = listOf(
            RecipeStepDto(1, "Fry the onions"),
            RecipeStepDto(2, "Add the mince"),
            RecipeStepDto(3, "Bake")
        )
    )

    private lateinit var store: MemorySessionStore
    private var now = 1_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = MemorySessionStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = CookingViewModel(
        recipeRepository = FakeRecipeRepository(recipe),
        historyRepository = FakeHistoryRepository(),
        sessionStore = store,
        clock = { now }
    )

    @Test
    fun theRecipeAndStepComeBackAfterTheAppIsReopened() {
        val first = newViewModel()
        first.loadRecipe("123")
        first.nextStep()
        first.nextStep()

        val reopened = newViewModel()

        assertEquals("Bobotie", reopened.uiState.value.recipe?.title)
        assertEquals(2, reopened.uiState.value.currentStepIndex)
    }

    @Test
    fun aRunningTimerKeepsCountingWhileTheAppIsClosed() {
        val first = newViewModel()
        first.startStandaloneTimer(10, "Rice")

        now += 4 * 60_000L
        val reopened = newViewModel()

        val timer = reopened.uiState.value.timers.single()
        assertEquals(6 * 60, timer.remainingSeconds)
        assertTrue(timer.isRunning)
        assertFalse(timer.isFinished)
    }

    @Test
    fun aTimerThatEndedWhileTheAppWasClosedShowsAsFinished() {
        val first = newViewModel()
        first.startStandaloneTimer(1, "Eggs")

        now += 5 * 60_000L
        val reopened = newViewModel()

        val timer = reopened.uiState.value.timers.single()
        assertEquals(0, timer.remainingSeconds)
        assertTrue(timer.isFinished)
        assertFalse(timer.isRunning)
    }

    @Test
    fun aPausedTimerKeepsItsTimeLeft() {
        val first = newViewModel()
        first.startStandaloneTimer(10, "Sauce")
        now += 3 * 60_000L
        first.togglePauseTimer(first.uiState.value.timers.single().id)

        now += 30 * 60_000L
        val reopened = newViewModel()

        val timer = reopened.uiState.value.timers.single()
        assertEquals(7 * 60, timer.remainingSeconds)
        assertFalse(timer.isRunning)
        assertFalse(timer.isFinished)
    }

    @Test
    fun resumingAPausedTimerCountsDownFromWhereItStopped() {
        val viewModel = newViewModel()
        viewModel.startStandaloneTimer(10, "Sauce")
        now += 2 * 60_000L
        val id = viewModel.uiState.value.timers.single().id
        viewModel.togglePauseTimer(id)
        now += 60 * 60_000L
        viewModel.togglePauseTimer(id)

        now += 60_000L
        val reopened = newViewModel()

        assertEquals(7 * 60, reopened.uiState.value.timers.single().remainingSeconds)
    }

    @Test
    fun addingAMinuteMovesTheEndTimeToo() {
        val first = newViewModel()
        first.startStandaloneTimer(1, "Toast")
        first.addMinuteToTimer(first.uiState.value.timers.single().id)

        now += 90_000L
        val reopened = newViewModel()

        assertEquals(30, reopened.uiState.value.timers.single().remainingSeconds)
    }

    @Test
    fun finishingCookingClearsTheSavedSession() {
        val viewModel = newViewModel()
        viewModel.loadRecipe("123")
        viewModel.startTimerForCurrentStep(5)

        viewModel.completeCooking(rating = 5, difficulty = null, note = null) {}

        assertNull(store.session)
        assertNull(newViewModel().uiState.value.recipe)
    }

    @Test
    fun stoppingAllTimersWithNoRecipeClearsTheSavedSession() {
        val viewModel = newViewModel()
        viewModel.startStandaloneTimer(5, "Tea")

        viewModel.stopAllTimers()

        assertNull(store.session)
    }

    @Test
    fun aSavedStepPastTheEndOfTheRecipeIsKeptInRange() {
        store.session = CookingSession(recipe = recipe, currentStepIndex = 9)

        assertEquals(2, newViewModel().uiState.value.currentStepIndex)
    }
}