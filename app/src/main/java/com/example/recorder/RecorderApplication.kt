package com.example.recorder

import android.app.Application
import com.example.recorder.shortcut.RecordingShortcutNotifier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RecorderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RecordingShortcutNotifier.showQuickActionNotification(this)
    }
}
