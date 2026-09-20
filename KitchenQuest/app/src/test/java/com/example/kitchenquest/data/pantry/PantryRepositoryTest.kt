package com.example.kitchenquest.data.pantry

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PantryRepositoryTest {

    private class FakePantryApi : PantryApi {
        val items = mutableListOf<PantryItemDto>()

        override suspend fun getPantryItems() = items.toList()

        override suspend fun addPantryItem(request: CreatePantryItemRequest): PantryItemDto {
            val item = PantryItemDto(
                id = "id-${items.size}",
                ingredientName = request.ingredientName,
                quantity = request.quantity,
                unit = request.unit,
                category = request.category,
                expiryDate = request.expiryDate
            )
            items.add(item)
            return item
        }

        override suspend fun updatePantryItem(id: String, request: UpdatePantryItemRequest): PantryItemDto {
            val index = items.indexOfFirst { it.id == id }
            val updated = items[index].copy(
                ingredientName = request.ingredientName ?: items[index].ingredientName,
                quantity = request.quantity ?: items[index].quantity
            )
            items[index] = updated
            return updated
        }

        override suspend fun deletePantryItem(id: String) {
            items.removeAll { it.id == id }
        }
    }

    @Test
    fun addPantryItemAppendsTheNewIngredient() = runBlocking {
        val api = FakePantryApi()
        val repository = DefaultPantryRepository(api)

        repository.addPantryItem("Chicken breast", 500.0, "g", "Meat", "2026-08-28")

        assertEquals(1, api.items.size)
        assertEquals("Chicken breast", api.items.first().ingredientName)
    }

    @Test
    fun deletePantryItemRemovesTheIngredient() = runBlocking {
        val api = FakePantryApi()
        val repository = DefaultPantryRepository(api)
        val added = repository.addPantryItem("Milk", 1.0, "L", "Dairy", null).getOrThrow()

        repository.deletePantryItem(added.id)

        assertEquals(0, api.items.size)
    }
}