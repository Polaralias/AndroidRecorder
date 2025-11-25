package com.example.recorder.domain.transcription

import kotlinx.coroutines.flow.Flow

interface CloudTranscriptionEngine {
    fun transcribe(
        recordingId: Long,
        source: TranscriptionSource
    ): Flow<TranscriptionUpdate>

    suspend fun cancel(recordingId: Long)

    suspend fun ensureModel()
}
