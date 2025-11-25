package com.example.recorder

import com.example.recorder.data.local.RecordingEntity
import com.example.recorder.data.local.toDomain
import com.example.recorder.data.local.toEntity
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.TranscriptionStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingEntityTest {
    @Test
    fun `entity round trip preserves fields`() {
        val domain = Recording(
            id = 1L,
            title = "Test",
            filePath = "/tmp/file.m4a",
            createdAt = Instant.ofEpochMilli(1_000L),
            durationMillis = 5000L,
            transcriptionStatus = TranscriptionStatus.IN_PROGRESS,
            isBackedUp = true,
            driveFileId = "drive123",
            lastBackupAttempt = Instant.ofEpochMilli(2_000L)
        )

        val entity = domain.toEntity()
        val roundTrip = entity.toDomain()

        assertEquals(domain, roundTrip)
        assertEquals(domain.id, entity.id)
        assertEquals(domain.title, entity.title)
        assertEquals(domain.filePath, entity.filePath)
        assertEquals(domain.createdAt.toEpochMilli(), entity.createdAt)
        assertEquals(domain.driveFileId, entity.driveFileId)
        assertEquals(domain.lastBackupAttempt?.toEpochMilli(), entity.lastBackupAttempt)
    }
}
