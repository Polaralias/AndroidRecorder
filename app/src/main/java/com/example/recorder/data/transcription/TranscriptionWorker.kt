package com.example.recorder.data.transcription

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.recorder.domain.model.TranscriptionStatus
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.repository.TranscriptionPreferencesRepository
import com.example.recorder.domain.transcription.CloudTranscriptionEngine
import com.example.recorder.domain.transcription.LocalTranscriptionEngine
import com.example.recorder.domain.transcription.TranscriptionSource
import com.example.recorder.domain.transcription.TranscriptionUpdate
import com.example.recorder.domain.transcription.TranscriptionMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepository,
    private val localTranscriptionEngine: LocalTranscriptionEngine,
    private val cloudTranscriptionEngine: CloudTranscriptionEngine,
    private val preferencesRepository: TranscriptionPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targetId = inputData.getLong(KEY_RECORDING_ID, -1L).takeIf { it != -1L }
        val requestedMode = inputData.getString(KEY_TRANSCRIPTION_MODE)
            ?.let { value -> runCatching { TranscriptionMode.valueOf(value) }.getOrNull() }
            ?: preferencesRepository.transcriptionMode.first()
        val targetRecordings = targetId?.let { id ->
            listOfNotNull(recordingRepository.getRecording(id))
        } ?: recordingRepository.getPendingTranscriptions()

        if (targetRecordings.isEmpty()) return Result.success()

        return runCatching {
            val engine = selectEngine(requestedMode)
            engine.ensureModel()
            targetRecordings.forEach { recording ->
                recordingRepository.updateTranscriptionStatus(recording.id, TranscriptionStatus.IN_PROGRESS)
                engine
                    .transcribe(recording.id, TranscriptionSource.FilePath(recording.filePath))
                    .collect { update ->
                        when (update) {
                            is TranscriptionUpdate.Completed -> recordingRepository.updateTranscriptionStatus(
                                recording.id,
                                TranscriptionStatus.COMPLETED,
                                update.text
                            )

                            is TranscriptionUpdate.Failed -> recordingRepository.updateTranscriptionStatus(
                                recording.id,
                                TranscriptionStatus.FAILED,
                                null
                            )

                            is TranscriptionUpdate.Progress -> Unit
                        }
                    }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    private suspend fun selectEngine(mode: TranscriptionMode) = when (mode) {
        TranscriptionMode.CLOUD -> {
            val apiKey = preferencesRepository.apiKey.first()
            if (apiKey.isNullOrBlank()) localTranscriptionEngine else cloudTranscriptionEngine
        }

        TranscriptionMode.LOCAL -> localTranscriptionEngine
    }

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
        const val KEY_TRANSCRIPTION_MODE = "transcription_mode"
    }
}
