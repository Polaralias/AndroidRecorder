package com.example.recorder.data.transcription

import android.content.Context
import com.example.recorder.domain.transcription.LocalTranscriptionEngine
import com.example.recorder.domain.transcription.TranscriptionProgress
import com.example.recorder.domain.transcription.TranscriptionSource
import com.example.recorder.domain.transcription.TranscriptionUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhisperTranscriptionEngine @Inject constructor(
    private val modelManager: WhisperModelManager,
    private val bridge: WhisperBridge,
    @ApplicationContext private val context: Context
) : LocalTranscriptionEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningJobs = mutableMapOf<Long, kotlinx.coroutines.Job>()

    override fun transcribe(
        recordingId: Long,
        source: TranscriptionSource
    ): Flow<TranscriptionUpdate> = channelFlow {
        val job = scope.launch {
            runCatching {
                send(
                    TranscriptionUpdate.Progress(
                        TranscriptionProgress(0, "Preparing Whisper model")
                    )
                )
                val modelFile = modelManager.ensureModel()
                val audioFile = materializeAudio(source)
                send(
                    TranscriptionUpdate.Progress(
                        TranscriptionProgress(10, "Running local inference")
                    )
                )
                val transcript = bridge.runInference(modelFile, audioFile) { percent ->
                    trySend(
                        TranscriptionUpdate.Progress(
                            TranscriptionProgress(percent, "Processing frames")
                        )
                    )
                }
                TranscriptionUpdate.Completed(transcript)
            }.onSuccess { update ->
                send(update)
            }.onFailure { throwable ->
                send(TranscriptionUpdate.Failed(throwable))
            }
        }
        runningJobs[recordingId] = job
        job.invokeOnCompletion { cause ->
            if (cause == null) {
                close()
            } else {
                close(cause)
            }
        }
        awaitClose {
            runningJobs.remove(recordingId)?.cancel()
        }
    }

    override suspend fun cancel(recordingId: Long) {
        runningJobs.remove(recordingId)?.cancel()
    }

    override suspend fun ensureModel() {
        modelManager.ensureModel()
    }

    private suspend fun materializeAudio(source: TranscriptionSource): File = withContext(Dispatchers.IO) {
        when (source) {
            is TranscriptionSource.FilePath -> File(source.path)
            is TranscriptionSource.Stream -> writeTempAudio(source.open)
        }
    }

    private suspend fun writeTempAudio(open: suspend () -> InputStream): File = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("transcription", ".wav", context.cacheDir)
        open().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }
}
