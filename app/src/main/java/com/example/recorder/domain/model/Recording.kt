package com.example.recorder.domain.model

import java.time.Instant

enum class TranscriptionStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class Recording(
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val createdAt: Instant,
    val durationMillis: Long,
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.NOT_STARTED,
    val isBackedUp: Boolean = false
)

sealed interface RecordingSessionState {
    data object Idle : RecordingSessionState
    data class Active(
        val startTime: Instant,
        val filePath: String,
        val isPaused: Boolean,
        val amplitude: Int,
        val elapsedMillis: Long
    ) : RecordingSessionState
    data class Error(val message: String) : RecordingSessionState
}
