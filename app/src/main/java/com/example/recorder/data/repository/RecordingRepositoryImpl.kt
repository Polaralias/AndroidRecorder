package com.example.recorder.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.recorder.R
import com.example.recorder.data.file.RecordingFileManager
import com.example.recorder.data.local.RecordingDao
import com.example.recorder.data.local.toDomain
import com.example.recorder.data.local.toEntity
import com.example.recorder.data.recording.RecorderController
import com.example.recorder.data.recording.RecordingStateStore
import com.example.recorder.data.transcription.TranscriptionScheduler
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingSessionState
import com.example.recorder.domain.model.TranscriptionStatus
import com.example.recorder.domain.repository.DriveBackupRepository
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.repository.TranscriptionPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val fileManager: RecordingFileManager,
    private val controller: RecorderController,
    private val stateStore: RecordingStateStore,
    private val driveBackupRepository: DriveBackupRepository,
    private val transcriptionScheduler: TranscriptionScheduler,
    private val transcriptionPreferencesRepository: TranscriptionPreferencesRepository,
    @ApplicationContext private val context: Context,
) : RecordingRepository {

    init {
        controller.requestStateSync()
    }

    override val sessionState: Flow<RecordingSessionState> = stateStore.state

    override fun observeRecordings(): Flow<List<Recording>> =
        recordingDao.observeRecordings().map { list -> list.map { it.toDomain() } }

    override fun observeRecording(id: Long): Flow<Recording?> =
        recordingDao.observeRecording(id).map { it?.toDomain() }

    override suspend fun getRecording(id: Long): Recording? =
        recordingDao.getRecording(id)?.toDomain()

    override suspend fun startRecording(): RecordingSessionState = withContext(Dispatchers.IO) {
        if (!hasMicrophonePermission()) {
            val error = RecordingSessionState.Error(
                context.getString(R.string.error_microphone_permission)
            )
            stateStore.setError(error.message)
            return@withContext error
        }

        val file = runCatching { fileManager.createFile() }
            .getOrElse { throwable ->
                val message = when (throwable) {
                    is IOException -> context.getString(R.string.error_storage_unavailable)
                    else -> throwable.message ?: context.getString(R.string.error_storage_unavailable)
                }
                stateStore.setError(message)
                return@withContext RecordingSessionState.Error(message)
            }

        val startTime = Instant.now()
        controller.startRecording(file.absolutePath, startTime.toEpochMilli())
        stateStore.startSession(startTime, file.absolutePath)
        stateStore.state.value
    }

    override suspend fun pauseRecording() {
        controller.pauseRecording()
        stateStore.setPaused()
    }

    override suspend fun resumeRecording() {
        controller.resumeRecording()
        stateStore.setResumed()
    }

    override suspend fun stopRecording(): Recording? = withContext(Dispatchers.IO) {
        val current = stateStore.state.value
        controller.stopRecording()
        if (current is RecordingSessionState.Active) {
            val endTime = Instant.now()
            val duration = current.elapsedMillis
            val recording = Recording(
                title = endTime.toString(),
                filePath = current.filePath,
                createdAt = current.startTime,
                durationMillis = duration,
                transcriptionStatus = TranscriptionStatus.NOT_STARTED,
                transcriptionText = null,
                transcriptionUpdatedAt = null,
                isBackedUp = false,
                driveFileId = null,
                lastBackupAttempt = null
            )
            val id = recordingDao.upsert(recording.toEntity())
            val stored = recording.copy(id = id)
            stateStore.stopSession()
            triggerAutoBackupIfEnabled(stored)
            val mode = transcriptionPreferencesRepository.transcriptionMode.first()
            transcriptionScheduler.enqueue(stored.id, mode)
            stored
        } else {
            stateStore.stopSession()
            null
        }
    }

    private suspend fun triggerAutoBackupIfEnabled(recording: Recording) {
        val autoBackupEnabled = driveBackupRepository.backupSettings.first().autoBackupEnabled
        if (autoBackupEnabled) {
            driveBackupRepository.backupRecording(recording)
        }
    }

    override suspend fun updateTranscriptionStatus(
        id: Long,
        status: TranscriptionStatus,
        text: String?
    ) {
        recordingDao.updateTranscription(
            id = id,
            status = status,
            text = text,
            updatedAt = Instant.now().toEpochMilli()
        )
    }

    override suspend fun getPendingTranscriptions(): List<Recording> =
        recordingDao.getPendingTranscriptions().map { it.toDomain() }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
}
