package com.example.kitchenquest.data.sync

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.pantry.CreatePantryItemRequest
import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.shopping.TransferToPantryRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncContractTest {

    @Test
    fun newClientIdsAreUniqueAndAcceptedByTheApi() {
        val first = ClientIds.newId()
        val second = ClientIds.newId()

        assertNotEquals(first, second)
        assertTrue(ClientIds.isValid(first))
    }

    @Test
    fun clientIdsWithUnsafeCharactersOrBadLengthAreRejected() {
        assertFalse(ClientIds.isValid("short"))
        assertFalse(ClientIds.isValid("has spaces in it"))
        assertFalse(ClientIds.isValid("a".repeat(65)))
        assertTrue(ClientIds.isValid("a".repeat(64)))
    }

    @Test
    fun pantryResponseWithSyncFieldsIsReadCorrectly() {
        val json = """
            {"id":"507f1f77bcf86cd799439011","clientId":"3f2b8c1e-9d4a-4c6e-8b1a-2f7d5e9c0a11",
             "ingredientName":"Milk","quantity":1,"unit":"L","category":"Dairy",
             "expiryDate":null,"updatedAt":"2026-10-20T08:00:00.000Z"}
        """.trimIndent()

        val item = ApiClient.json.decodeFromString<PantryItemDto>(json)

        assertEquals("3f2b8c1e-9d4a-4c6e-8b1a-2f7d5e9c0a11", item.clientId)
        assertEquals("2026-10-20T08:00:00.000Z", item.updatedAt)
    }

    @Test
    fun responseFromAnOlderItemWithoutSyncFieldsStillWorks() {
        val pantry = ApiClient.json.decodeFromString<PantryItemDto>(
            """{"id":"1","ingredientName":"Rice","quantity":2,"unit":"kg","category":"Pantry"}"""
        )
        val shopping = ApiClient.json.decodeFromString<ShoppingItemDto>(
            """{"id":"2","clientId":null,"ingredientName":"Eggs","quantity":null,"unit":"","isPurchased":false}"""
        )

        assertNull(pantry.clientId)
        assertNull(pantry.updatedAt)
        assertNull(shopping.clientId)
    }

    @Test
    fun createRequestSendsTheClientIdAndLeavesItOutWhenNotSet() {
        val withId = ApiClient.json.encodeToString(
            CreatePantryItemRequest.serializer(),
            CreatePantryItemRequest(
                clientId = "client-id-1234",
                ingredientName = "Milk",
                quantity = 1.0,
                unit = "L",
                category = "Dairy"
            )
        )
        val withoutId = ApiClient.json.encodeToString(
            CreatePantryItemRequest.serializer(),
            CreatePantryItemRequest(
                ingredientName = "Milk",
                quantity = 1.0,
                unit = "L",
                category = "Dairy"
            )
        )

        assertTrue(withId.contains("\"clientId\":\"client-id-1234\""))
        assertFalse(withoutId.contains("clientId"))
    }

    @Test
    fun transferRequestCarriesBothTheShoppingReferenceAndThePantryItem() {
        val request = TransferToPantryRequest(
            shoppingClientId = "shopping-client-1",
            pantryItem = CreatePantryItemRequest(
                clientId = "pantry-client-1",
                ingredientName = "Eggs",
                quantity = 12.0,
                unit = "pcs",
                category = "Dairy"
            )
        )

        val json = ApiClient.json.encodeToString(TransferToPantryRequest.serializer(), request)

        assertTrue(json.contains("\"shoppingClientId\":\"shopping-client-1\""))
        assertTrue(json.contains("\"pantryItem\":{"))
        assertFalse(json.contains("shoppingItemId"))
    }
}
