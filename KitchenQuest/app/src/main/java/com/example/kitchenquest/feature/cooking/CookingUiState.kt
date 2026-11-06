package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.recipes.RecipeDetailDto
import kotlinx.serialization.Serializable

@Serializable
enum class TimerSource { RECIPE, STANDALONE }

@Serializable
data class CookingTimer(
    val id: String,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val isFinished: Boolean,
    val sourceType: TimerSource,
    val endsAtMillis: Long? = null
)

enum class CookingSaveFailure { HISTORY, NOTE }

data class CookingUiState(
    val isLoading: Boolean = false,
    val recipe: RecipeDetailDto? = null,
    val currentStepIndex: Int = 0,
    val timers: List<CookingTimer> = emptyList(),
    val errorMessage: String? = null,
    val dishPhotoUri: String? = null,
    val isCompleting: Boolean = false,
    val completionError: CookingSaveFailure? = null,
    // A history entry can succeed even when its note fails. Remember its ID so
    // retrying the note does not create a duplicate history entry.
    val completedHistoryId: String? = null,
    val completionClientId: String? = null
)