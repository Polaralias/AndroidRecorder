package com.example.recorder.data.transcription.googlecloud

import android.util.Base64
import com.example.recorder.data.transcription.googlecloud.model.GoogleLongRunningRecognizeRequest
import com.example.recorder.data.transcription.googlecloud.model.GoogleRecognitionAudio
import com.example.recorder.data.transcription.googlecloud.model.GoogleRecognitionConfig
import com.example.recorder.data.transcription.googlecloud.model.GoogleRecognizeRequest
import com.example.recorder.data.transcription.googlecloud.model.ProxyTranscriptionResponse
import com.example.recorder.data.transcription.googlecloud.model.ProxyValidateKeyRequest
import com.example.recorder.domain.repository.ApiKeyTestResult
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class GoogleSpeechClient @Inject constructor(
    private val speechApi: GoogleSpeechApi,
    private val proxyApi: SpeechProxyApi?,
    private val proxyBaseUrl: String
) {

    suspend fun transcribe(apiKey: String, audioFile: File): String = withContext(Dispatchers.IO) {
        val proxyResult = proxyApi
            ?.takeIf { proxyBaseUrl.isNotBlank() }
            ?.runCatching { transcribeViaProxy(apiKey, audioFile) }
            ?.getOrNull()

        if (proxyResult != null) return@withContext proxyResult.transcript

        transcribeDirect(apiKey, audioFile)
    }

    suspend fun testApiKey(apiKey: String): ApiKeyTestResult = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return@withContext ApiKeyTestResult(false, "API key cannot be empty")

        proxyApi
            ?.takeIf { proxyBaseUrl.isNotBlank() }
            ?.runCatching { testKeyThroughProxy(trimmed) }
            ?.getOrNull()
            ?.let { return@withContext ApiKeyTestResult(it.valid, it.message ?: "Proxy connection succeeded") }

        return@withContext try {
            val request = GoogleRecognizeRequest(
                config = GoogleRecognitionConfig(
                    encoding = "LINEAR16",
                    languageCode = "en-US"
                ),
                audio = GoogleRecognitionAudio(content = "")
            )
            val response = speechApi.recognize(trimmed, request)
            val transcript = response.results.firstOrNull()?.alternatives?.firstOrNull()?.transcript
            ApiKeyTestResult(true, transcript?.ifBlank { "Key accepted" } ?: "Key accepted")
        } catch (exception: HttpException) {
            if (exception.code() == 400) {
                ApiKeyTestResult(true, "Key accepted; audio missing in test request")
            } else {
                ApiKeyTestResult(false, exception.message())
            }
        } catch (throwable: Throwable) {
            ApiKeyTestResult(false, throwable.message ?: "Unknown error")
        }
    }

    private suspend fun transcribeDirect(apiKey: String, audioFile: File): String {
        val audioBytes = withContext(Dispatchers.IO) { audioFile.readBytes() }
        val base64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val request = GoogleLongRunningRecognizeRequest(
            config = GoogleRecognitionConfig(
                encoding = "LINEAR16",
                languageCode = "en-US",
                enableAutomaticPunctuation = true,
                audioChannelCount = null
            ),
            audio = GoogleRecognitionAudio(content = base64)
        )

        val operation = speechApi.longRunningRecognize(apiKey, request)
        val response = operation.response
        if (operation.error != null) {
            throw IllegalStateException(operation.error.message ?: "Cloud transcription failed")
        }

        val transcript = response?.results
            ?.flatMap { it.alternatives }
            ?.mapNotNull { it.transcript }
            ?.joinToString(separator = " ")

        return transcript?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Transcription started. Check backend proxy for completion of operation ${operation.name}")
    }

    private suspend fun transcribeViaProxy(apiKey: String, audioFile: File): ProxyTranscriptionResponse {
        val requestBody = audioFile.asRequestBody("audio/wav".toMediaType())
        val part = MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
        val apiKeyPart: RequestBody = apiKey.toRequestBody("text/plain".toMediaType())
        val languagePart: RequestBody = "en-US".toRequestBody("text/plain".toMediaType())
        return proxyApi!!.createTranscription(part, apiKeyPart, languagePart)
    }

    private suspend fun testKeyThroughProxy(apiKey: String) = proxyApi!!.testKey(
        ProxyValidateKeyRequest(apiKey)
    )
}
