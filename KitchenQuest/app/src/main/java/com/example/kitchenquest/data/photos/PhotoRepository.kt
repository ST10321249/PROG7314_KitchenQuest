package com.example.kitchenquest.data.photos

import com.example.kitchenquest.data.network.ApiClient
import com.example.kitchenquest.data.network.apiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

interface PhotoRepository {
    suspend fun upload(bytes: ByteArray, contentType: String): Result<String>
    suspend fun attachToHistory(historyEntryId: String, photoFileId: String): Result<Unit>
    suspend fun download(photoFileId: String): Result<ByteArray>
}

object NoOpPhotoRepository : PhotoRepository {
    private val unavailable = IllegalStateException("Photos are not available")
    override suspend fun upload(bytes: ByteArray, contentType: String) = Result.failure<String>(unavailable)
    override suspend fun attachToHistory(historyEntryId: String, photoFileId: String) = Result.failure<Unit>(unavailable)
    override suspend fun download(photoFileId: String) = Result.failure<ByteArray>(unavailable)
}

class DefaultPhotoRepository(
    private val photoApi: PhotoApi = ApiClient.create(PhotoApi::class.java)
) : PhotoRepository {

    override suspend fun upload(bytes: ByteArray, contentType: String): Result<String> = apiCall {
        photoApi.upload(bytes.toRequestBody(contentType.toMediaType())).id
    }

    override suspend fun attachToHistory(historyEntryId: String, photoFileId: String): Result<Unit> = apiCall {
        photoApi.attachToHistory(historyEntryId, AttachPhotoRequest(photoFileId))
        Unit
    }

    override suspend fun download(photoFileId: String): Result<ByteArray> = apiCall {
        photoApi.download(photoFileId).use { it.bytes() }
    }
}