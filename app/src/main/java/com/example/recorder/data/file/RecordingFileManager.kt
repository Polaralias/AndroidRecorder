package com.example.recorder.data.file

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingFileManager @Inject constructor(
    private val context: Context
) {
    fun createFile(timestamp: Instant = Instant.now()): File {
        val directory = File(context.filesDir, "recordings").apply { mkdirs() }
        val fileName = "REC_${DateTimeFormatter.ISO_INSTANT.format(timestamp)}.m4a"
        return File(directory, fileName.replace(":", "-")).absoluteFile
    }
}
