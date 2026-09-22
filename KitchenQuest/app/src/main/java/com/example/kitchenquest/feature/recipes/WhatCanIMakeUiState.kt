package com.example.kitchenquest.feature.recipes

import com.example.kitchenquest.data.recipes.RecipeRecommendationDto

data class WhatCanIMakeUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val recommendations: List<RecipeRecommendationDto> = emptyList(),
    val errorMessage: String? = null
)