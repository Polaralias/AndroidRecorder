package com.example.recorder.presentation.recordingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.data.transcription.TranscriptionScheduler
import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.model.TranscriptionStatus
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.repository.TranscriptionPreferencesRepository
import com.example.recorder.domain.transcription.TranscriptionMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecordingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecordingRepository,
    private val transcriptionScheduler: TranscriptionScheduler,
    private val transcriptionPreferencesRepository: TranscriptionPreferencesRepository
) : ViewModel() {

    private val recordingId: Long = checkNotNull(savedStateHandle[STATE_KEY_RECORDING_ID])

    val uiState: StateFlow<RecordingDetailUiState> = combine(
        repository.observeRecording(recordingId),
        transcriptionPreferencesRepository.transcriptionMode,
        transcriptionPreferencesRepository.apiKey
    ) { recording, mode, apiKey ->
        RecordingDetailUiState(
            recording = recording,
            mode = mode,
            hasCloudKey = !apiKey.isNullOrBlank()
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecordingDetailUiState()
        )

    fun requestTranscription() {
        updateStatus(TranscriptionStatus.IN_PROGRESS)
        transcriptionScheduler.enqueue(recordingId, uiState.value.mode)
    }

    fun retryTranscription() {
        updateStatus(TranscriptionStatus.IN_PROGRESS)
        transcriptionScheduler.enqueue(recordingId, uiState.value.mode)
    }

    fun onModeSelected(mode: TranscriptionMode) {
        viewModelScope.launch {
            transcriptionPreferencesRepository.setMode(mode)
        }
    }

    private fun updateStatus(status: TranscriptionStatus) {
        viewModelScope.launch {
            repository.updateTranscriptionStatus(recordingId, status)
        }
    }

    companion object {
        const val STATE_KEY_RECORDING_ID = "recordingId"
    }
}

data class RecordingDetailUiState(
    val recording: Recording? = null,
    val mode: TranscriptionMode = TranscriptionMode.LOCAL,
    val hasCloudKey: Boolean = false
)
