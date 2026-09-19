package com.example.kitchenquest.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    private class FakeTokenProvider(
        private val token: String?
    ) : TokenProvider {

        var calls = 0

        override fun getToken(): String? {
            calls++
            return token
        }
    }

    private fun send(provider: TokenProvider): Request {
        var sent: Request? = null

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(provider))
            .addInterceptor(
                Interceptor { chain ->
                    sent = chain.request()
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("".toResponseBody(null))
                        .build()
                }
            )
            .build()

        client.newCall(
            Request.Builder()
                .url("https://example.com/api/users/me")
                .build()
        ).execute().close()

        return requireNotNull(sent)
    }

    @Test
    fun addsBearerTokenWhenSignedIn() {
        val request = send(FakeTokenProvider("abc123"))

        assertEquals("Bearer abc123", request.header("Authorization"))
    }

    @Test
    fun sendsNoAuthorizationHeaderWhenSignedOut() {
        val request = send(FakeTokenProvider(null))

        assertNull(request.header("Authorization"))
        assertNotNull(request.url)
    }

    @Test
    fun asksForATokenOnEveryRequest() {
        val provider = FakeTokenProvider("abc123")

        send(provider)
        send(provider)

        assertEquals(2, provider.calls)
    }
}
