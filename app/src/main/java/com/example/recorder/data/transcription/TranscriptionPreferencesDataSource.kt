package com.example.recorder.data.transcription

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.recorder.domain.transcription.TranscriptionMode
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TranscriptionPreferencesDataSource @Inject constructor(
    @Named("transcriptionPrefs") private val dataStore: DataStore<Preferences>
) {

    val transcriptionMode: Flow<TranscriptionMode> = dataStore.data.map { preferences ->
        preferences[MODE_KEY]?.let { stored ->
            runCatching { TranscriptionMode.valueOf(stored) }.getOrDefault(TranscriptionMode.LOCAL)
        } ?: TranscriptionMode.LOCAL
    }

    suspend fun setMode(mode: TranscriptionMode) {
        dataStore.edit { prefs ->
            prefs[MODE_KEY] = mode.name
        }
    }

    companion object {
        private val MODE_KEY = stringPreferencesKey("transcription_mode")
    }
}
