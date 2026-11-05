package com.example.kitchenquest.data.photos

import com.example.kitchenquest.data.history.CookingHistoryDto
import kotlinx.serialization.Serializable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class StoredPhotoDto(
    val id: String = "",
    val contentType: String = "",
    val size: Long = 0,
    val createdAt: String? = null
)

@Serializable
data class AttachPhotoRequest(
    val photoFileId: String?
)

interface PhotoApi {

    @POST("api/files")
    suspend fun upload(@Body photo: RequestBody): StoredPhotoDto

    @GET("api/files/{id}")
    suspend fun download(@Path("id") id: String): ResponseBody

    @PATCH("api/history/{id}")
    suspend fun attachToHistory(
        @Path("id") historyEntryId: String,
        @Body request: AttachPhotoRequest
    ): CookingHistoryDto
}