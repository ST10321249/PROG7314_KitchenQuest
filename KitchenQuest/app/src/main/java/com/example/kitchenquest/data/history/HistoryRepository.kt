package com.example.kitchenquest.data.history

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall

interface HistoryRepository {
    suspend fun getHistory(): Result<List<CookingHistoryDto>>
    suspend fun addHistoryEntry(
        recipeSourceId: String,
        recipeTitle: String,
        servings: Int?,
        rating: Int?,
        difficultyFeedback: String?
    ): Result<CookingHistoryDto>
    suspend fun getNote(recipeId: String): Result<RecipeNoteDto?>
    suspend fun saveNote(recipeSourceId: String, note: String): Result<RecipeNoteDto>
}

class DefaultHistoryRepository(
    private val historyApi: HistoryApi = ApiClient.create(HistoryApi::class.java)
) : HistoryRepository {

    override suspend fun getHistory(): Result<List<CookingHistoryDto>> = apiCall {
        historyApi.getHistory()
    }

    override suspend fun addHistoryEntry(
        recipeSourceId: String,
        recipeTitle: String,
        servings: Int?,
        rating: Int?,
        difficultyFeedback: String?
    ): Result<CookingHistoryDto> = apiCall {
        historyApi.addHistoryEntry(
            AddHistoryRequest(recipeSourceId, recipeTitle, servings, rating, difficultyFeedback)
        )
    }

    override suspend fun getNote(recipeId: String): Result<RecipeNoteDto?> = apiCall {
        historyApi.getNote(recipeId)
    }

    override suspend fun saveNote(recipeSourceId: String, note: String): Result<RecipeNoteDto> = apiCall {
        historyApi.saveNote(SaveRecipeNoteRequest(recipeSourceId, note))
    }
}