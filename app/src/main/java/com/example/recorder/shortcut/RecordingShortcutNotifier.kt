package com.example.recorder.shortcut

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.recorder.R
import com.example.recorder.MainActivity

object RecordingShortcutNotifier {

    private const val CHANNEL_ID = "quick_recorder_controls"
    private const val NOTIFICATION_ID = 2002

    fun showQuickActionNotification(context: Context) {
        if (!hasNotificationPermission(context)) return

        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_quick_action_title))
            .setContentText(context.getString(R.string.notification_quick_action_body))
            .setSmallIcon(R.drawable.ic_recorder)
            .setContentIntent(
                androidx.core.app.TaskStackBuilder.create(context).run {
                    addNextIntent(Intent(context, MainActivity::class.java))
                    getPendingIntent(0, pendingFlags)
                }
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play,
                    context.getString(R.string.notification_action_start),
                    PendingIntents.start(context)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    context.getString(R.string.notification_action_stop),
                    PendingIntents.stop(context)
                )
            )
            .setOngoing(false)
            .setAutoCancel(false)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_quick_actions),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private object PendingIntents {
        private const val REQUEST_CODE_START = 1201
        private const val REQUEST_CODE_STOP = 1202

        val pendingFlags: Int = PendingIntentFlags.value

        fun start(context: Context) =
            android.app.PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_START,
                RecordingShortcutReceiver.startIntent(context),
                pendingFlags
            )

        fun stop(context: Context) =
            android.app.PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_STOP,
                RecordingShortcutReceiver.stopIntent(context),
                pendingFlags
            )
    }

    private object PendingIntentFlags {
        val value: Int
            get() = android.app.PendingIntent.FLAG_IMMUTABLE or
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
    }
}
