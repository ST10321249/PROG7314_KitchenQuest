package com.example.kitchenquest.data.pantry

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall

interface PantryRepository {

    suspend fun getPantryItems(): Result<List<PantryItemDto>>

    suspend fun addPantryItem(
        ingredientName: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ): Result<PantryItemDto>

    suspend fun updatePantryItem(
        id: String,
        ingredientName: String? = null,
        quantity: Double? = null,
        unit: String? = null,
        category: String? = null,
        expiryDate: String? = null
    ): Result<PantryItemDto>

    suspend fun deletePantryItem(id: String): Result<Unit>
}

class DefaultPantryRepository(
    private val pantryApi: PantryApi =
        ApiClient.create(PantryApi::class.java)
) : PantryRepository {

    override suspend fun getPantryItems(): Result<List<PantryItemDto>> = apiCall {
        pantryApi.getPantryItems()
    }

    override suspend fun addPantryItem(
        ingredientName: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ): Result<PantryItemDto> = apiCall {
        pantryApi.addPantryItem(
            CreatePantryItemRequest(
                ingredientName = ingredientName,
                quantity = quantity,
                unit = unit,
                category = category,
                expiryDate = expiryDate
            )
        )
    }

    override suspend fun updatePantryItem(
        id: String,
        ingredientName: String?,
        quantity: Double?,
        unit: String?,
        category: String?,
        expiryDate: String?
    ): Result<PantryItemDto> = apiCall {
        pantryApi.updatePantryItem(
            id,
            UpdatePantryItemRequest(
                ingredientName = ingredientName,
                quantity = quantity,
                unit = unit,
                category = category,
                expiryDate = expiryDate
            )
        )
    }

    override suspend fun deletePantryItem(id: String): Result<Unit> = apiCall {
        pantryApi.deletePantryItem(id)
    }
}