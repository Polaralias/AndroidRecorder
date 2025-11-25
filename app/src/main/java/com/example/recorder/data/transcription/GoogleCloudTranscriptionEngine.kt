package com.example.recorder.data.transcription

import android.content.Context
import com.example.recorder.data.transcription.googlecloud.GoogleSpeechClient
import com.example.recorder.domain.repository.TranscriptionPreferencesRepository
import com.example.recorder.domain.transcription.CloudTranscriptionEngine
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GoogleCloudTranscriptionEngine @Inject constructor(
    private val speechClient: GoogleSpeechClient,
    private val credentialsRepository: TranscriptionPreferencesRepository,
    @ApplicationContext private val context: Context
) : CloudTranscriptionEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runningJobs = mutableMapOf<Long, kotlinx.coroutines.Job>()

    override fun transcribe(
        recordingId: Long,
        source: TranscriptionSource
    ): Flow<TranscriptionUpdate> = channelFlow {
        val job = scope.launch {
            runCatching {
                val apiKey = credentialsRepository.apiKey.firstOrNull()
                if (apiKey.isNullOrBlank()) {
                    throw IllegalStateException("Google Cloud API key is missing")
                }

                send(TranscriptionUpdate.Progress(TranscriptionProgress(0, "Preparing audio")))
                val audioFile = materializeAudio(source)
                send(TranscriptionUpdate.Progress(TranscriptionProgress(20, "Uploading to Google Cloud")))
                val transcript = speechClient.transcribe(apiKey, audioFile)
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
        awaitClose { runningJobs.remove(recordingId)?.cancel() }
    }

    override suspend fun cancel(recordingId: Long) {
        runningJobs.remove(recordingId)?.cancel()
    }

    override suspend fun ensureModel() {
        // No-op for cloud transcription
    }

    private suspend fun materializeAudio(source: TranscriptionSource): File = withContext(Dispatchers.IO) {
        when (source) {
            is TranscriptionSource.FilePath -> File(source.path)
            is TranscriptionSource.Stream -> writeTempAudio(source.open)
        }
    }

    private suspend fun writeTempAudio(open: suspend () -> InputStream): File = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("cloud_transcription", ".wav", context.cacheDir)
        open().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    }
}
