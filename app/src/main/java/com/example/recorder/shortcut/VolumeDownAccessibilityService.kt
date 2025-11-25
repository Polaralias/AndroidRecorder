package com.example.recorder.shortcut

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.recorder.domain.usecase.ToggleRecordingUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

@AndroidEntryPoint
class VolumeDownAccessibilityService : AccessibilityService() {

    @Inject lateinit var toggleRecordingUseCase: ToggleRecordingUseCase

    private val presses: ArrayDeque<Long> = ArrayDeque()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_DOWN) {
            val now = SystemClock.uptimeMillis()
            presses.addLast(now)
            trimOldPresses(now)
            if (presses.size >= REQUIRED_PRESSES) {
                presses.clear()
                triggerToggle()
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    private fun triggerToggle() {
        serviceScope.launch { toggleRecordingUseCase() }
    }

    private fun trimOldPresses(now: Long) {
        while (presses.isNotEmpty() && now - presses.first() > WINDOW_MS) {
            presses.removeFirst()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val REQUIRED_PRESSES = 3
        private const val WINDOW_MS = 1_500L
    }
}
