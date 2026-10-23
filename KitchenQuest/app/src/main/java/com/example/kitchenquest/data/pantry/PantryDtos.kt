package com.example.kitchenquest.data.pantry

import kotlinx.serialization.Serializable

@Serializable
data class PantryItemDto(
    val id: String = "",
    val clientId: String? = null,
    val ingredientName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val category: String = "",
    val expiryDate: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class CreatePantryItemRequest(
    val clientId: String? = null,
    val ingredientName: String,
    val quantity: Double,
    val unit: String,
    val category: String,
    val expiryDate: String? = null
)

// Null fields are left out of the JSON so the API only updates what changed.
@Serializable
data class UpdatePantryItemRequest(
    val ingredientName: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val category: String? = null,
    val expiryDate: String? = null
)