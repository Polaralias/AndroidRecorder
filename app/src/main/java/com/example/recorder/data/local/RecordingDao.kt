package com.example.recorder.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.recorder.domain.model.TranscriptionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY created_at DESC")
    fun observeRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observeRecording(id: Long): Flow<RecordingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(recording: RecordingEntity): Long

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecording(id: Long): RecordingEntity?

    @Query("SELECT is_backed_up FROM recordings WHERE id = :id")
    suspend fun isBackedUp(id: Long): Boolean?

    @Query("SELECT * FROM recordings WHERE is_backed_up = 0")
    suspend fun getPendingBackups(): List<RecordingEntity>

    @Query(
        "UPDATE recordings SET is_backed_up = :isBackedUp, drive_file_id = :driveFileId, last_backup_attempt = :lastBackupAttempt WHERE id = :id"
    )
    suspend fun updateBackupState(
        id: Long,
        isBackedUp: Boolean,
        driveFileId: String?,
        lastBackupAttempt: Long?
    )

    @Query(
        "UPDATE recordings SET transcription_status = :status, transcription_text = :text, transcription_updated_at = :updatedAt WHERE id = :id"
    )
    suspend fun updateTranscription(
        id: Long,
        status: TranscriptionStatus,
        text: String?,
        updatedAt: Long?
    )

    @Query("SELECT * FROM recordings WHERE transcription_status IN ('NOT_STARTED', 'FAILED')")
    suspend fun getPendingTranscriptions(): List<RecordingEntity>
}
