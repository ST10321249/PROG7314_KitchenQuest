package com.example.kitchenquest.data.local

import com.example.kitchenquest.data.pantry.PantryItemDto
import com.example.kitchenquest.data.shopping.ShoppingItemDto
import com.example.kitchenquest.data.sync.SyncState

// Screens keep using the same item types as before. The id they see is the local key,
// which does not change when an item finishes syncing.

fun PantryItemEntity.toDto() = PantryItemDto(
    id = id,
    // Items created before client ids existed use the server id as their local key.
    clientId = id.takeIf { it != serverId },
    ingredientName = ingredientName,
    quantity = quantity,
    unit = unit,
    category = category,
    expiryDate = expiryDate,
    updatedAt = updatedAt,
    syncState = syncState
)

fun PantryItemDto.toEntity(userId: String, localId: String) = PantryItemEntity(
    userId = userId,
    id = localId,
    serverId = id,
    ingredientName = ingredientName,
    quantity = quantity,
    unit = unit,
    category = category,
    expiryDate = expiryDate,
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED
)

fun ShoppingItemEntity.toDto() = ShoppingItemDto(
    id = id,
    clientId = id.takeIf { it != serverId },
    ingredientName = ingredientName,
    quantity = quantity,
    unit = unit,
    isPurchased = isPurchased,
    updatedAt = updatedAt,
    syncState = syncState
)

fun ShoppingItemDto.toEntity(userId: String, localId: String) = ShoppingItemEntity(
    userId = userId,
    id = localId,
    serverId = id,
    ingredientName = ingredientName,
    quantity = quantity,
    unit = unit,
    isPurchased = isPurchased,
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED
)
