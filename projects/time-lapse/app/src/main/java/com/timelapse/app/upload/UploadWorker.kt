package com.timelapse.app.upload

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val TAG = "UploadWorker"

class UploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_DNG_PATH = "key_dng_path"
        const val KEY_JPEG_PATH = "key_jpeg_path"
        const val KEY_FOLDER_ID = "key_folder_id"
        const val TAG_UPLOAD = "timelapse_upload"
        private const val NOTIFICATION_CHANNEL_ID = "timelapse_upload_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val dngPath = inputData.getString(KEY_DNG_PATH)
        val jpegPath = inputData.getString(KEY_JPEG_PATH)
        val folderId = inputData.getString(KEY_FOLDER_ID)

        if (dngPath.isNullOrBlank() || jpegPath.isNullOrBlank() || folderId.isNullOrBlank()) return@withContext Result.failure()

        val dngFile = File(dngPath)
        val jpegFile = File(jpegPath)
        if (!dngFile.exists() || !jpegFile.exists()) return@withContext Result.failure()

        val uploader = DriveUploader(applicationContext)
        val drive = uploader.buildDriveService() ?: return@withContext Result.failure()

        try {
            drive.files().create(DriveFile().apply { name = dngFile.name; parents = listOf(folderId) }, FileContent("image/x-adobe-dng", dngFile)).execute()
            drive.files().create(DriveFile().apply { name = jpegFile.name; parents = listOf(folderId) }, FileContent("image/jpeg", jpegFile)).execute()
            dngFile.delete()
            jpegFile.delete()
            Result.success()
        } catch (e: UserRecoverableAuthIOException) {
            applicationContext.sendBroadcast(android.content.Intent(ACTION_AUTH_REQUIRED).apply {
                setPackage(applicationContext.packageName)
                putExtra(EXTRA_AUTH_INTENT, e.intent)
            })
            Result.failure()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = android.app.Notification.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TimeLapse Upload")
            .setContentText("Uploading to Google Drive…")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}
