package com.example.recorder.di

import com.example.recorder.BuildConfig
import com.example.recorder.data.transcription.googlecloud.GoogleSpeechApi
import com.example.recorder.data.transcription.googlecloud.GoogleSpeechClient
import com.example.recorder.data.transcription.googlecloud.SpeechProxyApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideGoogleRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("https://speech.googleapis.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()

    @Provides
    @Singleton
    fun provideGoogleSpeechApi(retrofit: Retrofit): GoogleSpeechApi =
        retrofit.create(GoogleSpeechApi::class.java)

    @Provides
    @Singleton
    fun provideProxyRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit? {
        if (BuildConfig.SPEECH_PROXY_BASE_URL.isBlank()) return null
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SPEECH_PROXY_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(client)
            .build()
    }

    @Provides
    @Singleton
    fun provideSpeechProxyApi(retrofit: Retrofit?): SpeechProxyApi? =
        retrofit?.create(SpeechProxyApi::class.java)

    @Provides
    @Singleton
    fun provideSpeechClient(
        speechApi: GoogleSpeechApi,
        proxyApi: SpeechProxyApi?
    ): GoogleSpeechClient = GoogleSpeechClient(
        speechApi = speechApi,
        proxyApi = proxyApi,
        proxyBaseUrl = BuildConfig.SPEECH_PROXY_BASE_URL
    )
}
