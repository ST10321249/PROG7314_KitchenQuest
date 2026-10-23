package com.example.kitchenquest.data.shopping

import com.example.kitchenquest.data.pantry.CreatePantryItemRequest
import com.example.kitchenquest.data.sync.SyncState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ShoppingItemDto(
    val id: String = "",
    val clientId: String? = null,
    val ingredientName: String = "",
    val quantity: Double? = null,
    val unit: String = "",
    val isPurchased: Boolean = false,
    val updatedAt: String? = null,
    // Kept on the phone only, so screens can show whether a change has reached the server.
    @Transient
    val syncState: SyncState = SyncState.SYNCED
)

@Serializable
data class CreateShoppingItemRequest(
    val clientId: String? = null,
    val ingredientName: String,
    val quantity: Double? = null,
    val unit: String? = null
)

@Serializable
data class UpdateShoppingItemRequest(
    val ingredientName: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val isPurchased: Boolean? = null
)

// Moves a bought item into the pantry in one request. The pantry item needs a clientId so
// the request can safely be sent again if the first attempt timed out.
@Serializable
data class TransferToPantryRequest(
    val shoppingItemId: String? = null,
    val shoppingClientId: String? = null,
    val pantryItem: CreatePantryItemRequest
)
