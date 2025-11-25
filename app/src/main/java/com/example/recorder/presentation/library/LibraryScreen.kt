package com.example.recorder.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recorder.domain.model.Recording
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LibraryRoute(viewModel: LibraryViewModel) {
    val recordings by viewModel.recordings.collectAsState()
    LibraryScreen(recordings = recordings)
}

@Composable
fun LibraryScreen(recordings: List<Recording>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Library", style = MaterialTheme.typography.titleLarge)
        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(recordings) { recording ->
                RecordingRow(recording)
            }
        }
    }
}

@Composable
private fun RecordingRow(recording: Recording) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    val created = recording.createdAt.atZone(ZoneId.systemDefault()).format(formatter)
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = recording.title.ifBlank { created }, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Duration: ${recording.durationMillis / 1000}s · $created",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
