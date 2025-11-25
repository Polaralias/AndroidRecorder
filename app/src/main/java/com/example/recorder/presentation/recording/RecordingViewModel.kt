package com.example.recorder.presentation.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorder.domain.model.RecordingSessionState
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.usecase.PauseRecordingUseCase
import com.example.recorder.domain.usecase.ResumeRecordingUseCase
import com.example.recorder.domain.usecase.StartRecordingUseCase
import com.example.recorder.domain.usecase.StopRecordingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val startRecording: StartRecordingUseCase,
    private val pauseRecording: PauseRecordingUseCase,
    private val resumeRecording: ResumeRecordingUseCase,
    private val stopRecording: StopRecordingUseCase,
    repository: RecordingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.sessionState.collectLatest { state ->
                _uiState.value = _uiState.value.copy(sessionState = state)
                when (state) {
                    is RecordingSessionState.Active -> startTimer(state.startTime.toEpochMilli())
                    RecordingSessionState.Idle, is RecordingSessionState.Error -> stopTimer()
                }
            }
        }
    }

    fun onStart() {
        viewModelScope.launch { startRecording() }
    }

    fun onPause() {
        viewModelScope.launch { pauseRecording() }
    }

    fun onResume() {
        viewModelScope.launch { resumeRecording() }
    }

    fun onStop() {
        viewModelScope.launch { stopRecording() }
    }

    private fun startTimer(startMillis: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(elapsedMillis = now - startMillis)
                delay(200)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(elapsedMillis = 0L)
    }
}
