package com.example.recorder.domain.usecase

import com.example.recorder.domain.model.RecordingSessionState
import com.example.recorder.domain.repository.RecordingRepository
import javax.inject.Inject

class StartRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(): RecordingSessionState = repository.startRecording()
}
