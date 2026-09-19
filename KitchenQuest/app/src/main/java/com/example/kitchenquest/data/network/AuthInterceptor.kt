package com.example.kitchenquest.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
            ?: return chain.proceed(chain.request())

        val request = chain.request()
            .newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(request)
    }
}
