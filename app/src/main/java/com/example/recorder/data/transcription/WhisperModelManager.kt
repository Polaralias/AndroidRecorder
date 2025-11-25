package com.example.recorder.data.transcription

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhisperModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDir: File = File(context.filesDir, "whisper/models").apply { mkdirs() }
    private val modelFile = File(modelDir, DEFAULT_MODEL_FILENAME)

    suspend fun ensureModel(): File = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) {
            downloadModel()
        }
        modelFile
    }

    suspend fun replaceModel(fromUrl: String = DEFAULT_MODEL_URL): File = withContext(Dispatchers.IO) {
        if (modelFile.exists()) {
            modelFile.delete()
        }
        downloadModel(fromUrl)
    }

    private suspend fun downloadModel(url: String = DEFAULT_MODEL_URL): File = withContext(Dispatchers.IO) {
        URL(url).openStream().use { input ->
            modelFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        modelFile
    }

    companion object {
        private const val DEFAULT_MODEL_FILENAME = "ggml-tiny.bin"
        const val DEFAULT_MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
    }
}
