package com.example.recorder.data.recording

import com.example.recorder.di.RecordingStateDispatcher
import com.example.recorder.di.RecordingStateNowProvider
import com.example.recorder.di.RecordingStateScope
import com.example.recorder.domain.model.RecordingSessionState
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class RecordingStateStore @Inject constructor(
    @RecordingStateScope private val scope: CoroutineScope,
    @RecordingStateDispatcher private val dispatcher: CoroutineDispatcher,
    @RecordingStateNowProvider private val nowProvider: () -> Long
) {
    private val _state = MutableStateFlow<RecordingSessionState>(RecordingSessionState.Idle)
    val state: StateFlow<RecordingSessionState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var baseElapsedMillis: Long = 0L
    private var trackingStartMillis: Long? = null

    fun startSession(startTime: Instant, filePath: String) {
        baseElapsedMillis = 0L
        trackingStartMillis = nowProvider()
        _state.value = RecordingSessionState.Active(
            startTime = startTime,
            filePath = filePath,
            isPaused = false,
            amplitude = 0,
            elapsedMillis = 0L
        )
        startTicker()
    }

    fun setAmplitude(amplitude: Int) {
        updateActiveState { current ->
            current.copy(amplitude = amplitude)
        }
    }

    fun setPaused() {
        val pausedAt = nowProvider()
        updateActiveState { current ->
            val elapsed = currentElapsed(pausedAt)
            baseElapsedMillis = elapsed
            trackingStartMillis = null
            stopTicker()
            current.copy(isPaused = true, elapsedMillis = elapsed)
        }
    }

    fun setResumed() {
        updateActiveState { current ->
            trackingStartMillis = nowProvider()
            startTicker()
            current.copy(isPaused = false)
        }
    }

    fun setError(message: String) {
        stopTicker()
        _state.value = RecordingSessionState.Error(message)
    }

    suspend fun stopSession() = withContext(dispatcher) {
        stopTicker()
        baseElapsedMillis = 0L
        trackingStartMillis = null
        _state.emit(RecordingSessionState.Idle)
    }

    private fun startTicker() {
        stopTicker()
        tickerJob = scope.launch(dispatcher) {
            while (isActive) {
                val elapsed = currentElapsed(nowProvider())
                updateActiveState { current ->
                    current.copy(elapsedMillis = elapsed)
                }
                delay(TICK_DELAY_MS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun currentElapsed(nowMillis: Long): Long {
        val runningElapsed = trackingStartMillis?.let { startMillis ->
            nowMillis - startMillis
        } ?: 0L
        return (baseElapsedMillis + runningElapsed).coerceAtLeast(0L)
    }

    private inline fun updateActiveState(update: (RecordingSessionState.Active) -> RecordingSessionState.Active) {
        val current = _state.value
        if (current is RecordingSessionState.Active) {
            _state.value = update(current)
        }
    }

    companion object {
        private const val TICK_DELAY_MS = 500L
    }
}
