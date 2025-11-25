package com.example.recorder.data.transcription.googlecloud

import com.example.recorder.data.transcription.googlecloud.model.ProxyTranscriptionRequest
import com.example.recorder.data.transcription.googlecloud.model.ProxyTranscriptionResponse
import com.example.recorder.data.transcription.googlecloud.model.ProxyValidateKeyRequest
import com.example.recorder.data.transcription.googlecloud.model.ProxyValidateKeyResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SpeechProxyApi {
    @Multipart
    @POST("v1/transcriptions")
    suspend fun createTranscription(
        @Part audio: MultipartBody.Part,
        @Part("apiKey") apiKey: RequestBody,
        @Part("languageCode") languageCode: RequestBody
    ): ProxyTranscriptionResponse

    @POST("v1/keys:test")
    suspend fun testKey(
        @Body request: ProxyValidateKeyRequest
    ): ProxyValidateKeyResponse
}
