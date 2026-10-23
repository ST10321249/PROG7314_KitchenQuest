package com.example.kitchenquest.feature.cooking

import com.example.kitchenquest.data.recipes.RecipeDetailDto

enum class TimerSource { RECIPE, STANDALONE }

data class CookingTimer(
    val id: String,
    val label: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val isFinished: Boolean,
    val sourceType: TimerSource
)

data class CookingUiState(
    val isLoading: Boolean = false,
    val recipe: RecipeDetailDto? = null,
    val currentStepIndex: Int = 0,
    val timers: List<CookingTimer> = emptyList(),
    val errorMessage: String? = null
)