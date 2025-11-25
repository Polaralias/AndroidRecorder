package com.example.recorder.data.drive

import com.example.recorder.data.local.RecordingDao
import com.example.recorder.data.local.toDomain
import com.example.recorder.data.local.toEntity
import com.example.recorder.domain.model.BackupResult
import com.example.recorder.domain.model.BackupSettings
import com.example.recorder.domain.model.BackupStatus
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.DriveBackupRepository
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile

@Singleton
class DriveBackupRepositoryImpl @Inject constructor(
    private val authManager: DriveAuthManager,
    private val recordingDao: RecordingDao,
    private val preferences: BackupPreferencesDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : DriveBackupRepository {

    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    override val backupStatus: Flow<BackupStatus> = _status.asStateFlow()
    override val backupSettings: Flow<BackupSettings> = preferences.settings

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        preferences.setAutoBackup(enabled)
    }

    override suspend fun hasExistingBackup(recordingId: Long): Boolean =
        recordingDao.isBackedUp(recordingId) == true

    override suspend fun backupPendingRecordings(): BackupStatus = withContext(dispatcher) {
        val account = authManager.lastSignedInAccount()
            ?: return@withContext BackupStatus.Failed("Sign in to Google Drive to start backups").also {
                _status.value = it
            }
        val driveService = authManager.buildDriveService(account)
        val pending = recordingDao.getPendingBackups().map { it.toDomain() }
        if (pending.isEmpty()) {
            val now = Instant.now()
            preferences.updateLastAttempt(now, null)
            val completed = BackupStatus.Completed(now, 0)
            _status.value = completed
            return@withContext completed
        }
        _status.value = BackupStatus.InProgress
        var successCount = 0
        var failureMessage: String? = null
        pending.forEach { recording ->
            when (val result = performBackup(recording, driveService)) {
                is BackupResult.Success -> successCount++
                is BackupResult.Failure -> failureMessage = result.throwable.message ?: "Failed to backup"
                else -> Unit
            }
        }
        val completed = if (failureMessage != null) {
            BackupStatus.Failed(failureMessage!!)
        } else {
            BackupStatus.Completed(Instant.now(), successCount)
        }
        _status.value = completed
        completed
    }

    override suspend fun backupRecording(recording: Recording): BackupResult = withContext(dispatcher) {
        val account = authManager.lastSignedInAccount()
            ?: return@withContext BackupResult.Skipped("Sign in to Google Drive first")
        val driveService = authManager.buildDriveService(account)
        performBackup(recording, driveService)
    }

    private suspend fun performBackup(recording: Recording, driveService: Drive): BackupResult {
        preferences.updateLastAttempt(Instant.now(), null)
        val initialStatus = recordingDao.isBackedUp(recording.id)
        if (initialStatus == true && recording.driveFileId != null) {
            return BackupResult.Skipped("Already backed up")
        }

        var delayMs = 1_000L
        repeat(MAX_ATTEMPTS) { attempt ->
            val now = Instant.now()
            try {
                val fileId = uploadRecordingFile(driveService, recording)
                val entity = recording.copy(
                    isBackedUp = true,
                    driveFileId = fileId,
                    lastBackupAttempt = now
                ).toEntity()
                recordingDao.upsert(entity)
                preferences.updateLastAttempt(now, null)
                _status.value = BackupStatus.Completed(now, 1)
                return BackupResult.Success(fileId)
            } catch (io: IOException) {
                recordingDao.updateBackupState(
                    id = recording.id,
                    isBackedUp = false,
                    driveFileId = recording.driveFileId,
                    lastBackupAttempt = now.toEpochMilli()
                )
                preferences.updateLastAttempt(now, io.message)
                if (attempt == MAX_ATTEMPTS - 1) {
                    _status.value = BackupStatus.Failed(io.message ?: "Network error")
                    return BackupResult.Failure(io)
                }
                _status.value = BackupStatus.Failed(io.message ?: "Network unavailable")
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            } catch (throwable: Throwable) {
                _status.value = BackupStatus.Failed(throwable.message ?: "Backup failed")
                return BackupResult.Failure(throwable)
            }
        }
        return BackupResult.Failure(IOException("Unknown backup failure"))
    }

    private fun uploadRecordingFile(drive: Drive, recording: Recording): String {
        val metadata = DriveFile().apply {
            name = recording.title.ifBlank { "Recording_${recording.id}" }
            description = "Audio recording from Recorder app"
            parents = listOf("appDataFolder")
        }
        val file = File(recording.filePath)
        val mediaContent = FileContent("audio/m4a", file)
        val created = drive.files().create(metadata, mediaContent)
            .setFields("id")
            .execute()
        return created.id
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val MAX_BACKOFF_MS = 8_000L
    }
}
