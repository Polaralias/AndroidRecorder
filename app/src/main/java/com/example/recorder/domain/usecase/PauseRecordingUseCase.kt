package com.example.recorder.domain.usecase

import com.example.recorder.domain.repository.RecordingRepository
import javax.inject.Inject

class PauseRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke() = repository.pauseRecording()
}
