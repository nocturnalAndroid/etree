package com.timelapse.app.upload

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private const val TAG = "DriveUploader"
const val ACTION_AUTH_REQUIRED = "com.timelapse.app.ACTION_AUTH_REQUIRED"
const val EXTRA_AUTH_INTENT = "extra_auth_intent"

class DriveUploader(private val context: Context) {

    internal fun buildDriveService(): Drive? {
        val prefs = context.getSharedPreferences("timelapse_prefs", Context.MODE_PRIVATE)
        val accountEmail = prefs.getString("drive_account", null)
        if (accountEmail.isNullOrBlank()) return null

        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
            .apply { selectedAccountName = accountEmail }

        return Drive.Builder(AndroidHttp.newCompatibleTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("TimeLapse")
            .build()
    }

    suspend fun createSessionFolder(sessionName: String): String = withContext(Dispatchers.IO) {
        val drive = buildDriveService() ?: throw IllegalStateException("No Drive account configured")
        val folderMetadata = DriveFile().apply {
            name = sessionName
            mimeType = "application/vnd.google-apps.folder"
        }
        try {
            drive.files().create(folderMetadata).setFields("id").execute().id
        } catch (e: UserRecoverableAuthIOException) {
            broadcastAuthRequired(e.intent)
            throw e
        }
    }

    fun enqueueUpload(dngPath: String, jpegPath: String, folderId: String) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_DNG_PATH to dngPath, UploadWorker.KEY_JPEG_PATH to jpegPath, UploadWorker.KEY_FOLDER_ID to folderId))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun getPendingCount(): Int {
        return WorkManager.getInstance(context).getWorkInfosByTag(UploadWorker.TAG_UPLOAD).get()
            .count { it.state == androidx.work.WorkInfo.State.ENQUEUED || it.state == androidx.work.WorkInfo.State.RUNNING || it.state == androidx.work.WorkInfo.State.BLOCKED }
    }

    fun getFailedCount(): Int {
        return WorkManager.getInstance(context).getWorkInfosByTag(UploadWorker.TAG_UPLOAD).get()
            .count { it.state == androidx.work.WorkInfo.State.FAILED }
    }

    fun getFolderUrl(folderId: String): String = "https://drive.google.com/drive/folders/$folderId"

    private fun broadcastAuthRequired(authIntent: Intent?) {
        context.sendBroadcast(Intent(ACTION_AUTH_REQUIRED).apply {
            setPackage(context.packageName)
            if (authIntent != null) putExtra(EXTRA_AUTH_INTENT, authIntent)
        })
    }
}
