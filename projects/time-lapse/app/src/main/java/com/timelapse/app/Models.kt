package com.timelapse.app

data class SessionConfig(
    val intervalMs: Long,
    val cameraId: String,   // "main" or "ultrawide"
    val wbMode: String      // "auto","daylight","cloudy","shade","fluorescent","incandescent","tungsten"
)

data class SessionStatus(
    val isRunning: Boolean,
    val photoCount: Int,
    val startTimeMs: Long,       // epoch ms, 0 if not running
    val nextCaptureMs: Long,     // epoch ms of next scheduled capture
    val lastJpegPath: String?,   // absolute path on device
    val uploadPending: Int,
    val uploadFailed: Int,
    val driveSessionFolder: String?
)

data class CameraInfo(
    val id: String,      // "main" or "ultrawide"
    val displayName: String
)
