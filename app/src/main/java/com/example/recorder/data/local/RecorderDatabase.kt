package com.example.recorder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.recorder.data.local.util.InstantConverter
import com.example.recorder.data.local.util.TranscriptionStatusConverter

@Database(
    entities = [RecordingEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(InstantConverter::class, TranscriptionStatusConverter::class)
abstract class RecorderDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
