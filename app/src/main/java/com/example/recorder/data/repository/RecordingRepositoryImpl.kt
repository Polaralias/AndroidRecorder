package com.example.recorder.data.repository

import com.example.recorder.data.file.RecordingFileManager
import com.example.recorder.data.local.RecordingDao
import com.example.recorder.data.local.toDomain
import com.example.recorder.data.local.toEntity
import com.example.recorder.data.recording.RecordingController
import com.example.recorder.data.recording.RecordingStateStore
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.RecordingSessionState
import com.example.recorder.domain.model.TranscriptionStatus
import com.example.recorder.domain.repository.RecordingRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao,
    private val fileManager: RecordingFileManager,
    private val controller: RecordingController,
    private val stateStore: RecordingStateStore
) : RecordingRepository {

    override val sessionState: Flow<RecordingSessionState> = stateStore.state

    override fun observeRecordings(): Flow<List<Recording>> =
        recordingDao.observeRecordings().map { list -> list.map { it.toDomain() } }

    override suspend fun startRecording(): RecordingSessionState = withContext(Dispatchers.IO) {
        val file = fileManager.createFile()
        controller.startRecording(file.absolutePath)
        val startedState = RecordingSessionState.Active(
            startTime = Instant.now(),
            filePath = file.absolutePath,
            isPaused = false,
            amplitude = 0
        )
        stateStore.update(startedState)
        startedState
    }

    override suspend fun pauseRecording() {
        controller.pauseRecording()
        val current = stateStore.state.value
        if (current is RecordingSessionState.Active) {
            stateStore.update(current.copy(isPaused = true))
        }
    }

    override suspend fun resumeRecording() {
        controller.resumeRecording()
        val current = stateStore.state.value
        if (current is RecordingSessionState.Active) {
            stateStore.update(current.copy(isPaused = false))
        }
    }

    override suspend fun stopRecording(): Recording? = withContext(Dispatchers.IO) {
        val current = stateStore.state.value
        controller.stopRecording()
        if (current is RecordingSessionState.Active) {
            val endTime = Instant.now()
            val duration = endTime.toEpochMilli() - current.startTime.toEpochMilli()
            val recording = Recording(
                title = endTime.toString(),
                filePath = current.filePath,
                createdAt = current.startTime,
                durationMillis = duration,
                transcriptionStatus = TranscriptionStatus.NOT_STARTED,
                isBackedUp = false
            )
            val id = recordingDao.upsert(recording.toEntity())
            val stored = recording.copy(id = id)
            stateStore.update(RecordingSessionState.Idle)
            stored
        } else {
            stateStore.update(RecordingSessionState.Idle)
            null
        }
    }
}
