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
    // Stable client id allows safe retries if the server saved a cooking record but
    // the reply was lost. Simple fake repositories can use the original method.
    suspend fun addHistoryEntryWithClientId(
        clientId: String,
        recipeSourceId: String,
        recipeTitle: String,
        servings: Int?,
        rating: Int?,
        difficultyFeedback: String?
    ): Result<CookingHistoryDto> = addHistoryEntry(
        recipeSourceId, recipeTitle, servings, rating, difficultyFeedback
    )

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

    override suspend fun addHistoryEntryWithClientId(
        clientId: String,
        recipeSourceId: String,
        recipeTitle: String,
        servings: Int?,
        rating: Int?,
        difficultyFeedback: String?
    ): Result<CookingHistoryDto> = apiCall {
        historyApi.addHistoryEntry(
            AddHistoryRequest(recipeSourceId, recipeTitle, servings, rating, difficultyFeedback, clientId)
        )
    }

    override suspend fun getNote(recipeId: String): Result<RecipeNoteDto?> = apiCall {
        historyApi.getNote(recipeId)
    }

    override suspend fun saveNote(recipeSourceId: String, note: String): Result<RecipeNoteDto> = apiCall {
        historyApi.saveNote(SaveRecipeNoteRequest(recipeSourceId, note))
    }
}