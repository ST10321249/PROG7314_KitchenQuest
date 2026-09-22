package com.example.kitchenquest.feature.cooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.history.DefaultHistoryRepository
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.recipes.DefaultRecipeRepository
import com.example.kitchenquest.data.recipes.RecipeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CookingViewModel(
    private val recipeRepository: RecipeRepository = DefaultRecipeRepository(),
    private val historyRepository: HistoryRepository = DefaultHistoryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    fun loadRecipe(recipeId: String) {
        if (_uiState.value.recipe?.recipeSourceId == recipeId) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            recipeRepository.getRecipeById(recipeId).fold(
                onSuccess = { recipe ->
                    _uiState.update { it.copy(isLoading = false, recipe = recipe, currentStepIndex = 0) }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = failure.message) }
                }
            )
        }
    }

    fun nextStep() {
        val recipe = _uiState.value.recipe ?: return
        _uiState.update {
            it.copy(currentStepIndex = (it.currentStepIndex + 1).coerceAtMost(recipe.steps.size - 1))
        }
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStepIndex = (it.currentStepIndex - 1).coerceAtLeast(0)) }
    }

    fun startTimerForCurrentStep(minutes: Int) {
        val stepNumber = _uiState.value.currentStepIndex + 1
        addTimer(label = "Step $stepNumber", minutes = minutes, sourceType = TimerSource.RECIPE)
    }

    fun startStandaloneTimer(minutes: Int, label: String) {
        addTimer(label = label.ifBlank { "Kitchen Timer" }, minutes = minutes, sourceType = TimerSource.STANDALONE)
    }

    private fun addTimer(label: String, minutes: Int, sourceType: TimerSource) {
        val totalSeconds = (minutes * 60).coerceAtLeast(1)
        val timer = CookingTimer(
            id = UUID.randomUUID().toString(),
            label = label,
            totalSeconds = totalSeconds,
            remainingSeconds = totalSeconds,
            isRunning = true,
            isFinished = false,
            sourceType = sourceType
        )
        _uiState.update { it.copy(timers = it.timers + timer) }
        ensureTicking()
    }

    fun addMinuteToTimer(id: String) {
        _uiState.update { state ->
            state.copy(timers = state.timers.map {
                if (it.id == id) it.copy(
                    remainingSeconds = it.remainingSeconds + 60,
                    totalSeconds = it.totalSeconds + 60,
                    isFinished = false
                ) else it
            })
        }
    }

    fun togglePauseTimer(id: String) {
        _uiState.update { state ->
            state.copy(timers = state.timers.map {
                if (it.id == id && !it.isFinished) it.copy(isRunning = !it.isRunning) else it
            })
        }
    }

    fun cancelTimer(id: String) {
        _uiState.update { state -> state.copy(timers = state.timers.filterNot { it.id == id }) }
    }

    fun stopAllTimers() {
        _uiState.update { it.copy(timers = emptyList()) }
    }

    fun completeCooking(rating: Int?, difficulty: String?, note: String?, onComplete: () -> Unit) {
        val recipe = _uiState.value.recipe

        viewModelScope.launch {
            if (recipe != null) {
                historyRepository.addHistoryEntry(
                    recipeSourceId = recipe.recipeSourceId,
                    recipeTitle = recipe.title,
                    servings = null,
                    rating = rating,
                    difficultyFeedback = difficulty
                )

                if (!note.isNullOrBlank()) {
                    historyRepository.saveNote(recipe.recipeSourceId, note)
                }
            }

            stopAllTimers()
            onComplete()
        }
    }

    private fun ensureTicking() {
        if (tickerJob != null) return
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { state ->
                    state.copy(
                        timers = state.timers.map { timer ->
                            if (timer.isRunning && !timer.isFinished) {
                                val remaining = (timer.remainingSeconds - 1).coerceAtLeast(0)
                                timer.copy(
                                    remainingSeconds = remaining,
                                    isFinished = remaining == 0,
                                    isRunning = remaining > 0
                                )
                            } else timer
                        }
                    )
                }
            }
        }
    }
}