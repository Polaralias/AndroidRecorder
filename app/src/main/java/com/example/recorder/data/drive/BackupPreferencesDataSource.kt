package com.example.recorder.data.drive

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.recorder.domain.model.BackupSettings
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BackupPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val autoBackupKey = booleanPreferencesKey("auto_backup_enabled")
    private val lastBackupEpochKey = longPreferencesKey("last_backup_epoch")
    private val lastBackupErrorKey = stringPreferencesKey("last_backup_error")

    val settings: Flow<BackupSettings> = dataStore.data.map { preferences ->
        BackupSettings(
            autoBackupEnabled = preferences[autoBackupKey] ?: false,
            lastBackupAttempt = preferences[lastBackupEpochKey]?.let(Instant::ofEpochMilli),
            lastError = preferences[lastBackupErrorKey]
        )
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[autoBackupKey] = enabled }
    }

    suspend fun updateLastAttempt(timestamp: Instant, error: String?) {
        dataStore.edit { prefs ->
            prefs[lastBackupEpochKey] = timestamp.toEpochMilli()
            if (error == null) {
                prefs.remove(lastBackupErrorKey)
            } else {
                prefs[lastBackupErrorKey] = error
            }
        }
    }
}
