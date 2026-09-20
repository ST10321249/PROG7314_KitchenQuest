package com.example.kitchenquest.data.shopping

import kotlinx.serialization.Serializable

@Serializable
data class ShoppingItemDto(
    val id: String = "",
    val ingredientName: String = "",
    val quantity: Double? = null,
    val unit: String = "",
    val isPurchased: Boolean = false
)

@Serializable
data class CreateShoppingItemRequest(
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