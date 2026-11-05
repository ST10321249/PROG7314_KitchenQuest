package com.example.kitchenquest.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kitchenquest.data.AppRepositories
import com.example.kitchenquest.data.push.TestNotificationResultDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PushTestState {
    data object Idle : PushTestState
    data object Sending : PushTestState
    data object Sent : PushTestState
    data class Failed(val reason: String) : PushTestState
}

class PushTestViewModel(
    private val sendTest: suspend () -> Result<TestNotificationResultDto> = { AppRepositories.pushTester().sendTest() }
) : ViewModel() {

    private val _state = MutableStateFlow<PushTestState>(PushTestState.Idle)
    val state: StateFlow<PushTestState> = _state.asStateFlow()

    fun send() {
        if (_state.value == PushTestState.Sending) return
        _state.value = PushTestState.Sending
        viewModelScope.launch {
            _state.value = sendTest().fold(
                onSuccess = { result ->
                    if (result.sent > 0) PushTestState.Sent
                    else PushTestState.Failed("Firebase did not accept the message")
                },
                onFailure = { PushTestState.Failed(it.message ?: "Unknown error") }
            )
        }
    }
}