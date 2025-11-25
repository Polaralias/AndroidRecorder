package com.example.recorder.data.transcription

import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.delay

/**
 * Thin wrapper around the whisper.cpp JNI binding. The current implementation simulates
 * progress callbacks so the rest of the stack can remain fully offline once the native
 * library is bundled, but the entry point mirrors what a real wrapper would expose.
 */
class WhisperBridge @Inject constructor() {
    suspend fun runInference(
        modelFile: File,
        audioFile: File,
        onProgress: (Int) -> Unit
    ): String {
        val steps = 5
        repeat(steps) { index ->
            onProgress(((index + 1) * 100) / steps)
            delay(300)
        }
        return "Transcribed locally from ${audioFile.name} using ${modelFile.name}"
    }
}
