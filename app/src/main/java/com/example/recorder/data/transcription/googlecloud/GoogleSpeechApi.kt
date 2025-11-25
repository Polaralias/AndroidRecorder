package com.example.recorder.data.transcription.googlecloud

import com.example.recorder.data.transcription.googlecloud.model.GoogleLongRunningRecognizeRequest
import com.example.recorder.data.transcription.googlecloud.model.GoogleOperationResponse
import com.example.recorder.data.transcription.googlecloud.model.GoogleRecognizeRequest
import com.example.recorder.data.transcription.googlecloud.model.GoogleRecognizeResponse
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface GoogleSpeechApi {
    @GET("v1/operations/{name}")
    suspend fun getOperation(
        @Path("name") name: String,
        @Query("key") apiKey: String
    ): GoogleOperationResponse

    @POST("v1/speech:longrunningrecognize")
    suspend fun longRunningRecognize(
        @Query("key") apiKey: String,
        @Body request: GoogleLongRunningRecognizeRequest
    ): GoogleOperationResponse

    @POST("v1/speech:recognize")
    suspend fun recognize(
        @Query("key") apiKey: String,
        @Body request: GoogleRecognizeRequest
    ): GoogleRecognizeResponse
}
