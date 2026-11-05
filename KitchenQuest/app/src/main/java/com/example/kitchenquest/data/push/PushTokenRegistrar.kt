package com.example.kitchenquest.data.push

import android.util.Log
import com.example.kitchenquest.data.network.apiCall
import kotlinx.coroutines.CancellationException

class PushTokenRegistrar(
    private val deviceApi: DeviceApi,
    private val tokenProvider: suspend () -> String,
    private val isSignedIn: () -> Boolean
) {

    suspend fun currentToken(): Result<String> = try {
        Result.success(tokenProvider())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Log.w(TAG, "Could not get the Firebase Messaging token", error)
        Result.failure(error)
    }

    suspend fun registerCurrentDevice(): Result<String> {
        if (!isSignedIn()) return Result.failure(IllegalStateException("Nobody is signed in"))
        val token = currentToken().getOrElse { return Result.failure(it) }
        return register(token).map { token }
    }

    suspend fun register(token: String): Result<Unit> {
        if (!isSignedIn()) {
            Log.d(TAG, "Skipped registering the push token because nobody is signed in")
            return Result.success(Unit)
        }
        return apiCall { deviceApi.register(RegisterDeviceRequest(token = token, platform = PLATFORM)) }
            .map { }
            .onSuccess { Log.d(TAG, "Registered this phone for push notifications") }
            .onFailure { Log.w(TAG, "Could not register this phone for push notifications", it) }
    }

    private companion object {
        const val TAG = "PushTokenRegistrar"
        const val PLATFORM = "android"
    }
}

class PushTester(
    private val registrar: PushTokenRegistrar,
    private val pushTestApi: PushTestApi
) {

    suspend fun sendTest(): Result<TestNotificationResultDto> {
        val token = registrar.registerCurrentDevice().getOrElse { return Result.failure(it) }
        return apiCall { pushTestApi.sendTest(TestNotificationRequest(token = token)) }
    }
}