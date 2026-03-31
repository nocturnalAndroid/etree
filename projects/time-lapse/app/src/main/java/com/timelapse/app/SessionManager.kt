package com.timelapse.app

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.timelapse.app.upload.DriveUploader
import com.timelapse.app.camera.CameraController
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

object SessionManager {

    @Volatile private var isRunning: Boolean = false
    private val photoCount = AtomicInteger(0)
    private val startTimeMs = AtomicLong(0L)
    private val nextCaptureMs = AtomicLong(0L)
    private val lastJpegPath = AtomicReference<String?>(null)
    private val driveSessionFolder = AtomicReference<String?>(null)
    private val folderId = AtomicReference<String?>(null)
    private val currentConfig = AtomicReference<SessionConfig?>(null)

    private var driveUploader: DriveUploader? = null

    private fun getUploader(context: Context): DriveUploader {
        return driveUploader ?: DriveUploader(context.applicationContext).also {
            driveUploader = it
        }
    }

    fun startSession(context: Context, config: SessionConfig) {
        currentConfig.set(config)
        isRunning = true
        photoCount.set(0)
        startTimeMs.set(System.currentTimeMillis())
        nextCaptureMs.set(System.currentTimeMillis() + config.intervalMs)
        lastJpegPath.set(null)

        val sessionName = "session_${startTimeMs.get()}"
        driveSessionFolder.set(sessionName)
        folderId.set(null)

        val intent = Intent(context, CaptureService::class.java).apply {
            putExtra(CaptureService.EXTRA_INTERVAL_MS, config.intervalMs)
            putExtra(CaptureService.EXTRA_CAMERA_ID, config.cameraId)
            putExtra(CaptureService.EXTRA_WB_MODE, config.wbMode)
            putExtra(CaptureService.EXTRA_SESSION_NAME, sessionName)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopSession(context: Context) {
        isRunning = false
        val intent = Intent(context, CaptureService::class.java).apply {
            action = CaptureService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun getStatus(): SessionStatus {
        val uploader = driveUploader
        return SessionStatus(
            isRunning = isRunning,
            photoCount = photoCount.get(),
            startTimeMs = startTimeMs.get(),
            nextCaptureMs = nextCaptureMs.get(),
            lastJpegPath = lastJpegPath.get(),
            uploadPending = uploader?.getPendingCount() ?: 0,
            uploadFailed = uploader?.getFailedCount() ?: 0,
            driveSessionFolder = driveSessionFolder.get()
        )
    }

    fun getAvailableCameras(context: Context): List<CameraInfo> {
        val controller = CameraController(context)
        return try {
            controller.getAvailableCameras()
        } finally {
            controller.release()
        }
    }

    fun getLastJpegFile(): File? {
        val path = lastJpegPath.get() ?: return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    fun getNtfyTopic(context: Context): String {
        val prefs = context.getSharedPreferences(App.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(App.PREF_NTFY_TOPIC, "") ?: ""
    }

    fun setNtfyTopic(context: Context, topic: String) {
        context.getSharedPreferences(App.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(App.PREF_NTFY_TOPIC, topic)
            .apply()
    }

    fun onPhotoCaptured(context: Context, dngFile: File, jpegFile: File) {
        photoCount.incrementAndGet()
        lastJpegPath.set(jpegFile.absolutePath)

        val folder = folderId.get()
        if (folder != null) {
            getUploader(context).enqueueUpload(dngFile.absolutePath, jpegFile.absolutePath, folder)
        }
    }

    fun setFolderId(id: String) {
        folderId.set(id)
    }

    fun getFolderId(): String? = folderId.get()

    fun updateNextCaptureMs(timestampMs: Long) {
        nextCaptureMs.set(timestampMs)
    }
}
