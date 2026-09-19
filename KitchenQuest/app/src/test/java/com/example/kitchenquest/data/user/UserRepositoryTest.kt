package com.example.kitchenquest.data.user

import com.example.kitchenquest.data.network.ApiErrorType
import com.example.kitchenquest.data.network.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class UserRepositoryTest {

    private class FakeUserApi(
        private val failure: Throwable? = null
    ) : UserApi {

        var lastSync: SyncUserRequest? = null
        var lastUpdate: UpdateProfileRequest? = null

        private val profile = UserProfileDto(
            displayName = "Test User",
            email = "test@example.com",
            dietaryPreferences = listOf("Vegetarian"),
            avoidedIngredients = listOf("Peanuts")
        )

        override suspend fun syncUser(request: SyncUserRequest): UserProfileDto {
            lastSync = request
            failure?.let { throw it }
            return profile
        }

        override suspend fun getMe(): UserProfileDto {
            failure?.let { throw it }
            return profile
        }

        override suspend fun updateMe(request: UpdateProfileRequest): UserProfileDto {
            lastUpdate = request
            failure?.let { throw it }
            return profile
        }
    }

    private fun httpError(code: Int, body: String = "{}"): HttpException {
        return HttpException(
            Response.error<Any>(
                code,
                body.toResponseBody("application/json".toMediaType())
            )
        )
    }

    private fun errorTypeOf(failure: Throwable): ApiErrorType = runBlocking {
        val result = DefaultUserRepository(FakeUserApi(failure)).getProfile()
        assertTrue(result.isFailure)
        (result.exceptionOrNull() as ApiException).type
    }

    @Test
    fun getProfileReturnsTheProfile() = runBlocking {
        val result = DefaultUserRepository(FakeUserApi()).getProfile()

        assertEquals("Test User", result.getOrThrow().displayName)
        assertEquals(listOf("Peanuts"), result.getOrThrow().avoidedIngredients)
    }

    @Test
    fun syncUserPassesOnlyTheSuppliedFields() = runBlocking {
        val api = FakeUserApi()

        DefaultUserRepository(api).syncUser(
            displayName = "Test User",
            dietaryPreferences = listOf("Vegan")
        )

        assertEquals("Test User", api.lastSync?.displayName)
        assertEquals(listOf("Vegan"), api.lastSync?.dietaryPreferences)
        assertNull(api.lastSync?.avoidedIngredients)
    }

    @Test
    fun updateProfilePassesTheChangedFields() = runBlocking {
        val api = FakeUserApi()

        DefaultUserRepository(api).updateProfile(
            avoidedIngredients = listOf("Shellfish")
        )

        assertNull(api.lastUpdate?.displayName)
        assertEquals(listOf("Shellfish"), api.lastUpdate?.avoidedIngredients)
    }

    @Test
    fun networkFailuresBecomeNoConnection() {
        assertEquals(ApiErrorType.NO_CONNECTION, errorTypeOf(IOException("offline")))
    }

    @Test
    fun timeoutsBecomeTimeout() {
        assertEquals(ApiErrorType.TIMEOUT, errorTypeOf(SocketTimeoutException()))
    }

    @Test
    fun httpStatusCodesAreMappedToErrorTypes() {
        assertEquals(ApiErrorType.UNAUTHORIZED, errorTypeOf(httpError(401)))
        assertEquals(ApiErrorType.NOT_FOUND, errorTypeOf(httpError(404)))
        assertEquals(ApiErrorType.SERVER, errorTypeOf(httpError(500)))
        assertEquals(ApiErrorType.SERVER, errorTypeOf(httpError(503)))
        assertEquals(ApiErrorType.UNKNOWN, errorTypeOf(httpError(418)))
        assertEquals(ApiErrorType.UNKNOWN, errorTypeOf(IllegalStateException("boom")))
    }

    @Test
    fun invalidRequestShowsTheServerMessage() = runBlocking {
        val failure = httpError(
            400,
            """{"error":"avoidedIngredients: Invalid input: expected array, received string"}"""
        )

        val result = DefaultUserRepository(FakeUserApi(failure)).updateProfile(
            displayName = "x"
        )
        val error = result.exceptionOrNull() as ApiException

        assertEquals(ApiErrorType.INVALID_REQUEST, error.type)
        assertEquals(
            "avoidedIngredients: Invalid input: expected array, received string",
            error.message
        )
    }

    @Test
    fun invalidRequestWithoutAReadableBodyUsesAGenericMessage() = runBlocking {
        val result = DefaultUserRepository(
            FakeUserApi(httpError(400, "not json"))
        ).getProfile()
        val error = result.exceptionOrNull() as ApiException

        assertEquals(ApiErrorType.INVALID_REQUEST, error.type)
        assertEquals("Some of the information entered isn't valid.", error.message)
    }

    @Test
    fun cancellationIsNotSwallowed() {
        try {
            runBlocking {
                DefaultUserRepository(
                    FakeUserApi(CancellationException("cancelled"))
                ).getProfile()
            }
            fail("Expected CancellationException")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }
}
