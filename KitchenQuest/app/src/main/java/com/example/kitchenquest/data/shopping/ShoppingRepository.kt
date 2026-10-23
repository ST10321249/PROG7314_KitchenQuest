package com.example.kitchenquest.data.shopping

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall
import com.example.kitchenquest.data.pantry.CreatePantryItemRequest
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.sync.ClientIds

interface ShoppingRepository {
    suspend fun getShoppingItems(): Result<List<ShoppingItemDto>>
    suspend fun addShoppingItem(name: String, quantity: Double?, unit: String?): Result<ShoppingItemDto>
    suspend fun setPurchased(id: String, purchased: Boolean): Result<ShoppingItemDto>
    suspend fun deleteShoppingItem(id: String): Result<Unit>

    // Moves a bought item into the pantry as a single step that is safe to repeat.
    suspend fun transferToPantry(
        id: String,
        ingredientName: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ): Result<PantryItemDto> = Result.failure(
        UnsupportedOperationException("This shopping list cannot move items to the pantry in one step.")
    )
}

class DefaultShoppingRepository(
    private val shoppingApi: ShoppingApi = ApiClient.create(ShoppingApi::class.java)
) : ShoppingRepository {

    override suspend fun getShoppingItems(): Result<List<ShoppingItemDto>> = apiCall {
        shoppingApi.getShoppingItems()
    }

    override suspend fun addShoppingItem(
        name: String,
        quantity: Double?,
        unit: String?
    ): Result<ShoppingItemDto> = apiCall {
        shoppingApi.addShoppingItem(
            CreateShoppingItemRequest(
                ingredientName = name,
                quantity = quantity,
                unit = unit
            )
        )
    }

    override suspend fun setPurchased(id: String, purchased: Boolean): Result<ShoppingItemDto> = apiCall {
        shoppingApi.updateShoppingItem(id, UpdateShoppingItemRequest(isPurchased = purchased))
    }

    override suspend fun deleteShoppingItem(id: String): Result<Unit> = apiCall {
        shoppingApi.deleteShoppingItem(id)
    }

    override suspend fun transferToPantry(
        id: String,
        ingredientName: String,
        quantity: Double,
        unit: String,
        category: String,
        expiryDate: String?
    ): Result<PantryItemDto> = apiCall {
        shoppingApi.transferToPantry(
            TransferToPantryRequest(
                shoppingItemId = id,
                pantryItem = CreatePantryItemRequest(
                    clientId = ClientIds.newId(),
                    ingredientName = ingredientName,
                    quantity = quantity,
                    unit = unit,
                    category = category,
                    expiryDate = expiryDate
                )
            )
        )
    }
}
