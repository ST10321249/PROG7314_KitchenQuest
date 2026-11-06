package com.example.kitchenquest.feature.cooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.R
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.UserMessages
import com.example.kitchenquest.data.history.DefaultHistoryRepository
import com.example.kitchenquest.data.history.HistoryRepository
import com.example.kitchenquest.data.photos.DishPhotoReader
import com.example.kitchenquest.data.photos.PhotoRepository
import com.example.kitchenquest.data.recipes.RecipeRepository
import com.example.kitchenquest.notifications.TimerAlarms
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CookingViewModel(
    // The app-wide repository can load a recipe from its offline copy when there is no connection.
    private val recipeRepository: RecipeRepository = AppRepositories.recipes,
    private val historyRepository: HistoryRepository = DefaultHistoryRepository(),
    private val sessionStore: CookingSessionStore = AppRepositories.cookingSessions(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val timerAlarms: TimerAlarms = AppRepositories.timerAlarms(),
    private val photoRepository: PhotoRepository = AppRepositories.photos(),
    private val dishPhotoReader: DishPhotoReader = AppRepositories.dishPhotoReader(),
    private val userMessages: UserMessages = AppRepositories.userMessages()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CookingUiState())
    val uiState: StateFlow<CookingUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        restoreSession()
    }

    fun loadRecipe(recipeId: String) {
        if (_uiState.value.recipe?.recipeSourceId == recipeId) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            recipeRepository.getRecipeById(recipeId).fold(
                onSuccess = { recipe ->
                    _uiState.update { it.copy(isLoading = false, recipe = recipe, currentStepIndex = 0, completedHistoryId = null, completionClientId = null, completionError = null) }
                    saveSession()
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
        saveSession()
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStepIndex = (it.currentStepIndex - 1).coerceAtLeast(0)) }
        saveSession()
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
            sourceType = sourceType,
            endsAtMillis = clock() + totalSeconds * 1000L
        )
        _uiState.update { it.copy(timers = it.timers + timer) }
        scheduleAlarm(timer)
        saveSession()
        ensureTicking()
    }

    fun addMinuteToTimer(id: String) {
        val now = clock()
        _uiState.update { state ->
            state.copy(timers = state.timers.map {
                if (it.id != id) {
                    it
                } else if (it.isRunning) {
                    val endsAt = (it.endsAtMillis ?: now).coerceAtLeast(now) + 60_000L
                    it.copy(
                        remainingSeconds = it.remainingSeconds + 60,
                        totalSeconds = it.totalSeconds + 60,
                        isFinished = false,
                        endsAtMillis = endsAt
                    )
                } else if (it.isFinished) {
                    it.copy(
                        remainingSeconds = 60,
                        totalSeconds = it.totalSeconds + 60,
                        isFinished = false,
                        isRunning = true,
                        endsAtMillis = now + 60_000L
                    )
                } else {
                    it.copy(
                        remainingSeconds = it.remainingSeconds + 60,
                        totalSeconds = it.totalSeconds + 60
                    )
                }
            })
        }
        syncAlarm(id)
        saveSession()
        ensureTicking()
    }

    fun togglePauseTimer(id: String) {
        val now = clock()
        _uiState.update { state ->
            state.copy(timers = state.timers.map {
                when {
                    it.id != id || it.isFinished -> it
                    it.isRunning -> it.copy(
                        isRunning = false,
                        remainingSeconds = remainingAt(it, now),
                        endsAtMillis = null
                    )
                    else -> it.copy(
                        isRunning = true,
                        endsAtMillis = now + it.remainingSeconds * 1000L
                    )
                }
            })
        }
        syncAlarm(id)
        saveSession()
        ensureTicking()
    }

    fun cancelTimer(id: String) {
        timerAlarms.cancel(id)
        _uiState.update { state -> state.copy(timers = state.timers.filterNot { it.id == id }) }
        saveSession()
    }

    fun stopAllTimers() {
        _uiState.value.timers.forEach { timerAlarms.cancel(it.id) }
        _uiState.update { it.copy(timers = emptyList()) }
        saveSession()
    }

    fun setDishPhoto(uri: String?) {
        _uiState.update { it.copy(dishPhotoUri = uri) }
    }

    fun completeCooking(rating: Int?, difficulty: String?, note: String?, onComplete: () -> Unit) {
        if (_uiState.value.isCompleting) return
        val recipe = _uiState.value.recipe ?: return
        _uiState.update { it.copy(isCompleting = true, completionError = null) }

        viewModelScope.launch {
            // History and the optional note must both succeed before leaving the
            // screen. Keep the recipe, timers and feedback available on failure.
            var entryId = _uiState.value.completedHistoryId
            if (entryId == null) {
                val clientId = _uiState.value.completionClientId ?: UUID.randomUUID().toString().also { newId ->
                    _uiState.update { it.copy(completionClientId = newId) }
                    saveSession()
                }
                val result = historyRepository.addHistoryEntryWithClientId(
                    clientId = clientId,
                    recipeSourceId = recipe.recipeSourceId,
                    recipeTitle = recipe.title,
                    servings = null,
                    rating = rating,
                    difficultyFeedback = difficulty
                )
                if (result.isFailure) {
                    _uiState.update { it.copy(isCompleting = false, completionError = CookingSaveFailure.HISTORY) }
                    return@launch
                }
                entryId = result.getOrThrow().id
                _uiState.update { it.copy(completedHistoryId = entryId) }
                saveSession()
            }

            if (!note.isNullOrBlank()) {
                val result = historyRepository.saveNote(recipe.recipeSourceId, note)
                if (result.isFailure) {
                    _uiState.update { it.copy(isCompleting = false, completionError = CookingSaveFailure.NOTE) }
                    return@launch
                }
            }

            val photoUri = _uiState.value.dishPhotoUri
            // Photo uploading is optional and already reports its own failure.
            // It must not make an otherwise saved cooking record disappear.
            val historyIdForPhoto = entryId
            if (photoUri != null) {
                if (historyIdForPhoto.isNullOrBlank()) {
                    userMessages.show(R.string.dish_photo_upload_failed)
                } else {
                    viewModelScope.launch { uploadDishPhoto(historyIdForPhoto, photoUri) }
                }
            }

            _uiState.value.timers.forEach { timerAlarms.cancel(it.id) }
            _uiState.update {
                it.copy(
                    recipe = null,
                    currentStepIndex = 0,
                    timers = emptyList(),
                    errorMessage = null,
                    dishPhotoUri = null,
                    isCompleting = false,
                    completionError = null,
                    completedHistoryId = null,
                    completionClientId = null
                )
            }
            sessionStore.clear()
            onComplete()
        }
    }

    private suspend fun uploadDishPhoto(historyEntryId: String, uri: String) {
        val photo = dishPhotoReader.read(uri)
        if (photo == null) {
            userMessages.show(R.string.dish_photo_unreadable)
            return
        }
        val saved = photoRepository.upload(photo.bytes, photo.contentType)
            .mapCatching { fileId -> photoRepository.attachToHistory(historyEntryId, fileId).getOrThrow() }
        userMessages.show(if (saved.isSuccess) R.string.dish_photo_saved else R.string.dish_photo_upload_failed)
    }

    private fun restoreSession() {
        val session = sessionStore.load() ?: return
        val now = clock()
        val timers = session.timers.map { refreshTimer(it, now) }
        val lastStep = (session.recipe?.steps?.size ?: 1) - 1
        _uiState.update {
            it.copy(
                recipe = session.recipe,
                currentStepIndex = session.currentStepIndex.coerceIn(0, lastStep.coerceAtLeast(0)),
                timers = timers,
                completedHistoryId = session.completedHistoryId,
                completionClientId = session.completionClientId
            )
        }
        timers.filter { it.isRunning }.forEach { scheduleAlarm(it) }
        if (timers.any { it.isRunning }) ensureTicking()
    }

    private fun scheduleAlarm(timer: CookingTimer) {
        val endsAt = timer.endsAtMillis ?: return
        if (timer.isRunning && !timer.isFinished) {
            timerAlarms.schedule(timer.id, timer.label, endsAt)
        }
    }

    private fun syncAlarm(id: String) {
        val timer = _uiState.value.timers.firstOrNull { it.id == id } ?: return
        if (timer.isRunning && !timer.isFinished) scheduleAlarm(timer) else timerAlarms.cancel(id)
    }

    private fun saveSession() {
        val state = _uiState.value
        if (state.recipe == null && state.timers.isEmpty()) {
            sessionStore.clear()
            return
        }
        sessionStore.save(
            CookingSession(
                recipe = state.recipe,
                currentStepIndex = state.currentStepIndex,
                timers = state.timers,
                completedHistoryId = state.completedHistoryId,
                completionClientId = state.completionClientId,
                savedAtMillis = clock()
            )
        )
    }

    private fun remainingAt(timer: CookingTimer, now: Long): Int {
        val endsAt = timer.endsAtMillis ?: return timer.remainingSeconds
        val millisLeft = (endsAt - now).coerceAtLeast(0)
        return ((millisLeft + 999) / 1000).toInt()
    }

    private fun refreshTimer(timer: CookingTimer, now: Long): CookingTimer {
        if (!timer.isRunning || timer.isFinished) return timer
        val remaining = remainingAt(timer, now)
        return if (remaining == 0) {
            timer.copy(remainingSeconds = 0, isFinished = true, isRunning = false, endsAtMillis = null)
        } else {
            timer.copy(remainingSeconds = remaining)
        }
    }

    private fun ensureTicking() {
        if (tickerJob != null) return
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = clock()
                var anyFinished = false
                _uiState.update { state ->
                    state.copy(
                        timers = state.timers.map { timer ->
                            val refreshed = refreshTimer(timer, now)
                            if (refreshed.isFinished && !timer.isFinished) anyFinished = true
                            refreshed
                        }
                    )
                }
                if (anyFinished) saveSession()
            }
        }
    }
}