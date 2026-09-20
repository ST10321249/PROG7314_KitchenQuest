package com.example.kitchenquest.data.shopping

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall

interface ShoppingRepository {
    suspend fun getShoppingItems(): Result<List<ShoppingItemDto>>
    suspend fun addShoppingItem(name: String, quantity: Double?, unit: String?): Result<ShoppingItemDto>
    suspend fun setPurchased(id: String, purchased: Boolean): Result<ShoppingItemDto>
    suspend fun deleteShoppingItem(id: String): Result<Unit>
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
        shoppingApi.addShoppingItem(CreateShoppingItemRequest(name, quantity, unit))
    }

    override suspend fun setPurchased(id: String, purchased: Boolean): Result<ShoppingItemDto> = apiCall {
        shoppingApi.updateShoppingItem(id, UpdateShoppingItemRequest(isPurchased = purchased))
    }

    override suspend fun deleteShoppingItem(id: String): Result<Unit> = apiCall {
        shoppingApi.deleteShoppingItem(id)
    }
}