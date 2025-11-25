package com.example.recorder.data.recording

import android.content.Context
import android.content.Intent
import com.example.recorder.recording.service.ForegroundRecorderService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun startRecording(filePath: String) {
        context.startForegroundService(
            Intent(context, ForegroundRecorderService::class.java)
                .setAction(ForegroundRecorderService.ACTION_START)
                .putExtra(ForegroundRecorderService.EXTRA_FILE_PATH, filePath)
        )
    }

    fun stopRecording() {
        context.startService(
            Intent(context, ForegroundRecorderService::class.java)
                .setAction(ForegroundRecorderService.ACTION_STOP)
        )
    }

    fun pauseRecording() {
        context.startService(
            Intent(context, ForegroundRecorderService::class.java)
                .setAction(ForegroundRecorderService.ACTION_PAUSE)
        )
    }

    fun resumeRecording() {
        context.startService(
            Intent(context, ForegroundRecorderService::class.java)
                .setAction(ForegroundRecorderService.ACTION_RESUME)
        )
    }
}
