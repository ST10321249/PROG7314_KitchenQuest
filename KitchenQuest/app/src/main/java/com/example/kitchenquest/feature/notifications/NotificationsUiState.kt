package com.example.kitchenquest.feature.notifications

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val expiringCount: Int = 0,
    val shoppingCount: Int = 0,
    val errorMessage: String? = null
)
