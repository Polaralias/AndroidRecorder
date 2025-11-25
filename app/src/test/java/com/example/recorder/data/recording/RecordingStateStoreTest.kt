package com.example.recorder.data.recording

import com.example.recorder.domain.model.RecordingSessionState
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingStateStoreTest {

    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val nowProvider: () -> Long = { dispatcher.scheduler.currentTime }

    @Test
    fun `elapsed time increases while recording`() = runTest(dispatcher) {
        val store = RecordingStateStore(scope, dispatcher, nowProvider)
        val start = Instant.parse("2024-01-01T00:00:00Z")

        store.startSession(start, "/tmp/test.m4a")
        dispatcher.scheduler.advanceTimeBy(1_000)

        val state = store.state.value
        assertTrue(state is RecordingSessionState.Active)
        state as RecordingSessionState.Active
        assertEquals(1_000, state.elapsedMillis)
    }

    @Test
    fun `pausing freezes elapsed time until resumed`() = runTest(dispatcher) {
        val store = RecordingStateStore(scope, dispatcher, nowProvider)
        val start = Instant.parse("2024-01-01T00:00:00Z")

        store.startSession(start, "/tmp/test.m4a")
        dispatcher.scheduler.advanceTimeBy(1_500)
        store.setPaused()
        dispatcher.scheduler.advanceTimeBy(3_000)
        store.setResumed()
        dispatcher.scheduler.advanceTimeBy(2_500)

        val state = store.state.value as RecordingSessionState.Active
        assertEquals(4_000, state.elapsedMillis)
    }
}
