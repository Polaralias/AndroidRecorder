package com.example.recorder.data.transcription

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.recorder.domain.transcription.TranscriptionMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    fun enqueue(recordingId: Long? = null, mode: TranscriptionMode = TranscriptionMode.LOCAL) {
        val constraintsBuilder = Constraints.Builder()
            .setRequiresBatteryNotLow(true)

        if (mode == TranscriptionMode.CLOUD) {
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
        } else {
            constraintsBuilder
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        }

        val constraints = constraintsBuilder.build()

        val dataBuilder = Data.Builder()
            .putString(TranscriptionWorker.KEY_TRANSCRIPTION_MODE, mode.name)
        if (recordingId != null) {
            dataBuilder.putLong(TranscriptionWorker.KEY_RECORDING_ID, recordingId)
        }
        val data = dataBuilder.build()

        val requestBuilder = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setConstraints(constraints)

        requestBuilder.setInputData(data)

        val request = requestBuilder.build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        private const val WORK_NAME = "transcription_work"
    }
}
