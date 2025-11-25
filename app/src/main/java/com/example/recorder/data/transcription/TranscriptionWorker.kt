package com.example.recorder.data.transcription

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.recorder.domain.model.TranscriptionStatus
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.transcription.LocalTranscriptionEngine
import com.example.recorder.domain.transcription.TranscriptionSource
import com.example.recorder.domain.transcription.TranscriptionUpdate
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepository,
    private val transcriptionEngine: LocalTranscriptionEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val targetId = inputData.getLong(KEY_RECORDING_ID, -1L).takeIf { it != -1L }
        val targetRecordings = targetId?.let { id ->
            listOfNotNull(recordingRepository.getRecording(id))
        } ?: recordingRepository.getPendingTranscriptions()

        if (targetRecordings.isEmpty()) return Result.success()

        return runCatching {
            transcriptionEngine.ensureModel()
            targetRecordings.forEach { recording ->
                recordingRepository.updateTranscriptionStatus(recording.id, TranscriptionStatus.IN_PROGRESS)
                transcriptionEngine
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

    companion object {
        const val KEY_RECORDING_ID = "recording_id"
    }
}
