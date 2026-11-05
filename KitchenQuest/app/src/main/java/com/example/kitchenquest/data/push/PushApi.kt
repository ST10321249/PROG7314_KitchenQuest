package com.example.kitchenquest.data.push

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String
)

@Serializable
data class DeviceDto(
    val id: String = "",
    val platform: String = "",
    val lastSeenAt: String? = null
)

@Serializable
data class TestNotificationRequest(
    val token: String
)

@Serializable
data class TestNotificationResultDto(
    val sent: Int = 0,
    val failed: Int = 0,
    val removed: Int = 0
)

interface DeviceApi {

    @POST("api/devices")
    suspend fun register(@Body request: RegisterDeviceRequest): DeviceDto

    @DELETE("api/devices/{token}")
    suspend fun unregister(@Path("token") token: String)
}

interface PushTestApi {

    @POST("api/notifications/test")
    suspend fun sendTest(@Body request: TestNotificationRequest): TestNotificationResultDto
}