package com.example.recorder.presentation.recordingdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recorder.domain.model.TranscriptionStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecordingDetailRoute(viewModel: RecordingDetailViewModel) {
    val state by viewModel.uiState.collectAsState()
    RecordingDetailScreen(
        state = state,
        onTranscribe = viewModel::requestTranscription,
        onRetry = viewModel::retryTranscription
    )
}

@Composable
fun RecordingDetailScreen(
    state: RecordingDetailUiState,
    onTranscribe: () -> Unit,
    onRetry: () -> Unit
) {
    val recording = state.recording
    if (recording == null) {
        Text(
            modifier = Modifier.padding(24.dp),
            text = "Recording not found",
            style = MaterialTheme.typography.bodyLarge
        )
        return
    }

    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    val createdAt = recording.createdAt.atZone(ZoneId.systemDefault()).format(formatter)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(recording.title.ifBlank { "Recording #${recording.id}" }, style = MaterialTheme.typography.titleLarge)
        Text(
            "Recorded $createdAt · ${recording.durationMillis / 1000}s",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Transcription", style = MaterialTheme.typography.titleMedium)
            when (recording.transcriptionStatus) {
                TranscriptionStatus.COMPLETED -> {
                    Text(
                        text = recording.transcriptionText.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                TranscriptionStatus.IN_PROGRESS -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Transcription in progress...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                TranscriptionStatus.FAILED -> {
                    Text(
                        text = "Transcription failed. You can retry when the device is charging.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetry) {
                        Text("Retry transcription")
                    }
                }

                TranscriptionStatus.NOT_STARTED -> {
                    Button(onClick = onTranscribe) {
                        Text("Transcribe locally")
                    }
                    Text(
                        text = "Runs fully offline using the bundled Whisper model.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (recording.transcriptionUpdatedAt != null && recording.transcriptionStatus == TranscriptionStatus.COMPLETED) {
            val updated = recording.transcriptionUpdatedAt.atZone(ZoneId.systemDefault()).format(formatter)
            Text(
                text = "Updated at $updated" ,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
