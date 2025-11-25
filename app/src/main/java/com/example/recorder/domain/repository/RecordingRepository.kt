package com.example.recorder.domain.repository

import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingSessionState
import com.example.recorder.domain.model.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    val sessionState: Flow<RecordingSessionState>

    fun observeRecordings(): Flow<List<Recording>>

    fun observeRecording(id: Long): Flow<Recording?>

    suspend fun getRecording(id: Long): Recording?

    suspend fun startRecording(): RecordingSessionState

    suspend fun pauseRecording()

    suspend fun resumeRecording()

    suspend fun stopRecording(): Recording?

    suspend fun updateTranscriptionStatus(
        id: Long,
        status: TranscriptionStatus,
        text: String? = null
    )

    suspend fun getPendingTranscriptions(): List<Recording>
}
