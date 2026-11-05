package com.example.kitchenquest.data.push

import com.example.kitchenquest.data.network.ApiException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class PushTokenRegistrarTest {

    private class FakeDeviceApi : DeviceApi {
        val registered = mutableListOf<RegisterDeviceRequest>()
        var failWith: Exception? = null

        override suspend fun register(request: RegisterDeviceRequest): DeviceDto {
            failWith?.let { throw it }
            registered += request
            return DeviceDto(id = "d1", platform = request.platform)
        }

        override suspend fun unregister(token: String) = Unit
    }

    private class FakePushTestApi : PushTestApi {
        val requests = mutableListOf<TestNotificationRequest>()
        var sent = 1

        override suspend fun sendTest(request: TestNotificationRequest): TestNotificationResultDto {
            requests += request
            return TestNotificationResultDto(sent = sent)
        }
    }

    private val token = "phone-token-1234567890-abcdefghij"
    private val deviceApi = FakeDeviceApi()
    private var signedIn = true
    private var tokenError: Exception? = null

    private val registrar = PushTokenRegistrar(
        deviceApi = deviceApi,
        tokenProvider = { tokenError?.let { throw it } ?: token },
        isSignedIn = { signedIn }
    )

    @Test
    fun aSignedInPhoneSendsItsTokenToTheServer() = runBlocking {
        val result = registrar.registerCurrentDevice()

        assertEquals(token, result.getOrThrow())
        assertEquals(listOf(RegisterDeviceRequest(token, "android")), deviceApi.registered)
    }

    @Test
    fun nothingIsSentWhenNobodyIsSignedIn() = runBlocking {
        signedIn = false

        assertTrue(registrar.registerCurrentDevice().isFailure)
        assertTrue(deviceApi.registered.isEmpty())
    }

    @Test
    fun aNewTokenWhileSignedOutIsIgnoredWithoutAnError() = runBlocking {
        signedIn = false

        assertTrue(registrar.register(token).isSuccess)
        assertTrue(deviceApi.registered.isEmpty())
    }

    @Test
    fun aTokenThatCannotBeFetchedGivesAFailure() = runBlocking {
        tokenError = IllegalStateException("No Google Play services")

        assertTrue(registrar.registerCurrentDevice().isFailure)
        assertTrue(deviceApi.registered.isEmpty())
    }

    @Test
    fun noConnectionGivesAnApiFailure() = runBlocking {
        deviceApi.failWith = IOException("offline")

        val error = registrar.registerCurrentDevice().exceptionOrNull()

        assertTrue(error is ApiException)
    }

    @Test
    fun theTestPushRegistersThisPhoneThenAsksForAMessageToIt() = runBlocking {
        val pushTestApi = FakePushTestApi()
        val tester = PushTester(registrar, pushTestApi)

        val result = tester.sendTest()

        assertEquals(1, result.getOrThrow().sent)
        assertEquals(1, deviceApi.registered.size)
        assertEquals(listOf(TestNotificationRequest(token)), pushTestApi.requests)
    }

    @Test
    fun theTestPushIsNotSentIfRegisteringFails() = runBlocking {
        val pushTestApi = FakePushTestApi()
        deviceApi.failWith = IOException("offline")

        assertTrue(PushTester(registrar, pushTestApi).sendTest().isFailure)
        assertTrue(pushTestApi.requests.isEmpty())
    }
}