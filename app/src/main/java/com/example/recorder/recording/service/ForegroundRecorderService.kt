package com.example.recorder.recording.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.recorder.R
import com.example.recorder.data.recording.RecordingStateStore
import com.example.recorder.domain.model.RecordingSessionState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForegroundRecorderService : Service() {

    @Inject lateinit var stateStore: RecordingStateStore

    private val scope = CoroutineScope(Dispatchers.Main)
    private var amplitudeJob: Job? = null
    private var mediaRecorder: MediaRecorder? = null
    private var startTime: Instant? = null
    private var outputFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_STOP -> handleStop()
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: return
        outputFile = File(filePath)
        startForeground(NOTIFICATION_ID, buildNotification())
        startTime = Instant.now()
        startRecorder(outputFile!!)
        beginAmplitudeUpdates()
    }

    private fun handleStop() {
        amplitudeJob?.cancel()
        mediaRecorder?.apply {
            runCatching { stop() }
            reset()
            release()
        }
        mediaRecorder = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        stateStore.update(RecordingSessionState.Idle)
    }

    private fun handlePause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
        }
    }

    private fun handleResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
        }
    }

    private fun startRecorder(file: File) {
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
            } catch (ioe: IOException) {
                stateStore.update(RecordingSessionState.Error(ioe.message ?: "Recorder error"))
                stopSelf()
            }
        }
    }

    private fun beginAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (true) {
                val amplitude = mediaRecorder?.maxAmplitude ?: 0
                val current = stateStore.state.value
                if (current is RecordingSessionState.Active) {
                    stateStore.update(current.copy(amplitude = amplitude))
                }
                delay(200)
            }
        }
    }

    private fun buildNotification(): Notification {
        createChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(getString(R.string.notification_recording_body))
            .setSmallIcon(R.drawable.ic_recorder)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_recording),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.example.recorder.action.START_RECORDING"
        const val ACTION_STOP = "com.example.recorder.action.STOP_RECORDING"
        const val ACTION_PAUSE = "com.example.recorder.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.example.recorder.action.RESUME_RECORDING"
        const val EXTRA_FILE_PATH = "extra.FILE_PATH"

        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1001
    }
}
