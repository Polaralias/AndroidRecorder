package com.example.recorder.recording.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.recorder.R
import com.example.recorder.MainActivity
import com.example.recorder.data.recording.RecordingStateStore
import com.example.recorder.domain.model.RecordingSessionState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class RecorderService : Service() {

    @Inject lateinit var stateStore: RecordingStateStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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
            ACTION_SYNC -> pushCurrentState()
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val startedAtMillis = intent.getLongExtra(EXTRA_START_TIME, System.currentTimeMillis())
        if (filePath.isNullOrEmpty()) {
            reportError(getString(R.string.error_missing_output_path))
            stopSelf()
            return
        }

        outputFile = File(filePath)
        startTime = Instant.ofEpochMilli(startedAtMillis)
        startForeground(NOTIFICATION_ID, buildNotification(0L, isPaused = false))
        val started = startRecorder(outputFile!!)
        if (started) {
            stateStore.startSession(startTime!!, filePath)
            beginAmplitudeUpdates()
        }
    }

    private fun handleStop() {
        amplitudeJob?.cancel()
        runCatching { mediaRecorder?.stop() }
        mediaRecorder?.reset()
        mediaRecorder?.release()
        mediaRecorder = null
        serviceScope.launch { stateStore.stopSession() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun handlePause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { mediaRecorder?.pause() }
            stateStore.setPaused()
            updateNotification()
        }
    }

    private fun handleResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { mediaRecorder?.resume() }
            stateStore.setResumed()
            updateNotification()
        }
    }

    private fun pushCurrentState() {
        val current = stateStore.state.value
        if (current is RecordingSessionState.Active) {
            stateStore.setAmplitude(current.amplitude)
            updateNotification()
        } else if (mediaRecorder != null && startTime != null && outputFile != null) {
            stateStore.startSession(startTime!!, outputFile!!.absolutePath)
            updateNotification()
        }
    }

    private fun startRecorder(file: File): Boolean {
        return runCatching {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            true
        }.getOrElse { throwable ->
            when (throwable) {
                is SecurityException -> reportError(getString(R.string.error_microphone_permission))
                is IOException -> reportError(getString(R.string.error_unable_to_start_recording))
                else -> reportError(throwable.message ?: getString(R.string.error_unable_to_start_recording))
            }
            mediaRecorder?.release()
            mediaRecorder = null
            false
        }
    }

    private fun beginAmplitudeUpdates() {
        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (true) {
                val amplitude = max(mediaRecorder?.maxAmplitude ?: 0, 0)
                stateStore.setAmplitude(amplitude)
                updateNotification()
                delay(500)
            }
        }
    }

    private fun buildNotification(elapsed: Long, isPaused: Boolean): Notification {
        createChannel()
        val contentText = if (isPaused) {
            getString(R.string.notification_paused_body)
        } else {
            getString(R.string.notification_recording_body, formatElapsed(elapsed))
        }
        val pauseResumeAction = if (isPaused) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                getString(R.string.notification_action_resume),
                servicePendingIntent(ACTION_RESUME)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                getString(R.string.notification_action_pause),
                servicePendingIntent(ACTION_PAUSE)
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_recorder)
            .setContentIntent(activityPendingIntent())
            .addAction(pauseResumeAction)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.notification_action_stop),
                    servicePendingIntent(ACTION_STOP)
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification() {
        val state = stateStore.state.value
        if (state is RecordingSessionState.Active) {
            val notification = buildNotification(state.elapsedMillis, state.isPaused)
            startForeground(NOTIFICATION_ID, notification)
        }
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

    private fun reportError(message: String) {
        stateStore.setError(message)
        updateNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun activityPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        REQUEST_CODE_LAUNCH,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun servicePendingIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, RecorderService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    override fun onDestroy() {
        amplitudeJob?.cancel()
        serviceScope.cancel()
        if (stateStore.state.value is RecordingSessionState.Active) {
            runBlocking { stateStore.stopSession() }
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.recorder.action.START_RECORDING"
        const val ACTION_STOP = "com.example.recorder.action.STOP_RECORDING"
        const val ACTION_PAUSE = "com.example.recorder.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.example.recorder.action.RESUME_RECORDING"
        const val ACTION_SYNC = "com.example.recorder.action.SYNC_RECORDING"
        const val EXTRA_FILE_PATH = "extra.FILE_PATH"
        const val EXTRA_START_TIME = "extra.START_TIME"

        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_LAUNCH = 1101

        private fun formatElapsed(elapsedMillis: Long): String {
            val totalSeconds = elapsedMillis / 1_000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3_600
            return "%02d:%02d:%02d".format(hours, minutes, seconds)
        }
    }
}
