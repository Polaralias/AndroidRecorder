package com.example.recorder.data.transcription.googlecloud.model

import com.squareup.moshi.Json

data class GoogleLongRunningRecognizeRequest(
    @Json(name = "config") val config: GoogleRecognitionConfig,
    @Json(name = "audio") val audio: GoogleRecognitionAudio
)

data class GoogleRecognizeRequest(
    @Json(name = "config") val config: GoogleRecognitionConfig,
    @Json(name = "audio") val audio: GoogleRecognitionAudio
)

data class GoogleRecognitionConfig(
    @Json(name = "encoding") val encoding: String,
    @Json(name = "languageCode") val languageCode: String,
    @Json(name = "enableAutomaticPunctuation") val enableAutomaticPunctuation: Boolean = true,
    @Json(name = "audioChannelCount") val audioChannelCount: Int? = null
)

data class GoogleRecognitionAudio(
    @Json(name = "content") val content: String
)

data class GoogleOperationResponse(
    @Json(name = "name") val name: String?,
    @Json(name = "done") val done: Boolean?,
    @Json(name = "response") val response: GoogleRecognizeResponse?,
    @Json(name = "error") val error: GoogleStatus?
)

data class GoogleRecognizeResponse(
    @Json(name = "results") val results: List<GoogleSpeechRecognitionResult> = emptyList()
)

data class GoogleSpeechRecognitionResult(
    @Json(name = "alternatives") val alternatives: List<GoogleSpeechRecognitionAlternative> = emptyList()
)

data class GoogleSpeechRecognitionAlternative(
    @Json(name = "transcript") val transcript: String?,
    @Json(name = "confidence") val confidence: Double?
)

data class GoogleStatus(
    @Json(name = "code") val code: Int?,
    @Json(name = "message") val message: String?
)
