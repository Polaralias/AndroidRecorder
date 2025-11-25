package com.example.recorder.domain.usecase

import com.example.recorder.domain.model.RecordingSessionState
import com.example.recorder.domain.repository.RecordingRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ToggleRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase
) {
    suspend operator fun invoke(): RecordingSessionState {
        val current = repository.sessionState.first()
        return if (current is RecordingSessionState.Active) {
            stopRecordingUseCase()
            repository.sessionState.first()
        } else {
            startRecordingUseCase()
        }
    }
}
