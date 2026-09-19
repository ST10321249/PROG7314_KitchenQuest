package com.example.kitchenquest.data.user

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserApi {

    @POST("api/users/sync")
    suspend fun syncUser(
        @Body request: SyncUserRequest
    ): UserProfileDto

    @GET("api/users/me")
    suspend fun getMe(): UserProfileDto

    @PUT("api/users/me")
    suspend fun updateMe(
        @Body request: UpdateProfileRequest
    ): UserProfileDto
}
