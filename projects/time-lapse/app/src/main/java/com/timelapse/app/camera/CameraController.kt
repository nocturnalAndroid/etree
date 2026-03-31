package com.timelapse.app.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.DngCreator
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import com.timelapse.app.CameraInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "CameraController"

/** Thrown when the camera disconnects unexpectedly during an active session. */
class CameraDisconnectedException(message: String) : Exception(message)

class CameraController(private val context: Context) {

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var cameraThread: HandlerThread
    private lateinit var cameraHandler: Handler

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var rawImageReader: ImageReader? = null
    private var jpegImageReader: ImageReader? = null
    private var cameraCharacteristics: CameraCharacteristics? = null

    private var lockedFocusDistance: Float = 0f
    private var resolvedCameraId: String? = null
    private var rawSupported: Boolean = false

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun getAvailableCameras(): List<CameraInfo> {
        val rearCameras = mutableListOf<Pair<String, Float>>()

        for (id in cameraManager.cameraIdList) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val facing = chars.get(CameraCharacteristics.LENS_FACING) ?: continue
            if (facing != CameraCharacteristics.LENS_FACING_BACK) continue
            val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            if (focalLengths.isNullOrEmpty()) continue
            rearCameras.add(id to focalLengths.min())
        }

        if (rearCameras.isEmpty()) return emptyList()
        val sorted = rearCameras.sortedBy { it.second }
        val result = mutableListOf<CameraInfo>()
        if (sorted.size >= 2) result.add(CameraInfo(id = "ultrawide", displayName = "Ultra-Wide Camera"))
        result.add(CameraInfo(id = "main", displayName = "Main Camera"))
        return result
    }

    suspend fun initialize(cameraId: String, wbMode: String) {
        startCameraThread()

        val physicalId = resolvePhysicalCameraId(cameraId)
            ?: throw IllegalArgumentException("Camera '$cameraId' not found on this device")
        resolvedCameraId = physicalId

        val chars = cameraManager.getCameraCharacteristics(physicalId)
        cameraCharacteristics = chars

        val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val rawSizes = streamMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
        rawSupported = !rawSizes.isNullOrEmpty()

        if (!rawSupported) Log.w(TAG, "RAW_SENSOR not supported on $physicalId; JPEG only")

        if (rawSupported) {
            val maxRaw = rawSizes!!.maxByOrNull { it.width.toLong() * it.height }!!
            rawImageReader = ImageReader.newInstance(maxRaw.width, maxRaw.height, ImageFormat.RAW_SENSOR, 2)
        }

        val jpegSizes = streamMap?.getOutputSizes(ImageFormat.JPEG)
        val maxJpeg = jpegSizes?.maxByOrNull { it.width.toLong() * it.height } ?: Size(4032, 3024)
        jpegImageReader = ImageReader.newInstance(maxJpeg.width, maxJpeg.height, ImageFormat.JPEG, 2)

        cameraDevice = openCamera(physicalId)
        captureSession = createCaptureSession()
        lockFocus(wbMode)
    }

    suspend fun capturePhoto(outputDir: File): Pair<File, File> {
        val session = captureSession ?: throw IllegalStateException("Call initialize() first.")
        val device = cameraDevice ?: throw CameraDisconnectedException("Camera device is null.")

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val rawDeferred = if (rawSupported) CompletableDeferred<Image>() else null
        val jpegDeferred = CompletableDeferred<Image>()

        rawImageReader?.setOnImageAvailableListener({ reader ->
            val img = reader.acquireNextImage()
            if (img != null) rawDeferred?.complete(img)
            else rawDeferred?.completeExceptionally(IllegalStateException("RAW reader returned null"))
        }, cameraHandler)

        jpegImageReader?.setOnImageAvailableListener({ reader ->
            val img = reader.acquireNextImage()
            if (img != null) jpegDeferred.complete(img)
            else jpegDeferred.completeExceptionally(IllegalStateException("JPEG reader returned null"))
        }, cameraHandler)

        val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            jpegImageReader?.surface?.let { addTarget(it) }
            if (rawSupported) rawImageReader?.surface?.let { addTarget(it) }
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, lockedFocusDistance)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO)
            set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF)
        }

        val captureResultDeferred = CompletableDeferred<TotalCaptureResult>()
        session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) {
                captureResultDeferred.complete(result)
            }
            override fun onCaptureFailed(s: CameraCaptureSession, r: CaptureRequest, failure: CaptureFailure) {
                captureResultDeferred.completeExceptionally(RuntimeException("Capture failed: ${failure.reason}"))
            }
        }, cameraHandler)

        val captureResult = captureResultDeferred.await()
        val jpegImage = jpegDeferred.await()

        return withContext(Dispatchers.IO) {
            outputDir.mkdirs()
            val jpegFile = File(outputDir, "timelapse_${timestamp}.jpg")
            jpegImage.use { img ->
                val buffer = img.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                FileOutputStream(jpegFile).use { it.write(bytes) }
            }

            val dngFile: File
            if (rawSupported && rawDeferred != null) {
                val rawImage = rawDeferred.await()
                dngFile = File(outputDir, "timelapse_${timestamp}.dng")
                rawImage.use { img ->
                    DngCreator(cameraCharacteristics!!, captureResult).use { dng ->
                        FileOutputStream(dngFile).use { dng.writeImage(it, img) }
                    }
                }
            } else {
                dngFile = jpegFile
            }
            Pair(dngFile, jpegFile)
        }
    }

    fun release() {
        try { captureSession?.close() } catch (e: Exception) { Log.w(TAG, e) }
        captureSession = null
        try { cameraDevice?.close() } catch (e: Exception) { Log.w(TAG, e) }
        cameraDevice = null
        rawImageReader?.close(); rawImageReader = null
        jpegImageReader?.close(); jpegImageReader = null
        if (::cameraThread.isInitialized && cameraThread.isAlive) cameraThread.quitSafely()
        cameraCharacteristics = null; resolvedCameraId = null; rawSupported = false; lockedFocusDistance = 0f
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun startCameraThread() {
        if (::cameraThread.isInitialized && cameraThread.isAlive) return
        cameraThread = HandlerThread("CameraControllerThread").also { it.start() }
        cameraHandler = Handler(cameraThread.looper)
    }

    private fun resolvePhysicalCameraId(logicalId: String): String? {
        val rearCameras = mutableListOf<Pair<String, Float>>()
        for (id in cameraManager.cameraIdList) {
            val chars = cameraManager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) continue
            val fl = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            if (fl.isNullOrEmpty()) continue
            rearCameras.add(id to fl.min())
        }
        if (rearCameras.isEmpty()) return null
        val sorted = rearCameras.sortedBy { it.second }
        return when (logicalId) {
            "ultrawide" -> if (sorted.size >= 2) sorted.first().first else null
            "main" -> sorted.last().first
            else -> null
        }
    }

    private suspend fun openCamera(physicalId: String): CameraDevice =
        suspendCancellableCoroutine { cont ->
            cameraManager.openCamera(physicalId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) { cont.resume(camera) }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cont.resumeWithException(CameraDisconnectedException("$physicalId disconnected"))
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cont.resumeWithException(RuntimeException("Camera open error: $error"))
                }
            }, cameraHandler)
        }

    private suspend fun createCaptureSession(): CameraCaptureSession {
        val device = cameraDevice ?: throw CameraDisconnectedException("No camera device")
        val surfaces = buildList {
            jpegImageReader?.surface?.let { add(it) }
            if (rawSupported) rawImageReader?.surface?.let { add(it) }
        }
        return suspendCancellableCoroutine { cont ->
            val cb = object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) { cont.resume(session) }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cont.resumeWithException(RuntimeException("Session config failed"))
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val executor: Executor = Executor { cameraHandler.post(it) }
                device.createCaptureSession(SessionConfiguration(SessionConfiguration.SESSION_REGULAR, surfaces.map { OutputConfiguration(it) }, executor, cb))
            } else {
                @Suppress("DEPRECATION") device.createCaptureSession(surfaces, cb, cameraHandler)
            }
        }
    }

    private suspend fun lockFocus(wbMode: String) {
        val session = captureSession ?: throw IllegalStateException("No session for AF lock")
        val device = cameraDevice ?: throw CameraDisconnectedException("No device for AF lock")

        val afTriggerRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            jpegImageReader?.surface?.let { addTarget(it) }
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO)
            applyWhiteBalance(this, wbMode)
        }

        val afLockedDeferred = CompletableDeferred<Float>()
        session.setRepeatingRequest(afTriggerRequest.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, result: TotalCaptureResult) {
                if (afLockedDeferred.isCompleted) return
                when (result.get(CaptureResult.CONTROL_AF_STATE)) {
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ->
                        afLockedDeferred.complete(result.get(CaptureResult.LENS_FOCUS_DISTANCE) ?: 0f)
                }
            }
            override fun onCaptureFailed(s: CameraCaptureSession, r: CaptureRequest, failure: CaptureFailure) {
                if (!afLockedDeferred.isCompleted) afLockedDeferred.completeExceptionally(RuntimeException("AF failed"))
            }
        }, cameraHandler)

        lockedFocusDistance = afLockedDeferred.await()
        session.stopRepeating()

        val lockedRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            jpegImageReader?.surface?.let { addTarget(it) }
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            set(CaptureRequest.LENS_FOCUS_DISTANCE, lockedFocusDistance)
            set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_AUTO)
            applyWhiteBalance(this, wbMode)
        }
        session.setRepeatingRequest(lockedRequest.build(), null, cameraHandler)
    }

    private fun applyWhiteBalance(builder: CaptureRequest.Builder, wbMode: String) {
        builder.set(CaptureRequest.CONTROL_AWB_MODE, mapWbMode(wbMode))
        if (wbMode != "auto") builder.set(CaptureRequest.CONTROL_AWB_LOCK, true)
    }

    private fun mapWbMode(wbMode: String): Int = when (wbMode) {
        "daylight" -> CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT
        "cloudy" -> CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT
        "shade" -> CaptureRequest.CONTROL_AWB_MODE_SHADE
        "fluorescent" -> CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
        "incandescent" -> CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT
        "tungsten" -> CaptureRequest.CONTROL_AWB_MODE_TWILIGHT
        else -> CaptureRequest.CONTROL_AWB_MODE_AUTO
    }
}
