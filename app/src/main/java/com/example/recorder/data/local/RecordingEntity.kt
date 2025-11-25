package com.example.recorder.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.TranscriptionStatus
import java.time.Instant

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    @ColumnInfo(name = "transcription_status") val transcriptionStatus: TranscriptionStatus,
    @ColumnInfo(name = "transcription_text") val transcriptionText: String?,
    @ColumnInfo(name = "transcription_updated_at") val transcriptionUpdatedAt: Long?,
    @ColumnInfo(name = "is_backed_up") val isBackedUp: Boolean,
    @ColumnInfo(name = "drive_file_id") val driveFileId: String?,
    @ColumnInfo(name = "last_backup_attempt") val lastBackupAttempt: Long?
)

fun RecordingEntity.toDomain(): Recording = Recording(
    id = id,
    title = title,
    filePath = filePath,
    createdAt = Instant.ofEpochMilli(createdAt),
    durationMillis = durationMillis,
    transcriptionStatus = transcriptionStatus,
    transcriptionText = transcriptionText,
    transcriptionUpdatedAt = transcriptionUpdatedAt?.let { Instant.ofEpochMilli(it) },
    isBackedUp = isBackedUp,
    driveFileId = driveFileId,
    lastBackupAttempt = lastBackupAttempt?.let { Instant.ofEpochMilli(it) }
)

fun Recording.toEntity(): RecordingEntity = RecordingEntity(
    id = id,
    title = title,
    filePath = filePath,
    createdAt = createdAt.toEpochMilli(),
    durationMillis = durationMillis,
    transcriptionStatus = transcriptionStatus,
    transcriptionText = transcriptionText,
    transcriptionUpdatedAt = transcriptionUpdatedAt?.toEpochMilli(),
    isBackedUp = isBackedUp,
    driveFileId = driveFileId,
    lastBackupAttempt = lastBackupAttempt?.toEpochMilli()
)
