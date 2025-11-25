package com.example.recorder.domain.transcription

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

sealed interface TranscriptionSource {
    data class FilePath(val path: String) : TranscriptionSource
    data class Stream(val open: suspend () -> InputStream) : TranscriptionSource
}

data class TranscriptionProgress(
    val percent: Int,
    val message: String? = null
)

sealed interface TranscriptionUpdate {
    data class Progress(val progress: TranscriptionProgress) : TranscriptionUpdate
    data class Completed(val text: String) : TranscriptionUpdate
    data class Failed(val throwable: Throwable) : TranscriptionUpdate
}

interface LocalTranscriptionEngine {
    fun transcribe(
        recordingId: Long,
        source: TranscriptionSource
    ): Flow<TranscriptionUpdate>

    suspend fun cancel(recordingId: Long)

    suspend fun ensureModel()
}
