package com.timelapse.app

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.timelapse.app.server.TimeLapseHttpServer

class ListenerService : Service() {

    private lateinit var httpServer: TimeLapseHttpServer

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        httpServer = TimeLapseHttpServer(this)
        httpServer.start()
        return START_STICKY
    }

    override fun onDestroy() {
        if (::httpServer.isInitialized) {
            httpServer.stop()
        }
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, App.CHANNEL_LISTENER)
            .setContentTitle("Time Lapse")
            .setContentText("Listening on :8080")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1
    }
}
