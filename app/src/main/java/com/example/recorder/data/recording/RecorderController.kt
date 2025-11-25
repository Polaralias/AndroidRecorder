package com.example.recorder.data.recording

import android.content.Context
import android.content.Intent
import com.example.recorder.recording.service.RecorderService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecorderController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun startRecording(filePath: String, startedAtMillis: Long) {
        context.startForegroundService(
            Intent(context, RecorderService::class.java)
                .setAction(RecorderService.ACTION_START)
                .putExtra(RecorderService.EXTRA_FILE_PATH, filePath)
                .putExtra(RecorderService.EXTRA_START_TIME, startedAtMillis)
        )
    }

    fun stopRecording() {
        context.startService(
            Intent(context, RecorderService::class.java)
                .setAction(RecorderService.ACTION_STOP)
        )
    }

    fun pauseRecording() {
        context.startService(
            Intent(context, RecorderService::class.java)
                .setAction(RecorderService.ACTION_PAUSE)
        )
    }

    fun resumeRecording() {
        context.startService(
            Intent(context, RecorderService::class.java)
                .setAction(RecorderService.ACTION_RESUME)
        )
    }

    fun requestStateSync() {
        context.startService(
            Intent(context, RecorderService::class.java).setAction(RecorderService.ACTION_SYNC)
        )
    }
}
