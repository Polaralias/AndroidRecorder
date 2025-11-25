package com.example.recorder.presentation.recording

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recorder.domain.model.RecordingSessionState
import kotlin.math.roundToInt

@Composable
fun RecordingRoute(viewModel: RecordingViewModel) {
    val state by viewModel.uiState.collectAsState()
    RecordingScreen(
        state = state,
        onStart = viewModel::onStart,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onStop = viewModel::onStop
    )
}

@Composable
fun RecordingScreen(
    state: RecordingUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Recording",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formatMillis(state.elapsedMillis),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = 16.dp)
        )
        if (state.sessionState is RecordingSessionState.Error) {
            Text(
                text = (state.sessionState as RecordingSessionState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        LevelIndicator(state)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            when (val session = state.sessionState) {
                RecordingSessionState.Idle -> Button(onClick = onStart) { Text("Start") }
                is RecordingSessionState.Active -> {
                    IconButton(onClick = { if (session.isPaused) onResume() else onPause() }) {
                        Crossfade(targetState = session.isPaused, label = "playpause") { paused ->
                            Icon(
                                imageVector = if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null
                            )
                        }
                    }
                    IconButton(onClick = onStop) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                    }
                }
                is RecordingSessionState.Error -> Button(onClick = onStart) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun LevelIndicator(state: RecordingUiState) {
    val amplitude = when (val session = state.sessionState) {
        is RecordingSessionState.Active -> session.amplitude
        else -> 0
    }.coerceIn(0, 32767)
    val percent = amplitude / 32767f
    val height = (percent * 120).coerceAtLeast(8f)
    Row(
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .height(height.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                .padding(horizontal = 40.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Level ${(percent * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
