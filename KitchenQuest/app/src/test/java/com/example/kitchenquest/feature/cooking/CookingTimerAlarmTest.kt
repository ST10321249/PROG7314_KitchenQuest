package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.history.RecipeNoteDto
import com.example.kitchenquest.data.recipes.RecipeDetailDto
import com.example.kitchenquest.data.recipes.RecipeRecommendationDto
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.data.recipes.RecipeSearchResultDto
import com.example.kitchenquest.data.recipes.RecipeStepDto
import com.example.kitchenquest.notifications.TimerAlarms
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
class CookingTimerAlarmTest {

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

    private class FakeTimerAlarms : TimerAlarms {
        val scheduled = mutableMapOf<String, Long>()
        val labels = mutableMapOf<String, String>()

        override fun schedule(timerId: String, label: String, triggerAtMillis: Long) {
            scheduled[timerId] = triggerAtMillis
            labels[timerId] = label
        }

        override fun cancel(timerId: String) {
            scheduled.remove(timerId)
        }
    }

    private val recipe = RecipeDetailDto(
        recipeSourceId = "123",
        title = "Bobotie",
        steps = listOf(RecipeStepDto(1, "Fry the onions"), RecipeStepDto(2, "Bake"))
    )

    private lateinit var store: MemorySessionStore
    private lateinit var alarms: FakeTimerAlarms
    private var now = 1_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        store = MemorySessionStore()
        alarms = FakeTimerAlarms()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = CookingViewModel(
        recipeRepository = FakeRecipeRepository(recipe),
        historyRepository = FakeHistoryRepository(),
        sessionStore = store,
        clock = { now },
        timerAlarms = alarms
    )

    private fun CookingViewModel.onlyTimerId() = uiState.value.timers.single().id

    @Test
    fun startingATimerSetsAnAlarmForWhenItEnds() {
        val viewModel = newViewModel()

        viewModel.startStandaloneTimer(5, "Rice")

        val id = viewModel.onlyTimerId()
        assertEquals(now + 5 * 60_000L, alarms.scheduled[id])
        assertEquals("Rice", alarms.labels[id])
    }

    @Test
    fun aStepTimerAlarmUsesTheStepLabel() {
        val viewModel = newViewModel()
        viewModel.loadRecipe("123")
        viewModel.nextStep()

        viewModel.startTimerForCurrentStep(10)

        assertEquals("Step 2", alarms.labels[viewModel.onlyTimerId()])
    }

    @Test
    fun pausingRemovesTheAlarmAndResumingSetsItAgain() {
        val viewModel = newViewModel()
        viewModel.startStandaloneTimer(10, "Sauce")
        val id = viewModel.onlyTimerId()

        now += 4 * 60_000L
        viewModel.togglePauseTimer(id)
        assertFalse(alarms.scheduled.containsKey(id))

        now += 20 * 60_000L
        viewModel.togglePauseTimer(id)
        assertEquals(now + 6 * 60_000L, alarms.scheduled[id])
    }

    @Test
    fun addingAMinuteMovesTheAlarm() {
        val viewModel = newViewModel()
        viewModel.startStandaloneTimer(1, "Toast")
        val id = viewModel.onlyTimerId()

        viewModel.addMinuteToTimer(id)

        assertEquals(now + 2 * 60_000L, alarms.scheduled[id])
    }

    @Test
    fun cancellingATimerRemovesItsAlarm() {
        val viewModel = newViewModel()
        viewModel.startStandaloneTimer(5, "Tea")
        val id = viewModel.onlyTimerId()

        viewModel.cancelTimer(id)

        assertTrue(alarms.scheduled.isEmpty())
    }

    @Test
    fun stoppingAllTimersRemovesEveryAlarm() {
        val viewModel = newViewModel()
        viewModel.startStandaloneTimer(5, "Tea")
        viewModel.startStandaloneTimer(8, "Eggs")

        viewModel.stopAllTimers()

        assertTrue(alarms.scheduled.isEmpty())
    }

    @Test
    fun finishingCookingRemovesEveryAlarm() {
        val viewModel = newViewModel()
        viewModel.loadRecipe("123")
        viewModel.startTimerForCurrentStep(5)

        viewModel.completeCooking(rating = null, difficulty = null, note = null) {}

        assertTrue(alarms.scheduled.isEmpty())
    }

    @Test
    fun reopeningTheAppSetsAlarmsOnlyForTimersStillRunning() {
        val first = newViewModel()
        first.startStandaloneTimer(1, "Short")
        first.startStandaloneTimer(30, "Long")
        alarms.scheduled.clear()

        now += 5 * 60_000L
        val reopened = newViewModel()

        val long = reopened.uiState.value.timers.first { it.label == "Long" }
        assertEquals(setOf(long.id), alarms.scheduled.keys)
    }
}