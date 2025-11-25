package com.example.recorder.presentation.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.data.drive.DriveAuthManager
import com.example.recorder.domain.model.BackupSettings
import com.example.recorder.domain.model.BackupStatus
import com.example.recorder.domain.repository.DriveBackupRepository
import com.example.recorder.domain.repository.TranscriptionPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val driveBackupRepository: DriveBackupRepository,
    private val authManager: DriveAuthManager,
    private val transcriptionPreferencesRepository: TranscriptionPreferencesRepository
) : ViewModel() {

    private val _accountEmail = MutableStateFlow(authManager.lastSignedInAccount()?.email)
    private val _apiKeyInput = MutableStateFlow("")
    private val _keyTestMessage = MutableStateFlow("")
    private val _isTestingKey = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        driveBackupRepository.backupSettings,
        driveBackupRepository.backupStatus,
        _accountEmail,
        transcriptionPreferencesRepository.apiKey,
        _apiKeyInput,
        _keyTestMessage,
        _isTestingKey
    ) { settings, status, accountEmail, storedApiKey, apiKeyInput, keyTestMessage, isTestingKey ->
        val resolvedInput = apiKeyInput.ifBlank { storedApiKey.orEmpty() }
        SettingsUiState(
            autoBackupEnabled = settings.autoBackupEnabled,
            accountEmail = accountEmail,
            lastBackupLabel = settings.lastBackupAttempt?.let { lastAttempt ->
                "Last backup: ${formatRelativeTime(lastAttempt)}"
            } ?: "Backup has not run yet",
            statusMessage = status.toMessage(settings),
            isBackingUp = status is BackupStatus.InProgress,
            apiKeyInput = resolvedInput,
            hasStoredApiKey = !storedApiKey.isNullOrBlank(),
            keyTestMessage = keyTestMessage,
            isTestingKey = isTestingKey
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    val signInIntent get() = authManager.signInClient().signInIntent

    fun onAutoBackupChanged(enabled: Boolean) {
        viewModelScope.launch { driveBackupRepository.setAutoBackupEnabled(enabled) }
    }

    fun onBackupAll() {
        viewModelScope.launch { driveBackupRepository.backupPendingRecordings() }
    }

    fun onSignInResult(intent: Intent?) {
        viewModelScope.launch {
            val account = runCatching { authManager.getAccountFromIntent(intent) }.getOrNull()
            _accountEmail.value = account?.email
        }
    }

    fun onApiKeyChanged(value: String) {
        _apiKeyInput.value = value
    }

    fun onSaveApiKey() {
        viewModelScope.launch {
            if (_apiKeyInput.value.isBlank()) {
                _keyTestMessage.value = "Enter an API key first"
                return@launch
            }
            transcriptionPreferencesRepository.updateApiKey(_apiKeyInput.value)
            _keyTestMessage.value = "API key saved securely"
        }
    }

    fun onClearApiKey() {
        viewModelScope.launch {
            transcriptionPreferencesRepository.clearApiKey()
            _apiKeyInput.value = ""
            _keyTestMessage.value = "API key removed"
        }
    }

    fun onTestApiKey() {
        viewModelScope.launch {
            if (_apiKeyInput.value.isBlank()) {
                _keyTestMessage.value = "Enter an API key to test"
                return@launch
            }
            _isTestingKey.value = true
            val result = transcriptionPreferencesRepository.testApiKey(_apiKeyInput.value)
            _keyTestMessage.value = result.message
            if (result.success) {
                transcriptionPreferencesRepository.updateApiKey(_apiKeyInput.value)
            }
            _isTestingKey.value = false
        }
    }

    private fun BackupStatus.toMessage(settings: BackupSettings): String = when (this) {
        BackupStatus.Idle -> settings.lastBackupAttempt?.let { last ->
            "Last backup: ${formatRelativeTime(last)}"
        } ?: "Waiting to start backups"

        BackupStatus.InProgress -> "Backing up recordings…"
        is BackupStatus.Completed -> "Backed up $uploadedCount recordings"
        is BackupStatus.Failed -> "Some recordings failed to back up, tap to retry: ${message}"
    }

    private fun formatRelativeTime(instant: Instant): String {
        val now = Instant.now()
        val duration = Duration.between(instant, now)
        val hours = duration.toHours()
        return when {
            hours > 24 -> DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)

            hours >= 1 -> "${hours}h ago"
            duration.toMinutes() >= 1 -> "${duration.toMinutes()}m ago"
            else -> "Just now"
        }
    }
}

data class SettingsUiState(
    val autoBackupEnabled: Boolean = false,
    val accountEmail: String? = null,
    val lastBackupLabel: String = "Backup has not run yet",
    val statusMessage: String = "",
    val isBackingUp: Boolean = false,
    val apiKeyInput: String = "",
    val hasStoredApiKey: Boolean = false,
    val keyTestMessage: String = "",
    val isTestingKey: Boolean = false
)
