package com.example.kitchenquest.data.user

import com.example.kitchenquest.data.network.ApiClient
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UserApiTest {

    private class Recorded(
        val method: String,
        val path: String,
        val body: String?
    )

    private val profileJson = """
        {
          "_id": "6aadb1c1ed585ca3c83ebd40",
          "firebaseUid": "uid-1",
          "__v": 0,
          "displayName": "Test User",
          "email": "test@example.com",
          "dietaryPreferences": ["Vegetarian"],
          "avoidedIngredients": ["Peanuts"],
          "createdAt": "2026-09-18T21:48:49.623Z",
          "updatedAt": "2026-09-18T21:48:49.811Z"
        }
    """.trimIndent()

    private fun api(
        responseCode: Int = 200,
        responseBody: String = profileJson,
        onRequest: (Recorded) -> Unit = {}
    ): UserApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    val request: Request = chain.request()
                    val body = request.body?.let {
                        val buffer = Buffer()
                        it.writeTo(buffer)
                        buffer.readUtf8()
                    }
                    onRequest(
                        Recorded(request.method, request.url.encodedPath, body)
                    )

                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(responseCode)
                        .message("test")
                        .body(
                            responseBody.toResponseBody(
                                "application/json".toMediaType()
                            )
                        )
                        .build()
                }
            )
            .build()

        return Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(
                ApiClient.json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(UserApi::class.java)
    }

    @Test
    fun getMeCallsTheRightEndpointAndParsesTheProfile() = runBlocking {
        var recorded: Recorded? = null

        val profile = api(onRequest = { recorded = it }).getMe()

        assertEquals("GET", recorded?.method)
        assertEquals("/api/users/me", recorded?.path)
        assertEquals("Test User", profile.displayName)
        assertEquals("test@example.com", profile.email)
        assertEquals(listOf("Vegetarian"), profile.dietaryPreferences)
        assertEquals(listOf("Peanuts"), profile.avoidedIngredients)
    }

    @Test
    fun syncUserPostsOnlyTheFieldsThatAreSet() = runBlocking {
        var recorded: Recorded? = null

        api(responseCode = 201, onRequest = { recorded = it }).syncUser(
            SyncUserRequest(
                displayName = "Test User",
                dietaryPreferences = listOf("Vegan")
            )
        )

        assertEquals("POST", recorded?.method)
        assertEquals("/api/users/sync", recorded?.path)
        assertEquals(
            """{"displayName":"Test User","dietaryPreferences":["Vegan"]}""",
            recorded?.body
        )
    }

    @Test
    fun updateMePutsOnlyTheChangedFieldsAndKeepsEmptyLists() = runBlocking {
        var recorded: Recorded? = null

        api(onRequest = { recorded = it }).updateMe(
            UpdateProfileRequest(avoidedIngredients = emptyList())
        )

        assertEquals("PUT", recorded?.method)
        assertEquals("/api/users/me", recorded?.path)
        assertEquals("""{"avoidedIngredients":[]}""", recorded?.body)
    }

    @Test
    fun missingProfileFieldsFallBackToDefaults() = runBlocking {
        val profile = api(responseBody = """{"displayName":"Only Name"}""").getMe()

        assertEquals("Only Name", profile.displayName)
        assertEquals("", profile.email)
        assertTrue(profile.dietaryPreferences.isEmpty())
        assertTrue(profile.avoidedIngredients.isEmpty())
    }

    @Test
    fun errorResponsesThrowHttpExceptionWithTheStatusCode() = runBlocking {
        for (code in listOf(401, 404)) {
            try {
                api(
                    responseCode = code,
                    responseBody = """{"error":"nope"}"""
                ).getMe()
                fail("Expected HttpException for $code")
            } catch (error: HttpException) {
                assertEquals(code, error.code())
            }
        }
    }

    @Test
    fun nullRequestFieldsAreNotSerialised() {
        val json = ApiClient.json.encodeToString(
            SyncUserRequest.serializer(),
            SyncUserRequest()
        )

        assertEquals("{}", json)
        assertNull(SyncUserRequest().displayName)
    }
}
