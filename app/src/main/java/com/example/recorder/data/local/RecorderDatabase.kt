package com.example.recorder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.recorder.data.local.util.InstantConverter
import com.example.recorder.data.local.util.TranscriptionStatusConverter

@Database(
    entities = [RecordingEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(InstantConverter::class, TranscriptionStatusConverter::class)
abstract class RecorderDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recordings ADD COLUMN drive_file_id TEXT")
        db.execSQL("ALTER TABLE recordings ADD COLUMN last_backup_attempt INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recordings ADD COLUMN transcription_text TEXT")
        db.execSQL("ALTER TABLE recordings ADD COLUMN transcription_updated_at INTEGER")
    }
}
