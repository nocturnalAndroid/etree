package com.timelapse.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.timelapse.app.HeartbeatWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        HeartbeatWorker.schedule(this)
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            CHANNEL_LISTENER, "Time Lapse Listener",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Always-on listener for remote control" }
            .also { manager.createNotificationChannel(it) }

        NotificationChannel(
            CHANNEL_CAPTURE, "Time Lapse Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Active photo capture session" }
            .also { manager.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_LISTENER = "timelapse_listener"
        const val CHANNEL_CAPTURE = "timelapse_capture"
        const val PREFS_NAME = "timelapse_prefs"
        const val PREF_NTFY_TOPIC = "ntfy_topic"
        const val PREF_DRIVE_ACCOUNT = "drive_account"
    }
}
