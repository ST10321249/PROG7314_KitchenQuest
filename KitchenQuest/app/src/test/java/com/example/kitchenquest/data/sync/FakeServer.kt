package com.example.kitchenquest.data.sync

import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import com.example.kitchenquest.data.pantry.CreatePantryItemRequest
import com.example.kitchenquest.data.pantry.PantryApi
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.pantry.UpdatePantryItemRequest
import com.example.kitchenquest.data.shopping.CreateShoppingItemRequest
import com.example.kitchenquest.data.shopping.ShoppingApi
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.shopping.TransferToPantryRequest
import com.example.kitchenquest.data.shopping.UpdateShoppingItemRequest
import kotlinx.coroutines.delay
import java.io.IOException

// A stand-in for the real API that follows the same rules: creates with a clientId that
// already exists return the existing item, deleting something missing is "not found",
// and so on. It can also be switched off, made to fail, or made to lose its replies.
class FakeServer : PantryApi, ShoppingApi {

    val pantry = linkedMapOf<String, PantryItemDto>()
    val shopping = linkedMapOf<String, ShoppingItemDto>()

    var online = true
    var delayMs = 0L

    // Errors to throw for the next calls, one per call, before anything is processed.
    val failures = ArrayDeque<ApiException>()

    // The next calls are carried out, but the reply never reaches the app.
    var lostReplies = 0

    // Items with this name are refused as invalid.
    var rejectName: String? = null

    val calls = mutableListOf<String>()

    private var counter = 0

    private suspend fun enter(call: String) {
        calls.add(call)
        if (delayMs > 0) delay(delayMs)
        if (!online) throw IOException("offline")
        failures.removeFirstOrNull()?.let { throw it }
    }

    private fun <T> reply(value: T): T {
        if (lostReplies > 0) {
            lostReplies--
            throw java.net.SocketTimeoutException("reply lost")
        }
        return value
    }

    private fun refuse(name: String) {
        if (name == rejectName) {
            throw ApiException(ApiErrorType.INVALID_REQUEST, "$name is not valid")
        }
    }

    private fun notFound(): Nothing =
        throw ApiException(ApiErrorType.NOT_FOUND, "not found")

    private fun createPantry(request: CreatePantryItemRequest): PantryItemDto {
        refuse(request.ingredientName)
        val existing = request.clientId?.let { id -> pantry.values.firstOrNull { it.clientId == id } }
        if (existing != null) return existing

        val item = PantryItemDto(
            id = "srv-p${++counter}",
            clientId = request.clientId,
            ingredientName = request.ingredientName,
            quantity = request.quantity,
            unit = request.unit,
            category = request.category,
            expiryDate = request.expiryDate,
            updatedAt = "2026-10-21T10:00:0$counter.000Z"
        )
        pantry[item.id] = item
        return item
    }

    override suspend fun getPantryItems(): List<PantryItemDto> {
        enter("GET pantry")
        return reply(pantry.values.toList())
    }

    override suspend fun addPantryItem(request: CreatePantryItemRequest): PantryItemDto {
        enter("POST pantry")
        return reply(createPantry(request))
    }

    override suspend fun updatePantryItem(id: String, request: UpdatePantryItemRequest): PantryItemDto {
        enter("PATCH pantry/$id")
        val current = pantry[id] ?: notFound()
        val updated = current.copy(
            ingredientName = request.ingredientName ?: current.ingredientName,
            quantity = request.quantity ?: current.quantity,
            unit = request.unit ?: current.unit,
            category = request.category ?: current.category,
            expiryDate = request.expiryDate ?: current.expiryDate
        )
        pantry[id] = updated
        return reply(updated)
    }

    override suspend fun deletePantryItem(id: String) {
        enter("DELETE pantry/$id")
        pantry.remove(id) ?: notFound()
        reply(Unit)
    }

    override suspend fun getShoppingItems(): List<ShoppingItemDto> {
        enter("GET shopping")
        return reply(shopping.values.toList())
    }

    override suspend fun addShoppingItem(request: CreateShoppingItemRequest): ShoppingItemDto {
        enter("POST shopping")
        refuse(request.ingredientName)
        val existing = request.clientId?.let { id -> shopping.values.firstOrNull { it.clientId == id } }
        if (existing != null) return reply(existing)

        val item = ShoppingItemDto(
            id = "srv-s${++counter}",
            clientId = request.clientId,
            ingredientName = request.ingredientName,
            quantity = request.quantity,
            unit = request.unit.orEmpty()
        )
        shopping[item.id] = item
        return reply(item)
    }

    override suspend fun updateShoppingItem(id: String, request: UpdateShoppingItemRequest): ShoppingItemDto {
        enter("PATCH shopping/$id")
        val current = shopping[id] ?: notFound()
        val updated = current.copy(
            ingredientName = request.ingredientName ?: current.ingredientName,
            isPurchased = request.isPurchased ?: current.isPurchased
        )
        shopping[id] = updated
        return reply(updated)
    }

    override suspend fun transferToPantry(request: TransferToPantryRequest): PantryItemDto {
        enter("POST shopping/transfer")
        val pantryItem = createPantry(request.pantryItem)
        shopping.values
            .firstOrNull { it.id == request.shoppingItemId || it.clientId == request.shoppingClientId }
            ?.let { shopping.remove(it.id) }
        return reply(pantryItem)
    }

    override suspend fun deleteShoppingItem(id: String) {
        enter("DELETE shopping/$id")
        shopping.remove(id) ?: notFound()
        reply(Unit)
    }

    // Puts an item on the server as if it had been created by another device.
    fun seedPantry(name: String, quantity: Double = 1.0): PantryItemDto =
        createPantry(
            CreatePantryItemRequest(
                ingredientName = name,
                quantity = quantity,
                unit = "unit",
                category = "General"
            )
        )

    fun seedShopping(name: String): ShoppingItemDto {
        val item = ShoppingItemDto(id = "srv-s${++counter}", ingredientName = name)
        shopping[item.id] = item
        return item
    }
}
