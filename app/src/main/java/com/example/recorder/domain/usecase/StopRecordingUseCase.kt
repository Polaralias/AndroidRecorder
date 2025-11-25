package com.example.recorder.domain.usecase

import com.example.recorder.domain.model.Recording
import com.example.recorder.domain.repository.RecordingRepository
import javax.inject.Inject

class StopRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(): Recording? = repository.stopRecording()
}
