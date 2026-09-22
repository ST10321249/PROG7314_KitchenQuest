package com.example.kitchenquest.data.favourites

import kotlinx.serialization.Serializable

@Serializable
data class FavouriteRecipeDto(
    val id: String = "",
    val recipeSourceId: String = "",
    val recipeTitle: String = "",
    val imageUrl: String? = null
)

@Serializable
data class AddFavouriteRequest(
    val recipeSourceId: String,
    val recipeTitle: String,
    val imageUrl: String? = null
)