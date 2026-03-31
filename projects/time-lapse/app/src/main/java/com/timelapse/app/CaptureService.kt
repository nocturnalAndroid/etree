package com.timelapse.app

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.timelapse.app.camera.CameraController
import com.timelapse.app.upload.DriveUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class CaptureService : LifecycleService() {

    private lateinit var cameraController: CameraController
    private lateinit var outputDir: File
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var intervalMs: Long = 60_000L
    private var cameraId: String = "main"
    private var wbMode: String = "auto"
    private var sessionName: String = ""

    private val captureRunnable = object : Runnable {
        override fun run() {
            serviceScope.launch {
                try {
                    val (dngFile, jpegFile) = cameraController.capturePhoto(outputDir)
                    SessionManager.onPhotoCaptured(applicationContext, dngFile, jpegFile)
                    val count = SessionManager.getStatus().photoCount
                    updateNotification(count)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val nextMs = System.currentTimeMillis() + intervalMs
            SessionManager.updateNextCaptureMs(nextMs)
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP) {
            stopCapture()
            return START_REDELIVER_INTENT
        }

        intervalMs = intent?.getLongExtra(EXTRA_INTERVAL_MS, 60_000L) ?: 60_000L
        cameraId = intent?.getStringExtra(EXTRA_CAMERA_ID) ?: "main"
        wbMode = intent?.getStringExtra(EXTRA_WB_MODE) ?: "auto"
        sessionName = intent?.getStringExtra(EXTRA_SESSION_NAME) ?: "session_${System.currentTimeMillis()}"

        startForeground(NOTIF_ID, buildNotification(0))
        outputDir = File(getExternalFilesDir(null), "sessions/$sessionName").also { it.mkdirs() }

        serviceScope.launch {
            try {
                cameraController = CameraController(applicationContext)
                cameraController.initialize(cameraId, wbMode)

                val uploader = DriveUploader(applicationContext)
                val folderId = uploader.createSessionFolder(sessionName)
                SessionManager.setFolderId(folderId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            handler.post(captureRunnable)
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    private fun stopCapture() {
        handler.removeCallbacks(captureRunnable)
        serviceScope.launch {
            try {
                if (::cameraController.isInitialized) cameraController.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                serviceScope.cancel()
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(photoCount: Int): Notification {
        val stopIntent = Intent(this, CaptureService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, App.CHANNEL_CAPTURE)
            .setContentTitle("Recording")
            .setContentText("$photoCount photos taken")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setSilent(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(photoCount: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIF_ID, buildNotification(photoCount))
    }

    companion object {
        const val ACTION_STOP = "com.timelapse.app.action.STOP_CAPTURE"
        const val EXTRA_INTERVAL_MS = "intervalMs"
        const val EXTRA_CAMERA_ID = "cameraId"
        const val EXTRA_WB_MODE = "wbMode"
        const val EXTRA_SESSION_NAME = "sessionName"
        private const val NOTIF_ID = 2
    }
}
