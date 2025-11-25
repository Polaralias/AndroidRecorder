package com.example.recorder.domain.repository

import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingSessionState
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    val sessionState: Flow<RecordingSessionState>

    fun observeRecordings(): Flow<List<Recording>>

    suspend fun startRecording(): RecordingSessionState

    suspend fun pauseRecording()

    suspend fun resumeRecording()

    suspend fun stopRecording(): Recording?
}
