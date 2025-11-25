package com.example.recorder.data.transcription

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    fun enqueue(recordingId: Long? = null) {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val data = recordingId?.let {
            Data.Builder()
                .putLong(TranscriptionWorker.KEY_RECORDING_ID, it)
                .build()
        }

        val requestBuilder = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setConstraints(constraints)

        data?.let { requestBuilder.setInputData(it) }

        val request = requestBuilder.build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        private const val WORK_NAME = "local_transcription_work"
    }
}
