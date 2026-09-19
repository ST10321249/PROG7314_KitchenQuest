package com.example.kitchenquest.data.network

import com.example.kitchenquest.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiClientTest {

    @Test
    fun baseUrlEndsWithSlash() {
        assertTrue(BuildConfig.API_BASE_URL.endsWith("/"))
    }

    @Test
    fun retrofitUsesConfiguredBaseUrl() {
        assertEquals(
            BuildConfig.API_BASE_URL,
            ApiClient.retrofit.baseUrl().toString()
        )
    }
}
