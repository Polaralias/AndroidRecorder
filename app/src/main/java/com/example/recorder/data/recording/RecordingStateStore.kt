package com.example.recorder.data.recording

import com.example.recorder.domain.model.RecordingSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingStateStore @Inject constructor() {
    private val _state = MutableStateFlow<RecordingSessionState>(RecordingSessionState.Idle)
    val state: StateFlow<RecordingSessionState> = _state

    fun update(state: RecordingSessionState) {
        _state.value = state
    }
}
