package com.example.recorder.domain.model

import java.time.Instant

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object InProgress : BackupStatus
    data class Failed(val message: String) : BackupStatus
    data class Completed(val timestamp: Instant, val uploadedCount: Int) : BackupStatus
}

/**
 * User configurable backup settings surfaced to the UI.
 */
data class BackupSettings(
    val autoBackupEnabled: Boolean = false,
    val lastBackupAttempt: Instant? = null,
    val lastError: String? = null
)

sealed interface BackupResult {
    data class Success(val driveFileId: String) : BackupResult
    data class Skipped(val reason: String) : BackupResult
    data class Retrying(val attempt: Int) : BackupResult
    data class Failure(val throwable: Throwable) : BackupResult
}
