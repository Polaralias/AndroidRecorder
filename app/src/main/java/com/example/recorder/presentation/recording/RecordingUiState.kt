package com.example.recorder.presentation.recording

import com.example.recorder.domain.model.RecordingSessionState

data class RecordingUiState(
    val sessionState: RecordingSessionState = RecordingSessionState.Idle,
    val elapsedMillis: Long = 0L
)
