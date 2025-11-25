package com.example.recorder.shortcut

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.DrawableRes
import com.example.recorder.R
import com.example.recorder.domain.model.RecordingSessionState
import com.example.recorder.domain.repository.RecordingRepository
import com.example.recorder.domain.usecase.ToggleRecordingUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class RecordingToggleTileService : TileService() {

    @Inject lateinit var repository: RecordingRepository
    @Inject lateinit var toggleRecordingUseCase: ToggleRecordingUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        stateJob = serviceScope.launch {
            repository.sessionState.collectLatest { state ->
                updateTile(state)
            }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        stateJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun { performToggle() }
        } else {
            performToggle()
        }
    }

    private fun performToggle() {
        serviceScope.launch {
            val state = withContext(Dispatchers.Default) { toggleRecordingUseCase() }
            updateTile(state)
        }
    }

    private fun updateTile(state: RecordingSessionState) {
        val tile = qsTile ?: return
        when (state) {
            is RecordingSessionState.Active -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.quick_tile_label)
                tile.subtitle = getString(R.string.quick_tile_subtitle_active)
                tile.icon = icon
            }
            is RecordingSessionState.Error -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.quick_tile_label)
                tile.subtitle = state.message
                tile.icon = icon
            }
            RecordingSessionState.Idle -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.quick_tile_label)
                tile.subtitle = getString(R.string.quick_tile_subtitle_idle)
                tile.icon = icon
            }
        }
        tile.updateTile()
    }

    private val icon
        get() = iconFromResource(R.drawable.ic_recorder)

    private fun iconFromResource(@DrawableRes drawable: Int) =
        android.graphics.drawable.Icon.createWithResource(this, drawable)
}
