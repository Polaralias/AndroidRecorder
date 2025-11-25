package com.example.recorder.data.transcription.googlecloud.model

import com.squareup.moshi.Json

data class ProxyTranscriptionRequest(
    @Json(name = "apiKey") val apiKey: String,
    @Json(name = "languageCode") val languageCode: String = "en-US"
)

data class ProxyTranscriptionResponse(
    @Json(name = "transcript") val transcript: String
)

data class ProxyValidateKeyRequest(
    @Json(name = "apiKey") val apiKey: String
)

data class ProxyValidateKeyResponse(
    @Json(name = "valid") val valid: Boolean,
    @Json(name = "message") val message: String? = null
)
