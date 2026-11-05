package com.example.kitchenquest.data.photos

import com.example.kitchenquest.data.history.CookingHistoryDto
import com.example.kitchenquest.data.network.ApiException
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PhotoRepositoryTest {

    private class FakePhotoApi : PhotoApi {
        var uploadedBytes: ByteArray? = null
        var uploadedType: String? = null
        val attached = mutableListOf<Pair<String, String?>>()
        var failUpload = false

        override suspend fun upload(photo: RequestBody): StoredPhotoDto {
            if (failUpload) throw IOException("offline")
            val buffer = Buffer()
            photo.writeTo(buffer)
            uploadedBytes = buffer.readByteArray()
            uploadedType = photo.contentType()?.toString()
            return StoredPhotoDto(id = "photo-1", contentType = uploadedType.orEmpty(), size = uploadedBytes!!.size.toLong())
        }

        override suspend fun download(id: String): ResponseBody =
            byteArrayOf(1, 2, 3, 4).toResponseBody()

        override suspend fun attachToHistory(historyEntryId: String, request: AttachPhotoRequest): CookingHistoryDto {
            attached += historyEntryId to request.photoFileId
            return CookingHistoryDto(id = historyEntryId, photoFileId = request.photoFileId)
        }
    }

    private val api = FakePhotoApi()
    private val repository = DefaultPhotoRepository(api)

    @Test
    fun uploadingSendsTheBytesWithTheirTypeAndReturnsTheServerId() = runBlocking {
        val bytes = byteArrayOf(9, 8, 7)

        val id = repository.upload(bytes, "image/jpeg").getOrThrow()

        assertEquals("photo-1", id)
        assertArrayEquals(bytes, api.uploadedBytes)
        assertEquals("image/jpeg", api.uploadedType)
    }

    @Test
    fun attachingLinksThePhotoToTheHistoryEntry() = runBlocking {
        repository.attachToHistory("entry-1", "photo-1").getOrThrow()

        assertEquals(listOf("entry-1" to "photo-1"), api.attached)
    }

    @Test
    fun downloadingReturnsTheStoredBytes() = runBlocking {
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), repository.download("photo-1").getOrThrow())
    }

    @Test
    fun anUploadWithoutConnectionFailsCleanly() = runBlocking {
        api.failUpload = true

        assertTrue(repository.upload(byteArrayOf(1), "image/jpeg").exceptionOrNull() is ApiException)
    }
}