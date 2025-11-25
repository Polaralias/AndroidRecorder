package com.example.recorder.domain.repository

import com.example.recorder.domain.transcription.TranscriptionMode
import kotlinx.coroutines.flow.Flow

data class ApiKeyTestResult(
    val success: Boolean,
    val message: String
)

interface TranscriptionPreferencesRepository {
    val apiKey: Flow<String?>
    val transcriptionMode: Flow<TranscriptionMode>

    suspend fun updateApiKey(apiKey: String)

    suspend fun clearApiKey()

    suspend fun setMode(mode: TranscriptionMode)

    suspend fun testApiKey(apiKey: String): ApiKeyTestResult
}
