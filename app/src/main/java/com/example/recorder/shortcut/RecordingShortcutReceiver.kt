package com.example.recorder.shortcut

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.recorder.domain.usecase.StartRecordingUseCase
import com.example.recorder.domain.usecase.StopRecordingUseCase
import com.example.recorder.domain.usecase.ToggleRecordingUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecordingShortcutReceiver : BroadcastReceiver() {

    @Inject lateinit var toggleRecording: ToggleRecordingUseCase
    @Inject lateinit var startRecording: StartRecordingUseCase
    @Inject lateinit var stopRecording: StopRecordingUseCase

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                when (intent?.action) {
                    ACTION_TOGGLE -> toggleRecording()
                    ACTION_START -> startRecording()
                    ACTION_STOP -> stopAndClear()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun stopAndClear() {
        try {
            stopRecording()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.recorder.action.TOGGLE_RECORDING"
        const val ACTION_START = "com.example.recorder.action.START_SHORTCUT_RECORDING"
        const val ACTION_STOP = "com.example.recorder.action.STOP_SHORTCUT_RECORDING"

        fun toggleIntent(context: Context): Intent = Intent(context, RecordingShortcutReceiver::class.java)
            .setAction(ACTION_TOGGLE)

        fun startIntent(context: Context): Intent = Intent(context, RecordingShortcutReceiver::class.java)
            .setAction(ACTION_START)

        fun stopIntent(context: Context): Intent = Intent(context, RecordingShortcutReceiver::class.java)
            .setAction(ACTION_STOP)
    }
}
