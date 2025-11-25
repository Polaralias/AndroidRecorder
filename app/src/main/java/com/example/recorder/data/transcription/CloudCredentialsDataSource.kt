package com.example.recorder.data.transcription

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class CloudCredentialsDataSource @Inject constructor(
    @ApplicationContext context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        "cloud_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val apiKeyFlow = MutableStateFlow(preferences.getString(KEY_API, null))

    val apiKey: StateFlow<String?> get() = apiKeyFlow

    suspend fun updateApiKey(value: String) = withContext(Dispatchers.IO) {
        preferences.edit().putString(KEY_API, value).apply()
        apiKeyFlow.value = value
    }

    suspend fun clearApiKey() = withContext(Dispatchers.IO) {
        preferences.edit().remove(KEY_API).apply()
        apiKeyFlow.value = null
    }

    companion object {
        private const val KEY_API = "google_cloud_api_key"
    }
}
