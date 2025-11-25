package com.example.recorder.data.repository

import com.example.recorder.data.transcription.CloudCredentialsDataSource
import com.example.recorder.data.transcription.TranscriptionPreferencesDataSource
import com.example.recorder.data.transcription.googlecloud.GoogleSpeechClient
import com.example.recorder.domain.repository.ApiKeyTestResult
import com.example.recorder.domain.repository.TranscriptionPreferencesRepository
import com.example.recorder.domain.transcription.TranscriptionMode
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

class TranscriptionPreferencesRepositoryImpl @Inject constructor(
    private val credentialsDataSource: CloudCredentialsDataSource,
    private val preferencesDataSource: TranscriptionPreferencesDataSource,
    private val speechClient: GoogleSpeechClient
) : TranscriptionPreferencesRepository {

    private val storedApiKey: StateFlow<String?> = credentialsDataSource.apiKey

    override val apiKey: Flow<String?> = storedApiKey

    override val transcriptionMode: Flow<TranscriptionMode> = combine(
        preferencesDataSource.transcriptionMode,
        storedApiKey
    ) { mode, key ->
        if (mode == TranscriptionMode.CLOUD && key.isNullOrBlank()) TranscriptionMode.LOCAL else mode
    }

    override suspend fun updateApiKey(apiKey: String) {
        credentialsDataSource.updateApiKey(apiKey.trim())
    }

    override suspend fun clearApiKey() {
        credentialsDataSource.clearApiKey()
    }

    override suspend fun setMode(mode: TranscriptionMode) {
        preferencesDataSource.setMode(mode)
    }

    override suspend fun testApiKey(apiKey: String): ApiKeyTestResult {
        return speechClient.testApiKey(apiKey.trim())
    }
}
