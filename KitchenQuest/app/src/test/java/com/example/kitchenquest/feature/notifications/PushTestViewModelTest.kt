package com.example.kitchenquest.feature.notifications

import com.example.kitchenquest.data.push.TestNotificationResultDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushTestViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun itStartsIdle() {
        assertEquals(PushTestState.Idle, PushTestViewModel { Result.success(TestNotificationResultDto(sent = 1)) }.state.value)
    }

    @Test
    fun aDeliveredTestShowsSent() {
        val viewModel = PushTestViewModel { Result.success(TestNotificationResultDto(sent = 1)) }

        viewModel.send()

        assertEquals(PushTestState.Sent, viewModel.state.value)
    }

    @Test
    fun aTestFirebaseRejectedShowsFailed() {
        val viewModel = PushTestViewModel { Result.success(TestNotificationResultDto(sent = 0, failed = 1)) }

        viewModel.send()

        assertTrue(viewModel.state.value is PushTestState.Failed)
    }

    @Test
    fun aServerErrorShowsTheReason() {
        val viewModel = PushTestViewModel { Result.failure(IllegalStateException("No registered device")) }

        viewModel.send()

        assertEquals(PushTestState.Failed("No registered device"), viewModel.state.value)
    }
}