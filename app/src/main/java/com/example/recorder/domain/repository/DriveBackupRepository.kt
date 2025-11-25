package com.example.recorder.domain.repository

import com.example.recorder.domain.model.BackupResult
import com.example.recorder.domain.model.BackupSettings
import com.example.recorder.domain.model.BackupStatus
import com.example.recorder.domain.model.Recording
import kotlinx.coroutines.flow.Flow

interface DriveBackupRepository {
    val backupStatus: Flow<BackupStatus>
    val backupSettings: Flow<BackupSettings>

    suspend fun setAutoBackupEnabled(enabled: Boolean)

    suspend fun backupRecording(recording: Recording): BackupResult

    suspend fun backupPendingRecordings(): BackupStatus

    suspend fun hasExistingBackup(recordingId: Long): Boolean
}
